package controller.conformance;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.DataConformance.framework.ActivityMatchCost;
import org.processmining.plugins.DataConformance.framework.ReplayableActivity;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;

import controller.common.AbstractController;
import controller.conformance.datarow.FlowCostDataRow;
import datatable.AbstractDataRow.RowStatus;
import datatable.CellDataWrapper;
import datatable.cell.ActionCell;
import datatable.cell.ComboBoxCell;
import datatable.cell.PositiveDecimalCell;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import task.conformance.ActivityMappingResult;
import util.DataTableUtils;

public class FlowSettingsController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox flowSettingsVbox;
	@FXML
	private TableView<FlowCostDataRow> flowCostsTable;
	@FXML
	private TableColumn<FlowCostDataRow,CellDataWrapper<String>> activityNameColumn;
	@FXML
	private TableColumn<FlowCostDataRow,CellDataWrapper<BigDecimal>> insertionCostColumn;
	@FXML
	private TableColumn<FlowCostDataRow,CellDataWrapper<BigDecimal>> deletionCostColumn;
	@FXML
	private TableColumn<FlowCostDataRow,RowStatus> rowActionsColumn;
	@FXML
	private Button addFlowCostButton;
	@FXML
	private Button closeButton;

	private Label flowCostTablePlaceholder = new Label("Log needs to be selected before specific costs can be added");

	private BigDecimal defaultInsertionCost;
	private BigDecimal defaultDeletionCost;
	private ActivityMappingResult activityMappingResult;
	private ObservableList<String> selectableActivities = FXCollections.observableArrayList();

	@FXML
	private void initialize() {
		flowCostsTable.setPlaceholder(flowCostTablePlaceholder);

		DataTableUtils.setDefaultRowFactory(flowCostsTable);
		DataTableUtils.addScrollFilter(flowCostsTable);
		DataTableUtils.setContentBasedHeight(flowCostsTable, 31d, 53d);

		activityNameColumn.setCellValueFactory(new PropertyValueFactory<>("activityName"));
		activityNameColumn.setCellFactory(param -> new ComboBoxCell<FlowCostDataRow, String>(selectableActivities, null));
		activityNameColumn.setReorderable(false);

		insertionCostColumn.setCellValueFactory(new PropertyValueFactory<>("insertionCost"));
		insertionCostColumn.setCellFactory(param -> new PositiveDecimalCell<FlowCostDataRow>());
		insertionCostColumn.setReorderable(false);

		deletionCostColumn.setCellValueFactory(new PropertyValueFactory<>("deletionCost"));
		deletionCostColumn.setCellFactory(param -> new PositiveDecimalCell<FlowCostDataRow>());
		deletionCostColumn.setReorderable(false);

		rowActionsColumn.setCellValueFactory(new PropertyValueFactory<>("rowStatus"));
		rowActionsColumn.setCellFactory(param -> new ActionCell<FlowCostDataRow>(activityNameColumn));
		rowActionsColumn.setReorderable(false);

		flowSettingsVbox.setOnMouseClicked(event ->
				event.consume() //Avoids closing the layer when user clicks on an empty area of the settings region
		);
		
		updateAddingEnabled();

		logger.debug("Flow settings layer initialized");
	}

	@FXML
	private void addFlowCost() {
		FlowCostDataRow specificFlowCostRow = new FlowCostDataRow(null, defaultInsertionCost, defaultDeletionCost);
		this.flowCostsTable.getItems().add(specificFlowCostRow);

		flowCostsTable.scrollTo(flowCostsTable.getItems().size()-1);
		flowCostsTable.requestFocus();
		flowCostsTable.getSelectionModel().select(flowCostsTable.getItems().size()-1);
	}

	public Button getCloseButton() {
		return closeButton;
	}

	public void prepareForActivityMapUpdates(SimpleObjectProperty<ActivityMappingResult> activityMappingProperty) {
		activityMappingProperty.addListener((obs, oldValue, newValue) -> {
			if (newValue != null) {
				flowCostTablePlaceholder.setText("");
				activityMappingResult = activityMappingProperty.getValue();
				selectableActivities.add("-All Activities-");
				selectableActivities.addAll(activityMappingResult.getAllActivities());
			} else {
				flowCostTablePlaceholder.setText("Loading activities ...");
				activityMappingResult = null;
				selectableActivities.clear();
				flowCostsTable.getItems().clear(); //TODO: Remove only these settings that become invalid with the new activityMappingResult
			}
			updateAddingEnabled();
		});
	}

	public void setDefaultInsertionCost(BigDecimal defaultInsertionCost) {
		this.defaultInsertionCost = defaultInsertionCost;
		updateAddingEnabled();
	}

	public void setDefaultDeletionCost(BigDecimal defaultDeletionCost) {
		this.defaultDeletionCost = defaultDeletionCost;
		updateAddingEnabled();
	}

	public boolean isFlowCostsEmpty() {
		return flowCostsTable.getItems().isEmpty();
	}

	public void discardUnsavedChanges() {
		for (int i = flowCostsTable.getItems().size()-1; i >= 0; i--) {
			FlowCostDataRow specificFlowCostRow = flowCostsTable.getItems().get(i);
			if (specificFlowCostRow.getRowStatus() == RowStatus.NEW) {
				flowCostsTable.getItems().remove(i);
			} else if (specificFlowCostRow.getRowStatus() == RowStatus.EDITING) {
				specificFlowCostRow.cancelRowEdit();
			}
		}
		flowCostsTable.refresh();
	}

	public List<ActivityMatchCost> getActivityMatchCosts() {
		List<ActivityMatchCost> activityMatchCosts = new ArrayList<>();
		ActivityMatchCost activityMatchCost;
		
		

		//Insertion default (may be overridden below)
		ActivityMatchCost defaultInsertionMatchCost = new ActivityMatchCost();
		defaultInsertionMatchCost.setAllEvents(false);
		defaultInsertionMatchCost.setAllProcessActivities(true);
		defaultInsertionMatchCost.setCost(defaultInsertionCost.floatValue());
		defaultInsertionMatchCost.setEventClass(null);
		defaultInsertionMatchCost.setProcessActivity(null);

		//Deletion default (may be overridden below)
		ActivityMatchCost defaultDeletionMatchCost = new ActivityMatchCost();
		defaultDeletionMatchCost.setAllEvents(true);
		defaultDeletionMatchCost.setAllProcessActivities(false);
		defaultDeletionMatchCost.setCost(defaultDeletionCost.floatValue());
		defaultDeletionMatchCost.setEventClass(null);
		defaultDeletionMatchCost.setProcessActivity(null);

		//Specific costs
		for (FlowCostDataRow specificFlowCostRow : flowCostsTable.getItems()) {
			if (specificFlowCostRow.getActivityName().equals("-All Activities-")) {
				//Overriding the defaults
				defaultInsertionMatchCost.setCost(specificFlowCostRow.getInsertionCost().floatValue());
				defaultDeletionMatchCost.setCost(specificFlowCostRow.getDeletionCost().floatValue());
			} else {
				//Insertion cost for activity
				ReplayableActivity replayableActivity = findReplayableActivity(specificFlowCostRow.getActivityName());
				if (replayableActivity != null) {
					activityMatchCost = new ActivityMatchCost();
					activityMatchCost.setAllEvents(false);
					activityMatchCost.setAllProcessActivities(false);
					activityMatchCost.setCost(specificFlowCostRow.getInsertionCost().floatValue());
					activityMatchCost.setEventClass(null);
					activityMatchCost.setProcessActivity(replayableActivity);
					activityMatchCosts.add(activityMatchCost);
				}

				//Deletion cost for activity
				XEventClass eventClass = findEventClass (specificFlowCostRow.getActivityName());
				if (eventClass != null) {
					activityMatchCost = new ActivityMatchCost();
					activityMatchCost.setAllEvents(false);
					activityMatchCost.setAllProcessActivities(false);
					activityMatchCost.setCost(specificFlowCostRow.getDeletionCost().floatValue());
					activityMatchCost.setEventClass(eventClass);
					activityMatchCost.setProcessActivity(null);
					activityMatchCosts.add(activityMatchCost);
				}
			}
		}
		
		//Default cost objects must be added last, otherwise Declare Replayer and DataAware Declare Replayer ignore specific costs
		activityMatchCosts.add(defaultInsertionMatchCost);
		activityMatchCosts.add(defaultDeletionMatchCost);

		return activityMatchCosts;
	}

	private ReplayableActivity findReplayableActivity(String activity) {
		for(ReplayableActivity replayableActivity: activityMappingResult.getActivityMapping().keySet()) {
			if (replayableActivity.getLabel().equals(activity)) {
				return replayableActivity;
			}
		}
		return null;
	}

	private XEventClass findEventClass (String activity) {
		Map<ReplayableActivityDefinition, XEventClass> activityMapping = activityMappingResult.getActivityMapping();

		for(ReplayableActivity replayableActivity: activityMapping.keySet()) {
			if(replayableActivity.getLabel().equals(activity)) {
				return activityMapping.get(replayableActivity);
			} else if (replayableActivity.getLabel().equals("TICK")) {
				XEventClass eventClass = activityMapping.get(replayableActivity);
				int indexOfMinus = activity.lastIndexOf('-');

				if(indexOfMinus != -1) {
					String leftOfMinus = activity.substring(0, indexOfMinus);
					String rightOfMinus = activity.substring(indexOfMinus+1);
					if(eventClass.getId().equals(leftOfMinus+"+"+rightOfMinus)) {
						return eventClass;
					}
				} else if (eventClass.getId().equals(activity)) {
					return eventClass;
				}
			}
		}
		return null;
	}

	private void updateAddingEnabled() {
		if (defaultInsertionCost != null && defaultDeletionCost != null && activityMappingResult != null) {
			addFlowCostButton.setDisable(false);
		} else {
			addFlowCostButton.setDisable(true);
		}
	}
}
