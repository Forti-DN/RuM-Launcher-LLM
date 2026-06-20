package treedata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeDataActivity extends TreeDataBase {

	private String activityName;

	//Each activity has a list of its attributes and each attribute has a list of activities it belongs to
	private List<TreeDataAttribute> attributes = new ArrayList<TreeDataAttribute>();

	private List<TreeDataAttribute> attributesUnmodifiable = Collections.unmodifiableList(attributes);

	public TreeDataActivity() {
		super();
	}

	public TreeDataActivity(String activityName) {
		super(activityName);
		this.activityName = activityName;
	}

	public String getActivityName() {
		return activityName;
	}
	public void setActivityName(String activityName) {
		this.activityName = activityName;
		setDisplayText(activityName);
	}

	public void addAttribute(TreeDataAttribute treeDataAttribute) {
		if (!attributes.contains(treeDataAttribute)) {
			attributes.add(treeDataAttribute);
			treeDataAttribute.addedToActivity(this);
			this.getChildren().add(treeDataAttribute);
		}
	}

	public void refreshAttribute(TreeDataAttribute treeDataAttribute) {
		//Workaround for a visual indentation bug that happens when treeDataAttribute is removed from another activity
		if (attributes.contains(treeDataAttribute)) {
			this.getChildren().remove(treeDataAttribute);
			this.getChildren().add(treeDataAttribute);
		}
	}

	public void removeAttribute(TreeDataAttribute treeDataAttribute) {
		attributes.remove(treeDataAttribute);
		treeDataAttribute.removedFromActivity(this);
		this.getChildren().remove(treeDataAttribute);
		for (TreeDataActivity treeDataActivity : treeDataAttribute.getActivitiesUnmodifiable()) {
			treeDataActivity.refreshAttribute(treeDataAttribute);
		}
	}

	protected void addedToAttribute(TreeDataAttribute treeDataAttribute) {
		attributes.add(treeDataAttribute);
		this.getChildren().add(treeDataAttribute);
	}

	protected void removedFromAttribute(TreeDataAttribute treeDataAttribute) {
		attributes.remove(treeDataAttribute);
		this.getChildren().remove(treeDataAttribute);
		for (TreeDataActivity treeDataActivity : treeDataAttribute.getActivitiesUnmodifiable()) {
			treeDataActivity.refreshAttribute(treeDataAttribute);
		}
	}

	//Attributes can not be changed directly because this would cause tree item children and treeDataAttribute.activities lists to become out of sync
	public List<TreeDataAttribute> getAttributesUnmodifiable() {
		return attributesUnmodifiable;
	}

	@Override
	public String toString() {
		return "TreeDataActivity [activityName=" + activityName + "]";
	}
}
