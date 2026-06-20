package controller.conformance;

import controller.conformance.plannerTextBased.model.PlannerAlgorithm;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javafx.scene.control.RadioButton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.ToggleSwitch;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.deckfour.xes.out.XesXmlSerializer;
import org.kordamp.ikonli.javafx.FontIcon;

import controller.common.AbstractController;
import controller.common.eventcell.EventCell;
import controller.common.eventcell.EventData;
import controller.common.layers.AlertLayerController;
import controller.common.layers.AlertLayerController.AlertType;
import controller.common.layers.ProgressLayerController;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import task.conformance.ActivityConformanceType;
import task.conformance.ActivityMappingResult;
import task.conformance.ActivityMappingService;
import task.conformance.ConformanceStatisticType;
import task.conformance.ConformanceTaskAnalyzer;
import task.conformance.ConformanceTaskDataAwareReplayer;
import task.conformance.ConformanceTaskReplayer;
import task.conformance.ConformanceTaskResult;
import task.conformance.ConformanceTaskResultDetail;
import task.conformance.ConformanceTaskResultGroup;
import task.conformance.ConformanceTaskPlanBased;
import util.AlertUtils;
import util.ConstraintTemplate;
import util.FileUtils;
import util.LogUtils;
import util.ModelExporter;
import util.ModelUtils;
import util.TemplateUtils;
import util.ValidationUtils;

public class ConformanceTabController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");
	
	private XConceptExtension xce = XConceptExtension.instance();

	@FXML
	private StackPane rootRegion;
	@FXML
	private HBox mainContents;
	@FXML
	private VBox parametersSection;
	@FXML
	private Button logFileButton;
	@FXML
	private Label logPathLabel;
	@FXML
	private ChoiceBox<ConformanceMethod> methodChoice;
	@FXML
	private Label methodWarningLabel;
	@FXML
	private TitledPane flowSettingsPane;
	@FXML
	private TextField defaultInsertionCost;
	@FXML
	private TextField defaultDeletionCost;
	@FXML
	private TitledPane plannerAlgorithmSettingsPane;
	@FXML
	private RadioButton symbaButton;
	@FXML
	private RadioButton fastDownwardButton;
	@FXML
	private Button toggleFlowSettingsButton; //TODO: Probably should be a togglebutton
	@FXML
	private TitledPane dataSettingsPane;
	@FXML
	private TextField defaultNonWritingCost;
	@FXML
	private TextField defaultFaultyValueCost;
	@FXML
	private Button toggleDataSettingsButton; //TODO: Probably should be a togglebutton
	@FXML
	private StackPane settingsLayer;
	@FXML
	private VBox resultsPane;	
	@FXML
	private Button exportFulfilledLogButton;
	@FXML
	private Button exportViolatedLogButton;

	@FXML
	private Button exportAlignedLogButton;
	@FXML
	private Button groupByTracesButton;
	@FXML
	private Button groupByConstraintsButton;
	@FXML
	private ChoiceBox<ConformanceStatisticType> traceSortChoice;
	@FXML
	private VBox legendVBox;
	@FXML
	private Label firstLegendLabel;
	@FXML
	private Label secondLegendLabel;
	@FXML
	private Label thirdLegendLabel;
	@FXML
	private Label globalLegendLabel;
	@FXML
	private VBox statisticsVBox;

	@FXML private Label firstStatisticLabel;
	@FXML private Label secondStatisticLabel;
	@FXML private Label thirdStatisticLabel;
	@FXML private Label globalStatisticLabel;
	@FXML private Label totalTracesLabel;

	@FXML private Label fulfillmentLegendEntry;
	@FXML private Label violationLegendEntry;
	@FXML private Label vacFulfillmentLegendEntry;
	@FXML private Label vacViolationLegendEntry;
	@FXML private Label insertionLegendEntry;
	@FXML private Label deletionLegendEntry;
	@FXML private Label dataDifferenceLegendEntry;

	@FXML
	private ListView<ConformanceTaskResultGroup> resultGroupsListView;
	@FXML
	private VBox eventsHeaderVBox;
	@FXML
	private Label selectedTraceLabel;
	@FXML
	private ToggleSwitch showPayloadsToggle;
	@FXML
	private HBox applyAlignmentHBox;
	@FXML
	private ToggleSwitch applyAlignmentToggle;
	@FXML
	private ListView<EventData> eventsListView;

	private File xmlModel;
	private boolean modelWithData;

	private List<String> constraintList;
	private File logFile;
	private FlowSettingsController flowSettingsController;
	private DataSettingsController dataSettingsController;
	private TranslateTransition flowSettingsSlideTransition;
	private TranslateTransition dataSettingsSlideTransition;
	private ConformanceTaskResult conformanceTaskResult;

	private boolean isAlignment;

	private ConformanceTaskResultDetail currentResultDetail; //Keeps track of the trace currently displayed in the eventsListView
	private TableView<ConformanceTaskResultDetail> previousResultGroupTable; //Needed to update the visual indicator of currently selected result detail

	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private ActivityMappingService activityMappingService = new ActivityMappingService();

	//Settings controllers listen to changes in this value
	private SimpleObjectProperty<ActivityMappingResult> activityMappingProperty = new SimpleObjectProperty<>();

	public ConformanceTabController() {
		super();
		setupActivityMappingService();
	}

	@FXML
	private void initialize() {
		parametersSection.setViewOrder(-1); //Makes sure that flow and data settings slide in from under the parameters

		//Sets the texts that are shown in the UI
		methodChoice.setConverter(new StringConverter<ConformanceMethod>() {
			@Override
			public String toString(ConformanceMethod conformanceMethod) {
				return conformanceMethod.getDisplayText();
			}

			@Override
			public ConformanceMethod fromString(String string) {
				return null;
			}
		});

		traceSortChoice.setConverter(new StringConverter<ConformanceStatisticType>() {
			@Override
			public String toString(ConformanceStatisticType conformanceStatisticType) {
				return conformanceStatisticType.getDisplayText();
			}

			@Override
			public ConformanceStatisticType fromString(String string) {
				return null;
			}
		});

		traceSortChoice.getSelectionModel().selectedItemProperty().addListener((ov, oldV, newV) -> {
			if (newV != null) {
				sortResults(newV);
			}
		});

		//Visibility of settings panes based on selected method
		methodChoice.getSelectionModel().selectedItemProperty().addListener((ov, oldV, newV) -> {
			methodWarningLabel.setText(null);
			switch (newV) {
			case ANALYZER:
				flowSettingsPane.setDisable(true);
				flowSettingsPane.setExpanded(false);
				flowSettingsController.discardUnsavedChanges();
				settingsLayer.getChildren().remove(flowSettingsController.getRootRegion());
				dataSettingsPane.setDisable(true);
				dataSettingsPane.setExpanded(false);
				dataSettingsController.discardUnsavedChanges();
				settingsLayer.getChildren().remove(dataSettingsController.getRootRegion());
				plannerAlgorithmSettingsPane.setDisable(true);
				plannerAlgorithmSettingsPane.setExpanded(false);
				break;
			case REPLAYER:
				flowSettingsPane.setDisable(false);
				flowSettingsPane.setExpanded(true);
				dataSettingsPane.setDisable(true);
				dataSettingsPane.setExpanded(false);
				dataSettingsController.discardUnsavedChanges();
				settingsLayer.getChildren().remove(dataSettingsController.getRootRegion());
				if (modelWithData) {
					methodWarningLabel.setText("Declare Replayer ignores data conditions!");
				}
				plannerAlgorithmSettingsPane.setDisable(true);
				plannerAlgorithmSettingsPane.setExpanded(false);
				break;
			case DATA_REPLAYER:
				flowSettingsPane.setDisable(false);
				flowSettingsPane.setExpanded(true);
				dataSettingsPane.setDisable(false);
				dataSettingsPane.setExpanded(true);
				plannerAlgorithmSettingsPane.setDisable(true);
				plannerAlgorithmSettingsPane.setExpanded(false);
				break;
			case PLAN_BASED:
				flowSettingsPane.setDisable(true);
				flowSettingsPane.setExpanded(false);
				flowSettingsController.discardUnsavedChanges();
				settingsLayer.getChildren().remove(flowSettingsController.getRootRegion());
				dataSettingsPane.setDisable(true);
				dataSettingsPane.setExpanded(false);
				dataSettingsController.discardUnsavedChanges();
				settingsLayer.getChildren().remove(dataSettingsController.getRootRegion());
				plannerAlgorithmSettingsPane.setDisable(false);
				plannerAlgorithmSettingsPane.setExpanded(true);
				break;
			default:
				//TODO: Disable conformance checking and show error to user
				logger.error("Unhandled conformance checking method selected: {}", newV);
				break;
			}
		});

		
		methodChoice.getItems().add(ConformanceMethod.ANALYZER);
		methodChoice.getItems().add(ConformanceMethod.REPLAYER);
		//TODO: Data-aware Replayer is disabled because soon or later it will be removed
		//methodChoice.getItems().add(ConformanceMethod.DATA_REPLAYER);
		
		//TODO: Planner is currently available only in debug mode because it does not work correctly
		if (Objects.equals(System.getProperty("RumDebug"), "true")) {
			methodChoice.getItems().add(ConformanceMethod.PLAN_BASED);
		} else {
			plannerAlgorithmSettingsPane.setVisible(false);
			plannerAlgorithmSettingsPane.setManaged(false);
		}

		prepareSettingsLayers();
		setupInputs();

		//resultsSplitPane will be shown once it has contents
		resultsPane.setVisible(false);

		//Each event is displayed as defined in EventListCell class
		eventsListView.setCellFactory(value -> new EventCell(true, showPayloadsToggle.isSelected(), null));
		showPayloadsToggle.selectedProperty().addListener((observable, oldValue, newValue) ->
			eventsListView.setCellFactory(value -> new EventCell(true, newValue, null))
		);

		applyAlignmentToggle.setSelected(false);
		applyAlignmentToggle.selectedProperty().addListener((observable, oldValue, newValue) ->
			eventsListView.getItems().setAll(
				LogUtils.createEventDataList(
				currentResultDetail.getXtrace(),
				currentResultDetail.getActivityConformanceTypes(),
				false,
				newValue)
			)
		);
	}

	@FXML
	private void selectLogFile() {
		if (!flowSettingsController.isFlowCostsEmpty() || !dataSettingsController.isDataCostsEmpty()) {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
				Node alertLayer = loader.load();
				AlertLayerController alertLayerController = loader.getController();
				alertLayerController.setAlertMessage(AlertType.WARNING, "Specific costs will be discarded when a new log is chosen");
				alertLayerController.getOkButton().setOnAction(e -> {
					rootRegion.getChildren().remove(alertLayer);
					mainContents.setDisable(false);
					startLogFileChoice();
				});
				alertLayerController.getCancelButton().setOnAction(e -> {
					rootRegion.getChildren().remove(alertLayer);
					mainContents.setDisable(false);
				});
				rootRegion.getChildren().add(alertLayer);
				mainContents.setDisable(true);
			} catch (IOException | IllegalStateException e) {
				logger.error("Can not load alert layer.", e);
				//If alert layer can not be displayed then use regular alert instead
				AlertUtils.showWarning("Specific costs will be discarded when a new log is chosen"); //TODO: Allow user to cancel in the alert
				startLogFileChoice();
			}
		} else {
			startLogFileChoice();
		}
	}

	private void startLogFileChoice() {
		InventorySavedElement eventLog = FileUtils.showSavedElementDialog(InventoryElementTypeEnum.EVENT_LOG);

		if (eventLog != null) {
			// MARCUS - Select log file in conformance checking
			logger.info("Opened log in conformance tab: {}", eventLog.getFile().getAbsolutePath());
			activityMappingService.cancel();
			activityMappingService.setLogFile(eventLog.getFile());
			activityMappingService.restart();

			logPathLabel.setText(eventLog.getFile().getAbsolutePath());
			logFileButton.pseudoClassStateChanged(ValidationUtils.errorClass, false);
			this.logFile = eventLog.getFile();
		} else if (this.logFile == null) {
			logFileButton.pseudoClassStateChanged(ValidationUtils.errorClass, true);
		}
	}

	@FXML
	private void hideSettings() {
		if (settingsLayer.getChildren().remove(flowSettingsController.getRootRegion())) {
			flowSettingsController.discardUnsavedChanges();
		}
		if (settingsLayer.getChildren().remove(dataSettingsController.getRootRegion())) {
			dataSettingsController.discardUnsavedChanges();
		}
	}

	@FXML
	private void toggleFlowSettings() {
		if (settingsLayer.getChildren().remove(flowSettingsController.getRootRegion())) {
			flowSettingsController.discardUnsavedChanges();
		} else {
			if (settingsLayer.getChildren().remove(dataSettingsController.getRootRegion())) {
				dataSettingsController.discardUnsavedChanges();
			}
			settingsLayer.getChildren().add(flowSettingsController.getRootRegion());
			flowSettingsSlideTransition.play();
		}
	}

	@FXML
	private void toggleDataSettings() {
		if (settingsLayer.getChildren().remove(dataSettingsController.getRootRegion())) {
			dataSettingsController.discardUnsavedChanges();
		} else {
			if (settingsLayer.getChildren().remove(flowSettingsController.getRootRegion())) {
				flowSettingsController.discardUnsavedChanges();
			}
			settingsLayer.getChildren().add(dataSettingsController.getRootRegion());
			dataSettingsSlideTransition.play();
		}
	}

	@FXML
	private void checkConformance() {
		if (validateParameters() && validateConstraints(constraintList)) {
			// MARCUS - Check pressed
			logger.debug("Starting conformance checking with model: {} and log: {}", xmlModel.getAbsolutePath(), logFile.getAbsolutePath());

			//Close settings and discard unsaved changes if settings open
			if (settingsLayer.getChildren().remove(flowSettingsController.getRootRegion())) {
				flowSettingsController.discardUnsavedChanges();
			}
			if (settingsLayer.getChildren().remove(dataSettingsController.getRootRegion())) {
				dataSettingsController.discardUnsavedChanges();
			}

			try {
				//Load the progress layer
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/ProgressLayer.fxml"));
				Node progressLayer = loader.load();
				ProgressLayerController progressLayerController = loader.getController();
				progressLayerController.getProgressTextLabel().setText("Checking conformance...");

				//Create the task
				Task<ConformanceTaskResult> task = createConformanceCheckingTask();
				addHandlersToTask(task, progressLayer, progressLayerController);

				//Start the task
				rootRegion.getChildren().add(progressLayer);
				mainContents.setDisable(true);
				executorService.execute(task);
			} catch (IOException | IllegalStateException e) {
				//TODO: Feedback to the user
				logger.error("Can not load progress layer.", e);
			}
		}
	}

	@FXML
	private void groupByTraces() {
		logger.debug("Grouping conformance checking results by traces");
		groupByTracesButton.pseudoClassStateChanged(selectedClass, true);
		groupByConstraintsButton.pseudoClassStateChanged(selectedClass, false);
		if (conformanceTaskResult != null) {
			double detailNameWidth = conformanceTaskResult.getMaxConstraintNameWidth() + 20d;
			int numberOfDetails = conformanceTaskResult.getConstraintList().size();
			resultGroupsListView.setCellFactory(value -> new ResultGroupCell("constraint", detailNameWidth, null, numberOfDetails, this, isAlignment));
			resultGroupsListView.getItems().setAll(conformanceTaskResult.getResultsGroupedByTrace());
			updateTraceSortChoice(methodChoice.getSelectionModel().getSelectedItem());
		}
		setCurrentResultDetail(null, null);
	}

	@FXML
	private void groupByConstraints() {
		logger.debug("Grouping conformance checking results by constraints");
		groupByTracesButton.pseudoClassStateChanged(selectedClass, false);
		groupByConstraintsButton.pseudoClassStateChanged(selectedClass, true);
		if (conformanceTaskResult != null) {
			double detailNameWidth = conformanceTaskResult.getMaxTraceNameWidth() + 20d;
			double detailContentsWidth = conformanceTaskResult.getMaxTraceLength() * 13d;
			int numberOfDetails = conformanceTaskResult.getTraceList().size();
			resultGroupsListView.setCellFactory(value -> new ResultGroupCell("traceName", detailNameWidth, detailContentsWidth, numberOfDetails, this, isAlignment));
			resultGroupsListView.getItems().setAll(conformanceTaskResult.getResultsGroupedByConstraint());
			updateTraceSortChoice(methodChoice.getSelectionModel().getSelectedItem());
		}
		setCurrentResultDetail(null, null);
	}

	public ConformanceTaskResult getConformanceTaskResult() {
		return conformanceTaskResult;
	}

	public void setCurrentResultDetail(ConformanceTaskResultDetail currentResultDetail, TableView<ConformanceTaskResultDetail> detailsTableView) {
		this.currentResultDetail = currentResultDetail;
		if (previousResultGroupTable != null && previousResultGroupTable != detailsTableView) {
			previousResultGroupTable.getSelectionModel().clearSelection();
		}
		this.previousResultGroupTable = detailsTableView;

		if (currentResultDetail == null) {
			this.currentResultDetail = null;
			eventsListView.getItems().clear();
			eventsHeaderVBox.setVisible(false);
		} else {
			selectedTraceLabel.setText("Trace ID: " + currentResultDetail.getTraceName());
			eventsHeaderVBox.setVisible(true);
			//TODO: Should create these lists as part of conformance check (as is done in log generation)
			this.currentResultDetail = currentResultDetail;
			eventsListView.getItems().setAll(LogUtils
					.createEventDataList(currentResultDetail.getXtrace(), currentResultDetail.getActivityConformanceTypes(),
							false, applyAlignmentToggle.isSelected()));
		}
	}

	public void setModelData(File xmlModel, List<String> constraintList, SortedSet<String> attributes) {
		this.xmlModel = xmlModel;
		this.constraintList = constraintList;
		logger.debug("Model in conformance checking tab set to: {}", xmlModel.getAbsolutePath());

		activityMappingService.setXmlModel(xmlModel);
		modelWithData = constraintList.stream().map(ModelUtils::containsData).reduce(false, (a, b) -> a || b);

		methodChoice.getSelectionModel().selectFirst();

		this.dataSettingsController.setAttributes(attributes);
	}

	private void arrangeCheckerChoices(List<String> constraintList) {
		if (!methodChoice.getItems().contains(ConformanceMethod.ANALYZER)) {
			methodChoice.getItems().add(ConformanceMethod.ANALYZER);
		}
		if (!methodChoice.getItems().contains(ConformanceMethod.REPLAYER)
				&& Boolean.TRUE.equals( !constraintList.stream().map(ModelUtils::containsData).reduce(false, (a, b) -> a || b)) ) {
			methodChoice.getItems().add(ConformanceMethod.REPLAYER);
		}
		//TODO: Data-aware Replayer is disabled because soon or later it will be removed
		/*if (!methodChoice.getItems().contains(ConformanceMethod.DATA_REPLAYER)) {
			methodChoice.getItems().add(ConformanceMethod.DATA_REPLAYER);
		}*/
		//TODO: Planner is currently available only in debug mode because it does not work correctly
		if (!methodChoice.getItems().contains(ConformanceMethod.PLAN_BASED) && Objects.equals(System.getProperty("RumDebug"), "true")) {
			methodChoice.getItems().add(ConformanceMethod.PLAN_BASED);
		}
	}

	private void prepareSettingsLayers() {
		// Setting the results pane and toggle buttons to change based on settings pane children
		settingsLayer.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
			while (change.next()) {
				resultsPane.setDisable(change.getList().size() > 1);
				if (change.getAddedSubList().contains(flowSettingsController.getRootRegion())) {
					toggleFlowSettingsButton.setText("Minimize specific costs");
					toggleFlowSettingsButton.pseudoClassStateChanged(selectedClass, true);
					((FontIcon) toggleFlowSettingsButton.getGraphic()).setIconLiteral("fa-angle-double-left");
				} else if (change.getAddedSubList().contains(dataSettingsController.getRootRegion())) {
					toggleDataSettingsButton.setText("Minimize specific costs");
					toggleDataSettingsButton.pseudoClassStateChanged(selectedClass, true);
					((FontIcon) toggleDataSettingsButton.getGraphic()).setIconLiteral("fa-angle-double-left");
				} else if (change.getRemoved().contains(flowSettingsController.getRootRegion())) {
					toggleFlowSettingsButton.setText("Add specific costs");
					toggleFlowSettingsButton.pseudoClassStateChanged(selectedClass, false);
					((FontIcon) toggleFlowSettingsButton.getGraphic()).setIconLiteral("fa-angle-double-right");
				} else if (change.getRemoved().contains(dataSettingsController.getRootRegion())) {
					toggleDataSettingsButton.setText("Add specific costs");
					toggleDataSettingsButton.pseudoClassStateChanged(selectedClass, false);
					((FontIcon) toggleDataSettingsButton.getGraphic()).setIconLiteral("fa-angle-double-right");
				}
			}
		});

		// Preloading flow and data settings
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/conformance/FlowSettings.fxml"));
			loader.load();
			flowSettingsController = loader.getController();
			flowSettingsController.prepareForActivityMapUpdates(activityMappingProperty);

			//Preparation for slide in animation
			Region flowSettingsRootRegion = flowSettingsController.getRootRegion();
			flowSettingsSlideTransition = new TranslateTransition(new Duration(300), flowSettingsRootRegion);
			flowSettingsSlideTransition.setFromX(-1 * flowSettingsRootRegion.getMaxWidth());
			flowSettingsSlideTransition.setToX(-1); //-1 so that it would cover the parameters section border
		} catch (IOException | IllegalStateException e) {
			logger.error("Can not load flow settings layer", e);
			toggleFlowSettingsButton.setDisable(true);
		}
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/conformance/DataSettings.fxml"));
			loader.load();
			dataSettingsController = loader.getController();
			dataSettingsController.prepareForActivityMapUpdates(activityMappingProperty);

			//Preparation for slide in animation
			Region dataSettingsRootRegion = dataSettingsController.getRootRegion();
			dataSettingsSlideTransition = new TranslateTransition(new Duration(300), dataSettingsRootRegion);
			dataSettingsSlideTransition.setFromX(-1 * dataSettingsRootRegion.getMaxWidth());
			dataSettingsSlideTransition.setToX(-1); //-1 so that the slide-in border would align correctly
		} catch (IOException | IllegalStateException e) {
			logger.error("Can not load data settings layer", e);
			toggleDataSettingsButton.setDisable(true);
		}

		flowSettingsController.getCloseButton().setOnAction(event -> hideSettings());
		dataSettingsController.getCloseButton().setOnAction(event -> hideSettings());
	}

	private void setupInputs() {
		//Default values
		defaultInsertionCost.setText("10.0");
		flowSettingsController.setDefaultInsertionCost(new BigDecimal("10.0"));
		defaultInsertionCost.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(Boolean.FALSE) && !defaultInsertionCost.getText().isBlank())
				flowSettingsController.setDefaultInsertionCost(new BigDecimal(defaultInsertionCost.getText()));
		});

		defaultDeletionCost.setText("10.0");
		flowSettingsController.setDefaultDeletionCost(new BigDecimal("10.0"));
		defaultDeletionCost.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(Boolean.FALSE) && !defaultDeletionCost.getText().isBlank())
				flowSettingsController.setDefaultDeletionCost(new BigDecimal(defaultDeletionCost.getText()));
		});

		defaultNonWritingCost.setText("1.0");
		dataSettingsController.setDefaultNonWritingCost(new BigDecimal("1.0"));
		defaultNonWritingCost.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(Boolean.FALSE) && !defaultNonWritingCost.getText().isBlank())
				dataSettingsController.setDefaultNonWritingCost(new BigDecimal(defaultNonWritingCost.getText()));
		});

		defaultFaultyValueCost.setText("1.0");
		dataSettingsController.setDefaultFaultyValueCost(new BigDecimal("1.0"));
		defaultFaultyValueCost.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(Boolean.FALSE) && !defaultFaultyValueCost.getText().isBlank()) {
				dataSettingsController.setDefaultFaultyValueCost(new BigDecimal(defaultFaultyValueCost.getText()));
			}
		});

		ValidationUtils.addMandatoryPositiveDecimalBehavior(defaultInsertionCost, defaultDeletionCost, defaultNonWritingCost, defaultFaultyValueCost);
	}

	private void setupActivityMappingService() {
		activityMappingService.setExecutor(executorService);

		activityMappingService.stateProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue == State.SUCCEEDED) {
				activityMappingProperty.set(activityMappingService.getValue());
				arrangeCheckerChoices(constraintList);
			} else if (newValue == State.FAILED) {
				logger.error("Activity mapping failed for model: {} and log: {}",
					activityMappingService.getXmlModel().getAbsolutePath(),
					activityMappingService.getLogFile().getAbsolutePath(),
					activityMappingService.getException()
				);

				mainContents.setDisable(true);
				activityMappingProperty.set(null);
				methodChoice.getItems().removeAll(ConformanceMethod.REPLAYER, ConformanceMethod.DATA_REPLAYER, ConformanceMethod.PLAN_BASED);
				if (methodChoice.getSelectionModel().getSelectedIndex() == -1) {
					methodChoice.getSelectionModel().select(0);
				}

				try {
					FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
					Node alertLayer = loader.load();
					AlertLayerController alertLayerController = loader.getController();
					alertLayerController.setAlertMessage(AlertType.WARNING, "Activity mapping failed. Declare Replayer and DataAware Declare Replayer unavailable");
					alertLayerController.getOkButton().setOnAction(e -> {
						rootRegion.getChildren().remove(alertLayer);
						mainContents.setDisable(false);
					});
					rootRegion.getChildren().add(alertLayer);
				} catch (IOException | IllegalStateException e) {
					logger.error("Can not load alert layer", e);
					//If alert layer can not be displayed then use regular alert instead
					AlertUtils.showWarning("Activity mapping failed. Declare Replayer and DataAware Declare Replayer unavailable");
				}
			} else {
				activityMappingProperty.set(null);
			}
		});
	}

	private void updateTraceSortChoice(ConformanceMethod conformanceMethod) {
		ConformanceStatisticType previousSelection = traceSortChoice.getSelectionModel().getSelectedItem();
		if ((conformanceMethod == ConformanceMethod.REPLAYER || conformanceMethod == ConformanceMethod.DATA_REPLAYER
				|| conformanceMethod == ConformanceMethod.PLAN_BASED) && groupByConstraintsButton.getPseudoClassStates().contains(selectedClass)) {
			traceSortChoice.getItems().setAll(ConformanceStatisticType.NAME, ConformanceStatisticType.INSERTIONS, ConformanceStatisticType.DELETIONS);
		} else {
			traceSortChoice.getItems().setAll(ConformanceStatisticType.values(conformanceMethod));
		}
		traceSortChoice.getSelectionModel().select(previousSelection);

		//Selects previous sorting if possible; triggers sortResults call
		if (traceSortChoice.getItems().contains(previousSelection)) {
			traceSortChoice.getSelectionModel().select(previousSelection);
		} else {
			traceSortChoice.getSelectionModel().selectFirst();
		}
	}

	private void sortResults(ConformanceStatisticType conformanceStatisticType) {
		switch (conformanceStatisticType) {
		case NAME:
			resultGroupsListView.getItems().sort((o1, o2) -> {
				String v1 = o1.getGroupStatistics().get(conformanceStatisticType);
				String v2 = o2.getGroupStatistics().get(conformanceStatisticType);
				return v1.compareTo(v2);
			});
			break;
		case INSERTIONS: //Fall through intended
		case DELETIONS: //Fall through intended
		case ACTIVATIONS: //Fall through intended
		case FULFILLMENTS: //Fall through intended
		case VIOLATIONS: //Fall through intended
		case VACUOUS_FULFILLMENTS: //Fall through intended
		case VACUOUS_VIOLATIONS: //Fall through intended
		case DATA_DIFFERENCES:
			resultGroupsListView.getItems().sort((o1, o2) -> {
				Integer v1 = Integer.valueOf(o1.getGroupStatistics().get(conformanceStatisticType));
				Integer v2 = Integer.valueOf(o2.getGroupStatistics().get(conformanceStatisticType));
				return v2.compareTo(v1);
			});
			break;
		case FITNESS:
			resultGroupsListView.getItems().sort((o1, o2) -> {
				Float v1 = Float.valueOf(o1.getGroupStatistics().get(conformanceStatisticType));
				Float v2 = Float.valueOf(o2.getGroupStatistics().get(conformanceStatisticType));
				return v2.compareTo(v1);
			});
			break;
		default:
			logger.error("Unhandled conformance statistic type selected for sorting: {}", conformanceStatisticType);
			break;
		}
	}

	private boolean validateParameters() {
		boolean valid = true;
		if (logFile == null) {
			logFileButton.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			logFileButton.requestFocus();
			valid = false;
		}
		if (defaultInsertionCost.getText().trim().length() == 0) {
			defaultInsertionCost.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			flowSettingsPane.setExpanded(true);
			defaultInsertionCost.requestFocus();
			valid = false;
		}
		if (defaultDeletionCost.getText().trim().length() == 0) {
			defaultDeletionCost.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			flowSettingsPane.setExpanded(true);
			defaultDeletionCost.requestFocus();
			valid = false;
		}
		if (defaultNonWritingCost.getText().trim().length() == 0) {
			defaultNonWritingCost.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			dataSettingsPane.setExpanded(true);
			defaultNonWritingCost.requestFocus();
			valid = false;
		}
		if (defaultFaultyValueCost.getText().trim().length() == 0) {
			defaultFaultyValueCost.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			dataSettingsPane.setExpanded(true);
			defaultFaultyValueCost.requestFocus();
			valid = false;
		}
		logger.debug("Result of parameter validation: {}", valid);
		return valid;
	}

	private boolean validateConstraints(List<String> constraintList) {
		ConformanceMethod conformanceMethod = methodChoice.getSelectionModel().getSelectedItem();

		switch (conformanceMethod) {
		case ANALYZER:
		case REPLAYER:
			for (String s : constraintList) {
				String name = s.substring(0, s.indexOf('['));
				ConstraintTemplate template = ConstraintTemplate.getByTemplateName(name);
				if (!TemplateUtils.getAnalyzerConformanceSupportedConstraints().contains(template)) {
					AlertUtils.showError(template + " is not valid template for " + conformanceMethod.getDisplayText());
					return false;
				}
			}
			return true;
		/*	
		case DATA_REPLAYER:
			break;
		*/	
		// TODO: Test plan-based conformance supported templates
		case PLAN_BASED:
			return false;

		default:
			return false;
		}
	}

	private void updateLegendAndStatistics(Task<ConformanceTaskResult> task) {

		if (task instanceof ConformanceTaskAnalyzer) {
			legendVBox.setVisible(true);
			statisticsVBox.setVisible(true);
			firstLegendLabel.setText("Fulfilled traces:");
			firstStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.FULFILLMENTS));
			secondLegendLabel.setText("Violated traces:");
			secondStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.VIOLATIONS));
			thirdLegendLabel.setVisible(false);
			thirdLegendLabel.setManaged(false);
			thirdStatisticLabel.setVisible(false);
			thirdStatisticLabel.setManaged(false);
			globalLegendLabel.setText("Vacuous traces:");
			globalStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.VACUOUS_FULFILLMENTS));
			totalTracesLabel.setText(Integer.toString(task.getValue().getTraceList().size()));

			fulfillmentLegendEntry.setVisible(true);
			fulfillmentLegendEntry.setManaged(true);
			violationLegendEntry.setVisible(true);
			violationLegendEntry.setManaged(true);
			vacFulfillmentLegendEntry.setVisible(true);
			vacFulfillmentLegendEntry.setManaged(true);
			vacViolationLegendEntry.setVisible(true);
			vacViolationLegendEntry.setManaged(true);
			insertionLegendEntry.setVisible(false);
			insertionLegendEntry.setManaged(false);
			deletionLegendEntry.setVisible(false);
			deletionLegendEntry.setManaged(false);
			dataDifferenceLegendEntry.setVisible(false);
			dataDifferenceLegendEntry.setManaged(false);

		} else if (task instanceof ConformanceTaskReplayer) {
			legendVBox.setVisible(true);
			statisticsVBox.setVisible(true);
			firstLegendLabel.setText("Insertion:");
			firstStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.INSERTIONS));
			secondLegendLabel.setText("Deletion:");
			secondStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.DELETIONS));
			thirdLegendLabel.setVisible(false);
			thirdLegendLabel.setManaged(false);
			thirdStatisticLabel.setVisible(false);
			thirdStatisticLabel.setManaged(false);
			globalLegendLabel.setText("Average Fitnes:");
			globalStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.FITNESS));
			totalTracesLabel.setText(Integer.toString(task.getValue().getTraceList().size()));

			fulfillmentLegendEntry.setVisible(false);
			fulfillmentLegendEntry.setManaged(false);
			violationLegendEntry.setVisible(false);
			violationLegendEntry.setManaged(false);
			vacFulfillmentLegendEntry.setVisible(false);
			vacFulfillmentLegendEntry.setManaged(false);
			vacViolationLegendEntry.setVisible(false);
			vacViolationLegendEntry.setManaged(false);
			insertionLegendEntry.setVisible(true);
			insertionLegendEntry.setManaged(true);
			deletionLegendEntry.setVisible(true);
			deletionLegendEntry.setManaged(true);
			dataDifferenceLegendEntry.setVisible(false);
			dataDifferenceLegendEntry.setManaged(false);

		} else if (task instanceof ConformanceTaskDataAwareReplayer) {
			legendVBox.setVisible(true);
			statisticsVBox.setVisible(true);
			firstLegendLabel.setText("Insertion:");
			firstStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.INSERTIONS));
			secondLegendLabel.setText("Deletion:");
			secondStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.DELETIONS));
			thirdLegendLabel.setVisible(true);
			thirdLegendLabel.setManaged(true);
			thirdLegendLabel.setText("Data-difference:");
			thirdStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.DATA_DIFFERENCES));
			thirdStatisticLabel.setVisible(true);
			thirdStatisticLabel.setManaged(true);
			globalLegendLabel.setText("Average Fitnes:");
			globalStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.FITNESS));
			totalTracesLabel.setText(Integer.toString(task.getValue().getTraceList().size()));

			fulfillmentLegendEntry.setVisible(false);
			fulfillmentLegendEntry.setManaged(false);
			violationLegendEntry.setVisible(false);
			violationLegendEntry.setManaged(false);
			vacFulfillmentLegendEntry.setVisible(false);
			vacFulfillmentLegendEntry.setManaged(false);
			vacViolationLegendEntry.setVisible(false);
			vacViolationLegendEntry.setManaged(false);
			insertionLegendEntry.setVisible(true);
			insertionLegendEntry.setManaged(true);
			deletionLegendEntry.setVisible(true);
			deletionLegendEntry.setManaged(true);
			dataDifferenceLegendEntry.setVisible(true);
			dataDifferenceLegendEntry.setManaged(true);

		} else if (task instanceof ConformanceTaskPlanBased) {
			legendVBox.setVisible(true);
			// statisticsVBox.setVisible(true);
			firstLegendLabel.setText("Insertion:");
			firstStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.INSERTIONS));
			secondLegendLabel.setText("Deletion:");
			secondStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.DELETIONS));
			thirdLegendLabel.setVisible(false);
			thirdLegendLabel.setManaged(false);
			thirdStatisticLabel.setVisible(false);
			thirdStatisticLabel.setManaged(false);
			// globalLegendLabel.setText("Average Fitnes:");
			// globalStatisticLabel.setText(task.getValue().getGlobalStatisticsMap().get(ConformanceStatisticType.FITNESS));
			totalTracesLabel.setText(Integer.toString(task.getValue().getTraceList().size()));

			fulfillmentLegendEntry.setVisible(false);
			fulfillmentLegendEntry.setManaged(false);
			violationLegendEntry.setVisible(false);
			violationLegendEntry.setManaged(false);
			vacFulfillmentLegendEntry.setVisible(false);
			vacFulfillmentLegendEntry.setManaged(false);
			vacViolationLegendEntry.setVisible(false);
			vacViolationLegendEntry.setManaged(false);
			insertionLegendEntry.setVisible(true);
			insertionLegendEntry.setManaged(true);
			deletionLegendEntry.setVisible(true);
			deletionLegendEntry.setManaged(true);
			dataDifferenceLegendEntry.setVisible(false);
			dataDifferenceLegendEntry.setManaged(false);

		} else {
			legendVBox.setVisible(false);
			statisticsVBox.setVisible(false);
		}
	}

	@FXML
	private void exportStatistics() {
		File chosenFile = FileUtils.showCsvSaveDialog(this.getStage(), null);

		if(chosenFile != null) {
			List<ConformanceTaskResultGroup> results = new LinkedList<>();

			if (groupByTracesButton.getPseudoClassStates().contains(selectedClass))	{ // Results are grouped by traces
				results = conformanceTaskResult.getResultsGroupedByTrace();
			} else if (groupByConstraintsButton.getPseudoClassStates().contains(selectedClass))	{ // Results are grouped by constraints
				results = conformanceTaskResult.getResultsGroupedByConstraint();
			}
			List<Map<ConformanceStatisticType, String>> statistics = results.stream()
					.map(ConformanceTaskResultGroup::getGroupStatistics)
					.collect(Collectors.toList());
			
			
			logger.debug("Exporting statistics to file: {}", chosenFile.getAbsolutePath());
			try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(chosenFile.getAbsolutePath()))) {
				writer.write(ModelExporter.getConformanceStatisticsAsCsv(statistics));
				logger.info("Statistics exported to file: {}", chosenFile.getAbsolutePath());
				AlertUtils.showSuccess("Statistics successfully exported");
			} catch (IOException e) {
				AlertUtils.showError("Exporting statistics failed!");
				logger.error("Unable to export statistics: {}", chosenFile.getAbsolutePath(), e);
			}
		}
	}
	
	
	@FXML
	private void exportReport() {
		File chosenFile = FileUtils.showCsvSaveDialog(this.getStage(), null);
		
		if(chosenFile != null) {
			List<ConformanceTaskResultGroup> results = new LinkedList<>();
			
			if (groupByTracesButton.getPseudoClassStates().contains(selectedClass))	{ // Results are grouped by traces
				results = conformanceTaskResult.getResultsGroupedByTrace();
			} else if (groupByConstraintsButton.getPseudoClassStates().contains(selectedClass))	{ // Results are grouped by constraints
				results = conformanceTaskResult.getResultsGroupedByConstraint();
			}
			
			List<Map<String,String>> reportData = new ArrayList<Map<String,String>>();
			
			for (ConformanceTaskResultGroup resultGroup : results) {
				for (ConformanceTaskResultDetail groupDetail : resultGroup.getGroupDetails()) {
					if (groupDetail.getVacuousConformance().getType() == ActivityConformanceType.Type.FULFILLMENT || groupDetail.getVacuousConformance().getType() == ActivityConformanceType.Type.VIOLATION) {
						Map<String, String> reportRow = new LinkedHashMap<String, String>();
						reportRow.put("Trace", groupDetail.getTraceName());
						reportRow.put("Constraint", groupDetail.getConstraint().replace("\n", ""));
						reportRow.put("Result type", "vac. " + groupDetail.getVacuousConformance().getType().toString().toLowerCase());
						reportRow.put("Activity name", "");
						reportRow.put("Activity index", "");
						reportData.add(reportRow);
					} else {
						for (int i = 0; i < groupDetail.getActivityConformanceTypes().size(); i++) {
							ActivityConformanceType acType = groupDetail.getActivityConformanceTypes().get(i);
							if (acType.getType() == ActivityConformanceType.Type.FULFILLMENT || acType.getType() == ActivityConformanceType.Type.VIOLATION || acType.getType() == ActivityConformanceType.Type.INSERTION || acType.getType() == ActivityConformanceType.Type.DELETION || acType.getType() == ActivityConformanceType.Type.DATA_DIFFERENCE) {
								Map<String, String> reportRow = new LinkedHashMap<String, String>();
								reportRow.put("Trace", groupDetail.getTraceName());
								reportRow.put("Constraint", groupDetail.getConstraint().replace("\n", ""));
								reportRow.put("Result type", acType.getType().toString().toLowerCase());
								reportRow.put("Activity name", xce.extractName(groupDetail.getXtrace().get(i)));
								reportRow.put("Activity index", Integer.toString(i+1));
								reportData.add(reportRow);
							}
						}
					}
				}
			}
			
			logger.debug("Exporting report to file: {}", chosenFile.getAbsolutePath());
			try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(chosenFile.getAbsolutePath()))) {
				writer.write(ModelExporter.getConformanceReportAsCsv(reportData));
				logger.info("Report exported to file: {}", chosenFile.getAbsolutePath());
				AlertUtils.showSuccess("Report successfully exported");
			} catch (IOException e) {
				AlertUtils.showError("Exporting report failed!");
				logger.error("Unable to export report: {}", chosenFile.getAbsolutePath(), e);
			}
		}
	}

	@FXML
	private void exportFulfilledLog() {
		List<String> fulfilledTraceNames = new LinkedList<>();
		ConformanceMethod method = methodChoice.getSelectionModel().getSelectedItem();

		List<ConformanceTaskResultGroup> results = conformanceTaskResult.getResultsGroupedByTrace();
		for (ConformanceTaskResultGroup res : results) {
			String traceName = res.getXtrace().getAttributes().get(XConceptExtension.KEY_NAME).toString();

			switch (method) {
			case ANALYZER:
				String violationNum = res.getGroupStatistics().get(ConformanceStatisticType.VIOLATIONS);
				String vacuousViolationNum = res.getGroupStatistics().get(ConformanceStatisticType.VACUOUS_VIOLATIONS);

				if (Integer.parseInt(violationNum) <= 0 && Integer.parseInt(vacuousViolationNum) <= 0)
					fulfilledTraceNames.add(traceName);

				break;

			case REPLAYER:
			case DATA_REPLAYER:
				String fitness = res.getGroupStatistics().get(ConformanceStatisticType.FITNESS);

				if (Double.parseDouble(fitness) == 1.0)
					fulfilledTraceNames.add(traceName);

				break;

			default:
				logger.error("Unknown conformance method: {}", method);
				AlertUtils.showError("Unknown conformance method: " + method);
				break;
			}
		}

		if (!fulfilledTraceNames.isEmpty()) {
			//chosenFile might be null, because the user can just close the fileChooser instead of choosing a file
			File chosenFile = FileUtils.showXesSaveDialog(this.getStage(), null);

			if(chosenFile != null) {
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());

				XLog originalLog = LogUtils.convertToXlog(logFile);
				String newName;
				if (XConceptExtension.instance().extractName(originalLog) != null)
					newName = XConceptExtension.instance().extractName(originalLog) + " - Fulfilled traces";
				else
					newName = "Fulfilled traces extracted from: " + logFile.getName();

				List<XTrace> fulfilledTraces = originalLog.stream()
						.filter(trace -> fulfilledTraceNames.contains(trace.getAttributes().get(XConceptExtension.KEY_NAME).toString()))
						.collect(Collectors.toList());

				XLog fulfilledLog = new XLogImpl(new XAttributeMapImpl());
				fulfilledLog.addAll(fulfilledTraces);
				XConceptExtension.instance().assignName(fulfilledLog, newName);
				fulfilledLog.getExtensions().addAll(originalLog.getExtensions());
				fulfilledLog.getClassifiers().addAll(originalLog.getClassifiers());
				fulfilledLog.getGlobalEventAttributes().addAll(originalLog.getGlobalEventAttributes());
				fulfilledLog.getGlobalTraceAttributes().addAll(originalLog.getGlobalTraceAttributes());

				FileOutputStream outStream;
				try {
					outStream = new FileOutputStream(chosenFile.getAbsolutePath());
					new XesXmlSerializer().serialize(fulfilledLog, outStream);
					outStream.flush();
					outStream.close();
					logger.info("Log exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Log successfully exported");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the log failed!");
					logger.error("Unable to export log: {}", chosenFile.getAbsolutePath(), e);
				}
			}

		} else {
			AlertUtils.showWarning("This log doesn't contain fulfilled traces!");
		}
	}

	@FXML
	private void exportViolatedLog() {
		List<String> violatedTraceNames = new LinkedList<>();
		ConformanceMethod method = methodChoice.getSelectionModel().getSelectedItem();

		List<ConformanceTaskResultGroup> results = conformanceTaskResult.getResultsGroupedByTrace();
		for (ConformanceTaskResultGroup res : results) {
			String traceName = res.getXtrace().getAttributes().get(XConceptExtension.KEY_NAME).toString();

			switch (method) {
			case ANALYZER:
				String violationNum = res.getGroupStatistics().get(ConformanceStatisticType.VIOLATIONS);
				String vacuousViolationNum = res.getGroupStatistics().get(ConformanceStatisticType.VACUOUS_VIOLATIONS);

				if (Integer.parseInt(violationNum) > 0 || Integer.parseInt(vacuousViolationNum) > 0)
					violatedTraceNames.add(traceName);

				break;

			case REPLAYER:
			case DATA_REPLAYER:
				String fitness = res.getGroupStatistics().get(ConformanceStatisticType.FITNESS);

				if (Double.parseDouble(fitness) < 1.0)
					violatedTraceNames.add(traceName);

				break;

			default:
				logger.error("Unknown conformance method: {}", method);
				AlertUtils.showError("Unknown conformance method: " + method);
				break;
			}
		}

		if (!violatedTraceNames.isEmpty()) {
			//chosenFile might be null, because the user can just close the fileChooser instead of choosing a file
			File chosenFile = FileUtils.showXesSaveDialog(this.getStage(), null);

			if(chosenFile != null) {
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());

				XLog originalLog = LogUtils.convertToXlog(logFile);
				String newName;
				if (XConceptExtension.instance().extractName(originalLog) != null)
					newName = XConceptExtension.instance().extractName(originalLog) + " - Violated traces";
				else
					newName = "Violated traces extracted from: " + logFile.getName();

				List<XTrace> violatedTraces = originalLog.stream()
						.filter(trace -> violatedTraceNames.contains(trace.getAttributes().get(XConceptExtension.KEY_NAME).toString()))
						.collect(Collectors.toList());

				XLog violatedLog = new XLogImpl(new XAttributeMapImpl());
				violatedLog.addAll(violatedTraces);
				XConceptExtension.instance().assignName(violatedLog, newName);
				violatedLog.getExtensions().addAll(originalLog.getExtensions());
				violatedLog.getClassifiers().addAll(originalLog.getClassifiers());
				violatedLog.getGlobalEventAttributes().addAll(originalLog.getGlobalEventAttributes());
				violatedLog.getGlobalTraceAttributes().addAll(originalLog.getGlobalTraceAttributes());

				FileOutputStream outStream;
				try {
					outStream = new FileOutputStream(chosenFile.getAbsolutePath());
					new XesXmlSerializer().serialize(violatedLog, outStream);
					outStream.flush();
					outStream.close();
					logger.info("Log exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Log successfully exported");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the log failed!");
					logger.error("Unable to export log: {}", chosenFile.getAbsolutePath(), e);
				}
			}

		} else {
			AlertUtils.showWarning("This log doesn't contain violated traces!");
		}
	}

	@FXML
	private void exportAlignedLog() {
		XLog originalLog = LogUtils.convertToXlog(logFile);

		String newName;
		if (XConceptExtension.instance().extractName(originalLog) != null)
		 	newName = XConceptExtension.instance().extractName(originalLog) + " - Aligned";
		else
			newName = "Aligned log extracted from: " + logFile.getName();

		XLog alignedLog = new XLogImpl(new XAttributeMapImpl());
		XConceptExtension.instance().assignName(alignedLog, newName);
		alignedLog.getExtensions().addAll(originalLog.getExtensions());
		alignedLog.getClassifiers().addAll(originalLog.getClassifiers());
		alignedLog.getGlobalEventAttributes().addAll(originalLog.getGlobalEventAttributes());
		alignedLog.getGlobalTraceAttributes().addAll(originalLog.getGlobalTraceAttributes());

		List<ConformanceTaskResultGroup> results = conformanceTaskResult.getResultsGroupedByTrace();
		for (ConformanceTaskResultGroup res : results) {
			XTrace alignedTrace = new XTraceImpl(new XAttributeMapImpl());
			XTrace resTrace = res.getXtrace();

			for (Map.Entry<String, XAttribute> entry : resTrace.getAttributes().entrySet())
				alignedTrace.getAttributes().put(entry.getKey(), entry.getValue());

			List<ActivityConformanceType> conformanceTypes = res.getGroupDetails().get(0).getActivityConformanceTypes();
			for (int i=0; i<conformanceTypes.size(); i++)
				if (conformanceTypes.get(i).getType() != ActivityConformanceType.Type.DELETION_OTHER
				&& conformanceTypes.get(i).getType() != ActivityConformanceType.Type.DELETION)
					alignedTrace.add(resTrace.get(i));

			alignedLog.add(alignedTrace);
		}

		if (!alignedLog.isEmpty()) {
			//chosenFile might be null, because the user can just close the fileChooser instead of choosing a file
			File chosenFile = FileUtils.showXesSaveDialog(this.getStage(), null);

			if(chosenFile != null) {
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());
				FileOutputStream outStream;
				try {
					outStream = new FileOutputStream(chosenFile.getAbsolutePath());
					new XesXmlSerializer().serialize(alignedLog, outStream);
					outStream.flush();
					outStream.close();
					logger.info("Log exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Log successfully exported");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the log failed!");
					logger.error("Unable to export log: {}", chosenFile.getAbsolutePath(), e);
				}
			}

		} else {
			AlertUtils.showWarning("This log doesn't contain aligned traces!");
		}
	}

	private Task<ConformanceTaskResult> createConformanceCheckingTask() {
		Task<ConformanceTaskResult> task = null;

		ConformanceMethod selectedMethod = methodChoice.getSelectionModel().getSelectedItem();
		switch (selectedMethod) {
		case ANALYZER:
			ConformanceTaskAnalyzer conformanceTaskAnalyzer = new ConformanceTaskAnalyzer();
			conformanceTaskAnalyzer.setXmlModel(xmlModel);
			conformanceTaskAnalyzer.setLogFile(logFile);
			task = conformanceTaskAnalyzer;
			break;
		case REPLAYER:
			ConformanceTaskReplayer conformanceTaskReplayer = new ConformanceTaskReplayer();
			conformanceTaskReplayer.setXmlModel(xmlModel);
			conformanceTaskReplayer.setLogFile(logFile);
			conformanceTaskReplayer.setActivityMapping(activityMappingProperty.getValue().getActivityMapping());
			conformanceTaskReplayer.setActivityMatchCosts(flowSettingsController.getActivityMatchCosts());
			task = conformanceTaskReplayer;
			break;
		case PLAN_BASED:
			// TODO integrate planner tool jar, siin peaks olema ka algoritmi valik
			// MARCUS - siin peaks kutsuma valja minu tehtud jari
			ConformanceTaskPlanBased conformanceTaskPlanBased = new ConformanceTaskPlanBased();
			conformanceTaskPlanBased.setXmlModel(xmlModel);
			conformanceTaskPlanBased.setLogFile(logFile);
			conformanceTaskPlanBased.setActivityMapping(activityMappingProperty.getValue().getActivityMapping());
			conformanceTaskPlanBased.setActivityMatchCosts(flowSettingsController.getActivityMatchCosts());
			if (symbaButton.isSelected()) {
				conformanceTaskPlanBased.setAlgorithm(PlannerAlgorithm.SYMBA);
			}
			if (fastDownwardButton.isSelected()) {
				conformanceTaskPlanBased.setAlgorithm(PlannerAlgorithm.FAST_DOWNWARD);
			}
			task = conformanceTaskPlanBased;
			break;
		case DATA_REPLAYER:
			ConformanceTaskDataAwareReplayer conformanceTaskDataAwareReplayer = new ConformanceTaskDataAwareReplayer();
			conformanceTaskDataAwareReplayer.setXmlModel(xmlModel);
			conformanceTaskDataAwareReplayer.setLogFile(logFile);
			conformanceTaskDataAwareReplayer.setActivityMapping(activityMappingProperty.getValue().getActivityMapping());
			conformanceTaskDataAwareReplayer.setActivityMatchCosts(flowSettingsController.getActivityMatchCosts());
			conformanceTaskDataAwareReplayer.setVariableMatchCosts(dataSettingsController.getVariableMatchCosts());
			task = conformanceTaskDataAwareReplayer;
			break;
		default:
			//TODO: Show error to user
			logger.error("Can not create conformance checking task for unhandled method: {}", selectedMethod);
			break;
		}
		return task;
	}

	private void addHandlersToTask(Task<ConformanceTaskResult> task, Node progressLayer, ProgressLayerController progressLayerController) {
		//Handle cancelling the task
		progressLayerController.getCancelButton().setOnAction(e -> {
			task.cancel(true);
			rootRegion.getChildren().remove(progressLayer);
			mainContents.setDisable(false);
		});

		//Handle task success
		task.setOnSucceeded(event -> {

			if (task instanceof ConformanceTaskReplayer || task instanceof ConformanceTaskDataAwareReplayer || task instanceof ConformanceTaskPlanBased)
				isAlignment = true;
			else
				isAlignment = false;


			conformanceTaskResult = task.getValue();
			if (groupByTracesButton.getPseudoClassStates().contains(selectedClass)) {
				groupByTraces();
				resultGroupsListView.getItems().setAll(conformanceTaskResult.getResultsGroupedByTrace());

			} else if (groupByConstraintsButton.getPseudoClassStates().contains(selectedClass)) {
				groupByConstraints();
				resultGroupsListView.getItems().setAll(conformanceTaskResult.getResultsGroupedByConstraint());
			}

			updateTraceSortChoice(methodChoice.getSelectionModel().getSelectedItem());

			rootRegion.getChildren().remove(progressLayer);
			eventsListView.getItems().clear();
			eventsHeaderVBox.setVisible(false);
			if (!resultsPane.isVisible()) {
				resultsPane.setVisible(true);
				groupByTraces();
			}
			mainContents.setDisable(false);

			//TODO: Find a better way of setting the visibility of Show Original Trace selection
			//boolean showOriginalTraceSelection = task instanceof ConformanceTaskReplayer || task instanceof ConformanceTaskDataAwareReplayer || task instanceof ConformanceTaskPlanBased;
			applyAlignmentHBox.setVisible(isAlignment);
			applyAlignmentHBox.setManaged(isAlignment);
			exportAlignedLogButton.setVisible(isAlignment);
			exportAlignedLogButton.setManaged(isAlignment);
			if (isAlignment) {
				exportFulfilledLogButton.setText("Export fitting traces");
				exportViolatedLogButton.setText("Export non-fitting traces");
			} else {
				exportFulfilledLogButton.setText("Export fulfilled traces");
				exportViolatedLogButton.setText("Export violated traces");
			}
			
			updateLegendAndStatistics(task);
		});

		//Handle task failure
		task.setOnFailed(event -> {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
				Node alertLayer = loader.load();
				AlertLayerController alertLayerController = loader.getController();
				alertLayerController.setAlertMessage(AlertType.ERROR, "Conformance checking failed");
				alertLayerController.getOkButton().setOnAction(e -> {
					rootRegion.getChildren().remove(alertLayer);
					mainContents.setDisable(false);
				});
				rootRegion.getChildren().remove(progressLayer);
				rootRegion.getChildren().add(alertLayer);
			} catch (IOException | IllegalStateException e) {
				logger.error("Can not load alert layer", e);
				//If alert layer can not be displayed then use regular alert instead
				AlertUtils.showWarning("Conformance checking failed");
				rootRegion.getChildren().remove(progressLayer);
				mainContents.setDisable(false);
			}
		});
	}
}
