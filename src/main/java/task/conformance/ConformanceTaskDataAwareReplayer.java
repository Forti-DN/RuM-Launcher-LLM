package task.conformance;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.OffsetDateTime;
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
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.framework.ActivityMatchCost;
import org.processmining.plugins.DataConformance.framework.VariableMatchCost;
import org.processmining.plugins.DataConformance.visualization.DataAwareStepTypes;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;
import org.processmining.plugins.DeclareConformance.ViolationIdentifier;
import org.processmining.plugins.dataawaredeclarereplayer.Runner;
import org.processmining.plugins.dataawaredeclarereplayer.gui.AnalysisSingleResult;
import org.processmining.plugins.dataawaredeclarereplayer.result.AlignmentAnalysisResult;
import org.processmining.plugins.declareminer.visualizing.AssignmentModel;
import org.processmining.plugins.declareminer.visualizing.AssignmentViewBroker;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;
import org.processmining.plugins.declareminer.visualizing.DeclareMap;
import org.processmining.plugins.declareminer.visualizing.XMLBrokerFactory;

import javafx.concurrent.Task;
import task.conformance.ActivityConformanceType.Type;
import util.LogUtils;

public class ConformanceTaskDataAwareReplayer extends Task<ConformanceTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File xmlModel;
	private File logFile;
	private Map<ReplayableActivityDefinition,XEventClass> activityMapping;
	private List<ActivityMatchCost> activityMatchCosts;
	private List<VariableMatchCost> variableMatchCosts;

	public ConformanceTaskDataAwareReplayer() {
		super();
	}

	public ConformanceTaskDataAwareReplayer(File xmlModel, File logFile,
			Map<ReplayableActivityDefinition, XEventClass> activityMapping, List<ActivityMatchCost> activityMatchCosts,
			List<VariableMatchCost> variableMatchCosts) {
		super();
		this.xmlModel = xmlModel;
		this.logFile = logFile;
		this.activityMapping = activityMapping;
		this.activityMatchCosts = activityMatchCosts;
		this.variableMatchCosts = variableMatchCosts;
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

	public void setVariableMatchCosts(List<VariableMatchCost> variableMatchCosts) {
		this.variableMatchCosts = variableMatchCosts;
	}

	@Override
	protected ConformanceTaskResult call() throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			//Running the conformance check (alignment would be more precise to say)
			AlignmentAnalysisResult alignmentAnalysisResult = Runner.run(logFile.getAbsolutePath(), xmlModel.getAbsolutePath(), activityMapping, activityMatchCosts, variableMatchCosts);

			//Setting the trace list for the task result
			ConformanceTaskResult conformanceTaskResult = new ConformanceTaskResult();
			XLog xlog = LogUtils.convertToXlog(logFile);
			
			//Sorting traces by name (this order is used when displaying results grouped by constraint)
			List<XTrace> analysisResultTraces = new ArrayList<>(xlog);
			analysisResultTraces.sort((o1, o2) -> {
				String traceName1 = XConceptExtension.instance().extractName(o1) != null ? XConceptExtension.instance().extractName(o1) : "";
				String traceName2 = XConceptExtension.instance().extractName(o2) != null ? XConceptExtension.instance().extractName(o2) : "";
				return traceName1.compareTo(traceName2);
			});
			conformanceTaskResult.setTraceList(analysisResultTraces);

			//Setting the constraint list for the task result
			ArrayList<String> constraintLabels = new ArrayList<>();
			DeclareMap declareModel = getDeclareModel(xmlModel.getAbsolutePath()); //Needed for getting the constraints
			Map<String,String> constraintsToLabels = new HashMap<>(); //Needed for setting constraint statistics
			for (ConstraintDefinition constraintDefinition : declareModel.getModel().getConstraintDefinitions()) {
				String constraintLabel = constraintDefinition.getCaption() + "\n" + constraintDefinition.getCondition();
				constraintsToLabels.put(constraintDefinition.getCaption(), constraintLabel);
				constraintLabels.add(constraintLabel);
			}
			conformanceTaskResult.setConstraintList(constraintLabels);

			//Mapping of trace names to traces
			Map<String,XTrace> namesToTraces = new HashMap<>();
			for (XTrace xtrace : analysisResultTraces) {
				String traceName = XConceptExtension.instance().extractName(xtrace);
				namesToTraces.put(traceName, xtrace);
			}

			//Maps for counting constraint statistics
			Map<String, Integer> constraintInsertionCounts = new HashMap<>();
			Map<String, Integer> constraintDeletionCounts = new HashMap<>();

			//For counting global statistics
			int tracesWithInsertions = 0;
			int tracesWithDeletions = 0;
			int tracesWithDataDifferences = 0;
			
			//Counting constraint statistics and setting trace statistics
			for (Alignment alignment : alignmentAnalysisResult.getAlignments()) { //One alignment per trace
				XTrace xtrace = namesToTraces.get(alignment.getTraceName());
				ViolationIdentifier violationIdentifier = new ViolationIdentifier(alignment.getLogTrace(),alignment.getProcessTrace(),declareModel);
				List<DataAwareStepTypes> dataAwareStepTypes = alignment.getStepTypes();

				//Setting up a map for tracking activity conformance types for each constraint separately
				Map<String, List<ActivityConformanceType>> constraintConformanceMap = new HashMap<>();
				for (String constraintLabel : constraintLabels) {
					constraintConformanceMap.put(constraintLabel, new ArrayList<>());
				}
				
				// Computing an average of time intervals between subsequent events in order to set a realistic interval for insertions at the extremes of the trace
				Duration avgTimeInterval = Duration.ZERO;
				for (int i=0; i < xtrace.size(); i++) {
					if (i > 0) {
						OffsetDateTime prevTimestamp = OffsetDateTime.parse(xtrace.get(i-1).getAttributes().get("time:timestamp").toString());
						OffsetDateTime currTimestamp = OffsetDateTime.parse(xtrace.get(i).getAttributes().get("time:timestamp").toString());
						
						if (prevTimestamp!=null && currTimestamp!=null)
							avgTimeInterval = avgTimeInterval.plus(Duration.between(prevTimestamp, currTimestamp));
					}
				}
				avgTimeInterval = avgTimeInterval.dividedBy(xtrace.size());

				for (int i = 0; i < dataAwareStepTypes.size(); i++) {
					if (dataAwareStepTypes.get(i) == DataAwareStepTypes.MREAL) {
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
					} else if (dataAwareStepTypes.get(i) == DataAwareStepTypes.LMNOGOOD) {
						for (String constraintLabel : constraintLabels) {
							constraintConformanceMap.get(constraintLabel).add(new ActivityConformanceType(Type.DATA_DIFFERENCE));
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
					conformanceTaskResultDetail.setTraceName(xtrace.getAttributes().get("concept:name") != null ? xtrace.getAttributes().get("concept:name").toString() : "");
					conformanceTaskResultDetail.setXtrace(xtrace);
					conformanceTaskResultDetail.setConstraint(constraintLabel);
					conformanceTaskResultDetail.setActivityConformanceTypes(constraintConformanceMap.get(constraintLabel));
					conformanceTaskResult.addResultDetail(conformanceTaskResultDetail);
				}

				//Setting trace statistics
				AnalysisSingleResult analysisSingleResult = alignmentAnalysisResult.getDetailedResults().get(alignment).stream().findFirst().get();
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.FITNESS, String.format("%.2f", alignment.getFitness()));
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.INSERTIONS, Integer.toString(analysisSingleResult.getMovesInModel().size()));
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.DELETIONS, Integer.toString(analysisSingleResult.getMovesInLog().size()));
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.DATA_DIFFERENCES, Integer.toString(analysisSingleResult.getMovesInBothDiffData().size()));
			
				if (!analysisSingleResult.getMovesInModel().isEmpty())
					tracesWithInsertions++;
				
				if (!analysisSingleResult.getMovesInLog().isEmpty())
					tracesWithDeletions++;
				
				if (!analysisSingleResult.getMovesInBothDiffData().isEmpty())
					tracesWithDataDifferences++;
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
			conformanceTaskResult.setGlobalStatistic(ConformanceStatisticType.DATA_DIFFERENCES, Integer.toString(tracesWithDataDifferences));
			conformanceTaskResult.setGlobalStatistic(ConformanceStatisticType.FITNESS, String.format("%.2f", alignmentAnalysisResult.getAverageFitness()));

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

	private DeclareMap getDeclareModel(String xmlModelPath) {
		//Taken from method org.processmining.plugins.DeclareConformance.Replayer.getModel()
		AssignmentViewBroker assignmentViewBroker = XMLBrokerFactory.newAssignmentBroker(xmlModelPath);
		AssignmentModel assignmentModel = assignmentViewBroker.readAssignment();
		return new DeclareMap(assignmentModel, null, null, null, null, null);
	}

	//Increments the statistics count for each constraint that had a violation
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
		return "ConformanceTaskDataAwareReplayer [xmlModel=" + xmlModel + ", logFile=" + logFile + "]";
	}
}
