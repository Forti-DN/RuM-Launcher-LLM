package task.conformance;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.framework.ActivityMatchCost;
import org.processmining.plugins.DataConformance.visualization.DataAwareStepTypes;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;
import org.processmining.plugins.DeclareConformance.Replayer;
import org.processmining.plugins.DeclareConformance.ResultReplayDeclare;
import org.processmining.plugins.DeclareConformance.ViolationIdentifier;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;

import javafx.concurrent.Task;
import task.conformance.ActivityConformanceType.Type;

public class ConformanceTaskReplayer extends Task<ConformanceTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File xmlModel;
	private File logFile;
	private Map<ReplayableActivityDefinition,XEventClass> activityMapping;
	private List<ActivityMatchCost> activityMatchCosts;

	public ConformanceTaskReplayer() {
		super();
	}

	public ConformanceTaskReplayer(File xmlModel, File logFile,
			Map<ReplayableActivityDefinition, XEventClass> activityMapping,
			List<ActivityMatchCost> activityMatchCosts) {
		super();
		this.xmlModel = xmlModel;
		this.logFile = logFile;
		this.activityMapping = activityMapping;
		this.activityMatchCosts = activityMatchCosts;
	}

	public void setXmlModel(File xmlModel) {
		this.xmlModel = xmlModel;
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	public void setActivityMapping(Map<ReplayableActivityDefinition, XEventClass> activityMapping) {
		this.activityMapping = activityMapping;
	}

	public void setActivityMatchCosts(List<ActivityMatchCost> activityMatchCosts) {
		this.activityMatchCosts = activityMatchCosts;
	}

	@Override
	protected ConformanceTaskResult call() throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			//Running the conformance check (alignment would be more precise to say)
			ResultReplayDeclare resultReplayDeclare = Replayer.run(logFile.getAbsolutePath(), xmlModel.getAbsolutePath(), activityMapping, activityMatchCosts);

			//Setting the trace list for the task result
			ConformanceTaskResult conformanceTaskResult = new ConformanceTaskResult();

			//Sorting traces by name (this order is used when displaying results grouped by constraint)
			List<XTrace> analysisResultTraces = new ArrayList<>(resultReplayDeclare.getAlignedLog());
			analysisResultTraces.sort((o1, o2) -> {
				String traceName1 = XConceptExtension.instance().extractName(o1) != null ? XConceptExtension.instance().extractName(o1) : "";
				String traceName2 = XConceptExtension.instance().extractName(o2) != null ? XConceptExtension.instance().extractName(o2) : "";
				return traceName1.compareTo(traceName2);
			});
			conformanceTaskResult.setTraceList(analysisResultTraces);


			//Setting the constraint list for the task result
			ArrayList<String> constraintLabels = new ArrayList<>();
			Map<String,String> constraintsToLabels = new HashMap<>(); //Needed for setting constraint statistics
			for (ConstraintDefinition constraintDefinition : resultReplayDeclare.getDeclareModel().getModel().getConstraintDefinitions()) {
				String constraintLabel = constraintDefinition.getCaption() + "\n" + constraintDefinition.getCondition();
				constraintsToLabels.put(constraintDefinition.getCaption(), constraintLabel);
				constraintLabels.add(constraintLabel);
			}
			conformanceTaskResult.setConstraintList(constraintLabels);

			//Maps for counting constraint statistics
			Map<String, Integer> constraintInsertionCounts = new HashMap<>();
			Map<String, Integer> constraintDeletionCounts = new HashMap<>();
			
			//For counting global statistics
			int tracesWithInsertions = 0;
			int tracesWithDeletions = 0;

			//Results processing
			for (XTrace xtrace : analysisResultTraces) {
				String traceName = XConceptExtension.instance().extractName(xtrace);
				Alignment alignment = resultReplayDeclare.getAlignmentByTraceName(traceName);
				int insertionsToTrace = 0;
				int deletionsToTrace = 0;
				ViolationIdentifier violationIdentifier = new ViolationIdentifier(alignment.getLogTrace(),alignment.getProcessTrace(),resultReplayDeclare.getDeclareModel());
				List<DataAwareStepTypes> dataAwareStepTypes = alignment.getStepTypes();

				// Setting up a map for tracking activity conformance types for each constraint separately
				Map<String, List<ActivityConformanceType>> constraintConformanceMap = new HashMap<>();
				for (String constraintLabel : constraintLabels)
					constraintConformanceMap.put(constraintLabel, new ArrayList<>());

				// Computing an average of time intervals between subsequent events in order to set a realistic interval for insertions at the extremes of the trace
				Duration avgTimeInterval = Duration.ZERO;
				for (int i=0; i < xtrace.size(); i++) {
					if (i > 0) {
						Date prevTimestamp = XTimeExtension.instance().extractTimestamp(xtrace.get(i-1));
						Date currTimestamp = XTimeExtension.instance().extractTimestamp(xtrace.get(i));
						
						if (prevTimestamp!=null && currTimestamp!=null)
							avgTimeInterval = avgTimeInterval.plusMillis(currTimestamp.getTime() - prevTimestamp.getTime());
					}
				}
				avgTimeInterval = avgTimeInterval.dividedBy(xtrace.size());
				
				// Counting constraint statistics and setting trace statistics
				for (int i=0; i < dataAwareStepTypes.size(); i++) { //Going trough each activity in the aligned trace
					if (dataAwareStepTypes.get(i) == DataAwareStepTypes.MREAL) {
						insertionsToTrace++;
						addToStatisticsList(violationIdentifier.getViolationsSolved(i), constraintsToLabels, constraintInsertionCounts);
						for (String constraintLabel : constraintLabels) {
							constraintConformanceMap.get(constraintLabel).add(new ActivityConformanceType(Type.INSERTION_OTHER));
						}
						for (String solvedViolation : violationIdentifier.getViolationsSolved(i)) {
							String constraint = solvedViolation.replace("(", ": ").replace(")", "").replace(",", ", ");
							String constraintLabel = constraintsToLabels.get(constraint);
							if (constraintLabel != null) {
								constraintConformanceMap.get(constraintLabel).set(constraintConformanceMap.get(constraintLabel).size()-1, new ActivityConformanceType(Type.INSERTION));
							}
						}
						
						// Computing a coherent timestamp for the inserted event
						Date newTimestamp;
						if (i == 0) {
							Date firstTimestamp = XTimeExtension.instance().extractTimestamp( xtrace.get(0) );
							newTimestamp = new Date(firstTimestamp.getTime() - avgTimeInterval.toMillis());

						} else if (i >= xtrace.size()) {
							Date lastTimestamp = XTimeExtension.instance().extractTimestamp( xtrace.get(xtrace.size()-1) );
							newTimestamp = new Date(lastTimestamp.getTime() - avgTimeInterval.multipliedBy((long)i-xtrace.size()+1).toMillis());

						} else {
							Date prevTimestamp = XTimeExtension.instance().extractTimestamp( xtrace.get(i-1) );
							Date currTimestamp = XTimeExtension.instance().extractTimestamp( xtrace.get(i) );

							if (prevTimestamp.before(currTimestamp))
								newTimestamp = new Date( ThreadLocalRandom.current().nextLong(prevTimestamp.getTime(), currTimestamp.getTime()) );
							else
								newTimestamp = prevTimestamp;
						}
						
						// Adding an event that should be in the trace based on the alignment
						XEvent xevent = new XEventImpl(new XAttributeMapImpl());
						XConceptExtension.instance().assignName(xevent, alignment.getProcessTrace().get(i).getActivity() );
						XLifecycleExtension.instance().assignStandardTransition(xevent, StandardModel.COMPLETE);
						XTimeExtension.instance().assignTimestamp(xevent, newTimestamp);
						xtrace.add(i, xevent);

					} else if (dataAwareStepTypes.get(i) == DataAwareStepTypes.L) {
						deletionsToTrace++;
						addToStatisticsList(violationIdentifier.getViolationsSolved(i), constraintsToLabels, constraintDeletionCounts);
						for (String constraintLabel : constraintLabels) {
							constraintConformanceMap.get(constraintLabel).add(new ActivityConformanceType(Type.DELETION_OTHER));
						}
						for (String solvedViolation : violationIdentifier.getViolationsSolved(i)) {
							String constraint = solvedViolation.replace("(", ": ").replace(")", "").replace(",", ", ");
							String constraintLabel = constraintsToLabels.get(constraint);
							if (constraintLabel != null) {
								constraintConformanceMap.get(constraintLabel).set(constraintConformanceMap.get(constraintLabel).size()-1, new ActivityConformanceType(Type.DELETION));
							}
						}
					} else {
						for (String constraintLabel : constraintLabels) {
							constraintConformanceMap.get(constraintLabel).add(new ActivityConformanceType(Type.NONE));
						}
					}
				}

				//Adding the result details of each constraint
				for (String constraintLabel : constraintLabels) {
					ConformanceTaskResultDetail conformanceTaskResultDetail = new ConformanceTaskResultDetail();
					String traceNameStr = XConceptExtension.instance().extractName(xtrace);
					conformanceTaskResultDetail.setTraceName(traceNameStr != null ? traceNameStr : "");
					conformanceTaskResultDetail.setXtrace(xtrace);
					conformanceTaskResultDetail.setConstraint(constraintLabel);
					conformanceTaskResultDetail.setActivityConformanceTypes(constraintConformanceMap.get(constraintLabel));
					conformanceTaskResult.addResultDetail(conformanceTaskResultDetail);
				}

				//Setting trace statistics
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.FITNESS, String.format("%.2f", alignment.getFitness()));
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.INSERTIONS, Integer.toString(insertionsToTrace));
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.DELETIONS, Integer.toString(deletionsToTrace));
				
				if (insertionsToTrace > 0) {
					tracesWithInsertions++;
				}
				if (deletionsToTrace > 0) {
					tracesWithDeletions++;
				}
			}

			//Setting constraint statistics
			for (String constraint : constraintLabels) {
				if (constraintInsertionCounts.get(constraint) == null) {
					conformanceTaskResult.addConstraintStatistic(constraint, ConformanceStatisticType.INSERTIONS, "0");
				} else {
					conformanceTaskResult.addConstraintStatistic(constraint, ConformanceStatisticType.INSERTIONS, Integer.toString(constraintInsertionCounts.get(constraint)));
				}
				if (constraintDeletionCounts.get(constraint) == null) {
					conformanceTaskResult.addConstraintStatistic(constraint, ConformanceStatisticType.DELETIONS, "0");
				} else {
					conformanceTaskResult.addConstraintStatistic(constraint, ConformanceStatisticType.DELETIONS, Integer.toString(constraintDeletionCounts.get(constraint)));
				}
			}
			
			//Setting global statistics
			conformanceTaskResult.setGlobalStatistic(ConformanceStatisticType.INSERTIONS, Integer.toString(tracesWithInsertions));
			conformanceTaskResult.setGlobalStatistic(ConformanceStatisticType.DELETIONS, Integer.toString(tracesWithDeletions));
			double meanFitness = Math.floor(resultReplayDeclare.meanFitness * 100) / 100.0; //Needed because otherwise 0.995 gets rounded to 1.00 by String.format
			conformanceTaskResult.setGlobalStatistic(ConformanceStatisticType.FITNESS, String.format("%.2f", meanFitness));

			//Prepares the result groups for displaying
			conformanceTaskResult.createResultGroupings();

			logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);
			
			return conformanceTaskResult;
		
		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}
	}

	// Increments the statistics count for each constraint that had a violation
	private void addToStatisticsList(String[] violationsSolved, Map<String, String> constraintsToLabels, Map<String, Integer> statisticsList) {
		for (String solvedViolation : violationsSolved) {
			String constraint = solvedViolation.replace("(", ": ").replace(")", "").replace(",", ", ");
			String constraintLabel = constraintsToLabels.get(constraint);
			if (constraintLabel != null) {
				if (statisticsList.get(constraintLabel) == null) {
					statisticsList.put(constraintLabel, 1);
				} else {
					statisticsList.put(constraintLabel, statisticsList.get(constraintLabel) + 1);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "ConformanceTaskReplayer [xmlModel=" + xmlModel + ", logFile=" + logFile + "]";
	}
}
