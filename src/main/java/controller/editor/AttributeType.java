package controller.editor;

public enum AttributeType {
	INTEGER("Integer"),
	FLOAT("Float"),
	ENUMERATION("Enumeration");

	private String displayText;

	private AttributeType(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}

}
