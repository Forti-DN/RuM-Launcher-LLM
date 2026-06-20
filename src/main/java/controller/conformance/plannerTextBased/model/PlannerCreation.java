package controller.conformance.plannerTextBased.model;

import java.io.File;
import java.util.Vector;

public class PlannerCreation {

  private final File declareModel;
  private final File logFile;
  private final PlannerAlgorithm selectedAlgorithm;
  private final String resultPath;
  // TODO kas neid final inte on vaja
  private final int numberOfTracesToCheckFrom;
  private final int numberOfTracesToCheckTo;
  private final int lengthOfTracesToCheckFrom;
  private final int lengthOfTracesToCheckTo;
  private final boolean removeDuplicatedTraces;
  // TODO rumis control flow cost model
  private final Vector<Vector<String>> activitiesCostVector;


  public PlannerCreation(
      File declareModel,
      File logFile,
      PlannerAlgorithm selectedAlgorithm,
      String resultPath,
      int numberOfTracesToCheckFrom,
      int numberOfTracesToCheckTo,
      int lengthOfTracesToCheckFrom,
      int lengthOfTracesToCheckTo,
      boolean removeDuplicatedTraces,
      Vector<Vector<String>> activitiesCostVector
  ) {
    this.declareModel = declareModel;
    this.logFile = logFile;
    this.selectedAlgorithm = selectedAlgorithm;
    this.resultPath = resultPath;
    this.numberOfTracesToCheckFrom = numberOfTracesToCheckFrom;
    this.numberOfTracesToCheckTo = numberOfTracesToCheckTo;
    this.lengthOfTracesToCheckFrom = lengthOfTracesToCheckFrom;
    this.lengthOfTracesToCheckTo = lengthOfTracesToCheckTo;
    this.removeDuplicatedTraces = removeDuplicatedTraces;
    this.activitiesCostVector = activitiesCostVector;
  }

  public File getDeclareModel() {
    return declareModel;
  }

  public File getLogFile() {
    return logFile;
  }

  public PlannerAlgorithm getSelectedAlgorithm() {
    return selectedAlgorithm;
  }

  public String getResultPath() {
    return resultPath;
  }

  public int getNumberOfTracesToCheckFrom() {
    return numberOfTracesToCheckFrom;
  }

  public int getNumberOfTracesToCheckTo() {
    return numberOfTracesToCheckTo;
  }

  public int getLengthOfTracesToCheckFrom() {
    return lengthOfTracesToCheckFrom;
  }

  public int getLengthOfTracesToCheckTo() {
    return lengthOfTracesToCheckTo;
  }

  public boolean getRemoveDuplicatedTraces() {
    return removeDuplicatedTraces;
  }

  // no use case currently
  public Vector<Vector<String>> getActivitiesCostVector() {
    return activitiesCostVector;
  }
}
