package controller.filtering;

import controller.common.AbstractController;
import controller.common.eventcell.EventCell;
import controller.common.eventcell.EventData;
import controller.common.layers.AlertLayerController;
import controller.common.layers.ProgressLayerController;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TraceInspectorController extends AbstractController {
    @FXML
    public ListView<InstanceModel> tracesListView;
    @FXML
    ListView<RuleModel> filtersListView;
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
        tracesListView.setCellFactory(this::createTraceListViewCell);
        tracesListView.getSelectionModel().selectedItemProperty().addListener(this::onTraceSelected);
        filtersListView.setCellFactory(this::createFiltersListCell);
        eventsListView.setCellFactory(this::createEventListViewCell);
        showPayloadsToggle.selectedProperty().addListener(this::onShowPayloadTogglePressed);
        applyEventFiltersToggle.selectedProperty().addListener(this::onAppliedEventFilers);
    }

    private List<RuleModel> getAppliedFilters() {
        String selectedTraceId = tracesListView.getSelectionModel().getSelectedItem().getInstance().getAttributes().get(CONCEPT_NAME_ATTRIBUTE).toString();

        return filteringTabController.filtersAndTraceResult.stream()
                .flatMap(e -> e.entrySet().stream()).filter(entry -> !entry.getKey().getRuleName().equals("All filters applied")).filter(entry -> entry.getValue().stream()
                        .anyMatch(instanceModel -> {
                            String currentId = instanceModel.getInstance().getAttributes().get(CONCEPT_NAME_ATTRIBUTE).toString();
                            return currentId.equals(selectedTraceId);
                        })).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private void onTraceSelected(ObservableValue<? extends InstanceModel> obs, InstanceModel oldValue, InstanceModel selectedTrace) {
        if (selectedTrace != null) {
            filtersListView.refresh();
            filtersListView.setItems(FXCollections.observableList(getAppliedFilters()));
            eventsListView.getItems().setAll(ResultsViewController.getEventData(selectedTrace));
            selectedTraceLabel.setText("Trace ID: " + selectedTrace.getInstance().getAttributes().get(CONCEPT_NAME_ATTRIBUTE).toString());
            filtersListView.getSelectionModel().selectFirst();
        }
        applyEventFiltersToggle.setSelected(false);
        filtersListView.setVisible(selectedTrace != null);
        eventsListView.setVisible(selectedTrace != null);
    }

    private ListCell<InstanceModel> createTraceListViewCell(ListView<InstanceModel> listView) {

        final Color COLOR_BLACK = Color.BLACK;
        final Color COLOR_WHITE = Color.WHITE;

        return new ListCell<>() {
            @Override
            protected void updateItem(InstanceModel trace, boolean empty) {
                super.updateItem(trace, empty);
                if (empty || trace == null) {
                    setText(null);
                } else {
                    String matchingRatio = (int) trace.getHealthDegree() + "/" + (filteringTabController.resultsViewController.filtersInspector.filtersListView.getItems().size() - 1);

                    HBox hbox = new HBox();
                    hbox.setPrefWidth(USE_COMPUTED_SIZE);

                    Label traceIdLabel = new Label("Trace ID. " + trace.getInstance().getAttributes().get(CONCEPT_NAME_ATTRIBUTE).toString());
                    traceIdLabel.setTextFill(COLOR_BLACK);

                    Label matchingRatioLabel = new Label(matchingRatio);
                    matchingRatioLabel.setTextFill(COLOR_BLACK);

                    HBox.setHgrow(traceIdLabel, Priority.ALWAYS);
                    HBox.setHgrow(matchingRatioLabel, Priority.ALWAYS);

                    HBox idHBox = new HBox();
                    idHBox.getChildren().add(traceIdLabel);
                    HBox.setHgrow(idHBox, Priority.ALWAYS);
                    idHBox.setAlignment(Pos.CENTER_LEFT);

                    HBox ratioHBox = new HBox();
                    ratioHBox.getChildren().add(matchingRatioLabel);
                    HBox.setHgrow(ratioHBox, Priority.ALWAYS);
                    ratioHBox.setAlignment(Pos.CENTER_RIGHT);

                    if (trace.equals(tracesListView.getSelectionModel().getSelectedItem())) {
                        matchingRatioLabel.setTextFill(COLOR_WHITE);
                        traceIdLabel.setTextFill(COLOR_WHITE);
                    }

                    hbox.getChildren().addAll(idHBox, ratioHBox);
                    setGraphic(hbox);
                }
            }
        };
    }

    private ListCell<RuleModel> createFiltersListCell(ListView<RuleModel> traceListView) {
        return new ListCell<>() {
            private final TextFlow textFlowPane = new TextFlow();
            private final Text filterName = new Text();

            {
                textFlowPane.setPrefWidth(0); // initialize with 0 to force computation
                textFlowPane.setMaxWidth(Double.MAX_VALUE);
                filterName.wrappingWidthProperty().bind(traceListView.widthProperty());
                textFlowPane.getChildren().add(filterName);
            }

            @Override
            protected void updateItem(RuleModel filter, boolean empty) {
                super.updateItem(filter, empty);

                if (empty || filter == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                filterName.setText(filter.getRuleName() + "\t");

                if (filter.equals(traceListView.getSelectionModel().getSelectedItem())) {
                    filterName.setFill(Color.WHITE);
                } else {
                    filterName.setFill(Color.BLACK);
                }

                if (applyEventFiltersToggle.selectedProperty().getValue() && !traceListView.getItems().isEmpty() && eventsListView.getItems().isEmpty()) {
                    filterName.setFill(Color.RED);
                }

                setGraphic(textFlowPane);
            }

            @Override
            protected double computePrefWidth(double height) {
                return USE_COMPUTED_SIZE;
            }
        };
    }

    private ListCell<EventData> createEventListViewCell(ListView<EventData> eventDataListView) {
        return new EventCell(false, showPayloadsToggle.isSelected(), null);
    }


    private void onShowPayloadTogglePressed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        eventsListView.setCellFactory(value -> new EventCell(false, newValue, null));
    }

    private void onAppliedEventFilers(ObservableValue<? extends Boolean> observable, Boolean eventFiltersOFF, Boolean eventFiltersON) {
        if (eventFiltersON && FXCollections.observableList(filteringTabController.selectedFiltersListView).stream().anyMatch(filter -> filter.getFilteringMode().equals("keepSelectedMode") || filter.getFilteringMode().equals("trimToTimeframe") || filter.getFilteringMode().equals("trimFirst") || filter.getFilteringMode().equals("trimLongest"))) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/ProgressLayer.fxml"));
                Node progressLayer = loader.load();
                ProgressLayerController progressLayerController = loader.getController();
                progressLayerController.getProgressTextLabel().setText("Filtering log...");

                EventFilteringTask eventFilteringTask = new EventFilteringTask();
                eventFilteringTask.setFilteringTabController(filteringTabController);
                eventFilteringTask.setSelectedFiltersListView(FXCollections.observableList(filteringTabController.selectedFiltersListView));
                eventFilteringTask.setIsFilterInspector(false);

                addHandlersToTask(eventFilteringTask, progressLayer, progressLayerController);
                filteringTabController.rootRegion.getChildren().add(progressLayer);
                filteringTabController.mainContents.setDisable(true);
                filteringTabController.executorService.execute(eventFilteringTask);

            } catch (Exception e) {
                AlertUtils.showWarning("Can not load progress layer");
            }
        } else {
            filtersListView.refresh();
            eventsListView.getItems().setAll(ResultsViewController.getEventData(tracesListView.getSelectionModel().getSelectedItem()));
        }
    }

    private void addHandlersToTask(Task<EventFilteringTaskResult> task, Node progressLayer, ProgressLayerController progressLayerController) {

        progressLayerController.getCancelButton().setOnAction(e -> {
            filteringTabController.rootRegion.getChildren().remove(progressLayer);
            filteringTabController.mainContents.setDisable(false);
            task.cancel(true);
        });

        task.setOnSucceeded(event -> {
            eventFiltersListOfTraces = task.getValue().getResultAfterAppliedEvenFilters();
            eventsListView.getItems().setAll(ResultsViewController.getEventData(eventFiltersListOfTraces.get(task.getValue().getSelectedFilterIndex())));
            if (!filtersListView.getItems().isEmpty()) {
                int indexOfSelectedTrace = filtersListView.getSelectionModel().getSelectedIndex();
                filtersListView.getItems().setAll(FXCollections.observableList(getAppliedFilters()));
                filtersListView.getSelectionModel().select(indexOfSelectedTrace);
                filtersListView.setVisible(true);
                filtersListView.refresh();
            } else {
                filtersListView.setVisible(false);
            }

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

    public void updateTraceData() {
        tracesListView.setItems(FXCollections.observableList(filteringTabController.unmodifiedTraces));
        tracesListView.getSelectionModel().selectFirst();
        tracesListView.scrollTo(tracesListView.getSelectionModel().getSelectedIndex());
        filtersListView.getSelectionModel().selectFirst();
        filtersListView.scrollTo(filtersListView.getSelectionModel().getSelectedIndex());
    }

    public void setFilteringTabController(FilteringTabController filteringTabController) {
        this.filteringTabController = filteringTabController;
    }
}