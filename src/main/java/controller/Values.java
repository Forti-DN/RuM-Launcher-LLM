package controller;

public class Values {
	private String constraint;
	private int activations;
	private int fulfillments;
	private int violations;
	private int conflicts;
	
	public Values(String constraint,int activations,int fulfillments,int violations,int conflicts) {
		this.constraint = constraint;
		this.activations = activations;
		this.fulfillments = fulfillments;
		this.violations = violations;
		this.conflicts = conflicts;
	}
	
	public int getActivations() {
		return activations;
	}
	public int getConflicts() {
		return conflicts;
	}public String getConstraint() {
		return constraint;
	}public int getFulfillments() {
		return fulfillments;
	}public int getViolations() {
		return violations;
	}
	public void setActivations(int activations) {
		this.activations = activations;
	}
	public void setConflicts(int conflicts) {
		this.conflicts = conflicts;
	}
	public void setConstraint(String constraint) {
		this.constraint = constraint;
	}
	public void setFulfillments(int fulfillments) {
		this.fulfillments = fulfillments;
	}
	public void setViolations(int violations) {
		this.violations = violations;
	}
}

