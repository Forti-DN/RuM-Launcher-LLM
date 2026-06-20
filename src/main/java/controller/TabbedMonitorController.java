package controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import theFirst.MoBuConClient;

public class TabbedMonitorController extends TabPane {
	@FXML
	private AnchorPane modelView;
	@FXML
	private AnchorPane monitorView;
	@FXML
	private TabPane tabPane;
	@FXML
	private Tab modelTab;
	@FXML
	private Tab monitorTab;
	
	private ModelController mc;
	
	private MoBuConClient monc;
		
	public TabbedMonitorController(ModelController mc, MoBuConClient mob, boolean ifModelTab) {
		this.mc = mc;
		this.monc = mob;
		
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/TabbedMonitorView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
            if (!ifModelTab) {
            	tabPane.getSelectionModel().select(monitorTab);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	@FXML
	public void initialize() {
		modelView.getChildren().clear();
		modelView.getChildren().add(mc);
		
		monitorView.getChildren().clear();
		monitorView.getChildren().add(monc);
		
	}
	public TabPane getTabPane() {
		return tabPane;
	}
	public void setTabPane(TabPane tabPane) {
		this.tabPane = tabPane;
	}
	public Tab getMonitorTab() {
		return monitorTab;
	}
	public void setMonitorTab(Tab monitorTab) {
		this.monitorTab = monitorTab;
	}
}
