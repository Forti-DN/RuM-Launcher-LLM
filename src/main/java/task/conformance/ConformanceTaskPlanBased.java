package task.conformance;

import controller.conformance.plannerTextBased.PlannerTextBasedMain;
import controller.conformance.plannerTextBased.model.PlannerAlgorithm;
import controller.conformance.plannerTextBased.model.PlannerCreation;
import controller.conformance.plannerTextBased.model.PlannerPlanningBasedConformanceCheckerResult;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javafx.concurrent.Task;
import task.conformance.ActivityConformanceType.Type;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.DataConformance.framework.ActivityMatchCost;
import org.processmining.plugins.DataConformance.visualization.DataAwareStepTypes;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;
import org.processmining.plugins.declareminer.visualizing.AssignmentModel;
import org.processmining.plugins.declareminer.visualizing.AssignmentViewBroker;
import org.processmining.plugins.declareminer.visualizing.XMLBrokerFactory;

public class ConformanceTaskPlanBased extends Task<ConformanceTaskResult> {

  private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private File xmlModel;
  private File logFile;
  private Map<ReplayableActivityDefinition, XEventClass> activityMapping;
  private List<ActivityMatchCost> activityMatchCosts;
  private PlannerAlgorithm algorithm;

  private final String RESULT_PATH = "FIELD_NOT_USED";
  private final int NUMBER_OF_TRACES_TO_CHECK_FROM = 1;
  private final int NUMBER_OF_TRACES_TO_CHECK_TO = 1;
  private final int LENGTH_OF_TRACES_TO_CHECK_FROM = 1;
  private final int LENGTH_OF_TRACES_TO_CHECK_TO = 1000;
  // TODO if boolean is true then wont work atm, nothing sets the lengths and numbers
  private final boolean REMOVE_DUPLICATED_TRACES = false;


  public ConformanceTaskPlanBased() {
    super();
  }

  public ConformanceTaskPlanBased(File xmlModel, File logFile,
      Map<ReplayableActivityDefinition, XEventClass> activityMapping,
      List<ActivityMatchCost> activityMatchCosts, PlannerAlgorithm algorithm) {
    super();
    this.xmlModel = xmlModel;
    this.logFile = logFile;
    this.activityMapping = activityMapping;
    this.activityMatchCosts = activityMatchCosts;
    this.algorithm = algorithm;
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

  public void setAlgorithm(PlannerAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  @Override
  protected ConformanceTaskResult call() throws Exception {
    long taskStartTime = System.currentTimeMillis();
		logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

    File declareModel = new File(xmlModel.getAbsolutePath());
    File log = new File(logFile.getAbsolutePath());
    
    Files.readAllLines(declareModel.toPath());
    
    List<String> costStringList = new ArrayList<>();
	
	AssignmentViewBroker broker = XMLBrokerFactory.newAssignmentBroker(xmlModel.getAbsolutePath());
    AssignmentModel model = broker.readAssignment();
    model.getActivityDefinitions().forEach( item -> costStringList.add(item.toString().toLowerCase()) );
    
    Vector<Vector<String>> activityInsertionDeletionCosts = new Vector<>();
    
    for (String act : costStringList) {
    	Vector<String> actWithCosts = new Vector<>();
    	actWithCosts.add(act);
    	actWithCosts.add("1");
    	actWithCosts.add("1");
    	activityInsertionDeletionCosts.add(actWithCosts);
    }
    
    PlannerCreation creation = new PlannerCreation(
        declareModel,
        log,
        algorithm,
        RESULT_PATH,
        NUMBER_OF_TRACES_TO_CHECK_FROM,
        NUMBER_OF_TRACES_TO_CHECK_TO,
        LENGTH_OF_TRACES_TO_CHECK_FROM,
        LENGTH_OF_TRACES_TO_CHECK_TO,
        REMOVE_DUPLICATED_TRACES,
        activityInsertionDeletionCosts
    );
    PlannerPlanningBasedConformanceCheckerResult conformanceCheckerResult = PlannerTextBasedMain.run(creation);

    //Setting the trace list for the task result
    ConformanceTaskResult conformanceTaskResult = new ConformanceTaskResult();

    List<XTrace> analysisResultTraces = new ArrayList<>(conformanceCheckerResult.getResultXLog());
    analysisResultTraces.sort((o1, o2) -> {
      String traceName1 = XConceptExtension.instance().extractName(o1) != null ? XConceptExtension.instance().extractName(o1) : "";
      String traceName2 = XConceptExtension.instance().extractName(o2) != null ? XConceptExtension.instance().extractName(o2) : "";
      return traceName1.compareTo(traceName2);
    });
    conformanceTaskResult.setTraceList(analysisResultTraces);

    //Setting the constraint list for the task result
    ArrayList<String> constraintLabels = new ArrayList<>();
    Map<String, String> constraintToLabels = new HashMap<>(); // Neede for setting constraint statistics

    for (int j = 0; j < conformanceCheckerResult.getResultXLog().size(); j++) {
      String constraintLabel = "Constraint label " + j;
      String constraintCaption = "Constraint caption " + j;
      constraintToLabels.put(constraintCaption, constraintLabel);
      constraintLabels.add(constraintLabel);
    }

    //Maps for counting constraint statistics
    Map<String, Integer> constraintInsertionCounts = new HashMap<>();
    Map<String, Integer> constraintDeletionCounts = new HashMap<>();

    //For counting global statistics
    int tracesWithInsertions = 0;
    int tracesWithDeletions = 0;

    conformanceTaskResult.setConstraintList(constraintLabels);

    //Results processing
    for (int i = 0; i < analysisResultTraces.size(); i++) {
      XTrace xtrace = analysisResultTraces.get(i);
      String traceName = XConceptExtension.instance().extractName(xtrace);
      // TODO alignment kasutada evente for event in xtrace
      // Alignment alignment = resultReplayDeclare.getAlignmentByTraceName(traceName);
      int insertionsToTrace = 0;
      int deletionsToTrace = 0;
      // ViolationIdentifier violationIdentifier = new ViolationIdentifier(alignment.getLogTrace(),alignment.getProcessTrace(),resultReplayDeclare.getDeclareModel());
      List<DataAwareStepTypes> dataAwareStepTypes = generateStepTypes(xtrace);

      //Setting up a map for tracking activity conformance types for each constraint separately
      Map<String, List<ActivityConformanceType>> constraintConformanceMap = new HashMap<>();
      for (String constraintLabel : constraintLabels) {
        constraintConformanceMap.put(constraintLabel, new ArrayList<>());
      }

      List<ActivityConformanceType> conformanceTypes = conformanceCheckerResult.getTraceActivityTypes().get(i);
      for (int j = 0; j < conformanceTypes.size(); j++) {
        ActivityConformanceType activityConformanceType = conformanceTypes.get(j);
        if (activityConformanceType.getType() == ActivityConformanceType.Type.DELETION) {
          for (String constraintLabel : constraintLabels) {
            constraintConformanceMap.get(constraintLabel).add(activityConformanceType);
          }
        } else if (activityConformanceType.getType() == ActivityConformanceType.Type.DELETION) {
          for (String constraintLabel : constraintLabels) {
            constraintConformanceMap.get(constraintLabel).add(activityConformanceType);
          }
        } else {
          for (String constraintLabel : constraintLabels) {
            constraintConformanceMap.get(constraintLabel).add(new ActivityConformanceType(Type.NONE));
          }
        }
      }

      for (String constraintLabel : constraintLabels) {
        ConformanceTaskResultDetail conformanceTaskResultDetail = new ConformanceTaskResultDetail();
        conformanceTaskResultDetail
            .setTraceName(xtrace.getAttributes().get("concept:name") != null ? xtrace.getAttributes().get("concept:name").toString() : "");
        conformanceTaskResultDetail.setXtrace(xtrace);
        conformanceTaskResultDetail.setConstraint(constraintLabel);
        //conformanceTaskResultDetail.setActivityConformanceTypes(conformanceCheckerResult.getTraceActivityTypes().get(i));
        conformanceTaskResultDetail.setActivityConformanceTypes(constraintConformanceMap.get(constraintLabel));
        conformanceTaskResult.addResultDetail(conformanceTaskResultDetail);
      }

      //Setting trace statistics
      // conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.FITNESS, String.format("%.2f", alignment.getFitness()));
      conformanceTaskResult
          .addTraceStatistic(xtrace, ConformanceStatisticType.INSERTIONS, Integer.toString(conformanceCheckerResult.getInsertedEvents().get(i)));
      conformanceTaskResult.addTraceStatistic(xtrace, ConformanceStatisticType.DELETIONS, Integer.toString(conformanceCheckerResult.getDeletedEvents().get(i)));
    }

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
    conformanceTaskResult
        .setGlobalStatistic(ConformanceStatisticType.INSERTIONS, Integer.toString(calculateGlobalStatistics(conformanceCheckerResult).get("insertions")));
    conformanceTaskResult
        .setGlobalStatistic(ConformanceStatisticType.DELETIONS, Integer.toString(calculateGlobalStatistics(conformanceCheckerResult).get("deletions")));
/*
    double meanFitness = Math.floor(resultReplayDeclare.meanFitness * 100) / 100.0; //Needed because otherwise 0.995 gets rounded to 1.00 by String.format
    conformanceTaskResult.setGlobalStatistic(ConformanceStatisticType.FITNESS, String.format("%.2f", meanFitness));
*/

    //Prepares the result trace group
    conformanceTaskResult.createResultGroupings();

    logger.info("{} ({}) completed at: {} - total time: {}",
			this.getClass().getSimpleName(),
			this.hashCode(),
			System.currentTimeMillis(),
			(System.currentTimeMillis() - taskStartTime)
		);

    return conformanceTaskResult;

    //TODO: It seems strange that this method doesn't catch exceptions (like in all other ConformanceTask classes)

  }

  private Map<String, Integer> calculateGlobalStatistics(PlannerPlanningBasedConformanceCheckerResult conformanceCheckerResult) {
    Map<String, Integer> statisticsMap = new HashMap<>();
    int deletions = 0;
    int insertions = 0;

    for (int deletedEvents : conformanceCheckerResult.getDeletedEvents()) {
      deletions += deletedEvents;
    }

    for (int insertedEvents : conformanceCheckerResult.getInsertedEvents()) {
      insertions += insertedEvents;
    }

    statisticsMap.put("insertions", insertions);
    statisticsMap.put("deletions", deletions);

    return statisticsMap;
  }

  // TODO. right now only returns MINVI because I could not figure out the solution
  private List<DataAwareStepTypes> generateStepTypes(XTrace trace) {
    List<DataAwareStepTypes> stepTypes = new ArrayList<>();
    for (XEvent xEvent : trace) {
      stepTypes.add(DataAwareStepTypes.MINVI);
    }
    return stepTypes;
  }

  @Override
  public String toString() {
    return "PlanBasedConformanceChecker [xmlModel=" + xmlModel + ", logFile=" + logFile + "]";
  }
}
