package task.discovery.mp_enhancer;

import controller.discovery.DataConditionType;
import controller.discovery.data.DiscoveredConstraint;
import task.discovery.data.Predicate;
import util.ConstraintUtils;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public final class CorrelationMiner {
	
	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final int NUM_OF_CLUSTERS = 2;

	private CorrelationMiner() {
	}

	// Called by the new RuM
	public static void findCorrelations(XLog log, List<DiscoveredConstraint> discoveredConstraints, DataConditionType conditionType, double minSupport, boolean vacuityAsViolation, boolean considerLifecycle) {
		logger.debug("Number of constraints to process: {}", discoveredConstraints.size());
		int i=0;
		for (DiscoveredConstraint c : discoveredConstraints) {
			logger.debug("Processing constraint {}: {}", ++i, c.toString());
			
			RuleEvaluationOutput ruleEvaluationOutput = processConstraint(log, c, conditionType, null, considerLifecycle);
			if (ruleEvaluationOutput != null) {
				double highestSupport = -1;
				Rule ruleWithHighestSupport = null;
				
				for (Rule r : ruleEvaluationOutput.getRules()) {
					DiscoveredConstraint dummy = new DiscoveredConstraint(c.getTemplate(), c.getActivationActivity(), c.getTargetActivity());
					dummy.setDataCondition(r);
					
					double newSupp = ConstraintUtils.computeTraceBasedSupport(log, dummy, vacuityAsViolation);
					if (newSupp > highestSupport) {
						highestSupport = newSupp;
						ruleWithHighestSupport = r;
					}
				}
				
				if (ruleWithHighestSupport != null) {
					c.setDataCondition(ruleWithHighestSupport);
					c.setConstraintSupport((float) highestSupport);
				}
			}
		}
	}

	private static RuleEvaluationOutput processConstraint(XLog log, DiscoveredConstraint constraint, DataConditionType conditionType, List<Double> execTimes, boolean considerLifecycle) {
		long startTime = System.currentTimeMillis();
		
		Map<XTrace, List<FeatureVector>> fvMap = RulesExtractor.extractFeatureVectors(log, constraint, considerLifecycle);
		List<FeatureVector> fulfillments = new LinkedList<>();
		List<FeatureVector> violations = new LinkedList<>();
		
		for (List<FeatureVector> fvList : fvMap.values()) {
			for (FeatureVector fv : fvList) {
				if (fv.isFulfillment)
					fulfillments.add(fv);
				else
					violations.add(fv);
			}
		}
		
		List<Cluster> clusters = new ArrayList<>();
		List<FeatureVector> vectorsToClassify = new ArrayList<>();
		
		switch (conditionType) {
		case ACTIVATIONS: {
			if (!fulfillments.isEmpty()) {
				Cluster cluster = new Cluster(Predicate.fulfillmentPredicate);
				cluster.setElements(fulfillments);
				cluster.giveLabels();
				
				clusters.add(cluster);
				vectorsToClassify.addAll(fulfillments);
			}
			break;
		}
		case CORRELATIONS: {
			if (!fulfillments.isEmpty()) {
				// Lowered max_iterations to 100 instead of default value of 10000 because in some cases clustering the algorithm never converges
				KMedoidsClusterer kMedoids = new KMedoidsClusterer(NUM_OF_CLUSTERS, 100, true);	// new KMedoidsClusterer(NUM_OF_CLUSTERS);
				clusters.addAll( kMedoids.clustering(fulfillments) );
				
				clusters = clusters.stream()
						.filter(cluster -> !cluster.getElements().isEmpty() && cluster.getLabel() == null)
						.collect(Collectors.toList());
				
				for (Cluster cluster : clusters) {
					Predicate label = ClusterDescriptor.describeCluster(cluster, fulfillments, constraint);
					cluster.setLabel(label);
					cluster.giveLabels();
				}
				
				vectorsToClassify.addAll(fulfillments);
			}
			break;
		}
		default:
			logger.error("No data condition discovery type selected !!!");
			break;
		}
		
		clusters = clusters.stream()
				.filter(cluster -> !cluster.getElements().isEmpty() && cluster.getLabel() != null)
				.collect(Collectors.toList());
		
		List<Rule> allRules = new ArrayList<>();
		
		if (!clusters.isEmpty()) {
			RuleEvaluationOutput classification = JRipClassifier.classify(clusters, vectorsToClassify, constraint);
			allRules.addAll(classification.getRules());
			
		}
		
		String relationName = constraint.toString();
		RuleEvaluationOutput ruleEvaluationOutput = new RuleEvaluationOutput(relationName, allRules);
		
		long stopTime = System.currentTimeMillis();
		
		if (execTimes != null) 	// Null-check added because i decided to omit execution times from the new RuM
			execTimes.add((stopTime - startTime) / 1000.0);
		
		return ruleEvaluationOutput;
	}
	
	public static Set<XAttribute> getCommonElements(Collection<XAttributeMap> maps) {
		Set<XAttribute> all = new HashSet<>();
		for (XAttributeMap map : maps)
			all.addAll(map.values());
		
		Set<XAttribute> common = new HashSet<>(all);
		for (XAttributeMap map : maps)
			common.retainAll(map.values());	// Computes intersection over the string sets
		
		return common;
	}
	
	
	
	
	//Called by the old RuM
	public static Output findCorrelations_old(XLog log, List<DiscoveredConstraint> declareConstraints, boolean considerViolations, int k, double minNodeSize, boolean pruning, boolean considerActivations) {

//		List<Evaluation> evaluationResults = new ArrayList<>();
		List<RuleEvaluationOutput> evaluationResults = new ArrayList<>();
		List<Double> execTimes = new ArrayList<>();
		double totalExecTime = 0.0;

		long totalStartTime = System.currentTimeMillis();
//		HashMap<String, List<Event>> cases = new HashMap<>();
//      String format = file.substring(file.lastIndexOf(".") + 1);
//
//      if(format.equals("csv"))
//          cases = LogReader.readCSV(file);
//      else if(format.equals("xes"))
//          cases = LogReader.readXES(file);

		for (DiscoveredConstraint constraint: declareConstraints) {
			//Extracted original loop contents to processConstraint method so that it would be easier to use in the new RuM
			RuleEvaluationOutput ruleEvaluationOutput = processConstraint_old(log, constraint, considerViolations, k, execTimes);
			evaluationResults.add(ruleEvaluationOutput);
		}
		long totalStopTime = System.currentTimeMillis();
		//System.out.println("\n\nTotal Execution Time: " + (totalStopTime - totalStartTime) / 1000.0 + " sec");
		totalExecTime = (totalStopTime - totalStartTime) / 1000.0;
		return new Output(evaluationResults, execTimes, totalExecTime);
	}
	
	private static RuleEvaluationOutput processConstraint_old(XLog log, DiscoveredConstraint constraint, boolean considerViolations, int numOfClusters, List<Double> execTimes) {
		long startTime = System.currentTimeMillis();
		
		/*
		System.out.println("Constraint: " + constraint.toString());
		var count = 0;
        for(var caseID: cases.keySet())
            for(var event: cases.get(caseID))
                if(event.activityName.equals("action not required"))
                    count++;

		LogWriter.writeLog("C:\\Volodymyr\\PhD\\JOURNAL EXTENSION\\Real Experiments\\BPIC2017\\" + rule + "(" + itemset.items.get(0) + "," + itemset.items.get(1) + ").csv", featureVectors);
		*/
		
		String relationName = constraint.toString();
		
		Map<XTrace, List<FeatureVector>> fvMap = RulesExtractor.extractFeatureVectors(log, constraint, true);
		List<FeatureVector> fulfillments = new LinkedList<>();
		List<FeatureVector> violations = new LinkedList<>();
		
		for (List<FeatureVector> fvList : fvMap.values()) {
			for (FeatureVector fv : fvList) {
				if (fv.isFulfillment)
					fulfillments.add(fv);
				else
					violations.add(fv);
			}
		}
		
		List<Rule> allRules = new ArrayList<Rule>();
		
		if (!fulfillments.isEmpty()) {
			KMedoidsClusterer kMedoids = new KMedoidsClusterer(numOfClusters);
			List<Cluster> clusters = new ArrayList<Cluster>(kMedoids.clustering(fulfillments));
			clusters = clusters.stream()
					.filter(cluster -> !cluster.getElements().isEmpty() && cluster.getLabel() == null)
					.collect(Collectors.toList());
			
			for (Cluster cluster : clusters) {
				Predicate label = ClusterDescriptor.describeCluster(cluster, fulfillments, constraint);
				cluster.setLabel(label);
				cluster.giveLabels();
			}
			
			clusters = clusters.stream()
					.filter(cluster -> !cluster.getElements().isEmpty() && cluster.getLabel() != null)
					.collect(Collectors.toList());
			
			if (!clusters.isEmpty()) {
				//double fulfillmentsRatio = (double) fulfillments.size() / (fulfillments.size() + violations.size());
				RuleEvaluationOutput fulfillmentsClassification = JRipClassifier.classify(clusters, fulfillments, constraint);
				
				allRules.addAll(fulfillmentsClassification.getRules());
			}
		}

		if (considerViolations && !violations.isEmpty()) {
			Cluster cluster = new Cluster(Predicate.violationPredicate);
			cluster.setElements(violations);
			cluster.giveLabels();
			
			List<Cluster> clusters = Arrays.asList(cluster);
			
			//double violationsRatio = (double) violations.size() / (fulfillments.size() + violations.size());
			RuleEvaluationOutput violationClassification = JRipClassifier.classify(clusters, violations, constraint);
			
			allRules.addAll(violationClassification.getRules());	// There should be at most one violation Rule
		}
		
		RuleEvaluationOutput ruleEvaluationOutput = new RuleEvaluationOutput(relationName, allRules);
		
		long stopTime = System.currentTimeMillis();
		
		if (execTimes != null) 	// Null-check added because i decided to omit execution times from the new RuM
			execTimes.add((stopTime - startTime) / 1000.0);
		
		return ruleEvaluationOutput;
	}
}
