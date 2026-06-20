package controller.filtering;

import java.util.List;

public class EventAttribute {
    protected String attributeName;
    protected List<String> attributeValue;

    protected EventAttribute(String attributeName, List<String> attributeValue) {
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    public String getAttributeName() {
        return this.attributeName;
    }
}
