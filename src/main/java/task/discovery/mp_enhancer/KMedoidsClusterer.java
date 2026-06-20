package task.discovery.mp_enhancer;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.impl.XAttributeBooleanImpl;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;

public class KMedoidsClusterer {
	private static final int ITERATION_NUM = 10000;
	private static final boolean CONSIDER_ACTIVATIONS = true;
	
    private int maxIterations;
    private int numberOfClusters;
    private Random rg;
    private Cluster[] output;

    private NumericAttributeSummary fromSummary;
    private NumericAttributeSummary toSummary;
    //private HashMap<StringPair, Double> editDistances;
    private Map<String, Map<StringPair, Double>> rangesDistances;

    private boolean considerActivations;

    public KMedoidsClusterer() {
        this(4, 100, true);
    }
    
    public KMedoidsClusterer(int numberOfClusters) {
    	this(numberOfClusters, ITERATION_NUM, CONSIDER_ACTIVATIONS);
    }

    public KMedoidsClusterer(int numberOfClusters, int maxIterations, boolean considerActivations) {
        this.numberOfClusters = numberOfClusters;
        this.maxIterations = maxIterations;
        this.rg = new Random(System.currentTimeMillis());

        //this.rangesSummary = new NumericAttributeSummary();
        //this.editDistances = new HashMap<>();
        this.rangesDistances = new HashMap<>();

        this.considerActivations = considerActivations;

        this.output = new Cluster[numberOfClusters];
        for(int i = 0; i < numberOfClusters; i++)
            output[i] = new Cluster();
    }
    
    public List<Cluster> clustering(List<FeatureVector> points) {
        //rangesSummary = Distance.computeRanges(points);
        //editDistances = Distance.computeEditDistances(points);
    	
    	fromSummary = new NumericAttributeSummary( points.stream().map(fv -> fv.from).collect(Collectors.toList()) );
    	toSummary = new NumericAttributeSummary( points.stream().map(fv -> fv.to).collect(Collectors.toList()) );
        rangesDistances = Distance.computeRangesDistances(points);

        // Reduce number of clusters in case it exceeds max possible number of clusters (might happen for example if one of the events has no payloads):
        /*if (rangesDistances.isEmpty() && rangesSummary.getAttrMax().get("from").isEmpty() && rangesSummary.getAttrMax().get("to").isEmpty()) {
            Set<Double> uniqueValues = new HashSet<>();
            for (Double value : editDistances.values())
                uniqueValues.add(value);
            
            int maxClusters = uniqueValues.size();

            //System.out.println("Max: " + maxClusters);
            if (numberOfClusters > maxClusters) {
                numberOfClusters = maxClusters;
                
                output = new Cluster[numberOfClusters];
                for(int i = 0; i < numberOfClusters; i++)
                    output[i] = new Cluster();
            }
        }*/

        FeatureVector[] medoids = new FeatureVector[numberOfClusters];
        
        for (int i = 0; i < numberOfClusters; i++) {
            int random = rg.nextInt(points.size());
            //try {
            	medoids[i] = points.get(random);
			//} catch (IndexOutOfBoundsException e) {
				/*
				 * This try-catch is not part of the original implementation
				 * It is added because otherwise an NPE may occur and MP mining will fail entirely
				 * Handling of this exception is probably incorrect as it can cause exceptions later in the code
				 * 		(but it seems to be enough to get the MP mining to complete in case of an NPE)
				 */
				
			//	System.out.println("There are less points than the numberOfClusters - trying to use the first point for medoids");
			//	medoids[i] = points.get(0);
			//}
        }

        boolean changed = true;
        int count = 0;

        while(changed && count < maxIterations) {
            changed = false;
            count++;
            int[] assignment = assign(medoids, points);
            changed = recalculateMedoids(assignment, medoids, points);
            //TODO: In some cases the algorithm never converges (reaches maxIterations), it happens when the clustering requires less
            // 		clusters than the value of numberOfClusters. In particular, the line: medoids[i] = points.get(rg.nextInt(points.size()));
            // 		in the "recalculateMedoids" method triggers "changed=true" each time a cluster has any element inside.
        }

        return Arrays.asList(output);
    }

    private int[] assign(FeatureVector[] medoids, List<FeatureVector> points) {
        int[] out = new int[points.size()];
        
        for (int i = 0; i < points.size(); i++) {
            double bestDistance = getDistance(points.get(i), medoids[0]);
            
            int bestIndex = 0;
            
            for (int j = 1; j < medoids.length; j++) {
                double tmpDistance = getDistance(points.get(i), medoids[j]);
                
                if (tmpDistance < bestDistance) {
                    bestDistance = tmpDistance;
                    bestIndex = j;
                }
            }
            out[i] = bestIndex;
        }
        
        return out;
    }
    
    private double getDistance(FeatureVector fv1, FeatureVector fv2) {
    	double outputDistance;
    	
    	if (considerActivations) {
    		outputDistance = Distance.computeDistance(fv1.to, fv2.to, toSummary, rangesDistances)
    							+ Distance.computeDistance(fv1.from, fv2.from, fromSummary, rangesDistances);
    		// Distance normalization
    		outputDistance /= (fv1.to.size() + fv2.to.size() + fv1.from.size() + fv2.from.size());
    		
    	} else {
    		outputDistance = Distance.computeDistance(fv1.to, fv2.to, toSummary, rangesDistances);
			// Distance normalization
			outputDistance /= (fv1.to.size() + fv2.to.size());
    	}
    	
    	return outputDistance;
    }

    private boolean recalculateMedoids(int[] assignment, FeatureVector[] medoids, List<FeatureVector> points) {
        boolean changed = false;

        for (int i = 0; i < numberOfClusters; i++) {
            output[i].setElements(new ArrayList<>());
            
            for (int j = 0; j < assignment.length; j++)
                if (assignment[j] == i)
                    output[i].getElements().add(points.get(j));
            
            if (output[i].getElements().isEmpty()) {
                medoids[i] = points.get(rg.nextInt(points.size()));
                changed = true;
            } else {
                FeatureVector centroid = getCentroid(output[i]);
                FeatureVector oldMedoid = medoids[i];
                medoids[i] = kNearest(1, centroid, points).iterator().next();
                if (!medoids[i].equals(oldMedoid))
                    changed = true;
            }
        }
        
        return changed;
    }

    private FeatureVector getCentroid(Cluster cluster) {
        XAttributeMap from = new XAttributeMapImpl();
        
        List<XAttributeMap> froms = cluster.getElements().stream().map(fv -> fv.from).collect(Collectors.toList());
        
        // The centroid is computed only over the set of attributes common to all the feature vectors
        Set<XAttribute> attributesFrom = CorrelationMiner.getCommonElements(froms);
        
        if (attributesFrom.isEmpty())	// If it is empty, then the cluster considers all attributes
        	for (XAttributeMap f : froms)
        		attributesFrom.addAll(f.values());
        
        for (XAttribute attribute : attributesFrom) {
        	List<XAttribute> xAttributes = cluster.getElements().stream()
														.map(fv -> fv.from.values().stream()
															.filter(att -> att.getClass().equals(attribute.getClass()) 
								    									&& att.getKey().equals(attribute.getKey()))
															.findAny().orElse(null) )
														.filter(att -> att != null)
														.distinct().collect(Collectors.toList());
        	
        	// All attributes should belong to the same class, so only the first one is checked
        	if (xAttributes.get(0) instanceof XAttributeContinuous) {
        		double sum = 0d;
        		for (XAttribute att : xAttributes)
        			sum += ((XAttributeContinuous) att).getValue();
        		
        		from.put(attribute.getKey(), new XAttributeContinuousImpl(attribute.getKey(), sum/xAttributes.size()) );
        	
        	} else if (xAttributes.get(0) instanceof XAttributeDiscrete) {
        		double sum = 0d;
        		for (XAttribute att : xAttributes)
        			sum += (double) ((XAttributeDiscrete) att).getValue();
        		
        		from.put(attribute.getKey(), new XAttributeContinuousImpl(attribute.getKey(), sum/xAttributes.size()) );
        	
        	} else if (xAttributes.get(0) instanceof XAttributeBoolean) {
        		int trueCount = 0;
        		int falseCount = 0;
        		
        		for (XAttribute att : xAttributes) {
        			if ( ((XAttributeBoolean) att).getValue() )
        				trueCount++;
        			else
        				falseCount++;
        		}
        		
        		boolean val = (trueCount >= falseCount);
        		from.put(attribute.getKey(), new XAttributeBooleanImpl(attribute.getKey(), val));
        	
        	} else {	// Literal (string) attribute
                HashMap<String, Integer> categories = new HashMap<>();
                
                for (XAttribute att : xAttributes) {
                	String value = ((XAttributeLiteral) att).getValue();
                    if (categories.containsKey(value))
                        categories.put(value, categories.get(value) + 1);
                    else
                        categories.put(value, 1);
                }
                
                from.put(
                		attribute.getKey(), 
                		new XAttributeLiteralImpl(attribute.getKey(), Collections.max(categories.entrySet(), comparingByValue()).getKey())
                );
            }
        }

        XAttributeMap to = new XAttributeMapImpl();
        
        List<XAttributeMap> tos = cluster.getElements().stream().map(fv -> fv.to).collect(Collectors.toList());
        
        // The centroid is computed only over the set of attributes common to all the feature vectors
        Set<XAttribute> attributesTo = CorrelationMiner.getCommonElements(tos);
        
        if (attributesTo.isEmpty())	// If it is empty, then the cluster considers all attributes
        	for (XAttributeMap t : tos)
        		attributesTo.addAll(t.values());
        
        for (XAttribute attribute : attributesTo) {
        	List<XAttribute> xAttributes = cluster.getElements().stream()
														.map(fv -> fv.to.values().stream()
															.filter(att -> att.getClass().equals(attribute.getClass()) 
									    								&& att.getKey().equals(attribute.getKey()))
															.findAny().orElse(null) )
														.filter(att -> att != null)
														.distinct().collect(Collectors.toList());

        	// All attributes should belong to the same class, so only the first one is checked
        	if (xAttributes.get(0) instanceof XAttributeContinuous) {
        		double sum = 0d;
        		for (XAttribute att : xAttributes)
        			sum += ((XAttributeContinuous) att).getValue();
        		
        		to.put(attribute.getKey(), new XAttributeContinuousImpl(attribute.getKey(), sum/xAttributes.size()) );
        	
        	} else if (xAttributes.get(0) instanceof XAttributeDiscrete) {
        		double sum = 0d;
        		for (XAttribute att : xAttributes)
        			sum += (double) ((XAttributeDiscrete) att).getValue();
        		
        		to.put(attribute.getKey(), new XAttributeContinuousImpl(attribute.getKey(), sum/xAttributes.size()) );
        	
        	} else if (xAttributes.get(0) instanceof XAttributeBoolean) {
        		int trueCount = 0;
        		int falseCount = 0;
        		
        		for (XAttribute att : xAttributes) {
        			if ( ((XAttributeBoolean) att).getValue() )
        				trueCount++;
        			else
        				falseCount++;
        		}
        		
        		boolean val = (trueCount >= falseCount);
        		to.put(attribute.getKey(), new XAttributeBooleanImpl(attribute.getKey(), val));
        	
        	} else {	// Literal (string) attribute
                HashMap<String, Integer> categories = new HashMap<>();
                
                for (XAttribute att : xAttributes) {
                	String value = ((XAttributeLiteral) att).getValue();
                    if (categories.containsKey(value))
                        categories.put(value, categories.get(value) + 1);
                    else
                        categories.put(value, 1);
                }
                
                to.put(
                		attribute.getKey(), 
                		new XAttributeLiteralImpl(attribute.getKey(), Collections.max(categories.entrySet(), comparingByValue()).getKey())
                );
            }
        }
        
        FeatureVector centroid = new FeatureVector(from, to);
        return centroid;
    }

    List<FeatureVector> kNearest(int k, FeatureVector centroid, List<FeatureVector> points) {
        List<FeatureVector> knearest = new ArrayList<>();
        Map<FeatureVector, Double> distances = new HashMap<>();
        
        for (FeatureVector fv: points)
            distances.put(fv, Math.abs(getDistance(fv, centroid)));
            
        distances = distances.entrySet().stream().sorted(comparingByValue()).collect(toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2, LinkedHashMap::new));

        for (int i = 0; i < k; i++)
            knearest.add(distances.keySet().toArray(new FeatureVector[distances.size()])[i]);
        
        return knearest;
    }
}
