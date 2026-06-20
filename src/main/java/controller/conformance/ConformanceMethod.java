package controller.conformance;

public enum ConformanceMethod {
	ANALYZER("Declare Analyzer"),
	REPLAYER("Declare Replayer"),
	DATA_REPLAYER("DataAware Declare Replayer"),
	PLAN_BASED("Plan-Based Declarative Checker");

	private String displayText;

	private ConformanceMethod(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}

}
