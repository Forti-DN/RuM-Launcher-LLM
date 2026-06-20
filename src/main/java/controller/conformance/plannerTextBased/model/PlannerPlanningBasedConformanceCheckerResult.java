package controller.conformance.plannerTextBased.model;

import java.util.List;
import org.deckfour.xes.model.XLog;
import task.conformance.ActivityConformanceType;

public class PlannerPlanningBasedConformanceCheckerResult {

  private XLog resultXLog;
  private List<Integer> traceDeletedEvents;
  private List<Integer> traceInsertedEvents;
  private List<List<ActivityConformanceType>> traceActivityTypes;

  public PlannerPlanningBasedConformanceCheckerResult(XLog resultXLog, List<List<ActivityConformanceType>> traceActivityTypes,
      List<Integer> traceDeletedEvents, List<Integer> traceInsertedEvents) {
    this.resultXLog = resultXLog;
    this.traceActivityTypes = traceActivityTypes;
    this.traceDeletedEvents = traceDeletedEvents;
    this.traceInsertedEvents = traceInsertedEvents;
  }

  public XLog getResultXLog() {
    return resultXLog;
  }

  public void setResultXLog(XLog resultXLog) {
    this.resultXLog = resultXLog;
  }

  public List<Integer> getDeletedEvents() {
    return traceDeletedEvents;
  }

  public void setDeletedEventsIds(List<Integer> deletedEventsIds) {
    this.traceDeletedEvents = deletedEventsIds;
  }

  public List<Integer> getInsertedEvents() {
    return traceInsertedEvents;
  }

  public void setInsertedEventsIds(List<Integer> insertedEventsIds) {
    this.traceInsertedEvents = insertedEventsIds;
  }

  public List<List<ActivityConformanceType>> getTraceActivityTypes() {
    return traceActivityTypes;
  }

  public void setTraceActivityTypes(List<List<ActivityConformanceType>> traceActivityTypes) {
    this.traceActivityTypes = traceActivityTypes;
  }
}
