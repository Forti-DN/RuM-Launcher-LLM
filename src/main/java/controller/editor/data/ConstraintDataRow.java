package controller.editor.data;

import datatable.AbstractDataRow;
import datatable.CellDataWrapper;
import treedata.TreeDataActivity;
import util.ConstraintTemplate;
import util.ValidationUtils;

public class ConstraintDataRow extends AbstractDataRow {

	//Each CellDataWrapper must have a corresponding XProperty() getter where X is the attribute name
	private CellDataWrapper<ConstraintTemplate> template = new CellDataWrapper<ConstraintTemplate>(this, ValidationUtils.checkMandatoryConstraintTemplate);
	private CellDataWrapper<TreeDataActivity> activationActivity = new CellDataWrapper<TreeDataActivity>(this, ValidationUtils.checkMandatoryActivity);
	private CellDataWrapper<String> activationCondition = new CellDataWrapper<String>(this, ValidationUtils.checkConstraintDataCondition);
	private CellDataWrapper<TreeDataActivity> targetActivity = new CellDataWrapper<TreeDataActivity>(this, null);
	private CellDataWrapper<String> correlationCondition = new CellDataWrapper<String>(this, ValidationUtils.checkConstraintDataCondition);
	private CellDataWrapper<String> timeCondition = new CellDataWrapper<String>(this, ValidationUtils.checkConstraintTimeCondition);

	public ConstraintDataRow() {
	}

	public ConstraintDataRow(ConstraintTemplate template, TreeDataActivity activationActivity,
			String activationCondition, TreeDataActivity targetActivity,
			String correlationCondition, String timeCondition) {
		this.template.setEditingValue(template);
		this.activationActivity.setEditingValue(activationActivity);
		this.activationCondition.setEditingValue(activationCondition);
		this.targetActivity.setEditingValue(targetActivity);
		this.correlationCondition.setEditingValue(correlationCondition);
		this.timeCondition.setEditingValue(timeCondition);
	}

	public CellDataWrapper<ConstraintTemplate> templateProperty() {
		return template;
	}
	public ConstraintTemplate getTemplate() {
		return template.getSavedValue();
	}

	public CellDataWrapper<TreeDataActivity> activationActivityProperty() {
		return activationActivity;
	}
	public TreeDataActivity getActivationActivity() {
		return activationActivity.getSavedValue();
	}

	public CellDataWrapper<String> activationConditionProperty() {
		return activationCondition;
	}
	public String getActivationCondition() {
		return activationCondition.getSavedValue();
	}

	public CellDataWrapper<TreeDataActivity> targetActivityProperty() {
		return targetActivity;
	}
	public TreeDataActivity getTargetActivity() {
		return targetActivity.getSavedValue();
	}

	public CellDataWrapper<String> correlationConditionProperty() {
		return correlationCondition;
	}
	public String getCorrelationCondition() {
		return correlationCondition.getSavedValue();
	}

	public CellDataWrapper<String> timeConditionProperty() {
		return timeCondition;
	}
	public String getTimeCondition() {
		return timeCondition.getSavedValue();
	}


	@Override
	protected boolean validateDaraRow() {
		boolean allCellsValid = true;
		if (template.getEditingValue().getIsBinary()) {
			if (ValidationUtils.checkMandatoryActivity.apply(targetActivity.getEditingValue()) != null) {
				targetActivity.setEditingError("Target Activity is required for binary template");
				allCellsValid = false;
			}
			correlationCondition.setEditingError(ValidationUtils.checkConstraintDataCondition.apply(correlationCondition.getEditingValue()));
			if (correlationCondition.getEditingError() != null) {
				allCellsValid = false;
			}
		} else {
			if (targetActivity.getEditingValue() != null) {
				targetActivity.setEditingError("Target Activity must be empty for unary template");
				allCellsValid = false;
			}
			if (correlationCondition.getEditingValue() != null && !correlationCondition.getEditingValue().isBlank()) {
				correlationCondition.setEditingError("Correlation Condition must be empty for unary template");
				allCellsValid = false;
			}
		}
		return allCellsValid;
	}

	@Override
	public String toString() {
		return "ConstraintDataRow [template=" + template.getSavedValue() + ", activationActivity=" + activationActivity.getSavedValue()
		+ ", activationCondition=" + activationCondition.getSavedValue() + ", targetActivity=" + targetActivity.getSavedValue()
		+ ", correlationCondition=" + correlationCondition.getSavedValue() + ", timeCondition=" + timeCondition.getSavedValue()
		+ "]";
	}
}