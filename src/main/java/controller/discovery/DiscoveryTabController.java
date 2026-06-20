package controller.discovery;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.ToggleSwitch;
import org.deckfour.xes.model.XLog;
import org.kordamp.ikonli.javafx.FontIcon;
import org.w3c.dom.Element;

import controller.common.AbstractController;
import controller.common.layers.AlertLayerController;
import controller.common.layers.AlertLayerController.AlertType;
import controller.common.layers.ProgressLayerController;
import controller.discovery.data.DiscoveredActivity;
import controller.discovery.data.DiscoveredConstraint;
import global.Inventory;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import gui.ava.html.Html2Image;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javafx.util.StringConverter;
import minerful.MinerFulSimplificationLauncher;
import minerful.concept.ProcessModel;
import minerful.index.comparator.modular.ConstraintSortingPolicy;
import minerful.postprocessing.params.PostProcessingCmdParameters;
import minerful.postprocessing.params.PostProcessingCmdParameters.PostProcessingAnalysisType;
import netscape.javascript.JSObject;
import task.discovery.DiscoveryTaskDeclare;
import task.discovery.DiscoveryTaskMinerful;
import task.discovery.DiscoveryTaskResult;
import task.discovery.mp_enhancer.MpEnhancer;
import util.AlertUtils;
import util.ConstraintUtils;
import util.FileUtils;
import util.GraphGenerator;
import util.LogUtils;
import util.ModelExportChoice;
import util.ModelExporter;
import util.ModelViewType;
import util.ValidationUtils;

public class DiscoveryTabController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");

	@FXML
	private StackPane rootStackPane;
	@FXML
	private HBox mainContents;
	@FXML
	private VBox parametersSection;
	@FXML
	private ChoiceBox<DiscoveryMethod> methodChoice;
	@FXML
	private Button toggleTemplateSettingsButton;
	@FXML
	private Label noTemplatesSelectedError;
	@FXML
	private TitledPane generalParametersPane;
	@FXML
	private Spinner<Integer> minConstraintSupportSpinner;
	@FXML
	private Label pruningLabel;
	@FXML
	private ChoiceBox<DeclarePruningType> declarePruningChoice;
	@FXML
	private ChoiceBox<PostProcessingAnalysisType> minerfulPruningChoice;
	@FXML
	private ToggleSwitch vacuityAsViolationToggle;
	@FXML
	private ToggleSwitch considerLifecycleToggle;
	@FXML
	private ToggleSwitch timeConditionDiscoveryToggle;
	@FXML
	private ChoiceBox<DataConditionType> dataConditionsChoice;
	@FXML
	private StackPane settingsLayer;
	@FXML
	private VBox resultsPane;
	@FXML
	private Slider zoomSlider;
	@FXML
	private TextField zoomValueField;
	@FXML
	private ChoiceBox<ModelViewType> modelViewChoiceBox;
	@FXML
	private HBox saveButtonsHbox;
	@FXML
	private Button snapshotButton;
	@FXML
	private Slider activityFilterSlider;
	@FXML
	private TextField activityFilterField;
	@FXML
	private HBox constraintFilterBox;
	@FXML
	private Slider constraintFilterSlider;
	@FXML
	private TextField constraintFilterField;
	@FXML
	private HBox constraintLabelsHBox;
	@FXML
	private HBox alternativeLayoutHbox;
	@FXML
	private ToggleSwitch showConstraintsToggle;
	@FXML
	private ChoiceBox<ShowLabelEnum> showLabelsChoice;
	@FXML
	private ToggleSwitch alternativeLayoutToggle;
	@FXML
	private ToggleSwitch showAllActivitiesToggle;
	@FXML
	private WebView visualizationWebView;

	private static String precentageFormat = "%.1f";

	private TemplateSettingsController templateSettingsController;
	private TranslateTransition templateSettingsSlideTransition;

	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	//Prevents slider value properties from being garbage collected
	private ObjectProperty<Double> activityFilterObject;
	private ObjectProperty<Double> constraintFilterObject;
	private ObjectProperty<Double> zoomSliderValueObject;
	private ObjectProperty<Double> visualizationZoomObject;

	private double valueAtFilterFocusGained;	// Used to check if visualization needs to be updated because of filter change

	private List<DiscoveredActivity> filteredActivities;
	private List<DiscoveredConstraint> filteredConstraints;

	private File logFile;

	private DiscoveryTaskResult discoveryTaskResult;
	private String initialWebViewScript;
	private boolean isDataAwareDiscovery;
	private DiscoveryStatus discoveryStatus;

	@FXML
	private void initialize() {
		prepareTemplateSettingsLayer(DiscoveryMethod.DECLARE);	// No matters what is the DiscoveryMethod here
		
		initializeListeners();
		setupSettings();
		setupVisualizationWebView();

		parametersSection.setViewOrder(-1);	// Makes sure that template settings slide in from under the parameters

		resultsPane.setVisible(false);

		logger.debug("Discovery tab initialized");
	}
	
	private void prepareTemplateSettingsLayer(DiscoveryMethod currentMethod) {
		// Setting the results pane and toggle buttons to change based on settings pane children
		settingsLayer.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
			while (change.next()) {
				resultsPane.setDisable(change.getList().size() > 1);
				if (change.getAddedSubList().contains(templateSettingsController.getRootRegion())) {
					toggleTemplateSettingsButton.setText("Minimize templates");
					toggleTemplateSettingsButton.pseudoClassStateChanged(selectedClass, true);
					((FontIcon)toggleTemplateSettingsButton.getGraphic()).setIconLiteral("fa-angle-double-left");
				} else if (change.getRemoved().contains(templateSettingsController.getRootRegion())) {
					toggleTemplateSettingsButton.setText("Show templates");
					toggleTemplateSettingsButton.pseudoClassStateChanged(selectedClass, false);
					((FontIcon)toggleTemplateSettingsButton.getGraphic()).setIconLiteral("fa-angle-double-right");
				}
			}
		});

		// Preloading template settings
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/discovery/TemplateSettings.fxml"));
			loader.load();
			templateSettingsController = loader.getController();
			templateSettingsController.setAvailableTemplates(currentMethod, timeConditionDiscoveryToggle.isSelected());

			// Preparation for slide in animation
			Region templateSettingsRootRegion = templateSettingsController.getRootRegion();
			templateSettingsSlideTransition = new TranslateTransition(new Duration(300), templateSettingsRootRegion);
			templateSettingsSlideTransition.setFromX(-1 * templateSettingsRootRegion.getMaxWidth());
			templateSettingsSlideTransition.setToX(-1); //-1 so that it would cover the parameters section border

		} catch (IOException | IllegalStateException e) {
			logger.error("Can not load template settings layer", e);
			//TODO: Alert the user
			toggleTemplateSettingsButton.setDisable(true);
		}
		
		templateSettingsController.getCloseButton().setOnAction(event -> hideTemplateSettings() );
	}
	
	private void initializeListeners() {
		// Visibility of settings panes based on selected method and updating template settings
		methodChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			
			DataConditionType dct = dataConditionsChoice.getSelectionModel().getSelectedItem();
			
			DiscoveryMethod currentMethod = getSelectedMethod(dct!=null && dct.isDataAware(), newV);
			switch (currentMethod) {
			
			case DECLARE:
			case MP_DECLARE:
				pruningLabel.setText("Pruning Type (Declare):");
				declarePruningChoice.setVisible(true);
				minerfulPruningChoice.setVisible(false);
				vacuityAsViolationToggle.setDisable(false);
				considerLifecycleToggle.setDisable(false);
				break;
			
			case MINERFUL:
			case MP_MINERFUL:
				pruningLabel.setText("Pruning Type (MINERful):");
				declarePruningChoice.setVisible(false);
				minerfulPruningChoice.setVisible(true);
				vacuityAsViolationToggle.setDisable(true);
				considerLifecycleToggle.setDisable(true);
				break;
				
			default:
				//TODO: Disable discovery and show error to user
				logger.error("Unhandled discovery method selected: {}", currentMethod);
				break;
			}
			
			templateSettingsController.setAvailableTemplates(currentMethod, timeConditionDiscoveryToggle.isSelected());
			boolean areSelectedTemplatesEmpty = templateSettingsController.getSelectedTemplates().isEmpty();
			noTemplatesSelectedError.setVisible( areSelectedTemplatesEmpty );
		});
		
		
		modelViewChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if (newV == ModelViewType.DECLARE) {
				constraintLabelsHBox.setDisable(false);
				alternativeLayoutHbox.setDisable(false);
			} else if (newV == ModelViewType.TEXTUAL) {
				constraintLabelsHBox.setDisable(true);
				alternativeLayoutHbox.setDisable(true);
			} else if (newV == ModelViewType.AUTOMATON) {
				constraintLabelsHBox.setDisable(true);
				alternativeLayoutHbox.setDisable(false);
			} else {
				logger.error("Filtering layout unhandeled for: {}", newV);
			}
			updateVisualization();
		});
		
		
		showLabelsChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) ->  updateVisualization() );
		
		dataConditionsChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
			DiscoveryMethod currentMethod = getSelectedMethod(newV.isDataAware(), methodChoice.getSelectionModel().getSelectedItem());
			templateSettingsController.setAvailableTemplates(currentMethod, timeConditionDiscoveryToggle.isSelected());
		});
		
		
		timeConditionDiscoveryToggle.selectedProperty().addListener((ov,oldV,newV) -> {
			DataConditionType dct = dataConditionsChoice.getSelectionModel().getSelectedItem();
			DiscoveryMethod currentMethod = getSelectedMethod(dct!=null && dct.isDataAware(), methodChoice.getSelectionModel().getSelectedItem());
			templateSettingsController.setAvailableTemplates(currentMethod, newV);
		});
		
		showConstraintsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (modelViewChoiceBox.getSelectionModel().getSelectedItem() == ModelViewType.DECLARE)
				updateVisualization();
		});
		
		alternativeLayoutToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (modelViewChoiceBox.getSelectionModel().getSelectedItem() == ModelViewType.DECLARE || modelViewChoiceBox.getSelectionModel().getSelectedItem() == ModelViewType.AUTOMATON)
				updateVisualization();
		});
		
		showAllActivitiesToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			updateFilteredResultLists();
			updateVisualization();
		});
	}
	
	private void setupSettings() {
		methodChoice.getItems().setAll(FXCollections.observableArrayList(DiscoveryMethod.DECLARE, DiscoveryMethod.MINERFUL));
		methodChoice.getSelectionModel().selectFirst();
		methodChoice.setConverter(new StringConverter<DiscoveryMethod>() {
			@Override
			public String toString(DiscoveryMethod discoveryMethod) {
				return discoveryMethod.getDisplayText();
			}
			@Override
			public DiscoveryMethod fromString(String string) {
				return null;
			}
		});
		
		noTemplatesSelectedError.setVisible(false);
		
		declarePruningChoice.getItems().setAll(DeclarePruningType.values());
		declarePruningChoice.getSelectionModel().selectFirst();
		declarePruningChoice.setConverter(new StringConverter<DeclarePruningType>() {
			@Override
			public String toString(DeclarePruningType declarePruningType) {
				return declarePruningType.getDisplayText();
			}
			@Override
			public DeclarePruningType fromString(String string) {
				return null;
			}
		});

		minerfulPruningChoice.getItems().setAll(PostProcessingAnalysisType.values());
		minerfulPruningChoice.getSelectionModel().select(1);	// Selecting Hierarchy as default choice
		minerfulPruningChoice.setConverter(new StringConverter<PostProcessingAnalysisType>() {
			@Override
			public String toString(PostProcessingAnalysisType minerfulPruningType) {
				switch (minerfulPruningType) {
				case NONE:
					return "None";
				case HIERARCHY:
					return "Hierarchy";
				case HIERARCHYCONFLICT:
					return "Conflicts";
				case HIERARCHYCONFLICTREDUNDANCY:
					return "Redundancy";
				case HIERARCHYCONFLICTREDUNDANCYDOUBLE:
					return "Double redundancy";
				
				default:
					return "Not yet implemented";
				}
			}
			@Override
			public PostProcessingAnalysisType fromString(String string) {
				return null;
			}
		});
		
		vacuityAsViolationToggle.setSelected(true);
		considerLifecycleToggle.setSelected(false);
		timeConditionDiscoveryToggle.setSelected(false);
		
		dataConditionsChoice.getItems().setAll(DataConditionType.values());
		dataConditionsChoice.getSelectionModel().selectLast();
		dataConditionsChoice.setConverter(new StringConverter<DataConditionType>() {
			@Override
			public String toString(DataConditionType minerfulPruningType) {
				return minerfulPruningType.getDisplayText();
			}
			@Override
			public DataConditionType fromString(String string) {
				return null;
			}
		});
		
		minConstraintSupportSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 100, 90, 1)); // Causes NPE on empty input
		ValidationUtils.addMandatoryPositiveIntegerBehavior(minConstraintSupportSpinner);
		
		modelViewChoiceBox.getItems().setAll(ModelViewType.DECLARE, ModelViewType.TEXTUAL, ModelViewType.AUTOMATON);
		modelViewChoiceBox.getSelectionModel().selectFirst();
		modelViewChoiceBox.setConverter(new StringConverter<ModelViewType>() {
			@Override
			public String toString(ModelViewType modelViewType) {
				return modelViewType.getDisplayText();
			}
			@Override
			public ModelViewType fromString(String string) {
				return null;
			}
		});
		
		setupSliders();
		
		showAllActivitiesToggle.setSelected(false);
		showConstraintsToggle.setSelected(true);
		
		showLabelsChoice.getItems().setAll(ShowLabelEnum.SUPPORT, ShowLabelEnum.NONE);
		showLabelsChoice.getSelectionModel().selectFirst();
		showLabelsChoice.setConverter(new StringConverter<ShowLabelEnum>() {
			@Override
			public String toString(ShowLabelEnum label) {
				return label.getDisplayText();
			}
			@Override
			public ShowLabelEnum fromString(String string) {
				return null;
			}
		});
		
		alternativeLayoutToggle.setSelected(false);
	}
	
	private void setupSliders() {
		activityFilterObject = activityFilterSlider.valueProperty().asObject();
		Bindings.bindBidirectional(activityFilterField.textProperty(), activityFilterObject, new StringConverter<Double>() {
			@Override
			public String toString(Double object) {
				return String.format(precentageFormat, object.doubleValue());
			}
			@Override
			public Double fromString(String string) {
				try {
					return Double.parseDouble(string);
				} catch (NumberFormatException e) {
					//logger.debug("Invalid activity filter value: {}", string, e);
					return activityFilterSlider.getMax(); //Defaulting to max because its easier to handle visualization this way
				}
			}
		});

		activityFilterSlider.focusedProperty().addListener((observable, oldValue, newValue) -> 
			valueAtFilterFocusGained = activityFilterObject.getValue()
		);

		activityFilterSlider.setOnMouseReleased(event -> {
			if (valueAtFilterFocusGained != activityFilterObject.getValue()) {
				updateFilteredResultLists();
				updateVisualization();
				valueAtFilterFocusGained = activityFilterObject.getValue();
			}
		});
		
		activityFilterSlider.setOnKeyReleased(event -> {
			if(event.getCode().isArrowKey() && valueAtFilterFocusGained != activityFilterObject.getValue()) {
				updateFilteredResultLists();
				updateVisualization();
				valueAtFilterFocusGained = activityFilterObject.getValue();
			}
		});

		activityFilterField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(Boolean.TRUE)) {
				valueAtFilterFocusGained = activityFilterObject.getValue();
			} else if (valueAtFilterFocusGained != activityFilterObject.getValue()) {
				updateFilteredResultLists();
				updateVisualization();
				valueAtFilterFocusGained = activityFilterObject.getValue();
			}
		});
		activityFilterField.setOnKeyReleased(event -> {
			if(event.getCode() == KeyCode.ENTER && valueAtFilterFocusGained != activityFilterObject.getValue()) {
				updateFilteredResultLists();
				updateVisualization();
				valueAtFilterFocusGained = activityFilterObject.getValue();
			}
		});


		constraintFilterObject = constraintFilterSlider.valueProperty().asObject();
		Bindings.bindBidirectional(constraintFilterField.textProperty(), constraintFilterObject, new StringConverter<Double>() {
			@Override
			public String toString(Double object) {
				return String.format(precentageFormat, object.doubleValue());
			}
			@Override
			public Double fromString(String string) {
				try {
					return Double.parseDouble(string);
				} catch (NumberFormatException e) {
					//logger.debug("Invalid constraint filter value: " + string, e);
					return constraintFilterSlider.getMax(); //Defaulting to max because its easier to handle visualization this way
				}
			}
		});

		constraintFilterSlider.focusedProperty().addListener((observable, oldValue, newValue) ->
			valueAtFilterFocusGained = constraintFilterObject.getValue()
		);

		constraintFilterSlider.setOnMouseReleased(event -> {
			if (valueAtFilterFocusGained != constraintFilterObject.getValue()) {
				updateFilteredResultLists();
				updateVisualization();
				valueAtFilterFocusGained = constraintFilterObject.getValue();
			}
		});

		constraintFilterSlider.setOnKeyReleased(event -> {
			if(event.getCode().isArrowKey() && valueAtFilterFocusGained != constraintFilterObject.getValue()) {
				updateFilteredResultLists();
				updateVisualization();
				valueAtFilterFocusGained = constraintFilterObject.getValue();
			}
		});

		constraintFilterField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(Boolean.TRUE)) {
				valueAtFilterFocusGained = constraintFilterObject.getValue();
			} else if (valueAtFilterFocusGained != constraintFilterObject.getValue()) {
				updateFilteredResultLists();
				updateVisualization();
				valueAtFilterFocusGained = constraintFilterObject.getValue();
			}
		});
		constraintFilterField.setOnKeyPressed(event -> {
			if(event.getCode() == KeyCode.ENTER && valueAtFilterFocusGained != constraintFilterObject.getValue()) {
				updateFilteredResultLists();
				updateVisualization();
				valueAtFilterFocusGained = constraintFilterObject.getValue();
			}
		});

		ValidationUtils.addMandatoryPrecentageBehavior(precentageFormat, 100d, constraintFilterField, activityFilterField);
	}
	
	private DiscoveryMethod getSelectedMethod(boolean isMP, DiscoveryMethod method) {
		switch (method) {
		case DECLARE:
			return isMP ? DiscoveryMethod.MP_DECLARE : DiscoveryMethod.DECLARE;
		
		case MINERFUL:
			return isMP ? DiscoveryMethod.MP_MINERFUL : DiscoveryMethod.MINERFUL;
		
		default:
			//TODO: Disable discovery and show error to user
			logger.error("Unhandled discovery method selected: {}", method);
			return null;
		}
	}
	
	private void setupVisualizationWebView() {
		visualizationWebView.getEngine().load((getClass().getClassLoader().getResource("test.html")).toString());
		visualizationWebView.setContextMenuEnabled(false); //Setting it in FXML causes an IllegalArgumentException
		JSObject window = (JSObject) visualizationWebView.getEngine().executeScript("window");
		window.setMember("rum_application", this); //Allows calling public methods of this class from JavaScript

		visualizationWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue == Worker.State.SUCCEEDED && initialWebViewScript != null) {
				logger.debug("Updating visualization in discovery tab: {}", StringUtils.abbreviate(initialWebViewScript, 1000));
				visualizationWebView.getEngine().executeScript(initialWebViewScript);
			}
			logger.debug("Visualization html loaded in discovery tab");
		});

		visualizationWebView.addEventFilter(ScrollEvent.SCROLL, e -> {
			if (e.isControlDown()) {
				double deltaY = e.getDeltaY();
				//Setting the value of zoom slider (instead of WebView), because then the slider also defines min and max zoom levels
				if (deltaY > 0) {
					zoomSlider.setValue(zoomSlider.getValue() + 0.1d);
				} else if (deltaY < 0) {
					zoomSlider.setValue(zoomSlider.getValue() - 0.1d);
				}
				e.consume();
			}
		});

		zoomSliderValueObject = zoomSlider.valueProperty().asObject();
		Bindings.bindBidirectional(zoomValueField.textProperty(), zoomSliderValueObject, new StringConverter<Double>() {
			@Override
			public String toString(Double object) {
				return String.format(precentageFormat, object.doubleValue() * 100);
			}
			@Override
			public Double fromString(String string) {
				try {
					double value = Double.parseDouble(string) / 100;
					if (value > zoomSlider.getMax()) {
						return zoomSlider.getMax();
					} else {
						return value;
					}
				} catch (NumberFormatException e) {
					//logger.debug("Invalid zoom value: {}", string, e);
					return 1d; //Defaulting to 100% zoom level
				}
			}
		});

		visualizationZoomObject = visualizationWebView.zoomProperty().asObject();
		Bindings.bindBidirectional(zoomSliderValueObject, visualizationZoomObject);
		ValidationUtils.addMandatoryPrecentageBehavior(precentageFormat, zoomSlider.getMax() * 100, zoomValueField);
	}
	
	@FXML
	private void hideTemplateSettings() {
		settingsLayer.getChildren().remove(templateSettingsController.getRootRegion());
	}
	
	@FXML
	private void toggleTemplateSettings() {
		if (settingsLayer.getChildren().remove(templateSettingsController.getRootRegion())) {
			boolean areSelectedTemplatesEmpty = templateSettingsController.getSelectedTemplates().isEmpty();
			noTemplatesSelectedError.setVisible( areSelectedTemplatesEmpty );
			
		} else {
			settingsLayer.getChildren().add(templateSettingsController.getRootRegion());
			noTemplatesSelectedError.setVisible(false);
			templateSettingsSlideTransition.play();
		}
	}
	
	@FXML
	private void discoverModel() {
		if (validateParameters()) {
			logger.debug("Starting model discovery from event log: {}", logFile.getAbsolutePath());

			// Close template settings if open
			settingsLayer.getChildren().remove(templateSettingsController.getRootRegion());
			isDataAwareDiscovery = dataConditionsChoice.getSelectionModel().getSelectedItem().isDataAware();
			
			try {
				if (isDataAwareDiscovery) {
					LogUtils.checkDataExistence(logFile);
				}
				// Load the progress layer
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/ProgressLayer.fxml"));
				Node progressLayer = loader.load();
				ProgressLayerController progressLayerController = loader.getController();
				progressLayerController.getProgressTextLabel().setText("Discovering model...");

				// Create the task
				Task<DiscoveryTaskResult> task = createDiscoveryTask();
				addHandlersToTask(task, progressLayer, progressLayerController);

				// Start the task
				rootStackPane.getChildren().add(progressLayer);
				mainContents.setDisable(true);
				executorService.execute(task);
			} catch (IOException | IllegalStateException e) {
				//TODO: Feedback to the user
				logger.error("Can not load progress layer", e);
			} catch (Exception e) {
				AlertUtils.showWarning("No data attributes present!\nDiscovering without data conditions");
				logger.debug("No data attributes present! Discovering without data conditions");
				dataConditionsChoice.getSelectionModel().select(DataConditionType.NONE);
				discoverModel();
			}
			
			if (timeConditionDiscoveryToggle.isSelected())
				showLabelsChoice.getItems().setAll(ShowLabelEnum.values());
			else
				showLabelsChoice.getItems().setAll(ShowLabelEnum.SUPPORT, ShowLabelEnum.NONE);
			
			ShowLabelEnum prevValue = showLabelsChoice.getSelectionModel().getSelectedItem();
			if (showLabelsChoice.getItems().contains(prevValue))
				showLabelsChoice.getSelectionModel().select(prevValue);
			else
				showLabelsChoice.getSelectionModel().selectFirst();
		}
	}
	
	@FXML
	private void exportModel() {
		ModelViewType selectedModelView = modelViewChoiceBox.getSelectionModel().getSelectedItem();
		ModelExportChoice modelExportChoice = FileUtils.showModelSaveDialog(this.getStage(), null, selectedModelView);

		if (modelExportChoice != null) {
			File chosenFile = modelExportChoice.getChosenFile();
			ModelViewType chosenExportType = modelExportChoice.getChosenExportType();

			switch (chosenExportType) {
			case DECLARE:
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());
				try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(chosenFile.getAbsolutePath()))) {
					writer.write(ModelExporter.getDeclString(logFile, filteredActivities, filteredConstraints));
					logger.info("Model exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Model successfully exported");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the model failed!");
					logger.error("Unable to export model: {}", chosenFile.getAbsolutePath(), e);
				}
				break;
			case TEXTUAL:
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());
				try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(chosenFile.getAbsolutePath()))) {
					writer.write(ModelExporter.getTextString(filteredActivities, filteredConstraints));
					logger.info("Model exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Model exported successfully");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the model failed!");
					logger.error("Unable to export model: {}", chosenFile.getAbsolutePath(), e);
				}
				break;
			case AUTOMATON:
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());
				boolean exportSuccessful = ModelExporter.exportAutomaton(filteredActivities, filteredConstraints, chosenFile);
				if (exportSuccessful) {
					logger.info("Model exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Model exported successfully");
				} else {
					AlertUtils.showError("Exporting the model failed!");
					logger.error("Unable to export model: {}", chosenFile.getAbsolutePath()); //No stacktrace to log because automaton exporter fails silently
				}
				break;
			case XML_MODEL:
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());
				try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(chosenFile.getAbsolutePath()))) {
					writer.write(ModelExporter.getXmlString(filteredActivities, filteredConstraints));
					logger.info("Model exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Model successfully exported");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the model failed!");
					logger.error("Unable to export model: {}", chosenFile.getAbsolutePath(), e);
				}
				break;
			default:
				logger.error("Unhandled type for exporting: {}", chosenExportType);
				AlertUtils.showError("Unhandled type for exporting: " + chosenExportType);
				break;
			}	
		}
	}

	@FXML
	private void takeScreenshot() {
		logger.info("Save screenshot of model");
		ModelViewType selectedModelView = modelViewChoiceBox.getSelectionModel().getSelectedItem();
		File chosenFile = FileUtils.showImageSaveDialog(this.getStage());

		if (chosenFile != null) {
			logger.debug("Saving screenshot to file: {}", chosenFile.getAbsolutePath());

			try {
				if (selectedModelView == ModelViewType.TEXTUAL) {
					String html = (String) visualizationWebView.getEngine().executeScript("document.documentElement.outerHTML");
					Html2Image h = Html2Image.fromHtml(html);
					h.getImageRenderer().setImageType("png");
					h.getImageRenderer().saveImage(chosenFile);
					logger.info("Screenshot saved to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Screenshot saved successfully");

				} else {
					Element e = visualizationWebView.getEngine().getDocument().getElementById("rootDiv");
					DOMSource domSource = new DOMSource(e.getLastChild());
					StringWriter writer = new StringWriter();
					StreamResult result = new StreamResult(writer);
					TransformerFactory tf = TransformerFactory.newInstance();
					// Disable access to external entities in XML parsing (XXE attack)
					tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
					tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

					Transformer transformer = tf.newTransformer();
					transformer.transform(domSource, result);
					System.out.println(writer.toString());
					
					TranscoderInput inputSvgImage = new TranscoderInput(new ByteArrayInputStream(writer.toString().replace(" stroke=\"transparent\" ", " ").getBytes()));
					OutputStream pngOstream = new FileOutputStream(chosenFile);
			        TranscoderOutput outputPngImage = new TranscoderOutput(pngOstream);              
			        PNGTranscoder myConverter = new PNGTranscoder();        
			        myConverter.transcode(inputSvgImage, outputPngImage);
			        pngOstream.flush();
			        pngOstream.close();

					logger.info("Screenshot saved to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Screenshot saved successfully");
				}
			} catch (Exception e) {
				AlertUtils.showError("Saving the screenshot failed!");
				logger.error("Unable to save a screenshot: {}", chosenFile.getAbsolutePath(), e);
			}
		}
	}

	@FXML
	private void takeSnapshot() {
		logger.info("Save snapshot of model");

		String placeholder = logFile.getName();
		Matcher m = Pattern.compile("(\\.(xes|mxml)(\\.gz)?)$").matcher(placeholder);
		if (m.find())
			placeholder = placeholder.substring(0, placeholder.length()-m.group().length());
		
		TextInputDialog dialog = new TextInputDialog(placeholder + ".decl");
		dialog.setTitle("Save snapshot of model");
		dialog.setHeaderText("Save snapshot of model");
		dialog.setContentText("Modelname:");
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
				File file = new File(Files.createTempDirectory("snap_model").toFile(), fileName.get());
	
				logger.debug("Exporting model to temp. file: {}", file.getAbsolutePath());
				try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(file.getAbsolutePath()))) {
					writer.write(ModelExporter.getDeclString(logFile, filteredActivities, filteredConstraints));
					logger.info("Model exported to temp. file: {}", file.getAbsolutePath());
				}
			
				InventorySavedElement inventorySavedModel = new InventorySavedElement(file, new Date(), fileName.get(), InventoryElementTypeEnum.PROCESS_MODEL);
				Inventory.storeSavedModelSnapshot(inventorySavedModel);
				
			} catch (IOException e) {
				AlertUtils.showError("Exporting the model failed!");
				logger.error("Unable to export model.", e);
			}
		} else {
			logger.info("Canceled save of snapshot");
		}
	}
	
	@FXML
	private void removeInconsistencies() {
		if (discoveryTaskResult != null && discoveryTaskResult.getDiscoveryModel() != null) {
			
			PostProcessingCmdParameters postParams = PostProcessingCmdParameters.makeParametersForNoPostProcessing(); 
			postParams.supportThreshold = discoveryStatus.getSupportThreshold() / 100d;
			postParams.cropRedundantAndInconsistentConstraints = true;
			postParams.postProcessingAnalysisType = PostProcessingAnalysisType.HIERARCHYCONFLICTREDUNDANCY;

			postParams.sortingPolicies = new ConstraintSortingPolicy[] { ConstraintSortingPolicy.RANDOM };
			
			MinerFulSimplificationLauncher miFuSimpLa = new MinerFulSimplificationLauncher(discoveryTaskResult.getDiscoveryModel(), postParams);
			ProcessModel simplifiedModel = miFuSimpLa.simplify();
			
			XLog log = LogUtils.convertToXlog(logFile);
			List<DiscoveredActivity> allActivities = new ArrayList<>( ConstraintUtils.getAllActivitiesFromLog(log, considerLifecycleToggle.isSelected()) );
			List<DiscoveredConstraint> discoveredConstraints = ConstraintUtils.extractConstraintsFromMinerfulModel(log, simplifiedModel, allActivities);
			
			for (DiscoveredConstraint c : discoveredConstraints)
				c.setConstraintSupport((float) ConstraintUtils.computeTraceBasedSupport(log, c, discoveryStatus.isVacuityAsViolation()));
			
			discoveryTaskResult.setActivities(allActivities);
			discoveryTaskResult.setConstraints(discoveredConstraints);
			
			updateFilteredResultLists();
			updateVisualization();
		}
	}

	public void setLogData(File logFile) {
		this.logFile = logFile;
		logger.debug("Event log in discovery tab set to: {}", logFile.getAbsolutePath());
		discoverModel(); //Running the discovery with default settings when a tab is opened
	}

	private boolean validateParameters() {
		boolean valid = true;
		
		if (templateSettingsController.getSelectedTemplates().isEmpty()) {
			noTemplatesSelectedError.setVisible(true);
			valid = false;
		}
		
		if (minConstraintSupportSpinner.getValue() == null) {
			generalParametersPane.setExpanded(true);
			minConstraintSupportSpinner.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			minConstraintSupportSpinner.requestFocus();
			valid = false;
		}
		
		return valid;
	}

	private Task<DiscoveryTaskResult> createDiscoveryTask() {
		Task<DiscoveryTaskResult> task = null;
		
		DiscoveryMethod selectedMethod = getSelectedMethod(dataConditionsChoice.getSelectionModel().getSelectedItem().isDataAware(), 
																methodChoice.getSelectionModel().getSelectedItem());
		switch(selectedMethod) {
		
		case DECLARE: // Fall through intended
		case MP_DECLARE:
			DiscoveryTaskDeclare discoveryTaskDeclare = new DiscoveryTaskDeclare();
			discoveryTaskDeclare.setLogFile(logFile);
			
			discoveryTaskDeclare.setVacuityAsViolation(vacuityAsViolationToggle.isSelected());
			discoveryTaskDeclare.setConsiderLifecycle(considerLifecycleToggle.isSelected());
			discoveryTaskDeclare.setPruningType(declarePruningChoice.getSelectionModel().getSelectedItem());
			discoveryTaskDeclare.setSelectedTemplates(templateSettingsController.getSelectedTemplates());
			discoveryTaskDeclare.setComuputeTimeDistances(timeConditionDiscoveryToggle.isSelected());
			if (selectedMethod == DiscoveryMethod.MP_DECLARE) {
				discoveryTaskDeclare.setMinSupport(0);
				discoveryTaskDeclare.setMpEnhancer(createMpEnhancer());
			} else {
				discoveryTaskDeclare.setMinSupport(minConstraintSupportSpinner.getValue());
			}
			task = discoveryTaskDeclare;
			break;
		
		case MINERFUL: // Fall through intended
		case MP_MINERFUL:
			DiscoveryTaskMinerful discoveryTaskMinerful = new DiscoveryTaskMinerful();
			discoveryTaskMinerful.setLogFile(logFile);
			
			discoveryTaskMinerful.setPruningType(minerfulPruningChoice.getSelectionModel().getSelectedItem());
			discoveryTaskMinerful.setSelectedTemplates(templateSettingsController.getSelectedTemplates());
			discoveryTaskMinerful.setComuputeTimeDistances(timeConditionDiscoveryToggle.isSelected());
			if (selectedMethod == DiscoveryMethod.MP_MINERFUL) {
				discoveryTaskMinerful.setMinSupport(0d);
				discoveryTaskMinerful.setMpEnhancer(createMpEnhancer());
			} else {
				discoveryTaskMinerful.setMinSupport(minConstraintSupportSpinner.getValue()/100d);
			}
			task = discoveryTaskMinerful;
			break;
		
		default:
			//TODO: Show error to user
			logger.error("Can not create discovery task for unhandled method: {}", selectedMethod);
			break;
		}
		return task;
	}

	private MpEnhancer createMpEnhancer() {
		MpEnhancer mpEnhancer = new MpEnhancer();
		mpEnhancer.setMinSupport(minConstraintSupportSpinner.getValue()/100d);
		mpEnhancer.setConditionType(dataConditionsChoice.getSelectionModel().getSelectedItem());
		
		return mpEnhancer;
	}

	private void addHandlersToTask(Task<DiscoveryTaskResult> task, Node progressLayer, ProgressLayerController progressLayerController) {
		//Handle canceling the task
		progressLayerController.getCancelButton().setOnAction(e -> {
			task.cancel(true);
			rootStackPane.getChildren().remove(progressLayer);
			mainContents.setDisable(false);
		});

		//Handle task success
		task.setOnSucceeded(event -> {
			discoveryTaskResult = task.getValue();
			
			constraintFilterSlider.setMin(minConstraintSupportSpinner.getValue());
			constraintFilterSlider.setValue(minConstraintSupportSpinner.getValue());
			constraintFilterBox.setDisable(minConstraintSupportSpinner.getValue() == 100);
			
			updateFilteredResultLists();
			updateVisualization();
			discoveryStatus = new DiscoveryStatus(this);

			rootStackPane.getChildren().remove(progressLayer);
			resultsPane.setVisible(true);
			mainContents.setDisable(false);
		});

		//Handle task failure
		task.setOnFailed(event -> {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
				Node alertLayer = loader.load();
				AlertLayerController alertLayerController = loader.getController();
				alertLayerController.setAlertMessage(AlertType.ERROR, "Model discovery failed");
				alertLayerController.getOkButton().setOnAction(e -> {
					rootStackPane.getChildren().remove(alertLayer);
					mainContents.setDisable(false);
				});
				rootStackPane.getChildren().remove(progressLayer);
				rootStackPane.getChildren().add(alertLayer);
			} catch (IOException | IllegalStateException e) {
				logger.error("Can not load alert layer", e);
				//If alert layer can not be displayed then use regular alert instead
				AlertUtils.showWarning("Model discovery failed");
				rootStackPane.getChildren().remove(progressLayer);
				mainContents.setDisable(false);
			}
		});
	}

	private void updateFilteredResultLists() {
		if (discoveryTaskResult != null) {
			float activitiesSupport = (float) activityFilterSlider.getValue() / 100;
			float constraintsSupport = (float) constraintFilterSlider.getValue() / 100;
				
			// Constraints that meet the constraint support filter and reference activities that meet the activity support filter
			filteredConstraints = discoveryTaskResult.getConstraints().stream()
					.filter(item -> item.getConstraintSupport() >= constraintsSupport
									&& item.getActivationActivity().getActivitySupport() >= activitiesSupport
									&& (item.getTargetActivity() == null || item.getTargetActivity().getActivitySupport() >= activitiesSupport) )
					.collect(Collectors.toList());
			
			// Activities that meet the activity support filter
			filteredActivities = discoveryTaskResult.getActivities().stream()
					.filter(item -> item.getActivitySupport() >= activitiesSupport)
					.collect(Collectors.toList());
			
			if (!showAllActivitiesToggle.isSelected()) {	// Hides the activities not related to any constraint
				Set<DiscoveredActivity> constraintActivitiesSet = new HashSet<>();
				
				for (DiscoveredConstraint constraint : filteredConstraints) {
					constraintActivitiesSet.add(constraint.getActivationActivity());
					if (constraint.getTemplate().getIsBinary())
						constraintActivitiesSet.add(constraint.getTargetActivity());
				}
				
				Set<DiscoveredActivity> filteredActivitiesSet = new LinkedHashSet<>(filteredActivities);
				filteredActivitiesSet.retainAll(constraintActivitiesSet);
				filteredActivities = new LinkedList<>(filteredActivitiesSet);
			}
		}
	}

	private void updateVisualization() {
		String visualizationString;
		visualizationWebView.setDisable(true);
		
		if (discoveryTaskResult != null) {
			if (!filteredActivities.isEmpty() || !filteredConstraints.isEmpty()) {
				ModelViewType selectedModelView = modelViewChoiceBox.getSelectionModel().getSelectedItem();
				String script;
				switch (selectedModelView) {
				case DECLARE:
					visualizationString = GraphGenerator.createDeclareVisualizationString(filteredActivities, filteredConstraints, showConstraintsToggle.isSelected(), showLabelsChoice.getSelectionModel().getSelectedItem(), alternativeLayoutToggle.isSelected());
					if (visualizationString != null) {
						script = "setModel('" + visualizationString + "')";
						if (visualizationWebView.getEngine().getLoadWorker().stateProperty().get() == Worker.State.SUCCEEDED) {
							logger.debug("Executing visualization script: " + StringUtils.abbreviate(script, 1000));
							visualizationWebView.getEngine().executeScript(script);
						} else {
							initialWebViewScript = script;
						}
					}
					break;
				case TEXTUAL:
					String activitiesString = GraphGenerator.createActivitiesTextualString(filteredActivities);
					String constraintsString = GraphGenerator.createConstraintsTextualString(filteredConstraints);
	
					script = "setText('"+activitiesString+"','"+constraintsString+"')";
					if (visualizationWebView.getEngine().getLoadWorker().stateProperty().get() == Worker.State.SUCCEEDED) {
						logger.debug("Executing visualization script: {}", StringUtils.abbreviate(script, 1000));
						visualizationWebView.getEngine().executeScript(script);
					} else {
						initialWebViewScript = script;
					}
					break;
				case AUTOMATON:
					visualizationString = GraphGenerator.createAutomatonVisualizationString(filteredActivities, filteredConstraints, alternativeLayoutToggle.isSelected());
					if (visualizationString != null) {
						script = "setModel('" + visualizationString + "')";
						if (visualizationWebView.getEngine().getLoadWorker().stateProperty().get() == Worker.State.SUCCEEDED) {
							logger.debug("Executing visualization script: {}", StringUtils.abbreviate(script, 1000));
							visualizationWebView.getEngine().executeScript(script);
						} else {
							initialWebViewScript = script;
						}
					}
					break;
				default:
					//TODO: Show error to user
					logger.error("Unhandled model view selected: {}", selectedModelView);
					break;
				}
				saveButtonsHbox.setDisable(false);
			} else {
				//Reloading the page in case a previous visualization script is still executing
				//TODO: Should instead track if a visualization script is still executing and stop it (if it is possible)
				initialWebViewScript = null; //Has to be set to null because it will otherwise be executed after reload
				visualizationWebView.getEngine().reload();
				saveButtonsHbox.setDisable(true);
			}
		}

		visualizationWebView.setDisable(false);
	}
	
	// Class containing the status of parameters used for last execution of discovery 
	private class DiscoveryStatus {
		private int supportThreshold;
		private boolean vacuityAsViolation;
		private boolean considerLifecycle;
		
		public DiscoveryStatus(DiscoveryTabController discoveryController) {
			this.supportThreshold = discoveryController.minConstraintSupportSpinner.getValue();
			this.vacuityAsViolation = discoveryController.vacuityAsViolationToggle.isSelected();
			this.considerLifecycle = discoveryController.considerLifecycleToggle.isSelected();
		}

		public int getSupportThreshold() {
			return supportThreshold;
		}
		
		public boolean isVacuityAsViolation() {
			return vacuityAsViolation;
		}

		public boolean isConsiderLifecycle() {
			return considerLifecycle;
		}
	}
}
