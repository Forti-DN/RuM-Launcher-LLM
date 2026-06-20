package global;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "savedElements")
public class InventorySavedElementListWrapper {
	
	private List<InventorySavedElement> savedElements;

	@XmlElement(name = "savedElement")
	public List<InventorySavedElement> getSavedElements() {
		return savedElements;
	}

	public void setSavedElements(List<InventorySavedElement> savedElements) {
		this.savedElements = savedElements;
	}
}
