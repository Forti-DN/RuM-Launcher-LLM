package task.discovery.mp_enhancer;

import java.util.ArrayList;
import java.util.List;

public class RuleEvaluationOutput {

	private String constraintName;
	private List<Rule> rules;
    private String outputAsString;

    public RuleEvaluationOutput(String constraintName, List<Rule> rules) {
    	this.constraintName = constraintName;
        this.rules = rules;
        
        List<Integer> lengths = new ArrayList<>();
        String output = "\n<b>" + this.constraintName + "</b>\n";
        
        if (!rules.isEmpty()) {
        	
	        for (Rule rule : this.rules) {
	        	output += "\n" + rule.toString() + "\n";
	        	lengths.add(rule.getRuleLength());
	        }
	        	        
	        output += "\nAverage rule length: " + String.format("%.2f", lengths.stream().mapToDouble(e -> e).average().getAsDouble());
	        output += "\nRules in total: " + rules.size();
        }
        
        this.outputAsString = output;
    }
    
    public String getConstraintName() {
    	return this.constraintName;
    }
    
    public List<Rule> getRules() {
		return this.rules;
	}

    public String getOutputAsString() {
        return outputAsString;
    }
}
