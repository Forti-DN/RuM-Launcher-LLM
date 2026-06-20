package controller.common;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import global.Inventory;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.util.Callback;
import listview.EventLogCell;
import listview.ProcessModelCell;
import util.FileUtils;
import util.InventoryEvent;

public class InventoryController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	ListView<InventorySavedElement> eventLogList;
	@FXML
	ListView<InventorySavedElement> modelList;

	ReadOnlyObjectProperty<InventorySavedElement> selectedLogProperty;
	InventorySavedElement selectedLog;

	private EventHandler<InventoryEvent> eventHandler;

	@FXML
	public void initialize() {

		FileUtils.loadSavedElementsDataFromFile();

		eventHandler = new EventHandler<InventoryEvent>() {

			@Override
			public void handle(InventoryEvent event) {
				// Close Inventory window after button click
				closeStage(event);
			}

		};

		eventLogList.setCellFactory(new Callback<ListView<InventorySavedElement>, ListCell<InventorySavedElement>>() {
			@Override
			public ListCell<InventorySavedElement> call(ListView<InventorySavedElement> listView) {		
				EventLogCell eventLogCell = new EventLogCell(InventoryController.this.getStage());
				eventLogCell.addEventHandler(InventoryEvent.INVENTORY_EVENT_TYPE, eventHandler);
				return eventLogCell;
			}
		});

		modelList.setCellFactory(new Callback<ListView<InventorySavedElement>, ListCell<InventorySavedElement>>() {
			@Override
			public ListCell<InventorySavedElement> call(ListView<InventorySavedElement> listView) {
				ProcessModelCell processModelCell = new ProcessModelCell(InventoryController.this.getStage());
				processModelCell.addEventHandler(InventoryEvent.INVENTORY_EVENT_TYPE, eventHandler);
				return processModelCell;
			}
		});

		eventLogList.setItems(Inventory.getSavedEventLogInstance().getAvailableElements());
		modelList.setItems(Inventory.getSavedModelInstance().getAvailableElements());

		//Placeholder for lists
		eventLogList.setPlaceholder(new Label("Start with importing an event log"));
		modelList.setPlaceholder(new Label("Start with importing a process model"));
	}

	@FXML
	private void importModel() {
		File openFile = FileUtils.showModelOpenDialog(this.getStage());

		if (openFile != null) {
			logger.info("File selected: {}", openFile.getAbsolutePath());
			InventorySavedElement inventorySavedElement = new InventorySavedElement(openFile, new Date(), openFile.getName(), InventoryElementTypeEnum.PROCESS_MODEL);
			Inventory.getSavedModelInstance().addFurtherElement(inventorySavedElement);
		} else {
			logger.info("Fileselection canceled!");
		}
	}

	@FXML
	private void importEventLog() {
		File openFile = FileUtils.showLogOpenDialog(this.getStage());

		if (openFile != null) {
			logger.info("File selected: {}", openFile.getAbsolutePath());
			InventorySavedElement inventorySavedElement = new InventorySavedElement(openFile, new Date(), openFile.getName(), InventoryElementTypeEnum.EVENT_LOG);
			Inventory.getSavedEventLogInstance().addFurtherElement(inventorySavedElement);
		} else {
			logger.info("Fileselection canceled!");
		}
	}

	public ReadOnlyObjectProperty<InventorySavedElement> getSelectedLogProperty() {
		return selectedLogProperty;
	}	

	private void closeStage(Event event) {
		Node source = (Node) event.getSource();
		Stage stage = (Stage) source.getScene().getWindow();
		stage.close();
	}

}
