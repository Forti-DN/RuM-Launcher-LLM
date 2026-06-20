package controller.filtering;

import javafx.collections.FXCollections;
import javafx.scene.chart.PieChart;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.processmining.plugins.ltlchecker.InstanceModel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;


public class StatisticsUtils {

    protected static int getNumberOfTraces(LinkedList<InstanceModel> log) {
        return log.size();
    }

    protected static long getNumberOfEventsInTrace(InstanceModel trace) {
        return trace.getInstance().size();
    }

    protected static long getNumberOfEventsInLog(LinkedList<InstanceModel> events) {
        return events.stream().map(StatisticsUtils::getNumberOfEventsInTrace).reduce(0L, Long::sum);
    }

    protected static long getMinimumNumberOfEvents(LinkedList<InstanceModel> listOfTraces) {
        return listOfTraces.stream().mapToLong(StatisticsUtils::getNumberOfEventsInTrace).min().orElse(0L);
    }

    protected static long getMaximumNumberOfEvents(LinkedList<InstanceModel> listOfTraces) {
        return listOfTraces.stream().mapToLong(StatisticsUtils::getNumberOfEventsInTrace).max().orElse(0L);
    }

    protected static LocalDateTime getStartTimestampOfTrace(InstanceModel trace) {
        if (trace == null) {
            return null;
        }

        return trace.getInstance().stream()
                .map(XEvent::getAttributes)
                .filter(attributes -> attributes.get("time:timestamp") != null)
                .map(attributes -> (XAttributeTimestampImpl) attributes.get("time:timestamp"))
                .map(XAttributeTimestampImpl::getValue)
                .reduce((d1, d2) -> d1.before(d2) ? d1 : d2)
                .map(date -> Objects.requireNonNull(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .orElse(null);
    }

    protected static LocalDateTime getStartTimestampOfLog(LinkedList<InstanceModel> traces) {
        if (traces == null) {
            return null;
        }

        return traces.stream()
                .map(StatisticsUtils::getStartTimestampOfTrace)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    protected static LocalDateTime getEndTimestampOfTrace(InstanceModel trace) {
        return trace.getInstance().stream()
                .map(XEvent::getAttributes)
                .filter(attributes -> attributes.get("time:timestamp") != null)
                .map(attributes -> (XAttributeTimestampImpl) attributes.get("time:timestamp"))
                .map(XAttributeTimestampImpl::getValue)
                .reduce((d1, d2) -> d1.after(d2) ? d1 : d2)
                .map(date -> Objects.requireNonNull(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .orElse(null);
    }

    protected static LocalDateTime getEndTimestampOfLog(LinkedList<InstanceModel> traces) {
        if (traces == null) {
            return null;
        }

        return traces.stream().map(StatisticsUtils::getEndTimestampOfTrace).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
    }

    protected static double getDurationOfTrace(InstanceModel trace) {
        if (trace.getInstance().isEmpty()) {
            return 0;
        }

        double startTimestamp = getStartTimestampOfTrace(trace).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double endTimestamp = getEndTimestampOfTrace(trace).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double differenceInMilliseconds = endTimestamp - startTimestamp;
        double differenceInHours = differenceInMilliseconds / (1000 * 60 * 60);

        return DoubleStream.of(differenceInHours).map(d -> Math.round(d * 10) / 10.0).findFirst().orElse(0.0);
    }

    protected static List<Double> getDurationOfAllTracesInLog(LinkedList<InstanceModel> traces) {
        return traces.stream().map(StatisticsUtils::getDurationOfTrace).collect(Collectors.toList());
    }

    protected static double getMinimumDurationOfTrace(List<Double> listOfDurations) {
        return listOfDurations.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    protected static double getMaximumDurationOfTrace(List<Double> listOfDurations) {
        return listOfDurations.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    protected static double getMeanTraceDuration(List<Double> listOfDurations) {
        if (listOfDurations.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(String.format("%.1f", listOfDurations.stream().reduce(0.0, Double::sum) / listOfDurations.size()));
    }

    protected static double getMedianTraceDuration(List<Double> listOfDurations) {
        return listOfDurations.stream()
                .sorted()
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    int size = list.size();
                    if (size == 0 || size == 1) {
                        return 0.0;
                    }
                    int middle = size / 2;
                    return size % 2 == 1 ? list.get(middle - 1) : (list.get(middle - 1) + list.get(middle)) / 2.0;
                }));
    }


    /**
     * This method is used to convert a given duration (in hours) into a more human-readable format.
     * It converts the hours into minutes, seconds, days, months or years based on the value of the input.
     *
     * @param durationInHours The duration in hours that needs to be formatted.
     * @return String representation of duration in a more human-readable format.
     */
    public static String formatDuration(double durationInHours) {

        final double HOURS_PER_DAY = 24.0;
        final double DAYS_PER_YEAR = 365.0;
        final double DAYS_PER_MONTH = 30.0;
        final double MINUTES_PER_HOUR = 60.0;
        final double SECONDS_PER_MINUTE = 60.0;

        StringBuilder result = new StringBuilder();

        if (durationInHours > HOURS_PER_DAY) {
            double durationInDays = durationInHours / HOURS_PER_DAY;
            if (durationInDays > DAYS_PER_YEAR) {
                result.append(String.format("%.1f years", durationInDays / DAYS_PER_YEAR));
            } else if (durationInDays > DAYS_PER_MONTH) {
                result.append(String.format("%.1f months", durationInDays / DAYS_PER_MONTH));
            } else {
                result.append(String.format("%.1f days", durationInDays));
            }
        } else if (durationInHours < 1) {
            double durationInMinutes = durationInHours * MINUTES_PER_HOUR;
            if (durationInMinutes >= 1) {
                result.append(String.format("%.1f min", durationInMinutes));
            } else {
                result.append(String.format("%.1f sec", durationInMinutes * SECONDS_PER_MINUTE));
            }
        } else if (durationInHours == HOURS_PER_DAY) {
            result.append("1.0 day");
        } else {
            result.append(String.format("%.1f hours", durationInHours));
        }
        return result.toString();
    }

    protected static void updateStatistics(LinkedList<InstanceModel> traces, FilteringTabController filteringTabController) {
        LinkedList<InstanceModel> listOfInstanceModels = new LinkedList<>(traces);
        updateTimeValues(listOfInstanceModels, filteringTabController);
        updateTracesValues(listOfInstanceModels, filteringTabController);
        updateEventValues(listOfInstanceModels, filteringTabController);
    }

    private static void updateTracesValues(LinkedList<InstanceModel> listOfInstanceModels, FilteringTabController filteringTabController) {

        final String FILTERED_TRACE_COLOR = "-fx-pie-color: #eaeaea";
        final String REMAINING_TRACE_COLOR = "-fx-pie-color: #00a6fb";

        double remainingTracesPercentage = (double) StatisticsUtils.getNumberOfTraces(listOfInstanceModels) / (double) StatisticsUtils.getNumberOfTraces(new LinkedList<>(filteringTabController.unmodifiedTraces)) * 100;
        double filteredTracesPercentage = 100 - remainingTracesPercentage;

        filteringTabController.remainingTracesPiechartData = new PieChart.Data("traces", remainingTracesPercentage);
        filteringTabController.filteredTracesPiechartData = new PieChart.Data("filtered traces", filteredTracesPercentage);
        filteringTabController.chartOfCases.setData(FXCollections.observableArrayList(filteringTabController.remainingTracesPiechartData, filteringTabController.filteredTracesPiechartData));
        filteringTabController.filteredTracesPiechartData.getNode().setStyle(FILTERED_TRACE_COLOR);
        filteringTabController.remainingTracesPiechartData.getNode().setStyle(REMAINING_TRACE_COLOR);
        filteringTabController.tracesPercentage.setText(String.format("%.2f%%", remainingTracesPercentage));
        filteringTabController.tracesValue.setText(StatisticsUtils.getNumberOfTraces(listOfInstanceModels) + "/" + StatisticsUtils.getNumberOfTraces(new LinkedList<>(filteringTabController.unmodifiedTraces)));
    }

    private static void updateEventValues(LinkedList<InstanceModel> listOfInstanceModels, FilteringTabController filteringTabController) {

        final String FILTERED_EVENT_COLOR = "-fx-pie-color: #eaeaea";
        final String REMAINING_EVENT_COLOR = "-fx-pie-color: #00a6fb";

        double remainingEventsPercentage = (double) StatisticsUtils.getNumberOfEventsInLog(listOfInstanceModels) / (double) StatisticsUtils.getNumberOfEventsInLog(new LinkedList<>(filteringTabController.unmodifiedTraces)) * 100;
        double filteredEventsPercentage = 100 - remainingEventsPercentage;

        filteringTabController.remainingEventsPiechartData = new PieChart.Data("events", remainingEventsPercentage);
        filteringTabController.filteredEventsPiechartData = new PieChart.Data("filtered events", filteredEventsPercentage);
        filteringTabController.chartOfEvents.setData(FXCollections.observableArrayList(filteringTabController.remainingEventsPiechartData, filteringTabController.filteredEventsPiechartData));
        filteringTabController.filteredEventsPiechartData.getNode().setStyle(FILTERED_EVENT_COLOR);
        filteringTabController.remainingEventsPiechartData.getNode().setStyle(REMAINING_EVENT_COLOR);
        filteringTabController.eventsPercentage.setText(String.format("%.2f%%", remainingEventsPercentage));
        filteringTabController.eventsValue.setText(StatisticsUtils.getNumberOfEventsInLog(listOfInstanceModels) + "/" + StatisticsUtils.getNumberOfEventsInLog(new LinkedList<>(filteringTabController.unmodifiedTraces)));
    }

    private static void updateTimeValues(LinkedList<InstanceModel> listOfInstanceModels, FilteringTabController filteringTabController) {
        final String DATE_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";
        final String DEFAULT_DATE_TIME = "00.00.0000 00:00:00";
        final String DEFAULT_TRACE_DURATION = "0.0 sec";

        String endTime = listOfInstanceModels.isEmpty() ? DEFAULT_DATE_TIME : StatisticsUtils.getEndTimestampOfLog(listOfInstanceModels) == null ? DEFAULT_DATE_TIME : StatisticsUtils.getEndTimestampOfLog(listOfInstanceModels).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        String startTime = listOfInstanceModels.isEmpty() ? DEFAULT_DATE_TIME : StatisticsUtils.getStartTimestampOfLog(listOfInstanceModels) == null ? DEFAULT_DATE_TIME : StatisticsUtils.getStartTimestampOfLog(listOfInstanceModels).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        filteringTabController.endTimeValue.setText(endTime);
        filteringTabController.startTimeValue.setText(startTime);

        List<Double> listOfDuration = StatisticsUtils.getDurationOfAllTracesInLog(listOfInstanceModels);
        String median = StatisticsUtils.formatDuration(StatisticsUtils.getMedianTraceDuration(listOfDuration));
        String mean = StatisticsUtils.formatDuration(StatisticsUtils.getMeanTraceDuration(listOfDuration));
        filteringTabController.medianValue.setText(median);
        filteringTabController.meanValue.setText(mean);

        filteringTabController.minCaseDuration.setText(listOfInstanceModels.isEmpty() ? DEFAULT_TRACE_DURATION : StatisticsUtils.formatDuration(Collections.min(StatisticsUtils.getDurationOfAllTracesInLog(listOfInstanceModels))));
        filteringTabController.maxCaseDuration.setText(listOfInstanceModels.isEmpty() ? DEFAULT_TRACE_DURATION : StatisticsUtils.formatDuration(Collections.max(StatisticsUtils.getDurationOfAllTracesInLog(listOfInstanceModels))));
    }
}
