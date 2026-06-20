package controller.discovery;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import controller.common.AbstractController;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import util.AlertUtils;
import util.FileUtils;

public class DiscoveryPageController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox rootRegion;
	@FXML
	private Label introLabel;
	@FXML
	private TabPane discoveryTabPane;

	@FXML
	private void initialize() {

		//Set info label to be visible only if no tabs are open
		discoveryTabPane.getTabs().addListener(new ListChangeListener<Tab>() {
			@Override
			public void onChanged(Change<? extends Tab> change) {
				while (change.next()) {
					if (discoveryTabPane.getTabs().isEmpty()) {
						discoveryTabPane.setVisible(false);
						introLabel.setVisible(true);
						introLabel.setManaged(true);
					} else {
						discoveryTabPane.setVisible(true);
						introLabel.setVisible(false);
						introLabel.setManaged(false);
					}
				}
			}
		});

		logger.debug("Discovery page initialized");
	}

	@FXML
	private void openLog() {
		InventorySavedElement eventLog = FileUtils.showSavedElementDialog(InventoryElementTypeEnum.EVENT_LOG);
		openLogInTab(eventLog);
	}
	
	public void openLogFromInventory(InventorySavedElement eventLog) {
		openLogInTab(eventLog);
	}
	
	private void openLogInTab(InventorySavedElement eventLog) {
		if (eventLog != null) {
			logger.info("Opened event log in discovery page: {}", eventLog.getFile().getAbsolutePath());
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/discovery/DiscoveryTab.fxml"));
				Region rootPane = loader.load(); //There seems to be a bug in JavaFX framework that causes IllegalStateException to be thrown instead of IOException
				DiscoveryTabController discoveryTabController = loader.getController();
				discoveryTabController.setStage(this.getStage());
				discoveryTabController.setLogData(eventLog.getFile());

				Tab tab = new Tab();
				tab.setContent(rootPane);
				tab.setText(eventLog.getFile().getName());
				discoveryTabPane.getTabs().add(tab);
				discoveryTabPane.getSelectionModel().select(tab);
			} catch (IOException | IllegalStateException e) {
				AlertUtils.showError("Error opening Discovery tab!");
				logger.error("Can not load discovery tab", e);
			}
		}
	}
}
