package controller.filtering;

import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

public class FilteringTabWindowService {

    public FilteringTabController filteringTabController;

    public FilteringTabWindowService(FilteringTabController filteringTabController) {
        this.filteringTabController = filteringTabController;
    }

    public void initializeWindows() {
        initFiltersChoiceBox();
        addListenerToFiltersChoiceBox();
    }

    private void initFiltersChoiceBox() {
        filteringTabController.addFiltersChoiceBox.setItems(FXCollections.observableArrayList("Add filters", "Attribute", "Performance", "Endpoints", "Follower", "Timeframe", "Ltl formulas")); // initialize checkbox for adding filters
        filteringTabController.addFiltersChoiceBox.getSelectionModel().selectFirst();
    }

    private void addListenerToFiltersChoiceBox() {
        filteringTabController.addFiltersChoiceBox.getSelectionModel().selectedItemProperty().addListener(this::addFilterCheckboxListener);
        filteringTabController.addFiltersChoiceBox.getSelectionModel().selectedItemProperty().addListener(getCheckboxManipulationListener(filteringTabController.addFiltersChoiceBox));
    }

    private void disableContents() {
        filteringTabController.resultsView.setDisable(true);
        filteringTabController.statisticsContents.setDisable(true);
    }

    private void enableContents() {
        filteringTabController.resultsView.setDisable(false);
        filteringTabController.statisticsContents.setDisable(false);
    }

    private ChangeListener<String> getCheckboxManipulationListener(ChoiceBox<String> addFiltersChoiceBox) {
        return (ObservableValue<? extends String> observable, String closedWindow, String openedWindow) -> {
            if (closedWindow.equals("Add filters")) {
                addFiltersChoiceBox.getItems().remove("Add filters");
            }
        };
    }

    public void openLTLFiltersWindow() {
        disableContents();
        filteringTabController.ltlWindowController.setLtlCheckerTabController(filteringTabController);
        filteringTabController.stackPaneResults.getChildren().remove(filteringTabController.ltlWindowController.getRootRegion());
        filteringTabController.ltlWindowController.updateData();
        filteringTabController.stackPaneResults.getChildren().add(filteringTabController.ltlWindowController.getRootRegion());
        TranslateTransition translateTransition = new TranslateTransition(new Duration(300), filteringTabController.ltlWindowController.getRootRegion());
        translateTransition.setFromX(-1 * filteringTabController.ltlWindowController.getRootRegion().getMaxWidth());
        translateTransition.setToX(3); //Not sure why, but 3 needs to be used in this section instead of the usual -1
        translateTransition.play();
    }

    public void openBasicFiltersWindow(String filterCategory) {
        disableContents();
        filteringTabController.basicFiltersWindowController.setSelectedFilterCategory(filterCategory);

        if (!filteringTabController.stackPaneResults.getChildren().contains(filteringTabController.basicFiltersWindowController.getRootRegion())) {
            filteringTabController.stackPaneResults.getChildren().add(filteringTabController.basicFiltersWindowController.getRootRegion());
            //Having the translateTransition here means that it will not play when switching between different basic filters
            TranslateTransition translateTransition = new TranslateTransition(new Duration(300), filteringTabController.basicFiltersWindowController.getRootRegion());
            translateTransition.setFromX(-1 * filteringTabController.basicFiltersWindowController.getRootRegion().getMaxWidth());
            translateTransition.setToX(3); //Not sure why, but 3 needs to be used in this section instead of the usual -1
            translateTransition.play();
            filteringTabController.basicFiltersWindowController.getRootRegion().requestFocus();


            // Method to add "ESC" key event handler to the basic filters window
            EventHandler<KeyEvent> escHandler = event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    // Close the window when "ESC" key is pressed
                    filteringTabController.basicFiltersWindowController.closeWindow();
                }
            };

            // Add the key event handler to the RootRegion and all its ancestors
            Node node = filteringTabController.basicFiltersWindowController.getRootRegion();
            while (node != null) {
                node.setOnKeyPressed(escHandler);
                node = node.getParent();
            }
        }
    }

    private void addFilterCheckboxListener(ObservableValue<? extends String> observable, String closedWindow, String openedWindow) {
        enableContents();

        // If the choice box for the filter attributes is empty, initialize it.
        if (filteringTabController.basicFiltersWindowController.choiceBoxAttributeEndpointFollower.getItems().isEmpty()) {
            filteringTabController.basicFiltersWindowController.initializeWithAttributes();
        }

        // If the "Ltl formulas" window is being closed, remove its pane from the contents layer.
        if (closedWindow.equals("Ltl formulas")) {
            filteringTabController.stackPaneResults.getChildren().remove(filteringTabController.ltlWindowController.getRootRegion());
        }

        // If a window other than "Ltl formulas" is being opened and the "Add filters" window is being closed, open the normal filters window.
        if (!openedWindow.equals("Ltl formulas") && closedWindow.equals("Add filters")) {
            openBasicFiltersWindow(openedWindow);
            return;
        }

        // If a window other than "Add filters" is being closed and the "Add filters" window is being opened, remove the normal filters window.
        if (!closedWindow.equals("Add filters") && openedWindow.equals("Add filters")) {
            filteringTabController.stackPaneResults.getChildren().remove(filteringTabController.basicFiltersWindowController.getRootRegion());
            return;
        }

        // If a window other than "Ltl formulas" is being opened, remove the "Ltl formulas" window and open the normal filters window.
        if (!openedWindow.equals("Ltl formulas")) {
            filteringTabController.stackPaneResults.getChildren().remove(filteringTabController.ltlWindowController.getRootRegion());
            openBasicFiltersWindow(openedWindow);
            return;
        }

        filteringTabController.stackPaneResults.getChildren().remove(filteringTabController.basicFiltersWindowController.getRootRegion());
        openLTLFiltersWindow();
    }
}
