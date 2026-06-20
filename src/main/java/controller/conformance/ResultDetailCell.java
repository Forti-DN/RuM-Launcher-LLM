package controller.conformance;

import java.util.List;

import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import task.conformance.ActivityConformanceType;
import task.conformance.ConformanceTaskResultDetail;

public class ResultDetailCell extends TableCell<ConformanceTaskResultDetail, List<ActivityConformanceType>> {

	private boolean hideConformanceTypeOther;

	public ResultDetailCell(boolean hideConformanceTypeOther) {
		this.hideConformanceTypeOther = hideConformanceTypeOther;
	}

	@Override
	protected void updateItem(List<ActivityConformanceType> item, boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
		} else {
			HBox detailContainer = new HBox();
			detailContainer.resize(Double.MAX_VALUE, Double.MAX_VALUE);
			
			HBox detailContainer2 = new HBox();
			detailContainer2.getStyleClass().add("detail-background");
			detailContainer.getChildren().add(detailContainer2);
			switch (this.getTableRow().getItem().getVacuousConformance().getType()) {
			case FULFILLMENT:
				detailContainer2.getStyleClass().add("detail--vac-fulfillment");
				break;
			case VIOLATION:
				detailContainer2.getStyleClass().add("detail--vac-violation");
				break;
			case NONE:
				detailContainer2.setStyle("-fx-background-color: transparent");
				break;
			default:
				break;
			}

			for (ActivityConformanceType activityConformanceType : item) {
				if (!hideConformanceTypeOther || (activityConformanceType.getType() != ActivityConformanceType.Type.INSERTION_OTHER && activityConformanceType.getType() != ActivityConformanceType.Type.DELETION_OTHER)) {
					Region detail = new Region();
					detail.getStyleClass().clear();
					detail.getStyleClass().addAll("detail", activityConformanceType.getCssClass());
					if (activityConformanceType.getTooltipText() != null) {
						Tooltip.install(detail, new Tooltip(activityConformanceType.getTooltipText()));
					}
					detailContainer2.getChildren().add(detail);
				}
			}
			
			this.getTableRow().getItem().getVacuousConformance();
			setText(null);
			setGraphic(detailContainer);
		}
	}
}
