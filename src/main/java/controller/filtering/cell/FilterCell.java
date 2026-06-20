package controller.filtering.cell;

import controller.filtering.Filter;
import controller.filtering.FilteringTabController;
import controller.filtering.BasicFiltersWindowController;
import controller.filtering.LTLWindowController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.util.converter.NumberStringConverter;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.stream.IntStream;


public class FilterCell extends ListCell<Filter> {

    @FXML
    protected Label filterDescription;
    @FXML
    public Button modifyButton;
    @FXML
    protected Button deleteButton;
    @FXML
    protected Button moveDownButton;
    @FXML
    protected Button moveUpButton;
    @FXML
    protected VBox rootRegion;
    private FXMLLoader loader;
    public FilteringTabController filteringTabController;
    private static FilterCell selectedCell;

    public FilterCell(FilteringTabController filteringTabController) {
        this.filteringTabController = filteringTabController;
        initializeDragAndDropHandlers();
        handleDoubleClick();
    }

    public static FilterCell getSelectedCell() {
        return selectedCell;
    }

    public static void setSelectedCell(FilterCell cell) {
        selectedCell = cell;
    }

    public void initializeDragAndDropHandlers() {
        setOnDragDetected(this::handleDragDetected);
        setOnDragOver(this::handleDragOver);
        setOnDragEntered(this::handleDragEntered);
        setOnDragExited(event -> setStyle(""));
        setOnDragDropped(this::handleDragDropped);
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();

        if (getItem() == null) {
            return;
        }

        boolean success = false;

        if (dragboard.hasString()) {
            int selectedItemIndex = getListView().getSelectionModel().getSelectedIndex();
            int draggedIndex = getListView().getItems().indexOf(getItem());

            // Reorder the items
            Collections.swap(filteringTabController.selectedFiltersListView, draggedIndex, selectedItemIndex);
            getListView().refresh();
            setSelectedCell(null);
            selectAndScrollTo(draggedIndex);
            filteringTabController.isChanged.set(true);
            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    private void handleDragEntered(DragEvent event) {
        final String HIGHLIGHT_COLOR = "-fx-background-color: lightgray;";
        if (event.getGestureSource() != this && event.getDragboard().hasString()) {
            setStyle(HIGHLIGHT_COLOR);
        }
    }

    private void handleDragOver(DragEvent event) {
        Dragboard dragboard = event.getDragboard();

        if (event.getGestureSource() != this && dragboard.hasString()) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
        event.consume();
    }

    private void handleDragDetected(MouseEvent event) {
        if (getItem() == null) {
            return;
        }

        Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();
        content.putString(getItem().toString());
        dragboard.setContent(content);
        event.consume();
    }

    @Override
    protected void updateItem(Filter item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (loadFxml()) {
                filterDescription.setText(item.toString());
                modifyButton.setOnAction(event -> editFilter(item));
                deleteButton.setOnAction(event -> deleteRow());
                moveUpButton.setOnAction(event -> changeFilterPosition(-1));
                moveDownButton.setOnAction(event -> changeFilterPosition(1));
            }
            setText(null);
            setGraphic(rootRegion);
        }
    }

    private void handleDoubleClick() {
        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                // Handle double-click action (edit the filter)
                if (getItem() != null) {
                    editFilter(getItem());
                }
            }
        });
    }

    public void editFilter(Filter filterToEdit) {

        // Reset the 'edited' text in the previous selected cell, if any
        if (selectedCell != null && selectedCell != this) {
            selectedCell.modifyButton.setText("");
        }

        // If the 'edit' button is pressed multiple times on the same cell, reset the editing state
        if (selectedCell == this) {
            resetFilterEditingState();
            return;
        }

        filteringTabController.appliedFiltersListView.getSelectionModel().select(filterToEdit);
        filteringTabController.basicFiltersWindowController.addFilterButton.setOnAction(null);
        filteringTabController.ltlWindowController.addFilterButton.setOnAction(null);

        selectedCell = this;    // Set the current cell as the selected cell for edit
        selectedCell.modifyButton.setText("Editing");   // Update the button text for the current cell

        // Update the text of add filter buttons to indicate "Update filter"
        filteringTabController.basicFiltersWindowController.addFilterButton.setText("Update filter");
        filteringTabController.ltlWindowController.addFilterButton.setText("Update filter");

        // Based on the filter category, handle different types of filter editing
        switch (filterToEdit.getCategory()) {
            case "Attribute": {
                handleAttributeFilterEditing(filterToEdit);
                break;
            }
            case "Performance": {
                handlePerformanceFilterEditing(filterToEdit);
                break;
            }
            case "Endpoints": {
                handleEndpointsEditing(filterToEdit);
                break;
            }
            case "Follower": {
                handleFollowerFilterEditing(filterToEdit);
                break;
            }
            case "Timeframe": {
                handleTimeframeFilterEditing(filterToEdit);
                break;
            }
            default: { // LTL
                handleLTLFilterEditing(filterToEdit);
                break;
            }
        }

        // Set the actions for add filter buttons to perform the appropriate edit actions
        filteringTabController.basicFiltersWindowController.addFilterButton.setOnAction(event -> filteringTabController.basicFiltersWindowController.editFilter(filterToEdit));
        filteringTabController.ltlWindowController.addFilterButton.setOnAction(event -> filteringTabController.ltlWindowController.parseLTLFilter(filterToEdit, null, null, true));
    }


    private void handleAttributeFilterEditing(Filter filter) {
        filteringTabController.addFiltersChoiceBox.getSelectionModel().select(filter.getCategory());
        filteringTabController.filtersWindowService.openBasicFiltersWindow(filter.getCategory());
        filteringTabController.basicFiltersWindowController.choiceBoxAttributeEndpointFollower.getSelectionModel().select(filter.getAttribute());
        ToggleButton toggleButton = (ToggleButton) filteringTabController.basicFiltersWindowController.toggleGroupAttribute.getToggles().stream().filter(t -> ((RadioButton) t).getId().equals(filter.getFilteringMode())).findFirst().orElse(null);
        filteringTabController.basicFiltersWindowController.toggleGroupAttribute.selectToggle(toggleButton);
        filteringTabController.basicFiltersWindowController.listViewAttribute.getSelectionModel().clearSelection();
        filteringTabController.basicFiltersWindowController.listViewAttribute.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        filter.getStartValue().forEach((String currentStartValue) -> filteringTabController.basicFiltersWindowController.listViewAttribute.getSelectionModel().select(currentStartValue));
    }

    private void handlePerformanceFilterEditing(Filter filter) {
        filteringTabController.addFiltersChoiceBox.getSelectionModel().select(filter.getCategory());
        filteringTabController.filtersWindowService.openBasicFiltersWindow(filter.getCategory());
        filteringTabController.basicFiltersWindowController.choiceBoxPerformance.getSelectionModel().select(filter.getFilteringMode());

        if (filteringTabController.basicFiltersWindowController.choiceBoxPerformance.getSelectionModel().getSelectedItem().equals("Trace duration")) {
            filteringTabController.basicFiltersWindowController.setupPerformanceSliders(filteringTabController.basicFiltersWindowController.minimumDuration, filteringTabController.basicFiltersWindowController.maximumDuration, "Minimum duration", "Maximum duration", BasicFiltersWindowController.getConverter());
        } else {
            filteringTabController.basicFiltersWindowController.setupPerformanceSliders(filteringTabController.basicFiltersWindowController.minimumNumberOfEvents, filteringTabController.basicFiltersWindowController.maximumNumberOfEvents, "Minimum number of events", "Maximum number of events", new NumberStringConverter(NumberFormat.getIntegerInstance()));
        }
        filteringTabController.basicFiltersWindowController.setSliderValue(filteringTabController.basicFiltersWindowController.sliderMinimum, Double.parseDouble(filter.getStartValue().get(0)));
        filteringTabController.basicFiltersWindowController.setSliderValue(filteringTabController.basicFiltersWindowController.sliderMaximum, Double.parseDouble(filter.getEndValue().get(0)));

    }

    private void handleEndpointsEditing(Filter filter) {
        filteringTabController.addFiltersChoiceBox.getSelectionModel().select(filter.getCategory());
        filteringTabController.filtersWindowService.openBasicFiltersWindow(filter.getCategory());
        filteringTabController.basicFiltersWindowController.choiceBoxAttributeEndpointFollower.getSelectionModel().select(filter.getAttribute());
        ToggleButton toggleButton = (ToggleButton) filteringTabController.basicFiltersWindowController.toggleGroupEndpoints.getToggles().stream().filter(t -> ((RadioButton) t).getId().equals(filter.getFilteringMode())).findFirst().orElse(null);
        filteringTabController.basicFiltersWindowController.toggleGroupEndpoints.selectToggle(toggleButton);
        filteringTabController.basicFiltersWindowController.listViewInitialEndpoints.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        filteringTabController.basicFiltersWindowController.listViewEndEndpoints.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        filter.getStartValue().forEach((String currentStartValue) -> filteringTabController.basicFiltersWindowController.listViewInitialEndpoints.getSelectionModel().select(currentStartValue));
        filter.getEndValue().forEach((String currentEndValue) -> filteringTabController.basicFiltersWindowController.listViewEndEndpoints.getSelectionModel().select(currentEndValue));
    }

    private void handleFollowerFilterEditing(Filter filter) {
        filteringTabController.addFiltersChoiceBox.getSelectionModel().select(filter.getCategory());
        filteringTabController.basicFiltersWindowController.choiceBoxAttributeEndpointFollower.getSelectionModel().select(filter.getAttribute());
        ToggleButton toggleButton = (ToggleButton) filteringTabController.basicFiltersWindowController.toggleGroupFollower.getToggles().stream().filter(t -> ((RadioButton) t).getId().equals(filter.getFilteringMode())).findFirst().orElse(null);
        filteringTabController.basicFiltersWindowController.toggleGroupFollower.selectToggle(toggleButton);
        filteringTabController.basicFiltersWindowController.listViewInitialFollower.getSelectionModel().clearSelection();
        filteringTabController.basicFiltersWindowController.listViewEndFollower.getSelectionModel().clearSelection();
        filteringTabController.basicFiltersWindowController.listViewInitialFollower.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        filteringTabController.basicFiltersWindowController.listViewEndFollower.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        filter.getStartValue().forEach((String currentStartValue) -> filteringTabController.basicFiltersWindowController.listViewInitialFollower.getSelectionModel().select(currentStartValue));
        filter.getEndValue().forEach((String currentEndValue) -> filteringTabController.basicFiltersWindowController.listViewEndFollower.getSelectionModel().select(currentEndValue));
    }

    private void handleTimeframeFilterEditing(Filter filter) {
        filteringTabController.addFiltersChoiceBox.getSelectionModel().select(filter.getCategory());
        filteringTabController.filtersWindowService.openBasicFiltersWindow(filter.getCategory());
        ToggleButton toggleButton = (ToggleButton) filteringTabController.basicFiltersWindowController.toggleGroupTimeframe.getToggles().stream().filter(t -> ((RadioButton) t).getId().equals(filter.getFilteringMode())).findFirst().orElse(null);
        filteringTabController.basicFiltersWindowController.toggleGroupTimeframe.selectToggle(toggleButton);
        filteringTabController.basicFiltersWindowController.dateTimeStart.setDateTimeValue(LocalDateTime.parse(filter.getStartValue().get(0), DateTimeFormatter.ofPattern(filteringTabController.basicFiltersWindowController.dateFormat)));
        filteringTabController.basicFiltersWindowController.dateTimeEnd.setDateTimeValue(LocalDateTime.parse(filter.getEndValue().get(0), DateTimeFormatter.ofPattern(filteringTabController.basicFiltersWindowController.dateFormat)));
    }

    private void handleLTLFilterEditing(Filter filter) {
        filteringTabController.contentsLayer.getChildren().remove(filteringTabController.ltlWindowController.getRootRegion());
        filteringTabController.contentsLayer.getChildren().remove(filteringTabController.basicFiltersWindowController.getRootRegion());

        filteringTabController.filtersWindowService.openLTLFiltersWindow();
        filteringTabController.addFiltersChoiceBox.getSelectionModel().select("Ltl formulas");
        for (LTLWindowController.Item current : filteringTabController.ltlWindowController.formulasListView.getItems()) {
            if (current.getName().equals(filter.getFilteringMode().substring(0, filter.getFilteringMode().indexOf(" ")))) {// will select only 1
                filteringTabController.ltlWindowController.formulasListView.getSelectionModel().select(current);
                current.setOn(true);
                filteringTabController.ltlWindowController.formulasListView.scrollTo(current);
                IntStream.range(0, current.getParameters().size()).forEach(i -> current.getParameters().get(i).setValue(filter.getStartValue().get(i)));
                filteringTabController.ltlWindowController.formulasListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                filteringTabController.ltlWindowController.vboxFormulaInfo.setVvalue(0.0); // to scroll at the beginning of the scroll-pane
                filteringTabController.ltlWindowController.filterEditON = true;
                break;
            }
        }
    }

    public void resetFilterEditingState() {
        selectedCell.modifyButton.setText("");
        FilterCell.setSelectedCell(null);
        filteringTabController.basicFiltersWindowController.addFilterButton.setText("Add filter");
        filteringTabController.ltlWindowController.addFilterButton.setText("Add filter");
        filteringTabController.basicFiltersWindowController.selectedFilterCategory.set("Add filters");
        filteringTabController.stackPaneResults.getChildren().remove(filteringTabController.basicFiltersWindowController.getRootRegion());
        filteringTabController.stackPaneResults.getChildren().remove(filteringTabController.basicFiltersWindowController.getRootRegion());
        filteringTabController.basicFiltersWindowController.addFilterButton.setOnAction(event -> filteringTabController.basicFiltersWindowController.addFilter());
        filteringTabController.ltlWindowController.addFilterButton.setOnAction(event -> filteringTabController.ltlWindowController.addLTLFilter());
        filteringTabController.applyFilterCheckboxManipulation(filteringTabController.addFiltersChoiceBox);
    }

    private void changeFilterPosition(int moveBy) {

        // Check if the row to be moved was being edited, and reset the editing state if necessary
        if (selectedCell != null) {
            resetFilterEditingState();
        }

        int currentPosition = this.getIndex();
        int newPosition = (this.getListView().getItems().size() + currentPosition + moveBy) % this.getListView().getItems().size();
        updateListView(currentPosition, newPosition);
        selectAndScrollTo(newPosition);
    }

    private void deleteRow() {

        // Check if the removed row was being edited, and reset the editing state if necessary
        if (selectedCell == this) {
            resetFilterEditingState();
        }

        int index = getListView().getItems().indexOf(getItem());
        this.getListView().getItems().remove(getIndex());
        filteringTabController.selectedFiltersListView.setAll(this.getListView().getItems());
        int count = getListView().getItems().size(); // select the nearest element in listview

        if (index < count) {
            selectAndScrollTo(index);
        } else if (count > 0) {
            selectAndScrollTo(index - 1);
        } else {
            // No items left in the listview
        }
    }

    private void updateListView(int oldIndex, Integer newIndex) {
        if (newIndex != null) {
            Collections.swap(filteringTabController.selectedFiltersListView, oldIndex, newIndex);
        }
        filteringTabController.selectedFiltersListView.setAll(this.getListView().getItems());
    }

    private void selectAndScrollTo(int index) {
        getListView().getSelectionModel().select(index);
        getListView().scrollTo(index);
    }

    private boolean loadFxml() {
        if (loader == null) {
            loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/filtering/FilterCell.fxml"));
            loader.setController(this);
            try {
                loader.load();
                return true;
            } catch (IOException | IllegalStateException e) {
                return false;
            }
        } else {
            return true;
        }
    }
}