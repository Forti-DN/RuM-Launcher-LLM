package global;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import controller.common.InventoryController;
import controller.common.InventorySavedElementController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import util.AlertUtils;

/**
 * Inventory class used to store and display snapshots.
 *
 */
public class Inventory {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static InventorySavedElementController savedModelController;

	private static InventorySavedElementController savedEventLogController;

	private static InventorySavedElementController savedLTLModelController;

	private static InventoryController inventoryController;

	//Private constructor to avoid unnecessary instantiation of the class
	private Inventory() {
	}

	/**
	 * Creates and returns instance of saved model dialog
	 *
	 * @return  {@link InventorySavedElementController}
	 */
	public static InventorySavedElementController getSavedModelInstance() {
		if (Inventory.savedModelController == null) {

			try {
				Inventory.savedModelController = setupController("Saved process models", InventoryElementTypeEnum.PROCESS_MODEL);
			} catch (IOException e) {
				AlertUtils.showError("Error loading dialog for saved models!");
				logger.error("Can not load saved models dialog", e);
				return null;
			}

		}
		return Inventory.savedModelController;
	}

	/**
	 * Creates and returns instance of saved event log dialog
	 *
	 * @return  {@link InventorySavedElementController}
	 */
	public static InventorySavedElementController getSavedEventLogInstance() {
		if (Inventory.savedEventLogController == null) {

			try {
				Inventory.savedEventLogController = setupController("Saved event logs", InventoryElementTypeEnum.EVENT_LOG);
			} catch (IOException e) {
				AlertUtils.showError("Error loading dialog for saved event logs!");
				logger.error("Can not load saved event logs dialog", e);
				return null;
			}

		}
		return Inventory.savedEventLogController;
	}

	/**
	 * Creates and returns instance of saved LTL model dialog
	 *
	 * @return  {@link InventorySavedElementController}
	 */
	public static InventorySavedElementController getSavedLTLModelInstance() {
		if (Inventory.savedLTLModelController == null) {

			try {
				Inventory.savedLTLModelController = setupController("Saved LTL models", InventoryElementTypeEnum.LTL_MODEL);
			} catch (IOException e) {
				AlertUtils.showError("Error loading dialog for saved LTL models!");
				logger.error("Can not load saved LTL models dialog", e);
				return null;
			}

		}
		return Inventory.savedLTLModelController;
	}
	private static InventorySavedElementController setupController(String title, InventoryElementTypeEnum elementType) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(
				Inventory.class.getClassLoader().getResource("pages/common/InventorySavedElement.fxml"));

		Stage stage = new Stage();
		Region root = fxmlLoader.load();
		InventorySavedElementController savedElementController = (InventorySavedElementController) fxmlLoader.getController();
		savedElementController.setElementType(elementType);
		savedElementController.setAdditionalInformation();
		root.getStylesheets().add("main.css");
		stage.setHeight(390.0);
		stage.setWidth(500.0);
		stage.setScene(new Scene(root));
		stage.setTitle(title);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(null);
		savedElementController.setStage(stage);

		return savedElementController;
	}

	/**
	 * Creates and returns instance of inventory dialog
	 *
	 * @return  {@link InventoryController}
	 */
	public static InventoryController getInventoryInstance() {
		if (Inventory.inventoryController == null) {

			try {
				FXMLLoader fxmlLoader = new FXMLLoader(
						Inventory.class.getClassLoader().getResource("pages/common/Inventory.fxml"));

				Stage stage = new Stage();
				Region root = fxmlLoader.load();
				InventoryController inventoryController = (InventoryController) fxmlLoader.getController();
				root.getStylesheets().add("main.css");
				stage.setMinWidth(960);
				stage.setMinHeight(600);
				stage.setScene(new Scene(root));
				stage.setTitle("Inventory");
				stage.initModality(Modality.WINDOW_MODAL);
				stage.initOwner(null);
				inventoryController.setStage(stage);

				Inventory.inventoryController = inventoryController;

			} catch (IOException e) {
				AlertUtils.showError("Error loading inventory!");
				logger.error("Can not load inventory", e);
				return null;
			}

		}
		return Inventory.inventoryController;
	}

	/**
	 * Add snapshot of declare model to the inventory
	 *
	 * @param Snapshot added to the inventory as {@link InventorySavedElement}
	 */
	public static void storeSavedModelSnapshot(InventorySavedElement inventorySavedModel) {
		getSavedModelInstance().addFurtherElement(inventorySavedModel);
	}

	/**
	 * Get all stored snapshots of declare models
	 *
	 * @param Snapshot loaded from the saved declare models as {@link InventorySavedElement}
	 */
	public static void getSavedModelSnapshots() {
		getSavedModelInstance().getAvailableElements();
	}

	/**
	 * Add snapshot of declare model to the inventory
	 *
	 * @param Snapshot added to the inventory as {@link InventorySavedElement}
	 */
	public static void storeEventLogSnapshot(InventorySavedElement inventorySavedEventLog) {
		getSavedEventLogInstance().addFurtherElement(inventorySavedEventLog);
	}

	/**
	 * Get all stored snapshots of event logs
	 *
	 * @param Snapshot loaded from the saved event logs as {@link InventorySavedElement}
	 */
	public static void getEventLogSnapshots() {
		getSavedEventLogInstance().getAvailableElements();
	}

}
