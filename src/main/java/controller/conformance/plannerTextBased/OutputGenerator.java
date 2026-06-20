package controller.conformance.plannerTextBased;

import controller.conformance.plannerTextBased.model.PlannerPlanningBasedConformanceCheckerResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import controller.conformance.plannerTextBased.model.Constants;
import task.conformance.ActivityConformanceType;
import task.conformance.ActivityConformanceType.Type;

public class OutputGenerator {

  public PlannerPlanningBasedConformanceCheckerResult generateXLogOutput() throws IOException {
    if (Constants.getDeclareModelTracesLog() != null) {
      switch (Constants.getSelectedPlannerAlgorithm()) {
        case FAST_DOWNWARD:
          System.out.println("Generate output with Fast-Downward");
          return generateXLogWithFastDownwardAlgorithm();
        case SYMBA:
          System.out.println("Generate output with Symba");
          return generateXLogWithFastDownwardAlgorithm();
        default:
          System.out.println("Algorithm implementation does not exist");
      }
    }
    return new PlannerPlanningBasedConformanceCheckerResult(Constants.getDeclareModelTracesLog(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
  }

  private PlannerPlanningBasedConformanceCheckerResult generateXLogWithFastDownwardAlgorithm() throws IOException {
    XLog originalXLogFile = Constants.getDeclareModelTracesLog();
    XLog newXLog = (XLog) originalXLogFile.clone();/*; new XLogImpl(originalXLogFile.getAttributes())*/

    List<Integer> traceDeletedEvents = new ArrayList<>();
    List<Integer> traceInsertedEvents = new ArrayList<>();

    List<List<ActivityConformanceType>> traceActivityTypes = new ArrayList<>();

    for (int i = 0; i < originalXLogFile.size(); i++) {
      int deletedEvents = 0;
      int insertedEvents = 0;

      traceActivityTypes.add(new ArrayList<>());

      runAlgorithm(i);
      List<String> sasPlanOutput = readSasPlan();

      XTrace oldXTrace = originalXLogFile.get(i);
      int traceEventId = 0;
      for (String sasPlanLine : sasPlanOutput) {
        if (sasPlanLine != null) {
          String firstPartOfLine = sasPlanLine.substring(0, 5);
          if (firstPartOfLine.contains("del")) {
            traceActivityTypes.get(i).add(new ActivityConformanceType(Type.DELETION));
            deletedEvents += 1;
            traceEventId++;
          } else if (firstPartOfLine.contains("add")) {
            XEvent xevent = new XEventImpl();
            XAttributeMap xatributeMap = new XAttributeMapImpl();
            // TODO nimi
/*
            String traceName = "Case No. " + String.format(caseNrFormat, i + 1);
            generatedLog.get(i).getAttributes().put("concept:name", new XAttributeLiteralImpl("concept:name", traceName));*/

            XAttribute xatribute = new XAttributeLiteralImpl("concept:name", "random name");
            xatributeMap.put("concept:name", xatribute);
            xevent.setAttributes(xatributeMap);
            oldXTrace.add(traceEventId, xevent);

            insertedEvents += 1;
            traceActivityTypes.get(i).add(new ActivityConformanceType(Type.INSERTION));
          } // sync
          else {
            traceActivityTypes.get(i).add(new ActivityConformanceType(Type.NONE));
            traceEventId++;
          }
        }
      }
      traceDeletedEvents.add(deletedEvents);
      traceInsertedEvents.add(insertedEvents);
    }

    return new PlannerPlanningBasedConformanceCheckerResult(newXLog, traceActivityTypes, traceDeletedEvents, traceInsertedEvents);
  }

  private List<String> readSasPlan() throws IOException {
    String filename = "";
    switch (Constants.getSelectedPlannerAlgorithm()) {
      case SYMBA:
        filename = "plan-based-conformance-scripts/seq-opt-symba-2/out.txt";
        break;
      case FAST_DOWNWARD:
        filename = "plan-based-conformance-scripts/fast-downward/src/sas_plan";
        break;
    }
    return Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
  }


  public void runAlgorithm(int traceIndex) throws IOException {
    switch (Constants.getSelectedPlannerAlgorithm()) {
      case FAST_DOWNWARD:
        System.out.println("Run FD algorithm for every trace");
        runAlgorithmShellScript("plan-based-conformance-scripts" + System.getProperty("file.separator") + "run_FD_all", traceIndex);
        break;
      case SYMBA:
        System.out.println("Run Symba algorithm for every trace");
        runAlgorithmShellScript("plan-based-conformance-scripts" + System.getProperty("file.separator") + "run_SYMBA", traceIndex);
        break;
      default:
        System.out.println("Algorithm implementation does not exist");
    }
  }

/*  private void runAlgorithmShScript(String scriptFileName) throws IOException, InterruptedException {
    boolean isWindowsOperatingSystem = System.getProperty("os.name").toLowerCase().startsWith("windows");

    String homeDirectory = System.getProperty("user.home");

    Process process = isWindowsOperatingSystem ?
        Runtime.getRuntime().exec(String.format("cmd.exe /c dir %s", homeDirectory)) :
        Runtime.getRuntime().exec(String.format("sh -c ls %s", homeDirectory));

    StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
    Executors.newSingleThreadExecutor().submit(streamGobbler);
    int exitCode = process.waitFor();
    assert exitCode == 0;
  }*/

/*  private void runAlgorithmShellScript(String scriptFileName) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(scriptFileName);
    Process p = pb.start();
    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null) {
      System.out.println(line);
    }
  }*/

  public void runAlgorithmShellScript(String filePath, int traceIndex) throws IOException {
    File file = new File(filePath);
    if (!file.isFile()) {
      throw new IllegalArgumentException("The file " + filePath + " does not exist");
    }
    if (isLinux()) {
      Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", filePath + " " + traceIndex + 1}, null);
    } else if (isWindows()) {
      Process p = Runtime.getRuntime().exec("cmd /c start " + filePath + " " + traceIndex + 1);
      /*try {
		ProcessBuilder pb = new ProcessBuilder(file.getAbsolutePath());
		pb.inheritIO();
		Process process = pb.start();
		process.waitFor();
	  } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	  }*/
      
      
    } else {
    	throw new IOException("Operating System found! " + filePath.toString());
    }
  }

  public boolean isLinux() {
    return System.getProperty("os.name").toLowerCase().contains("linux");
  }

  public boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("windows");
  }
  
  class StreamGobbler extends Thread {
	    InputStream is;

	    // reads everything from is until empty. 
	    StreamGobbler(InputStream is) {
	        this.is = is;
	    }

	    public void run() {
	        try {
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
	            while ( (line = br.readLine()) != null)
	                System.out.println(line);    
	        } catch (IOException ioe) {
	            ioe.printStackTrace();  
	        }
	    }
	}
}
