package controller.conformance.plannerTextBased;

import static controller.conformance.plannerTextBased.utils.TestUtils.mockPlannerCreation;

import controller.conformance.plannerTextBased.model.PlannerCreation;
import controller.conformance.plannerTextBased.model.PlannerPlanningBasedConformanceCheckerResult;
import java.io.File;
import java.io.IOException;

public class PlannerTextBasedMain {

  public static void main(String[] args) throws IOException {
    // Miski mis votab data sisse
    // Miski mis laseb algoritmi valida
    // Miski mis valjastab selle xmli
    run(mockPlannerCreation());
  }

  public static PlannerPlanningBasedConformanceCheckerResult run(PlannerCreation creation) throws IOException {
    // Force executable files of Fast/downward to be executable
    //Force the executable files of Fast-downward and LPG to be executable
    new File("plan-based-conformance-scripts/translate_script").setExecutable(true);
    new File("plan-based-conformance-scripts/preprocess_script").setExecutable(true);
    new File("plan-based-conformance-scripts/planner_subopt_script").setExecutable(true);
    new File("plan-based-conformance-scripts/planner_opt_script").setExecutable(true);
    new File("plan-based-conformance-scripts/lpg_script").setExecutable(true);
    new File("plan-based-conformance-scripts/fast-downward/src/translate/translate.py").setExecutable(true);
    new File("plan-based-conformance-scripts/fast-downward/src/preprocess/preprocess").setExecutable(true);
    new File("plan-based-conformance-scripts/fast-downward/src/search/downward").setExecutable(true);
    new File("plan-based-conformance-scripts/fast-downward/src/search/unitcost").setExecutable(true);
    new File("plan-based-conformance-scripts/fast-downward/src/search/downward-release").setExecutable(true);
    new File("plan-based-conformance-scripts/LPG/lpg").setExecutable(true);

    new File("plan-based-conformance-scripts/run_FD").setExecutable(true);
    new File("plan-based-conformance-scripts/run_FD_all").setExecutable(true);
    new File("plan-based-conformance-scripts/run_SYMBA").setExecutable(true);
    new File("plan-based-conformance-scripts/run_SYMBA_all").setExecutable(true);
    new File("plan-based-conformance-scripts/checkNumberOfTraces").setExecutable(true);


    new PlannerFieldFactory(creation).initialize();

    PlannerPerspectiveLogic perspectiveLogic = new PlannerPerspectiveLogic();
    perspectiveLogic.generatePDDL();

    OutputGenerator outputGenerator = new OutputGenerator();

    return outputGenerator.generateXLogOutput();
  }
}
