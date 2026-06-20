package task.discovery;

import java.util.List;

import controller.discovery.data.DiscoveredActivity;
import controller.discovery.data.DiscoveredConstraint;
import minerful.concept.ProcessModel;

public class DiscoveryTaskResult {
	
	private List<DiscoveredActivity> activities;
	private List<DiscoveredConstraint> constraints;
	private ProcessModel discoveryModel;

	public DiscoveryTaskResult() {
	}

	public List<DiscoveredActivity> getActivities() {
		return activities;
	}
	
	public void setActivities(List<DiscoveredActivity> activities) {
		this.activities = activities;
	}

	public List<DiscoveredConstraint> getConstraints() {
		return constraints;
	}
	
	public void setConstraints(List<DiscoveredConstraint> constraints) {
		this.constraints = constraints;
	}

	public ProcessModel getDiscoveryModel() {
		return discoveryModel;
	}

	public void setDiscoveryModel(ProcessModel discoveryModel) {
		this.discoveryModel = discoveryModel;
	}
}
