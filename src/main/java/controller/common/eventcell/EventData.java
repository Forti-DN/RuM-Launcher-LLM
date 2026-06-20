package controller.common.eventcell;

import java.util.Date;
import java.util.Map;

import task.conformance.ActivityConformanceType;

public class EventData {
	private int eventNumber; //Event number in the trace
	private String conceptName; //Event name (concept:name)
	private Date timeTimestamp; //Event timestamp (time:timestamp)

	private Map<String, String> payload; //All event attributes except name and timestamp

	private ActivityConformanceType activityConformanceType; //Used only for conformance checking

	private Map<String, String> constraintStates; //Used only for monitoring

	public int getEventNumber() {
		return eventNumber;
	}
	public void setEventNumber(int eventNumber) {
		this.eventNumber = eventNumber;
	}

	public String getConceptName() {
		return conceptName;
	}
	public void setConceptName(String conceptName) {
		this.conceptName = conceptName;
	}

	public Date getTimeTimestamp() {
		return timeTimestamp;
	}
	public void setTimeTimestamp(Date timeTimestamp) {
		this.timeTimestamp = timeTimestamp;
	}

	public Map<String, String> getPayload() {
		return payload;
	}
	public void setPayload(Map<String, String> payload) {
		this.payload = payload;
	}

	public ActivityConformanceType getActivityConformanceType() {
		return activityConformanceType;
	}
	public void setActivityConformanceType(ActivityConformanceType activityConformanceType) {
		this.activityConformanceType = activityConformanceType;
	}

	public Map<String, String> getConstraintStates() {
		return constraintStates;
	}
	public void setConstraintStates(Map<String, String> constraintStates) {
		this.constraintStates = constraintStates;
	}

	@Override
	public String toString() {
		//Payload is left out because it's size is theoretically unlimited
		return "EventData [eventNumber=" + eventNumber + ", conceptName=" + conceptName + ", timeTimestamp="
		+ timeTimestamp + ", activityConformanceType=" + activityConformanceType +  ", constraintStates="
		+ constraintStates + "]";
	}
}
