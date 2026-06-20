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
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XLog;

import core.AlloyRunner;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;
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
import util.AlertUtils;

public class LogGenController extends GridPane {
	
	@FXML
	private TextField modelName;
	
	@FXML
	private TextField minTraceLength;
	
	@FXML
	private TextField maxTraceLength;
	
	@FXML
	private TextField amountOfTraces;
	
	@FXML
	private AnchorPane alloyConfig;
	
	@FXML
	private RadioButton vacTrue;
	
	@FXML
	private RadioButton vacFalse;
	
	@FXML
	private RadioButton negTrue;
	
	@FXML
	private RadioButton negFalse;
	
	@FXML
	private RadioButton evenTrue;
	
	@FXML
	private RadioButton evenFalse;
	
	private boolean group;
	
	private List<String> constraintList;
	
	@FXML
	private ListView<String> constraintListView;
	
	private String mpFile;
	
	private Stage stage;
	
	public LogGenController(boolean group, Stage stage) {
		this.group = group;
		this.stage = stage;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/LogGenConfigView.fxml"));
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
		alloyConfig.setVisible(group);
		vacTrue.selectedProperty().addListener((ov,oldV,newV) -> {
			if(newV) vacFalse.setSelected(false);
		});
		vacFalse.selectedProperty().addListener((ov,oldV,newV) -> {
			if(newV) vacTrue.setSelected(false);
		});
		negTrue.selectedProperty().addListener((ov,oldV,newV) -> {
			if(newV) negFalse.setSelected(false);
		});
		negFalse.selectedProperty().addListener((ov,oldV,newV) -> {
			if(newV) negTrue.setSelected(false);
		});
		evenTrue.selectedProperty().addListener((ov,oldV,newV) -> {
			if(newV) evenFalse.setSelected(false);
		});
		evenFalse.selectedProperty().addListener((ov,oldV,newV) -> {
			if(newV) evenTrue.setSelected(false);
		});
	}
	
	@FXML
	public void upMinTrace() {
		String text = minTraceLength.getText();
		try {
			int before = Integer.valueOf(text);
			before = before + 1;
			minTraceLength.setText(before+"");
		}catch(NumberFormatException nfe) {
			if(text.isEmpty()) minTraceLength.setText("1");
			
			else AlertUtils.showError("Min trace length is not an integer!");
		}
	}
	
	@FXML
	public void downMinTrace() {
		String text = minTraceLength.getText();
		try {
			int before = Integer.valueOf(text);
			before = before > 0 ? (before - 1) : before;
			minTraceLength.setText(before+"");
		}catch(NumberFormatException e) {
			if(text.isEmpty()) minTraceLength.setText("0");
			
			else AlertUtils.showError("Min trace length is not an integer!");
		}
	}
	
	@FXML
	public void upMaxTrace() {
		String text = maxTraceLength.getText();
		try {
			int before = Integer.valueOf(text);
			before = before + 1;
			maxTraceLength.setText(before+"");
		}catch(NumberFormatException e) {
			if(text.isEmpty()) maxTraceLength.setText("1");
			
			else AlertUtils.showError("Max trace length is not an integer!");
		}
	}
	
	@FXML
	public void downMaxTrace() {
		String text = maxTraceLength.getText();
		try {
			int before = Integer.valueOf(text);
			before = before > 0 ? (before - 1) : before;
			maxTraceLength.setText(before+"");
		}catch(NumberFormatException e) {
			if(text.isEmpty()) maxTraceLength.setText("0");
			
			else AlertUtils.showError("Max trace length is not an integer");
		}
	}
	
	@FXML
	public void upTraceAmount() {
		String text = amountOfTraces.getText();
		try {
			int before = Integer.valueOf(text);
			before = before + 1;
			amountOfTraces.setText(before+"");
		}catch(NumberFormatException e) {
			if(text.isEmpty()) amountOfTraces.setText("1");
			else AlertUtils.showError("Amount of trace is not an integer");
		}
	}
	
	@FXML
	public void downTraceAmount() {
		String text = amountOfTraces.getText();
		try {
			int before = Integer.valueOf(text);
			before = before > 0 ? (before - 1) : before;
			amountOfTraces.setText(before+"");
		}catch(NumberFormatException e) {
			if(text.isEmpty()) amountOfTraces.setText("0");
			else AlertUtils.showError("Amount of trace is not an integer");
		}
	}
	
	private String isVacuityEnabled() {
		if(vacTrue.isSelected()) {
			return " -vacuity ";
		}
		else return "";
	}
	
	private String isNegativeTraces() {
		if(negTrue.isSelected()) {
			return " -negative ";
		}
		else return "";
	}
	
	private String isEvenLength() {
		if(evenTrue.isSelected()) {
			return " -eld";
		}
		else return "";
	}
	
	public void setMpFile(String mpFile) {
		this.mpFile = mpFile;
	}
	
	public void setConstraintList(List<String> constraintList) {
		this.constraintList = constraintList;
		this.constraintListView.getItems().clear();
		this.constraintListView.getItems().addAll(this.constraintList);
	}
	
	@FXML
	public void selectFileToSave() {
		FileChooser fileChooser = new FileChooser();
		ExtensionFilter filter = new ExtensionFilter("Log file",Arrays.asList("*.xes"));
		fileChooser.getExtensionFilters().add(filter);
		File file = fileChooser.showSaveDialog(stage);
		if(file != null) {
			modelName.setText(file.getAbsolutePath());
		}
	}
	
	public List<String> getFields() {
		return Arrays.asList(modelName.getText(),
				minTraceLength.getText(),
				maxTraceLength.getText(),
				amountOfTraces.getText());
	}
	
	public void setFields(List<String> fields) {
		modelName.setText(fields.get(0));
		minTraceLength.setText(fields.get(1));
		maxTraceLength.setText(fields.get(2));
		amountOfTraces.setText(fields.get(3));
	}
	
	private HashMap<Integer,List<String>> getConstraintParametersMap() {
		List<String> listOfConstraints = new ArrayList<String>();
		constraintList.forEach(c -> listOfConstraints.add(c));
		
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
	
	private HashMap<Integer,String> getTemplatesMap() {
		List<String> listOfConstraints = new ArrayList<String>();
		constraintList.forEach(c -> listOfConstraints.add(c));
		
		HashMap<Integer,String> templatesMap = new HashMap<Integer,String>();
		int index = 0;
		for(String s:listOfConstraints) {
			int lbr = s.indexOf('[');
			int rbr = s.indexOf(']');
			String t = s.substring(0, lbr).replace(' ', '_');
			String acts = s.substring(lbr+1,rbr);
			int comma = acts.indexOf(',');
			if(comma != -1) {
				String a = acts.substring(0,comma);
				String b = acts.substring(comma+2);
				templatesMap.put(index, t);
				List<String> l = new ArrayList<String>();
				l.add(a); l.add(b);
				index++;
			}
			else {
				String a = acts;
				templatesMap.put(index, t);
				List<String> l = new ArrayList<String>();
				l.add(a);
				index++;
			}
		}
		
		return templatesMap;
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
			if(template.equals("Co-Existence")) {
				return new CoExistence(involved.get(0),involved.get(1));
			}
			if(template.equals("Responded_Existence")) {
				return new RespondedExistence(involved.get(0),involved.get(1));
			}
			if(template.equals("Not_Co-Existence")) {
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
	
	//Setup for Minerful log generation, call to minerful library and storing the result
	public String generateWithMinerFul() {
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
		logMakParameters.outputLogFile = new File(modelName.getText());
		try {
			logMak.storeLog();
			/*Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Log Generation");
			alert.setHeaderText("Successful");
			alert.setContentText("Log is successfully generated with MinerFul");
			alert.showAndWait();*/
			return modelName.getText();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "Error with Minerful";
		}
	}
	
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
	
	private String activitySpaceToUnderscore(String line) {
		Matcher mA = Pattern.compile("activity (.*)").matcher(line);
		Matcher mB = Pattern.compile("bind (.*)(:.*)").matcher(line);
		Matcher mUnary = Pattern.compile("(.*)\\[(.*)\\](.*)").matcher(line);
		Matcher mBinary = Pattern.compile("(.*)\\[(.*), (.*)\\](.*)").matcher(line);
		if(mA.find()) {
			String rest = mA.group(1).trim().replace(" ", "_");
			return "activity "+rest;
		}
		if(mB.find()) {
			String rest1 = mB.group(1).trim().replace(" ", "_");
			String rest2 = mB.group(2).trim();
			return "bind "+rest1+rest2;
		}
		if(mBinary.find()) {
			String rest1 = mBinary.group(2).trim().replace(" ", "_");
			String rest2 = mBinary.group(3).trim().replace(" ", "_");
			return mBinary.group(1)+"["+rest1+", "+rest2+"]"+mBinary.group(4).trim();
		}
		if(mUnary.find()) {
			String rest1 = mUnary.group(2).trim().replace(" ", "_");
			return mUnary.group(1)+"["+rest1+"]"+mUnary.group(3).trim();
		}
		return line;
	}
	
	private String workWithScanner(Scanner sc) {
		StringBuilder sb = new StringBuilder();
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			line = activitySpaceToUnderscore(line);
			if(line.indexOf('[') != -1 && line.indexOf(']') != -1) {
				line = line.replace("T.", "B.");
				line = organizeForAlloy(line);
				int index = line.lastIndexOf('|');
				String cond = index != -1 ? line.substring(0,index) : line;
				line = cond;
			}
			sb.append(line+"\n");
		}
		Path p = Paths.get("model.tmp");
		try (BufferedWriter writer = Files.newBufferedWriter(p))
		{
		    writer.write(sb.toString());
		    writer.close();
		    sc.close();
		    return p.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			sc.close();
			return "";
		}
	}
	
	private String organizeForAlloy(String line) {
		// TODO Auto-generated method stub
		int lb = line.indexOf('[');
		int rb = line.indexOf(']');
		String template = line.substring(0, lb);
		String[] arr = template.split(" ");
		if(arr.length == 1) {
			if(arr[0].endsWith("1")) {
				String activity = line.substring(lb+1,rb);
				String rest = line.substring(rb+1);
				return arr[0].substring(0, arr[0].length()-1)+"["+activity+", 1]"+rest;
			}
			if(arr[0].endsWith("2")) {
				String activity = line.substring(lb+1,rb);
				String rest = line.substring(rb+1);
				return arr[0].substring(0, arr[0].length()-1)+"["+activity+", 2]"+rest;
			}
			else if(arr[0].endsWith("3")) {
				String activity = line.substring(lb+1,rb);
				String rest = line.substring(rb+1);
				return arr[0].substring(0, arr[0].length()-1)+"["+activity+", 3]"+rest;
			}
			return line;
		}
		else {
			String rest = line.substring(lb);
			String res = "";
			for(String s: arr) {
				res += s;
			}
			return res+rest;
		}
	}

	//Creates a new file where "T." is replaced with "B." and does some other preprocessing
	private String replaceTDotWithBDot(String path) {
		try {
			if(path.endsWith(".decl")) {
				Scanner s = new Scanner(new File(path));
				return workWithScanner(s);
			}
			else {
				Scanner s = new Scanner(path);
				return workWithScanner(s);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return "";
		}
	}
	
	//Setup for Alloy log generation, call to minerful library and storing the result
	public String generateWithAlloyLogGen() {
		System.out.println("Preparing input model...");
		String tmp_file = replaceTDotWithBDot(mpFile); //mpFile is the input model path
		if(tmp_file != null && !tmp_file.isEmpty()) {
//			StringBuilder builder = new StringBuilder("java -jar AlloyLogGenerator2.jar ");
//			builder.append(minTraceLength.getText())
//			.append(' ').append(maxTraceLength.getText())
//			.append(' ').append(amountOfTraces.getText())
//			.append(" \"").append(tmp_file)
//			.append("\" \"").append(modelName.getText()).append('"');
//			builder.append(isVacuityEnabled())
//					.append(isNegativeTraces())
//					.append(isEvenLength());
//            builder.append(" -msi ").append(2)
//                .append(" -shuffle ").append(Integer.valueOf(amountOfTraces.getText()))
//                .append(" -is ").append(1);
//            builder.append(" -underscore_spaces");
//            
//            String args = builder.toString();
            /*Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Log Generation");
			alert.setHeaderText("Successful");
			alert.setContentText("Log is successfully generated with AlloyLogGenerator");
			//alert.showAndWait();*/
			boolean isOk = false;
//			StringBuilder errorBuilder = new StringBuilder();
            try {
            	System.out.println("Starting the generation...");
            	AlloyRunner.generateLog( // Changed from MPDeclareLogGenerator to AlloyLogGenerator library
            			minTraceLength.getText(), 
            			maxTraceLength.getText(), 
            			amountOfTraces.getText(), 
            			tmp_file, 
            			modelName.getText(), //NB! modelName is actually the output path
            			vacTrue.isSelected(), 
            			negTrue.isSelected(), 
            			evenTrue.isSelected());
            	isOk = true;
            	/*ProcessBuilder pb = new ProcessBuilder(args.split(" "));
            	//pb.inheritIO();
        		Process p = pb.start();
        		isOk = isSuccessful(p.getInputStream(),errorBuilder);*/
			} catch (Exception e) {
				System.out.println("Log not generated!");
				// TODO Auto-generated catch block
				return "Error: Log is not generated with AlloyLogGenerator";
			}
            if(isOk) {
            	System.out.println("Task returned a path");
            	//System.out.println("Oldu be!");
            	return modelName.getText();
            }
            else {
            	System.out.println("Log not generated!");
            	//System.out.println("Hata var!");
            	return "Error: Log is not generated with AlloyLogGenerator";
            }
		}
		System.out.println("Log not generated!");
		return "Error";
	}
	
	//Called by LogGenTask to start the log generation
	public String generateLog(String choice) {
		System.out.println("Log gen starts millis: "+System.currentTimeMillis());
		//String choice = modelGeneratorChoice.getSelectionModel().getSelectedItem();
		if(choice == null) {
			return "A generator must be selected";
		}
		else {
			if(modelName.getText().trim().isEmpty()) {
				return "Model name must not be empty";
			}
			else {
				if(minTraceLength.getText().trim().isEmpty()) {
					return "Minimum trace length must not be empty";
				}
				else {
					if(maxTraceLength.getText().trim().isEmpty()) {
						return "Maximum trace length must not be empty";
					}
					else {
						if(amountOfTraces.getText().trim().isEmpty()) {
							return "Amount of traces must not be empty";
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
									return generateWithMinerFul();
								}
								else if(choice.equals("AlloyLogGenerator")) {
									return generateWithAlloyLogGen();
								}
								return "Generator not available";
							}
							catch(NumberFormatException e) {
								if(i == -1) return "Minimum trace length value is not valid";
								if(j == -1) return "Maximum trace length value is not valid";
								if(k == -1) return "Amount of traces value is not valid";
								return "Error";
							}
						}
					}
				}
			}
		}
	}

}
