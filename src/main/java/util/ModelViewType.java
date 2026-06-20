package util;

public enum ModelViewType {
	DECLARE("Declare"),
	TEXTUAL("Textual"),
	AUTOMATON("Automaton"),
	XML_MODEL("XML Model"); //Only used for export

	private String displayText;
	
	private ModelViewType(String displayText) {
		this.displayText = displayText;
	}
	
	public String getDisplayText() {
		return displayText;
	}
	
}
