package controller;

import java.io.IOException;

import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class DeclareTemplateController extends Pane{
	
	@FXML
	private Rectangle exRectA;
	
	@FXML
	private Text existenceActivityA;
	
	@FXML
	private Rectangle rectA;
	
	@FXML
	private Text supportA;
	
	@FXML
	private Text activityA;
	
	@FXML
	private Rectangle exRectB;
	
	@FXML
	private Text existenceActivityB;
	
	@FXML
	private Rectangle rectB;
	
	@FXML
	private Text supportB;
	
	@FXML
	private Text activityB;
	
	private String existenceA;
	
	private double suppA;
	
	private String actA;
	
	private String existenceB;
	
	private double suppB;
	
	private String actB;
	
	private DeclareTemplate declareTemplate;
	
	private double conSupp;
	
	public DeclareTemplateController(String existenceA,
									double suppA,
									String actA,
									String existenceB,
									double suppB,
									String actB,
									DeclareTemplate declareTemplate,
									double conSupp) {
		this.existenceA = existenceA;
		this.suppA = suppA;
		this.actA = actA;
		this.existenceB = existenceB;
		this.suppB = suppB;
		this.actB = actB;
		this.declareTemplate = declareTemplate;
		this.conSupp = conSupp;
		
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/DeclareTemplate.fxml"));
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
		existenceActivityA.setText(existenceA);
		supportA.setText(String.format("Support: %.2f", suppA));
		activityA.setText(actA);
		
		existenceActivityB.setText(existenceB);
		supportB.setText(String.format("Support: %.2f", suppB));
		activityB.setText(actB);
		
		drawConnection();
	}
	
	private void drawConnection() {
		switch(declareTemplate) {
			case Responded_Existence:
				drawRespondedExistenceConnection();
				break;
			case Response:
				drawResponseConnection();
				break;
			case Alternate_Response:
				drawAlternateResponseConnection();
				break;
			case Chain_Response:
				drawChainResponseConnection();
				break;
			case Precedence:
				drawPrecedenceConnection();
				break;
			case Alternate_Precedence:
				drawAlternatePrecedenceConnection();
				break;
			case Chain_Precedence:
				drawChainPrecedenceConnection();
				break;
			case Succession:
				drawSuccessionConnection();
				break;
			case Alternate_Succession:
				drawAlternateSucessionConnection();
				break;
			case Chain_Succession:
				drawChainSuccessionConnection();
				break;
			case Not_CoExistence:
				drawNotCoExistenceConnection();
				break;
			case Not_Succession:
				drawNotSuccessionConnection();
				break;
			case Not_Chain_Succession:
				drawNotChainSuccession();
				break;
			case Not_Chain_Precedence:
				drawNotChainPrecedence();
				break;
			case Not_Chain_Response:
				drawNotChainResponse();
				break;
			case Not_Precedence:
				drawNotPrecedenceConnection();
				break;
			case Not_Response:
				drawNotResponseConnection();
				break;
			case Not_Responded_Existence:
				drawNotRespondedExistenceConnection();
				break;
			case CoExistence:
				drawCoExistenceConnection();
				break;
			default:
				drawExistence();
		}
	}
	
	private void drawExistence() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		this.getChildren().removeAll(exRectB, existenceActivityB, rectB,
									supportB, activityB);
	}
	
	private void drawRespondedExistenceConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		//this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY()+5);
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY());
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(arrowBody.getEndX());
		arrowLeft.setStartY(arrowBody.getEndY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		//this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(arrowBody.getEndX());
		arrowRight.setStartY(arrowBody.getEndY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		//this.getChildren().add(arrowRight);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 55);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 8);
		label.setRotate(-90.0);
		label.setText("RespondedExistence");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 7.5));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(dotStart.getCenterX() + 5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 9.5));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawResponseConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		//this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY()+5);
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY());
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(arrowBody.getEndX());
		arrowLeft.setStartY(arrowBody.getEndY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(arrowBody.getEndX());
		arrowRight.setStartY(arrowBody.getEndY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 40);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 10);
		label.setRotate(-90.0);
		label.setText("Response");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(label.getLayoutX() + 33);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(label.getFont());
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	
	}
	
	private void drawAlternateResponseConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		//this.getChildren().add(dotEnd);
		
		Line arrowBody1 = new Line();
		arrowBody1.setStartX(dotStart.getCenterX()-5);
		arrowBody1.setStartY(dotStart.getCenterY()+5);
		arrowBody1.setEndX(dotEnd.getCenterX()-5);
		arrowBody1.setEndY(dotEnd.getCenterY()-10);
		arrowBody1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody1);
		
		Line arrowBody2 = new Line();
		arrowBody2.setStartX(dotStart.getCenterX()+5);
		arrowBody2.setStartY(dotStart.getCenterY()+5);
		arrowBody2.setEndX(dotEnd.getCenterX()+5);
		arrowBody2.setEndY(dotEnd.getCenterY()-10);
		arrowBody2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody2);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotEnd.getCenterX());
		arrowLeft.setStartY(dotEnd.getCenterY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotEnd.getCenterX());
		arrowRight.setStartY(dotEnd.getCenterY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowMid = new Line();
		arrowMid.setStartX(arrowLeft.getEndX());
		arrowMid.setStartY(dotEnd.getCenterY()-10);
		arrowMid.setEndX(arrowRight.getEndX());
		arrowMid.setEndY(dotEnd.getCenterY()-10);
		arrowMid.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowMid);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 60);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 7);
		label.setRotate(-90.0);
		label.setText("AlternateResponse");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 8.5));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(dotStart.getCenterX() + 10);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawChainResponseConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		//this.getChildren().add(dotEnd);
		
		Line arrowBody1 = new Line();
		arrowBody1.setStartX(dotStart.getCenterX()-5);
		arrowBody1.setStartY(dotStart.getCenterY()+5);
		arrowBody1.setEndX(dotEnd.getCenterX()-5);
		arrowBody1.setEndY(dotEnd.getCenterY()-10);
		arrowBody1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody1);
		
		Line arrowBody2 = new Line();
		arrowBody2.setStartX(dotStart.getCenterX()+5);
		arrowBody2.setStartY(dotStart.getCenterY()+5);
		arrowBody2.setEndX(dotEnd.getCenterX()+5);
		arrowBody2.setEndY(dotEnd.getCenterY()-10);
		arrowBody2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody2);
		
		Line arrowBody3 = new Line();
		arrowBody3.setStartX(dotStart.getCenterX());
		arrowBody3.setStartY(dotStart.getCenterY()+5);
		arrowBody3.setEndX(dotEnd.getCenterX());
		arrowBody3.setEndY(dotEnd.getCenterY()-10);
		arrowBody3.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody3);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotEnd.getCenterX());
		arrowLeft.setStartY(dotEnd.getCenterY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotEnd.getCenterX());
		arrowRight.setStartY(dotEnd.getCenterY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowMid = new Line();
		arrowMid.setStartX(arrowLeft.getEndX());
		arrowMid.setStartY(dotEnd.getCenterY()-10);
		arrowMid.setEndX(arrowRight.getEndX());
		arrowMid.setEndY(dotEnd.getCenterY()-10);
		arrowMid.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowMid);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 55);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 7);
		label.setRotate(-90.0);
		label.setText("ChainResponse");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 10));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(dotStart.getCenterX() + 10);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawPrecedenceConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		//this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY());
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY()-5);
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(arrowBody.getEndX());
		arrowLeft.setStartY(arrowBody.getEndY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(arrowBody.getEndX());
		arrowRight.setStartY(arrowBody.getEndY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 40);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 10);
		label.setRotate(-90.0);
		label.setText("Precedence");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(label.getLayoutX() + 33);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(label.getFont());
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawAlternatePrecedenceConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		//this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody1 = new Line();
		arrowBody1.setStartX(dotStart.getCenterX()-5);
		arrowBody1.setStartY(dotStart.getCenterY());
		arrowBody1.setEndX(dotEnd.getCenterX()-5);
		arrowBody1.setEndY(dotEnd.getCenterY()-15);
		arrowBody1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody1);
		
		Line arrowBody2 = new Line();
		arrowBody2.setStartX(dotStart.getCenterX()+5);
		arrowBody2.setStartY(dotStart.getCenterY());
		arrowBody2.setEndX(dotEnd.getCenterX()+5);
		arrowBody2.setEndY(dotEnd.getCenterY()-15);
		arrowBody2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody2);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotEnd.getCenterX());
		arrowLeft.setStartY(dotEnd.getCenterY()-5);
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotEnd.getCenterX());
		arrowRight.setStartY(dotEnd.getCenterY()-5);
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowMid = new Line();
		arrowMid.setStartX(arrowLeft.getEndX());
		arrowMid.setStartY(dotEnd.getCenterY()-15);
		arrowMid.setEndX(arrowRight.getEndX());
		arrowMid.setEndY(dotEnd.getCenterY()-15);
		arrowMid.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowMid);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 60);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 7);
		label.setRotate(-90.0);
		label.setText("AlternatePrecedence");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 7.5));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(dotStart.getCenterX() + 10);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawChainPrecedenceConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		//this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody1 = new Line();
		arrowBody1.setStartX(dotStart.getCenterX()-5);
		arrowBody1.setStartY(dotStart.getCenterY());
		arrowBody1.setEndX(dotEnd.getCenterX()-5);
		arrowBody1.setEndY(dotEnd.getCenterY()-15);
		arrowBody1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody1);
		
		Line arrowBody2 = new Line();
		arrowBody2.setStartX(dotStart.getCenterX()+5);
		arrowBody2.setStartY(dotStart.getCenterY());
		arrowBody2.setEndX(dotEnd.getCenterX()+5);
		arrowBody2.setEndY(dotEnd.getCenterY()-15);
		arrowBody2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody2);
		
		Line arrowBody3 = new Line();
		arrowBody3.setStartX(dotStart.getCenterX());
		arrowBody3.setStartY(dotStart.getCenterY());
		arrowBody3.setEndX(dotEnd.getCenterX());
		arrowBody3.setEndY(dotEnd.getCenterY()-15);
		arrowBody3.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody3);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotEnd.getCenterX());
		arrowLeft.setStartY(dotEnd.getCenterY()-5);
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotEnd.getCenterX());
		arrowRight.setStartY(dotEnd.getCenterY()-5);
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowMid = new Line();
		arrowMid.setStartX(arrowLeft.getEndX());
		arrowMid.setStartY(dotEnd.getCenterY()-15);
		arrowMid.setEndX(arrowRight.getEndX());
		arrowMid.setEndY(dotEnd.getCenterY()-15);
		arrowMid.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowMid);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 55);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 7);
		label.setRotate(-90.0);
		label.setText("ChainPrecedence");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 9.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(dotStart.getCenterX() + 5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB)); 
	}
	
	private void drawSuccessionConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY()+5);
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY()-5);
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(arrowBody.getEndX());
		arrowLeft.setStartY(arrowBody.getEndY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(arrowBody.getEndX());
		arrowRight.setStartY(arrowBody.getEndY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 10);
		label.setRotate(-90.0);
		label.setText("Succession");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(label.getLayoutX() + 40);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(label.getFont());
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawAlternateSucessionConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody1 = new Line();
		arrowBody1.setStartX(dotStart.getCenterX()-5);
		arrowBody1.setStartY(dotStart.getCenterY()+5);
		arrowBody1.setEndX(dotEnd.getCenterX()-5);
		arrowBody1.setEndY(dotEnd.getCenterY()-15);
		arrowBody1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody1);
		
		Line arrowBody2 = new Line();
		arrowBody2.setStartX(dotStart.getCenterX()+5);
		arrowBody2.setStartY(dotStart.getCenterY()+5);
		arrowBody2.setEndX(dotEnd.getCenterX()+5);
		arrowBody2.setEndY(dotEnd.getCenterY()-15);
		arrowBody2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody2);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotEnd.getCenterX());
		arrowLeft.setStartY(dotEnd.getCenterY()-5);
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotEnd.getCenterX());
		arrowRight.setStartY(dotEnd.getCenterY()-5);
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowMid = new Line();
		arrowMid.setStartX(arrowLeft.getEndX());
		arrowMid.setStartY(dotEnd.getCenterY()-15);
		arrowMid.setEndX(arrowRight.getEndX());
		arrowMid.setEndY(dotEnd.getCenterY()-15);
		arrowMid.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowMid);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 60);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 7);
		label.setRotate(-90.0);
		label.setText("AlternateSuccession");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 7.5));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(dotStart.getCenterX() + 10);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawChainSuccessionConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody1 = new Line();
		arrowBody1.setStartX(dotStart.getCenterX()-5);
		arrowBody1.setStartY(dotStart.getCenterY()+5);
		arrowBody1.setEndX(dotEnd.getCenterX()-5);
		arrowBody1.setEndY(dotEnd.getCenterY()-15);
		arrowBody1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody1);
		
		Line arrowBody2 = new Line();
		arrowBody2.setStartX(dotStart.getCenterX()+5);
		arrowBody2.setStartY(dotStart.getCenterY()+5);
		arrowBody2.setEndX(dotEnd.getCenterX()+5);
		arrowBody2.setEndY(dotEnd.getCenterY()-15);
		arrowBody2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody2);
		
		Line arrowBody3 = new Line();
		arrowBody3.setStartX(dotStart.getCenterX());
		arrowBody3.setStartY(dotStart.getCenterY()+5);
		arrowBody3.setEndX(dotEnd.getCenterX());
		arrowBody3.setEndY(dotEnd.getCenterY()-15);
		arrowBody3.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody3);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotEnd.getCenterX());
		arrowLeft.setStartY(dotEnd.getCenterY()-5);
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotEnd.getCenterX());
		arrowRight.setStartY(dotEnd.getCenterY()-5);
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowMid = new Line();
		arrowMid.setStartX(arrowLeft.getEndX());
		arrowMid.setStartY(dotEnd.getCenterY()-15);
		arrowMid.setEndX(arrowRight.getEndX());
		arrowMid.setEndY(dotEnd.getCenterY()-15);
		arrowMid.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowMid);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 60);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 7);
		label.setRotate(-90.0);
		label.setText("ChainSuccession");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 9.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(dotStart.getCenterX() + 5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawNotCoExistenceConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY()+5);
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY()-5);
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotStart.getCenterX() - 10);
		arrowLeft.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 - 2);
		arrowLeft.setEndX(dotStart.getCenterX() + 10);
		arrowLeft.setEndY(arrowLeft.getStartY());
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotStart.getCenterX() - 10);
		arrowRight.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 + 2);
		arrowRight.setEndX(dotStart.getCenterX() + 10);
		arrowRight.setEndY(arrowRight.getStartY());
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Label label = new Label();
		label.setLayoutX(arrowLeft.getStartX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 8);
		label.setRotate(-90.0);
		label.setText("NotCoExistence");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 10.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(arrowLeft.getEndX()-5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(label.getFont());
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawNotSuccessionConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY()+5);
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY()-5);
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(arrowBody.getEndX());
		arrowLeft.setStartY(arrowBody.getEndY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(arrowBody.getEndX());
		arrowRight.setStartY(arrowBody.getEndY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowCenter1 = new Line();
		arrowCenter1.setStartX(dotStart.getCenterX() - 10);
		arrowCenter1.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 - 2);
		arrowCenter1.setEndX(dotStart.getCenterX() + 10);
		arrowCenter1.setEndY(arrowCenter1.getStartY());
		arrowCenter1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter1);
		
		Line arrowCenter2 = new Line();
		arrowCenter2.setStartX(dotStart.getCenterX() - 10);
		arrowCenter2.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 + 2);
		arrowCenter2.setEndX(dotStart.getCenterX() + 10);
		arrowCenter2.setEndY(arrowCenter2.getStartY());
		arrowCenter2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter2);
		
		Label label = new Label();
		label.setLayoutX(arrowCenter1.getStartX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 8);
		label.setRotate(-90.0);
		label.setText("NotSuccession");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 10.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(arrowCenter1.getEndX()-5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(label.getFont());
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawNotChainSuccession() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody1 = new Line();
		arrowBody1.setStartX(dotStart.getCenterX()-5);
		arrowBody1.setStartY(dotStart.getCenterY()+5);
		arrowBody1.setEndX(dotEnd.getCenterX()-5);
		arrowBody1.setEndY(dotEnd.getCenterY()-15);
		arrowBody1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody1);
		
		Line arrowBody2 = new Line();
		arrowBody2.setStartX(dotStart.getCenterX()+5);
		arrowBody2.setStartY(dotStart.getCenterY()+5);
		arrowBody2.setEndX(dotEnd.getCenterX()+5);
		arrowBody2.setEndY(dotEnd.getCenterY()-15);
		arrowBody2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody2);
		
		Line arrowBody3 = new Line();
		arrowBody3.setStartX(dotStart.getCenterX());
		arrowBody3.setStartY(dotStart.getCenterY()+5);
		arrowBody3.setEndX(dotEnd.getCenterX());
		arrowBody3.setEndY(dotEnd.getCenterY()-15);
		arrowBody3.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody3);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotEnd.getCenterX());
		arrowLeft.setStartY(dotEnd.getCenterY()-5);
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotEnd.getCenterX());
		arrowRight.setStartY(dotEnd.getCenterY()-5);
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowMid = new Line();
		arrowMid.setStartX(arrowLeft.getEndX());
		arrowMid.setStartY(dotEnd.getCenterY()-15);
		arrowMid.setEndX(arrowRight.getEndX());
		arrowMid.setEndY(dotEnd.getCenterY()-15);
		arrowMid.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowMid);
		
		Line arrowCenter1 = new Line();
		arrowCenter1.setStartX(dotStart.getCenterX() - 10);
		arrowCenter1.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 - 2);
		arrowCenter1.setEndX(dotStart.getCenterX() + 10);
		arrowCenter1.setEndY(arrowCenter1.getStartY());
		arrowCenter1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter1);
		
		Line arrowCenter2 = new Line();
		arrowCenter2.setStartX(dotStart.getCenterX() - 10);
		arrowCenter2.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 + 2);
		arrowCenter2.setEndX(dotStart.getCenterX() + 10);
		arrowCenter2.setEndY(arrowCenter2.getStartY());
		arrowCenter2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter2);
		
		Label label = new Label();
		label.setLayoutX(arrowCenter1.getStartX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 8);
		label.setRotate(-90.0);
		label.setText("NotChainSuccession");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 7.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(arrowCenter1.getEndX()-5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 10.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawNotChainPrecedence() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		//this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody1 = new Line();
		arrowBody1.setStartX(dotStart.getCenterX()-5);
		arrowBody1.setStartY(dotStart.getCenterY());
		arrowBody1.setEndX(dotEnd.getCenterX()-5);
		arrowBody1.setEndY(dotEnd.getCenterY()-15);
		arrowBody1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody1);
		
		Line arrowBody2 = new Line();
		arrowBody2.setStartX(dotStart.getCenterX()+5);
		arrowBody2.setStartY(dotStart.getCenterY());
		arrowBody2.setEndX(dotEnd.getCenterX()+5);
		arrowBody2.setEndY(dotEnd.getCenterY()-15);
		arrowBody2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody2);
		
		Line arrowBody3 = new Line();
		arrowBody3.setStartX(dotStart.getCenterX());
		arrowBody3.setStartY(dotStart.getCenterY());
		arrowBody3.setEndX(dotEnd.getCenterX());
		arrowBody3.setEndY(dotEnd.getCenterY()-15);
		arrowBody3.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody3);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotEnd.getCenterX());
		arrowLeft.setStartY(dotEnd.getCenterY()-5);
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotEnd.getCenterX());
		arrowRight.setStartY(dotEnd.getCenterY()-5);
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowMid = new Line();
		arrowMid.setStartX(arrowLeft.getEndX());
		arrowMid.setStartY(dotEnd.getCenterY()-15);
		arrowMid.setEndX(arrowRight.getEndX());
		arrowMid.setEndY(dotEnd.getCenterY()-15);
		arrowMid.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowMid);
		
		Line arrowCenter1 = new Line();
		arrowCenter1.setStartX(dotStart.getCenterX() - 10);
		arrowCenter1.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 - 2);
		arrowCenter1.setEndX(dotStart.getCenterX() + 10);
		arrowCenter1.setEndY(arrowCenter1.getStartY());
		arrowCenter1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter1);
		
		Line arrowCenter2 = new Line();
		arrowCenter2.setStartX(dotStart.getCenterX() - 10);
		arrowCenter2.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 + 2);
		arrowCenter2.setEndX(dotStart.getCenterX() + 10);
		arrowCenter2.setEndY(arrowCenter2.getStartY());
		arrowCenter2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter2);
		
		Label label = new Label();
		label.setLayoutX(arrowCenter1.getStartX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 8);
		label.setRotate(-90.0);
		label.setText("NotChainPrecedence");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 7.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(arrowCenter1.getEndX()-5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 10.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB)); 
	}
	
	private void drawNotChainResponse() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		//this.getChildren().add(dotEnd);
		
		Line arrowBody1 = new Line();
		arrowBody1.setStartX(dotStart.getCenterX()-5);
		arrowBody1.setStartY(dotStart.getCenterY()+5);
		arrowBody1.setEndX(dotEnd.getCenterX()-5);
		arrowBody1.setEndY(dotEnd.getCenterY()-10);
		arrowBody1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody1);
		
		Line arrowBody2 = new Line();
		arrowBody2.setStartX(dotStart.getCenterX()+5);
		arrowBody2.setStartY(dotStart.getCenterY()+5);
		arrowBody2.setEndX(dotEnd.getCenterX()+5);
		arrowBody2.setEndY(dotEnd.getCenterY()-10);
		arrowBody2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody2);
		
		Line arrowBody3 = new Line();
		arrowBody3.setStartX(dotStart.getCenterX());
		arrowBody3.setStartY(dotStart.getCenterY()+5);
		arrowBody3.setEndX(dotEnd.getCenterX());
		arrowBody3.setEndY(dotEnd.getCenterY()-10);
		arrowBody3.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody3);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(dotEnd.getCenterX());
		arrowLeft.setStartY(dotEnd.getCenterY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(dotEnd.getCenterX());
		arrowRight.setStartY(dotEnd.getCenterY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowMid = new Line();
		arrowMid.setStartX(arrowLeft.getEndX());
		arrowMid.setStartY(dotEnd.getCenterY()-10);
		arrowMid.setEndX(arrowRight.getEndX());
		arrowMid.setEndY(dotEnd.getCenterY()-10);
		arrowMid.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowMid);
		
		Line arrowCenter1 = new Line();
		arrowCenter1.setStartX(dotStart.getCenterX() - 10);
		arrowCenter1.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 - 2);
		arrowCenter1.setEndX(dotStart.getCenterX() + 10);
		arrowCenter1.setEndY(arrowCenter1.getStartY());
		arrowCenter1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter1);
		
		Line arrowCenter2 = new Line();
		arrowCenter2.setStartX(dotStart.getCenterX() - 10);
		arrowCenter2.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 + 2);
		arrowCenter2.setEndX(dotStart.getCenterX() + 10);
		arrowCenter2.setEndY(arrowCenter2.getStartY());
		arrowCenter2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter2);
		
		Label label = new Label();
		label.setLayoutX(arrowCenter1.getStartX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 8);
		label.setRotate(-90.0);
		label.setText("NotChainResponse");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 7.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(arrowCenter1.getEndX()-5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 10.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawNotPrecedenceConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		//this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY());
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY()-5);
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(arrowBody.getEndX());
		arrowLeft.setStartY(arrowBody.getEndY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(arrowBody.getEndX());
		arrowRight.setStartY(arrowBody.getEndY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowCenter1 = new Line();
		arrowCenter1.setStartX(dotStart.getCenterX() - 10);
		arrowCenter1.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 - 2);
		arrowCenter1.setEndX(dotStart.getCenterX() + 10);
		arrowCenter1.setEndY(arrowCenter1.getStartY());
		arrowCenter1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter1);
		
		Line arrowCenter2 = new Line();
		arrowCenter2.setStartX(dotStart.getCenterX() - 10);
		arrowCenter2.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 + 2);
		arrowCenter2.setEndX(dotStart.getCenterX() + 10);
		arrowCenter2.setEndY(arrowCenter2.getStartY());
		arrowCenter2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter2);
		
		Label label = new Label();
		label.setLayoutX(arrowCenter1.getStartX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 8);
		label.setRotate(-90.0);
		label.setText("NotPrecedence");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 8.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(arrowCenter1.getEndX()-5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 10.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawNotResponseConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		//this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY()+5);
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY());
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(arrowBody.getEndX());
		arrowLeft.setStartY(arrowBody.getEndY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(arrowBody.getEndX());
		arrowRight.setStartY(arrowBody.getEndY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowRight);
		
		Line arrowCenter1 = new Line();
		arrowCenter1.setStartX(dotStart.getCenterX() - 10);
		arrowCenter1.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 - 2);
		arrowCenter1.setEndX(dotStart.getCenterX() + 10);
		arrowCenter1.setEndY(arrowCenter1.getStartY());
		arrowCenter1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter1);
		
		Line arrowCenter2 = new Line();
		arrowCenter2.setStartX(dotStart.getCenterX() - 10);
		arrowCenter2.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 + 2);
		arrowCenter2.setEndX(dotStart.getCenterX() + 10);
		arrowCenter2.setEndY(arrowCenter2.getStartY());
		arrowCenter2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter2);
		
		Label label = new Label();
		label.setLayoutX(arrowCenter1.getStartX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 8);
		label.setRotate(-90.0);
		label.setText("NotResponse");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 8.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(arrowCenter1.getEndX()-5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 10.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawNotRespondedExistenceConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		//this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY()+5);
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY());
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(arrowBody.getEndX());
		arrowLeft.setStartY(arrowBody.getEndY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		//this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(arrowBody.getEndX());
		arrowRight.setStartY(arrowBody.getEndY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		//this.getChildren().add(arrowRight);
		
		Line arrowCenter1 = new Line();
		arrowCenter1.setStartX(dotStart.getCenterX() - 10);
		arrowCenter1.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 - 2);
		arrowCenter1.setEndX(dotStart.getCenterX() + 10);
		arrowCenter1.setEndY(arrowCenter1.getStartY());
		arrowCenter1.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter1);
		
		Line arrowCenter2 = new Line();
		arrowCenter2.setStartX(dotStart.getCenterX() - 10);
		arrowCenter2.setStartY((dotStart.getCenterY()+dotEnd.getCenterY()) / 2 + 2);
		arrowCenter2.setEndX(dotStart.getCenterX() + 10);
		arrowCenter2.setEndY(arrowCenter2.getStartY());
		arrowCenter2.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowCenter2);
		
		Label label = new Label();
		label.setLayoutX(arrowCenter1.getStartX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 8);
		label.setRotate(-90.0);
		label.setText("NotRespondedExistence");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 6.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(arrowCenter1.getEndX()-5);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 10.0));
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
	}
	
	private void drawCoExistenceConnection() {
		if(existenceA.isEmpty()) {
			this.getChildren().remove(2);
		}
		Circle dotStart = new Circle();
		dotStart.setRadius(10.0);
		dotStart.setFill(Color.BLACK);
		dotStart.setCenterX(rectA.getX() + (rectA.getWidth() / 2));
		dotStart.setCenterY(rectA.getY() + rectA.getHeight());
		this.getChildren().add(dotStart);
		
		Circle dotEnd = new Circle();
		dotEnd.setRadius(10.0);
		dotEnd.setFill(Color.BLACK);
		if(!existenceB.isEmpty()) {
			dotEnd.setCenterX(exRectB.getX() + (exRectB.getWidth() / 2));
			dotEnd.setCenterY(exRectB.getY());
		}
		else {
			dotEnd.setCenterX(rectB.getX() + (rectB.getWidth() / 2));
			dotEnd.setCenterY(rectB.getY());
			if(existenceA.isEmpty()) this.getChildren().remove(2);
			else this.getChildren().remove(3);
		}
		this.getChildren().add(dotEnd);
		
		Line arrowBody = new Line();
		arrowBody.setStartX(dotStart.getCenterX());
		arrowBody.setStartY(dotStart.getCenterY()+5);
		arrowBody.setEndX(dotEnd.getCenterX());
		arrowBody.setEndY(dotEnd.getCenterY()-5);
		arrowBody.setStrokeWidth(5 * conSupp * conSupp);
		this.getChildren().add(arrowBody);
		
		Line arrowLeft = new Line();
		arrowLeft.setStartX(arrowBody.getEndX());
		arrowLeft.setStartY(arrowBody.getEndY());
		arrowLeft.setEndX(arrowLeft.getStartX() - 10);
		arrowLeft.setEndY(arrowLeft.getStartY() - 10);
		arrowLeft.setStrokeWidth(5 * conSupp * conSupp);
		//this.getChildren().add(arrowLeft);
		
		Line arrowRight = new Line();
		arrowRight.setStartX(arrowBody.getEndX());
		arrowRight.setStartY(arrowBody.getEndY());
		arrowRight.setEndX(arrowRight.getStartX() + 10);
		arrowRight.setEndY(arrowRight.getStartY() - 10);
		arrowRight.setStrokeWidth(5 * conSupp * conSupp);
		//this.getChildren().add(arrowRight);
		
		Label label = new Label();
		label.setLayoutX(dotStart.getCenterX() - 45);
		label.setLayoutY((dotStart.getCenterY() + dotEnd.getCenterY()) / 2 - 10);
		label.setRotate(-90.0);
		label.setText("CoExistence");
		label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 11.0));
		this.getChildren().add(label);
		
		Label supLabel = new Label();
		supLabel.setLayoutX(label.getLayoutX() + 40);
		supLabel.setLayoutY(label.getLayoutY());
		supLabel.setRotate(label.getRotate());
		supLabel.setText(String.format("%.1f%%", conSupp * 100));
		supLabel.setFont(label.getFont());
		this.getChildren().add(supLabel);
		
		rectA.setFill(getColor(suppA));
		supportA.setFill(getColorText(suppA));
		activityA.setFill(getColorText(suppA));
		
		rectB.setFill(getColor(suppB));
		supportB.setFill(getColorText(suppB));
		activityB.setFill(getColorText(suppB));
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
