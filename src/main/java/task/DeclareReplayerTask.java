package task;

import java.util.List;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.DataConformance.framework.ActivityMatchCost;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;
import org.processmining.plugins.DeclareConformance.Replayer;
import org.processmining.plugins.DeclareConformance.ResultReplayDeclare;

import javafx.concurrent.Task;

public class DeclareReplayerTask extends Task<ResultReplayDeclare> {
	
	private String logFile;
	private String modelFile;
	private Map<ReplayableActivityDefinition,XEventClass> mapping;
	private List<ActivityMatchCost> lamc;
	
	public DeclareReplayerTask(String log, String model, Map<ReplayableActivityDefinition,XEventClass> mapping, List<ActivityMatchCost> lamc) {
		this.logFile = log;
		this.mapping = mapping;
		this.modelFile = model;
		this.lamc = lamc;
	}

	@Override
	protected ResultReplayDeclare call() throws Exception {
		// TODO Auto-generated method stub
		System.out.println("Conformance checking starts millis: "+System.currentTimeMillis());
		return Replayer.run(logFile, modelFile, mapping, lamc);
	}

}
