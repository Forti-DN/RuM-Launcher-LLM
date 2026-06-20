package controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class InitController extends Pane {

	@FXML
	private Label activityLabel;
	
	@FXML
	private Label supportLabel;
	
	private String activity;
	
	private String activitySupport;
	
	public InitController(String activity, String activitySupport) {
		this.activity = activity;
		this.activitySupport = activitySupport;
		
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/Init.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	@FXML
	public void initialize() {
		this.activityLabel.setText(activity);
		this.supportLabel.setText(activitySupport);
	}
}
