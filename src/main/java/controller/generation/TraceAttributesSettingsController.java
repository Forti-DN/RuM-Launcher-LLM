package controller.generation;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import controller.common.AbstractController;
import controller.editor.AttributeType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import treedata.TreeDataAttribute;
import treedata.TreeDataBase;
import util.AlertUtils;
import util.ValidationUtils;

public class TraceAttributesSettingsController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	@FXML
	private VBox traceAttributeVbox;
	@FXML
	private Label titleLabel;
	@FXML
	private Button deleteButton;
	@FXML
	private TextField attNameTextField;
	@FXML
	private ChoiceBox<AttributeType> attTypeChoiceBox;
	@FXML
	private Label valueRangeLabel;
	@FXML
	private TextField attValueFrom;
	@FXML
	private TextField attValueTo;
	@FXML
	private HBox valueRangeHBox;
	@FXML
	private Label possibleValuesLabel;
	@FXML
	private VBox possibleValuesVBox;
	@FXML
	private TextField attPossibleValues;
	@FXML
	private Button saveAttributeButton;
	@FXML
	private Button closeButton;
	
	private GenerationTabController controller;
	private TreeDataBase attributesRoot;
	
	@FXML
	private void initialize() {
		attTypeChoiceBox.getItems().setAll(AttributeType.values());
		attTypeChoiceBox.setConverter(new StringConverter<AttributeType>() {
			@Override
			public String toString(AttributeType attributeType) {
				return attributeType.getDisplayText();
			}
			@Override
			public AttributeType fromString(String string) {
				return null;
			}
		});

		attTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
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
				ValidationUtils.addMandatoryIntegerBehavior(attValueFrom, attValueTo);
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
				ValidationUtils.addMandatoryDecimalBehavior(attValueFrom, attValueTo);
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
				ValidationUtils.addMandatoryBehavior(attPossibleValues);
				break;
			default:
				//TODO: Disable attribute editing and show error to user
				logger.error("Unhandled attribute type selected: {}", newV);
				break;
			}
		});
		
		attTypeChoiceBox.getSelectionModel().selectFirst();
		
		// Avoids closing the layer when user clicks on an empty area of the settings region
		traceAttributeVbox.setOnMouseClicked(event -> event.consume());
		
		logger.debug("Trace attributes generation panel initialized.");
	}
	
	public void setController(GenerationTabController controller) {
		this.controller = controller;
	}
	
	public void setAttributesRoot(TreeDataBase attributesRoot) {
		this.attributesRoot = attributesRoot;
	}

	public void setEditingAttribute(TreeDataBase currentlyEditingTreeData) {
		if (currentlyEditingTreeData == attributesRoot) {
			// Update panel for creating a new attribute
			titleLabel.setText("Add attribute");
			
			deleteButton.setVisible(false);
			deleteButton.setManaged(false);
			deleteButton.setDisable(true);
			
			attNameTextField.setText("");
			attTypeChoiceBox.getSelectionModel().selectFirst();
			attValueFrom.setText("");
			attValueTo.setText("");
			attPossibleValues.setText("");

			FontIcon plusIcon = new FontIcon("fa-plus");
			plusIcon.getStyleClass().add("cost-model__add-icon");
			saveAttributeButton.setText("Add attribute");
			saveAttributeButton.setGraphic(plusIcon);
			saveAttributeButton.setOnAction(event -> saveEditingAttribute(null));
			
		} else if (currentlyEditingTreeData instanceof TreeDataAttribute) {
			// Update panel for editing an existing attribute
			TreeDataAttribute treeDataAttribute = (TreeDataAttribute) currentlyEditingTreeData;
			titleLabel.setText("Editing \"" + treeDataAttribute.getAttributeName() + "\" attribute");
			
			deleteButton.setVisible(true);
			deleteButton.setManaged(true);
			deleteButton.setDisable(false);
			deleteButton.setOnAction(event -> controller.deleteTreeDataItem(treeDataAttribute));
			
			attNameTextField.setText(treeDataAttribute.getAttributeName());
			attTypeChoiceBox.getSelectionModel().select(treeDataAttribute.getAttributeType());
			switch (treeDataAttribute.getAttributeType()) {
			case INTEGER: //Fall through intended
			case FLOAT:
				attValueFrom.setText(treeDataAttribute.getValueFrom().toString());
				attValueTo.setText(treeDataAttribute.getValueTo().toString());
				break;
				
			case ENUMERATION:
				attPossibleValues.setText(String.join(", ", treeDataAttribute.getPossibleValues()));
				break;
			}

			FontIcon checkIcon = new FontIcon("fa-check");
			checkIcon.getStyleClass().add("cost-model__add-icon");
			saveAttributeButton.setText("Save");
			saveAttributeButton.setGraphic(checkIcon);
			saveAttributeButton.setOnAction(event -> saveEditingAttribute(treeDataAttribute));

		} else {
			AlertUtils.showError("Failed setting item to edit!"); // Users should never really see this error
			logger.error("Can not use attribute editing panel for editing activities");
		}
	}
	
	private void saveEditingAttribute(TreeDataAttribute treeDataAttribute) {
		if (validateAttribute(treeDataAttribute)) {
			
			boolean isNewAttribute = false;
			if (treeDataAttribute == null) {
				treeDataAttribute = new TreeDataAttribute();
				isNewAttribute = true;
			}
			
			treeDataAttribute.setAttributeName(attNameTextField.getText());
			treeDataAttribute.setAttributeType(attTypeChoiceBox.getSelectionModel().getSelectedItem());
			switch (attTypeChoiceBox.getSelectionModel().getSelectedItem()) {
			case INTEGER: //Fall through intended
			case FLOAT:
				treeDataAttribute.setValueFrom(new BigDecimal(attValueFrom.getText()));
				treeDataAttribute.setValueTo(new BigDecimal(attValueTo.getText()));
				break;
				
			case ENUMERATION:
				List<String> possibleValues = new ArrayList<>();
				for (String possibleValue : attPossibleValues.getText().split(","))
					possibleValues.add(possibleValue.strip());
				
				treeDataAttribute.setPossibleValues(possibleValues);
				break;
			}
			
			if (isNewAttribute) {
				attributesRoot.getChildren().add(treeDataAttribute);
				setEditingAttribute(attributesRoot);
			}
		}
	}

	//TODO: Remove code duplication (same code is in ActivityEditingPanel)
	private boolean validateAttribute(TreeDataAttribute treeDataAttribute) {
		boolean isValid = true;

		if (!ValidationUtils.validateMandatory(attNameTextField.getText())) {
			attNameTextField.pseudoClassStateChanged(ValidationUtils.errorClass, true);
			attNameTextField.requestFocus();
			isValid = false;
		
		} else {
			for (TreeItem<TreeDataBase> treeItem : attributesRoot.getChildren()) {
				TreeDataAttribute existingAttribute = (TreeDataAttribute) treeItem;
				
				if (Objects.equals(treeDataAttribute, existingAttribute))
					continue;
				
				if (existingAttribute.getAttributeName().equals(attNameTextField.getText())) {
					attNameTextField.pseudoClassStateChanged(ValidationUtils.errorClass, true);
					attNameTextField.requestFocus();
					isValid = false;
				}
			}
		}

		switch (attTypeChoiceBox.getSelectionModel().getSelectedItem()) {
		case INTEGER:
			try {
				Integer.parseInt(attValueFrom.getText());
			} catch (NumberFormatException e) {
				attValueFrom.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				attValueFrom.requestFocus();
				isValid = false;
			}
			
			try {
				Integer.parseInt(attValueTo.getText());
			} catch (NumberFormatException e) {
				attValueTo.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				attValueTo.requestFocus();
				isValid = false;
			}
			
			if (isValid && Integer.parseInt(attValueFrom.getText()) > Integer.parseInt(attValueTo.getText())) {
				attValueFrom.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				attValueTo.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				attValueFrom.requestFocus();
				isValid = false;
			}
			break;
		
		case FLOAT:
			try {
				Float.parseFloat(attValueFrom.getText());
			} catch (NumberFormatException e) {
				attValueFrom.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				attValueFrom.requestFocus();
				isValid = false;
			}
			
			try {
				Float.parseFloat(attValueTo.getText());
			} catch (NumberFormatException e) {
				attValueTo.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				attValueTo.requestFocus();
				isValid = false;
			}
			
			if (isValid && Float.parseFloat(attValueFrom.getText()) > Float.parseFloat(attValueTo.getText())) {
				attValueFrom.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				attValueTo.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				attValueFrom.requestFocus();
				isValid = false;
			}
			break;
		
		case ENUMERATION:
			if (!ValidationUtils.validateMandatory(attPossibleValues.getText())) {
				attPossibleValues.pseudoClassStateChanged(ValidationUtils.errorClass, true);
				attPossibleValues.requestFocus();
				isValid = false;
			}
			break;
		}

		return isValid;
	}
	
	public Button getCloseButton() {
		return closeButton;
	}
}
