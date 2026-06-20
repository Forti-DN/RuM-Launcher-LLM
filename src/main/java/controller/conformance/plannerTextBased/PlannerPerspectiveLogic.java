package controller.conformance.plannerTextBased;

import controller.conformance.plannerTextBased.model.Trace;
import controller.conformance.plannerTextBased.model.Constants;
import controller.conformance.plannerTextBased.utils.Utilities;

public class PlannerPerspectiveLogic {

  public void generatePDDL() {
    // TODO fix diffchecker
    /**
     * Constants.getNumberOfTracesToCheckTo ja From peaks olema 1
     * Constants.getLengthOfTracesToCheckTo ja from peaks olema 3
     * https://www.diffchecker.com/HnoURTK3
     */

    Constants.setNumberOfTracesToCheckFrom(1);
    Constants.setNumberOfTracesToCheckTo(Constants.getAllTracesVector().size());

    Constants.setLengthOfTracesToCheckFrom(Constants.getMinimumLengthOfATrace());
    Constants.setLengthOfTracesToCheckTo(Constants.getMaximumLengthOfATrace());

    // Reset the counter
    Constants.setNumberOfPDDLFiles(0);
    int numberOfPddlFiles = 0;

    for (int i = Constants.getNumberOfTracesToCheckFrom() - 1; i < Constants.getNumberOfTracesToCheckTo(); i++) {
      Trace trace = Constants.getAllTracesVector().elementAt(i);

      // Remove duplicated traces
      if (Constants.isRemoveDuplicatedTraces()) {
        if (Constants.getContentOfAnyDifferentTraceHashtable().containsValue(trace.getTraceName())) {
          if (trace.getOriginalTraceContent_vector().size() >= Constants.getLengthOfTracesToCheckFrom() &&
              trace.getOriginalTraceContent_vector().size() <= Constants.getLengthOfTracesToCheckTo()) {

            createDomainAndProblemFile(i, trace);
            numberOfPddlFiles++;
          }
        }
      } else {
        if (trace.getOriginalTraceContent_vector().size() >= Constants.getLengthOfTracesToCheckFrom()
            && trace.getOriginalTraceContent_vector().size() <= Constants.getLengthOfTracesToCheckTo()) {

          createDomainAndProblemFile(i, trace);
          numberOfPddlFiles++;
        }
      }
    }
    Constants.setNumberOfPDDLFiles(numberOfPddlFiles);
  }


  private void createDomainAndProblemFile(int i, Trace trace) {
    StringBuffer sbDomain = Utilities.createPropositionalDomain(trace);
    StringBuffer sbProblem = Utilities.createPropositionalProblem(trace);

    int trace_real_number = i + 1;
    switch (Constants.getSelectedPlannerAlgorithm()) {
      case FAST_DOWNWARD:
        Utilities.createFile("plan-based-conformance-scripts/fast-downward/src/Conformance_Checking/domain" + trace_real_number + ".pddl", sbDomain);
        Utilities.createFile("plan-based-conformance-scripts/fast-downward/src/Conformance_Checking/problem" + trace_real_number + ".pddl", sbProblem);
        break;
      case SYMBA:
        Utilities.createFile("plan-based-conformance-scripts/seq-opt-symba-2/Conformance_Checking/domain" + trace_real_number + ".pddl", sbDomain);
        Utilities.createFile("plan-based-conformance-scripts/seq-opt-symba-2/Conformance_Checking/problem" + trace_real_number + ".pddl", sbProblem);
        break;
    }

    System.out.println("File should be created");
  }
}
