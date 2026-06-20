package controller.monitoring;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

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
import util.ModelUtils;

public class MonitoringPageController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox rootRegion;
	@FXML
	private Label introLabel;
	@FXML
	private TabPane monitoringTabPane;

	@FXML
	private void initialize() {

		//Set info label to be visible only if no tabs are open
		monitoringTabPane.getTabs().addListener(new ListChangeListener<Tab>() {
			@Override
			public void onChanged(Change<? extends Tab> change) {
				while (change.next()) {
					if (monitoringTabPane.getTabs().isEmpty()) {
						monitoringTabPane.setVisible(false);
						introLabel.setVisible(true);
						introLabel.setManaged(true);
					} else {
						monitoringTabPane.setVisible(true);
						introLabel.setVisible(false);
						introLabel.setManaged(false);
					}
				}
			}
		});

		logger.debug("Monitoring page initialized");
	}

	@FXML
	private void openModel() {
		InventorySavedElement declModel = FileUtils.showSavedElementDialog(InventoryElementTypeEnum.PROCESS_MODEL);
		openModelInTab(declModel);
	}

	public void openModelFromInventory(InventorySavedElement declModel) {
		openModelInTab(declModel);
	}

	private void openModelInTab(InventorySavedElement declModel) {
		if (declModel != null) {
			logger.info("Opened model in discovery page: {}", declModel.getFile().getAbsolutePath());
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/monitoring/MonitoringTab.fxml"));
				Region rootPane = loader.load(); //There seems to be a bug in JavaFX framework that causes IllegalStateException to be thrown instead of IOException
				MonitoringTabController monitoringTabController = loader.getController();
				monitoringTabController.setStage(this.getStage());
				List<String> constraintList = ModelUtils.getConstraintsList(declModel.getFile());
				monitoringTabController.setModelData(declModel.getFile(), constraintList);

				Tab tab = new Tab();
				tab.setContent(rootPane);
				tab.setText(declModel.getSaveName());
				monitoringTabPane.getTabs().add(tab);
				monitoringTabPane.getSelectionModel().select(tab);
			} catch (IOException | IllegalStateException e) {
				AlertUtils.showError("Error opening Monitoring tab!");
				logger.error("Can not load monitoring tab", e);
			}
		}
	}
}