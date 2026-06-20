package controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import test.TestRunner;
import view.Browser;

public class MyController2 extends VBox {
	
	public Stage stage;
	
	//private Stage configStage;
	
	@FXML
	public AnchorPane outputPane;
	
	@FXML
	public AnchorPane filterPane;
	
	@FXML
	private SplitPane mainPane;
	
	@FXML
	private Menu mining;
	
	private DeclareMinerOutput declareMinerOutput;
	
	//private String filePath;
	//private Browser browser;
	//private FilterController filterController;
	
	private List<File> files;
	
	private Browser currentBrowser;
	
	private FilterController currentFC;
	
	private FilterMinerFulController currentFMC;
	
	public MyController2(Stage stage, List<File> files) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/MyView2.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        this.stage = stage;
        this.files = files;
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
	
	public Browser getCurrentBrowser() {
		return this.currentBrowser;
	}
	
	public FilterController getCurrentFC() {
		return this.currentFC;
	}
	
	public FilterMinerFulController getCurrentFMC() {
		return this.currentFMC;
	}
	
	public void setOutputPaneChildren(Browser b) {
		this.currentBrowser = b;
		this.outputPane.getChildren().clear();
		this.outputPane.getChildren().add(b);
	}
	
	public void setFilterPaneChildren(FilterController fc) {
		this.currentFC = fc;
		this.filterPane.getChildren().clear();
		this.filterPane.getChildren().add(fc);
	}
	
	public void setFilterPaneMinerFul(FilterMinerFulController fmc) {
		this.currentFMC = fmc;
		this.filterPane.getChildren().clear();
		this.filterPane.getChildren().add(fmc);
	}
	
	public List<File> getFiles() {
		return this.files;
	}
	
	@FXML
	public void openConfigScene(String filePath) {
		//this.configStage = new Stage();
		this.stage.setTitle("Configuration for DeclareMiner");
		this.stage.setUserData(this);
		this.stage.setScene(new Scene(new ConfigurationController(this.stage, filePath, "DeclareMiner", this.files), Screen.getPrimary().getVisualBounds().getWidth(), Screen.getPrimary().getVisualBounds().getHeight()));
		this.stage.setMaximized(true);
		this.stage.show();
	}
	
	@FXML
	public void openConfigScene2(String filePath) {
		//this.configStage = new Stage();
		this.stage.setTitle("Configuration for MinerFul");
		this.stage.setUserData(this);
		this.stage.setScene(new Scene(new ConfigurationMinerFulController(this.stage,filePath,outputPane,filterPane,this.files), Screen.getPrimary().getVisualBounds().getWidth(), Screen.getPrimary().getVisualBounds().getHeight()));
		this.stage.setMaximized(true);
		this.stage.show();
	}
	
	@FXML
	public void selectNewFile() {
		//this.mining.getItems().clear();
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showOpenDialog(stage);
		//String absPath = "";
        if (file != null) {
            this.files.add(file);
            addMenu(file);
        }
	}
	
	private void addMenu(File file) {
		String fileName = file.getName();
		Menu m = new Menu();
		m.setText(fileName);
		
		MenuItem mi = new MenuItem();
		mi.setText("DeclareMiner");
		mi.setOnAction(_e -> openConfigScene(file.getAbsolutePath()));
		
		MenuItem mi2 = new MenuItem();
		mi2.setText("MinerFul");
		mi2.setOnAction(_e -> openConfigScene2(file.getAbsolutePath()));
		
		//int lastDot = fileName.lastIndexOf('.');
		
		//if(fileName.substring(lastDot + 1).equals("mxml")) m.getItems().addAll(mi);
		m.getItems().addAll(mi,mi2);
		
		this.mining.getItems().add(m);
	}
	
	@FXML
	public void openModelConstruct() {
		//Stage stage = new Stage();
		stage.setTitle("Model Construction");
		stage.setUserData(this);
		stage.setScene(new Scene(new ConstructionController(stage,new ArrayList<String>()),this.getScene().getWidth(),this.getScene().getHeight()));
		stage.setMaximized(true);
		stage.show();
	}
	
	/*@FXML
	public void openTabbedConstruct() {
		stage.setTitle("Model Construction");
		stage.setUserData(this);
		stage.setScene(new Scene(new TabbedConstructionController(stage),this.getScene().getWidth(),this.getScene().getHeight()));
		stage.setMaximized(true);
		stage.show();
	}*/
	
	@FXML
	public void buildModel() {
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showOpenDialog(stage);
		String absPath = "";
        if (file != null) {
        	absPath = file.getAbsolutePath();
        }
        
        Configuration configuration = new Configuration();
		XLog log = null;
		try {
			log = XLogReader.openLog(absPath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		configuration.log = log;
		
		DeclareMinerInput input = new DeclareMinerInput();
		List<DeclareTemplate> templates = new LinkedList<DeclareTemplate>();
		List<String> selectedTemplates = new LinkedList<String>();
		
		selectedTemplates.add("Response");
		
		for(String s:selectedTemplates) {
			templates.add(DeclareTemplate.valueOf(s));
		}
		int min_support = 60;
		int alpha = 0;
		//String miner = "DeclareMiner";
		
		input.setSelectedDeclareTemplateSet(new HashSet<DeclareTemplate>(templates));
		
		Set<DeclarePerspective> persp_set = Collections.singleton(DeclarePerspective.valueOf("Control_Flow"));
		input.setDeclarePerspectiveSet(persp_set);
		
		Set<DeclareTemplate> selectedDeclareTemplateSet = new HashSet<DeclareTemplate>();
		DeclareTemplate[] declareTemplates = DeclareTemplate.values();
		for(DeclareTemplate d : declareTemplates)
			selectedDeclareTemplateSet.add(d);
	
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
		DeclareMinerOutput output = getDeclareMinerOutput(configuration);
		this.declareMinerOutput = output;
		drawOutputAlt(getOutput(output), outputPane);
		//addFilterValues(filterPane, output);
	}
	
	/*private void addFilterValues(AnchorPane filterPane, DeclareMinerOutput declareMinerOutput) {
		filterPane.getChildren().clear();
		filterPane.getChildren().add(new FilterController(declareMinerOutput, outputPane));
	}*/
	
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
	
	private String getDeclareTemplate(String str) {
		int leftBracket = str.indexOf('(');
		return str.substring(0, leftBracket);
	}
	
	public void drawOutput(DeclareMinerOutput output, AnchorPane outputPane) {
		outputPane.getChildren().clear();
		List<String> outputList = getOutput(output);
		//int j = 0;
        for(int i=0; i<outputList.size(); i++) {
        	String activity = outputList.get(i);
        	
        	if(getDeclareTemplate(activity).equalsIgnoreCase("response")) {
        		TestRunner tr = new TestRunner();
        		String startActivity = tr.startEnd(activity).first;
        		String endActivity = tr.startEnd(activity).second;
        		double constraintSupport = getConstraintSupport(startActivity,endActivity,output);
        		ResponseController rc2 = new ResponseController(
        				startActivity, getActivitySupport(startActivity, output),
        				endActivity, getActivitySupport(endActivity, output),
        				constraintSupport
        				);
                //rc2.setLayoutX(j * 200.0);
                outputPane.getChildren().add(rc2);
                //j++;
        	}
        	
        }
	}
	
	private HashMap<Integer, Boolean> isPlottedMap(List<String> outputList) {
		HashMap<Integer, Boolean> map = new HashMap<Integer,Boolean>();
		TestRunner tr = new TestRunner();
		for(String s:outputList) {
			String startActivity = tr.startEnd(s).first;
    		String endActivity = tr.startEnd(s).second;
    		List<String> activity = Arrays.asList(startActivity,endActivity);
    		declareMinerOutput.getConstraintParametersMap().forEach((k,v) -> {
    			if(v.equals(activity)) map.put(k,false);
    		}); 
		}
		return map;
	}
	
	private int findActivity(List<String> activity,HashMap<Integer,Boolean> plotMap) {
		AtomicInteger foundId = new AtomicInteger();
		plotMap.forEach((k,drawn) -> {
			if(!drawn && foundId.get() == 0) {
				List<String> obtained = declareMinerOutput.getConstraintParametersMap().get(k);
				if(activity.get(1).equals(obtained.get(0))) foundId.set(k);
			}
		});
		return foundId.get();
	}
	
	public void drawOutputAlt(List<String> output, AnchorPane outputPane) {
		outputPane.getChildren().clear();
		HashMap<Integer,Boolean> plotMap = isPlottedMap(output);
		Set<Integer> ids = plotMap.keySet();
		int multi = 0;
		for(int id:ids) {
			if(!plotMap.get(id)) {
				if(declareMinerOutput.getTemplate().get(id).equals(DeclareTemplate.Init)) {
					List<String> activity = declareMinerOutput.getConstraintParametersMap().get(id);
					String startActivity = activity.get(0);
	        		InitController rc = new InitController(
	        				startActivity, 
	        				String.format("Support: %.2f",getActivitySupport(startActivity,declareMinerOutput))
	        				);
	        		rc.setLayoutX(multi * 200.0);
	                outputPane.getChildren().add(rc);
	                multi++;
				}
				if(declareMinerOutput.getTemplate().get(id).equals(DeclareTemplate.Response)) {
					List<String> activity = declareMinerOutput.getConstraintParametersMap().get(id);
					String startActivity = activity.get(0);
	        		String endActivity = activity.get(1);
	        		double constraintSupport = getConstraintSupport(startActivity,endActivity,declareMinerOutput);
	        		ResponseController rc = new ResponseController(
	        				startActivity, getActivitySupport(startActivity,declareMinerOutput),
	        				endActivity, getActivitySupport(endActivity,declareMinerOutput),
	        				constraintSupport
	        				);
	        		rc.setLayoutX(multi * 200.0);
	                outputPane.getChildren().add(rc);
	                plotMap.put(id, true);
	                multi++;
	                int other = findActivity(activity,plotMap);
	                if(other != 0) {
	                	activity = declareMinerOutput.getConstraintParametersMap().get(other);
						startActivity = activity.get(0);
		        		endActivity = activity.get(1);
		        		constraintSupport = getConstraintSupport(startActivity,endActivity,declareMinerOutput);
		        		ResponseController rc2 = new ResponseController(
		        				startActivity, getActivitySupport(startActivity, declareMinerOutput),
		        				endActivity, getActivitySupport(endActivity, declareMinerOutput),
		        				constraintSupport
		        				);
		        		rc2.setLayoutX(rc.getLayoutX());
		        		rc2.setLayoutY(rc.getLayoutY()+243.0);
		                outputPane.getChildren().add(rc2);
		                plotMap.put(other, true);
	                }
	                
				}
			}
		}
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
	
	private DeclareMinerOutput getDeclareMinerOutput(Configuration configuration) {
		Configuration conf = configuration;
		conf.setUnifiedLoggerPrunerType("replayers"); // will be obsolete after testing is done

		String miner_type = "DeclareMiner";
		DeclareMinerOutput output = new DeclareMinerOutput();
		// Choose the Miner type based on configuration
		// TODO
		if (conf.log != null && conf.input != null){
			if (miner_type.equals("DeclareMiner"))
				output = DeclareMiner.mineDeclareConstraints(null, conf.log, conf.input);
			else if (miner_type.equals("DeclareMinerNoHierarc"))
				output = DeclareMinerNoHierarc.mineDeclareConstraints(null, conf.log, conf.input);
			else if (miner_type.equals("DeclareMinerNoRed"))
				output = DeclareMinerNoRed.mineDeclareConstraints(null, conf.log, conf.input);
			else if (miner_type.equals("DeclareMinerNoTrans"))
				output = DeclareMinerNoTrans.mineDeclareConstraints(null, conf.log, conf.input);
			else
				throw new IllegalArgumentException(String.format("Invalid miner type '%s'", miner_type));

		} else {
			throw new IllegalArgumentException("No valid argument combination found");
		}
		return output;
	}
	
	@FXML
	public void initialize() {
		//mainPane.setDividerPositions(0.75);
		for(File f: this.files) {
			addMenu(f);
		}
	}
}
