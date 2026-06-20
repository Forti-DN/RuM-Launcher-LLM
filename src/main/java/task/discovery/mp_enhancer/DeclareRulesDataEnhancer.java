package task.discovery.mp_enhancer;

import org.deckfour.xes.model.XLog;

import controller.discovery.data.DiscoveredConstraint;

import java.util.ArrayList;
import java.util.List;

public class DeclareRulesDataEnhancer {

    private Integer numberOfClusters; // 3; // 1
    private Double minNodeSize; // 0.2;   // 0.05;
    private Boolean pruning; // true; // false
    private Boolean considerViolations; //true
    private Boolean considerActivations; // true;
    private ArrayList<ArrayList<String>> declareConstraints;
    private XLog xLog;

    public DeclareRulesDataEnhancer(Integer numberOfClusters, Double minNodeSize, Boolean pruning, Boolean considerViolations,
                                    Boolean considerActivations, ArrayList<ArrayList<String>> declareConstraints, XLog xLog) {
        this.numberOfClusters = numberOfClusters;
        this.minNodeSize = minNodeSize;
        this.pruning = pruning;
        this.considerViolations = considerViolations;
        this.considerActivations = considerActivations;
        this.declareConstraints = declareConstraints;
        this.xLog = xLog;
    }

    public String addDataAwareConditions(){
        List<DiscoveredConstraint> constraints = LogReader.readConstraintsFromRuM(declareConstraints);
        Output output = CorrelationMiner.findCorrelations_old(xLog, constraints, considerViolations, numberOfClusters, minNodeSize, pruning, considerActivations);
        String result = output.toString();
        //System.out.println(result);
        return result;
    }
}
