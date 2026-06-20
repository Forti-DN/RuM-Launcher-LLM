package task.generation;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import controller.common.eventcell.EventData;
import util.LogUtils;

public class GenerationTaskResult {
	private XLog generatedLog;
	private List<String> traceNames;
	private List<List<EventData>> traceEventsData;
	private List<EventData> traceAttributes;

	public GenerationTaskResult() {
		traceNames = new LinkedList<>();
		traceEventsData = new LinkedList<>();
		traceAttributes = new LinkedList<>();
	}

	public XLog getGeneratedLog() {
		return generatedLog;
	}
	
	public void setGeneratedLog(XLog generatedLog) {
		// Changing the trace names in the generated log
		int padding = (int) Math.log10(generatedLog.size()) +1;
		String caseNrFormat = "%0" + padding + "d";
		
		int i = 0;
		for (XTrace trace : generatedLog) {
			String traceType = trace.getAttributes().get("trace:type").toString();
			String traceName = "Case No. " + String.format(caseNrFormat, ++i) + " [" + traceType + "]";
			XConceptExtension.instance().assignName(trace, traceName);

			traceNames.add(traceName);
			traceEventsData.add(LogUtils.createEventDataList(trace, null, false, false));
			
			EventData traceAttributesEventData = new EventData();
			Map<String, String> payload = new TreeMap<>();
			
			trace.getAttributes().forEach((key, val) -> {
				if (!key.equals(XConceptExtension.KEY_NAME))
					payload.put(key, val.toString());
			});
			
			traceAttributesEventData.setConceptName("Trace Attributes");
			traceAttributesEventData.setPayload(payload);
			traceAttributes.add(traceAttributesEventData);
		}

		this.generatedLog = generatedLog;
	}

	public List<String> getTraceNames() {
		return traceNames;
	}
	/*
	public void setTraceNames(List<String> traceNames) {
		this.traceNames = traceNames;
	}
	*/
	public List<List<EventData>> getTraceEventsData() {
		return traceEventsData;
	}
	/*
	public void setTraceEventsData(List<List<EventData>> traceEventsData) {
		this.traceEventsData = traceEventsData;
	}
	*/
	public List<EventData> getTraceAttributes() {
		return traceAttributes;
	}
	/*
	public void setTraceAttributes(List<EventData> traceAttributes) {
		this.traceAttributes = traceAttributes;
	}
	*/
}
