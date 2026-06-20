package task.discovery.mp_enhancer;

import java.util.HashMap;
import java.util.List;

public class Event {
    private String caseID;
    private String activityName;
    private String timestamp;
    private HashMap<String, String> payload;
    private String transition; // "complete" if trace is complete

    public Event(List<String> attributes, String[] values) {
        this.caseID = values[0];
        this.activityName = values[1];
        this.timestamp = values[2];
        this.transition = values[3];
        
        this.payload = new HashMap<>();
        for (int i = 4; i < values.length; i++)
            if (!values[i].equals(""))
                payload.put(attributes.get(i), values[i]);
        /*
        if (payload.isEmpty())
            payload.put("no payload", "true");
        */
    }

    public Event(String caseID, String activityName, String timestamp, String transition) {
        this.caseID = caseID;
        this.activityName = activityName;
        this.timestamp = timestamp;
        this.transition = transition;
        this.payload = new HashMap<>();
    }

    public Event(Event event) {
        this.caseID = event.caseID;
        this.activityName = event.activityName;
        this.timestamp = event.timestamp;
        this.transition = event.transition;
        this.payload = new HashMap<>(event.payload);
    }

    public String getCaseID() {
		return caseID;
	}

	public String getActivityName() {
		return activityName + (transition != null ? "-"+transition : "");
	}

	public String getTimestamp() {
		return timestamp;
	}

	public HashMap<String, String> getPayload() {
		return payload;
	}

	public String getTransition() {
		return transition;
	}

	public String toString() {
    	return "(" + this.caseID + ", " + getActivityName() + ", " + this.timestamp + ", " + this.payload + ")";
    }
}