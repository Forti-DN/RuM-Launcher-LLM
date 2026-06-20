package theFirst;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.operationalsupport.xml.OSXMLConverter;

import com.fluxicon.slickerbox.factory.SlickerFactory;

import controller.monitoring.MonitoringMethod;
import declare.DeclareParserException;
import it.unibo.ai.rec.common.TimeGranularity;
import it.unibo.ai.rec.engine.FluentsConverter;
import it.unibo.ai.rec.model.FluentsModel;
import it.unibo.ai.rec.model.HappenedEventSet;
import it.unibo.ai.rec.model.NoGroupingStrategy;
import it.unibo.ai.rec.model.RecTrace;
import it.unibo.ai.rec.visualization.BasicDateEventOutputter;
import it.unibo.ai.rec.visualization.FluentChartContainer;
import it.unibo.ai.rec.visualization.FluentChartFactory;
import it.unibo.ai.rec.visualization.FluentChartStandardPanel;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import core.monitoring.MonitorRunner;

public class MoBuConClient extends GridPane implements MouseListener{

	@FXML
	private ListView<InstanceInfo> instancesList = new ListView<>();

	@FXML
	private AnchorPane runTimeMonitorView;

	private JPanel roundedPanelHealth;

	JScrollPane scrollPane;
	boolean primo = true;

	private Color chartBackgroundColor = new Color(232, 232, 232);

	private boolean conflict = true; // Should get here somehow to set the parameter.

	private JList<Integer> diagnosticsList;
	private Hashtable<String, Vector<Integer>> violationsModels;
	private FluentChartFactory factory;
	private String someFluent;
	private FluentsConverter converter;
	private FluentChartContainer chartPanel;
	public static final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss:S";
	private Vector<InstanceInfo> instances;
	private Vector<String> instancesAll; // this is the instances vector - this is where you get it on the board.
	private static int PORT = 4444;// to logstreamer
	private Hashtable<Long, Double> healthHash;
	public static void setPORT(int pORT) {
		PORT = pORT;
		System.out.println("Port: " + PORT );
	}

	private static String HOST;
	private Hashtable<String, XTrace> partialTraces;
	private XTrace trace;
	private OSXMLConverter osxmlConverter = new OSXMLConverter();
	private Vector<String> instancesViol;
	private Hashtable<String, FluentChartContainer> queryProbabilityGraphs;
	private HashMap<String, AnchorPane> boundaryPanes;

	private String selected = "";

	private MonitoringMethod monitoringMethod;

	static {
		try {
			HOST = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
		}
	}

	MonitorRunner mr;

	public MoBuConClient(MonitoringMethod monitoringMethod) {
		this.monitoringMethod = monitoringMethod;
		System.out.println("Monitoring method: " + monitoringMethod);
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/MonitorView.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			factory = new FluentChartFactory(TimeGranularity.MILLIS, new BasicDateEventOutputter(
					TimeGranularity.MILLIS, DATE_FORMAT), false, "stateColors.properties", "fluentColors.properties");
			fxmlLoader.load();
		} catch (Exception e) {
			e.printStackTrace();
		}

		instancesAll = new Vector<>();
		instancesViol = new Vector<>();
		violationsModels = new Hashtable<>();
		diagnosticsList = new JList<>();
		final SwingNode swingNode1 = new SwingNode();
		queryProbabilityGraphs = new Hashtable<>();

		createFirstSwingContent(swingNode1);
		runTimeMonitorView.getChildren().setAll(swingNode1);
		AnchorPane.setBottomAnchor(swingNode1, 0.0);
		if (monitoringMethod == MonitoringMethod.PROBDECLARE) {
			boundaryPanes = new HashMap<String, AnchorPane>();
			AnchorPane initialBoundaryPane = createInitialBoundaryPane();
			boundaryPanes.put("", initialBoundaryPane);
			runTimeMonitorView.getChildren().add(initialBoundaryPane);
			AnchorPane.setTopAnchor(swingNode1, 85.0); //Swing content moved down so that boundaries can be shown
		} else {
			AnchorPane.setTopAnchor(swingNode1, 0.0);
		}
		AnchorPane.setLeftAnchor(swingNode1, 0.0);
		AnchorPane.setRightAnchor(swingNode1, 0.0);
		instancesList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<InstanceInfo>() {
			@Override
			public void changed(ObservableValue<? extends InstanceInfo> arg0, InstanceInfo arg1, InstanceInfo arg2) {
				if(arg2 == null) return;
				else {
					instancesSelectionChanged();
					System.out.println("New value: " + arg2.toString());
				}
			}
		});
		primo = true;
	}

	private void createSwingContent(final SwingNode swingNode){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				swingNode.setContent(roundedPanelHealth);
			}
		});
	}
	
	private void createSwingContent2(final SwingNode swingNode){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				roundedPanelHealth.removeAll();
				if (queryProbabilityGraphs.get(selected) != null) {
					roundedPanelHealth.add(queryProbabilityGraphs.get(selected));
				}
				if (violationsModels.get(selected) != null) {
					System.out.println("Here in 2nd if");
					diagnosticsList.setModel(new ViolationListModel<Integer>(violationsModels.get(selected)));
				} else {
					diagnosticsList.setModel(new ViolationListModel<Integer>(new Vector<Integer>()));
				}
				swingNode.setContent(roundedPanelHealth);
			}
		});
	}
	private void createFirstSwingContent(final SwingNode swingNode) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				roundedPanelHealth = SlickerFactory.instance().createRoundedPanel(50, chartBackgroundColor);
				roundedPanelHealth.setLayout(new BoxLayout(roundedPanelHealth, BoxLayout.LINE_AXIS));
				swingNode.setContent(roundedPanelHealth);
			}
		});
	}

	private void sendRequest() {
		ProxyDaemon daemon;
		try {
			daemon = new ProxyDaemon(PORT,this,monitoringMethod);
			daemon.start();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void runMoBuConClient() {
		sendRequest();
	}

	private void updateOutput(String piID, String eventName, boolean isProbDeclare) {
		System.out.println("updateOutput piID: " + piID + " eventName: "+ eventName  );
		try {
			XTrace trace = partialTraces.get(piID);// Trace siit
			String fluent = someFluent;// String result from the matrix
			FluentsModel model = converter.toFluentsModel(fluent);
			Xes2RecTraceTranslator traceTranslator = new Xes2RecTraceTranslator(
					it.unibo.ai.rec.common.TimeGranularity.MILLIS, Xes2RecTraceTranslator.TimestampStrategy.ABSOLUTE);
			RecTrace rtrace = traceTranslator.translate(trace);

			if (isProbDeclare) {
				for (HappenedEventSet eventSet : rtrace.getHappenedEvents()) {
					for (int i = 0; i < eventSet.getEvents().size(); i++) {
						String event = eventSet.getEvents().get(i);
						event = event.replace("with data ", "");
						eventSet.getEvents().set(i, event);
					}
				}
			}

			chartPanel = new FluentChartStandardPanel(factory);

			chartPanel.update(rtrace, model);
			chartPanel.getChartPanel().setOpaque(true);
			chartPanel.getChartPanel().setBackground(Color.white);
			if (primo) {
				roundedPanelHealth.add(chartPanel);
				primo = false;
			}
			chartPanel.setAlignmentY(Component.TOP_ALIGNMENT);
			queryProbabilityGraphs.put(piID, chartPanel);
			if (selected.equals(piID)) {
				SwingNode swingNode2 = new SwingNode();
				//createSwingContent(swingNode2);
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						runTimeMonitorView.getChildren().setAll(swingNode2);
						AnchorPane.setBottomAnchor(swingNode2, 0.0);
						if (monitoringMethod == MonitoringMethod.PROBDECLARE) {
							AnchorPane.setTopAnchor(swingNode2, 85.0);
						} else {
							AnchorPane.setTopAnchor(swingNode2, 0.0);
						}
						AnchorPane.setLeftAnchor(swingNode2, 0.0);
						AnchorPane.setRightAnchor(swingNode2, 0.0);
						swingNode2.setContent(roundedPanelHealth);
					}
				});

				//Thread.sleep(1000);

			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	private AnchorPane createInitialBoundaryPane() {
		AnchorPane initialBoundaryPane = new AnchorPane();

		Label l = new Label();
		AnchorPane.setTopAnchor(l, 0.0);
		l.setText("sat: ");
		l.setMinWidth(85);
		l.setMaxHeight(20);
		l.setMinHeight(20);
		l.setAlignment(Pos.CENTER_RIGHT);
		l.setStyle("-fx-font-weight: bold");
		initialBoundaryPane.getChildren().add(l);
		l = new Label();
		AnchorPane.setTopAnchor(l, 20.0);
		l.setText("poss.sat: ");
		l.setMinWidth(85);
		l.setMaxHeight(20);
		l.setMinHeight(20);
		l.setAlignment(Pos.CENTER_RIGHT);
		l.setStyle("-fx-font-weight: bold");
		initialBoundaryPane.getChildren().add(l);
		l = new Label();
		AnchorPane.setTopAnchor(l, 40.0);
		l.setText("poss.viol: ");
		l.setMinWidth(85);
		l.setMaxHeight(20);
		l.setMinHeight(20);
		l.setAlignment(Pos.CENTER_RIGHT);
		l.setStyle("-fx-font-weight: bold");
		initialBoundaryPane.getChildren().add(l);
		l = new Label();
		AnchorPane.setTopAnchor(l, 60.0);
		l.setText("viol: ");
		l.setMinWidth(85);
		l.setMaxHeight(20);
		l.setMinHeight(20);
		l.setAlignment(Pos.CENTER_RIGHT);
		l.setStyle("-fx-font-weight: bold");
		initialBoundaryPane.getChildren().add(l);

		AnchorPane.setBottomAnchor(initialBoundaryPane, 0.0);
		AnchorPane.setTopAnchor(initialBoundaryPane, 0.0);
		AnchorPane.setLeftAnchor(initialBoundaryPane, 0.0);
		AnchorPane.setRightAnchor(initialBoundaryPane, 0.0);

		return initialBoundaryPane;
	}

	private void updateBoundaryPane(String piID, HashMap<String, ArrayList<Double>> currentBoundaries) {
		AnchorPane boundaryPane = boundaryPanes.get(piID);
		int eventNum = partialTraces.get(piID).size();
		double leftAncor = 85 + (eventNum-1) * 60.0;
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Label l =  createBoundaryLabel(currentBoundaries.get("sat"));
				AnchorPane.setTopAnchor(l, 0.0);
				AnchorPane.setLeftAnchor(l, leftAncor);
				boundaryPane.getChildren().add(l);

				l =  createBoundaryLabel(currentBoundaries.get("poss.sat"));
				AnchorPane.setTopAnchor(l, 20.0);
				AnchorPane.setLeftAnchor(l, leftAncor);
				boundaryPane.getChildren().add(l);

				l =  createBoundaryLabel(currentBoundaries.get("poss.viol"));
				AnchorPane.setTopAnchor(l, 40.0);
				AnchorPane.setLeftAnchor(l, leftAncor);
				boundaryPane.getChildren().add(l);

				l =  createBoundaryLabel(currentBoundaries.get("viol"));
				AnchorPane.setTopAnchor(l, 60.0);
				AnchorPane.setLeftAnchor(l, leftAncor);
				boundaryPane.getChildren().add(l);
			}
		});
	}

	private static Label createBoundaryLabel(ArrayList<Double> boundary) {
		Label l = new Label();
		l.setMaxWidth(60);
		l.setMinWidth(60);
		l.setMaxHeight(20);
		l.setMinHeight(20);
		l.setAlignment(Pos.CENTER);
		Double min = boundary.get(0);
		Double max = boundary.get(1);
		l.setText(min + "-" + max);

		String styleString = "-fx-text-fill: white;";

		if (min != 0.0 || max !=0.0) {
			styleString = styleString + " -fx-font-weight: bold;";
		}

		Double average = (min+max)/2;
		int colorCode = (int) Math.round((1-average)*140+60);
		styleString = styleString + " -fx-background-color: rgb(" + colorCode + "," + colorCode + "," + colorCode +");";
		l.setStyle(styleString);
		return l;


		//Hetkel on taust 244,244,244
		//Graafikut ümbritsev kast on 232,232,232
	}

	public class ProxySimulator extends Thread {

		private final Socket socket;
		private final OutputStream outStream;
		private final InputStream inStream;
		private MonitoringMethod monitoringMethod;
		public ProxySimulator(final Socket socket, final OutputStream outStream, final InputStream inStream, MonitoringMethod monitoringMethod) {
			super("Proxy Simulator");
			setDaemon(true);
			this.outStream = outStream;
			this.inStream = inStream;
			this.socket = socket;
			this.monitoringMethod = monitoringMethod;
		}
		@Override
		public void run() {
			switch (monitoringMethod) {
			case MP_DECLARE_ALLOY:
				runNonFlloat(true);
			case MOBUCON_LTL:
				runNonFlloat(false);
				break;
			case ONLINE_DECLARE:
				runNonFlloat(false);
				break;
			case FLLOAT:
				runFlloat(false);
				break;
			case PROBDECLARE:
				runFlloat(true);
				break;
			default:
				System.out.println("Unknown monitoring method");
				break;
			}
		}

		private void runFlloat(boolean isProbDeclare) {
			BufferedReader in = null;
			PrintWriter out = null;
			String[] completeSetEvents = null;
			partialTraces = new Hashtable<>();
			healthHash = new Hashtable<>();
			in = new BufferedReader(new InputStreamReader(inStream));
			out = new PrintWriter(outStream,true);
			String letto = null;
			File message = null;

			try {
				while (true) {
					letto = in.readLine();
					try (Socket socket = new Socket(HOST, 9875)) {
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						System.out.println("Sending request to Socket Server: " + letto);
						oos.writeObject(letto);
						if (letto.equals("END_WEIGHTS") || letto.equals("</model>")) {
							break;
						}
					}
				}

				completeSetEvents = new String[0];
				message = File.createTempFile("message", ".xml");
				message.deleteOnExit();
				try (PrintWriter printmessage = new PrintWriter(new FileWriter(message))) {
					letto = in.readLine();
					System.out.println(letto);
	
					while (!letto.contains("</org.deckfour.xes.model.impl.XTraceImpl>")) {
						letto = in.readLine();
						System.out.println(letto);
						printmessage.println(letto);
					}
					printmessage.println(letto);
					trace = (XTrace) osxmlConverter.fromXML(letto);// Letto should still be the original trace.
					System.out.println("Letto: " + letto);
					System.out.println("Tracesize after letto: " + trace.size());
	
					printmessage.flush();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			while(message != null && trace != null){
				XTrace partialTrace;
				String eventName = "";// Mine event name here
				long timestamp = 0;
				String piID = "";
				long old = -1;
				XEvent completeEvent = new XEventImpl();
				for(XEvent e : trace) {
					//XAttributeMap eventAttributeMap = e.getAttributes();
					completeEvent = e;
					eventName = XConceptExtension.instance().extractName(e).replaceAll(" ", "_");
					//String ts = eventAttributeMap.get(XLifecycleExtension.KEY_TRANSITION).getAttributes().toString();
					long current = XTimeExtension.instance().extractTimestamp(e).getTime();
					if (current <= old) {
						old = old + 1;
					} else {
						old = current;
					}
					timestamp = old;
					piID = XConceptExtension.instance().extractName(trace);
				}
				if (partialTraces.containsKey(piID)) {
					partialTrace = partialTraces.get(piID);
				} else {
					partialTrace = new XTraceImpl(new XAttributeMapImpl());
					if (!instancesAll.contains(piID)) {
						instancesAll.add(piID);
					}
				}

				converter = new FluentsConverter(new NoGroupingStrategy());
				partialTrace.add(completeEvent);
				partialTraces.put(piID, partialTrace);

				try (Socket socket = new Socket(HOST, 9875)) {
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

					String packet = "<?xml version=\"1.0\"?><Entry><ProcessID>0</ProcessID><ProcessInstanceID>"+XConceptExtension.instance().extractName(trace)+"</ProcessInstanceID><ModelID>Municipalities</ModelID><WorkflowModelElement>"+XConceptExtension.instance().extractName(trace.get(0))+"</WorkflowModelElement><Timestamp>"+XTimeExtension.instance().extractTimestamp(trace.get(0)).getTime()+"</Timestamp><EventType>complete</EventType></Entry>";
					System.out.println("Sending request to Socket Server: " + packet);
					oos.writeObject(packet);
					ObjectInputStream ois = null;
					ois = new ObjectInputStream(socket.getInputStream());
					Map<String, Object> analysis = (Map<String, Object>) ois.readObject(); //TODO: Check if the whole analysis object is needed here
					System.out.println("Received response: " + analysis.toString());
					if (isProbDeclare) {
						if (boundaryPanes.get(piID)==null) {
							boundaryPanes.put(piID, createInitialBoundaryPane());
						}
						updateBoundaryPane(piID, (HashMap<String, ArrayList<Double>>) analysis.get("currentBoundaries"));

						System.out.println("CurrentBoundaries are: " + analysis.get("currentBoundaries").toString());
					}
					someFluent = (String) analysis.get("fluents");

					if (!isProbDeclare) {
						//Not sure what this health check does, might be unnecessary
						double health = ((Double)analysis.get("health")).doubleValue();
						healthHash.put(timestamp,health);
						if (health < 1) {
							if (!instancesViol.contains(piID)) {
								instancesViol.add(piID);
							}
						} else {
							if (instancesViol.contains(piID)) {
								instancesViol.remove(piID);
							}
						}
					}

					//Removed showA check from here
					instances = new Vector<>();
					for (int i = 0; i < instancesAll.size(); i++) {
						InstanceInfo ii = new InstanceInfo();
						ii.setId((String) instancesAll.get(i));
						System.out.println("ii" + ii);
						if (instancesViol.contains(instancesAll.get(i))) {
							ii.setViolated(true);
						} else {
							ii.setViolated(false);
						}
						instances.add(ii);
					}


					Platform.runLater(new Runnable() {
						@Override public void run() {
							ObservableList<Integer> selectedIndices = instancesList.getSelectionModel().getSelectedIndices();
							int index = -1;
							if (selectedIndices.size() > 0) {
								index = selectedIndices.get(0);
							}
							instancesList.getItems().clear();
							instancesList.setItems(FXCollections.observableList(instances));
							instancesList.getSelectionModel().select(index);
						}
					});

					Vector<Integer> vv = violationsModels.get(piID);
					String diags = "Observed   " + eventName;
					int oldSize;
					if (vv == null) {
						oldSize = 0;
					} else {
						oldSize = vv.size();
					}

					if (!someFluent.equals("Nothing")) {
						updateOutput(piID, eventName, isProbDeclare);// TODO We get to about here
					}

					vv = violationsModels.get(piID);
					int newSize;
					if (vv == null) {
						newSize = 0;
					} else {
						newSize = vv.size();
					}
					if (oldSize == newSize) {
						out.println("");
					} else {
						String positive = (String)analysis.get("positive");
						if (!positive.isEmpty()) {
							if (positive != null) {
								String[] positivesForSize = positive.split(",");
								if (positivesForSize.length == 1) {
									diags = diags + ",   while for this reference model expecting   "
											+ positivesForSize[0];
								} else {
									String posList = positive.replace(",", ", ");
									int indCo = posList.lastIndexOf(",");
									String sub = posList.substring(indCo);
									posList = posList
											.replaceFirst(sub, sub.replaceFirst(", ", ", or "));
									diags = diags + ",   while for this reference model expecting   "
											+ posList;
								}
							}
						}
						String[] negativeSet = null;
						String negative = (String)analysis.get("negative");;
						if (!negative.isEmpty()) {
							if (negative != null) {
								negativeSet = negative.split(",");
							}
						}
						String[] positiveSet = positive.split(",");
						Vector<String> ve = new Vector<>();
						for (int i = 0; i < positiveSet.length; i++) {
							ve.add(positiveSet[i]);
						}
						if (negativeSet != null) {
							if (negativeSet.length >= 1) {
								Vector<String> vecCom = new Vector<>();
								for (int g = 0; g < completeSetEvents.length; g++) {
									vecCom.add(completeSetEvents[g]);
								}
								Vector<String> vecNeg = new Vector<>();
								for (int g = 0; g < negativeSet.length; g++) {
									vecNeg.add(negativeSet[g]);
								}
								for (int g = 0; g < completeSetEvents.length; g++) {
									if (vecNeg.contains(completeSetEvents[g])) {
										vecCom.remove(completeSetEvents[g]);
									}
								}
								diags = diags + "   (or otherwise everything different from   ";
								String last = null;
								if (vecNeg.size() == 1) {
									diags = diags + vecNeg.get(0) + ")";
								} else {
									for (int g = 0; g < vecNeg.size(); g++) {
										if (!ve.contains(vecNeg.get(g))) {
											diags = diags + vecNeg.get(g) + ", ";
											last = (String) vecNeg.get(g);
										}
									}
									diags = diags.replace(", " + last + ", ", " and " + last + ")");
								}
							}
						}
						out.println(diags);
					}
					if (selected.equals(piID)) {
						roundedPanelHealth.removeAll();
						System.out.println("Hereeeee!!");
						roundedPanelHealth.add(queryProbabilityGraphs.get(selected));
						diagnosticsList.setModel(new ViolationListModel<Integer>(violationsModels.get(selected)));
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				try (Socket socket = new Socket(HOST, 9875)) {
					message.delete();
					message = File.createTempFile("message", ".xml");
					message.deleteOnExit();
					PrintWriter printmessage = new PrintWriter(new FileWriter(message));
					letto = in.readLine();
					if(letto == null || letto.isEmpty() || letto.equalsIgnoreCase("exit")){
						message = null;
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						System.out.println("Sending request to Socket Server: " + letto);
						oos.writeObject(letto);
					}else{
						while(!letto.contains("</org.deckfour.xes.model.impl.XTraceImpl>")){
							printmessage.println(letto);
						}
						printmessage.println(letto);
						trace = (XTrace) osxmlConverter.fromXML(letto);
						printmessage.flush();
						printmessage.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}


		private void runNonFlloat (boolean integratedMethod) {
			if (integratedMethod) {
				BufferedReader in = null;
				String referenceDecl = null;
				PrintWriter out = null;
				String[] completeSetEvents = null;
				partialTraces = new Hashtable<>();
				in = new BufferedReader(new InputStreamReader(inStream));
				out = new PrintWriter(outStream, true);
				String letto = null;
				//File message = null;
				
				try {
					referenceDecl = in.readLine() + "\n";
					while (!referenceDecl.contains("</model>")) {
						referenceDecl = referenceDecl + in.readLine() + "\n";

					}
					
					//				Socket socket = new Socket(HOST, 9875);
					//				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					//				oos.writeObject("model");// Send model to myServer
					//				oos = new ObjectOutputStream(socket.getOutputStream());
					//				System.out.println("Sending request to Socket Server");
					//				oos.writeObject(referenceDecl);
					System.out.println("Writing model");
					mr = new MonitorRunner(conflict, referenceDecl); // Maybe some sort of error thingy here.

					completeSetEvents = new String[0];
					letto = in.readLine();

					while (!letto.contains("</org.deckfour.xes.model.impl.XTraceImpl>")) {
						letto = in.readLine();
					}
					trace = (XTrace) osxmlConverter.fromXML(letto);// Letto should still be the original trace.
					System.out.println("Letto: " + letto);
					System.out.println("Tracesize after letto: " + trace.size());

				} catch (IOException | DeclareParserException e1) {
					e1.printStackTrace();
					System.out.println("BAAAAAAD");
				}

				while (trace != null) {
					try {
						XTrace partialTrace;
						String eventName = "";// Mine event name here
						String piID = "";
						long old = -1;
						XEvent completeEvent = new XEventImpl();
						for(XEvent e : trace) {
							completeEvent = e;
							eventName = XConceptExtension.instance().extractName(e).replaceAll(" ", "_");
							long current = XTimeExtension.instance().extractTimestamp(e).getTime();
							if (current <= old) {
								old = old + 1;
							} else {
								old = current;
							}
							piID = XConceptExtension.instance().extractName(trace);
						}
						if (partialTraces.containsKey(piID)) {
							partialTrace = partialTraces.get(piID);
						} else {
							partialTrace = new XTraceImpl(new XAttributeMapImpl());
							if (!instancesAll.contains(piID)) {
								instancesAll.add(piID);
							}
						}
						trace.get(0);
						converter = new FluentsConverter(new NoGroupingStrategy());

						partialTrace.add(completeEvent);
						partialTraces.put(piID, partialTrace);
						
						//					Socket socket = new Socket(HOST, 9875);
						//					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						//					oos.writeObject("trace");// Send trace to myServer
						//					oos = new ObjectOutputStream(socket.getOutputStream());
						//					System.out.println("Sending request to Socket Server");
						//					oos.writeObject(letto);
						//					ObjectInputStream ois = null;
						//					ois = new ObjectInputStream(socket.getInputStream());
						//					someFluent = (String) ois.readObject();

						try {
							someFluent  = mr.setTrace(letto);
							System.out.println("Somefluent: " + someFluent);
							if (someFluent == null) {
								throw new InterruptedException();
							}
						}catch(InterruptedException error) {
							Platform.runLater(new Runnable() {
								@Override public void run() {
									Alert alert = new Alert(AlertType.ERROR);
									alert.setTitle("Oops");
									alert.setContentText("Something went wrong, make sure that your model and log match");
								}
							});


							//Platform.runLater(new Runnable() {
							//	@Override
							//	public void run() {
							//		Alert alert = new Alert(AlertType.ERROR);
							//		alert.setTitle("Oops!");
							//		alert.setContentText("Something went wrong, make sure your log and model match!");
							//   	}
							//	});

							Runtime.getRuntime().addShutdownHook(new Thread(){@Override
								public void run(){
								try {
									socket.close();
									System.out.println("The server is shut down!");
								} catch (IOException e) { /* failed */ }
							}});
							return;
						}

						//Removed showA check from here
						instances = new Vector<>();
						for (int i = 0; i < instancesAll.size(); i++) {
							InstanceInfo ii = new InstanceInfo();
							ii.setId((String) instancesAll.get(i));
							System.out.println("ii" + ii);
							if (instancesViol.contains(instancesAll.get(i))) {
								ii.setViolated(true);
							} else {
								ii.setViolated(false);
							}
							instances.add(ii);
						}

						Platform.runLater(new Runnable() {
							@Override public void run() {
								ObservableList<Integer> selectedIndices = instancesList.getSelectionModel().getSelectedIndices();
								int index = -1;
								if (selectedIndices.size() > 0) {
									index = selectedIndices.get(0);
								}
								instancesList.getItems().clear();
								instancesList.setItems(FXCollections.observableList(instances));
								instancesList.getSelectionModel().select(index);
							}
						});
						Thread.sleep(1000);
						Vector<Integer> vv = violationsModels.get(piID);
						String diags = "Observed   " + eventName;
						int oldSize;
						if (vv == null) {
							oldSize = 0;
						} else {
							oldSize = vv.size();
						}
						if (!someFluent.equals("Nothing")) {
							updateOutput(piID, eventName, false);// TODO We get to about here
						}
						vv = violationsModels.get(piID);
						int newSize;
						if (vv == null) {
							newSize = 0;
						} else {
							newSize = vv.size();
						}
						if (oldSize == newSize) {
							out.println("");
						} else {
							String positive = "";// no need for this
							if (!positive.isEmpty()) {
								if (positive != null) {
									String[] positivesForSize = positive.split(",");
									if (positivesForSize.length == 1) {
										diags = diags + ",   while for this reference model expecting   "
												+ positivesForSize[0];
									} else {
										String posList = positive.replace(",", ", ");
										int indCo = posList.lastIndexOf(",");
										String sub = posList.substring(indCo);
										posList = posList
												.replaceFirst(sub, sub.replaceFirst(", ", ", or "));
										diags = diags + ",   while for this reference model expecting   "
												+ posList;
									}
								}
							}
							String[] negativeSet = null;
							String negative = "";
							if (!negative.isEmpty()) {
								if (negative != null) {
									negativeSet = negative.split(",");
								}
							}
							String[] positiveSet = positive.split(",");
							Vector<String> ve = new Vector<>();
							for (int i = 0; i < positiveSet.length; i++) {
								ve.add(positiveSet[i]);
							}
							if (negativeSet != null) {
								if (negativeSet.length >= 1) {
									Vector<String> vecCom = new Vector<>();
									for (int g = 0; g < completeSetEvents.length; g++) {
										vecCom.add(completeSetEvents[g]);
									}
									Vector<String> vecNeg = new Vector<>();
									for (int g = 0; g < negativeSet.length; g++) {
										vecNeg.add(negativeSet[g]);
									}
									for (int g = 0; g < completeSetEvents.length; g++) {
										if (vecNeg.contains(completeSetEvents[g])) {
											vecCom.remove(completeSetEvents[g]);
										}
									}
									diags = diags + "   (or otherwise everything different from   ";
									String last = null;
									if (vecNeg.size() == 1) {
										diags = diags + vecNeg.get(0) + ")";
									} else {
										for (int g = 0; g < vecNeg.size(); g++) {
											if (!ve.contains(vecNeg.get(g))) {
												diags = diags + vecNeg.get(g) + ", ";
												last = (String) vecNeg.get(g);
											}
										}
										diags = diags.replace(", " + last + ", ", " and " + last + ")");
									}
								}
							}
							out.println(diags);
						}
						if (selected.equals(piID)) {
							roundedPanelHealth.removeAll();
							System.out.println("Hereeeee!!");
							diagnosticsList.setModel(new ViolationListModel<Integer>(violationsModels.get(selected)));
						}
					}catch (Exception e) {
						e.printStackTrace();
					}
					try {
						letto = in.readLine();
						System.out.println("letto: ");
						System.out.println(letto);
						if (letto == null || letto.isEmpty() || letto.equalsIgnoreCase("exit")) {
							//message = null;
							trace = null;
							System.out.println("Connection closed by log streamer");
							//						Socket socket = null;
							//						ObjectOutputStream oos = null;
							//						socket = new Socket(HOST, 9875);
							//						oos = new ObjectOutputStream(socket.getOutputStream());
							//						oos.writeObject("exit");// Send trace to myServer

						} else {
							trace = (XTrace) osxmlConverter.fromXML(letto);
						}
					}catch (IOException e) {
						//message = null;
						trace = null;
						e.printStackTrace();
					}
				}
				if (someFluent.equals("nothing")) {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Oops!");
					alert.setContentText("Something went wrong, make sure your log and model match!");
				}

			} else {
				BufferedReader in = null;
				String referenceDecl = null;
				PrintWriter out = null;
				String[] completeSetEvents = null;
				partialTraces = new Hashtable<>();
				in = new BufferedReader(new InputStreamReader(inStream));
				out = new PrintWriter(outStream, true);
				String letto = null;
				File message = null;
				try (Socket socket = new Socket(HOST, 9875)) {
					referenceDecl = in.readLine() + "\n";
					while (!referenceDecl.contains("</model>")) {
						referenceDecl = referenceDecl + in.readLine() + "\n";
					}
					
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject("model");// Send model to myServer
					oos = new ObjectOutputStream(socket.getOutputStream());
					System.out.println("Sending request to Socket Server");
					oos.writeObject(referenceDecl);

					completeSetEvents = new String[0];
					message = File.createTempFile("message", ".xml");
					message.deleteOnExit();
					
					try (PrintWriter printmessage = new PrintWriter(new FileWriter(message))) {
						letto = in.readLine();
	
						while (!letto.contains("</org.deckfour.xes.model.impl.XTraceImpl>")) {
							letto = in.readLine();
							printmessage.println(letto);
						}
						printmessage.println(letto);
						trace = (XTrace) osxmlConverter.fromXML(letto);// Letto should still be the original trace.
						System.out.println("Letto: " + letto);
						System.out.println("Tracesize after letto: " + trace.size());
	
						printmessage.flush();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				while (message != null && trace != null) {
					try (Socket socket = new Socket(HOST, 9875)) {
						//String modelID = "MyModel";//here could actually somehow get the name of the model from the model that has been inserted.
						//String processInstanceID = XConceptExtension.instance().extractName(trace);

						XTrace partialTrace;
						String eventName = "";// Mine event name here
						//long timestamp = 0;
						String piID = "";
						long old = -1;
						XEvent completeEvent = new XEventImpl();
						for(XEvent e : trace) {
							//XAttributeMap eventAttributeMap = e.getAttributes();
							completeEvent = e;
							eventName = XConceptExtension.instance().extractName(e).replaceAll(" ", "_");
							//String ts = eventAttributeMap.get(XLifecycleExtension.KEY_TRANSITION).getAttributes().toString();
							long current = XTimeExtension.instance().extractTimestamp(e).getTime();
							if (current <= old) {
								old = old + 1;
							} else {
								old = current;
							}
							//timestamp = old;
							piID = XConceptExtension.instance().extractName(trace);
						}
						if (partialTraces.containsKey(piID)) {
							partialTrace = partialTraces.get(piID);
						} else {
							partialTrace = new XTraceImpl(new XAttributeMapImpl());
							if (!instancesAll.contains(piID)) {
								instancesAll.add(piID);
							}
						}
						trace.get(0);
						converter = new FluentsConverter(new NoGroupingStrategy());

						partialTrace.add(completeEvent);
						partialTraces.put(piID, partialTrace);
						
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						oos.writeObject("trace");// Send trace to myServer
						oos = new ObjectOutputStream(socket.getOutputStream());
						System.out.println("Sending request to Socket Server");
						oos.writeObject(letto);
						ObjectInputStream ois = null;
						ois = new ObjectInputStream(socket.getInputStream());
						someFluent = (String) ois.readObject();

						//Removed showA check from here
						instances = new Vector<>();
						for (int i = 0; i < instancesAll.size(); i++) {
							InstanceInfo ii = new InstanceInfo();
							ii.setId((String) instancesAll.get(i));
							System.out.println("ii" + ii);
							if (instancesViol.contains(instancesAll.get(i))) {
								ii.setViolated(true);
							} else {
								ii.setViolated(false);
							}
							instances.add(ii);
						}

						Platform.runLater(new Runnable() {
							@Override public void run() {
								ObservableList<Integer> selectedIndices = instancesList.getSelectionModel().getSelectedIndices();
								int index = -1;
								if (selectedIndices.size() > 0) {
									index = selectedIndices.get(0);
								}
								instancesList.getItems().clear();
								instancesList.setItems(FXCollections.observableList(instances));
								instancesList.getSelectionModel().select(index);
							}
						});
						Vector<Integer> vv = violationsModels.get(piID);
						String diags = "Observed   " + eventName;
						int oldSize;
						if (vv == null) {
							oldSize = 0;
						} else {
							oldSize = vv.size();
						}
						if (!someFluent.equals("Nothing")) {
							updateOutput(piID, eventName, false);// TODO We get to about here
						}
						vv = violationsModels.get(piID);
						int newSize;
						if (vv == null) {
							newSize = 0;
						} else {
							newSize = vv.size();
						}
						if (oldSize == newSize) {
							out.println("");
						} else {
							String positive = "";// no need for this
							if (!positive.isEmpty()) {
								if (positive != null) {
									String[] positivesForSize = positive.split(",");
									if (positivesForSize.length == 1) {
										diags = diags + ",   while for this reference model expecting   "
												+ positivesForSize[0];
									} else {
										String posList = positive.replace(",", ", ");
										int indCo = posList.lastIndexOf(",");
										String sub = posList.substring(indCo);
										posList = posList
												.replaceFirst(sub, sub.replaceFirst(", ", ", or "));
										diags = diags + ",   while for this reference model expecting   "
												+ posList;
									}
								}
							}
							String[] negativeSet = null;
							String negative = "";
							if (!negative.isEmpty()) {
								if (negative != null) {
									negativeSet = negative.split(",");
								}
							}
							String[] positiveSet = positive.split(",");
							Vector<String> ve = new Vector<>();
							for (int i = 0; i < positiveSet.length; i++) {
								ve.add(positiveSet[i]);
							}
							if (negativeSet != null) {
								if (negativeSet.length >= 1) {
									Vector<String> vecCom = new Vector<>();
									for (int g = 0; g < completeSetEvents.length; g++) {
										vecCom.add(completeSetEvents[g]);
									}
									Vector<String> vecNeg = new Vector<>();
									for (int g = 0; g < negativeSet.length; g++) {
										vecNeg.add(negativeSet[g]);
									}
									for (int g = 0; g < completeSetEvents.length; g++) {
										if (vecNeg.contains(completeSetEvents[g])) {
											vecCom.remove(completeSetEvents[g]);
										}
									}
									diags = diags + "   (or otherwise everything different from   ";
									String last = null;
									if (vecNeg.size() == 1) {
										diags = diags + vecNeg.get(0) + ")";
									} else {
										for (int g = 0; g < vecNeg.size(); g++) {
											if (!ve.contains(vecNeg.get(g))) {
												diags = diags + vecNeg.get(g) + ", ";
												last = (String) vecNeg.get(g);
											}
										}
										diags = diags.replace(", " + last + ", ", " and " + last + ")");
									}
								}
							}
							out.println(diags);
						}
						if (selected.equals(piID)) {
							roundedPanelHealth.removeAll();
							System.out.println("Hereeeee!!");
							diagnosticsList.setModel(new ViolationListModel<Integer>(violationsModels.get(selected)));
						}
					}catch (Exception e) {
						e.printStackTrace();
					}
					try (Socket socket = new Socket(HOST, 9875)) {
						message.delete();
						message = File.createTempFile("message", ".xml");
						message.deleteOnExit();
						PrintWriter printmessage = new PrintWriter(new FileWriter(message));
						letto = in.readLine();
						System.out.println("letto: ");
						System.out.println(letto);
						if (letto == null || letto.isEmpty() || letto.equalsIgnoreCase("exit")) {
							message = null;
							trace = null;
							System.out.println("Connection closed by log streamer");
							ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
							oos.writeObject("exit");// Send trace to myServer

						} else {
							while (!letto.contains("</org.deckfour.xes.model.impl.XTraceImpl>")) {
								printmessage.println(letto);
							}
							printmessage.println(letto);
							trace = (XTrace) osxmlConverter.fromXML(letto);
							printmessage.flush();
							printmessage.close();
						}
					} catch (IOException e) {
						message = null;
						trace = null;
						e.printStackTrace();
					}
				}
			}
		}
	}
	protected void instancesSelectionChanged() {
		ObservableList<Integer> selectedIndices = instancesList.getSelectionModel().getSelectedIndices();
		if (selectedIndices.size() > 0) {
			selected =((InstanceInfo) instances.get(selectedIndices.get(0))).getId();
			final SwingNode swingNode = new SwingNode();
			createSwingContent2(swingNode);
			runTimeMonitorView.getChildren().setAll(swingNode);
			AnchorPane.setBottomAnchor(swingNode, 0.0);
			if (monitoringMethod == MonitoringMethod.PROBDECLARE) {
				for (AnchorPane bp : boundaryPanes.values()) {
					runTimeMonitorView.getChildren().remove(bp);
				}
				runTimeMonitorView.getChildren().add(boundaryPanes.get(selected));
				AnchorPane.setTopAnchor(swingNode, 85.0);
			} else {
				AnchorPane.setTopAnchor(swingNode, 0.0);
			}
			AnchorPane.setLeftAnchor(swingNode, 0.0);
			AnchorPane.setRightAnchor(swingNode, 0.0);
		}
	}


	public void setConflict(boolean conflict) {
		this.conflict = conflict;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}
}
