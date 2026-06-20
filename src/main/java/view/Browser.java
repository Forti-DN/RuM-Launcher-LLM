package view;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import graph.VisEdge;
import graph.VisGraph;
import javafx.concurrent.Worker;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import util.ConstraintTemplate;

public class Browser extends Pane {

    private final WebView browser = new WebView();
    private final WebEngine webEngine = browser.getEngine();
    private VisGraph graph;
    
    private Slider zoomSlider;
    
    private Map<Integer,String> activitiesMap;

	private String MPDeclareResults;

	public String getMPDeclareResults() {
		return MPDeclareResults;
	}

	public void setMPDeclareResults(String MPDeclareResults) {
		this.MPDeclareResults = MPDeclareResults;
	}
    
    public Map<Integer, String> getActivitiesMap() {
		return activitiesMap;
	}

	public void setActivitiesMap(Map<Integer, String> activitiesMap) {
		this.activitiesMap = activitiesMap;
	}

	public Map<Integer, Double> getActSuppMap() {
		return actSuppMap;
	}

	public void setActSuppMap(Map<Integer, Double> actSuppMap) {
		this.actSuppMap = actSuppMap;
	}

	public Map<Integer, ConstraintTemplate> getTemplatesMap() {
		return templatesMap;
	}

	public void setTemplatesMap(Map<Integer, ConstraintTemplate> templatesMap) {
		this.templatesMap = templatesMap;
	}

	public Map<Integer, List<String>> getConstraintParametersMap() {
		return constraintParametersMap;
	}

	public void setConstraintParametersMap(Map<Integer, List<String>> constraintParametersMap) {
		this.constraintParametersMap = constraintParametersMap;
	}

	public Map<Integer, Double> getConstraintSuppMap() {
		return constraintSuppMap;
	}

	public void setConstraintSuppMap(Map<Integer, Double> constraintSuppMap) {
		this.constraintSuppMap = constraintSuppMap;
	}
	
	public Slider getZoomSlider() {
		return zoomSlider;
	}
	public void setZoomSlider(Slider zoomSlider) {
		this.zoomSlider = zoomSlider;
	}

	private Map<Integer,Double> actSuppMap;
    private Map<Integer,ConstraintTemplate> templatesMap; 
    private Map<Integer,List<String>> constraintParametersMap; 
    private Map<Integer,Double> constraintSuppMap;

    public Browser(VisGraph g,double h,double w,Slider zoom) {
    	setZoomSlider(zoom);
        this.graph = g;
        //apply the styles
        getStyleClass().add("browser");
        // load the web page
        System.out.println("Loaded: "+(getClass().getClassLoader().getResource("baseGraph2.html")).toString());
        webEngine.load((getClass().getClassLoader().getResource("baseGraph2.html")).toString());
        //add the web view to the scene
        browser.setPrefHeight(h);
        browser.setPrefWidth(w);
        browser.addEventFilter(ScrollEvent.ANY, e -> {
        	double before = zoomSlider.getValue();
        	zoomSlider.setValue(before + e.getDeltaY()/4);
        	e.consume();
        });
        browser.setContextMenuEnabled(false);
        browser.addEventFilter(MouseEvent.ANY, e -> {
        	if(e.getButton() == MouseButton.SECONDARY) {
        		e.consume();
        	}
        });
        getChildren().add(browser);
        setGraph();

    }
    
    public Browser(VisGraph g,double h,double w,String resource) {
        this.graph = g;
        //apply the styles
        getStyleClass().add("browser");
        // load the web page
        //System.out.println("Loaded: "+(getClass().getClassLoader().getResource(resource)).toString());
        webEngine.load((getClass().getClassLoader().getResource(resource)).toString());
        //add the web view to the scene
        browser.setPrefHeight(h);
        browser.setPrefWidth(w);
        getChildren().add(browser);
        setGraph();

    }
    
    public Browser(double h, double w, String dotGraph, Slider zoom) {
    	setZoomSlider(zoom);
    	getStyleClass().add("browser");
    	System.out.println("Loading initial page...");
    	System.out.println("Before millis: "+System.currentTimeMillis());
        webEngine.load((getClass().getClassLoader().getResource("test.html")).toString());
        this.setPrefHeight(h);
        this.setPrefWidth(w);
        browser.setPrefHeight(h);
        browser.setPrefWidth(w);
        browser.addEventFilter(ScrollEvent.ANY, e -> {
        	double before = zoomSlider.getValue();
        	zoomSlider.setValue(before + e.getDeltaY()/4);
        	e.consume();
        });
        browser.setContextMenuEnabled(false);
        /*browser.addEventFilter(MouseEvent.ANY, e -> {
        	if(e.getButton() == MouseButton.SECONDARY) {
        		e.consume();
        	}
        });*/
        getChildren().add(browser);
        setDotGraph(dotGraph);
    }
    
    public Browser(double h, double w, String activities, String constraints, Slider zoom) {
    	setZoomSlider(zoom);
    	getStyleClass().add("browser");
        webEngine.load((getClass().getClassLoader().getResource("textualView.html")).toString());
        browser.setPrefHeight(h);
        browser.setPrefWidth(w);
        browser.addEventFilter(ScrollEvent.ANY, e -> {
        	double before = zoomSlider.getValue();
        	zoomSlider.setValue(before + e.getDeltaY()/4);
        	e.consume();
        });
        browser.setContextMenuEnabled(false);
        browser.addEventFilter(MouseEvent.ANY, e -> {
        	if(e.getButton() == MouseButton.SECONDARY) {
        		e.consume();
        	}
        });
        getChildren().add(browser);
        setText(activities,constraints);
    }

	public Browser(double h, double w, String activities, String constraints, String MPDeclareResults, Slider zoom) {
		setZoomSlider(zoom);
		getStyleClass().add("browser");
		webEngine.load((getClass().getClassLoader().getResource("textualView.html")).toString());
		browser.setPrefHeight(h);
		browser.setPrefWidth(w);
		browser.addEventFilter(ScrollEvent.ANY, e -> {
			double before = zoomSlider.getValue();
			zoomSlider.setValue(before + e.getDeltaY()/4);
			e.consume();
		});
		browser.setContextMenuEnabled(false);
		browser.addEventFilter(MouseEvent.ANY, e -> {
			if(e.getButton() == MouseButton.SECONDARY) {
				e.consume();
			}
		});
		getChildren().add(browser);
		//setText(activities,constraints);
		setText(activities, constraints, MPDeclareResults);
	}
    
    private void setText(String activities, String constraints) {
    	String script = "setText('"+activities+"','"+constraints+"')";
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == Worker.State.SUCCEEDED) {
            	webEngine.executeScript(script);
            }
            else System.out.println("It is not loaded successfully!");
        });
	}
	private void setText(String activities, String constraints, String MPDeclareResults) {
		String script = "setText2('"+activities+"','"+constraints+"','"+MPDeclareResults+"')";
		webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue == Worker.State.SUCCEEDED) {
				webEngine.executeScript(script);
			}
			else System.out.println("It is not loaded successfully!");
		});
	}

	private void setDotGraph(String dg) {
    	String script = "setModel('" + dg + "')";
    	JSObject window = (JSObject) webEngine.executeScript("window");
    	window.setMember("rum_application", this);
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == Worker.State.SUCCEEDED) {
            	System.out.println("Page loaded...");
            	System.out.println("After millis: "+System.currentTimeMillis());
            	System.out.println("Execute script: " + script);
            	webEngine.executeScript(script);      		
            	System.out.println("After executing millis: "+System.currentTimeMillis());
            }
            else System.out.println("It is not loaded successfully!");
        });
    }
	
	//Called form JavaScript
	public void addGraphClickHandlers() {
		Element graphRootElement = webEngine.getDocument().getElementById("graphRoot");
		for (int i = 0; i < graphRootElement.getChildNodes().getLength(); i++) {
			String id;
			if (graphRootElement.getChildNodes().item(i).getNodeName().equals("g")) {
				id = graphRootElement.getChildNodes().item(i).getAttributes().getNamedItem("id").getNodeValue();
				((EventTarget) graphRootElement.getChildNodes().item(i)).addEventListener("click", new EventListener() {
			        public void handleEvent(Event ev) {
			            System.out.println("clicked on " + id);
			        }
			    }, false);
			}
		}
	}
    
    public void addMetricLabel(String metric,Map<Integer,List<Double>> valuesMap) {
    	for(VisEdge edge : graph.getEdges()) {
    		if(metric.equals("Support")) {
				double metricValue = valuesMap.get(edge.getId()).get(0);
				edge.setLabel(String.format("%.2f%%", metricValue*100));
				edge.setWidth(6*metricValue);
			}
			else if(metric.equals("Confidence")) {
				System.out.println("new metric is this");
				double metricValue = valuesMap.get(edge.getId()).get(1);
				edge.setLabel(String.format("%.2f%%", metricValue*100));
				edge.setWidth(6*metricValue);
			}
			else if(metric.equals("Interest Factor")) {
				System.out.println("new metric is this");
				double metricValue = valuesMap.get(edge.getId()).get(2);
				edge.setLabel(String.format("%.2f%%", metricValue*100));
				edge.setWidth(6*metricValue);
			}
    	}
    	setGraph();
    }
    
    private void setGraph(){
        String script = "setTheData(" + graph.getNodesJson() +  "," + graph.getEdgesJson() + ")";
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == Worker.State.SUCCEEDED) {
            	webEngine.executeScript(script);
            }
            else System.out.println("It is not loaded successfully!");
        });
    }
    
    public WebView getBrowser() {
		return browser;
	}

    /*@Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(browser,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
    }*/
    
}
