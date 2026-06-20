package theFirst;

import java.util.Optional;

import controller.TabbedMainViewController;
import javafx.application.*;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.*;
 
public class RumOld extends Application {
	
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
    	System.out.println("Rum start");
        primaryStage.setTitle("Rum");
        //Parent root2 = FXMLLoader.load(getClass().getResource("/view/MyView.fxml"));
        //AlignmentAnalysisResult result = Tester.run("test6.xes", "temp.xml");
        //AlignmentResultController arc = new AlignmentResultController(result);
       //List<String> le = Arrays.asList("le1","le2","","*");
       //List<String> pa = Arrays.asList("pa1","pa2","","*");
       //Map<ReplayableActivityDefinition, XEventClass> map = Runner.getMapping("test6.xes", "temp.xml");
       //CostConfigController ccc = new CostConfigController(map);
       //Scene scene = new Scene(ccc,800,800);
       TabbedMainViewController tmvc = new TabbedMainViewController(primaryStage);
       Scene scene = new Scene(tmvc, 800, 500);
       //TraceElement te1 = new TraceElement("ff00ff","Element 1");
       //TraceElement te2 = new TraceElement("ff00ff","Element 2");
       //te1.setAttributes(Arrays.asList("a1","a2"));
       //te2.setAttributes(Arrays.asList("a1","a2"));
       //TraceElement te2 = new TraceElement("ff00ff","Element 2");
       //Scene scene = new Scene(new TraceViewController(Arrays.asList(te1,te2),600,800), 600, 800);
       /* StringBuilder sb = new StringBuilder();
        sb.append("digraph G {")
          .append("size=\"10\" ratio=\"fill\"")
          .append("node [style=rounded, shape=box, fontsize=\"8\", fontname=\"Helvetica\"]")
          .append("examine_patient [shape=record, label=\"{Init|Examine Patient}\"]")
          .append("check_xray_risk [label=\"Check X-ray risk\"]")
          .append("perform_xray [label=\"Perform X-ray\"]")
          .append("perform_reposition [label=\"Perform reposition\"]")
          .append("apply_cast [label=\"Apply cast\"]")
          .append("perform_surgery [label=\"Perform surgery\"]")
          .append("remove_cast [label=\"Remove cast\"]")
          .append("rehab [label=\"Rehab\"]")
          .append("check_xray_risk -> perform_xray [color=\"black:black\", arrowhead=\"dotnormal\"]")
          .append("perform_xray -> perform_reposition [arrowhead=\"dotnormal\"]")
          .append("perform_xray -> apply_cast [arrowhead=\"dotnormal\"]")
          .append("apply_cast -> remove_cast [dir=\"both\", arrowhead=\"dotnormal\", arrowtail=\"dot\"]")
          .append("perform_xray -> perform_surgery [arrowhead=\"dotnormal\"]")
          .append("perform_surgery -> rehab [dir=\"both\", arrowhead=\"normal\", arrowtail=\"dot\"]")
          .append("}");*/
        double h = Screen.getPrimary().getVisualBounds().getHeight();
        double w = Screen.getPrimary().getVisualBounds().getWidth();
        //Scene scene = new Scene(new DeclareModelViewController(new Browser(h*0.9,w*0.75,sb.toString())), 500, 500);
        scene.getStylesheets().add("mainOld.css");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setMinHeight(h);
        primaryStage.setMinWidth(w);
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirm Exit");
        alert.setContentText("Do you want to exit Rum?");

        
        primaryStage.setOnCloseRequest(e -> {
        	Optional<ButtonType> result = alert.showAndWait();
        	if (result.get() == ButtonType.OK){
                // ... user chose OK
        		tmvc.shutdown();
            } else {
                // ... user chose CANCEL or closed the dialog
            	e.consume();
            }
        });
        //primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        //primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.show();
    }
}
