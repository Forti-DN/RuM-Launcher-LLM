package task;

import org.processmining.plugins.declareminer.visualizing.DeclareMinerOutput;

import controller.ConfigurationController;
import javafx.concurrent.Task;

public class DiscoverTask extends Task<DeclareMinerOutput>{
	
	private ConfigurationController controller;
	
	public DiscoverTask(ConfigurationController controller) {
		this.controller = controller;
	}
	@Override
	protected DeclareMinerOutput call() throws Exception {
		return controller.getDiscoveryResult();
	}

}
