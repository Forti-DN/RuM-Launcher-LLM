package controller.filtering.cell;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ParameterCell {
    public final StringProperty name;
    public final StringProperty type;
    public final TextField value;

    public ParameterCell(String name, String type, String value) {
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        TextField textField = new TextField(value);
        HBox.setHgrow(textField, Priority.ALWAYS);
        textField.setText(value);
        this.value = textField;
    }

    public TextField getValue() {
        return value;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getType() {
        return type.get();
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public void setValue(String value) {
        this.value.setText(value);
    }
}
