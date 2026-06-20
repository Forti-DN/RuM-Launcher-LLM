package task.filtering;

import controller.filtering.*;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.plugins.ltlchecker.InstanceModel;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class EventFilteringTask extends Task<EventFilteringTaskResult> {
	
	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
    FilteringTabController filteringTabController;
    ObservableList<Filter> selectedFiltersListView;

    boolean isFilterInspector;

    private List<Filter> getEventFilters(ObservableList<Filter> selectedFilters) {
        return selectedFilters.stream().filter(filter -> filter.getFilteringMode().equals("keepSelectedMode") || filter.getFilteringMode().equals("trimToTimeframe") || filter.getFilteringMode().equals("trimFirst") || filter.getFilteringMode().equals("trimLongest")).collect(Collectors.toList());
    }

    public void setFilteringTabController(FilteringTabController filteringTabController) {
        this.filteringTabController = filteringTabController;
    }

    public void setSelectedFiltersListView(ObservableList<Filter> selectedFiltersListView) {
        this.selectedFiltersListView = selectedFiltersListView;
    }

    public void setIsFilterInspector(boolean isFilterInspector){
        this.isFilterInspector=isFilterInspector;
    }

    @Override
    protected EventFilteringTaskResult call() throws Exception {
    	long taskStartTime = System.currentTimeMillis();
		logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);
    	
        EventFilteringTaskResult eventFilteringTaskResult = new EventFilteringTaskResult();

        if (isFilterInspector) {
            int indexOfSelectedFilter_FilterInspector = filteringTabController.resultsViewController.filtersInspector.filtersListView.getSelectionModel().getSelectedIndex();
            eventFilteringTaskResult.setSelectedFilterIndex(indexOfSelectedFilter_FilterInspector);
            LinkedList<InstanceModel> eventFiltersListOfData_FilterInspector = FilteringPageController.deepCloneTracesList(filteringTabController.filtersAndTraceResult.get(indexOfSelectedFilter_FilterInspector).get(filteringTabController.filtersAndTraceResult.get(indexOfSelectedFilter_FilterInspector).keySet().toArray()[0]));

            for (Filter currentFilter : getEventFilters(selectedFiltersListView)) {
                if (filteringTabController.xLog.isEmpty()) {
                    break;
                }
                Filter.applyFilterAux(currentFilter.getCategory(), currentFilter.getFilteringMode(), currentFilter.getAttribute(), currentFilter.getStartValue(), currentFilter.getEndValue(), eventFiltersListOfData_FilterInspector, filteringTabController.xLog);
                filteringTabController.xLog = FilteringUtils.createLogFromInstances(eventFiltersListOfData_FilterInspector);
            }
            eventFilteringTaskResult.addFiltersAndCorrespondingResult(eventFiltersListOfData_FilterInspector);

        }
        else {
            int indexOfSelectedFilter_TraceInspector = filteringTabController.resultsViewController.traceInspector.tracesListView.getSelectionModel().getSelectedIndex();
            eventFilteringTaskResult.setSelectedFilterIndex(indexOfSelectedFilter_TraceInspector);

            LinkedList<InstanceModel> eventFiltersListOfData_TraceInspector = FilteringPageController.deepCloneTracesList(new LinkedList<>(filteringTabController.unmodifiedTraces));
            for (Filter currentFilter : getEventFilters(selectedFiltersListView)) {
                if (filteringTabController.xLog.isEmpty()) {
                    break;
                }
                Filter.applyFilterAux(currentFilter.getCategory(), currentFilter.getFilteringMode(), currentFilter.getAttribute(), currentFilter.getStartValue(), currentFilter.getEndValue(), eventFiltersListOfData_TraceInspector, filteringTabController.xLog);
                filteringTabController.xLog = FilteringUtils.createLogFromInstances(eventFiltersListOfData_TraceInspector);
            }

            eventFilteringTaskResult.addFiltersAndCorrespondingResult(eventFiltersListOfData_TraceInspector);
        }

        logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);
        
        return eventFilteringTaskResult;
    }

}
