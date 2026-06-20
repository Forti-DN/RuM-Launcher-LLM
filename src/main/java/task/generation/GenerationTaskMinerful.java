package task.generation;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;

import javafx.concurrent.Task;
import minerful.concept.ProcessModel;
import minerful.concept.TaskChar;
import minerful.concept.TaskCharArchive;
import minerful.concept.TaskCharFactory;
import minerful.concept.constraint.Constraint;
import minerful.concept.constraint.ConstraintsBag;
import minerful.logmaker.MinerFulLogMaker;
import minerful.logmaker.params.LogMakerParameters;
import treedata.TreeDataAttribute;
import util.ConstraintTemplate;
import util.ConstraintUtils;
import util.ModelUtils;

public class GenerationTaskMinerful extends Task<GenerationTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File declModel; //Not actually used, but keeping it for logging purposes
	private int minTraceLength;
	private int maxTraceLength;
	private int numberOfTraces;
	private List<String> constraintList; //Generation input
	private List<TreeDataAttribute> traceAttributes;

	public GenerationTaskMinerful() {
		super();
	}

	public GenerationTaskMinerful(File declModel, int minTraceLength, int maxTraceLength, int numberOfTraces, List<String> constraintList) {
		super();
		this.declModel = declModel;
		this.minTraceLength = minTraceLength;
		this.maxTraceLength = maxTraceLength;
		this.numberOfTraces = numberOfTraces;
		this.constraintList = constraintList;
	}

	public void setDeclModel(File declModel) {
		this.declModel = declModel;
	}

	public void setMinTraceLength(int minTraceLength) {
		this.minTraceLength = minTraceLength;
	}

	public void setMaxTraceLength(int maxTraceLength) {
		this.maxTraceLength = maxTraceLength;
	}

	public void setNumberOfTraces(int numberOfTraces) {
		this.numberOfTraces = numberOfTraces;
	}

	public void setConstraintList(List<String> constraintList) {
		this.constraintList = constraintList;
	}
	
	public void setTraceAttributes(List<TreeDataAttribute> traceAttributes) {
		this.traceAttributes = traceAttributes;
	}

	@Override
	protected GenerationTaskResult call() throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			List<String> allActivitiesInvolved = new ArrayList<>(getActivitiesMap().values());

			TaskCharFactory tChFactory = new TaskCharFactory();
			List<TaskChar> tcList = allActivitiesInvolved.stream().map(tChFactory::makeTaskChar).collect(Collectors.toList());
			TaskChar[] tcArray = tcList.toArray(new TaskChar[tcList.size()]);
			TaskCharArchive taChaAr = new TaskCharArchive(tcArray);

			ConstraintsBag bag = new ConstraintsBag(taChaAr.getTaskChars());
			Map<Integer,List<String>> constraintsMap = getConstraintParametersMap();
			Map<Integer,String> templatesMap = getTemplatesMap();

			for (Map.Entry<Integer, List<String>> e : constraintsMap.entrySet()) {
				ConstraintTemplate t = ConstraintTemplate.getByTemplateName( templatesMap.get(e.getKey()) );
				List<TaskChar> involved = e.getValue().stream()
											.map(taChaAr::getTaskChar)
											.collect(Collectors.toList());

				Constraint constraint = ConstraintUtils.getMinerfulConstraint(t, involved);
				if(constraint != null) 
					bag.add(constraint);
			}

			ProcessModel proMod = new ProcessModel(taChaAr, bag);
			LogMakerParameters logMakParameters = new LogMakerParameters(
					minTraceLength,
					maxTraceLength,
					Long.valueOf(numberOfTraces));
			MinerFulLogMaker logMak = new MinerFulLogMaker(logMakParameters);
			XLog generatedLog = logMak.createLog(proMod);
			
			for (XTrace trace : generatedLog) {
				XAttributeMap attributeMap = trace.getAttributes();

				for (TreeDataAttribute att : traceAttributes) {
					String rndValue = null;
					
					switch (att.getAttributeType()) {
					case ENUMERATION:
						int rndIndex = ThreadLocalRandom.current().nextInt(0, att.getPossibleValues().size());
						rndValue = att.getPossibleValues().get(rndIndex);
						break;
						
					case FLOAT:
						Random r = new Random();
						rndValue = String.valueOf( att.getValueFrom().floatValue() + r.nextFloat() * (att.getValueTo().floatValue() - att.getValueFrom().floatValue()) );
						break;
						
					case INTEGER:
						rndValue = String.valueOf( ThreadLocalRandom.current().nextInt(att.getValueFrom().intValue(), att.getValueTo().intValue() + 1) );
						break;
					}
					
					attributeMap.put(att.getAttributeName(), new XAttributeLiteralImpl(att.getAttributeName(), rndValue));
				}
				
				attributeMap.put("trace:type", new XAttributeLiteralImpl("trace:type", "positive"));
			}

			//Preparing the result object
			GenerationTaskResult generationTaskResult = new GenerationTaskResult();
			generationTaskResult.setGeneratedLog(generatedLog);

			logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);
			
			return generationTaskResult;
		
		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}
	}

	private HashMap<Integer,String> getActivitiesMap() {
		HashMap<Integer,String> activitiesMap = new HashMap<>();
		List<List<String>> tmp = new ArrayList<>();
		tmp.addAll(getConstraintParametersMap().values());
		List<String> list = new ArrayList<>();
		tmp.forEach(list::addAll);
		ModelUtils.getActivityList(declModel).forEach(list::add);
		List<String> activityList = list.stream().distinct().collect(Collectors.toList());
		int index = 0;
		for(String s:activityList) {
			activitiesMap.put(index, s);
			index++;
		}

		return activitiesMap;
	}

	private HashMap<Integer,List<String>> getConstraintParametersMap() {
		HashMap<Integer,List<String>> constraintParametersMap = new HashMap<>();
		int index = 0;
		for(String constraint : this.constraintList) {
			int lbr = constraint.indexOf('[');
			int rbr = constraint.indexOf(']');
			String acts = constraint.substring(lbr+1,rbr);
			int comma = acts.indexOf(',');
			if(comma != -1) {
				String a = acts.substring(0,comma);
				String b = acts.substring(comma+2);
				List<String> l = new ArrayList<>();
				l.add(a); l.add(b);
				constraintParametersMap.put(index, l);
				index++;
			
			} else {
				String a = acts;
				List<String> l = new ArrayList<>();
				l.add(a);
				constraintParametersMap.put(index, l);
				index++;
			}
		}

		return constraintParametersMap;
	}

	private HashMap<Integer,String> getTemplatesMap() {
		List<String> listOfConstraints = new ArrayList<>();
		constraintList.forEach(listOfConstraints::add);

		HashMap<Integer,String> templatesMap = new HashMap<>();
		int index = 0;
		for(String s:listOfConstraints) {
			int lbr = s.indexOf('[');
			int rbr = s.indexOf(']');
			String t = s.substring(0, lbr);
			String acts = s.substring(lbr+1,rbr);
			int comma = acts.indexOf(',');
			if(comma != -1) {
				String a = acts.substring(0,comma);
				String b = acts.substring(comma+2);
				templatesMap.put(index, t);
				List<String> l = new ArrayList<>();
				l.add(a); l.add(b);
				index++;
			
			} else {
				String a = acts;
				templatesMap.put(index, t);
				List<String> l = new ArrayList<>();
				l.add(a);
				index++;
			}
		}
		
		return templatesMap;
	}

	@Override
	public String toString() {
		return "GenerationTaskMinerful [declModel=" + declModel + ", minTraceLength=" + minTraceLength
				+ ", maxTraceLength=" + maxTraceLength + ", numberOfTraces=" + numberOfTraces + "]";
	}
}
