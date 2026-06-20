package controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import declareextraction.constructs.DeclareModel;
import declareextraction.textprocessing.DeclareConstructor;
import org.beldr.InfiniteSpeechRecognizer;
import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;

import core.AlloyRunner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import minerful.concept.TaskChar;
import minerful.concept.constraint.Constraint;
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
import util.AlertUtils;
import util.ConstraintTemplate;
import util.GraphGenerator;
import view.Browser;

public class TabbedConstructionController extends TabPane {
	
	@FXML
	private TreeView<String> allActivities;
	
	@FXML
	private TextField activityName;
	
	@FXML
	private TextField enumDataName;
	
	@FXML
	private TextField enumAttributeValue;
	
	@FXML
	private ListView<String> enumAttributeValues;
	
	@FXML
	private TextField numDataName;
	
	@FXML
	private ChoiceBox<String> numChoice;
	
	@FXML
	private TextField numFrom;
	
	@FXML
	private TextField numTo;
	
	@FXML
	private ChoiceBox<String> activityBoxA;
	
	@FXML
	private TextField actDataA;
	
	@FXML
	private TextField actDataB;
	
	@FXML
	private TextField timeC;
	
	@FXML
	private ChoiceBox<String> activityBoxB;
	
	@FXML
	private Label actBLabel;
	
	@FXML
	private Label actDataBLabel;
	
	@FXML
	private Button dataInfoB;
	
	@FXML
	private ListView<String> templatesList;
	
	@FXML
	private TextArea templateDescription;
	
	@FXML
	private ListView<String> createdConstraintsList;
	
	@FXML
	private Pane modelPane;
	
	@FXML
	private Button addActivityButton;
	
	@FXML
	private Button addDataEnum;
	
	@FXML
	private Button addDataNum;
	
	@FXML
	private Button addAttributeButton;
	
	@FXML
	private Button addConstraintButton;
	
	@FXML
	private AnchorPane timeCondition;
	
	@FXML
	private AnchorPane correlationCondition;
	
	@FXML
	private AnchorPane activationCondition;
	
	private Stage stage;
	
	private Map<String,Set<String>> dataNames;
	
	private Map<String,String> dataDefinitions;
	
	//private Map<String,List<String>> dataHolders;
	
	private File file;
	
	private String inMemory;
	
	private ChoiceBox<String> genChoice;
	
	public TabbedConstructionController(Stage stage,ChoiceBox<String> genChoice) {
		this.stage = stage;
		this.dataNames = new HashMap<String,Set<String>>();
		this.dataDefinitions = new HashMap<String,String>();
		this.genChoice = genChoice;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/TabbedConstruction2.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public TabbedConstructionController(File file,ChoiceBox<String> genChoice,Stage stage) {
		this.file = file;
		this.stage = stage;
		this.dataNames = new HashMap<String,Set<String>>();
		this.dataDefinitions = new HashMap<String,String>();
		this.genChoice = genChoice;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/TabbedConstruction2.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public TabbedConstructionController(String inMemory,ChoiceBox<String> genChoice,Stage stage) {
		this.inMemory = inMemory;
		this.stage = stage;
		this.dataNames = new HashMap<String,Set<String>>();
		this.dataDefinitions = new HashMap<String,String>();
		this.genChoice = genChoice;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/TabbedConstruction2.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
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
			if(template.equals("Alternate_Response")) {
				return new AlternateResponse(involved.get(0),involved.get(1));
			}
			if(template.equals("Chain_Response")) {
				return new ChainResponse(involved.get(0),involved.get(1));
			}
			if(template.equals("Precedence")) {
				return new Precedence(involved.get(0),involved.get(1));
			}
			if(template.equals("Alternate_Precedence")) {
				return new AlternatePrecedence(involved.get(0),involved.get(1));
			}
			if(template.equals("Chain_Precedence")) {
				return new ChainPrecedence(involved.get(0),involved.get(1));
			}
			if(template.equals("Succession")) {
				return new Succession(involved.get(0),involved.get(1));
			}
			if(template.equals("Alternate_Succession")) {
				return new AlternateSuccession(involved.get(0),involved.get(1));
			}
			if(template.equals("Chain_Succession")) {
				return new ChainSuccession(involved.get(0),involved.get(1));
			}
			if(template.equals("CoExistence")) {
				return new CoExistence(involved.get(0),involved.get(1));
			}
			if(template.equals("Responded_Existence")) {
				return new RespondedExistence(involved.get(0),involved.get(1));
			}
			if(template.equals("Not_CoExistence")) {
				return new NotCoExistence(involved.get(0),involved.get(1));
			}
			if(template.equals("Not_Chain_Succession")) {
				return new NotChainSuccession(involved.get(0),involved.get(1));
			}
			if(template.equals("Not_Succession")) {
				return new NotSuccession(involved.get(0),involved.get(1));
			}
			return null;
		}
	}
	
	//@FXML
	/*public void generateLog(String choice) {
		//String choice = modelGeneratorChoice.getSelectionModel().getSelectedItem();
		if(choice == null) {
			showAlert("A generator must be selected");
		}
		else {
			if(modelName.getText().trim().isEmpty()) {
				showAlert("Model name must not be empty");
			}
			else {
				if(minTraceLength.getText().trim().isEmpty()) {
					showAlert("Minimum trace length must not be empty");
				}
				else {
					if(maxTraceLength.getText().trim().isEmpty()) {
						showAlert("Maximum trace length must not be empty");
					}
					else {
						if(amountOfTraces.getText().trim().isEmpty()) {
							showAlert("Amount of traces must not be empty");
						}
						else {
							int i = -1;
							int j = -1;
							int k = -1;
							try {
								i = Integer.valueOf(minTraceLength.getText());
								j = Integer.valueOf(maxTraceLength.getText());
								k = Integer.valueOf(amountOfTraces.getText());
								if(choice.equals("MinerFul Log Generator")) {
									generateWithMinerFul();
								}
								else if(choice.equals("AlloyLogGenerator")) {
									generateWithAlloyLogGen();
								}
							}
							catch(NumberFormatException e) {
								if(i == -1) showAlert("Minimum trace length value is not valid");
								if(j == -1) showAlert("Maximum trace length value is not valid");
								if(k == -1) showAlert("Amount of traces value is not valid");
							}
						}
					}
				}
			}
		}
	}*/
	
	/*public void generateWithMinerFul() {
		long numberOfTraces = Long.valueOf(this.amountOfTraces.getText());
		int minTraceLength = Integer.valueOf(this.minTraceLength.getText());
		int maxTraceLength = Integer.valueOf(this.maxTraceLength.getText());
		List<String> allActivitiesInvolved = new ArrayList<String>(getActivitiesMap().values());
		
		TaskCharFactory tChFactory = new TaskCharFactory();
		
		List<TaskChar> tcList = allActivitiesInvolved.stream().map(activity -> tChFactory.makeTaskChar(activity)).collect(Collectors.toList());
		TaskChar[] tcArray = (TaskChar[]) tcList.toArray(new TaskChar[tcList.size()]); 
		TaskCharArchive taChaAr = new TaskCharArchive(tcArray);
		
		ConstraintsBag bag = new ConstraintsBag(taChaAr.getTaskChars());
		Map<Integer,List<String>> constraintsMap = getConstraintParametersMap();
		Map<Integer,String> templatesMap = getTemplatesMap();
		for(int k:constraintsMap.keySet()) {
			String t = templatesMap.get(k);
			List<String> involvedActivities = constraintsMap.get(k);
			List<TaskChar> involved = involvedActivities.stream().map(activity -> taChaAr.getTaskChar(activity)).collect(Collectors.toList());
			Constraint constraint = getConstraint(t, involved);
			if(constraint != null) bag.add(constraint);
		}
		
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
		try {
			logMak.storeLog();
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Log Generation");
			alert.setHeaderText("Successful");
			alert.setContentText("Log is successfully generated with MinerFul");
			alert.showAndWait();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	/*public void generateWithAlloyLogGen() {
		String tmp_file = obtainTmpFile();
		if(tmp_file != null) {
			StringBuilder builder = new StringBuilder("java -jar AlloyLogGenerator.jar ");
			builder.append(minTraceLength.getText())
			.append(' ').append(maxTraceLength.getText())
			.append(' ').append(amountOfTraces.getText())
			.append(" \"").append(tmp_file)
			.append("\" \"").append(modelName.getText()+".xes").append('"');
            builder.append(" -msi ").append(2)
                .append(" -shuffle ").append(3)
                .append(" -is ").append(1);
            
            String args = builder.toString();
            Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Log Generation");
			alert.setHeaderText("Successful");
			alert.setContentText("Log is successfully generated with AlloyLogGenerator");
			//alert.showAndWait();
			boolean isOk = false;
			StringBuilder errorBuilder = new StringBuilder();
            try {
            	ProcessBuilder pb = new ProcessBuilder(args.split(" "));
            	//pb.inheritIO();
        		Process p = pb.start();
        		isOk = isSuccessful(p.getInputStream(),errorBuilder);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Error");
				alert.setContentText("Log is not generated with AlloyLogGenerator");
				alert.showAndWait();
			}
            if(isOk) {
            	//System.out.println("Oldu be!");
            	alert.showAndWait();
            }
            else {
            	//System.out.println("Hata var!");
            	alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Log is not generated with AlloyLogGenerator");
				alert.setContentText(errorBuilder.toString());
				alert.showAndWait();
            }
		}
	}*/
	
	private boolean isSuccessful(InputStream is,StringBuilder sb) throws Exception{
		BufferedReader reader = 
                new BufferedReader(new InputStreamReader(is));
		//StringBuilder builder = new StringBuilder();
		String line = null;
		while ( (line = reader.readLine()) != null) {
			//System.out.println(line);
			if(line.contains("SUCCESS")) return true;
			if(line.contains("press enter to close")) return false;
			sb.append(line).append("\n");
		}
		return false;
	}
	
	public String getExportData() {
		Map<String,String> allData = new HashMap<String,String>();
		StringBuilder builder = new StringBuilder();
		Pattern p = Pattern.compile("(.*) : (.*)");
		allActivities.getRoot().getChildren().forEach(ti -> {
			builder.append("activity "+ti.getValue()).append('\n');
			List<TreeItem<String>> children = ti.getChildren();
			if(children != null && !children.isEmpty()) {
				builder.append("bind "+ti.getValue()+": ");
				children.forEach(c -> {
					Matcher m = p.matcher(c.getValue());
					if(m.find()) {
						allData.put(m.group(1), m.group(2));
						builder.append(m.group(1));
						builder.append(", ");
					}
				});
				builder.delete(builder.length()-2,builder.length());
				builder.append('\n');
			}
		});
		allData.forEach((k,v) -> {
			if(v.startsWith("{")) {
				builder.append(k+": ");
				builder.append(v.substring(1, v.length()-1));
				builder.append('\n');
			}
			else {
				Pattern p2 = Pattern.compile("(\\w+) (\\[(.+)[.]{2}(.+)\\])");
				Matcher m2 = p2.matcher(v);
				if(m2.find()) {
					builder.append(k+": ");
					builder.append(m2.group(1))
						   .append(" between ")
					       .append(m2.group(3))
					       .append(" and ")
					       .append(m2.group(4)).append("\n");
					
				}
			}
		});
		createdConstraintsList.getItems().forEach(item -> {
			builder.append(item).append('\n');
		});
		return builder.toString();
	}
	
	private String obtainTmpFile() {
		Map<String,String> allData = new HashMap<String,String>();
		StringBuilder builder = new StringBuilder();
		Pattern p = Pattern.compile("(.*) : (.*)");
		allActivities.getRoot().getChildren().forEach(ti -> {
			builder.append("activity "+ti.getValue()).append('\n');
			List<TreeItem<String>> children = ti.getChildren();
			if(children != null && !children.isEmpty()) {
				builder.append("bind "+ti.getValue()+": ");
				children.forEach(c -> {
					Matcher m = p.matcher(c.getValue());
					if(m.find()) {
						allData.put(m.group(1), m.group(2));
						builder.append(m.group(1));
						builder.append(", ");
					}
				});
				builder.delete(builder.length()-2,builder.length());
				builder.append('\n');
			}
		});
		allData.forEach((k,v) -> {
			if(v.startsWith("{")) {
				builder.append(k+": ");
				builder.append(v.substring(1, v.length()-1));
				builder.append('\n');
			}
			else {
				Pattern p2 = Pattern.compile("(\\w+) (\\[(.+)[.]{2}(.+)\\])");
				Matcher m2 = p2.matcher(v);
				if(m2.find()) {
					builder.append(k+": ");
					builder.append(m2.group(1))
						   .append(" between ")
					       .append(m2.group(3))
					       .append(" and ")
					       .append(m2.group(4)).append("\n");
					
				}
			}
		});
		createdConstraintsList.getItems().forEach(item -> {
			builder.append(item).append('\n');
		});
		Path path = Paths.get("im.tmp");
		try (BufferedWriter writer = Files.newBufferedWriter(path))
		{
		    writer.write(builder.toString());
		    writer.close();
		    return path.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
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
	
	private String getDataValue(String s) {
		if(s.startsWith("integer between ")) {
			int l = "integer between ".length();
			String[] values = s.substring(l).split(" and ");
			return "integer ["+values[0]+".."+values[1]+"]";
		}
		if(s.startsWith("float between ")) {
			int l = "float between ".length();
			String[] values = s.substring(l).split(" and ");
			return "float ["+values[0]+".."+values[1]+"]";
		}
		return "{"+s+"}";
	}
	
	private void takeAction(String line) {
		if(line.startsWith("activity")) {
			String activity = line.substring(9);
			TreeItem<String> ti = new TreeItem<String>(activity);
			ti.setExpanded(true);
			allActivities.getRoot().getChildren().add(ti);
			dataNames.put(ti.getValue(), new HashSet<String>());
		}
		else if(line.startsWith("bind")) {
			String mapping = line.substring(5);
			Pattern p = Pattern.compile("(.+): (.+)");
			Matcher m = p.matcher(mapping);
			if(m.find()) {
				String activity = m.group(1);
				String[] data_names = m.group(2).split(",");
				Set<String> set = dataNames.get(activity);
				if(set != null) {
					for(String d: data_names) {
						set.add(d.trim());
						dataDefinitions.put(d.trim(), null);
					}
					dataNames.put(activity, set);
				}
				for(TreeItem<String> ti : allActivities.getRoot().getChildren()) {
					if(ti.getValue().equals(activity)) {
						Set<String> ds = dataNames.get(activity);
						for(String s:ds) {
							ti.getChildren().add(new TreeItem<String>(s));
						}
					}
				}
			}
		}
		else {
			Pattern p = Pattern.compile("(.+): (.+)");
			Matcher m = p.matcher(line);
			if(m.find()) {
				String data_name = m.group(1);
				String data_value = m.group(2);
				for(TreeItem<String> ti : allActivities.getRoot().getChildren()) {
					for(TreeItem<String> data : ti.getChildren()) {
						if(data.getValue().equals(data_name)) {
							String insertValue = getDataValue(data_value);
							data.setValue(data_name+" : "+insertValue);
							dataDefinitions.put(data_name, data.getValue());
						}
					}
				}
			}
			else {
				createdConstraintsList.getItems().add(line);
			}
		}
	}
	
	private void loadInitialValues() {
		if(this.inMemory != null)  {
			try {
				Scanner s = new Scanner(this.inMemory);
				while(s.hasNextLine()) {
					String line = s.nextLine();
					takeAction(line);
				}
				s.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(file != null){
			try {
				Scanner s = new Scanner(file);
				while(s.hasNextLine()) {
					String line = s.nextLine();
					takeAction(line);
				}
				s.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@FXML
	public void initialize() {
		allActivities.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		ObservableList<String> numTypes = FXCollections.observableList(
				Arrays.asList("integer","float"));
		numChoice.setItems(numTypes);
		TreeItem<String> dummy = new TreeItem<String>("root");
		allActivities.setRoot(dummy);
		allActivities.setShowRoot(false);
		
		allActivities.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if(newV != null && !newV.getParent().equals(allActivities.getRoot())) {
				String parseIt = newV.getValue();
				Matcher m = Pattern.compile("(.*) : (.*)").matcher(parseIt);
				if(m.find()) {
					String attributeName = m.group(1);
					String value = m.group(2);
					if(value.startsWith("{")) {
						String[] data = value.substring(1, value.length()-1).split(",");
						enumDataName.setText(attributeName);
						enumAttributeValues.getItems().clear();
						for(int i=0; i<data.length; i++) {
							enumAttributeValues.getItems().add(data[i].trim());
						}
					}
					else if(value.startsWith("integer")) {
						String brackets = value.substring(8);
						Matcher mBound = Pattern.compile("\\[(\\d+)..(\\d+)\\]").matcher(brackets);
						mBound.find();
						String lower = mBound.group(1);
						String upper = mBound.group(2);
						numChoice.getSelectionModel().select("integer");
						numFrom.setText(lower);
						numTo.setText(upper);
						numDataName.setText(attributeName);
					}
					else if(value.startsWith("float")) {
						String brackets = value.substring(8);
						Matcher mBound = Pattern.compile("\\[(.*)..(.*)\\]").matcher(brackets);
						mBound.find();
						String lower = mBound.group(1);
						String upper = mBound.group(2);
						numChoice.getSelectionModel().select("float");
						numFrom.setText(lower);
						numTo.setText(upper);
						numDataName.setText(attributeName);
					}
				}
			}
		});
		
		this.getSelectionModel().selectedItemProperty().addListener((ov,oldT,newT) -> {
			String tab = ov.getValue().getText();
			if(tab.equals("Constraints")) {
				activityBoxA.getItems().clear();
				activityBoxB.getItems().clear();
				ObservableList<String> activities = 
						FXCollections.observableList(
								allActivities.getRoot().getChildren().stream()
								.map(ti -> ti.getValue())
								.collect(Collectors.toList()));
				activityBoxA.setItems(activities);
				activityBoxB.setItems(activities);
			}
			else if(tab.equals("Model")) {
				drawGraph();
			}
		});
		
		/*createdConstraintsList.getItems().addListener((ListChangeListener.Change<? extends String> a) -> {
			//System.out.println("Cagirdilar beni");
			arrangeGeneratorChoices();
		});*/
		initializeTemplateList();
		createdConstraintsList.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if(newV == null) {
				templateDescription.setText("");
				addConstraintButton.setText("Add");
				return;
			}
			templatesList.getSelectionModel().clearSelection();
			addConstraintButton.setText("Update");
			if(!isBinaryTemplate(newV)) {
				correlationCondition.setVisible(false);
				AnchorPane.setTopAnchor(activationCondition, 260.0);
				AnchorPane.setTopAnchor(timeCondition, 330.0);			
			}
			else {
				correlationCondition.setVisible(true);
				AnchorPane.setTopAnchor(activationCondition, 260.0);
				AnchorPane.setTopAnchor(correlationCondition, 330.0);
				AnchorPane.setTopAnchor(timeCondition, 400.0);
				
			}
			insertSelectedConstraint(newV);
		});
		loadInitialValues();
	}
	
	private void insertSelectedConstraint(String constraint) {
		templateDescription.setText(getModelDescription(constraint));
		Matcher mBinary = Pattern.compile("(.*)\\[(.*), (.*)\\] \\|(.*) \\|(.*) \\|(.*)").matcher(constraint);
		Matcher mUnary = Pattern.compile(".*\\[(.*)\\] \\|(.*) \\|(.*)").matcher(constraint);
		if(mBinary.find()) {
			String template = mBinary.group(1);
			String a = mBinary.group(2);
			String t = mBinary.group(3);
			String ac = mBinary.group(4);
			String cc = mBinary.group(5);
			String tc = mBinary.group(6);
			if(template.contains("recedence")) {
				activityBoxB.getSelectionModel().select(a);
				actDataB.setText(cc);
				activityBoxA.getSelectionModel().select(t);
				actDataA.setText(ac);
				timeC.setText(tc);
			}
			else {
				activityBoxA.getSelectionModel().select(a);
				actDataA.setText(ac);
				activityBoxB.getSelectionModel().select(t);
				actDataB.setText(cc);
				timeC.setText(tc);
			}
		}
		else if(mUnary.find()) {
			activityBoxA.getSelectionModel().select(mUnary.group(1));
			actDataA.setText(mUnary.group(2));
			timeC.setText(mUnary.group(3));
		}
	}
	/*private void arrangeGeneratorChoices() {
		List<String> l = createdConstraintsList.getItems();
		if(l.stream().map(s -> containsData(s)).reduce(false, (a,b) -> a || b)) {
			genChoice.setItems(
					FXCollections.observableArrayList("AlloyLogGenerator"));
			
		}
		else {
			genChoice.setItems(
					FXCollections.observableArrayList("MinerFul Log Generator","AlloyLogGenerator"));
			
		}
	}*/
	
	private boolean containsData(String s) {
   	 String[] arr = s.split("\\|");
   	 arr[0] = "";
   	 for(String str:arr) {
   		 if(!str.trim().isEmpty()) return true;
   	 }
   	 return false;
   }
	
	private HashMap<Integer,List<String>> getConstraintParametersMap() {
		List<String> listOfConstraints = new ArrayList<String>();
		createdConstraintsList.getItems().forEach(c -> listOfConstraints.add(c));
		
		HashMap<Integer,List<String>> constraintParametersMap = new HashMap<Integer, List<String>>();
		int index = 0;
		for(String s:listOfConstraints) {
			int lbr = s.indexOf('[');
			int rbr = s.indexOf(']');
			String acts = s.substring(lbr+1,rbr);
			int comma = acts.indexOf(',');
			if(comma != -1) {
				String a = acts.substring(0,comma);
				String b = acts.substring(comma+2);
				List<String> l = new ArrayList<String>();
				l.add(a); l.add(b);
				constraintParametersMap.put(index, l);
				index++;
			}
			else {
				String a = acts;
				List<String> l = new ArrayList<String>();
				l.add(a);
				constraintParametersMap.put(index, l);
				index++;
			}
		}
		return constraintParametersMap;
		
	}
	
	private HashMap<Integer, ConstraintTemplate> getTemplatesMap() {
		List<String> listOfConstraints = new ArrayList<>();
		createdConstraintsList.getItems().forEach(c -> listOfConstraints.add(c));
		
		HashMap<Integer, ConstraintTemplate> templatesMap = new HashMap<>();
		int index = 0;
		for (String s : listOfConstraints) {
			//New code
			int lbr = s.indexOf('[');
			String templateStr = s.substring(0, lbr).replace(' ', '_');
			templatesMap.put(index, ConstraintTemplate.getByTemplateName(templateStr));
			index++;
			
			//Original code, replaced because it is over complicated and has irrelevant lines 
//			int lbr = s.indexOf('[');
//			int rbr = s.indexOf(']');
//			String t = s.substring(0, lbr).replace(' ', '_');
//			String acts = s.substring(lbr+1,rbr);
//			int comma = acts.indexOf(',');
//			if(comma != -1) {
//				String a = acts.substring(0,comma);
//				String b = acts.substring(comma+2);
//				templatesMap.put(index, t);
//				List<String> l = new ArrayList<String>();
//				l.add(a); l.add(b);
//				index++;
//			}
//			else {
//				String a = acts;
//				templatesMap.put(index, t);
//				List<String> l = new ArrayList<String>();
//				l.add(a);
//				index++;
//			}
		}
		
		return templatesMap;
	}
	
	private HashMap<Integer,String> getActivitiesMap() {
		HashMap<Integer,String> activitiesMap = new HashMap<Integer,String>();
		List<List<String>> tmp = new ArrayList<List<String>>();
		tmp.addAll(getConstraintParametersMap().values());
		List<String> list = new ArrayList<String>();
		tmp.forEach(l -> list.addAll(l));
		List<String> activityList = list.stream().distinct().collect(Collectors.toList());
		int index = 0;
		for(String s:activityList) {
			activitiesMap.put(index, s);
			index++;
		}
		return activitiesMap;
	}
	
	private void drawGraph() {
		List<String> listOfConstraints = new ArrayList<String>();
		createdConstraintsList.getItems().forEach(c -> listOfConstraints.add(c));
		insertMapsForGraph(getActivitiesMap(), getConstraintParametersMap(), getTemplatesMap(),listOfConstraints);
	}
	
	private void insertMapsForGraph(
			HashMap<Integer, String> actM,
			HashMap<Integer, List<String>> cspM,
			HashMap<Integer, ConstraintTemplate> tM,
			List<String> lofC) {
		Browser b = GraphGenerator.browserify(actM, cspM, tM, lofC, new Slider());
		modelPane.getChildren().clear();
		modelPane.getChildren().add(b);
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
	
	private String getModelDescription(String template) {
		int left = template.indexOf('[');
		String dt = template.substring(0, left).replace(' ', '_');
		if(dt.equals("Co-Existence")) {
			return "Activation: A and B\nTarget: A and B\n\nA and B occur together\n\nPositive examples: AB, ABCCDAAA, CDE\n\nNegative examples: CCA, BBCD, EDAAAC";
		}
		if(dt.equals("Not_Co-Existence")) {
			return "Activation: A and B\nTarget: A and B\n\nA and B never occur together\n\nPositive examples: AACC, BBCCBD, CDE\n\nNegative examples: AABCC, ABBBCD";
		}
		DeclareTemplate d = DeclareTemplate.valueOf(dt);
		switch(d) {
			case Absence:
				return "Activation: A\n\nA does not occur\n\nPositive examples: BC, BBCCDD\n\nNegative examples: A, BCDAA";
			case Absence2:
				return "Activation: A\n\nA occurs at most once\n\nPositive examples: BA, BBCCDA\n\nNegative examples: AA, BCDAAA";
			case Absence3:
				return "Activation: A\n\nA occurs at most twice\n\nPositive examples: BA, BBCCDAA\n\nNegative examples: AAA, ABCDAAA";
			case Exactly1:
				return "Activation: A\n\nA occurs exactly once\n\nPositive examples: BA, BBCCDA\n\nNegative examples: AAA, ABCDA, EDBC";
			case Exactly2:
				return "Activation: A\n\nA occurs exactly twice\n\nPositive examples: BAA, BBCCDAA\n\nNegative examples: AAA, ABCD, EDBC";
			case Existence:
				return "Activation: A\n\nA occurs at least once\n\nPositive examples: BAA, BBCCDA\n\nNegative examples: BCC, BCD, EDBC";
			case Existence2:
				return "Activation: A\n\nA occurs at least twice\n\nPositive examples: BAA, BBCCDAAA\n\nNegative examples: BCCA, BCD, EDBC";
			case Existence3:
				return "Activation: A\n\nA occurs at least three times\n\nPositive examples: BAAA, BABCCDAAA\n\nNegative examples: BCCA, BAACD, EDBC";
			case Init:
				return "Activation: A\n\nA occurs first\n\nPositive examples: AA, ABCCDAAA\n\nNegative examples: BCCA, BAACD, EDBC";
			case Responded_Existence:
				return "Activation: A\nTarget: B\n\nIf A occurs then B occurs as well\n\nPositive examples: AB, ABCCDAAA\n\nNegative examples: CCA, AACD, EDAAAC";
			case Response:
				return "Activation: A\nTarget: B\n\nIf A occurs then B occurs after A\n\nPositive examples: ABCD ,AAAAB, BCCD\n\nNegative examples: CCA, AACD, EDAAAC";
			case Alternate_Response:
				return "Activation: A\nTarget: B\n\nEach time A occurs, then B occurs afterwards before A recurs\n\nPositive examples: ABCADB ,ACDEB\n\nNegative examples: CCAA, AABCD, EDBAAAC";
			case Chain_Response:
				return "Activation: A\nTarget: B\n\nEach time A occurs, then B occurs immediately afterwards\n\nPositive examples: ABCAB ,CDEAB\n\nNegative examples: CCAACB, EDBAAAC";
			case Precedence:
				return "Activation: B\nTarget: A\n\nB occurs if preceded by A\n\nPositive examples: ABCD ,AAAAB, AACCD\n\nNegative examples: CCBA, BBCD, EDBAC";
			case Alternate_Precedence:
				return "Activation: B\nTarget: A\n\nEach time B occurs, it is preceded by A and no other B can recur in between\n\nPositive examples: ABCD ,ABACAAB, AACCD\n\nNegative examples: CACBBA, ABBABCB";
			case Chain_Precedence:
				return "Activation: B\nTarget: A\n\nEach time B occurs, then A occurs immediately beforehand\n\nPositive examples: ABCABAA ,CDEABAB\n\nNegative examples: CCAACBB, EDBAAAC";
			case Succession:
				return "Activation: A and B\nTarget: A and B\n\nA occurs if and only if it is followed by B\n\nPositive examples: AB, ABCCDBB\n\nNegative examples: BCCA, BBCD, EDBAC";
			case Alternate_Succession:
				return "Activation: A and B\nTarget: A and B\n\nA and B occur together if and only if the latter follows the former, and they alternate each other\n\nPositive examples: ACDBACB, ABCCABD\n\nNegative examples: AABCCA, BBCDAA, EDBAC";
			case Chain_Succession:
				return "Activation: A and B\nTarget: A and B\n\nA and B occur together if and only if the latter immediately follows the former\n\nPositive examples: ABABCC, CCD\n\nNegative examples: AABCCA, BBCDAA, EDBAC";
			case Not_Chain_Succession:
				return "Activation: A and B\nTarget: A and B\n\nA and B occur together if and only if the latter does not immediately follow the former\n\nPositive examples: ABBAABCC, BBCCD\n\nNegative examples: ABCC, ABABCD";
			case Not_Succession:
				return "Activation: A and B\nTarget: A and B\n\nA can never occur before B\n\nPositive examples: BBAACC, BBCCBAD\n\nNegative examples: AABBCC, ABBCD";
			case Not_Chain_Precedence:
				return "Activation: B\nTarget: A\n\nEach time B occurs, then A does not occur immediately beforehand\n\nPositive examples: BABBCD, ACDE \n\nNegative examples: ABCABAA ,CDEABAB";
			case Not_Chain_Response:
				return "Activation: A\nTarget: B\n\nEach time A occurs, then B does not occur immediately afterwards\n\nPositive examples: AABCAA, BCDE\n\nNegative examples: ABCAB ,CDEAB";
			case Not_Precedence:
				return "Activation: B\nTarget: A\n\nB occurs if it is not preceded by A\n\nPositive examples: CCBA, BBCD, EDBAC\n\nNegative examples: ABCD, AAAABDE";
			case Not_Response:
				return "Activation: A\nTarget: B\n\nIf A occurs then B does not occur after A\n\nPositive examples: CCA, AACD, EDAAAC\n\nNegative examples: AABCD, ABBED";
			case Not_Responded_Existence:
				return "Activation: A\nTarget: B\n\nIf A occurs then B does not occur\n\nPositive examples: CCA, AACD, EDAAAC, BCDE\n\nNegative examples: ADCDB, ABAEB";
			default:
				return "";
		}
	}
	
	private void initializeTemplateList() {
		List<String> templates = new ArrayList<String>();
		for(DeclareTemplate dt : DeclareTemplate.values()) {
			if(dt != DeclareTemplate.Choice && 
					dt != DeclareTemplate.Exclusive_Choice) {
				templates.add(getModelName(dt.name()).replace('_', ' '));
				
			}
		}
		//templates.add("End[A]");
		ObservableList<String> items =FXCollections.observableArrayList (
			    (String[]) templates.toArray(new String[templates.size()]));
		
		templatesList.setItems(items);
		
		templatesList.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if(newV == null) {
				templateDescription.setText("");
				return;
			}
			createdConstraintsList.getSelectionModel().clearSelection();
			addConstraintButton.setText("Add");
			templateDescription.setText(getModelDescription(newV));
			if(!isBinaryTemplate(newV)) {
				correlationCondition.setVisible(false);
				AnchorPane.setTopAnchor(activationCondition, 260.0);
				AnchorPane.setTopAnchor(timeCondition, 330.0);			
			}
			else {
				correlationCondition.setVisible(true);
				AnchorPane.setTopAnchor(activationCondition, 260.0);
				AnchorPane.setTopAnchor(correlationCondition, 330.0);
				AnchorPane.setTopAnchor(timeCondition, 400.0);
				
			}
		});
	}
	
	private boolean isBinaryTemplate(String template) {
		if(template == null) return false;
		int left = template.indexOf('[');
		String dt = template.substring(0, left).replace(' ', '_');
		if(dt.contains("Co-Existence")) return true;
		if(dt.equals("End")) return false;
		DeclareTemplate d = DeclareTemplate.valueOf(dt);
		switch(d) {
			case Absence:
				return false;
			case Absence2:
				return false;
			case Absence3:
				return false;
			case Exactly1:
				return false;
			case Exactly2:
				return false;
			case Existence:
				return false;
			case Existence2:
				return false;
			case Existence3:
				return false;
			case Init:
				return false;
			default:
				return true;
		}
	}
	
	@FXML
	public void showHelp() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About Entering Data for Activities");
		alert.setHeaderText("Tutorial");
		alert.setContentText( "You can refer to attributes of activity A and T using '.':" +
                "  'A.TransportType'\n" +
                "  'T.PhoneNumber'\n" +
                "Operations on attribute values for enum\n" +
                "  'A.TransportType is Car'\n" +
                "  'A.TransportType is not Car'\n" +
                "  'A.TransportType in (Car, Train)'\n" +
                "  'A.TransportType not in (Car, Train)'\n" +
                "NOTE: Please pay attention to spaces, operands must be separated with only one space, not more or less!"+
                "Operations on attribute values for numeric attribute\n" +
                "  'A.Price > 10'\n" +
                "  'A.Price <= 5'\n" +
                "  'A.Angle = 12.4'\n" +
                "Operations can be joined with 'and' and 'or':\n" +
                "  'T.Price <= 10 or T.Price>100'\n" +
                "  'A.Angle = 12.4 and A.Angle < 13'\n" +
                "If both A and T have the same data attribute, 'same' and 'different' constraints can be used:\n" +
                "  'same Price'\n" +
                "  'different Group'\n");

		alert.showAndWait();
	}
	
	@FXML
	public void showHelpForTime() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About Entering Time Condition");
		alert.setHeaderText("Tutorial");
		StringBuilder sb = new StringBuilder();
		sb.append("You can add time condition for the constraints as follows:\n\n");
		sb.append("Example: 2,5,h -> For a unary constraint, the activation is at a time distance of at least 2 hours and at most 5 hours from the beginning of the trace. For a binary constraint, the target is at a time distance of at least 2 hours and at most 5 hours from the activation.\n\n");
		sb.append("Format: after_activation_unit_min,after_activation_unit_max,time_unit where after_activation_unit_min and after_activation_unit_max are non-negative integers (after_activation_unit_max >= after_activation_unit_min), and time_unit is one of four units below\n\n");
		sb.append("s: second, m: minute, h: hour, d: day");
		alert.setContentText(sb.toString());
		alert.showAndWait();
	}
	
	@FXML
	public void addActivity() {
		String activity = activityName.getText();
		List<String> activityNames = allActivities.getRoot().getChildren().
				stream().map(ti -> ti.getValue()).collect(Collectors.toList());
		if(activity != null && !activity.trim().isEmpty() && !activityNames.contains(activity)) {
			TreeItem<String> item = new TreeItem<String>(activity);
			item.setExpanded(true);
			allActivities.getRoot().getChildren().add(item);
			activityName.setText("");
			//addActivityButton.setTooltip(null);
		}
		else if(activity == null || activity.trim().isEmpty()) {
			//Tooltip t = new Tooltip("Activity name must not be empty!");
			//Tooltip.install(addActivityButton, t);
			AlertUtils.showError("Activity name must not be empty!");
			//addActivityButton.setTooltip();
		}
		else if(activityNames.contains(activity)) {
			//addActivityButton.setTooltip(new Tooltip("This activity name exists!"));
			AlertUtils.showError("This activity name exists!");
		}
		/*else if(!activity.matches("^\\S+$")) {
			//addActivityButton.setTooltip(new Tooltip("This activity name exists!"));
			showAlert("This activity name is not valid (must be nonempty and without whitespace characters)!");
		}*/
	}
	
	@FXML
	public void addConstraint() {
		String template = templatesList.getSelectionModel().getSelectedItem();
		String edited = createdConstraintsList.getSelectionModel().getSelectedItem();
		if(template == null && edited == null) {
			AlertUtils.showError("A template must be selected!");
			//addConstraintButton.setTooltip(new Tooltip("A template must be selected!"));
		}
		else {
			if(isBinaryTemplate(template) || isBinaryTemplate(edited)) {
				String a = activityBoxA.getSelectionModel().getSelectedItem();
				String ad = actDataA.getText();
				String b = activityBoxB.getSelectionModel().getSelectedItem();
				String bd = actDataB.getText();
				String tc = timeC.getText();
				if(a == null) {
					AlertUtils.showError("Activation is not selected!");
					//addConstraintButton.setTooltip(new Tooltip("Activity A is not selected!"));
				}
				else {
					if(b == null) {
						AlertUtils.showError("Target is not selected!");
						//addConstraintButton.setTooltip(new Tooltip("Activity B is not selected!"));
					}
					else {
						boolean adValid = ad.isEmpty() || isValidActivityData(ad);
						boolean bdValid = bd.isEmpty() || isValidActivityData(bd);
						boolean tcValid = tc.isEmpty() || tc.matches("^\\d+,\\d+,[s,m,h,d]$");
						if(!adValid) AlertUtils.showError("Data for Activity A is invalid");
						else if(!bdValid) AlertUtils.showError("Data for Activity B is invalid");
						else if(!tcValid) AlertUtils.showError("Time condition is not valid");
						else {
							if(template != null) {
								int br = template.indexOf('[');
								String tmplt = template.substring(0, br);
								a = a.trim();
								b = b.trim();
								ad = ad.trim();
								bd = bd.trim();
								tc = tc.trim();
								if(tmplt.contains("Precedence")) {
									String created = tmplt + "[" + b + ", " + a + "] "+"|"+ad+" |"+bd+" |"+tc;
									createdConstraintsList.getItems().add(created);
								}
								else {
									String created = tmplt + "[" + a + ", " + b + "] "+"|"+ad+" |"+bd+" |"+tc;
									createdConstraintsList.getItems().add(created);
								}
								templatesList.getSelectionModel().clearSelection();
							}
							else if(edited != null) {
								int pos = createdConstraintsList.getItems().indexOf(edited);
								int br = edited.indexOf('[');
								String tmplt = edited.substring(0, br);
								a = a.trim();
								b = b.trim();
								ad = ad.trim();
								bd = bd.trim();
								tc = tc.trim();
								if(tmplt.contains("Precedence")) {
									String created = tmplt + "[" + b + ", " + a + "] "+"|"+ad+" |"+bd+" |"+tc;
									createdConstraintsList.getItems().remove(pos);
									createdConstraintsList.getItems().add(pos,created);
									createdConstraintsList.getSelectionModel().select(pos);
									createdConstraintsList.getSelectionModel().clearSelection();
								}
								else {
									String created = tmplt + "[" + a + ", " + b + "] "+"|"+ad+" |"+bd+" |"+tc;
									createdConstraintsList.getItems().remove(pos);
									createdConstraintsList.getItems().add(pos,created);
									createdConstraintsList.getSelectionModel().select(pos);
									createdConstraintsList.getSelectionModel().clearSelection();
								}
							}
							//addConstraintButton.setTooltip(null);
						}
						
					}
				}
			}
			else {
				String a = activityBoxA.getSelectionModel().getSelectedItem();
				String ad = actDataA.getText();
				String tc = timeC.getText();
				if(a == null) {
					AlertUtils.showError("Activity A is not selected!");
				}
				else {
					boolean adValid = ad.isEmpty() || ((!ad.isEmpty()) && isValidActivityData(ad));
					boolean tcValid = tc.isEmpty() || tc.matches("^\\d+,\\d+,[s,m,h,d]$");
					if(!adValid) AlertUtils.showError("Data for Activity A is invalid");
					else if(!tcValid) AlertUtils.showError("Time condition is not valid");
					else {
						if(template != null) {
							int br = template.indexOf('[');
							a = a.trim();
							ad = ad.trim();
							tc = tc.trim();
							String created = template.substring(0, br) + "[" + a + "] "+"|"+ad+" |"+tc;
							createdConstraintsList.getItems().add(created);
							templatesList.getSelectionModel().clearSelection();
						}
						else if(edited != null) {
							createdConstraintsList.getSelectionModel().clearSelection();
							int pos = createdConstraintsList.getItems().indexOf(edited);
							int br = edited.indexOf('[');
							a = a.trim();
							ad = ad.trim();
							tc = tc.trim();
							String created = edited.substring(0, br) + "[" + a + "] "+"|"+ad+" |"+tc;
							createdConstraintsList.getItems().remove(pos);
							createdConstraintsList.getItems().add(pos,created);
							createdConstraintsList.getSelectionModel().select(pos);
							createdConstraintsList.getSelectionModel().clearSelection();
						}
						//addConstraintButton.setTooltip(null);
					}
						
				}
				
			}
		}
	}
	
	private boolean isValidActivityData(String s) {
		return AlloyRunner.isValidDataExpression(s); // Changed from MPDeclareLogGenerator to AlloyLogGenerator library
		/*String cmd = "java -jar AlloyLogGenerator2.jar 0 0 0 0 0 -validatefn "+"\""+s+"\"";
		ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
		try {
			Process p = pb.start();
			Scanner sc = new Scanner(p.getInputStream());
			String res = sc.nextLine();
			sc.close();
			
			Pattern pattern = Pattern.compile("\\{\"errorCode\":(\\d)");
    		Matcher m = pattern.matcher(res);
    		return m.find() && m.group(1).equals("0");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}*/
	}
	
	@FXML
	public void removeConstraint() {
		int pos = createdConstraintsList.getSelectionModel().getSelectedIndex();
		createdConstraintsList.getItems().remove(pos);
	}
	
	@FXML
	public void addAttributeValue() {
		String value = enumAttributeValue.getText();
		if(value == null || value.trim().isEmpty()) {
			AlertUtils.showError("Attribute value must not be empty!");
		}
		else {
			List<String> attributeValues = enumAttributeValues.getItems();
			if(attributeValues.contains(value)) {
				AlertUtils.showError("This value already exists!");
			}
			else {
				attributeValues.add(value);
				enumAttributeValue.setText("");
			}
		}
	}
	
	@FXML
	public void removeAttributeValue() {
		int index = enumAttributeValues.getSelectionModel().getSelectedIndex();
		enumAttributeValues.getItems().remove(index);
	}
	
	private void _addEnumData(TreeItem<String> selected) {
		String attributeName = enumDataName.getText();
		if(attributeName == null || attributeName.trim().isEmpty() || !attributeName.matches("^\\S+$")) {
			AlertUtils.showError("Attribute name for Enumerative Data must valid (nonempty and without whitespace characters)!");
		}
		else {
			List<String> attributeValues = enumAttributeValues.getItems();
			if(attributeValues == null || attributeValues.isEmpty()) {
				AlertUtils.showError("Attribute list must contain at least one element!");
			}
			else {
				String enumData = attributeName + " : " + stringify(attributeValues);
				//enumAttributeValues.getItems().clear();
				if(selected == null) {
					AlertUtils.showError("Select an activity to add the data");
				}
				else if(selected.getParent().equals(allActivities.getRoot())){
					String actName = selected.getValue();
					Set<String> set = dataNames.get(actName);
					if(set == null) {
						String definition = dataDefinitions.get(attributeName);
						if(definition == null) {
							dataDefinitions.put(attributeName, enumData);
							selected.getChildren().add(new TreeItem<String>(enumData));
							Set<String> newSet = new HashSet<String>();
							newSet.add(attributeName);
							dataNames.put(actName,newSet);
							//enumDataName.setText("");
							//addDataEnum.setTooltip(null);
						}
						else if(definition.equals(enumData)) {
							selected.getChildren().add(new TreeItem<String>(enumData));
							Set<String> newSet = new HashSet<String>();
							newSet.add(attributeName);
							dataNames.put(actName,newSet);
							//enumDataName.setText("");
							//addDataEnum.setTooltip(null);
						}
						else {
							dataDefinitions.put(attributeName, enumData);
							for(TreeItem<String> ti: allActivities.getRoot().getChildren()) {
								Set<String> data = dataNames.get(ti.getValue());
								if(data != null && data.contains(attributeName)) {
									for(TreeItem<String> di: ti.getChildren()) {
										if(di.getValue().startsWith(attributeName)) {
											di.setValue(dataDefinitions.get(attributeName));
										}
									}
								}
							}
							//showAlert("This data already exists with different definition in "+selected.getValue());
						}
					}
					else if(!set.contains(attributeName)) {
						String definition = dataDefinitions.get(attributeName);
						if(definition == null) {
							dataDefinitions.put(attributeName, definition);
							selected.getChildren().add(new TreeItem<String>(enumData));
							set.add(attributeName);
							dataNames.put(actName,set);
							//enumDataName.setText("");
							//addDataEnum.setTooltip(null);
						}
						else if(definition.equals(enumData)) {
							selected.getChildren().add(new TreeItem<String>(enumData));
							set.add(attributeName);
							dataNames.put(actName,set);
							//enumDataName.setText("");
							//addDataEnum.setTooltip(null);
						}
						else {
							dataDefinitions.put(attributeName, enumData);
							for(TreeItem<String> ti: allActivities.getRoot().getChildren()) {
								Set<String> data = dataNames.get(ti.getValue());
								if(data != null && data.contains(attributeName)) {
									for(TreeItem<String> di: ti.getChildren()) {
										if(di.getValue().startsWith(attributeName)) {
											di.setValue(dataDefinitions.get(attributeName));
										}
									}
								}
							}
							//showAlert("This data already exists with different definition in"+selected.getValue());
						}
					}
					else {
						dataDefinitions.put(attributeName, enumData);
						for(TreeItem<String> ti: allActivities.getRoot().getChildren()) {
							Set<String> data = dataNames.get(ti.getValue());
							if(data != null && data.contains(attributeName)) {
								for(TreeItem<String> di: ti.getChildren()) {
									if(di.getValue().startsWith(attributeName)) {
										di.setValue(dataDefinitions.get(attributeName));
									}
								}
							}
						}
					}
				}
				else {
					AlertUtils.showError("Activity not selected!");
				}
				
			}
		}
	}
	
	@FXML
	public void addEnumData() {
		_addEnumData(allActivities.getSelectionModel().getSelectedItem());
	}
	
	private boolean isInAnotherActivity(String data) {
		for(String s: dataNames.keySet()) {
			if(dataNames.get(s).contains(data)) {
				return true;
			}
		}
		return false;
	}
	
	@FXML
	public void _removeSelected() {
		//System.out.println("remove!!!");
		TreeItem<String> selected = allActivities.getSelectionModel().getSelectedItem();
		if(selected == null) {
			AlertUtils.showError("An element must be selected!");
			return;
		}
		else {
			String str = selected.getValue();
			TreeItem<String> selectedParent = selected.getParent();
			selectedParent.getChildren().removeIf(ti -> ti.getValue().equals(str));
			if(selectedParent.equals(allActivities.getRoot())) {
				Set<String> data = dataNames.get(str);
				dataNames.remove(str);
				for(String d: data) {
					if(!isInAnotherActivity(d)) {
						dataDefinitions.remove(d);
					}
				}
			}
			else {
				Matcher mKey = Pattern.compile("(.*) :.*").matcher(str);
				mKey.find();
				dataNames.get(selectedParent.getValue()).remove(mKey.group(1));
				if(!isInAnotherActivity(str)) {
					dataDefinitions.remove(str);
				}
			}
		}
		
	}
	
	private void _addNumData(TreeItem<String> selected) {
		String attributeName = numDataName.getText();
		if(attributeName == null || attributeName.trim().isEmpty() || !attributeName.matches("^\\S+$")) {
			AlertUtils.showError("Attribute name for Numeric Data must be valid (nonempty and without whitespace characters)");
		}
		else {
			String chosenType = numChoice.getSelectionModel().getSelectedItem();
			if(chosenType == null) {
				AlertUtils.showError("Type must be chosen");
			}
			else {
				String numData = "";
				String fromS = numFrom.getText();
				String toS = numTo.getText();
				if(fromS == null || fromS.trim().isEmpty()) {
					AlertUtils.showError("From value must not be empty");
				}
				else {
					if(toS == null || toS.trim().isEmpty()) {
						AlertUtils.showError("To value must not be empty");
					}
					else {
						if(chosenType.equals("integer")) {
							try {
								int from = Integer.valueOf(numFrom.getText());
								int to = Integer.valueOf(numTo.getText());
								if(to < from) {
									AlertUtils.showError("From value must not be greater than to value");
								}
								else {
									numData = attributeName + " : " + "integer [" + from + ".." + to + "]";
									//TreeItem<String> selected = allActivities.getSelectionModel().getSelectedItem();
									if(selected == null) {
										AlertUtils.showError("Select an activity!");
									}
									else if(selected.getParent().equals(allActivities.getRoot())) {
										Set<String> set = dataNames.get(selected.getValue());
										if(set == null) {
											String definition = dataDefinitions.get(attributeName);
											if(definition == null) {
												dataDefinitions.put(attributeName, numData);
												selected.getChildren().add(new TreeItem<String>(numData));
												Set<String> newSet = new HashSet<String>();
												newSet.add(attributeName);
												dataNames.put(selected.getValue(), newSet);
											}
											else if(definition.equals(numData)) {
												selected.getChildren().add(new TreeItem<String>(numData));
												Set<String> newSet = new HashSet<String>();
												newSet.add(attributeName);
												dataNames.put(selected.getValue(), newSet);
											}
											else {
												dataDefinitions.put(attributeName, numData);
												for(TreeItem<String> ti: allActivities.getRoot().getChildren()) {
													Set<String> data = dataNames.get(ti.getValue());
													if(data != null && data.contains(attributeName)) {
														for(TreeItem<String> di: ti.getChildren()) {
															if(di.getValue().startsWith(attributeName)) {
																di.setValue(dataDefinitions.get(attributeName));
															}
														}
													}
												}
												//showAlert("This data already exists with different definition in "+selected.getValue());
											}
										}
										else if(set.add(attributeName)) {
											String definition = dataDefinitions.get(attributeName);
											if(definition == null) {
												dataDefinitions.put(attributeName, numData);
												selected.getChildren().add(new TreeItem<String>(numData));
												dataNames.put(selected.getValue(), set);
											}
											else if(definition.equals(numData)) {
												selected.getChildren().add(new TreeItem<String>(numData));
												dataNames.put(selected.getValue(), set);
											}
											else {
												dataDefinitions.put(attributeName, numData);
												for(TreeItem<String> ti: allActivities.getRoot().getChildren()) {
													Set<String> data = dataNames.get(ti.getValue());
													if(data != null && data.contains(attributeName)) {
														for(TreeItem<String> di: ti.getChildren()) {
															if(di.getValue().startsWith(attributeName)) {
																di.setValue(dataDefinitions.get(attributeName));
															}
														}
													}
												}
												//showAlert("This data already exists with different definition in "+selected.getValue());
											}
										}
										else {
											dataDefinitions.put(attributeName, numData);
											for(TreeItem<String> ti: allActivities.getRoot().getChildren()) {
												Set<String> data = dataNames.get(ti.getValue());
												if(data != null && data.contains(attributeName)) {
													for(TreeItem<String> di: ti.getChildren()) {
														if(di.getValue().startsWith(attributeName)) {
															di.setValue(dataDefinitions.get(attributeName));
														}
													}
												}
											}
											//showAlert("This attribute name already exists in "+selected.getValue());
										}
									}
									else {
										AlertUtils.showError("Activity not selected!");
									}
								}
							}catch(NumberFormatException e) {
								AlertUtils.showError("Invalid integer values");
							}
						}
						else {
							try {
								float from = Float.valueOf(numFrom.getText());
								float to = Float.valueOf(numTo.getText());
								if(to < from) {
									AlertUtils.showError("From value must not be greater than to value");
								}
								else {
									numData = attributeName + " : " + "float [" + from + ".." + to + "]";
									//TreeItem<String> selected = allActivities.getSelectionModel().getSelectedItem();
									if(selected == null) {
										AlertUtils.showError("Select an activity!");
									}
									else {
										Set<String> set = dataNames.get(selected.getValue());
										if(set == null) {
											selected.getChildren().add(new TreeItem<String>(numData));
											Set<String> newSet = new HashSet<String>();
											newSet.add(attributeName);
											dataNames.put(selected.getValue(), newSet);
										}
										else if(set.add(attributeName)) {
											selected.getChildren().add(new TreeItem<String>(numData));
											dataNames.put(selected.getValue(), set);
										}
										else {
											dataDefinitions.put(attributeName, numData);
											for(TreeItem<String> ti: allActivities.getRoot().getChildren()) {
												Set<String> data = dataNames.get(ti.getValue());
												if(data != null && data.contains(attributeName)) {
													for(TreeItem<String> di: ti.getChildren()) {
														if(di.getValue().startsWith(attributeName)) {
															di.setValue(dataDefinitions.get(attributeName));
														}
													}
												}
											}
											//showAlert("This attribute name already exists for this activity!");
										}
									}
								}
							}catch(NumberFormatException e) {
								AlertUtils.showError("Invalid float values");
							}
						}
					}
				}
			}
		}
	}
	
	@FXML
	public void addNumData() {
		_addNumData(allActivities.getSelectionModel().getSelectedItem());
	}
	
	@FXML
	public void addNumDataToSelecteds() {
		List<TreeItem<String>> list = allActivities.getSelectionModel().getSelectedItems();
		if(list.isEmpty()) {
			AlertUtils.showError("An activity must be selected!");
			return;
		}
		for(TreeItem<String> ti: list) {
			if(!ti.getParent().equals(allActivities.getRoot())) {
				AlertUtils.showError(ti.getValue()+" is not an activity, all selections must be an activity!");
				allActivities.getSelectionModel().clearSelection();
				return;
			}
			else {
				_addNumData(ti);
			}
		}
		allActivities.getSelectionModel().clearSelection();
	}
	
	@FXML
	public void addNumDataToAll() {
		allActivities.getSelectionModel().clearSelection();
		for(TreeItem<String> ti: allActivities.getRoot().getChildren()) {
				_addNumData(ti);
		}
	}
	
	@FXML
	public void addEnumDataToAll() {
		allActivities.getSelectionModel().clearSelection();
		for(TreeItem<String> ti: allActivities.getRoot().getChildren()) {
				_addEnumData(ti);
		}
	}
	
	@FXML
	public void addEnumDataToSelecteds() {
		List<TreeItem<String>> list = allActivities.getSelectionModel().getSelectedItems();
		if(list.isEmpty()) {
			AlertUtils.showError("An activity must be selected!");
			return;
		}
		for(TreeItem<String> ti: list) {
			if(!ti.getParent().equals(allActivities.getRoot())) {
				AlertUtils.showError(ti.getValue()+" is not an activity, all selections must be an activity!");
				allActivities.getSelectionModel().clearSelection();
				return;
			}
			else {
				_addEnumData(ti);
			}
		}
		allActivities.getSelectionModel().clearSelection();
	}
	
	private String stringify(List<String> l) {
		String s = "{";
		for(int i=0; i<l.size(); i++) {
			s += l.get(i);
			if(i+1 != l.size()) {
				s += ", ";
			}
		}
		s += "}";
		return s;
	}
	
	@FXML
	private void startReccording() {
		System.out.println("Start recording");
		try {
			InfiniteSpeechRecognizer.infiniteStreamingRecognize();
			System.out.println(InfiniteSpeechRecognizer.getLastRawString());

			final DeclareConstructor constructor = new DeclareConstructor();
			final DeclareModel generatedModel = constructor.convertToDeclareModel(InfiniteSpeechRecognizer.getLastRawString());
			//any number from 0-... (mostly 1-2)
			if (generatedModel != null) {
				generatedModel.getConstraints().forEach(e-> System.out.println(e.toRuMString()));
			}
		} catch (Exception e) {
			System.out.println("Error during voice recording: " + e.getMessage());
		}
	}

}
