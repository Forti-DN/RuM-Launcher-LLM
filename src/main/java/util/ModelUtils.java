package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.PrimitiveIterator.OfInt;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import controller.editor.AttributeType;
import treedata.TreeDataActivity;
import treedata.TreeDataAttribute;
import core.alloy.codegen.NameEncoder;
import core.alloy.codegen.NameEncoder.DataMappingElement;
import declare.DeclareParser;
import declare.DeclareParserException;

public final class ModelUtils {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	//Private constructor to avoid unnecessary instantiation of the class
	private ModelUtils() {
	}

	//Gets valid activities from the model file
	public static List<String> getActivityList(File declModel) {
		List<String> aL = new ArrayList<>();

		try {
			Scanner sc = new Scanner(declModel);
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				if(line.startsWith("activity") && line.length() > 9) {
					String activity = line.substring(9);
					aL.add(activity);
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			//TODO: Should throw the exception instead of returning an empty list
			logger.error("Can not load activities from model: {}", declModel.getAbsolutePath(), e);
		}
		return aL;
	}

	public static List<TreeDataAttribute> getTreeDataAttributes(File declModel) {
		List<TreeDataAttribute> treeDataAttributes = new ArrayList<>();
		Pattern p = Pattern.compile("(.+): (.+)");

		try {
			Scanner sc = new Scanner(declModel);
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				Matcher m = p.matcher(line);
				if(!line.startsWith("activity") && !line.startsWith("bind") && m.find()) {
					String data_name = m.group(1);
					String data_value = m.group(2);

					TreeDataAttribute treeDataAttribute = new TreeDataAttribute();
					treeDataAttribute.setAttributeName(data_name);

					if(data_value.startsWith("integer between ")) {
						treeDataAttribute.setAttributeType(AttributeType.INTEGER);
						int l = "integer between ".length();
						String[] values = data_value.substring(l).split(" and ");
						treeDataAttribute.setValueFrom(new BigDecimal(values[0]));
						treeDataAttribute.setValueTo(new BigDecimal(values[1]));
					} else if(data_value.startsWith("float between ")) {
						treeDataAttribute.setAttributeType(AttributeType.FLOAT);
						int l = "float between ".length();
						String[] values = data_value.substring(l).split(" and ");
						treeDataAttribute.setValueFrom(new BigDecimal(values[0]));
						treeDataAttribute.setValueTo(new BigDecimal(values[1]));
					} else {
						treeDataAttribute.setAttributeType(AttributeType.ENUMERATION);
						List<String> possibleValues = new ArrayList<>();
						for (String possibleValue : data_value.split(", ")) {
							possibleValues.add(possibleValue);
						}
						treeDataAttribute.setPossibleValues(possibleValues);
					}

					treeDataAttributes.add(treeDataAttribute);
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			//TODO: Should throw an exception instead of returning an empty list
			logger.error("Can not load attributes from model: {}", declModel.getAbsolutePath(), e);
		}
		return treeDataAttributes;
	}

	//TODO: This could be solved much more effectively, but no time to do it now (should really use maps instead of brute force loops and the entire file should probably be processed in a single function)
	public static void addAttributesToActivities(List<TreeDataActivity> treeDataActivities, List<TreeDataAttribute> treeDataAttributes, File declModel) {
		Pattern p = Pattern.compile("(.+): (.+)");
		try {
			Scanner sc = new Scanner(declModel);
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				if(line.startsWith("bind")) {
					String mapping = line.substring(5);
					Matcher m = p.matcher(mapping);
					if(m.find()) {
						String activity = m.group(1);
						String[] data_names = m.group(2).split(",");

						for (TreeDataActivity treeDataActivity : treeDataActivities) {
							if (treeDataActivity.getActivityName().equals(activity)) {
								for (String dataName : data_names) {
									for (TreeDataAttribute treeDataAttribute : treeDataAttributes) {
										if (treeDataAttribute.getAttributeName().equals(dataName.trim())) {
											treeDataActivity.addAttribute(treeDataAttribute);
										}
									}
								}
								break;
							}
						}
					}
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			//TODO: Should throw an exception instead of returning an empty list
			logger.error("Can not load activity-attribute bindings from model: {}", declModel.getAbsolutePath(), e);
		}
	}

	//Gets valid activities from the model string
	public static List<String> getActivityList(String declModelString) {
		String[] declModelLines = declModelString.split("[\\r\\n]+");
		List<String> aL = new ArrayList<>();

		for (String line : declModelLines) {
			if(line.startsWith("activity") && line.length() > 9) {
				String activity = line.substring(9);
				aL.add(activity);
			}
		}
		return aL;
	}

	//Gets valid constraint strings from the model file
	public static List<String> getConstraintsList(File declModel) {
		List<String> cL = new ArrayList<>();
		try {
			Scanner sc = new Scanner(declModel);
			Pattern p = Pattern.compile("\\w+(\\[.*\\]) \\|");
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				if(!line.startsWith("activity") && !line.startsWith("bind")) {
					Matcher m = p.matcher(line);
					if(m.find()) {
						cL.add(line);
					}
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			//TODO: Should throw an exception instead of returning an empty list
			logger.error("Can not load constraints from model: {}", declModel.getAbsolutePath(), e);
		}
		return cL;
	}

	//Formating the constraints for displaying (log generation and monitoring)
	public static List<String> getFormattedListOfConstraints(List<String> constraintList) {
		List<String> fcL = new ArrayList<>();
		for (String string : constraintList) {
			fcL.add(string.replace(" [", "[").replace("] ", "]").replace("[", "\n[").replace("]", "]\n"));
		}
		return fcL;
	}

	//Gets valid constraints from the model string
	public static List<String> getConstraintsList(String declModelString) {
		String[] declModelLines = declModelString.split("[\\r\\n]+");
		List<String> cL = new ArrayList<String>();
		Pattern p = Pattern.compile("\\w+(\\[.*\\]) \\|");

		for (String line : declModelLines) {
			if(!line.startsWith("activity") && !line.startsWith("bind")) {
				Matcher m = p.matcher(line);
				if(m.find()) {
					cL.add(line);
				}
			}
		}
		return cL;
	}

	//Checks if a constraint contains data conditions
	public static boolean containsData(String constraint) {
		String[] arr = constraint.split("\\|");
		arr[0] = "";
		for(String str:arr) {
			if(!str.trim().isEmpty()) return true;
		}
		return false;
	}

	// Checks if a constraint contains a time condition
	public static boolean containsTimeCondition(String constraint) {
		int lp = constraint.lastIndexOf('|');
		if (lp == -1)
			return false; // Invalid constraint format
		
		String cond = constraint.substring(lp+1);
		return !cond.isEmpty();
	}

	public static String createTmpXmlString(File declModel) throws DeclareParserException, IOException {
		
		// Temporarily removing time conditions because NameEncoder cannot handle them
		StringBuilder sb = new StringBuilder();
		Queue<String> removedTimeConds = new LinkedList<>();
		for (String line : Files.readAllLines(declModel.toPath())) {
			if (DeclareParser.isDataConstraint(line)) {
				String lastCond = line.substring(line.lastIndexOf('|')+1);
				// Check if the last condition is an (empty) time condition
				if (lastCond.trim().isEmpty() || lastCond.trim().matches("\\d+,\\d+,[smhd]")) {
					line = line.substring(0, line.lastIndexOf('|')).trim();
					removedTimeConds.add(lastCond);
				}
			}

			sb.append(line + "\n");
		}

		String modelStrWithoutTimeConds = sb.toString();

		
		// Encoding
		NameEncoder encoder = new NameEncoder();
		encoder.createDeclMapping(modelStrWithoutTimeConds);
		String encodedDeclare = encoder.encodeDeclModel(modelStrWithoutTimeConds);
		
		
		// Building XML model
		List<String> activityList = new ArrayList<>();
		List<String> constraintList = new ArrayList<>();

		for (String encLine : encodedDeclare.split("\n")) {
			if (DeclareParser.isActivity(encLine))
				activityList.add( encLine.substring(9) );
			
			else if (DeclareParser.isDataConstraint(encLine))
				// Restoring the removed time conditions too
				constraintList.add( encLine.trim() + " |" + removedTimeConds.poll() );

			else if (DeclareParser.isConstraint(encLine))
				constraintList.add( encLine.trim() );
		}
		
		String tmpXML = buildXmlModelString(activityList, constraintList);
		
		
		// Decoding
		for (Map.Entry<String,String> entry : encoder.getActivityMapping().entrySet())
			tmpXML = tmpXML.replace(entry.getKey(), entry.getValue());
        
        for (DataMappingElement d : encoder.getDataMapping()) {
        	tmpXML = tmpXML.replace(d.getEncodedName(), d.getOriginalName());
        	
        	for (Map.Entry<String,String> entry : d.getValuesMapping().entrySet())
        		tmpXML = tmpXML.replace(entry.getKey(), entry.getValue());
        }
		
		return tmpXML;
	}

	//Creates a temporary file that contains the model in an older xml format
	//isSingleQuote removed, temporary file handling changed
	//Old method name: convertToXml
	public static File createTmpXmlModel(File declModel) throws IOException, IllegalArgumentException {
		
		File tmpXmlModel = null;
		try {
			String xmlModelString = createTmpXmlString(declModel);
			tmpXmlModel = File.createTempFile("RuM_conformance_model-", ".xml");
			tmpXmlModel.deleteOnExit();
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpXmlModel))) {
				bw.write(xmlModelString);
			}
		} catch (IOException | DeclareParserException e) {
			logger.error("Can not create tmpXmlModel", e);
		}
		return tmpXmlModel;
	}

	//Needed for createTmpXmlModel
	//TODO: Should be done with an xml library, instead of building as a string
	//TODO: Should write the file on the fly
	public static String buildXmlModelString(List<String> activityList, List<String> constraintList) throws IllegalArgumentException{
		String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"+
				"<model><assignment language=\"ConDec\" name=\"new model\">"+
				"<activitydefinitions>";
		StringBuilder sb = new StringBuilder(header);
		for(int i=0; i<activityList.size(); i++) {
			sb.append("<activity id=\""+(i+1)+"\" name=\""+activityList.get(i)+"\"/>");
		}
		sb.append("</activitydefinitions>");
		sb.append("<constraintdefinitions>");
		for(int i=0; i<constraintList.size(); i++) {
			sb.append("<constraint id=\""+(11+i)+"\" mandatory=\"true\">");
			String constraint = constraintList.get(i);
			String cname = constraint.substring(0,constraint.indexOf('[')).toLowerCase();
			
			sb.append("<condition>");
			String condition = getCondition(constraint).replace("&&","&amp;&amp;").replace("<", "&lt;");
			//System.out.println(condition);
			sb.append(condition);
			sb.append("</condition>");

			sb.append("<name>"+cname+"</name>");
			sb.append("<template>");
			sb.append("<description>"+cname+"</description>");
			sb.append("<display>"+cname+"</display>");
			sb.append("<name>"+cname+"</name>");
			sb.append("<text>"+cname+"</text>");
			sb.append("<parameters>");
			Matcher mBinary = Pattern.compile(".*\\[(.*), (.*)\\] \\|.* \\|.* \\|.*").matcher(constraint);
			Matcher mUnary = Pattern.compile(".*\\[(.*)\\] \\|.* \\|.*").matcher(constraint);
			if(mBinary.find()) {

				String first = mBinary.group(1);
				String second = mBinary.group(2);

				sb.append("<parameter branchable=\"true\" id=\"1\" name=\"A\">");
				sb.append("<graphical>");
				sb.append("<style number=\"1\"/>");
				sb.append("<begin fill=\"true\" style=\"5\"/>");
				sb.append("<middle fill=\"false\" style=\"0\"/>");
				sb.append("<end fill=\"false\" style=\"0\"/>");
				sb.append("</graphical>");
				sb.append("</parameter>");

				sb.append("<parameter branchable=\"true\" id=\"2\" name=\"B\">");
				sb.append("<graphical>");
				sb.append("<style number=\"1\"/>");
				sb.append("<begin fill=\"true\" style=\"5\"/>");
				sb.append("<middle fill=\"false\" style=\"0\"/>");
				sb.append("<end fill=\"false\" style=\"0\"/>");
				sb.append("</graphical>");
				sb.append("</parameter>");
				sb.append("</parameters></template>");
				sb.append("<constraintparameters>");

				sb.append("<parameter templateparameter=\"1\">");
				sb.append("<branches>");
				sb.append("<branch name=\""+first+"\"/>");
				sb.append("</branches></parameter>");

				sb.append("<parameter templateparameter=\"2\">");
				sb.append("<branches>");
				sb.append("<branch name=\""+second+"\"/>");
				sb.append("</branches></parameter>");

				sb.append("</constraintparameters></constraint>");
			}
			else if(mUnary.find()) {

				String first = mUnary.group(1);

				sb.append("<parameter branchable=\"false\" id=\"1\" name=\"A\">");
				sb.append("<graphical>");
				sb.append("<style number=\"1\"/>");
				sb.append("<begin fill=\"true\" style=\"0\"/>");
				sb.append("<middle fill=\"false\" style=\"0\"/>");
				sb.append("<end fill=\"false\" style=\"0\"/>");
				sb.append("</graphical>");
				sb.append("</parameter>");
				sb.append("</parameters></template>");
				sb.append("<constraintparameters>");

				sb.append("<parameter templateparameter=\"1\">");
				sb.append("<branches>");
				sb.append("<branch name=\""+first+"\"/>");
				sb.append("</branches></parameter>");

				sb.append("</constraintparameters></constraint>");
			}
		}

		sb.append("</constraintdefinitions></assignment></model>");
		return sb.toString();
	}

	// Needed for createTmpXmlModel
	private static String getCondition(String constraint) {
		Matcher mBinary = Pattern.compile("(.*)\\[.*\\] \\|(.*) \\|(.*) \\|(.*)").matcher(constraint);
		Matcher mUnary = Pattern.compile("(.*)\\[.*\\] \\|(.*) \\|(.*)").matcher(constraint);
		
		if (mBinary.find() && !TemplateUtils.isUnaryTemplate(mBinary.group(1)))
			return "["+prepareCondition(mBinary.group(2))+"]"+"["+prepareCondition(mBinary.group(3))+"]"+"["+mBinary.group(4)+"]";
		
		else if (mUnary.find() && TemplateUtils.isUnaryTemplate(mUnary.group(1)))
			return "["+prepareCondition(mUnary.group(2))+"]"+"[]"+"["+mUnary.group(3)+"]";
		
		else {
			StringBuilder error = new StringBuilder();
			error.append("\"" + constraint + "\" is invalid.\n");
			
			if (TemplateUtils.isUnaryTemplate(constraint))
				error.append("Unary constraint format: template[activity] |activation_condition |time_condition");
			else
				error.append("Binary constraint format: template[activity1, activity2] |activation_condition |correlation_condition |time_condition");
				
			throw new IllegalArgumentException(error.toString());
		}
	}

	//Needed for createTmpXmlModel
	//isSingleQuote = true was used for Declare Analyzer
	private static String prepareCondition(String condition) {
		condition = condition.trim();
		condition = condition.replaceAll("(?i)\\s+and\\s+", " && ").replaceAll("(?i)\\s+or\\s+", " || ");
		
		String outputCondition = "";
		
		StringBuffer newCond = new StringBuffer();
		
		OfInt it = condition.chars().iterator();
		while (it.hasNext()) {
			newCond.append( (char) it.nextInt() );
			

			if (newCond.charAt(0) == '(') {
				if (newCond.charAt(newCond.length()-1) == ')') {
					outputCondition += "(" + prepareCondition( newCond.substring(1, newCond.length()-1) ) + ")";
					newCond.delete(0, newCond.length());
				}
				
			} else if (newCond.toString().endsWith(" && ")) {
				if (newCond.toString().equals(" && "))
					outputCondition += " && ";
				else
					outputCondition += processPredicate(newCond.substring(0, newCond.length()-4)) + " && ";
				
				newCond.delete(0, newCond.length());
			
			} else if (newCond.toString().endsWith(" || ")) {
				if (newCond.toString().equals(" || "))
					outputCondition += " || ";
				else
					outputCondition += processPredicate(newCond.substring(0, newCond.length()-4)) + " || ";
				
				newCond.delete(0, newCond.length());
			}
		}
		
		if (newCond.length() > 0) {
			outputCondition += processPredicate(newCond.toString());
		}
		
		return outputCondition;
	}
	
	private static String processPredicate(String p) {
		p = p.trim();
		
		if (p.contains(" = "))		p = p.replace("=", "==");
		if (p.contains(" is not "))	p = p.replace("is not", "!=");
		if (p.contains(" is "))		p = p.replace("is", "==");
		
		Matcher mSame = Pattern.compile("same (.*)").matcher(p);
		if (mSame.find())
			p = "A."+mSame.group(1)+" == "+"T."+mSame.group(1);
		
		Matcher mDiff = Pattern.compile("different (.*)").matcher(p);
		if (mDiff.find())
			p = "A."+mDiff.group(1)+" != "+"T."+mDiff.group(1);
		
		Matcher mNotIn = Pattern.compile("(\\w\\.\\w+) not in \\((.*)\\)").matcher(p);
		if (mNotIn.find()) {
			String lhs = mNotIn.group(1);
			String[] rha = mNotIn.group(2).split(",");
			String ss2 = "";
			
			for (String r: rha) {
				ss2 += "("+lhs + " != \"" + r.trim() + "\") && ";
				//TODO: Create a method that can replace double quote with single quote - needed for Declare Analyzer
				/* if (isSingleQuote) {
					ss2 += "("+lhs + " != '" + r.trim() + "') && ";
				} else {
					ss2 += "("+lhs + " != \"" + r.trim() + "\") && ";
				} */
			}
			
			p = ss2.substring(0, ss2.length()-4);
			return p;
		}
		
		Matcher mIn = Pattern.compile("(\\w\\.\\w+) in \\((.*)\\)").matcher(p);
		if (mIn.find()) {
			String lhs = mIn.group(1);
			String[] rha = mIn.group(2).split(",");
			String ss2 = "";
			
			for (String r: rha) {
				ss2 += "("+lhs + " == \"" + r.trim() + "\") || ";
				/* if (isSingleQuote) {
					ss2 += "("+lhs + " == '" + r.trim() + "') || ";
				} else {
					ss2 += "("+lhs + " == \"" + r.trim() + "\") || ";
				} */
			}
			
			p = ss2.substring(0, ss2.length()-4);
			return p;
		}
		
		Matcher m = Pattern.compile("(.*) ?(<=|>=|<|>|==|!=) ?(.*)").matcher(p);
		if (m.find()) {
			String data = m.group(3);
			
			if (data.matches("^\\d+.\\d+$|^\\d+$")) {
				String mg1 = m.group(1).trim();
				String mg2 = m.group(2).trim();
				String mg3 = m.group(3).trim();
				return mg1+" "+mg2+" "+""+mg3+"";
			
			} else if (data.matches("^\\D\\..*$")) {
				String mg1 = m.group(1).trim();
				String mg2 = m.group(2).trim();
				String mg3 = m.group(3).trim();
				return mg1+" "+mg2+" "+""+mg3+"";
			} /* else if (isSingleQuote) {
				String mg1 = m.group(1).trim();
				String mg2 = m.group(2).trim();
				String mg3 = m.group(3).trim();
				return mg1+" "+mg2+" "+"'"+mg3+"'";
			} */
			else {
				String mg1 = m.group(1).trim();
				String mg2 = m.group(2).trim();
				String mg3 = m.group(3).trim();
				return mg1+" "+mg2+" "+"\""+mg3+"\"";
			}
		}
		
		return p;
	}

	//Creates a new xml model file where condition double quotes are replaced with single quotes
	public static File xmlModelToSingleQuote(File xmlModel) throws IOException, SAXException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		File xmlModelSq = File.createTempFile("RuM_conformance_model_sq-", ".xml");
		xmlModelSq.deleteOnExit();

		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlModel);
		document.getDocumentElement().normalize();
		NodeList conditionNodes = document.getElementsByTagName("condition");

		for (int i = 0; i < conditionNodes.getLength(); i++) {
			Node conditionNode = conditionNodes.item(i);
			if (conditionNode.getNodeType() == Node.ELEMENT_NODE) {
				Element conditionElement = (Element) conditionNode;
				conditionElement.setTextContent(conditionElement.getTextContent().replace("\"", "'"));
			}
		}

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		DOMSource domSource = new DOMSource(document);
		StreamResult streamResult = new StreamResult(xmlModelSq);

		transformer.transform(domSource, streamResult);

		return xmlModelSq;
	}

	//Gets attributes from the model file
	public static TreeSet<String> getAttributes(File declModel) {
		//TODO: Should review the code to see if it can be simplified
		TreeSet<String> attributes = new TreeSet<>();
		try {
			Scanner sc = new Scanner(declModel);
			List<String> activations = new ArrayList<>();
			List<String> correlations = new ArrayList<>();
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
			sc.close();
			activations.addAll(correlations);
			attributes.addAll(activations);
		} catch (FileNotFoundException e) {
			//TODO: Should throw an exception instead of returning an empty list
			logger.error("Can not load attributes from model: {}", declModel.getAbsolutePath(), e);
		}
		return attributes;
	}
}
