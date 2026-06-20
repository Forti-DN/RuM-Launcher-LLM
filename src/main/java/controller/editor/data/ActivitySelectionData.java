package controller.editor.data;

import javafx.beans.property.SimpleBooleanProperty;
import treedata.TreeDataActivity;

public class ActivitySelectionData {
	private TreeDataActivity treeDataActivity;
	private SimpleBooleanProperty isSelectedProperty = new SimpleBooleanProperty();
	private boolean isNew;

	public ActivitySelectionData(TreeDataActivity treeDataActivity, boolean isSelected, boolean isNew) {
		this.treeDataActivity = treeDataActivity;
		this.isSelectedProperty.set(isSelected);
		this.isNew = isNew;
	}

	public TreeDataActivity getTreeDataActivity() {
		return treeDataActivity;
	}

	public boolean getIsNew() {
		return isNew;
	}

	public SimpleBooleanProperty isSelectedProperty() {
		return this.isSelectedProperty;
	}
	public boolean getIsSelected() {
		return isSelectedProperty.get();
	}
	public void setIsSelected(boolean isSelected) {
		this.isSelectedProperty.set(isSelected);
	}
}
