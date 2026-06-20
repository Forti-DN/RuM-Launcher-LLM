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
import controller.editor.data.ActivitySelectionData;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import treedata.TreeDataActivity;
import treedata.TreeDataAttribute;
import treedata.TreeDataBase;
import util.AlertUtils;
import util.ValidationUtils;

public class AttributeEditingPanelController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox attributeEditingVbox;
	@FXML
	private Label titleLabel;
	@FXML
	private Button deleteButton;
	@FXML
	private GridPane attributeDataGridPane;
	@FXML
	private TextField attributeNameTextField;
	@FXML
	private ChoiceBox<AttributeType> newAttributeType;
	@FXML
	private TextField newAttributeValueFrom;
	@FXML
	private TextField newAttributeValueTo;
	@FXML
	private Label valueRangeLabel;
	@FXML
	private HBox valueRangeHBox;
	@FXML
	private Label possibleValuesLabel;
	@FXML
	private VBox possibleValuesVBox;
	@FXML
	private TextField newAttributePossibleValues;
	@FXML
	private ListView<ActivitySelectionData> activitySelectionsListView;
	@FXML
	private Label noActivitiesSelectedError;
	/*
	@FXML
	private Button newActivityButton;
	*/
	@FXML
	private VBox newActivityVBox;
	@FXML
	private TextField newActivityName;
	@FXML
	private Button saveAttributeButton;
	@FXML
	private FontIcon saveAttributeFontIcon;
	@FXML
	private Button closeButton;

	private EditorTabController editorTabController;
	private TreeDataBase activitiesRoot;
	private TreeDataAttribute currentlyEditingAttribute;
	private ObservableList<ActivitySelectionData> activitySelections = FXCollections.observableArrayList();
	private List<ActivitySelectionData> newActivitySelections = new ArrayList<ActivitySelectionData>();

	private TreeDataBase attributesDummyRoot;

	@FXML
	private void initialize() {
		newActivityVBox.setVisible(false);
		newActivityVBox.setManaged(false);

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

		StringConverter<ActivitySelectionData> templateConverter = new StringConverter<ActivitySelectionData>() {
			@Override
			public String toString(ActivitySelectionData object) {
				return object != null ? object.getTreeDataActivity().getDisplayText() : "";
			}
			@Override
			public ActivitySelectionData fromString(String string) {
				return null;
			}
		};
		activitySelectionsListView.setItems(activitySelections);
		activitySelectionsListView.setCellFactory(CheckBoxListCell.forListView(ActivitySelectionData::isSelectedProperty, templateConverter));
		activitySelectionsListView.setFixedCellSize(30d);
		activitySelections.addListener((ListChangeListener<ActivitySelectionData>)(change -> {
			activitySelectionsListView.setPrefHeight(activitySelectionsListView.getItems().size() * 30d + 20d);
		}));

		activitySelectionsListView.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == true) {
				noActivitiesSelectedError.setVisible(false);
				noActivitiesSelectedError.setManaged(false);
			}
		});

		ValidationUtils.addMandatoryBehavior(attributeNameTextField, newActivityName);
		noActivitiesSelectedError.setVisible(false);
		noActivitiesSelectedError.setManaged(false);
		
		attributeEditingVbox.setOnMouseClicked(event -> {
			event.consume(); //Avoids closing the layer when user clicks on an empty area of the editing region
		});

		logger.debug("Attribute editing panel initialized");
	}

	@FXML
	private void addNewActivity() {
		setNewActivityMode(true);
		newActivityName.setText("");
	}

	@FXML
	private void newActivityDone() {
		if (validateNewActivity()) {
			TreeDataActivity treeDataActivity = new TreeDataActivity();
			treeDataActivity.setActivityName(newActivityName.getText());

			ActivitySelectionData newActivitySelectionData = new ActivitySelectionData(treeDataActivity, true, true);
			activitySelections.add(newActivitySelectionData);
			newActivitySelections.add(newActivitySelectionData);

			setNewActivityMode(false);
			newActivityName.setText("");
		}
	}

	@FXML
	private void newActivityCancel() {
		setNewActivityMode(false);
		newActivityName.setText("");
	}

	private boolean validateNewActivity() {
		boolean isValid = true;

		if (!ValidationUtils.validateMandatory(newActivityName.getText())) {
			newActivityName.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			newActivityName.requestFocus();
			isValid = false;
		} else {
			for (ActivitySelectionData activitySelectionData : activitySelections) {
				if (activitySelectionData.getTreeDataActivity().getActivityName().equals(newActivityName.getText())) {
					newActivityName.pseudoClassStateChanged(ValidationUtils.errorClass, true);
					newActivityName.requestFocus();
					isValid = false;
				}
			}
		}

		return isValid;
	}

	private void setNewActivityMode(boolean enabled) {
		attributeDataGridPane.setDisable(enabled);
		//newActivityButton.setVisible(!enabled);
		//newActivityButton.setManaged(!enabled);
		newActivityVBox.setVisible(enabled);
		newActivityVBox.setManaged(enabled);
		saveAttributeButton.setDisable(enabled);
		closeButton.setDisable(enabled);
	}

	public Button getCloseButton() {
		return closeButton;
	}

	public void setEditorTabController(EditorTabController editorTabController) {
		this.editorTabController = editorTabController;
	}

	public void connectToActivitiesRoot(TreeDataBase activitiesRoot) {
		this.activitiesRoot = activitiesRoot;

		activitiesRoot.addEventHandler(TreeItem.childrenModificationEvent(), new EventHandler<TreeItem.TreeModificationEvent<TreeDataBase>>() {
			@Override
			public void handle(TreeModificationEvent<TreeDataBase> event) {
				for (TreeItem<TreeDataBase> treeItem : event.getAddedChildren()) {
					if (treeItem instanceof TreeDataActivity) {
						TreeDataActivity treeDataActivity = (TreeDataActivity) treeItem;
						if (currentlyEditingAttribute != null && currentlyEditingAttribute.getActivitiesUnmodifiable().contains(treeDataActivity)) {
							activitySelections.add(new ActivitySelectionData(treeDataActivity, true, false));
						} else {
							activitySelections.add(new ActivitySelectionData(treeDataActivity, false, false));
						}
					}
				}

				for (TreeItem<TreeDataBase> treeItem : event.getRemovedChildren()) {
					if (treeItem instanceof TreeDataActivity) {
						TreeDataActivity treeDataActivity = (TreeDataActivity) treeItem;
						for (int i = activitySelections.size()-1; i >= 0; i--) {
							if (activitySelections.get(i).getTreeDataActivity().equals(treeDataActivity)) {
								activitySelections.remove(i);
							}
						}
					}
				}
			}
		});
	}

	public void setAttributesDummyRoot(TreeDataBase attributesDummyRoot) {
		this.attributesDummyRoot = attributesDummyRoot;
	}

	public void setEditingAttribute(TreeDataBase currentlyEditingTreeData) {
		if (currentlyEditingTreeData == attributesDummyRoot) {
			//Update panel for creating a new attribute
			titleLabel.setText("Add attribute");
			deleteButton.setVisible(false);
			deleteButton.setManaged(false);
			deleteButton.setDisable(true);
			attributeNameTextField.setText("");
			newAttributeType.getSelectionModel().selectFirst();
			newAttributeValueFrom.setText("");
			newAttributeValueTo.setText("");
			newAttributePossibleValues.setText("");

			saveAttributeButton.setText("Add attribute");
			saveAttributeFontIcon.setIconLiteral("fa-plus");
			saveAttributeButton.setOnAction(event -> saveEditingAttribute(null));

			//Clears activity checkboxes
			for (int i = activitySelections.size()-1; i >= 0; i--) {
				if (activitySelections.get(i).getIsNew()) {
					activitySelections.remove(i);
				} else {
					activitySelections.get(i).setIsSelected(false);
				}
			}
		} else if (currentlyEditingTreeData instanceof TreeDataAttribute) {
			//Update panel for editing an existing attribute
			TreeDataAttribute treeDataAttribute = (TreeDataAttribute) currentlyEditingTreeData;
			titleLabel.setText("Editing \"" + treeDataAttribute.getAttributeName() + "\" attribute");
			deleteButton.setVisible(true);
			deleteButton.setManaged(true);
			deleteButton.setDisable(false);
			deleteButton.setOnAction(event -> {
				for (int i = treeDataAttribute.getActivitiesUnmodifiable().size()-1; i >= 0; i--) {
					treeDataAttribute.removeActivity(treeDataAttribute.getActivitiesUnmodifiable().get(i));
				}
				editorTabController.startTreeDataItemEdit(attributesDummyRoot);
			});
			attributeNameTextField.setText(treeDataAttribute.getAttributeName());
			newAttributeType.getSelectionModel().select(treeDataAttribute.getAttributeType());
			switch (treeDataAttribute.getAttributeType()) {
			case INTEGER: //Fall through intended
			case FLOAT:
				newAttributeValueFrom.setText(treeDataAttribute.getValueFrom().toString());
				newAttributeValueTo.setText(treeDataAttribute.getValueTo().toString());
				break;
			case ENUMERATION:
				newAttributePossibleValues.setText(String.join(", ", treeDataAttribute.getPossibleValues()));
				break;
			default:
				logger.error("Unhandled attribute type for editing: {}", newAttributeType.getSelectionModel().getSelectedItem());
				AlertUtils.showWarning("Unhandled attribute type (" + newAttributeType.getSelectionModel().getSelectedItem() + "), attribute may be saved incorrectly!");
				break;
			}

			saveAttributeButton.setText("Save");
			saveAttributeFontIcon.setIconLiteral("fa-check");
			saveAttributeButton.setOnAction(event -> saveEditingAttribute(treeDataAttribute));

			//Clears activity checkboxes
			for (int i = activitySelections.size()-1; i >= 0; i--) {
				if (activitySelections.get(i).getIsNew()) {
					activitySelections.remove(i);
				} else if (treeDataAttribute.getActivitiesUnmodifiable().contains(activitySelections.get(i).getTreeDataActivity())) {
					activitySelections.get(i).setIsSelected(true);
				} else {
					activitySelections.get(i).setIsSelected(false);
				}
			}
		}
		else {
			AlertUtils.showError("Failed setting item to edit!"); //Users should never really see this error
			logger.error("Can not use attribute editing panel for editing activities");
		}
	}

	private void saveEditingAttribute(TreeDataAttribute treeDataAttribute) {
		if (validateAttribute(treeDataAttribute)) {
			boolean wasNew = false;
			if (treeDataAttribute == null) {
				treeDataAttribute = new TreeDataAttribute();
				wasNew = true;
			}
			treeDataAttribute.setAttributeName(attributeNameTextField.getText());
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

			List<TreeDataActivity> newActivities = new ArrayList<TreeDataActivity>();
			for (ActivitySelectionData activitySelectionData : activitySelections) {
				if (activitySelectionData.getIsSelected()) {
					if (newActivitySelections.contains(activitySelectionData)) {
						activitySelectionData.getTreeDataActivity().addAttribute(treeDataAttribute);
						newActivities.add(activitySelectionData.getTreeDataActivity());
					} else {
						treeDataAttribute.addActivity(activitySelectionData.getTreeDataActivity());
					}
				}
			}

			for (TreeDataActivity newActivity : newActivities) {
				activitiesRoot.getChildren().add(newActivity);
			}

			if (!wasNew) {
				for (int i = activitySelections.size()-1; i >= 0; i--) {
					if (!activitySelections.get(i).getIsSelected()) {
						treeDataAttribute.removeActivity(activitySelections.get(i).getTreeDataActivity());
					}
				}
			}

			for (ActivitySelectionData activitySelectionData : newActivitySelections) {
				activitySelections.remove(activitySelectionData);
			}

			if (wasNew) {
				setEditingAttribute(attributesDummyRoot);
			}
		}
	}

	//TODO: Remove code duplication (same code is in ActivityEditingPanel)
	private boolean validateAttribute(TreeDataAttribute treeDataAttribute) {
		boolean isValid = true;

		if (!ValidationUtils.validateMandatory(attributeNameTextField.getText())) {
			attributeNameTextField.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			attributeNameTextField.requestFocus();
			isValid = false;
		} else {
			for (TreeItem<TreeDataBase> treeItem : activitiesRoot.getChildren()) {
				TreeDataActivity treeDataActivity = (TreeDataActivity) treeItem;
				for (TreeDataAttribute existingAttribute : treeDataActivity.getAttributesUnmodifiable()) {
					if (Objects.equals(treeDataAttribute, existingAttribute)) {
						continue;
					}
					if (existingAttribute.getAttributeName().equals(attributeNameTextField.getText())) {
						attributeNameTextField.pseudoClassStateChanged(ValidationUtils.errorClass, true);
						attributeNameTextField.requestFocus();
						isValid = false;
					}
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

		boolean activitySelected = false;
		for (ActivitySelectionData activitySelectionData : activitySelections) {
			if (activitySelectionData.getIsSelected()) {
				activitySelected = true;
				break;
			}
		}

		if (!activitySelected) {
			isValid = false;
		}

		noActivitiesSelectedError.setVisible(!activitySelected);
		noActivitiesSelectedError.setManaged(!activitySelected);

		return isValid;
	}


}
