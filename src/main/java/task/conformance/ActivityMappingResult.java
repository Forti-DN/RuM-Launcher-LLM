package task.conformance;

import java.util.List;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;

public class ActivityMappingResult {

	private Map<ReplayableActivityDefinition,XEventClass> activityMapping; //map or mapping in previous version
	private List<String> allActivities; //logEvents in previous version - activities for flowSettings
	private List<String> matchedActivities; //matched in previous version - activities for dataSettings
	private List<String> unmatchedActivities; //unmatched in previous version

	public ActivityMappingResult() {
	}

	public ActivityMappingResult(Map<ReplayableActivityDefinition, XEventClass> activityMapping,
			List<String> allActivities, List<String> matchedActivities, List<String> unmatchedActivities) {
		this.activityMapping = activityMapping;
		this.allActivities = allActivities;
		this.matchedActivities = matchedActivities;
		this.unmatchedActivities = unmatchedActivities;
	}

	public Map<ReplayableActivityDefinition, XEventClass> getActivityMapping() {
		return activityMapping;
	}

	public void setActivityMapping(Map<ReplayableActivityDefinition, XEventClass> activityMapping) {
		this.activityMapping = activityMapping;
	}

	public List<String> getAllActivities() {
		return allActivities;
	}

	public void setAllActivities(List<String> allActivities) {
		this.allActivities = allActivities;
	}

	public List<String> getMatchedActivities() {
		return matchedActivities;
	}

	public void setMatchedActivities(List<String> matchedActivities) {
		this.matchedActivities = matchedActivities;
	}

	public List<String> getUnmatchedActivities() {
		return unmatchedActivities;
	}

	public void setUnmatchedActivities(List<String> unmatchedActivities) {
		this.unmatchedActivities = unmatchedActivities;
	}

	@Override
	public String toString() {
		return "ActivityMappingResult [activityMapping=" + activityMapping + ", allActivities=" + allActivities
				+ ", matchedActivities=" + matchedActivities + ", unmatchedActivities=" + unmatchedActivities + "]";
	}
}
