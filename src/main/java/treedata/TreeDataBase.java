package treedata;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.scene.control.TreeItem;

public class TreeDataBase extends TreeItem<TreeDataBase> {

	private SimpleStringProperty displayText = new SimpleStringProperty();
	private ReadOnlyBooleanWrapper isEditing = new ReadOnlyBooleanWrapper(false);

	private TreeModificationEvent<TreeDataBase> treeModificationEvent = new TreeModificationEvent<TreeDataBase>(TreeItem.valueChangedEvent(), this);

	public TreeDataBase() {
		super.setValue(this);
		this.setExpanded(true);
	}

	public TreeDataBase(String displayText) {
		this();
		this.displayText.set(displayText);
	}

	public String getDisplayText() {
		return displayText.get();
	}
	protected void setDisplayText(String displayText) {
		//Display text should be updated trough setters of extending classes
		this.displayText.set(displayText);
		Event.fireEvent(this, treeModificationEvent);
	}
	public SimpleStringProperty displayTextProperty() {
		return displayText;
	}

	public boolean getIsEditing() {
		return isEditing.get();
	}
	public void setIsEditing(boolean isEditing) {
		this.isEditing.set(isEditing);
		Event.fireEvent(this, treeModificationEvent);
	}
	public ReadOnlyBooleanWrapper getIsEditingWrapper() {
		return isEditing;
	}

	@Override
	public String toString() {
		return "TreeDataBase [displayText=" + displayText + ", isEditing=" + isEditing + "]";
	}
}
