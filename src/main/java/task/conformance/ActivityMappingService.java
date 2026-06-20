package task.conformance;

import java.io.File;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class ActivityMappingService extends Service<ActivityMappingResult> {

	private File xmlModel;
	private File logFile;

	public File getXmlModel() {
		return xmlModel;
	}
	public void setXmlModel(File xmlModel) {
		this.xmlModel = xmlModel;
	}

	public File getLogFile() {
		return logFile;
	}
	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	@Override
	protected Task<ActivityMappingResult> createTask() {
		ActivityMappingTask activityMappingTask = new ActivityMappingTask();
		activityMappingTask.setXmlModel(xmlModel);
		activityMappingTask.setLogFile(logFile);

		return activityMappingTask;
	}

}
