package controller.filtering.cell;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.processmining.plugins.ltlchecker.InstanceModel;
import org.processmining.plugins.ltlchecker.RuleModel;
import util.AlertUtils;

import java.io.IOException;

public class TemplateCell extends ListCell<RuleModel> {

    @FXML
    Label filter;
    @FXML
    VBox vbox;
    @FXML
    Text statisticLabel;

    public enum Type {
        FILTER_INSPECTOR,
        TRACE_INSPECTOR
    }

    private final PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");
    private final Enum<Type> type;
    protected MultipleSelectionModel<InstanceModel> traceSelectionModel;
    private MultipleSelectionModel<RuleModel> ruleSelectionModel;
    protected FXMLLoader loader;
    protected RuleModel rule;

    public TemplateCell(Enum<Type> type) {
        this.type = type;
        loadFXML();
    }

    @Override
    protected void updateItem(RuleModel rule, boolean empty) {
        super.updateItem(rule, empty);
        setText(null);
        if (empty || rule == null || type == Type.TRACE_INSPECTOR && traceSelectionModel.getSelectedItem() == null) {
            setGraphic(null);
        } else {
            this.rule = rule;
            filter.setText(rule.getRuleName());

            if (type == Type.FILTER_INSPECTOR) {
                boolean isSelected = rule.equals(ruleSelectionModel.getSelectedItem());
                vbox.pseudoClassStateChanged(selectedClass, isSelected);
                Color textColor = isSelected ? Color.WHITE : new Color(0.224, 0.224, 0.224, 1);
                filter.setTextFill(textColor);
                statisticLabel.setFill(textColor);
                statisticLabel.setText("Support: " + String.format("%.2f", rule.getCoverage()) + "%");
            } else if (type == Type.TRACE_INSPECTOR) {
                filter.setTextFill(new Color(0.224, 0.224, 0.224, 1));
            }

            setGraphic(vbox);
        }
    }

    private void loadFXML() {
        if (loader == null) {
            loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/filtering/TemplateCell.fxml"));
            loader.setController(this);
            try {
                loader.load();
            } catch (IOException e) {
                AlertUtils.showWarning("Can not load cell");
            }
        }
    }

    public void setRuleSelectionModel(MultipleSelectionModel<RuleModel> ruleSelectionModel) {
        this.ruleSelectionModel = ruleSelectionModel;
    }

}