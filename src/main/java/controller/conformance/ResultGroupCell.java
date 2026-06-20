package controller.conformance;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.extension.std.XConceptExtension;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import task.conformance.ActivityConformanceType;
import task.conformance.ActivityConformanceType.Type;
import task.conformance.ConformanceStatisticType;
import task.conformance.ConformanceTaskResultDetail;
import task.conformance.ConformanceTaskResultGroup;

public class ResultGroupCell extends ListCell<ConformanceTaskResultGroup> {

	//Most of logging commented out because cells can be updated very often and at arbitrary times
	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@FXML
	private TitledPane rootPane;

	@FXML
	private VBox titleVbox;
	@FXML
	private Label nameLabel;
	@FXML
	private HBox statisticsHbox;
	@FXML
	private TableView<ConformanceTaskResultDetail> detailsTableView;
	@FXML
	private TableColumn<ConformanceTaskResultDetail, String> detailName;
	@FXML
	private TableColumn<ConformanceTaskResultDetail, List<ActivityConformanceType>> detailContents;

	private FXMLLoader loader;
	private String detailNameAttribute;
	private double detailNameWidth;
	private Double detailContentsWidth; //If null then column width will be set based on first detail row
	private int numberOfDetails;
	private boolean isAlignment;

	private ConformanceTabController conformanceTabController;

	private ConformanceTaskResultGroup previousItem;

	//TODO: Find a better way of updating the trace details view
	public ResultGroupCell(String detailNameAttribute, double detailNameWidth, Double detailContentsWidth, int numberOfDetails, ConformanceTabController conformanceTabController, boolean isAlignment) {
		this.detailNameAttribute = detailNameAttribute;
		this.detailNameWidth = detailNameWidth;
		this.detailContentsWidth = detailContentsWidth;
		if (isAlignment) {
			this.numberOfDetails = numberOfDetails + 1; //Accounts for the combined trace row
		} else {
			this.numberOfDetails = numberOfDetails;
		}
		this.conformanceTabController = conformanceTabController;
		this.isAlignment = isAlignment;
	}

	@FXML
	private void initialize() {
		detailName.setCellValueFactory(new PropertyValueFactory<ConformanceTaskResultDetail, String>(detailNameAttribute));
		detailName.setReorderable(false);
		detailName.setMinWidth(detailNameWidth);
		detailName.setStyle( "-fx-alignment: CENTER-LEFT;");

		detailContents.setCellValueFactory(new PropertyValueFactory<ConformanceTaskResultDetail, List<ActivityConformanceType>>("activityConformanceTypes"));

		detailContents.setCellFactory(value -> new ResultDetailCell(detailNameAttribute.equals("traceName")));
		detailContents.setReorderable(false);
		if (detailContentsWidth != null) {
			detailContents.setMinWidth(detailContentsWidth);
		}

		//Needed for the title pane header graphic to fill all available space
		titleVbox.minWidthProperty().bind(rootPane.widthProperty().subtract(25d));

		if (detailContentsWidth == null) {
			if (numberOfDetails > 13) {
				detailsTableView.setPrefHeight(11 * 40d + 10d);
			} else {
				detailsTableView.setPrefHeight(numberOfDetails * 40d + 10d);
			}
		} else {
			if (numberOfDetails > 24) {
				detailsTableView.setPrefHeight(20 * 22d + 10d);
			} else {
				detailsTableView.setPrefHeight(numberOfDetails * 22d + 10d);
			}
		}

		//https://stackoverflow.com/questions/37130122/javafx-how-to-limit-cell-width-to-the-width-of-the-listview
		setPrefWidth(0);

		detailsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
			if (newItem != null) {
				conformanceTabController.setCurrentResultDetail(newItem, detailsTableView);
				this.getListView().getSelectionModel().clearAndSelect(this.getIndex());
			}
		});

		//logger.debug("Result group cell initialized");
	}

	@Override
	protected void updateItem(ConformanceTaskResultGroup item, boolean empty) {
		//https://openjfx.io/javadoc/11/javafx.controls/javafx/scene/control/Cell.html#updateItem(T,boolean)
		super.updateItem(item, empty);
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
			this.previousItem = item;
		} else if (this.previousItem != item) { //Prevents redrawing the cell if the item has not changed (redrawing causes the ListView to visibly "jump" when a different ListCell is selected)
			this.previousItem = item;
			if (loadFxml()) {
				//logger.debug("Updating result group cell to item: " + item.toString());
				nameLabel.setText(item.getGroupStatistics().get(ConformanceStatisticType.NAME));

				statisticsHbox.getChildren().clear();
				for (ConformanceStatisticType statisticType : item.getGroupStatistics().keySet()) {
					if (statisticType != ConformanceStatisticType.NAME) {
						statisticsHbox.getChildren().add(new Label(statisticType.getDisplayText() + ": " + item.getGroupStatistics().get(statisticType)));
					}
				}

				if (detailContentsWidth == null) {
					detailContents.setMinWidth(item.getGroupDetails().get(0).getXtrace().size() * 13d);
					detailsTableView.setFixedCellSize(40d);
				} else {
					detailsTableView.setFixedCellSize(22d);
				}
				
				for (ConformanceTaskResultDetail detail : item.getGroupDetails())
					for (int i=0; i<detail.getXtrace().size(); i++)
						detail.getActivityConformanceTypes().get(i).setTooltipText( XConceptExtension.instance().extractName(detail.getXtrace().get(i)) );
				
				detailsTableView.getItems().setAll(item.getGroupDetails());

				if (isAlignment && item.getXtrace() != null) {
					addCombinedTrace(item); //Adds a trace that includes all insertions and deletions from all constraints
				}

				rootPane.setExpanded(item.getIsExpanded());
				rootPane.expandedProperty().addListener((obsVal, oldVal, newVal) -> {
					this.getListView().getItems().get(getIndex()).setIsExpanded(newVal);
				});

				setText(null);
				setGraphic(rootPane);
				//logger.debug("Updated result group cell to item: " + item.toString());
			}
		}
	}

	private void addCombinedTrace(ConformanceTaskResultGroup item) {
		//TODO: Combined trace is sometimes not shown for some reason
		//logger.debug("Adding combined trace to item: " + item.toString());
		ConformanceTaskResultDetail combinedTraceDetail = new ConformanceTaskResultDetail();
		combinedTraceDetail.setConstraint("All constraints");
		combinedTraceDetail.setXtrace(item.getXtrace());
		combinedTraceDetail.setTraceName(item.getXtrace().getAttributes().get("concept:name") != null ? item.getXtrace().getAttributes().get("concept:name").toString() : "");

		List<ActivityConformanceType> combinedConformanceTypes = new ArrayList<ActivityConformanceType>();
		for (ActivityConformanceType activityConformanceType : item.getGroupDetails().get(0).getActivityConformanceTypes()) {
			//Currently each detail has all insertions and deletions from all constraints, some are just labelt other and some are not
			if (activityConformanceType.getType() == ActivityConformanceType.Type.INSERTION_OTHER) {
				combinedConformanceTypes.add(new ActivityConformanceType(Type.INSERTION));
			} else if (activityConformanceType.getType() == ActivityConformanceType.Type.DELETION_OTHER) {
				combinedConformanceTypes.add(new ActivityConformanceType(Type.DELETION));
			} else {
				combinedConformanceTypes.add(activityConformanceType);
			}
		}
		combinedTraceDetail.setActivityConformanceTypes(combinedConformanceTypes);

		for (int i=0; i<combinedTraceDetail.getXtrace().size(); i++)
			combinedTraceDetail.getActivityConformanceTypes().get(i).setTooltipText(combinedTraceDetail.getXtrace().get(i).getAttributes().get(XConceptExtension.KEY_NAME).toString());
		
		detailsTableView.getItems().add(0, combinedTraceDetail);
		//logger.debug("Added combined trace to item: " + item.toString());
	}

	private boolean loadFxml() {
		if (loader == null) {
			//Load cell contents if not already loaded
			loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/conformance/ResultGroupCell.fxml"));
			loader.setController(this);
			try {
				loader.load();
				return true;
			} catch (IOException | IllegalStateException e) {
				logger.error("Can not load result group cell", e);
				return false;
			}
		} else {
			return true;
		}
	}
}
