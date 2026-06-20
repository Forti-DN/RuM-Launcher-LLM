package controller.filtering;


import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;

import java.io.File;
import java.util.Collections;

public class FilteringTabEventHandler {
    static StringBuilder typedLetters = new StringBuilder();
    static final long[] lastTimeTyped = new long[]{0};

    public void initializeHandlers(FilteringTabController filteringTabController) {
        setOnKeyPressedForAppliedFiltersListView(filteringTabController);
        configureImportFiltersButtonDragAndDrop(filteringTabController);
    }

    private void setOnKeyPressedForAppliedFiltersListView(FilteringTabController filteringTabController) {

        filteringTabController.appliedFiltersListView.setOnKeyPressed(event -> {

            // Get the selected item in the applied filters list view
            Filter selectedItem = filteringTabController.appliedFiltersListView.getSelectionModel().getSelectedItem();

            // Ensure there is a selected item before proceeding
            if (selectedItem != null) {

                // If backspace or delete key is pressed, remove the selected item from the list
                if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
                    removeSelectedFilter(filteringTabController, selectedItem);
                }

                // Handle up or down movement on arrow key press with shortcut key (CTRL or CMD)
                else if (event.isShortcutDown()) {
                    handleShortcutKeyPress(filteringTabController, event);
                }
            }
        });

        filteringTabController.ltlWindowController.formulasListView.setOnKeyTyped(event -> handleKeyTypedEvent(filteringTabController, event));

    }

    private static void handleKeyTypedEvent(FilteringTabController filteringTabController, KeyEvent event) {

        // if more than 1 second has passed since last keystroke, clear the buffer
        if (System.currentTimeMillis() - lastTimeTyped[0] > 1000) {
            typedLetters.setLength(0);
        }
        lastTimeTyped[0] = System.currentTimeMillis();  // Update the first (and only) element of the array

        typedLetters.append(event.getCharacter().toLowerCase());
        String searchString = typedLetters.toString();

        if (!searchString.isEmpty() && Character.isAlphabetic(searchString.charAt(0))) {
            for (int i = 0; i < filteringTabController.ltlWindowController.formulasListView.getItems().size(); i++) {
                LTLWindowController.Item item = filteringTabController.ltlWindowController.formulasListView.getItems().get(i);
                if (item.getName().toLowerCase().startsWith(searchString)) {
                    filteringTabController.ltlWindowController.formulasListView.scrollTo(i);
                    filteringTabController.ltlWindowController.formulasListView.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }

    private static void removeSelectedFilter(FilteringTabController filteringTabController, Filter selectedItem) {

        // Get the index of the selected item before removal
        int index = filteringTabController.appliedFiltersListView.getSelectionModel().getSelectedIndex();

        filteringTabController.selectedFiltersListView.remove(selectedItem);

        // If the list is not empty after removal
        if (!filteringTabController.selectedFiltersListView.isEmpty()) {

            // Select the last item if deleted item was last, else select the next item
            if (index == filteringTabController.selectedFiltersListView.size()) {
                filteringTabController.appliedFiltersListView.getSelectionModel().selectLast();
            } else {
                filteringTabController.appliedFiltersListView.getSelectionModel().select(index);
            }
        }
    }

    private void handleShortcutKeyPress(FilteringTabController filteringTabController, KeyEvent event) {
        int index = filteringTabController.appliedFiltersListView.getSelectionModel().getSelectedIndex();

        if (event.getCode() == KeyCode.DOWN) {
            swapItems(filteringTabController, index, (index + 1 == filteringTabController.appliedFiltersListView.getItems().size()) ? 0 : index + 1);
        } else if (event.getCode() == KeyCode.UP) {
            swapItems(filteringTabController, index, (index == 0) ? filteringTabController.appliedFiltersListView.getItems().size() - 1 : index - 1);
        }
    }

    private void swapItems(FilteringTabController filteringTabController, int currentIndex, int targetIndex) {
        Collections.swap(filteringTabController.selectedFiltersListView, currentIndex, targetIndex);
        filteringTabController.selectedFiltersListView.setAll(filteringTabController.appliedFiltersListView.getItems());
        filteringTabController.appliedFiltersListView.getSelectionModel().select(targetIndex);
        filteringTabController.appliedFiltersListView.scrollTo(targetIndex);
    }

    private void configureImportFiltersButtonDragAndDrop(FilteringTabController filteringTabController) {
        // Set the behavior when a file is dragged over the importFiltersButton. The dragged file should be a JSON file, otherwise, the button won't accept it.
        filteringTabController.importFiltersButton.setOnDragOver(event -> {
            boolean areAllJsonFiles = event.getDragboard().getFiles().stream().allMatch(file -> file.isFile() && file.getName().endsWith(".json"));
            // If the gesture source is not the importFiltersButton itself and all dragged files are JSON files
            if (event.getGestureSource() != filteringTabController.importFiltersButton && areAllJsonFiles) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        // Set the behavior when a file is dropped onto the importFiltersButton.
        filteringTabController.importFiltersButton.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (event.getDragboard().hasFiles()) {
                db.getFiles().forEach((File currentFile) -> ImportAgent.importFilters(currentFile, filteringTabController.logFile, filteringTabController.attributeAndValue, filteringTabController.selectedFiltersListView, filteringTabController.appliedFiltersListView, filteringTabController));
                success = true;
            }

            // Complete the drop
            event.setDropCompleted(success);
            event.consume();
        });
    }
}
