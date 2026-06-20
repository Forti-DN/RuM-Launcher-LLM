package controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;
import org.processmining.plugins.declareminer.visualizing.ActivityDefinition;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;
import org.processmining.plugins.declareminer.visualizing.DeclareMap;
import org.processmining.plugins.declareminer.visualizing.DeclareMinerOutput;
import org.processmining.plugins.declareminer.visualizing.Parameter;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import util.AlertUtils;
import util.GraphGenerator;
import util.ModelExporter;
import view.Browser;

public class FilterController extends Pane {

	@FXML
	private Label minSupportLabel;
	
	private MySlider minSupportConstraintSlider;
	
	private MySlider minSupportActivitySlider;
	
	@FXML
	private Label constraintLabel;
	
	@FXML
	private Label activityLabel;
	
	@FXML
	private ChoiceBox<String> modelViewChoice;
	
	private final DeclareMinerOutput declareMinerOutput;
	
	private Set<Integer> constraintFilterSet;
	
	private Set<Integer> activityFilterSet;
	
	private Set<Integer> filteredActivitySet;
	
	private double oldActivitySupport;
	private double oldConstraintSupport;
	
	private Pane outputPane;
	
	private ConfigurationController controller;
	
	private Browser currentBrowser;
	
	private String view;
	
	private TabbedMainViewController tmvc;
	
	public FilterController(DeclareMinerOutput declareMinerOutput, Pane outputPane, ConfigurationController controller) {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/Filter.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        this.declareMinerOutput = declareMinerOutput;
        this.constraintFilterSet = declareMinerOutput.getConstraintParametersMap().keySet();
        this.activityFilterSet = declareMinerOutput.getConstraintParametersMap().keySet();
        this.filteredActivitySet = declareMinerOutput.getAllActivities().keySet();
        this.outputPane = outputPane;
        this.controller = controller;
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public Browser getCurrentBrowser() {
		return currentBrowser;
	}
	
	public void setTmvc(TabbedMainViewController tmvc) {
		this.tmvc = tmvc;
	}
	
	public void setViewChoice(String v) {
		this.view = v;
	}
	
	public void setOutputPane(AnchorPane outputPane) {
		this.outputPane = outputPane;
	}
	
	private void changeOutputView() {
		String choice = modelViewChoice.getSelectionModel().getSelectedItem();
		Browser b = (Browser) this.outputPane.getChildren().get(0);
		if(choice.equals("Textual")) {
			Browser btextual = GraphGenerator.browserify
					(b.getActivitiesMap(), 
					b.getActSuppMap(), 
					b.getTemplatesMap(), 
					b.getConstraintParametersMap(), 
					b.getConstraintSuppMap(), 
					b.getZoomSlider(), "Textual", "traces");
			this.outputPane.getChildren().clear();
			this.outputPane.getChildren().add(btextual);
		}
		if(choice.equals("Declare")) {
			Browser btextual = GraphGenerator.browserify
					(b.getActivitiesMap(), 
					b.getActSuppMap(), 
					b.getTemplatesMap(), 
					b.getConstraintParametersMap(), 
					b.getConstraintSuppMap(), 
					b.getZoomSlider(), "Declare", "traces");
			this.outputPane.getChildren().clear();
			this.outputPane.getChildren().add(btextual);
		}
		if(choice.equals("Automaton")) {
			Browser bautomaton = GraphGenerator.browserify
					(b.getActivitiesMap(), 
					b.getActSuppMap(), 
					b.getTemplatesMap(), 
					b.getConstraintParametersMap(), 
					b.getConstraintSuppMap(), 
					b.getZoomSlider(), "Automaton", "traces");
			this.outputPane.getChildren().clear();
			this.outputPane.getChildren().add(bautomaton);
		}
	}
	
	@FXML
	public void saveDiscovered() {
		Browser b = (Browser) this.outputPane.getChildren().get(0);
		String data = ModelExporter.getDeclString(b.getActivitiesMap(), 
				b.getTemplatesMap(), b.getConstraintParametersMap());
		TextInputDialog dialog = new TextInputDialog(tmvc.getDiscoveredLogName());
		dialog.setTitle("Discovered model name");
		dialog.setHeaderText("Enter a name for discovered model");
		dialog.setContentText("Model name:");
		
		Optional<String> res = dialog.showAndWait();
		if(res.isPresent()) {
			tmvc.addInMemory(res.get()+".decl", data);
			AlertUtils.showSuccess("Saved successfully");
		}
	}
	
	@FXML
	public void exportModel() {
		String choice = modelViewChoice.getSelectionModel().getSelectedItem();
		Browser b = (Browser) this.outputPane.getChildren().get(0);
		if(choice.equals("Textual")) {
			String data = ModelExporter.getTextString
					(b.getActivitiesMap(),
					b.getActSuppMap(), 
					b.getTemplatesMap(), 
					b.getConstraintParametersMap(), 
					b.getConstraintSuppMap());
			FileChooser fileChooser = new FileChooser();
			ExtensionFilter filter = new ExtensionFilter("Text",Arrays.asList("*.txt"));
			fileChooser.getExtensionFilters().add(filter);
			File file = fileChooser.showSaveDialog(controller.getStage());
			//String absPath = "";
	        if (file != null) {
	        	Path path = Paths.get(file.getAbsolutePath());
	    		try (BufferedWriter writer = Files.newBufferedWriter(path))
	    		{
	    		    writer.write(data);
	    		    writer.close();
	    		    AlertUtils.showSuccess("Model is exported successfully!");
	    		} catch (Exception e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	        }
		}
		if(choice.equals("Automaton")) {
			String data = ModelExporter.getDotString(
					b.getActivitiesMap(), 
					b.getTemplatesMap(),
					b.getConstraintParametersMap());
			FileChooser fileChooser = new FileChooser();
			ExtensionFilter filter = new ExtensionFilter("Dot graph",Arrays.asList("*.dot"));
			fileChooser.getExtensionFilters().add(filter);
			File file = fileChooser.showSaveDialog(controller.getStage());
			//String absPath = "";
	        if (file != null) {
	        	Path path = Paths.get(file.getAbsolutePath());
	    		try (BufferedWriter writer = Files.newBufferedWriter(path))
	    		{
	    		    writer.write(data);
	    		    writer.close();
	    		    AlertUtils.showSuccess("Model is exported successfully!");
	    		} catch (Exception e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	        }
			
		}
		if(choice.equals("Declare")) {
			String data = ModelExporter.getDeclString(b.getActivitiesMap(), 
					b.getTemplatesMap(), b.getConstraintParametersMap());
			FileChooser fileChooser = new FileChooser();
			ExtensionFilter filter = new ExtensionFilter("Declare model",Arrays.asList("*.decl"));
			fileChooser.getExtensionFilters().add(filter);
			File file = fileChooser.showSaveDialog(controller.getStage());
			//String absPath = "";
	        if (file != null) {
	        	Path path = Paths.get(file.getAbsolutePath());
	    		try (BufferedWriter writer = Files.newBufferedWriter(path))
	    		{
	    		    writer.write(data);
	    		    writer.close();
	    		    AlertUtils.showSuccess("Model is exported successfully!");
	    		    tmvc.addModelFile(file);
	    		} catch (Exception e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	        }
		}
	}
	
	@FXML
	public void initialize() {
		modelViewChoice.getItems().addAll("Declare","Textual","Automaton");
		modelViewChoice.getSelectionModel().selectFirst();
		
		modelViewChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if(!oldV.equals(newV)) {
				changeOutputView();
			}
		});
		minSupportActivitySlider = new MySlider();
		minSupportConstraintSlider = new MySlider();
		//zoomLabel.setText(String.format("%.1f%%", zoomSlider.getValue()));
		minSupportConstraintSlider.setMin(this.declareMinerOutput.getInput().getMinSupport());
		constraintLabel.setText(String.format("%.1f%%", minSupportConstraintSlider.getMin()));
		minSupportActivitySlider.setMin(0.0);
		activityLabel.setText(String.format("%.1f%%", minSupportActivitySlider.getMin()));
		oldConstraintSupport = minSupportConstraintSlider.getValue() / 100.0;
		oldActivitySupport = minSupportActivitySlider.getValue() / 100.0;
		
		minSupportActivitySlider.valueChangingProperty().addListener((ov, oldVal, newVal) -> {
			if(!ov.getValue()) {
				if(applyActivitySupportFilter()) {
					System.out.print(LocalDateTime.now());
					System.out.print(" - ");
					System.out.println("Activity Slider has changed!");
					drawConstraints();
				}
			}
		});
		
		minSupportActivitySlider.valueProperty().addListener((ov, oldVal, newVal) -> {
				activityLabel.setText(String.format("%.1f%%", newVal.doubleValue()));
		});
		
		/*zoomSlider.valueProperty().addListener((ov,oldV,newV) -> {
			Pane p = (Pane) outputPane.getChildren().get(0);
			WebView w = (WebView) p.getChildren().get(0);
			w.setZoom(newV.doubleValue()/100);
			//outputPane.getChildren().get(0).setScaleX(newV.doubleValue()/100);
			//outputPane.getChildren().get(0).setScaleY(newV.doubleValue()/100);
			zoomLabel.setText(String.format("%.1f%%", newV.doubleValue()));
		});*/
		
		minSupportActivitySlider.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> minSupportActivitySlider.setValueChanging(true));
		minSupportActivitySlider.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			minSupportActivitySlider.setValueChanging(false);
		});
		
		
		
		minSupportConstraintSlider.valueChangingProperty().addListener((ov, oldVal, newVal) -> {
			if(!ov.getValue()) {
				if(applyConstraintSupportFilter()) {
					System.out.print(LocalDateTime.now());
					System.out.print(" - ");
					System.out.println("Constraint Slider has changed!");
					drawConstraints();
				}
			}
		});
		
		minSupportConstraintSlider.valueProperty().addListener((ov, oldVal, newVal) -> {
			constraintLabel.setText(String.format("%.1f%%", newVal.doubleValue()));
		});
		
		minSupportConstraintSlider.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> minSupportConstraintSlider.setValueChanging(true));
		minSupportConstraintSlider.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			minSupportConstraintSlider.setValueChanging(false);
		});
		
		minSupportActivitySlider.setLayoutX(activityLabel.getLayoutX()-220);
		minSupportActivitySlider.setPrefWidth(200.0);
		minSupportActivitySlider.setPrefHeight(21.0);
		minSupportActivitySlider.setLayoutY(activityLabel.getLayoutY()+4);
		//System.out.printf("Label y: %f, label height: %f, slider height: %f\n", activityLabel.getLayoutY(), activityLabel.getPrefHeight(), minSupportActivitySlider.getPrefHeight());
		
		minSupportConstraintSlider.setLayoutX(constraintLabel.getLayoutX()-220);
		minSupportConstraintSlider.setPrefWidth(200.0);
		minSupportConstraintSlider.setPrefHeight(21.0);
		minSupportConstraintSlider.setLayoutY(constraintLabel.getLayoutY()+4);
		this.getChildren().addAll(minSupportActivitySlider,minSupportConstraintSlider);
		//System.out.printf("Label y: %f, label height: %f, slider height: %f\n", constraintLabel.getLayoutY(), constraintLabel.getPrefHeight(), minSupportConstraintSlider.getPrefHeight());
	}
	
	private void drawConstraints() {
		Set<Integer> keys = activityFilterSet.stream()
								.filter(constraintFilterSet::contains)
								.collect(Collectors.toSet());
		
		/*HashMap<Integer, List<String>> filteredConstraints = new HashMap<Integer,List<String>>();
		
		keys.forEach((k)-> {
			filteredConstraints.put(k, declareMinerOutput.getVisibleConstraintParametersMap().get(k));
		});
		
		HashMap<Integer, DeclareTemplate> filteredConstraintsTemplates = new HashMap<Integer, DeclareTemplate>();
		keys.forEach((k) -> {
			filteredConstraintsTemplates.put(k, declareMinerOutput.getTemplate().get(k));
		});
		
		List<ActivityDefinition> allActivities = new LinkedList<ActivityDefinition>();
		declareMinerOutput.getModel().getModel().getActivityDefinitions().forEach(ad -> {
			allActivities.add(ad);
		});
		
		List<String> allActivitiesInFiltered = new LinkedList<String>();
		filteredConstraints.values().forEach(l -> {
			allActivitiesInFiltered.addAll(l);
		});
		
		Set<String> filteredActivitiesSet = new HashSet<String>(allActivitiesInFiltered);
		List<ActivityDefinition> filteredActivities = allActivities.stream()
				.filter(ad -> filteredActivitiesSet.contains(ad.getName()))
				.collect(Collectors.toList());
		
		//System.out.println(filteredConstraints);
		//System.out.println(filteredConstraintsTemplates);
		//System.out.println(filteredActivitiesSet);
		
		Vector<ConstraintDefinition> allDiscoveredConstraints = new Vector<ConstraintDefinition>();
		HashMap<Integer,Float> constraintSupportRuleMap = new HashMap<Integer,Float>();
		HashMap<Integer,DeclareTemplate> constraintTemplateMap = new HashMap<Integer,DeclareTemplate>();
		
		filteredConstraints.forEach((k,v) -> {
			ConstraintDefinition cd = declareMinerOutput.getAllDiscoveredConstraints().stream().filter(def -> (def.getId() == k)).findFirst().get();
			allDiscoveredConstraints.add(cd);
			constraintSupportRuleMap.put(k, declareMinerOutput.getSupportRule().get(k));
			constraintTemplateMap.put(k, declareMinerOutput.getTemplate().get(k));
		});
		
		HashMap<String, HashMap<String, ConstraintDefinition>> visible = DeclareModelGenerator.getVisibleCd
																						(declareMinerOutput.getInput().getMinSupport() / 100.0f, 
																						allDiscoveredConstraints, 
																						constraintSupportRuleMap, 
																						new Vector<String>(), 
																						new Vector<String>(), 
																						new Vector<String>());
		List<Integer> visibleKeys = new ArrayList<Integer>();
		visible.forEach((k,v) -> {
			v.forEach((kk,vv) -> {
				visibleKeys.add(vv.getId());
			});
		});
		
		Set<Integer> finalKeys = new HashSet<Integer>(visibleKeys);
		
		DeclareModelGenerator gen = new DeclareModelGenerator();
		if(declareMinerOutput.isTrans()) {
			gen.setTransitiveClosureCoexistenceConstraints(new Vector<Integer>());
			gen.setTransitiveClosureNotCoexistenceConstraints(new Vector<Integer>());
			gen.setTransitiveClosurePrecedenceConstraints(new Vector<Integer>());
			gen.setTransitiveClosureRespondedExistenceConstraints(new Vector<Integer>());
			gen.setTransitiveClosureResponseConstraints(new Vector<Integer>());
			gen.setTransitiveClosureSuccessionConstraints(new Vector<Integer>());
			
			if(constraintTemplateMap.values().contains(DeclareTemplate.CoExistence)) {
				gen.getTransitiveClosureCoexistenceConstraints(null, filteredActivities, filteredConstraints, filteredConstraintsTemplates);
			}
			
			if(constraintTemplateMap.values().contains(DeclareTemplate.Not_CoExistence)) {
				gen.getTransitiveClosureNotCoexistenceConstraints(null, filteredActivities, filteredConstraints, filteredConstraintsTemplates);
			}
			
			if(constraintTemplateMap.values().contains(DeclareTemplate.Precedence)) {
				gen.getTransitiveClosurePrecedenceConstraints(null, filteredActivities, filteredConstraints, filteredConstraintsTemplates);
			}
			
			if(constraintTemplateMap.values().contains(DeclareTemplate.Responded_Existence)) {
				gen.getTransitiveClosureRespondedExistenceConstraints(null, filteredActivities, filteredConstraints, filteredConstraintsTemplates);
			}
			
			if(constraintTemplateMap.values().contains(DeclareTemplate.Response)) {
				gen.getTransitiveClosureResponseConstraints(null, filteredActivities, filteredConstraints, filteredConstraintsTemplates);
			}
			
			if(constraintTemplateMap.values().contains(DeclareTemplate.Succession)) {
				gen.getTransitiveClosureSuccessionConstraints(null, filteredActivities, filteredConstraints, filteredConstraintsTemplates);
			}
			
		}
		//System.out.println(gen.getTransitiveClosureResponseConstraints());
		if(gen.getTransitiveClosureCoexistenceConstraints() != null) {
			gen.getTransitiveClosureCoexistenceConstraints().forEach(k -> {
				finalKeys.remove(k);
			});
		}
		
		if(gen.getTransitiveClosureNotCoexistenceConstraints() != null) {
			gen.getTransitiveClosureNotCoexistenceConstraints().forEach(k -> {
				finalKeys.remove(k);
			});
		}
		
		if(gen.getTransitiveClosurePrecedenceConstraints() != null) {
			gen.getTransitiveClosurePrecedenceConstraints().forEach(k -> {
				finalKeys.remove(k);
			});
		}
		
		if(gen.getTransitiveClosureRespondedExistenceConstraints() != null) {
			gen.getTransitiveClosureRespondedExistenceConstraints().forEach(k -> {
				finalKeys.remove(k);
			});
		}
		
		if(gen.getTransitiveClosureResponseConstraints() != null) {
			gen.getTransitiveClosureResponseConstraints().forEach(k -> {
				finalKeys.remove(k);
			});
		}
		
		if(gen.getTransitiveClosureSuccessionConstraints() != null) {
			gen.getTransitiveClosureSuccessionConstraints().forEach(k -> {
				finalKeys.remove(k);
			});
		}
		*/
		//drawOutputAlt(finalKeys, outputPane);
		List<Integer> finalKeys = new ArrayList<Integer>();
		keys.forEach(k -> {
			DeclareTemplate dt = declareMinerOutput.getTemplate().get(k);
			if(controller.isSelectedTemplate(dt)) {
				finalKeys.add(k);
			}
		});
		Browser b = controller.getBrowserFrom(declareMinerOutput,filteredActivitySet,finalKeys,modelViewChoice.getSelectionModel().getSelectedItem());
		this.currentBrowser = b;
		this.outputPane.getChildren().clear();
		this.outputPane.getChildren().add(b);
	}

	/*private HashMap<Integer,Float> applyVacuityFilter() {
		int alpha = this.declareMinerOutput.getInput().getAlpha();
		if(vacuityTrue.isSelected() && alpha==0) {
			return this.declareMinerOutput.getSupportRule();
		}
		
		if(vacuityFalse.isSelected() && alpha==100) {
			return this.declareMinerOutput.getSupportRule();
		}
		
		if(alpha == 0) {
			return this.declareMinerOutput.getSupportRule();
		}
		else {
			HashMap<Pair<String, String>, HashMap<String, Double>> activitiesStats = 
					this.declareMinerOutput.getDeclareModel()
					.getConstraints().get(DeclareTemplate.Response);
			
			HashMap<Pair<String,String>, Float> filtered = new HashMap<Pair<String,String>, Float>();
			activitiesStats.forEach((k,v) -> {
				double violatedTraces = v.get("violatedTraces");
				double completedTraces = v.get("completedTraces");
				double vacuouslySatisfiedTraces = v.get("vacuouslySatisfiedTraces");
				double minSupport = declareMinerOutput.getMinSupport() / 100.0;
				
				double calculatedSupport = (completedTraces-violatedTraces-vacuouslySatisfiedTraces) / completedTraces;
				if(calculatedSupport >= minSupport) {
					filtered.put(k, Float.valueOf(String.valueOf(calculatedSupport)));
				}
			});
			
			HashMap<Integer,Float> result = new HashMap<Integer,Float>();
			filtered.forEach((k,v) -> {
				List<String> activity = new ArrayList<String>();
				activity.add(k.getFirst());
				activity.add(k.getSecond());
				this.declareMinerOutput.getConstraintParametersMap().forEach((kk,vv) -> {
					if(vv.equals(activity)) result.put(kk, v);
				});
			});
			
			return result;
		}
	}*/
	
	private boolean applyActivitySupportFilter() {
		double support = this.minSupportActivitySlider.getValue()/100.0;
		if(support == oldActivitySupport) {
			return false;
		}
		oldActivitySupport = support;
		List<Integer> keys = new ArrayList<Integer>();
		Set<Integer> validKeys = this.declareMinerOutput.getVisiblesupportRule().keySet();
		this.declareMinerOutput.getConstraintParametersMap().forEach((k,v) -> {
			if(validKeys.contains(k)) {
				boolean isOk = true;
				for(String s: v) {
					if(getActivitySupport(s) < support) isOk = false;
				}
				if(isOk) keys.add(k);
			}	
		});
		activityFilterSet = new HashSet<Integer>(keys);
		List<Integer> keys2 = new ArrayList<Integer>();
		this.declareMinerOutput.getAllActivities().forEach((k,v) -> {
			double s = this.declareMinerOutput.getActSupp().get(k);
			if(s >= support) keys2.add(k);
		});
		filteredActivitySet = new HashSet<Integer>(keys2);
		return true;
	}
	
	private boolean applyConstraintSupportFilter() {
		double support = this.minSupportConstraintSlider.getValue()/100.0;
		if(oldConstraintSupport == support) {
			return false;
		}
		oldConstraintSupport = support;
		List<Integer> keys = new ArrayList<Integer>();
		this.declareMinerOutput.getVisiblesupportRule().forEach((k,v) -> {
			if(v >= support) keys.add(k);
		});
		constraintFilterSet = new HashSet<Integer>(keys);
		return true;
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
	
	public void drawOutputAlt(Set<Integer> keys, AnchorPane outputPane) {
		System.out.println();
		System.out.println("################# FILTERING ###########################3");
		System.out.println();
		getOutput(keys).forEach(s -> System.out.println(s));
		outputPane.getChildren().clear();
		HashMap<Integer,Boolean> plotMap = isPlottedMap(keys);
		Set<Integer> ids = plotMap.keySet();
		int multi = 0;
		int multi2 = 0;
		for(int id:ids) {
			if(!plotMap.get(id)) {
				if(declareMinerOutput.getConstraintParametersMap().get(id).size() == 1) {
					plotMap.put(id, true);
					List<String> activity = declareMinerOutput.getConstraintParametersMap().get(id);
					String startActivity = activity.get(0);
	        		String endActivity = "";
	        		double constraintSupport = declareMinerOutput.getSupportRule().get(id);
	        		String exA = declareMinerOutput.getTemplate().get(id).name()+"\n"+findExistenceConstraint(startActivity, plotMap, declareMinerOutput);
	        		String exB = "";
	        		DeclareTemplateController dtc = new DeclareTemplateController(
	        				exA,
	        				getActivitySupport(startActivity),
	        				startActivity,
	        				exB,
	        				0,
	        				endActivity,
	        				declareMinerOutput.getTemplate().get(id),
	        				constraintSupport
	        				);
	        		dtc.setLayoutX(multi2 * 200.0);
	                outputPane.getChildren().add(dtc);
	                multi2++;
				}
				if(declareMinerOutput.getConstraintParametersMap().get(id).size() == 2) {
					List<String> activity = declareMinerOutput.getConstraintParametersMap().get(id);
					String startActivity = activity.get(0);
	        		String endActivity = activity.get(1);
	        		double constraintSupport = declareMinerOutput.getSupportRule().get(id);
	        		String exA = findExistenceConstraint(startActivity, plotMap, declareMinerOutput);
	        		String exB = findExistenceConstraint(endActivity, plotMap, declareMinerOutput);
	        		DeclareTemplateController dtc = new DeclareTemplateController(
	        				exA,
	        				getActivitySupport(startActivity),
	        				startActivity,
	        				exB,
	        				getActivitySupport(endActivity),
	        				endActivity,
	        				declareMinerOutput.getTemplate().get(id),
	        				constraintSupport
	        				);
	        		dtc.setLayoutY(130.0);
	        		dtc.setLayoutX(multi * 200.0);
	                outputPane.getChildren().add(dtc);
	                plotMap.put(id, true);
	                multi++;
	                int other = findActivity(activity,plotMap);
	                if(other != 0) {
	                	activity = declareMinerOutput.getConstraintParametersMap().get(other);
						startActivity = activity.get(0);
		        		endActivity = activity.get(1);
		        		constraintSupport = declareMinerOutput.getSupportRule().get(other);
		        		exA = findExistenceConstraint(startActivity, plotMap, declareMinerOutput);
		        		exB = findExistenceConstraint(endActivity, plotMap, declareMinerOutput);
		        		DeclareTemplateController dtc2 = new DeclareTemplateController(
		        				exA,
		        				getActivitySupport(startActivity),
		        				startActivity,
		        				exB,
		        				getActivitySupport(endActivity),
		        				endActivity,
		        				declareMinerOutput.getTemplate().get(other),
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
	
	private HashMap<Integer, Boolean> isPlottedMap(Set<Integer> keys) {
		HashMap<Integer, Boolean> map = new HashMap<Integer,Boolean>();
		keys.forEach(k -> map.put(k,false));
		return map;
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
	
	private String getDeclareTemplate(String str) {
		int leftBracket = str.indexOf('(');
		return str.substring(0, leftBracket);
	}
	
	private double getActivitySupport(String activity) {
		//int id;
		Map<Integer, String> activities = declareMinerOutput.getAllActivities();
		Set<Integer> keys = activities.keySet();
		Optional<Integer> opt = keys.stream().filter(k -> activities.get(k).equals(activity)).findFirst();
		if(opt.isPresent()) {
			return declareMinerOutput.getActSupp().get(opt.get());
		}
		else return 0.0;
	}
	
	private List<String> getOutput(Set<Integer> keys) {
		//String configuration_file_path = args[0]
		LinkedList<String> outputList = new LinkedList<String>();
		
		DeclareMinerOutput output = declareMinerOutput;
		// Choose the Miner type based on configuration
		// TODO

		DeclareMap model = output.getModel();

		Iterator<ConstraintDefinition> constraints_iter = model.getModel().getConstraintDefinitions().iterator();
		while(constraints_iter.hasNext()) {
			ConstraintDefinition constraint = constraints_iter.next();
			if(!keys.contains(constraint.getId())) continue;
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
}

class MySlider extends Slider {
	@Override
	public void requestFocus() {}
}
