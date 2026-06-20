package controller.conformance.datarow;

import java.math.BigDecimal;

import datatable.AbstractDataRow;
import datatable.CellDataWrapper;
import util.ValidationUtils;

public class FlowCostDataRow extends AbstractDataRow {

	//Each CellDataWrapper must have a corresponding XProperty() getter where X is the attribute name
	private CellDataWrapper<String> activityName = new CellDataWrapper<String>(this, ValidationUtils.checkMandatoryString);
	private CellDataWrapper<BigDecimal> insertionCost = new CellDataWrapper<BigDecimal>(this, ValidationUtils.checkMandatoryPositiveDecimal);
	private CellDataWrapper<BigDecimal> deletionCost = new CellDataWrapper<BigDecimal>(this, ValidationUtils.checkMandatoryPositiveDecimal);

	public FlowCostDataRow() {
	}
	
	public FlowCostDataRow(String activityName, BigDecimal insertionCost, BigDecimal deletionCost) {
		this.activityName.setEditingValue(activityName);
		this.insertionCost.setEditingValue(insertionCost);
		this.deletionCost.setEditingValue(deletionCost);
	}
	
	public CellDataWrapper<String> activityNameProperty() {
		return activityName;
	}
	public String getActivityName() {
		return activityName.getSavedValue();
	}
	
	public CellDataWrapper<BigDecimal> insertionCostProperty() {
		return insertionCost;
	}
	public BigDecimal getInsertionCost() {
		return insertionCost.getSavedValue();
	}
	
	public CellDataWrapper<BigDecimal> deletionCostProperty() {
		return deletionCost;
	}
	public BigDecimal getDeletionCost() {
		return deletionCost.getSavedValue();
	}
	
	
	@Override
	protected boolean validateDaraRow() {
		//No row level validation needed for flow costs
		return true;
	}

	@Override
	public String toString() {
		return "FlowCostDataRow [activityName=" + activityName.getSavedValue() + ", insertionCost=" + insertionCost.getSavedValue()
				+ ", deletionCost=" + deletionCost.getSavedValue() + "]";
	}

}
