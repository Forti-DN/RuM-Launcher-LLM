package controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;

public class ProgressController extends AnchorPane {
	
	@FXML
	private Label operationName;
	
	@FXML
	private ProgressIndicator progress;
	
	@FXML
	private Button cancelOperation;
	
	public ProgressController() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ProgressView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public void setOperationName(String operation) {
		operationName.setText(operation);
	}
	
	public ProgressIndicator getIndicator() {
		return this.progress;
	}
	
	public Button getCancelOperation() {
		return cancelOperation;
	}

}
