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

public class EventLogCell extends ListCell<InventorySavedElement> {

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
	private Button discoveryButton;
	@FXML
	private Button ltlCheckerButton;
	@FXML
	private Button exportButton;
	@FXML
	private Button deleteButton;

	private Stage stage;
	private FXMLLoader loader;

	public EventLogCell(Stage stage) {
		super();
		this.stage = stage;
	}

	@FXML
	public void initialize() {
		// Space between icons
		HBox.setHgrow(spacer, Priority.ALWAYS);

		deleteButton.setOnAction((ActionEvent event) -> {
			// delete element from the list
			Inventory.getSavedEventLogInstance().getAvailableElements().remove(getIndex());
		});

		// Open inventory element in discovery area
		discoveryButton.setOnAction(value -> {
			discoveryButton.fireEvent(new InventoryEvent(PageType.DISCOVERY, Inventory.getSavedEventLogInstance().getAvailableElements().get(getIndex())));
		});
		discoveryButton.setTooltip(new Tooltip("Open event log in discovery area"));

		// Open inventory element in LTL Checker area
		ltlCheckerButton.setOnAction(value -> {
			ltlCheckerButton.fireEvent(new InventoryEvent(PageType.FILTERING, Inventory.getSavedEventLogInstance().getAvailableElements().get(getIndex())));
		});
		ltlCheckerButton.setTooltip(new Tooltip("Open event log in LTL Checker area"));

		// Export event log
		exportButton.setOnAction((ActionEvent event) -> {

			InventorySavedElement exportElement = Inventory.getSavedEventLogInstance().getAvailableElements().get(getIndex());

			File chosenFile = FileUtils.showXesSaveDialog(stage, null);

			//chosenFile might be null, because the user can just close the fileChooser instead of choosing a file
			if(chosenFile != null) {
				logger.debug("Exporting temp. log to file: {}", chosenFile.getAbsolutePath());

				try {
					Files.copy(exportElement.getFile(), chosenFile);
					logger.info("Log {} exported to file: {}", exportElement.getFile(), chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Log successfully exported");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the log failed!");
					logger.error("Unable to export log: {}", chosenFile.getAbsolutePath(), e);
				}
			}
		});
		exportButton.setTooltip(new Tooltip("Export event log"));

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
			loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/listview/EventLogCell.fxml"));
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
