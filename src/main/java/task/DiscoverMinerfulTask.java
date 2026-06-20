package task;

import controller.ConfigurationMinerFulController;
import javafx.concurrent.Task;

public class DiscoverMinerfulTask extends Task<MinerfulResult> {
	
	private ConfigurationMinerFulController controller;
	
	public DiscoverMinerfulTask(ConfigurationMinerFulController controller) {
		this.controller = controller;
	}

	@Override
	protected MinerfulResult call() throws Exception {
		// TODO Auto-generated method stub
		return controller.getDiscoveryResult();
	}

}