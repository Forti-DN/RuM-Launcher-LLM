package controller.conformance;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.plugins.DataConformance.framework.VariableMatchCost;

import controller.common.AbstractController;
import controller.conformance.datarow.DataCostDataRow;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import task.conformance.ActivityMappingResult;
import util.DataTableUtils;

public class DataSettingsController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox dataSettingsVbox;
	@FXML
	private TableView<DataCostDataRow> dataCostsTable;
	@FXML
	private TableColumn<DataCostDataRow,CellDataWrapper<String>> activityNameColumn;
	@FXML
	private TableColumn<DataCostDataRow,CellDataWrapper<String>> attributeNameColumn;
	@FXML
	private TableColumn<DataCostDataRow,CellDataWrapper<BigDecimal>> nonWritingCostColumn;
	@FXML
	private TableColumn<DataCostDataRow,CellDataWrapper<BigDecimal>> faultyValueCostColumn;
	@FXML
	private TableColumn<DataCostDataRow,RowStatus> rowActionsColumn;
	@FXML
	private Button addDataCostButton;
	@FXML
	private Button closeButton;

	private Label dataCostTablePlaceholder = new Label("Log needs to be selected before specific costs can be added");

	private BigDecimal defaultNonWritingCost;
	private BigDecimal defaultFaultyValueCost;
	private ActivityMappingResult activityMappingResult;
	private ObservableList<String> selectableActivities = FXCollections.observableArrayList();
	private ObservableList<String> selectableAttributes = FXCollections.observableArrayList();

	@FXML
	private void initialize() {
		dataCostsTable.setPlaceholder(dataCostTablePlaceholder);

		DataTableUtils.setDefaultRowFactory(dataCostsTable);
		DataTableUtils.addScrollFilter(dataCostsTable);
		DataTableUtils.setContentBasedHeight(dataCostsTable, 31d, 53d);

		activityNameColumn.setCellValueFactory(new PropertyValueFactory<>("activityName"));
		activityNameColumn.setCellFactory(param -> new ComboBoxCell<DataCostDataRow, String>(selectableActivities, null));
		activityNameColumn.setReorderable(false);

		attributeNameColumn.setCellValueFactory(new PropertyValueFactory<>("attributeName"));
		attributeNameColumn.setCellFactory(param -> new ComboBoxCell<DataCostDataRow, String>(selectableAttributes, null));
		attributeNameColumn.setReorderable(false);

		nonWritingCostColumn.setCellValueFactory(new PropertyValueFactory<>("nonWritingCost"));
		nonWritingCostColumn.setCellFactory(param -> new PositiveDecimalCell<DataCostDataRow>());
		nonWritingCostColumn.setReorderable(false);

		faultyValueCostColumn.setCellValueFactory(new PropertyValueFactory<>("faultyValueCost"));
		faultyValueCostColumn.setCellFactory(param -> new PositiveDecimalCell<DataCostDataRow>());
		faultyValueCostColumn.setReorderable(false);

		rowActionsColumn.setCellValueFactory(new PropertyValueFactory<>("rowStatus"));
		rowActionsColumn.setCellFactory(param -> new ActionCell<DataCostDataRow>(activityNameColumn, attributeNameColumn));
		rowActionsColumn.setReorderable(false);
		
		dataSettingsVbox.setOnMouseClicked(MouseEvent::consume); //Avoids closing the layer when user clicks on an empty area of the settings region

		updateAddingEnabled();

		logger.debug("Data settings layer initialized");
	}

	@FXML
	private void addDataCost() {
		try {
			DataCostDataRow specificDataCostRow = new DataCostDataRow(null, null, defaultNonWritingCost, defaultFaultyValueCost);
			this.dataCostsTable.getItems().add(specificDataCostRow);

			dataCostsTable.scrollTo(dataCostsTable.getItems().size()-1);
			dataCostsTable.requestFocus();
			dataCostsTable.getSelectionModel().select(dataCostsTable.getItems().size()-1);
		} catch (NumberFormatException e) {
			//Assume that faulty value feedback is already visible
		}
	}

	public Button getCloseButton() {
		return closeButton;
	}

	public void prepareForActivityMapUpdates(SimpleObjectProperty<ActivityMappingResult> activityMappingProperty) {
		activityMappingProperty.addListener((obs, oldValue, newValue) -> {
			if (newValue != null) {
				dataCostTablePlaceholder.setText("");
				activityMappingResult = activityMappingProperty.getValue();
				selectableActivities.add("-All Activities-");
				selectableActivities.addAll(activityMappingResult.getMatchedActivities());
			} else {
				dataCostTablePlaceholder.setText("Loading activities ...");
				activityMappingResult = null;
				selectableActivities.clear();
				dataCostsTable.getItems().clear(); //TODO: Remove only these settings that become invalid with the new activityMappingResult
			}
			updateAddingEnabled();
		});
	}

	public void setAttributes(SortedSet<String> attributes) {
		selectableAttributes.add("-All attributes-");
		selectableAttributes.addAll(attributes);
	}

	public void setDefaultNonWritingCost(BigDecimal defaultNonWritingCost) {
		this.defaultNonWritingCost = defaultNonWritingCost;
		updateAddingEnabled();
	}

	public void setDefaultFaultyValueCost(BigDecimal defaultFaultyValueCost) {
		this.defaultFaultyValueCost = defaultFaultyValueCost;
		updateAddingEnabled();
	}

	public boolean isDataCostsEmpty() {
		return dataCostsTable.getItems().isEmpty();
	}

	public void discardUnsavedChanges() {
		for (int i = dataCostsTable.getItems().size()-1; i >= 0; i--) {
			DataCostDataRow specificDataCostRow = dataCostsTable.getItems().get(i);
			if (specificDataCostRow.getRowStatus() == RowStatus.NEW) {
				dataCostsTable.getItems().remove(i);
			} else if (specificDataCostRow.getRowStatus() == RowStatus.EDITING) {
				specificDataCostRow.cancelRowEdit();
			}
		}
		dataCostsTable.refresh();
	}

	public List<VariableMatchCost> getVariableMatchCosts() {
		List<VariableMatchCost> variableMatchCosts = new ArrayList<>();
		VariableMatchCost variableMatchCost;
		
		//Default cost object (may be overridden below)
		VariableMatchCost defaultVariableMatchCost = new VariableMatchCost();
		defaultVariableMatchCost.setActivity(null);
		defaultVariableMatchCost.setVariable(null);
		defaultVariableMatchCost.setCostNotWriting(defaultNonWritingCost.floatValue());
		defaultVariableMatchCost.setCostFaultyValue(defaultFaultyValueCost.floatValue());

		//Specific costs
		for (DataCostDataRow specificDataCostRow : dataCostsTable.getItems()) {
			if (specificDataCostRow.getActivityName().equals("-All Activities-") && specificDataCostRow.getAttributeName().equals("-All attributes-")) {
				//Overriding the defaults
				defaultVariableMatchCost.setCostNotWriting(specificDataCostRow.getNonWritingCost().floatValue());
				defaultVariableMatchCost.setCostFaultyValue(specificDataCostRow.getFaultyValueCost().floatValue());
			} else {
				variableMatchCost = new VariableMatchCost();
				if (specificDataCostRow.getActivityName().equals("-All Activities-")) {
					variableMatchCost.setActivity(null);
				} else {
					variableMatchCost.setActivity(specificDataCostRow.getActivityName());
				}
				if (specificDataCostRow.getAttributeName().equals("-All attributes-")) {
					variableMatchCost.setVariable(null);
				} else {
					variableMatchCost.setVariable(specificDataCostRow.getAttributeName());
				}
				variableMatchCost.setCostNotWriting(specificDataCostRow.getNonWritingCost().floatValue());
				variableMatchCost.setCostFaultyValue(specificDataCostRow.getFaultyValueCost().floatValue());
				variableMatchCosts.add(variableMatchCost);
			}
		}

		//Default cost object must be added last, otherwise DataAware Declare Replayer ignores specific costs
		variableMatchCosts.add(defaultVariableMatchCost);

		return variableMatchCosts;
	}

	private void updateAddingEnabled() {
		boolean disableDataCostButton = !(defaultNonWritingCost != null && defaultFaultyValueCost != null && activityMappingResult != null);
		addDataCostButton.setDisable(disableDataCostButton);
	}
}
