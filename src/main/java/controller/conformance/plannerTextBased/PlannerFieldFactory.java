package controller.conformance.plannerTextBased;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import controller.conformance.plannerTextBased.model.CombinationOfRelevantTransitions;
import controller.conformance.plannerTextBased.model.DeclareTemplate;
import controller.conformance.plannerTextBased.utils.LTLFormula;
import controller.conformance.plannerTextBased.model.RelevantTransition;
import controller.conformance.plannerTextBased.model.Trace;
import controller.conformance.plannerTextBased.utils.Utilities;
import org.processmining.ltl2automaton.plugins.automaton.Automaton;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.ltl2automaton.plugins.automaton.Transition;
import controller.conformance.plannerTextBased.model.Constants;
import controller.conformance.plannerTextBased.model.PlannerCreation;

public class PlannerFieldFactory {

  private PlannerCreation creation;

  public PlannerFieldFactory(PlannerCreation creation) {
    this.creation = creation;
  }

  public void initialize() {
    //TODO Kontrollida, kas H_PlannerPerspective rida 120 discard duplicate traces vajalik

    // H_MenuPerspective rida 80 ehk open log file
    //TODO rida 144 CostCheckbox?
    setInitialValues();
    clearExistingOldFiles();
    clearExistingRepositories();

    // Read files
    FileContentExtractor contentExtractor = new FileContentExtractor();
    if (creation.getDeclareModel() != null) {
      System.out.println(creation.getDeclareModel().getAbsolutePath());
      contentExtractor.readDeclareModelFileContent(creation.getDeclareModel());
    }
    if (creation.getLogFile() != null) {
      System.out.println(creation.getLogFile().getAbsolutePath());
      contentExtractor.readLogFileContent(creation.getLogFile());
    }

    initializeConstraintsPerspectiveLogic();
  }

  private void clearExistingOldFiles() {
    switch (Constants.getSelectedPlannerAlgorithm()) {
      case SYMBA:
        Utilities.emptyFolder("plan-based-conformance-scripts/seq-opt-symba-2/Conformance_Checking");
      case FAST_DOWNWARD:
        Utilities.emptyFolder("plan-based-conformance-scripts/fast-downward/src/Conformance_Checking");
    }
  }

  // Call before reading declmodel or xes file
  private void clearExistingRepositories() {
    Constants.setActivitiesRepositoryVector(new Vector<>());
    Constants.setAllTracesVector(new Vector<>());
  }

  private void setInitialValues() {
    //Number of traces to check rida 167
    Constants.setNumberOfTracesToCheckFrom(creation.getNumberOfTracesToCheckFrom());
    Constants.setNumberOfTracesToCheckTo(creation.getNumberOfTracesToCheckTo());
    Constants.setLengthOfTracesToCheckFrom(creation.getLengthOfTracesToCheckFrom());
    Constants.setLengthOfTracesToCheckTo(creation.getLengthOfTracesToCheckTo());
    Constants.setRemoveDuplicatedTraces(creation.getRemoveDuplicatedTraces());
    Constants.setSelectedPlannerAlgorithm(creation.getSelectedAlgorithm());
    Constants.setActivitiesCostVector(creation.getActivitiesCostVector());
  }

  // Taken from class H_ConstraintsPerspective, refer to the GUI tool
  private void initializeConstraintsPerspectiveLogic() {
    Constants.setActivitiesCostVector(new Vector<>());
    Constants.setMinimumLengthOfATrace(0);
    Constants.setMaximumLengthOfATrace(0);
    Constants.setAllConstraints_vector(new Vector<>());
    Constants.setAlphabetOfTheConstraintsVector(new Vector<>());

    //String ltl_formula_for_product_automaton = new String();
    Vector<Automaton> automata_vector = new Vector<>();

    // Create a local vector containing the relevant transitions (a transition is said to be "relevant" if the source and the target state
    // are different) of any automaton representing a Declare/LTL constraint.
    //
    Vector<RelevantTransition> relevant_transitions_vector = new Vector<RelevantTransition>();

    Constants.setAutomataInitialStates_vector(new Vector<>());
    Constants.setAutomataAcceptingStates_vector(new Vector<>());
    Constants.setAutomataAllStates_vector(new Vector<>());

    Constants.setPDDLAutomataInitialStates_sb(new StringBuffer());
    Constants.setPDDLAutomataAcceptingStates_sb(new StringBuffer());
    Constants.setPDDLAutomataAllStates_sb(new StringBuffer());

    Constants.setAutomata_abstract_accepting_states_vector(new Vector<String>());
    Constants.setAutomataSinkNonAcceptingStatesVector(new Vector<>());

    Constants.setAlphabetOfTheConstraintsVector(new Vector<String>());

    String st_prefix = "s";
    String tr_prefix = "tr";
    int automaton_index = 0;
    int single_tr_index = 0;

    Multimap<String, String> transitions_map = HashMultimap.create();

    for (int k = 0; k < Constants.getConstraintsListModel().size(); k++) {
      String ltl_formula = new String();
      Vector<String> automaton_accepting_states_vector = new Vector<String>();
      single_tr_index = 0;

      String temporal_constraint = Constants.getConstraintsListModel().getElementAt(k);
      Constants.getAllConstraints_vector().addElement(temporal_constraint);

      String[] constraint_splitted = temporal_constraint.split("\\(");

      // Extract the name of the constraint (existence, response, etc.).
      String constraint_name = constraint_splitted[0];
      String[] constraint_splitted_2 = constraint_splitted[1].split("\\)");

      // FIRST CASE: the constraint involves two activities (e.g., response(A,B)).
      if (constraint_splitted_2[0].contains(",")) {
        String[] constraint_splitted_3 = constraint_splitted_2[0].split(",");
        String activity1 = constraint_splitted_3[0];
        String activity2 = constraint_splitted_3[1];

        if (!Constants.getAlphabetOfTheConstraintsVector().contains(activity1)) {
          Constants.getAlphabetOfTheConstraintsVector().addElement(activity1);
        }

        if (!Constants.getAlphabetOfTheConstraintsVector().contains(activity2)) {
          Constants.getAlphabetOfTheConstraintsVector().addElement(activity2);
        }

        if (Constants.getPDDL_encoding().equalsIgnoreCase("AAAI17")) {
          if (constraint_name.equalsIgnoreCase("choice")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Choice, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("exclusive choice")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Exclusive_Choice, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("responded existence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Responded_Existence, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("not responded existence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Not_Responded_Existence, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("co-existence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.CoExistence, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("not co-existence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Not_CoExistence, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("response")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Response, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("precedence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Precedence, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("succession")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Succession, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("chain response")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Chain_Response, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("chain precedence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Chain_Precedence, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("chain succession")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Chain_Succession, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("alternate response")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Alternate_Response, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("alternate precedence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Alternate_Precedence, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("alternate succession")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Alternate_Succession, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("not response")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Not_Response, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("not precedence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Not_Precedence, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("not succession")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Not_Succession, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("not chain response")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Not_Chain_Response, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("not chain precedence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Not_Chain_Precedence, activity1, activity2);
          } else if (constraint_name.equalsIgnoreCase("not chain succession")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Not_Chain_Succession, activity1, activity2);
          }
        }

      } else {
        String activity = constraint_splitted_2[0];
        if (!Constants.getAlphabetOfTheConstraintsVector().contains(activity)) {
          Constants.getAlphabetOfTheConstraintsVector().addElement(activity);
        }

        if (Constants.getPDDL_encoding().equalsIgnoreCase("AAAI17")) {
          if (constraint_name.equalsIgnoreCase("existence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Existence, activity, null);
          } else if (constraint_name.equalsIgnoreCase("absence")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Absence, activity, null);
          } else if (constraint_name.equalsIgnoreCase("init")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Init, activity, null);
          } else if (constraint_name.equalsIgnoreCase("absence2")) {
            ltl_formula = LTLFormula.getFormulaByTemplate(DeclareTemplate.Absence2, activity, null);
          }
        }
      }

      if (Constants.getPDDL_encoding().equalsIgnoreCase("AAAI17")) {
        Automaton automaton = LTLFormula.generateAutomatonByLTLFormula(ltl_formula);
        State initial_state_of_the_automaton = automaton.getInit();

        Iterator<State> it_states = automaton.iterator();

        while (it_states.hasNext()) {
          State s = (State) it_states.next();
          if (!s.isAccepting()) {
            Constants.getAutomataSinkNonAcceptingStatesVector().addElement(st_prefix + "_" + automaton_index + "_" + s.getId());
          }
        }

        // Identify the initial state of the specific automaton under consideration and records it in the global vector/stringbuffer of the initial states.
        if (!Constants.getAutomataInitialStates_vector().contains(st_prefix + "_" + automaton_index + "_" + initial_state_of_the_automaton.getId())) {
          Constants.getAutomataInitialStates_vector().addElement(st_prefix + "_" + automaton_index + "_" + initial_state_of_the_automaton.getId());
          Constants.getPDDLAutomataInitialStates_sb()
              .append("(currstate " + st_prefix + "_" + automaton_index + "_" + initial_state_of_the_automaton.getId() + ")\n");
        }

        Iterator<Transition> it = automaton.transitions().iterator();

        while (it.hasNext()) {

          Transition transition = (Transition) it.next();
          int tr_source_state_id = transition.getSource().getId();
          int tr_target_state_id = transition.getTarget().getId();

          if (tr_source_state_id != tr_target_state_id) {
            if (Constants.getAutomataSinkNonAcceptingStatesVector().contains(st_prefix + "_" + automaton_index + "_" + tr_source_state_id)) {
              Constants.getAutomataSinkNonAcceptingStatesVector().removeElement(st_prefix + "_" + automaton_index + "_" + tr_source_state_id);
            }

            String tr_source_state = st_prefix + "_" + automaton_index + "_" + tr_source_state_id;
            String tr_target_state = st_prefix + "_" + automaton_index + "_" + tr_target_state_id;
            String tr_id = null;
            String tr_label = null;

            if (!transition.isNegative()) {
              tr_id = tr_prefix + "_" + automaton_index + "_" + single_tr_index;
              tr_label = transition.getPositiveLabel();

              //
              // Create a new RelevantTransition object and records it in the global vector of relevant transitions.
              //
              RelevantTransition relevant_transition = new RelevantTransition(tr_id, tr_source_state, tr_target_state, tr_label,
                  transition.getPositiveLabel());
              relevant_transitions_vector.addElement(relevant_transition);
              transitions_map.put(tr_label, tr_id);

              single_tr_index++;
            } else {
              Collection<String> coll = transition.getNegativeLabels();

              for (int ix = 0; ix < Constants.getActivitiesRepositoryVector().size(); ix++) {
                tr_id = tr_prefix + "_" + automaton_index + "_" + single_tr_index;
                String symbol = Constants.getActivitiesRepositoryVector().elementAt(ix);
                if (!coll.contains(symbol)) {
                  tr_label = symbol;
                  RelevantTransition relevantTransition = new RelevantTransition(tr_id, tr_source_state, tr_target_state, tr_label,
                      transition.getPositiveLabel());
                  relevant_transitions_vector.addElement(relevantTransition);
                  transitions_map.put(tr_label, tr_id);
                  single_tr_index++;
                }
              }
            }
            if (!Constants.getAutomataAllStates_vector().contains(tr_source_state)) {
              Constants.getAutomataAllStates_vector().addElement(tr_source_state);
              Constants.getPDDLAutomataAllStates_sb().append(tr_source_state + " - state\n");
            }
            if (!Constants.getAutomataAllStates_vector().contains(tr_target_state)) {
              Constants.getAutomataAllStates_vector().addElement(tr_target_state);
              Constants.getPDDLAutomataAllStates_sb().append(tr_target_state + " - state\n");
            }
            if (transition.getSource().isAccepting() && !automaton_accepting_states_vector.contains(tr_source_state)) {
              automaton_accepting_states_vector.addElement(tr_source_state);
            }
            if (transition.getTarget().isAccepting() && !automaton_accepting_states_vector.contains(tr_target_state)) {
              automaton_accepting_states_vector.addElement(tr_target_state);
            }
          }
        }
        if (automaton_accepting_states_vector.size() > 1) {
          String aut_abstract_state = st_prefix + "_" + automaton_index + "_" + "abstract";

          Constants.getAutomata_abstract_accepting_states_vector().addElement(aut_abstract_state);

          Constants.getPDDLAutomataAcceptingStates_sb().append("(currstate " + aut_abstract_state + ")\n");

          Constants.getPDDLAutomataAllStates_sb().append(aut_abstract_state + " - state\n");

          for (int yu = 0; yu < automaton_accepting_states_vector.size(); yu++) {
            if (!Constants.getAutomataAcceptingStates_vector().contains(automaton_accepting_states_vector.elementAt(yu))) {
              Constants.getAutomataAcceptingStates_vector().addElement(automaton_accepting_states_vector.elementAt(yu));
            }
            if (!Constants.getAutomataAllStates_vector().contains(automaton_accepting_states_vector.elementAt(yu))) {
              Constants.getAutomataAllStates_vector().addElement(automaton_accepting_states_vector.elementAt(yu));
            }
          }
        } else {
          Constants.getAutomataAcceptingStates_vector().addElement(automaton_accepting_states_vector.elementAt(0));
          Constants.getPDDLAutomataAcceptingStates_sb().append("(currstate " + automaton_accepting_states_vector.elementAt(0) + ")\n");
        }
        automata_vector.addElement(automaton);
        automaton_index++;
      }
    }

    if (Constants.getPDDL_encoding().equalsIgnoreCase("AAAI17")) {

      Constants.setAutomatonVector(automata_vector);
      Constants.setRelevantTransitions_vector(relevant_transitions_vector);

      Constants.setCombinationOfRelevantTransitions_vector(new Vector<CombinationOfRelevantTransitions>());

      // Reset the global vector containing the combinations of relevant transitions
      Constants.setRelevantTransitions_map(transitions_map);

      for (int as = 0; as < Constants.getAutomataSinkNonAcceptingStatesVector().size(); as++) {
        Constants.getAutomataAllStates_vector().removeElement(Constants.getAutomataSinkNonAcceptingStatesVector().elementAt(as));
        String all_states_string = Constants.getPDDLAutomataAllStates_sb().toString()
            .replaceAll(Constants.getAutomataSinkNonAcceptingStatesVector().elementAt(as) + " - state\n", "");
        StringBuffer sb = new StringBuffer(all_states_string);
        Constants.setPDDLAutomataAllStates_sb(sb);
      }

      // muruw eemaldada
      Constants.setCombinationOfAcceptingStates_vector(new Vector<>());
      Vector<String> automata_id_of_accepting_states_vector = new Vector<String>();
      for (int q = 0; q < Constants.getAutomataAcceptingStates_vector().size(); q++) {
        String state_id = Constants.getAutomataAcceptingStates_vector().elementAt(q);
        //System.out.println(state_id);
        int first_underscore = state_id.indexOf("_");
        int last_underscore = state_id.lastIndexOf("_");
        String automaton_id = state_id.substring(first_underscore + 1, last_underscore);
        //System.out.println(automata_id);
        if (!automata_id_of_accepting_states_vector.contains(automaton_id)) {
          automata_id_of_accepting_states_vector.addElement(automaton_id);
        }
      }
      int k_value = automata_id_of_accepting_states_vector.size();
      Object[] arr = Constants.getAutomataAcceptingStates_vector().toArray();

      Utilities.findCombinationsOfAcceptingStates(arr, k_value, 0, new String[k_value]);

      Set<String> set_of_keys = Constants.getRelevantTransitions_map().keySet();

      Iterator<String> it = set_of_keys.iterator();
      while (it.hasNext()) {

        String key = (String) it.next();
        Collection<String> values = Constants.getRelevantTransitions_map().get(key);
        Object[] values_array = values.toArray();

        //
        // Given a specific label (e.g., A), which groups several transitions of different automata
        // (e.g., tr_0_0, tr_1_1, tr_1_2), it is important to discard those combinations that contain
        // transitions of the same automaton (for example, any combination that includes at the same time
        // tr_1_1 and tr_1_2 must be discarded).
        //
        // FIRST OF ALL, we identify the underlying automata of the relevant transitions associated to the
        // specific label. In the above example, two different automata having ID "0" and "1" are considered.
        //
        Vector<String> automata_id_of_relevant_transitions_vector = new Vector<>();
        for (int l = 0; l < values_array.length; l++) {
          String transition_id = values_array[l].toString();
          //System.out.println(transition_id);
          int first_underscore = transition_id.indexOf("_");
          int last_underscore = transition_id.lastIndexOf("_");
          String automaton_id = transition_id.substring(first_underscore + 1, last_underscore);
          //System.out.println(automata_id);
          if (!automata_id_of_relevant_transitions_vector.contains(automaton_id)) {
            automata_id_of_relevant_transitions_vector.addElement(automaton_id);
          }
        }

        //
        // To identify the number of different automata involved in the relevant transitions helps to set the
        // maximum "k" value to calculate the combination of relevant transitions (e.g., in our example, we
        // calculate combinations with k=1 and k=2 at maximum).
        // The method invoked removes automatically any combination that contains two transitions of the same automaton.
        //
        for (int kl = 1; kl <= automata_id_of_relevant_transitions_vector.size(); kl++) {
          Utilities.findCombinationsOfTransitions(values_array, key, kl, kl, 0, new String[kl]);
        }
      }
    }
    Constants.setContentOfAnyDifferentTraceHashtable(new Hashtable<>());

    for (int j = 0; j < Constants.getAllTracesVector().size(); j++) {

      Trace trace = Constants.getAllTracesVector().elementAt(j);

      //
      // Update the global Hashtable used to record the content of all the different traces of the log (in the String format).
      //
      if (!Constants.getContentOfAnyDifferentTraceHashtable().containsKey(trace.getOriginalTraceContent_string())) {
        Constants.getContentOfAnyDifferentTraceHashtable().put(trace.getOriginalTraceContent_string().toString(), trace.getTraceName());
      }

      //
      // For any analyzed trace, update the variables recording the minimum and maximum length of a log trace.
      //
      /////////////////////////////////////////
      if (j == 0) {
        Constants.setMinimumLengthOfATrace(trace.getOriginalTraceContent_vector().size());
      }
      if (Constants.getMinimumLengthOfATrace() > trace.getOriginalTraceContent_vector().size()) {
        Constants.setMinimumLengthOfATrace(trace.getOriginalTraceContent_vector().size());
      }
      if (Constants.getMaximumLengthOfATrace() < trace.getOriginalTraceContent_vector().size()) {
        Constants.setMaximumLengthOfATrace(trace.getOriginalTraceContent_vector().size());
      }
      /////////////////////////////////////////

      trace.setTraceMissingActivities_vector(new Vector<String>());
      trace.setTraceAlphabetWithMissingActivitiesOfTheConstraints_vector(trace.getTraceAlphabet_vector());

      //Update the missing activities for the specific trace
      for (int kj = 0; kj < Constants.getActivitiesRepositoryVector().size(); kj++) {
        String activity = Constants.getActivitiesRepositoryVector().elementAt(kj);
        trace.getTraceMissingActivities_vector().addElement(activity);
      }

      // A -- Remove from the vector of the missing activities of the trace all the activities that already appear in the trace
      for (int f = 0; f < trace.getOriginalTraceContent_vector().size(); f++) {
        String string = trace.getOriginalTraceContent_vector().elementAt(f);
        trace.getTraceMissingActivities_vector().removeElement(string);

        /////
        if (!trace.getTraceAlphabetWithMissingActivitiesOfTheConstraints_vector().contains(string)) {
          trace.getTraceAlphabetWithMissingActivitiesOfTheConstraints_vector().addElement(string);
        }
        //////
      }

      // B -- Remove from the vector of the missing activities of the trace all the activities that do not appear in any of the DECLARE constraints
      Vector<String> final_missing_activities_vector = new Vector<String>(trace.getTraceMissingActivities_vector());

      for (int hj = 0; hj < trace.getTraceMissingActivities_vector().size(); hj++) {
        String missing_activity = trace.getTraceMissingActivities_vector().elementAt(hj);
        if (!Constants.getAlphabetOfTheConstraintsVector().contains(missing_activity)) {
          final_missing_activities_vector.removeElement(missing_activity);
        }
      }

      // C -- Create possible instances for the missing activities
      trace.setTraceMissingActivities_vector(final_missing_activities_vector);
    }
  }
}
