package controller.editor;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.javafx.FontIcon;
import org.processmining.ltl2automaton.plugins.ltl.SyntaxParserException;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventTarget;

import controller.common.AbstractController;
import controller.editor.data.ConstraintDataRow;
import datatable.AbstractDataRow.RowStatus;
import datatable.CellDataWrapper;
import datatable.cell.ActionCell;
import datatable.cell.ComboBoxCell;
import datatable.cell.TextCell;
import global.Inventory;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import gui.ava.html.Html2Image;
import javafx.animation.TranslateTransition;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ScrollEvent;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javafx.util.StringConverter;
import netscape.javascript.JSObject;
import rum.algorithms.mobuconltl.model.DeclareTemplate;
import rum.algorithms.mobuconltl.utils.AutomatonUtils;
import treedata.TreeDataActivity;
import treedata.TreeDataAttribute;
import treedata.TreeDataBase;
import util.AlertUtils;
import util.ConstraintTemplate;
import util.ConstraintUtils;
import util.DataTableUtils;
import util.FileUtils;
import util.GraphGenerator;
import util.ModelExportChoice;
import util.ModelExporter;
import util.ModelUtils;
import util.ModelViewType;
import util.ValidationUtils;

public class EditorTabController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private final PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");

	@FXML
	private VBox activitiesVBox;
	@FXML
	private TitledPane voiceInputPane;
	@FXML
	private TitledPane activitiesAttributesPane;
	@FXML
	private TreeView<TreeDataBase> activitiesTreeView;
	@FXML
	private Button editNewActivityButton;
	@FXML
	private Button editNewAttributeButton;
	@FXML
	private StackPane editorSidePanelLayer;
	@FXML
	private SplitPane editorSplitPane;
	@FXML
	private Slider zoomSlider;
	@FXML
	private TextField zoomValueField;
	@FXML
	private ChoiceBox<ModelViewType> modelViewChoiceBox;
	@FXML
	private HBox saveButtonsHbox;
	@FXML
	private HBox showLabelsHBox;
	@FXML
	private ToggleSwitch enableGroqToggle;
	@FXML
	private ToggleSwitch constraintLabelsToggle;
	@FXML
	private ToggleSwitch conditionLabelsToggle;
	@FXML
	private ToggleSwitch alternativeLayoutToggle;
	@FXML
	private WebView visualizationWebView;
	@FXML
	private Button checkConsistencyButton;
	@FXML
	private TableView<ConstraintDataRow> constraintsTable;
	@FXML
	private TableColumn<ConstraintDataRow,CellDataWrapper<ConstraintTemplate>> templateColumn;
	@FXML
	private TableColumn<ConstraintDataRow,CellDataWrapper<TreeDataActivity>> activationActivityColumn;
	@FXML
	private TableColumn<ConstraintDataRow,CellDataWrapper<String>> activationConditionColumn;
	@FXML
	private FontIcon activationConditionHelpIcon;
	@FXML
	private TableColumn<ConstraintDataRow,CellDataWrapper<TreeDataActivity>> targetActivityColumn;
	@FXML
	private TableColumn<ConstraintDataRow,CellDataWrapper<String>> correlationConditionColumn;
	@FXML
	private FontIcon correlationConditionHelpIcon;
	@FXML
	private TableColumn<ConstraintDataRow,CellDataWrapper<String>> timeConditionColumn;
	@FXML
	private FontIcon timeConditionHelpIcon;
	@FXML
	private TableColumn<ConstraintDataRow,RowStatus> constraintRowActionsColumn;

	private static String precentageFormat = "%.1f";
	
	//Prevents slider value properties from being garbage collected
	private ObjectProperty<Double> zoomSliderValueObject;
	private ObjectProperty<Double> visualizationZoomObject;

	private File declModel;
	private Tab editorTab;

	private ListView<HBox> chatListView;

	private TreeDataBase activitiesRoot;
	private TreeDataBase attributesDummyRoot;
	private TreeDataBase currentlyEditingTreeData;

	private ActivityEditingPanelController activityEditingPanelController;
	private AttributeEditingPanelController attributeEditingPanelController;
	private TranslateTransition activityEditingSlideTransition;
	private TranslateTransition attributeEditingSlideTransition;

	private ObservableList<TreeDataAttribute> allAttributes = FXCollections.observableArrayList(
			e -> new Observable[] {e.displayTextProperty()} );
	private ObservableList<TreeDataActivity> allActivitiesWithNull = FXCollections.observableArrayList(
			e -> new Observable[] {e.displayTextProperty()} );

	private String initialWebViewScript;

	@FXML
	private void initialize() {
		activitiesVBox.setViewOrder(-1); //Makes sure that editing panels slide in from under the activities overview

		VoiceInputChatBox voiceInputChatBox = new VoiceInputChatBox();
		voiceInputChatBox.setEditorTabController(this);
		chatListView = voiceInputChatBox.getRoot();
		voiceInputPane.setContent(chatListView);

		setupActivitiesTreeView();
		setupConstraintsTable();
		installConstraintsTableTooltips();
		setupVisualizationWebView();

		//Saving enabled only when there is something in the model
		saveButtonsHbox.disableProperty().bind(
			Bindings.and(
				Bindings.size(activitiesRoot.getChildren()).isEqualTo(0),
				Bindings.size(constraintsTable.getItems().filtered(predicate -> predicate.getRowStatus() != RowStatus.NEW)).isEqualTo(0)
			)
		);
		
		//Checking consistency enabled only when there is something in the model
		checkConsistencyButton.disableProperty().bind(
			Bindings.size(constraintsTable.getItems().filtered(predicate -> predicate.getRowStatus() != RowStatus.NEW)).isEqualTo(0)
		);

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

		modelViewChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			boolean isDeclare = (newV == ModelViewType.DECLARE);
			showLabelsHBox.setVisible(isDeclare);
			showLabelsHBox.setManaged(isDeclare);
			
			updateVisualization();
		});

		enableGroqToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue){
				askForAPIKey();
			}else{
				enableGroqToggle.setDisable(false);
				enableGroqToggle.setSelected(false);
				setIsValidAPIKey(false);
			}

		});

		constraintLabelsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (modelViewChoiceBox.getSelectionModel().getSelectedItem() == ModelViewType.DECLARE)
				updateVisualization();
		});
		conditionLabelsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (modelViewChoiceBox.getSelectionModel().getSelectedItem() == ModelViewType.DECLARE)
				updateVisualization();
		});
		alternativeLayoutToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (modelViewChoiceBox.getSelectionModel().getSelectedItem() == ModelViewType.DECLARE)
				updateVisualization();
		});
		
		// Disables the editor area when a slide in panel is visible
		editorSidePanelLayer.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
			while (change.next())
				editorSplitPane.setDisable(change.getList().size() > 1);
		});

		logger.debug("Editor tab initialized");
	}

	@FXML
	private void editNewActivity() {
		startTreeDataItemEdit(activitiesRoot);
	}

	@FXML
	private void editNewAttribute() {
		startTreeDataItemEdit(attributesDummyRoot);
	}
	
	@FXML
	private void hideSidePanel() {
		editorSidePanelLayer.getChildren().remove(activityEditingPanelController.getRootRegion());
		editorSidePanelLayer.getChildren().remove(attributeEditingPanelController.getRootRegion());
		updateCurrentlyEditingTreeData(null);
	}

	private boolean isValidAPIKey;
	private boolean isGroqToggleEnabled;

	public void askForAPIKey(){
		enableGroqToggle.setDisable(true);

		GridPane pane = new GridPane();
		pane.setStyle("-fx-alignment: center;");
		pane.setStyle("-fx-font-size: 12px");
		pane.setAlignment(Pos.CENTER);
		pane.setVgap(10);
		pane.setHgap(10);
		pane.setPadding(new Insets(10,10,10,10));

		Scene scene = new Scene(pane, 300,100);
		TextField textField = new TextField();
		textField.setFont(Font.font("Arial"));
		pane.add(textField, 0,0);

		Button button = new Button("send");
		button.setFont(Font.font("Arial"));
		pane.add(button, 1,0);

		Stage stage = new Stage();

		stage.setScene(scene);
		stage.setTitle("Please insert the API Key");
		stage.setResizable(false);
		stage.show();

		button.setOnAction(event -> {
			String newAPIKey = textField.getText();
            try {
                setIsValidAPIKey(validateAPIKey(newAPIKey));
				isGroqToggleEnabled = true;
				if (getIsValidAPIKey()){
					stage.close();
					enableGroqToggle.setDisable(false);
				}
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

		if (!getIsValidAPIKey()){
			stage.setOnCloseRequest(event -> {
				enableGroqToggle.setDisable(false);
				enableGroqToggle.setSelected(false);
			});
		}
	}

	public void setIsValidAPIKey(boolean validAPIKey){
		isValidAPIKey = validAPIKey;
	}

	public boolean getIsValidAPIKey(){
		return isValidAPIKey;
	}

	public boolean getIsGroqToggleEnabled(){
		return isGroqToggleEnabled;
	}

	public boolean checkInternetStatus(){
		return true;
	}

	public boolean validateAPIKey(String APIKey) throws IOException {
		URL url = URI.create("http://localhost:8080/api/v1/llm/keyValidation").toURL();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");

		String inputForm = "{ \"query\":" + "\"" + APIKey + "\"" + "}";

		try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())){
			os.writeBytes(inputForm);
		}

		String line = "";
		String fullLine = "";

		try (BufferedReader bf = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
			while ((line = bf.readLine()) != null){
				fullLine = line;
			}
		}

//		System.out.println(fullLine);

        return fullLine.equals("{\"status\":\"success\",\"response\":\"true\"}");
    }

	public void startTreeDataItemEdit(TreeDataBase itemToEdit) {
		
		if (itemToEdit == currentlyEditingTreeData) {
			editorSidePanelLayer.getChildren().remove(activityEditingPanelController.getRootRegion());
			editorSidePanelLayer.getChildren().remove(attributeEditingPanelController.getRootRegion());
			updateCurrentlyEditingTreeData(itemToEdit);
		} else if (itemToEdit == activitiesRoot || itemToEdit instanceof TreeDataActivity) {
			updateCurrentlyEditingTreeData(itemToEdit);

			activityEditingPanelController.setEditingActivity(currentlyEditingTreeData);

			if (!editorSidePanelLayer.getChildren().contains(activityEditingPanelController.getRootRegion())) {
				if (editorSidePanelLayer.getChildren().remove(attributeEditingPanelController.getRootRegion())) {
					activityEditingPanelController.getRootRegion().setTranslateX(-1);
					editorSidePanelLayer.getChildren().add(activityEditingPanelController.getRootRegion());
				} else {
					editorSidePanelLayer.getChildren().add(activityEditingPanelController.getRootRegion());
					activityEditingSlideTransition.play();
				}
			}
		} else if (itemToEdit == attributesDummyRoot || itemToEdit instanceof TreeDataAttribute) {
			updateCurrentlyEditingTreeData(itemToEdit);

			attributeEditingPanelController.setEditingAttribute(currentlyEditingTreeData);

			if (!editorSidePanelLayer.getChildren().contains(attributeEditingPanelController.getRootRegion())) {
				if (editorSidePanelLayer.getChildren().remove(activityEditingPanelController.getRootRegion())) {
					attributeEditingPanelController.getRootRegion().setTranslateX(-1);
					editorSidePanelLayer.getChildren().add(attributeEditingPanelController.getRootRegion());
				} else {
					editorSidePanelLayer.getChildren().add(attributeEditingPanelController.getRootRegion());
					attributeEditingSlideTransition.play();
				}
			}
		}
	}

	public void deleteTreeDataItem(TreeDataBase itemToDelete) {
		TreeDataActivity treeDataActivity = (TreeDataActivity) itemToDelete;
		for (int i = treeDataActivity.getAttributesUnmodifiable().size()-1; i >= 0; i--)
			//Have to remove attributes from the activity first, otherwise they may remain in selections
			treeDataActivity.removeAttribute(treeDataActivity.getAttributesUnmodifiable().get(i));
		
		activitiesRoot.getChildren().remove(itemToDelete);
		if (itemToDelete == currentlyEditingTreeData)
			startTreeDataItemEdit(activitiesRoot);
		
		updateVisualization();
	}

	private void updateCurrentlyEditingTreeData(TreeDataBase itemToEdit) {
		if (itemToEdit == null) {
			if (currentlyEditingTreeData != null)
				currentlyEditingTreeData.setIsEditing(false);
			
			currentlyEditingTreeData = null;
		
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
	private void addConstraint() {
		ConstraintDataRow constraintRow = new ConstraintDataRow();
		constraintsTable.getItems().add(constraintRow);
		constraintsTable.scrollTo(constraintsTable.getItems().size()-1);
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
					writer.write(ModelExporter.getDeclString(activitiesRoot, allAttributes, constraintsTable.getItems()));
					editorTab.setText(chosenFile.getName());
					declModel = chosenFile;
					logger.info("Model exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Model saved successfully");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the model failed!");
					logger.error("Unable to export model: {}", chosenFile.getAbsolutePath(), e);
				}
				break;
			case TEXTUAL:
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());
				try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(chosenFile.getAbsolutePath()))) {
					writer.write(ModelExporter.getTextString(activitiesRoot, constraintsTable.getItems()));
					logger.info("Model exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Model saved successfully");
				} catch (IOException e) {
					AlertUtils.showError("Exporting the model failed!");
					logger.error("Unable to export model: {}", chosenFile.getAbsolutePath(), e);
				}
				break;
			case AUTOMATON:
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());
				boolean exportSuccessful = ModelExporter.exportAutomaton(activitiesRoot, constraintsTable.getItems(), chosenFile);
				if (exportSuccessful) {
					logger.info("Model exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Model saved successfully");
				} else {
					AlertUtils.showError("Exporting the model failed!");
					logger.error("Unable to export model: {}", chosenFile.getAbsolutePath()); //No stacktrace to log because automaton exporter fails silently
				}
				break;
			case XML_MODEL:
				logger.debug("Exporting model to file: {}", chosenFile.getAbsolutePath());
				try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(chosenFile.getAbsolutePath()))) {
					writer.write(ModelExporter.getXmlString(activitiesRoot, allAttributes, constraintsTable.getItems()));
					editorTab.setText(chosenFile.getName());
					declModel = chosenFile;
					logger.info("Model exported to file: {}", chosenFile.getAbsolutePath());
					AlertUtils.showSuccess("Model saved successfully");
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

		TextInputDialog dialog;
		if (declModel == null) {
			dialog = new TextInputDialog("newmodel.decl");
		} else if (declModel.getName().endsWith(".decl")){
			dialog = new TextInputDialog(declModel.getName());
		} else {
			dialog = new TextInputDialog(declModel.getName() + ".decl");
		}
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
					writer.write(ModelExporter.getDeclString(activitiesRoot, allAttributes, constraintsTable.getItems()));
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
	private void checkConsistency() {
		List<String> formulasList = new ArrayList<>();
		
		// Encoding activity names to avoid syntax errors in LTL Formulas when special characters are used
		Map<String, String> activityEncoding = new HashMap<>();
		int ctr = 0;
		
		for (ConstraintDataRow cdr : constraintsTable.getItems()) {
			if (cdr.getRowStatus() == RowStatus.SAVED) {
				DeclareTemplate template = DeclareTemplate.valueOf(cdr.getTemplate().name());
				String ltlFormula = AutomatonUtils.getGenericLtlFormula(template);
				
				// Replacing activity placeholders in the generic formula with activity encodings based on the model
				String actName = cdr.getActivationActivity().getActivityName();
				if (!activityEncoding.containsKey(actName))
					activityEncoding.put(actName, "act" + (ctr++) );
				
				ltlFormula = ltlFormula.replace("\"A\"", activityEncoding.get(actName));
				
				if (template.getIsBinary()) {
					String trgName = cdr.getTargetActivity().getActivityName();
					if (!activityEncoding.containsKey(trgName))
						activityEncoding.put(trgName, "act" + (ctr++) );
					
					ltlFormula = ltlFormula.replace("\"B\"", activityEncoding.get(trgName));
				}
				
				formulasList.add(ltlFormula);
			}
		}
		
		String globalLtlFormula = "(" + String.join(") && (", formulasList) + ")";
		try {
			ExecutableAutomaton globalAutomaton = new ExecutableAutomaton(AutomatonUtils.createAutomatonForLtlFormula(globalLtlFormula));
			
			if (globalAutomaton.stateCount() == 1 && !globalAutomaton.states().iterator().next().isAccepting())
				AlertUtils.showWarning("There are conflicting constraints!");
			else
				AlertUtils.showSuccess("No conflicting constraints found");
			
		} catch (SyntaxParserException e) {
			AlertUtils.showError("Wrong constraint(s) syntax");
			logger.error("Wrong constraint(s) syntax", e);
		}
	}

	public void setModelData(File declModel) {
		this.declModel = declModel;

		List<String> activityList = ModelUtils.getActivityList(declModel);
		List<TreeDataActivity> treeDataActivities = new ArrayList<>();
		for (String activity : activityList) {
			TreeDataActivity treeDataActivity = new TreeDataActivity();
			treeDataActivity.setActivityName(activity);
			treeDataActivities.add(treeDataActivity);
		}

		List<String> constraintsList = ModelUtils.getConstraintsList(declModel);
		List<ConstraintDataRow> constraintRows = new ArrayList<>();
		for (String constraintString : constraintsList) {
			ConstraintDataRow detectedConstraint = ConstraintUtils.getConstraintDataRow(constraintString, treeDataActivities);
			if (detectedConstraint.validateRowEdit()) {
				detectedConstraint.confirmRowEdit();
			}
			constraintRows.add(detectedConstraint);
		}

		List<TreeDataAttribute> treeDataAttributes = ModelUtils.getTreeDataAttributes(declModel);
		if (!treeDataAttributes.isEmpty()) {
			ModelUtils.addAttributesToActivities(treeDataActivities, treeDataAttributes, declModel);
		}


		activitiesRoot.getChildren().addAll(treeDataActivities);
		constraintsTable.getItems().addAll(constraintRows);
		activitiesTreeView.setPrefHeight(activitiesTreeView.getExpandedItemCount() * 42d + 40d);
		updateVisualization();

		logger.debug("Model in editor tab set to: {}", declModel.getAbsolutePath());
	}

	public void setTab(Tab tab) {
		this.editorTab = tab;
	}

	public TreeDataBase getActivitiesRoot() {
		return activitiesRoot;
	}

	public ObservableList<ConstraintDataRow> getConstraintRows() {
		return constraintsTable.getItems();
	}

	public void scrollTablesToEnd() {
		activitiesTreeView.scrollTo(activitiesTreeView.getExpandedItemCount());
		constraintsTable.scrollTo(constraintsTable.getItems().size()-1);
	}

	private void setupConstraintsTable() {
		constraintsTable.setPlaceholder(new Label("No constraints added"));

		List<ConstraintTemplate> constraintTemplates = Arrays.asList(ConstraintTemplate.values()).stream().filter(templ -> !templ.equals(ConstraintTemplate.End)).collect(Collectors.toList());
		StringConverter<ConstraintTemplate> templateConverter = new StringConverter<ConstraintTemplate>() {
			@Override
			public String toString(ConstraintTemplate object) {
				return object != null ? object.getDisplayText() : "";
			}
			@Override
			public ConstraintTemplate fromString(String string) {
				return null;
			}
		};

		StringConverter<TreeDataActivity> activityConverter = new StringConverter<TreeDataActivity>() {
			@Override
			public String toString(TreeDataActivity object) {
				return object != null ? object.getActivityName() : "";
			}
			@Override
			public TreeDataActivity fromString(String string) {
				return null;
			}
		};


		ObservableList<ConstraintDataRow> constraintsTableItems = FXCollections.observableArrayList(
			e -> new Observable[] { e.rowStatusProperty() }
		);
		constraintsTable.setItems(constraintsTableItems);

		DataTableUtils.setDefaultRowFactory(constraintsTable);
		DataTableUtils.addScrollFilter(constraintsTable);
		DataTableUtils.setContentBasedHeight(constraintsTable, 31d, 33d);

		templateColumn.setCellValueFactory(new PropertyValueFactory<>("template"));
		templateColumn.setCellFactory(param -> new ComboBoxCell<ConstraintDataRow, ConstraintTemplate>(constraintTemplates, templateConverter));
		templateColumn.setReorderable(false);
		templateColumn.sortTypeProperty().addListener((obs, oldValue, newValue) ->
			templateColumn.setSortType(SortType.ASCENDING)	// The table should be sorted only in ascending order
		);
		
		activationActivityColumn.setCellValueFactory(new PropertyValueFactory<>("activationActivity"));
		activationActivityColumn.setCellFactory(param -> new ComboBoxCell<ConstraintDataRow, TreeDataActivity>(allActivitiesWithNull, activityConverter));
		activationActivityColumn.setReorderable(false);
		activationActivityColumn.sortTypeProperty().addListener((obs, oldValue, newValue) ->
			activationActivityColumn.setSortType(SortType.ASCENDING)	// The table should be sorted only in ascending order
		);
		
		activationConditionColumn.setCellValueFactory(new PropertyValueFactory<>("activationCondition"));
		activationConditionColumn.setCellFactory(param -> new TextCell<ConstraintDataRow>());
		activationConditionColumn.setReorderable(false);
		activationConditionColumn.sortTypeProperty().addListener((obs, oldValue, newValue) ->
			activationConditionColumn.setSortType(SortType.ASCENDING)	// The table should be sorted only in ascending order
		);
		
		targetActivityColumn.setCellValueFactory(new PropertyValueFactory<>("targetActivity"));
		targetActivityColumn.setCellFactory(param -> new ComboBoxCell<ConstraintDataRow, TreeDataActivity>(allActivitiesWithNull, activityConverter));
		targetActivityColumn.setReorderable(false);
		targetActivityColumn.sortTypeProperty().addListener((obs, oldValue, newValue) ->
			targetActivityColumn.setSortType(SortType.ASCENDING)	// The table should be sorted only in ascending order
		);
		
		correlationConditionColumn.setCellValueFactory(new PropertyValueFactory<>("correlationCondition"));
		correlationConditionColumn.setCellFactory(param -> new TextCell<ConstraintDataRow>());
		correlationConditionColumn.setReorderable(false);
		correlationConditionColumn.sortTypeProperty().addListener((obs, oldValue, newValue) ->
			correlationConditionColumn.setSortType(SortType.ASCENDING)	// The table should be sorted only in ascending order
		);
		
		timeConditionColumn.setCellValueFactory(new PropertyValueFactory<>("timeCondition"));
		timeConditionColumn.setCellFactory(param -> new TextCell<ConstraintDataRow>());
		timeConditionColumn.setReorderable(false);
		timeConditionColumn.sortTypeProperty().addListener((obs, oldValue, newValue) ->
			timeConditionColumn.setSortType(SortType.ASCENDING)		// The table should be sorted only in ascending order
		);
		

		constraintRowActionsColumn.setCellValueFactory(new PropertyValueFactory<>("rowStatus"));
		constraintRowActionsColumn.setCellFactory(param -> new ActionCell<ConstraintDataRow>(templateColumn, activationActivityColumn, activationConditionColumn, targetActivityColumn, correlationConditionColumn, timeConditionColumn));
		constraintRowActionsColumn.setReorderable(false);

		constraintsTable.getItems().addListener((ListChangeListener<ConstraintDataRow>)(change -> {
			while (change.next()) {
				if (change.wasUpdated()) {
					for (int i = change.getFrom(); i < change.getTo(); ++i) {
						if (constraintsTable.getItems().get(i).getRowStatus() == RowStatus.SAVED) {
							//Updates visualization only when a row is saved
							updateVisualization();
							break;
						}
					}
				} else if (change.wasRemoved()) {
					updateVisualization();
				}
			}
		}));

		logger.debug("Constraints table setup done");
	}

	private void installConstraintsTableTooltips() {
		String activationCorrelationHelpString = "You can refer to attributes of activity A and T using '.':" +
				"  'A.TransportType'\n" +
				"  'T.PhoneNumber'\n" +
				"Operations on attribute values for enum\n" +
				"  'A.TransportType is Car'\n" +
				"  'A.TransportType is not Car'\n" +
				"  'A.TransportType in (Car, Train)'\n" +
				"  'A.TransportType not in (Car, Train)'\n" +
				"Operations on attribute values for numeric attribute\n" +
				"  'A.Price > 10'\n" +
				"  'A.Price <= 5'\n" +
				"  'A.Price = 3'\n" +
				"Operations can be joined with 'and' and 'or':\n" +
				"  'T.Price <= 10 or T.Price>100'\n" +
				"If both A and T have the same data attribute, 'same' and 'different' constraints can be used:\n" +
				"  'same Price'\n" +
				"  'different Group'\n";

		String timeHelpString = "Time conditions use the syntax:\n" +
				"after_activation_unit_min,after_activation_unit_max,time_unit\n" +
				"  after_activation_unit_min - positive integer\n" +
				"  after_activation_unit_max - positive integer\n" +
				"  time_unit - one of the following: s, m, h or d\n" +
				"    s: second, m: minute, h: hour, d: day\n\n" +
				"Example: 2,5,h -> between two and five hours";

		
		Tooltip activationTooltip = new Tooltip(activationCorrelationHelpString);
		activationTooltip.setStyle("-fx-font-size: 12px");
		activationTooltip.setShowDuration(Duration.INDEFINITE);
		activationTooltip.setOnShowing(event -> {
			Bounds boundsInScene = activationConditionHelpIcon.localToScreen(activationConditionHelpIcon.getBoundsInLocal());
			double desiredX = boundsInScene.getCenterX() - activationTooltip.getWidth()/2;
			double desiredY = boundsInScene.getMinY() - activationTooltip.getHeight();
			activationTooltip.show(activationConditionHelpIcon, desiredX, desiredY);
		});
		Tooltip.install(activationConditionHelpIcon, activationTooltip);
		
		
		Tooltip correlationTooltip = new Tooltip(activationCorrelationHelpString);
		correlationTooltip.setStyle("-fx-font-size: 12px");
		correlationTooltip.setShowDuration(Duration.INDEFINITE);
		correlationTooltip.setOnShowing(event -> {
			Bounds boundsInScene = correlationConditionHelpIcon.localToScreen(correlationConditionHelpIcon.getBoundsInLocal());
			double desiredX = boundsInScene.getCenterX() - correlationTooltip.getWidth()/2;
			double desiredY = boundsInScene.getMinY() - correlationTooltip.getHeight();
			correlationTooltip.show(correlationConditionHelpIcon, desiredX, desiredY);
		});
		Tooltip.install(correlationConditionHelpIcon, correlationTooltip);
		
		
		Tooltip timeTooltip = new Tooltip(timeHelpString);
		timeTooltip.setStyle("-fx-font-size: 12px");
		timeTooltip.setShowDuration(Duration.INDEFINITE);
		timeTooltip.setOnShowing(event -> {
			Bounds boundsInScene = timeConditionHelpIcon.localToScreen(timeConditionHelpIcon.getBoundsInLocal());
			double desiredX = boundsInScene.getCenterX() - timeTooltip.getWidth()/2;
			double desiredY = boundsInScene.getMinY() - timeTooltip.getHeight();
			timeTooltip.show(timeConditionHelpIcon, desiredX, desiredY);
		});
		Tooltip.install(timeConditionHelpIcon, timeTooltip);
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

	private void setupActivitiesTreeView() {
		activitiesRoot = new TreeDataBase();
		activitiesTreeView.setRoot(activitiesRoot);
		activitiesTreeView.setShowRoot(false);
		activitiesRoot.getIsEditingWrapper().addListener((observable,oldValue,newValue) -> {
			if (newValue.equals(Boolean.TRUE)) {
				editNewActivityButton.pseudoClassStateChanged(selectedClass, true);
				editNewActivityButton.setText("Minimize activities");
				((FontIcon)editNewActivityButton.getGraphic()).setIconLiteral("fa-angle-double-left");
			} else {
				editNewActivityButton.pseudoClassStateChanged(selectedClass, false);
				editNewActivityButton.setText("Add activities");
				((FontIcon)editNewActivityButton.getGraphic()).setIconLiteral("fa-angle-double-right");
			}
		});
		activitiesTreeView.setCellFactory(param -> new TreeDataCell(this));
		activitiesTreeView.setFixedCellSize(42d);

		activitiesAttributesPane.expandedProperty().addListener((observable, oldVal, newVal) -> {
			if (newVal.equals(Boolean.TRUE))
				chatListView.scrollTo(chatListView.getItems().size()-1);
		});

		attributesDummyRoot = new TreeDataBase();
		attributesDummyRoot.getIsEditingWrapper().addListener((observable,oldValue,newValue) -> {
			if (newValue.equals(Boolean.TRUE)) {
				editNewAttributeButton.pseudoClassStateChanged(selectedClass, true);
				editNewAttributeButton.setText("Minimize attributes");
				((FontIcon)editNewAttributeButton.getGraphic()).setIconLiteral("fa-angle-double-left");
			} else {
				editNewAttributeButton.pseudoClassStateChanged(selectedClass, false);
				editNewAttributeButton.setText("Add attributes");
				((FontIcon)editNewAttributeButton.getGraphic()).setIconLiteral("fa-angle-double-right");
			}
		});

		//Preloading activityEditingPanel
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/editor/ActivityEditingPanel.fxml"));
			loader.load();
			activityEditingPanelController = loader.getController();

			//Preparation for slide in animation
			Region activityEditingPanelRootRegion = activityEditingPanelController.getRootRegion();
			activityEditingSlideTransition = new TranslateTransition(new Duration(200), activityEditingPanelRootRegion);
			activityEditingSlideTransition.setFromX(-1 * activityEditingPanelRootRegion.getPrefWidth());
			activityEditingSlideTransition.setToX(-1); //-1 so that it would cover the parameters section border

			activityEditingPanelController.setEditorTabController(this);
			activityEditingPanelController.setActivitiesRoot(activitiesRoot);
			activityEditingPanelController.connectToAttributesList(allAttributes);
			activityEditingPanelController.getCloseButton().setOnAction(event -> {
				editorSidePanelLayer.getChildren().remove(activityEditingPanelController.getRootRegion());
				updateCurrentlyEditingTreeData(null);
			});
		} catch (IOException | IllegalStateException e) {
			logger.error("Can not load activity editing panel", e);
			//TODO: Alert the user and disable activity editing buttons
			editNewActivityButton.setDisable(true);
		}

		//Preloading attributeEditingPanel
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/editor/AttributeEditingPanel.fxml"));
			loader.load();
			attributeEditingPanelController = loader.getController();

			//Preparation for slide in animation
			Region attributeEditingPanelRootRegion = attributeEditingPanelController.getRootRegion();
			attributeEditingSlideTransition = new TranslateTransition(new Duration(200), attributeEditingPanelRootRegion);
			attributeEditingSlideTransition.setFromX(-1 * attributeEditingPanelRootRegion.getPrefWidth());
			attributeEditingSlideTransition.setToX(-1); //-1 so that it would cover the parameters section border

			attributeEditingPanelController.setEditorTabController(this);
			attributeEditingPanelController.setAttributesDummyRoot(attributesDummyRoot);
			attributeEditingPanelController.connectToActivitiesRoot(activitiesRoot);
			attributeEditingPanelController.getCloseButton().setOnAction(event -> {
				editorSidePanelLayer.getChildren().remove(attributeEditingPanelController.getRootRegion());
				updateCurrentlyEditingTreeData(null);
			});
		} catch (IOException | IllegalStateException e) {
			logger.error("Can not load attribute editing panel", e);
			//TODO: Alert the user and disable attribute editing buttons
			editNewAttributeButton.setDisable(true);
		}

		activitiesRoot.addEventHandler(TreeItem.valueChangedEvent(), event -> updateVisualization() );

		activitiesRoot.addEventHandler(TreeItem.childrenModificationEvent(), new EventHandler<TreeItem.TreeModificationEvent<TreeDataBase>>() {
			@Override
			public void handle(TreeModificationEvent<TreeDataBase> event) {

				for (TreeItem<TreeDataBase> treeItem : event.getAddedChildren()) {
					if (treeItem instanceof TreeDataActivity) {
						allActivitiesWithNull.add((TreeDataActivity) treeItem);
						for (TreeItem<TreeDataBase> treeDataAttribute : treeItem.getChildren()) {
							if (treeDataAttribute instanceof TreeDataAttribute && !allAttributes.contains(treeDataAttribute)) {
								allAttributes.add((TreeDataAttribute) treeDataAttribute);
							}
						}
						activitiesTreeView.scrollTo(activitiesTreeView.getExpandedItemCount());
					} else if (treeItem instanceof TreeDataAttribute && !allAttributes.contains(treeItem)) {
						allAttributes.add((TreeDataAttribute) treeItem);
					}
				}

				for (TreeItem<TreeDataBase> treeItem : event.getRemovedChildren()) {
					if (treeItem instanceof TreeDataActivity) {
						allActivitiesWithNull.remove(treeItem);
						for (TreeItem<TreeDataBase> treeDataAttribute : treeItem.getChildren()) {
							if (treeDataAttribute instanceof TreeDataAttribute && ((TreeDataAttribute)treeDataAttribute).getActivitiesUnmodifiable().isEmpty()) {
								allAttributes.remove(treeDataAttribute);
							}
						}
					} else if (treeItem instanceof TreeDataAttribute && ((TreeDataAttribute)treeItem).getActivitiesUnmodifiable().isEmpty()) {
						allAttributes.remove(treeItem);
					}
				}
			}
		});

		activitiesRoot.addEventHandler(TreeItem.treeNotificationEvent(), event ->
			activitiesTreeView.setPrefHeight(activitiesTreeView.getExpandedItemCount() * 42d + 40d)
		);

		allActivitiesWithNull.add(null);
	}

	protected void updateVisualization() {
		if (!activitiesRoot.getChildren().isEmpty()) {
			ModelViewType selectedModelView = modelViewChoiceBox.getSelectionModel().getSelectedItem();
			String visualizationString;
			String script;

			switch (selectedModelView) {
			case DECLARE:
				visualizationString = GraphGenerator.createEditorVisualizationString(activitiesRoot, constraintsTable.getItems(), constraintLabelsToggle.isSelected(), conditionLabelsToggle.isSelected(), alternativeLayoutToggle.isSelected());
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
				String activitiesString = GraphGenerator.createActivitiesTextualString(activitiesRoot);
				String constraintsString = GraphGenerator.createConstraintsTextualString(constraintsTable.getItems());

				script = "setText('"+activitiesString+"','"+constraintsString+"')";
				if (visualizationWebView.getEngine().getLoadWorker().stateProperty().get() == Worker.State.SUCCEEDED) {
					logger.debug("Executing visualization script: {}", StringUtils.abbreviate(script, 1000));
					visualizationWebView.getEngine().executeScript(script);
				} else {
					initialWebViewScript = script;
				}
				break;
			case AUTOMATON:
				visualizationString = GraphGenerator.createAutomatonVisualizationString(activitiesRoot, constraintsTable.getItems());
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


		} else {
			//Reloading the page in case a previous visualization script is still executing
			//TODO: Should instead track if a visualization script is still executing and stop it (if it is possible)
			initialWebViewScript = null; //Has to be set to null because it will otherwise be executed after reload
			visualizationWebView.getEngine().reload();
		}

	}

	public ObservableList<TreeDataAttribute> getAllAttributes() {
		return allAttributes;
	}

	//Called form JavaScript (not used at the moment)
	public void addGraphClickHandlers() {
		Element graphRootElement = visualizationWebView.getEngine().getDocument().getElementById("graphRoot");
		for (int i = 0; i < graphRootElement.getChildNodes().getLength(); i++) {
			String id;
			if (graphRootElement.getChildNodes().item(i).getNodeName().equals("g")) {
				id = graphRootElement.getChildNodes().item(i).getAttributes().getNamedItem("id").getNodeValue();
				((EventTarget) graphRootElement.getChildNodes().item(i)).addEventListener("click", ev -> logger.debug("Clicked on visualization graph element {}", id), false);
			}
		}
	}
}
