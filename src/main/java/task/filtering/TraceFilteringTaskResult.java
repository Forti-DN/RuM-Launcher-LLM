package task.filtering;

import org.processmining.plugins.ltlchecker.InstanceModel;
import org.processmining.plugins.ltlchecker.RuleModel;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TraceFilteringTaskResult {

    public List<Map<RuleModel, LinkedList<InstanceModel>>> filtersAndTraceResult =  new LinkedList<>();
    public LinkedList<InstanceModel> listOfTraces = new LinkedList<>();

    public void addFiltersAndCorrespondingResult(Map<RuleModel, LinkedList<InstanceModel>> filtersAndTraceResult){
        this.filtersAndTraceResult.add(filtersAndTraceResult);
    }

    public List<Map<RuleModel, LinkedList<InstanceModel>>> getFilterToResults(){
        return this.filtersAndTraceResult;
    }

    public void addToListOfTraces(LinkedList<InstanceModel> listOfTraces){
        this.listOfTraces.addAll(listOfTraces);
    }

    public LinkedList<InstanceModel> getListOfTraces(){
        return this.listOfTraces;
    }

}
