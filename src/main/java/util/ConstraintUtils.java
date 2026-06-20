package util;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.declareminer.DeclareMiner;
import org.processmining.plugins.declareminer.DeclareMinerInput;
import org.processmining.plugins.declareminer.enumtypes.AprioriKnowledgeBasedCriteria;
import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;
import org.processmining.plugins.declareminer.templates.*;
import org.processmining.plugins.declareminer.visualizing.ActivityDefinition;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;
import org.processmining.plugins.declareminer.visualizing.Parameter;

import controller.discovery.DataConditionType;
import controller.discovery.data.DiscoveredActivity;
import controller.discovery.data.DiscoveredConstraint;
import controller.editor.data.ConstraintDataRow;
import datatable.AbstractDataRow.RowStatus;
import minerful.concept.ProcessModel;
import minerful.concept.TaskChar;
import minerful.concept.constraint.Constraint;
import minerful.concept.constraint.existence.*;
import minerful.concept.constraint.relation.*;
import task.discovery.data.AttributeOperator;
import task.discovery.data.AttributePredicate;
import task.discovery.data.LogicalOperator;
import task.discovery.data.LogicalPredicate;
import task.discovery.data.Predicate;
import task.discovery.mp_enhancer.Rule;
import task.discovery.mp_enhancer.RulesExtractor;
import treedata.TreeDataActivity;

public final class ConstraintUtils {
	
	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	// These variables were made global to speed-up Time Distance computation, otherwise they had to be reinitialized many times
	private static DeclareMinerInput dmi;
	private static Map<DeclareTemplate, org.processmining.plugins.declareminer.visualizing.ConstraintTemplate> map;
	
	//Private constructor to avoid unnecessary instantiation of the class
	private ConstraintUtils() {
	}
	
	public static ConstraintTemplate getTemplateFromDeclare(DeclareTemplate dt) {
		switch(dt) {
		case Absence:
			return ConstraintTemplate.Absence;
		case Absence2:
			return ConstraintTemplate.Absence2;
		case Absence3:
			return ConstraintTemplate.Absence3;
		case Alternate_Precedence:
			return ConstraintTemplate.Alternate_Precedence;
		case Alternate_Response:
			return ConstraintTemplate.Alternate_Response;
		case Alternate_Succession:
			return ConstraintTemplate.Alternate_Succession;
		case Chain_Precedence:
			return ConstraintTemplate.Chain_Precedence;
		case Chain_Response:
			return ConstraintTemplate.Chain_Response;
		case Chain_Succession:
			return ConstraintTemplate.Chain_Succession;
		case Choice:
			return ConstraintTemplate.Choice;
		case CoExistence:
			return ConstraintTemplate.CoExistence;
		case Exactly1:
			return ConstraintTemplate.Exactly1;
		case Exactly2:
			return ConstraintTemplate.Exactly2;
		case Exclusive_Choice:
			return ConstraintTemplate.Exclusive_Choice;
		case Existence:
			return ConstraintTemplate.Existence;
		case Existence2:
			return ConstraintTemplate.Existence2;
		case Existence3:
			return ConstraintTemplate.Existence3;
		case Init:
			return ConstraintTemplate.Init;
		case Not_Chain_Succession:
			return ConstraintTemplate.Not_Chain_Succession;
		case Not_CoExistence:
			return ConstraintTemplate.Not_CoExistence;
		case Not_Succession:
			return ConstraintTemplate.Not_Succession;
		case Precedence:
			return ConstraintTemplate.Precedence;
		case Responded_Existence:
			return ConstraintTemplate.Responded_Existence;
		case Response:
			return ConstraintTemplate.Response;
		case Succession:
			return ConstraintTemplate.Succession;
		default:
			throw new NoSuchElementException("Template " + dt + " not yet supported in RuM.");
		}
	}
	
	public static ConstraintTemplate getTemplateFromMinerful(Constraint c) {
		switch(c.getTemplateName()) {
		case "AtLeast1":
			return ConstraintTemplate.Existence;
		case "AtLeast2":
			return ConstraintTemplate.Existence2;
		case "AtLeast3":
			return ConstraintTemplate.Existence3;
		case "Exactly1":
			return ConstraintTemplate.Exactly1;
		case "Exactly2":
			return ConstraintTemplate.Exactly2;
		case "End":
			return ConstraintTemplate.End;
		case "Init":
			return ConstraintTemplate.Init;
		case "Absence":
			return ConstraintTemplate.Absence;
		case "AtMost1":
			return ConstraintTemplate.Absence2;
		case "AtMost2":
			return ConstraintTemplate.Absence3;
		case "Choice":
			return ConstraintTemplate.Choice;
		case "CoExistence":
			return ConstraintTemplate.CoExistence;
		case "Succession":
			return ConstraintTemplate.Succession;
		case "AlternateSuccession":
			return ConstraintTemplate.Alternate_Succession;
		case "ChainSuccession":
			return ConstraintTemplate.Chain_Succession;
		case "ExclusiveChoice":
			return ConstraintTemplate.Exclusive_Choice;
		case "NotChainSuccession":
			return ConstraintTemplate.Not_Chain_Succession;
		case "NotSuccession":
			return ConstraintTemplate.Not_Succession;
		case "NotCoExistence":
			return ConstraintTemplate.Not_CoExistence;
		case "RespondedExistence":
			return ConstraintTemplate.Responded_Existence;
		case "Precedence":
			return ConstraintTemplate.Precedence;
		case "AlternatePrecedence":
			return ConstraintTemplate.Alternate_Precedence;
		case "ChainPrecedence":
			return ConstraintTemplate.Chain_Precedence;
		case "Response":
			return ConstraintTemplate.Response;
		case "AlternateResponse":
			return ConstraintTemplate.Alternate_Response;
		case "ChainResponse":
			return ConstraintTemplate.Chain_Response;
		default:
			throw new NoSuchElementException("Template: " + c.getTemplateName() + "not yet implemented in RuM.");
		}
	}
	
	public static Constraint getMinerfulConstraint(ConstraintTemplate template, List<TaskChar> involved) {
		switch (template) {
		case Absence:
			return new Absence(involved.get(0));
		case Absence2:
			return new AtMost1(involved.get(0));
		case Absence3:
			return new AtMost2(involved.get(0));
		case Alternate_Precedence:
			return new AlternatePrecedence(involved.get(0), involved.get(1));
		case Alternate_Response:
			return new AlternateResponse(involved.get(0), involved.get(1));
		case Alternate_Succession:
			return new AlternateSuccession(involved.get(0), involved.get(1));
		case Chain_Precedence:
			return new ChainPrecedence(involved.get(0), involved.get(1));
		case Chain_Response:
			return new ChainResponse(involved.get(0), involved.get(1));
		case Chain_Succession:
			return new ChainSuccession(involved.get(0), involved.get(1));
		case Choice:
			return new Choice(involved.get(0), involved.get(1));
		case CoExistence:
			return new CoExistence(involved.get(0), involved.get(1));
		case End:
			return new End(involved.get(0));
		case Exactly1:
			return new Exactly1(involved.get(0));
		case Exactly2:
			return new Exactly2(involved.get(0));
		case Exclusive_Choice:
			return new ExclusiveChoice(involved.get(0), involved.get(1));
		case Existence:
			return new AtLeast1(involved.get(0));
		case Existence2:
			return new AtLeast2(involved.get(0));
		case Existence3:
			return new AtLeast3(involved.get(0));
		case Init:
			return new Init(involved.get(0));
		case Not_Chain_Succession:
			return new NotChainSuccession(involved.get(0), involved.get(1));
		case Not_CoExistence:
			return new NotCoExistence(involved.get(0), involved.get(1));
		case Not_Succession:
			return new NotSuccession(involved.get(0), involved.get(1));
		case Precedence:
			return new Precedence(involved.get(0), involved.get(1));
		case Responded_Existence:
			return new RespondedExistence(involved.get(0),involved.get(1));
		case Response:
			return new Response(involved.get(0), involved.get(1));
		case Succession:
			return new Succession(involved.get(0), involved.get(1));
		default:
			return null;
		}
	}
	
	//TODO: Review the code
	public static ConstraintDataRow getConstraintDataRow(String constraint, List<TreeDataActivity> activityRows) {

		ConstraintTemplate template = null;
		TreeDataActivity activationActivity = null;
		TreeDataActivity targetActivity = null;

		String activationCondition = null;
		String targetCondition = null;
		String timeCondition = null;

		Matcher mBinary = Pattern.compile("(.*)\\[(.*), (.*)\\] \\|(.*) \\|(.*) \\|(.*)").matcher(constraint);
		Matcher mUnary = Pattern.compile(".*\\[(.*)\\] \\|(.*) \\|(.*)").matcher(constraint);
		if(mBinary.find()) {
			template = ConstraintTemplate.getByTemplateName(mBinary.group(1));
			String a = mBinary.group(2);
			String t = mBinary.group(3);
			String ac = mBinary.group(4);
			String cc = mBinary.group(5);
			String tc = mBinary.group(6);
			if(template.getReverseActivationTarget()) {
				targetActivity = getActivityDataRowFromName(activityRows, a);
				targetCondition = cc;
				activationActivity = getActivityDataRowFromName(activityRows, t);
				activationCondition = ac;
				timeCondition = tc;
			}
			else {
				activationActivity = getActivityDataRowFromName(activityRows, a);
				activationCondition = ac;
				targetActivity = getActivityDataRowFromName(activityRows, t);
				targetCondition = cc;
				timeCondition = tc;
			}
		}
		else if(mUnary.find()) {
			template = ConstraintTemplate.getByTemplateName(mUnary.group(0).substring(0, mUnary.group(0).indexOf("["))); //TODO: Should be done more intelligently
			activationActivity = getActivityDataRowFromName(activityRows, mUnary.group(1));
			activationCondition = mUnary.group(2);
			timeCondition = mUnary.group(3);
		}

		ConstraintDataRow constraintRow = new ConstraintDataRow(template, activationActivity, activationCondition, targetActivity, targetCondition, timeCondition);
		constraintRow.validateRowEdit();
		constraintRow.confirmRowEdit();
		return constraintRow;
	}

	public static ConstraintDataRow getConstraintDataRow(controller.editor.Constraint constraint, List<TreeDataActivity> activityRows) {

		ConstraintTemplate template = null;
		TreeDataActivity activationActivity = null;
		TreeDataActivity targetActivity = null;

		String activationCondition = null;
		String targetCondition = null;
		String timeCondition = null;

//		Matcher mBinary = Pattern.compile("(.*)\\[(.*), (.*)\\] \\|(.*) \\|(.*) \\|(.*)").matcher(constraint);
//		Matcher mUnary = Pattern.compile(".*\\[(.*)\\] \\|(.*) \\|(.*)").matcher(constraint);

//		if(mBinary.find()) {
//			template = ConstraintTemplate.getByTemplateName(mBinary.group(1));
//			String a = mBinary.group(2);
//			String t = mBinary.group(3);
//			String ac = mBinary.group(4);
//			String cc = mBinary.group(5);
//			String tc = mBinary.group(6);
//			if(template.getReverseActivationTarget()) {
//				targetActivity = getActivityDataRowFromName(activityRows, a);
//				targetCondition = cc;
//				activationActivity = getActivityDataRowFromName(activityRows, t);
//				activationCondition = ac;
//				timeCondition = tc;
//			}
//			else {
//				activationActivity = getActivityDataRowFromName(activityRows, a);
//				activationCondition = ac;
//				targetActivity = getActivityDataRowFromName(activityRows, t);
//				targetCondition = cc;
//				timeCondition = tc;
//			}
//		}
//		else if(mUnary.find()) {
//			template = ConstraintTemplate.getByTemplateName(mUnary.group(0).substring(0, mUnary.group(0).indexOf("["))); //TODO: Should be done more intelligently
//			activationActivity = getActivityDataRowFromName(activityRows, mUnary.group(1));
//			activationCondition = mUnary.group(2);
//			timeCondition = mUnary.group(3);
//		}


		template = ConstraintTemplate.getByTemplateName(constraint.getRelation());
		activationActivity = getActivityDataRowFromName(activityRows, constraint.getActivity_one());
		if (!constraint.getActivity_two().isEmpty()){
			targetActivity = getActivityDataRowFromName(activityRows, constraint.getActivity_two());
		}

		ConstraintDataRow constraintRow = new ConstraintDataRow(template, activationActivity, null, targetActivity, null, null);
		constraintRow.validateRowEdit();
		constraintRow.confirmRowEdit();
		return constraintRow;
	}

	public static String getConstraintString(ConstraintDataRow constraintDataRow, boolean includeDataConditions) {
		if (constraintDataRow.getRowStatus() == RowStatus.EDITING || constraintDataRow.getRowStatus() == RowStatus.SAVED) {
			StringBuilder constraintStringBuilder = new StringBuilder();

			constraintStringBuilder.append(constraintDataRow.getTemplate().toString());
			constraintStringBuilder.append("[");

			if (!constraintDataRow.getTemplate().getIsBinary()) {
				constraintStringBuilder.append(constraintDataRow.getActivationActivity().getActivityName());
			} else {
				if (constraintDataRow.getTemplate().getReverseActivationTarget()) {
					constraintStringBuilder.append(constraintDataRow.getTargetActivity().getActivityName());
					constraintStringBuilder.append(", ");
					constraintStringBuilder.append(constraintDataRow.getActivationActivity().getActivityName());
				} else {
					constraintStringBuilder.append(constraintDataRow.getActivationActivity().getActivityName());
					constraintStringBuilder.append(", ");
					constraintStringBuilder.append(constraintDataRow.getTargetActivity().getActivityName());
				}
			}

			if (!includeDataConditions) {
				constraintStringBuilder.append("]");
			} else {
				constraintStringBuilder.append("] |");

				if (constraintDataRow.getActivationCondition() != null) {
					constraintStringBuilder.append(constraintDataRow.getActivationCondition());
				}
				constraintStringBuilder.append(" |");

				if (constraintDataRow.getTemplate().getIsBinary()) {
					if (constraintDataRow.getCorrelationCondition() != null) {
						constraintStringBuilder.append(constraintDataRow.getCorrelationCondition());
					}
					constraintStringBuilder.append(" |");
				}

				if (constraintDataRow.getTimeCondition() != null) {
					constraintStringBuilder.append(constraintDataRow.getTimeCondition());
				}
			}

			return constraintStringBuilder.toString();
		} else {
			return null;
		}
	}

	public static String getConstraintString(DiscoveredConstraint discoveredConstraint) {
		StringBuilder constraintStringBuilder = new StringBuilder();

		constraintStringBuilder.append(discoveredConstraint.getTemplate().toString());
		constraintStringBuilder.append("[");
		
		if (discoveredConstraint.getTemplate().getIsBinary()) {
			if (discoveredConstraint.getTemplate().getReverseActivationTarget()) {
				constraintStringBuilder.append(discoveredConstraint.getTargetActivity().getActivityFullName());
				constraintStringBuilder.append(", ");
				constraintStringBuilder.append(discoveredConstraint.getActivationActivity().getActivityFullName());
				
			} else {
				constraintStringBuilder.append(discoveredConstraint.getActivationActivity().getActivityFullName());
				constraintStringBuilder.append(", ");
				constraintStringBuilder.append(discoveredConstraint.getTargetActivity().getActivityFullName());
			}
			
		} else {
			constraintStringBuilder.append(discoveredConstraint.getActivationActivity().getActivityFullName());
		}
		
		constraintStringBuilder.append("]");
		
		if (discoveredConstraint.getDataCondition() != null) {
			Rule dataCond = discoveredConstraint.getDataCondition();
			
			constraintStringBuilder.append(" |" + dataCond.getAntecedents().toDotString("A"));
			
			if (dataCond.getType() == DataConditionType.CORRELATIONS)
				constraintStringBuilder.append(" |" + dataCond.getConsequents().toDotString("T"));
			else
				constraintStringBuilder.append(" |");
			
		} else {
			constraintStringBuilder.append(" |");
			
			if (discoveredConstraint.getTemplate().getIsBinary())
				constraintStringBuilder.append(" |");
		}
		
		Duration minTD = discoveredConstraint.getMinTD();
		Duration maxTD = discoveredConstraint.getMaxTD();
		
		if (minTD != null && maxTD != null) {
			long minAmount, maxAmount;
			String unit;
			if (minTD.toSecondsPart() != 0 || maxTD.toSecondsPart() != 0) {
				minAmount = minTD.toSeconds();
				maxAmount = maxTD.toSeconds();
				unit = "s";
			} else if (minTD.toMinutesPart() != 0 || maxTD.toMinutesPart() != 0) {
				minAmount = minTD.toMinutes();
				maxAmount = maxTD.toMinutes();
				unit = "m";
			} else {
				minAmount = minTD.toHours();
				maxAmount = maxTD.toHours();
				unit = "h";
			}
			
			constraintStringBuilder.append(" |");
			constraintStringBuilder.append(minAmount);
			constraintStringBuilder.append(",");
			constraintStringBuilder.append(maxAmount);
			constraintStringBuilder.append("," + unit);
		} else {
			constraintStringBuilder.append(" |");
		}
		
		return constraintStringBuilder.toString();
	}

	private static TreeDataActivity getActivityDataRowFromName(List<TreeDataActivity> treeDataActivities, String activityName) {
		for (TreeDataActivity activityDataRow : treeDataActivities) {
			if (activityName != null && activityName.equals(activityDataRow.getActivityName())) {
				return activityDataRow;
			}
		}
		return null;
	}
	
	public static void initializeTDVariables() {
		dmi = new DeclareMinerInput();
		dmi.setAprioriKnowledgeBasedCriteriaSet(new HashSet<>(Collections.singleton(AprioriKnowledgeBasedCriteria.valueOf("AllActivitiesWithEventTypes"))));
		dmi.setVerbose(false);
		dmi.setThreadNumber(4);
		dmi.setReferenceEventType("complete");
		
		Map<String, DeclareTemplate> stringToDeclareTemplateMap = new HashMap<>(); //This seems to be needed for some kind of internal mapping in Declare Miner (template name -> condec name)?
		for (ConstraintTemplate constraintTemplate : ConstraintTemplate.values()) {
			if (!constraintTemplate.equals(ConstraintTemplate.End)) {
				DeclareTemplate declareTemplate = TemplateUtils.getDeclareTemplate(constraintTemplate);
				String templateNameString;
				
				if (constraintTemplate.equals(ConstraintTemplate.Choice))
					templateNameString = "choice 1 of 2";
				else
					templateNameString = declareTemplate.name().replaceAll("_", " ").toLowerCase();
				
				stringToDeclareTemplateMap.put(templateNameString, declareTemplate);
			}
		}
		
		map = DeclareMiner.readConstraintTemplates(stringToDeclareTemplateMap);
	}
	
	public static void setConstraintTDs(XLog log, DiscoveredConstraint constraint) {
		// timeDists contains all the time distances between the activation and target along traces
		List<Duration> timeDists = new ArrayList<>(); 

		for (XTrace trace : log) {
			DeclareTemplate template = TemplateUtils.getDeclareTemplate(constraint.getTemplate());
			ConstraintDefinition cd = new ConstraintDefinition(0, null, map.get(template));
			
			for (Parameter par : map.get(template).getParameters()) {
				if (par.getName().equals("A"))
					cd.addBranch(par, new ActivityDefinition(constraint.getActivationActivity().getActivityFullName(), 0, null));
				else
					cd.addBranch(par, new ActivityDefinition(constraint.getTargetActivity().getActivityFullName(), 0, null));
			}
			
			Set<Integer> actIndSet = getActivationIndices(trace, constraint);
			if (!checkTraceFulfillment(trace, constraint, actIndSet))
				actIndSet.clear();
			
			TemplateInfo templateInfo;
			
			switch(template) {
			case Succession:
			case Alternate_Succession:
			case Chain_Succession:
				templateInfo = new SuccessionInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, actIndSet).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			/*case Choice:
				templateInfo = new ChoiceInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, fulfillments).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Exclusive_Choice:
				templateInfo = new ExclusiveChoiceInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, fulfillments).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;*/
			case Existence:
				templateInfo = new ExistenceInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, actIndSet).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Existence2:
				templateInfo = new Existence2Info();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, actIndSet).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Existence3:
				templateInfo = new Existence3Info();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, actIndSet).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			/*case Init:
				templateInfo = new InitInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, fulfillments).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Absence:
				templateInfo = new AbsenceInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, conflicts).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Absence2:
				templateInfo = new Absence2Info();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, conflicts).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Absence3:
				templateInfo = new Absence3Info();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, conflicts).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Exactly1:
				templateInfo = new Exactly1Info();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, fulfillments).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Exactly2:
				templateInfo = new Exactly2Info();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, fulfillments).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;*/
			case Responded_Existence:
				templateInfo = new RespondedExistenceInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, actIndSet).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Response:
			case Alternate_Response:
			case Chain_Response:
				templateInfo = new ResponseInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, actIndSet).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Precedence:
			case Alternate_Precedence:
			case Chain_Precedence:
				templateInfo = new PrecedenceInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, actIndSet).elements())) {
					timeDists.add(Duration.ofMillis(td.longValue()));
				}
				break;
			case CoExistence:
				templateInfo = new CoexistenceInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, actIndSet).elements())) {
					timeDists.add(Duration.ofMillis(td.longValue()));
				}
				break;
			/*case Not_CoExistence:
				templateInfo = new NotCoexistenceInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, conflicts).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;
			case Not_Succession:
			case Not_Chain_Succession:
				templateInfo = new NegativeRelationInfo();
				for (Long td : Collections.list(templateInfo.getTimeDistances(dmi, trace, cd, conflicts).elements()))
					timeDists.add(Duration.ofMillis(td.longValue()));
				break;*/
			default:
				logger.error("Time distance computation not yet implemented for template {}", template);
			}
		}
		
		if (!timeDists.isEmpty()) {			
			constraint.setMinTD(Collections.min(timeDists).truncatedTo(ChronoUnit.SECONDS));
			constraint.setMaxTD(Collections.max(timeDists).truncatedTo(ChronoUnit.SECONDS));
			
			Duration avgTD = timeDists.stream().reduce(Duration.ZERO, Duration::plus).dividedBy(timeDists.size());
			constraint.setAvgTD(avgTD.truncatedTo(ChronoUnit.SECONDS));
		}
	}
	
	public static Set<DiscoveredActivity> getAllActivitiesFromLog(XLog log, boolean considerLifecycle) {
		Map<DiscoveredActivity, Integer> activityFreqs = new HashMap<>();
		
		for (XTrace trace : log) {
			Set<DiscoveredActivity> temp = new HashSet<>();
			
			for (XEvent evt : trace) {
				String name = XConceptExtension.instance().extractName(evt);
				String transition = XLifecycleExtension.instance().extractTransition(evt);
				
				DiscoveredActivity a;
				if (considerLifecycle)
					a = new DiscoveredActivity(name, transition, 0); 
				else
					a = new DiscoveredActivity(name, 0);
				
				a.setPayloadAttributes(
					evt.getAttributes().values().stream()
						.filter(att -> !att.getKey().equals(XConceptExtension.KEY_NAME) 
								&& !att.getKey().equals(XLifecycleExtension.KEY_TRANSITION)
								&& !att.getKey().equals(XTimeExtension.KEY_TIMESTAMP))
						.map(att -> att.getKey())
						.collect(Collectors.toSet())
				);
				
				temp.add(a);
			}
			
			for (DiscoveredActivity act : temp) {
				if (activityFreqs.containsKey(act)) {
					DiscoveredActivity key = activityFreqs.keySet().stream().filter(k -> k.equals(act)).findFirst().get();
					key.getPayloadAttributes().addAll(act.getPayloadAttributes());
					
					activityFreqs.replace(act, activityFreqs.get(act)+1);
						
					} else {
						act.setPayloadAttributes(act.getPayloadAttributes());
						activityFreqs.put(act, 1);
				}
			}
			
		}
		
		Set<DiscoveredActivity> allActivities = new HashSet<>();
		for (Map.Entry<DiscoveredActivity, Integer> entry : activityFreqs.entrySet()) {
			DiscoveredActivity tempAct = entry.getKey();
			int frequency = entry.getValue();
			
			DiscoveredActivity act = new DiscoveredActivity(tempAct.getActivityName(), tempAct.getActivityTransition(), (float) frequency / log.size() );
			act.setPayloadAttributes(tempAct.getPayloadAttributes());
			
			allActivities.add(act);
		}
		
		return allActivities;
	
	}
	
	public static List<DiscoveredConstraint> extractConstraintsFromMinerfulModel(XLog log, ProcessModel model, List<DiscoveredActivity> actList) {
		List<DiscoveredConstraint> discoveredConstraints = new ArrayList<>();
		List<DiscoveredConstraint> coExistenceConstraints = new ArrayList<>();	// For checking duplicate CoExistence constraints
		
		for (Constraint constraint : model.getAllConstraints()) {
			// Create DiscoveredConstraint object
			ConstraintTemplate template = TemplateUtils.getConstraintTemplateFromMinerful(constraint.getTemplateName());

			List<String> parametersList = constraint.getParameters().stream().map(i -> i.toString()).collect(Collectors.toList());
			
			DiscoveredActivity activationActivity = actList.stream()
										.filter(act -> act.getActivityName().equals(parametersList.get(0)))
										.findFirst().get();
			
			DiscoveredActivity targetActivity = null;
			if (template.getIsBinary()) {
				if (template.getReverseActivationTarget()) {
					activationActivity = actList.stream()
							.filter(act -> act.getActivityName().equals(parametersList.get(1)))
							.findFirst().get();
					
					targetActivity = actList.stream()
							.filter(act -> act.getActivityName().equals(parametersList.get(0)))
							.findFirst().get();
					
				} else {
					
					targetActivity = actList.stream()
							.filter(act -> act.getActivityName().equals(parametersList.get(1)))
							.findFirst().get();
				}
			}
				
			DiscoveredConstraint discoveredConstraint = new DiscoveredConstraint(template, activationActivity, targetActivity);
			
			boolean isDuplicate = false;
			if (template == ConstraintTemplate.CoExistence || template == ConstraintTemplate.Not_CoExistence) {
				// Check if CoExistence constraint is already added with same parameters
				if (coExistenceConstraints.contains(discoveredConstraint))
					isDuplicate = true;
				else
					coExistenceConstraints.add(discoveredConstraint);
			}
			
			if (!isDuplicate)
				discoveredConstraints.add(discoveredConstraint);
		}
		
		return discoveredConstraints;
	}

	public static double computeTraceBasedSupport(XLog log, DiscoveredConstraint constraint, boolean vacuityAsViolation) {
		long fulfilledTracesCnt = 0;
		long vacuousTracesCnt = 0;
		
		for (XTrace trace : log) {
			Set<Integer> activationInds = getActivationIndices(trace, constraint);
			//Set<Integer> targetInds = getTargetIndices(trace, constraint);
			
			ConstraintTemplate template = constraint.getTemplate();
			// Unary templates
			if (!template.getIsBinary()) {
				if (checkTraceFulfillment(trace, constraint, activationInds))
					fulfilledTracesCnt++;
				
			// Binary templates
			} else {
				if (activationInds.isEmpty()) {
					// Check for templates that have both activities as activations and targets
					if (template == ConstraintTemplate.Succession || template == ConstraintTemplate.Alternate_Succession
							|| template == ConstraintTemplate.Chain_Succession || template == ConstraintTemplate.CoExistence
							|| template == ConstraintTemplate.Choice || template == ConstraintTemplate.Exclusive_Choice
							|| template == ConstraintTemplate.Not_Succession || template == ConstraintTemplate.Not_Chain_Succession
							|| template == ConstraintTemplate.Not_CoExistence) {
						
						if (getTargetIndices(trace, constraint).isEmpty())
							vacuousTracesCnt++;
						else if (checkTraceFulfillment(trace, constraint, activationInds))
							fulfilledTracesCnt++;
					
					} else {
						vacuousTracesCnt++;
					}
					
				} else {
					if (checkTraceFulfillment(trace, constraint, activationInds))
						fulfilledTracesCnt++;
				}
			}
		}
		
		if (vacuityAsViolation)
			return (double) fulfilledTracesCnt / log.size();
		else
			return (double) (fulfilledTracesCnt + vacuousTracesCnt) / log.size();
	}
	
	private static Set<Integer> getActivationIndices(XTrace trace, DiscoveredConstraint constraint) {
		Set<Integer> indices = new HashSet<>();
		
		for (XEvent evt : trace)
			if (isActivation(evt, constraint))
				indices.add(trace.indexOf(evt));
		
		return indices;
	}
	
	private static boolean isActivation(XEvent evt, DiscoveredConstraint constraint) {
		DiscoveredActivity activation = constraint.getActivationActivity();
		
		if (XConceptExtension.instance().extractName(evt).equals( activation.getActivityName() ))
			if (activation.getActivityTransition() == null
					|| XLifecycleExtension.instance().extractTransition(evt).equals( activation.getActivityTransition() ))
				if (constraint.getDataCondition() == null
						|| predicateSatisfaction(RulesExtractor.getPayload(evt), constraint.getDataCondition().getAntecedents()))
					return true;
		
		return false;
	}
	
	private static boolean checkTraceFulfillment(XTrace trace, DiscoveredConstraint constraint, Set<Integer> activationInds) {
		// NOTE - This method returns true also in case of vacuous fulfillment!
		TreeSet<Integer> actSet = new TreeSet<>(activationInds);
		
		switch(constraint.getTemplate()) {
		case Absence:
			return actSet.isEmpty();
			
		case Absence2:
			return actSet.size() <= 1;
			
		case Absence3:
			return actSet.size() <= 2;
			
		case Alternate_Precedence: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			for (int actInd : actSet) {
				if (trgSet.floor(actInd-1) == null)
					return false;
				
				if (actSet.floor(actInd-1) != null
						&& actSet.floor(actInd-1) > trgSet.floor(actInd-1))
					return false;
			}
			
			return true;
		}
		case Alternate_Response: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			for (int actInd : actSet) {
				if (trgSet.ceiling(actInd+1) == null)
					return false;
				
				if (actSet.ceiling(actInd+1) != null
						&& actSet.ceiling(actInd+1) < trgSet.ceiling(actInd+1))
					return false;
        	}
			
			return true;
		}
		case Alternate_Succession: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			for (int actInd : actSet) {
				if (trgSet.ceiling(actInd+1) == null)
					return false;
				
				if (actSet.ceiling(actInd+1) != null
						&& actSet.ceiling(actInd+1) < trgSet.ceiling(actInd+1))
					return false;
        	}
			
			for (int trgInd : trgSet) {
				if (actSet.floor(trgInd-1) == null)
					return false;
				
				if (trgSet.floor(trgInd-1) != null
						&& trgSet.floor(trgInd-1) > actSet.floor(trgInd-1))
					return false;
			}
			
			return true;
		}
		case Chain_Precedence: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			for (int actInd : actSet)
        		if (!trgSet.contains(actInd-1))
        			return false;
			
			return true;
		}
		case Chain_Response: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			for (int actInd : actSet) 
        		if (!trgSet.contains(actInd+1))
        			return false;
			
			return true;
		}
		case Chain_Succession: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			
			for (int actInd : actSet) 
        		if (!trgSet.contains(actInd+1))
        			return false;
			
			for (int trgInd : trgSet)
				if (!actSet.contains(trgInd-1))
					return false;
			
			return true;
		}
		case Choice: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			return !actSet.isEmpty() || !trgSet.isEmpty();
		}
		case CoExistence: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			if (actSet.isEmpty() && trgSet.isEmpty())
				return true;	// Vacuous fulfillment
			else
				return !actSet.isEmpty() && !trgSet.isEmpty();
		}
		case End:
			return !actSet.isEmpty() && actSet.last() == trace.size()-1;
			
		case Exactly1:
			return actSet.size() == 1;
			
		case Exactly2:
			return actSet.size() == 2;
			
		case Exclusive_Choice: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			return actSet.isEmpty() ^ trgSet.isEmpty();
		}
		case Existence:
			return actSet.size() >= 1;
			
		case Existence2:
			return actSet.size() >= 2;
			
		case Existence3:
			return actSet.size() >= 3;
			
		case Init:
			return !actSet.isEmpty() && actSet.first() == 0;

		case Not_Chain_Succession: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			
			for (int actInd : actSet) 
        		if (!trgSet.contains(actInd+1))
        			return true;
			
			for (int trgInd : trgSet)
				if (!actSet.contains(trgInd-1))
					return true;
			
			return actSet.isEmpty() && trgSet.isEmpty();
		}
		case Not_CoExistence: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			return !( !actSet.isEmpty() && !trgSet.isEmpty() );
		}
		case Not_Succession: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			if (!actSet.isEmpty() && !trgSet.isEmpty())
				return !( trgSet.floor(actSet.first()-1) == null && trgSet.ceiling(actSet.last()+1) != null );
			else
				return true;
		}
		case Precedence: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			if (actSet.isEmpty())
				return true;	// Vacuous fulfillment
			else
				return trgSet.floor(actSet.first()-1) != null;
		}
		case Responded_Existence: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			if (actSet.isEmpty())
				return true;	// Vacuous fulfillment
			else
				return !trgSet.isEmpty();
		}
		case Response: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			if (actSet.isEmpty())
				return true;	// Vacuous fulfillment
			else
				return trgSet.ceiling(actSet.last()+1) != null;
		}
		case Succession: {
			TreeSet<Integer> trgSet = new TreeSet<>(getTargetIndices(trace, constraint));
			if (actSet.isEmpty() && trgSet.isEmpty())
				return true;	// Vacuous fulfillment
			else if (actSet.isEmpty() ^ trgSet.isEmpty())
				return false;
			else	// Both actSet and trgSet not empty 
				return actSet.floor(trgSet.first()-1) != null && trgSet.ceiling(actSet.last()+1) != null;
		}
		default:
			throw new NoSuchElementException("Template " + constraint.getTemplate() + " not yet implemented in RuM.");
		}
	}
	
	private static Set<Integer> getTargetIndices(XTrace trace, DiscoveredConstraint constraint) {
		Set<Integer> indices = new HashSet<>();
		
		for (XEvent evt : trace)
			if (isTarget(evt, constraint))
				indices.add(trace.indexOf(evt));
		
		return indices;
	}
	
	private static boolean isTarget(XEvent evt, DiscoveredConstraint constraint) {
		DiscoveredActivity target = constraint.getTargetActivity();
		
		if (target != null && XConceptExtension.instance().extractName(evt).equals( target.getActivityName() ))
			if (target.getActivityTransition() == null
					|| XLifecycleExtension.instance().extractTransition(evt).equals( target.getActivityTransition() ))
				if (constraint.getDataCondition() == null
						|| predicateSatisfaction(RulesExtractor.getPayload(evt), constraint.getDataCondition().getConsequents()))
					return true;
		
		return false;
	}
	
	private static boolean predicateSatisfaction(XAttributeMap payload, Predicate predicate) {
    	
    	// RECURSIVE PART: if the predicate is logical then check rule satisfaction over inner predicates
    	if (predicate instanceof LogicalPredicate) {
    		LogicalPredicate logPred = (LogicalPredicate) predicate;
    		LogicalOperator logOp = (LogicalOperator) logPred.getOperator();
    		
    		List<Boolean> innerSatisfactions = new ArrayList<>();
    		
    		for (Predicate innerPred : logPred.getChildren())
    			innerSatisfactions.add(predicateSatisfaction(payload, innerPred));
    		
    		if (logOp == LogicalOperator.AND)
    			return innerSatisfactions.contains(false) ? false : true;
    		
    		else	// logOp is OR
    			return innerSatisfactions.contains(true) ? true : false;
    	
    	// BASE CASE 1: the parent rule is a fulfillment
    	} else if (predicate.equals(Predicate.fulfillmentPredicate)) {
    		return true;
    	
    	// BASE CASE 2: if the predicate is attribute then check if payload satisfies the predicate
    	} else {
    		AttributePredicate attrPred = (AttributePredicate) predicate;
    		String attribute = attrPred.getAttribute();
    		AttributeOperator attrOp = (AttributeOperator) attrPred.getOperator();
    		String value = attrPred.getValue();
    		
    		if (payload == null || !payload.containsKey(attribute)) {
    			return false;
    		
    		} else {
    			switch (attrOp) {
                
    			case IS:
    			case EQ:
    				return payload.get(attribute).toString().equals(value) ? true : false;
                
    			case IS_NOT:
                case NEQ:
                	return !payload.get(attribute).toString().equals(value) ? true : false;
                
                case GT:
                	return Double.parseDouble(payload.get(attribute).toString()) > Double.parseDouble(value) ? true : false;
                
                case GEQ:
                	return Double.parseDouble(payload.get(attribute).toString()) >= Double.parseDouble(value) ? true : false;
                
                case LT:
                	return Double.parseDouble(payload.get(attribute).toString()) < Double.parseDouble(value) ? true : false;
                
                case LEQ:
                	return Double.parseDouble(payload.get(attribute).toString()) <= Double.parseDouble(value) ? true : false;
                	
                default:	//TODO: Implement behavior for remaining AttributeOperator values
                	return false;
    			}
    		}
    	}
    }
}
