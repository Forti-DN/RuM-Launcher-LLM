package datatable.cell;

import java.math.BigDecimal;
import java.util.function.UnaryOperator;

import datatable.AbstractDataRow;
import datatable.AbstractDataRow.RowStatus;
import datatable.CellDataWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import javafx.util.StringConverter;
import util.ValidationUtils;

public class PositiveDecimalCell<T extends AbstractDataRow> extends TableCell<T , CellDataWrapper<BigDecimal>> {

	private Label savedValueLabel = new Label();
	private TextField editingTextField = new TextField();
	private TextFormatter<BigDecimal> valueFormatter;
	private Tooltip errorTooltip = new Tooltip();

	public PositiveDecimalCell() {
		StringConverter<BigDecimal> converter = new StringConverter<BigDecimal>() {
			@Override
			public String toString(BigDecimal object) {
				return object != null ? object.toString() : null;
			}
			@Override
			public BigDecimal fromString(String string) {
				return (string != null && !string.isBlank()) ? new BigDecimal(string) : null;
			}
		};
		UnaryOperator<Change> filter = new UnaryOperator<TextFormatter.Change>() {
			@Override
			public Change apply(Change change) {
				if (change.getControlNewText().matches("[.][0-9]{0,6}|[0-9]{0,6}|[0-9]{1,6}[.]|[0-9]{1,6}[.][0-9]{0,6}")) {
					return change;
				} else {
					PositiveDecimalCell.this.getItem().setEditingError("Only decimal inputs allowed");
					return null;
				}
			}
		};
		valueFormatter = new TextFormatter<BigDecimal>(converter, null, filter);
		editingTextField.setTextFormatter(valueFormatter);

		editingTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && !newValue.isBlank()) {
				setErrorMessage(null);
			}
		});

		editingTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == false) {
				PositiveDecimalCell.this.getItem().setEditingValue(valueFormatter.getValue());
				PositiveDecimalCell.this.getItem().validateCellEdit();
			}
		});

		editingTextField.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		errorTooltip.setShowDelay(Duration.millis(200));
		errorTooltip.setShowDuration(Duration.INDEFINITE);
	}


	@Override
	protected void updateItem(CellDataWrapper<BigDecimal> item, boolean empty) {
		super.updateItem(item, empty);
		setText(null);

		if (empty || item == null) {
			setGraphic(null);
		} else {
			valueFormatter.setValue(item.getEditingValue()); //Sets editingTextField trough the corresponding converter
			savedValueLabel.setText(valueFormatter.getValueConverter().toString(item.getSavedValue()));

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