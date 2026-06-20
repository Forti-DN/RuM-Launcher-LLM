package controller.common;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.fxml.FXML;
import util.PageType;

public class StartPageController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private Consumer<PageType> navigationCallback;

	@FXML
	private void initialize() {
		logger.debug("Start page initialized");
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

	private void handleNavigation(PageType pageType) {
		if (navigationCallback != null) {
			//Notification that this class has been used for navigation
			navigationCallback.accept(pageType);
		}
	}

	public void setNavigationCallback(Consumer<PageType> navigationCallback) {
		this.navigationCallback = navigationCallback;
	}
}
