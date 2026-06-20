package util;

public class VariableCostConfig {
	
	private String activity;
	private String attribute;
	private float nonWritingCost;
	private float faultyValueCost;
	
	public VariableCostConfig() {
		
	}
	
	public void setActivity(String activity) {
		this.activity = activity;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	public void setFaultyValueCost(float faultyValueCost) {
		this.faultyValueCost = faultyValueCost;
	}
	public void setNonWritingCost(float nonWritingCost) {
		this.nonWritingCost = nonWritingCost;
	}
	public String getActivity() {
		return activity;
	}
	public String getAttribute() {
		return attribute;
	}
	public float getFaultyValueCost() {
		return faultyValueCost;
	}
	public float getNonWritingCost() {
		return nonWritingCost;
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if(obj == null) return false;
		if(!(obj instanceof VariableCostConfig)) return false;
		else {
			VariableCostConfig vcc = (VariableCostConfig) obj;
			return vcc.getActivity().equals(this.activity) &&
					vcc.getAttribute().equals(this.attribute);
		}
	}

}
