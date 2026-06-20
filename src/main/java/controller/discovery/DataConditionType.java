package controller.discovery;

public enum DataConditionType {
	ACTIVATIONS("Activations", true),
	//VIOLATIONS("Violations", true),
	CORRELATIONS("Correlations", true),
	NONE("None", false);

	private String displayText;
	private boolean isDataAware;
	
	private DataConditionType(String displayText, boolean isDataAware) {
		this.displayText = displayText;
		this.isDataAware = isDataAware;
	}

	public String getDisplayText() {
		return displayText;
	}
	
	public boolean isDataAware() {
		return isDataAware;
	}
}
