package task.discovery.mp_enhancer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeMap;

public class NumericAttributeSummary {
	private Map<String, Double> minValuesMap;
	private Map<String, Double> maxValuesMap;
	
	public NumericAttributeSummary(List<XAttributeMap> attributeMapList) {
		this.minValuesMap = new HashMap<>();
		this.maxValuesMap = new HashMap<>();
		
		Set<String> attributeNames = new HashSet<>();
        for (Set<String> set : attributeMapList.stream().map(attMap -> attMap.keySet()).collect(Collectors.toSet()))
        	attributeNames.addAll(set);
		
        for (String name : attributeNames) {
        	List<XAttribute> numericAttributes = attributeMapList.stream()
												.map(map -> map.values().stream()
													.filter(att -> att.getKey().equals(name))
													.findAny().orElse(null) )
												.filter(att -> att != null)
												.filter(att -> att instanceof XAttributeContinuous || att instanceof XAttributeDiscrete)
												.distinct().collect(Collectors.toList());
        	
        	if (!numericAttributes.isEmpty()) {
        		double min = numericAttributes.stream().mapToDouble(att -> Double.parseDouble(att.toString())).min().getAsDouble();
        		double max = numericAttributes.stream().mapToDouble(att -> Double.parseDouble(att.toString())).max().getAsDouble();
        		minValuesMap.put(name, min);
        		maxValuesMap.put(name, max);
        	}
        }
	}

	public Map<String, Double> getMaxValuesMap() {
		return maxValuesMap;
	}

	public Map<String, Double> getMinValuesMap() {
		return minValuesMap;
	}
}