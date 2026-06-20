package task;

import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;
import org.processmining.plugins.dataawaredeclarereplayer.Runner;

import javafx.concurrent.Task;

public class ActivityMappingTask extends Task<Map<ReplayableActivityDefinition,XEventClass>> {
	
	private String logFile;
	private String modelFile;
	
	public ActivityMappingTask(String logFile, String modelFile) {
		this.logFile = logFile;
		this.modelFile = modelFile;
	}
	@Override
	protected Map<ReplayableActivityDefinition,XEventClass> call() {
		// TODO Auto-generated method stub
		try {
			return Runner.getMapping(logFile, modelFile);
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
