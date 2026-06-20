package task;

import org.processmining.plugins.declareanalyzer.AnalysisResult;
import org.processmining.plugins.declareanalyzer.Tester;

import javafx.concurrent.Task;

public class DeclareAnalyzerTask extends Task<AnalysisResult>{
	
	private String logFile;
	
	private String modelFile;
	
	public DeclareAnalyzerTask(String log, String model) {
		this.logFile = log;
		this.modelFile = model;
	}

	@Override
	protected AnalysisResult call() throws Exception {
		// TODO Auto-generated method stub
		System.out.println("Conformance checking starts millis: "+System.currentTimeMillis());
		return Tester.run(logFile, modelFile);
	}

}
