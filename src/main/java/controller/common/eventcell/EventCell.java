package controller.common.eventcell;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

import datatable.cell.ActionCell;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javax.swing.*;

public class EventCell extends ListCell<EventData> {

	//Most of logging commented out because cells can be updated very often and at arbitrary times
	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox rootRegion;

	@FXML
	private Label eventNumberLabel;
	@FXML
	private Label conceptNameLabel;
	@FXML
	private Label timeTimestampLabel;
	@FXML
	private Region eventTypeStrip;
	@FXML
	private VBox payloadVBox;

	private int eventNumber;

	private boolean showEventType;
	private boolean showPayload;
	private Consumer<Integer> selectionCallback;
	private FXMLLoader loader;


	public EventCell(boolean showEventType, boolean showPayload, Consumer<Integer> selectionCallback) {
		this.showEventType = showEventType;
		this.showPayload = showPayload;
		this.selectionCallback = selectionCallback;
	}

	@FXML
	private void initialize() {
		if (!showEventType) {
			eventTypeStrip.setVisible(false);
			eventTypeStrip.setManaged(false);
		}
		if (!showPayload) {
			payloadVBox.setVisible(false);
			payloadVBox.setManaged(false);
		}

		if (selectionCallback != null) {
			this.setOnMouseClicked(event -> {
				if (this.isSelected()) {
					selectionCallback.accept(eventNumber);
				} else {
					selectionCallback.accept(0);
				}
			});
		}

		//logger.debug("Event cell initialized");
	}

	@Override
	protected void updateItem(EventData item, boolean empty) {
		//https://openjfx.io/javadoc/11/javafx.controls/javafx/scene/control/Cell.html#updateItem(T,boolean)
		super.updateItem(item, empty);
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
		} else {
			if (loadFxml()) {
				//logger.debug("Updating event cell to item: {}", item.toString());
				eventNumber = item.getEventNumber();
				eventNumberLabel.setText(Integer.toString(eventNumber));
				conceptNameLabel.setText(item.getConceptName());

				if (item.getTimeTimestamp() != null) {
					timeTimestampLabel.setText(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(item.getTimeTimestamp()));
					timeTimestampLabel.setVisible(true);
					timeTimestampLabel.setManaged(true);
				} else {
					timeTimestampLabel.setText("");
					timeTimestampLabel.setVisible(false);
					timeTimestampLabel.setManaged(false);
				}

				if (showEventType && item.getActivityConformanceType() != null) {
					eventTypeStrip.getStyleClass().setAll(item.getActivityConformanceType().getCssClass());
					
					/* Removed tooltip since in the current implementation it shows the activity name, that is already visible in the EventCell
					
					if (item.getActivityConformanceType().getTooltipText() != null)
						Tooltip.install(eventTypeStrip, new Tooltip(item.getActivityConformanceType().getTooltipText()));
					*/
				}

				if (showPayload && item.getPayload() != null) {
					payloadVBox.getChildren().clear();
					if (item.getPayload().isEmpty()) {
						Label l = new Label("No payload");
						l.getStyleClass().add("event-data-text");
						l.setStyle("-fx-font-style: italic");
						payloadVBox.getChildren().add(l);
					} else {
						item.getPayload().forEach((k,v) -> {
							Label l = new Label(k+ ": " +v);
							l.getStyleClass().add("event-data-text");
							payloadVBox.getChildren().add(l);
						});
					}
				}

				setText(null);
				setGraphic(rootRegion);
				//logger.debug("Updated event cell to item: {}", item.toString());
			}
		}
	}

	private boolean loadFxml() {
		if (loader == null) {
			//Load ActionCell contents if not already loaded
			loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/eventcell/EventCell.fxml"));
			loader.setController(this);
			try {
				loader.load();
				return true;
			} catch (IOException | IllegalStateException e) {
				logger.error("Cannot load event cell.", e);
				return false;
			}
		} else {
			return true;
		}
	}
}