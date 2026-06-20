package controller.common;

import java.lang.invoke.MethodHandles;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import global.Inventory;
import global.InventorySavedElement;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.VBox;
import util.InventoryEvent;
import util.PageType;

public class NavigationSidebarController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final PseudoClass highlightedClass = PseudoClass.getPseudoClass("highlighted");

	@FXML
	private VBox rootRegion;
	@FXML
	private Button discoveryButton;
	@FXML
	private Button conformanceButton;
	@FXML
	private Button generationButton;
	@FXML
	private Button editorButton;
	@FXML
	private Button monitoringButton;
	@FXML
	public Button filteringButton;
	@FXML
	private FontIcon minimizeIcon;

	private boolean isMinimized;
	private Button currentlyHighlighted;

	private Consumer<PageType> navigationCallback;

	private BiConsumer<PageType, InventorySavedElement> navigationWithInventoryElementCallback;

	@FXML
	private void initialize() {

		EventHandler<InventoryEvent> eventHandler = (event ->
			// open corresponding area with saved element
			handleNavigationWithSavedElement(event.getPageType(), event.getSavedElement())
		);

		Inventory.getInventoryInstance().getStage().addEventFilter(InventoryEvent.INVENTORY_EVENT_TYPE, eventHandler);

		logger.debug("Navigation sidebar initialized");
	}

	@FXML
	private void openStartPage() {
		handleNavigation(PageType.START);
	}

	@FXML
	private void openDiscovery() {
		handleNavigation(PageType.DISCOVERY);
	}

	@FXML
	private void openConformance() {
		handleNavigation(PageType.CONFORMANCE);
	}

	@FXML
	private void openGeneration() {
		handleNavigation(PageType.GENERATION);
	}

	@FXML
	private void openEditor() {
		handleNavigation(PageType.EDITOR);
	}

	@FXML
	private void openMonitoring() {
		handleNavigation(PageType.MONITORING);
	}

	@FXML
	private void openFiltering() {
		handleNavigation(PageType.FILTERING);
	}

	@FXML
	private void openInventory() {
		Inventory.getInventoryInstance().getStage().showAndWait();
	}

	@FXML
	private void toggleMinimize() {
		if (isMinimized) {
			for (Node node : rootRegion.getChildren()) {
				if (node instanceof Button) {
					((Button) node).setContentDisplay(ContentDisplay.LEFT);
				}
			}
			minimizeIcon.setIconLiteral("fa-angle-double-left");
			isMinimized = false;
			logger.debug("Navigation sidebar maximized");
		} else {
			for (Node node : rootRegion.getChildren()) {
				if (node instanceof Button) {
					((Button) node).setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				}
			}
			minimizeIcon.setIconLiteral("fa-angle-double-right");
			isMinimized = true;
			logger.debug("Navigation sidebar minimized");
		}
	}

	private void handleNavigation(PageType pageType) {
		if (navigationCallback != null) {
			// Notification that this class has been used for navigation
			navigationCallback.accept(pageType);
		}
		updateHighlight(pageType);
	}

	private void handleNavigationWithSavedElement(PageType pageType, InventorySavedElement savedElement) {
		if (navigationWithInventoryElementCallback != null) {
			// Notification that this class has been used for navigation
			navigationWithInventoryElementCallback.accept(pageType, savedElement);
		}
		updateHighlight(pageType);
	}

	// Made public so that highlights can be updated even if navigation happens
	// outside this class
	public void updateHighlight(PageType pageType) {
		if (currentlyHighlighted != null) {
			currentlyHighlighted.pseudoClassStateChanged(highlightedClass, false);
		}
		switch (pageType) {
		case START:
			// Start (RuM logo and title) does not get highlighted
			currentlyHighlighted = null;
			break;
		case DISCOVERY:
			discoveryButton.pseudoClassStateChanged(highlightedClass, true);
			currentlyHighlighted = discoveryButton;
			break;
		case CONFORMANCE:
			conformanceButton.pseudoClassStateChanged(highlightedClass, true);
			currentlyHighlighted = conformanceButton;
			break;
		case GENERATION:
			generationButton.pseudoClassStateChanged(highlightedClass, true);
			currentlyHighlighted = generationButton;
			break;
		case EDITOR:
			editorButton.pseudoClassStateChanged(highlightedClass, true);
			currentlyHighlighted = editorButton;
			break;
		case MONITORING:
			monitoringButton.pseudoClassStateChanged(highlightedClass, true);
			currentlyHighlighted = monitoringButton;
			break;
		case FILTERING:
			filteringButton.pseudoClassStateChanged(highlightedClass, true);
			currentlyHighlighted = filteringButton;
			break;
		default:
			break;
		}
		logger.debug("Navigation sidebar highlight updated for page: {}", pageType);
	}

	public void setNavigationCallback(Consumer<PageType> navigationCallback) {
		this.navigationCallback = navigationCallback;
	}

	public void setNavigationWithInventoryElementCallback(
			BiConsumer<PageType, InventorySavedElement> navigationCallback) {
		this.navigationWithInventoryElementCallback = navigationCallback;
	}
}
