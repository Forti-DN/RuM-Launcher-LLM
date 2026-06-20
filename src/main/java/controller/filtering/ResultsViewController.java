package controller.filtering;

import controller.common.AbstractController;
import controller.common.eventcell.EventData;
import global.Inventory;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.processmining.plugins.log.exporting.ExportLogXes;
import org.processmining.plugins.ltlchecker.InstanceModel;
import util.AlertUtils;
import util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ResultsViewController extends AbstractController {
    @FXML
    VBox mainContents;
    @FXML
    ChoiceBox<String> perspectiveChoice;
    public FilterInspectorController filtersInspector;
    public TraceInspectorController traceInspector;
    private FilteringTabController filteringTabController;
    private Region filtersInspectorView;
    private Region traceInspectorView;
    private File logFile;

    @FXML
    private void initialize() throws IOException {
        FXMLLoader filtersInspectorLoader = new FXMLLoader(getClass().getClassLoader().getResource("pages/filtering/FilterInspector.fxml"));
        filtersInspectorView = filtersInspectorLoader.load();
        filtersInspector = filtersInspectorLoader.getController();

        FXMLLoader traceInspectorLoader = new FXMLLoader(getClass().getClassLoader().getResource("pages/filtering/TraceInspector.fxml"));
        traceInspectorView = traceInspectorLoader.load();
        traceInspector = traceInspectorLoader.getController();

        initializePerspectiveChoice();
        perspectiveChoice.getSelectionModel().selectedItemProperty().addListener(getPerspectiveChangeListener());
        mainContents.getChildren().add(filtersInspectorView);

        VBox.setVgrow(filtersInspectorView, Priority.ALWAYS);
        VBox.setVgrow(traceInspectorView, Priority.ALWAYS);
    }

    @FXML
    public void exportFulfilledTraces() throws Exception {
        File chosenFile = FileUtils.showXesSaveDialog(this.getStage(), null);
        if (chosenFile != null) {
            ExportLogXes.export(FilteringUtils.createLogFromInstances(new LinkedList<>(filtersInspector.tracesListView.getItems())), chosenFile);
        }
    }

    @FXML
    public void exportViolatedTraces() throws Exception {
        File chosenFile = FileUtils.showXesSaveDialog(this.getStage(), null);
        if (chosenFile != null) {
            ExportLogXes.export(FilteringUtils.getViolatedTraces(new LinkedList<>(filtersInspector.tracesListView.getItems()), filteringTabController.logFile), chosenFile);
        }
    }

    @FXML
    public void takeSnapshot() throws IOException {
        String suggestedName = logFile.getName().contains(".") ? logFile.getName().substring(0, logFile.getName().lastIndexOf(".")) + " snap" : logFile.getName() + " snap";
        TextInputDialog dialog = new TextInputDialog(suggestedName);
        dialog.setTitle("Save snapshot of event log");
        dialog.setHeaderText("Save snapshot of event log");
        dialog.setContentText("Event log name:");
        dialog.getDialogPane().setMinWidth(500.0);

        dialog.getDialogPane().getStylesheets().add("main.css");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Okay");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        (dialog.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("small-button");
        (dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("small-button");

        Optional<String> fileName = dialog.showAndWait();

        // check is saving was canceled
        if (fileName.isPresent()) {
            //Adding .xes extension because otherwise XLogReader.openLog() will fail when the snapshot is loaded
            File chosenFile = new File(File.createTempFile("prefix", "suffix").getParentFile(), fileName.get() + ".xes");
            try {
                ExportLogXes.export(FilteringUtils.createLogFromInstances(new LinkedList<>(filtersInspector.tracesListView.getItems())), chosenFile);
            } catch (IOException e) {
                AlertUtils.showError("Exporting the log failed!");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            InventorySavedElement inventorySavedElement = new InventorySavedElement(chosenFile, new Date(), fileName.get(), InventoryElementTypeEnum.EVENT_LOG);
            Inventory.storeEventLogSnapshot(inventorySavedElement);
        }
    }


    private ChangeListener<String> getPerspectiveChangeListener() {
        return (observable, oldPerspective, newPerspective) -> {
            mainContents.getChildren().set(1, "Filter Inspector".equals(newPerspective) ? filtersInspectorView : traceInspectorView);
            if (!"Filter Inspector".equals(newPerspective)) {
                traceInspector.updateTraceData();
                filtersInspector.showPayloadsToggle.setSelected(false);
                filtersInspector.applyEventFiltersToggle.setSelected(false);
                filtersInspector.filtersListView.getSelectionModel().selectFirst();
                filtersInspector.tracesListView.getSelectionModel().selectFirst();
                filtersInspector.filtersListView.scrollTo(filtersInspector.filtersListView.getSelectionModel().getSelectedIndex());
            } else {
                traceInspector.applyEventFiltersToggle.setSelected(false);
                traceInspector.showPayloadsToggle.setSelected(false);
            }
        };
    }

    protected void initializePerspectiveChoice() {
        perspectiveChoice.setItems(FXCollections.observableList(Arrays.asList("Filter Inspector", "Trace Inspector")));
        perspectiveChoice.setValue("Filter Inspector");
    }

    public void setResult() {
        traceInspector.setFilteringTabController(filteringTabController);
        filtersInspector.setFilteringTabController(filteringTabController);
        traceInspector.updateTraceData();
        filtersInspector.updateFiltersData();
        filtersInspector.updateTraceData();
        traceInspector.tracesListView.refresh();
    }

    protected static List<EventData> getEventData(InstanceModel selectedTrace) {
        Map<String, String> payload;
        XTrace trace = selectedTrace.getInstance();
        List<EventData> events = new LinkedList<>();
        for (int eventIndex = 0; eventIndex < trace.size(); eventIndex++) {
            payload = new LinkedHashMap<>();
            EventData event = new EventData();
            XAttributeMap attributes = trace.get(eventIndex).getAttributes();
            event.setEventNumber(eventIndex+1);
            event.setConceptName(attributes.get("concept:name").toString());
            event.setTimeTimestamp(((XAttributeTimestampImpl) attributes.get("time:timestamp")).getValue());
            for (String key : attributes.keySet()) {
                if (!(key.equals("concept:name") || key.equals("time:timestamp"))) {
                    payload.put(key, attributes.get(key).toString());
                }
            }
            event.setPayload(payload);
            events.add(event);
        }
        return events;
    }

    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }

    protected void setLtlCheckerTabController(FilteringTabController filteringTabController) {
        this.filteringTabController = filteringTabController;
    }
}