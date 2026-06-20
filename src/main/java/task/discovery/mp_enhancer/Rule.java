package task.discovery.mp_enhancer;

import controller.discovery.DataConditionType;
import task.discovery.data.Predicate;

public class Rule {
    private Predicate ruleAntecedents;
    private Predicate ruleConsequents;
    private int ruleLength;
    private DataConditionType type;

    public Rule(Predicate ants, Predicate conseqs) {
        this.ruleAntecedents = ants;
        this.ruleConsequents = conseqs;
        this.ruleLength = ants.getSize() + conseqs.getSize();
    }

    public String toString() {
    	return "| " + ruleAntecedents.toString() + " | " + ruleConsequents.toString(); // in order to get standard notation: | activation condition | target condition
    }

    public String toHtmlString() {
    	return "| " + ruleAntecedents.toTextualString() + " | " + ruleConsequents.toTextualString(); // it carries HTML codes for special symbols such as less or equal
    }
    
    public String toDeclareString() {
    	return "| " + ruleAntecedents.toDeclareString() + " | " + ruleConsequents.toDeclareString();
    }
    
    public Predicate getAntecedents() {
    	return this.ruleAntecedents;
    }
    
    public Predicate getConsequents() {
    	return this.ruleConsequents;
    }

    public int getRuleLength(){
        return this.ruleLength;
    }
    
    public DataConditionType getType() {
    	return this.type;
    }

	public void setType(DataConditionType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ruleAntecedents == null) ? 0 : ruleAntecedents.hashCode());
		result = prime * result + ((ruleConsequents == null) ? 0 : ruleConsequents.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rule other = (Rule) obj;
		if (ruleAntecedents == null) {
			if (other.ruleAntecedents != null)
				return false;
		} else if (!ruleAntecedents.equals(other.ruleAntecedents))
			return false;
		if (ruleConsequents == null) {
			if (other.ruleConsequents != null)
				return false;
		} else if (!ruleConsequents.equals(other.ruleConsequents))
			return false;
		return true;
	}
}
