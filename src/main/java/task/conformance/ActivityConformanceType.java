package task.conformance;

public class ActivityConformanceType {
	/*
	FULFILLMENT("detail--fulfillment", "Fulfillment"),	// Activity fulfills this constraint
	VIOLATION("detail--violation", "Violation"), 		// Activity violates this constraint
	INSERTION("detail--insertion", "Insertion\n(contributes to solve this constraint)"),	// Activity should be inserted into the trace to solve this constraint
	INSERTION_OTHER("detail--insertion-other", "Insertion\n(contributes to solve a different constraint)"),	// Activity should be inserted into the trace to solve another constraint
	DELETION("detail--deletion", "Deletion\n(contributes to solve this constraint)"), 		// Activity should be deleted from the trace to solve this constraint
	DELETION_OTHER("detail--deletion-other", "Deletion\n(contributes to solve a different constraint)"), 	// Activity should be deleted from the trace to solve another constraint
	DATA_DIFFERENCE("detail--data-difference", "Data difference"),	// Activity should have different data in the trace
	NONE("detail--none", null);	// None of the above holds true for this activity and constraint
	*/
	
	public enum Type {
		FULFILLMENT("detail--fulfillment"),	// Activity fulfills this constraint
		VIOLATION("detail--violation"), 		// Activity violates this constraint
		INSERTION("detail--insertion"),	// Activity should be inserted into the trace to solve this constraint
		INSERTION_OTHER("detail--insertion-other"),	// Activity should be inserted into the trace to solve another constraint
		DELETION("detail--deletion"), 		// Activity should be deleted from the trace to solve this constraint
		DELETION_OTHER("detail--deletion-other"), 	// Activity should be deleted from the trace to solve another constraint
		DATA_DIFFERENCE("detail--data-difference"),	// Activity should have different data in the trace
		NONE("detail--none");	// None of the above holds true for this activity and constraint
	
		private final String cssClass;
		
		private Type(String cssClass) { //, String tooltipText) {
			this.cssClass = cssClass;
		}
	}
	
	private Type type;
	private String tooltipText;

	public ActivityConformanceType(Type type) {
		this.type = type;
		this.tooltipText = null;
	}
	
	public Type getType() {
		return this.type;
	}
	
	public String getCssClass() {
		return type.cssClass;
	}
	
	public String getTooltipText() {
		return tooltipText;
	}
	
	public void setTooltipText(String tooltipText) {
		this.tooltipText = tooltipText;
	}
}
