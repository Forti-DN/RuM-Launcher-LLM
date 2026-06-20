package controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XLog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import minerful.concept.ProcessModel;
import minerful.concept.TaskChar;
import minerful.concept.TaskCharArchive;
import minerful.concept.TaskCharFactory;
import minerful.concept.constraint.Constraint;
import minerful.concept.constraint.ConstraintsBag;
import minerful.concept.constraint.existence.AtMost1;
import minerful.concept.constraint.existence.Exactly1;
import minerful.concept.constraint.existence.Init;
import minerful.concept.constraint.existence.AtLeast1;
import minerful.concept.constraint.relation.AlternatePrecedence;
import minerful.concept.constraint.relation.AlternateResponse;
import minerful.concept.constraint.relation.AlternateSuccession;
import minerful.concept.constraint.relation.ChainPrecedence;
import minerful.concept.constraint.relation.ChainResponse;
import minerful.concept.constraint.relation.ChainSuccession;
import minerful.concept.constraint.relation.CoExistence;
import minerful.concept.constraint.relation.NotChainSuccession;
import minerful.concept.constraint.relation.NotCoExistence;
import minerful.concept.constraint.relation.NotSuccession;
import minerful.concept.constraint.relation.Precedence;
import minerful.concept.constraint.relation.RespondedExistence;
import minerful.concept.constraint.relation.Response;
import minerful.concept.constraint.relation.Succession;
import minerful.logmaker.MinerFulLogMaker;
import minerful.logmaker.params.LogMakerParameters;
import minerful.logmaker.params.LogMakerParameters.Encoding;

public class ConstructionController extends Pane {
	
	@FXML
	public ListView<String> templateList;
	
	@FXML
	private TextField modelName;
	
	@FXML
	private TextField numberOfTraces;
	
	@FXML
	private TextField minTraceLength;
	
	@FXML
	private TextField maxTraceLength;
	
	private Stage stage;
	
	private List<String> templates;
	
	public ConstructionController(Stage stage, List<String> templates) {
		this.stage = stage;
		this.templates = templates;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/Construction.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
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
	
	@FXML
	public void addTemplate() {
		//Stage stage = new Stage();
		stage.setTitle("New Declare Template");
		stage.setScene(new Scene(new ModelSelectionController(this.templates,stage),this.getScene().getWidth(),this.getScene().getHeight()));
		stage.setMaximized(true);
		stage.show();
	}
	
	@FXML
	public void initialize() {
		ObservableList<String> items =FXCollections.observableArrayList ();
		items.addAll(this.templates);
		templateList.setItems(items);
	}
	
	@FXML
	public void create() throws Exception {
		long numberOfTraces = Long.valueOf(this.numberOfTraces.getText());
		int minTraceLength = Integer.valueOf(this.minTraceLength.getText());
		int maxTraceLength = Integer.valueOf(this.maxTraceLength.getText());
		List<String> allActivitiesInvolved = new ArrayList<String>();
		for(String s : templateList.getItems()) {
			allActivitiesInvolved.addAll(getInvolvedActivities(s));
		}
		Set<String> allActivities = new TreeSet<String>(allActivitiesInvolved);
		System.out.println(allActivities);
		System.out.printf("Number of traces: %d, min trace length: %d, max trace length: %d\n",numberOfTraces,minTraceLength,maxTraceLength);
		
		TaskCharFactory tChFactory = new TaskCharFactory();
		
		List<TaskChar> tcList = allActivities.stream().map(activity -> tChFactory.makeTaskChar(activity)).collect(Collectors.toList());
		TaskChar[] tcArray = (TaskChar[]) tcList.toArray(new TaskChar[tcList.size()]); 
		TaskCharArchive taChaAr = new TaskCharArchive(tcArray);
		
		ConstraintsBag bag = new ConstraintsBag(taChaAr.getTaskChars());
		System.out.println(bag);
		
		for(String s : templateList.getItems()) {
			String temp = getWithoutParanthesis(s);
			List<TaskChar> involved = getInvolvedActivities(s).stream().map(activity -> taChaAr.getTaskChar(activity)).collect(Collectors.toList());
			Constraint constraint = getConstraint(temp, involved);
			if(constraint != null) bag.add(constraint);
		}
		
		System.out.println(bag);
		ProcessModel proMod = new ProcessModel(taChaAr, bag);
		LogMakerParameters logMakParameters =
				new LogMakerParameters(
						minTraceLength, maxTraceLength, numberOfTraces);
		
		MinerFulLogMaker logMak = new MinerFulLogMaker(logMakParameters);

		// Create the event log
		XLog log = logMak.createLog(proMod);

		// Store the log
		logMakParameters.outputEncoding = Encoding.xes;
		logMakParameters.outputLogFile = new File(System.getProperty("user.dir")+System.getProperty("file.separator")+modelName.getText()+".xes");
		logMak.storeLog();
		
		//new ConfigurationMinerFulController(outputPane).drawInOutputPane(proMod);
		
		//this.stage.close();
		MyController2 saved = (MyController2) this.stage.getUserData();
		MyController2 mc = new MyController2(this.stage,saved.getFiles());
		
		//ConfigurationMinerFulController controller = new ConfigurationMinerFulController();
		//mc.setOutputPaneChildren(controller.drawInOutputPane(proMod));
		
		//TaskCharArchive archive = new TaskCharArchive(proMod.getTaskCharArchive().getCopyOfTaskChars());
		//ConstraintsBag bag2 = (ConstraintsBag) proMod.bag.clone();
		//ProcessModel pm = new ProcessModel(archive,bag2);
		/*PostProcessingCmdParameters postParams =
				new PostProcessingCmdParameters();
		postParams.postProcessingAnalysisType = PostProcessingAnalysisType.HIERARCHYCONFLICTREDUNDANCYDOUBLE;
		postParams.cropRedundantAndInconsistentConstraints = true;*/
		
		//mc.setFilterPaneMinerFul(controller.addFilterValues(proMod, pm, postParams, mc.outputPane));
		this.stage.setScene(new Scene(mc,this.stage.getScene().getWidth(),this.stage.getScene().getHeight()));
		this.stage.setMaximized(true);
		this.stage.setTitle("Hello World");
		this.stage.show();
	}
	
	private List<String> getInvolvedActivities(String template) {
		int leftP = template.indexOf('(');
		int rightP = template.indexOf(')');
		String inBetween = template.substring(leftP+1, rightP);
		int comma = inBetween.indexOf(',');
		if(comma != -1) {
			return Arrays.asList(inBetween.substring(0, comma), inBetween.substring(comma+2));
		}
		else {
			return Arrays.asList(inBetween);
		}
	}
	
	private String getWithoutParanthesis(String selected) {
		int index = selected.indexOf('(');
		return selected.substring(0, index);
	}
	
	private void printTemplateInfo(String template, List<TaskChar> involved) {
		System.out.println(template+" "+involved);
	}
	
	private Constraint getConstraint(String template, List<TaskChar> involved) {
		if(involved.size() == 1) {
			if(template.equals("Init")) {
				return new Init(involved.get(0));
			}
			else if(template.startsWith("Existence")) {
				return new AtLeast1(involved.get(0));
			}
			else if(template.equals("Exactly1")) {
				return new Exactly1(involved.get(0));
			}
			else if(template.equals("Absence2")) {
				return new AtMost1(involved.get(0));
			}
			else return null;
		}
		else {
			if(template.equals("Response")) {
				return new Response(involved.get(0),involved.get(1));
			}
			if(template.equals("Alternate Response")) {
				return new AlternateResponse(involved.get(0),involved.get(1));
			}
			if(template.equals("Chain Response")) {
				return new ChainResponse(involved.get(0),involved.get(1));
			}
			if(template.equals("Precedence")) {
				return new Precedence(involved.get(0),involved.get(1));
			}
			if(template.equals("Alternate Precedence")) {
				return new AlternatePrecedence(involved.get(0),involved.get(1));
			}
			if(template.equals("Chain Precedence")) {
				return new ChainPrecedence(involved.get(0),involved.get(1));
			}
			if(template.equals("Succession")) {
				return new Succession(involved.get(0),involved.get(1));
			}
			if(template.equals("Alternate Succession")) {
				return new AlternateSuccession(involved.get(0),involved.get(1));
			}
			if(template.equals("Chain Succession")) {
				return new ChainSuccession(involved.get(0),involved.get(1));
			}
			if(template.equals("CoExistence")) {
				return new CoExistence(involved.get(0),involved.get(1));
			}
			if(template.equals("Responded Existence")) {
				return new RespondedExistence(involved.get(0),involved.get(1));
			}
			if(template.equals("Not CoExistence")) {
				return new NotCoExistence(involved.get(0),involved.get(1));
			}
			if(template.equals("Not Chain Succession")) {
				return new NotChainSuccession(involved.get(0),involved.get(1));
			}
			if(template.equals("Not Succession")) {
				return new NotSuccession(involved.get(0),involved.get(1));
			}
			return null;
		}
	}
}
