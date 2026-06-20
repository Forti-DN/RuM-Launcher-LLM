package task.monitoring;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import controller.common.eventcell.EventData;
import edu.stanford.nlp.util.ArraySet;
import it.unibo.ai.rec.engine.FluentsConverter;
import it.unibo.ai.rec.model.Fluent;
import it.unibo.ai.rec.model.FluentGroup;
import it.unibo.ai.rec.model.FluentState;
import it.unibo.ai.rec.model.FluentsModel;
import it.unibo.ai.rec.model.MVI;
import it.unibo.ai.rec.model.NoGroupingStrategy;
import it.unibo.ai.rec.model.OpenMVI;
import javafx.concurrent.Task;
import core.monitoring.MonitorRunner;
import declare.DeclareModel;
import declare.DeclareParser;
import declare.DeclareParserException;
import declare.lang.Activity;
import declare.lang.data.EnumeratedData;
import declare.lang.data.FloatData;
import declare.lang.data.IntegerData;
import util.ConstraintTemplate;
import util.FileUtils;
import util.LogUtils;

public class MonitoringTaskMpDeclareAlloy extends Task<MonitoringTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File declModel;
	private File logFile;
	private boolean conflictCheck;

	private long taskStartTime;
	private FluentsConverter fluentsConverter = new FluentsConverter(new NoGroupingStrategy());
	private XConceptExtension xce = XConceptExtension.instance();

	private MonitoringTaskResult monitoringTaskResult;
	private Semaphore lock;
	private BlockingDeque<String> traceNames;

	public MonitoringTaskMpDeclareAlloy(Semaphore lock, BlockingDeque<String> traceNames) {
		super();
		this.lock = lock;
		this.traceNames = traceNames;
	}
	
	/*
	public MonitoringTaskMpDeclareAlloy(File declModel, File logFile, boolean conflictCheck) {
		super();
		this.declModel = declModel;
		this.logFile = logFile;
		this.conflictCheck = conflictCheck;
	}
	*/
	
	public void setDeclModel(File declModel) {
		this.declModel = declModel;
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	public void setConflictCheck(boolean conflictCheck) {
		this.conflictCheck = conflictCheck;
	}

	@Override
	protected MonitoringTaskResult call() throws Exception {
		try {
			this.taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			String preprocessedModel = FileUtils.preprocessModel(declModel, false);
			
			String declare = String.join("\n", Files.readAllLines( Path.of(preprocessedModel) ) );
			
			XLog xlog = LogUtils.convertToXlog(logFile);
			
			// Extending declare lines with activities and bindings found in the log
			// In this way MonitoringWithAlloy can work even with logs containing events not present in the declare model
			List<String> referenceDeclLines = extendModel(declare, xlog);
			
			Set<String> eventsInModel = new ArraySet<>();
			
			for (String line : referenceDeclLines) {
				// Used to detect events that are only in event log
				if (DeclareParser.isActivity(line))
					eventsInModel.add(line.substring(9)); 
				
				// Adding +1 to attribute value upper bound, because of a bug in MpDeclareAlloy (upper bound should be inclusive)
				if (DeclareParser.isData(line)) {
					String[] arributeValuesSplit = line.split(": ");
					
					if (arributeValuesSplit[1].matches("(integer|float)\\s+between\\s+\\d+(\\.\\d+)?\\s+and\\s+\\d+(\\.\\d+)?")) {
						String[] valueSplit = arributeValuesSplit[1].split(" ");
						
						String newUpperBound;
						if (arributeValuesSplit[1].startsWith("integer"))
							newUpperBound = String.valueOf( Integer.valueOf(valueSplit[valueSplit.length-1]) + 1 );
						else	// Starts with "float"
							newUpperBound = String.valueOf( Float.valueOf(valueSplit[valueSplit.length-1]) + 1 ); // Should actually add lowest amount possible
							
						valueSplit[valueSplit.length - 1] = newUpperBound;
						String newLine = arributeValuesSplit[0] + ": " + String.join(" ", valueSplit);
						referenceDeclLines.set(referenceDeclLines.indexOf(line), newLine);
					}
				}
			}
			
			String referenceDecl = String.join(System.lineSeparator(), referenceDeclLines);
			MonitorRunner monitorRunner = new MonitorRunner(conflictCheck, referenceDecl);
			
			// Lists for gathering the results
			List<String> traceNamesList = xlog.stream().map(trace -> xce.extractName(trace)).collect(Collectors.toList());
			traceNames.addAll(traceNamesList);
			List<List<EventData>> traceEventsData = new ArrayList<>(Collections.nCopies(traceNames.size(), (List<EventData>) null));

			// Process the log one event at a time (taken from LogStreamer and MoBuConClient.ProxySimulator)
			while (!traceNames.isEmpty()) {
				String traceName = traceNames.take();
				XTrace xtrace = xlog.stream().filter(trace -> traceName.equals(xce.extractName(trace))).findFirst().get();
				
				if (isCancelled()) break;
				
				List<EventData> eventDataList = LogUtils.createEventDataList(xtrace, null, false, false);

				for (int i=0; i < xtrace.size(); i++) {
					XEvent xevent = xtrace.get(i);
					String wrappedEvent = LogUtils.wrapEventForMonitoring(xevent, xtrace, i);
					
					lock.acquire();
					String fluentsString = monitorRunner.setTrace(wrappedEvent);
					lock.release();

					if (fluentsString == null)
						throw new Exception("Possible mismatch between the model and the log");
					
					// Finalizing the constraint states at the end of the trace
					if (i == xtrace.size()-1) {
						String endEvent = LogUtils.createEndEventString(xtrace); // Using a special event 'complete' that signals the end of the trace
						
						lock.acquire();
						fluentsString = monitorRunner.setTrace(endEvent);
						lock.release();
					}

					// Adding constraint states for the current event
					eventDataList.get(i).setConstraintStates(restoreTranslatedTemplates(processFluents(fluentsString), declModel));
				}

				int index = traceNamesList.indexOf( xce.extractName(xtrace) );
				traceEventsData.set(index, eventDataList);

				// Updating the result object
				monitoringTaskResult = new MonitoringTaskResult();
				monitoringTaskResult.setXlog(xlog);
				monitoringTaskResult.setTraceNames(traceNamesList);
				monitoringTaskResult.setTraceEventsData(traceEventsData);
				updateValue(monitoringTaskResult);
			}

			if (!isCancelled()) {
				logger.info("{} ({}) completed at: {} - total time: {}",
					this.getClass().getSimpleName(),
					this.hashCode(),
					System.currentTimeMillis(),
					(System.currentTimeMillis() - taskStartTime)
				);

			} else {
				logger.info("{} ({}) canceled at: {} - total time: {}",
					this.getClass().getSimpleName(),
					this.hashCode(),
					System.currentTimeMillis(),
					(System.currentTimeMillis() - taskStartTime)
				);
			}
			
			return monitoringTaskResult;
		
		} catch (InterruptedException e) {
			if (!isCancelled()) throw e;
			return monitoringTaskResult;
			
		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}
	}

	// Partially duplicates the code in other monitoring tasks
	private Map<String, String> processFluents(String fluentsString) throws Exception {
		Map<String, String> constraintStates = new HashMap<String, String>();

		FluentsModel model = fluentsConverter.toFluentsModel(fluentsString);

		for (FluentGroup fluentGroup : model.getFluentGroups().values()) {	// A group of constraints - no grouping is used
			// Removing brackets added by fluentsConverter
			String constraintString = new StringBuilder(fluentGroup.getName())
					.deleteCharAt(fluentGroup.getName().indexOf("("))
					.deleteCharAt(fluentGroup.getName().lastIndexOf(")")-1)
					.toString().trim();

			for (Fluent fluent : fluentGroup.getFluents().values())			// Constraint with all of the states it has had
				for (FluentState fluentState : fluent.getStates().values()) // A single state that the constraint has had
					for (MVI mvi : fluentState.getMVIs())					// One or more time intervals where the given constraint had a given state
						if (mvi.getClass() == OpenMVI.class)				// Assuming the latest state always has an open interval
							constraintStates.put(restoreConstraintFormat(constraintString), fluentState.getName());
		}

		return constraintStates;
	}
	
	private String restoreConstraintFormat(String s) {
		
		Matcher constraintMatcher = Pattern.compile("(.+)\\[(.+),\\s*(.+)\\](.+)").matcher(s);
		if (constraintMatcher.find()) {
			String templ = constraintMatcher.group(1);
			// Restoring blank spaces in the templates - NOTE: Order of replace matters!
			templ = templ.replace("RespondedExistence", ConstraintTemplate.Responded_Existence.toString());
			templ = templ.replace("CoExistence", ConstraintTemplate.CoExistence.toString());
			templ = templ.replace("ExclusiveChoice", ConstraintTemplate.Exclusive_Choice.toString());
			templ = templ.replace("AlternateResponse", ConstraintTemplate.Alternate_Response.toString());
			templ = templ.replace("ChainResponse", ConstraintTemplate.Chain_Response.toString());
			templ = templ.replace("AlternatePrecedence", ConstraintTemplate.Alternate_Precedence.toString());
			templ = templ.replace("ChainPrecedence", ConstraintTemplate.Chain_Precedence.toString());
			templ = templ.replace("AlternateSuccession", ConstraintTemplate.Alternate_Succession.toString());
			templ = templ.replace("ChainSuccession", ConstraintTemplate.Chain_Succession.toString());
			
			if (ConstraintTemplate.getByTemplateName(templ).getReverseActivationTarget())
				// Restoring correct position of activation and target for Precedence templates
				s = templ + "[" + constraintMatcher.group(3) + ", " + constraintMatcher.group(2) + "]" + constraintMatcher.group(4);
			else
				s = templ + "[" + constraintMatcher.group(2) + ", " + constraintMatcher.group(3) + "]" + constraintMatcher.group(4);
		}
		
    	// Restoring the position of numeric quantifiers
    	Matcher quantifierRestoreMatcher = Pattern.compile("^(Existence|Absence|Exactly)\\[(.+),\\s*(\\d+)\\](.+)").matcher(s);
    	if (quantifierRestoreMatcher.find()) {
    		String template = quantifierRestoreMatcher.group(1);
    		String activation = quantifierRestoreMatcher.group(2);
    		String quantifier = quantifierRestoreMatcher.group(3);
    		String dataConds = quantifierRestoreMatcher.group(4);
    		
    		if (!template.equals("Exactly") && Integer.parseInt(quantifier) == 1)
    			s = template + "[" + activation + "]" + dataConds;
    		else
    			s = template + quantifier + "[" + activation + "]" + dataConds;
    	}
    	
    	return s;
    }
	
	private Map<String, String> restoreTranslatedTemplates(Map<String, String> constraintStates, File declModel) throws IOException {
		// Alloy monitoring doesn't support natively Not Succession, Not Chain Succession and Not Co-Existence
		// So, it translates them resp. with Not Response, Not Chain Response and Not Responded Existence
		// Now there is the need to translate them back!
		List<String> constraintLines = Files.readAllLines(declModel.toPath()).stream()
											.filter(line -> DeclareParser.isDataConstraint(line)
													&& line.matches("^Not(( Chain)? Succession| Co-Existence).*$"))
											.collect(Collectors.toList());
		
		for (String l : constraintLines) {
			String template = l.split("\\[")[0];
			
			String translated = "";
			switch(template) {
			case "Not Succession":
				translated = l.replaceFirst("Not Succession", "Not Response");
				break;
			case "Not Chain Succession":
				translated = l.replaceFirst("Not Chain Succession", "Not Chain Response");
				break;
			case "Not Co-Existence":
				translated = l.replaceFirst("Not Co-Existence", "Not Co-Existence");
				break;
			}
			
			constraintStates.put(l, constraintStates.remove(translated));
		}
		
		return constraintStates;
	}
	
	private List<String> extendModel(String declare, XLog log) throws DeclareParserException {
		DeclareModel extendedModel = DeclareParser.parse(declare);
		
		// Extending model with activities and data bindings from the log
		for (XTrace trace : log) {
			// TODO: Maybe it is needed also to extract trace attributes here
			for (XEvent event : trace) {
				String actName = XConceptExtension.instance().extractName(event);
				extendedModel.getActivities().add( new Activity(actName) );
								
				for (XAttribute att : event.getAttributes().values()) {
					String attName = att.getKey();
					
					if (!attName.equals(XConceptExtension.KEY_NAME)
							&& !attName.equals(XTimeExtension.KEY_TIMESTAMP)
							&& !attName.equals(XLifecycleExtension.KEY_TRANSITION)) {
												
						if (att instanceof XAttributeContinuous) {
							float val = Float.parseFloat(att.toString());
							Optional<FloatData> opt = extendedModel.getFloatData().stream()
														.filter(attr -> attr.getType().equals(attName))
														.findFirst();
							if (opt.isPresent()) {
								FloatData floatAtt = opt.get();
								
								if (val < floatAtt.getMin()) {
									extendedModel.getFloatData().remove(floatAtt);
									extendedModel.getFloatData().add(new FloatData(floatAtt.getType(), val, floatAtt.getMax(), floatAtt.isRequired()));
								
								} else if (val > floatAtt.getMax()) {
									extendedModel.getFloatData().remove(floatAtt);
									extendedModel.getFloatData().add(new FloatData(floatAtt.getType(), floatAtt.getMin(), val, floatAtt.isRequired()));
								}
							
							} else {
								extendedModel.getFloatData().add(new FloatData(attName, val, val, true));
							}
							
						} else if (att instanceof XAttributeDiscrete) {
							int val = Integer.parseInt(att.toString());
							Optional<IntegerData> opt = extendedModel.getIntegerData().stream()
														.filter(attr -> attr.getType().equals(attName))
														.findFirst();
							if (opt.isPresent()) {
								IntegerData intAtt = opt.get();
								
								if (val < intAtt.getMin()) {
									extendedModel.getIntegerData().remove(intAtt);
									extendedModel.getIntegerData().add(new IntegerData(intAtt.getType(), val, intAtt.getMax(), intAtt.isRequired()));
								
								} else if (val > intAtt.getMax()) {
									extendedModel.getIntegerData().remove(intAtt);
									extendedModel.getIntegerData().add(new IntegerData(intAtt.getType(), intAtt.getMin(), val, intAtt.isRequired()));
								}
							
							} else {
								extendedModel.getIntegerData().add(new IntegerData(attName, val, val, true));
							}	
							
						} else {	// All attributes that aren't numerical (int or float) will be treated as literal ones (enumeration)
							String val = att.toString();
							Optional<EnumeratedData> opt = extendedModel.getEnumeratedData().stream()
															.filter(attr -> attr.getType().equals(attName))
															.findFirst();
							if (opt.isPresent()) {
								EnumeratedData enumAtt = opt.get();
								
								if (!enumAtt.getValues().contains(val)) {
									extendedModel.getEnumeratedData().remove(enumAtt);
									List<String> valList = enumAtt.getValues();
									valList.add(val);
									extendedModel.getEnumeratedData().add(new EnumeratedData(enumAtt.getType(), valList, true));
								}
								
							} else {
								extendedModel.getEnumeratedData().add(new EnumeratedData(attName, List.of(val), true));
							}
						}
						
						if (extendedModel.getActivityToData().containsKey(actName))
							extendedModel.getActivityToData().get(actName).add(attName);
						else
							extendedModel.getActivityToData().put(actName, new HashSet<String>(Arrays.asList(attName)));
						
						if (extendedModel.getDataToActivity().containsKey(attName))
							extendedModel.getDataToActivity().get(attName).add(actName);
						else
							extendedModel.getDataToActivity().put(attName, new HashSet<String>(Arrays.asList(actName)));
					}
				}
			}
		}
		
		List<String> extendedLines = new ArrayList<>();
		
		for (Activity act : extendedModel.getActivities()) {
			extendedLines.add("activity " + act.getName());
			
			if (extendedModel.getActivityToData().containsKey(act.getName()))
				extendedLines.add("bind " + act.getName() + ": " + String.join(", ", extendedModel.getActivityToData().get(act.getName())) );
		}
		
		for (IntegerData intAtt : extendedModel.getIntegerData())
			extendedLines.add(intAtt.getType() + ": integer between " + intAtt.getMin() + " and " + intAtt.getMax());
		
		for (FloatData floatAtt : extendedModel.getFloatData())
			extendedLines.add(floatAtt.getType() + ": float between " + floatAtt.getMin() + " and " + floatAtt.getMax());
		
		for (EnumeratedData enumAtt : extendedModel.getEnumeratedData())
			extendedLines.add(enumAtt.getType() + ": " + String.join(", ", enumAtt.getValues()));
		
		
		// Adding constraints from the original model 
		for (String line : declare.split("\\n"))
			if (DeclareParser.isConstraint(line) || DeclareParser.isDataConstraint(line))
				extendedLines.add(line);
		
		return extendedLines;
	}
	
	@Override
	public String toString() {
		return "MonitoringTaskMpDeclareAlloy [declModel=" + declModel + ", logFile=" + logFile + ", conflictCheck="
				+ conflictCheck + ", taskStartTime=" + taskStartTime + "]";
	}
}
