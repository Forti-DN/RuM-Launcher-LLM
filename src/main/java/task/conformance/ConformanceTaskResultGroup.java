package task.conformance;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.deckfour.xes.model.XTrace;

public class ConformanceTaskResultGroup {

	private TreeMap<ConformanceStatisticType,String> groupStatistics; //Includes group name
	private List<ConformanceTaskResultDetail> groupDetails;
	private XTrace xtrace;

	//Used for tracking if the group should be open in the UI
	private boolean isExpanded;

	public ConformanceTaskResultGroup() {
	}

	public Map<ConformanceStatisticType, String> getGroupStatistics() {
		return groupStatistics;
	}
	public void setGroupStatistics(TreeMap<ConformanceStatisticType, String> groupStatistics) {
		this.groupStatistics = groupStatistics;
	}
	public List<ConformanceTaskResultDetail> getGroupDetails() {
		return groupDetails;
	}
	public void setGroupDetails(List<ConformanceTaskResultDetail> groupDetails) {
		this.groupDetails = groupDetails;
	}
	public XTrace getXtrace() {
		return xtrace;
	}
	public void setXtrace(XTrace xtrace) {
		this.xtrace = xtrace;
	}

	public boolean getIsExpanded() {
		return isExpanded;
	}
	public void setIsExpanded(boolean isExpanded) {
		this.isExpanded = isExpanded;
	}

	@Override
	public String toString() {
		//xtrace is left out because it's size is theoretically unlimited
		return "ConformanceTaskResultGroup [groupStatistics=" + groupStatistics + ", groupDetails=" + groupDetails
				+ ", isExpanded=" + isExpanded + "]";
	}
}
