package controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StatsController extends VBox {
	
	@FXML
	private Label averageFitness;
	
	//@FXML
	//private Label medianFitness;
	
	@FXML
	private Label traceCount;
	
	@FXML
	private Label numConstraint;
	
	//@FXML
	//private Label compMsec;
	
	@FXML
	private Label header1;
	
	//@FXML
	//private Label header2;
	
	@FXML
	private Label header3;
	
	@FXML
	private Label header4;
	
	//@FXML
	//private Label header5;
	
	private String s1,s2,s3,s4,s5;
	
	public StatsController(String s1, String s2, String s3, String s4, String s5) {
		this.s1 = s1;
		this.s2 = s2;
		this.s3 = s3;
		this.s4 = s4;
		this.s5 = s5;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/StatsView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public StatsController() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/StatsView.fxml"));
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
		if(s1 != null) {
			averageFitness.setText(s1);
			averageFitness.getStyleClass().add("labelText");
		}
		
		/*if(s2 != null) {
			medianFitness.setText(s2);
			medianFitness.getStyleClass().add("labelText");
		}*/
		
		if(s3 != null) {
			traceCount.setText(s3);
			traceCount.getStyleClass().add("labelText");
		}
		
		if(s4 != null) {
			numConstraint.setText(s4);
			numConstraint.getStyleClass().add("labelText");
		}
		
		/*if(s5 != null) {
			compMsec.setText(s5);
			compMsec.getStyleClass().add("labelText");
		}*/
	}
	
	public void setHeader1(String header, String value) {
		header1.setText(header);
		averageFitness.setText(value);
		averageFitness.getStyleClass().add("labelText");
	}
	
	/*public void setHeader2(String header, String value) {
		header2.setText(header);
		medianFitness.setText(value);
		medianFitness.getStyleClass().add("constraintText");
	}*/
	
	public void setHeader3(String header, String value) {
		header3.setText(header);
		traceCount.setText(value);
		traceCount.getStyleClass().add("labelText");
	}
	
	public void setHeader4(String header, String value) {
		header4.setText(header);
		numConstraint.setText(value);
		numConstraint.getStyleClass().add("labelText");
	}
	
	
	/*public void setHeader5(String header, String value) {
		header5.setText(header);
		compMsec.setText(value);
	}*/
	
	public void hideHeader1() {
		header1.setVisible(false);
		averageFitness.setVisible(false);
	}
	
	/*public void hideHeader2() {
		header2.setVisible(false);
		medianFitness.setVisible(false);
	}*/
	
	public void hideHeader3() {
		header3.setVisible(false);
		traceCount.setVisible(false);
	}
	
	public void hideHeader4() {
		header4.setVisible(false);
		numConstraint.setVisible(false);
	}
	
	/*public void hideHeader5() {
		header5.setVisible(false);
		compMsec.setVisible(false);
	}*/

}
