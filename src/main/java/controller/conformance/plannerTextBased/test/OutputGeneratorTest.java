package controller.conformance.plannerTextBased.test;

import controller.conformance.plannerTextBased.OutputGenerator;
import controller.conformance.plannerTextBased.model.Constants;
import controller.conformance.plannerTextBased.model.PlannerAlgorithm;
import java.io.IOException;

public class OutputGeneratorTest {

  public static void main(String[] args) throws IOException {
    testAlgorithmRun();
  }

  public static void testAlgorithmRun() throws IOException {
    Constants.setSelectedPlannerAlgorithm(PlannerAlgorithm.FAST_DOWNWARD);

    new OutputGenerator().runAlgorithm(0);
  }
}
