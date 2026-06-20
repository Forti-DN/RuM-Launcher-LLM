package task.filtering;

import org.deckfour.xes.model.XLog;

import java.util.LinkedHashMap;
import java.util.List;

public class LogPreprocessingTaskResult {

    public LinkedHashMap<String, List<String>> attributeAndValue;
    public XLog xLog;

    public void setXLog(XLog xLog) {
        this.xLog = xLog;
    }

    public XLog getXLog() {
        return this.xLog;
    }

    public void setAttributeAndValue(LinkedHashMap<String, List<String>> attributeAndValue) {
        this.attributeAndValue = attributeAndValue;
    }

    public LinkedHashMap<String, List<String>> getAttributeAndValue() {
        return this.attributeAndValue;
    }
}
