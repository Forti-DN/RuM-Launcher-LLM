package controller.filtering;

import controller.common.AbstractController;
import controller.common.eventcell.EventCell;
import controller.common.eventcell.EventData;
import controller.common.layers.AlertLayerController;
import controller.common.layers.ProgressLayerController;
import controller.filtering.cell.TemplateCell;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.controlsfx.control.ToggleSwitch;
import org.processmining.plugins.ltlchecker.InstanceModel;
import org.processmining.plugins.ltlchecker.RuleModel;
import task.filtering.EventFilteringTask;
import task.filtering.EventFilteringTaskResult;
import util.AlertUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class FilterInspectorController extends AbstractController {
    @FXML
    public ListView<RuleModel> filtersListView;
    @FXML
    ListView<InstanceModel> tracesListView;
    @FXML
    ListView<EventData> eventsListView;
    @FXML
    Label selectedTraceLabel;
    @FXML
    ToggleSwitch showPayloadsToggle;
    @FXML
    ToggleSwitch applyEventFiltersToggle;
    private FilteringTabController filteringTabController;
    private LinkedList<InstanceModel> eventFiltersListOfTraces;
    private static final String CONCEPT_NAME_ATTRIBUTE = "concept:name";

    @FXML
    private void initialize() {
        filtersListView.getSelectionModel().selectedItemProperty().addListener(this::onFilterSelected);
        filtersListView.setCellFactory(this::createFilterListViewCell);
        tracesListView.getSelectionModel().selectedItemProperty().addListener(this::onTraceSelected);
        tracesListView.setCellFactory(this::createTraceListViewCell);
        eventsListView.setCellFactory(this::createEventListViewCell);
        showPayloadsToggle.selectedProperty().addListener(this::onShowPayloadTogglePressed);
        applyEventFiltersToggle.selectedProperty().addListener(this::onAppliedEventFilers);
    }

    private void onShowPayloadTogglePressed(ObservableValue<? extends Boolean> observable, Boolean oldVal, Boolean newVal) {
        eventsListView.setCellFactory(value -> new EventCell(false, newVal, null));
    }

    private void onAppliedEventFilers(ObservableValue<? extends Boolean> observable, Boolean eventFiltersOFF, Boolean eventFiltersON) {
        int selectedTrace = tracesListView.getSelectionModel().getSelectedIndex();

        if (tracesListView.getSelectionModel().getSelectedItems().isEmpty()) {
            return;
        }

        if (eventFiltersON) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/ProgressLayer.fxml"));
                Node progressLayer = loader.load();
                ProgressLayerController progressLayerController = loader.getController();
                progressLayerController.getProgressTextLabel().setText("Filtering log...");

                EventFilteringTask eventFilteringTask = new EventFilteringTask();
                eventFilteringTask.setFilteringTabController(filteringTabController);
                eventFilteringTask.setSelectedFiltersListView(FXCollections.observableList(filteringTabController.selectedFiltersListView));
                eventFilteringTask.setIsFilterInspector(true);

                addHandlersToTask(eventFilteringTask, progressLayer, progressLayerController);
                filteringTabController.rootRegion.getChildren().add(progressLayer);
                filteringTabController.mainContents.setDisable(true);
                filteringTabController.executorService.execute(eventFilteringTask);

            } catch (Exception e) {
                AlertUtils.showWarning("Can not load progress layer");
            }
        } else {
            tracesListView.getItems().setAll(filteringTabController.filtersAndTraceResult.get(filtersListView.getSelectionModel().getSelectedIndex()).get(filteringTabController.filtersAndTraceResult.get(filtersListView.getSelectionModel().getSelectedIndex()).keySet().toArray()[0]));
            tracesListView.getSelectionModel().select(selectedTrace);
            eventsListView.getItems().setAll(ResultsViewController.getEventData(tracesListView.getSelectionModel().getSelectedItem()));
            StatisticsUtils.updateStatistics(filteringTabController.filtersAndTraceResult.get(filtersListView.getSelectionModel().getSelectedIndex()).get(filteringTabController.filtersAndTraceResult.get(filtersListView.getSelectionModel().getSelectedIndex()).keySet().toArray()[0]), filteringTabController);
        }
    }

    private void addHandlersToTask(Task<EventFilteringTaskResult> task, Node progressLayer, ProgressLayerController progressLayerController) {

        progressLayerController.getCancelButton().setOnAction(e -> {
            filteringTabController.rootRegion.getChildren().remove(progressLayer);
            filteringTabController.mainContents.setDisable(false);
            task.cancel(true);
        });

        task.setOnSucceeded(event -> {
            // Set the filtered traces as the items for the trace list view
            eventFiltersListOfTraces = task.getValue().getResultAfterAppliedEvenFilters();
            int selectedTrace = tracesListView.getSelectionModel().getSelectedIndex();
            tracesListView.getItems().setAll(eventFiltersListOfTraces);

            // Select the trace at the specified index in the trace list view
            tracesListView.getSelectionModel().select(selectedTrace);

            // Set the events associated with the selected trace as the items for the event list view
            eventsListView.getItems().setAll(ResultsViewController.getEventData(eventFiltersListOfTraces.get(selectedTrace)));

            // Update the statistics for the selected filters
            StatisticsUtils.updateStatistics(new LinkedList<>(eventFiltersListOfTraces), filteringTabController);

            filteringTabController.rootRegion.getChildren().remove(progressLayer);
            filteringTabController.mainContents.setDisable(false);
            task.cancel(true);
        });

        task.setOnFailed(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
                Node alertLayer = loader.load();
                AlertLayerController alertLayerController = loader.getController();
                alertLayerController.setAlertMessage(AlertLayerController.AlertType.ERROR, "Filtering log failed");
                alertLayerController.getOkButton().setOnAction(onFailedTaskAction(alertLayer));
                filteringTabController.rootRegion.getChildren().remove(progressLayer);
                filteringTabController.rootRegion.getChildren().add(alertLayer);
            } catch (IOException | IllegalStateException e) {
                AlertUtils.showWarning("Filtering log failed");
                filteringTabController.rootRegion.getChildren().remove(progressLayer);
                filteringTabController.mainContents.setDisable(false);
            }
        });
    }

    private EventHandler<ActionEvent> onFailedTaskAction(Node alertLayer) {
        return e -> {
            filteringTabController.rootRegion.getChildren().remove(alertLayer);
            filteringTabController.mainContents.setDisable(false);
        };
    }

    private void onTraceSelected(ObservableValue<? extends InstanceModel> obs, InstanceModel oldValue, InstanceModel selectedTrace) {
        if (selectedTrace == null) {
            eventsListView.setVisible(false);
            return;
        }
        eventsListView.setVisible(true);

        if (applyEventFiltersToggle.selectedProperty().get() && FXCollections.observableList(filteringTabController.selectedFiltersListView).stream().anyMatch(filter -> filter.getFilteringMode().equals("keepSelectedMode") || filter.getFilteringMode().equals("trimToTimeframe") || filter.getFilteringMode().equals("trimFirst") || filter.getFilteringMode().equals("trimLongest"))) {
            eventsListView.getItems().setAll(ResultsViewController.getEventData(eventFiltersListOfTraces.get(tracesListView.getSelectionModel().getSelectedIndex())));
        } else {
            filtersListView.refresh();
            eventsListView.getItems().setAll(ResultsViewController.getEventData(selectedTrace));
        }
        selectedTraceLabel.setText("Trace ID: " + selectedTrace.getInstance().getAttributes().get(CONCEPT_NAME_ATTRIBUTE).toString());
    }

    protected TemplateCell createFilterListViewCell(ListView<RuleModel> rule) {
        TemplateCell templateCell = new TemplateCell(TemplateCell.Type.FILTER_INSPECTOR);
        templateCell.setRuleSelectionModel(filtersListView.getSelectionModel());
        return templateCell;
    }

    private ListCell<EventData> createEventListViewCell(ListView<EventData> eventDataListView) {
        return new EventCell(false, showPayloadsToggle.isSelected(), null);
    }

    private ListCell<InstanceModel> createTraceListViewCell(ListView<InstanceModel> traceListView) {
        return new ListCell<>() {
            @Override
            protected void updateItem(InstanceModel trace, boolean empty) {
                super.updateItem(trace, empty);
                if (empty || trace == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                RuleModel selectedItem = filtersListView.getSelectionModel().getSelectedItem();
                if (selectedItem == null) {
                    setText("Trace ID. " + trace.getInstance().getAttributes().get(CONCEPT_NAME_ATTRIBUTE).toString());
                    setGraphic(null);
                    return;
                }

                TextFlow textFlowPane = new TextFlow();
                Text traceID = new Text("Trace ID. " + trace.getInstance().getAttributes().get(CONCEPT_NAME_ATTRIBUTE).toString() + "\t");
                textFlowPane.setPrefWidth(0);
                textFlowPane.setMaxWidth(Double.MAX_VALUE);
                traceID.wrappingWidthProperty().bind(traceListView.widthProperty());

                if (trace.equals(traceListView.getSelectionModel().getSelectedItem())) {
                    traceID.setFill(Color.WHITE);
                }

                if (trace.getInstance().isEmpty()) {
                    traceID.setFill(Color.RED);
                }

                textFlowPane.getChildren().add(traceID);
                setGraphic(textFlowPane);
            }
        };
    }

    private void onFilterSelected(ObservableValue<? extends RuleModel> obs, RuleModel oldValue, RuleModel selectedRule) {
        if (selectedRule == null) {
            tracesListView.setVisible(false);
            eventsListView.setVisible(false);
            return;
        }
        applyEventFiltersToggle.setSelected(false);
        int indexOfSelectedRule = filtersListView.getSelectionModel().getSelectedIndex();
        tracesListView.refresh();
        tracesListView.setVisible(true);
        tracesListView.getItems().setAll(FXCollections.observableList(filteringTabController.filtersAndTraceResult.get(indexOfSelectedRule).get(filteringTabController.filtersAndTraceResult.get(indexOfSelectedRule).keySet().toArray()[0])));
        eventsListView.setVisible(tracesListView.getSelectionModel().getSelectedItem() != null);
        tracesListView.getSelectionModel().selectFirst();
        StatisticsUtils.updateStatistics(new LinkedList<>(tracesListView.getItems()), filteringTabController);
    }

    protected void updateTraceData() {
        tracesListView.getItems().setAll(FXCollections.observableList(Arrays.asList(filteringTabController.filtersAndTraceResult.get(0).get(filteringTabController.filtersAndTraceResult.get(0).keySet().toArray()[0]).toArray(InstanceModel[]::new))));
        tracesListView.getSelectionModel().selectFirst();
    }

    protected void updateFiltersData() {
        filtersListView.setItems(FXCollections.observableList(Arrays.stream(filteringTabController.currentFiltersForInspector.toArray(RuleModel[]::new)).filter(filter -> filter.getDescription().equals("TRACE_FILTER")).collect(Collectors.toList())));
        filtersListView.getSelectionModel().selectFirst();
    }

    public void setFilteringTabController(FilteringTabController filteringTabController) {
        this.filteringTabController = filteringTabController;
    }
}
