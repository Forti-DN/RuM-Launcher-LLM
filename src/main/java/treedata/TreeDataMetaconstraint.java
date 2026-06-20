package treedata;

import controller.monitoring.MetaconstraintTemplate;
import rum.algorithms.mobuconldl.MonitoringState;

public class TreeDataMetaconstraint extends TreeDataBase {
	private MetaconstraintTemplate metaconstraintTemplate;
	private String firstConstraint;
	private MonitoringState constraintStatus;
	private String activity;
	private String secondConstraint;
	
	private String metaconstraintText;
	
	public TreeDataMetaconstraint() {
		super();
	}
	
	public MetaconstraintTemplate getMetaconstraintTemplate() {
		return metaconstraintTemplate;
	}
	public void setMetaconstraintTemplate(MetaconstraintTemplate metaconstraintTemplate) {
		this.metaconstraintTemplate = metaconstraintTemplate;
	}
	

	public String getFirstConstraint() {
		return firstConstraint;
	}
	public void setFirstConstraint(String firstConstraint) {
		this.firstConstraint = firstConstraint;
	}
	
	public MonitoringState getConstraintStatus() {
		return constraintStatus;
	}
	public void setConstraintStatus(MonitoringState constraintStatus) {
		this.constraintStatus = constraintStatus;
	}
	
	public String getActivity() {
		return activity;
	}
	public void setActivity(String activity) {
		this.activity = activity;
	}
	
	public String getSecondConstraint() {
		return secondConstraint;
	}
	public void setSecondConstraint(String secondConstraint) {
		this.secondConstraint = secondConstraint;
	}
	
	public void setMetaconstraintText(String metaconstraintText) {
		this.metaconstraintText = metaconstraintText;
	}
	public String getMetaconstraintText() {
		return metaconstraintText;
	}
	
	//To be called after all values are set
	public void updateDisplayText() {
		switch (metaconstraintTemplate) {
		case CONTEXTUAL_ABSENCE:
			setDisplayText(metaconstraintTemplate.getDisplayText() + "\n  " + firstConstraint + "\n  " + constraintStatus.getMobuconltlName() + " | " + activity);
			break;
		case REACTIVE_COMPENSATION: //Fallthrough intended
		case COMPENSATION:
			setDisplayText(metaconstraintTemplate.getDisplayText() + "\n  " + firstConstraint + "\n  " + secondConstraint);
			break;
		default:
			System.err.println("Unsupported metaconstraint template selected: " + metaconstraintTemplate);
			break;
		}
	}
}
