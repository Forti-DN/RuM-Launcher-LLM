package controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;
import graph.ArrowProperty;
import graph.Arrows;
import graph.Font;
import graph.Smooth;
import graph.VisEdge;
import graph.VisGraph;
import graph.VisNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import minerful.MinerFulMinerLauncher;
import minerful.MinerFulSimplificationLauncher;
import minerful.concept.ProcessModel;
import minerful.concept.TaskChar;
import minerful.concept.TaskCharSet;
import minerful.concept.constraint.Constraint;
import minerful.miner.params.MinerFulCmdParameters;
import minerful.params.InputLogCmdParameters;
import minerful.params.SystemCmdParameters;
import minerful.postprocessing.params.PostProcessingCmdParameters;
import minerful.postprocessing.params.PostProcessingCmdParameters.PostProcessingAnalysisType;
import task.MinerfulResult;
import util.ConstraintTemplate;
import util.ConstraintUtils;
import util.GraphGenerator;
import util.Templates;
import view.Browser;

public class ConfigurationMinerFulController extends GridPane {
	
	@FXML
	private Slider supportSlider;
	
	@FXML
	private Label supportLabel;
	
	@FXML
	private ListView<String> declareTemplatesList;
	
	private String filePath;
	
	public AnchorPane outputPane;
	
	private AnchorPane filterPane;
	
	private Stage stage;
	
	private List<File> files;
	
	private Map<String,Double> activitySupportMap;
	
	private double activitySupportThreshold = 0.0;
	
	private double constraintSupportThreshold = 0.9;
	
	private Slider zoom;

	private ChoiceBox<String> viewChoice;
	
	private TabbedMainViewController tmvc;
	
	public ConfigurationMinerFulController(Stage stage,String filePath,AnchorPane outputPane,AnchorPane filterPane,List<File> files) {
		this.filterPane = filterPane;
		this.outputPane = outputPane;
		this.stage = stage;
		this.filePath = filePath;
		this.files = files;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ConfigurationMinerFul.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public ConfigurationMinerFulController() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ConfigurationMinerFul.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public Stage getStage() {
		return stage;
	}
	
	public void setTmvc(TabbedMainViewController tmvc) {
		this.tmvc = tmvc;
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public void setZoom(Slider zoom) {
		this.zoom = zoom;
	}
	
	public void setViewChoice(ChoiceBox<String> viewChoice) {
		this.viewChoice = viewChoice;
	}
	/*@FXML
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
	}*/
	
	@FXML
	public void initialize() {
		supportSlider.setValue(90.0);
		
		supportLabel.setText(String.format("%.1f%%", supportSlider.getValue()));
		
		supportSlider.valueProperty().addListener((ov,o,n) -> {
			supportLabel.setText(String.format("%.1f%%", supportSlider.getValue()));
		});
		setConstraintSupportThreshold(supportSlider.getValue() / 100);
		
		insertDeclareTemplates();
		declareTemplatesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		selectNonNegatives();
	}
	
	@FXML
	public void undo() {
		tmvc.cancel();
	}
	
	@FXML
	public void restoreDefault() {
		declareTemplatesList.getSelectionModel().clearSelection();
		supportSlider.setValue(90.0);
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
	
	private void insertDeclareTemplates() {
		/*List<String> templates = new ArrayList<String>();
		for(DeclareTemplate dt : DeclareTemplate.values()) {
			if(dt != DeclareTemplate.Choice && dt != DeclareTemplate.Exclusive_Choice) {
				templates.add(getModelName(dt.name()).replace('_', ' '));
			}
		}
		ObservableList<String> items =FXCollections.observableArrayList (
			    (String[]) templates.toArray(new String[templates.size()]));*/
		
		declareTemplatesList.getItems().addAll(Templates.getMinerFulTemplates());
	}
	
	public void selectNonNegatives() {
		for(String s: declareTemplatesList.getItems()) {
			if(!s.startsWith("Not")) {
				declareTemplatesList.getSelectionModel().select(s);
			}
		}
	}
	
	public boolean isSelectedTemplate(ConstraintTemplate t) {
		for(String selected : declareTemplatesList.getSelectionModel().getSelectedItems()) {
			String discovered = getModelName(t.name()).replace('_', ' ');
			String ss = selected.substring(0, selected.indexOf('['));
			
			if(Templates.isParentOf(ss, discovered))
				return true;
		}
		
		return false;
	}
	
	public boolean isValidKey(Map<Integer,String> map, int k) {
		return isSelectedTemplate(ConstraintTemplate.valueOf(map.get(k)));
	}
	
	private String getModelName(String template) {
		ConstraintTemplate t = ConstraintTemplate.valueOf(template);
		switch(t) {
			case Absence2:
				return "AtMost1";
			case Existence:
				return "AtLeast1";
			case Init:
				return "Init";
			case Exactly1:
				return "Exactly1";
			case CoExistence:
				return "Co-Existence";
			case Not_CoExistence:
				return "Not Co-Existence";
			default:
				return template;
		}
	}
	
	public FilterMinerFulController addFilterValues(ProcessModel initial,
			ProcessModel filtered, PostProcessingCmdParameters params, Pane outputPane) {
		//filterPane.getChildren().clear();
		return new FilterMinerFulController(this,initial,filtered,params,outputPane);
	}
	
	public void setActivitySupportMap(ProcessModel pm) {
		Map<String,Double> map = new HashMap<String,Double>();
		for(Constraint c:pm.getAllConstraints()) {
			if(c.getTemplateName().equals("AtLeast1")) {
				c.getInvolvedTaskChars().forEach(tc -> {
					map.put(tc.toString(), c.getSupport());
				});
			}
		}
		activitySupportMap = map;
	}
	
	public void setActivitySupportThreshold(double t) {
		activitySupportThreshold = t;
	}
	
	public void setConstraintSupportThreshold(double t) {
		this.constraintSupportThreshold = t;
	}
	
	public double getActivitySupportThreshold() {
		return activitySupportThreshold;
	}
	
	public Map<String, Double> getActivitySupportMap() {
		return activitySupportMap;
	}
	
	public MinerfulResult getDiscoveryResult() {
		System.out.println("Discovery start millis: "+System.currentTimeMillis());
		InputLogCmdParameters inputParams =
				new InputLogCmdParameters();
		MinerFulCmdParameters minerFulParams =
				new MinerFulCmdParameters();
		SystemCmdParameters systemParams =
				new SystemCmdParameters();
		PostProcessingCmdParameters postParams =
				new PostProcessingCmdParameters();
		
		inputParams.inputLogFile = new File(filePath);
		double cs = supportSlider.getValue() / 100;
		setConstraintSupportThreshold(cs);
		postParams.supportThreshold = cs;
		postParams.confidenceThreshold = 0.0;
		postParams.interestFactorThreshold = 0.0;
		postParams.postProcessingAnalysisType = PostProcessingAnalysisType.NONE;
		
		MinerFulMinerLauncher miFuMiLa = new MinerFulMinerLauncher(inputParams, minerFulParams, postParams, systemParams);
		ProcessModel pmForActSupp = miFuMiLa.mine();
		/*for(Constraint c: pmForActSupp.getAllConstraints()) {
			System.out.println(c.getTemplateName()+" "+c.getInvolvedTaskChars()+" "+c.getSupport());
		}*/
		setActivitySupportMap(pmForActSupp);
		/*System.out.println("After collecting support...");
		postParams.supportThreshold = supportSlider.getValue() / 100;
		postParams.postProcessingAnalysisType = PostProcessingAnalysisType.HIERARCHYCONFLICTREDUNDANCYDOUBLE;
		MinerFulMinerLauncher miFuMiLa2 = new MinerFulMinerLauncher(inputParams, minerFulParams, postParams, systemParams);
		ProcessModel processModel = miFuMiLa2.mine();*/
		/*postParams.supportThreshold = supportSlider.getValue() / 100;
		postParams.confidenceThreshold = confidenceSlider.getValue() / 100;
		postParams.interestFactorThreshold = interestSlider.getValue() / 100;
		postParams.postProcessingAnalysisType = PostProcessingAnalysisType.HIERARCHYCONFLICTREDUNDANCYDOUBLE;
		postParams.cropRedundantAndInconsistentConstraints = true;
		
		setActivitySupportThreshold(actSuppSlider.getValue()/100);
		// Run the discovery algorithm
		System.out.println("Running the discovery algorithm...");
		
		MinerFulSimplificationLauncher miFuSiLa = new MinerFulSimplificationLauncher(pmForActSupp, postParams);*/
		//ProcessModel processModel = miFuSiLa.simplify();
		MinerFulSimplificationLauncher miFuSiLa = new MinerFulSimplificationLauncher(pmForActSupp, postParams);
		postParams.postProcessingAnalysisType = PostProcessingAnalysisType.HIERARCHY;
		ProcessModel processModel = miFuSiLa.simplify();
		//for(Constraint c: processModel.getAllConstraints()) {
			//System.out.println(c.getTemplateName()+" "+c.getInvolvedTaskChars()+" "+c.getSupport());
		//}
		return new MinerfulResult(this.activitySupportMap,processModel,postParams);
	}
	
	/*@FXML
	public void apply() {
		InputLogCmdParameters inputParams =
				new InputLogCmdParameters();
		MinerFulCmdParameters minerFulParams =
				new MinerFulCmdParameters();
		ViewCmdParameters viewParams =
				new ViewCmdParameters();
		OutputModelParameters outParams =
				new OutputModelParameters();
		SystemCmdParameters systemParams =
				new SystemCmdParameters();
		PostProcessingCmdParameters postParams =
				new PostProcessingCmdParameters();
		
		inputParams.inputLogFile = new File(filePath);
		postParams.supportThreshold = supportSlider.getValue() / 100;
		postParams.confidenceThreshold = confidenceSlider.getValue() / 100;
		postParams.interestFactorThreshold = interestSlider.getValue() / 100;
		postParams.postProcessingAnalysisType = PostProcessingAnalysisType.HIERARCHYCONFLICTREDUNDANCYDOUBLE;
		postParams.cropRedundantAndInconsistentConstraints = true;
		
		// Run the discovery algorithm
		System.out.println("Running the discovery algorithm...");
		
		MinerFulMinerLauncher miFuMiLa = new MinerFulMinerLauncher(inputParams, minerFulParams, postParams, systemParams);
		ProcessModel processModel = miFuMiLa.mine();
		
		System.out.println(processModel);
		MyController2 mc = new MyController2(this.stage,this.files);
		//drawOutputAlt(getConstraintParametersMap(processModel), getTemplatesMap(processModel));
		Browser b = drawInOutputPane(processModel,activitySupportMap);
		TaskCharArchive archive = new TaskCharArchive(processModel.getTaskCharArchive().getCopyOfTaskChars());
		ConstraintsBag bag = (ConstraintsBag) processModel.bag.clone();
		ProcessModel pm = new ProcessModel(archive,bag);
		FilterMinerFulController fmc = addFilterValues(processModel, pm, postParams,mc.outputPane);
		mc.setOutputPaneChildren(b);
		mc.setFilterPaneMinerFul(fmc);
		this.stage.setScene(new Scene(mc,this.stage.getScene().getWidth(),this.stage.getScene().getHeight()));
		this.stage.setMaximized(true);
		this.stage.setTitle("Hello World");
		this.stage.show();
		//this.stage.close();
	}*/
	
	private HashMap<Integer, Boolean> isPlottedMap(Set<Integer> keys) {
		HashMap<Integer, Boolean> map = new HashMap<Integer,Boolean>();
		keys.forEach(k -> map.put(k, false));
		return map;
	}
	
	private String findExistenceConstraint(String activity, HashMap<Integer,Boolean> plotMap, HashMap<Integer,List<String>> parametersMap, HashMap<Integer,String> templatesMap) {
		List<String> constraints = new ArrayList<String>();
		parametersMap.forEach((k,v) -> {
			if(plotMap.get(k)!= null && !plotMap.get(k) && v.size() == 1) {
				if(v.get(0).equals(activity)) {
					constraints.add(templatesMap.get(k));
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
	
	private int findActivity(List<String> activity,HashMap<Integer,Boolean> plotMap,HashMap<Integer,List<String>> constraintMap) {
		AtomicInteger foundId = new AtomicInteger();
		plotMap.forEach((k,drawn) -> {
			if(!drawn && foundId.get() == 0) {
				List<String> obtained = constraintMap.get(k);
				if(obtained.size() == 2 && activity.get(1).equals(obtained.get(0))) foundId.set(k);
			}
		});
		return foundId.get();
	}
	
	private boolean equalsIgnoreOrder(List<String> l1, List<String> l2) {
		List<String> cl1 = new ArrayList<String>(l1);
		List<String> cl2 = new ArrayList<String>(l2);
		cl1.sort((s1,s2) -> {
			return s1.compareTo(s2);
		});
		cl2.sort((s1,s2) -> {
			return s1.compareTo(s2);
		});
		return cl1.equals(cl2);
	}
	
	public Browser drawInOutputPane(ProcessModel processModel, Map<String,Double> actSuppMap, String view) {
		Map<Integer,String> allActivities = getAllActivities(processModel);
		Map<Integer,ConstraintTemplate> templatesMap = getTemplatesMap(processModel);
		Map<Integer,List<String>> constraintParametersMap = getConstraintParametersMap(processModel);
		Map<Integer,Double> constraintSuppMap = getConstraintSuppMap(getValuesMap(processModel));
		
		Set<Integer> remove = new HashSet<Integer>();
		for(int k: constraintParametersMap.keySet()) {
			List<String> vk = constraintParametersMap.get(k);
			for(int j: constraintParametersMap.keySet()) {
				ConstraintTemplate template1 = templatesMap.get(k);
				if(Templates.isParentOf("Co-Existence", template1.toString())) {
					if(j > k && equalsIgnoreOrder(vk, constraintParametersMap.get(j))) {
						ConstraintTemplate template2 = templatesMap.get(j);
						if(template1.equals(template2)) {
							remove.add(j);
						}
					}
				}
				else {
					if(j > k && vk.equals(constraintParametersMap.get(j))) {
						ConstraintTemplate template2 = templatesMap.get(j);
						if(template1.equals(template2)) {
							remove.add(j);
						}
					}
				}
			}
		}
		remove.forEach(r -> {
			templatesMap.remove(r);
			constraintParametersMap.remove(r);
			constraintSuppMap.remove(r);
		});
		
		return GraphGenerator.browserify(
				allActivities, 
				getActSuppMap(allActivities,actSuppMap), 
				templatesMap, 
				constraintParametersMap, 
				constraintSuppMap, 
				zoom,view,"event occurences");
	}
	
	public Map<Integer,Double> getActSuppMap(Map<Integer,String> aM, Map<String,Double> asM) {
		Map<Integer,Double> map = new HashMap<Integer,Double>();
		aM.forEach((k,v) -> {
			map.put(k, asM.get(v));
		});
		return map;
	}
	
	public Map<Integer,Double> getConstraintSuppMap(Map<Integer,List<Double>> vM) {
		Map<Integer,Double> map = new HashMap<Integer,Double>();
		vM.forEach((k,v) -> {
			map.put(k, v.get(0));
		});
		return map;
	}
	
	public String findAllExistenceConstraints(HashMap<Integer,List<String>> constraintParameters,HashMap<Integer,String> templates, String activity, Set<Integer> picked, HashMap<Integer,Boolean> isDrawn, Map<Integer,List<Double>> valuesMap, String metric) {
		List<String> list = Arrays.asList(activity);
		List<Integer> keys = new ArrayList<Integer>();
		constraintParameters.forEach((k,v) -> {
			if(v.equals(list) && picked.contains(k) && !isDrawn.get(k)) keys.add(k);
		});
		String existence = "";
		List<String> templateList = new ArrayList<String>();
		for(int k: keys) {
			if(!isDrawn.get(k)) {
				isDrawn.put(k,true);
				/*if(metric.equals("Support")) {
					String supp = "_"+String.format("%.2f%%", valuesMap.get(k).get(0)*100);
					templateList.add(templates.get(k)+supp);
				}
				else if(metric.equals("Confidence")) {
					String supp = "_"+String.format("%.2f%%", valuesMap.get(k).get(1)*100);
					templateList.add(templates.get(k)+supp);
				}
				else if(metric.equals("Interest Factor")) {
					String supp = "_"+String.format("%.2f%%", valuesMap.get(k).get(2)*100);
					templateList.add(templates.get(k)+supp);
				}*/
				
				templateList.add(templates.get(k));
				
				//existence += templates.get(k) + "\n";
			}
		}
		Set<String> templateSet = new HashSet<String>(templateList);
		for(String s: templateSet) {
			existence += s + "\n";
		}
		if(existence.equals("")) return existence;
		else return existence+"\n";
	}
	
	public Browser drawGraph(HashMap<Integer,String> allActivities,HashMap<Integer,List<String>> constraintParameters, HashMap<Integer,String> templates, HashMap<Integer,List<Double>> valuesMap,String metric,Map<String,Double> actSuppMap) {
		//outputPane.getChildren().clear();
		VisGraph graph = new VisGraph();
		Set<Integer> keys = constraintParameters.keySet();
		List<VisEdge> listOfEdges = new ArrayList<VisEdge>();
		HashMap<String,List<String>> adjacencyMap = new HashMap<String,List<String>>();
		HashMap<Integer,Boolean> isDrawn = new HashMap<Integer,Boolean>();
		HashMap<Integer,VisNode> allNodes = new HashMap<Integer,VisNode>();
		HashMap<TreeSet<Integer>,Integer> offsetMap = new HashMap<TreeSet<Integer>,Integer>();
		keys.forEach(k -> isDrawn.put(k, false));
		allActivities.forEach((k,v) -> {
			List<String> l = new ArrayList<String>();
			constraintParameters.forEach((kk,list) -> {
				if(isValidKey(templates,kk) && !isDrawn.get(kk) && list.size() == 2 && list.get(0).equals(v)) {
					isDrawn.put(kk, true);
					String str = list.get(1);
					l.add(str);
					allActivities.forEach((kkk,vvv) -> {
						if(vvv.equals(str)) {
							String ex1 = findAllExistenceConstraints(constraintParameters,templates, v, keys, isDrawn,valuesMap,metric);
							String ex2 = findAllExistenceConstraints(constraintParameters,templates, vvv, keys, isDrawn,valuesMap,metric);
							VisNode start = allNodes.get(k);
							if(start == null) {
								start = new VisNode(k,ex1+v,"Will be inserted");
								double supp = actSuppMap.get(v);
								start.setLabel(start.getLabel()+"_"+String.format("%.2f%%", supp*100));
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
								end = new VisNode(kkk,ex2+vvv,"Will be inserted");
								double supp = actSuppMap.get(vvv);
								end.setLabel(end.getLabel()+"_"+String.format("%.2f%%", supp*100));
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
							VisEdge edge = getCorrespondingEdge(start,end,DeclareTemplate.valueOf(templates.get(kk)),value);
							edge.setId(kk);
							if(metric.equals("Support")) {
								double metricValue = valuesMap.get(kk).get(0);
								edge.setLabel(String.format("%.2f%%", metricValue*100));
								edge.setWidth(4*metricValue);
							}
							else if(metric.equals("Confidence")) {
								double metricValue = valuesMap.get(kk).get(1);
								edge.setLabel(String.format("%.2f%%", metricValue*100));
								edge.setWidth(4*metricValue);
							}
							else if(metric.equals("Interest Factor")) {
								double metricValue = valuesMap.get(kk).get(2);
								edge.setLabel(String.format("%.2f%%", metricValue*100));
								edge.setWidth(4*metricValue);
							}
							offsetMap.put(offsetKey, ++value);
							listOfEdges.add(edge);
						}
					});
				}
			});
			adjacencyMap.put(v, l);
		});
		System.out.println("Map: "+isDrawn);
		System.out.println("Node Map: "+allNodes);
		List<Integer> remainingSingle = new ArrayList<Integer>();
		isDrawn.forEach((k,v) -> {
			if(!v) {
				remainingSingle.add(k);
			}
 		});
		for(int k: remainingSingle) {
			String activity = constraintParameters.get(k).get(0);
			String exA = findAllExistenceConstraints(constraintParameters,templates, activity, keys, isDrawn, valuesMap, metric);
			allActivities.forEach((kk,vv) -> {
				if(vv.equals(activity)) {
					VisNode node = allNodes.get(kk);
					if(node == null) {
						node = new VisNode(kk,exA+activity,"Will be inserted");
						double supp = actSuppMap.get(vv);
						node.setLabel(node.getLabel()+"_"+String.format("%.2f%%", supp*100));
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
		/*for(int i=0; i<nodeArray.length; i++) {
			double angle = portion * (i);
			nodeArray[i].setFixed(Boolean.TRUE);
			nodeArray[i].setX(0 - radius * Math.sin(angle));
			nodeArray[i].setY(0 - radius + radius * Math.cos(angle));
		}*/
		graph.addNodes(nodeArray);		
		graph.addEdges((VisEdge[]) listOfEdges.toArray(new VisEdge[listOfEdges.size()]));
		return new Browser(graph,Screen.getPrimary().getVisualBounds().getHeight()*0.9,Screen.getPrimary().getVisualBounds().getWidth() * 0.75,zoom);
	}
	
	public void drawOutputAlt(HashMap<Integer,List<String>> constraintParameters, HashMap<Integer,String> templates) {
		outputPane.getChildren().clear();
		HashMap<Integer,Boolean> plotMap = isPlottedMap(constraintParameters.keySet());
		Set<Integer> ids = plotMap.keySet();
		int multi1 = 0;
		int multi2 = 0;
		for(int id:ids) {
			if(!plotMap.get(id)) {
				if(constraintParameters.get(id).size() == 1) {
					List<String> activity = constraintParameters.get(id);
					String startActivity = activity.get(0);
	        		double constraintSupport = 0.5;
	        		String exA = findExistenceConstraint(startActivity, plotMap, constraintParameters, templates);
	        		//String exB = findExistenceConstraint(endActivity, plotMap, constraintParameters, templates);
	        		DeclareTemplateController dtc = new DeclareTemplateController(
	        				exA,
	        				0.5,
	        				startActivity,
	        				"",
	        				0.5,
	        				"",
	        				DeclareTemplate.valueOf(templates.get(id)),
	        				constraintSupport
	        				);
	        		dtc.setLayoutX(multi1 * 200.0);
	                outputPane.getChildren().add(dtc);
	                plotMap.put(id, true);
	                multi1++;
				}
				if(constraintParameters.get(id).size() == 2) {
					List<String> activity = constraintParameters.get(id);
					String startActivity = activity.get(0);
	        		String endActivity = activity.get(1);
	        		double constraintSupport = 0.5;
	        		String exA = findExistenceConstraint(startActivity, plotMap, constraintParameters, templates);
	        		String exB = findExistenceConstraint(endActivity, plotMap, constraintParameters, templates);
	        		DeclareTemplateController dtc = new DeclareTemplateController(
	        				exA,
	        				0.5,
	        				startActivity,
	        				exB,
	        				0.5,
	        				endActivity,
	        				DeclareTemplate.valueOf(templates.get(id)),
	        				constraintSupport
	        				);
	        		dtc.setLayoutX(multi2 * 200.0);
	        		dtc.setLayoutY(130.0);
	                outputPane.getChildren().add(dtc);
	                plotMap.put(id, true);
	                multi2++;
	                int other = findActivity(activity,plotMap,constraintParameters);
	                if(other != 0) {
	                	activity = constraintParameters.get(other);
						startActivity = activity.get(0);
		        		endActivity = activity.get(1);
		        		constraintSupport = 0.5;
		        		exA = findExistenceConstraint(startActivity, plotMap, constraintParameters, templates);
		        		exB = findExistenceConstraint(endActivity, plotMap, constraintParameters, templates);
		        		DeclareTemplateController dtc2 = new DeclareTemplateController(
		        				exA,
		        				0.5,
		        				startActivity,
		        				exB,
		        				0.5,
		        				endActivity,
		        				DeclareTemplate.valueOf(templates.get(other)),
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
	
	public HashMap<Integer, List<String>> getConstraintParametersMap(ProcessModel processModel) {
		HashMap<Integer, List<String>> map = new HashMap<>();
		int index = 0;
		
		for(Constraint c : processModel.getAllConstraints()) {
			if(c.getSupport() < this.constraintSupportThreshold)
				continue;
			
			List<String> list = c.getParameters().stream()
					.map(TaskCharSet::toString).collect(Collectors.toList());
			
			boolean isSafe = list.stream()
					.map(s -> (activitySupportMap.get(s) >= activitySupportThreshold))
					.reduce(true, (a,b) -> a && b);
			
			if(isSafe && isSelectedTemplate(ConstraintUtils.getTemplateFromMinerful(c)))
				map.put(++index, list);
		}
		
		return map;
	}
	
	public HashMap<Integer,String> getAllActivities(ProcessModel processModel) {
		HashMap<Integer, String> allActivities = new HashMap<>();
		int index = 0;
		
		for(TaskChar tc : processModel.getTasks())
			if(activitySupportMap.get(tc.toString()) >= activitySupportThreshold)
				allActivities.put(++index, tc.toString());
		
		return allActivities;
	}
	
	
	public HashMap<Integer, ConstraintTemplate> getTemplatesMap(ProcessModel processModel) {
		HashMap<Integer, ConstraintTemplate> map = new HashMap<>();
		int index = 0;
		for(Constraint c : processModel.getAllConstraints()) {
			if(c.getSupport() < this.constraintSupportThreshold)
				continue;
			
			List<String> list = c.getParameters().stream()
					.map(TaskCharSet::toString).collect(Collectors.toList());
			
			boolean isSafe = list.stream()
					.map(s -> (activitySupportMap.get(s) >= activitySupportThreshold))
					.reduce(true, (a,b) -> a && b);
			
			if(isSafe)
				map.put(++index, ConstraintUtils.getTemplateFromMinerful(c));
		}
		
		return map;
	}

	public HashMap<Integer, List<Double>> getValuesMap(ProcessModel processModel) {
		HashMap<Integer, List<Double>> map = new HashMap<>();
		int index = 0;
		
		for(Constraint c : processModel.getAllConstraints()) {
			if(c.getSupport() < this.constraintSupportThreshold)
				continue;
			
			List<String> list = c.getParameters().stream()
					.map(TaskCharSet::toString).collect(Collectors.toList());
			
			boolean isSafe = list.stream()
					.map(s -> (activitySupportMap.get(s) >= activitySupportThreshold))
					.reduce(true, (a,b) -> a && b);
			
			if(isSafe && isSelectedTemplate(ConstraintUtils.getTemplateFromMinerful(c))) {
				double support = c.getSupport();
				double confidence = c.getConfidence();
				double interest = c.getInterestFactor();
				map.put(++index, Arrays.asList(support,confidence,interest));
			}
		}
		
		return map;
	}
	
	public VisEdge getCorrespondingEdge(VisNode start, VisNode end, DeclareTemplate template,int offset) {
		int last = start.getLabel().lastIndexOf('\n');
		String startActivity = start.getLabel().substring(last+1);
		last = end.getLabel().lastIndexOf('\n');
		String endActivity = end.getLabel().substring(last+1);
		
		int underline = startActivity.lastIndexOf('_');
		if(underline != -1) startActivity = startActivity.substring(0, underline);
		
		underline = endActivity.lastIndexOf('_');
		if(underline != -1) endActivity = endActivity.substring(0, underline);
		
		String constraint = template.name(); // + "(" +startActivity+", "+endActivity+")";
		if(template == DeclareTemplate.Responded_Existence) {
			Smooth smooth = new Smooth(true, "dynamic");
			return new VisEdge(start,end,null,smooth,constraint);//,diff*400*Math.pow(1.25, offset));
		}
		if(template == DeclareTemplate.Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);
			
			Smooth smooth = new Smooth(true,"dynamic");
			return new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
		}
		if(template == DeclareTemplate.Alternate_Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);
			
			Smooth smooth = new Smooth(true,"doubleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			Arrows arrows = new Arrows(to,null,null);
			
			Smooth smooth = new Smooth(true,"dynamic");
			return new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
		}
		if(template == DeclareTemplate.Alternate_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			Arrows arrows = new Arrows(to,null,null);
			
			Smooth smooth = new Smooth(true,"doubleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Chain_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			Arrows arrows = new Arrows(to,null,null);
			
			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			return new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
		}
		if(template == DeclareTemplate.Not_CoExistence) {
			ArrowProperty to = new ArrowProperty(true,"circle");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			return new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
		}
		if(template == DeclareTemplate.Not_Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);
			
			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
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
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		return new VisEdge(start,end,null,null,constraint);//diff*400*Math.pow(1.25, offset));
 	}

}
