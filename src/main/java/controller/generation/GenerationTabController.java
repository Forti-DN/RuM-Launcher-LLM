package controller.generation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.ToggleSwitch;
import org.deckfour.xes.out.XesXmlSerializer;
import org.kordamp.ikonli.javafx.FontIcon;

import controller.common.AbstractController;
import controller.common.eventcell.EventCell;
import controller.common.eventcell.EventData;
import controller.common.layers.AlertLayerController;
import controller.common.layers.AlertLayerController.AlertType;
import controller.common.layers.ProgressLayerController;
import global.Inventory;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import task.generation.GenerationTaskAlloy;
import task.generation.GenerationTaskAsp;
import task.generation.GenerationTaskMinerful;
import task.generation.GenerationTaskResult;
import treedata.TreeDataAttribute;
import treedata.TreeDataBase;
import util.AlertUtils;
import util.ConstraintTemplate;
import util.FileUtils;
import util.ModelUtils;
import util.TemplateUtils;
import util.ValidationUtils;

public class GenerationTabController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");

	@FXML
	private StackPane rootRegion;
	@FXML
	private HBox mainContents;
	@FXML
	private VBox parametersSection;
	@FXML
	private ChoiceBox<GenerationMethod> methodChoice;
	@FXML
	private Label methodWarningLabel;
	@FXML
	private TitledPane parametersPane;
	@FXML
	private Spinner<Integer> minTraceLengthSpinner;
	@FXML
	private Spinner<Integer> maxTraceLengthSpinner;
	@FXML
	private Spinner<Integer> numberOfPosTracesSpinner;
	@FXML
	private Spinner<Integer> percentageOfPosVacuousTracesSpinner;
	@FXML
	private GridPane alloyParametersPane;
	@FXML
	private Spinner<Integer> numberOfNegTracesSpinner;
	@FXML
	private Spinner<Integer> percentageOfNegVacuousTracesSpinner;
	@FXML
	private TitledPane traceAttributesPane;
	@FXML
	private Button addAttributeButton;
	@FXML
	private TreeView<TreeDataBase> traceAttributesTreeView;
	@FXML
	private ListView<String> constraintListView;
	@FXML
	private StackPane resultsContainer;
	@FXML
	private SplitPane resultsSplitPane;
	@FXML
	private ListView<String> tracesListView;
	@FXML
	private Label selectedTraceLabel;
	@FXML
	private HBox saveButtonsHbox;
	@FXML
	private ToggleSwitch showTraceAttributesToggle;
	@FXML
	private ToggleSwitch showPayloadsToggle;
	@FXML
	private VBox resultsVBox;
	@FXML
	private ListView<EventData> eventsListView;

	private TraceAttributesSettingsController traceAttributesSettingsController;
	private TranslateTransition traceAttributesSettingsSlideTransition;
	private FXMLLoader cellLoader;

	private File declModel;
	private boolean modelWithData;
	private boolean modelWithTimeCondition;
	private List<String> constraintList;
	private GenerationTaskResult generationTaskResult;
	private TreeDataBase attributesRoot;
	private TreeDataBase currentlyEditingTreeData;

	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	@FXML
	private void initialize() {
		prepareTraceAttributesLayer();
		prepareTraceAttributesCell();

		minTraceLengthSpinner.setValueFactory(new IntegerSpinnerValueFactory(2, Integer.MAX_VALUE, 10, 1));	// Causes NPE on empty input
		maxTraceLengthSpinner.setValueFactory(new IntegerSpinnerValueFactory(2, Integer.MAX_VALUE, 10, 1));	// Causes NPE on empty input

		numberOfPosTracesSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 20, 1));	// Causes NPE on empty input
		percentageOfPosVacuousTracesSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 0, 1));	// Causes NPE on empty input

		numberOfNegTracesSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 20, 1));	// Causes NPE on empty input
		percentageOfNegVacuousTracesSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 0, 1));	// Causes NPE on empty input

		ValidationUtils.addMandatoryPositiveIntegerBehavior(minTraceLengthSpinner, maxTraceLengthSpinner,
				numberOfPosTracesSpinner, percentageOfPosVacuousTracesSpinner,
				numberOfNegTracesSpinner, percentageOfNegVacuousTracesSpinner);

		// Sets the texts that are shown in the UI
		methodChoice.setConverter(new StringConverter<GenerationMethod>() {
			@Override
			public String toString(GenerationMethod generationMethod) {
				return generationMethod.getDisplayText();
			}
			@Override
			public GenerationMethod fromString(String string) {
				return null;
			}
		});

		//Visibility of settings panes based on selected method
		methodChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			alloyParametersPane.setVisible(newV.equals(GenerationMethod.ALLOY));
			alloyParametersPane.setManaged(newV.equals(GenerationMethod.ALLOY));
			
			methodWarningLabel.setText(null);
			switch (newV) {
			case MINERFUL:
				if (modelWithData)
					methodWarningLabel.setText(newV.getDisplayText() + " ignores data conditions!");
				break;

			case ALLOY:
			case ASP:
				if (modelWithTimeCondition)
					methodWarningLabel.setText(newV.getDisplayText() + " ignores time conditions!");
				break;

			default:
				//TODO: Disable log generation and show error to user
				logger.error("Unhandled log generation method selected: {}", newV);
				break;
			}
		});

		methodChoice.getItems().setAll(GenerationMethod.values());

		//Updates eventsListView to correspond to the selected trace
		tracesListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
			if (newIndex.intValue() >= 0) {
				eventsListView.setVisible(true);

				eventsListView.getItems().setAll(generationTaskResult.getTraceEventsData().get(newIndex.intValue()));
				selectedTraceLabel.setText("Trace ID: " + tracesListView.getSelectionModel().getSelectedItem());

				VBox attributesVBox = (VBox) cellLoader.getNamespace().get("payloadVBox");
				attributesVBox.getChildren().clear();

				Map<String,String> traceAttributes = generationTaskResult.getTraceAttributes().get(newIndex.intValue()).getPayload();
				if (traceAttributes.isEmpty()) {
					Label l = new Label("No payload");
					l.getStyleClass().add("event-data-text");
					l.setStyle("-fx-font-style: italic; -fx-text-fill: #000000");
					attributesVBox.getChildren().add(l);

				} else {
					traceAttributes.forEach((k,v) -> {
						Label l = new Label(k+ ": " +v);
						l.getStyleClass().add("event-data-text");
						l.setStyle("-fx-text-fill: #000000");
						attributesVBox.getChildren().add(l);
					});
				}

			} else {
				eventsListView.setVisible(false);
			}
		});

		// Each event is displayed as defined in EventListCell class
		eventsListView.setCellFactory(value -> new EventCell(false, showPayloadsToggle.isSelected(), null));
		showPayloadsToggle.selectedProperty().addListener((observable, oldValue, newValue) ->
			eventsListView.setCellFactory(value -> new EventCell(false, newValue, null))
		);

		((Parent)cellLoader.getRoot()).setVisible(false);
		((Parent)cellLoader.getRoot()).setManaged(false);
		showTraceAttributesToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			((Parent)cellLoader.getRoot()).setVisible(newValue);
			((Parent)cellLoader.getRoot()).setManaged(newValue);
		});

		//resultsPane will be shown once it has contents
		resultsSplitPane.setVisible(false);
		parametersSection.setViewOrder(-1); // Makes sure that the trace attributes panel slides in from under the parameters

		//Disables the results area when a slide in panel is visible
		resultsContainer.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
			while (change.next())
				resultsSplitPane.setDisable(change.getList().size() > 1);
		});

		logger.debug("Generation tab initialized");
	}

	private void prepareTraceAttributesLayer() {
		// Setting the results pane and toggle buttons to change based on settings pane children
		attributesRoot = new TreeDataBase();
		traceAttributesTreeView.setRoot(attributesRoot);
		traceAttributesTreeView.setShowRoot(false);
		attributesRoot.getIsEditingWrapper().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(Boolean.TRUE)) {
				addAttributeButton.pseudoClassStateChanged(selectedClass, true);
				addAttributeButton.setText("Minimize trace attributes");
				((FontIcon)addAttributeButton.getGraphic()).setIconLiteral("fa-angle-double-left");
			} else {
				addAttributeButton.pseudoClassStateChanged(selectedClass, false);
				addAttributeButton.setText("Add trace attributes");
				((FontIcon)addAttributeButton.getGraphic()).setIconLiteral("fa-angle-double-right");
			}
		});

		traceAttributesTreeView.setCellFactory(param -> new TraceAttributeCell(this));
		traceAttributesTreeView.setFixedCellSize(42d);
		traceAttributesTreeView.prefHeightProperty().bind(Bindings.size(traceAttributesTreeView.getRoot().getChildren()).multiply(42d).add(20d));

		// Preloading trace attribute settings
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/generation/TraceAttributesSettings.fxml"));
			loader.load();
			traceAttributesSettingsController = loader.getController();
			traceAttributesSettingsController.setController(this);
			traceAttributesSettingsController.setAttributesRoot(attributesRoot);

			// Preparation for slide in animation
			Region traceAttributesSettingsRootRegion = traceAttributesSettingsController.getRootRegion();
			traceAttributesSettingsSlideTransition = new TranslateTransition(new Duration(300), traceAttributesSettingsRootRegion);
			traceAttributesSettingsSlideTransition.setFromX(-1 * traceAttributesSettingsRootRegion.getPrefWidth());
			traceAttributesSettingsSlideTransition.setToX(-1); //-1 so that it would cover the parameters section border

		} catch (IOException | IllegalStateException e) {
			logger.error("Can not load trace attributes layer", e);
			//TODO: Alert the user and disable attribute editing buttons
			addAttributeButton.setDisable(true);
		}

		traceAttributesSettingsController.getCloseButton().setOnAction(event ->
			hideTraceAttributeSettings()
		);
	}

	private void prepareTraceAttributesCell() {
		try {
			cellLoader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/eventcell/EventCell.fxml"));
			cellLoader.load();

			Parent cellRoot = (Parent) cellLoader.getRoot();

			// Setting transparent background to the root child containing the trace attributes 
			cellRoot.getChildrenUnmodifiable().get(0).setStyle("-fx-background-color: transparent");

			Label numberLabel = (Label) cellLoader.getNamespace().get("eventNumberLabel");
			numberLabel.setVisible(false);
			numberLabel.setManaged(false);

			Label title = (Label) cellLoader.getNamespace().get("conceptNameLabel");
			title.setText("Trace Attributes");
			title.setStyle("-fx-text-fill: #000000");

			Label timestamp = (Label) cellLoader.getNamespace().get("timeTimestampLabel");
			timestamp.setVisible(false);
			timestamp.setManaged(false);

			Region strip = (Region) cellLoader.getNamespace().get("eventTypeStrip");
			strip.setVisible(false);
			strip.setManaged(false);

			VBox attributesVBox = (VBox) cellLoader.getNamespace().get("payloadVBox");
			attributesVBox.getChildren().clear();

			resultsVBox.getChildren().add(0, cellRoot);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void showTraceAttributeLayer() {
		startTreeDataItemEdit(attributesRoot);
	}

	public void startTreeDataItemEdit(TreeDataBase itemToEdit) {

		if (itemToEdit == currentlyEditingTreeData) {
			resultsContainer.getChildren().remove(traceAttributesSettingsController.getRootRegion());
			updateCurrentlyEditingTreeData(itemToEdit);

		} else if (itemToEdit == attributesRoot || itemToEdit instanceof TreeDataAttribute) {
			if (!resultsContainer.getChildren().contains(traceAttributesSettingsController.getRootRegion())) {
				resultsContainer.getChildren().add(traceAttributesSettingsController.getRootRegion());
				traceAttributesSettingsSlideTransition.play();
			}

			updateCurrentlyEditingTreeData(itemToEdit);
			traceAttributesSettingsController.setEditingAttribute(currentlyEditingTreeData);
		}
	}

	public void deleteTreeDataItem(TreeDataBase itemToDelete) {
		attributesRoot.getChildren().remove(itemToDelete);
		if (itemToDelete == currentlyEditingTreeData)
			startTreeDataItemEdit(attributesRoot);
	}

	private void updateCurrentlyEditingTreeData(TreeDataBase itemToEdit) {

		if (itemToEdit == null) {
			if (currentlyEditingTreeData != null) {
				currentlyEditingTreeData.setIsEditing(false);
				currentlyEditingTreeData = null;
			}

		} else if (itemToEdit == currentlyEditingTreeData) {
			currentlyEditingTreeData.setIsEditing(false);
			currentlyEditingTreeData = null;

		} else {
			if (currentlyEditingTreeData != null)
				currentlyEditingTreeData.setIsEditing(false);

			currentlyEditingTreeData = itemToEdit;
			currentlyEditingTreeData.setIsEditing(true);
		}
	}

	@FXML
	private void hideTraceAttributeSettings() {
		resultsContainer.getChildren().remove(traceAttributesSettingsController.getRootRegion());
		updateCurrentlyEditingTreeData(null);
	}

	@FXML
	private void generateLog() {
		hideTraceAttributeSettings();	// Close trace attributes settings if open

		if (numberOfPosTracesSpinner.getValue() + numberOfNegTracesSpinner.getValue() > 0) {
			//TODO: It would be better to do "validateConstraints" proactively before the user starts log generation.
			if (validateParameters() && validateConstraints(constraintList)) {
				logger.debug("Starting log generation from model: {}", declModel.getAbsolutePath());

				try {
					//Load the progress layer
					FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/ProgressLayer.fxml"));
					Node progressLayer = loader.load();
					ProgressLayerController progressLayerController = loader.getController();
					progressLayerController.getProgressTextLabel().setText("Generating log...");

					//Create the task
					Task<GenerationTaskResult> task = createGenerationTask();
					addHandlersToTask(task, progressLayer, progressLayerController);

					//Start the task
					rootRegion.getChildren().add(progressLayer);
					mainContents.setDisable(true);
					executorService.execute(task);
				} catch (IOException | IllegalStateException e) {
					//TODO: Feedback to the user
					logger.error("Can not load progress layer", e);
				}
			}

		} else {
			AlertUtils.showWarning("Generated log should have at least one trace!");
		}
	}

	@FXML
	private void exportGeneratedLog() {
		File chosenFile = FileUtils.showXesSaveDialog(this.getStage(), null);

		//chosenFile might be null, because the user can just close the fileChooser instead of choosing a file
		if(chosenFile != null) {
			logger.debug("Exporting log to file: {}", chosenFile.getAbsolutePath());
			FileOutputStream outStream;
			try {
				//TODO: Check for export differences between the generation methods
				outStream = new FileOutputStream(chosenFile.getAbsolutePath());
				new XesXmlSerializer().serialize(generationTaskResult.getGeneratedLog(), outStream);
				outStream.flush();
				outStream.close();
				logger.info("Log exported to file: {}", chosenFile.getAbsolutePath());
				AlertUtils.showSuccess("Log successfully exported");
			} catch (IOException e) {
				AlertUtils.showError("Exporting the log failed!");
				logger.error("Unable to export log: {}", chosenFile.getAbsolutePath(), e);
			}
		}
	}

	public void setModelData(File declModel, List<String> constraintList) {
		this.declModel = declModel;
		this.constraintList = constraintList;

		//Setting cell height, list padding and list preferred height
		double cellHeight = 55;
		double padding = 15;
		this.constraintListView.setFixedCellSize(cellHeight);
		this.constraintListView.setPadding(new Insets(padding));
		this.constraintListView.setPrefHeight(constraintList.size() * cellHeight + 2 * padding);

		modelWithData = constraintList.stream().map(ModelUtils::containsData).reduce(false, (a,b) -> a || b);
		modelWithTimeCondition = constraintList.stream().map(ModelUtils::containsTimeCondition).reduce(false, (a,b) -> a || b);

		List<String> formattedConstraintsList = ModelUtils.getFormattedListOfConstraints(constraintList);
		this.constraintListView.getItems().addAll(formattedConstraintsList);

		methodChoice.getSelectionModel().selectFirst();

		logger.debug("Model in generation tab set to: {}", declModel.getAbsolutePath());
	}

	private boolean validateParameters() {
		boolean valid = true;
		if (numberOfPosTracesSpinner.getValue() == null) {
			numberOfPosTracesSpinner.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			numberOfPosTracesSpinner.requestFocus();
			valid = false;
		}

		if (maxTraceLengthSpinner.getValue() == null) {
			maxTraceLengthSpinner.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			maxTraceLengthSpinner.requestFocus();
			valid = false;
		}

		if (minTraceLengthSpinner.getValue() == null) {
			minTraceLengthSpinner.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			minTraceLengthSpinner.requestFocus();
			valid = false;
		}

		if (maxTraceLengthSpinner.getValue() < minTraceLengthSpinner.getValue()) {
			maxTraceLengthSpinner.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			minTraceLengthSpinner.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			AlertUtils.showError("Maximum length must be greater than or equal to minimum");
			valid = false;
		}

		if (methodChoice.getSelectionModel().getSelectedItem() == GenerationMethod.ALLOY) {
			if (percentageOfPosVacuousTracesSpinner.getValue() == null) {
				percentageOfPosVacuousTracesSpinner.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				percentageOfPosVacuousTracesSpinner.requestFocus();
				valid = false;
			}

			if (numberOfNegTracesSpinner.getValue() == null) {
				numberOfNegTracesSpinner.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				numberOfNegTracesSpinner.requestFocus();
				valid = false;
			}

			if (percentageOfNegVacuousTracesSpinner.getValue() == null) {
				percentageOfNegVacuousTracesSpinner.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				percentageOfNegVacuousTracesSpinner.requestFocus();
				valid = false;
			}
		}

		if (!valid) {
			parametersPane.setExpanded(true);
		}

		logger.debug("Result of parameter validation: {}", valid);
		return valid;
	}

	private boolean validateConstraints(List<String> constraintList) {
		GenerationMethod selectedMethod = methodChoice.getSelectionModel().getSelectedItem();

		for(String s: constraintList) {
			String template = s.substring(0,s.indexOf('['));
			
			if(selectedMethod == GenerationMethod.ALLOY) {
				if (!TemplateUtils.getAlloyGenerationSupportedConstraints().contains( ConstraintTemplate.getByTemplateName(template) )) {
					AlertUtils.showError(template + " is not a valid template for Alloy Log Generator");
					return false;
				}
				
				if(TemplateUtils.isUnaryTemplate(template)) {
					int lp = s.lastIndexOf('|');
					if(lp == -1) {
						AlertUtils.showError(template + " must be written as template[activity] |activation_condition |time_condition, where time condition will be ignored if exists.");
						return false;
					}
				
				} else {
					int lp = s.lastIndexOf('|');
					if(lp == -1) {
						AlertUtils.showError(template + " must be written as template[activity] |activation_condition |correlation_condition |time_condition, where time condition will be ignored if exists.");
						return false;
					}
				}
			
			} else if (selectedMethod == GenerationMethod.MINERFUL) {
				if (!TemplateUtils.getMinerfulGenerationSupportedConstraints().contains( ConstraintTemplate.getByTemplateName(template) )) {
					AlertUtils.showError(template + " is not valid template for Minerful Log Generator");
					return false;
				}
			}
		}

		return true;
	}

	private Task<GenerationTaskResult> createGenerationTask() {
		Task<GenerationTaskResult> task = null;

		GenerationMethod selectedMethod = methodChoice.getSelectionModel().getSelectedItem();
		switch (selectedMethod) {
		case MINERFUL:
			GenerationTaskMinerful generationTaskMinerful = new GenerationTaskMinerful();
			generationTaskMinerful.setDeclModel(declModel);
			generationTaskMinerful.setMinTraceLength(minTraceLengthSpinner.getValue());
			generationTaskMinerful.setMaxTraceLength(maxTraceLengthSpinner.getValue());
			generationTaskMinerful.setNumberOfTraces(numberOfPosTracesSpinner.getValue());
			generationTaskMinerful.setConstraintList(constraintList);
			generationTaskMinerful.setTraceAttributes(attributesRoot.getChildren().stream().map(TreeDataAttribute.class::cast).collect(Collectors.toList()));
			task = generationTaskMinerful;
			break;

		case ALLOY:
			GenerationTaskAlloy generationTaskAlloy = new GenerationTaskAlloy();
			generationTaskAlloy.setDeclModel(declModel);
			generationTaskAlloy.setMinTraceLength(minTraceLengthSpinner.getValue());
			generationTaskAlloy.setMaxTraceLength(maxTraceLengthSpinner.getValue());
			generationTaskAlloy.setNumberOfPositiveTraces(numberOfPosTracesSpinner.getValue());
			generationTaskAlloy.setVacuousPositiveTracesPercentage(percentageOfPosVacuousTracesSpinner.getValue());
			generationTaskAlloy.setNumberOfNegativeTraces(numberOfNegTracesSpinner.getValue());
			generationTaskAlloy.setVacuousNegativeTracesPercentage(percentageOfNegVacuousTracesSpinner.getValue());
			generationTaskAlloy.setTraceAttributes(attributesRoot.getChildren().stream().map(TreeDataAttribute.class::cast).collect(Collectors.toList()));
			task = generationTaskAlloy;
			break;

		case ASP:
			GenerationTaskAsp generationTaskAsp = new GenerationTaskAsp();
			generationTaskAsp.setDeclModel(declModel);
			generationTaskAsp.setMinTraceLength(minTraceLengthSpinner.getValue());
			generationTaskAsp.setMaxTraceLength(maxTraceLengthSpinner.getValue());
			generationTaskAsp.setNumberOfPositiveTraces(numberOfPosTracesSpinner.getValue());
			generationTaskAsp.setTraceAttributes(attributesRoot.getChildren().stream().map(TreeDataAttribute.class::cast).collect(Collectors.toList()));
			task = generationTaskAsp;
			break;

		default:
			//TODO: Show error to user
			logger.error("Can not create log generation task for unhandled method: {}", selectedMethod);
			break;
		}

		return task;
	}

	private void addHandlersToTask(Task<GenerationTaskResult> task, Node progressLayer, ProgressLayerController progressLayerController) {
		// Handle cancelling the task
		progressLayerController.getCancelButton().setOnAction(e -> {
			task.cancel(true);
			rootRegion.getChildren().remove(progressLayer);
			mainContents.setDisable(false);
		});

		// Handle task success
		task.setOnSucceeded(event -> {
			rootRegion.getChildren().remove(progressLayer);
			resultsSplitPane.setVisible(false);
			tracesListView.getItems().clear();
			eventsListView.getItems().clear();
			
			boolean isLogEmpty = task.getValue().getGeneratedLog().isEmpty()
								|| task.getValue().getGeneratedLog().stream().allMatch(List::isEmpty);

			if (isLogEmpty) {
				setupAlertLayer(AlertType.ERROR, "Cannot generate log according to this model, due to unconsistent constraint set OR unsufficient minimum number of events");
				
			} else {
				this.generationTaskResult = task.getValue();

				long posCount = generationTaskResult.getGeneratedLog().stream().filter(t -> t.getAttributes().get("trace:type").toString().equals("positive")).count();
				if (numberOfPosTracesSpinner.getValue()>0 && posCount<=0)
					setupAlertLayer(AlertType.WARNING, "Cannot generate positive traces according to this model, due to unconsistent constraint set OR unsufficient minimum number of events");

				long negCount = generationTaskResult.getGeneratedLog().stream().filter(t -> t.getAttributes().get("trace:type").toString().equals("negative")).count();
				if (alloyParametersPane.isVisible() && numberOfNegTracesSpinner.getValue()>0 && negCount <= 0)
					setupAlertLayer(AlertType.WARNING, "Cannot generate negative traces according to this model, due to unconsistent constraint set OR unsufficient minimum number of events");
					
				tracesListView.getItems().setAll(generationTaskResult.getTraceNames());
				tracesListView.getSelectionModel().select(0);
				resultsSplitPane.setVisible(true);
				mainContents.setDisable(false);
				saveButtonsHbox.setDisable(isLogEmpty);
			}
			
		});

		// Handle task failure
		task.setOnFailed(event -> {
			rootRegion.getChildren().remove(progressLayer);
			resultsSplitPane.setVisible(false);
			setupAlertLayer(AlertType.ERROR, "Log generation failed");
		});
	}

	private void setupAlertLayer(AlertType alertType, String message) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
			Node alertLayer = loader.load();
			AlertLayerController alertLayerController = loader.getController();
			alertLayerController.setAlertMessage(alertType, message);
			alertLayerController.getOkButton().setOnAction(e -> {
				rootRegion.getChildren().remove(alertLayer);
				mainContents.setDisable(false);
			});

			rootRegion.getChildren().add(alertLayer);

		} catch (IOException e) {
			logger.error("Can not load alert layer", e);
			// If alert layer can not be displayed then use regular alert instead
			switch (alertType) {
				case ERROR:
				AlertUtils.showError(message);
					break;
				case INFO:
				case WARNING:
					AlertUtils.showWarning(message);
					break;
			}

			mainContents.setDisable(false);
		}
	}

	@FXML
	private void takeSnapshot() {
		logger.info("Save snapshot of event log");

		String placeholder = declModel.getName();
		Matcher m = Pattern.compile("(\\.(decl))$").matcher(placeholder);
		if (m.find())
			placeholder = placeholder.substring(0, placeholder.length()-m.group().length());

		TextInputDialog dialog = new TextInputDialog(placeholder + ".xes");
		dialog.setTitle("Save snapshot of event log");
		dialog.setHeaderText("Save snapshot of event log");
		dialog.setContentText("Event log name:");
		dialog.getDialogPane().setMinWidth(500.0);

		dialog.getDialogPane().getStylesheets().add("main.css");
		((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Okay");
		((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
		((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("small-button");
		((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("small-button");

		Optional<String> fileName = dialog.showAndWait();

		// check is saving was canceled
		if(fileName.isPresent()) {
			try {
				//Adding .xes extension because otherwise XLogReader.openLog() will fail when the snapshot is loaded
				File file = new File(Files.createTempDirectory("snap_log").toFile(), fileName.get() + ".xes");
	
				logger.debug("Exporting log to temp. file: {}", file.getAbsolutePath());
				try (FileOutputStream outStream = new FileOutputStream(file.getAbsolutePath())) {
					//TODO: Check for export differences between the generation methods
					new XesXmlSerializer().serialize(generationTaskResult.getGeneratedLog(), outStream);
					outStream.flush();
					logger.info("Log exported to temp. file: {}", file.getAbsolutePath());
				}
	
				InventorySavedElement inventorySavedElement = new InventorySavedElement(file, new Date(), fileName.get(), InventoryElementTypeEnum.EVENT_LOG);
				Inventory.storeEventLogSnapshot(inventorySavedElement);
				
			} catch (IOException e) {
				AlertUtils.showError("Exporting the log failed!");
				logger.error("Unable to export log.", e);
			}
		} else {
			logger.info("Canceled save of log");
		}
	}
}
