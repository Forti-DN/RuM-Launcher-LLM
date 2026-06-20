package controller;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import util.TraceElement;

public class TraceViewController extends ScrollPane {
	
	@FXML
	private Pane tracePane;
	
	@FXML
	private ScrollPane mainPane;
	
	@FXML
	private Pane element;
	
	@FXML
	private Line arrowBody;
	
	@FXML
	private Line arrowLeft;
	
	@FXML
	private Line arrowRight;
	
	@FXML
	private Label elementLabel;
	
	@FXML
	private Label indexLabel;
	
	@FXML
	private VBox attributes;
	
	@FXML
	private Line attributeLine;
	
	private List<TraceElement> list;
	
	private double prefHeight;
	
	private double prefWidth;
	
	private String common;
	
	public TraceViewController(List<TraceElement> list,double prefHeight,double prefWidth) {
		this.list = list;
		this.prefHeight = prefHeight;
		this.prefWidth = prefWidth;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/TraceView.fxml"));
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
		tracePane.setPrefHeight(this.prefHeight);
		//tracePane.setPrefWidth(this.prefWidth);
		mainPane.setPrefHeight(this.prefHeight);
		mainPane.setPrefWidth(this.prefWidth);
		this.common = element.getStyle();
		int size = list.size();
		if(size > 1) {
			double height = tracePane.getPrefHeight();
			tracePane.setPrefHeight(height/3.0 * size);
			elementLabel.setText(list.get(0).getText());
			indexLabel.setText("1");
			element.setStyle(common+"-fx-background-color: "+list.get(0).getColor()+";");
			element.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
				attributes.setVisible(true);
				attributeLine.setVisible(true);
			});
			element.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
				attributes.setVisible(false);
				attributeLine.setVisible(false);
			});
			elementLabel.setText(list.get(0).getText());
			attributes.setPrefHeight(list.get(0).getAttributes().size()*25.0+20.0);
			for(String s:list.get(0).getAttributes()) {
				Label l = new Label(s);
				if(s.contains("Attributes") || s.contains("Contributes")) {
					l.setStyle("-fx-font-weight: bold;");
				}
				attributes.getChildren().add(l);
			}
			attributes.setVisible(false);
			attributeLine.setVisible(false);
			for(int i=1; i<size; i++) {
				if(i == (size-1)) {
					insertElement(true,list.get(i),i);
				}
				else {
					insertElement(false,list.get(i),i);
				}
			}
		}
		else if(size == 1) {
			element.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
				attributes.setVisible(true);
				attributeLine.setVisible(true);
			});
			element.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
				attributes.setVisible(false);
				attributeLine.setVisible(false);
			});
			elementLabel.setText(list.get(0).getText());
			for(String s:list.get(0).getAttributes()) {
				Label l = new Label(s);
				if(s.contains("Attributes")) {
					l.setStyle("-fx-font-weight: bold;");
				}
				attributes.getChildren().add(l);
			}
			attributes.setVisible(false);
			attributeLine.setVisible(false);
			tracePane.getChildren().removeAll(arrowBody,arrowLeft,arrowRight);
		}
	}
	
	private void insertElement(boolean isEnd, TraceElement te, int index) {
		Label label = new Label();
		label.setText(te.getText());
		label.setPrefHeight(elementLabel.getPrefHeight());
		label.setPrefWidth(elementLabel.getPrefWidth());
		label.setLayoutX(elementLabel.getLayoutX());
		label.setLayoutY(elementLabel.getLayoutY());
		label.setStyle(elementLabel.getStyle());
		Pane elem = new Pane();
		elem.setPrefHeight(element.getPrefHeight());
		elem.setPrefWidth(element.getPrefWidth());
		elem.setLayoutX(element.getLayoutX());
		elem.setLayoutY(element.getLayoutY()+(185.0*index));
		elem.setStyle(element.getStyle());
		elem.setStyle(common+"-fx-background-color: "+te.getColor()+";");
		elem.getChildren().add(label);
		Line aB = new Line();
		aB.setStartX(arrowBody.getStartX());
		aB.setStartY(arrowBody.getStartY());
		aB.setEndX(arrowBody.getEndX());
		aB.setEndY(arrowBody.getEndY());
		aB.setLayoutX(arrowBody.getLayoutX());
		aB.setLayoutY(arrowBody.getLayoutY()+(185.0*index));
		aB.setStrokeWidth(arrowBody.getStrokeWidth());
		Line aL = new Line();
		aL.setStartX(arrowLeft.getStartX());
		aL.setStartY(arrowLeft.getStartY());
		aL.setEndX(arrowLeft.getEndX());
		aL.setEndY(arrowLeft.getEndY());
		aL.setLayoutX(arrowLeft.getLayoutX());
		aL.setLayoutY(arrowLeft.getLayoutY()+(185.0*index));
		aL.setStrokeWidth(arrowLeft.getStrokeWidth());
		Line aR = new Line();
		aR.setStartX(arrowRight.getStartX());
		aR.setStartY(arrowRight.getStartY());
		aR.setEndX(arrowRight.getEndX());
		aR.setEndY(arrowRight.getEndY());
		aR.setLayoutX(arrowRight.getLayoutX());
		aR.setLayoutY(arrowRight.getLayoutY()+(185.0*index));
		aR.setStrokeWidth(arrowRight.getStrokeWidth());
		
		Line atrL = new Line();
		atrL.setStartX(attributeLine.getStartX());
		atrL.setStartY(attributeLine.getStartY()+(185.0*index));
		atrL.setEndX(attributeLine.getEndX());
		atrL.setEndY(atrL.getStartY());
		atrL.setStrokeWidth(attributeLine.getStrokeWidth());
		
		VBox atr = new VBox();
		atr.setStyle(attributes.getStyle());
		for(String s:te.getAttributes()) {
			Label l = new Label(s);
			if(s.contains("Attributes") || s.contains("Contributes")) {
				l.setStyle("-fx-font-weight: bold;");
			}
			atr.getChildren().add(l);
		}
		atr.setPrefHeight(te.getAttributes().size()*25.0+20.0);
		atr.setPrefWidth(attributes.getPrefWidth());
		atr.setLayoutX(attributes.getLayoutX());
		atr.setLayoutY(attributes.getLayoutY()+(185.0*index));
		
		elem.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
			if(!atr.getChildren().isEmpty()) {
				atr.setVisible(true);
				atrL.setVisible(true);
			}
		});
		elem.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
			atr.setVisible(false);
			atrL.setVisible(false);
		});
		atr.setVisible(false);
		atrL.setVisible(false);
		
		Label iL = new Label(index+1+"");
		iL.setLayoutX(indexLabel.getLayoutX());
		iL.setLayoutY(indexLabel.getLayoutY()+(185.0*index));
		iL.setStyle(indexLabel.getStyle());
		iL.setPadding(indexLabel.getPadding());
		iL.setPrefHeight(indexLabel.getPrefHeight());
		iL.setPrefWidth(indexLabel.getPrefWidth());
		
		if(isEnd) tracePane.getChildren().addAll(elem,atr,atrL,iL);
		
		else tracePane.getChildren().addAll(elem,aB,aR,aL,atr,atrL,iL);
	}

}
