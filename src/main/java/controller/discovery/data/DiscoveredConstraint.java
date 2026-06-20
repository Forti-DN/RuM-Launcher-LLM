package controller.discovery.data;

import java.time.Duration;

import task.discovery.mp_enhancer.Rule;
import util.ConstraintTemplate;

public class DiscoveredConstraint {

	private ConstraintTemplate template;
	private DiscoveredActivity activationActivity;
	private DiscoveredActivity targetActivity;
	private float constraintSupport;
	private Duration minTD, avgTD, maxTD;
	private Rule dataCondition;
	
	public DiscoveredConstraint(ConstraintTemplate template, DiscoveredActivity activationActivity, DiscoveredActivity targetActivity) {
		this.template = template;
		this.activationActivity = activationActivity;
		this.targetActivity = targetActivity;
	}

	public ConstraintTemplate getTemplate() {
		return template;
	}

	public DiscoveredActivity getActivationActivity() {
		return activationActivity;
	}

	public DiscoveredActivity getTargetActivity() {
		return targetActivity;
	}

	public float getConstraintSupport() {
		return constraintSupport;
	}
	
	public void setConstraintSupport(float constraintSupport) {
		this.constraintSupport = constraintSupport;
	}
	
	public Duration getMinTD() {
		return minTD;
	}
	
	public void setMinTD(Duration minTD) {
		this.minTD = minTD;
	}

	public Duration getAvgTD() {
		return avgTD;
	}
	
	public void setAvgTD(Duration avgTD) {
		this.avgTD = avgTD;
	}
	
	public Duration getMaxTD() {
		return maxTD;
	}
	
	public void setMaxTD(Duration maxTD) {
		this.maxTD = maxTD;
	}
	
	public Rule getDataCondition() {
		return dataCondition;
	}
	
	public void setDataCondition(Rule dataCond) {
		this.dataCondition = dataCond;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((activationActivity == null) ? 0 : activationActivity.hashCode());
		result = prime * result + ((targetActivity == null) ? 0 : targetActivity.hashCode());
		result = prime * result + ((template == null) ? 0 : template.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DiscoveredConstraint other = (DiscoveredConstraint) obj;
		if (activationActivity == null) {
			if (other.activationActivity != null)
				return false;
		} else if (!activationActivity.equals(other.activationActivity))
			return false;
		if (targetActivity == null) {
			if (other.targetActivity != null)
				return false;
		} else if (!targetActivity.equals(other.targetActivity))
			return false;
		if (template != other.template)
			return false;
		return true;
	}

	@Override
	public String toString() {
		String output = "Constraint";
		if (constraintSupport > 0)
			output += "(supp=" + constraintSupport + ")";
		output += ": " + template + "[";
		
		if (template.getIsBinary())
			output += template.getReverseActivationTarget() ? 
					targetActivity.getActivityFullName() + ", " + activationActivity.getActivityFullName()
					: activationActivity.getActivityFullName() + ", " + targetActivity.getActivityFullName();
		else
			output += activationActivity.getActivityFullName();
		
		output += "] ";
		
		if (dataCondition != null)
			output += dataCondition.toString();
		else
			output += template.getIsBinary() ? "| |" : "|";
		
		if (minTD != null && avgTD != null && maxTD != null)
			output += " --- TDs [min=" + minTD + ", avg=" + avgTD + ", max=" + maxTD + "]";

		return output;
	}

}
