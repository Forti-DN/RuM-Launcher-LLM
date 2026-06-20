package task.discovery.mp_enhancer;

import java.util.Objects;

import org.deckfour.xes.model.XAttributeMap;
import task.discovery.data.Predicate;

public class FeatureVector {
	public boolean isFulfillment; // it refers to CONTROL-FLOW fulfillment
	public Predicate label;
    public XAttributeMap from;
    public XAttributeMap to;
    
    public FeatureVector(XAttributeMap from, XAttributeMap to) {
    	this.isFulfillment = false;
        this.from = from;
        this.to = to;
    }
    
    public FeatureVector(boolean isFulfillment, XAttributeMap from, XAttributeMap to) {
    	this.isFulfillment = isFulfillment;
        this.from = from;
        this.to = to;
    }

    public String toString() {
        return from + " => " + to + " label = " + label;
    }

	@Override
	public int hashCode() {
		return Objects.hash(from, isFulfillment, label, to);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FeatureVector other = (FeatureVector) obj;
		return Objects.equals(from, other.from) && isFulfillment == other.isFulfillment
				&& Objects.equals(label, other.label) && Objects.equals(to, other.to);
	}
}