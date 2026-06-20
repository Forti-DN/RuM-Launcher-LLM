package controller.discovery;

public enum DiscoveryMethod {
	DECLARE("Declare Miner"),
	MINERFUL("MINERful"),
	MP_DECLARE("MP Declare Miner"),
	MP_MINERFUL("MP MINERful");

	private String displayText;

	private DiscoveryMethod(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}

}
