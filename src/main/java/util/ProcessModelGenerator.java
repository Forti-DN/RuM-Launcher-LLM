package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import controller.discovery.data.DiscoveredActivity;
import controller.discovery.data.DiscoveredConstraint;
import controller.editor.data.ConstraintDataRow;
import datatable.AbstractDataRow.RowStatus;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import minerful.concept.ProcessModel;
import minerful.concept.TaskChar;
import minerful.concept.TaskCharArchive;
import minerful.concept.TaskCharFactory;
import minerful.concept.constraint.Constraint;
import minerful.concept.constraint.ConstraintsBag;
import treedata.TreeDataActivity;
import treedata.TreeDataBase;

public class ProcessModelGenerator {
	
	public static ProcessModel obtainProcessModel(
			Map<Integer, String> activitiesMap,
			Map<Integer, ConstraintTemplate> templatesMap,
			Map<Integer, List<String>> constraintParametersMap) {
		
		List<String> allActivitiesInvolved = new ArrayList<>(activitiesMap.values());
		TaskCharFactory tChFactory = new TaskCharFactory();
		
		List<TaskChar> tcList = allActivitiesInvolved.stream().map(activity -> tChFactory.makeTaskChar(activity)).collect(Collectors.toList());
		TaskChar[] tcArray = (TaskChar[]) tcList.toArray(new TaskChar[tcList.size()]); 
		TaskCharArchive taChaAr = new TaskCharArchive(tcArray);
		ConstraintsBag bag = new ConstraintsBag(taChaAr.getTaskChars());
		Map<Integer,List<String>> constraintsMap = constraintParametersMap;
		for(int k:constraintsMap.keySet()) {
			ConstraintTemplate t = templatesMap.get(k);
			List<String> involvedActivities = constraintsMap.get(k);
			List<TaskChar> involved = involvedActivities.stream().map(activity -> taChaAr.getTaskChar(activity)).collect(Collectors.toList());
			Constraint constraint = ConstraintUtils.getMinerfulConstraint(t, involved);
			if(constraint != null) bag.add(constraint);
		}
		ProcessModel proMod = new ProcessModel(taChaAr, bag);
		return proMod;
	}
	
	public static ProcessModel obtainProcessModel(List<DiscoveredActivity> filteredActivities, List<DiscoveredConstraint> filteredConstraints) {
		List<TaskChar> tcList = new ArrayList<TaskChar>();
		TaskCharFactory tChFactory = new TaskCharFactory();
		
		for (DiscoveredActivity discoveredActivity : filteredActivities) {
			tcList.add(tChFactory.makeTaskChar(discoveredActivity.getActivityFullName()));
		}
		
		TaskChar[] tcArray = tcList.toArray(new TaskChar[tcList.size()]); 
		TaskCharArchive taChaAr = new TaskCharArchive(tcArray);
		ConstraintsBag bag = new ConstraintsBag(taChaAr.getTaskChars());
		
		for (DiscoveredConstraint discoveredConstraint : filteredConstraints) {
			ConstraintTemplate t = ConstraintTemplate.getByTemplateName( discoveredConstraint.getTemplate().toString() );
			List<String> involvedActivities = new ArrayList<String>();
			
			involvedActivities.add(discoveredConstraint.getActivationActivity().getActivityFullName());
			if (discoveredConstraint.getTemplate().getIsBinary()) {
				involvedActivities.add(discoveredConstraint.getTargetActivity().getActivityFullName());
			}
			List<TaskChar> involved = involvedActivities.stream().map(activity -> taChaAr.getTaskChar(activity)).collect(Collectors.toList());
			Constraint constraint = ConstraintUtils.getMinerfulConstraint(t, involved);
			if(constraint != null) bag.add(constraint);
		}
		
		ProcessModel proMod = new ProcessModel(taChaAr, bag);
		return proMod;
	}

	//TODO: Should try to unify with obtainProcessModel(List<DiscoveredActivity> filteredActivities, List<DiscoveredConstraint> filteredConstraints)
	public static ProcessModel obtainProcessModel(TreeItem<TreeDataBase> activitiesRoot, ObservableList<ConstraintDataRow> constraintDataRows) {
		List<TaskChar> tcList = new ArrayList<TaskChar>();
		TaskCharFactory tChFactory = new TaskCharFactory();
		
		for (TreeItem<TreeDataBase> treeItem : activitiesRoot.getChildren()) {
			TreeDataActivity treeDataActivity = (TreeDataActivity) treeItem;
			tcList.add(tChFactory.makeTaskChar(treeDataActivity.getActivityName()));
		}
		
		TaskChar[] tcArray = tcList.toArray(new TaskChar[tcList.size()]); 
		TaskCharArchive taChaAr = new TaskCharArchive(tcArray);
		ConstraintsBag bag = new ConstraintsBag(taChaAr.getTaskChars());
		
		for (ConstraintDataRow constraintDataRow : constraintDataRows) {
			
			if (constraintDataRow.getRowStatus() == RowStatus.NEW) {
				continue;
			}
			
			ConstraintTemplate t = ConstraintTemplate.getByTemplateName( constraintDataRow.getTemplate().toString() );
			List<String> involvedActivities = new ArrayList<String>();
			
			if (constraintDataRow.getTemplate().getIsBinary()) {
				if (constraintDataRow.getTemplate().getReverseActivationTarget()) {
					involvedActivities.add(constraintDataRow.getTargetActivity().getActivityName());
					involvedActivities.add(constraintDataRow.getActivationActivity().getActivityName());
				} else {
					involvedActivities.add(constraintDataRow.getActivationActivity().getActivityName());
					involvedActivities.add(constraintDataRow.getTargetActivity().getActivityName());
				}
			} else {
				involvedActivities.add(constraintDataRow.getActivationActivity().getActivityName());
			}
			List<TaskChar> involved = involvedActivities.stream().map(activity -> taChaAr.getTaskChar(activity)).collect(Collectors.toList());
			Constraint constraint = ConstraintUtils.getMinerfulConstraint(t, involved);
			if(constraint != null) bag.add(constraint);
		}
		
		ProcessModel proMod = new ProcessModel(taChaAr, bag);
		return proMod;
	}

}
