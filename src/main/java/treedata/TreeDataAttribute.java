package treedata;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import controller.editor.AttributeType;

public class TreeDataAttribute extends TreeDataBase {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String attributeName;
	private AttributeType attributeType;
	private BigDecimal valueFrom;
	private BigDecimal valueTo;
	private List<String> possibleValues = new ArrayList<>();


	//Each activity (in case of editor section) has a list of its attributes and each attribute has a list of activities it belongs to
	private List<TreeDataActivity> activities = new ArrayList<>();
	private List<TreeDataActivity> activitiesUnmodifiable = Collections.unmodifiableList(activities);

	public TreeDataAttribute() {
		super();
	}

	public String getAttributeName() {
		return attributeName;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
		updateDisplayText();
	}

	public AttributeType getAttributeType() {
		return attributeType;
	}
	public void setAttributeType(AttributeType attributeType) {
		this.attributeType = attributeType;
		updateDisplayText();
	}

	public BigDecimal getValueFrom() {
		return valueFrom;
	}
	public void setValueFrom(BigDecimal valueFrom) {
		this.valueFrom = valueFrom;
		updateDisplayText();
	}

	public BigDecimal getValueTo() {
		return valueTo;
	}
	public void setValueTo(BigDecimal valueTo) {
		this.valueTo = valueTo;
		updateDisplayText();
	}

	public List<String> getPossibleValues() {
		return possibleValues;
	}
	public void setPossibleValues(List<String> possibleValues) {
		this.possibleValues = possibleValues;
		updateDisplayText();
	}

	public void addActivity(TreeDataActivity treeDataActivity) {
		if (!activities.contains(treeDataActivity)) {
			activities.add(treeDataActivity);
			treeDataActivity.addedToAttribute(this);
		}
	}

	public void removeActivity(TreeDataActivity treeDataActivity) {
		activities.remove(treeDataActivity);
		treeDataActivity.removedFromAttribute(this);
	}

	protected void addedToActivity(TreeDataActivity treeDataActivity) {
		activities.add(treeDataActivity);
	}

	protected void removedFromActivity(TreeDataActivity treeDataActivity) {
		activities.remove(treeDataActivity);
	}

	//Activities can not be changed directly because this would cause the treeDataActivity.attributes lists to become out of sync
	public List<TreeDataActivity> getActivitiesUnmodifiable() {
		return activitiesUnmodifiable;
	}

	private void updateDisplayText() {
		StringBuffer sb = new StringBuffer();
		sb.append(attributeName);
		sb.append("\n");
		if (attributeType != null) {
			sb.append(attributeType.getDisplayText());
			switch (attributeType) {
			case INTEGER: //Fall through intended
			case FLOAT:
				if (valueFrom != null && valueTo != null) {
					sb.append(" [");
					sb.append(valueFrom.toString());
					sb.append(", ");
					sb.append(valueTo.toString());
					sb.append("]");
				}
				break;
			case ENUMERATION:
				if (possibleValues != null && !possibleValues.isEmpty()) {
					sb.append(" of ");
					sb.append(possibleValues.size());
					sb.append(" values");
				}
				break;
			default:
				logger.error("Unhandled attribute type for updating display text: {}", attributeType);
				break;
			}
		}

		setDisplayText(sb.toString());
	}

	@Override
	public String toString() {
		return "TreeDataAttribute [attributeName=" + attributeName + ", attributeType=" + attributeType + ", valueFrom="
				+ valueFrom + ", valueTo=" + valueTo + ", possibleValues=" + possibleValues + "]";
	}
}
