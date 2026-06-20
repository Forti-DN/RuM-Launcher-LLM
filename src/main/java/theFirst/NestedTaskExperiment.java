package theFirst;

import java.time.LocalDateTime;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class NestedTaskExperiment extends Application{
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		Button b = new Button("Click me!");
		AnchorPane pane = new AnchorPane();
		pane.setPrefHeight(300.0);
		pane.setPrefWidth(300.0);
		b.setOnAction(e -> {
			ButtonTask bt = new ButtonTask(1);
			LabelTask lt = new LabelTask();
			bt.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, ee -> {
				System.out.println(bt.getValue()+" "+LocalDateTime.now());
				new Thread(lt).run();
			});
			lt.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, ee -> {
				pane.getChildren().add(lt.getValue());
			});
			new Thread(bt).run();
		});
		pane.getChildren().add(b);
		AnchorPane.setLeftAnchor(b, 50.0);
		AnchorPane.setRightAnchor(b, 50.0);
		Scene scene = new Scene(pane,500,500);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

}

class ButtonTask extends Task<String> {
	
	private int id;
	
	public ButtonTask(int id) {
		this.id = id;
	}

	@Override
	protected String call() throws Exception {
		// TODO Auto-generated method stub
		return id+" I am called!";
	}
	
}

class LabelTask extends Task<Label> {

	@Override
	protected Label call() throws Exception {
		// TODO Auto-generated method stub
		Label l = new Label("Button clicked!");
		l.setLayoutX(Math.random()*300);
		l.setLayoutY(Math.random()*300);
		return l;
	}
	
}
