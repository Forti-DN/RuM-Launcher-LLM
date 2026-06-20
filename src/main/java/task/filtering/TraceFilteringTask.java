package task.filtering;

import controller.filtering.*;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.ltlchecker.InstanceModel;
import org.processmining.plugins.ltlchecker.RuleModel;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TraceFilteringTask extends Task<TraceFilteringTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    private XLog xLog;
    private FilteringTabController filteringTabController;
    private TraceFilteringTaskResult traceFilteringTaskResult;
    private final List<String> eventFilterModes = Arrays.asList("keepSelectedMode", "trimToTimeframe", "trimLongest", "trimFirst");

    protected enum RuleType {
        SINGLE_TRACE_FILTER,
        ALL_TRACE_FILTERS,
    }


    public void setFilteringTabController(FilteringTabController filteringTabController) {
        this.filteringTabController = filteringTabController;
    }

    public void setXLog(XLog xLog) {
        this.xLog = xLog;
    }

    public void calculateMatchingRatio(LinkedList<InstanceModel> listOfTraces, ObservableList<InstanceModel> originalTraces) {
        originalTraces.forEach(trace -> listOfTraces.stream().filter(newTrace -> trace.getInstance().getAttributes().get("concept:name").toString().equals(newTrace.getInstance().getAttributes().get("concept:name").toString())).findFirst().ifPresent(newTrace -> trace.setHealthDegree(trace.getHealthDegree() + 1)));
    }

    private double calculateCoverage(LinkedList<InstanceModel> listOfTraces, int numberOfOriginalCases) {
        return listOfTraces.size() == 0 ? 0 : (double) listOfTraces.size() / numberOfOriginalCases * 100;
    }

    private String determineDescription(Filter filter) {
        if (filter != null) {
            return String.valueOf(isEventFilter(filter.getFilteringMode()) ? Filter.FilterType.EVENT_FILTER : Filter.FilterType.TRACE_FILTER);
        } else {
            return String.valueOf(Filter.FilterType.TRACE_FILTER);
        }
    }
    private List<Filter> getTraceFilters(ObservableList<Filter> selectedFilters) {
        return selectedFilters.stream()
                .filter(filter -> !isEventFilter(filter.getFilteringMode()))
                .collect(Collectors.toList());
    }

    private boolean isEventFilter(String filteringMode) {
        return eventFilterModes.contains(filteringMode);
    }

    private RuleModel createRuleModel(LinkedList<InstanceModel> listOfTraces, Filter filter, int numberOfOriginalCases) {
        RuleModel newFilter = new RuleModel();
        newFilter.setRuleName(filter != null ? filter.toString() : "All filters applied");
        newFilter.setCoverage(calculateCoverage(listOfTraces, numberOfOriginalCases));
        newFilter.setDescription(determineDescription(filter));
        return newFilter;
    }

    private HashMap<RuleModel, LinkedList<InstanceModel>> createFilterAndTraceMapping(LinkedList<InstanceModel> listOfTraces, LinkedList<RuleModel> currentFiltersForInspector, Filter filter, int numberOfOriginalCases) {
        HashMap<RuleModel, LinkedList<InstanceModel>> filterAndTrace = new HashMap<>();
        RuleModel newFilter = createRuleModel(listOfTraces, filter, numberOfOriginalCases);
        filterAndTrace.put(newFilter, listOfTraces);
        currentFiltersForInspector.add(newFilter);
        return filterAndTrace;
    }

    private void applySingleFilter(Filter filter, ObservableList<InstanceModel> originalTraces, LinkedList<InstanceModel> mutatedTraces, XLog xLog, ObservableList<InstanceModel> unmodifiedTraces, LinkedList<RuleModel> currentFiltersForInspector) {
        Filter.applyFilterAux(filter.getCategory(), filter.getFilteringMode(), filter.getAttribute(), filter.getStartValue(), filter.getEndValue(), mutatedTraces, xLog);
        calculateMatchingRatio(mutatedTraces, originalTraces);
        traceFilteringTaskResult.addFiltersAndCorrespondingResult(createFilterAndTraceMapping(mutatedTraces, currentFiltersForInspector, filter, unmodifiedTraces.size()));
    }

    private void applyAllFilters(ObservableList<Filter> selectedFilters, LinkedList<InstanceModel> mutatedTraces, XLog xLog, ObservableList<InstanceModel> unmodifiedTraces, LinkedList<RuleModel> currentFiltersForInspector) {
        for (Filter currentFilter : getTraceFilters(selectedFilters)) {
            if (xLog.isEmpty()) {
                break;
            }
            Filter.applyFilterAux(currentFilter.getCategory(), currentFilter.getFilteringMode(), currentFilter.getAttribute(), currentFilter.getStartValue(), currentFilter.getEndValue(), mutatedTraces, xLog);
            xLog = FilteringUtils.createLogFromInstances(mutatedTraces);
        }
        traceFilteringTaskResult.getListOfTraces().clear();
        traceFilteringTaskResult.addToListOfTraces(mutatedTraces);
        traceFilteringTaskResult.addFiltersAndCorrespondingResult(createFilterAndTraceMapping(mutatedTraces, currentFiltersForInspector, null, unmodifiedTraces.size()));
    }

    public void applyFilter(LinkedList<InstanceModel> listOfTraces, Filter currentFilter, RuleType type, ObservableList<Filter> selectedFilters, ObservableList<InstanceModel> originalTraces, LinkedList<RuleModel> currentFiltersForInspector) {
        LinkedList<InstanceModel> mutatedTraces = new LinkedList<>(FilteringPageController.deepCloneTracesList(listOfTraces));
        if (type == RuleType.ALL_TRACE_FILTERS) {
            applyAllFilters(selectedFilters, new LinkedList<>(FilteringPageController.deepCloneTracesList(mutatedTraces)), (XLog) xLog.clone(), originalTraces, currentFiltersForInspector);
        }
        else if (type == RuleType.SINGLE_TRACE_FILTER && !eventFilterModes.contains(currentFilter.getFilteringMode()) && traceFilteringTaskResult.getFilterToResults().stream().flatMap(map -> map.keySet().stream()).anyMatch(rule -> !rule.getRuleName().equals(currentFilter.toString()))){
            applySingleFilter(currentFilter, originalTraces, new LinkedList<>(FilteringPageController.deepCloneTracesList(new LinkedList<>(originalTraces))), (XLog) xLog.clone(), originalTraces, currentFiltersForInspector);
        }
    }

    @Override
    protected TraceFilteringTaskResult call() throws Exception {
    	long taskStartTime = System.currentTimeMillis();
		logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

        traceFilteringTaskResult = new TraceFilteringTaskResult();
        applyFilter(filteringTabController.listOfTraces, filteringTabController.selectedFiltersListView.get(0), RuleType.ALL_TRACE_FILTERS, filteringTabController.selectedFiltersListView, filteringTabController.unmodifiedTraces, filteringTabController.currentFiltersForInspector);
        filteringTabController.selectedFiltersListView.forEach(currentFilter -> applyFilter(filteringTabController.listOfTraces, currentFilter, RuleType.SINGLE_TRACE_FILTER, filteringTabController.selectedFiltersListView, filteringTabController.unmodifiedTraces, filteringTabController.currentFiltersForInspector));

        logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);

        return traceFilteringTaskResult;
    }
}
