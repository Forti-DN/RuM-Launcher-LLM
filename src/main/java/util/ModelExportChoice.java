package util;

import java.io.File;

public class ModelExportChoice {
	private File chosenFile;
	private ModelViewType chosenExportType; //TODO: Should maybe create a new enum for this

	public ModelExportChoice(File chosenFile, ModelViewType chosenExportType) {
		super();
		this.chosenFile = chosenFile;
		this.chosenExportType = chosenExportType;
	}

	public File getChosenFile() {
		return chosenFile;
	}

	public ModelViewType getChosenExportType() {
		return chosenExportType;
	}

}
