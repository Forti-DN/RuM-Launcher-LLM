package global;

public enum InventoryElementTypeEnum {
	PROCESS_MODEL("process model"),
	EVENT_LOG("event log"),
	LTL_MODEL("ltl model"); //TODO: This is probably never used at the moment, should double-check

	String name;
	
	private InventoryElementTypeEnum(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
