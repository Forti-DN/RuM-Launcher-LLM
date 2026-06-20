package task.discovery.mp_enhancer;

import weka.classifiers.rules.JRip;
import weka.classifiers.rules.JRip.Antd;
import weka.classifiers.rules.JRip.RipperRule;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.ClassOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeMap;

import controller.discovery.data.DiscoveredConstraint;
import task.discovery.data.LogicalOperator;
import task.discovery.data.LogicalPredicate;
import task.discovery.data.AttributeOperator;
import task.discovery.data.AttributePredicate;
import task.discovery.data.Predicate;

public class JRipClassifier {
	private static final double MIN_NODE_SIZE = 0.05;
	private static final boolean PRUNING = true;
	private static List<Object> classLabels;

	public static RuleEvaluationOutput classify(List<Cluster> clusters, List<FeatureVector> featureVectors, DiscoveredConstraint constraint) {
        try {
            List<XAttributeMap> froms = featureVectors.stream().map(fv -> fv.from).collect(Collectors.toList());
            Set<XAttribute> commonAttributes = CorrelationMiner.getCommonElements(froms);
            
            Set<XAttribute> selectedAttributes = new HashSet<>();
            
            if (!commonAttributes.isEmpty())
            	selectedAttributes = commonAttributes;
            
            else	// If there aren't common attributes, the classification is made over all the attributes
            	for (XAttributeMap f : froms)
            		selectedAttributes.addAll(f.values());
            
            Map<XAttribute, List<Rule>> allRules = new HashMap<>();
            
            for (XAttribute attribute : selectedAttributes) {
            	List<RipperRule> ripperRules = getRuleList(Set.of(attribute), featureVectors, constraint.toString());
            	
            	List<Rule> attributeRules = new ArrayList<>();
            	
            	for (int i=0; i<classLabels.size(); i++) {
            		int labelIndex = i;
            		List<RipperRule> rulesWithSameLabel = ripperRules.stream()
				            					.filter(rule -> (int)Math.round(rule.getConsequent()) == labelIndex)
				            					.collect(Collectors.toList());
            		
            		if (!rulesWithSameLabel.isEmpty()) {
	            		Set<Predicate> conds = new HashSet<>();
	            		
	            		for (RipperRule rr : rulesWithSameLabel) {
        					Set<Predicate> preds = new HashSet<>();
        					
        					List<Antd> antdList = rr.getAntds();
			        		for (Antd antd : antdList)
			        			preds.add(Predicate.getPredicateFromRipperAntd(antd));
			        		
			        		conds.add(new LogicalPredicate(null, LogicalOperator.AND).addChildren(preds));
            			}
	            		
	            		Predicate antecedents = new LogicalPredicate(null, LogicalOperator.OR).addChildren(conds);
	            		
	            		String labelStr = classLabels.get((int)Math.round(rulesWithSameLabel.get(0).getConsequent())).toString();
		        		Predicate consequents = Predicate.getPredicateFromString(labelStr);
		        		
	        			attributeRules.add(new Rule(antecedents, consequents));
            		}
            	}
            	
        		allRules.put(attribute, attributeRules);
        	}
            
            Map<XAttribute, Predicate> antecedentsForDefaultRules = new HashMap<>();
        	
        	for (Map.Entry<XAttribute, List<Rule>> entry : allRules.entrySet()) {
        		for (Rule rule : entry.getValue()) {
	            	Predicate consequents = rule.getConsequents();
        			
	            	List<Cluster> clustersMatchingRuleConseq = new ArrayList<>();
        			for (Cluster c : clusters)
        				if (c.getLabel().equals(consequents))
        					clustersMatchingRuleConseq.add(c);
        			
        			List<FeatureVector> fvs = new ArrayList<>();
        			for (Cluster c : clustersMatchingRuleConseq)
        				fvs.addAll(c.getElements());
        			
        			Set<XAttribute> fvsCommonAttributes = CorrelationMiner.getCommonElements(fvs.stream().map(fv -> fv.from).collect(Collectors.toSet()));
        			
        			if (rule.getAntecedents().isEmpty()) {
        				Predicate antds;
        				
        				LogicalOperator operator;
    					if (fvsCommonAttributes.containsAll(selectedAttributes))
    						operator = LogicalOperator.AND;
                        else
                        	operator = LogicalOperator.OR;
    					
        				if (entry.getValue().size() == 1) {
        					
        					Set<Predicate> preds = getAntecedents(fvs, selectedAttributes);
        					antds = new LogicalPredicate(null, operator).addChildren(preds);
        				
        				} else {
        					
        					Set<Predicate> preds = new HashSet<>();
	        				
        					for (Rule dummyRule : entry.getValue())
	        					if (!dummyRule.equals(rule))
	        						preds.add(dummyRule.getAntecedents());
	        				
        					antds = new LogicalPredicate(null, operator).addChildren(preds).makeOpposite();
        				}
        				
        				antecedentsForDefaultRules.put(entry.getKey(), antds);
        			}
        		}
        	}
        	
        	List<Rule> outputRules = new ArrayList<>();
        	
        	Set<Predicate> differentConseqs = clusters.stream().map(c -> c.getLabel()).collect(Collectors.toSet());
        	for (Predicate consequents : differentConseqs) {
        		
        		List<Cluster> clustersWithSameLabel = new ArrayList<>();
    			for (Cluster c : clusters)
    				if (c.getLabel().equals(consequents))
    					clustersWithSameLabel.add(c);
    			
    			List<FeatureVector> fvs = new ArrayList<>();
    			for (Cluster c : clustersWithSameLabel)
    				fvs.addAll(c.getElements());
    			
        		Set<XAttribute> fvsCommonAttributes = CorrelationMiner.getCommonElements(fvs.stream().map(fv -> fv.from).collect(Collectors.toSet()));
        		
        		Set<Rule> rulesWithSameLabel = new HashSet<>();
        		for (List<Rule> list : allRules.values()) 
            		rulesWithSameLabel.addAll(
            				list.stream()
            					.filter(rule -> rule.getConsequents().equals(consequents))
            					.collect(Collectors.toList()) );
        	
            	if (!rulesWithSameLabel.isEmpty()) {
            		
            		Set<Predicate> preds = new HashSet<>();
	            	for (Rule rule : rulesWithSameLabel) {
	            		
	            		if (!rule.getAntecedents().isEmpty())
	            			preds.add(rule.getAntecedents());
	            		
	            		else
	            			for (Map.Entry<XAttribute, List<Rule>> entry : allRules.entrySet())
	            				if (entry.getValue().contains(rule))
	            					preds.add(antecedentsForDefaultRules.get(entry.getKey()));
	            				
	            	}
	            	
	            	Predicate antecedents;
            		if (fvsCommonAttributes.containsAll(selectedAttributes))
                    	antecedents = new LogicalPredicate(null, LogicalOperator.AND).addChildren(preds);
                    else
                    	antecedents = new LogicalPredicate(null, LogicalOperator.OR).addChildren(preds);
	            	
	            	Rule rule = new Rule(antecedents, consequents);
	            	outputRules.add(rule);
            	}
            }
        	
            return new RuleEvaluationOutput(constraint.toString(), outputRules);
        
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	
	private static List<RipperRule> getRuleList(Set<XAttribute> selectedAttribs, List<FeatureVector> featureVectors, String relationName) throws Exception {
		ArrayList<Attribute> attributes = new ArrayList<>();
		
		for (XAttribute attr : selectedAttribs) {
			if (attr instanceof XAttributeContinuous || attr instanceof XAttributeDiscrete) {
                attributes.add(new Attribute(attr.getKey()));
        	
        	} else {
        		Set<String> attVals = new HashSet<>();
        		featureVectors.forEach(fv -> {
        			attVals.addAll(fv.from.values().stream()
				    				.filter(att -> att.getClass().equals(attr.getClass()) 
				    							&& att.getKey().equals(attr.getKey()))
				    				.map(XAttribute::toString)
				    				.collect(Collectors.toSet())
        			);
        		});
        		
                attributes.add(new Attribute(attr.getKey(), List.copyOf(attVals)));
        	}
        }
        
        Set<Predicate> labels = featureVectors.stream().map(fv -> fv.label).collect(Collectors.toSet());
        
        if (labels.isEmpty())
        	return null;
        
        if (labels.size() == 1) // This means that the whole dataset belongs to the same cluster
        	// Since the JRip classifier cannot handle classifications with one cluster, 
        	// we manually add a dummy label that will never be displayed.
        	labels.add(Predicate.dummyPredicate);
        
        attributes.add(new Attribute("Class", labels.stream().map(Predicate::toString).collect(Collectors.toList()) ));
        
        Instances data = new Instances(relationName, attributes, featureVectors.size());
        
        for (FeatureVector fv : featureVectors) {
            Instance inst = new DenseInstance(attributes.size());
            
            for (Attribute attr : attributes) {
            	String value;
                
            	if (attr.name().equals("Class")) {
            		value = fv.label.toString();
            	
            	} else {
                	Optional<XAttribute> opt = fv.from.values().stream()
									.filter(att -> att.getKey().equals(attr.name()))
									.findAny();
                	
                	value = opt.isPresent() ? opt.get().toString() : null;
                }
            	
            	if (value != null) {
                    if (attr.isNumeric())
                    	inst.setValue(attr, Double.parseDouble(value));
                    else
                        inst.setValue(attr, value);
            	}
            }
            
            data.add(inst);
        }
        
        while (data.size() < 3) {
        	// Since JRip classifier requires at least 3 data instances,
    		// empty instances are added to let the algorithm work
    		
    		Instance emptyInst = new DenseInstance(attributes.size());
    		
    		for (Attribute attr : attributes)
                if (attr.name().equals("Class"))
            		emptyInst.setValue(attr, featureVectors.get(0).label.toString()); // The value of the class label is useless since it has no values for attributes
    		
    		data.add(emptyInst);
        }

        if (data.classIndex() < 0)
            data.setClassIndex(data.numAttributes()-1);
                
    	String[] options;
        if (!PRUNING) {
            options = new String[3];
            options[0] = "-N";
            options[1] = "1.0";
            options[2] = "-P";
        } else {
            options = new String[2];
            options[0] = "-N";
            options[1] = String.valueOf((double) Math.round(MIN_NODE_SIZE * featureVectors.size()));
        }

        JRip classifier = new JRip();
        classifier.setOptions(options);
        classifier.buildClassifier(data);
        
        // This part is needed since the classifier can modify the order of labels in the class attribute
        Filter m_Filter = new ClassOrder();
        ((ClassOrder) m_Filter).setSeed(data.getRandomNumberGenerator(classifier.getSeed()).nextInt());
        ((ClassOrder) m_Filter).setClassOrder(ClassOrder.FREQ_ASCEND);
        m_Filter.setInputFormat(data);
        data = Filter.useFilter(data, m_Filter);
        
        // classLabels contains the ordered list of labels
        classLabels = Collections.list(data.classAttribute().enumerateValues());
        
        List<RipperRule> ruleList = classifier.getRuleset().stream().map(rule -> (RipperRule)rule).collect(Collectors.toList());
        return ruleList;
	}
	
    private static Set<Predicate> getAntecedents(List<FeatureVector> featureVectorList, Set<XAttribute> selectedAttributes) {
    	Set<Predicate> preds = new HashSet<>();
    	
    	if (!featureVectorList.isEmpty()) {
        	for (XAttribute attribute : selectedAttributes) {
        		
        		List<String> values = new LinkedList<>();
        		featureVectorList.forEach(fv -> {
        			values.addAll(fv.from.values().stream()
				    				.filter(att -> att.getClass().equals(attribute.getClass()) 
				    							&& att.getKey().equals(attribute.getKey()))
				    				.map(XAttribute::toString)
				    				.collect(Collectors.toList())
        			);
        		});
            	
	        	if (attribute instanceof XAttributeContinuous || attribute instanceof XAttributeDiscrete) {
	        		List<Double> doubleArray = values.stream().map(e -> Double.parseDouble(e)).collect(Collectors.toList());
	                double max = Collections.max(doubleArray);
	                double min = Collections.min(doubleArray);
	                
	                if (min == max) {
	                	preds.add(new AttributePredicate(null, attribute.getKey(), AttributeOperator.EQ, String.valueOf(min)));
	                	
	                } else {
	                	AttributePredicate minPred = new AttributePredicate(null, attribute.getKey(), AttributeOperator.GEQ, String.valueOf(min));
	                	AttributePredicate maxPred = new AttributePredicate(null, attribute.getKey(), AttributeOperator.LEQ, String.valueOf(max));
	                	preds.add(new LogicalPredicate(null, LogicalOperator.AND).addChildren(Set.of(minPred, maxPred)));
	                }
	            
	        	} else {
                    HashMap<String, Integer> frequencies = new HashMap<>();
                    for (String value : values)
                    	frequencies.put(value, frequencies.containsKey(value) ? frequencies.get(value) + 1 : 1);
                    
                    String mostFrequentLabel = values.get(0);
                    int max = frequencies.get(mostFrequentLabel);
                    
                    for (String key : frequencies.keySet())
                        if (frequencies.get(key) > max) {
                            max = frequencies.get(key);
                            mostFrequentLabel = key;
                        }
                    
                    preds.add(new AttributePredicate(null, attribute.getKey(), AttributeOperator.IS, mostFrequentLabel));
                    // TODO: Instead of taking only the most frequent value, use the IN / NOT IN attribute operators
	            }
	        }
        }
        
    	return preds;
    }
}
