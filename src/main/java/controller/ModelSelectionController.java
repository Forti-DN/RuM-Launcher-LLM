package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ModelSelectionController extends Pane {
	
	@FXML
	private ListView<String> modelList;
	
	@FXML
	private TextArea modelDescription;
	
	@FXML
	private TextField activityA;
	
	@FXML
	private TextField activityB;
	
	private List<String> templates;
	
	private Stage stage;
	
	public ModelSelectionController(List<String> templates, Stage stage) {
		this.stage = stage;
		this.templates = templates;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ModelSelection.fxml"));
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
		//MyController2 mc = (MyController2) this.stage.getUserData();
		this.stage.setTitle("New Declare Template");
		this.stage.setScene(new Scene(new ConstructionController(this.stage,this.templates),this.getScene().getWidth(),this.getScene().getHeight()));
		this.stage.setMaximized(true);
		this.stage.show();
	}
	
	@FXML
	public void addItem() {
		String selected = modelList.getSelectionModel().getSelectedItem();
		String activityA = "";
		String activityB = "";
		String template = "";
		activityA = this.activityA.getText();
		if(isBinaryTemplate(selected)) {
			activityB = this.activityB.getText();
			template = getWithoutParanthesis(selected)+"("+activityA+", "+activityB+")";
		}
		else {
			template = getWithoutParanthesis(selected)+"("+activityA+")";
		}
		templates.add(template);
		this.stage.setTitle("New Declare Template");
		this.stage.setScene(new Scene(new ConstructionController(this.stage,this.templates),this.getScene().getWidth(),this.getScene().getHeight()));
		this.stage.setMaximized(true);
		this.stage.show();
	}
	
	@FXML
	public void initialize() {
		modelDescription.setWrapText(true);
		modelDescription.setEditable(false);
		List<String> templates = new ArrayList<String>();
		for(DeclareTemplate dt : DeclareTemplate.values()) {
			if(dt != DeclareTemplate.Choice && dt != DeclareTemplate.Exclusive_Choice) {
				templates.add(getModelName(dt.name()).replace('_', ' '));
			}
		}
		ObservableList<String> items =FXCollections.observableArrayList (
			    (String[]) templates.toArray(new String[templates.size()]));
		
		modelList.setItems(items);
		
		modelList.getSelectionModel().selectedItemProperty().addListener((ov,oldV,newV) -> {
			modelDescription.setText(getModelDescription(newV));
			if(!isBinaryTemplate(newV)) {
				activityB.setDisable(true);
			}
			else {
				activityB.setDisable(false);
			}
		});
	}
	
	private String getModelDescription(String template) {
		int left = template.indexOf('(');
		String dt = template.substring(0, left).replace(' ', '_');
		DeclareTemplate d = DeclareTemplate.valueOf(dt);
		switch(d) {
			case Absence:
				return "A does not occur\n\nPositive examples: BC, BBCCDD\n\nNegative examples: A, BCDAA";
			case Absence2:
				return "A occurs at most one time\n\nPositive examples: BA, BBCCDA\n\nNegative examples: AA, BCDAAA";
			case Absence3:
				return "A occurs at most two times\n\nPositive examples: BA, BBCCDAA\n\nNegative examples: AAA, ABCDAAA";
			case Exactly1:
				return "A occurs exactly once\n\nPositive examples: BA, BBCCDA\n\nNegative examples: AAA, ABCDA, EDBC";
			case Exactly2:
				return "A occurs exactly twice\n\nPositive examples: BAA, BBCCDAA\n\nNegative examples: AAA, ABCD, EDBC";
			case Existence:
				return "A occurs at least once\n\nPositive examples: BAA, BBCCDA\n\nNegative examples: BCC, BCD, EDBC";
			case Existence2:
				return "A occurs at least twice\n\nPositive examples: BAA, BBCCDAAA\n\nNegative examples: BCCA, BCD, EDBC";
			case Existence3:
				return "A occurs at least three times\n\nPositive examples: BAAA, BABCCDAAA\n\nNegative examples: BCCA, BAACD, EDBC";
			case Init:
				return "A occurs first\n\nPositive examples: AA, ABCCDAAA\n\nNegative examples: BCCA, BAACD, EDBC";
			case Responded_Existence:
				return "If A occurs then B occurs as well\n\nPositive examples: AB, ABCCDAAA\n\nNegative examples: CCA, AACD, EDAAAC";
			case Response:
				return "If A occurs then B occurs after A\n\nPositive examples: ABCD ,AAAAB, BCCD\n\nNegative examples: CCA, AACD, EDAAAC";
			case Alternate_Response:
				return "Each time A occurs, then B occurs afterwards before A recurs\n\nPositive examples: ABCADB ,ACDEB\n\nNegative examples: CCAA, AABCD, EDBAAAC";
			case Chain_Response:
				return "Each time A occurs, then B occurs immediately afterwards\n\nPositive examples: ABCAB ,CDEAB\n\nNegative examples: CCAACB, EDBAAAC";
			case Precedence:
				return "B occurs if preceded by A\n\nPositive examples: ABCD ,AAAAB, AACCD\n\nNegative examples: CCBA, BBCD, EDBAC";
			case Alternate_Precedence:
				return "Each time B occurs, it is preceded by A and no other B can recur in between\n\nPositive examples: ABCD ,ABACAAB, AACCD\n\nNegative examples: CACBBA, ABBABCB";
			case Chain_Precedence:
				return "Each time B occurs, then A occurs immediately beforehand\n\nPositive examples: ABCABAA ,CDEABAB\n\nNegative examples: CCAACBB, EDBAAAC";
			case CoExistence:
				return "A and B occur together\n\nPositive examples: AB, ABCCDAAA, CDE\n\nNegative examples: CCA, BBCD, EDAAAC";
			case Succession:
				return "A occurs if and only if it is followed by B\n\nPositive examples: AB, ABCCDBB\n\nNegative examples: BCCA, BBCD, EDBAC";
			case Alternate_Succession:
				return "A and B occur together if and only if the latter follows the former, and they alternate each other\n\nPositive examples: ACDBACB, ABCCABD\n\nNegative examples: AABCCA, BBCDAA, EDBAC";
			case Chain_Succession:
				return "A and B occur together if and only if the latter immediately follows the former\n\nPositive examples: ABABCC, CCD\n\nNegative examples: AABCCA, BBCDAA, EDBAC";
			case Not_Chain_Succession:
				return "A and B occur together if and only if the latter does not immediately follow the former\n\nPositive examples: ABBAABCC, BBCCD\n\nNegative examples: ABCC, ABABCD";
			case Not_Succession:
				return "A can never occur before B\n\nPositive examples: BBAACC, BBCCBAD\n\nNegative examples: AABBCC, ABBCD";
			case Not_CoExistence:
				return "A and B never occur together\n\nPositive examples: AACC, BBCCBD, CDE\n\nNegative examples: AABCC, ABBBCD";
			case Not_Chain_Precedence:
				return "Each time B occurs, then A does not occur immediately beforehand\n\nPositive examples: BABBCD, ACDE \n\nNegative examples: ABCABAA ,CDEABAB";
			case Not_Chain_Response:
				return "Each time A occurs, then B does not occur immediately afterwards\n\nPositive examples: AABCAA, BCDE\n\nNegative examples: ABCAB ,CDEAB";
			case Not_Precedence:
				return "B occurs if it is not preceded by A\n\nPositive examples: CCBA, BBCD, EDBAC\n\nNegative examples: ABCD, AAAABDE";
			case Not_Response:
				return "If A occurs then B does not occur after A\n\nPositive examples: CCA, AACD, EDAAAC\n\nNegative examples: AABCD, ABBED";
			case Not_Responded_Existence:
				return "If A occurs then B does not occur\n\nPositive examples: CCA, AACD, EDAAAC, BCDE\n\nNegative examples: ADCDB, ABAEB";
			default:
				return "";
		}
	}
	
	private String getModelName(String template) {
		DeclareTemplate d = DeclareTemplate.valueOf(template);
		switch(d) {
			case Absence:
				return template+"(A)";
			case Absence2:
				return template+"(A)";
			case Absence3:
				return template+"(A)";
			case Exactly1:
				return template+"(A)";
			case Exactly2:
				return template+"(A)";
			case Existence:
				return template+"(A)";
			case Existence2:
				return template+"(A)";
			case Existence3:
				return template+"(A)";
			case Init:
				return template+"(A)";
			default:
				return template+"(A, B)";
		}
	}
	
	private boolean isBinaryTemplate(String template) {
		int left = template.indexOf('(');
		String dt = template.substring(0, left).replace(' ', '_');
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
	
	private String getWithoutParanthesis(String selected) {
		int index = selected.indexOf('(');
		return selected.substring(0, index);
	}
	
}
