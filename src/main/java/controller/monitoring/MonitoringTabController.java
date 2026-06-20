package controller.monitoring;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import org.kordamp.ikonli.javafx.FontIcon;
import org.w3c.dom.Element;

import controller.common.AbstractController;
import controller.common.eventcell.EventCell;
import controller.common.eventcell.EventData;
import controller.common.layers.AlertLayerController;
import controller.common.layers.AlertLayerController.AlertType;
import controller.common.layers.ProgressLayerController;
import treedata.TreeDataBase;
import treedata.TreeDataMetaconstraint;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javafx.util.StringConverter;
import netscape.javascript.JSObject;
import task.monitoring.MonitoringTaskMobuconLdlRum;
import task.monitoring.MonitoringTaskMobuconLtlRum;
import task.monitoring.MonitoringTaskMpDeclareAlloy;
import task.monitoring.MonitoringTaskResult;
import util.AlertUtils;
import util.ConstraintTemplate;
import util.FileUtils;
import util.GraphGenerator;
import util.ModelUtils;
import util.TemplateUtils;
import util.ValidationUtils;

public class MonitoringTabController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");

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
	private ChoiceBox<MonitoringMethod> methodChoice;
	@FXML
	private Label methodWarningLabel;
	@FXML
	private ToggleSwitch conflictCheckToggle;
	@FXML
	private TitledPane metaconstraintsPane;
	@FXML
	private TreeView<TreeDataBase> metaconstraintsTreeView;
	@FXML
	private Button addMetaconstraintButton;
	@FXML
	private ListView<String> constraintListView;
	@FXML
	private Button monitoringButton;
	@FXML
	private StackPane resultsContainer;
	@FXML
	private SplitPane resultsSplitPane;
	@FXML
	private ComboBox<String> traceChoice;
	@FXML
	private Slider zoomSlider;
	@FXML
	private TextField zoomValueField;
	@FXML
	private Button takeScreenshotButton;
	@FXML
	private ChoiceBox<MonitorViewType> monitorViewChoiceBox;
	@FXML
	private HBox visualizationTogglesHBox;
	@FXML
	private ToggleSwitch constraintLabelsToggle;
	@FXML
	private ToggleSwitch conditionLabelsToggle;
	@FXML
	private HBox conditionLabelsToggleHBox;
	@FXML
	private ToggleSwitch horizontalToggle;
	@FXML
	private WebView visualizationWebView;
	@FXML
	private ScrollPane fluentsVisualization;
	@FXML
	private FluentsVisualizationController fluentsVisualizationController;
	@FXML
	private HBox timelineControls;
	@FXML
	private Button stepBackwardButton;
	@FXML
	private Button playPauseButton;
	@FXML
	private FontIcon playFonticon;
	@FXML
	private Button stepForwardButton;
	@FXML
	private Label currentEventNumber;
	@FXML
	private Label totalEventsNumber;
	@FXML
	private Slider eventSlider;
	@FXML
	private VBox eventsHeaderVBox;
	@FXML
	private ToggleSwitch showPayloadsToggle;
	@FXML
	private ListView<EventData> eventsListView;

	private MetaconstraintsSettingsController metaconstraintsSettingsController;
	private TranslateTransition metaconstraintsSettingsSlideTransition;

	private File declModel;
	private File logFile;
	private List<String> constraintList;

	private MonitoringTaskResult monitoringTaskResult;
	private Semaphore lock = new Semaphore(1);

	private static String precentageFormat = "%.1f";
	private ObjectProperty<Double> zoomSliderValueObject;
	private ObjectProperty<Double> visualizationZoomObject;

	private String initialWebViewScript;

	private Timeline animationTimeline; //Controls eventSlider movement during animation
	private boolean animationInProgress; //Used to allow navigation of events during animation
	private SimpleIntegerProperty currentEventIndex = new SimpleIntegerProperty(0); //Various ui elements listen to this value for updates
	private FontIcon pauseFontIcon; //Icon to be displayed when animation is paused
	private Node progressLayer;
	private BlockingDeque<String> traceNamesDeque;
	private TreeDataBase metaconstraintsRoot;
	private TreeDataBase currentlyEditingTreeData;


	@FXML
	private void initialize() {
		setupVisualizationWebView();
		setupTimelineControls();
		prepareMetaconstraintsLayer();

		// Creating icons not defined in fxml
		pauseFontIcon = new FontIcon("fa-pause");
		pauseFontIcon.getStyleClass().add("small-button__icon");

		//methodChoice.getItems().addAll(MonitoringMethod.MP_DECLARE_ALLOY, MonitoringMethod.MOBUCON_LTL, MonitoringMethod.MOBUCON_LDL); // Adding only methods that are fully or partially implemented
		methodChoice.getItems().add(MonitoringMethod.MP_DECLARE_ALLOY);
		methodChoice.getItems().add(MonitoringMethod.MOBUCON_LTL);
		
		// TODO: LDL is hidden since it is only partially implemented
		if (Objects.equals(System.getProperty("RumDebug"), "true")) {
			methodChoice.getItems().add(MonitoringMethod.MOBUCON_LDL);
		}
		
		// Sets the texts that are shown in the UI
		methodChoice.setConverter(new StringConverter<MonitoringMethod>() {
			@Override
			public String toString(MonitoringMethod monitoringMethod) {
				return monitoringMethod.getDisplayText();
			}
			@Override
			public MonitoringMethod fromString(String string) {
				return null;
			}
		});

		// Visibility of settings panes based on selected method
		methodChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			methodWarningLabel.setText(null);
			switch (newV) {
			case MP_DECLARE_ALLOY:
				metaconstraintsPane.setDisable(true);
				metaconstraintsPane.setExpanded(false);
				resultsContainer.getChildren().remove(metaconstraintsSettingsController.getRootRegion());
				break;
			case MOBUCON_LTL:
				metaconstraintsPane.setDisable(true);
				metaconstraintsPane.setExpanded(false);
				resultsContainer.getChildren().remove(metaconstraintsSettingsController.getRootRegion());
				
				methodWarningLabel.setText(newV.getDisplayText() + " ignores data conditions!");
				break;
			case MOBUCON_LDL:
				metaconstraintsPane.setDisable(false);
				metaconstraintsPane.setExpanded(true);
				
				methodWarningLabel.setText(newV.getDisplayText() + " ignores data conditions!");
				break;
				//	case ONLINE_DECLARE:
				//		break;
				//	case PROBDECLARE:
				//		break;
			default:
				break;
			}
		});
		
		methodChoice.getSelectionModel().selectFirst();

		monitorViewChoiceBox.setConverter(new StringConverter<MonitorViewType>() {
			@Override
			public String toString(MonitorViewType modelViewType) {
				return modelViewType.getDisplayText();
			}
			@Override
			public MonitorViewType fromString(String string) {
				return null;
			}
		});
		monitorViewChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if (newV == MonitorViewType.DECLARE) {
				visualizationTogglesHBox.setVisible(true);
				visualizationTogglesHBox.setManaged(true);
				visualizationWebView.setVisible(true);
				visualizationWebView.setManaged(true);
				fluentsVisualization.setVisible(false);
				fluentsVisualization.setManaged(false);
				takeScreenshotButton.setDisable(false);
			} else {
				visualizationTogglesHBox.setVisible(false);
				visualizationTogglesHBox.setManaged(false);
				visualizationWebView.setVisible(false);
				visualizationWebView.setManaged(false);
				fluentsVisualization.setVisible(true);
				fluentsVisualization.setManaged(true);
				takeScreenshotButton.setDisable(true);
			}
			updateVisualization();
		});

		constraintLabelsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			updateVisualization();
		});
		conditionLabelsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			updateVisualization();
		});
		horizontalToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			updateVisualization();
		});

		// Updates eventsListView to correspond to the selected trace
		traceChoice.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
			if (newIndex.intValue() >= 0) {
				List<EventData> traceEvents = monitoringTaskResult.getTraceEventsData().get(newIndex.intValue());

				if (traceEvents != null) {
					eventsListView.setVisible(true);
					eventsListView.getItems().setAll(traceEvents);
					updateTimelineControls(traceEvents);
				} else {
					if (!rootRegion.getChildren().contains(progressLayer)) {
						rootRegion.getChildren().add(progressLayer);
						mainContents.setDisable(true);
					}

					String newSelectedName = traceChoice.getItems().get(newIndex.intValue());
					if (!traceNamesDeque.peek().equals(newSelectedName)) {	// Avoid to remove and insert again the head of the queue
						traceNamesDeque.remove(newSelectedName);
						try {
							traceNamesDeque.putFirst(newSelectedName);
						} catch (InterruptedException e) {}
					}
				}

			} else {
				eventsListView.getItems().clear();
				updateTimelineControls(null);
			}

			updateVisualization();
		});

		//traceChoice.setVisibleRowCount(7);

		Consumer<Integer> selectionCallback = new Consumer<Integer>() {
			@Override
			public void accept(Integer selectedIndex) {
				if (animationInProgress) {
					animationTimeline.stop();
				}
				eventSlider.setValue(selectedIndex);
				currentEventIndex.setValue(selectedIndex);
				animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));
				if (animationInProgress) {
					animationTimeline.pause();
					playPauseButton.setGraphic(playFonticon);
					animationInProgress = false;
				}
			}
		};

		// Each event is displayed as defined in EventListCell class
		eventsListView.setCellFactory(value -> new EventCell(false, showPayloadsToggle.isSelected(), selectionCallback));
		showPayloadsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			eventsListView.setCellFactory(value -> new EventCell(false, newValue, selectionCallback));
			if (currentEventIndex.intValue() == 0) {
				eventsListView.scrollTo(currentEventIndex.intValue());
			} else {
				eventsListView.scrollTo(currentEventIndex.intValue()-1);
			}
		});

		// resultsSplitPane will be shown once it has contents
		resultsSplitPane.setVisible(false);
		parametersSection.setViewOrder(-1); // Makes sure that the metaconstraints panel slides in from under the parameters

		traceNamesDeque = new LinkedBlockingDeque<>();

		//Disables the results area when a slide in panel is visible
		resultsContainer.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
			@Override
			public void onChanged(Change<? extends Node> change) {
				while (change.next()) {
					resultsSplitPane.setDisable(change.getList().size() > 1);
				}
			}
		});
		

		//Hiding metaconstraintsPane because MoBuConLDL is currently hidden
		if (!(Objects.equals(System.getProperty("RumDebug"), "true"))) {
			metaconstraintsPane.setVisible(false);
			metaconstraintsPane.setManaged(false);	
		}

		logger.debug("Monitoring tab initialized");
	}

	private void prepareMetaconstraintsLayer() {
		// Setting the results pane and toggle buttons to change based on settings pane children
		metaconstraintsRoot = new TreeDataBase();
		metaconstraintsTreeView.setRoot(metaconstraintsRoot);
		metaconstraintsTreeView.setShowRoot(false);

		metaconstraintsRoot.getIsEditingWrapper().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(Boolean.TRUE)) {
				addMetaconstraintButton.pseudoClassStateChanged(selectedClass, true);
				addMetaconstraintButton.setText("Minimize trace attributes");
				((FontIcon)addMetaconstraintButton.getGraphic()).setIconLiteral("fa-angle-double-left");
			} else {
				addMetaconstraintButton.pseudoClassStateChanged(selectedClass, false);
				addMetaconstraintButton.setText("Add trace attributes");
				((FontIcon)addMetaconstraintButton.getGraphic()).setIconLiteral("fa-angle-double-right");
			}
		});

		metaconstraintsTreeView.setCellFactory(param -> new MetaconstraintCell(this));
		metaconstraintsTreeView.setFixedCellSize(58d);
		metaconstraintsTreeView.prefHeightProperty().bind(Bindings.size(metaconstraintsTreeView.getRoot().getChildren()).multiply(58d).add(20d));

		// Preloading trace attribute settings
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/monitoring/MetaconstraintsSettings.fxml"));
			loader.load();
			metaconstraintsSettingsController = loader.getController();
			metaconstraintsSettingsController.setController(this);
			metaconstraintsSettingsController.setAttributesRoot(metaconstraintsRoot);

			// Preparation for slide in animation
			Region traceAttributesSettingsRootRegion = metaconstraintsSettingsController.getRootRegion();
			metaconstraintsSettingsSlideTransition = new TranslateTransition(new Duration(300), traceAttributesSettingsRootRegion);
			metaconstraintsSettingsSlideTransition.setFromX(-1 * traceAttributesSettingsRootRegion.getPrefWidth());
			metaconstraintsSettingsSlideTransition.setToX(-1); //-1 so that it would cover the parameters section border

		} catch (IOException | IllegalStateException e) {
			logger.error("Can not load trace attributes layer", e);
			//TODO: Alert the user and disable attribute editing buttons
			addMetaconstraintButton.setDisable(true);
		}

		metaconstraintsSettingsController.getCloseButton().setOnAction(event -> {
			hideMetaconstraintSettings();
		});

	}

	@FXML
	private void selectLogFile() {
		InventorySavedElement eventLog = FileUtils.showSavedElementDialog(InventoryElementTypeEnum.EVENT_LOG);

		if(eventLog != null) {
			this.logFile = eventLog.getFile();
			logger.info("Opened log in monitoring tab: {}", logFile.getAbsolutePath());
			logPathLabel.setText(logFile.getAbsolutePath());
			logFileButton.pseudoClassStateChanged(ValidationUtils.errorClass, false);

		} else if (this.logFile == null) {
			logFileButton.pseudoClassStateChanged(ValidationUtils.errorClass, true);
		}
	}

	@FXML
	private void showMetaconstraintLayer() {
		startTreeDataItemEdit(metaconstraintsRoot);
	}

	public void startTreeDataItemEdit(TreeDataBase itemToEdit) {

		if (itemToEdit == currentlyEditingTreeData) {
			resultsContainer.getChildren().remove(metaconstraintsSettingsController.getRootRegion());
			updateCurrentlyEditingTreeData(itemToEdit);

		} else if (itemToEdit == metaconstraintsRoot || itemToEdit instanceof TreeDataMetaconstraint) {
			if (!resultsContainer.getChildren().contains(metaconstraintsSettingsController.getRootRegion())) {
				resultsContainer.getChildren().add(metaconstraintsSettingsController.getRootRegion());
				metaconstraintsSettingsSlideTransition.play();
			}

			updateCurrentlyEditingTreeData(itemToEdit);
			metaconstraintsSettingsController.setEditingAttribute(currentlyEditingTreeData);
		}
	}

	public void deleteTreeDataItem(TreeDataBase itemToDelete) {
		metaconstraintsRoot.getChildren().remove(itemToDelete);
		if (itemToDelete == currentlyEditingTreeData)
			startTreeDataItemEdit(metaconstraintsRoot);
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
	private void hideMetaconstraintSettings() {
		resultsContainer.getChildren().remove(metaconstraintsSettingsController.getRootRegion());
		updateCurrentlyEditingTreeData(null);
	}

	@FXML
	private void startMonitoring() {
		hideMetaconstraintSettings();	// Close metaconstraints layer if open

		if (animationInProgress) {
			playPauseButton.setGraphic(playFonticon);
			animationTimeline.pause();
			animationInProgress = false;
		}

		if (validateParameters() && validateConstraints(constraintList)) {
			logger.debug("Starting monitoring with model: {} and log: {}", declModel.getAbsolutePath(), logFile.getAbsolutePath());

			try {
				// Load the progress layer
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/ProgressLayer.fxml"));
				progressLayer = loader.load();
				ProgressLayerController progressLayerController = loader.getController();
				progressLayerController.getProgressTextLabel().setText("Collecting data...");

				traceChoice.getItems().clear();
				eventsListView.getItems().clear();
				monitoringTaskResult = null;

				// Start the task after that all previous tasks are shut down (if any)
				if (!rootRegion.getChildren().contains(progressLayer)) {
					rootRegion.getChildren().add(progressLayer);
					mainContents.setDisable(true);
				}

				// Create the new task
				Task<MonitoringTaskResult> task = createMonitoringTask();

				ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
				executorService.execute(task);
				executorService.shutdown();

				addHandlersToTask(executorService, task, progressLayer, progressLayerController);

			} catch (IOException | IllegalStateException e) {
				//TODO: Feedback to the user
				logger.error("Can not load progress layer", e);
			}
		}
	}

	@FXML
	private void takeScreenshot() {
		if (animationInProgress) {
			playPauseButton.setGraphic(playFonticon);
			animationTimeline.pause();
			animationInProgress = false;
		}

		logger.info("Save screenshot of model");
		File chosenFile = FileUtils.showImageSaveDialog(this.getStage());

		if (chosenFile != null) {
			logger.debug("Saving screenshot to file: {}", chosenFile.getAbsolutePath());

			if (monitorViewChoiceBox.getSelectionModel().getSelectedItem() == MonitorViewType.DECLARE) {
				try {
					Element e = visualizationWebView.getEngine().getDocument().getElementById("rootDiv");
					DOMSource domSource = new DOMSource(e.getLastChild());
					StringWriter writer = new StringWriter();
					StreamResult result = new StreamResult(writer);
					TransformerFactory tf = TransformerFactory.newInstance();
					Transformer transformer = tf.newTransformer();
					transformer.transform(domSource, result);
					System.out.println(writer.toString());

					TranscoderInput input_svg_image = new TranscoderInput(new ByteArrayInputStream(writer.toString().replace(" stroke=\"transparent\" ", " ").getBytes()));
					OutputStream png_ostream = new FileOutputStream(chosenFile);
					TranscoderOutput output_png_image = new TranscoderOutput(png_ostream);              
					PNGTranscoder my_converter = new PNGTranscoder();        
					my_converter.transcode(input_svg_image, output_png_image);
					png_ostream.flush();
					png_ostream.close();

					logger.info("Screenshot saved to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Screenshot saved successfully");

				} catch (Exception e) {
					AlertUtils.showError("Saving the screenshot failed!");
					logger.error("Unable to save a screenshot: {}", chosenFile.getAbsolutePath(), e);
				}
			} else if (monitorViewChoiceBox.getSelectionModel().getSelectedItem() == MonitorViewType.FLUENTS) {
				//TODO
			} else {
				logger.error("Unhandeled MonitorViewType: {}", monitorViewChoiceBox.getSelectionModel().getSelectedItem());
			}
		}
	}

	@FXML
	private void playPause() {
		if (animationInProgress) {
			playPauseButton.setGraphic(playFonticon);
			animationTimeline.pause();
			animationInProgress = false;
		} else {
			playPauseButton.setGraphic(pauseFontIcon);
			animationTimeline.play();
			animationInProgress = true;
		}
	}

	@FXML
	private void stepBackward() {
		if (animationInProgress)
			animationTimeline.stop();

		eventSlider.setValue(eventSlider.getValue() - 1);
		currentEventIndex.setValue((int)eventSlider.getValue());
		animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));

		if (animationInProgress) {
			animationTimeline.pause();
			playPauseButton.setGraphic(playFonticon);
			animationInProgress = false;
		}
	}

	@FXML
	private void stepForward() {
		if (animationInProgress)
			animationTimeline.stop();

		eventSlider.setValue(eventSlider.getValue() + 1);
		currentEventIndex.setValue((int)eventSlider.getValue());

		if (eventSlider.getValue() == eventSlider.getMax())
			animationTimeline.jumpTo(new Duration(0));
		else
			animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));

		if (animationInProgress) {
			animationTimeline.pause();
			playPauseButton.setGraphic(playFonticon);
			animationInProgress = false;
		}
	}

	public void setModelData(File declModel, List<String> constraintList) {
		this.declModel = declModel;
		this.constraintList = constraintList;

		// Setting cell height, list padding and list preferred height
		double cellHeight = 55;
		double padding = 15;
		this.constraintListView.setFixedCellSize(cellHeight);
		this.constraintListView.setPadding(new Insets(padding));
		this.constraintListView.setPrefHeight(constraintList.size() * cellHeight + 2 * padding);

		List<String> formattedConstraintsList = ModelUtils.getFormattedListOfConstraints(constraintList);
		this.constraintListView.getItems().addAll(formattedConstraintsList);

		metaconstraintsSettingsController.setConstraints(constraintList);
		metaconstraintsSettingsController.setActivities(ModelUtils.getActivityList(declModel));
	}

	private boolean validateParameters() {
		boolean valid = true;
		if (logFile == null) {
			logFileButton.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			logFileButton.requestFocus();
			valid = false;
		}

		logger.debug("Result of parameter validation: {}", valid);
		return valid;
	}

	private boolean validateConstraints(List<String> constraintList) {
		MonitoringMethod selectedMethod = methodChoice.getSelectionModel().getSelectedItem();

		if (selectedMethod == MonitoringMethod.MP_DECLARE_ALLOY) {
			for (String s : constraintList) {
				String template = s.substring(0,s.indexOf('['));
				
				if (!TemplateUtils.getAlloyMonitoringSupportedConstraints().contains( ConstraintTemplate.getByTemplateName(template) )) {
					AlertUtils.showError(template + " is not a valid template for " + selectedMethod.getDisplayText());
					return false;
				}
			}
			
		} else if (selectedMethod == MonitoringMethod.MOBUCON_LTL) {
			for(String s: constraintList) {
				String template = s.substring(0,s.indexOf('['));
				if (!TemplateUtils.getMobuconMonitoringSupportedConstraints().contains( ConstraintTemplate.getByTemplateName(template) )) {
					AlertUtils.showError(template + " is not a valid template for " + selectedMethod.getDisplayText());
					return false;
				}
			}
			
		} else if (selectedMethod == MonitoringMethod.MOBUCON_LDL) {
			for(String s: constraintList) {
				String template = s.substring(0,s.indexOf('['));
				if (!TemplateUtils.getMobuconMonitoringSupportedConstraints().contains( ConstraintTemplate.getByTemplateName(template) )) {
					AlertUtils.showError(template + " is a not valid template for " + selectedMethod.getDisplayText());
					return false;
				}
			}
		}

		return true;
	}

	private Task<MonitoringTaskResult> createMonitoringTask() {
		Task<MonitoringTaskResult> task = null;

		MonitoringMethod selectedMethod = methodChoice.getSelectionModel().getSelectedItem();
		switch (selectedMethod) {
		case MP_DECLARE_ALLOY:
			MonitoringTaskMpDeclareAlloy monitoringTaskMpDeclareAlloy = new MonitoringTaskMpDeclareAlloy(lock, traceNamesDeque);
			monitoringTaskMpDeclareAlloy.setDeclModel(declModel);
			monitoringTaskMpDeclareAlloy.setLogFile(logFile);
			monitoringTaskMpDeclareAlloy.setConflictCheck(conflictCheckToggle.isSelected());
			task = monitoringTaskMpDeclareAlloy;
			break;
		case MOBUCON_LTL:
			MonitoringTaskMobuconLtlRum monitoringTaskMobuconLtlRum = new MonitoringTaskMobuconLtlRum(traceNamesDeque);
			monitoringTaskMobuconLtlRum.setDeclModel(declModel);
			monitoringTaskMobuconLtlRum.setLogFile(logFile);
			monitoringTaskMobuconLtlRum.setConflictCheck(conflictCheckToggle.isSelected());
			task = monitoringTaskMobuconLtlRum;
			break;
		case MOBUCON_LDL:
			MonitoringTaskMobuconLdlRum monitoringTaskMobuconLdlRum = new MonitoringTaskMobuconLdlRum(traceNamesDeque);
			monitoringTaskMobuconLdlRum.setDeclModel(declModel);
			monitoringTaskMobuconLdlRum.setMetaconstraints(new LinkedList<TreeDataMetaconstraint>(metaconstraintsRoot.getChildren().stream().map(item -> (TreeDataMetaconstraint)item).collect(Collectors.toList())));
			monitoringTaskMobuconLdlRum.setLogFile(logFile);
			monitoringTaskMobuconLdlRum.setConflictCheck(conflictCheckToggle.isSelected());
			task = monitoringTaskMobuconLdlRum;
			break;
			//case ONLINE_DECLARE:
			//	break;
			//case PROBDECLARE:
			//	break;
		default:
			//TODO: Show error to user
			logger.error("Can not create monitoring task for unhandled method: {}", selectedMethod);
			break;
		}

		return task;
	}

	private void addHandlersToTask(ExecutorService executorService, Task<MonitoringTaskResult> task, Node progressLayer, ProgressLayerController progressLayerController) {
		// Handle canceling the task
		progressLayerController.getCancelButton().setOnAction(e -> {
			task.cancel();
			executorService.shutdown();
			rootRegion.getChildren().remove(progressLayer);
			mainContents.setDisable(false);
		});

		monitoringButton.setOnAction(e -> {
			task.cancel();
			executorService.shutdown();
			startMonitoring();
		});

		// Updating results from task trace by trace
		task.valueProperty().addListener((ob, oldVal, newVal) -> {
			monitoringTaskResult = newVal;

			if (traceChoice.getItems().isEmpty()) {
				traceChoice.getItems().addAll(monitoringTaskResult.getTraceNames());
				traceChoice.getSelectionModel().selectFirst();
				
				if (task instanceof MonitoringTaskMobuconLdlRum) {
					monitorViewChoiceBox.getItems().setAll(MonitorViewType.FLUENTS);
				} else {
					monitorViewChoiceBox.getItems().setAll(MonitorViewType.values());
				}
				monitorViewChoiceBox.getSelectionModel().selectFirst();
				
				conditionLabelsToggleHBox.setDisable(!(task instanceof MonitoringTaskMpDeclareAlloy));
				updateVisualization();
			}
		});

		task.setOnSucceeded(event -> {
			System.out.println("Done!");

			if (monitoringTaskResult == null) { //Only necessary when the task returns no results
				try {
					FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
					Node alertLayer = loader.load();
					AlertLayerController alertLayerController = loader.getController();
					alertLayerController.setAlertMessage(AlertType.ERROR, "Task returned no results");
					alertLayerController.getOkButton().setOnAction(e -> {
						rootRegion.getChildren().remove(alertLayer);
						mainContents.setDisable(false);
					});
					rootRegion.getChildren().remove(progressLayer);
					rootRegion.getChildren().add(alertLayer);
				} catch (IOException | IllegalStateException e) {
					logger.error("Can not load alert layer", e);
					//If alert layer can not be displayed then use regular alert instead
					AlertUtils.showWarning("Processing traces failed");
					rootRegion.getChildren().remove(progressLayer);
					mainContents.setDisable(false);
				}
			}
		});

		// Handle task failure
		task.setOnFailed(event -> {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/common/layers/AlertLayer.fxml"));
				Node alertLayer = loader.load();
				AlertLayerController alertLayerController = loader.getController();
				alertLayerController.setAlertMessage(AlertType.ERROR, "Processing traces failed");
				alertLayerController.getOkButton().setOnAction(e -> {
					rootRegion.getChildren().remove(alertLayer);
					mainContents.setDisable(false);
				});
				rootRegion.getChildren().remove(progressLayer);
				rootRegion.getChildren().add(alertLayer);
			} catch (IOException | IllegalStateException e) {
				logger.error("Can not load alert layer", e);
				//If alert layer can not be displayed then use regular alert instead
				AlertUtils.showWarning("Processing traces failed");
				rootRegion.getChildren().remove(progressLayer);
				mainContents.setDisable(false);
			}
		});
	}

	private void setupVisualizationWebView() {
		visualizationWebView.getEngine().load((getClass().getClassLoader().getResource("test.html")).toString());
		visualizationWebView.setContextMenuEnabled(false); //Setting it in FXML causes an IllegalArgumentException
		JSObject window = (JSObject) visualizationWebView.getEngine().executeScript("window");
		window.setMember("rum_application", this); //Allows calling public methods of this class from JavaScript

		visualizationWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue == Worker.State.SUCCEEDED && initialWebViewScript != null) {
				logger.debug("Updating visualization in editor tab: {}", StringUtils.abbreviate(initialWebViewScript, 1000));
				visualizationWebView.getEngine().executeScript(initialWebViewScript);
			}
			logger.debug("Visualization html loaded in editor tab");
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

	private void updateVisualization() {
		if (monitorViewChoiceBox.getSelectionModel().getSelectedItem() == MonitorViewType.DECLARE) {
			String visualizationString;
			String script;

			if (traceChoice.getSelectionModel().isEmpty()) {
				//Reloading the page in case a previous visualization script is still executing
				//TODO: Should instead track if a visualization script is still executing and stop it (if it is possible)
				initialWebViewScript = null; //Has to be set to null because it will otherwise be executed after reload
				visualizationWebView.getEngine().reload();
			} else if (monitoringTaskResult.getTraceEventsData().get(traceChoice.getSelectionModel().getSelectedIndex()) != null) {
				if (currentEventIndex.getValue() == 0) {
					EventData currentEventData = monitoringTaskResult.getTraceEventsData().get(traceChoice.getSelectionModel().getSelectedIndex()).get(currentEventIndex.getValue());
					visualizationString = GraphGenerator.createMonitoringVisualizationString(currentEventData.getConstraintStates(), false, constraintLabelsToggle.isSelected(), conditionLabelsToggle.isSelected(), horizontalToggle.isSelected());
				} else {
					EventData currentEventData = monitoringTaskResult.getTraceEventsData().get(traceChoice.getSelectionModel().getSelectedIndex()).get(currentEventIndex.getValue() -1);
					visualizationString = GraphGenerator.createMonitoringVisualizationString(currentEventData.getConstraintStates(), true, constraintLabelsToggle.isSelected(), conditionLabelsToggle.isSelected(), horizontalToggle.isSelected());
				}

				if (visualizationString != null) {
					script = "setModel('" + visualizationString + "')";
					if (visualizationWebView.getEngine().getLoadWorker().stateProperty().get() == Worker.State.SUCCEEDED) {
						logger.debug("Executing visualization script: {}", StringUtils.abbreviate(script, 1000));
						visualizationWebView.getEngine().executeScript(script);
					} else {
						initialWebViewScript = script;
					}
				}

				rootRegion.getChildren().remove(progressLayer);
				resultsSplitPane.setVisible(true);
				mainContents.setDisable(false);
			}

		} else if (monitorViewChoiceBox.getSelectionModel().getSelectedItem() == MonitorViewType.FLUENTS) {
			if (traceChoice.getSelectionModel().isEmpty()) {
				fluentsVisualizationController.setTraceEvents(null);
			} else if (monitoringTaskResult.getTraceEventsData().get(traceChoice.getSelectionModel().getSelectedIndex()) != null) {
				List<EventData> traceEvents = monitoringTaskResult.getTraceEventsData().get(traceChoice.getSelectionModel().getSelectedIndex());
				fluentsVisualizationController.setTraceEvents(traceEvents);
				fluentsVisualizationController.setVisualizationEventIndex(currentEventIndex.getValue());

				rootRegion.getChildren().remove(progressLayer);
				resultsSplitPane.setVisible(true);
				mainContents.setDisable(false);
			}
		}
	}

	private void setupTimelineControls() {
		currentEventNumber.textProperty().bind(currentEventIndex.asString());

		eventSlider.setOnMousePressed(event -> {
			if (animationInProgress) {
				animationTimeline.stop();
			}
		});

		eventSlider.setOnMouseReleased(event -> {
			animationTimeline.jumpTo(animationTimeline.getTotalDuration().multiply(eventSlider.getValue() / eventSlider.getMax()));
			if (animationInProgress) {
				animationTimeline.play();
			}
			currentEventIndex.set((int)eventSlider.getValue());
		});

		eventSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (animationTimeline.getStatus() == Status.RUNNING && oldValue.intValue() != newValue.intValue()) {
				currentEventIndex.set(newValue.intValue());
			}

			if (newValue.doubleValue() == 0) {
				stepBackwardButton.setDisable(true);
				stepForwardButton.setDisable(false);
			} else if (newValue.doubleValue() == eventSlider.getMax()) {
				stepBackwardButton.setDisable(false);
				stepForwardButton.setDisable(true);
			} else {
				stepBackwardButton.setDisable(false);
				stepForwardButton.setDisable(false);
			}
		});

		currentEventIndex.addListener((observable, oldValue, newValue) -> {
			updateVisualization();
			if (newValue.intValue() == 0) {
				eventsListView.scrollTo(newValue.intValue());
				eventsListView.getSelectionModel().clearSelection();
			} else {
				eventsListView.scrollTo(newValue.intValue()-1);
				eventsListView.getSelectionModel().clearAndSelect(newValue.intValue()-1);
			}
		});

		stepBackwardButton.setDisable(true);
	}

	private void updateTimelineControls(List<EventData> traceEvents) {
		if (animationInProgress) {
			animationTimeline.stop();
			playPauseButton.setGraphic(playFonticon);
			animationInProgress = false;
		}

		if (traceEvents == null) {
			timelineControls.setVisible(false);
		} else {
			totalEventsNumber.setText(Integer.toString(traceEvents.size()));
			eventSlider.setMax(traceEvents.size());
			eventSlider.setValue(0d);
			currentEventIndex.setValue(0);

			animationTimeline = new Timeline();
			animationTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(0),
					new KeyValue(eventSlider.valueProperty(), eventSlider.getMin())));
			animationTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(1500 * eventSlider.getMax()),
					new KeyValue(eventSlider.valueProperty(), eventSlider.getMax())));

			animationTimeline.setOnFinished(event -> {
				playPauseButton.setGraphic(playFonticon);
				animationInProgress = false;
			});

			currentEventIndex.setValue(0);
			timelineControls.setVisible(true);
		}
	}
}
