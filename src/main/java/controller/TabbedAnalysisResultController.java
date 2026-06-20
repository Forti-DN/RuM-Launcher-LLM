package controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

public class TabbedAnalysisResultController extends TabPane{
	
	@FXML
	private AnchorPane generalResultView;
	
	@FXML
	private AnchorPane detailedResultView;
	
	@FXML
	private Tab tab1;
	
	@FXML
	private Tab tab2;
	
	private AnalysisResultController arc;
	
	private DetailedAnalysisResultController darc;
	
	private String name1;
	
	private String name2;
	
	public TabbedAnalysisResultController(AnalysisResultController arc, DetailedAnalysisResultController darc) {
		this.arc = arc;
		this.darc = darc;
		
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/TabbedAnalysisResultView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public TabbedAnalysisResultController(String name1, String name2) {
		// TODO Auto-generated constructor stub
		this.name1 = name1;
		this.name2 = name2;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/TabbedAnalysisResultView.fxml"));
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
		if(arc != null) {
			generalResultView.getChildren().clear();
			generalResultView.getChildren().add(arc);
			AnchorPane.setBottomAnchor(arc, 0.0);
			AnchorPane.setTopAnchor(arc, 0.0);
		}
		if(darc != null) {
			detailedResultView.getChildren().clear();
			detailedResultView.getChildren().add(darc);
			AnchorPane.setBottomAnchor(darc, 0.0);
			AnchorPane.setTopAnchor(darc, 0.0);
			AnchorPane.setLeftAnchor(darc, 0.0);
			AnchorPane.setRightAnchor(darc, 0.0);
		}
		
		if(name1 != null) tab1.setText(name1);
		if(name2 != null) tab2.setText(name2);
	}
	
	public void setToFirstTab(StatsController p) {
		generalResultView.getChildren().clear();
		generalResultView.getChildren().add(p);
		AnchorPane.setBottomAnchor(p, 0.0);
		AnchorPane.setTopAnchor(p, 0.0);
		AnchorPane.setLeftAnchor(p, 0.0);
		AnchorPane.setRightAnchor(p, 0.0);
	}
	
	public void setToSecondTab(AlignmentResultController arc) {
		detailedResultView.getChildren().clear();
		detailedResultView.getChildren().add(arc);
		AnchorPane.setBottomAnchor(arc, 0.0);
		AnchorPane.setTopAnchor(arc, 0.0);
		AnchorPane.setLeftAnchor(arc, 0.0);
		AnchorPane.setRightAnchor(arc, 0.0);
	}

}
