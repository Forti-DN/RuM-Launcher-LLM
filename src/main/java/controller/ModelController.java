package controller;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import controller.monitoring.MonitoringMethod;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;


public class ModelController extends GridPane {
	
	ArrayList<String> activities= new ArrayList<>();
	ArrayList<String> dataCode= new ArrayList<>();
	ArrayList<String> dataBindingsCode= new ArrayList<>();
	ArrayList<String> constraintsCode= new ArrayList<>();
	


	@FXML
	private ListView<String> activitiesView = new ListView<>();
	
	@FXML
	private ListView<String> dataCodeView = new ListView<>();
	
	@FXML
	private ListView<String> dataBindingsCodeView = new ListView<>();
	
	@FXML
	private ListView<String> constraintsCodeView = new ListView<>();
	
	private String modelText;
	
	public ModelController(String modelText, MonitoringMethod monitoringMethod) {
		this.modelText = modelText;
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ModelView.fxml"));
		if (!modelText.equals("")) {
			switch (monitoringMethod) {
			case MP_DECLARE_ALLOY:
				String[] splitModel = modelText.replace("\r\n", "\n").split("\n");
				inputSortDecl(splitModel);
				break;
			case MOBUCON_LTL: //Fallthrough intended
			case ONLINE_DECLARE:
				inputSortXml(modelText);
				break;
			case FLLOAT:
				inputSortTxt(modelText);
				break;
			case PROBDECLARE:
				inputSortTxt2(modelText);
				break;
			default:
				System.out.println("Unknown monitoring method");
				break;
			}
		}
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	
    public boolean isActivity(String line) {
        return line.startsWith("activity ");
    }
    public String getModelText() {
		return modelText;
	}


	public void setModelText(String modelText) {
		this.modelText = modelText;
	}


	public boolean isDataBinding(String line) {
        return line.startsWith("bind ");
    }
    public boolean isConstraint(String line) {
        return line.contains("[");
    }
    public boolean isData(String line) {
        return (StringUtils.countMatches(line, ':') % 2 == 1) && !isDataBinding(line);
    }
    private void inputSortDecl(String[] st) {
        for (String i : st) {

            if (i.isEmpty() || i.startsWith("/"))
                continue;

            if (isActivity(i))
            	activities.add(i);

            if (isData(i))
                dataCode.add(i);

            if (isDataBinding(i))
                dataBindingsCode.add(i);

            if (isConstraint(i))
                constraintsCode.add(i);
        }
    }
    
    private void inputSortXml(String modelString) {
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = null;
    	try {
    		//TODO: Maybe there is already a library for parsing activities, constraints, data and activity to data binds? 
    		builder = factory.newDocumentBuilder();
    		Document modelXml = builder.parse(new InputSource(new StringReader(modelString)));
    		XPathFactory xPathfactory = XPathFactory.newInstance();
    		XPath xpath = xPathfactory.newXPath();
    		
    		XPathExpression expr = xpath.compile("/model/assignment/activitydefinitions//activity");
    		NodeList nodes = (NodeList) expr.evaluate(modelXml, XPathConstants.NODESET);
    		for (int i = 0; i < nodes.getLength(); i++) {
    			activities.add(nodes.item(i).getAttributes().getNamedItem("name").getNodeValue());
    		}
    		
    		//TODO: Should instead combine text with relevant parameters
    		expr = xpath.compile("/model/assignment/constraintdefinitions//constraint/template/description");
    		nodes = (NodeList) expr.evaluate(modelXml, XPathConstants.NODESET);
    		for (int i = 0; i < nodes.getLength(); i++) {
    			constraintsCode.add(nodes.item(i).getTextContent());
    		}
    		
    		//TODO: data and activity to data binds
    		
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private void inputSortTxt(String modelString) {
    	modelString = modelString.replace("\n", "").replace("\r", "");
    	String[] splitModel = modelString.split("#");
    	for (String s : splitModel[0].split(",")) {
			activities.add(s);
		}
    	for (String s : splitModel[1].split(",")) {
    		constraintsCode.add(s);
		}
    	if (splitModel.length>3) {
    		constraintsCode.add("Query: " + splitModel[3]);
		}
    }
    
    private void inputSortTxt2(String modelString) {
    	String[] splitModel = modelString.split("\\r?\\n");
    	for (String s : splitModel) {
    		constraintsCode.add(s);
		}
    	constraintsCode.remove(constraintsCode.size()-1);
    }
    
    
    @FXML
	public void initialize() {
        activitiesView.setItems(FXCollections.observableList(activities));
        constraintsCodeView.setItems(FXCollections.observableList(constraintsCode));
        dataBindingsCodeView.setItems(FXCollections.observableList(dataBindingsCode));
        dataCodeView.setItems(FXCollections.observableList(dataCode));
    }
}
