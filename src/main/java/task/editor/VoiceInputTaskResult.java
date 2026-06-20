package task.editor;

public class VoiceInputTaskResult {

	private String recordedString;
	private String resultString;

	private TaskType taskType;

	public VoiceInputTaskResult() {
	}

	public String getRecordedString() {
		return recordedString;
	}
	public void setRecordedString(String recordedString) {
		this.recordedString = recordedString;
	}

	public String getResultString() {
		return resultString;
	}
	public void setResultString(String resultString) {
		this.resultString = resultString;
	}

	public TaskType getTaskType() {
		return taskType;
	}
	public void setTaskType(TaskType taskType) {
		this.taskType = taskType;
	}
}
