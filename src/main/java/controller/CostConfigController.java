package controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.DataConformance.framework.ActivityMatchCost;
import org.processmining.plugins.DataConformance.framework.VariableMatchCost;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import util.ActivityCostConfig_old;
import util.AlertUtils;
import util.VariableCostConfig;

public class CostConfigController extends TabPane {
	
	@FXML
	private ChoiceBox<String> logEventChoice;
	
	@FXML
	private ChoiceBox<String> processActivityChoice;
	
	@FXML
	private ChoiceBox<String> activityChoice;
	
	@FXML
	private ChoiceBox<String> attributeChoice;
	
	@FXML
	private TextField costField;
	
	@FXML
	private TextField nonWritingText;
	
	@FXML
	private TextField faultyValueText;
	
	@FXML
	private TableView<ActivityCostConfig_old> activityCostTable;
	
	@FXML
	private TableView<VariableCostConfig> variableCostTable;
	
	@FXML
	private TableColumn<ActivityCostConfig_old,String> logEventColumn;
	
	@FXML
	private TableColumn<ActivityCostConfig_old,String> processActivityColumn;
	
	@FXML
	private TableColumn<ActivityCostConfig_old,Float> costColumn;
	
	@FXML
	private TableColumn<VariableCostConfig,String> activityColumn;
	
	@FXML
	private TableColumn<VariableCostConfig,String> attributeColumn;
	
	@FXML
	private TableColumn<VariableCostConfig,Float> nonWritingColumn;
	
	@FXML
	private TableColumn<VariableCostConfig,Float> faultyValueColumn;
	
	@FXML
	private Tab variableCostTab;
	
	private Map<ReplayableActivityDefinition, XEventClass> map;
	
	private String filepath;
	
	private List<String> logEvents = new ArrayList<String>(); //Used only to fill the activity ChoiceBox in the flow cost model
	
	private List<String> processActivities = new ArrayList<String>(); //Move types (insertion/deletion)
	
	private List<String> matched = new ArrayList<String>(); //Used only to fill the activity ChoiceBox in the data cost model
	private List<String> unmatched = new ArrayList<String>(); //Used only in insertToAMCL
	
	private Set<String> attributes; //Used only to fill the attribute ChoiceBox in the data cost model
	
	//private Map<String,List<String>> aaMap = new HashMap<String,List<String>>();
	
	private List<ActivityMatchCost> lamc = new ArrayList<ActivityMatchCost>();
	
	private List<VariableMatchCost> lvmc = new ArrayList<VariableMatchCost>();
	
	private boolean isVariableCostApplied;
	
	private TabbedMainViewController tmvc;
	
	public CostConfigController(Map<ReplayableActivityDefinition, XEventClass> map, String filepath, boolean isVariableCostApplied) {
		this.map = map;
		this.filepath = filepath;
		this.isVariableCostApplied = isVariableCostApplied;
		initLists(); //Divides activities into lists logEvents, matched and unmatched
		initLists2(); //Finds attributes used in the model
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/CostConfigView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public Map<ReplayableActivityDefinition, XEventClass> getMap() {
		return map;
	}
	
	public void setTmvc(TabbedMainViewController tmvc) {
		this.tmvc = tmvc;
	}
	
	public boolean isVariableCostApplied() {
		return isVariableCostApplied;
	}
	
	private void initLists() {
		map.forEach((k,v) -> {
			if(!k.getLabel().equals("TICK") && v != null) {
				logEvents.add(k.getLabel());
				matched.add(k.getLabel());
			}
			else if(k.getLabel().equals("TICK") && v != null) {
				String logE = v.getId();
				int lp = logE.lastIndexOf('+');
				if(lp != -1) {
					String leftPlus = logE.substring(0, lp);
					String rightPlus = logE.substring(lp+1);
					logEvents.add(leftPlus+"-"+rightPlus);
				}
				else logEvents.add(v.getId());
			}
			else if(v == null) {
				unmatched.add(k.getLabel());
				logEvents.add(k.getLabel());
			}
		});
		//logEvents.add("");
		logEvents.add("*");
		
		processActivities.add("Move In Model (Insertion)");
		processActivities.add("Move In Log (Deletion)");
		
	}
	
	private void scannerJob(Scanner sc) {
		List<String> activations = new ArrayList<String>();
		List<String> correlations = new ArrayList<String>();
		Pattern pA = Pattern.compile(" ?A\\.(\\w+) (.*)");
		Pattern pT = Pattern.compile(" ?T\\.(\\w+) (.*)");
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if(line.startsWith("activity")) {}
			else if(line.startsWith("bind")) {}
			else {
				Pattern p = Pattern.compile("\\w+(\\[.*\\]) \\|");
				Matcher m = p.matcher(line);
				if(m.find()) {
					Matcher mA = pA.matcher(line);
					Matcher mT = pT.matcher(line);
					while(mA.find()) {
						activations.add(mA.group(1));
						mA = pA.matcher(mA.group(2));
					}
					while(mT.find()) {
						correlations.add(mT.group(1));
						mT = pT.matcher(mT.group(2));
					}
				}
			}
		}
		activations.addAll(correlations);
		this.attributes = new HashSet<String>(activations);
		sc.close();
	}
	
	private void initLists2() {
		try {
			if(filepath.endsWith(".decl")) {
				Scanner s = new Scanner(new File(filepath));
				scannerJob(s);
			}
			else {
				Scanner s = new Scanner(filepath);
				scannerJob(s);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void insertToAMCL(String le, String pa, float cost) {
		ActivityMatchCost amc = new ActivityMatchCost();
		if(le.equals("*") && pa.equals("Move In Log (Deletion)")) {
			amc.setAllEvents(true);
			amc.setAllProcessActivities(false);
			amc.setCost(cost);
			lamc.add(amc);
			System.out.println(amc);
			return;
		}
		if(le.equals("*") && pa.equals("Move In Model (Insertion)")) {
			amc.setAllEvents(false);
			amc.setAllProcessActivities(true);
			amc.setCost(cost);
			lamc.add(amc);
			System.out.println(amc);
			return;
		}
		if(!le.isEmpty() && pa.equals("Move In Log (Deletion)")) {
			for(ReplayableActivityDefinition rad : map.keySet()) {
				if(rad.getLabel().equals(le) && map.get(rad) != null) {
					amc.setEventClass(map.get(rad));
					amc.setCost(cost);
					lamc.add(amc);
					return;
				}
				else if(rad.getLabel().equals("TICK")) {
					XEventClass xec = map.get(rad);
					
					int lm = le.lastIndexOf('-');
					if(lm != -1) {
						String leftMinus = le.substring(0, lm);
						String rightMinus = le.substring(lm+1); 
						if(xec.getId().equals(leftMinus+"+"+rightMinus)) {
							amc.setEventClass(xec);
							amc.setCost(cost);
							lamc.add(amc);
						}
					}
					else {
						if(xec.getId().equals(le)) {
							amc.setEventClass(xec);
							amc.setCost(cost);
							lamc.add(amc);
						}
					}
				}
			}
		}
		if(!le.isEmpty() && pa.equals("Move In Model (Insertion)")) {
			for(ReplayableActivityDefinition rad : map.keySet()) {
				if(rad.getLabel().equals(le)) {
					amc.setProcessActivity(rad);
					amc.setCost(cost);
					lamc.add(amc);
					System.out.println(amc);
					return;
				}
			}
			if(unmatched.contains(le)) {
				//This seems redundant, because everything that can be found here would have been found in the previous loop
				map.forEach((k,v) -> {
					if(v == null && k.getLabel().equals(le)) {
						amc.setProcessActivity(k);
					}
				});
				amc.setCost(cost);
				lamc.add(amc);
				System.out.println(amc);
				return;
			}
		}
	}
	
	private void insertToAVCL(String activity, String attribute, float nonWC, float fvC) {
		VariableMatchCost vmc = new VariableMatchCost();
		if(activity.equals("*")) vmc.setActivity(null);
		else vmc.setActivity(activity);
		if(attribute.equals("*")) vmc.setVariable(null);
		else vmc.setVariable(attribute);
		vmc.setCostFaultyValue(fvC);
		vmc.setCostNotWriting(nonWC);
		System.out.println(vmc);
		lvmc.add(vmc);
	}
	
	public List<ActivityMatchCost> getLamc() {
		lamc.clear();
		for(ActivityCostConfig_old acc: activityCostTable.getItems()) {
			insertToAMCL(acc.getLogEvent(), acc.getAction(), acc.getCost());
		}
		return lamc;
	}
	
	public static List<ActivityMatchCost> defaultLamc() {
		List<ActivityMatchCost> list = new ArrayList<ActivityMatchCost>();
		ActivityMatchCost amc = new ActivityMatchCost();
		amc.setAllEvents(true);
		amc.setAllProcessActivities(false);
		amc.setCost(10.0f);
		list.add(amc);
		
		ActivityMatchCost amc2 = new ActivityMatchCost();
		amc2.setAllEvents(false);
		amc2.setAllProcessActivities(true);
		amc2.setCost(10.0f);
		list.add(amc2);
		return list;
	}
	
	public List<VariableMatchCost> getLvmc() {
		lvmc.clear();
		for(VariableCostConfig vcc: variableCostTable.getItems()) {
			insertToAVCL(vcc.getActivity(), vcc.getAttribute(), vcc.getNonWritingCost(), vcc.getFaultyValueCost());
		}
		return lvmc;
	}
	
	public static List<VariableMatchCost> defaultLvmc() {
		List<VariableMatchCost> list = new ArrayList<VariableMatchCost>();
		VariableMatchCost vmc = new VariableMatchCost();
		vmc.setActivity(null);
		vmc.setVariable(null);
		vmc.setCostFaultyValue(1.0f);
		vmc.setCostNotWriting(1.0f);
		list.add(vmc);
		return list;
	}
	
	@FXML
	public void closeCC() {
		this.tmvc.closeCostConfig();
	}
	
	@FXML
	public void restoreCC() {
		activityCostTable.getItems().clear();
		variableCostTable.getItems().clear();
		
		ActivityCostConfig_old acc = new ActivityCostConfig_old();
		acc.setCost(10.0f);
		acc.setLogEvent("*");
		acc.setAction("Move In Model (Insertion)");
		activityCostTable.getItems().add(acc);
		ActivityCostConfig_old acc2 = new ActivityCostConfig_old();
		acc2.setCost(10.0f);
		acc2.setLogEvent("*");
		acc2.setAction("Move In Log (Deletion)");
		activityCostTable.getItems().add(acc2);
		
		VariableCostConfig vcc = new VariableCostConfig();
		vcc.setActivity("*");
		vcc.setAttribute("*");
		vcc.setNonWritingCost(1.0f);
		vcc.setFaultyValueCost(1.0f);
		variableCostTable.getItems().add(vcc);
	}
	
	@FXML
	public void initialize() {
		logEventColumn.setCellValueFactory(new PropertyValueFactory<ActivityCostConfig_old,String>("logEvent"));
		processActivityColumn.setCellValueFactory(new PropertyValueFactory<ActivityCostConfig_old,String>("action"));
		costColumn.setCellValueFactory(new PropertyValueFactory<ActivityCostConfig_old,Float>("cost"));
		
		logEventColumn.setPrefWidth(activityCostTable.getPrefWidth() / 3.0);
		processActivityColumn.setPrefWidth(activityCostTable.getPrefWidth() / 3.0);
		costColumn.setPrefWidth(activityCostTable.getPrefWidth() / 3.0);
		
		logEventChoice.getItems().addAll(logEvents);
		
		processActivityChoice.getItems().addAll(processActivities);
		
		logEventChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			String choice = newV;
			if(choice == null) return;
			else {
				processActivityChoice.getSelectionModel().selectFirst();
			}
		});
		
		/*processActivityChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			String choice = newV;
			if(choice == null) return;
			if(choice.equals("")) {
				logEventChoice.getItems().clear();
				logEventChoice.getItems().addAll(logEvents);
			}
			else {
				logEventChoice.getItems().clear();
			}
		});*/
		
		activityCostTable.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if(newV == null) return;
			String le = newV.getLogEvent();
			String pa = newV.getAction();
			float cost = newV.getCost();
			logEventChoice.getSelectionModel().select(le);
			processActivityChoice.getSelectionModel().select(pa);
			costField.setText(cost+"");
		});
		
		ActivityCostConfig_old acc = new ActivityCostConfig_old();
		acc.setCost(10.0f);
		acc.setLogEvent("*");
		acc.setAction("Move In Model (Insertion)");
		activityCostTable.getItems().add(acc);
		ActivityCostConfig_old acc2 = new ActivityCostConfig_old();
		acc2.setCost(10.0f);
		acc2.setLogEvent("*");
		acc2.setAction("Move In Log (Deletion)");
		activityCostTable.getItems().add(acc2);
		
		if(!isVariableCostApplied) {
			this.getTabs().remove(variableCostTab);
		}
		else {
			activityColumn.setCellValueFactory(new PropertyValueFactory<VariableCostConfig,String>("activity"));
			attributeColumn.setCellValueFactory(new PropertyValueFactory<VariableCostConfig,String>("attribute"));
			nonWritingColumn.setCellValueFactory(new PropertyValueFactory<VariableCostConfig,Float>("nonWritingCost"));
			faultyValueColumn.setCellValueFactory(new PropertyValueFactory<VariableCostConfig,Float>("faultyValueCost"));
			
			activityChoice.getItems().addAll(matched);
			activityChoice.getItems().add("*");
			attributeChoice.getItems().addAll(this.attributes);
			attributeChoice.getItems().add("*");
			/*activityChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
				String a = newV;
				if(!a.equals("*")) {
					attributeChoice.getItems().clear();
					attributeChoice.getItems().addAll(aaMap.get(a));
					attributeChoice.getItems().add("*");
					attributeChoice.getSelectionModel().select("*");
				}
				else if(a.equals("*")) {
					attributeChoice.getItems().clear();
					attributeChoice.getItems().addAll(attributes);
					attributeChoice.getItems().add("*");
					attributeChoice.getSelectionModel().select("*");
				}
			});*/
			
			variableCostTable.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
				if(newV == null) return;
				activityChoice.getSelectionModel().select(newV.getActivity());
				attributeChoice.getSelectionModel().select(newV.getAttribute());
				nonWritingText.setText(newV.getNonWritingCost()+"");
				faultyValueText.setText(newV.getFaultyValueCost()+"");
			});
			
			VariableCostConfig vcc = new VariableCostConfig();
			vcc.setActivity("*");
			vcc.setAttribute("*");
			vcc.setNonWritingCost(1.0f);
			vcc.setFaultyValueCost(1.0f);
			variableCostTable.getItems().add(vcc);
		}
	}
	
	@FXML
	public void addMapping() {
		activityCostTable.getSelectionModel().clearSelection();
		try {
			String le = logEventChoice.getSelectionModel().getSelectedItem();
			String pa = processActivityChoice.getSelectionModel().getSelectedItem();
			if(le == null) {
				AlertUtils.showError("A log event must be selected!");
				return;
			}
			if(pa == null) {
				AlertUtils.showError("An action must be selected!");
				return;
			}
			float cost = Float.valueOf(costField.getText());
			ActivityCostConfig_old acc = new ActivityCostConfig_old();
			acc.setCost(cost);
			acc.setLogEvent(le);
			acc.setAction(pa);
			activityCostTable.getItems().remove(acc);
			if(acc.getLogEvent().equals("*")) {
				activityCostTable.getItems().add(acc);
			}
			else {
				activityCostTable.getItems().add(0, acc);
			}
			activityCostTable.refresh();
		}catch(Exception e) {
			AlertUtils.showError("Cost value is invalid!");
			return;
		}
	}
	
	@FXML
	public void addMapping2() {
		variableCostTable.getSelectionModel().clearSelection();
		String activity = activityChoice.getSelectionModel().getSelectedItem();
		String attribute = attributeChoice.getSelectionModel().getSelectedItem();
		if(activity == null) {
			AlertUtils.showError("Activity must be selected!");
			return;
		}
		try {
			float nonWritingCost = Float.valueOf(nonWritingText.getText());
			float faultyValueCost = Float.valueOf(faultyValueText.getText());
			VariableCostConfig vcc = new VariableCostConfig();
			vcc.setActivity(activity);
			vcc.setAttribute(attribute);
			vcc.setFaultyValueCost(faultyValueCost);
			vcc.setNonWritingCost(nonWritingCost);
			variableCostTable.getItems().remove(vcc);
			if(vcc.getActivity().equals("*") && vcc.getAttribute().equals("*")) {
				variableCostTable.getItems().add(vcc);
			}	
			else {
				variableCostTable.getItems().add(0,vcc);
			}
			variableCostTable.refresh();
		}catch(Exception e) {
			AlertUtils.showError("Cost value for either non writing or faulty value is invalid!");
			return;
		}
	}
	
	@FXML
	public void removeMapping2() {
		VariableCostConfig vcc = variableCostTable.getSelectionModel().getSelectedItem();
		variableCostTable.getSelectionModel().clearSelection();
		if(vcc == null) {
			AlertUtils.showError("An entry must be selected!");
			return;
		}
		if(vcc.getActivity().equals("*") && vcc.getAttribute().equals("*")) {
			return;
		}
		else {
			variableCostTable.getItems().remove(vcc);
			variableCostTable.refresh();
		}
	}
	
	@FXML
	public void removeMapping() {
		ActivityCostConfig_old selected = activityCostTable.getSelectionModel().getSelectedItem();
		if(selected == null) {
			AlertUtils.showError("An entry must be selected!");
			return;
		}
		activityCostTable.getSelectionModel().clearSelection();
		if(selected.getLogEvent().equals("*")) return;
		activityCostTable.getItems().remove(selected);
		activityCostTable.refresh();
	}

}
