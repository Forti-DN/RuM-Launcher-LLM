package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.framework.ExecutionStep;
import org.processmining.plugins.DataConformance.visualization.DataAwareStepTypes;
import org.processmining.plugins.DeclareConformance.Alignstep;
import org.processmining.plugins.DeclareConformance.ViolationIdentifier;
import org.processmining.plugins.dataawaredeclarereplayer.gui.AnalysisSingleResult;
import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;
import org.processmining.plugins.declareminer.visualizing.DeclareMap;

import controller.TraceViewController;
import controller.discovery.ShowLabelEnum;
import controller.discovery.data.DiscoveredActivity;
import controller.discovery.data.DiscoveredConstraint;
import controller.editor.data.ConstraintDataRow;
import datatable.AbstractDataRow.RowStatus;
import graph.ArrowProperty;
import graph.Arrows;
import graph.Font;
import graph.Smooth;
import graph.VisEdge;
import graph.VisGraph;
import graph.VisNode;
import javafx.collections.ObservableList;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeItem;
import javafx.stage.Screen;
import minerful.concept.ProcessModel;
import minerful.io.ConstraintsPrinter;
import task.discovery.DiscoveryTaskResult;
import task.discovery.data.Predicate;
import task.discovery.mp_enhancer.Rule;
import treedata.TreeDataActivity;
import treedata.TreeDataAttribute;
import treedata.TreeDataBase;
import view.Browser;

public class GraphGenerator {

	public static Browser browserify(List<XAttributeMap> activityListInTrace, Set<Integer> fulfillments, Set<Integer> violations) {
		VisGraph graph = new VisGraph();
		List<VisNode> nodes = new ArrayList<VisNode>();
		List<VisEdge> edges = new ArrayList<VisEdge>();
		int id = 0;
		for(XAttributeMap xmap : activityListInTrace) {
			VisNode node = new VisNode(id,"("+(id+1)+")  "+xmap.get("concept:name").toString(),null);
			StringBuilder sb = new StringBuilder();
			sb.append("Event attributes<br>");
			xmap.forEach((k,v) -> {
				sb.append(k+" = "+v.toString()+"<br>");
			});
			if(fulfillments.contains(id)) {
				node.setColor("#00ff00");
				node.setTitle("This activity is a fulfillment"+"<br>"+sb.toString());
			}
			else if(violations.contains(id)) {
				node.setColor("red");
				node.setTitle("This activity is a violation"+"<br>"+sb.toString());
			}
			else {
				node.setTitle(sb.toString());
			}
			nodes.add(node);
			id++;
		}
		for(int i = 0; i<nodes.size()-1; i++) {
			VisEdge edge = new VisEdge(nodes.get(i),nodes.get(i+1),null,null,null);
			edges.add(edge);
		}
		VisNode[] nodeArr = nodes.toArray(new VisNode[nodes.size()]);
		VisEdge[] edgeArr = edges.toArray(new VisEdge[edges.size()]);
		graph.addEdges(edgeArr);
		graph.addNodes(nodeArr);
		return new Browser(graph,Screen.getPrimary().getVisualBounds().getHeight()*0.9,Screen.getPrimary().getVisualBounds().getWidth()*0.6,"baseGraph3.html");
	}

	public static TraceViewController getTraceView(List<XAttributeMap> activityListInTrace, Set<Integer> fulfillments, Set<Integer> violations) {
		List<TraceElement> list = new ArrayList<TraceElement>();
		int id = 0;
		for(XAttributeMap xmap : activityListInTrace) {
			TraceElement element = new TraceElement();
			element.setText(xmap.get("concept:name").toString());
			List<String> attributes = new ArrayList<String>();
			xmap.forEach((k,v) -> {
				attributes.add(k+" = "+v.toString()+"\n");
			});
			if(fulfillments.contains(id)) {
				attributes.add(0,"This activity is a fulfillment\n");
				attributes.add(1,"\nEvent Attributes\n");
				element.setColor("#00ff00");
				element.setAttributes(attributes);
				list.add(element);
			}
			else if(violations.contains(id)) {
				attributes.add(0,"This activity is a violation\n");
				attributes.add(1,"\nEvent Attributes\n");
				element.setColor("red");
				element.setAttributes(attributes);
				list.add(element);
			}
			else {
				attributes.add(0,"Event Attributes\n");
				element.setColor("white");
				element.setAttributes(attributes);
				list.add(element);
			}
			id++;
		}
		return new TraceViewController(list,Screen.getPrimary().getVisualBounds().getHeight()*0.9,Screen.getPrimary().getVisualBounds().getWidth()*0.56);
	}

	public static Browser browserify(AnalysisSingleResult asr) {
		VisGraph graph = new VisGraph();
		List<VisNode> nodes = new ArrayList<VisNode>();
		List<VisEdge> edges = new ArrayList<VisEdge>();
		Set<Integer> s1 = asr.getMovesInBoth();
		Set<Integer> s2 = asr.getMovesInBothDiffData();
		Set<Integer> s3 = asr.getMovesInLog();
		Set<Integer> s4 = asr.getMovesInModel();
		List<ExecutionStep> l = asr.getAlignment().getLogTrace();
		List<ExecutionStep> l2 = asr.getAlignment().getProcessTrace();
		for(int i=0; i<l.size(); i++) {
			ExecutionStep es = l.get(i);
			ExecutionStep es2 = l2.get(i);
			String name = (es.getActivity() != null) ? es.getActivity() : es2.getActivity();
			String color = "";
			String title = null;
			if(s1.contains(i)) {
				color = "#00d200";
				title = "This is a move in log and model";
			}
			if(s2.contains(i)) {
				color = "#ffffff";
				title = "This is a move in log and model with different data";
			}
			if(s3.contains(i)) {
				color = "#ffff00";
				title = "This is a move in log";
			}
			if(s4.contains(i)) {
				color = "#e0b0ff";
				title = "This is a move in model";
			}
			if(!es.isEmpty()) {
				title += "<p><b>Trace Attributes</b><br>";
				for(String s: es.keySet()) {
					String logValue = es.get(s).toString();
					String processValue = es2.get(s).toString();
					if(processValue.equals(logValue)) {
						title += s + " = " + logValue + "<br>";
					}
					else {
						title += s + " = " + processValue + " &#8800; " + logValue + "<br>";
					}
				}
				title += "</p>";
			}
			VisNode node = new VisNode(i,name,title);
			node.setColor(color);
			nodes.add(node);
		}
		for(int i=0; i<nodes.size()-1; i++) {
			VisEdge edge = new VisEdge(nodes.get(i),nodes.get(i+1),null,null,null);
			edges.add(edge);
		}
		VisNode[] nodeArr = nodes.toArray(new VisNode[nodes.size()]);
		VisEdge[] edgeArr = edges.toArray(new VisEdge[edges.size()]);
		graph.addEdges(edgeArr);
		graph.addNodes(nodeArr);
		return new Browser(graph,Screen.getPrimary().getVisualBounds().getHeight()*0.6,Screen.getPrimary().getVisualBounds().getWidth()*0.73,"baseGraph3.html");
	}

	public static TraceViewController getTraceView(AnalysisSingleResult asr, XLog traces) {
		List<TraceElement> list = new ArrayList<TraceElement>();
		Set<Integer> s1 = asr.getMovesInBoth();
		Set<Integer> s2 = asr.getMovesInBothDiffData();
		Set<Integer> s3 = asr.getMovesInLog();
		Set<Integer> s4 = asr.getMovesInModel();
		List<ExecutionStep> l = asr.getAlignment().getLogTrace();
		List<ExecutionStep> l2 = asr.getAlignment().getProcessTrace();
		for(int i=0; i<l.size(); i++) {
			ExecutionStep es = l.get(i);
			ExecutionStep es2 = l2.get(i);
			String name = (es.getActivity() != null) ? es.getActivity() : es2.getActivity();
			TraceElement te = new TraceElement();
			List<String> attributes = new ArrayList<String>();
			te.setText(name);
			if(s1.contains(i)) {
				te.setColor("#00ff00");
				attributes.add("This is a move in log and model");
			}
			if(s2.contains(i)) {
				te.setColor("#ffffff");
				attributes.add("This is a move in log and model with different data");
			}
			if(s3.contains(i)) {
				te.setColor("#ffff00");
				attributes.add("This is a move in log");
			}
			if(s4.contains(i)) {
				te.setColor("#a020f0");
				attributes.add("This is a move in model");
			}
			if(!es.isEmpty() && !es2.isEmpty()) {
				for(String s: es.keySet()) {
					String logValue = es.get(s).toString();
					String processValue = es2.get(s).toString();
					if(processValue.equals(logValue)) {
						attributes.add(s + " = " + logValue + "\n");
					}
					else {
						attributes.add(s + " = " + logValue + " replaced by " + processValue + "\n");
					}
				}
			}
			Optional<List<String>> opt = Optional.empty();
			if(es.getActivity() != null) {
				opt = EventFinder.getEventAttributes(traces, asr.getAlignment().getTraceName(), i);
			}
			if(opt.isPresent()) {
				List<String> ea = opt.get();
				if(!ea.isEmpty()) attributes.add("\nEvent Attributes:\n");
				for(String str: ea) {
					attributes.add(str);
				}
			}
			te.setAttributes(attributes);
			list.add(te);
		}
		return new TraceViewController(list,Screen.getPrimary().getVisualBounds().getHeight()*0.9,Screen.getPrimary().getVisualBounds().getWidth()*0.5);
	}

	public static Browser browserify(Alignment alignment,DeclareMap model) {
		VisGraph graph = new VisGraph();
		List<VisNode> nodes = new ArrayList<VisNode>();
		List<VisEdge> edges = new ArrayList<VisEdge>();
		List<DataAwareStepTypes> steps = alignment.getStepTypes();
		ViolationIdentifier vid=null;
		try {
			vid = new ViolationIdentifier(alignment.getLogTrace(),alignment.getProcessTrace(),model);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for(int i=0; i<steps.size(); i++) {
			String step = steps.get(i).toString();
			if(step.endsWith("Log")) {
				String contribution = "<p>"+new Alignstep(alignment.getLogTrace().get(i).getActivity(),vid,i,steps.get(i)).getToolTipText()+"</p>";
				VisNode node = new VisNode(i,alignment.getLogTrace().get(i).getActivity(),"This is a move in log"+contribution);
				node.setColor("#ffff00");
				nodes.add(node);
			}
			else if(step.endsWith("Model")) {
				String contribution = "<p>"+new Alignstep(alignment.getLogTrace().get(i).getActivity(),vid,i,steps.get(i)).getToolTipText()+"</p>";
				VisNode node = new VisNode(i,alignment.getProcessTrace().get(i).getActivity(),"This is a move in model"+contribution);
				node.setColor("#e0b0ff");
				nodes.add(node);
			}
			else if(step.endsWith("Both")) {
				VisNode node = new VisNode(i,alignment.getLogTrace().get(i).getActivity(),"This is a move in log and model");
				node.setColor("#00d200");
				nodes.add(node);
			}
		}
		for(int i=0; i<nodes.size()-1; i++) {
			VisEdge edge = new VisEdge(nodes.get(i),nodes.get(i+1),null,null,null);
			edges.add(edge);
		}
		VisNode[] nodeArr = nodes.toArray(new VisNode[nodes.size()]);
		VisEdge[] edgeArr = edges.toArray(new VisEdge[edges.size()]);
		graph.addEdges(edgeArr);
		graph.addNodes(nodeArr);
		return new Browser(graph,Screen.getPrimary().getVisualBounds().getHeight()*0.6,Screen.getPrimary().getVisualBounds().getWidth()*0.73,"baseGraph3.html");
	}

	private static String[] getContributeArray(Alignstep alignstep) {
		String[] contributed=alignstep.getContributedToSolve();
		String[] solved=alignstep.getSolved();
		if (contributed.length+solved.length==0)
			return null;
		String[] contributed2=new String[contributed.length+solved.length];
		int j=0;
		for(String x : contributed)
			contributed2[j++]=x;
		for(String x : solved)
			contributed2[j++]=x;
		return contributed2;
	}

	public static TraceViewController getTraceView(Alignment alignment,DeclareMap model,XLog traces) {
		List<TraceElement> list = new ArrayList<TraceElement>();
		List<DataAwareStepTypes> steps = alignment.getStepTypes();
		ViolationIdentifier vid=null;
		try {
			vid = new ViolationIdentifier(alignment.getLogTrace(),alignment.getProcessTrace(),model);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for(int i=0; i<steps.size(); i++) {
			String step = steps.get(i).toString();
			TraceElement te = new TraceElement();
			if(step.endsWith("Log")) {
				Alignstep alignstep = new Alignstep(alignment.getLogTrace().get(i).getActivity(),vid,i,steps.get(i));
				String[] contributes = getContributeArray(alignstep);
				List<String> attributes = new ArrayList<String>();
				attributes.add("This is a move in log\n");
				if(contributes != null) {
					attributes.add("\nContributes to solve: \n");
					for(String s: contributes) {
						attributes.add(s+"\n");
					}
				}
				Optional<List<String>> opt = EventFinder.getEventAttributes(traces, alignment.getTraceName(), i);
				if(opt.isPresent()) {
					List<String> la = opt.get();
					if(!la.isEmpty()) {
						attributes.add("\nEvent Attributes:\n");
						for(String s: la) {
							attributes.add(s);
						}
					}
				}
				te.setAttributes(attributes);
				te.setText(alignment.getLogTrace().get(i).getActivity());
				te.setColor("#ffff00");
				list.add(te);
			}
			else if(step.endsWith("Model")) {
				Alignstep alignstep = new Alignstep(alignment.getLogTrace().get(i).getActivity(),vid,i,steps.get(i));
				String[] contributes = getContributeArray(alignstep);
				List<String> attributes = new ArrayList<String>();
				attributes.add("This is a move in model\n");
				if(contributes != null) {
					attributes.add("\nContributes to solve: \n");
					for(String s: contributes) {
						attributes.add(s+"\n");
					}
				}
				te.setAttributes(attributes);
				te.setText(alignment.getProcessTrace().get(i).getActivity());
				te.setColor("#a020f0");
				list.add(te);
			}
			else if(step.endsWith("Both")) {
				List<String> attributes = new ArrayList<String>();
				attributes.add("This is a move in log and model\n");
				Optional<List<String>> opt = EventFinder.getEventAttributes(traces, alignment.getTraceName(), i);
				if(opt.isPresent()) {
					List<String> la = opt.get();
					if(!la.isEmpty()) {
						attributes.add("\nEvent Attributes:\n");
						for(String s: la) {
							attributes.add(s);
						}
					}
				}
				te.setAttributes(attributes);
				te.setText(alignment.getLogTrace().get(i).getActivity());
				te.setColor("#00ff00");
				list.add(te);
			}
		}
		return new TraceViewController(list,Screen.getPrimary().getVisualBounds().getHeight()*0.9,Screen.getPrimary().getVisualBounds().getWidth()*0.56);
	}

	private static String getForTitle(String str1, String str2) {
		// TODO Auto-generated method stub
		Matcher m1 = Pattern.compile("\\{(.*)=(.*)\\}").matcher(str1);
		Matcher m2 = Pattern.compile("\\{(.*)=(.*)\\}").matcher(str2);
		if(m1.find() && m2.find()) {
			return m1.group(1)+" = "+m2.group(2)+" &#8800; "+m1.group(2);
		}
		return "";
	}

	public static Browser browserify(HashMap<Integer,String> actL, HashMap<Integer,List<String>> cspL, HashMap<Integer,ConstraintTemplate> tL, List<String> cL, Slider zoom) {
		/*
		 * actL HashMap of unique activities used in the constraints (keyed by index)
		 * cspL HashMap of activities used in each constraint (keyed by index)
		 * tL HashMap of template used in each constraint (keyed by index)
		 * cL list of constraint strings
		 */

		String dotGraphString = getDotGraphString(actL, cspL, tL, cL, null, null, false, true, false);
		return new Browser(Screen.getPrimary().getVisualBounds().getHeight()*0.76,Screen.getPrimary().getVisualBounds().getWidth() * 1,dotGraphString,zoom);

	}

	public static String createDiscoveryVisualizationString(DiscoveryTaskResult discoveryTaskResult) {
		return null;
	}

	//TODO: Review the code
	public static String createEditorVisualizationString(TreeItem<TreeDataBase> activitiesRoot, ObservableList<ConstraintDataRow> constraintDataRows, boolean showConstraintLabels, boolean showConditionLabels, boolean alternativeLayout) {

		HashMap<Integer,List<String>> constraintParametersMap = new HashMap<>(); //cspM (cspM)
		HashMap<Integer,ConstraintTemplate> templatesMap = new HashMap<>(); //tM
		List<String> constraintStrings = new ArrayList<>(); //lofC
		int index = 0;
		for (ConstraintDataRow constraintDataRow : constraintDataRows) {

			if (constraintDataRow.getRowStatus() == RowStatus.NEW)
				continue;
			
			//cspM (cspM)
			List<String> constraintParametersList = new ArrayList<>();
			
			constraintParametersList.add(constraintDataRow.getActivationActivity().getActivityName());
			if (constraintDataRow.getTemplate().getIsBinary())
				constraintParametersList.add(constraintDataRow.getTargetActivity().getActivityName());
			
			constraintParametersMap.put(index, constraintParametersList);

			//tM
			templatesMap.put(index, constraintDataRow.getTemplate());

			//lofC
			constraintStrings.add(ConstraintUtils.getConstraintString(constraintDataRow, true)); //TODO: Convert constraintDataRow to textual representation

			index++;
		}

		//actM (actL)
		
		
		Set<String> activities = new HashSet<>();
		for (List<String> constraintParameters : constraintParametersMap.values())
			for (String act : constraintParameters)
				activities.add(act);
		
		HashMap<Integer,String> activitiesMap = new HashMap<>();
		index = 0;
		for (String s : activities)
			activitiesMap.put(index++, s);

		//Not from original implementation, needed to visualize activities that are not used in constraints
		List<String> activitiesNotInConstraints = new ArrayList<>();
		for (TreeItem<TreeDataBase> treeItem : activitiesRoot.getChildren()) {
			String activityName = ((TreeDataActivity)treeItem).getActivityName();
			if (!activitiesMap.containsValue(activityName))
				activitiesNotInConstraints.add(activityName);
		}

		if (!constraintStrings.isEmpty() || !activitiesNotInConstraints.isEmpty())
			return GraphGenerator.getDotGraphString(activitiesMap, constraintParametersMap, templatesMap, constraintStrings, activitiesNotInConstraints, null, showConstraintLabels, showConditionLabels, alternativeLayout);
		else
			return null;
	}

	public static String createMonitoringVisualizationString(Map<String, String> constraintStates, boolean drawConstraintStates, boolean showConstraintLabels, boolean showConditionLabels, boolean horizontal) {
		HashMap<Integer,List<String>> constraintParametersMap = new HashMap<>(); //cspM (cspM)
		HashMap<Integer,ConstraintTemplate> templatesMap = new HashMap<>(); //tM
		List<String> constraintStrings = new ArrayList<>(); //lofC
		int index = 0;

		HashMap<Integer,String> constraintStatesMap = null;
		if (drawConstraintStates)
			constraintStatesMap = new HashMap<>();

		for (String constraint : constraintStates.keySet()) {
			Matcher mBinary = Pattern.compile("(.*)\\[(.*), (.*)\\]").matcher(constraint);
			Matcher mUnary = Pattern.compile(".*\\[(.*)\\]").matcher(constraint);

			ConstraintTemplate template = null;
			List<String> constraintParametersList = new ArrayList<>();

			if (mBinary.find()) {
				template = ConstraintTemplate.getByTemplateName(mBinary.group(1));
				if (template.getReverseActivationTarget()) {
					constraintParametersList.add(mBinary.group(3));
					constraintParametersList.add(mBinary.group(2));
				} else {
					constraintParametersList.add(mBinary.group(2));
					constraintParametersList.add(mBinary.group(3));
				}
			} else if(mUnary.find()) {
				template = ConstraintTemplate.getByTemplateName(mUnary.group(0).substring(0, mUnary.group(0).indexOf("["))); //TODO: Should be done more intelligently
				constraintParametersList.add(mUnary.group(1));
			}
			
			if (template == null) {
				continue;
			}

			constraintParametersMap.put(index, constraintParametersList);
			templatesMap.put(index, template);
			constraintStrings.add(constraint);
			if (drawConstraintStates)
				constraintStatesMap.put(index, constraintStates.get(constraint));
			
			index++;
		}

		//actM (actL)
		HashMap<Integer,String> activitiesMap = new HashMap<Integer,String>();
		List<List<String>> tmp = new ArrayList<List<String>>();
		tmp.addAll(constraintParametersMap.values());
		List<String> list = new ArrayList<String>();
		tmp.forEach(l -> list.addAll(l));
		List<String> activityList = list.stream().distinct().collect(Collectors.toList());
		index = 0;
		for(String s:activityList) {
			activitiesMap.put(index, s);
			index++;
		}

		return GraphGenerator.getDotGraphString(activitiesMap, constraintParametersMap, templatesMap, constraintStrings, null, constraintStatesMap, showConstraintLabels, showConditionLabels, horizontal);
	}

	public static String getDotGraphString(HashMap<Integer,String> actL, HashMap<Integer,List<String>> cspL, HashMap<Integer,ConstraintTemplate> tL, List<String> cL, List<String> activitiesNotInConstraints, HashMap<Integer,String> constraintStatesMap, boolean showConstraintLabels, boolean showConditionLabels, boolean horizontal) {
		/*
		 * actL - HashMap of unique activities used in the constraints (keyed by index)
		 * cspL - HashMap of activities used in each constraint (keyed by index)
		 * tL - HashMap of template used in each constraint (keyed by index)
		 * cL - list of constraint strings
		 * activitiesNotInConstraints - Not from original implementation, needed to visualize activities that are not used in constraints
		 * constraintStatesMap - Added for monitoring, contains fulfillment/violation states of the constraints
		 */

		Map<Integer,Boolean> isDrawnMap = new HashMap<>();
		Map<String,String> nodesMap = new HashMap<>();
		cspL.keySet().forEach(k -> isDrawnMap.put(k, false));
		StringBuilder sb = new StringBuilder("digraph \"\" {");
		//sb.append("size = \"6\"");
		//sb.append("ratio = \"fill\"");
		if (horizontal)
			sb.append("rankdir = \"LR\"");
		
		sb.append("id = \"graphRoot\"");
		sb.append("ranksep = \".6\"");
		sb.append("nodesep = \".5\"");
		List<String> edges = new ArrayList<>();
		cspL.forEach((k,v) -> {
			if(!isDrawnMap.get(k) && v.size() == 2) {
				String a = v.get(0);
				String b = v.get(1);
				Map<Integer, String> allUnaryForA = findAllUnaryFor(a,cspL,tL,cL,isDrawnMap,showConditionLabels);
				Map<Integer, String> allUnaryForB = findAllUnaryFor(b,cspL,tL,cL,isDrawnMap,showConditionLabels);
				int ka = getKeyFor(a,actL);
				int kb = getKeyFor(b,actL);
				String nodeA = "node"+ka;
				String nodeB = "node"+kb;
				
				if(nodesMap.get(nodeA) == null)
					nodesMap.put(nodeA, buildNodeString(nodeA,a,allUnaryForA, horizontal, constraintStatesMap));
				
				if(nodesMap.get(nodeB) == null)
					nodesMap.put(nodeB, buildNodeString(nodeB,b,allUnaryForB, horizontal, constraintStatesMap));
				
				String constraintStates = constraintStatesMap==null ? null : constraintStatesMap.get(k);
				edges.add(buildEdgeString(nodeA,nodeB,tL.get(k),getLabelFromConstraint(cL.get(k), showConstraintLabels, showConditionLabels),constraintStates));
				
				isDrawnMap.put(k, true);
			}
			
			if(!isDrawnMap.get(k) && v.size() == 1) {
				String activityName = v.get(0);
				Map<Integer, String> allUnaryForA = findAllUnaryFor(activityName,cspL,tL,cL,isDrawnMap,showConditionLabels);
				int ka = getKeyFor(activityName,actL);
				String nodeA = "node"+ka;
				
				if(nodesMap.get(nodeA) == null)
					nodesMap.put(nodeA, buildNodeString(nodeA,activityName,allUnaryForA, horizontal, constraintStatesMap));
				
				//nodes.add(buildNodeString(nodeA,a,allUnaryForA));
				isDrawnMap.put(k, true);
			}
		});
		sb.append("node [style=\"filled\", shape=box, fontsize=\"8\", fontname=\"Helvetica\"]");
		sb.append("edge [fontsize=\"8\", fontname=\"Helvetica\" arrowsize=\".8\"]");
		for(String s: nodesMap.values()) {
			sb.append(s);
		}
		for(String s: edges) {
			sb.append(s);
		}

		//Not from original implementation, adds activities that are not used in constraints
		if (activitiesNotInConstraints != null) {
			int nodeNumber = actL.size();
			for (String activityName : activitiesNotInConstraints) {
				//TODO: Replace null with something better
				sb.append(buildNodeString("node"+nodeNumber,activityName,new HashMap<Integer, String>(), horizontal, constraintStatesMap));
				nodeNumber++;
			}
		}

		sb.append("}");

		return sb.toString().replace("'", "\\'"); //A crude fix to allow ' characters in activity names
	}


	private static String findAllExistenceConstraints(HashMap<Integer,List<String>> constraintParameters,HashMap<Integer,String> templates, String activity, Set<Integer> picked, HashMap<Integer,Boolean> isDrawn, List<String> cL) {
		List<String> list = Arrays.asList(activity);
		List<Integer> keys = new ArrayList<Integer>();
		constraintParameters.forEach((k,v) -> {
			if(v.equals(list) && picked.contains(k) && !isDrawn.get(k)) keys.add(k);
		});
		String existence = "";
		List<String> templateList = new ArrayList<String>();
		for(int k: keys) {
			if(!isDrawn.get(k)) {
				isDrawn.put(k,true);
				templateList.add(templates.get(k)+"\n"+getLabelFromConstraint(cL.get(k)));
				//existence += templates.get(k) + "\n";
			}
		}
		Set<String> templateSet = new HashSet<String>(templateList);
		for(String s: templateSet) {
			existence += s + "\n";
		}
		if(existence.equals("")) return existence;
		else return existence+"\n";
	}

	private static String getLabelFromConstraint(String c) {
		Matcher mBinary = Pattern.compile(".*\\[.*\\] \\|(.*) \\|(.*) \\|(.*)").matcher(c);
		Matcher mUnary = Pattern.compile(".*\\[.*\\] \\|(.*) \\|(.*)").matcher(c);
		if(mBinary.find()) {
			return "[" + mBinary.group(1) + "]" + "[" + mBinary.group(2) + "]" + "[" + mBinary.group(3) + "]";
		}
		if(mUnary.find()) {
			return "[" + mUnary.group(1) + "]" + "[" + mUnary.group(2) + "]";
		}
		return "";
	}

	private static String getLabelFromConstraint(String constraintString, boolean showConstraintLabels, boolean showConditionLabels) {
		Matcher mBinaryConditions = Pattern.compile(".*\\[.*\\] \\|(.*) \\|(.*) \\|(.*)").matcher(constraintString);
		Matcher mUnaryConditions = Pattern.compile(".*\\[.*\\] \\|(.*) \\|(.*)").matcher(constraintString);

		Matcher mBinary = Pattern.compile(".*\\[.*\\]").matcher(constraintString);
		Matcher mUnary = Pattern.compile(".*\\[.*\\]").matcher(constraintString);

		StringBuilder sbLabel = new StringBuilder();

		if (mBinary.find()) {
			if (showConstraintLabels) {
				sbLabel.append(constraintString.substring(0, constraintString.indexOf("[")));
			}
			if (showConditionLabels && mBinaryConditions.find()) {
				if (!sbLabel.toString().equals("")) {
					sbLabel.append("\\n");
				}
				sbLabel.append("[").append(mBinaryConditions.group(1)).append("][").append(mBinaryConditions.group(2)).append("][").append(mBinaryConditions.group(3).trim()).append("]");
			}
		} else if (mUnary.find()) { //TODO: Check if this branch is even needed
			if (showConstraintLabels) {
				sbLabel.append(mUnary.group(0));
			}
			if (showConditionLabels && mUnaryConditions.find()) {
				if (!sbLabel.toString().equals("")) {
					sbLabel.append("\\n");
				}
				sbLabel.append("[").append(mUnaryConditions.group(1)).append("][").append(mUnaryConditions.group(2)).append("]");
			}
		}
		System.out.println(sbLabel.toString());
		return sbLabel.toString();
	}

	private static VisEdge getCorrespondingEdge(VisNode start, VisNode end, DeclareTemplate template, String c) {
		int last = start.getLabel().lastIndexOf('\n');
		String startActivity = start.getLabel().substring(last+1);
		last = end.getLabel().lastIndexOf('\n');
		String endActivity = end.getLabel().substring(last+1);
		//int diff = Math.abs(start.getId()-end.getId());
		String constraint = template.name() + "[" +startActivity+", "+endActivity+"]";
		String label = getLabelFromConstraint(c);
		if(template == DeclareTemplate.Responded_Existence) {
			Smooth smooth = new Smooth(true, "dynamic");
			VisEdge edge = new VisEdge(start,end,null,smooth,constraint);//,diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			return edge;
		}
		if(template == DeclareTemplate.Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);

			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			return edge;
		}
		if(template == DeclareTemplate.Alternate_Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);

			Smooth smooth = new Smooth(true,"doubleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Chain_Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);

			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Chain_Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);

			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Response) {
			ArrowProperty to = new ArrowProperty(true,"arrow");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);

			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			Arrows arrows = new Arrows(to,null,null);

			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			return edge;
		}
		if(template == DeclareTemplate.Alternate_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			Arrows arrows = new Arrows(to,null,null);

			Smooth smooth = new Smooth(true,"doubleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Chain_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			Arrows arrows = new Arrows(to,null,null);

			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Chain_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty middle = new ArrowProperty(true, "bar");
			Arrows arrows = new Arrows(to,middle,null);

			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Precedence) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty middle = new ArrowProperty(true, "bar");
			Arrows arrows = new Arrows(to,middle,null);

			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.CoExistence) {
			ArrowProperty to = new ArrowProperty(true,"circle");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);

			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			return edge;
		}
		if(template == DeclareTemplate.Not_CoExistence) {
			ArrowProperty to = new ArrowProperty(true,"circle");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);

			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);

			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			return edge;
		}
		if(template == DeclareTemplate.Not_Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);

			Smooth smooth = new Smooth(true,"dynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Alternate_Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);

			Smooth smooth = new Smooth(true,"doubleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Chain_Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,null,from);

			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-5);
			edge.setFont(f);
			return edge;
		}
		if(template == DeclareTemplate.Not_Chain_Succession) {
			ArrowProperty to = new ArrowProperty(true,"circlearrow");
			ArrowProperty middle = new ArrowProperty(true,"bar");
			ArrowProperty from = new ArrowProperty(true,"circle");
			Arrows arrows = new Arrows(to,middle,from);

			Smooth smooth = new Smooth(true,"tripleDynamic");
			VisEdge edge = new VisEdge(start,end,arrows,smooth,constraint);//diff*400*Math.pow(1.25, offset));
			edge.setLabel(label);
			Font f = new Font("#000000");
			f.setVadjust(-15);
			edge.setFont(f);
			return edge;
		}
		VisEdge edge = new VisEdge(start,end,null,null,constraint);//diff*400*Math.pow(1.25, offset));
		edge.setLabel(label);
		return edge;
	}

	private static Map<Integer, String> findAllUnaryFor(String s, Map<Integer,List<String>> cpM, Map<Integer,ConstraintTemplate> tM, Map<Integer,Boolean> idM) {
		Map<Integer, String> l = new HashMap<Integer, String>();
		cpM.forEach((k,v) -> {
			if(!idM.get(k) && v.size() == 1 && v.get(0).equals(s)) {
				l.put(k, tM.get(k).name());
				idM.put(k, true);
			}
		});
		return l;
	}

	private static Map<Integer, String> findAllUnaryFor(String s, Map<Integer,List<String>> cpM, Map<Integer,ConstraintTemplate> tL, List<String> cL, Map<Integer,Boolean> idM, boolean showConditionLabels) {
		Map<Integer, String> l = new HashMap<Integer, String>();
		cpM.forEach((k,v) -> {
			if(!idM.get(k) && v.size() == 1 && v.get(0).equals(s)) {
				String constraint = cL.get(k);
				String insertIt = tL.get(k).name();
				if (showConditionLabels) {
					insertIt = insertIt+getLabelFromConstraint(constraint);
				}
				l.put(k, insertIt.replace("<", "&lt;").replace(">","&gt;").replace("||","\\|\\|"));
				idM.put(k, true);
			}
		});
		return l;
	}

	private static int getKeyFor(String s, Map<Integer,String> aM) {
		for(int k: aM.keySet()) {
			if(aM.get(k).equals(s)) return k;
		}
		return -1;
	}

	private static String getHexValue(long value) {
		long b1 = value / 16;
		long b2 = value % 16;
		String s = "";
		if(b1 == 0) s = "0";
		if(b1 == 1) s = "1";
		if(b1 == 2) s = "2";
		if(b1 == 3) s = "3";
		if(b1 == 4) s = "4";
		if(b1 == 5) s = "5";
		if(b1 == 6) s = "6";
		if(b1 == 7) s = "7";
		if(b1 == 8) s = "8";
		if(b1 == 9) s = "9";
		if(b1 == 10) s = "a";
		if(b1 == 11) s = "b";
		if(b1 == 12) s = "c";
		if(b1 == 13) s = "d";
		if(b1 == 14) s = "e";
		if(b1 == 15) s = "f";

		if(b2 == 0) s += "0";
		if(b2 == 1) s += "1";
		if(b2 == 2) s += "2";
		if(b2 == 3) s += "3";
		if(b2 == 4) s += "4";
		if(b2 == 5) s += "5";
		if(b2 == 6) s += "6";
		if(b2 == 7) s += "7";
		if(b2 == 8) s += "8";
		if(b2 == 9) s += "9";
		if(b2 == 10) s += "a";
		if(b2 == 11) s += "b";
		if(b2 == 12) s += "c";
		if(b2 == 13) s += "d";
		if(b2 == 14) s += "e";
		if(b2 == 15) s += "f";

		return s;
	}

	private static String getColorFrom(double supp,int size) {
		double res = 51 + 26 * (1-supp) * 27.46;
		double portion = 1.5 / (size+1.5);
		String color = "";
		if(res > 255) {
			long remaining = Math.round((res - 255) / 2);
			color = "#"+getHexValue(remaining)+getHexValue(remaining)+"ff";
		}
		else {
			color = "#0000"+getHexValue(Math.round(res));
		}
		String fc = "#e6e600";
		return "fillcolor=\""+color+";"+portion+":#808080\" gradientangle=90 fontcolor=\""+fc+"\"";
	}

	//Used for monitoring and editor; creates a node strings with html table if the node has unary constraints (otherwise normal node strings are used)
	private static String buildNodeString(String nodeId, String activityName, Map<Integer, String> ls, boolean horizontal, HashMap<Integer,String> constraintStatesMap) {
		StringBuilder sb = new StringBuilder(nodeId);

		if(ls.isEmpty()) {
			//A normal graphviz node is used if there are no unary constraints for this activity
			sb.append(" [label=" + "\"").append(activityName).append("\\\\n\"");
			sb.append(" fillcolor=\"#0000ff\" fontcolor=\"#ffffff\"");
			sb.append(" tooltip=\"").append(activityName).append("\"]");
			return sb.toString();
		} else {
			//An html table is used as the graphviz node label if there are any unary constraints for this activity
			sb.append(" [shape=none, margin=0, label=<<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\">");

			if (constraintStatesMap != null) { //Monitoring
				ls.forEach((k, v) -> {
					sb.append("<tr><td width=\"60\" bgcolor=\"");
					//Colouring based on the constraint status
					if (constraintStatesMap.get(k).equals("conflict")) {
						sb.append("#ff9900\">").append(v);
					} else if (constraintStatesMap.get(k).equals("sat")) {
						sb.append("#66ccff\">").append(v);
					} else if (constraintStatesMap.get(k).equals("viol")) {
						sb.append("#d44942\">").append(v);
					} else if (constraintStatesMap.get(k).equals("poss.viol")) {
						sb.append("#ffd700\">").append(v);
					} else if (constraintStatesMap.get(k).equals("poss.sat")) {
						sb.append("#79a888\">").append(v);
					} else {
						sb.append("#000000\"><font color=\"white\">").append(v).append("</font>");
					}
					sb.append("</td></tr>");
				});
			} else { //Editor and monitoring initial state
				ls.forEach((k, v) -> {
					sb.append("<tr><td width=\"60\" bgcolor=\"#000000\">");
					sb.append("<font color=\"white\">").append(v).append("</font>");
					sb.append("</td></tr>");
				});
			}

			sb.append("<tr><td bgcolor=\"blue\"><font color=\"white\">");
			sb.append(activityName);
			sb.append("</font></td></tr></table>>]");

			return sb.toString();
		}
	}

	//Used for discovery; creates a standard graphviz node strings
	private static String buildNodeString(String nodeId, String activityName, Map<Integer, String> ls, double supp, boolean horizontal) {
		String color = "";
		String ss = "";
		if(supp != -1) {
			color = getColorFrom(supp,ls.size());
			ss = String.format("%.1f%%", supp*100);
		}
		if(supp == -1) {
			double portion = 1.0 / (ls.size()+1);
			color = "#0000ff";
			String fc= "#ffffff";
			color = "fillcolor=\""+color+";"+portion+":#000000\" gradientangle=90 fontcolor=\""+fc+"\"";
		}
		if(ls.isEmpty()) {
			return nodeId + " [label=" + "\"" + activityName + "\\\\n" + ss +"\"" + color +" tooltip=\""+activityName+"\"]";
		}
		else {
			String unaryRep = "\"";
			if (!horizontal) {
				unaryRep += "{";
			}
			for(String u : ls.values()) {
				unaryRep += u + "|";
			}
			unaryRep += activityName + "\\\\n" + ss;
			if (!horizontal) {
				unaryRep += "}";
			}
			unaryRep += "\"";
			//System.out.println(n + " [shape=\"record\" label="+ unaryRep +" "+color+"]");
			return nodeId + " [shape=\"record\" label="+ unaryRep +" "+color+" tooltip=\""+activityName+"\"]";
		}
	}

	private static String getStyleForTemplate(ConstraintTemplate template, String label, String penwidth, String constraintState) {
		String color = "#000000";;
		if (constraintState != null) {
			switch(constraintState) {
			case "conflict":
				color = "#ff9900";
				break;
			case "sat":
				color = "#66ccff";
				break;
			case "viol":
				color = "#d44942";
				break;
			case "poss.viol":
				color = "#ffd700";
				break;
			case "poss.sat":
				color = "#79a888";
				break;
			}
		}
		
		switch(template) {
		case Responded_Existence:
			return "[dir=\"both\", edgetooltip=\"Responded Existence\", labeltooltip=\"Responded Existence\",arrowhead=\"none\",arrowtail=\"dot\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Response:
			return "[dir=\"both\", edgetooltip=\"Response\", labeltooltip=\"Response\", arrowhead=\"normal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Alternate_Response:
			return "[edgetooltip=\"Alternate Response\", labeltooltip=\"Alternate Response\", dir=\"both\", arrowhead=\"normal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+":"+color+"\","+penwidth+"]";
		case Chain_Response:
			return "[edgetooltip=\"Chain Response\", labeltooltip=\"Chain Response\", dir=\"both\", arrowhead=\"normal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case Precedence:
			return "[arrowhead=\"dotnormal\", edgetooltip=\"Precedence\", labeltooltip=\"Precedence\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Alternate_Precedence:
			return "[arrowhead=\"dotnormal\", edgetooltip=\"Alternate Precedence\", labeltooltip=\"Alternate Precedence\", label=\""+label+"\", color=\""+color+":"+color+"\","+penwidth+"]";
		case Chain_Precedence:
			return "[arrowhead=\"dotnormal\", edgetooltip=\"Chain Precedence\", labeltooltip=\"Chain Precedence\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case Succession:
			return "[dir=\"both\", edgetooltip=\"Succession\", labeltooltip=\"Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Alternate_Succession:
			return "[dir=\"both\", edgetooltip=\"Alternate Succession\", labeltooltip=\"Alternate Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+":"+color+"\","+penwidth+"]";
		case Chain_Succession:
			return "[dir=\"both\", edgetooltip=\"Chain Succession\", labeltooltip=\"Chain Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case CoExistence:
			return "[dir=\"both\", edgetooltip=\"CoExistence\", labeltooltip=\"CoExistence\", arrowhead=\"dot\", arrowtail=\"dot\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Choice:
			return "[dir=\"both\", edgetooltip=\"Choice\", labeltooltip=\"Choice\", arrowhead=\"odiamond\", arrowtail=\"odiamond\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Exclusive_Choice:
			return "[dir=\"both\", edgetooltip=\"Exclusive Choice\", labeltooltip=\"Exclusive Choice\", arrowhead=\"diamond\", arrowtail=\"diamond\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Not_Chain_Succession:
			return "[dir=\"both\", edgetooltip=\"Not Chain Succession\", labeltooltip=\"Not Chain Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", style=\"dashed\", label=\""+label+"\", color=\""+color+":"+color+":"+color+"\","+penwidth+"]";
		case Not_CoExistence:
			return "[dir=\"both\", edgetooltip=\"Not CoExistence\", labeltooltip=\"Not CoExistence\", arrowhead=\"dot\", arrowtail=\"dot\", style=\"dashed\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		case Not_Succession:
			return "[dir=\"both\", edgetooltip=\"Not Succession\", labeltooltip=\"Not Succession\", arrowhead=\"dotnormal\", arrowtail=\"dot\", style=\"dashed\", label=\""+label+"\", color=\""+color+"\","+penwidth+"]";
		default:
			throw new NoSuchElementException("Style not defined for template " + template);
		}
	}

	private static String getStyleForTemplate(String template, String label, double support) {
		ConstraintTemplate t = ConstraintTemplate.getByTemplateName(template);
		String penwidth = "penwidth="+String.format("%.1f", 0.5+support);
		return getStyleForTemplate(t, label, penwidth, null);
	}

	private static String buildEdgeString(String nodeA, String nodeB, ConstraintTemplate template, String label, String constraintState) {
		String style = getStyleForTemplate(template, label, "", constraintState);
		
		if (template.getReverseActivationTarget())
			return nodeB + " -> " + nodeA + " " + style;
		else
			return nodeA + " -> " + nodeB + " " + style;
	}

	public static Browser browserify(Map<Integer,String> activitiesMap, Map<Integer,Double> actSuppMap, Map<Integer,ConstraintTemplate> templatesMap, Map<Integer,List<String>> constraintParametersMap, Map<Integer,Double> constraintSuppMap, Slider zoom, String view, String supportFor) {
		if(view.equals("Declare")) {
			System.out.println("Preparing Declare view...");
			Map<Integer,Boolean> isDrawnMap = new HashMap<Integer,Boolean>();
			Map<String,String> nodesMap = new HashMap<String,String>();
			constraintParametersMap.keySet().forEach(k -> isDrawnMap.put(k, false));
			StringBuilder sb = new StringBuilder("digraph \"\" {");
			//sb.append("size = \"6\"");
			//sb.append("ratio = \"fill\"");
			//sb.append("center = \"true\"");
			sb.append("ranksep = \".6\"");
			sb.append("nodesep = \".5\"");
			//List<String> nodes = new ArrayList<>();
			List<String> edges = new ArrayList<>();
			constraintParametersMap.forEach((k,v) -> {
				if(!isDrawnMap.get(k) && v.size() == 2) {
					String a = v.get(0);
					String b = v.get(1);
					Map<Integer, String> allUnaryForA = findAllUnaryFor(a,constraintParametersMap,templatesMap,isDrawnMap);
					Map<Integer, String> allUnaryForB = findAllUnaryFor(b,constraintParametersMap,templatesMap,isDrawnMap);
					int ka = getKeyFor(a,activitiesMap);
					int kb = getKeyFor(b,activitiesMap);
					String nodeA = "node"+ka;
					String nodeB = "node"+kb;
					if(nodesMap.get(nodeA) == null) {
						nodesMap.put(nodeA, buildNodeString(nodeA,a,allUnaryForA,actSuppMap.get(ka), false));
					}
					if(nodesMap.get(nodeB) == null) {
						nodesMap.put(nodeB, buildNodeString(nodeB,b,allUnaryForB,actSuppMap.get(kb), false));
					}
					String label = String.format("%.1f%%", constraintSuppMap.get(k)*100);
					edges.add(buildEdgeString(nodeA,nodeB,templatesMap.get(k),label, null));
					isDrawnMap.put(k, true);
				}
				if(!isDrawnMap.get(k) && v.size() == 1) {
					String a = v.get(0);
					Map<Integer, String> allUnaryForA = findAllUnaryFor(a,constraintParametersMap,templatesMap,isDrawnMap);
					int ka = getKeyFor(a,activitiesMap);
					String nodeA = "node"+ka;
					if(nodesMap.get(nodeA) == null) {
						nodesMap.put(nodeA, buildNodeString(nodeA,a,allUnaryForA,actSuppMap.get(ka), false));
					}
					//nodes.add(buildNodeString(eA,a,allUnaryForA));
					isDrawnMap.put(k, true);
				}
			});
			sb.append("node [style=\"filled\", shape=box, fontsize=\"8\", fontname=\"Helvetica\"]");
			sb.append("edge [fontsize=\"8\", fontname=\"Helvetica\" arrowsize=\".8\"]");
			for(String s: nodesMap.values()) {
				sb.append(s);
			}
			for(String s: edges) {
				//System.out.println(s);
				sb.append(s);
			}
			sb.append("}");
			System.out.println("Dot string is ready...");
			Browser browser = new Browser(Screen.getPrimary().getVisualBounds().getHeight()*0.79,Screen.getPrimary().getVisualBounds().getWidth() * 0.75,sb.toString(),zoom);
			browser.setActivitiesMap(activitiesMap);
			browser.setActSuppMap(actSuppMap);
			browser.setTemplatesMap(templatesMap);
			browser.setConstraintParametersMap(constraintParametersMap);
			browser.setConstraintSuppMap(constraintSuppMap);
			return browser;
		}
		if(view.equals("Automaton")) {
			ProcessModel pm = ProcessModelGenerator.obtainProcessModel
					(activitiesMap, templatesMap, constraintParametersMap);

			ConstraintsPrinter cPrin = new ConstraintsPrinter(pm);
			String dotRep = cPrin.printDotAutomaton().replace("\n", "");
			
			Browser browser = new Browser(Screen.getPrimary().getVisualBounds().getHeight()*0.79,Screen.getPrimary().getVisualBounds().getWidth() * 0.75,dotRep,zoom);
			browser.setActivitiesMap(activitiesMap);
			browser.setActSuppMap(actSuppMap);
			browser.setTemplatesMap(templatesMap);
			browser.setConstraintParametersMap(constraintParametersMap);
			browser.setConstraintSuppMap(constraintSuppMap);
			return browser;
		}
		StringBuilder sbActivity = new StringBuilder();
		StringBuilder sbConstraint = new StringBuilder();
		int index = 1;
		List<Integer> la = activitiesMap.keySet().stream().sorted((i1,i2) -> {
			double s1 = actSuppMap.get(i1);
			double s2 = actSuppMap.get(i2);
			if(s2 > s1) return 1;
			else if(s1 > s2) return -1;
			else return 0;
		}).collect(Collectors.toList());
		for(int k:la) {
			double s = actSuppMap.get(k);
			String act = activitiesMap.get(k);
			String addIt = String.format("%d) %s : Exists in %.2f%% of traces in the log\\n", index, act, s*100);
			sbActivity.append(addIt);
			index++;
		}
		index = 1;
		List<Integer> lc = templatesMap.keySet().stream().sorted((i1,i2) -> {
			double s1 = constraintSuppMap.get(i1);
			double s2 = constraintSuppMap.get(i2);
			if(s2 > s1) return 1;
			else if(s1 > s2) return -1;
			else return 0;
		}).collect(Collectors.toList());
		for(int k:lc) {
			double s = constraintSuppMap.get(k);
			List<String> params = constraintParametersMap.get(k);
			ConstraintTemplate template = templatesMap.get(k);
			String[] p = params.toArray(new String[params.size()]);
			String exp = TemplateDescription.get(templatesMap.get(k), p);
			if(template == ConstraintTemplate.Absence2 || template == ConstraintTemplate.Existence || template == ConstraintTemplate.Init || template == ConstraintTemplate.End) {
				String addIt = String.format("%d) In %.2f%% of %s in the log, %s\\n", index, s*100, "traces", exp);
				sbConstraint.append(addIt);
			}
			else {
				String addIt = String.format("%d) In %.2f%% of %s in the log, %s\\n", index, s*100, supportFor, exp);
				sbConstraint.append(addIt);
			}
			index++;
		}

		Browser browser = new Browser(Screen.getPrimary().getVisualBounds().getHeight()*0.79,Screen.getPrimary().getVisualBounds().getWidth() * 0.75,sbActivity.toString(),sbConstraint.toString(),zoom);
		//System.out.println("Activities: "+sbActivity.toString());
		//System.out.println("Constraints: "+sbConstraint.toString());
		browser.setActivitiesMap(activitiesMap);
		browser.setActSuppMap(actSuppMap);
		browser.setTemplatesMap(templatesMap);
		browser.setConstraintParametersMap(constraintParametersMap);
		browser.setConstraintSuppMap(constraintSuppMap);
		return browser;
	}

	//BROWSER FOR MP DECLARE:
	public static Browser browserify(Map<Integer,String> activitiesMap, Map<Integer,Double> actSuppMap, Map<Integer,ConstraintTemplate> templatesMap, Map<Integer,List<String>> constraintParametersMap, Map<Integer,Double> constraintSuppMap, String MPDeclareResults, Slider zoom, String view, String supportFor) {
		if(view.equals("Declare")) {
			System.out.println("Preparing Declare view...");
			Map<Integer,Boolean> isDrawnMap = new HashMap<Integer,Boolean>();
			Map<String,String> nodesMap = new HashMap<String,String>();
			constraintParametersMap.keySet().forEach(k -> isDrawnMap.put(k, false));
			StringBuilder sb = new StringBuilder("digraph \"\" {");
			//sb.append("size = \"6\"");
			//sb.append("ratio = \"fill\"");
			//sb.append("center = \"true\"");
			sb.append("ranksep = \".6\"");
			sb.append("nodesep = \".5\"");
			//List<String> nodes = new ArrayList<>();
			List<String> edges = new ArrayList<>();
			constraintParametersMap.forEach((k,v) -> {
				if(!isDrawnMap.get(k) && v.size() == 2) {
					String a = v.get(0);
					String b = v.get(1);
					Map<Integer, String> allUnaryForA = findAllUnaryFor(a,constraintParametersMap,templatesMap,isDrawnMap);
					Map<Integer, String> allUnaryForB = findAllUnaryFor(b,constraintParametersMap,templatesMap,isDrawnMap);
					int ka = getKeyFor(a,activitiesMap);
					int kb = getKeyFor(b,activitiesMap);
					String nodeA = "node"+ka;
					String nodeB = "node"+kb;
					if(nodesMap.get(nodeA) == null) {
						nodesMap.put(nodeA, buildNodeString(nodeA,a,allUnaryForA,actSuppMap.get(ka), false));
					}
					if(nodesMap.get(nodeB) == null) {
						nodesMap.put(nodeB, buildNodeString(nodeB,b,allUnaryForB,actSuppMap.get(kb), false));
					}
					String label = String.format("%.1f%%", constraintSuppMap.get(k)*100);
					edges.add(buildEdgeString(nodeA,nodeB,templatesMap.get(k),label,null));
					isDrawnMap.put(k, true);
				}
				if(!isDrawnMap.get(k) && v.size() == 1) {
					String a = v.get(0);
					Map<Integer, String> allUnaryForA = findAllUnaryFor(a,constraintParametersMap,templatesMap,isDrawnMap);
					int ka = getKeyFor(a,activitiesMap);
					String nodeA = "node"+ka;
					if(nodesMap.get(nodeA) == null) {
						nodesMap.put(nodeA, buildNodeString(nodeA,a,allUnaryForA,actSuppMap.get(ka), false));
					}
					//nodes.add(buildNodeString(eA,a,allUnaryForA));
					isDrawnMap.put(k, true);
				}
			});
			sb.append("node [style=\"filled\", shape=box, fontsize=\"8\", fontname=\"Helvetica\"]");
			sb.append("edge [fontsize=\"8\", fontname=\"Helvetica\" arrowsize=\".8\"]");
			for(String s: nodesMap.values()) {
				sb.append(s);
			}
			for(String s: edges) {
				//System.out.println(s);
				sb.append(s);
			}
			sb.append("}");
			System.out.println("Dot string is ready...");
			Browser browser = new Browser(Screen.getPrimary().getVisualBounds().getHeight()*0.79,Screen.getPrimary().getVisualBounds().getWidth() * 0.75,sb.toString(),zoom);
			browser.setActivitiesMap(activitiesMap);
			browser.setActSuppMap(actSuppMap);
			browser.setTemplatesMap(templatesMap);
			browser.setConstraintParametersMap(constraintParametersMap);
			browser.setConstraintSuppMap(constraintSuppMap);
			browser.setMPDeclareResults(MPDeclareResults);
			return browser;
		}
		if(view.equals("Automaton")) {
			ProcessModel pm = ProcessModelGenerator.obtainProcessModel
					(activitiesMap, templatesMap, constraintParametersMap);

			ConstraintsPrinter cPrin = new ConstraintsPrinter(pm);
			String dotRep = cPrin.printDotAutomaton().replace("\n", "");
			Browser browser = new Browser(Screen.getPrimary().getVisualBounds().getHeight()*0.79,Screen.getPrimary().getVisualBounds().getWidth() * 0.75,dotRep,zoom);
			browser.setActivitiesMap(activitiesMap);
			browser.setActSuppMap(actSuppMap);
			browser.setTemplatesMap(templatesMap);
			browser.setConstraintParametersMap(constraintParametersMap);
			browser.setConstraintSuppMap(constraintSuppMap);
			browser.setMPDeclareResults(MPDeclareResults);
			return browser;
		}
		StringBuilder sbActivity = new StringBuilder();
		StringBuilder sbConstraint = new StringBuilder();
		int index = 1;
		List<Integer> la = activitiesMap.keySet().stream().sorted((i1,i2) -> {
			double s1 = actSuppMap.get(i1);
			double s2 = actSuppMap.get(i2);
			if(s2 > s1) return 1;
			else if(s1 > s2) return -1;
			else return 0;
		}).collect(Collectors.toList());
		for(int k:la) {
			double s = actSuppMap.get(k);
			String act = activitiesMap.get(k);
			String addIt = String.format("%d) %s : Exists in %.2f%% of traces in the log\\n", index, act, s*100);
			sbActivity.append(addIt);
			index++;
		}
		index = 1;
		List<Integer> lc = templatesMap.keySet().stream().sorted((i1,i2) -> {
			double s1 = constraintSuppMap.get(i1);
			double s2 = constraintSuppMap.get(i2);
			if(s2 > s1) return 1;
			else if(s1 > s2) return -1;
			else return 0;
		}).collect(Collectors.toList());
		for(int k:lc) {
			double s = constraintSuppMap.get(k);
			List<String> params = constraintParametersMap.get(k);
			ConstraintTemplate template = templatesMap.get(k);
			String[] p = params.toArray(new String[params.size()]);
			String exp = TemplateDescription.get(templatesMap.get(k), p);
			if(template == ConstraintTemplate.Absence2 || template == ConstraintTemplate.Existence || template == ConstraintTemplate.Init || template == ConstraintTemplate.End) {
				String addIt = String.format("%d) In %.2f%% of %s in the log, %s\\n", index, s*100, "traces", exp);
				sbConstraint.append(addIt);
			}
			else {
				String addIt = String.format("%d) In %.2f%% of %s in the log, %s\\n", index, s*100, supportFor, exp);
				sbConstraint.append(addIt);
			}
			index++;
		}

		Browser browser = new Browser(Screen.getPrimary().getVisualBounds().getHeight()*0.79,Screen.getPrimary().getVisualBounds().getWidth() * 0.75, sbActivity.toString(),sbConstraint.toString(), MPDeclareResults,zoom);
		//System.out.println("Activities: "+sbActivity.toString());
		//System.out.println("Constraints: "+sbConstraint.toString());
		browser.setActivitiesMap(activitiesMap);
		browser.setActSuppMap(actSuppMap);
		browser.setTemplatesMap(templatesMap);
		browser.setConstraintParametersMap(constraintParametersMap);
		browser.setConstraintSuppMap(constraintSuppMap);
		browser.setMPDeclareResults(MPDeclareResults);
		return browser;
	}

	public static String createTemplateDescriptionVisualizationString(List<DiscoveredActivity> sampleActivities, DiscoveredConstraint sampleConstraint) {
		StringBuilder sb = new StringBuilder("digraph \"\" {");
		//sb.append("size = \"6\"");
		//sb.append("ratio = \"fill\"");
		sb.append("rankdir = \"LR\"");
		sb.append("id = \"graphRoot\"");
		sb.append("ranksep = \".6\"");
		sb.append("nodesep = \".5\"");
		sb.append("node [style=\"filled\", shape=box, fontsize=\"8\", fontname=\"Helvetica\"]");
		sb.append("edge [fontsize=\"8\", fontname=\"Helvetica\" arrowsize=\".8\"]");
		
		Map<DiscoveredActivity, String> nodeNames = new HashMap<>();
		Map<String, Map<Integer, String>> activityToUnaryConstraints = new HashMap<>();
		
		for (DiscoveredActivity a : sampleActivities) {
			nodeNames.put(a, "node"+sampleActivities.indexOf(a));
			activityToUnaryConstraints.put(a.getActivityFullName(), new HashMap<>());
		}
		
		List<String> edges = new ArrayList<String>();
		String label;
		if (sampleConstraint.getTemplate().getIsBinary()) {
			label = sampleConstraint.getTemplate().toString();
			edges.add(buildEdgeString(nodeNames.get(sampleConstraint.getActivationActivity()), nodeNames.get(sampleConstraint.getTargetActivity()), sampleConstraint.getTemplate(), label, null));
		
		} else {
			label = sampleConstraint.getTemplate().name();
			int index = activityToUnaryConstraints.get(sampleConstraint.getActivationActivity().getActivityFullName()).size();
			activityToUnaryConstraints.get(sampleConstraint.getActivationActivity().getActivityFullName()).put(index, label);
		}
		
		for (DiscoveredActivity a : sampleActivities)
			sb.append(buildNodeString(nodeNames.get(a), a.getActivityFullName(), activityToUnaryConstraints.get(a.getActivityFullName()), a.getActivitySupport(), true));

		for (String string : edges)
			sb.append(string);

		sb.append("}");
		
		return sb.toString();
	}
	
	//TODO: Review the code
	public static String createDeclareVisualizationString(List<DiscoveredActivity> filteredActivities, List<DiscoveredConstraint> filteredConstraints, boolean showConstraints, ShowLabelEnum labelToBeShown, boolean alternativeLayout) {
		StringBuilder sb = new StringBuilder("digraph \"\" {");
		
		if (alternativeLayout)
			sb.append("rankdir = \"LR\"");
		
		sb.append("ranksep = \".6\"");
		sb.append("nodesep = \".5\"");
		sb.append("node [style=\"filled\", shape=box, fontsize=\"8\", fontname=\"Helvetica\"]");
		sb.append("edge [fontsize=\"8\", fontname=\"Helvetica\" arrowsize=\".8\"]");

		Map<DiscoveredActivity, String> nodeNames = new HashMap<>();
		Map<String, Map<Integer, String>> activityToUnaryConstraints = new HashMap<>();
		
		for (DiscoveredActivity a : filteredActivities) {
			nodeNames.put(a, "node"+filteredActivities.indexOf(a));
			activityToUnaryConstraints.put(a.getActivityFullName(), new HashMap<>());
		}

		List<String> edges = new ArrayList<>();
		for (DiscoveredConstraint c : filteredConstraints) {
			String label = "";
			
			if (c.getTemplate().getIsBinary()) {
				if (showConstraints)
					label += c.getTemplate().toString();
				
				if (c.getDataCondition() != null) {
					if (showConstraints)
						label += "\\\\n";
					
					label += c.getDataCondition().toDeclareString();
				}
				
				if (!labelToBeShown.equals(ShowLabelEnum.NONE)) {
					
					label += "\\\\n";
					
					switch (labelToBeShown) {
					case SUPPORT:
						float support = c.getConstraintSupport();
						label += String.format("%.1f%%", support*100);
						break;
					
					case MIN_TD:
						if (c.getMinTD() != null)
							label += c.getMinTD().toString().substring(2).toLowerCase();
						break;
						
					case AVG_TD:
						if (c.getAvgTD() != null)
							label += c.getAvgTD().toString().substring(2).toLowerCase();
						break;
						
					case MAX_TD:
						if (c.getMaxTD() != null)
							label += c.getMaxTD().toString().substring(2).toLowerCase();
						break;
						
					default:
						break;
					}
				}
				
				edges.add(buildEdgeString(nodeNames.get(c.getActivationActivity()), nodeNames.get(c.getTargetActivity()), c.getTemplate(), label, null));
			
			} else {
				label = c.getTemplate().toString();
				int index = activityToUnaryConstraints.get(c.getActivationActivity().getActivityFullName()).size();
				
				switch (labelToBeShown) {
				case SUPPORT:
					label += " - " + String.format("%.1f%%", c.getConstraintSupport()*100);
					break;
				case MIN_TD:
					if (c.getMinTD() != null)
						label += " - " + c.getMinTD().toString().substring(2).toLowerCase();
					break;
				case AVG_TD:
					if (c.getAvgTD() != null)
						label += " - " + c.getAvgTD().toString().substring(2).toLowerCase();
					break;
				case MAX_TD:
					if (c.getMaxTD() != null)
						label += " - " + c.getMaxTD().toString().substring(2).toLowerCase();
					break;
				default:
					break;
				}
				
				activityToUnaryConstraints.get(c.getActivationActivity().getActivityFullName()).put(index, label);
			}
		}

		for (DiscoveredActivity discoveredActivity : filteredActivities)
			sb.append(buildNodeString(nodeNames.get(discoveredActivity), discoveredActivity.getActivityFullName(), activityToUnaryConstraints.get(discoveredActivity.getActivityFullName()), discoveredActivity.getActivitySupport(), alternativeLayout));

		for (String string : edges)
			sb.append(string);

		sb.append("}");
		
		return sb.toString();
	}

	public static String createAutomatonVisualizationString(List<DiscoveredActivity> filteredActivities, List<DiscoveredConstraint> filteredConstraints, boolean alternativeLayout) {
		ProcessModel pm = ProcessModelGenerator.obtainProcessModel(filteredActivities, filteredConstraints);
		ConstraintsPrinter cPrin = new ConstraintsPrinter(pm);
		String dotString = cPrin.printDotAutomaton().replace("\n", "");
		if (alternativeLayout) {
			dotString = dotString.replaceFirst("rankdir = LR;", "ranksep = \"1\" ");
		}
		return dotString;
	}

	public static String createAutomatonVisualizationString(TreeItem<TreeDataBase> activitiesRoot, ObservableList<ConstraintDataRow> constraintDataRows) {
		ProcessModel pm = ProcessModelGenerator.obtainProcessModel(activitiesRoot, constraintDataRows);
		ConstraintsPrinter cPrin = new ConstraintsPrinter(pm);
		String dotString = cPrin.printDotAutomaton().replace("\n", "");
		return dotString;
	}

	public static String createActivitiesTextualString(List<DiscoveredActivity> filteredActivities) {
		StringBuilder sbActivity = new StringBuilder();

		for (int i = 0; i < filteredActivities.size(); i++) {
			DiscoveredActivity activity = filteredActivities.get(i);
			String addIt = String.format("%d) %s : Exists in %.2f%% of traces in the log\\n", i+1, activity.getActivityFullName(), activity.getActivitySupport()*100);
			sbActivity.append(addIt);
		}

		return sbActivity.toString();
	}

	public static String createActivitiesTextualString(TreeItem<TreeDataBase> activitiesRoot) {
		StringBuilder sbActivity = new StringBuilder();

		for (int i = 0; i < activitiesRoot.getChildren().size(); i++) {
			TreeDataActivity treeDataActivity = (TreeDataActivity) activitiesRoot.getChildren().get(i);
			String addIt = String.format("%d) %s\\n", i+1, treeDataActivity.getActivityName());
			sbActivity.append(addIt);
			for (TreeDataAttribute treeDataAttribute : treeDataActivity.getAttributesUnmodifiable()) {
				sbActivity.append("<p style = \"margin-left : 30px;\">");
				sbActivity.append(treeDataAttribute.getAttributeName() + " : " + treeDataAttribute.getAttributeType().getDisplayText());
				switch(treeDataAttribute.getAttributeType()) {
				case INTEGER: //Fall through intended
				case FLOAT:
					sbActivity.append(" [" + treeDataAttribute.getValueFrom().toString() + ", " + treeDataAttribute.getValueTo().toString() + "]");
					break;
				case ENUMERATION:
					sbActivity.append(" {" + String.join(", ", treeDataAttribute.getPossibleValues()) + "}");
					break;
				default:
					break;
				}
				sbActivity.append("</p>\\n");
			}
		}

		return sbActivity.toString();
	}

	public static String createConstraintsTextualString(List<DiscoveredConstraint> filteredConstraints) {
		StringBuilder sbConstraint = new StringBuilder();

		for (int i=0; i < filteredConstraints.size(); i++) {
			DiscoveredConstraint constraint = filteredConstraints.get(i);
			Rule dataCond = constraint.getDataCondition();
			boolean isBinary = constraint.getTemplate().getIsBinary();
			
			String fulfillmentOrViolationSentence = "";
			String actSentence = constraint.getActivationActivity().getActivityFullName();
			String trgSentence = isBinary ? trgSentence = constraint.getTargetActivity().getActivityFullName() : "";
			
			if (dataCond != null) {	// Data-aware discovery
				actSentence += " with condition &#12296;" + dataCond.getAntecedents().toTextualString() + "&#12297;";
				
				if (dataCond.getConsequents().equals(Predicate.fulfillmentPredicate))
					fulfillmentOrViolationSentence = "it is true that ";
				else if (dataCond.getConsequents().equals(Predicate.violationPredicate))
					fulfillmentOrViolationSentence = "it is false that ";
				else
					trgSentence += " with condition &#12296;" + dataCond.getConsequents().toTextualString() + "&#12297;";
			}
			
			String description;
			if (isBinary)
				description = TemplateDescription.get(constraint.getTemplate(), actSentence, trgSentence);
			else
				description = TemplateDescription.get(constraint.getTemplate(), actSentence);
			
			float support = constraint.getConstraintSupport();
			
			sbConstraint.append(String.format("%d) In %.2f%% of cases in the log, %s", i+1, support*100, fulfillmentOrViolationSentence+description));
			sbConstraint.append("\\n");
		}

		return sbConstraint.toString();
	}

	public static String createConstraintsTextualString(ObservableList<ConstraintDataRow> constraintDataRows) {
		StringBuilder sbConstraint = new StringBuilder();

		for (int i = 0; i < constraintDataRows.size(); i++) {
			ConstraintDataRow constraintDataRow = constraintDataRows.get(i);
			
			if (constraintDataRow.getRowStatus() == RowStatus.NEW) {
				continue;
			}
			
			String exp;
			if (constraintDataRow.getTemplate().getIsBinary()) {
				if (constraintDataRow.getTemplate().getReverseActivationTarget()) {
					exp = TemplateDescription.get(constraintDataRow.getTemplate(), constraintDataRow.getTargetActivity().getActivityName(), constraintDataRow.getActivationActivity().getActivityName());
				} else {
					exp = TemplateDescription.get(constraintDataRow.getTemplate(), constraintDataRow.getActivationActivity().getActivityName(), constraintDataRow.getTargetActivity().getActivityName());
				}
			} else {
				exp = TemplateDescription.get(constraintDataRow.getTemplate(), constraintDataRow.getActivationActivity().getActivityName());
			}
			String addIt = String.format("%d) %s\\n", i+1, exp);
			sbConstraint.append(addIt);

			if (constraintDataRow.getActivationCondition() != null && constraintDataRow.getActivationCondition().strip().length() != 0) {
				sbConstraint.append("<p style = \"margin-left : 30px;\">");
				sbConstraint.append("Activation condition: " + constraintDataRow.getActivationCondition());
				sbConstraint.append("</p>\\n");
			}

			if (constraintDataRow.getCorrelationCondition() != null && constraintDataRow.getCorrelationCondition().strip().length() != 0) {
				sbConstraint.append("<p style = \"margin-left : 30px;\">");
				sbConstraint.append("Correlation condition: " + constraintDataRow.getCorrelationCondition());
				sbConstraint.append("</p>\\n");
			}

			if (constraintDataRow.getTimeCondition() != null && constraintDataRow.getTimeCondition().strip().length() != 0) {
				sbConstraint.append("<p style = \"margin-left : 30px;\">");
				sbConstraint.append("Time condition: " + constraintDataRow.getTimeCondition());
				sbConstraint.append("</p>\\n");
			}
		}

		return sbConstraint.toString();
	}

}
