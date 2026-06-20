package controller.common;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import global.Inventory;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import util.FileUtils;

public class InventorySavedElementController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final ObservableList<InventorySavedElement> availableElements = FXCollections.observableArrayList();

	private boolean pressedOkay = false;

	@FXML
	TableView<InventorySavedElement> elementTable;
	@FXML
	TableColumn<InventorySavedElement, Date> timestampColumn;
	@FXML
	TableColumn<InventorySavedElement, String> elementnameColumn;
	@FXML
	TableColumn<InventorySavedElement, Void> actionColumn;
	@FXML
	Button okayButton;
	@FXML
	Label dialogHeaderLabel;

	private InventorySavedElement selectedRow;

	private InventoryElementTypeEnum elementType;

	@FXML
	public void initialize() {

		okayButton.setDisable(true);

		// define date-column and set format
		timestampColumn.setCellValueFactory(new PropertyValueFactory<InventorySavedElement, Date>("saveTimestamp"));

		timestampColumn.setCellFactory(column -> {
			TableCell<InventorySavedElement, Date> cell = new TableCell<InventorySavedElement, Date>() {
				private SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

				@Override
				protected void updateItem(Date item, boolean empty) {
					super.updateItem(item, empty);
					if (empty) {
						setText(null);
					} else {
						if (item != null)
							this.setText(format.format(item));
					}
				}
			};

			return cell;
		});

		// define element table
		elementnameColumn.setCellValueFactory(new PropertyValueFactory<InventorySavedElement, String>("saveName"));
		elementnameColumn.setCellFactory(column -> {
			TableCell<InventorySavedElement, String> cell = new TableCell<InventorySavedElement, String>() {

				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);

					if (empty) {
						setText(null);
					} else {
						if (item != null) {
							File f = new File(item);
							this.setText(f.getName());
						}
					}
				}
			};

			return cell;
		});

		actionColumn.getStyleClass().add("action-cell");

		// define action column
		actionColumn.setCellFactory(column -> {
			TableCell<InventorySavedElement, Void> cell = new TableCell<InventorySavedElement, Void>() {

				private final Button actionBtn = new Button("");

				{
					FontIcon fontIcon = new FontIcon("fa-trash");
					fontIcon.getStyleClass().add("action-cell__delete-icon");
					actionBtn.setGraphic(fontIcon);
					actionBtn.getStyleClass().add("action-cell__button");
					actionBtn.setOnAction((ActionEvent event) -> {
						// if element is selected then deselect it
						if (selectedRow == getTableView().getItems().get(getIndex())) {
							resetSelectedRow();
						}
						// delete element from the list
						availableElements.remove(getTableView().getItems().get(getIndex()));
					});
				}

				@Override
				protected void updateItem(Void item, boolean empty) {
					super.updateItem(item, empty);

					if (empty) {
						setGraphic(null);
					} else {
						setGraphic(actionBtn);
					}
				}
			};

			return cell;
		});

		elementTable.setRowFactory(tv -> {
			TableRow<InventorySavedElement> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
					selectRow(row.getItem());
					if (event.getClickCount() == 2) {
						pressedOkay = true;
						closeStage(event);
					} else {
						okayButton.setDisable(false);
					}
				}
			});
			return row;
		});

		elementTable.setItems(availableElements);

	}

	public void setAdditionalInformation() {
		if(InventoryElementTypeEnum.PROCESS_MODEL.equals(elementType)) {
			elementTable.setPlaceholder(new Label("Start with importing a process model"));
			dialogHeaderLabel.setText("Saved process models");
		} else if(InventoryElementTypeEnum.LTL_MODEL.equals(elementType)) {
			elementTable.setPlaceholder(new Label("Start with importing a LTL model"));
			dialogHeaderLabel.setText("Saved LTL models");
		}else {
			elementTable.setPlaceholder(new Label("Start with importing an event log"));
			dialogHeaderLabel.setText("Saved event logs");
		}

	}

	public void pressOk(ActionEvent event) {
		pressedOkay = true;
		closeStage(event);
	}

	public void pressCancel(ActionEvent event) {
		closeStage(event);
	}

	public void pressImport(ActionEvent event) {

		File openFile;

		if(InventoryElementTypeEnum.PROCESS_MODEL.equals(elementType)) {
			openFile = FileUtils.showModelOpenDialog(this.getStage());

			if (openFile != null) {
				logger.info("File selected: {}", openFile.getAbsolutePath());
				InventorySavedElement inventorySavedElement = new InventorySavedElement(openFile, new Date(), openFile.getName(), InventoryElementTypeEnum.PROCESS_MODEL);
				Inventory.getSavedModelInstance().addFurtherElement(inventorySavedElement);
				elementTable.getSelectionModel().select(inventorySavedElement);
				okayButton.setDisable(false);
				this.selectedRow = inventorySavedElement;
			} else {
				logger.info("Fileselection canceled!");
			}

		} else if(InventoryElementTypeEnum.LTL_MODEL.equals(elementType)) {
			openFile = FileUtils.showLTLModelOpenDialog(this.getStage());

			if (openFile != null) {
				logger.info("File selected: " + openFile.getAbsolutePath());
				InventorySavedElement inventorySavedElement = new InventorySavedElement(openFile, new Date(), openFile.getName(), InventoryElementTypeEnum.LTL_MODEL);
				Inventory.getSavedLTLModelInstance().addFurtherElement(inventorySavedElement);
				elementTable.getSelectionModel().select(inventorySavedElement);
				okayButton.setDisable(false);
				this.selectedRow = inventorySavedElement;
			} else {
				logger.info("Fileselection canceled!");
			}
		} else {
			openFile = FileUtils.showLogOpenDialog(this.getStage());

			if (openFile != null) {
				logger.info("File selected: {}", openFile.getAbsolutePath());
				InventorySavedElement inventorySavedElement = new InventorySavedElement(openFile, new Date(), openFile.getName(), InventoryElementTypeEnum.EVENT_LOG);
				Inventory.getSavedEventLogInstance().addFurtherElement(inventorySavedElement);
				elementTable.getSelectionModel().select(inventorySavedElement);
				okayButton.setDisable(false);
				this.selectedRow = inventorySavedElement;
			} else {
				logger.info("Fileselection canceled!");
			}
		}
	}

	public void selectRow(InventorySavedElement row) {
		this.selectedRow = row;
	}

	public InventorySavedElement getSelectedRow() {
		if (pressedOkay) {
			return selectedRow;
		} else {
			return null;
		}
	}

	public void resetSelectedRow() {
		pressedOkay = false;
		selectedRow = null;
		okayButton.setDisable(true);
		elementTable.getSelectionModel().clearSelection();
	}

	private void closeStage(Event event) {
		Node source = (Node) event.getSource();
		Stage stage = (Stage) source.getScene().getWindow();
		stage.close();
	}

	public void addFurtherElement(InventorySavedElement inventorySavedElement) {
		availableElements.add(inventorySavedElement);
	}

	public InventoryElementTypeEnum getElementType() {
		return elementType;
	}

	public void setElementType(InventoryElementTypeEnum elementType) {
		this.elementType = elementType;
	}

	public ObservableList<InventorySavedElement> getAvailableElements() {
		return availableElements;
	}

}
