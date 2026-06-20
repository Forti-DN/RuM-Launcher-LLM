package controller.editor.data;

import javafx.beans.property.SimpleBooleanProperty;
import treedata.TreeDataAttribute;

public class AttributeSelectionData {
	private TreeDataAttribute treeDataAttribute;
	private SimpleBooleanProperty isSelectedProperty = new SimpleBooleanProperty();
	private boolean isNew;

	public AttributeSelectionData(TreeDataAttribute treeDataAttribute, boolean isSelected, boolean isNew) {
		this.treeDataAttribute = treeDataAttribute;
		this.isSelectedProperty.set(isSelected);
		this.isNew = isNew;
	}

	public TreeDataAttribute getTreeDataAttribute() {
		return treeDataAttribute;
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
