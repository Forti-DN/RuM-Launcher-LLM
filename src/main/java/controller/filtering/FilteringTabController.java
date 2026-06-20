package controller.filtering;

import controller.common.AbstractController;
import controller.common.layers.AlertLayerController;
import controller.common.layers.ProgressLayerController;
import controller.filtering.cell.FilterCell;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.ltlchecker.InstanceModel;
import org.processmining.plugins.ltlchecker.RuleModel;
import task.filtering.LogPreprocessingTask;
import task.filtering.LogPreprocessingTaskResult;
import task.filtering.TraceFilteringTask;
import task.filtering.TraceFilteringTaskResult;
import util.AlertUtils;
import util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static controller.filtering.cell.FilterCell.setSelectedCell;


public class FilteringTabController extends AbstractController {
    public final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public final AtomicBoolean isChanged = new AtomicBoolean(false);
    @FXML
    public Button importFiltersButton;
    @FXML
    public ChoiceBox<String> addFiltersChoiceBox;
    @FXML
    public StackPane contentsLayer;
    @FXML
    public HBox mainContents;
    @FXML
    public HBox statisticsContents;
    @FXML
    public ListView<Filter> appliedFiltersListView;
    @FXML
    public StackPane stackPaneResults;
    public LinkedList<RuleModel> currentFiltersForInspector = new LinkedList<>();
    public List<Map<RuleModel, LinkedList<InstanceModel>>> filtersAndTraceResult = new LinkedList<>();
    public ObservableList<InstanceModel> unmodifiedTraces = FXCollections.observableArrayList();
    public ObservableList<Filter> selectedFiltersListView = FXCollections.observableArrayList();
    public LinkedList<InstanceModel> listOfTraces;
    public LTLWindowController ltlWindowController;
    public ResultsViewController resultsViewController;
    public BasicFiltersWindowController basicFiltersWindowController;
    public XLog xLog;
    public File logFile;
    public Region resultsView;
    public TraceFilteringTask traceFilteringTask;
    public FilteringTabWindowService filtersWindowService;
    public FilteringTabEventHandler filteringTabEventHandler;
    public LogPreprocessingTask logPreprocessingTask;
    protected LinkedHashMap<String, List<String>> attributeAndValue;
    @FXML
    StackPane rootRegion;
    @FXML
    VBox parametersSection;
    @FXML
    PieChart chartOfCases;
    @FXML
    PieChart chartOfEvents;
    @FXML
    Label medianValue;
    @FXML
    Label startTimeValue;
    @FXML
    Label endTimeValue;
    @FXML
    Label meanValue;
    @FXML
    Label eventsValue;
    @FXML
    Label tracesValue;
    @FXML
    Label tracesPercentage;
    @FXML
    Label eventsPercentage;
    @FXML
    Label maxCaseDuration;
    @FXML
    Label minCaseDuration;
    PieChart.Data remainingTracesPiechartData;
    PieChart.Data filteredTracesPiechartData;
    PieChart.Data filteredEventsPiechartData;
    PieChart.Data remainingEventsPiechartData;
    @FXML
    private Separator separator;
    @FXML
    private Label filteringIntroLabel;

    @FXML
    private void initialize() throws IOException, NullPointerException {
        FXMLLoader loaderResultsView = new FXMLLoader(getClass().getClassLoader().getResource("pages/filtering/ResultsView.fxml"));
        resultsView = loaderResultsView.load();
        resultsViewController = loaderResultsView.getController();
        resultsViewController.setLtlCheckerTabController(this);

        FXMLLoader loaderLTLWindow = new FXMLLoader(getClass().getClassLoader().getResource("pages/filtering/LtlWindow.fxml"));
        loaderLTLWindow.load();
        ltlWindowController = loaderLTLWindow.getController();

        FXMLLoader loaderBasicFiltersWindow = new FXMLLoader(getClass().getClassLoader().getResource("pages/filtering/BasicFiltersWindow.fxml"));
        loaderBasicFiltersWindow.load();
        basicFiltersWindowController = loaderBasicFiltersWindow.getController();
        basicFiltersWindowController.setFilteringTabController(this);

        filteringTabEventHandler = new FilteringTabEventHandler();
        filteringTabEventHandler.initializeHandlers(this);

        filtersWindowService = new FilteringTabWindowService(this);
        filtersWindowService.initializeWindows();

        parametersSection.setViewOrder(-1); // Makes sure that template settings slide in from under the parameters

        appliedFiltersListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        appliedFiltersListView.setCellFactory(value -> new FilterCell(this));
        appliedFiltersListView.getItems().addListener(getFilterListChangeListener());

        selectedFiltersListView.addListener((ListChangeListener<Filter>) change -> appliedFiltersListView.getItems().setAll(selectedFiltersListView));
        selectedFiltersListView.addListener(getFilterListChangeListener(isChanged));
        unmodifiedTraces.addListener(getInstanceModelListChangeListener(unmodifiedTraces));

        filteringIntroLabel.prefHeightProperty().bind(parametersSection.prefHeightProperty());
        filteringIntroLabel.prefWidthProperty().bind(appliedFiltersListView.prefWidthProperty());
    }

    @FXML
    public void filter() throws Exception {
        if (selectedFiltersListView.isEmpty()) {
            AlertUtils.showError("Please select a filter before applying!");
            return;
        }

        if (!filterChainHasChanged(isChanged, listOfTraces, currentFiltersForInspector, filtersAndTraceResult, unmodifiedTraces)) {
            AlertUtils.showError("Please make changes to the filter chain before applying!");
            return;
        }

        // If an existing filter was currently being edited, this block aborts the edit process.
        if (FilterCell.getSelectedCell() != null) {
            FilterCell.getSelectedCell().modifyButton.setText("");
            setSelectedCell(null);
            basicFiltersWindowController.addFilterButton.setText("Add filter");
            ltlWindowController.addFilterButton.setText("Add filter");
            basicFiltersWindowController.selectedFilterCategory.set("Add filters");
            stackPaneResults.getChildren().remove(basicFiltersWindowController.getRootRegion());
            stackPaneResults.getChildren().remove(basicFiltersWindowController.getRootRegion());
            basicFiltersWindowController.addFilterButton.setOnAction(event -> basicFiltersWindowController.addFilter());
            ltlWindowController.addFilterButton.setOnAction(event -> ltlWindowController.addLTLFilter());
            applyFilterCheckboxManipulation(addFiltersChoiceBox);
        }

        basicFiltersWindowController.resetUIElements(true);
        applyFilters();
        applyFilterCheckboxManipulation(addFiltersChoiceBox);
    }

    @FXML
    public void importFilters() {
        File selectedJsonFile = FileUtils.showJsonOpenDialog(this.getStage());
        if (selectedJsonFile != null) {
            ImportAgent.importFilters(selectedJsonFile, logFile, attributeAndValue, selectedFiltersListView, appliedFiltersListView, this);
        }
    }

    @FXML
    public void exportFilters() {
        File selectedExportFile = FileUtils.showJsonSaveDialog(this.getStage(), null);
        if (selectedExportFile != null) {
            ExportAgent.exportFilters(selectedExportFile, selectedFiltersListView);
        }
    }

    @FXML
    public void hideFilterSettings() {
        if (stackPaneResults.getChildren().contains(basicFiltersWindowController.getRootRegion())) {
            basicFiltersWindowController.closeWindow();
        } else if (stackPaneResults.getChildren().contains(ltlWindowController.getRootRegion())) {
            ltlWindowController.close();
        }
    }

    public void applyFilters() {
        traceFilteringTask = new TraceFilteringTask();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/ProgressLayer.fxml"));
            Node progressLayer = loader.load();
            ProgressLayerController progressLayerController = loader.getController();
            progressLayerController.getProgressTextLabel().setText("Filtering log...");

            traceFilteringTask.setXLog(FilteringUtils.createLogFromInstances(listOfTraces));
            traceFilteringTask.setFilteringTabController(this);
            updateToggleButtons();

            addHandlersToTask(traceFilteringTask, progressLayer, progressLayerController);
            rootRegion.getChildren().add(progressLayer);
            mainContents.setDisable(true);
            executorService.execute(traceFilteringTask);

        } catch (IOException | IllegalStateException e) {
            AlertUtils.showWarning("Can not load progress layer");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addHandlersToTask(Task<TraceFilteringTaskResult> task, Node progressLayer, ProgressLayerController progressLayerController) {

        progressLayerController.getCancelButton().setOnAction(e -> {
            rootRegion.getChildren().remove(progressLayer);
            mainContents.setDisable(false);
            task.cancel(true);
        });

        task.setOnSucceeded(event -> {
            filtersAndTraceResult = task.getValue().getFilterToResults();
            rootRegion.getChildren().remove(progressLayer);
            resultsViewController.setResult();
            resultsViewController.setLogFile(logFile);
            resultsViewController.setStage(this.getStage());
            mainContents.setDisable(false);

            if (!contentsLayer.getChildren().contains(resultsView)) {
                contentsLayer.getChildren().remove(ltlWindowController.getRootRegion());
                contentsLayer.getChildren().remove(basicFiltersWindowController.getRootRegion());
                contentsLayer.getChildren().add(resultsView);
            }

            task.cancel(true);
        });

        task.setOnFailed(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
                Node alertLayer = loader.load();
                AlertLayerController alertLayerController = loader.getController();
                if (task.getException().getClass().equals(ClassCastException.class)) {
                    alertLayerController.setAlertMessage(AlertLayerController.AlertType.ERROR, "Filtering Log failed.Can't apply follower formula on attributes");
                } else {
                    alertLayerController.setAlertMessage(AlertLayerController.AlertType.ERROR, "Filtering Log failed");
                }
                alertLayerController.getOkButton().setOnAction(onFailedTaskAction(alertLayer));
                rootRegion.getChildren().remove(progressLayer);
                rootRegion.getChildren().add(alertLayer);
            } catch (IOException | IllegalStateException e) {
                AlertUtils.showWarning("Can not load alert layer");
                rootRegion.getChildren().remove(progressLayer);
                mainContents.setDisable(false);
            }
        });
    }

    public void preprocessLog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/ProgressLayer.fxml"));
            Node progressLayer = loader.load();
            ProgressLayerController progressLayerController = loader.getController();
            progressLayerController.getProgressTextLabel().setText("Loading the event log...");

            // Create the task
            logPreprocessingTask = new LogPreprocessingTask();
            logPreprocessingTask.setLogFile(logFile);
            addHandlersToLogPreprocessingTask(logPreprocessingTask, progressLayer, progressLayerController);

            // Start the task
            rootRegion.getChildren().add(progressLayer);
            mainContents.setDisable(true);
            executorService.execute(logPreprocessingTask);
        } catch (Exception e) {
            AlertUtils.showWarning("Loading event log failed");
        }
    }

    private void addHandlersToLogPreprocessingTask(Task<LogPreprocessingTaskResult> task, Node progressLayer, ProgressLayerController progressLayerController) {

        progressLayerController.getCancelButton().setOnAction(e -> {
            rootRegion.getChildren().remove(progressLayer);
            mainContents.setDisable(false);
            task.cancel(true);
        });

        task.setOnSucceeded(event -> {
            this.attributeAndValue = task.getValue().getAttributeAndValue();
            this.xLog = task.getValue().getXLog();

            LinkedList<InstanceModel> newList = FilteringPageController.deepCloneTracesList(LTLFiltersUtils.convertXLogToInstanceModelList(xLog));
            this.listOfTraces = newList;
            this.unmodifiedTraces.setAll(newList);
            mainContents.setDisable(false);
            statisticsContents.setVisible(true);
            rootRegion.getChildren().remove(progressLayer);
        });

        task.setOnFailed(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
                Node alertLayer = loader.load();
                AlertLayerController alertLayerController = loader.getController();
                alertLayerController.setAlertMessage(AlertLayerController.AlertType.ERROR, "Loading event log failed");
                alertLayerController.getOkButton().setOnAction(onFailedTaskAction(alertLayer));

                parametersSection.setDisable(true);
                separator.setDisable(true);

                rootRegion.getChildren().remove(progressLayer);
                rootRegion.getChildren().add(alertLayer);
            } catch (IOException | IllegalStateException e) {
                //If alert layer can not be displayed then use regular alert instead
                AlertUtils.showWarning("Loading event log failed");
                rootRegion.getChildren().remove(progressLayer);
                stackPaneResults.setDisable(false);
            }
        });
    }

    public void updateToggleButtons() {
        if (selectedFiltersListView.stream().anyMatch(filter -> filter.getFilteringMode().equals("keepSelectedMode") || filter.getFilteringMode().equals("trimToTimeframe") || filter.getFilteringMode().equals("trimFirst") || filter.getFilteringMode().equals("trimLongest"))) {
            resultsViewController.traceInspector.applyEventFiltersToggle.setDisable(false);
            resultsViewController.filtersInspector.applyEventFiltersToggle.setDisable(false);
        } else {
            resultsViewController.traceInspector.applyEventFiltersToggle.setDisable(true);
            resultsViewController.filtersInspector.applyEventFiltersToggle.setDisable(true);
        }
    }

    public void resetMatchingRatio(ObservableList<InstanceModel> unmodifiedTraces) {
        unmodifiedTraces.forEach((InstanceModel trace) -> trace.setHealthDegree(0));
    }

    private ListChangeListener<Filter> getFilterListChangeListener() {
        return change -> {
            boolean isEmptyChain = appliedFiltersListView.getItems().isEmpty();
            filteringIntroLabel.setVisible(isEmptyChain);
            filteringIntroLabel.setManaged(isEmptyChain);
            appliedFiltersListView.refresh();
        };
    }

    private boolean filterChainHasChanged(AtomicBoolean isChanged, LinkedList<InstanceModel> listOfTraces, LinkedList<RuleModel> currentFiltersForInspector, List<Map<RuleModel, LinkedList<InstanceModel>>> filtersAndTraceResult, ObservableList<InstanceModel> unmodifiedTraces) {
        if (currentFiltersForInspector.isEmpty() | isChanged.get()) {
            listOfTraces.clear();
            listOfTraces.addAll(new LinkedList<>(unmodifiedTraces));
            filtersAndTraceResult.clear();
            currentFiltersForInspector.clear();
            resetMatchingRatio(unmodifiedTraces);
            isChanged.set(false);
            return true;
        }
        return false;
    }

    private ListChangeListener<Filter> getFilterListChangeListener(AtomicBoolean isChanged) {
        return change -> {
            while (change.next()) {
                if (change.wasUpdated() || change.wasAdded() || change.wasReplaced() || change.wasPermutated() || change.wasRemoved()) {
                    isChanged.set(true);
                }
            }
        };
    }

    public void applyFilterCheckboxManipulation(ChoiceBox<String> addFiltersChoiceBox) {
        addFiltersChoiceBox.getItems().removeIf(filter -> filter.equals("Add filters"));
        addFiltersChoiceBox.getItems().add(0, "Add filters");
        addFiltersChoiceBox.getSelectionModel().selectFirst();
    }

    private ListChangeListener<InstanceModel> getInstanceModelListChangeListener(ObservableList<InstanceModel> unmodifiedTraces) {
        return list -> StatisticsUtils.updateStatistics(new LinkedList<>(unmodifiedTraces), this);
    }

    private EventHandler<ActionEvent> onFailedTaskAction(Node alertLayer) {
        return e -> {
            rootRegion.getChildren().remove(alertLayer);
            mainContents.setDisable(false);
        };
    }

    protected void setLog(File logFile) {
        this.logFile = logFile;
    }
}