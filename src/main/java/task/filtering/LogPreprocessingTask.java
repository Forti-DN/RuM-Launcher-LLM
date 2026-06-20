package task.filtering;

import controller.filtering.LTLFiltersUtils;
import javafx.concurrent.Task;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.declareminer.util.XLogReader;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class LogPreprocessingTask extends Task<LogPreprocessingTaskResult> {

    private File eventLog;
    private XLog xLog;

    public void setLogFile(File eventLog) {
        this.eventLog = eventLog;
    }

    private LinkedHashMap<String, List<String>> getAttributesWithValues() throws Exception {
        this.xLog = XLogReader.openLog(this.eventLog.getAbsolutePath());
        LinkedHashMap<String, List<String>> attributeAndValue = new LinkedHashMap<>();

        LTLFiltersUtils.convertXLogToInstanceModelList(xLog)
                .stream()
                .flatMap(selectedTrace -> selectedTrace.getInstance().stream())
                .flatMap(xEvent -> xEvent.getAttributes().entrySet().stream())
                .filter(entry -> !entry.getKey().equals("time:timestamp"))
                .forEach(entry -> attributeAndValue
                        .computeIfAbsent(entry.getKey(), k -> new LinkedList<>())
                        .stream()
                        .filter(value -> value.equals(entry.getValue().toString()))
                        .findFirst()
                        .orElseGet(() -> {
                            attributeAndValue.get(entry.getKey()).add(entry.getValue().toString());
                            return null;
                        }));

        attributeAndValue.forEach((key, value) -> Collections.sort(value));
        return attributeAndValue;
    }

    @Override
    protected LogPreprocessingTaskResult call() throws Exception {

        LogPreprocessingTaskResult logPreprocessingTaskResult = new LogPreprocessingTaskResult();
        logPreprocessingTaskResult.setAttributeAndValue(getAttributesWithValues());
        logPreprocessingTaskResult.setXLog(xLog);

        return logPreprocessingTaskResult;
    }
}
