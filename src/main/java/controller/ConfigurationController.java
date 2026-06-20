package controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.declareminer.DeclareMiner;
import org.processmining.plugins.declareminer.DeclareMinerInput;
import org.processmining.plugins.declareminer.DeclareMinerNoHierarc;
import org.processmining.plugins.declareminer.DeclareMinerNoRed;
import org.processmining.plugins.declareminer.DeclareMinerNoTrans;
import org.processmining.plugins.declareminer.enumtypes.AprioriKnowledgeBasedCriteria;
import org.processmining.plugins.declareminer.enumtypes.DeclarePerspective;
import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;
import org.processmining.plugins.declareminer.enumtypes.MapTemplateConfiguration;
import org.processmining.plugins.declareminer.util.Configuration;
import org.processmining.plugins.declareminer.util.UnifiedLogger;
import org.processmining.plugins.declareminer.util.XLogReader;
import org.processmining.plugins.declareminer.visualizing.ActivityDefinition;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;
import org.processmining.plugins.declareminer.visualizing.ConstraintTemplate;
import org.processmining.plugins.declareminer.visualizing.DeclareMap;
import org.processmining.plugins.declareminer.visualizing.DeclareMinerOutput;
import org.processmining.plugins.declareminer.visualizing.Parameter;

import graph.ArrowProperty;
import graph.Arrows;
import graph.Font;
import graph.Smooth;
import graph.VisEdge;
import graph.VisGraph;
import graph.VisNode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import util.ConstraintUtils;
import util.GraphGenerator;
import util.LifecycleTransition;
import view.Browser;

public class ConfigurationController extends GridPane {
	@FXML
	private Label fileName;
	
	@FXML
	private Slider supportSlider;
	
	@FXML
	private Label minSupportText;
	
	@FXML
	private RadioButton vacuityTrue;
	
	@FXML
	private RadioButton vacuityFalse;
	
	@FXML
	private ChoiceBox<String> pruneChoice;
	
	@FXML
	private ListView<String> declareTemplatesList;
	
	private Stage stage;
	
	private String filePath;
	
	private String miner;
	
	private List<File> files;
	
	private Slider zoom;
	
	private TabbedMainViewController tmvc;
	
	public ConfigurationController(Stage stage,String filePath,String miner,List<File> files) {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/Configuration.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        this.stage = stage;
        this.filePath = filePath;
        this.miner = miner;
        this.files = files;
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public void setTmvc(TabbedMainViewController tmvc) {
		this.tmvc = tmvc;
	}
	
	@FXML
	public void undo() {
		tmvc.cancel();
	}
	
	public ConfigurationController() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/Configuration.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public void setMiner(String miner) {
		this.miner = miner;
	}
	
	public void setZoom(Slider zoom) {
		this.zoom = zoom;
	}
	
	@FXML
	public void cancel() {
		MyController2 mc = (MyController2) this.stage.getUserData();
		this.stage.setTitle("Hello World!");
		MyController2 mc2 = new MyController2(this.stage,mc.getFiles());
		if(mc.getCurrentBrowser() != null) mc2.setOutputPaneChildren(mc.getCurrentBrowser());
		if(mc.getCurrentFC() != null) {
			FilterController fc = mc.getCurrentFC();
			fc.setOutputPane(mc2.outputPane);
			mc2.setFilterPaneChildren(fc);
			if(fc.getCurrentBrowser() != null) mc2.setOutputPaneChildren(fc.getCurrentBrowser());
		}
		if(mc.getCurrentFMC() != null) {
			FilterMinerFulController fmc = mc.getCurrentFMC();
			fmc.setOutputPane(mc2.outputPane);
			mc2.setFilterPaneMinerFul(fmc);
			if(fmc.getCurrentBrowser() != null) mc2.setOutputPaneChildren(fmc.getCurrentBrowser());
		}
		this.stage.setScene(new Scene(mc2,this.getScene().getWidth(),this.getScene().getHeight()));
		this.stage.setMaximized(true);
		this.stage.show();
	}
	
	public DeclareMinerOutput getDiscoveryResult() {
		//this.viewChoice = viewChoice;
		//String view = this.viewChoice.getSelectionModel().getSelectedItem();
		System.out.println("Discovery start millis: "+System.currentTimeMillis());
		Configuration configuration = new Configuration();
		XLog log = null;
		try {
			log = XLogReader.openLog(filePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		configuration.log = log;
		
		DeclareMinerInput input = new DeclareMinerInput();
		int min_support = (int) supportSlider.getValue();
		int alpha = 100;
		if(vacuityTrue.isSelected()) alpha = 0;
		
		input.setSelectedDeclareTemplateSet(getSelectedSet());
		
		Set<DeclarePerspective> persp_set = Collections.singleton(DeclarePerspective.valueOf("Control_Flow"));
		input.setDeclarePerspectiveSet(persp_set);
		
		
		DeclareTemplate[] declareTemplates = DeclareTemplate.values();
		Map<String, DeclareTemplate> templateNameStringDeclareTemplateMap = new HashMap<String, DeclareTemplate>();
	
		for(DeclareTemplate d : declareTemplates){
			String templateNameString = d.toString().replaceAll("_", " ").toLowerCase();
			templateNameStringDeclareTemplateMap.put(templateNameString, d);
		}
	
		Map<DeclareTemplate, ConstraintTemplate> declareTemplateConstraintTemplateMap = DeclareMiner.readConstraintTemplates(templateNameStringDeclareTemplateMap);
		
		input.setDeclareTemplateConstraintTemplateMap(declareTemplateConstraintTemplateMap);
		input.setMapTemplateConfiguration(MapTemplateConfiguration.valueOf("DiscoverProvidedTemplatesAcrossAllActivitesInLog"));
		input.setMinSupport(min_support);
		input.setAlpha(alpha);
		
		Set<AprioriKnowledgeBasedCriteria> apriori_set = new HashSet<AprioriKnowledgeBasedCriteria>(Collections.singleton(AprioriKnowledgeBasedCriteria.valueOf("AllActivitiesWithEventTypes")));
		input.setAprioriKnowledgeBasedCriteriaSet(apriori_set);
		input.setVerbose(false);
		input.setThreadNumber(4);
		configuration.input = input;
		UnifiedLogger.unified_log_path = "./output/all_results.log";
		UnifiedLogger.unified_memory_log_path = "./output/mem.log";
		DeclareMinerOutput output = getDeclareMinerOutput(configuration,"DeclareMiner",pruneChoice.getSelectionModel().getSelectedItem());
		//drawOutputAlt(output, outputPane);
		return output;
		/*DiscoveryResultController drc = new DiscoveryResultController();
		FilterController fc = addFilterValues(output,drc.getOutputPane());
		
		List<Integer> finalKeys = new ArrayList<Integer>();
		output.getVisiblesupportRule().keySet().forEach(k -> {
			DeclareTemplate dt = output.getTemplate().get(k);
			if(isSelectedTemplate(dt)) {
				finalKeys.add(k);
			}
		});
		List<Integer> actKeys = new ArrayList<Integer>();
		finalKeys.forEach(k -> {
			List<String> l = output.getConstraintParametersMap().get(k);
			if(l.size() == 1) {
				String s = l.get(0);
				output.getAllActivities().forEach((kk,v) -> {
					if(v.equals(s)) actKeys.add(kk);
				});
			}
			else {
				String s = l.get(0);
				String s2 = l.get(1);
				output.getAllActivities().forEach((kk,v) -> {
					if(v.equals(s)) actKeys.add(kk);
					if(v.equals(s2)) actKeys.add(kk);
				});
			}
		});
		Set<Integer> actKeysSet = new HashSet<Integer>(actKeys);
		//Browser b = drawGraph(output,new HashSet<Integer>(finalKeys));
		drc.setView(view);
		drc.setFc(fc);
		//drc.getFilterPane().getChildren().add(new Label("Hello World"));
		drc.setBrowser(getBrowserFrom(output,actKeysSet,finalKeys));
		//drc.setBrowser(b);
		return drc;*/
	}
	
	public Set<DeclareTemplate> getSelectedSet() {
		List<DeclareTemplate> l = new ArrayList<DeclareTemplate>();
		declareTemplatesList.getSelectionModel().getSelectedItems().forEach(s -> {
			int lb = s.indexOf('[');
			String t = s.substring(0, lb);
			try {
				l.add(DeclareTemplate.valueOf(t.replace(" ","_")));
			}catch(Exception e) {
				if(t.equals("Co-Existence")) {
					l.add(DeclareTemplate.CoExistence);
				}
				else if(t.equals("Not Co-Existence")) {
					l.add(DeclareTemplate.Not_CoExistence);
				}
			}
		});
		return new HashSet<DeclareTemplate>(l);
	}
	
	public Browser getBrowserFrom(DeclareMinerOutput output,Set<Integer> actKeys, List<Integer> finalKeys,String view) {
		return GraphGenerator.browserify(getActivitiesMap(output,actKeys),
				getActSuppMap(output,actKeys), 
				getTemplatesMap(output,finalKeys), 
				getConstraintParametersMap(output,finalKeys), 
				getConstraintSuppMap(output,finalKeys),zoom,view,"traces");
	}
	
	public Map<Integer,String> getActivitiesMap(DeclareMinerOutput o, Set<Integer> actKeys) {
		Map<Integer,String> map = new HashMap<Integer,String>();
		o.getAllActivities().forEach((k,v) -> {
			if(actKeys.contains(k)) {
				int l = v.lastIndexOf('-');
				String ending = v.substring(l+1);
				if(LifecycleTransition.isEndingValid(ending)) {
					map.put(k, v.substring(0,l));
				}
				else {
					map.put(k,v);
				}
			}
		});
		return map;
	}
	
	public Map<Integer,List<String>> getConstraintParametersMap(DeclareMinerOutput o, List<Integer> keys) {
		Map<Integer,List<String>> map = new HashMap<Integer,List<String>>();
		o.getConstraintParametersMap().forEach((k,v) -> {
			if(keys.contains(k)) {
				List<String> l = v.stream().map(a -> {
					int li = a.lastIndexOf('-');
					String ending = a.substring(li+1);
					if(LifecycleTransition.isEndingValid(ending)) {
						return a.substring(0,li);
					}
					else {
						return a;
					}
				}).collect(Collectors.toList());
				map.put(k, l);
			}
		});
		return map;
	}
	
	public Map<Integer,Double> getActSuppMap(DeclareMinerOutput o, Set<Integer> actKeys) {
		Map<Integer,Double> map = new HashMap<Integer,Double>();
		o.getActSupp().forEach((k,v) -> {
			int key = (int) k;
			if(actKeys.contains(key))
			map.put(key, (double) v);
		});
		return map;
	}
	
	public Map<Integer, util.ConstraintTemplate> getTemplatesMap(DeclareMinerOutput o, List<Integer> keys) {
		Map<Integer, util.ConstraintTemplate> map = new HashMap<>();
		o.getTemplate().forEach((k,v) -> {
			if(keys.contains(k))
				map.put(k, ConstraintUtils.getTemplateFromDeclare(v));
		});
		
		return map;
	}
	
	public Map<Integer,Double> getConstraintSuppMap(DeclareMinerOutput o, List<Integer> keys) {
		Map<Integer,Double> map = new HashMap<Integer,Double>();
		o.getVisiblesupportRule().forEach((k,v) -> {
			if(keys.contains(k))
			map.put(k, (double) v);
		});
		return map;
	}
	
	private HashMap<Integer, Boolean> isPlottedMap(DeclareMinerOutput output) {
		HashMap<Integer, Boolean> map = new HashMap<Integer,Boolean>();
		output.getVisiblesupportRule().keySet().forEach(k -> map.put(k, false));
		return map;
	}
	
	private int findActivity(List<String> activity,HashMap<Integer,Boolean> plotMap,DeclareMinerOutput output) {
		AtomicInteger foundId = new AtomicInteger();
		plotMap.forEach((k,drawn) -> {
			if(!drawn && foundId.get() == 0) {
				List<String> obtained = output.getConstraintParametersMap().get(k);
				if(obtained.size() == 2 && activity.get(1).equals(obtained.get(0))) foundId.set(k);
			}
		});
		return foundId.get();
	}
	
	private double getConstraintSupport(String startActivity, String endActivity, DeclareMinerOutput output) {
		// TODO Auto-generated method stub
		List<String> constraint = Arrays.asList(startActivity,endActivity);
		List<Double> supportSingleton = new ArrayList<Double>();
		output.getConstraintParametersMap().forEach((k,v) -> {
			if(v.equals(constraint)) {
				supportSingleton.add((double)output.getSupportRule().get(k));
			}
		});
		return supportSingleton.get(0);
	}
	
	private String findExistenceConstraint(String activity, HashMap<Integer,Boolean> plotMap, DeclareMinerOutput dmOutput) {
		List<String> constraints = new ArrayList<String>();
		dmOutput.getConstraintParametersMap().forEach((k,v) -> {
			if(plotMap.get(k)!= null && !plotMap.get(k) && v.size() == 1) {
				if(v.get(0).equals(activity)) {
					constraints.add(dmOutput.getTemplate().get(k).name());
					plotMap.put(k, true);
				}
			}
		});
		String result = "";
		for(int i=0; i<constraints.size(); i++) {
			if(i == constraints.size() - 1) {
				result += constraints.get(i);
			}
			else result = result + constraints.get(i) + "\n";
		}
		return result;
	}
	
	public void drawOutputAlt(DeclareMinerOutput dmOutput, AnchorPane outputPane) {
		getOutput(dmOutput).stream().forEach(s -> System.out.println(s));
		outputPane.getChildren().clear();
		HashMap<Integer,Boolean> plotMap = isPlottedMap(dmOutput);
		Set<Integer> ids = plotMap.keySet();
		int multi = 0;
		int multi2 = 0;
		for(int id:ids) {
			if(!plotMap.get(id)) {
				if(dmOutput.getConstraintParametersMap().get(id).size() == 1) {
					plotMap.put(id, true);
					List<String> activity = dmOutput.getConstraintParametersMap().get(id);
					String startActivity = activity.get(0);
	        		String endActivity = "";
	        		double constraintSupport = dmOutput.getVisiblesupportRule().get(id);
	        		String exA = dmOutput.getTemplate().get(id).name()+"\n"+findExistenceConstraint(startActivity, plotMap, dmOutput);
	        		String exB = "";
	        		DeclareTemplateController dtc = new DeclareTemplateController(
	        				exA,
	        				getActivitySupport(startActivity,dmOutput),
	        				startActivity,
	        				exB,
	        				0,
	        				endActivity,
	        				dmOutput.getTemplate().get(id),
	        				constraintSupport
	        				);
	        		dtc.setLayoutX(multi2 * 200.0);
	                outputPane.getChildren().add(dtc);
	                multi2++;
				}
				if(dmOutput.getConstraintParametersMap().get(id).size() == 2) {
					List<String> activity = dmOutput.getConstraintParametersMap().get(id);
					String startActivity = activity.get(0);
	        		String endActivity = activity.get(1);
	        		double constraintSupport = getConstraintSupport(startActivity,endActivity,dmOutput);
	        		String exA = findExistenceConstraint(startActivity, plotMap, dmOutput);
	        		String exB = findExistenceConstraint(endActivity, plotMap, dmOutput);
	        		DeclareTemplateController dtc = new DeclareTemplateController(
	        				exA,
	        				getActivitySupport(startActivity,dmOutput),
	        				startActivity,
	        				exB,
	        				getActivitySupport(endActivity,dmOutput),
	        				endActivity,
	        				dmOutput.getTemplate().get(id),
	        				constraintSupport
	        				);
	        		dtc.setLayoutX(multi * 200.0);
	        		dtc.setLayoutY(130.0);
	                outputPane.getChildren().add(dtc);
	                plotMap.put(id, true);
	                multi++;
	                int other = findActivity(activity,plotMap,dmOutput);
	                if(other != 0) {
	                	activity = dmOutput.getConstraintParametersMap().get(other);
						startActivity = activity.get(0);
		        		endActivity = activity.get(1);
		        		constraintSupport = getConstraintSupport(startActivity,endActivity,dmOutput);
		        		exA = findExistenceConstraint(startActivity, plotMap, dmOutput);
		        		exB = findExistenceConstraint(endActivity, plotMap, dmOutput);
		        		DeclareTemplateController dtc2 = new DeclareTemplateController(
		        				exA,
		        				getActivitySupport(startActivity,dmOutput),
		        				startActivity,
		        				exB,
		        				getActivitySupport(endActivity,dmOutput),
		        				endActivity,
		        				dmOutput.getTemplate().get(other),
		        				constraintSupport
		        				);
		        		dtc2.setLayoutX(dtc.getLayoutX());
		        		dtc2.setLayoutY(dtc.getLayoutY()+202.0);
		                outputPane.getChildren().add(dtc2);
		                plotMap.put(other, true);
	                }
	                
				}
			}
		}
	}
	
	/*public void drawOutput(DeclareMinerOutput output, HBox outputPane) {
		outputPane.getChildren().clear();
		List<String> outputList = getOutput(output);
		int j = 0;
        for(int i=0; i<outputList.size(); i++) {
        	String activity = outputList.get(i);
        	
        	if(getDeclareTemplate(activity).equalsIgnoreCase("response")) {
        		TestRunner tr = new TestRunner();
        		String startActivity = tr.startEnd(activity).first;
        		String endActivity = tr.startEnd(activity).second;
        		ResponseController rc2 = new ResponseController(
        				startActivity, getActivitySupport(startActivity, output),
        				endActivity, getActivitySupport(endActivity, output)
        				);
                rc2.setLayoutY(j * 100.0);
                outputPane.getChildren().add(rc2);
                j++;
        	}
        	
        }
	}*/
	
	private double getActivitySupport(String activity, DeclareMinerOutput output) {
		//int id;
		Map<Integer, String> activities = output.getAllActivities();
		Set<Integer> keys = activities.keySet();
		Optional<Integer> opt = keys.stream().filter(k -> activities.get(k).equals(activity)).findFirst();
		if(opt.isPresent()) {
			return output.getActSupp().get(opt.get());
		}
		else return 0.0;
	}
	
	public FilterController addFilterValues(DeclareMinerOutput declareMinerOutput, Pane outputPane) {
		//filterPane.getChildren().clear();
		return new FilterController(declareMinerOutput, outputPane, this);
	}
	
	public Stage getStage() {
		return stage;
	}
	
	private String getDeclareTemplate(String str) {
		int leftBracket = str.indexOf('(');
		return str.substring(0, leftBracket);
	}
	
	private DeclareMinerOutput getDeclareMinerOutput(Configuration configuration, String miner, String pruneChoice) {
		Configuration conf = configuration;
		conf.setUnifiedLoggerPrunerType("replayers"); // will be obsolete after testing is done

		String miner_type = miner;
		DeclareMinerOutput output = new DeclareMinerOutput();
		// Choose the Miner type based on configuration
		// TODO
		if (conf.log != null && conf.input != null){
			String p = pruneChoice;
			if (p.equals("All Reductions"))
				output = DeclareMiner.mineDeclareConstraints(null, conf.log, conf.input);
			else if (p.equals("Hierarchy-based"))
				output = DeclareMinerNoHierarc.mineDeclareConstraints(null, conf.log, conf.input);
			else if (p.equals("None"))
				output = DeclareMinerNoRed.mineDeclareConstraints(null, conf.log, conf.input);
			else if (p.equals("Transitive Closure"))
				output = DeclareMinerNoTrans.mineDeclareConstraints(null, conf.log, conf.input);
			else
				throw new IllegalArgumentException(String.format("Invalid miner type '%s'", miner_type));

		} else {
			throw new IllegalArgumentException("No valid argument combination found");
		}
		return output;
	}
	
	private List<String> getOutput(DeclareMinerOutput declareMinerOutput) {
		//String configuration_file_path = args[0]
		LinkedList<String> outputList = new LinkedList<String>();
		
		DeclareMinerOutput output = declareMinerOutput;
		// Choose the Miner type based on configuration
		// TODO

		DeclareMap model = output.getModel();

		Iterator<ConstraintDefinition> constraints_iter = model.getModel().getConstraintDefinitions().iterator();
		while(constraints_iter.hasNext()) {
			ConstraintDefinition constraint = constraints_iter.next();
			//Iterator<Pair<String, String>> pair_iter = constraints.get(template).keySet().iterator();
			//while(pair_iter.hasNext()) {
			//	Pair<String, String> pair = pair_iter.next();
			//Double support = constraint.getId();
			ArrayList<String> params = new ArrayList<String>();
			for(Parameter p: constraint.getParameters()){
				for(ActivityDefinition a : constraint.getBranches(p)){
					params.add(a.getName());
				}
			}
			String line = null;
			if(params.size() == 1){
				line = constraint.getName() + "(" + params.get(0) + "): support";
			}else{
				line = constraint.getName() + "(" + params.get(0) + ", " + params.get(1) + "): support";
			}
			outputList.add(line);
		}
		return outputList;
	}
	
	@FXML
	public void initialize() {
		
		/*minSupportText.setOnMouseClicked(_e -> {
			minSupportText.setEditable(true);
		});*/
		
		minSupportText.setText(String.format("%.1f%%", supportSlider.getValue()));
		
		supportSlider.valueProperty().addListener(_v -> {
			minSupportText.setText(String.format("%.1f%%", supportSlider.getValue()));
		});
		
		vacuityTrue.setSelected(true);
		
		vacuityFalse.setOnAction((e) -> {
			if(vacuityFalse.isSelected()) vacuityTrue.setSelected(false);
		});
		
		vacuityTrue.setOnAction((e) -> {
			if(vacuityTrue.isSelected()) vacuityFalse.setSelected(false);
		});
		
		pruneChoice.setItems(FXCollections.
				observableArrayList("All Reductions","Hierarchy-based","Transitive Closure","None"));
		
		pruneChoice.getSelectionModel().select("All Reductions");
		insertDeclareTemplates();
		declareTemplatesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		selectNonNegatives();
	}
	
	@FXML
	public void clearAllSelections() {
		declareTemplatesList.getSelectionModel().clearSelection();
	}
	
	@FXML
	public void selectAll() {
		declareTemplatesList.getSelectionModel().selectAll();
	}
	
	@FXML
	public void restoreDefault() {
		declareTemplatesList.getSelectionModel().clearSelection();
		pruneChoice.getSelectionModel().select("All Reductions");
		supportSlider.setValue(100.0);
		selectNonNegatives();
		vacuityTrue.setSelected(true);
		vacuityFalse.setSelected(false);
	}
	
	public void selectNonNegatives() {
		for(DeclareTemplate dt : DeclareTemplate.values()) {
			if(dt != DeclareTemplate.Choice && dt != DeclareTemplate.Exclusive_Choice) {
				String t = getModelName(dt.name()).replace('_', ' ');
				if(!t.startsWith("Not")) {
					declareTemplatesList.getSelectionModel().select(t);
				}
			}
		}
	}
	
	public boolean isSelectedTemplate(DeclareTemplate dt) {
		return declareTemplatesList.getSelectionModel().getSelectedItems().contains(
				getModelName(dt.name()).replace('_', ' ')
				);
	}
	
	public boolean isValidKey(Map<Integer,DeclareTemplate> map, int k) {
		return isSelectedTemplate(map.get(k));
	}
	
	private String getModelName(String template) {
		DeclareTemplate d = DeclareTemplate.valueOf(template);
		switch(d) {
			case Absence:
				return template+"[A]";
			case Absence2:
				return template+"[A]";
			case Absence3:
				return template+"[A]";
			case Exactly1:
				return template+"[A]";
			case Exactly2:
				return template+"[A]";
			case Existence:
				return template+"[A]";
			case Existence2:
				return template+"[A]";
			case Existence3:
				return template+"[A]";
			case Init:
				return template+"[A]";
			case CoExistence:
				return "Co-Existence[A, B]";
			case Not_CoExistence:
				return "Not Co-Existence[A, B]";
			default:
				return template+"[A, B]";
		}
	}
	
	private void insertDeclareTemplates() {
		List<String> templates = new ArrayList<String>();
		for(DeclareTemplate dt : DeclareTemplate.values()) {
			if(dt != DeclareTemplate.Choice && dt != DeclareTemplate.Exclusive_Choice) {
				templates.add(getModelName(dt.name()).replace('_', ' '));
			}
		}
		ObservableList<String> items =FXCollections.observableArrayList (
			    (String[]) templates.toArray(new String[templates.size()]));
		
		declareTemplatesList.setItems(items);
	}
	
	/*@FXML
	public void setValue() {
		supportSlider.setValue(Double.valueOf(minSupportText.getText()));
		minSupportText.setEditable(false);
	}*/
	
	public String findAllExistenceConstraints(DeclareMinerOutput output, String activity, Set<Integer> picked, HashMap<Integer,Boolean> isDrawn) {
		List<String> list = Arrays.asList(activity);
		List<Integer> keys = new ArrayList<Integer>();
		output.getConstraintParametersMap().forEach((k,v) -> {
			if(v.equals(list) && picked.contains(k)) keys.add(k);
		});
		String existence = "";
		List<String> templateList = new ArrayList<String>();
		for(int k: keys) {
			if(!isDrawn.get(k)) {
				isDrawn.put(k, true);
				templateList.add(output.getTemplate().get(k).name());
			}
		}
		Set<String> templateSet = new HashSet<String>(templateList);
		for(String s: templateSet) {
			existence += s + "\n";
		}
		if(existence.equals("")) return existence;
		else return existence;
	}
	
	public Browser drawGraph(DeclareMinerOutput output, Set<Integer> keys) {
		//outputPane.getChildren().clear();
		System.out.println("Output: "+getOutput(output));
		List<String> allActivities = new ArrayList<String>();
		output.getVisiblesupportRule().forEach((k,v) -> {
			allActivities.addAll(output.getConstraintParametersMap().get(k));
		});
		System.out.println(new HashSet<String>(allActivities));
		VisGraph graph = new VisGraph();
		List<VisEdge> listOfEdges = new ArrayList<VisEdge>();
		HashMap<String,List<String>> adjacencyMap = new HashMap<String,List<String>>();
		HashMap<Integer,Boolean> isDrawn = new HashMap<Integer,Boolean>();
		HashMap<Integer,VisNode> allNodes = new HashMap<Integer,VisNode>();
		HashMap<TreeSet<Integer>,Integer> offsetMap = new HashMap<TreeSet<Integer>,Integer>();
		List<Float> supports = new ArrayList<>(output.getSupportRule().values());
		List<Float> supportTree = supports.stream().distinct().sorted().collect(Collectors.toList());
		double plus = (supportTree.size() > 1) ? 5.0 / (supportTree.size()-1) : 5.0;
		keys.forEach(k -> isDrawn.put(k, false));
		Map<Integer,DeclareTemplate> tmap = output.getTemplate();
		output.getAllActivities().forEach((k,v) -> {
			List<String> l = new ArrayList<String>();
			output.getConstraintParametersMap().forEach((kk,list) -> {
				if(isValidKey(tmap,kk) && keys.contains(kk) && !isDrawn.get(kk) && list.size() == 2 && list.get(0).equals(v)) {
					String str = list.get(1);
					l.add(str);
					isDrawn.put(kk, true);
					output.getAllActivities().forEach((kkk,vvv) -> {
						if(vvv.equals(str)) {
							String ex1 = findAllExistenceConstraints(output, v, keys, isDrawn);
							String ex2 = findAllExistenceConstraints(output, vvv, keys, isDrawn);
							VisNode start = allNodes.get(k);
							if(start == null) {
								String s = String.format("%.2f%%",output.getActSupp().get(k)*100);
								start = new VisNode(k,ex1+v+"_"+s,"Support: "+s);
								double supp = output.getActSupp().get(k);
								double res = 51 + 26 * (1-supp) * 27.46;
								if(res > 255) {
									long remaining = Math.round((res - 255) / 2);
									String color = "#"+Long.toHexString(remaining)+Long.toHexString(remaining)+"ff";
									start.setColor(color);
								}
								else {
									String color = "#0000"+Long.toHexString(Math.round(res));
									start.setColor(color);
								}
								String fc = (supp >= 0.5) ? "#ffffff" : "#000000";
								start.setFont(new Font(fc));
								allNodes.put(k, start);
							}
							
							VisNode end = allNodes.get(kkk);
							if(end == null) {
								String s = String.format("%.2f%%",output.getActSupp().get(kkk)*100);
								end = new VisNode(kkk,ex2+vvv+"_"+s,"Support: "+s);
								double supp = output.getActSupp().get(kkk);
								double res = 51 + 26 * (1-supp) * 27.46;
								if(res > 255) {
									long remaining = Math.round((res - 255) / 2);
									String color = "#"+Long.toHexString(remaining)+Long.toHexString(remaining)+"ff";
									end.setColor(color);
								}
								else {
									String color = "#0000"+Long.toHexString(Math.round(res));
									end.setColor(color);
								}
								String fc = (supp >= 0.5) ? "#ffffff" : "#000000";
								end.setFont(new Font(fc));
								allNodes.put(kkk, end);
							}
							List<Integer> idList = Arrays.asList(start.getId(),end.getId());
							TreeSet<Integer> offsetKey = new TreeSet<Integer>(idList);
							Integer value = offsetMap.get(offsetKey);
							if(value == null) {
								offsetMap.put(offsetKey, 0);
							}
							value = offsetMap.get(offsetKey);
							float supp = output.getSupportRule().get(kk);
							double edgeWidth = (supportTree.size() > 1) ? 1 + plus * supportTree.indexOf(supp) : 3.1;
							VisEdge edge = getCorrespondingEdge(start,end,output.getTemplate().get(kk),(double) supp,edgeWidth,value);
							offsetMap.put(offsetKey, ++value);
							listOfEdges.add(edge);
						}
					});
				}
			});
			adjacencyMap.put(v, l);
		});
		List<Integer> remainingSingle = new ArrayList<Integer>();
		isDrawn.forEach((k,v) -> {
			if(!v) {
				remainingSingle.add(k);
			}
 		});
		for(int k: remainingSingle) {
			String activity = output.getConstraintParametersMap().get(k).get(0);
			String exA = findAllExistenceConstraints(output, activity, keys, isDrawn);
			output.getAllActivities().forEach((kk,vv) -> {
				if(vv.equals(activity)) {
					VisNode node = allNodes.get(kk);
					if(node == null) {
						String s = String.format("%.2f%%",output.getActSupp().get(kk)*100);
						node = new VisNode(kk,exA+activity+"_"+s,"Support: "+s);
						double supp = output.getActSupp().get(kk);
						double res = 51 + 26 * (1-supp) * 27.46;
						if(res > 255) {
							long remaining = Math.round((res - 255) / 2);
							String color = "#"+Long.toHexString(remaining)+Long.toHexString(remaining)+"ff";
							node.setColor(color);
						}
						else {
							String color = "#0000"+Long.toHexString(Math.round(res));
							node.setColor(color);
						}
						String fc = (supp >= 0.5) ? "#ffffff" : "#000000";
						node.setFont(new Font(fc));
						allNodes.put(kk, node);
					}
				}
				});
		}
		Collection<VisNode> listOfNodes = allNodes.values();
		VisNode[] nodeArray = (VisNode[]) listOfNodes.toArray(new VisNode[listOfNodes.size()]);
		double portion = (Math.PI * 2 / nodeArray.length);
		double radius = Math.sqrt((400 * 400) / (2 * (1 - Math.cos(portion))));
		double dist = radius * Math.sqrt(2 * (1 - Math.cos(portion)));
		System.out.printf("Distance: %.2f\n", dist);
		System.out.println("Number of nodes: "+nodeArray.length);
		System.out.println("#########################################");
		for(int i=0; i<nodeArray.length; i++) {
			System.out.println(nodeArray[i].getLabel());
			System.out.println("#########################################");
		}
		graph.addNodes(nodeArray);
		graph.addEdges((VisEdge[]) listOfEdges.toArray(new VisEdge[listOfEdges.size()]));
		System.out.println(offsetMap);
		return new Browser(graph,Screen.getPrimary().getVisualBounds().getHeight()*0.9,Screen.getPrimary().getVisualBounds().getWidth() * 0.75,zoom);
		//System.out.println(Screen.getPrimary().getVisualBounds());
		//System.out.println("#############################");
		//System.out.println(adjacencyMap);
		//System.out.println("#####################################");
		/*adjacencyMap.forEach((s,list) -> {
			list.forEach(a -> {
				List<String> l = Arrays.asList(s,a);
				output.getConstraintParametersMap().forEach((k,v) -> {
					if(v.equals(l) && isDrawn.get(k) != null && !isDrawn.get(k)) {
						isDrawn.put(k, true);
						String template = output.getTemplate().get(k).name();
						
						//System.out.println(template+" "+l);
					}
				});
			});
		});*/
	}
	
	public VisEdge getCorrespondingEdge(VisNode start, VisNode end, DeclareTemplate template, Double support, double edgeWidth, int offset) {
		int last = start.getLabel().lastIndexOf('\n');
		String startActivity = start.getLabel().substring(last+1);
		last = end.getLabel().lastIndexOf('\n');
		String endActivity = end.getLabel().substring(last+1);
		
		int underline = startActivity.lastIndexOf('_');
		if(underline != -1) startActivity = startActivity.substring(0, underline);
		
		underline = endActivity.lastIndexOf('_');
		if(underline != -1) endActivity = endActivity.substring(0, underline);
		
		String constraint = template.name(); //+ "(" +startActivity+", "+endActivity+")";
		if(template == DeclareTemplate.Responded_Existence) {
			Smooth smooth = new Smooth(true, "dynamic");
			//String constraint = template.name() + "(" +start.getLabel()+", "+end.getLabel()+")";
			VisEdge edge = new VisEdge(start,end,null,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			return edge;
		}
		if(template == DeclareTemplate.Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			return edge;
		}
		if(template == DeclareTemplate.Alternate_Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);
			
			Smooth smooth = new Smooth(true,"doubleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Chain_Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);
			
			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge =  new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Chain_Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);
			
			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			Arrows arrows = new Arrows(to,null,null);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			return edge;
		}
		if(template == DeclareTemplate.Alternate_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			Arrows arrows = new Arrows(to,null,null);
			
			Smooth smooth = new Smooth(true,"doubleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Chain_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			Arrows arrows = new Arrows(to,null,null);
			
			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Chain_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty middle = new ArrowProperty(true, "bar");
			Arrows arrows = new Arrows(to,middle,null);
			
			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty middle = new ArrowProperty(true, "bar");
			Arrows arrows = new Arrows(to,middle,null);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.CoExistence) {
			ArrowProperty to = new ArrowProperty(true,"circle");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			return edge;
		}
		if(template == DeclareTemplate.Not_CoExistence) {
			ArrowProperty to = new ArrowProperty(true,"circle");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			return edge;
		}
		if(template == DeclareTemplate.Not_Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Alternate_Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);
			
			Smooth smooth = new Smooth(true,"doubleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Chain_Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);
			
			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Chain_Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);
			
			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
			edge.setWidth(edgeWidth);
			edge.setLabel(String.format("%.2f%%", support*100));
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		VisEdge edge = new VisEdge(start,end,null,null,constraint+"<br>"+String.format("Support: %.2f%%", support*100));//,(diff*400*Math.pow(1.25, offset)));
		edge.setWidth(edgeWidth);
		edge.setLabel(String.format("%.2f%%", support*100));
		return edge;
	}
	
}