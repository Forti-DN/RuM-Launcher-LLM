package task.discovery.mp_enhancer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;

import controller.discovery.data.DiscoveredConstraint;

public final class RulesExtractor {
	
	public static Map<XTrace, List<FeatureVector>> extractFeatureVectors(XLog log, DiscoveredConstraint declareConstraint, boolean considerLifecycle) {
		Map <XTrace, List<FeatureVector>> fvs = new HashMap<>();
				
		for (XTrace trace : log) {
        	TreeSet<Integer> actSet = new TreeSet<>();
        	TreeSet<Integer> trgSet = new TreeSet<>();
        	SortedSet<Integer> fulfilledIndices = new TreeSet<>();
            
            for (XEvent event : trace) {
            	if (isNameMatching(event, declareConstraint.getActivationActivity().getActivityFullName(), considerLifecycle))
            		actSet.add(trace.indexOf(event));
            	
            	if (isNameMatching(event, declareConstraint.getTargetActivity().getActivityFullName(), considerLifecycle))
            		trgSet.add(trace.indexOf(event));
            }
            
    		switch (declareConstraint.getTemplate()) {
            case Response:
            	for (int actInd : actSet)
            		if (trgSet.ceiling(actInd+1) != null)
            			fulfilledIndices.add(actInd);
            	
            	break;
            	
            case Alternate_Response:
            	for (int actInd : actSet) {
            		if (trgSet.ceiling(actInd+1) != null) {
            			
            			if (actSet.ceiling(actInd+1) == null)
            				fulfilledIndices.add(actInd);
            			else if (actSet.ceiling(actInd+1) > trgSet.ceiling(actInd+1))
            				fulfilledIndices.add(actInd);
            		}
            	}
            	break;
            	
            case Chain_Response:
            	for (int actInd : actSet) 
            		if (trgSet.contains(actInd+1))
            			fulfilledIndices.add(actInd);
                		
            	break;
            	
            case Precedence:
            	for (int actInd : actSet)
            		if (trgSet.floor(actInd-1) != null)
            			fulfilledIndices.add(actInd);
            	
            	break;
            	
    		case Alternate_Precedence:
            	for (int actInd : actSet) {
            		if (trgSet.floor(actInd-1) != null) {
            			
            			if (actSet.floor(actInd-1) == null)
            				fulfilledIndices.add(actInd);
            			else if (actSet.floor(actInd-1) < trgSet.floor(actInd-1))
            				fulfilledIndices.add(actInd);
            		}
            	}
            	break;
            	
    		case Chain_Precedence:
            	for (int actInd : actSet)
            		if (trgSet.contains(actInd-1))
            			fulfilledIndices.add(actInd);
                	
            	break;
            	
    		case Responded_Existence:
    			if (!trgSet.isEmpty())
    				fulfilledIndices.addAll(actSet);
    			
    			break;
    			
    		default:
    			throw new UnsupportedOperationException("Data conditions extraction not (yet) supported for template: " + declareConstraint.getTemplate().toString());
    		}
    		
    		List<FeatureVector> traceFvs = new LinkedList<>();
    		
    		for (int actInd : actSet) {
    			if (fulfilledIndices.contains(actInd)) {
    				XAttributeMap fulfillmentsTo = new XAttributeMapImpl();
    				
    				switch (declareConstraint.getTemplate()) {
    				case Response:
    				case Alternate_Response:
    				case Chain_Response:
    					fulfillmentsTo.putAll( getPayload( trace.get(trgSet.ceiling(actInd+1)) ));
    					break;
    					
    				case Precedence:
    				case Alternate_Precedence:
    				case Chain_Precedence:
    					fulfillmentsTo.putAll( getPayload( trace.get(trgSet.floor(actInd-1)) ));
    					break;
    					
    				case Responded_Existence:
    					int closestFloor = trgSet.floor(actInd-1)!=null ? trgSet.floor(actInd-1) : Integer.MAX_VALUE;
            			int closestCeil = trgSet.ceiling(actInd+1)!=null ? trgSet.ceiling(actInd+1) : Integer.MAX_VALUE;
            			
            			int trgInd = Math.abs(actInd-closestFloor) < Math.abs(actInd-closestCeil) ? closestFloor : closestCeil;
            			fulfillmentsTo.putAll( getPayload( trace.get(trgInd) ));
            			break;
            			
    				default:
    	    			throw new UnsupportedOperationException("Data conditions extraction not (yet) supported for template: " + declareConstraint.getTemplate().toString());
    				}
    				
    				traceFvs.add(new FeatureVector(true, getPayload(trace.get(actInd)), fulfillmentsTo));
    			
    			} else
    				traceFvs.add(new FeatureVector(false, getPayload(trace.get(actInd)), new XAttributeMapImpl()));
    		}
    		
    		fvs.put(trace, traceFvs);
		}

		return fvs;
	}
	
	private static boolean isNameMatching(XEvent event, String activity, boolean considerLifecycle) {
    	String activityName = activity;
		String activityTransition = null;
    	
    	if (considerLifecycle) {	// The pattern of the activity name is: nameOfActivity-transition
    		int dashIndex = activity.lastIndexOf('-');
    		activityName = activity.substring(0, dashIndex);
    		activityTransition = activity.substring(dashIndex+1);
    	}
    	
    	if (activityName.equals( XConceptExtension.instance().extractName(event) )) {
    		if (considerLifecycle) {
    			if (activityTransition.equals( XLifecycleExtension.instance().extractTransition(event) ))
    				return true;
    		
    		} else {
    			return true;
    		}
    	}
    	
    	return false;
	}
	
	public static XAttributeMap getPayload(XEvent event) {
		XAttributeMap payload = new XAttributeMapImpl();
		
		for (var entry : event.getAttributes().entrySet()) {
			String attName = entry.getKey();
			
			if (!attName.equals(XConceptExtension.KEY_NAME) 
					&& !attName.equals(XTimeExtension.KEY_TIMESTAMP) 
					&& !attName.equals(XLifecycleExtension.KEY_TRANSITION))
				
				payload.put(attName, entry.getValue());
		}
		
		return payload;
	}
}
