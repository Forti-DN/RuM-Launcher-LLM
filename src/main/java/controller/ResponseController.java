package controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class ResponseController extends Pane{

	@FXML
	private Text startText;
	
	@FXML
	private Text endText;
	
	@FXML
	private Text startSup;
	
	@FXML
	private Text endSup;
	
	@FXML
	private Pane mainPane;
	
	@FXML
	private Rectangle startRect;
	
	@FXML
	private Rectangle endRect;
	
	@FXML
	private Label response;
	
	@FXML
	private Label conSupport;
	
	@FXML
	private Line arrowBody;
	
	@FXML
	private Line arrowHeadRight;
	
	@FXML
	private Line arrowHeadLeft;
	
	private String startActivity;
	
	private double startActivitySupport;
	
	private String endActivity;
	
	private double endActivitySupport;
	
	private double constraintSupport;
	
	public ResponseController(String startActivity, double startActivitySupport,
							String endActivity, double endActivitySupport, double constraintSupport) {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/Response.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        this.startActivity = startActivity;
        this.startActivitySupport = startActivitySupport;
        this.endActivity = endActivity;
        this.endActivitySupport = endActivitySupport;
        this.constraintSupport = constraintSupport;
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public void setTexts(String start, String end) {
		this.startText.setText(start);
		this.endText.setText(end);
	}
	
	@FXML
	public void initialize() {
		conSupport.setText(String.format("%.1f%%", this.constraintSupport * 100));
		startText.setText(this.startActivity);
		startText.setFill(getColorText(this.startActivitySupport));
		startSup.setText(String.format("support: %.2f", this.startActivitySupport));
		startSup.setFill(getColorText(this.startActivitySupport));
		endText.setText(this.endActivity);
		endText.setFill(getColorText(this.endActivitySupport));
		endSup.setText(String.format("support: %.2f", this.endActivitySupport));
		endSup.setFill(getColorText(this.endActivitySupport));
		
		startRect.setFill(getColor(startActivitySupport));
		endRect.setFill(getColor(endActivitySupport));
		
		arrowBody.setStrokeWidth(5 * constraintSupport * constraintSupport);
		arrowHeadRight.setStrokeWidth(5 * constraintSupport * constraintSupport);
		arrowHeadLeft.setStrokeWidth(5 * constraintSupport * constraintSupport);
	}
	
	private Color getColor(double d) {
		if(d >= 0.7) return Color.BLUE;
		else if(d >= 0.4) return Color.SKYBLUE;
		else return Color.LIGHTBLUE;
	}
	
	private Color getColorText(double d) {
		if(d >= 0.7) return Color.WHITE;
		else return Color.BLACK;
	}
}
