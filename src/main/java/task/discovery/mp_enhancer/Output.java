package task.discovery.mp_enhancer;

import java.util.List;

public class Output {

    private List<RuleEvaluationOutput> evaluationResults;
    private List<Double> execTimes;
    private double totalExecTime;

    public Output(List<RuleEvaluationOutput> evaluationResults, List<Double> execTimes, double totalExecTime) {
        this.evaluationResults = evaluationResults;
        this.execTimes = execTimes;
        this.totalExecTime = totalExecTime;
    }

    @Override
    public String toString() {
        String result = "";
        for (int i=0; i<evaluationResults.size(); i++){
            String result1= evaluationResults.get(i).getOutputAsString();
            String result1a=result1.replace("\n", "</br>");
            result1a+= "</br>Execution time: " + execTimes.get(i) + " sec\n";
            result+=result1a;
        }
        result+= "\nTotal Execution Time: " + totalExecTime + " sec";
        String result2 = result.replace("\n", "\\n");
        return  result2;
    }
}
