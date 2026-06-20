package task;

import java.util.Map;

import minerful.concept.ProcessModel;
import minerful.postprocessing.params.PostProcessingCmdParameters;

public class MinerfulResult {
	
	private Map<String,Double> actSuppMap;
	private ProcessModel model;
	private PostProcessingCmdParameters params;
	
	public MinerfulResult(Map<String,Double> map, ProcessModel model, PostProcessingCmdParameters params) {
		this.actSuppMap = map;
		this.model = model;
		this.params = params;
	}
	
	public Map<String, Double> getActSuppMap() {
		return actSuppMap;
	}
	public ProcessModel getModel() {
		return model;
	}
	public PostProcessingCmdParameters getParams() {
		return params;
	}

}
