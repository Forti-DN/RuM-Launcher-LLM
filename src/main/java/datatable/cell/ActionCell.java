package datatable.cell;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import datatable.AbstractDataRow;
import datatable.AbstractDataRow.RowStatus;
import datatable.CellDataWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;

public class ActionCell<T extends AbstractDataRow> extends TableCell<T, RowStatus> {

	//Most of logging commented out because cells can be updated very often and at arbitrary times
	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private HBox rootRegion;
	@FXML
	private Button firstButton;
	@FXML
	private Button secondButton;
	@FXML
	private FontIcon editFontIcon;
	@FXML
	private FontIcon deleteFontIcon;

	private FontIcon confirmFontIcon;
	private FontIcon cancelFontIcon;
	private FXMLLoader loader;

	private TableColumn<T, ? extends CellDataWrapper<? extends Object>>[] uniqueColumns;
	private String uniquenessError;

	@SafeVarargs
	public ActionCell(TableColumn<T, ? extends CellDataWrapper<? extends Object>>... uniqueColumns ) {
		this.uniqueColumns = uniqueColumns;

		if (uniqueColumns != null && uniqueColumns.length != 0) {
			if (uniqueColumns.length == 1) {
				uniquenessError = uniqueColumns[0].getText() + " must be unique";
			} else {
				List<String> uniqueColumnNames = new ArrayList<>();
				for (TableColumn<T, ? extends CellDataWrapper<? extends Object>> uniqeColumn : uniqueColumns) {
					uniqueColumnNames.add(uniqeColumn.getText());
				}
				uniquenessError = "Combination of " + String.join(", ", uniqueColumnNames.subList(0, uniqueColumnNames.size()-1)) + " and " + uniqueColumnNames.get(uniqueColumnNames.size()-1) + " must be unique";
			}
		}

		//Creating icons not defined in fxml
		confirmFontIcon = new FontIcon("fa-check");
		confirmFontIcon.getStyleClass().add("action-cell__confirm-icon");
		cancelFontIcon = new FontIcon("fa-close");
		cancelFontIcon.getStyleClass().add("action-cell__cancel-icon");
	}

	@Override
	protected void updateItem(RowStatus item, boolean empty) {
		super.updateItem(item, empty);
		setText(null);

		if (empty || item == null) {
			setGraphic(null);
		} else if(loadFxml()) {
			//logger.debug("Updating action cell for status: {}", item);
			if (item == RowStatus.NEW) {
				firstButton.setGraphic(confirmFontIcon);
				firstButton.setOnAction(event -> confirmRowEdit());
				secondButton.setGraphic(deleteFontIcon);
				secondButton.setOnAction(event -> deleteRow());
				setGraphic(rootRegion);
			} else if (item == RowStatus.EDITING) {
				firstButton.setGraphic(confirmFontIcon);
				firstButton.setOnAction(event -> confirmRowEdit());
				secondButton.setGraphic(cancelFontIcon);
				secondButton.setOnAction(event -> cancelRowEdit());
				setGraphic(rootRegion);
			} else if (item == RowStatus.SAVED) {
				firstButton.setGraphic(editFontIcon);
				firstButton.setOnAction(event -> startRowEdit());
				secondButton.setGraphic(deleteFontIcon);
				secondButton.setOnAction(event -> deleteRow());
				setGraphic(rootRegion);
			} else {
				setGraphic(null);
			}
			//logger.debug("Updated action cell for status: {}", item);
		}
	}

	private void confirmRowEdit() {
		if (this.getTableRow().getItem().validateRowEdit() && validateUniqueColumns()) {
			this.getTableRow().getItem().confirmRowEdit();
			this.getTableView().requestFocus();
			this.getTableView().getSelectionModel().select(getIndex());
		}
	}

	private void cancelRowEdit() {
		this.getTableRow().getItem().cancelRowEdit();
		this.getTableView().requestFocus();
	}

	private void startRowEdit() {
		this.getTableRow().getItem().startRowEdit();
		this.getTableView().requestFocus();
		this.getTableView().getSelectionModel().select(getIndex());
	}

	private void deleteRow() {
		this.getTableView().getItems().remove(getIndex());
		this.getTableView().requestFocus();
	}

	private boolean validateUniqueColumns() {
		if (uniqueColumns == null || uniqueColumns.length == 0) {
			return true;
		} else {
			boolean isValid = true;
			for (int rowIndex = 0; rowIndex < getTableView().getItems().size(); rowIndex++) {
				AbstractDataRow row = getTableView().getItems().get(rowIndex);
				if (rowIndex != getIndex() && row.getRowStatus() != RowStatus.NEW) {
					for (TableColumn<T, ? extends CellDataWrapper<? extends Object>> uniqueColumn : uniqueColumns) {
						if (Objects.equals(uniqueColumn.getCellData(getIndex()).getEditingValue(), uniqueColumn.getCellData(rowIndex).getSavedValue())) {
							isValid = false;
						} else {
							isValid = true;
							break;
						}
					}

					if (!isValid) {
						for (TableColumn<T, ? extends CellDataWrapper<? extends Object>> uniqueColumn : uniqueColumns) {
							uniqueColumn.getCellData(getIndex()).setEditingError(uniquenessError);
						}
						break;
					}
				}
			}
			return isValid;
		}
	}

	private boolean loadFxml() {
		if (loader == null) {
			//Load cell contents if not already loaded
			loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/datatable/cell/ActionCell.fxml"));
			loader.setController(this);
			try {
				loader.load();
				return true;
			} catch (IOException | IllegalStateException e) {
				logger.error("Can not load action cell", e);
				return false;
			}
		} else {
			return true;
		}
	}
}
