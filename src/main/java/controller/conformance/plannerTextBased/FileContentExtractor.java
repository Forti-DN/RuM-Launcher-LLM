package controller.conformance.plannerTextBased;

import java.io.File;
import java.util.Vector;
import controller.conformance.plannerTextBased.model.Trace;
import controller.conformance.plannerTextBased.utils.XLogReader;
import controller.conformance.plannerTextBased.model.Constants;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.declare.visualizing.AssignmentModel;
import org.processmining.plugins.declare.visualizing.AssignmentViewBroker;
import org.processmining.plugins.declare.visualizing.ConstraintDefinition;
import org.processmining.plugins.declare.visualizing.Parameter;
import org.processmining.plugins.declare.visualizing.XMLBrokerFactory;

public class FileContentExtractor {

  // read decl model
  public void readDeclareModelFileContent(File declareModel) {
    try {
      AssignmentViewBroker broker = XMLBrokerFactory.newAssignmentBroker(declareModel.getAbsolutePath());

      AssignmentModel mod = broker.readAssignment();

      for (ConstraintDefinition cd : mod.getConstraintDefinitions()) {
        boolean validConstraint = true;
        Vector<String> activitiesNotInRepoVector = new Vector<>();
        String constraint = cd.getName() + "(";

        int index = 0;
        for (Parameter p : cd.getParameters()) {
          if (cd.getBranches(p).iterator().hasNext()) {
            String activityName = modifiyActivityName(cd.getBranches(p).iterator().next().toString().toLowerCase());

            if (Constants.getActivitiesRepositoryVector().contains(activityName)) {
              activitiesNotInRepoVector.addElement(activityName);
              validConstraint = false;
            }

            cd.getBranches(p).iterator().next();
            constraint = constraint + activityName;

            if (index < cd.getParameters().size() - 1) {
              constraint = constraint + ",";
            }
            index++;
          }
        }

        constraint += ")";

        // TODO H_MenuPerspective.java row 400 constraint loogika. Kas seda vaja??
        if (!validConstraint) {
          throw new Exception(constraint + " constraint refers to the activity " + activitiesNotInRepoVector.elementAt(0) +
              " which is not listed in the activities repository. Such constraint can not be properly improted!");
        } else if (!Constants.getConstraintsListModel().contains(constraint)) {

          Constants.getConstraintsListModel().addElement(constraint);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // read log file
  // Based on H_MenuPerspective _view.getOpenMenuItem actionPerformed row 80
  public void readLogFileContent(File declareModelTracesLog) {
    try {
      XLog log = XLogReader.openLog(declareModelTracesLog.getAbsolutePath());

      int traceIntId = 0;

      // Vector used to record the complete alphabet of activities used in the log traces.
      Vector<String> loaded_alphabet_vector = new Vector<String>();

      // Vector used to record the activities (in their original order) of a specific trace of the log.
      Vector<String> loaded_trace_activities_vector = new Vector<String>();

      for (XTrace trace : log) {
        traceIntId++;
        String traceName = XConceptExtension.instance().extractName(trace);
        Trace t = new Trace("Trace#" + traceIntId, traceName);
        Constants.getTracesIds().add(t.getTraceID());

        loaded_trace_activities_vector = new Vector<>();

        for (XEvent event : trace) {
          String activityName = modifiyActivityName(XConceptExtension.instance().extractName(event).toLowerCase());

          String eventType = XLifecycleExtension.instance().extractTransition(event).toLowerCase();

          loaded_trace_activities_vector.addElement(activityName + "_" + eventType);

          if (!loaded_alphabet_vector.contains(activityName + "_" + eventType)) {
            loaded_alphabet_vector.addElement(activityName + "_" + eventType);
          }
        }

        String traceContent = "";
        for (int j = 0; j < loaded_trace_activities_vector.size(); j++) {
          String string = loaded_trace_activities_vector.elementAt(j);

          if (!t.getTraceAlphabet_vector().contains(string)) {
            t.getTraceAlphabet_vector().addElement(string);
          }

          // Hashtable used to set the number of activity instances of a trace
          if (t.getNumberOfActivityInstances_Hashtable().containsKey(string)) {
            int number_of_instances = t.getNumberOfActivityInstances_Hashtable().get(string);
            number_of_instances = number_of_instances + 1;
            t.getNumberOfActivityInstances_Hashtable().put(string, number_of_instances);
          } else {
            t.getNumberOfActivityInstances_Hashtable().put(string, 1);
          }

          for (int p = 0; p < loaded_trace_activities_vector.size(); p++) {

            String string_key = string + p;

            if (!t.getAssociationsToActivityInstances_Hashtable().containsKey(string_key)) {
              t.getAssociationsToActivityInstances_Hashtable().put(string_key, string);
              t.getTraceContentWithActivitiesInstances_vector().addElement(string_key);
              t.getOriginalTraceContent_vector().addElement(string);
              traceContent += string + ",";
              break;
            }
          }
        }
        t.setOriginalTraceContentString(traceContent.substring(0, traceContent.length() - 1));

        Constants.getAllTracesVector().addElement(t);
      }

      Constants.setNumberOfTracesToCheckTo(Constants.getAllTracesVector().size());
      Constants.setDeclareModelTracesLog(log);

      System.out.println("added all the vector traces");
      System.out.println(Constants.getAllTracesVector().size());
      System.out.println("Traces of Declare Model Log file read");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String modifiyActivityName(String activityName) {
    if (activityName.contains(" ")) {
      activityName = activityName.replaceAll(" ", "");
    }

    if (activityName.contains("/")) {
      activityName = activityName.replaceAll("\\/", "");
    }

    if (activityName.contains("(")) {
      activityName = activityName.replaceAll("\\(", "");
    }

    if (activityName.contains(")")) {
      activityName = activityName.replaceAll("\\)", "");
    }

    if (activityName.contains("<")) {
      activityName = activityName.replaceAll("\\<", "");
    }

    if (activityName.contains(">")) {
      activityName = activityName.replaceAll("\\>", "");
    }

    if (activityName.contains(".")) {
      activityName = activityName.replaceAll("\\.", "");
    }

    if (activityName.contains(",")) {
      activityName = activityName.replaceAll("\\,", "_");
    }

    if (activityName.contains("+")) {
      activityName = activityName.replaceAll("\\+", "_");
    }

    if (activityName.contains("-")) {
      activityName = activityName.replaceAll("\\-", "_");
    }

    return activityName;
  }
}
