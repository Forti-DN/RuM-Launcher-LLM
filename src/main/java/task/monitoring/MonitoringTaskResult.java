package task.monitoring;

import java.util.List;

import org.deckfour.xes.model.XLog;

import controller.common.eventcell.EventData;

public class MonitoringTaskResult {

	private XLog xlog;
	private List<String> traceNames;
	private List<List<EventData>> traceEventsData;

	public MonitoringTaskResult() {
	}

	public synchronized XLog getXlog() {
		return xlog;
	}
	
	public synchronized void setXlog(XLog xlog) {
		this.xlog = xlog;
	}
	
	public synchronized List<String> getTraceNames() {
		return traceNames;
	}
	
	public synchronized void setTraceNames(List<String> traceNames) {
		this.traceNames = traceNames;
	}
	
	public synchronized List<List<EventData>> getTraceEventsData() {
		return traceEventsData;
	}
	
	public synchronized void setTraceEventsData(List<List<EventData>> traceEventsData) {
		this.traceEventsData = traceEventsData;
	}
}
