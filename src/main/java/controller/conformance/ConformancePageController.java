package controller.conformance;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.TreeSet;

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

public class ConformancePageController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox rootRegion;
	@FXML
	private Label introLabel;
	@FXML
	private TabPane conformanceTabPane;

	@FXML
	private void initialize() {

		//Set info label to be visible only if no tabs are open
		conformanceTabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
			while (change.next()) {
				boolean isTabPaneEmpty = conformanceTabPane.getTabs().isEmpty();
				conformanceTabPane.setVisible(!isTabPaneEmpty);
				introLabel.setVisible(isTabPaneEmpty);
				introLabel.setManaged(isTabPaneEmpty);
			}
		});

		logger.debug("Conformance page initialized");
	}

	@FXML
	private void openModel() {
		InventorySavedElement declModel = FileUtils.showSavedElementDialog(InventoryElementTypeEnum.PROCESS_MODEL);
		openModelInTab(declModel);
	}
	
	public void openModelFromInventory(InventorySavedElement declModel) {
		openModelInTab(declModel);
	}

	// MARCUS - siin tehakse decl to xml teisendus ära
	private void openModelInTab(InventorySavedElement declModel) {
		if (declModel != null) {
			logger.info("Opened model in conformance page: {}", declModel.getSaveName());
			List<String> constraintList = ModelUtils.getConstraintsList(declModel.getFile());
			TreeSet<String> attributes = ModelUtils.getAttributes(declModel.getFile());
			File xmlModel = null;
			try {
				xmlModel = ModelUtils.createTmpXmlModel(declModel.getFile());
			} catch (IOException | IllegalArgumentException | IllegalStateException e) {
				//TODO: Check why IllegalStateException can be thrown with some models, could be a bug
				AlertUtils.showError("Error loading the model!");
				logger.error("Can not transform model to xml format: {}", declModel.getFile().getAbsolutePath(), e);
			}

			if (xmlModel != null) {
				try {
					//TODO: Should get the path from constants file or configuration file or enum
					FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/conformance/ConformanceTab.fxml"));
					Node rootPane = loader.load();
					ConformanceTabController conformanceTabController = loader.getController();
					conformanceTabController.setStage(this.getStage());
					conformanceTabController.setModelData(xmlModel, constraintList, attributes);

					Tab tab = new Tab();
					tab.setContent(rootPane);
					tab.setText(declModel.getSaveName());
					conformanceTabPane.getTabs().add(tab);
					conformanceTabPane.getSelectionModel().select(tab);
				} catch (IOException | IllegalStateException e) {
					AlertUtils.showError("Error opening Conformance tab!");
					logger.error("Can not load conformance tab", e);
				}
			}
		}
	}
}
