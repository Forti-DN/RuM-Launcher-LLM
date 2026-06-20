package controller.editor;

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

public class EditorPageController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final String editorTabFXMLpath = "pages/editor/EditorTab.fxml";

	@FXML
	private VBox rootRegion;
	@FXML
	private Label introLabel;
	@FXML
	private TabPane editorTabPane;

	@FXML
	private void initialize() {

		//Set info label to be visible only if no tabs are open
		editorTabPane.getTabs().addListener(new ListChangeListener<Tab>() {
			@Override
			public void onChanged(Change<? extends Tab> change) {
				while (change.next()) {
					if (editorTabPane.getTabs().isEmpty()) {
						editorTabPane.setVisible(false);
						introLabel.setVisible(true);
						introLabel.setManaged(true);
					} else {
						editorTabPane.setVisible(true);
						introLabel.setVisible(false);
						introLabel.setManaged(false);
					}
				}
			}
		});

		logger.debug("Editor page initialized");
	}

	@FXML
	private void openModel() {
		InventorySavedElement declModel = FileUtils.showSavedElementDialog(InventoryElementTypeEnum.PROCESS_MODEL);
		openModelInTab(declModel);
	}

	@FXML
	private void newModel() {
		try {
			logger.info("Opened a new model in editor page");
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(editorTabFXMLpath));
			Region rootPane = loader.load(); //There seems to be a bug in JavaFX framework that causes IllegalStateException to be thrown instead of IOException
			EditorTabController editorTabController = loader.getController();
			editorTabController.setStage(this.getStage());

			Tab tab = new Tab();
			tab.setContent(rootPane);
			tab.setText("-New model-");
			editorTabController.setTab(tab);
			editorTabPane.getTabs().add(tab);
			editorTabPane.getSelectionModel().select(tab);
		} catch (IOException | IllegalStateException e) {
			AlertUtils.showError("Error opening editor tab!");
			logger.error("Can not load editor tab", e);
		}
	}

	public void openModelFromInventory(InventorySavedElement declModel) {
		openModelInTab(declModel);
	}
	
	private void openModelInTab(InventorySavedElement declModel) {
		if (declModel != null) {
			logger.info("Opened model in editor page: {}", declModel.getFile().getAbsolutePath());
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(editorTabFXMLpath));
				Region rootPane = loader.load(); //There seems to be a bug in JavaFX framework that causes IllegalStateException to be thrown instead of IOException
				EditorTabController editorTabController = loader.getController();
				editorTabController.setStage(this.getStage());
				editorTabController.setModelData(declModel.getFile());

				Tab tab = new Tab();
				tab.setContent(rootPane);
				tab.setText(declModel.getSaveName());
				editorTabController.setTab(tab);
				editorTabPane.getTabs().add(tab);
				editorTabPane.getSelectionModel().select(tab);
			} catch (IOException | IllegalStateException e) {
				AlertUtils.showError("Error opening MP-Declare Editor tab!");
				logger.error("Can not load editor tab", e);
			}
		}
	}
}