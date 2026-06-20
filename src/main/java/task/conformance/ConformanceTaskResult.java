package task.conformance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.deckfour.xes.model.XTrace;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;

public class ConformanceTaskResult {

	private List<XTrace> traceList; // +
	// count number of del and insertions
	private Map<XTrace,TreeMap<ConformanceStatisticType,String>> traceStatisticsMap = new HashMap<XTrace, TreeMap<ConformanceStatisticType,String>>();
	private Map<XTrace,List<ConformanceTaskResultDetail>> traceConformanceMap = new HashMap<XTrace, List<ConformanceTaskResultDetail>>();
	// leave empty
	private List<String> constraintList;
	private Map<String,TreeMap<ConformanceStatisticType,String>> constraintStatisticsMap = new HashMap<String, TreeMap<ConformanceStatisticType,String>>();
	private Map<String,List<ConformanceTaskResultDetail>> constraintConformanceMap = new HashMap<String, List<ConformanceTaskResultDetail>>();
	private Map<ConformanceStatisticType,String> globalStatisticsMap = new HashMap<ConformanceStatisticType, String>();

	//Used for setting detailsTableView column widths
	private int maxTraceLength;
	private double maxTraceNameLength;
	private double maxConstraintNameLength;

	private ArrayList<ConformanceTaskResultGroup> resultsGroupedByTrace;
	private ArrayList<ConformanceTaskResultGroup> resultsGroupedByConstraint;

	public ConformanceTaskResult() {
	}

	public List<XTrace> getTraceList() {
		return traceList;
	}
	public void setTraceList(List<XTrace> traceList) {
		this.traceList = traceList;
	}

	public List<String> getConstraintList() {
		return constraintList;
	}
	public void setConstraintList(List<String> constraintList) {
		this.constraintList = constraintList;
		constraintList.sort(null);
	}

	// stays as it is - used for the counts of insertions and del and fitness
	// call it once for del, insert per trace
	public void addTraceStatistic(XTrace xtrace, ConformanceStatisticType statisticType, String statisticValue) {
		if (this.traceStatisticsMap.get(xtrace) == null) {
			TreeMap<ConformanceStatisticType, String> traceStatistics = new TreeMap<ConformanceStatisticType, String>();
			String traceName = xtrace.getAttributes().get("concept:name") != null ? xtrace.getAttributes().get("concept:name").toString() : "";
			traceStatistics.put(ConformanceStatisticType.NAME, "Trace ID: " + traceName);
			this.traceStatisticsMap.put(xtrace, traceStatistics);

			if (xtrace.size() > maxTraceLength) {
				maxTraceLength = xtrace.size();
			}

			Text text = new Text(traceName);
			new Scene(new Group(text));
			text.applyCss();
			if (text.getLayoutBounds().getWidth() > maxTraceNameLength) {
				maxTraceNameLength = text.getLayoutBounds().getWidth();
			}
		}
		this.traceStatisticsMap.get(xtrace).put(statisticType, statisticValue);
	}

	// remove, not needed
	public void addConstraintStatistic(String constraint, ConformanceStatisticType statisticType, String statisticValue) {
		if (this.constraintStatisticsMap.get(constraint) == null) {
			TreeMap<ConformanceStatisticType, String> constraintStatistics = new TreeMap<ConformanceStatisticType, String>();
			constraintStatistics.put(ConformanceStatisticType.NAME, constraint.replace("\n", ""));
			this.constraintStatisticsMap.put(constraint, constraintStatistics);

			Text text = new Text(constraint);
			new Scene(new Group(text));
			text.applyCss();
			if (text.getLayoutBounds().getWidth() > maxConstraintNameLength) {
				maxConstraintNameLength = text.getLayoutBounds().getWidth();
			}
		}
		this.constraintStatisticsMap.get(constraint).put(statisticType, statisticValue);
	}

	// create my own method with addit. checks
	public void addResultDetail(ConformanceTaskResultDetail conformanceTaskResultDetail) {
		XTrace xtrace = conformanceTaskResultDetail.getXtrace();
		// igas traces info palju lisati ja kustutati s[ndmusi
		if (traceConformanceMap.get(xtrace) == null) {
			traceConformanceMap.put(xtrace, new ArrayList<ConformanceTaskResultDetail>());
		}
		traceConformanceMap.get(xtrace).add(conformanceTaskResultDetail);

		String constraint = conformanceTaskResultDetail.getConstraint();
		if (constraintConformanceMap.get(constraint) == null) {
			constraintConformanceMap.put(constraint, new ArrayList<ConformanceTaskResultDetail>());
		}
		constraintConformanceMap.get(constraint).add(conformanceTaskResultDetail);
	}
	
	// i will use - total nr of insertions and total nr of deletions and overall fitness
	public void setGlobalStatistic(ConformanceStatisticType statisticType, String statisticValue) {
		globalStatisticsMap.put(statisticType, statisticValue);
	}
	
	public Map<ConformanceStatisticType, String> getGlobalStatisticsMap() {
		return globalStatisticsMap;
	}

	//Intended to be called as the last step of preparing the conformance check results
		public void createResultGroupings() {
			createResultTraceGroup();
			createResultConstraintGroup();
		}

		public void createResultTraceGroup() {
			resultsGroupedByTrace = new ArrayList<ConformanceTaskResultGroup>();
			ConformanceTaskResultGroup traceGroup;
			for (XTrace xtrace : traceList) {
				traceGroup = new ConformanceTaskResultGroup();
				traceGroup.setGroupStatistics(traceStatisticsMap.get(xtrace));
				traceGroup.setGroupDetails(traceConformanceMap.get(xtrace));
				traceGroup.setXtrace(xtrace);
				resultsGroupedByTrace.add(traceGroup);
			}
		}

		public void createResultConstraintGroup() {
			resultsGroupedByConstraint = new ArrayList<ConformanceTaskResultGroup>();
			ConformanceTaskResultGroup constraintGroup;
			for (String constraint : constraintList) {
				constraintGroup = new ConformanceTaskResultGroup();
				constraintGroup.setGroupStatistics(constraintStatisticsMap.get(constraint));
				constraintGroup.setGroupDetails(constraintConformanceMap.get(constraint));
				resultsGroupedByConstraint.add(constraintGroup);
			}
		}

	public int getMaxTraceLength() {
		return maxTraceLength;
	}
	public double getMaxTraceNameWidth() {
		return maxTraceNameLength;
	}
	public double getMaxConstraintNameWidth() {
		return maxConstraintNameLength;
	}

	public List<ConformanceTaskResultGroup> getResultsGroupedByTrace() {
		return resultsGroupedByTrace;
	}

	public List<ConformanceTaskResultGroup> getResultsGroupedByConstraint() {
		return resultsGroupedByConstraint;
	}
}
