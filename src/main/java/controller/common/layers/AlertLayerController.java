package controller.common.layers;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import controller.common.AbstractController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class AlertLayerController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private Label headerLabel;
	@FXML
	private FontIcon headerIcon;
	@FXML
	private Label detailsLabel;
	@FXML
	private HBox buttonsHBox;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;

	public static enum AlertType {INFO, WARNING, ERROR}

	@FXML
	private void initialize() {
		//Cancel button is removed by default and re-added on first get
		buttonsHBox.getChildren().remove(cancelButton);
		logger.debug("Alert layer initialized");
	}

	//Ok logic must be added to this button by the calling class
	public Button getOkButton() {
		return okButton;
	}

	//Cancel logic must be added to this button by the calling class
	public Button getCancelButton() {
		if (!buttonsHBox.getChildren().contains(cancelButton)) {
			buttonsHBox.getChildren().add(cancelButton);
		}
		return cancelButton;
	}

	public void setAlertMessage(AlertType alertType, String message) {
		logger.debug("Seting alert tupe: {} and message: {}", alertType, message);

		detailsLabel.setText(message);

		switch (alertType) {
		case INFO:
			headerLabel.setText("Info");
			headerIcon.setIconLiteral("fa-info-circle");
			headerIcon.setIconColor(Color.web("00a6fb"));
			break;
		case WARNING:
			headerLabel.setText("Warning");
			headerIcon.setIconLiteral("fa-exclamation-triangle");
			headerIcon.setIconColor(Color.web("ff9300"));
			break;
		case ERROR:
			headerLabel.setText("Error");
			headerIcon.setIconLiteral("fa-exclamation-triangle");
			headerIcon.setIconColor(Color.web("ff0000"));
			break;
		default:
			headerLabel.setText("Info");
			headerIcon.setIconLiteral("fa-info-circle");
			headerIcon.setIconColor(Color.web("00a6fb"));
			logger.error("Unhandled alertType: {} defaulting to: {}", alertType, AlertType.INFO);
			break;
		}
	}
}
