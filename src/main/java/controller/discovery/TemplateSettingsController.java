package controller.discovery;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import controller.common.AbstractController;
import controller.discovery.data.DiscoveredActivity;
import controller.discovery.data.DiscoveredConstraint;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.StringConverter;
import netscape.javascript.JSObject;
import util.ConstraintTemplate;
import util.GraphGenerator;
import util.TemplateDescription;
import util.TemplateUtils;

public class TemplateSettingsController extends AbstractController {

	private static final int CELL_HEIGHT = 23;
	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private Map<CheckBox, ListView<TemplateData>> templateSections = new HashMap<CheckBox, ListView<TemplateData>>();

	// Templates data list keeps track of all the modifications to the ListView elements, in order to maintain the selection status
	private List<TemplateData> templateDataList = new LinkedList<>();

	@FXML
	private GridPane rootRegion;
	@FXML
	private VBox tempalteSettingsVbox;
	@FXML
	private Label titleLabel;
	@FXML
	private CheckBox unaryCheckBox;
	@FXML
	private ListView<TemplateData> unaryListView;
	@FXML
	private CheckBox binaryPosCheckBox;
	@FXML
	private ListView<TemplateData> binaryPosListView;
	@FXML
	private CheckBox binaryNegCheckBox;
	@FXML
	private ListView<TemplateData> binaryNegListView;
	@FXML
	private CheckBox choiceCheckBox;
	@FXML
	private ListView<TemplateData> choiceListView;
	@FXML
	private Button closeButton;
	@FXML
	private VBox descriptionBox;
	@FXML
	private Label defaultDescriptionLabel;
	@FXML
	private GridPane descriptionPane;
	@FXML
	private Label templateNameLabel;
	@FXML
	private WebView templateDescriptionWebView;
	@FXML
	private Label templateDescriptionText;

	private boolean isWebViewInitialized = false;
	private String initialWebViewScript;

	@FXML
	private void initialize() {
		StringConverter<TemplateData> templateConverter = new StringConverter<TemplateData>() {
			@Override
			public String toString(TemplateData object) {
				return object != null ? object.getConstraintTemplate().getDisplayText() : "";
			}
			@Override
			public TemplateData fromString(String string) {
				return null;
			}
		};

		for (ConstraintTemplate ct : Arrays.asList(ConstraintTemplate.values())) {
			if (!ct.getIsBinary()) {
				templateDataList.add(new TemplateData(ct, true));

			} else if (!ct.getIsNegative()) {
				if (ct == ConstraintTemplate.Choice || ct == ConstraintTemplate.Exclusive_Choice)
					templateDataList.add(new TemplateData(ct, false));
				else
					templateDataList.add(new TemplateData(ct, true));
			} else
				templateDataList.add(new TemplateData(ct, false));
		}

		templateSections.put(unaryCheckBox, unaryListView);
		templateSections.put(binaryPosCheckBox, binaryPosListView);
		templateSections.put(binaryNegCheckBox, binaryNegListView);
		templateSections.put(choiceCheckBox, choiceListView);

		double layerWidth = 0.0;
		for (Map.Entry<CheckBox, ListView<TemplateData>> entry : templateSections.entrySet()) {
			CheckBox checkBox = entry.getKey();
			ListView<TemplateData> listView = entry.getValue();

			listView.setFixedCellSize(CELL_HEIGHT);
			layerWidth += listView.getMaxWidth();

			listView.setCellFactory(CheckBoxListCell.forListView(TemplateData::isSelectedProperty, templateConverter));

			listView.addEventFilter(ScrollEvent.ANY, scrollEvent -> {
				listView.requestFocus();
			});

			templateDataList.forEach(item -> {
				item.isSelectedProperty().addListener((observable, oldValue, newValue) -> {
					int totalElemSize = listView.getItems().size();
					int selectedElemSize = listView.getItems().filtered(templ -> templ.getIsSelected()).size();

					if (totalElemSize == selectedElemSize) {
						checkBox.setIndeterminate(false);
						checkBox.setSelected(true);
					} else if (selectedElemSize == 0) {
						checkBox.setIndeterminate(false);
						checkBox.setSelected(false);
					} else
						checkBox.setIndeterminate(true);
				});
			});

			checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
				// newValue is true when the check box is selected, false otherwise
				for (TemplateData templateData : listView.getItems())
					templateData.setIsSelected(newValue);
			});

			listView.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					defaultDescriptionLabel.setVisible(false);
					defaultDescriptionLabel.setManaged(false);
					descriptionPane.setVisible(true);
					descriptionPane.setManaged(true);

					if (!isWebViewInitialized)
						setupVisualizationWebView();

					if (listView.getSelectionModel().getSelectedItem() != null) {
						ConstraintTemplate ct = listView.getSelectionModel().getSelectedItem().getConstraintTemplate();
	
						templateNameLabel.setText(ct.getDisplayText());
						if (ct.getIsBinary())
							templateDescriptionText.setText(TemplateDescription.getWithExamples(ct, "A", "B"));
						else
							templateDescriptionText.setText(TemplateDescription.getWithExamples(ct, "A"));
						
						String visualization;
						DiscoveredActivity a = new DiscoveredActivity("A", -1);
	
						if (ct.getIsBinary()) {
							DiscoveredActivity b = new DiscoveredActivity("B", -1);
							DiscoveredConstraint c = new DiscoveredConstraint(ct, a, b);
							visualization = GraphGenerator.createTemplateDescriptionVisualizationString(List.of(a, b), c);
	
						} else {
							DiscoveredConstraint c = new DiscoveredConstraint(ct, a, null);
							visualization = GraphGenerator.createTemplateDescriptionVisualizationString(List.of(a), c);
						}
	
						if (visualization != null) {
							String script = "setModel('" + visualization + "')";
							if (templateDescriptionWebView.getEngine().getLoadWorker().stateProperty().get() == Worker.State.SUCCEEDED) {
								logger.debug("Executing visualization script: {}", StringUtils.abbreviate(script, 1000));
								templateDescriptionWebView.getEngine().executeScript(script);
	
							} else {
								initialWebViewScript = script;
							}
						}
					}
				}
			});
		}

		rootRegion.setMaxWidth(layerWidth);
		rootRegion.setMinWidth(layerWidth);

		tempalteSettingsVbox.setOnMouseClicked(event -> {
			event.consume(); //Avoids closing the layer when user clicks on an empty area of the settings region
		});
		
		descriptionPane.setVisible(false);
		descriptionPane.setManaged(false);

		logger.debug("Template settings layer initialized");
	}

	@FXML
	private void selectAll() {
		for (ListView<TemplateData> listView : templateSections.values())
			for (TemplateData templateData : listView.getItems())
				templateData.setIsSelected(true);
	}

	@FXML
	private void clearAll() {
		for (ListView<TemplateData> listView : templateSections.values())
			for (TemplateData templateData : listView.getItems())
				templateData.setIsSelected(false);
	}

	public Button getCloseButton() {
		return closeButton;
	}

	public void setAvailableTemplates(DiscoveryMethod discoveryMethod, boolean discoverTimeConditions) {
		for (ListView<TemplateData> listView : templateSections.values())
			listView.getItems().clear();

		List<ConstraintTemplate> availableTemplates = new LinkedList<>();
		switch (discoveryMethod) {
		case DECLARE:
			for (ConstraintTemplate ct : TemplateUtils.DECLARE_TEMPLATES) {
				if (!discoverTimeConditions)
					availableTemplates.add(ct);
				else if (TemplateUtils.TIMED_TEMPLATES.contains(ct))
					availableTemplates.add(ct);
			}
			break;

		case MINERFUL:
			for (ConstraintTemplate ct : TemplateUtils.MINERFUL_TEMPLATES) {
				if (!discoverTimeConditions)
					availableTemplates.add(ct);
				else if (TemplateUtils.TIMED_TEMPLATES.contains(ct))
					availableTemplates.add(ct);
			}
			break;

		case MP_DECLARE:
		case MP_MINERFUL:
			for (ConstraintTemplate ct : TemplateUtils.MP_TEMPLATES) {
				if (!discoverTimeConditions)
					availableTemplates.add(ct);
				else if (TemplateUtils.TIMED_TEMPLATES.contains(ct))
					availableTemplates.add(ct);
			}
			break;

		default:
			//TODO: Disable template settings layer
			titleLabel.setText("Selected Templates (Unknown Discovery Method)");
			logger.error("Can not set template selection to unhandled method: {}", discoveryMethod);
			break;
		}

		if (!availableTemplates.isEmpty()) {
			titleLabel.setText("Selected Templates (" + discoveryMethod.getDisplayText() + ")");
			logger.debug("Template selection set to method: {}", discoveryMethod);
		}

		List<TemplateData> unaryList = new LinkedList<>();
		List<TemplateData> binaryPosList = new LinkedList<>();
		List<TemplateData> binaryNegList = new LinkedList<>();
		List<TemplateData> choiceList = new LinkedList<>();

		for (ConstraintTemplate ct : availableTemplates) {
			TemplateData dummyTemplate = new TemplateData(ct, false);
			TemplateData templateToAdd = templateDataList.get(templateDataList.indexOf(dummyTemplate));
			templateToAdd.setIsSelected(templateToAdd.getIsSelected()); // For triggering the general checkbox listeners

			if (!ct.getIsBinary()) {
				unaryList.add(templateToAdd);

			} else if (!ct.getIsNegative()) {
				if (ct == ConstraintTemplate.Choice || ct == ConstraintTemplate.Exclusive_Choice)
					choiceList.add(templateToAdd);
				else
					binaryPosList.add(templateToAdd);
			} else
				binaryNegList.add(templateToAdd);
		}

		unaryListView.setItems(FXCollections.observableArrayList(unaryList));
		binaryPosListView.setItems(FXCollections.observableArrayList(binaryPosList));
		binaryNegListView.setItems(FXCollections.observableArrayList(binaryNegList));
		choiceListView.setItems(FXCollections.observableArrayList(choiceList));


		int maxHeight = 0;
		for (Map.Entry<CheckBox, ListView<TemplateData>> entry : templateSections.entrySet()) {
			CheckBox checkBox = entry.getKey();
			ListView<TemplateData> listView = entry.getValue();

			// General checkboxes initialization
			if (listView.getItems().stream().allMatch(item -> item.getIsSelected())) {
				checkBox.setSelected(true);
				checkBox.setIndeterminate(false);
			} else if (listView.getItems().stream().allMatch(item -> !item.getIsSelected())) {
				checkBox.setSelected(false);
				checkBox.setIndeterminate(false);
			} else
				checkBox.setIndeterminate(true);


			// Listview visualization settings
			if (!listView.getItems().isEmpty()) {
				listView.getParent().setVisible(true);
				listView.getParent().setManaged(true);				

				// Setting list view heights to the correct value
				if (CELL_HEIGHT*listView.getItems().size() +35 > maxHeight)
					maxHeight = CELL_HEIGHT*listView.getItems().size() +35; // +35 to avoid the appearing of scroll bars

			} else {
				listView.getParent().setVisible(false);
				listView.getParent().setManaged(false);
			}
		}

		for (ListView<TemplateData> listView : templateSections.values()) {
			listView.setMinHeight(maxHeight);
			listView.setMaxHeight(maxHeight);
		}
	}

	public List<ConstraintTemplate> getSelectedTemplates() {
		List<ConstraintTemplate> selectedTemplates = new LinkedList<>();

		for (ListView<TemplateData> listView : templateSections.values()) {
			List<TemplateData> templateData = listView.getItems().filtered(item -> item.getIsSelected());
			selectedTemplates.addAll(templateData.stream().map(TemplateData::getConstraintTemplate).collect(Collectors.toList()));
		}

		logger.info("Returning currently selected templates: {}", selectedTemplates);
		return selectedTemplates;
	}

	private void setupVisualizationWebView() {
		templateDescriptionWebView.getEngine().load((getClass().getClassLoader().getResource("test.html")).toString());
		templateDescriptionWebView.setContextMenuEnabled(false); //Setting it in FXML causes an IllegalArgumentException
		JSObject window = (JSObject) templateDescriptionWebView.getEngine().executeScript("window");
		window.setMember("rum_application", this); //Allows calling public methods of this class from JavaScript

		templateDescriptionWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue == Worker.State.SUCCEEDED && initialWebViewScript != null) {
				logger.debug("Updating visualization in discovery tab: {}", StringUtils.abbreviate(initialWebViewScript, 1000));
				templateDescriptionWebView.getEngine().executeScript(initialWebViewScript);
			}
			logger.debug("Visualization html loaded in templates settings layer.");
		});

		templateDescriptionWebView.setZoom(0.85);

		isWebViewInitialized = true;
	}
}
