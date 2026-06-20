package controller.monitoring;

public enum MonitoringMethod {
	MP_DECLARE_ALLOY("MP-Declare w Alloy"),
	MOBUCON_LTL("MoBuConLTL"),
	MOBUCON_LDL("MoBuConLDL"),
	FLLOAT("FLLOAT"), //Same underlying approach as MoBuConLDL, value kept here only for the old UI
	ONLINE_DECLARE("OnlineDeclareAnalyzer"),
	PROBDECLARE("Probdeclare");

	private String displayText;

	private MonitoringMethod(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}

}
