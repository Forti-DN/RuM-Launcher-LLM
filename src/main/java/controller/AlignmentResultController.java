package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.visualization.DataAwareStepTypes;
import org.processmining.plugins.DeclareConformance.ResultReplayDeclare;
import org.processmining.plugins.dataawaredeclarereplayer.gui.AnalysisSingleResult;
import org.processmining.plugins.dataawaredeclarereplayer.result.AlignmentAnalysisResult;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import util.GraphGenerator;

public class AlignmentResultController extends GridPane{
	
	@FXML
	private ListView<String> traceList;
	
	@FXML
	private ListView<String> constraintList;
	
	@FXML
	private HBox differentDataStat;
	
	@FXML
	private VBox statBox;
	
	@FXML
	private Label greenLabel;
	
	@FXML
	private Label yellowLabel;
	
	@FXML
	private Label purpleLabel;
	
	@FXML
	private Label whiteLabel;
	
	@FXML
	private AnchorPane viewPane;
	
	@FXML
	private ChoiceBox<String> sortTraceChoice;
	
	private AlignmentAnalysisResult result;
	
	private ResultReplayDeclare result2;
	
	private XLog traces;
	
	public AlignmentResultController(AlignmentAnalysisResult result, XLog traces) {
		this.result = result;
		this.traces = traces;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/AlignmentResultView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public AlignmentResultController(ResultReplayDeclare result, XLog traces) {
		this.result2 = result;
		this.traces = traces;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/AlignmentResultView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	@FXML
	public void initialize() {
		statBox.setVisible(false);
		sortTraceChoice.getItems().addAll("Alphabetical","Fitness");
		sortTraceChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if (newV != null) {
				if (newV.equals("Alphabetical")) {
					traceList.getItems().sort((s1,s2) -> {
						String name1 = s1.substring(0, s1.indexOf('\n'));
						String name2 = s2.substring(0, s2.indexOf('\n'));
						return name1.compareTo(name2);
					});
				
				} else {
					traceList.getItems().sort((s1,s2) -> {
						Matcher mFitness1 = Pattern.compile("Fitness: (.*)").matcher(s1);
						Matcher mFitness2 = Pattern.compile("Fitness: (.*)").matcher(s2);
						if(mFitness1.find() && mFitness2.find()) {
							Float f1 = Float.valueOf(mFitness1.group(1));
							Float f2 = Float.valueOf(mFitness2.group(1));
							return f2.compareTo(f1);
						}
						else return 0;
					});
				}
			}
		});

		if (result != null) {
			Set<Alignment> set = result.getAlignments();
			Map<String,Alignment> map = new HashMap<>();
			set.forEach(a -> {
				String str = a.getTraceName() + "\n" + String.format("Fitness: %.2f", a.getFitness());
				map.put(str, a);
			});

			traceList.getItems().addAll(map.keySet());
			traceList.getItems().sort((s1,s2) -> {
				String caseName1 = s1.split("\n")[0];
				String caseName2 = s2.split("\n")[0];
				return caseName1.compareTo(caseName2);
			});

			traceList.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
				if(newV == null) return;
				prepareValues(map.get(newV));
			});
		
		} else if (result2 != null) {
			statBox.getChildren().remove(differentDataStat);
			XLog alignedLog = result2.getAlignedLog();
			List<String> tL = new ArrayList<>();
			for(int i=0; i<alignedLog.size(); i++) {
				XTrace xt = alignedLog.get(i);
				tL.add(xt.getAttributes().get("concept:name").toString());
			}

			Map<String,Alignment> map = new HashMap<>();
			for(String t: tL) {
				Alignment a = result2.getAlignmentByTraceName(t);
				if(a != null) {
					String elem = t+"\n"+String.format("Fitness: %.2f", a.getFitness());
					map.put(elem, a);
				}
			}

			traceList.getItems().addAll(map.keySet());
			traceList.getItems().sort((s1,s2) -> {
				String caseName1 = s1.split("\n")[0];
				String caseName2 = s2.split("\n")[0];
				return caseName1.compareTo(caseName2);
			});

			traceList.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
				if(newV != null)
					prepareValues2(map.get(newV));
			});
		}
	}
	
	public void insertConstraints(List<String> cL) {
		this.constraintList.getItems().addAll(cL);
	}

	private void prepareValues2(Alignment alignment) {
		// TODO Auto-generated method stub
		statBox.setVisible(true);
		XLog t = null;
		if(traces != null) t = traces;
		TraceViewController trc = GraphGenerator.getTraceView(alignment,result2.getDeclareModel(),t);
		viewPane.getChildren().clear();
		viewPane.getChildren().add(trc);
		AnchorPane.setBottomAnchor(trc, 10.0);
		AnchorPane.setTopAnchor(trc, 0.0);
		AnchorPane.setLeftAnchor(trc, 0.0);
		AnchorPane.setRightAnchor(trc, 0.0);
		
		int both=0;
		int log=0;
		int model=0;
		for(DataAwareStepTypes step: alignment.getStepTypes()) {
			if(step.toString().endsWith("Both")) {
				both++;
			}
			if(step.toString().endsWith("Log")) {
				log++;
			}
			if(step.toString().endsWith("Model")) {
				model++;
			}
		}
		
		String s1 = String.format("  Moves in Log (Deletions): %d", log);
		String s2 = String.format("  Moves in Model (Insertions): %d", model);
		String s3 = String.format("  Moves in Log and Model: %d", both);
		greenLabel.setText(s3);
		yellowLabel.setText(s1);
		purpleLabel.setText(s2);
	}

	private void prepareValues(Alignment alignment) {
		statBox.setVisible(true);
		Set<AnalysisSingleResult> asr = result.getDetailedResults().get(alignment);
		AnalysisSingleResult analysisSingleResult = asr.stream().findFirst().get();
		XLog t = null;
		if(traces != null) t = traces;
		TraceViewController trc = GraphGenerator.getTraceView(analysisSingleResult,t);
		viewPane.getChildren().clear();
		viewPane.getChildren().add(trc);
		AnchorPane.setBottomAnchor(trc, 10.0);
		AnchorPane.setTopAnchor(trc, 0.0);
		AnchorPane.setLeftAnchor(trc, 0.0);
		AnchorPane.setRightAnchor(trc, 0.0);
		
		String s1 = String.format("  Moves in Log and Model (same data): %d", analysisSingleResult.getMovesInBoth().size());
		String s2 = String.format("  Moves in Log and Model (different data): %d", analysisSingleResult.getMovesInBothDiffData().size());
		String s3 = String.format("  Moves in Log (Deletions): %d", analysisSingleResult.getMovesInLog().size());
		String s4 = String.format("  Moves in Model (Insertions): %d", analysisSingleResult.getMovesInModel().size());
		greenLabel.setText(s1);
		yellowLabel.setText(s3);
		purpleLabel.setText(s4);
		whiteLabel.setText(s2);
	}

}
