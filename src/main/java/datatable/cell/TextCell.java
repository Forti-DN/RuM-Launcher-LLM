package datatable.cell;

import datatable.AbstractDataRow;
import datatable.AbstractDataRow.RowStatus;
import datatable.CellDataWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import util.ValidationUtils;

public class TextCell<T extends AbstractDataRow> extends TableCell<T , CellDataWrapper<String>> {

	private Label savedValueLabel = new Label();
	private TextField editingTextField = new TextField();
	private Tooltip errorTooltip = new Tooltip();

	public TextCell() {
		editingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && !newValue.isBlank()) {
				setErrorMessage(null);
			}
		});

		editingTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == false) {
				TextCell.this.getItem().setEditingValue(editingTextField.getText());
				TextCell.this.getItem().validateCellEdit();
			}
		});

		editingTextField.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		errorTooltip.setShowDelay(Duration.millis(200));
		errorTooltip.setShowDuration(Duration.INDEFINITE);
	}

	@Override
	protected void updateItem(CellDataWrapper<String> item, boolean empty) {
		super.updateItem(item, empty);
		setText(null);

		if (empty || item == null) {
			setGraphic(null);
		} else {
			editingTextField.setText(item.getEditingValue());
			savedValueLabel.setText(item.getSavedValue());

			if (item.getRowStatus() == RowStatus.NEW || item.getRowStatus() == RowStatus.EDITING) {
				setErrorMessage(item.getEditingError());
				setGraphic(editingTextField);
			} else if (item.getRowStatus() == RowStatus.SAVED) {
				setErrorMessage(null);
				setGraphic(savedValueLabel);
			} else {
				setGraphic(null);
			}
		}
	}

	private void setErrorMessage(String editingError) {
		if (editingError == null) {
			editingTextField.pseudoClassStateChanged(ValidationUtils.errorClass, false);
			Tooltip.uninstall(editingTextField, errorTooltip);
		} else {
			editingTextField.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			errorTooltip.setText(editingError);
			Tooltip.install(editingTextField, errorTooltip);
		}
	}
}
