package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import controller.discovery.data.DiscoveredActivity;
import controller.discovery.data.DiscoveredConstraint;
import controller.editor.data.ConstraintDataRow;
import datatable.AbstractDataRow.RowStatus;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import minerful.MinerFulOutputManagementLauncher;
import minerful.concept.ProcessModel;
import minerful.io.ConstraintsPrinter;
import minerful.io.params.OutputModelParameters;
import task.conformance.ConformanceStatisticType;
import treedata.TreeDataActivity;
import treedata.TreeDataAttribute;
import treedata.TreeDataBase;

public class ModelExporter {

	public static String getTextString(
			Map<Integer, String> activitiesMap,
			Map<Integer, Double> actSuppMap,
			Map<Integer, ConstraintTemplate> templatesMap,
			Map<Integer, List<String>> constraintParametersMap,
			Map<Integer, Double> constraintSuppMap) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("Activities:\n");
		int index = 1;
		for (int k : activitiesMap.keySet()) {
			double s = actSuppMap.get(k);
			String act = activitiesMap.get(k);
			String addIt = String.format("%d) %s : Exists in %.2f%% of traces in the log\n", index, act, s*100);
			sb.append(addIt);
			index++;
		}
		
		sb.append("Constraints:\n");
		index = 1;
		for (int k : templatesMap.keySet()) {
			double s = constraintSuppMap.get(k);
			List<String> params = constraintParametersMap.get(k);
			String[] p = (String[]) params.toArray(new String[params.size()]);
			String exp = TemplateDescription.get(templatesMap.get(k), p);
			String addIt = String.format("%d) In %.2f%% of traces in the log, %s\n", index, s*100, exp);
			sb.append(addIt);
			index++;
		}
		
		return sb.toString();
	}

	public static String getTextString(List<DiscoveredActivity> filteredActivities, List<DiscoveredConstraint> filteredConstraints) {
		StringBuilder sb = new StringBuilder();
		sb.append("Activities:\n");
		for (int i = 0; i < filteredActivities.size(); i++) {
			DiscoveredActivity activity = filteredActivities.get(i);
			String addIt = String.format("%d) %s : Exists in %.2f%% of traces in the log\n", i+1, activity.getActivityFullName(), activity.getActivitySupport()*100);
			sb.append(addIt);
		}

		sb.append("Constraints:\n");
		for (int i = 0; i < filteredConstraints.size(); i++) {
			DiscoveredConstraint constraint = filteredConstraints.get(i);
			String exp;
			if (constraint.getTemplate().getIsBinary()) {
				exp = TemplateDescription.get(constraint.getTemplate(), constraint.getActivationActivity().getActivityFullName(), constraint.getTargetActivity().getActivityFullName());
			} else {
				exp = TemplateDescription.get(constraint.getTemplate(), constraint.getActivationActivity().getActivityFullName());
			}
			String addIt = String.format("%d) In %.2f%% of traces in the log, %s\n", i+1, constraint.getConstraintSupport()*100, exp);
			sb.append(addIt);

			if (filteredConstraints.get(i).getDataCondition() != null) {
				sb.append("\t");
				sb.append(filteredConstraints.get(i).getDataCondition().toString());
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	public static String getTextString(TreeItem<TreeDataBase> activitiesRoot, ObservableList<ConstraintDataRow> constraintDataRows) {
		StringBuilder sb = new StringBuilder();

		sb.append("Activities:\n");
		for (int i = 0; i < activitiesRoot.getChildren().size(); i++) {
			TreeDataActivity treeDataActivity = (TreeDataActivity) activitiesRoot.getChildren().get(i);
			String addIt = String.format("%d) %s\n", i+1, treeDataActivity.getActivityName());
			sb.append(addIt);
			
			for (TreeDataAttribute treeDataAttribute : treeDataActivity.getAttributesUnmodifiable()) {
				sb.append("\t" + treeDataAttribute.getAttributeName() + " : " + treeDataAttribute.getAttributeType().getDisplayText());
				switch(treeDataAttribute.getAttributeType()) {
				case INTEGER: //Fall through intended
				case FLOAT:
					sb.append(" [" + treeDataAttribute.getValueFrom().toString() + ", " + treeDataAttribute.getValueTo().toString() + "]");
					break;
				case ENUMERATION:
					sb.append(" {" + String.join(", ", treeDataAttribute.getPossibleValues()) + "}");
					break;
				default:
					break;
				}
				sb.append("\n");
			}
		}

		if (!constraintDataRows.isEmpty()) {
			sb.append("Constraints:\n");
			for (int i = 0; i < constraintDataRows.size(); i++) {
				ConstraintDataRow constraint = constraintDataRows.get(i);
				
				String exp;
				if (constraint.getTemplate().getIsBinary())
					exp = TemplateDescription.get(constraint.getTemplate(), constraint.getActivationActivity().getActivityName(), constraint.getTargetActivity().getActivityName());
				else
					exp = TemplateDescription.get(constraint.getTemplate(), constraint.getActivationActivity().getActivityName());
				
				String addIt = String.format("%d) %s\n", i+1, exp);
				sb.append(addIt);
				if (constraint.getActivationCondition() != null && constraint.getActivationCondition().strip().length() != 0) {
					sb.append("\tActivation condition: " + constraint.getActivationCondition() + "\n");
				}
				if (constraint.getCorrelationCondition() != null && constraint.getCorrelationCondition().strip().length() != 0) {
					sb.append("\tCorrelation condition: " + constraint.getCorrelationCondition() + "\n");
				}
				if (constraint.getTimeCondition() != null && constraint.getTimeCondition().strip().length() != 0) {
					sb.append("\tTime condition: " + constraint.getTimeCondition() + "\n");
				}
			}
		}

		return sb.toString();
	}

	public static String getDotString(
			Map<Integer, String> activitiesMap,
			Map<Integer, ConstraintTemplate> templatesMap,
			Map<Integer, List<String>> constraintParametersMap) {
		
		ProcessModel pm = ProcessModelGenerator.obtainProcessModel(activitiesMap, templatesMap, constraintParametersMap);
		ConstraintsPrinter cPrin = new ConstraintsPrinter(pm);
		String dotRep = cPrin.printDotAutomaton().replace("\n", "");
		return dotRep;
	}

	public static boolean exportAutomaton(List<DiscoveredActivity> filteredActivities, List<DiscoveredConstraint> filteredConstraints, File outputFile) {
		//Extra checks for outputFile added because MinerFulOutputManagementLauncher fails silently if FileNotFoundException occurs
		if (!outputFile.exists()) {
			try {
				outputFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		if (outputFile.canWrite()) {
			ProcessModel pm = ProcessModelGenerator.obtainProcessModel(filteredActivities, filteredConstraints);
			OutputModelParameters outParams = new OutputModelParameters();
			outParams.fileToSaveDotFileForAutomaton = outputFile;
			new MinerFulOutputManagementLauncher().manageOutput(pm, outParams);
			return true;
		} else {
			return false;
		}
	}

	public static boolean exportAutomaton(TreeItem<TreeDataBase> activitiesRoot, ObservableList<ConstraintDataRow> constraintDataRows, File outputFile) {
		//Extra checks for outputFile added because MinerFulOutputManagementLauncher fails silently if FileNotFoundException occurs
		if (!outputFile.exists()) {
			try {
				outputFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		if (outputFile.canWrite()) {
			ProcessModel pm = ProcessModelGenerator.obtainProcessModel(activitiesRoot, constraintDataRows);
			OutputModelParameters outParams = new OutputModelParameters();
			outParams.fileToSaveDotFileForAutomaton = outputFile;
			new MinerFulOutputManagementLauncher().manageOutput(pm, outParams);
			return true;
		} else {
			return false;
		}
	}

	public static String getDeclString(
			Map<Integer, String> activitiesMap,
			Map<Integer, ConstraintTemplate> templatesMap,
			Map<Integer, List<String>> constraintParametersMap) {
		
		StringBuilder sb = new StringBuilder();
		
		for (String activity : activitiesMap.values()) {
			String addIt = String.format("activity %s\n", activity);
			sb.append(addIt);
		}
		
		for(Map.Entry<Integer, ConstraintTemplate> e : templatesMap.entrySet()) {
			int k = e.getKey();
			ConstraintTemplate t = e.getValue();
			
			List<String> params = constraintParametersMap.get(k);
			if (params.size() == 1) {
				String addIt = String.format("%s[%s] | |\n", t.toString(), params.get(0));
				sb.append(addIt);
			
			} else if (params.size() == 2) {
				String addIt = String.format("%s[%s, %s] | | |\n", t.toString(), params.get(0), params.get(1));
				sb.append(addIt);
			}
		}
		
		return sb.toString();
	}

	public static String getDeclString(File logFile, List<DiscoveredActivity> filteredActivities, List<DiscoveredConstraint> filteredConstraints) {
		StringBuilder sb = new StringBuilder();

		Set<String> allAttributes = new HashSet<>();
		for (DiscoveredActivity discoveredActivity : filteredActivities) {
			sb.append("activity " + discoveredActivity.getActivityFullName() + "\n");
			
			if (discoveredActivity.getPayloadAttributes() != null && !discoveredActivity.getPayloadAttributes().isEmpty()) {
				sb.append("bind "+ discoveredActivity.getActivityFullName() + ": " + String.join(", ", discoveredActivity.getPayloadAttributes()) + "\n");
				allAttributes.addAll(discoveredActivity.getPayloadAttributes());
			}
		}
		
		XLog log = LogUtils.convertToXlog(logFile);
		
		for (String attribute : allAttributes) {
			Class<? extends XAttribute> attClass = null;
			Set<String> values = new HashSet<>();
			
			for (XTrace trace : log)
				for (XEvent evt : trace)
					for (XAttribute att : evt.getAttributes().values())
						if (att.getKey().equals(attribute)) {
							attClass = att.getClass();
							values.add(att.toString());
						}

			if (attClass != null) {		
				String str;
				
				if (XAttributeDiscrete.class.isAssignableFrom(attClass)) {
					List<Integer> intValues = values.stream().map(val -> Integer.parseInt(val)).collect(Collectors.toList());
					str = "integer between " + Collections.min(intValues) + " and " + Collections.max(intValues);
				
				} else if (XAttributeContinuous.class.isAssignableFrom(attClass)) {
					List<Float> floatValues = values.stream().map(val -> Float.parseFloat(val)).collect(Collectors.toList());
					str = "float between " + Collections.min(floatValues) + " and " + Collections.max(floatValues);
				
				} else {
					str = String.join(", ", values);
				}
				
				sb.append(attribute + ": " + str + "\n");
			}
		}
		
		for (DiscoveredConstraint discoveredConstraint : filteredConstraints)
			sb.append(ConstraintUtils.getConstraintString(discoveredConstraint) + "\n");
		
		
		return sb.toString();
	}

	public static String getDeclString(TreeItem<TreeDataBase> activitiesRoot, ObservableList<TreeDataAttribute> allAttributes, ObservableList<ConstraintDataRow> constraintDataRows) {
		StringBuilder sb = new StringBuilder();

		for (TreeItem<TreeDataBase> treeItem : activitiesRoot.getChildren()) {
			TreeDataActivity treeDataActivity = (TreeDataActivity) treeItem;
			sb.append("activity " + treeDataActivity.getActivityName() + "\n");
			if (!treeDataActivity.getAttributesUnmodifiable().isEmpty()) {
				sb.append("bind " + treeDataActivity.getActivityName() + ": ");
				sb.append(String.join(", ", treeDataActivity.getAttributesUnmodifiable().stream().map(item -> item.getAttributeName()).collect(Collectors.toList())) + "\n");
			}
		}
		for (TreeDataAttribute treeDataAttribute : allAttributes) {
			sb.append(treeDataAttribute.getAttributeName() + ": ");
			switch(treeDataAttribute.getAttributeType()) {
			case INTEGER:
				sb.append("integer between " + treeDataAttribute.getValueFrom().toString() + " and " + treeDataAttribute.getValueTo().toString() + "\n");
				break;
			case FLOAT:
				sb.append("float between " + treeDataAttribute.getValueFrom().toString() + " and " + treeDataAttribute.getValueTo().toString() + "\n");
				break;
			case ENUMERATION:
				sb.append(String.join(", ", treeDataAttribute.getPossibleValues()) + "\n");
				break;
			default:
				sb.append("\n");
				break;
			}
		}
		for (ConstraintDataRow constraintDataRow : constraintDataRows) {
			if (constraintDataRow.getRowStatus() == RowStatus.SAVED) {
				sb.append(ConstraintUtils.getConstraintString(constraintDataRow, true) + "\n");
			}
		}
		return sb.toString();
	}
	
	public static String getXmlString(List<DiscoveredActivity> filteredActivities, List<DiscoveredConstraint> filteredConstraints) {
		List<String> activityList = new ArrayList<>();
		List<String> constraintList = new ArrayList<>();
		
		for (DiscoveredActivity activity : filteredActivities) {
			activityList.add(activity.getActivityFullName());
		}
		for (DiscoveredConstraint constraint : filteredConstraints) {
			constraintList.add(ConstraintUtils.getConstraintString(constraint));
		}
		
		return ModelUtils.buildXmlModelString(activityList, constraintList);
	}
	
	public static String getXmlString(TreeItem<TreeDataBase> activitiesRoot, ObservableList<TreeDataAttribute> allAttributes, ObservableList<ConstraintDataRow> constraintDataRows) {
		List<String> activityList = new ArrayList<>();
		List<String> constraintList = new ArrayList<>();

		
		for (TreeItem<TreeDataBase> treeItem : activitiesRoot.getChildren()) {
			TreeDataActivity treeDataActivity = (TreeDataActivity) treeItem;
			activityList.add(treeDataActivity.getActivityName());
		}
		
		for (ConstraintDataRow constraintDataRow : constraintDataRows) {
			if (constraintDataRow.getRowStatus() == RowStatus.SAVED) {
				constraintList.add(ConstraintUtils.getConstraintString(constraintDataRow, true));
			}
		}
		
		return ModelUtils.buildXmlModelString(activityList, constraintList);
	}
	
	public static String getConformanceStatisticsAsCsv(List<Map<ConformanceStatisticType, String>> statistics) {
		StringBuilder sb = new StringBuilder();
		
		// The first row contains the labels related to statistics
		sb.append( String.join(";", statistics.get(0).keySet().stream()
												.map(ConformanceStatisticType::getDisplayText)
												.collect(Collectors.toList())) );
		sb.append("\n");
		
		for (Map<ConformanceStatisticType, String> stat : statistics) {
			Collection<String> values = new ArrayList<>();
			
			for (String s : stat.values()) { // Deletes the initial prefix from the trace name string
				if (s.startsWith("Trace ID: "))
					s = s.substring("Trace ID: ".length());
				
				values.add(s);
			}
			
			sb.append( String.join(";", values) + "\n" );
		}
		
		return sb.toString();
	}

	public static String getConformanceReportAsCsv(List<Map<String, String>> reportData) {
		StringBuilder sb = new StringBuilder();
		
		// The first row contains the labels related to statistics
		sb.append( String.join(";", reportData.get(0).keySet().stream().collect(Collectors.toList())) );
		sb.append("\n");
		
		
		for (Map<String, String> stat : reportData) {
			Collection<String> values = new ArrayList<>();
			
			for (String s : stat.values()) { // Deletes the initial prefix from the trace name string
				values.add(s);
			}
			
			sb.append( String.join(";", values) + "\n" );
		}
		
		return sb.toString();
	}
}
