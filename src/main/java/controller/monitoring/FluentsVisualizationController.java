package controller.monitoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import controller.common.eventcell.EventData;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class FluentsVisualizationController {
	
	@FXML
	private VBox fluentsVbox;
	@FXML
	private HBox eventLabelHbox;
	
	
	private Map<String, HBox> constraintToFluentHbox = new HashMap<String, HBox>();
	private List<EventData> traceEvents;
	
	@FXML
	private void initialize() {
		fluentsVbox.getChildren().clear();
		eventLabelHbox.getChildren().clear();
	}
	
	public void setTraceEvents(List<EventData> traceEvents) {
		this.traceEvents = traceEvents;
		
		if (traceEvents == null) {
			fluentsVbox.getChildren().clear();
			eventLabelHbox.getChildren().clear();
			constraintToFluentHbox.clear();
		} else {
			if (constraintToFluentHbox.isEmpty()) {
				for (String constraintString : traceEvents.get(0).getConstraintStates().keySet()) {
					Text fluentNameLabel = new Text(constraintString);
					fluentNameLabel.getStyleClass().add("fluent-name");
					fluentsVbox.getChildren().add(fluentNameLabel);
					HBox constraintStateHbox = new HBox();
					constraintStateHbox.getStyleClass().add("fluent-line");
					fluentsVbox.getChildren().add(constraintStateHbox);
					constraintToFluentHbox.put(constraintString, constraintStateHbox);
				}
			}
		}
	}
	
	public void setVisualizationEventIndex(int eventIndex) {
		eventLabelHbox.getChildren().clear();
		for (HBox fluentHBox : constraintToFluentHbox.values()) {
			fluentHBox.getChildren().clear();
		}
		
		for (int i = 0; i < eventIndex; i++) {
			Label eventLabel = new Label(traceEvents.get(i).getConceptName());
			eventLabel.getStyleClass().add("fluent-event-label");
			Group eventLabelGroup = new Group(eventLabel); //Group node is necessary for the rotation of the label to apply correctly
			eventLabelHbox.getChildren().add(eventLabelGroup);
			
			for (String constraintString : traceEvents.get(i).getConstraintStates().keySet()) {
				String constraintState = traceEvents.get(i).getConstraintStates().get(constraintString);
				String stateString = null;
				Label fluentTile = new Label();
				fluentTile.getStyleClass().add("fluent-tile");
				
				//TODO: Should really use an enum for the monitoring states
				if (constraintState.equals("poss.viol")) {
					fluentTile.getStyleClass().add("fluent-tile__temp-viol");
					stateString = "temp.viol";
				} else if (constraintState.equals("viol")) {
					fluentTile.getStyleClass().add("fluent-tile__perm-viol");
					stateString = "perm.viol";
				} else if (constraintState.equals("poss.sat")) {
					fluentTile.getStyleClass().add("fluent-tile__temp-sat");
					stateString = "temp.sat";
				} else if (constraintState.equals("sat")) {
					fluentTile.getStyleClass().add("fluent-tile__perm-sat");
					stateString = "perm.sat";
				} else if (constraintState.equals("conflict")) {
					fluentTile.getStyleClass().add("fluent-tile__conflict");
					stateString = "conflict";
				}
				
				if (i == 0 || !traceEvents.get(i-1).getConstraintStates().get(constraintString).equals(constraintState)) {
					fluentTile.setText(stateString);
				}
				
				constraintToFluentHbox.get(constraintString).getChildren().add(fluentTile);
				
			}
		}
	}
	
}
