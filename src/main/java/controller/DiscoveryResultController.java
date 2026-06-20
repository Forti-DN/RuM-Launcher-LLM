package controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import view.Browser;

public class DiscoveryResultController extends GridPane {
	
	@FXML
	private Pane outputPane;
	
	@FXML
	private Pane filterPane;
	
	private Browser browser;
	
	private FilterController fc;
	
	private FilterMinerFulController fmc;

	private FilterMPDeclareController fmpc;
	
	private String view;
	
	public DiscoveryResultController() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/DiscoveryResultView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public void setBrowser(Browser browser) {
		this.browser = browser;
		outputPane.getChildren().clear();
		outputPane.getChildren().add(browser);
	}
	
	public void setFc(FilterController fc) {
		this.fc = fc;
		filterPane.getChildren().clear();
		//fc.setViewChoice(view);
		filterPane.getChildren().add(fc);
	}
	
	public void setFmc(FilterMinerFulController fmc) {
		this.fmc = fmc;
		filterPane.getChildren().clear();
		//fmc.setViewChoice(view);
		filterPane.getChildren().add(fmc);
	}

	public void setFmpc(FilterMPDeclareController fmpc) {
		this.fmpc = fmpc;
		filterPane.getChildren().clear();
		//fmpc.setViewChoice(view);
		filterPane.getChildren().add(fmpc);
	}
	
	public Pane getFilterPane() {
		return filterPane;
	}
	
	public Pane getOutputPane() {
		return outputPane;
	}
	
	public Browser getBrowser() {
		Browser b = (Browser)this.outputPane.getChildren().get(0);
		return b;
	}
	
	public void setView(String view) {
		this.view = view;
	}
	public String getView() {
		return view;
	}
	
	public FilterController getFc() {
		return fc;
	}
	public FilterMinerFulController getFmc() {
		return fmc;
	}
	public FilterMPDeclareController getFmpc() {
		return fmpc;
	}

}
