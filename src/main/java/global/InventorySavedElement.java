package global;

import java.io.File;
import java.util.Date;

/**
 *  The InventorySavedElement class stores data about a specific model or event log snapshot. 
 */
public class InventorySavedElement {
	
	File file;
	Date saveTimestamp;
	String saveName;
	InventoryElementTypeEnum typeEnum;
	
	public InventorySavedElement() {
	}
	
	public InventorySavedElement(File file, Date saveTimestamp, String saveName, InventoryElementTypeEnum typeEnum) {
		this.file = file;
		this.saveTimestamp = saveTimestamp;
		this.saveName = saveName;
		this.typeEnum = typeEnum;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public Date getSaveTimestamp() {
		return saveTimestamp;
	}

	public void setSaveTimestamp(Date saveTimestamp) {
		this.saveTimestamp = saveTimestamp;
	}

	public String getSaveName() {
		return saveName;
	}

	public void setSaveName(String saveName) {
		this.saveName = saveName;
	}

	public InventoryElementTypeEnum getTypeEnum() {
		return typeEnum;
	}

	public void setTypeEnum(InventoryElementTypeEnum typeEnum) {
		this.typeEnum = typeEnum;
	}

}
