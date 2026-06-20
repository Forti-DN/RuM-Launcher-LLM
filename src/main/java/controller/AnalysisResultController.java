package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.declareanalyzer.AnalysisResult;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

public class AnalysisResultController extends AnchorPane{
	
	@FXML
	private TableView<Values> analysisResultView;
	
	@FXML
	private TableColumn<Values, String> constraintColumn;
	
	@FXML
	private TableColumn<Values, Integer> activationColumn;
	
	@FXML
	private TableColumn<Values, Integer> fulfillmentColumn;
	
	@FXML
	private TableColumn<Values, Integer> violationColumn;
	
	private AnalysisResult analysisResult;
	
	public AnalysisResultController(AnalysisResult analysisResult) {
		this.analysisResult = analysisResult;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/AnalysisResultView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	private Map<String,String> getConstraintConditions() {
		XTrace xt = analysisResult.getTraces().stream().findFirst().get();
		Map<String,String> constraintConditions = new HashMap<String,String>();
		analysisResult.getResults(xt).forEach(asr -> {
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
	
	@FXML
	public void initialize() {
		constraintColumn.setCellValueFactory(new PropertyValueFactory<Values,String>("constraint"));
		activationColumn.setCellValueFactory(new PropertyValueFactory<Values,Integer>("activations"));
		fulfillmentColumn.setCellValueFactory(new PropertyValueFactory<Values,Integer>("fulfillments"));
		violationColumn.setCellValueFactory(new PropertyValueFactory<Values,Integer>("violations"));
		//conflictColumn.setVisible(false);
		//conflictColumn.setCellValueFactory(new PropertyValueFactory<Values,Integer>("conflicts"));
		
		List<Values> allValues = new ArrayList<Values>();
		//Map<String,String> conditions = getConstraintConditions();
		for(String s:analysisResult.getConstraints()) {
			Values values = new Values(
					s,
					analysisResult.getActivations(s),
					analysisResult.getFulfilments(s),
					analysisResult.getViolations(s),
					analysisResult.getConflicts(s));
			allValues.add(values);
		}
		
		analysisResultView.getItems().addAll(allValues);
		System.out.println(analysisResult.getConstraints());
	}
}