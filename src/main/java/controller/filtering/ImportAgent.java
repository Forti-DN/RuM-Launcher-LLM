package controller.filtering;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import org.apache.commons.collections15.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.declareminer.util.XLogReader;
import org.processmining.plugins.ltlchecker.parser.FormulaParameter;
import org.processmining.plugins.ltlchecker.parser.LTLParser;
import util.AlertUtils;
import util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


public class ImportAgent extends LTLFiltersUtils {
    public static LTLParser ltlParser;
    public static FilteringTabController filteringTabController;


    /**
     * This function is responsible for importing filters from a chosen file and updating the filter list views in the GUI.
     *
     * @param chosenFile              The file to import the filters from.
     * @param logFile                 The log file used for generating JSON content for the import.
     * @param attributeAndValue       LinkedHashMap for the attributes and their respective values.
     * @param selectedFiltersListView The observable list view for selected filters in the GUI.
     * @param appliedFiltersListView  The list view for applied filters in the GUI.
     * @param filteringTabControllers The controller for the filtering tab.
     */
    protected static void importFilters(File chosenFile, File logFile, LinkedHashMap<String, List<String>> attributeAndValue, ObservableList<Filter> selectedFiltersListView, ListView<Filter> appliedFiltersListView, FilteringTabController filteringTabControllers) {
        filteringTabController = filteringTabControllers;

        try {
            // Read the file content
            final String content = FileUtils.readFile(chosenFile.getAbsolutePath());

            // Generate filters from the content of the file using the log file
            List<Filter> importedFilters = generateJsonForImport(content, (XLog) (XLogReader.openLog(logFile.getAbsolutePath())).clone(), attributeAndValue);

            // If the importedFilters are empty, it means that the file is not correct. So, only proceed if there are imported filters
            if (!importedFilters.isEmpty()) {// if the importedFilters are empty it means that the files is not correct

                // Merge the imported filters with the existing ones and update the list view with the combined list of filters
                selectedFiltersListView.setAll(ListUtils.union(importedFilters, selectedFiltersListView));

                // Select the first filter in the list view for applied filters
                appliedFiltersListView.getSelectionModel().selectFirst();

                // Scroll to the selected filter
                appliedFiltersListView.scrollTo(appliedFiltersListView.getSelectionModel().getSelectedIndex());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    protected static List<Filter> generateJsonForImport(String jsonFromFile, XLog logFile, LinkedHashMap<String, List<String>> attributeAndValue) {

        List<Filter> parsedFilters = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();


        try {
            List<ObjectNode> listOfObjectNode = readObjectNodesFromJson(jsonFromFile, mapper);

            for (ObjectNode objectNode : listOfObjectNode) {
                String filterCategory = objectNode.get("category").asText();

                if (filterCategory.equals("LTL")) {
                    List<Filter> ltlFilters = parseLtlFilters(logFile, objectNode, parsedFilters);
                    if (ltlFilters.isEmpty()) {
                        return ltlFilters;
                    }
                } else {
                    List<Filter> filters = parseFilters(objectNode, attributeAndValue, parsedFilters);
                    if (filters.isEmpty()) {
                        return filters;
                    }
                }
            }

        } catch (Exception e) {
            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
        }


        return parsedFilters;
    }

    /**
     * Reads a list of ObjectNode instances from a JSON string.
     *
     * @param json   The JSON string to parse.
     * @param mapper The ObjectMapper to use for parsing the JSON.
     * @return A list of ObjectNode instances.
     */
    private static List<ObjectNode> readObjectNodesFromJson(String json, ObjectMapper mapper) throws IOException {
        return mapper.readValue(mapper.readTree(json).toString(), new TypeReference<>() {
        });
    }


    /**
     * * Parses the LTL filters from the provided JSON object node and adds them to the given list of parsed filters.
     *
     * @param xLog          The XLog file which is used to create the LTL parser if it doesn't exist already.
     * @param objectNode    The JSON object node that contains the filter parameters and their values for parsing.
     * @param parsedFilters The list of filters where parsed filters are added. This list is cleared if there is a parsing error.
     * @return The list of parsed filters with the newly parsed filters added. In case of parsing error, an empty list is returned.
     */
    public static List<Filter> parseLtlFilters(XLog xLog, ObjectNode objectNode, List<Filter> parsedFilters) {

        try {
            // Get the LTL parser and the formula from the objectNode
            if (ltlParser == null) {
                ltlParser = getLTLParser(xLog);
            }

            String ltlFormulas = objectNode.get("filteringMode").asText();

            // Verify formula existence in parser.
            if (!ltlParser.existsFormula(ltlFormulas)) {
                parsedFilters.clear();
                AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                return new ArrayList<>();
            }

            // Get the parameter values and parameters from the objectNode
            ArrayNode endValue = (ArrayNode) objectNode.get("parameterName");
            List<FormulaParameter> formulaParameters = ltlParser.getParameters(ltlFormulas);

            // Validate parameter names for formula.
            if (!parametersAndValuesAreCorrectForFormula(formulaParameters, endValue) || formulaParameters.size() != endValue.size()) {
                parsedFilters.clear();
                AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                return new ArrayList<>(); // return empty
            }

            ArrayNode startValue = (ArrayNode) objectNode.get("parameterValue");
            List<String> filterParameterValues_startValues = new ArrayList<>();
            List<String> filterParameters_endValues = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();

            // Handle case where formula has no parameters but file contains parameters.
            if (formulaParameters.isEmpty() && (!startValue.isEmpty() || !endValue.isEmpty())) {
                AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                parsedFilters.clear();
                return new ArrayList<>();
            }

            // Handle case where the number of formula parameters does not match the parsed end parameters.
            if (endValue.size() != startValue.size()) {
                AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                parsedFilters.clear();
                return new ArrayList<>();
            }

            objectNode.get("parameterValue").forEach(currentEntry -> {
                String entryText = currentEntry.asText().isEmpty() ? StringUtils.SPACE : currentEntry.asText();
                filterParameterValues_startValues.add(entryText);
                stringBuilder.append(entryText);
            });

            objectNode.get("parameterName").forEach(currentEntry -> filterParameters_endValues.add(currentEntry.asText()));

            // Create a filter and add it to the parsedFilters list
            parsedFilters.add(createLTLFilter(stringBuilder, ltlFormulas, filterParameterValues_startValues, filterParameters_endValues));
        } catch (Exception e) {
            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
            parsedFilters.clear();
            return new ArrayList<>();
        }

        return parsedFilters;
    }

    /**
     * This method creates and returns a new LTL Filter object with the specified parameters.
     *
     * @param stringBuilder         A StringBuilder whose string will be used in defining the filtering mode.
     * @param formula               A string that defines the formula used in the filter.
     * @param filterParameterValues A List of Strings that defines the start value of the filter.
     * @param filterParameters      A List of Strings that defines the end value of the filter.
     * @return A Filter object created with the specified parameters.
     */
    public static Filter createLTLFilter(StringBuilder stringBuilder, String formula, List<String> filterParameterValues, List<String> filterParameters) {
        Filter filter = new Filter();
        filter.setCategory("LTL");
        filter.setFilteringMode(formula.concat(StringUtils.SPACE).concat(stringBuilder.toString()));
        filter.setStartValue(filterParameterValues);
        filter.setEndValue(filterParameters);
        return filter;
    }

    public static List<Filter> parseFilters(ObjectNode objectNode, LinkedHashMap<String, List<String>> attributeAndValue, List<Filter> parsedFilters) {

        Filter filter = new Filter();

        try {
            if (!Arrays.asList(new String[]{"Attribute", "Endpoints", "Follower", "Timeframe", "Performance"}).contains(objectNode.get("category").asText())) {
                AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                parsedFilters.clear();
                return new ArrayList<>();
            }
            filter.setCategory(objectNode.get("category").asText());

            switch (filter.getCategory()) {
                case "Attribute": {
                    if (!Arrays.asList(new String[]{"forbiddenMode", "keepSelectedMode", "mandatoryMode"}).contains(objectNode.get("filteringMode").asText())) {
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        parsedFilters.clear();
                        return new ArrayList<>();
                    }
                    filter.setFilteringMode(objectNode.get("filteringMode").asText());
                    break;
                }
                case "Performance": {
                    if (!Arrays.asList(new String[]{"Number of events", "Trace duration"}).contains(objectNode.get("filteringMode").asText())) {
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        parsedFilters.clear();
                        return new ArrayList<>();
                    }
                    filter.setFilteringMode(objectNode.get("filteringMode").asText());
                    break;
                }
                case "Follower": {
                    if (!Arrays.asList(new String[]{"eventually_followed", "directly_followed", "never_eventually_followed", "never_directly_followed"}).contains(objectNode.get("filteringMode").asText())) {
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        parsedFilters.clear();
                        return new ArrayList<>();
                    }
                    filter.setFilteringMode(objectNode.get("filteringMode").asText());
                    break;
                }
                case "Endpoints": {
                    if (!Arrays.asList(new String[]{"discardTraces", "trimLongest", "trimFirst"}).contains(objectNode.get("filteringMode").asText())) {
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        parsedFilters.clear();
                        return new ArrayList<>();
                    }
                    filter.setFilteringMode(objectNode.get("filteringMode").asText());
                    break;
                }
                case "Timeframe": {
                    if (!Arrays.asList(new String[]{"containedInTimeFrame", "intersectingInTimeframe", "startedInTimeframe", "completedInTimeframe", "trimToTimeframe"}).contains(objectNode.get("filteringMode").asText())) {
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        parsedFilters.clear();
                        return new ArrayList<>();
                    }
                    filter.setFilteringMode(objectNode.get("filteringMode").asText());
                    break;
                }
            }


            ArrayNode objectNodeParameterName = (ArrayNode) objectNode.get("parameterName");
            switch (filter.getCategory()) {
                case "Performance": {
                    if ((!objectNodeParameterName.get(0).asText().equals("minLength") && !objectNodeParameterName.get(0).asText().equals("minDuration")) || (!objectNodeParameterName.get(1).asText().equals("maxLength") && !objectNodeParameterName.get(1).asText().equals("maxDuration"))) {
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        parsedFilters.clear();
                        return new ArrayList<>();
                    }
                    break;
                }
                case "Attribute": {
                    if (!objectNodeParameterName.get(0).asText().equals("attribute") || !objectNodeParameterName.get(1).asText().equals("value")) {
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        parsedFilters.clear();
                        return new ArrayList<>();
                    }
                    break;
                }
                case "Timeframe": {
                    if (!objectNodeParameterName.get(0).asText().equals("startTime") || !objectNodeParameterName.get(1).asText().equals("endTime")) {
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        parsedFilters.clear();
                        return new ArrayList<>();
                    }
                    break;
                }
                case "Follower":
                case "Endpoints": {
                    if (!objectNodeParameterName.get(0).asText().equals("attribute") || !objectNodeParameterName.get(1).asText().equals("startValue") || !objectNodeParameterName.get(2).asText().equals("endValue")) {
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        parsedFilters.clear();
                        return new ArrayList<>();
                    }
                    break;
                }
            }


            ArrayNode objectNodeParameterValue = (ArrayNode) objectNode.get("parameterValue");
            switch (filter.getCategory()) {
                case "Performance": {
                    if (filter.getFilteringMode().equals("Number of events")) {
                        if (!isNumber(objectNodeParameterValue.get(0).asText()) || !isNumber(objectNodeParameterValue.get(1).asText())) {
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            parsedFilters.clear();
                            return new ArrayList<>();
                        }

                        if (objectNodeParameterValue.size() != 2) {
                            parsedFilters.clear();
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            return new ArrayList<>();
                        }

                        double firstValue = Double.parseDouble(objectNodeParameterValue.get(0).asText());
                        double secondValue = Double.parseDouble(objectNodeParameterValue.get(1).asText());

                        // boolean isValidFilter = checkPerformance_numberOfEvents(firstValue, secondValue);
                        //      if (isValidFilter) {
                        filter.setStartValue(Collections.singletonList(objectNodeParameterValue.get(0).asText()));
                        filter.setEndValue(Collections.singletonList(objectNodeParameterValue.get(1).asText()));
                        parsedFilters.add(filter);
                        break;
                  /*      } else {
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            parsedFilters.clear();
                            return new ArrayList<>();
                        }*/
                    }

                    if (filter.getFilteringMode().equals("Trace duration")) {
                        if (!isNumber(objectNodeParameterValue.get(0).asText()) || !isNumber(objectNodeParameterValue.get(1).asText())) {
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            parsedFilters.clear();
                            return new ArrayList<>();
                        }

                        if (objectNodeParameterValue.size() != 2) {
                            parsedFilters.clear();
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            return new ArrayList<>();
                        }

                        double firstValue = Double.parseDouble(objectNodeParameterValue.get(0).asText());
                        double secondValue = Double.parseDouble(objectNodeParameterValue.get(1).asText());

                        // boolean isValidFilter = checkPerformance_traceDuration(firstValue, secondValue);
                        //      if (isValidFilter) {
                        filter.setStartValue(Collections.singletonList(objectNodeParameterValue.get(0).asText()));
                        filter.setEndValue(Collections.singletonList(objectNodeParameterValue.get(1).asText()));
                        parsedFilters.add(filter);
                        break;
                /*        } else {
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            parsedFilters.clear();
                            return new ArrayList<>();
                        }*/
                    }
                }

                case "Timeframe": {
                    if (!objectNodeParameterName.get(0).asText().equals("startTime") || !objectNodeParameterName.get(1).asText().equals("endTime")) {
                        parsedFilters.clear();
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        return new ArrayList<>();
                    }

                    if (objectNodeParameterValue.size() != 2) {
                        parsedFilters.clear();
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        return new ArrayList<>();
                    }

                    if (!checkTimeframeFiltersStartValues(objectNodeParameterValue.get(0).asText())) {
                        parsedFilters.clear();
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        return new ArrayList<>();
                    }

                    if (!checkTimeframeFiltersEndValues(objectNodeParameterValue.get(1).asText())) {
                        parsedFilters.clear();
                        AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                        return new ArrayList<>();
                    }

                    filter.setStartValue(Collections.singletonList(objectNodeParameterValue.get(0).asText()));
                    filter.setEndValue(Collections.singletonList(objectNodeParameterValue.get(1).asText()));
                    parsedFilters.add(filter);
                    break;
                }


                case "Attribute": {
                    if (new ArrayList<>(attributeAndValue.keySet()).contains(objectNode.get("parameterValue").get(0).asText())) { // Applicable only for timeframe and performance filters, which have null attribute
                        filter.setAttribute(objectNode.get("parameterValue").get(0).asText());

                        if (new ArrayList<>(attributeAndValue.keySet()).contains(objectNodeParameterValue.get(0).asText())) {
                            ArrayNode arrayStartNode = (ArrayNode) objectNodeParameterValue.get(1);
                            boolean allValid = true;
                            List<String> currentStartValue = new ArrayList<>();
                            for (JsonNode valueNode : arrayStartNode) {
                                String value = valueNode.asText();
                                if (!attributeAndValue.get(filter.getAttribute()).contains(value)) {
                                    allValid = false;
                                    break;
                                }
                                currentStartValue.add(value);
                            }
                            if (allValid) {
                                filter.setStartValue(currentStartValue);
                                parsedFilters.add(filter);
                            } else {
                                AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                                parsedFilters.clear();
                                return new ArrayList<>();
                            }
                            break;
                        } else {
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            parsedFilters.clear();
                            return new ArrayList<>();
                        }
                    }
                    AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                    parsedFilters.clear();
                    return new ArrayList<>();
                }
                case "Follower":
                case "Endpoints": {
                    if (new ArrayList<>(attributeAndValue.keySet()).contains(objectNodeParameterValue.get(0).asText())) {
                        filter.setAttribute(objectNode.get("parameterValue").get(0).asText());


                        if (objectNodeParameterValue.get(1).isEmpty() && objectNodeParameterValue.get(2).isEmpty()) {
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            parsedFilters.clear();
                            return new ArrayList<>();
                        }


                        ArrayNode arrayStartNode = (ArrayNode) objectNodeParameterValue.get(1);
                        boolean allValid = true;
                        List<String> currentStartValue = new ArrayList<>();
                        for (JsonNode valueNode : arrayStartNode) {
                            String value = valueNode.asText();
                            if (!attributeAndValue.get(filter.getAttribute()).contains(value)) {
                                allValid = false;
                                break;
                            }
                            currentStartValue.add(value);
                        }
                        if (allValid) {
                            filter.setStartValue(currentStartValue);
                        } else {
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            parsedFilters.clear();
                            return new ArrayList<>();
                        }


                        ArrayNode arrayEndNode = (ArrayNode) objectNodeParameterValue.get(2);
                        boolean allValidEndNodeFlag = true;
                        List<String> currentEndNode = new ArrayList<>();
                        for (JsonNode valueNode : arrayEndNode) {
                            String value = valueNode.asText();
                            if (!attributeAndValue.get(filter.getAttribute()).contains(value)) {
                                allValidEndNodeFlag = false;
                                break;
                            }
                            currentEndNode.add(value);
                        }
                        if (allValidEndNodeFlag) {
                            filter.setEndValue(currentEndNode);
                            parsedFilters.add(filter);
                        } else {
                            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                            parsedFilters.clear();
                            return new ArrayList<>();
                        }
                        break;
                    }
                    AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
                    parsedFilters.clear();
                    return new ArrayList<>();
                }
            }
        } catch (Exception e) {
            AlertUtils.showError("Unable to import the filters. The file you entered is incorrect!");
            parsedFilters.clear();
            return new ArrayList<>();
        }

        return parsedFilters;
    }


    public static boolean isLocalDateTime(String date) {
        try {
            LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            return true;

        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static LocalDateTime getLocalDateTime(String date) {
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    private static boolean isLocalDateTimeInLeftBoundaries(LocalDateTime localDateTime, LocalDateTime before) {
        return !localDateTime.isBefore(before);
    }

    private static boolean isLocalDateTimeInRightBoundaries(LocalDateTime localDateTime, LocalDateTime after) {
        return !localDateTime.isAfter(after);
    }

    public static boolean checkTimeframeFiltersStartValues(String listOfValues) {
        LocalDateTime startOfLog = StatisticsUtils.getStartTimestampOfLog(new LinkedList<>(filteringTabController.unmodifiedTraces));
        if (isLocalDateTime(listOfValues)) {
            if (!isLocalDateTimeInLeftBoundaries(getLocalDateTime(listOfValues), startOfLog)) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    public static boolean checkTimeframeFiltersEndValues(String listOfValues) {
        LocalDateTime endOfLog = StatisticsUtils.getEndTimestampOfLog(new LinkedList<>(filteringTabController.unmodifiedTraces));
        if (isLocalDateTime(listOfValues)) {
            if (!isLocalDateTimeInRightBoundaries(getLocalDateTime(listOfValues), endOfLog)) {
                return false;
            }
        } else {
            return false;

        }
        return true;
    }

    public boolean checkPerformance_traceDuration(double min, double max) {
        double realMIN = StatisticsUtils.getMinimumDurationOfTrace(StatisticsUtils.getDurationOfAllTracesInLog(new LinkedList<>(filteringTabController.unmodifiedTraces)));
        double realMAX = StatisticsUtils.getMaximumDurationOfTrace(StatisticsUtils.getDurationOfAllTracesInLog(new LinkedList<>(filteringTabController.unmodifiedTraces)));

        return min >= realMIN && max <= realMAX && min <= max;
    }

    public boolean checkPerformance_numberOfEvents(double min, double max) {
        double realMIN = StatisticsUtils.getMinimumNumberOfEvents(new LinkedList<>(filteringTabController.unmodifiedTraces));
        double realMAX = StatisticsUtils.getMaximumNumberOfEvents(new LinkedList<>(filteringTabController.unmodifiedTraces));

        return min >= realMIN && max >= realMAX;
    }

    public static boolean isNumber(String string) {
        try {
            Double.parseDouble(string);
            return true;

        } catch (Exception e) {
            return false;
        }

    }
}