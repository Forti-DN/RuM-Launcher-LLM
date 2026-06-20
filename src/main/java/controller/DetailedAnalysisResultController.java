package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.XAttributeMap;
import org.processmining.plugins.declareanalyzer.AnalysisResult;
import org.processmining.plugins.declareanalyzer.AnalysisSingleResult;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import util.GraphGenerator;

public class DetailedAnalysisResultController extends GridPane {

	private AnalysisResult ar;
	
	@FXML
	private ListView<String> tracesList;
	
	@FXML
	private ListView<String> constraintList;
	
	@FXML
	private AnchorPane detailedTraceView;
	
	@FXML
	private ChoiceBox<String> sortTraceChoice;
	
	@FXML
	private Label fulfillmentLabel;
	
	@FXML
	private Label violationLabel;
	
	@FXML
	private VBox statBox;
	
	private Map<String,XTrace> traceMap;
	
	private Map<String,List<Integer>> statsTable = new HashMap<String,List<Integer>>();
	
	public DetailedAnalysisResultController(AnalysisResult ar) {
		this.ar = ar;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/DetailedAnalysisResultView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	private Map<String,String> getConstraintConditions() {
		XTrace xt = ar.getTraces().stream().findFirst().get();
		Map<String,String> constraintConditions = new HashMap<String,String>();
		ar.getResults(xt).forEach(asr -> {
			StringBuilder sb = new StringBuilder();
			ConstraintDefinition cd = asr.getConstraint();
			sb.append(cd.getName()+": ");
			cd.getParameters().forEach(p -> {
				sb.append(cd.getBranches(p)+", ");
			});
			constraintConditions.put(sb.toString().replace(",", "").trim(), cd.getCondition().getText());
		});
		return constraintConditions;
	}
	
	private String statsForTrace(XTrace trace, String tname) {
		int activations = 0;
		int fulfillments = 0;
		int violations = 0;
		for(AnalysisSingleResult asr : ar.getResults(trace)) {
			activations += asr.getActivations().size();
			fulfillments += asr.getFulfilments().size();
			violations += asr.getViolations().size();
		}
		statsTable.put(tname, Arrays.asList(activations,fulfillments,violations));
		return activations+" activ.  "+violations+" viol.  "+fulfillments+" fulfil.";
	}
	
	@FXML
	public void initialize() {
		statBox.setVisible(false);

		sortTraceChoice.getItems().add("Activation number");
		sortTraceChoice.getItems().add("Fulfillment number");
		sortTraceChoice.getItems().add("Violation number");
		sortTraceChoice.getItems().add("Alphabetical");
		
		int i = 0;
		traceMap = new HashMap<String,XTrace>();
		for(XTrace xt : ar.getTraces()) {
			String tname = xt.getAttributes().get("concept:name") != null ? xt.getAttributes().get("concept:name").toString() : (i+1)+"";
			traceMap.put(tname, xt);
			tracesList.getItems().add(tname+"\n"+statsForTrace(xt,tname));
			i++;
		}
		
		Map<String,String> conditions = getConstraintConditions();
		for(String s: ar.getConstraints()) {
			constraintList.getItems().add(s);
		}
		
		constraintList.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			String t = tracesList.getSelectionModel().getSelectedItem();
			if(t != null && ov.getValue() != null) {
				Matcher mT = Pattern.compile("(.*)\\n").matcher(t);
				mT.find();
				showInTheTrace(mT.group(1),ov.getValue());
			}
		});
		
		tracesList.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			String t = constraintList.getSelectionModel().getSelectedItem();
			if (t != null && ov.getValue() != null) {
				Matcher mT = Pattern.compile("(.*)\\n").matcher(newV);
				mT.find();
				showInTheTrace(mT.group(1),t);
			}
		});
		sortTraceChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if(ov.getValue().equals("Activation number")) {
				tracesList.getItems().sort((s1,s2) -> {
					Matcher mT1 = Pattern.compile("(.*)\\n").matcher(s1);
					Matcher mT2 = Pattern.compile("(.*)\\n").matcher(s2);
					mT1.find(); mT2.find();
					Integer value1 = statsTable.get(mT1.group(1)).get(0);
					Integer value2 = statsTable.get(mT2.group(1)).get(0);
					return value2.compareTo(value1);
				});
			}
			if(ov.getValue().equals("Fulfillment number")) {
				tracesList.getItems().sort((s1,s2) -> {
					Matcher mT1 = Pattern.compile("(.*)\\n").matcher(s1);
					Matcher mT2 = Pattern.compile("(.*)\\n").matcher(s2);
					mT1.find(); mT2.find();
					Integer value1 = statsTable.get(mT1.group(1)).get(1);
					Integer value2 = statsTable.get(mT2.group(1)).get(1);
					return value2.compareTo(value1);
				});
			}
			if(ov.getValue().equals("Violation number")) {
				tracesList.getItems().sort((s1,s2) -> {
					Matcher mT1 = Pattern.compile("(.*)\\n").matcher(s1);
					Matcher mT2 = Pattern.compile("(.*)\\n").matcher(s2);
					mT1.find(); mT2.find();
					Integer value1 = statsTable.get(mT1.group(1)).get(2);
					Integer value2 = statsTable.get(mT2.group(1)).get(2);
					return value2.compareTo(value1);
				});
			}
			if(ov.getValue().equals("Alphabetical")) {
				tracesList.getItems().sort((s1,s2) -> {
					String name1 = s1.substring(0, s1.indexOf('\n'));
					String name2 = s2.substring(0, s2.indexOf('\n'));
					return name1.compareTo(name2);
				});
			}
		});
	}
	
	private void showInTheTrace(String trace, String constraint) {
		XTrace selected = traceMap.get(trace);
		//System.out.println("#############################");
		ar.getResults(selected).forEach(asr -> {
			String cname = asr.getConstraint().getCaption()+"\n"+asr.getConstraint().getCondition();
			if(cname.equals(constraint)) {
				//System.out.println("Activations: "+asr.getActivations());
				//System.out.println("Fulfillments: "+asr.getFulfilments());
				//System.out.println("Violations: "+asr.getViolations());
				List<XAttributeMap> activityOrderInTrace = new ArrayList<XAttributeMap>();
				asr.getTrace().forEach(xe -> {
					activityOrderInTrace.add(xe.getAttributes());
				});
				//System.out.println(activityOrderInTrace);
				statBox.setVisible(true);
				fulfillmentLabel.setText("  Fulfillments: "+asr.getFulfilments().size());
				violationLabel.setText("  Violations: "+asr.getViolations().size());
				TraceViewController trc = GraphGenerator.getTraceView(activityOrderInTrace, asr.getFulfilments(), asr.getViolations());
				detailedTraceView.getChildren().clear();
				detailedTraceView.getChildren().add(trc);
				AnchorPane.setBottomAnchor(trc, 10.0);
	    		AnchorPane.setTopAnchor(trc, 0.0);
	    		AnchorPane.setLeftAnchor(trc, 0.0);
	    		AnchorPane.setRightAnchor(trc, 0.0);
	    		return;
			}
		});
	}
}
