package controller.conformance.datarow;

import java.math.BigDecimal;

import datatable.AbstractDataRow;
import datatable.CellDataWrapper;
import util.ValidationUtils;

public class DataCostDataRow extends AbstractDataRow {

	//Each CellDataWrapper must have a corresponding XProperty() getter where X is the attribute name
	private CellDataWrapper<String> activityName = new CellDataWrapper<String>(this, ValidationUtils.checkMandatoryString);
	private CellDataWrapper<String> attributeName = new CellDataWrapper<String>(this, ValidationUtils.checkMandatoryString);
	private CellDataWrapper<BigDecimal> nonWritingCost = new CellDataWrapper<BigDecimal>(this, ValidationUtils.checkMandatoryPositiveDecimal);
	private CellDataWrapper<BigDecimal> faultyValueCost = new CellDataWrapper<BigDecimal>(this, ValidationUtils.checkMandatoryPositiveDecimal);

	public DataCostDataRow() {
	}
	
	public DataCostDataRow(String activityName, String attributeName, BigDecimal nonWritingCost, BigDecimal faultyValueCost) {
		this.activityName.setEditingValue(activityName);
		this.attributeName.setEditingValue(attributeName);
		this.nonWritingCost.setEditingValue(nonWritingCost);
		this.faultyValueCost.setEditingValue(faultyValueCost);
	}
	
	public CellDataWrapper<String> activityNameProperty() {
		return activityName;
	}
	public String getActivityName() {
		return activityName.getSavedValue();
	}
	
	public CellDataWrapper<String> attributeNameProperty() {
		return attributeName;
	}
	public String getAttributeName() {
		return attributeName.getSavedValue();
	}
	
	public CellDataWrapper<BigDecimal> nonWritingCostProperty() {
		return nonWritingCost;
	}
	public BigDecimal getNonWritingCost() {
		return nonWritingCost.getSavedValue();
	}
	
	public CellDataWrapper<BigDecimal> faultyValueCostProperty() {
		return faultyValueCost;
	}
	public BigDecimal getFaultyValueCost() {
		return faultyValueCost.getSavedValue();
	}
	
	
	@Override
	protected boolean validateDaraRow() {
		//No row level validation needed for flow costs
		return true;
	}

	@Override
	public String toString() {
		return "DataCostDataRow [activityName=" + activityName.getSavedValue() + ", attributeName=" + attributeName.getSavedValue()
				+ ", nonWritingCost=" + nonWritingCost.getSavedValue() + ", faultyValueCost=" + faultyValueCost.getSavedValue() + "]";
	}

}
