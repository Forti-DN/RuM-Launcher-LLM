package controller.common;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.EnumMap;

import controller.filtering.FilteringPageController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import controller.conformance.ConformancePageController;
import controller.discovery.DiscoveryPageController;
import controller.editor.EditorPageController;
import controller.generation.GenerationPageController;
import controller.monitoring.MonitoringPageController;
import global.InventorySavedElement;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import util.PageType;

public class RootLayoutController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private NavigationSidebarController navigationSidebarController;
	@FXML
	private StartPageController startPageController;
	@FXML
	private HBox rootRegion;

	private PageType currentPageType;
	private EnumMap<PageType, Region> loadedPages = new EnumMap<PageType, Region>(PageType.class);
	private EnumMap<PageType, AbstractController> loadedController = new EnumMap<PageType, AbstractController>(
			PageType.class);

	@FXML
	private void initialize() {
		navigateToPage(PageType.START);
		navigationSidebarController.setNavigationCallback(pageType -> {
			navigateToPage(pageType);
		});
		navigationSidebarController.setNavigationWithInventoryElementCallback((pageType, savedElement) -> {
			navigateToPageAndOpenElement(pageType, savedElement);

		});
		logger.debug("Root layout initialized");
	}

	private void navigateToPage(PageType pageType) {
		if (currentPageType != pageType) {
			logger.debug("Navigating to page: {}", pageType);
			Region loadedPage = loadedPages.get(pageType);
			if (loadedPage == null) {
				try {
					loadedPage = loadPage(pageType);
					logger.debug("Loaded page: {}", pageType);
				} catch (IOException | IllegalStateException e) {
					logger.error("Can not load page: {}", pageType, e);
				}
			}
			rootRegion.getChildren().set(1, loadedPage);
			HBox.setHgrow(loadedPage, Priority.ALWAYS); // Makes sure that the page fills available space
			currentPageType = pageType;
			logger.info("Navigated to page: {}", pageType);
		}
	}

	private void navigateToPageAndOpenElement(PageType pageType, InventorySavedElement savedElement) {
		if (currentPageType != pageType) {
			logger.debug("Navigating to page: {} and open {}", pageType, savedElement.getSaveName());
			Region loadedPage = loadedPages.get(pageType);
			if (loadedPage == null) {
				try {
					loadedPage = loadPage(pageType);
					logger.debug("Loaded page: {}", pageType);
				} catch (IOException | IllegalStateException e) {
					logger.error("Can not load page: {}", pageType, e);
				}
			}
			rootRegion.getChildren().set(1, loadedPage);
			HBox.setHgrow(loadedPage, Priority.ALWAYS); // Makes sure that the page fills available space
			currentPageType = pageType;
			logger.info("Navigated to page: {}", pageType);
		}


		if (savedElement != null) {
			AbstractController abstractController = loadedController.get(pageType);
			switch (pageType) {
			case DISCOVERY:
				DiscoveryPageController discoveryController = (DiscoveryPageController) abstractController;
				discoveryController.openLogFromInventory(savedElement);
				break;
			case CONFORMANCE:
				ConformancePageController conformanceController = (ConformancePageController) abstractController;
				conformanceController.openModelFromInventory(savedElement);
				break;
			case GENERATION:
				GenerationPageController generationController = (GenerationPageController) abstractController;
				generationController.openModelFromInventory(savedElement);
				break;
			case EDITOR:
				EditorPageController editorController = (EditorPageController) abstractController;
				editorController.openModelFromInventory(savedElement);
				break;
			case MONITORING:
				MonitoringPageController monitoringPageController = (MonitoringPageController) abstractController;
				monitoringPageController.openModelFromInventory(savedElement);
				break;
			case FILTERING:
				FilteringPageController FilteringPageController = (FilteringPageController) abstractController;
				FilteringPageController.openLogFromInventory(savedElement);
				break;
			default:
				logger.error("PageType {} not supported", pageType);
			}
		
			logger.info("Opened element: {}", savedElement.getSaveName());
		}
	}

	private Region loadPage(PageType pageTypeToLoad) throws IOException, IllegalStateException {
		String fxmlPath = pageTypeToLoad.getPathToFxml();
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource(fxmlPath));
		Region loadedPage = fxmlLoader.load(); // There seems to be a bug in JavaFX framework that causes
		// IllegalStateException to be thrown instead of IOException
		((AbstractController) fxmlLoader.getController()).setStage(this.getStage());

		// Allows to use the start page for navigation
		if (pageTypeToLoad == PageType.START) {
			((StartPageController) fxmlLoader.getController()).setNavigationCallback(pageType -> {
				navigateToPage(pageType);
				navigationSidebarController.updateHighlight(pageType);
			});
		}

		loadedPages.put(pageTypeToLoad, loadedPage);
		loadedController.put(pageTypeToLoad, fxmlLoader.getController());
		return loadedPage;
	}
}
