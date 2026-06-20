package task;


import java.util.List;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.DataConformance.framework.ActivityMatchCost;
import org.processmining.plugins.DataConformance.framework.VariableMatchCost;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;
import org.processmining.plugins.dataawaredeclarereplayer.Runner;
import org.processmining.plugins.dataawaredeclarereplayer.result.AlignmentAnalysisResult;
import javafx.concurrent.Task;

public class DataAwareDeclareReplayerTask extends Task<AlignmentAnalysisResult> {

	private String logFile;
	private String modelFile;
	private Map<ReplayableActivityDefinition,XEventClass> mapping;
	private List<ActivityMatchCost> lamc;
	private List<VariableMatchCost> lvmc;
	
	public DataAwareDeclareReplayerTask(String log, String model, Map<ReplayableActivityDefinition,XEventClass> mapping, List<ActivityMatchCost> lamc,List<VariableMatchCost> lvmc) {
		this.logFile = log;
		this.modelFile = model;
		this.mapping = mapping;
		this.lamc = lamc;
		this.lvmc = lvmc;
	}
	@Override
	protected AlignmentAnalysisResult call() throws Exception {
		// TODO Auto-generated method stub
		System.out.println("Conformance checking starts millis: "+System.currentTimeMillis());
		return Runner.run(logFile, modelFile, mapping, lamc, lvmc);
	}

}
