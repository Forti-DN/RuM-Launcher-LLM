package controller.filtering;

import controller.common.AbstractController;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.plugins.ltlchecker.InstanceModel;
import util.AlertUtils;
import util.FileUtils;
import util.LogUtils;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class FilteringPageController extends AbstractController {
    @FXML
    Label introLabel;
    @FXML
    TabPane filteringTabPane;
    public static File ltlFile = new File(System.getProperty("user.dir") + "/ltlformulas/ltlfilters.ltl");
    private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    public InventorySavedElement eventLog;

    @FXML
    private void initialize() {
        filteringTabPane.getTabs().addListener(getTabChangeListener()); //Set info label to be visible only if no tabs are open
    }

    @FXML
    private void openLog() {
        eventLog = FileUtils.showSavedElementDialog(InventoryElementTypeEnum.EVENT_LOG);
        openLogInTab(eventLog);
    }

    public void openLogFromInventory(InventorySavedElement eventLog) {
        openLogInTab(eventLog);
    }

    private void openLogInTab(InventorySavedElement eventLog) {
        if (eventLog != null) {

            logger.info("Opened event log in filtering page: {}", eventLog.getFile().getAbsolutePath());

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("pages/filtering/FilteringTab.fxml"));
                Region rootPane = loader.load();

                FilteringTabController controller = loader.getController();
                controller.setStage(this.getStage());
                controller.setLog(eventLog.getFile());
                controller.preprocessLog();

                Tab tab = new Tab();
                tab.setContent(rootPane);
                tab.setText(eventLog.getFile().getName());
                filteringTabPane.getTabs().add(tab);
                filteringTabPane.getSelectionModel().select(tab);

            } catch (Exception e) {
                AlertUtils.showError("Error opening Filtering tab!");
                logger.error("Can not load Filtering tab", e);
            }
        }
    }

    public static LinkedList<InstanceModel> deepCloneTracesList(LinkedList<InstanceModel> currentListOfInstanceModels) {
        return currentListOfInstanceModels.stream().map(LogUtils::deepCloneTrace).collect(Collectors.toCollection(LinkedList::new));
    }

    private ListChangeListener<Tab> getTabChangeListener() {
        return change -> {
            boolean isEmpty = filteringTabPane.getTabs().isEmpty();
            filteringTabPane.setVisible(!isEmpty);
            introLabel.setVisible(isEmpty);
            introLabel.setManaged(isEmpty);
        };
    }
}
