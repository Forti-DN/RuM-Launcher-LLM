package listview;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.io.Files;

import global.Inventory;
import global.InventorySavedElement;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import util.AlertUtils;
import util.FileUtils;
import util.InventoryEvent;
import util.PageType;

public class ProcessModelCell extends ListCell<InventorySavedElement> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox rootRegion;
	@FXML
	private HBox actionRegion;
	@FXML
	private Region spacer;
	@FXML
	private Label logName;
	@FXML
	private Label logDate;
	@FXML
	private Button conformanceButton;
	@FXML
	private Button logButton;
	@FXML
	private Button editorButton;
	@FXML
	private Button monitorButton;
	@FXML
	private Button exportButton;
	@FXML
	private Button deleteButton;

	private Stage stage;
	private FXMLLoader loader;

	public ProcessModelCell(Stage stage) {
		super();
		this.stage = stage;
	}

	@FXML
	public void initialize() {
		// Space between icons
		HBox.setHgrow(spacer, Priority.ALWAYS);

		// Open inventory element in conformance area
		conformanceButton.setOnAction(value ->
			conformanceButton.fireEvent(new InventoryEvent(PageType.CONFORMANCE, Inventory.getSavedModelInstance().getAvailableElements().get(getIndex())))
		);
		conformanceButton.setTooltip(new Tooltip("Open process model in conformance checking area"));

		// Open inventory element in generation area
		logButton.setOnAction(value ->
			logButton.fireEvent(new InventoryEvent(PageType.GENERATION, Inventory.getSavedModelInstance().getAvailableElements().get(getIndex())))
		);
		logButton.setTooltip(new Tooltip("Open process model in log generation area"));

		// Open inventory element in editor area
		editorButton.setOnAction(value ->
			editorButton.fireEvent(new InventoryEvent(PageType.EDITOR, Inventory.getSavedModelInstance().getAvailableElements().get(getIndex())))
		);
		editorButton.setTooltip(new Tooltip("Open process model in MP-declare editor area"));

		// Open inventory element in monitor area
		monitorButton.setOnAction(value ->
			monitorButton.fireEvent(new InventoryEvent(PageType.MONITORING, Inventory.getSavedModelInstance().getAvailableElements().get(getIndex())))
		);
		monitorButton.setTooltip(new Tooltip("Open process model in Monitoring area"));

		// Export event log
		exportButton.setOnAction((ActionEvent event) -> {

			InventorySavedElement exportElement = Inventory.getSavedModelInstance().getAvailableElements().get(getIndex());

			File chosenFile = FileUtils.showDeclSaveDialog(stage, null);

			//chosenFile might be null, because the user can just close the fileChooser instead of choosing a file
			if(chosenFile != null) {
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());

				try {
					Files.copy(exportElement.getFile(), chosenFile);
					logger.info("Model: {} exported to file: {}", exportElement.getFile(), chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Model successfully exported");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the model failed!");
					logger.error("Unable to export model: {}", chosenFile.getAbsolutePath(), e);
				}
			}
		});
		exportButton.setTooltip(new Tooltip("Export process model"));

		deleteButton.setOnAction((ActionEvent event) -> {
			// delete element from the list
			Inventory.getSavedModelInstance().getAvailableElements().remove(getIndex());
		});
	}

	@Override
	protected void updateItem(InventorySavedElement item, boolean empty) {
		super.updateItem(item, empty);
		setText(null);

		if (empty || item == null) {
			setGraphic(null);
		} else if(loadFxml()) {
			logName.setText(item.getSaveName());
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			logDate.setText(dateFormat.format(item.getSaveTimestamp()));
			setGraphic(rootRegion);
		}
	}


	private boolean loadFxml() {
		if (loader == null) {
			//Load cell contents if not already loaded
			loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/listview/ProcessModelCell.fxml"));
			loader.setController(this);
			try {
				loader.load();
				return true;
			} catch (IOException | IllegalStateException e) {
				logger.error("Can not load process model cell", e);
				return false;
			}
		} else {
			return true;
		}
	}

}
