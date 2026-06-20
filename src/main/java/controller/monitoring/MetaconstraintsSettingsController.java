package controller.monitoring;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import controller.common.AbstractController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import rum.algorithms.mobuconldl.MonitoringState;
import treedata.TreeDataBase;
import treedata.TreeDataMetaconstraint;

public class MetaconstraintsSettingsController extends AbstractController {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private VBox metaconstraintVbox;
	@FXML
	private Label titleLabel;
	@FXML
	private Button deleteButton;
	@FXML
	private ChoiceBox<MetaconstraintTemplate> templateChoiceBox;
	@FXML
	private ChoiceBox<String> firstConstraintChoiceBox;
	@FXML
	private Label constraintStatusLabel;
	@FXML
	private Label secondConstraintLabel;
	@FXML
	private ChoiceBox<MonitoringState> constraintStatusChoiceBox;
	@FXML
	private ChoiceBox<String> secondConstraintChoiceBox;
	@FXML
	private Label activityLabel;
	@FXML
	private ChoiceBox<String> activityChoiceBox;
	@FXML
	private Label metaconstraintTextLabel;
	@FXML
	private Button saveMetaconstraintButton;
	@FXML
	private Button closeButton;

	private MonitoringTabController controller;
	private TreeDataBase metaconstraintsRoot;

	@FXML
	private void initialize() {
		// Avoids closing the layer when user clicks on an empty area of the settings region
		metaconstraintVbox.setOnMouseClicked(event -> event.consume());

		templateChoiceBox.getItems().addAll(MetaconstraintTemplate.values());
		templateChoiceBox.setConverter(new StringConverter<MetaconstraintTemplate>() {
			@Override
			public String toString(MetaconstraintTemplate metaconstraintTemplate) {
				return metaconstraintTemplate.getDisplayText();
			}
			@Override
			public MetaconstraintTemplate fromString(String string) {
				return null;
			}
		});

		templateChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			switch (newV) {
			case CONTEXTUAL_ABSENCE:
				constraintStatusChoiceBox.setVisible(true);
				constraintStatusChoiceBox.setManaged(true);
				constraintStatusLabel.setVisible(true);
				constraintStatusLabel.setManaged(true);
				activityChoiceBox.setVisible(true);
				activityChoiceBox.setManaged(true);
				activityLabel.setVisible(true);
				activityLabel.setManaged(true);
				secondConstraintChoiceBox.setVisible(false);
				secondConstraintChoiceBox.setManaged(false);
				secondConstraintLabel.setVisible(false);
				secondConstraintLabel.setManaged(false);
				break;
			case REACTIVE_COMPENSATION: //Fallthrough intended
			case COMPENSATION:
				constraintStatusChoiceBox.setVisible(false);
				constraintStatusChoiceBox.setManaged(false);
				constraintStatusLabel.setVisible(false);
				constraintStatusLabel.setManaged(false);
				activityChoiceBox.setVisible(false);
				activityChoiceBox.setManaged(false);
				activityLabel.setVisible(false);
				activityLabel.setManaged(false);
				secondConstraintChoiceBox.setVisible(true);
				secondConstraintChoiceBox.setManaged(true);
				secondConstraintLabel.setVisible(true);
				secondConstraintLabel.setManaged(true);
				break;
			default:
				logger.error("Unsupported metaconstraint template selected: {}", newV);
				break;
			}
		});
		
		templateChoiceBox.getSelectionModel().selectFirst();


		constraintStatusChoiceBox.getItems().addAll(MonitoringState.SAT, MonitoringState.POSS_SAT, MonitoringState.POSS_VIOL, MonitoringState.VIOL);
		constraintStatusChoiceBox.setConverter(new StringConverter<MonitoringState>() {
			@Override
			public String toString(MonitoringState monitoringState) {
				return monitoringState.getMobuconltlName();
			}
			@Override
			public MonitoringState fromString(String string) {
				return null;
			}
		});
		constraintStatusChoiceBox.getSelectionModel().selectFirst();
		
		templateChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			updateMetaconstraintText();
		});
		constraintStatusChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			updateMetaconstraintText();
		});

		logger.debug("Trace attributes generation panel initialized.");
	}

	public void setController(MonitoringTabController controller) {
		this.controller = controller;
	}

	public void setAttributesRoot(TreeDataBase metaconstraintsRoot) {
		this.metaconstraintsRoot = metaconstraintsRoot;
	}


	public void setEditingAttribute(TreeDataBase currentlyEditingTreeData) {
		if (currentlyEditingTreeData == metaconstraintsRoot) {
			// Update panel for creating a new attribute
			titleLabel.setText("Add metaconstraint");

			deleteButton.setVisible(false);
			deleteButton.setManaged(false);
			deleteButton.setDisable(true);

			FontIcon plusIcon = new FontIcon("fa-plus");
			plusIcon.getStyleClass().add("cost-model__add-icon");
			saveMetaconstraintButton.setText("Add metaconstraint");
			saveMetaconstraintButton.setGraphic(plusIcon);
			saveMetaconstraintButton.setOnAction(event -> saveEditingAttribute(null));
		} else {
			// Update panel for editing an existing attribute
			TreeDataMetaconstraint treeDataMetaconstraint = (TreeDataMetaconstraint) currentlyEditingTreeData;
			titleLabel.setText("Editing \"" + treeDataMetaconstraint.getMetaconstraintTemplate().getDisplayText() + "\"");

			deleteButton.setVisible(true);
			deleteButton.setManaged(true);
			deleteButton.setDisable(false);
			deleteButton.setOnAction(event -> controller.deleteTreeDataItem(treeDataMetaconstraint));

			firstConstraintChoiceBox.getSelectionModel().select(treeDataMetaconstraint.getFirstConstraint());
			templateChoiceBox.getSelectionModel().select(treeDataMetaconstraint.getMetaconstraintTemplate());
			constraintStatusChoiceBox.getSelectionModel().select(treeDataMetaconstraint.getConstraintStatus());
			activityChoiceBox.getSelectionModel().select(treeDataMetaconstraint.getActivity());
			secondConstraintChoiceBox.getSelectionModel().select(treeDataMetaconstraint.getSecondConstraint());

			FontIcon checkIcon = new FontIcon("fa-check");
			checkIcon.getStyleClass().add("cost-model__add-icon");
			saveMetaconstraintButton.setText("Save");
			saveMetaconstraintButton.setGraphic(checkIcon);
			saveMetaconstraintButton.setOnAction(event -> saveEditingAttribute(treeDataMetaconstraint));
		}
	}
	
	public void setActivities(List<String> activitiesList) {
		activityChoiceBox.getItems().setAll(activitiesList);
		activityChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			updateMetaconstraintText();
		});
		activityChoiceBox.getSelectionModel().selectFirst();
	}

	public void setConstraints(List<String> constraintList) {
		List<String> constraintListWoData = new ArrayList<String>();
		for (String constraint : constraintList) {
			constraintListWoData.add(constraint.split(" \\|")[0]);
		}

		firstConstraintChoiceBox.getItems().setAll(constraintListWoData);
		secondConstraintChoiceBox.getItems().setAll(constraintListWoData);
		
		firstConstraintChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			updateMetaconstraintText();
		});
		secondConstraintChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			updateMetaconstraintText();
		});

		firstConstraintChoiceBox.getSelectionModel().selectFirst();
		secondConstraintChoiceBox.getSelectionModel().selectFirst();
	}
	
	private void updateMetaconstraintText() {
		switch (templateChoiceBox.getSelectionModel().getSelectedItem()) {
		case CONTEXTUAL_ABSENCE:
			metaconstraintTextLabel.setText("Activity '" + activityChoiceBox.getSelectionModel().getSelectedItem() + "' forbidden while constraint '" + firstConstraintChoiceBox.getSelectionModel().getSelectedItem() + "' is '" + constraintStatusChoiceBox.getSelectionModel().getSelectedItem().getMobuconltlName() + "'");
			break;
		case REACTIVE_COMPENSATION:
			metaconstraintTextLabel.setText("Permanent violation of constraint '" + firstConstraintChoiceBox.getSelectionModel().getSelectedItem() + "' is compensated by satisfying '" + secondConstraintChoiceBox.getSelectionModel().getSelectedItem() + "'  after the violation");
			break;
		case COMPENSATION:
			metaconstraintTextLabel.setText("Permanent violation of constraint '" + firstConstraintChoiceBox.getSelectionModel().getSelectedItem() + "' is compensated by satisfying '" + secondConstraintChoiceBox.getSelectionModel().getSelectedItem() + "' anywhere in the trace");
			break;
		default:
			logger.error("Unsupported metaconstraint template selected: {}", templateChoiceBox.getSelectionModel().getSelectedItem());
			break;
		}
	}

	private void saveEditingAttribute(TreeDataMetaconstraint treeDataMetaconstraint) {
		boolean isNewmetaconstraint = false;
		if (treeDataMetaconstraint == null) {
			treeDataMetaconstraint = new TreeDataMetaconstraint();
			isNewmetaconstraint = true;
		}

		treeDataMetaconstraint.setMetaconstraintTemplate(templateChoiceBox.getSelectionModel().getSelectedItem());
		treeDataMetaconstraint.setFirstConstraint(firstConstraintChoiceBox.getSelectionModel().getSelectedItem());
		treeDataMetaconstraint.setConstraintStatus(constraintStatusChoiceBox.getSelectionModel().getSelectedItem());
		treeDataMetaconstraint.setActivity(activityChoiceBox.getSelectionModel().getSelectedItem());
		treeDataMetaconstraint.setSecondConstraint(secondConstraintChoiceBox.getSelectionModel().getSelectedItem());
		treeDataMetaconstraint.setMetaconstraintText(metaconstraintTextLabel.getText());

		if (isNewmetaconstraint) {
			metaconstraintsRoot.getChildren().add(treeDataMetaconstraint);
			setEditingAttribute(metaconstraintsRoot);
		}
		
		treeDataMetaconstraint.updateDisplayText();
	}


	public Button getCloseButton() {
		return closeButton;
	}
}
