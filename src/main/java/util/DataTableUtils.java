package util;

import datatable.AbstractDataRow;
import datatable.AbstractDataRow.RowStatus;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.css.PseudoClass;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ScrollEvent;

public class DataTableUtils {

	private static PseudoClass lastRow = PseudoClass.getPseudoClass("last-row");

	//Private constructor to avoid unnecessary instantiation of the class
	private DataTableUtils() {
	}

	public static <T extends AbstractDataRow> void setDefaultRowFactory(TableView<T> tableView) {
		tableView.setRowFactory(tv -> {
			//For styling the last row (https://stackoverflow.com/questions/36132186/formatting-last-row-in-tableview-in-javafx)
			TableRow<T> row = new TableRow<T>() {
				@Override
				public void updateIndex(int index) {
					super.updateIndex(index);
					pseudoClassStateChanged(lastRow, index >= this.getTableView().getItems().size() - 1);
				}
			};
			//Allows to start row editing by double-clicking the row
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && !row.isEmpty() && row.getItem().getRowStatus() == RowStatus.SAVED) {
					row.getItem().startRowEdit();
				}
			});
			return row;
		});
	}

	public static void addScrollFilter(TableView<?> tableView) {
		//Prevents the focus from jumping to wrong nodes when scrolling with scroll wheel
		tableView.addEventFilter(ScrollEvent.ANY, scrollEvent -> {
			tableView.requestFocus();
		});
	}

	public static <T> void setContentBasedHeight(TableView<T> tableView, double cellHeight, double headerHeight) {
		tableView.setFixedCellSize(cellHeight);
		ReadOnlyListWrapper<T> dataCostsWrapper = new ReadOnlyListWrapper<T>(tableView.getItems());
		tableView.prefHeightProperty().bind(dataCostsWrapper.sizeProperty().multiply(cellHeight).add(headerHeight));
	}
}
