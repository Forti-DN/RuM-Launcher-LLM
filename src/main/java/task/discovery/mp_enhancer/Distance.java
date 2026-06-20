package task.discovery.mp_enhancer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;

import util.Parsers;

public final class Distance {
    public static double computeDistance(XAttributeMap map1, XAttributeMap map2, NumericAttributeSummary summary,
		    							Map<String, Map<StringPair, Double>> rangesDistances) {
        double distance = 0.0;
        
        Set<String> attributeNames = new HashSet<String>();
        attributeNames.addAll(map1.keySet());
        attributeNames.addAll(map2.keySet());
        
        for (String key : attributeNames) {
            if (map1.containsKey(key) && map2.containsKey(key)) {
            	XAttribute attribute1 = map1.get(key);
            	XAttribute attribute2 = map2.get(key);
            	
            	if ( (attribute1 instanceof XAttributeContinuous || attribute1 instanceof XAttributeDiscrete)
            			&& (attribute2 instanceof XAttributeContinuous || attribute2 instanceof XAttributeDiscrete)) {
            		
            		double val1, val2;
            		
            		if (attribute1 instanceof XAttributeContinuous)
            			val1 = ((XAttributeContinuous) attribute1).getValue();
            		else
            			val1 = (double) ((XAttributeDiscrete) attribute1).getValue();
            		
            		if (attribute2 instanceof XAttributeContinuous)
            			val2 = ((XAttributeContinuous) attribute2).getValue();
            		else
            			val2 = (double) ((XAttributeDiscrete) attribute2).getValue();
            		
            		double maxVal = summary.getMaxValuesMap().get(key);
            		double minVal = summary.getMinValuesMap().get(key);
            		distance += computeDistance(val1, val2, maxVal, minVal);
            	
            	} else if (attribute1 instanceof XAttributeBoolean && attribute2 instanceof XAttributeBoolean) {
            		XAttributeBoolean bool1 = (XAttributeBoolean) attribute1;
            		XAttributeBoolean bool2 = (XAttributeBoolean) attribute2;
            		
            		distance += computeDistance(bool1.getValue(), bool2.getValue());
            		
            	} else {	// Literal (string) attributes
            		XAttributeLiteral lit1 = (XAttributeLiteral) attribute1;
            		XAttributeLiteral lit2 = (XAttributeLiteral) attribute2;
            		
            		if (isRange(lit1.getValue()) && isRange(lit2.getValue()) ) {
            			StringPair pair = new StringPair(lit1.getValue(), lit2.getValue());
                        distance += rangesDistances.get(key).get(pair);
                        
            		} else {
                        distance += computeDistance(lit1.getValue(), lit2.getValue());
                    }
            	}

        	} //else
                //distance += 1.0;
        }
        
        return distance;
    }

    public static double computeDistance(double value1, double value2, double max, double min) {
        if(Double.isNaN(value1) || Double.isNaN(value2))
            return 1.0;
        double value = (Math.abs(value1 - value2))/(Math.abs(max - min));
        if(Double.isNaN(value))
            return 0.0;
        else
            return value;
    }

    public static double computeDistance(boolean value1, boolean value2) {
    	
    	return value1 == value2 ? 0d : 1d;
    }

    public static double computeDistance(String value1, String value2) { //this computes "edit distance" at the moment... :(
    	//TODO: check what's wrong with the currently not used calculateEditDistance method!
    	return value1.equals(value2) ? 0d : 1d;
    }

    public static Map<String, Map<StringPair, Double>> computeRangesDistances(List<FeatureVector> featureVectorList) {
        Map<String, Map<StringPair, Double>> rangesDistances = new HashMap<>();
        
        if (!featureVectorList.isEmpty()) {
        	
        	Set<String> attributesFrom = new HashSet<>();
            for (Set<String> set : featureVectorList.stream().map(fv -> fv.from.keySet()).collect(Collectors.toSet()))
            	attributesFrom.addAll(set);
            
            for (String attribute : attributesFrom) {
            	List<String> values = featureVectorList.stream()
														.map(fv -> fv.from.values().stream()
															.filter(att -> att.getKey().equals(attribute))
															.findAny().orElse(null) )
														.filter(att -> att != null)
														.map(att -> att.toString())
														.distinct().collect(Collectors.toList());

	        	if (isRange(values.get(0))) {
	                Map<String, Double> means = computeRangesMeans(values);
	                Double min = Collections.min(means.values());
	                Double max = Collections.max(means.values());
	                
	                Map<StringPair, Double> distances = new HashMap<>();
	                for(int i = 0; i < values.size(); i++)
	                    for (String value : values)
	                        distances.put(new StringPair(values.get(i), value), (Math.abs(means.get(values.get(i)) - means.get(value)) / (max - min)));
	                
	                rangesDistances.put(attribute, distances);
	            }
	        }
        	
            Set<String> attributesTo = new HashSet<>();
            for (Set<String> set : featureVectorList.stream().map(fv -> fv.to.keySet()).collect(Collectors.toSet()))
            	attributesTo.addAll(set);
            
            for (String attribute : attributesTo) {
            	List<String> values = featureVectorList.stream()
														.map(fv -> fv.to.values().stream()
															.filter(att -> att.getKey().equals(attribute))
															.findAny().orElse(null) )
														.filter(att -> att != null)
														.map(att -> att.toString())
														.distinct().collect(Collectors.toList());

	        	if (isRange(values.get(0))) {
	                Map<String, Double> means = computeRangesMeans(values);
	                Double min = Collections.min(means.values());
	                Double max = Collections.max(means.values());
	                
	                Map<StringPair, Double> distances = new HashMap<>();
	                for(int i = 0; i < values.size(); i++)
	                    for (String value : values)
	                        distances.put(new StringPair(values.get(i), value), (Math.abs(means.get(values.get(i)) - means.get(value)) / (max - min)));
	                
	                rangesDistances.put(attribute, distances);
	            }
	        }
        }
        
        return rangesDistances;
    }

    public static Map<String, Double> computeRangesMeans(List<String> ranges){
        Map<String, Double> rangesMeans = new HashMap<>();
        for(String range: ranges){
            String[] values = range.split("-");
            rangesMeans.put(range,(Double.parseDouble(values[0]) + Double.parseDouble(values[1]))/2);
        }
        return rangesMeans;
    }

    public static int calculateEditDistance(String value1, String value2){
        if(value1.equals(value2))
            return 0;
        else{
            int edits[][]=new int[value1.length()+1][value2.length()+1];
            for(int i=0;i<=value1.length();i++)
                edits[i][0]=i;
            for(int j=1;j<=value2.length();j++)
                edits[0][j]=j;
            for(int i=1;i<=value1.length();i++){
                for(int j=1;j<=value2.length();j++){
                    int u=(value1.charAt(i-1)==value2.charAt(j-1)?0:1);
                    edits[i][j]=Math.min(
                            edits[i-1][j]+1,
                            Math.min(
                                    edits[i][j-1]+1,
                                    edits[i-1][j-1]+u
                            )
                    );
                }
            }
            return edits[value1.length()][value2.length()];
        }
        //else
        //    return 1;
    }

    public static boolean isRange(String value) {
        String[] values = value.split("-");
        return values.length == 2 && Parsers.tryParseDouble(values[0]) && Parsers.tryParseDouble(values[1]);
    }
}
