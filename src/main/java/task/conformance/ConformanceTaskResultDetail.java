package task.conformance;

import java.util.List;

import org.deckfour.xes.model.XTrace;

import task.conformance.ActivityConformanceType.Type;

public class ConformanceTaskResultDetail {

	private XTrace xtrace;
	private String traceName; //Used as detail label when showing results grouped by constraints
	private String constraint; //Used as detail label when showing results grouped by traces
	private ActivityConformanceType vacuousConformance = new ActivityConformanceType(Type.NONE); //Used when no specific event can be tied to fulfillment/violation
	private List<ActivityConformanceType> activityConformanceTypes; // list of conformance types that was result of my algorithm

	public XTrace getXtrace() {
		return xtrace;
	}
	public void setXtrace(XTrace xtrace) {
		this.xtrace = xtrace;
	}
	public String getConstraint() {
		return constraint;
	}
	public void setConstraint(String constraint) {
		this.constraint = constraint;
	}
	public String getTraceName() {
		return traceName;
	}
	public void setTraceName(String traceName) {
		this.traceName = traceName;
	}
	public ActivityConformanceType getVacuousConformance() {
		return vacuousConformance;
	}
	public void setVacuousConformance(ActivityConformanceType vacuousConformance) {
		this.vacuousConformance = vacuousConformance;
	}
	public List<ActivityConformanceType> getActivityConformanceTypes() {
		return activityConformanceTypes;
	}
	public void setActivityConformanceTypes(List<ActivityConformanceType> activityConformanceTypes) {
		this.activityConformanceTypes = activityConformanceTypes;
	}
}
