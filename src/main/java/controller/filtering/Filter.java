package controller.filtering;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.ltlchecker.InstanceModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

public class Filter {
    private String category;
    private String attribute;
    private String filteringMode;
    private List<String> startValue;
    private List<String> endValue;
    public FilterType filterType;

    public enum FilterType {
        TRACE_FILTER,
        EVENT_FILTER

    }

    protected void setFilterType(FilterType color) {
        this.filterType = color;
    }

    public String getCategory() {
        return this.category;
    }

    protected void setCategory(String category) {
        this.category = category;
    }

    public String getAttribute() {
        return this.attribute;
    }

    protected void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getFilteringMode() {
        return this.filteringMode;
    }

    protected void setFilteringMode(String filteringMode) {
        this.filteringMode = filteringMode;
    }

    public List<String> getStartValue() {
        return this.startValue;
    }

    protected void setStartValue(List<String> startValue) {
        this.startValue = startValue;
    }

    public List<String> getEndValue() {
        return this.endValue;
    }

    protected void setEndValue(List<String> endValue) {
        this.endValue = endValue;
    }

    public static void applyFilterAux(String category, String mode, String attribute, List<String> startValues, List<String> endValues, LinkedList<InstanceModel> listOfTraces, XLog xLog) {

        switch (category) {
            case "Attribute": {
                switch (mode) {
                    case "keepSelectedMode": {
                        FilteringUtils.applyFilterByAttributes_KeepSelectedMode(listOfTraces, new EventAttribute(attribute, startValues));
                        break;
                    }
                    case "mandatoryMode": {
                        FilteringUtils.applyFilterByAttributes_MandatoryMode(listOfTraces, new EventAttribute(attribute, startValues));
                        break;
                    }
                    case "forbiddenMode": {
                        FilteringUtils.applyFilterByAttributes_ForbiddenMode(listOfTraces, new EventAttribute(attribute, startValues));
                        break;
                    }
                }
                break;
            }
            case "Endpoints": {
                switch (mode) {
                    case "discardTraces": {
                        FilteringUtils.applyFilterByEndpoint_ModeDiscardTraces(listOfTraces, new EventAttribute(attribute, startValues), new EventAttribute(attribute, endValues));
                        break;
                    }
                    case "trimFirst": {
                        FilteringUtils.applyFilterByEndpoint_ModeTrimFirst(listOfTraces, new EventAttribute(attribute, startValues), new EventAttribute(attribute, endValues));
                        break;
                    }
                    case "trimLongest": {
                        FilteringUtils.applyFilterByEndpoint_ModeTrimLongest(listOfTraces, new EventAttribute(attribute, startValues), new EventAttribute(attribute, endValues));
                        break;
                    }
                }
                break;
            }
            case "Follower": {
                FilteringUtils.applyFilterByFollower(mode, attribute, startValues, endValues, listOfTraces);
                break;
            }
            case "Performance": {
                switch (mode) {
                    case "Number of events": {
                        FilteringUtils.applyFilterByPerformance_NumberOfEvents(listOfTraces, (int) Math.round(Double.parseDouble(startValues.get(0))), (int) Math.round(Double.parseDouble(endValues.get(0))));
                        break;
                    }
                    case "Trace duration": {
                        FilteringUtils.applyFilterByPerformance_TraceDuration(listOfTraces, Double.parseDouble(startValues.get(0)), Double.parseDouble(endValues.get(0)));
                        break;
                    }
                }
                break;
            }
            case "Timeframe": {
                switch (mode) {
                    case "containedInTimeFrame": {
                        FilteringUtils.applyFilterByTimeframe_ContainedInTimeframe(listOfTraces, LocalDateTime.parse(startValues.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")), LocalDateTime.parse(endValues.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                        break;
                    }
                    case "intersectingInTimeframe": {
                        FilteringUtils.applyFilterByTimeframe_IntersectedInTimeframe(listOfTraces, LocalDateTime.parse(startValues.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")), LocalDateTime.parse(endValues.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                        break;
                    }
                    case "startedInTimeframe": {
                        FilteringUtils.applyFilterByTimeframe_StartedInTimeframe(listOfTraces, LocalDateTime.parse(startValues.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")), LocalDateTime.parse(endValues.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                        break;
                    }
                    case "completedInTimeframe": {
                        FilteringUtils.applyFilterByTimeframe_CompletedInTimeframe(listOfTraces, LocalDateTime.parse(endValues.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                        break;
                    }
                    case "trimToTimeframe": {
                        FilteringUtils.applyFilterByTimeframe_TrimToTimeframe(listOfTraces, LocalDateTime.parse(endValues.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")), LocalDateTime.parse(endValues.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                        break;
                    }
                }
                break;
            }
            case "LTL": {
                FilteringUtils.applyFilterByLTLFormulas(startValues, endValues, mode, xLog, listOfTraces);
                break;
            }
        }
    }

    @Override
    public String toString() {
        switch (this.category) {

            case "Attribute": {
                switch (this.filteringMode) {
                    case "forbiddenMode": {
                        return "[TRACE FILTER] Remove all traces with at least one event where the attribute '" + this.attribute + "' equals '" + this.startValue + "'.";
                    }
                    case "keepSelectedMode": {
                        return "[EVENT FILTER] Keep only those events where the attribute '" + this.attribute + "' equals '" + this.startValue + "'.";
                    }
                    case "mandatoryMode": {
                        return "[TRACE FILTER] Keep only traces where for at least one event, the attribute '" + this.attribute + "' equals '" + this.startValue + "'.";
                    }
                }
                break;
            }

            case "Endpoints": {
                switch (this.filteringMode) {
                    case "discardTraces": {
                        return "[TRACE FILTER] Keep only traces where the first event has '" + this.startValue +  "' for '" + this.attribute + "', and the last event has '" + this.endValue + "' for the same attribute.";
                    }
                    case "trimLongest": {
                        return "[EVENT FILTER] Trim traces by removing events occurring before the first instance of '" + this.startValue +  "' for '" + this.attribute  + "' and after the last occurrence of '" + this.endValue + "' for the same attribute.";
                    }
                    case "trimFirst": {
                        return "[EVENT FILTER] Trim traces by removing events occurring before the first instance of '" + this.startValue +  "' for '" + this.attribute  + "' and after the first occurrence of '" + this.endValue + "' for the same attribute.";
                    }
                }
            }

            case "Follower": {
                switch (this.filteringMode) {
                    case "eventually_followed": {
                        return "[TRACE FILTER] Keep only traces in which an event with '" + this.startValue + "' for '" + this.attribute + "' is eventually followed by an event with '" + this.endValue + "' for the same attribute.";
                    }
                    case "directly_followed": {
                        return "[TRACE FILTER] Keep only traces in which an event with '" + this.startValue + "' for '" + this.attribute + "' is directly followed by an event with '" + this.endValue+ "' for the same attribute.";
                    }
                    case "never_eventually_followed": {
                        return "[TRACE FILTER] Keep only traces in which an event with '" + this.startValue + "' for '" + this.attribute + "' is never eventually followed by an event with '" + this.endValue + "' for the same attribute.";
                    }
                    case "never_directly_followed": {
                        return "[TRACE FILTER] Keep only traces in which an event with '" + this.startValue + "' for '" + this.attribute + "' is never directly followed by an event with '" + this.endValue + "' for the same attribute.";
                    }
                }
            }

            case "Performance": {
                switch (this.filteringMode) {
                    case "Number of events": {
                        return "[TRACE FILTER] Keep only traces that have a number of events between " + this.startValue.get(0) + " and " + this.endValue.get(0) + ".";
                    }
                    case "Trace duration": {
                        return "[TRACE FILTER] Keep only traces with a duration that falls within the range of " + this.startValue.get(0) + " and " + this.endValue.get(0) + " hours.";
                    }
                }
            }

            case "Timeframe": {
                switch (this.filteringMode) {
                    case "containedInTimeFrame": {
                        return "[TRACE FILTER] Keep only traces that completely lie within the timeframe " + this.startValue.get(0) + " and " + this.endValue.get(0) + ".";
                    }
                    case "intersectingInTimeframe": {
                        return "[TRACE FILTER] Keep only traces that overlap with the timeframe between " + this.startValue.get(0) + " and " + this.endValue.get(0) + ".";
                    }
                    case "startedInTimeframe": {
                        return "[TRACE FILTER] Keep only traces that have started executing within the timeframe ending at " + this.endValue.get(0) + ".";
                    }
                    case "completedInTimeframe": {
                        return "[TRACE FILTER] Keep only traces that have been fully executed within the timeframe " + this.endValue.get(0) + ".";
                    }
                    case "trimToTimeframe": {
                        return "[EVENT FILTER] Remove all the events that occur outside the timeframe between " + this.startValue.get(0) + " and " + this.endValue.get(0) + ".";
                    }
                }
            }

            case "LTL": {
                String ltlFormula = !this.getFilteringMode().contains(" ") ? this.getFilteringMode() : this.getFilteringMode().substring(0, this.getFilteringMode().indexOf(" "));
                return "[TRACE FILTER] Keep only traces conforming to the LTL formula: " + ltlFormula + " for parameters " + this.startValue + ".";
            }

        }
        return "";
    }
}
