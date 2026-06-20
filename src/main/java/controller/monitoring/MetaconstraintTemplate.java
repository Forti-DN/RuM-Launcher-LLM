package controller.monitoring;

public enum MetaconstraintTemplate {
	CONTEXTUAL_ABSENCE("Contextual Absence"),
	REACTIVE_COMPENSATION("Reactive Compensation"),
	COMPENSATION("Compensation");

	private String displayText;

	private MetaconstraintTemplate(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}
}
