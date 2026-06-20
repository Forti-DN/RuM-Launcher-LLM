package util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XMxmlGZIPParser;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.operationalsupport.xml.OSXMLConverter;

import controller.common.eventcell.EventData;
import org.processmining.plugins.ltlchecker.InstanceModel;
import task.conformance.ActivityConformanceType;

public final class LogUtils {

	private static OSXMLConverter osxmlConverter = new OSXMLConverter();

	//Private constructor to avoid unnecessary instantiation of the class
	private LogUtils() {
	}

	public static XLog convertToXlog(File logFile) {
		XLog xlog = null;

		XParser parser = null;
		if (logFile.getName().toLowerCase().endsWith(".mxml"))
			parser = new XMxmlParser();
		
		else if (logFile.getName().toLowerCase().endsWith(".xes"))
			parser = new XesXmlParser();
			
		else if (logFile.getName().toLowerCase().endsWith(".xes.gz"))
			parser = new XesXmlGZIPParser();
			
		else if (logFile.getName().toLowerCase().endsWith(".mxml.gz"))
			parser = new XMxmlGZIPParser();
		
		
		try {
			if (parser!=null && parser.canParse(logFile))
				xlog = parser.parse(logFile).get(0);
		
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (XTrace trace : xlog)
			for (XEvent evt : trace)
				if (XLifecycleExtension.instance().extractTransition(evt) == null)
					XLifecycleExtension.instance().assignStandardTransition(evt, StandardModel.COMPLETE);
		
		return xlog;
	}

	//TODO: This method is used for too many different purposes
	public static List<EventData> createEventDataList(XTrace xTrace, List<ActivityConformanceType> activityConformanceTypes, boolean filterInsertions, boolean filterDeletions) {
		ArrayList<EventData> eventDataList = new ArrayList<>();
		int eventNumber = 0;
		EventData eventData;

		for (int i = 0; i < xTrace.size(); i++) {
			if (activityConformanceTypes != null) {
				if (activityConformanceTypes.get(i).getType() == ActivityConformanceType.Type.INSERTION_OTHER) {
					//Skipping the activity if insertion was caused by another constraint
					continue;
				} else if (activityConformanceTypes.get(i).getType() == ActivityConformanceType.Type.DELETION_OTHER) {
					//Skipping the activity if insertion was caused by another constraint
					continue;
				} else if (filterInsertions && (activityConformanceTypes.get(i).getType() == ActivityConformanceType.Type.INSERTION || activityConformanceTypes.get(i).getType() == ActivityConformanceType.Type.INSERTION_OTHER)) {
					//Skipping all insertion events if filterInsertions == true
					continue;
				} else if (filterDeletions && (activityConformanceTypes.get(i).getType() == ActivityConformanceType.Type.DELETION || activityConformanceTypes.get(i).getType() == ActivityConformanceType.Type.DELETION_OTHER)) {
					//Skipping all deletion events if filterInsertions == true
					continue;
				}
			}

			eventNumber++;

			XExtendedEvent extxevent = new XExtendedEvent(xTrace.get(i));
			eventData = new EventData();
			eventData.setEventNumber(eventNumber);
			eventData.setConceptName(extxevent.getName());
			eventData.setTimeTimestamp(extxevent.getTimestamp());

			Map<String, String> payload = new TreeMap<>();
			extxevent.getAttributes().forEach((k,v) -> payload.put(k, v.toString()) );

			payload.remove("concept:name");
			payload.remove("time:timestamp");
			eventData.setPayload(payload);

			if (activityConformanceTypes != null) {
				boolean isReplayer = activityConformanceTypes.stream().anyMatch(item -> item.getType() == ActivityConformanceType.Type.INSERTION
																						|| item.getType() == ActivityConformanceType.Type.INSERTION_OTHER
																						|| item.getType() == ActivityConformanceType.Type.DELETION
																						|| item.getType() == ActivityConformanceType.Type.DELETION_OTHER);
				
				if (isReplayer && filterDeletions) {	// Means that Apply Alignment is true in a Replayer method, so that we don't want colored strips under the cells
					ActivityConformanceType dummyType = new ActivityConformanceType(ActivityConformanceType.Type.NONE);
					dummyType.setTooltipText(activityConformanceTypes.get(i).getTooltipText());
					eventData.setActivityConformanceType(dummyType);
				
				} else {
					eventData.setActivityConformanceType(activityConformanceTypes.get(i));
				}
			}

			eventDataList.add(eventData);
		}

		return eventDataList;
	}


	//Used in monitoring to create an trace that contains only the event to be processed
	public static String wrapEventForMonitoring(XEvent ev, XTrace t, int eventIndex) {
		//From original implementation; wraps the event in a new trace object and copies original trace attributes to the wrapper trace
		XTraceImpl t1 = new XTraceImpl(t.getAttributes());
		String eventName = XConceptExtension.instance().extractName(ev);
		ev.getAttributes().putAll(t.getAttributes());
		XConceptExtension.instance().assignName(ev,eventName);

		//From original implementation; adds index of the event to the timestamp of the event (milliseconds)
		XAttributeTimestampImpl timestamp = (XAttributeTimestampImpl) ev.getAttributes().get("time:timestamp");
		((XAttributeTimestampImpl) ev.getAttributes().get("time:timestamp")).setValueMillis(timestamp.getValueMillis() + eventIndex);
		t1.add(ev);

		//Original implementation converted the wrapper trace to xml string using osxmlConverter.toXML and sent it to port 4444.
		//Original implementation then read the xml string and converted it back to xtrace object using osxmlConverter.fromXML

		//Using osxmlConverter.toXML because monitorRunner.setTrace expects a string
		return osxmlConverter.toXML(t1).replace('\n', ' ');
	}

	//Used in monitoring (MP-Declare w Alloy method) to create an artificial end event that signifies that the trace has ended
	public static String createEndEventString(XTrace t) {
		//From original implementation; creates a new trace object with original trace attributes and one event named 'complete'
		XTraceImpl lastTrace = new XTraceImpl(t.getAttributes());
		XFactory nxFactory = XFactoryRegistry.instance().currentDefault();
		XEvent le = nxFactory.createEvent();
		XConceptExtension nconcept = XConceptExtension.instance();
		nconcept.assignName(le, "complete");
		XLifecycleExtension nlc = XLifecycleExtension.instance();
		nlc.assignTransition(le, "complete");
		XTimeExtension ntimeExtension = XTimeExtension.instance();
		ntimeExtension.assignTimestamp(le, ntimeExtension.extractTimestamp(t.get(t.size() - 1)).getTime() + 1);
		lastTrace.add(le);

		//Using osxmlConverter.toXML because monitorRunner.setTrace expects a string
		return osxmlConverter.toXML(lastTrace).replace('\n', ' ');
	}

	//Used in monitoring (MoBuConLTL method) to create an artificial end event that signifies that the trace has ended
	public static String createStartEvent(XTrace t) {
		//From original implementation; creates a new trace object with original trace attributes and one event named 'complete'
		XTraceImpl lastTrace = new XTraceImpl(t.getAttributes());
		XFactory nxFactory = XFactoryRegistry.instance().currentDefault();
		XEvent le = nxFactory.createEvent();
		XConceptExtension nconcept = XConceptExtension.instance();
		nconcept.assignName(le, "start");
		XLifecycleExtension nlc = XLifecycleExtension.instance();
		nlc.assignTransition(le, "complete");
		XTimeExtension ntimeExtension = XTimeExtension.instance();
		ntimeExtension.assignTimestamp(le, ntimeExtension.extractTimestamp(t.get(t.size() - 1)).getTime() + 1);
		lastTrace.add(le);

		//Using osxmlConverter.toXML because monitorRunner.setTrace expects a string
		return osxmlConverter.toXML(lastTrace).replace('\n', ' ');
	}
	
	public static void checkDataExistence(File logFile) throws Exception {
		XLog log = convertToXlog(logFile);
		boolean isDataExisting = log.stream().anyMatch(trace -> 
					trace.stream().anyMatch(event -> 
						event.getAttributes().values().stream().anyMatch(att -> 
							!att.getKey().equals(XConceptExtension.KEY_NAME) && !att.getKey().equals(XLifecycleExtension.KEY_TRANSITION) && !att.getKey().equals(XTimeExtension.KEY_TIMESTAMP)
						)
					)
				);
		if (!isDataExisting)
			throw new Exception();
		
	}

	public static InstanceModel deepCloneTrace(InstanceModel instanceModel) {
		InstanceModel deepClone = new InstanceModel();
		deepClone.setId(instanceModel.getId());
		deepClone.setInstance((XTrace) instanceModel.getInstance().clone());
		deepClone.setCorrect(instanceModel.isCorrect());
		deepClone.setHealthDegree(instanceModel.getHealthDegree());
		deepClone.setRuleFulfillment(instanceModel.getRuleFulfillment());

		return deepClone;
	}
}
