package controller.discovery;

public enum ShowLabelEnum {
	SUPPORT("Support"),
	MIN_TD("Min. Time Distance"),
	AVG_TD("Avg. Time Distance"),
	MAX_TD("Max. Time Distance"),
	NONE("None");

	private String displayText;

	private ShowLabelEnum(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}
}
