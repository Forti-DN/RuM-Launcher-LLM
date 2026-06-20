package controller.monitoring;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.HBox;
import treedata.TreeDataBase;

public class MetaconstraintCell extends TreeCell<TreeDataBase> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private HBox rootRegion;
	@FXML
	private Label displayTextLabel;
	@FXML
	private Button editButton;
	@FXML
	private FontIcon editFontIcon;
	@FXML
	private Separator buttonsSeparator;
	@FXML
	private Button deleteButton;
	@FXML
	private FontIcon deleteFontIcon;
	
	private FXMLLoader loader;
	private MonitoringTabController monitoringTabController;

	public MetaconstraintCell(MonitoringTabController monitoringTabController) {
		this.monitoringTabController = monitoringTabController;
	}

	@Override
	protected void updateItem(TreeDataBase item, boolean empty) {
		super.updateItem(item, empty);
		setText(null);

		if (empty || item == null) {
			setGraphic(null);
		} else if(loadFxml()) {
			//logger.debug("Updating tree data cell to item: " + item.toString());
			displayTextLabel.setText(item.getDisplayText());
			editButton.setOnAction(event -> monitoringTabController.startTreeDataItemEdit(this.getItem()));
			deleteButton.setOnAction(event -> monitoringTabController.deleteTreeDataItem(this.getItem()));
			if (this.getItem().getIsEditing()) {
				editFontIcon.setIconLiteral("fa-angle-double-right");
				editButton.setText("Editing");
				editButton.setMinWidth(70d);
			} else {
				editFontIcon.setIconLiteral("fa-pencil");
				editButton.setText(null);
				editButton.setMinWidth(30d);
			}

			rootRegion.setStyle("-fx-border-color: #ededed; -fx-border-width: 0 0 1 0");
			rootRegion.setMaxWidth(244d);
			buttonsSeparator.setVisible(true);
			deleteButton.setVisible(true);
			
			setGraphic(rootRegion);
			//logger.debug("Updated tree data cell to item: {}", item.toString());
		}
	}
	
	private boolean loadFxml() {
		if (loader == null) {
			//Load TreeDataCell contents if not already loaded
			loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/editor/TreeDataCell.fxml"));
			loader.setController(this);
			try {
				loader.load();
				return true;
			} catch (IOException | IllegalStateException e) {
				logger.error("Can not load tree data cell", e);
				return false;
			}
		} else {
			return true;
		}
	}
}
