package datatable.cell;

import java.util.List;
import java.util.Objects;

import datatable.AbstractDataRow;
import datatable.AbstractDataRow.RowStatus;
import datatable.CellDataWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import javafx.util.StringConverter;
import util.ValidationUtils;

public class ComboBoxCell<T extends AbstractDataRow, D> extends TableCell<T , CellDataWrapper<D>> {

	private Label savedValueLabel = new Label();
	private ComboBox<D> editingComboBox = new ComboBox<D>();
	private Tooltip errorTooltip = new Tooltip();

	public ComboBoxCell(List<D> selectableItems, StringConverter<D> valueConverter) {
		editingComboBox.getItems().setAll(selectableItems);
		setupValueChoiceBox(valueConverter);
	}

	public ComboBoxCell(ObservableList<D> selectableItems, StringConverter<D> valueConverter) {
		//Handling cases when selected/saved item is removed form the list of selectable items
		selectableItems.addListener((ListChangeListener<D>)(change -> {
			if (ComboBoxCell.this.getItem() != null) {
				while (change.next()) {
					if (change.wasRemoved()) {
						for (D removedObject : change.getRemoved()) {
							if (Objects.equals(editingComboBox.getSelectionModel().getSelectedItem(), removedObject)) {
								editingComboBox.getSelectionModel().clearSelection();
							}
							if (Objects.equals(ComboBoxCell.this.getItem().getSavedValue(), removedObject)) {
								ComboBoxCell.this.getItem().invalidateSavedValue();
							}
							if (Objects.equals(ComboBoxCell.this.getItem().getEditingValue(), removedObject)) {
								ComboBoxCell.this.getItem().setEditingValue(null);
							}
						}
					} else if (change.wasUpdated() && change.getFrom() <= editingComboBox.getSelectionModel().getSelectedIndex() && change.getTo() > editingComboBox.getSelectionModel().getSelectedIndex()) {
						savedValueLabel.setText(editingComboBox.getConverter().toString(editingComboBox.getSelectionModel().getSelectedItem()));
					}
				}
			}
		}));

		editingComboBox.setItems(selectableItems);
		setupValueChoiceBox(valueConverter);
	}

	private void setupValueChoiceBox(StringConverter<D> valueConverter) {
		editingComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			setErrorMessage(null);
		});

		editingComboBox.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == false) {
				ComboBoxCell.this.getItem().setEditingValue(editingComboBox.getSelectionModel().getSelectedItem());
				ComboBoxCell.this.getItem().validateCellEdit();
			}
		});

		if (valueConverter != null) {
			editingComboBox.setConverter(valueConverter);
		}
		editingComboBox.setVisibleRowCount(35);

		editingComboBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		errorTooltip.setShowDelay(Duration.millis(200));
		errorTooltip.setShowDuration(Duration.INDEFINITE);

	}

	@Override
	protected void updateItem(CellDataWrapper<D> item, boolean empty) {
		super.updateItem(item, empty);
		setText(null);

		if (empty || item == null) {
			setGraphic(null);
		} else {

			if (item.getRowStatus() == RowStatus.NEW || item.getRowStatus() == RowStatus.EDITING) {
				editingComboBox.getSelectionModel().select(item.getEditingValue());
				savedValueLabel.setText(editingComboBox.getConverter().toString(item.getEditingValue()));
				setErrorMessage(item.getEditingError());
				setGraphic(editingComboBox);
			} else if (item.getRowStatus() == RowStatus.SAVED) {
				editingComboBox.getSelectionModel().select(item.getSavedValue());
				savedValueLabel.setText(editingComboBox.getConverter().toString(item.getSavedValue()));
				setErrorMessage(null);
				setGraphic(savedValueLabel);
			} else {
				setGraphic(null);
			}
		}
	}

	private void setErrorMessage(String editingError) {
		if (editingError == null) {
			editingComboBox.pseudoClassStateChanged(ValidationUtils.errorClass, false);
			Tooltip.uninstall(editingComboBox, errorTooltip);
		} else {
			editingComboBox.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			errorTooltip.setText(editingError);
			Tooltip.install(editingComboBox, errorTooltip);
		}
	}
}
