package task.filtering;

import org.processmining.plugins.ltlchecker.InstanceModel;

import java.util.LinkedList;

public class EventFilteringTaskResult {
    public LinkedList<InstanceModel> eventFiltersListOfData=  new LinkedList<>();
    public int selectedFilterIndex;

    public void addFiltersAndCorrespondingResult(LinkedList<InstanceModel> eventFiltersListOfData){
        this.eventFiltersListOfData= eventFiltersListOfData;
    }

    public LinkedList<InstanceModel> getResultAfterAppliedEvenFilters(){
        return this.eventFiltersListOfData;
    }

    public void setSelectedFilterIndex(int selectedIndex){
        this.selectedFilterIndex =selectedIndex;
    }
    public int getSelectedFilterIndex(){
        return this.selectedFilterIndex;
    }
}
