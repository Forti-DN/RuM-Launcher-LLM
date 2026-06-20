package controller.conformance.plannerTextBased.utils;

import java.io.File;
import java.util.Vector;
import controller.conformance.plannerTextBased.model.PlannerAlgorithm;
import controller.conformance.plannerTextBased.model.PlannerCreation;

public class TestUtils {
  private final static PlannerAlgorithm SELECTED_ALGORITHM = PlannerAlgorithm.FAST_DOWNWARD;
  private final static String RESULT_PATH = "FIELD_NOT_USED";
  private final static int NUMBER_OF_TRACES_TO_CHECK_FROM = 1;
  private final static int NUMBER_OF_TRACES_TO_CHECK_TO = 1;
  private final static int LENGTH_OF_TRACES_TO_CHECK_FROM = 1;
  private final static int LENGTH_OF_TRACES_TO_CHECK_TO = 1000;
  // TODO if boolean is true then wont work atm, nothing sets the lengths and numbers
  private final static boolean REMOVE_DUPLICATED_TRACES = false;

  private final static String pathToFile = "muru/";

  public static PlannerCreation mockPlannerCreation() {
    return new PlannerCreation(
        mockFile(pathToFile + "10Constraints.xml"),
        mockFile(pathToFile + "log-from-10constr-model-1constr_inverted-1-50.xes"),
        SELECTED_ALGORITHM,
        RESULT_PATH,
        NUMBER_OF_TRACES_TO_CHECK_FROM,
        NUMBER_OF_TRACES_TO_CHECK_TO,
        LENGTH_OF_TRACES_TO_CHECK_FROM,
        LENGTH_OF_TRACES_TO_CHECK_TO,
        REMOVE_DUPLICATED_TRACES,
        mockActivitiesCostVector());
  }

  private static File mockFile(String filePath) {
    return new File(filePath);
  }

  private static Vector<Vector<String>> mockActivitiesCostVector() {
    return new Vector<Vector<String>>();
  }
}
