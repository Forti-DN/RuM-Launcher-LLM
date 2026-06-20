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

import java.util.*;
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

public class ClusterDescriptor {
	private static final double MIN_NODE_SIZE = 0.05;
	private static List<Object> classLabels;
	
    public static Predicate describeCluster(Cluster cluster, List<FeatureVector> featureVectors, DiscoveredConstraint constraint) {
        try {
            List<XAttributeMap> tos = cluster.getElements().stream().map(fv -> fv.to).collect(Collectors.toList());            
            Set<XAttribute> commonAttributes = CorrelationMiner.getCommonElements(tos);
            
            Set<XAttribute> selectedAttributes = new HashSet<>();
            
            if (!commonAttributes.isEmpty())
            	selectedAttributes = commonAttributes;
            
            else	// If there aren't common attributes, the classification is made over all the attributes
            	for (XAttributeMap t : tos)
            		selectedAttributes.addAll(t.values());
            
            Set<Predicate> conds = new HashSet<>();
            
            for (XAttribute attribute : selectedAttributes) {
            	List<RipperRule> ruleList = getRuleList(Set.of(attribute), featureVectors, constraint.toString(), cluster);
        		
        		double positiveLabelIndex = classLabels.indexOf("positive");
                double negativeLabelIndex = classLabels.indexOf("negative");
                
                RipperRule defaultRule = ruleList.stream().filter(r -> r.getAntds().isEmpty()).collect(Collectors.toList()).get(0);
                
                if (defaultRule.getConsequent() == positiveLabelIndex) {	// The default rule has positive class label
                	
                	for (RipperRule negativeRule : ruleList.stream().filter(r -> r.getConsequent()==negativeLabelIndex).collect(Collectors.toList())) {
    					List<Antd> antds = negativeRule.getAntds();
    					Set<Predicate> preds = new HashSet<>();
    					
    					for (Antd antd : antds)
    						preds.add(Predicate.getPredicateFromRipperAntd(antd));
    						
    					conds.add(new LogicalPredicate(null, LogicalOperator.OR).addChildren(preds).makeOpposite());
    				}
                	
                } else {	// The default rule has negative class label
                	
    				for (RipperRule positiveRule : ruleList.stream().filter(r -> r.getConsequent()==positiveLabelIndex).collect(Collectors.toList()) ) {
    					List<Antd> antds = positiveRule.getAntds();
    					Set<Predicate> preds = new HashSet<>();
    					
    					for (Antd antd : antds)
    						preds.add(Predicate.getPredicateFromRipperAntd(antd));
    					
    					conds.add(new LogicalPredicate(null, LogicalOperator.AND).addChildren(preds));
    				}
    			}
            }
            
            Predicate predicate;
            if (!commonAttributes.isEmpty())
            	predicate = new LogicalPredicate(null, LogicalOperator.AND).addChildren(conds);
            else 
            	predicate = new LogicalPredicate(null, LogicalOperator.OR).addChildren(conds);
            
            
            if (predicate.isEmpty()) {
            	conds = getLabel(cluster, selectedAttributes);
            	predicate = predicate.addChildren(conds);
            }
            
            return predicate;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static List<RipperRule> getRuleList(Set<XAttribute> selectedAttribs, List<FeatureVector> featureVectors, String relationName, Cluster cluster) throws Exception {
    	ArrayList<Attribute> attributes = new ArrayList<>();
    	
    	for (XAttribute attr : selectedAttribs) {
        	if (attr instanceof XAttributeContinuous || attr instanceof XAttributeDiscrete) {
                attributes.add(new Attribute(attr.getKey()));
                
        	} else {
        		Set<String> attVals = new HashSet<>();
        		featureVectors.forEach(fv -> {
        			attVals.addAll(fv.to.values().stream()
				    				.filter(att -> att.getClass().equals(attr.getClass()) 
				    							&& att.getKey().equals(attr.getKey()))
				    				.map(XAttribute::toString)
				    				.collect(Collectors.toSet())
        			);
        		});
        		
                attributes.add(new Attribute(attr.getKey(), List.copyOf(attVals)));
        	}
        }
        
        List<String> labels = Arrays.asList("positive", "negative");
        attributes.add(new Attribute("Class", labels));

        Instances data = new Instances(relationName, attributes, featureVectors.size());
        
        for (FeatureVector fv : featureVectors) {
            Instance inst = new DenseInstance(attributes.size());
            
            for (Attribute attr : attributes) {
            	String value;
                
            	if (attr.name().equals("Class")) {
            		value = cluster.getElements().contains(fv) ? "positive" : "negative";
            	
            	} else {
                	Optional<XAttribute> opt = fv.to.values().stream()
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
            		emptyInst.setValue(attr, "positive"); // The value of the class label is useless since it has no values for attributes
    		
    		data.add(emptyInst);
    	}
    	
    	if (data.classIndex() < 0)
            data.setClassIndex(data.numAttributes() - 1);
        
    	String[] options = new String[2];
		options[0] = "-N";
        options[1] = String.valueOf((double) Math.round(MIN_NODE_SIZE * featureVectors.size()));
        
        JRip classifier = new JRip();
        classifier.setOptions(options);
        classifier.buildClassifier(data);
        
        // This part is needed since the classifier can modify the order of labels in the class attribute
        Filter m_Filter = new ClassOrder();
        ((ClassOrder) m_Filter).setSeed(data.getRandomNumberGenerator(classifier.getSeed()).nextInt());
        ((ClassOrder) m_Filter).setClassOrder(ClassOrder.FREQ_ASCEND);
        m_Filter.setInputFormat(data);
        data = Filter.useFilter(data, m_Filter);
        
        // Updating classLabels to get the new label order
        classLabels = Collections.list(data.classAttribute().enumerateValues());
        
        List<RipperRule> ruleList = classifier.getRuleset().stream().map(rule -> (RipperRule)rule).collect(Collectors.toList());
    	return ruleList;
    }
    
    private static Set<Predicate> getLabel(Cluster cluster, Set<XAttribute> selectedAttributes) {
    	Set<Predicate> preds = new HashSet<>();
    	
        if (!cluster.getElements().isEmpty()) {
        	for (XAttribute attribute : selectedAttributes) {
        		
        		List<String> values = new LinkedList<>();
        		cluster.getElements().forEach(fv -> {
        			values.addAll(fv.to.values().stream()
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
