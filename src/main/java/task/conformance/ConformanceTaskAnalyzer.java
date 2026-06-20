package task.conformance;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.declareanalyzer.AnalysisResult;
import org.processmining.plugins.declareanalyzer.AnalysisSingleResult;
import org.processmining.plugins.declareanalyzer.Tester;

import javafx.concurrent.Task;
import task.conformance.ActivityConformanceType.Type;
import util.ModelUtils;

public class ConformanceTaskAnalyzer extends Task<ConformanceTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File xmlModel;
	private File logFile;

	public ConformanceTaskAnalyzer() {
		super();
	}

	public ConformanceTaskAnalyzer(File xmlModel, File logFile) {
		super();
		this.xmlModel = xmlModel;
		this.logFile = logFile;
	}

	public void setXmlModel(File xmlModel) {
		this.xmlModel = xmlModel;
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	@Override
	protected ConformanceTaskResult call() throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			//TODO: Should somehow try to keep the reference to single quote file instead of recreating it
			//Running the conformance check
			File xmlModelSq = ModelUtils.xmlModelToSingleQuote(xmlModel);
			AnalysisResult analysisResult = Tester.run(logFile.getAbsolutePath(), xmlModelSq.getAbsolutePath());

			//Sorting traces by name (this order is used when displaying results grouped by constraint)
			List<XTrace> analysisResultTraces = new ArrayList<>(analysisResult.getTraces());
			analysisResultTraces.sort((o1, o2) -> {
				String traceName1 = XConceptExtension.instance().extractName(o1) != null ? XConceptExtension.instance().extractName(o1) : "";
				String traceName2 = XConceptExtension.instance().extractName(o2) != null ? XConceptExtension.instance().extractName(o2) : "";
				return traceName1.compareTo(traceName2);
			});
			
			//Creating the constraints list based on the analysis results of the first trace
			List<String> analysisResultConstraints = new ArrayList<>();
			for(AnalysisSingleResult analysisSingleResult : analysisResult.getResults(analysisResultTraces.get(0))) {
				analysisResultConstraints.add(analysisSingleResult.getConstraint().getCaption() + "\n" + analysisSingleResult.getConstraint().getCondition());
			}

			//Setting the trace and constraint lists for the task result
			ConformanceTaskResult conformanceTaskResult = new ConformanceTaskResult();
			conformanceTaskResult.setTraceList(analysisResultTraces);
			conformanceTaskResult.setConstraintList(analysisResultConstraints);

			//For counting global statistics
			int fulfilledTraces = 0;
			int violatedTraces = 0;
			int vacuousTraces = 0;
			
			//For counting constraint statistics
			Map<String,TreeMap<ConformanceStatisticType,Integer>> constraintStatisticsMap = new HashMap<>();
			for (String analysisResultConstraint : analysisResultConstraints) {
				//Values will be later converted to Strings and name will be added
				TreeMap<ConformanceStatisticType, Integer> constraintStatistics = new TreeMap<>();
				constraintStatisticsMap.put(analysisResultConstraint, constraintStatistics);
				constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.ACTIVATIONS, 0);
				constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.FULFILLMENTS, 0);
				constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.VIOLATIONS, 0);
				constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.VACUOUS_FULFILLMENTS, 0);
				constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.VACUOUS_VIOLATIONS, 0);
			}
			
			//Setting the trace statistics for the task result
			for (XTrace xtrace : analysisResultTraces) {
				int activations = 0;
				int fulfillments = 0;
				int violations = 0;
				int vacuousFulfilments = 0;
				int vacuousViolations = 0;
				for(AnalysisSingleResult analysisSingleResult : analysisResult.getResults(xtrace)) { //One result for each constraint
					String analysisResultConstraint = analysisSingleResult.getConstraint().getCaption() + "\n" + analysisSingleResult.getConstraint().getCondition();
					//Adding statistics for trace
					activations += analysisSingleResult.getActivations().size();
					fulfillments += analysisSingleResult.getFulfilments().size();
					violations += analysisSingleResult.getViolations().size();
					
					//Adding statistics for constraint
					constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.ACTIVATIONS, constraintStatisticsMap.get(analysisResultConstraint).get(ConformanceStatisticType.ACTIVATIONS) + analysisSingleResult.getActivations().size());
					constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.FULFILLMENTS, constraintStatisticsMap.get(analysisResultConstraint).get(ConformanceStatisticType.FULFILLMENTS) + analysisSingleResult.getFulfilments().size());
					constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.VIOLATIONS, constraintStatisticsMap.get(analysisResultConstraint).get(ConformanceStatisticType.VIOLATIONS) + analysisSingleResult.getViolations().size());

					//Setting basic data for a result detail (trace - constraint combination)
					ConformanceTaskResultDetail conformanceTaskResultDetail = new ConformanceTaskResultDetail();
					String traceNameStr = XConceptExtension.instance().extractName(xtrace);
					conformanceTaskResultDetail.setTraceName(traceNameStr != null ? traceNameStr : "");
					conformanceTaskResultDetail.setXtrace(xtrace);
					conformanceTaskResultDetail.setConstraint(analysisResultConstraint);

					//Creating and setting the activity types list for a result detail
					List<ActivityConformanceType> activityConformanceTypes = new ArrayList<>();
					for (int i = 0; i < xtrace.size(); i++) {
						if (analysisSingleResult.getFulfilments().contains(i)) {
							activityConformanceTypes.add(new ActivityConformanceType(Type.FULFILLMENT));
						} else if (analysisSingleResult.getViolations().contains(i)) {
							activityConformanceTypes.add(new ActivityConformanceType(Type.VIOLATION));
						} else {
							activityConformanceTypes.add(new ActivityConformanceType(Type.NONE));
						}
					}
					conformanceTaskResultDetail.setActivityConformanceTypes(activityConformanceTypes);
					
					//Check for vacuity (cases where a constraint can be fulfilled or violated without activations)
					ActivityConformanceType activityConformanceType = checkVacuousConformance(analysisSingleResult);
					conformanceTaskResultDetail.setVacuousConformance(activityConformanceType);
					if (activityConformanceType.getType() == Type.FULFILLMENT) {
						constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.VACUOUS_FULFILLMENTS, constraintStatisticsMap.get(analysisResultConstraint).get(ConformanceStatisticType.VACUOUS_FULFILLMENTS) + 1);
						vacuousFulfilments = vacuousFulfilments + 1;
					} else if (activityConformanceType.getType() == Type.VIOLATION) {
						constraintStatisticsMap.get(analysisResultConstraint).put(ConformanceStatisticType.VACUOUS_VIOLATIONS, constraintStatisticsMap.get(analysisResultConstraint).get(ConformanceStatisticType.VACUOUS_VIOLATIONS) + 1);
						vacuousViolations = vacuousViolations + 1;
					}
					
					
					conformanceTaskResult.addResultDetail(conformanceTaskResultDetail);
				}
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.ACTIVATIONS, Integer.toString(activations));
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.FULFILLMENTS, Integer.toString(fulfillments));
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.VIOLATIONS, Integer.toString(violations));
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.VACUOUS_FULFILLMENTS, Integer.toString(vacuousFulfilments));
				conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.VACUOUS_VIOLATIONS, Integer.toString(vacuousViolations));
				
				//Counting the log statistics
				if (fulfillments > 0 && violations == 0 && vacuousViolations == 0) {
					fulfilledTraces++;
				} else if (violations > 0 || vacuousViolations > 0) {
					violatedTraces++;
				} else {
					vacuousTraces++;
				}
			}
			
			//Setting the constraint statistics for the task result
			for (String constraint : constraintStatisticsMap.keySet()) {
				conformanceTaskResult.addConstraintStatistic(constraint, ConformanceStatisticType.ACTIVATIONS, constraintStatisticsMap.get(constraint).get(ConformanceStatisticType.ACTIVATIONS).toString());
				conformanceTaskResult.addConstraintStatistic(constraint, ConformanceStatisticType.FULFILLMENTS, constraintStatisticsMap.get(constraint).get(ConformanceStatisticType.FULFILLMENTS).toString());
				conformanceTaskResult.addConstraintStatistic(constraint, ConformanceStatisticType.VIOLATIONS, constraintStatisticsMap.get(constraint).get(ConformanceStatisticType.VIOLATIONS).toString());
				conformanceTaskResult.addConstraintStatistic(constraint, ConformanceStatisticType.VACUOUS_FULFILLMENTS, constraintStatisticsMap.get(constraint).get(ConformanceStatisticType.VACUOUS_FULFILLMENTS).toString());
				conformanceTaskResult.addConstraintStatistic(constraint, ConformanceStatisticType.VACUOUS_VIOLATIONS, constraintStatisticsMap.get(constraint).get(ConformanceStatisticType.VACUOUS_VIOLATIONS).toString());
			}
			
			//Setting global statistics
			conformanceTaskResult.setGlobalStatistic(ConformanceStatisticType.FULFILLMENTS, Integer.toString(fulfilledTraces));
			conformanceTaskResult.setGlobalStatistic(ConformanceStatisticType.VACUOUS_FULFILLMENTS, Integer.toString(vacuousTraces));
			conformanceTaskResult.setGlobalStatistic(ConformanceStatisticType.VIOLATIONS, Integer.toString(violatedTraces));

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
	
	private ActivityConformanceType checkVacuousConformance(AnalysisSingleResult analysisSingleResult) {
		if (!analysisSingleResult.getActivations().isEmpty()) {
			return new ActivityConformanceType(Type.NONE);
		} else {
			String template = analysisSingleResult.getConstraint().getName();
			if (template.equals("absence") || template.equals("absence2") || template.equals("absence3")
					 || template.equals("alternate precedence") || template.equals("chain precedence") || template.equals("precedence") || template.equals("not chain precedence") || template.equals("not precedence")
					 || template.equals("alternate response") || template.equals("chain response") || template.equals("response") || template.equals("not chain response") || template.equals("not response")
					 || template.equals("alternate succession") || template.equals("chain succession") || template.equals("succession") || template.equals("not chain succession") || template.equals("not succession")
					 || template.equals("responded existence") || template.equals("not responded existence")
					 || template.equals("co-existence") || template.equals("not co-existence")) {
				return new ActivityConformanceType(Type.FULFILLMENT);
			} else if (template.equals("exactly1") || template.equals("exactly2")
					|| template.equals("existence") || template.equals("existence2") || template.equals("existence3")
					|| template.equals("init")
					|| template.equals("choice") || template.equals("exclusive choice")) {
				return new ActivityConformanceType(Type.VIOLATION);
			} else {
				return new ActivityConformanceType(Type.NONE);
			}
		}
	}

	@Override
	public String toString() {
		return "ConformanceTaskAnalyzer [xmlModel=" + xmlModel + ", logFile=" + logFile + "]";
	}
}
