package controller.conformance.plannerTextBased.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import javax.swing.DefaultListModel;
import org.deckfour.xes.model.XLog;
import org.processmining.ltl2automaton.plugins.automaton.Automaton;

public class Constants {
  public static String PDDL_encoding = "AAAI17"; //It can be equal to "AAAI17" or to "ICAPS16".

  private static List<String> tracesIds = new ArrayList<>();

  private static Vector<Trace> allTracesVector = new Vector<Trace>();
  private static Vector<String> activitiesRepositoryVector = new Vector<>();

  private static int numberOfTracesToCheckFrom = 0;
  private static int numberOfTracesToCheckTo = 0;

  private static int lengthOfTracesToCheckFrom = 0;
  private static int lengthOfTracesToCheckTo = 0;

  private static DefaultListModel<String> constraintsListModel = new DefaultListModel<>();

  private static boolean removeDuplicatedTraces = false;

  private static PlannerAlgorithm selectedPlannerAlgorithm = PlannerAlgorithm.FAST_DOWNWARD;

  // This is set after reading xes file in FileContentExtractor class
  private static XLog declareModelTracesLog = null;

  // This Hashtable records the content of any trace (the KEY is the name of the trace, the VALUE is the original content of the trace).
  // Notice that if a trace TY has the SAME content of an already included trace TX (i.e., if two traces have the same content), TY will be discarded.
  private static Hashtable<String, String> contentOfAnyDifferentTraceHashtable = new Hashtable<String, String>();

  private static Vector<CombinationOfRelevantTransitions> combination_of_transitions_vector = new Vector<CombinationOfRelevantTransitions>();

  private static Multimap<String, String> relevant_transitions_map = HashMultimap.create();

  private static Vector<RelevantTransition> relevant_transitions_vector = new Vector<RelevantTransition>();

  private static Vector<Vector<String>> activitiesCostVector = new Vector<>();

  private static StringBuffer PDDL_automata_all_states_sb = new StringBuffer();
  private static StringBuffer PDDL_automata_accepting_states_sb = new StringBuffer();
  private static StringBuffer PDDL_automata_initial_states_sb = new StringBuffer();

  private static int minimumLengthOfATrace = 0;
  private static int maximumLengthOfATrace = 0;

  public static int getNumberOfPDDLFiles() {
    return numberOfPDDLFiles;
  }

  public static void setNumberOfPDDLFiles(int numberOfPDDLFiles) {
    Constants.numberOfPDDLFiles = numberOfPDDLFiles;
  }

  private static int numberOfPDDLFiles = 0;

  private static Vector<CombinationOfAcceptingStates> combination_of_accepting_states_vector = new Vector<CombinationOfAcceptingStates>();

  private static Vector<Automaton> automatonVector = new Vector<Automaton>();

  public static String getPDDL_encoding() {
    return PDDL_encoding;
  }

  public static void setPDDL_encoding(String PDDL_encoding) {
    Constants.PDDL_encoding = PDDL_encoding;
  }

  public static Vector<Automaton> getAutomatonVector() {
    return automatonVector;
  }

  public static void setAutomatonVector(Vector<Automaton> automatonVector) {
    Constants.automatonVector = automatonVector;
  }

  private static Vector<String> all_constraints_vector = new Vector<String>();

  private static Vector<String> automata_all_states_vector = new Vector<String>();
  private static Vector<String> automata_accepting_states_vector = new Vector<String>();
  private static Vector<String> automata_initial_states_vector = new Vector<String>();

  private static Vector<String> automata_abstract_accepting_states_vector = new Vector<String>();

  private static Vector<String> automataSinkNonAcceptingStatesVector = new Vector<String>();

  private static Vector<String> alphabetOfTheConstraintsVector = new Vector<String>();

  public static List<String> getTracesIds() {
    return tracesIds;
  }

  public static void setTracesIds(List<String> tracesIds) {
    Constants.tracesIds = tracesIds;
  }

  public static Vector<Trace> getAllTracesVector() {
    return allTracesVector;
  }

  public static void setAllTracesVector(Vector<Trace> allTracesVector) {
    Constants.allTracesVector = allTracesVector;
  }

  public static Vector<String> getActivitiesRepositoryVector() {
    return activitiesRepositoryVector;
  }

  public static void setActivitiesRepositoryVector(Vector<String> activitiesRepositoryVector) {
    Constants.activitiesRepositoryVector = activitiesRepositoryVector;
  }

  public static int getNumberOfTracesToCheckFrom() {
    return numberOfTracesToCheckFrom;
  }

  public static void setNumberOfTracesToCheckFrom(int numberOfTracesToCheckFrom) {
    Constants.numberOfTracesToCheckFrom = numberOfTracesToCheckFrom;
  }

  public static int getNumberOfTracesToCheckTo() {
    return numberOfTracesToCheckTo;
  }

  public static void setNumberOfTracesToCheckTo(int numberOfTracesToCheckTo) {
    Constants.numberOfTracesToCheckTo = numberOfTracesToCheckTo;
  }

  public static int getLengthOfTracesToCheckFrom() {
    return lengthOfTracesToCheckFrom;
  }

  public static void setLengthOfTracesToCheckFrom(int lengthOfTracesToCheckFrom) {
    Constants.lengthOfTracesToCheckFrom = lengthOfTracesToCheckFrom;
  }

  public static int getLengthOfTracesToCheckTo() {
    return lengthOfTracesToCheckTo;
  }

  public static void setLengthOfTracesToCheckTo(int lengthOfTracesToCheckTo) {
    Constants.lengthOfTracesToCheckTo = lengthOfTracesToCheckTo;
  }

  public static int getMinimumLengthOfATrace() {
    return minimumLengthOfATrace;
  }

  public static void setMinimumLengthOfATrace(int minimumLengthOfATrace) {
    Constants.minimumLengthOfATrace = minimumLengthOfATrace;
  }

  public static int getMaximumLengthOfATrace() {
    return maximumLengthOfATrace;
  }

  public static void setMaximumLengthOfATrace(int maximumLengthOfATrace) {
    Constants.maximumLengthOfATrace = maximumLengthOfATrace;
  }

  public static DefaultListModel<String> getConstraintsListModel() {
    return constraintsListModel;
  }

  public static void setConstraintsListModel(DefaultListModel<String> constraintsListModel) {
    Constants.constraintsListModel = constraintsListModel;
  }

  public static Hashtable<String, String> getContentOfAnyDifferentTraceHashtable() {
    return contentOfAnyDifferentTraceHashtable;
  }

  public static void setContentOfAnyDifferentTraceHashtable(Hashtable<String, String> contentOfAnyDifferentTraceHashtable) {
    Constants.contentOfAnyDifferentTraceHashtable = contentOfAnyDifferentTraceHashtable;
  }

  public static boolean isRemoveDuplicatedTraces() {
    return removeDuplicatedTraces;
  }

  public static void setRemoveDuplicatedTraces(boolean removeDuplicatedTraces) {
    Constants.removeDuplicatedTraces = removeDuplicatedTraces;
  }

  public static PlannerAlgorithm getSelectedPlannerAlgorithm() {
    return selectedPlannerAlgorithm;
  }

  public static void setSelectedPlannerAlgorithm(PlannerAlgorithm selectedPlannerAlgorithm) {
    Constants.selectedPlannerAlgorithm = selectedPlannerAlgorithm;
  }

  public static XLog getDeclareModelTracesLog() {
    return declareModelTracesLog;
  }

  public static void setDeclareModelTracesLog(XLog declareModelTracesLog) {
    Constants.declareModelTracesLog = declareModelTracesLog;
  }

  public static Vector<RelevantTransition> getRelevantTransitions_vector() {
    return relevant_transitions_vector;
  }

  public static void setRelevantTransitions_vector(Vector<RelevantTransition> transitions_vector) {
    Constants.relevant_transitions_vector = transitions_vector;
  }

  //
  // Getters and Setters to retrieve and manipulate the vectors containing all the automata and their relevant transitions (for AAAI17 encoding).
  //

  public static Vector<CombinationOfRelevantTransitions> getCombinationOfRelevantTransitions_vector() {
    return combination_of_transitions_vector;
  }

  public static void setCombinationOfRelevantTransitions_vector(Vector<CombinationOfRelevantTransitions> combination_of_transitions_vector) {
    Constants.combination_of_transitions_vector = combination_of_transitions_vector;
  }

  public static Multimap<String, String> getRelevantTransitions_map() {
    return relevant_transitions_map;
  }

  public static void setRelevantTransitions_map(Multimap<String, String> relevant_transitions_map) {
    Constants.relevant_transitions_map = relevant_transitions_map;
  }

  public static Vector<Vector<String>> getActivitiesCostVector() {
    return activitiesCostVector;
  }

  public static void setActivitiesCostVector(Vector<Vector<String>> activitiesCostVector) {
    Constants.activitiesCostVector = activitiesCostVector;
  }

  public static StringBuffer getPDDLAutomataAllStates_sb() {
    return PDDL_automata_all_states_sb;
  }

  public static StringBuffer getPDDLAutomataAcceptingStates_sb() {
    return PDDL_automata_accepting_states_sb;
  }

  public static StringBuffer getPDDLAutomataInitialStates_sb() {
    return PDDL_automata_initial_states_sb;
  }

  public static void setPDDLAutomataAllStates_sb(StringBuffer pDDL_automata_all_states_sb) {
    PDDL_automata_all_states_sb = pDDL_automata_all_states_sb;
  }

  public static void setPDDLAutomataAcceptingStates_sb(StringBuffer pDDL_automata_accepting_states_sb) {
    PDDL_automata_accepting_states_sb = pDDL_automata_accepting_states_sb;
  }

  public static void setPDDLAutomataInitialStates_sb(StringBuffer pDDL_automata_initial_states_sb) {
    PDDL_automata_initial_states_sb = pDDL_automata_initial_states_sb;
  }

  public static Vector<String> getAllConstraints_vector() {
    return all_constraints_vector;
  }

  public static void setAllConstraints_vector(Vector<String> cnt_vector) {
    Constants.all_constraints_vector = cnt_vector;
  }

  public static Vector<String> getAutomataAllStates_vector() {
    return automata_all_states_vector;
  }

  public static Vector<String> getAutomataAcceptingStates_vector() {
    return automata_accepting_states_vector;
  }

  public static Vector<String> getAutomataInitialStates_vector() {
    return automata_initial_states_vector;
  }

  public static void setAutomataAllStates_vector(Vector<String> automata_all_states) {
    Constants.automata_all_states_vector = automata_all_states;
  }

  public static void setAutomataAcceptingStates_vector(Vector<String> automata_accepting_states) {
    Constants.automata_accepting_states_vector = automata_accepting_states;
  }

  public static void setAutomataInitialStates_vector(Vector<String> automata_initial_states) {
    Constants.automata_initial_states_vector = automata_initial_states;
  }

  public static Vector<String> getAutomata_abstract_accepting_states_vector() {
    return automata_abstract_accepting_states_vector;
  }

  public static void setAutomata_abstract_accepting_states_vector(Vector<String> automata_abstract_accepting_states_vector) {
    Constants.automata_abstract_accepting_states_vector = automata_abstract_accepting_states_vector;
  }

  public static Vector<String> getAutomataSinkNonAcceptingStatesVector() {
    return automataSinkNonAcceptingStatesVector;
  }

  public static void setAutomataSinkNonAcceptingStatesVector(Vector<String> automataSinkNonAcceptingStatesVector) {
    Constants.automataSinkNonAcceptingStatesVector = automataSinkNonAcceptingStatesVector;
  }

  public static Vector<String> getAlphabetOfTheConstraintsVector() {
    return alphabetOfTheConstraintsVector;
  }

  public static void setAlphabetOfTheConstraintsVector(Vector<String> alphabetOfTheConstraintsVector) {
    Constants.alphabetOfTheConstraintsVector = alphabetOfTheConstraintsVector;
  }

  public static Vector<CombinationOfAcceptingStates> getCombinationOfAcceptingStates_vector() {
    return combination_of_accepting_states_vector;
  }
  public static void setCombinationOfAcceptingStates_vector(Vector<CombinationOfAcceptingStates> combination_vector) {
    Constants.combination_of_accepting_states_vector = combination_vector;
  }
}
