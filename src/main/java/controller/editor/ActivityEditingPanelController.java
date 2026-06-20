package controller.editor;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import controller.common.AbstractController;
import controller.editor.data.AttributeSelectionData;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import treedata.TreeDataActivity;
import treedata.TreeDataAttribute;
import treedata.TreeDataBase;
import util.AlertUtils;
import util.ValidationUtils;

public class ActivityEditingPanelController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox activityEditingVbox;
	@FXML
	private Label titleLabel;
	@FXML
	private Button deleteButton;
	@FXML
	private GridPane activityDataGridPane;
	@FXML
	private TextField activityNameTextField;
	@FXML
	private ListView<AttributeSelectionData> attributeSelectionsListView;
	/*
	@FXML
	private Button newAttributeButton;
	*/
	@FXML
	private VBox newAttributeVBox;
	@FXML
	private TextField newAttributeName;
	@FXML
	private ChoiceBox<AttributeType> newAttributeType;
	@FXML
	private Label valueRangeLabel;
	@FXML
	private HBox valueRangeHBox;
	@FXML
	private TextField newAttributeValueFrom;
	@FXML
	private TextField newAttributeValueTo;
	@FXML
	private Label possibleValuesLabel;
	@FXML
	private VBox possibleValuesVBox;
	@FXML
	private TextField newAttributePossibleValues;
	@FXML
	private Button saveActivityButton;
	@FXML
	private FontIcon saveActivityFontIcon;
	@FXML
	private Button closeButton;

	private EditorTabController editorTabController;
	private TreeDataBase activitiesRoot;
	private TreeDataActivity currentlyEditingActivity;
	private ObservableList<AttributeSelectionData> attributeSelections = FXCollections.observableArrayList();
	private List<AttributeSelectionData> newAttributeSelections = new ArrayList<AttributeSelectionData>();

	@FXML
	private void initialize() {
		newAttributeVBox.setVisible(false);
		newAttributeVBox.setManaged(false);

		newAttributeType.getItems().setAll(AttributeType.values());
		newAttributeType.setConverter(new StringConverter<AttributeType>() {
			@Override
			public String toString(AttributeType attributeType) {
				return attributeType.getDisplayText();
			}
			@Override
			public AttributeType fromString(String string) {
				return null;
			}
		});

		newAttributeType.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			switch (newV) {
			case INTEGER:
				valueRangeLabel.setVisible(true);
				valueRangeLabel.setManaged(true);
				valueRangeHBox.setVisible(true);
				valueRangeHBox.setManaged(true);
				possibleValuesLabel.setVisible(false);
				possibleValuesLabel.setManaged(false);
				possibleValuesVBox.setVisible(false);
				possibleValuesVBox.setManaged(false);
				ValidationUtils.addMandatoryIntegerBehavior(newAttributeValueFrom, newAttributeValueTo);
				break;
			case FLOAT:
				valueRangeLabel.setVisible(true);
				valueRangeLabel.setManaged(true);
				valueRangeHBox.setVisible(true);
				valueRangeHBox.setManaged(true);
				possibleValuesLabel.setVisible(false);
				possibleValuesLabel.setManaged(false);
				possibleValuesVBox.setVisible(false);
				possibleValuesVBox.setManaged(false);
				ValidationUtils.addMandatoryDecimalBehavior(newAttributeValueFrom, newAttributeValueTo);
				break;
			case ENUMERATION:
				valueRangeLabel.setVisible(false);
				valueRangeLabel.setManaged(false);
				valueRangeHBox.setVisible(false);
				valueRangeHBox.setManaged(false);
				possibleValuesLabel.setVisible(true);
				possibleValuesLabel.setManaged(true);
				possibleValuesVBox.setVisible(true);
				possibleValuesVBox.setManaged(true);
				ValidationUtils.addMandatoryBehavior(newAttributePossibleValues);
				break;
			default:
				//TODO: Disable attribute editing and show error to user
				logger.error("Unhandled attribute type selected: {}", newV);
				break;
			}
		});

		StringConverter<AttributeSelectionData> templateConverter = new StringConverter<AttributeSelectionData>() {
			@Override
			public String toString(AttributeSelectionData object) {
				return object != null ? object.getTreeDataAttribute().getDisplayText() : "";
			}
			@Override
			public AttributeSelectionData fromString(String string) {
				return null;
			}
		};
		attributeSelectionsListView.setItems(attributeSelections);
		attributeSelectionsListView.setCellFactory(CheckBoxListCell.forListView(AttributeSelectionData::isSelectedProperty, templateConverter));
		attributeSelectionsListView.setFixedCellSize(42d);
		attributeSelections.addListener((ListChangeListener<AttributeSelectionData>)(change -> {
			attributeSelectionsListView.setPrefHeight(attributeSelectionsListView.getItems().size() * 42d + 40d);
		}));

		activityNameTextField.setOnKeyReleased((event) -> {
			if(event.getCode() == KeyCode.ENTER) {
				saveActivityButton.fire();
			}
		});

		ValidationUtils.addMandatoryBehavior(activityNameTextField, newAttributeName);
		
		activityEditingVbox.setOnMouseClicked(event -> {
			event.consume(); //Avoids closing the layer when user clicks on an empty area of the editing region
		});

		logger.debug("Activity editing panel initialized");
	}

	@FXML
	private void addNewAttribute() {
		setNewAttributeMode(true);
		clearAttributeFields();
	}

	@FXML
	private void newAttributeDone() {
		if (validateNewAttribute()) {
			//TODO: Should be refactored so that only handled attribute types will be saved
			TreeDataAttribute treeDataAttribute = new TreeDataAttribute();
			treeDataAttribute.setAttributeName(newAttributeName.getText());
			treeDataAttribute.setAttributeType(newAttributeType.getSelectionModel().getSelectedItem());
			switch (newAttributeType.getSelectionModel().getSelectedItem()) {
			case INTEGER: //Fall through intended
			case FLOAT:
				treeDataAttribute.setValueFrom(new BigDecimal(newAttributeValueFrom.getText()));
				treeDataAttribute.setValueTo(new BigDecimal(newAttributeValueTo.getText()));
				break;
			case ENUMERATION:
				List<String> possibleValues = new ArrayList<String>();
				for (String possibleValue : newAttributePossibleValues.getText().split(",")) {
					possibleValues.add(possibleValue.strip());
				}
				treeDataAttribute.setPossibleValues(possibleValues);
				break;
			default:
				logger.error("Unhandled attribute type for saving: {}", newAttributeType.getSelectionModel().getSelectedItem());
				AlertUtils.showWarning("Unhandled attribute type (" + newAttributeType.getSelectionModel().getSelectedItem() + "), attribute may be saved incorrectly!");
				break;
			}
			AttributeSelectionData newAttributeSelectionData = new AttributeSelectionData(treeDataAttribute, true, true);
			attributeSelections.add(newAttributeSelectionData);
			newAttributeSelections.add(newAttributeSelectionData);

			setNewAttributeMode(false);
			clearAttributeFields();
		}
	}

	@FXML
	private void newAttributeCancel() {
		setNewAttributeMode(false);
		clearAttributeFields();
	}

	//TODO: Remove code duplication (same code is in AttributeEditingPanel)
	private boolean validateNewAttribute() {
		boolean isValid = true;

		if (!ValidationUtils.validateMandatory(newAttributeName.getText())) {
			newAttributeName.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			newAttributeName.requestFocus();
			isValid = false;
		} else {
			for (AttributeSelectionData attributeSelectionData : attributeSelections) {
				if (attributeSelectionData.getTreeDataAttribute().getAttributeName().equals(newAttributeName.getText())) {
					newAttributeName.pseudoClassStateChanged(ValidationUtils.errorClass, true);
					newAttributeName.requestFocus();
					isValid = false;
				}
			}
		}

		switch (newAttributeType.getSelectionModel().getSelectedItem()) {
		case INTEGER:
			try {
				Integer.parseInt(newAttributeValueFrom.getText());
			} catch (NumberFormatException e) {
				newAttributeValueFrom.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				newAttributeValueFrom.requestFocus();
				isValid = false;
			}
			try {
				Integer.parseInt(newAttributeValueTo.getText());
			} catch (NumberFormatException e) {
				newAttributeValueTo.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				newAttributeValueTo.requestFocus();
				isValid = false;
			}
			if (isValid && Integer.parseInt(newAttributeValueFrom.getText()) > Integer.parseInt(newAttributeValueTo.getText())) {
				newAttributeValueFrom.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				newAttributeValueTo.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				newAttributeValueFrom.requestFocus();
				isValid = false;
			}
			break;
		case FLOAT:
			try {
				Float.parseFloat(newAttributeValueFrom.getText());
			} catch (NumberFormatException e) {
				newAttributeValueFrom.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				newAttributeValueFrom.requestFocus();
				isValid = false;
			}
			try {
				Float.parseFloat(newAttributeValueTo.getText());
			} catch (NumberFormatException e) {
				newAttributeValueTo.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				newAttributeValueTo.requestFocus();
				isValid = false;
			}
			if (isValid && Float.parseFloat(newAttributeValueFrom.getText()) > Float.parseFloat(newAttributeValueTo.getText())) {
				newAttributeValueFrom.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				newAttributeValueTo.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				newAttributeValueFrom.requestFocus();
				isValid = false;
			}
			break;
		case ENUMERATION:
			if (!ValidationUtils.validateMandatory(newAttributePossibleValues.getText())) {
				newAttributePossibleValues.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				newAttributePossibleValues.requestFocus();
				isValid = false;
			}
			break;
		default:
			//Error is shown to user upon saving
			logger.error("Unhandled attribute type for validation: {}", newAttributeType.getSelectionModel().getSelectedItem());
			break;
		}

		return isValid;
	}

	private void setNewAttributeMode(boolean enabled) {
		activityDataGridPane.setDisable(enabled);
		//newAttributeButton.setVisible(!enabled);
		//newAttributeButton.setManaged(!enabled);
		newAttributeVBox.setVisible(enabled);
		newAttributeVBox.setManaged(enabled);
		saveActivityButton.setDisable(enabled);
		closeButton.setDisable(enabled);
	}

	private void clearAttributeFields() {
		newAttributeName.setText("");
		newAttributeType.getSelectionModel().selectFirst();
		newAttributeValueFrom.setText("");
		newAttributeValueTo.setText("");
		newAttributePossibleValues.setText("");
	}

	public Button getCloseButton() {
		return closeButton;
	}

	public void setEditorTabController(EditorTabController editorTabController) {
		this.editorTabController = editorTabController;
	}

	public void setActivitiesRoot(TreeDataBase activitiesRoot) {
		this.activitiesRoot = activitiesRoot;
	}

	//Used for showing all attributes that are available in the model
	public void connectToAttributesList(ObservableList<TreeDataAttribute> allAttributes) {
		allAttributes.addListener((ListChangeListener<TreeDataAttribute>)(change -> {
			while (change.next()) {
				if (change.wasRemoved()) {
					attributeSelections.remove(change.getFrom(), change.getTo()+1);
				} else if (change.wasAdded()) {
					for (TreeDataAttribute treeDataAttribute : change.getAddedSubList()) {
						if (currentlyEditingActivity != null && currentlyEditingActivity.getAttributesUnmodifiable().contains(treeDataAttribute)) {
							attributeSelections.add(new AttributeSelectionData(treeDataAttribute, true, false));
						} else {
							attributeSelections.add(new AttributeSelectionData(treeDataAttribute, false, false));
						}
					}
				}
			}
		}));
	}


	public void setEditingActivity(TreeDataBase currentlyEditingTreeData) {
		if (currentlyEditingTreeData == activitiesRoot) {
			//Update panel for creating a new activity
			currentlyEditingActivity = null;
			titleLabel.setText("Add activity");
			deleteButton.setVisible(false);
			deleteButton.setManaged(false);
			deleteButton.setDisable(true);
			activityNameTextField.setText("");
			activityNameTextField.pseudoClassStateChanged(ValidationUtils.errorClass, false);
			saveActivityButton.setText("Add activity");
			saveActivityFontIcon.setIconLiteral("fa-plus");
			saveActivityButton.setOnAction(event -> saveNewActivity());
			setNewAttributeMode(false);

			//Clears attribute checkboxes
			for (int i = attributeSelections.size()-1; i >= 0; i--) {
				if (attributeSelections.get(i).getIsNew()) {
					attributeSelections.remove(i);
				} else {
					attributeSelections.get(i).setIsSelected(false);
				}
			}
		} else if (currentlyEditingTreeData instanceof TreeDataActivity) {
			//Update panel for editing an existing activity
			TreeDataActivity treeDataActivity = (TreeDataActivity) currentlyEditingTreeData;
			currentlyEditingActivity = treeDataActivity;
			titleLabel.setText("Editing \"" + treeDataActivity.getActivityName() + "\" activity");
			deleteButton.setVisible(true);
			deleteButton.setManaged(true);
			deleteButton.setDisable(false);
			deleteButton.setOnAction(event -> {
				editorTabController.deleteTreeDataItem(treeDataActivity);
			});
			activityNameTextField.setText(treeDataActivity.getActivityName());
			activityNameTextField.pseudoClassStateChanged(ValidationUtils.errorClass, false);
			saveActivityButton.setText("Save");
			saveActivityFontIcon.setIconLiteral("fa-check");
			saveActivityButton.setOnAction(event -> saveEditingActivity(treeDataActivity));
			setNewAttributeMode(false);

			//Resets attribute checkboxes for the selected activity
			for (int i = attributeSelections.size()-1; i >= 0; i--) {
				if (attributeSelections.get(i).getIsNew()) {
					attributeSelections.remove(i);
				} else if (treeDataActivity.getAttributesUnmodifiable().contains(attributeSelections.get(i).getTreeDataAttribute())) {
					attributeSelections.get(i).setIsSelected(true);
				} else {
					attributeSelections.get(i).setIsSelected(false);
				}
			}
		} else {
			AlertUtils.showError("Failed setting item to edit!"); //Users should never really see this error
			logger.error("Can not use activity editing panel for editing attributes");
		}
	}

	private void saveNewActivity() {
		if (validateActivity(null)) {
			TreeDataActivity newActivity = new TreeDataActivity();
			newActivity.setActivityName(activityNameTextField.getText());
			for (int i = 0; i < attributeSelections.size(); i++) {
				if (attributeSelections.get(i).getIsSelected()) {
					newActivity.addAttribute(attributeSelections.get(i).getTreeDataAttribute());
				}
			}
			attributeSelections.removeAll(newAttributeSelections);
			activitiesRoot.getChildren().add(newActivity);
			setEditingActivity(activitiesRoot);
		}
	}

	private void saveEditingActivity(TreeDataActivity treeDataActivity) {
		if (validateActivity(treeDataActivity)) {
			treeDataActivity.setActivityName(activityNameTextField.getText());
			for (int i = attributeSelections.size()-1; i >= 0; i--) {
				if (!attributeSelections.get(i).getIsSelected()) {
					treeDataActivity.removeAttribute(attributeSelections.get(i).getTreeDataAttribute());
				}
			}

			for (int i = attributeSelections.size()-1; i >= 0; i--) {
				if (attributeSelections.get(i).getIsSelected()) {
					treeDataActivity.addAttribute(attributeSelections.get(i).getTreeDataAttribute());
				}
			}

			attributeSelections.removeAll(newAttributeSelections);
		}
	}

	private boolean validateActivity(TreeDataActivity currentTreeDataActivity) {
		boolean isValid = true;
		if (!ValidationUtils.validateMandatory(activityNameTextField.getText())) {
			activityNameTextField.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			activityNameTextField.requestFocus();
			isValid = false;
		} else {
			for (TreeItem<TreeDataBase> treeItem : activitiesRoot.getChildren()) {
				TreeDataActivity treeDataActivity = (TreeDataActivity) treeItem;
				if (Objects.equals(currentTreeDataActivity, treeDataActivity)) {
					continue;
				}
				if (treeDataActivity.getActivityName().equals(activityNameTextField.getText())) {
					activityNameTextField.pseudoClassStateChanged(ValidationUtils.errorClass, true);
					activityNameTextField.requestFocus();
					isValid = false;
				}
			}
		}

		return isValid;
	}

}
