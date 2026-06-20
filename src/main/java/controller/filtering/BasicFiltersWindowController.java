package controller.filtering;

import controller.common.AbstractController;
import controller.filtering.cell.FilterCell;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import javafx.util.converter.NumberStringConverter;
import tornadofx.control.DateTimePicker;
import util.AlertUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class BasicFiltersWindowController extends AbstractController {
    @FXML
    public AnchorPane allPane;
    @FXML
    public AnchorPane anchorPaneTimeframe;
    @FXML
    public AnchorPane anchorPaneAttribute;
    @FXML
    public AnchorPane anchorPaneEndpoints;
    @FXML
    public AnchorPane anchorPanePerformance;
    @FXML
    public AnchorPane anchorPaneFollower;
    @FXML
    public ToggleGroup toggleGroupAttribute;
    @FXML
    public ToggleGroup toggleGroupFollower;
    @FXML
    public ToggleGroup toggleGroupEndpoints;
    @FXML
    public ToggleGroup toggleGroupTimeframe;
    @FXML
    public ChoiceBox<String> choiceBoxAttributeEndpointFollower;
    @FXML
    public ChoiceBox<String> choiceBoxPerformance;
    @FXML
    public Label currentMaximum;
    @FXML
    public Label currentMinimum;
    @FXML
    public Label endEventValuesLabel;
    @FXML
    public Label endEventValuesLabelFollower;
    @FXML
    public Label maximumLabel;
    @FXML
    public Label minimumLabel;
    @FXML
    public Label nodeValues;
    @FXML
    public Label filterByLabel;
    @FXML
    public Label filteringModeLabel;
    @FXML
    public Label startEventValuesLabel;
    @FXML
    public Label startEventValuesLabelFollower;
    @FXML
    public Slider sliderMaximum;
    @FXML
    public Slider sliderMinimum;
    @FXML
    public ListView<String> listViewAttribute;
    @FXML
    public ListView<String> listViewInitialEndpoints;
    @FXML
    public ListView<String> listViewEndEndpoints;
    @FXML
    public ListView<String> listViewInitialFollower;
    @FXML
    public ListView<String> listViewEndFollower;
    @FXML
    public DateTimePicker dateTimeEnd;
    @FXML
    public DateTimePicker dateTimeStart;
    @FXML
    public Button addFilterButton;
    @FXML
    private Button clearAllButton;
    @FXML
    private Button selectAllButton;
    public long minimumNumberOfEvents;
    public long maximumNumberOfEvents;
    public double minimumDuration;
    public double maximumDuration;
    public final String dateFormat = "dd.MM.yyyy HH:mm:ss";
    public SimpleStringProperty selectedFilterCategory = new SimpleStringProperty();
    protected FilteringTabController filteringTabController;

    @FXML
    private void initialize() {
        selectedFilterCategory.addListener(getStringChangeListener());
        dateTimeEnd.setDayCellFactory(createDateCellFactory(false));
        dateTimeStart.setDayCellFactory(createDateCellFactory(true));
        setEscapeKeyHandler_RecursiveTreeTraversal(allPane);
        choiceBoxAttributeEndpointFollower.setViewOrder(-1); //Otherwise it seems to get blocked by some other element TODO: Figure out why

        //TODO: rootRegion should be restructured to be in-line with other slide-in panels
        //Avoids closing the layer when user clicks on an empty area of the settings region
        allPane.setOnMouseClicked(Event::consume);
    }

    @FXML
    public void clearAll() {
        if (anchorPaneAttribute.isVisible()) {
            listViewAttribute.getSelectionModel().clearSelection();
            return;
        }
        if (anchorPaneEndpoints.isVisible()) {
            listViewInitialEndpoints.getSelectionModel().clearSelection();
            listViewEndEndpoints.getSelectionModel().clearSelection();
            return;
        }
        listViewInitialFollower.getSelectionModel().clearSelection();
        listViewEndFollower.getSelectionModel().clearSelection();
    }

    @FXML
    public void selectAll() {
        if (anchorPaneAttribute.isVisible()) {
            listViewAttribute.getSelectionModel().selectAll();
            return;
        }
        if (anchorPaneEndpoints.isVisible()) {
            listViewInitialEndpoints.getSelectionModel().selectAll();
            listViewEndEndpoints.getSelectionModel().selectAll();
            return;
        }
        listViewInitialFollower.getSelectionModel().selectAll();
        listViewEndFollower.getSelectionModel().selectAll();
    }

    @FXML
    public void closeWindow() {
        if (!filteringTabController.addFiltersChoiceBox.getItems().contains("Add filters")) {
            filteringTabController.addFiltersChoiceBox.getItems().add(0, "Add filters");
            this.selectedFilterCategory.set("Add filters");
        }
        filteringTabController.addFiltersChoiceBox.getSelectionModel().selectFirst();
        resetUIElements(false);
        filteringTabController.contentsLayer.getChildren().remove(this.getRootRegion());
    }

    @FXML
    public void addFilter() {
        Filter filterToAdd = new Filter();
        String filterCategory = filteringTabController.addFiltersChoiceBox.getValue();
        try {
            switch (filterCategory) {
                case "Attribute": {
                    parseAttributeFilter(filterToAdd, filterCategory, true);
                    break;
                }
                case "Performance": {
                    parsePerformanceFilter(filterToAdd, filterCategory, true);
                    break;
                }
                case "Endpoints": {
                    parseEndpointsFilter(filterToAdd, filterCategory, true);
                    break;
                }
                case "Follower": {
                    parseFollowerFilter(filterToAdd, filterCategory, true);
                    break;
                }
                case "Timeframe": {
                    parseTimeframeFilter(filterToAdd, filterCategory, true);
                    break;
                }
            }
        } catch (Throwable e) {// to double-check

        }

    }

    public void createAndSelectFilter(Filter newFilter) {
        filteringTabController.selectedFiltersListView.add(newFilter);
        filteringTabController.appliedFiltersListView.getSelectionModel().select(newFilter);
        filteringTabController.appliedFiltersListView.scrollTo(newFilter);
    }

    public void editFilter(Filter filter) {
        switch (selectedFilterCategory.getValue()) {
            case "Attribute": {
                parseAttributeFilter(filter, selectedFilterCategory.getValue(), false);
                break;
            }
            case "Performance": {
                parsePerformanceFilter(filter, selectedFilterCategory.getValue(), false);
                break;
            }
            case "Endpoints": {
                parseEndpointsFilter(filter, selectedFilterCategory.getValue(), false);
                break;
            }
            case "Follower": {
                parseFollowerFilter(filter, selectedFilterCategory.getValue(), false);
                break;
            }
            case "Timeframe": {
                parseTimeframeFilter(filter, selectedFilterCategory.getValue(), false);
                break;
            }
        }
        filteringTabController.isChanged.set(true);
    }

    public void parseAttributeFilter(Filter newFilter, String filterCategory, boolean isFilterToAdd) {

        ArrayList<String> selectedItemsInListView = new ArrayList<>(listViewAttribute.getSelectionModel().getSelectedItems());

        boolean isAnyFieldEmpty = selectedItemsInListView.isEmpty();
        if (isAnyFieldEmpty) {
            AlertUtils.showWarning("Please make sure to select all the required fields!");

        } else {
            newFilter.setCategory(filterCategory);
            newFilter.setAttribute(choiceBoxAttributeEndpointFollower.getSelectionModel().selectedItemProperty().getValue());
            newFilter.setFilteringMode(((RadioButton) toggleGroupAttribute.getSelectedToggle()).getId());
            newFilter.setStartValue(selectedItemsInListView);
            newFilter.setFilterType(newFilter.getFilteringMode().equals("keepSelectedMode") ? Filter.FilterType.EVENT_FILTER : Filter.FilterType.TRACE_FILTER);

            handleFilterAction(newFilter, isFilterToAdd);

            filteringTabController.appliedFiltersListView.refresh();
        }
    }

    public void parsePerformanceFilter(Filter newFilter, String filterCategory, boolean isFilterToAdd) {
        newFilter.setCategory(filterCategory);
        newFilter.setFilteringMode(choiceBoxPerformance.getSelectionModel().getSelectedItem());

        if (choiceBoxPerformance.getSelectionModel().getSelectedItem().equals("Number of events")) {
            newFilter.setStartValue(Collections.singletonList(Integer.toString((int) Math.round(sliderMinimum.getValue()))));
            newFilter.setEndValue(Collections.singletonList(Integer.toString((int) Math.round(sliderMaximum.getValue()))));
        } else {
            DecimalFormat df = new DecimalFormat("#.###");
            newFilter.setStartValue(Collections.singletonList(df.format(sliderMinimum.getValue())));
            newFilter.setEndValue(Collections.singletonList(df.format(sliderMaximum.getValue())));
        }

        // If 'isFilterToAdd' is true (meaning the action is 'add'), then create and select the filter
        handleFilterAction(newFilter, isFilterToAdd);
        filteringTabController.appliedFiltersListView.refresh();
    }

    public void parseEndpointsFilter(Filter newFilter, String filterCategory, boolean isFilterToAdd) {

        ArrayList<String> selectedStartEventAttributeValues = new ArrayList<>(listViewInitialEndpoints.getSelectionModel().getSelectedItems());
        ArrayList<String> selectedEndEventAttributeValues = new ArrayList<>(listViewEndEndpoints.getSelectionModel().getSelectedItems());

        if (selectedStartEventAttributeValues.isEmpty() && selectedEndEventAttributeValues.isEmpty()) {
            AlertUtils.showWarning("Please make sure to select all the required fields!");
        } else {
            newFilter.setCategory(filterCategory);
            newFilter.setAttribute(choiceBoxAttributeEndpointFollower.getSelectionModel().selectedItemProperty().getValue());
            newFilter.setFilteringMode(((RadioButton) toggleGroupEndpoints.getSelectedToggle()).getId());
            newFilter.setStartValue(selectedStartEventAttributeValues);
            newFilter.setEndValue(selectedEndEventAttributeValues);
            newFilter.setFilterType((newFilter.getFilteringMode().equals("trimLongest") || newFilter.getFilteringMode().equals("trimFirst")) ? Filter.FilterType.EVENT_FILTER : Filter.FilterType.TRACE_FILTER);

            // If 'isFilterToAdd' is true (meaning the action is 'add'), then create and select the filter; otherwise, if the action was 'edit', do nothing.
            handleFilterAction(newFilter, isFilterToAdd);

            filteringTabController.appliedFiltersListView.refresh();
        }
    }

    public void parseFollowerFilter(Filter newFilter, String filterCategory, boolean isFilterToAdd) {

        ArrayList<String> selectedStartEventAttributeValues = new ArrayList<>(listViewInitialFollower.getSelectionModel().getSelectedItems());
        ArrayList<String> selectedEndEventAttributeValues = new ArrayList<>(listViewEndFollower.getSelectionModel().getSelectedItems());

        if (selectedStartEventAttributeValues.isEmpty() && selectedEndEventAttributeValues.isEmpty()) {
            AlertUtils.showWarning("Please make sure to select all the required fields!");
        } else {
            newFilter.setCategory(filterCategory);
            newFilter.setAttribute(choiceBoxAttributeEndpointFollower.getSelectionModel().selectedItemProperty().getValue());
            newFilter.setFilteringMode(((RadioButton) toggleGroupFollower.getSelectedToggle()).getId());
            newFilter.setStartValue(selectedStartEventAttributeValues);
            newFilter.setEndValue(selectedEndEventAttributeValues);

            // If 'isFilterToAdd' is true (meaning the action is 'add'), then create and select the filter; otherwise, if the action was 'edit', do nothing.
            handleFilterAction(newFilter, isFilterToAdd);

            filteringTabController.appliedFiltersListView.refresh();
        }
    }

    public void parseTimeframeFilter(Filter newFilter, String filterCategory, boolean isFilterToAdd) {

        ArrayList<String> selectedStartEventAttributeValues = new ArrayList<>(Collections.singleton(dateTimeStart.getDateTimeValue().format(DateTimeFormatter.ofPattern(dateFormat))));
        ArrayList<String> selectedEndEventAttributeValues = new ArrayList<>(Collections.singleton(dateTimeEnd.getDateTimeValue().format(DateTimeFormatter.ofPattern(dateFormat))));

        if (dateTimeStart.getDateTimeValue() == null || dateTimeEnd.getDateTimeValue() == null) {
            AlertUtils.showWarning("Please make sure to select all the required fields!");
        } else {
            newFilter.setCategory(filterCategory);
            newFilter.setFilteringMode(((RadioButton) toggleGroupTimeframe.getSelectedToggle()).getId());
            newFilter.setStartValue(selectedStartEventAttributeValues);
            newFilter.setEndValue(selectedEndEventAttributeValues);
            newFilter.setFilterType(newFilter.getFilteringMode().equals("trimToTimeframe") ? Filter.FilterType.EVENT_FILTER : Filter.FilterType.TRACE_FILTER);

            // If 'isFilterToAdd' is true (meaning the action is 'add'), then create and select the filter; otherwise, if the action was 'edit', do nothing.
            handleFilterAction(newFilter, isFilterToAdd);

            filteringTabController.appliedFiltersListView.refresh();
        }
    }

    private void resetFilterEditingState() {
        filteringTabController.basicFiltersWindowController.addFilterButton.setText("Add filter");
        filteringTabController.ltlWindowController.addFilterButton.setText("Add filter");
        FilterCell.getSelectedCell().modifyButton.setText("");
        addFilterButton.setOnAction(event -> addFilter());
        filteringTabController.ltlWindowController.addFilterButton.setOnAction(event -> filteringTabController.ltlWindowController.addLTLFilter());
    }

    private void handleFilterAction(Filter newFilter, boolean isFilterToAdd) {
        // Check if the action is to 'add' a filter
        if (isFilterToAdd) {
            createAndSelectFilter(newFilter);
        } else {  // If 'isFilterToAdd' is false (indicating an 'edit' action):
            resetFilterEditingState();
        }
    }

    private void initializeAttribute() {
        anchorPaneAttribute.setVisible(true);
        choiceBoxAttributeEndpointFollower.getSelectionModel().selectedItemProperty().addListener(this::filterByAttribute_selectedAttribute_OnChange); // update the listview according to the selected attribute
        listViewAttribute.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); // make possible to select many attributes
        if (choiceBoxAttributeEndpointFollower.getItems().contains("concept:name")) { //Should never be missing
            choiceBoxAttributeEndpointFollower.getSelectionModel().select("concept:name");
        } else {
            choiceBoxAttributeEndpointFollower.getSelectionModel().selectFirst();
        }

        listViewAttribute.getSelectionModel().getSelectedItems().addListener(updateLabel(nodeValues, listViewAttribute, "Event values:")); // update the header of the number of selected attributes.
        listViewAttribute.getSelectionModel().selectFirst();
        if (nodeValues.getText().isEmpty()) {
            nodeValues.setText("Event values: (" + new ArrayList<>(listViewAttribute.getSelectionModel().getSelectedItems()).size() + " of " + new ArrayList<>(listViewAttribute.getItems()).size() + " selected)");
        }
    }

    private void filterByAttribute_selectedAttribute_OnChange(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        listViewAttribute.getItems().clear();
        if (filteringTabController.attributeAndValue.get(newValue) != null) {
            listViewAttribute.getItems().addAll(filteringTabController.attributeAndValue.get(newValue));
            listViewAttribute.getSelectionModel().selectFirst();
        }
    }

    private void initializePerformance() {
        anchorPanePerformance.setVisible(true);
        choiceBoxPerformance.setItems(FXCollections.observableArrayList("Trace duration", "Number of events"));
        choiceBoxPerformance.getSelectionModel().selectedItemProperty().addListener(this::performanceFilterOnChange);
        choiceBoxPerformance.getSelectionModel().selectFirst();
        setupPerformanceSliders(minimumDuration, maximumDuration, "Minimum duration", "Maximum duration", getConverter());
    }

    private void performanceFilterOnChange(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (newValue != null && newValue.equals("Trace duration")) {
            setupPerformanceSliders(minimumDuration, maximumDuration, "Minimum duration", "Maximum duration", getConverter());
            return;
        }
        setupPerformanceSliders(minimumNumberOfEvents, maximumNumberOfEvents, "Minimum number of events", "Maximum number of events", new NumberStringConverter(NumberFormat.getIntegerInstance()));
    }

    public void setupPerformanceSliders(double minValue, double maxValue, String minLabel, String maxLabel, NumberStringConverter converter) {
        sliderMaximum.setMin(minValue);
        sliderMaximum.setMax(maxValue);
        setSliderValue(sliderMaximum, maxValue);

        sliderMinimum.setMin(minValue);
        sliderMinimum.setMax(maxValue);
        setSliderValue(sliderMinimum, minValue);

        minimumLabel.setText(minLabel);
        maximumLabel.setText(maxLabel);

        currentMaximum.textProperty().unbindBidirectional(sliderMaximum.valueProperty());
        currentMinimum.textProperty().unbindBidirectional(sliderMinimum.valueProperty());

        currentMaximum.textProperty().bindBidirectional(sliderMaximum.valueProperty(), converter);
        currentMinimum.textProperty().bindBidirectional(sliderMinimum.valueProperty(), converter);
    }

    public void setSliderValue(Slider slider, double value) {
        slider.setValue(value);
    }

    public void initializeEndpoints() {
        anchorPaneEndpoints.setVisible(true);
        setupSelectionUI_follower_endpoints(choiceBoxAttributeEndpointFollower, listViewInitialEndpoints, listViewEndEndpoints, startEventValuesLabel, endEventValuesLabel);
    }

    public void initializeFollower() {
        anchorPaneFollower.setVisible(true);
        setupSelectionUI_follower_endpoints(choiceBoxAttributeEndpointFollower, listViewInitialFollower, listViewEndFollower, startEventValuesLabelFollower, endEventValuesLabelFollower);
    }

    private void setupSelectionUI_follower_endpoints(ChoiceBox<String> choiceBoxAttribute, ListView<String> listViewInitial, ListView<String> listViewEnd, Label startEventValuesLabel, Label endEventValuesLabel) {
        choiceBoxAttribute.getSelectionModel().selectedItemProperty().addListener((observable, oldVal, newVal) -> {
            listViewInitial.getItems().clear();
            listViewEnd.getItems().clear();
            if (filteringTabController.attributeAndValue.get(newVal) != null) {
                listViewInitial.getItems().addAll(filteringTabController.attributeAndValue.get(newVal));
                listViewEnd.getItems().addAll(filteringTabController.attributeAndValue.get(newVal));
                listViewInitial.getSelectionModel().selectFirst();
                listViewEnd.getSelectionModel().selectFirst();
            }
        });
        if (choiceBoxAttribute.getItems().contains("concept:name")) { //Should never be missing
            choiceBoxAttribute.getSelectionModel().select("concept:name");
        } else {
            choiceBoxAttribute.getSelectionModel().selectFirst();
        }

        listViewInitial.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewInitial.getSelectionModel().selectFirst();
        listViewEnd.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewEnd.getSelectionModel().selectFirst();

        listViewInitial.getSelectionModel().getSelectedItems().addListener(updateLabel(startEventValuesLabel, listViewInitial, "Start event values:"));
        listViewEnd.getSelectionModel().getSelectedItems().addListener(updateLabel(endEventValuesLabel, listViewEnd, "End event values:"));

        if (startEventValuesLabel.getText().isEmpty() && endEventValuesLabel.getText().isEmpty()) {
            startEventValuesLabel.setText("Start event values: (" + new ArrayList<>(listViewInitial.getSelectionModel().getSelectedItems()).size() + " of " + new ArrayList<>(listViewInitial.getItems()).size() + " selected)");
            endEventValuesLabel.setText("End event values: (" + new ArrayList<>(listViewEnd.getSelectionModel().getSelectedItems()).size() + " of " + new ArrayList<>(listViewEnd.getItems()).size() + " selected)");
        }
    }

    private void initializeTimeframe() {
        dateTimeEnd.setDateTimeValue(StatisticsUtils.getEndTimestampOfLog(new LinkedList<>(filteringTabController.unmodifiedTraces)));
        dateTimeStart.setDateTimeValue(StatisticsUtils.getStartTimestampOfLog(new LinkedList<>(filteringTabController.unmodifiedTraces)));
        anchorPaneTimeframe.setVisible(true);
    }

    public <T> void clearAndAddAll(Collection<T> collection, Collection<? extends T> elements) {
        collection.clear();
        collection.addAll(elements);
    }

    private void initializeListView(ListView<String> listView, ChoiceBox<String> choiceBox) {
        clearAndAddAll(listView.getItems(), filteringTabController.attributeAndValue.get(choiceBox.getSelectionModel().getSelectedItem()));
        listView.getSelectionModel().selectFirst();
    }

    public void initializeWithAttributes() {
        choiceBoxAttributeEndpointFollower.getItems().addAll(new ArrayList<>(filteringTabController.attributeAndValue.keySet()));
        if (choiceBoxAttributeEndpointFollower.getItems().contains("concept:name")) { //Should never be missing
            choiceBoxAttributeEndpointFollower.getSelectionModel().select("concept:name");
        } else {
            choiceBoxAttributeEndpointFollower.getSelectionModel().selectFirst();
        }

        initializeListView(listViewAttribute, choiceBoxAttributeEndpointFollower);
        initializeListView(listViewInitialEndpoints, choiceBoxAttributeEndpointFollower);
        initializeListView(listViewEndEndpoints, choiceBoxAttributeEndpointFollower);
        initializeListView(listViewInitialFollower, choiceBoxAttributeEndpointFollower);
        initializeListView(listViewEndFollower, choiceBoxAttributeEndpointFollower);

        minimumNumberOfEvents = StatisticsUtils.getMinimumNumberOfEvents(new LinkedList<>(filteringTabController.unmodifiedTraces));
        maximumNumberOfEvents = StatisticsUtils.getMaximumNumberOfEvents(new LinkedList<>(filteringTabController.unmodifiedTraces));
        minimumDuration = StatisticsUtils.getMinimumDurationOfTrace(StatisticsUtils.getDurationOfAllTracesInLog(new LinkedList<>(filteringTabController.unmodifiedTraces)));
        maximumDuration = StatisticsUtils.getMaximumDurationOfTrace(StatisticsUtils.getDurationOfAllTracesInLog(new LinkedList<>(filteringTabController.unmodifiedTraces)));
    }

    private ChangeListener<String> getStringChangeListener() {
        return (observable, oldValue, newValue) -> {
            resetUIElements(false);
            handleSelectedCategory(newValue);
        };
    }

    private void handleSelectedCategory(String newValue) {
        switch (newValue) {
            case "Attribute":
            case "Follower":
            case "Endpoints":
                selectAllButton.setVisible(true);
                clearAllButton.setVisible(true);
                filterByLabel.setText("Attribute:");
                filteringModeLabel.setVisible(true);
                choiceBoxAttributeEndpointFollower.setVisible(true);
                if (newValue.equals("Attribute")) {
                    initializeAttribute();
                } else if (newValue.equals("Endpoints")) {
                    initializeEndpoints();
                } else {
                    initializeFollower();
                }
                break;
            case "Performance":
            case "Timeframe":
                filteringModeLabel.setVisible(false);
                selectAllButton.setVisible(false);
                clearAllButton.setVisible(false);
                if (newValue.equals("Timeframe")) {
                    filterByLabel.setVisible(false);
                    initializeTimeframe();
                } else {
                    filterByLabel.setText("Filter by:");
                    initializePerformance();
                }
                break;
        }
    }

    protected void resetUIElements(boolean isFilterPanelOpenAndFilterButtonPressed) {
        if (choiceBoxAttributeEndpointFollower.getItems().contains("concept:name")) { //Should never be missing
            choiceBoxAttributeEndpointFollower.getSelectionModel().select("concept:name");
        } else {
            choiceBoxAttributeEndpointFollower.getSelectionModel().selectFirst();
        }
        listViewAttribute.getSelectionModel().clearSelection();
        listViewAttribute.getSelectionModel().selectFirst();

        choiceBoxPerformance.getSelectionModel().selectFirst();

        setupPerformanceSliders(minimumDuration, maximumDuration, "Minimum duration", "Maximum duration", getConverter());

        listViewInitialEndpoints.getSelectionModel().clearSelection();
        listViewInitialEndpoints.getSelectionModel().selectFirst();
        listViewEndEndpoints.getSelectionModel().clearSelection();
        listViewEndEndpoints.getSelectionModel().selectFirst();
        listViewInitialFollower.getSelectionModel().clearSelection();
        listViewInitialFollower.getSelectionModel().selectFirst();
        listViewEndFollower.getSelectionModel().clearSelection();
        listViewEndFollower.getSelectionModel().selectFirst();

        toggleGroupAttribute.getToggles().get(0).setSelected(true);
        toggleGroupFollower.getToggles().get(0).setSelected(true);
        toggleGroupEndpoints.getToggles().get(0).setSelected(true);
        toggleGroupTimeframe.getToggles().get(0).setSelected(true);

        dateTimeEnd.setDateTimeValue(StatisticsUtils.getEndTimestampOfLog(new LinkedList<>(filteringTabController.unmodifiedTraces)));
        dateTimeStart.setDateTimeValue(StatisticsUtils.getStartTimestampOfLog(new LinkedList<>(filteringTabController.unmodifiedTraces)));

        if(!isFilterPanelOpenAndFilterButtonPressed) {
            filteringModeLabel.setVisible(false);
            filterByLabel.setVisible(true);
            choiceBoxAttributeEndpointFollower.setVisible(false);
            anchorPaneAttribute.setVisible(false);
            anchorPanePerformance.setVisible(false);
            anchorPaneEndpoints.setVisible(false);
            anchorPaneFollower.setVisible(false);
            anchorPaneTimeframe.setVisible(false);
        }
    }

    private ListChangeListener<String> updateLabel(Label label, ListView<String> listView, String labelText) {
        return (ListChangeListener.Change<? extends String> c) -> {
            int numberOfSelectedValues = listView.getSelectionModel().getSelectedItems().size();
            int maximumNumberOfValues = listView.getItems().size();

            if (maximumNumberOfValues == numberOfSelectedValues) {
                label.setText(labelText + " (all selected)");
            } else if (numberOfSelectedValues == 0) {
                label.setText(labelText + " (none selected)");
            } else {
                label.setText(labelText + " (" + numberOfSelectedValues + " of " + maximumNumberOfValues + " selected)");
            }
        };
    }


    // Traverses Window nodes to enable the close button to be pressed from anywhere within the LTL Window.
    private void setEscapeKeyHandler_RecursiveTreeTraversal(Node node) {
        if (node != null) {
            node.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    closeWindow();
                }
            });

            if (node instanceof Parent) {
                Parent parent = (Parent) node;
                for (Node child : parent.getChildrenUnmodifiable()) {
                    setEscapeKeyHandler_RecursiveTreeTraversal(child);
                }
            }
        }
    }

    public static NumberStringConverter getConverter() {
        return new NumberStringConverter() {
            @Override
            public String toString(Number value) {
                return String.format("%.1f hours", value.doubleValue()) + " (~" + StatisticsUtils.formatDuration(value.doubleValue()) + ")";
            }
        };
    }

    Callback<DatePicker, DateCell> createDateCellFactory(boolean isStartDatePicker) {
        return new Callback<>() {
            @Override
            public DateCell call(DatePicker datePicker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);

                        LocalDate logStartDate = StatisticsUtils.getStartTimestampOfLog(new LinkedList<>(filteringTabController.unmodifiedTraces)).toLocalDate();
                        LocalDate logEndDate = StatisticsUtils.getEndTimestampOfLog(new LinkedList<>(filteringTabController.unmodifiedTraces)).toLocalDate();

                        if (isStartDatePicker && (item.isBefore(logStartDate) || (dateTimeStart.getValue() != null && item.isAfter(logEndDate)))) {
                            setDisable(true);
                            return;
                        }

                        if (item.isAfter(logEndDate) || (dateTimeEnd.getValue() != null && item.isBefore(logStartDate))) {
                            setDisable(true);
                        }
                    }
                };
            }
        };
    }

    protected void setSelectedFilterCategory(String category) {
        this.selectedFilterCategory.set(category);
    }

    protected void setFilteringTabController(FilteringTabController filteringTabController) {
        this.filteringTabController = filteringTabController;
    }
}