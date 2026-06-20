package controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

public class MonitoringController extends GridPane{
	
	@FXML
	private ListView<String> caseList = new ListView<>();
	
	@FXML
	private AnchorPane runTimeMonitorView;
	
	//private String input;
	
	public MonitoringController(){
		
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/MonitorView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	
		
}
