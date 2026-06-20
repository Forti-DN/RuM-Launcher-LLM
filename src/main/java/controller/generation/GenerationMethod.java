package controller.generation;

public enum GenerationMethod {
	ALLOY("Alloy Log Generator"),
	MINERFUL("MINERful Log Generator"),
	ASP("ASP Log Generator");

	private String displayText;

	private GenerationMethod(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}
}
