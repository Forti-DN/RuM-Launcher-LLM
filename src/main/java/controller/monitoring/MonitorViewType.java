package controller.monitoring;

public enum MonitorViewType {
	DECLARE("Declare"),
	FLUENTS("Fluents");

	private String displayText;

	private MonitorViewType(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}
}
