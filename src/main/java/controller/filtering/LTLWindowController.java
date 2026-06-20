package controller.filtering;

import controller.common.AbstractController;
import controller.filtering.cell.FilterCell;
import controller.filtering.cell.ParameterCell;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;
import util.AlertUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class LTLWindowController extends AbstractController {
    @FXML
    public GridPane rootRegion;
    @FXML
    public ListView<Item> formulasListView;
    @FXML
    public Label instanceNameLabel;
    @FXML
    public Label instanceFormulaLabel;
    @FXML
    public Label instanceDescriptionLabel;
    @FXML
    public TableView<ParameterCell> parameterTableView = new TableView<>();
    @FXML
    public TableColumn<ParameterCell, String> argumentColumn;
    @FXML
    public TableColumn<ParameterCell, String> attributeColumn;
    @FXML
    public TableColumn<ParameterCell, String> valueColumn;
    @FXML
    public Button closeWindowButton;
    @FXML
    public Button addFilterButton;
    @FXML
    public ScrollPane vboxFormulaInfo;
    public boolean filterEditON;
    protected FilteringTabController filteringTabController;

    private static String loadLTLFileContents() {
        try {
            return Files.readString(FilteringPageController.ltlFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error loading LTL file contents", e);
        }
    }

    private static List<String[]> extractFormulaInfo(String fileContents) {

        final String FORMULA_NAME_REGEX = "formula([^(]*)";
        final String FORMULA_PARAMETERS_REGEX = "[^(]*\\(([^)]*)";
        final String FORMULA_DESCRIPTION_REGEX = "[^:]*:[^=]*=[^{]*\\{([^}]*)}";
        final String FORMULA_REGEX = "([^;]*);";

        List<String[]> allMatches = new LinkedList<>();
        String combinedRegexPattern = FORMULA_NAME_REGEX + FORMULA_PARAMETERS_REGEX + FORMULA_DESCRIPTION_REGEX + FORMULA_REGEX;
        Pattern pattern = Pattern.compile(combinedRegexPattern);
        Matcher matcher = pattern.matcher(fileContents);

        while (matcher.find()) {
            allMatches.add(new String[]{matcher.group(1).trim(), matcher.group(2).trim(), matcher.group(3).trim(), matcher.group(4).trim()});
        }

        return allMatches;
    }

    @FXML
    private void initialize() {
        configureFormulasListView();
        configureParameterTableView();
        setEscapeKeyHandler_RecursiveTreeTraversal(rootRegion);
    }

    @FXML
    protected void close() {
        if (!filteringTabController.addFiltersChoiceBox.getItems().contains("Add filters")) {
            filteringTabController.addFiltersChoiceBox.getItems().add(0, "Add filters");
        }
        filteringTabController.addFiltersChoiceBox.getSelectionModel().selectFirst();
        filteringTabController.contentsLayer.getChildren().remove(this.getRootRegion());
        filterEditON = false;
    }

    private void configureParameterTableView() {
        parameterTableView.setFixedCellSize(34);
        parameterTableView.setPlaceholder(new Label("No parameters"));
        argumentColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        attributeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
    }

    private void configureFormulasListView() {
        formulasListView.setCellFactory(this::createFormulaListViewCell);
        formulasListView.getSelectionModel().selectedItemProperty().addListener(this::onFormulaSelected);
        formulasListView.setFocusTraversable(true);
    }

    private ListCell<Item> createFormulaListViewCell(ListView<Item> itemListView) {

        return new ListCell<>() {
            public final CheckBox checkBox = new CheckBox();

            {
                checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    Item item = getItem();
                    if (item != null && newVal) {
                        item.setOn(true);
                        formulasListView.getSelectionModel().select(item);
                    }
                });
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setPrefWidth(0);
            }

            @Override
            public void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setFocusTraversable(false);
                    checkBox.setSelected(item.isOn());
                    Label label = new Label(item.getName());
                    label.setWrapText(true);
                    label.setMaxWidth(Double.MAX_VALUE);
                    setGraphic(new HBox(10, checkBox, label));
                    setPrefWidth(getListView().getWidth() - 50);
                    setMaxWidth(Double.MAX_VALUE);
                }
            }
        };
    }

    private void onFormulaSelected(javafx.beans.value.ObservableValue<? extends LTLWindowController.Item> obs, LTLWindowController.Item oldValue, LTLWindowController.Item newValue) {

        if (newValue != null) {
            instanceNameLabel.setText(newValue.getName());
            instanceFormulaLabel.setText(newValue.getFormula());
            parameterTableView.setItems(FXCollections.observableList(newValue.getParameters()));
            parameterTableView.prefHeightProperty().unbind();
            parameterTableView.prefHeightProperty().set(34 + parameterTableView.getFixedCellSize() * parameterTableView.getItems().size());
            instanceDescriptionLabel.setText(newValue.getDescription());
            vboxFormulaInfo.setVvalue(0.0); // to scroll at the beginning of the scroll-pane

            if (filterEditON) {
                for (Item item : formulasListView.getItems()) {
                    if (item.isOn() && !item.getName().equals(newValue.getName())) {
                        item.setOn(false);
                        item.getParameters().forEach((ParameterCell e) -> e.value.setText(""));
                    }
                }
                formulasListView.refresh();
            }
        }
    }

    protected List<String[]> getFormulaInfo() {
        String fileContents = loadLTLFileContents();
        return extractFormulaInfo(fileContents);
    }

    protected void updateData() {
        formulasListView.setItems(FXCollections.observableList(getFormulaInfo().stream().map(Item::new).collect(Collectors.toList())));
        formulasListView.getSelectionModel().selectFirst();
        formulasListView.scrollTo(formulasListView.getSelectionModel().getSelectedIndex());
        Platform.runLater(() -> {
            formulasListView.requestFocus();
            formulasListView.getFocusModel().focus(formulasListView.getSelectionModel().getSelectedIndex());
        });
        setEscapeKeyHandler_RecursiveTreeTraversal(formulasListView);
        setEscapeKeyHandler_RecursiveTreeTraversal(parameterTableView);
    }

    public Map<String, Map<String, String>> getParamTable() {
        return formulasListView.getItems().stream().filter(Item::isOn).collect(Collectors.toMap(Item::getName, Item::getParameterMap));
    }

    // Traverses Window nodes to enable the close button to be pressed from anywhere within the LTL Window.
    private void setEscapeKeyHandler_RecursiveTreeTraversal(Node node) {
        if (node != null) {
            node.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    close();
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

    @FXML
    public void addLTLFilter() {

        if (getParamTable().isEmpty()) {
            AlertUtils.showWarning("You have not selected an LTL-based filter yet. Please make a selection!");
            return;
        }

        getParamTable().forEach((String formula, Map<String, String> nestedMap) -> {  // add the selected filters to the chain of filters
            Filter filter = new Filter();
            parseLTLFilter(filter, nestedMap, formula, false);
            filteringTabController.selectedFiltersListView.add(filter);
            filteringTabController.appliedFiltersListView.getSelectionModel().select(filter);
            filteringTabController.appliedFiltersListView.scrollTo(filter);
        });

        filteringTabController.basicFiltersWindowController.addFilterButton.setText("Add filter");
        filteringTabController.ltlWindowController.addFilterButton.setText("Add filter");

        clearFilterParameters();

        formulasListView.refresh();

    }


    public void parseLTLFilter(Filter filter, Map<String, String> nestedMap, String formula, boolean isFilterToBeEdited) {
        filter.setCategory("LTL");

        List<String> listOfStartValue = new LinkedList<>();
        List<String> listOfEndValue = new LinkedList<>();
        StringBuilder stringBuilder = new StringBuilder();

        if (isFilterToBeEdited) {
            Map<String, Map<String, String>> parametersTable = new HashMap<>();
            formulasListView.getItems().stream().filter((Item selectedItem) -> Objects.equals(selectedItem.getName(), formulasListView.getSelectionModel().getSelectedItem().getName())).forEach(template -> parametersTable.put(template.getName(), template.getParameterMap()));
            nestedMap = parametersTable.entrySet().iterator().next().getValue();
            String selectedFormula = filteringTabController.ltlWindowController.formulasListView.getSelectionModel().getSelectedItem().getName().concat(StringUtils.SPACE).concat(stringBuilder.toString());
            filter.setFilteringMode(selectedFormula);
            clearFilterParameters();
        }

        nestedMap.forEach((String parameter, String parameterValue) -> {
            if (parameterValue.isEmpty() || parameterValue.isBlank()) {
                listOfStartValue.add(StringUtils.SPACE);
                listOfEndValue.add(parameter);
                stringBuilder.append(StringUtils.SPACE);
            } else {
                listOfStartValue.add(parameterValue);
                listOfEndValue.add(parameter);
                stringBuilder.append(parameterValue);
            }
        });

        filter.setStartValue(listOfStartValue);
        filter.setEndValue(listOfEndValue);

        if (!isFilterToBeEdited) {
            filter.setFilteringMode(formula.concat(StringUtils.SPACE).concat(stringBuilder.toString()));
        }

        filterEditON = false;
        filteringTabController.isChanged.set(true);
        if (FilterCell.getSelectedCell() != null) {
            FilterCell.getSelectedCell().modifyButton.setText("");
        }
        filteringTabController.basicFiltersWindowController.addFilterButton.setText("Add filter");
        filteringTabController.ltlWindowController.addFilterButton.setText("Add filter");
        filteringTabController.appliedFiltersListView.refresh();
        filteringTabController.ltlWindowController.addFilterButton.setOnAction(event -> addLTLFilter());
        filteringTabController.basicFiltersWindowController.addFilterButton.setOnAction(event -> filteringTabController.basicFiltersWindowController.addFilter());
    }

    private void clearFilterParameters() {
        formulasListView.getItems().forEach((Item currentItem) -> {
            currentItem.setOn(false);
            currentItem.getParameters().forEach((ParameterCell e) -> e.value.setText(""));
        });
    }

    public void setLtlCheckerTabController(FilteringTabController filteringTabController) {
        this.filteringTabController = filteringTabController;
    }

    public static class Item {
        private final StringProperty name = new SimpleStringProperty();
        private final BooleanProperty on = new SimpleBooleanProperty(false);
        private final String formula;
        private final String description;
        private final List<ParameterCell> parameters = new LinkedList<>();

        public Item(String[] strings) {
            String name = strings[0];
            String parameters = strings[1];
            String description = strings[2];
            String formula = strings[3];

            this.name.set(name);
            this.description = description;
            this.formula = formula;

            if (!parameters.isEmpty()) {
                for (String parameter : parameters.split(",")) {
                    String[] p = parameter.split(":");
                    String parameterName = p[0];
                    String parameterType = p[1];
                    this.parameters.add(new ParameterCell(parameterName.trim(), parameterType.trim(), ""));
                }
            }
        }

        public List<ParameterCell> getParameters() {
            return parameters;
        }

        public Map<String, String> getParameterMap() {
            return parameters.stream().collect(Collectors.toMap(ParameterCell::getName, parameter -> parameter.getValue().getText()));
        }

        public String getFormula() {
            return formula;
        }

        public String getDescription() {
            return description;
        }

        public final String getName() {
            return this.name.get();
        }

        public final boolean isOn() {
            return this.on.get();
        }

        public final void setOn(final boolean on) {
            this.on.set(on);
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}