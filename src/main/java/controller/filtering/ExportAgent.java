package controller.filtering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import util.AlertUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportAgent {

    /**
     * Generate JSON string for given list of filters.
     *
     * @param filtersForExport List of filters to export.
     * @return JSON String
     */
    protected static String generateJson(List<Filter> filtersForExport) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (Filter filter : filtersForExport) {
            ObjectNode jsonFilter = createJsonFilter(mapper, filter);
            arrayNode.add(jsonFilter);
        }

        return prettifyJson(mapper, arrayNode);
    }


    /**
     * Create JSON Object Node for the given filter
     *
     * @param mapper ObjectMapper to create JSON Object Node
     * @param filter Filter object
     * @return JSON String
     */
    private static ObjectNode createJsonFilter(ObjectMapper mapper, Filter filter) {
        ObjectNode jsonFilter = mapper.createObjectNode();
        jsonFilter.put("category", filter.getCategory());
        jsonFilter.put("filteringMode", filter.getCategory().equals("LTL") ? filter.getFilteringMode().substring(0, filter.getFilteringMode().indexOf(" ")) : filter.getFilteringMode());

        ArrayNode parameterName = jsonFilter.putArray("parameterName");
        ArrayNode parameterValue = jsonFilter.putArray("parameterValue");

        switch (filter.getCategory()) {
            case "Performance": {
                switch (filter.getFilteringMode()) {
                    case "Number of events": {
                        parameterName.add("minDuration");
                        parameterName.add("maxDuration");
                        break;
                    }
                    case "Trace duration": {
                        parameterName.add("minLength");
                        parameterName.add("maxLength");
                        break;
                    }
                }
                if (filter.getStartValue() != null) {
                    filter.getStartValue().forEach(parameterValue::add);
                }
                if (filter.getEndValue() != null) {
                    filter.getEndValue().forEach(parameterValue::add);
                }
                break;
            }
            case "LTL": {
                if (filter.getStartValue() != null) {
                    filter.getStartValue().forEach(parameterValue::add);
                }
                if (filter.getEndValue() != null) {
                    filter.getEndValue().forEach(parameterName::add);
                }
                break;
            }
            case "Timeframe": {
                parameterName.add("startTime");
                parameterName.add("endTime");
                if (filter.getStartValue() != null) {
                    filter.getStartValue().forEach(parameterValue::add);
                }
                if (filter.getEndValue() != null) {
                    filter.getEndValue().forEach(parameterValue::add);
                }
                break;
            }
            case "Attribute": {
                parameterName.add("attribute");
                parameterName.add("value");

                ArrayNode startValues = mapper.createArrayNode();
                parameterValue.add(filter.getAttribute());
                if (filter.getStartValue() != null) {
                    filter.getStartValue().forEach(startValues::add);
                    parameterValue.add(startValues);
                }
                break;
            }
            case "Follower":
            case "Endpoints": {
                parameterName.add("attribute");
                parameterName.add("startValue");
                parameterName.add("endValue");

                parameterValue.add(filter.getAttribute());
                ArrayNode startValues = mapper.createArrayNode();
                ArrayNode endValues = mapper.createArrayNode();

                if (filter.getStartValue() != null) {
                    filter.getStartValue().forEach(startValues::add);
                    parameterValue.add(startValues);
                }

                if (filter.getEndValue() != null) {
                    filter.getEndValue().forEach(endValues::add);
                    parameterValue.add(endValues);
                }
                break;
            }
        }

        return jsonFilter;
    }

    /**
     * Prettify the JSON string.
     *
     * @param mapper    ObjectMapper to prettify JSON.
     * @param arrayNode ArrayNode to be prettified.
     * @return Prettified JSON string.
     */
    private static String prettifyJson(ObjectMapper mapper, ArrayNode arrayNode) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode);
        } catch (JsonProcessingException e) {
            AlertUtils.showError("Unable to export filters due to incorrect JSON format.");
            return null;
        }
    }

    /**
     * Export the filters to the given file.
     *
     * @param chosenFile       File to write the filters.
     * @param filtersForExport List of filters to be written.
     */
    protected static void exportFilters(File chosenFile, List<Filter> filtersForExport) {
        String jsonForExport = ExportAgent.generateJson(filtersForExport);

        try {
            final FileWriter fileWriter = new FileWriter(chosenFile);
            fileWriter.write(jsonForExport);
            fileWriter.close();
        } catch (IOException IOe) {
            AlertUtils.showError("Unable to write filters to file!");
        }
    }
}
