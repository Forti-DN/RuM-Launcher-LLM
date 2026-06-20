package controller.generation;

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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import util.AlertUtils;
import util.FileUtils;
import util.ModelUtils;

public class GenerationPageController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox rootRegion;
	@FXML
	private Label introLabel;
	@FXML
	private TabPane generationTabPane;

	@FXML
	private void initialize() {

		//Set info label to be visible only if no tabs are open
		generationTabPane.getTabs().addListener(new ListChangeListener<Tab>() {
			@Override
			public void onChanged(Change<? extends Tab> change) {
				while (change.next()) {
					if (generationTabPane.getTabs().isEmpty()) {
						generationTabPane.setVisible(false);
						introLabel.setVisible(true);
						introLabel.setManaged(true);
					} else {
						generationTabPane.setVisible(true);
						introLabel.setVisible(false);
						introLabel.setManaged(false);
					}
				}
			}
		});

		logger.debug("Generation page initialized");
	}

	@FXML
	public void openModel() {
		InventorySavedElement declModel = FileUtils.showSavedElementDialog(InventoryElementTypeEnum.PROCESS_MODEL);
		openModelInTab(declModel);
	}

	public void openModelFromInventory(InventorySavedElement declModel) {
		openModelInTab(declModel);
	}
	
	private void openModelInTab(InventorySavedElement declModel) {
		if (declModel != null) {
			logger.info("Opened model in generation page: {}", declModel.getFile().getAbsolutePath());
			try {
				//TODO: Should get the path from constants file or configuration file or enum
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/generation/GenerationTab.fxml"));
				Node rootPane = loader.load();
				GenerationTabController generationTabController = loader.getController();
				generationTabController.setStage(this.getStage());
				List<String> constraintList = ModelUtils.getConstraintsList(declModel.getFile());
				generationTabController.setModelData(declModel.getFile(), constraintList);

				Tab tab = new Tab();
				tab.setContent(rootPane);
				tab.setText(declModel.getSaveName());
				generationTabPane.getTabs().add(tab);
				generationTabPane.getSelectionModel().select(tab);
			} catch (IOException | IllegalStateException e) {
				AlertUtils.showError("Error opening Log Generation tab!");
				logger.error("Can not load generation tab", e);
			}
		}
	}

}
