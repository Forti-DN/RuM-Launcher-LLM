package theFirst;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.operationalsupport.xml.OSXMLConverter;

import controller.monitoring.MonitoringMethod;

public class LogStreamer implements Runnable{
	private static int PORT = 4444;
	private static String HOST;
	private OSXMLConverter osxmlConverter = new OSXMLConverter();
	private Thread t;
	private String threadName;

	private String model;
	private XLog log;
	private int times;
	private MonitoringMethod monitoringMethod;

	static {
		try {
			HOST = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
		}
	}

	public LogStreamer(String model, XLog log, int times, MonitoringMethod monitoringMethod) {
		this.model = model;
		this.log = log;
		this.times = times;
		this.monitoringMethod = monitoringMethod;
		this.threadName = "LogStreamer";
		System.out.println("Creating LogStreamer thread");
	}
	public void start () {
		System.out.println("Starting LogStreamer" );
		if (t == null) {
			t = new Thread (this, threadName);
			t.start ();
		}
	}
	@Override
	public void run() {
		System.out.println("Running " + threadName);
		try (Socket socket = new Socket(HOST, PORT)) {
			socket.isConnected();
			PrintWriter writeOnTheSocket = new PrintWriter(socket.getOutputStream(), true);




			//TODO: LogStreaming should much more similar for all methods
			if (monitoringMethod == MonitoringMethod.MP_DECLARE_ALLOY || monitoringMethod == MonitoringMethod.MOBUCON_LTL || monitoringMethod == MonitoringMethod.ONLINE_DECLARE) {
				writeOnTheSocket.println(model);
				writeOnTheSocket.println("</model>");
				writeOnTheSocket.flush();
				for (XTrace t : log) {
					int eventIndex = 0;

					for (XEvent ev : t) {
						XTraceImpl t1 = new XTraceImpl(t.getAttributes());
						XAttributeTimestampImpl timestamp = (XAttributeTimestampImpl) ev.getAttributes().get("time:timestamp");
						((XAttributeTimestampImpl) ev.getAttributes().get("time:timestamp")).setValueMillis(timestamp.getValueMillis() + eventIndex);
						// add trace attributes to event
						String eventName = XConceptExtension.instance().extractName(ev);
						ev.getAttributes().putAll(t.getAttributes());
						XConceptExtension.instance().assignName(ev,eventName);
						// Add event to trace
						t1.add(ev);

						System.out.println("Writing to socket Event : " + ev.getAttributes().get("concept:name"));
						String packet = osxmlConverter.toXML(t1).replace('\n', ' ');
						// Transmit trace

						System.out.println(packet);
						writeOnTheSocket.println(packet);
						writeOnTheSocket.flush();
						eventIndex++;
						Thread.sleep(times);
					}

					//TODO: It is unclear why such a trace needs to be created artificially
					XTraceImpl lastTrace = new XTraceImpl(t.getAttributes());
					// Add last trace.
					XFactory nxFactory = XFactoryRegistry.instance().currentDefault();
					XEvent le = nxFactory.createEvent();
					//le.setAttributes(t.get(t.size() - 1).getAttributes());
					XConceptExtension nconcept = XConceptExtension.instance();
					nconcept.assignName(le, "complete");
					XLifecycleExtension nlc = XLifecycleExtension.instance();
					nlc.assignTransition(le, "complete");
					XTimeExtension ntimeExtension = XTimeExtension.instance();
					ntimeExtension.assignTimestamp(le, ntimeExtension.extractTimestamp(t.get(t.size() - 1)).getTime() + 1);
					lastTrace.add(le);
					System.out.println("Writing to socket Event : " + le.getAttributes().get("concept:name"));
					String packet = osxmlConverter.toXML(lastTrace).replace('\n', ' ');
					// Transmit trace
					writeOnTheSocket.println(packet);
					System.out.println("Kirjutame:");
					System.out.println(packet);
					writeOnTheSocket.flush();
				}

			} else if (monitoringMethod == MonitoringMethod.FLLOAT) {
				model = model.replace("\n", "").replace("\r", "");
				writeOnTheSocket.println(model);
				writeOnTheSocket.flush();

				//TODO: Sending weights could probably be removed
				double[] weights = new double[]{1.,1.,1.,1.,1.,1.};
				for(int i = 0; i<weights.length; i++){
					writeOnTheSocket.println(weights[i]);
					writeOnTheSocket.flush();
				}
				writeOnTheSocket.println("END_WEIGHTS");
				writeOnTheSocket.flush();

				for(XTrace t : log){
					int eventIndex = 0;

					for(XEvent ev : t){
						XTraceImpl t1 = new XTraceImpl(t.getAttributes());
						XAttributeTimestampImpl timestamp = (XAttributeTimestampImpl) ev.getAttributes().get("time:timestamp");
						((XAttributeTimestampImpl) ev.getAttributes().get("time:timestamp")).setValueMillis(timestamp.getValueMillis() + eventIndex);
						// add trace attributes to event
						String eventName = XConceptExtension.instance().extractName(ev);
						ev.getAttributes().putAll(t.getAttributes());
						XConceptExtension.instance().assignName(ev,eventName);
						// Add event to trace
						t1.add(ev);

						System.out.println("Writing to socket Event : " + ev.getAttributes().get("concept:name"));
						String packet = osxmlConverter.toXML(t1).replace('\n', ' ');
						System.out.println(packet);
						writeOnTheSocket.println(packet);
						writeOnTheSocket.flush();

						Thread.sleep(times);
					}
				}
			} else if (monitoringMethod == MonitoringMethod.PROBDECLARE) {
				writeOnTheSocket.println(model);
				writeOnTheSocket.println("</model>");
				writeOnTheSocket.flush();

				for(XTrace t : log){
					int eventIndex = 0;

					for(XEvent ev : t){
						XTraceImpl t1 = new XTraceImpl(t.getAttributes());
						XAttributeTimestampImpl timestamp = (XAttributeTimestampImpl) ev.getAttributes().get("time:timestamp");
						((XAttributeTimestampImpl) ev.getAttributes().get("time:timestamp")).setValueMillis(timestamp.getValueMillis() + eventIndex);
						// add trace attributes to event
						String eventName = XConceptExtension.instance().extractName(ev);
						ev.getAttributes().putAll(t.getAttributes());
						XConceptExtension.instance().assignName(ev,eventName);
						// Add event to trace
						t1.add(ev);

						System.out.println("Writing to socket Event : " + ev.getAttributes().get("concept:name"));
						String packet = osxmlConverter.toXML(t1).replace('\n', ' ');
						System.out.println(packet);
						writeOnTheSocket.println(packet);
						writeOnTheSocket.flush();

						Thread.sleep(times);
					}
				}
			}

			//Signal that log streaming has ended
			System.out.println("Kirjutame: exit");
			writeOnTheSocket.println("exit");
			writeOnTheSocket.flush();

			socket.shutdownOutput();
			return;
		}
		catch (Exception ex){ex.printStackTrace();}

	}
}