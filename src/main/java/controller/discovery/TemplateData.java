package controller.discovery;

import javafx.beans.property.SimpleBooleanProperty;
import util.ConstraintTemplate;

public class TemplateData {
	private ConstraintTemplate constraintTemplate;
	private SimpleBooleanProperty isSelectedProperty = new SimpleBooleanProperty();

	public TemplateData(ConstraintTemplate constraintTemplate, boolean isSelected) {
		this.constraintTemplate = constraintTemplate;
		this.isSelectedProperty.set(isSelected);
	}

	public ConstraintTemplate getConstraintTemplate() {
		return constraintTemplate;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constraintTemplate == null) ? 0 : constraintTemplate.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TemplateData other = (TemplateData) obj;
		if (constraintTemplate != other.constraintTemplate)
			return false;
		return true;
	}
}
