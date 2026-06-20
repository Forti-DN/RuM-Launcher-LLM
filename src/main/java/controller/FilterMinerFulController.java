package controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import minerful.concept.ProcessModel;
import minerful.concept.TaskCharArchive;
import minerful.concept.constraint.ConstraintsBag;
import minerful.postprocessing.params.PostProcessingCmdParameters;
import util.AlertUtils;
import util.GraphGenerator;
import util.ModelExporter;
import view.Browser;

public class FilterMinerFulController extends Pane{
	
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
	
	/*private MySlider supportSlider;
	
	@FXML
	private Label confidenceLabel;
	
	private MySlider confidenceSlider;
	
	@FXML
	private Label interestLabel;
	
	private MySlider interestSlider;
	
	@FXML
	private Label actSuppLabel;
	
	private MySlider actSuppSlider;
	
	@FXML
	private ChoiceBox<String> constraintMetricChoice; */
	
	private ConfigurationMinerFulController controller;
	
	private final ProcessModel initialModel;
	
	private ProcessModel filteredModel;
	
	private PostProcessingCmdParameters params;
	
	private Pane outputPane;
	
	private Browser currentBrowser;
	private TabbedMainViewController tmvc;
	
	public FilterMinerFulController(ConfigurationMinerFulController controller, ProcessModel initial, ProcessModel filtered,
			PostProcessingCmdParameters params,Pane outputPane) {
		this.controller = controller;
		this.initialModel = initial;
		this.filteredModel = filtered;
		this.params = params;
		this.outputPane = outputPane;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/Filter.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	public void setTmvc(TabbedMainViewController tmvc) {
		this.tmvc = tmvc;
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
					b.getZoomSlider(), "Textual", "event occurences");
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
					b.getZoomSlider(), "Declare", "event occurences");
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
					b.getZoomSlider(), "Automaton", "event occurences");
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
		/*constraintMetricChoice.setItems(FXCollections.observableList(Arrays.asList("Support","Confidence","Interest Factor")));
		constraintMetricChoice.getSelectionModel().select("Support");
		
		constraintMetricChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			addMetricLabel(ov.getValue());
		});
		
		supportSlider = new MySlider();
		confidenceSlider = new MySlider();
		interestSlider = new MySlider();
		actSuppSlider = new MySlider();*/
		modelViewChoice.getItems().addAll("Declare","Textual","Automaton");
		modelViewChoice.getSelectionModel().selectFirst();
		
		modelViewChoice.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			if(!oldV.equals(newV)) {
				changeOutputView();
			}
		});
		
		minSupportConstraintSlider = new MySlider();
		
		minSupportActivitySlider = new MySlider();
		/*zoomLabel.setText(String.format("%.1f%%", zoomSlider.getValue()));
		
		zoomSlider.valueProperty().addListener((ov,oldV,newV) -> {
			Pane p = (Pane) this.outputPane.getChildren().get(0);
			WebView w = (WebView) p.getChildren().get(0);
			w.setZoom(newV.doubleValue()/100);
			//outputPane.getChildren().get(0).setScaleX(newV.doubleValue()/100);
			//outputPane.getChildren().get(0).setScaleY(newV.doubleValue()/100);
			zoomLabel.setText(String.format("%.1f%%", newV.doubleValue()));
		});*/
		
		minSupportConstraintSlider.setMin(params.supportThreshold * 100);
		//confidenceSlider.setMin(params.confidenceThreshold * 100);
		//interestSlider.setMin(params.interestFactorThreshold * 100);
		minSupportActivitySlider.setMin(controller.getActivitySupportThreshold() * 100);
		
		//System.out.println(String.format("%.1f%%",supportSlider.getMin()));
		constraintLabel.setText(String.format("%.1f%%",minSupportConstraintSlider.getMin()));
		
		//System.out.println(String.format("%.1f%%",confidenceSlider.getMin()));
		//confidenceLabel.setText(String.format("%.1f%%",confidenceSlider.getMin()));
		
		//System.out.println(String.format("%.1f%%",interestSlider.getMin()));
		//interestLabel.setText(String.format("%.1f%%",interestSlider.getMin()));
		
		activityLabel.setText(String.format("%.1f%%",minSupportActivitySlider.getMin()));
		
		minSupportConstraintSlider.valueProperty().addListener((ov, oldVal, newVal) -> {
			constraintLabel.setText(String.format("%.1f%%", newVal.doubleValue()));
		});
		
		/*confidenceSlider.valueProperty().addListener((ov, oldVal, newVal) -> {
			confidenceLabel.setText(String.format("%.1f%%", newVal.doubleValue()));
		});
		
		interestSlider.valueProperty().addListener((ov, oldVal, newVal) -> {
			interestLabel.setText(String.format("%.1f%%", newVal.doubleValue()));
		});*/
		
		minSupportActivitySlider.valueProperty().addListener((ov, oldVal, newVal) -> {
			activityLabel.setText(String.format("%.1f%%", newVal.doubleValue()));
		});
		
		minSupportConstraintSlider.valueChangingProperty().addListener((ov, oldVal, newVal) -> {
			if(!ov.getValue()) {
				System.out.print(LocalDateTime.now());
				System.out.print(" - ");
				System.out.println("Support Slider has changed!");
				applyFiltering();
			}
		});
		
		/*confidenceSlider.valueChangingProperty().addListener((ov, oldVal, newVal) -> {
			if(!ov.getValue()) {
				System.out.print(LocalDateTime.now());
				System.out.print(" - ");
				System.out.println("Confidence Slider has changed!");
				applyFiltering();
			}
		});
		
		interestSlider.valueChangingProperty().addListener((ov, oldVal, newVal) -> {
			if(!ov.getValue()) {
				System.out.print(LocalDateTime.now());
				System.out.print(" - ");
				System.out.println("Interest Slider has changed!");
				applyFiltering();
			}
		});*/
		
		minSupportActivitySlider.valueChangingProperty().addListener((ov, oldVal, newVal) -> {
			if(!ov.getValue()) {
				System.out.print(LocalDateTime.now());
				System.out.print(" - ");
				System.out.println("Act Supp Slider has changed!");
				applyFiltering();
			}
		});
		
		minSupportConstraintSlider.setLayoutX(constraintLabel.getLayoutX()-205);
		minSupportConstraintSlider.setPrefWidth(205.0);
		minSupportConstraintSlider.setPrefHeight(21.0);
		minSupportConstraintSlider.setLayoutY(constraintLabel.getLayoutY());
		
		/*confidenceSlider.setLayoutX(confidenceLabel.getLayoutX()-205);
		confidenceSlider.setPrefWidth(205.0);
		confidenceSlider.setPrefHeight(21.0);
		confidenceSlider.setLayoutY(confidenceLabel.getLayoutY());
		
		interestSlider.setLayoutX(interestLabel.getLayoutX()-205);
		interestSlider.setPrefWidth(205.0);
		interestSlider.setPrefHeight(21.0);
		interestSlider.setLayoutY(interestLabel.getLayoutY());*/
		
		minSupportActivitySlider.setLayoutX(activityLabel.getLayoutX()-205);
		minSupportActivitySlider.setPrefWidth(205.0);
		minSupportActivitySlider.setPrefHeight(21.0);
		minSupportActivitySlider.setLayoutY(activityLabel.getLayoutY());
		
		this.getChildren().addAll(minSupportConstraintSlider,minSupportActivitySlider);
	}
	
	private void applyFiltering() {
		double support = minSupportConstraintSlider.getValue() / 100;
		controller.setConstraintSupportThreshold(support);
		drawResult();
	}
	
	private void reset() {
		TaskCharArchive archive = new TaskCharArchive(initialModel.getTaskCharArchive().getCopyOfTaskChars());
		ConstraintsBag bag = (ConstraintsBag) initialModel.bag.clone();
		filteredModel = new ProcessModel(archive,bag);
	}
	
	private void drawResult() {
		controller.setActivitySupportThreshold(minSupportActivitySlider.getValue()/100);
		Browser b = controller.drawInOutputPane(initialModel, controller.getActivitySupportMap(),this.modelViewChoice.getSelectionModel().getSelectedItem());
		this.currentBrowser = b;
		this.outputPane.getChildren().clear();
		this.outputPane.getChildren().add(b);
	}
	
	/*private void addMetricLabel(String metric) {
		Browser b = controller.drawGraph(controller.getAllActivities(filteredModel),controller.getConstraintParametersMap(filteredModel),
				controller.getTemplatesMap(filteredModel),controller.getValuesMap(filteredModel),metric,controller.getActivitySupportMap());
		this.currentBrowser = b;
		this.outputPane.getChildren().clear();
		this.outputPane.getChildren().add(b);
	}*/
	
	public Browser getCurrentBrowser() {
		return currentBrowser;
	}
	

}
