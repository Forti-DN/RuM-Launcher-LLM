package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import global.Inventory;
import global.InventoryElementTypeEnum;
import global.InventorySavedElement;
import global.InventorySavedElementListWrapper;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

public class FileUtils {

    private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static File previousDirectory;

    //Extension filters for models
    private static ExtensionFilter declExtensionFilter = new ExtensionFilter("Declare model", Arrays.asList("*.decl"));
    private static ExtensionFilter xmlExtensionFilter = new ExtensionFilter("XML model", Arrays.asList("*.xml"));
    private static ExtensionFilter txtExtensionFilter = new ExtensionFilter("Text file", Arrays.asList("*.txt"));
    private static ExtensionFilter dotExtensionFilter = new ExtensionFilter("Dot graph", Arrays.asList("*.dot"));
    private static ExtensionFilter ltlExtensionFilter = new ExtensionFilter("LTL Model", Arrays.asList("*.ltl"));
    //Extension filters for logs
    private static ExtensionFilter logImportExtensionFilter = new ExtensionFilter("Log file", Arrays.asList("*.xes", "*.mxml", "*.xes.gz", ".mxml.gz"));
    private static ExtensionFilter xesExtensionFilter = new ExtensionFilter("Log file", Arrays.asList("*.xes"));

    //Extension filters for images
    private static ExtensionFilter pngExtensionFilter = new ExtensionFilter("PNG", Arrays.asList("*.png"));

    //Additional extension filters
    private static ExtensionFilter csvExtensionFilter = new ExtensionFilter("CSV file", Arrays.asList("*.csv"));
    private static ExtensionFilter jsonExtensionFilter = new ExtensionFilter("JSon file", Arrays.asList("*.json"));
    private static ExtensionFilter allExtensionsFilter = new ExtensionFilter("All files", Arrays.asList("*.*"));

    // Private constructor to avoid unnecessary instantiation of the class
    private FileUtils() {
    }

    // Opening files

    public static File showModelOpenDialog(Stage stage) {
        return showOpenDialog(stage, declExtensionFilter, allExtensionsFilter);
    }

    public static File showTxtOpenDialog(Stage stage) {
        return showOpenDialog(stage, txtExtensionFilter, allExtensionsFilter);
    }
    public static File showJsonOpenDialog(Stage stage) {
        return showOpenDialog(stage, jsonExtensionFilter, allExtensionsFilter);
    }

    public static File showLTLModelOpenDialog(Stage stage) {
        return showOpenDialog(stage, ltlExtensionFilter);
    }

    public static File showLogOpenDialog(Stage stage) {
        return showOpenDialog(stage, logImportExtensionFilter, allExtensionsFilter);
    }

    private static File showOpenDialog(Stage stage, ExtensionFilter... extensionFilters) {
        FileChooser fileChooser = new FileChooser();

        if (previousDirectory != null && previousDirectory.exists())
            fileChooser.setInitialDirectory(previousDirectory);

        for (ExtensionFilter extensionFilter : extensionFilters)
            fileChooser.getExtensionFilters().add(extensionFilter);

        File chosenFile = fileChooser.showOpenDialog(stage);

        if (chosenFile != null)    // If true then the user just closed the dialog without choosing a file
            previousDirectory = chosenFile.getParentFile();

        return chosenFile;
    }

    public static InventorySavedElement showSavedElementDialog(InventoryElementTypeEnum elementType) {

        InventorySavedElement inventorySavedElement = null;

        // Either display saved process models or saved event logs
        if (InventoryElementTypeEnum.PROCESS_MODEL.equals(elementType)) {
            Inventory.getSavedModelInstance().getStage().showAndWait();

            // read selected model
            inventorySavedElement = Inventory.getSavedModelInstance().getSelectedRow();
            // reset selection
            Inventory.getSavedModelInstance().resetSelectedRow();
        } else if (elementType.equals(InventoryElementTypeEnum.LTL_MODEL)) {
            Inventory.getSavedLTLModelInstance().getStage().showAndWait();

            // read selected event log
            inventorySavedElement = Inventory.getSavedLTLModelInstance().getSelectedRow();
            // reset selection
            Inventory.getSavedLTLModelInstance().resetSelectedRow();
        } else {
            Inventory.getSavedEventLogInstance().getStage().showAndWait();

            // read selected event log
            inventorySavedElement = Inventory.getSavedEventLogInstance().getSelectedRow();
            // reset selection
            Inventory.getSavedEventLogInstance().resetSelectedRow();
        }

        // return the saved element
        return inventorySavedElement;

    }

    // Saving files

    public static File showDeclSaveDialog(Stage stage, File initialFile) {
        ModelExportChoice modelExportChoice = showModelSaveDialog(stage, initialFile, null, declExtensionFilter);
        return modelExportChoice != null ? modelExportChoice.getChosenFile() : null;
    }

    public static File showXesSaveDialog(Stage stage, File initialFile) {
        ModelExportChoice modelExportChoice = showModelSaveDialog(stage, initialFile, null, xesExtensionFilter);
        return modelExportChoice != null ? modelExportChoice.getChosenFile() : null;
    }

    public static File showTxtSaveDialog(Stage stage, File initialFile) {
        ModelExportChoice modelExportChoice = showModelSaveDialog(stage, initialFile, null, txtExtensionFilter);
        return modelExportChoice != null ? modelExportChoice.getChosenFile() : null;
    }

    public static File showJsonSaveDialog(Stage stage, File initialFile) {
        ModelExportChoice modelExportChoice = showModelSaveDialog(stage, initialFile, null, jsonExtensionFilter);
        return modelExportChoice != null ? modelExportChoice.getChosenFile() : null;
    }


    public static File showCsvSaveDialog(Stage stage, File initialFile) {
        ModelExportChoice modelExportChoice = showModelSaveDialog(stage, initialFile, null, csvExtensionFilter);
        return modelExportChoice != null ? modelExportChoice.getChosenFile() : null;
    }

    public static ModelExportChoice showModelSaveDialog(Stage stage, File initialFile, ModelViewType defaultExportType) {
        return showModelSaveDialog(stage, initialFile, defaultExportType, declExtensionFilter, txtExtensionFilter, dotExtensionFilter, xmlExtensionFilter);
    }

    private static ModelExportChoice showModelSaveDialog(Stage stage, File initialFile, ModelViewType defaultExportType, ExtensionFilter... extensionFilters) {
        FileChooser fileChooser = new FileChooser();
        if (initialFile != null) {
            if (initialFile.getParentFile().exists()) {
                fileChooser.setInitialDirectory(initialFile.getParentFile());
            }
            fileChooser.setInitialFileName(initialFile.getName());
        } else if (previousDirectory != null && previousDirectory.exists()) {
            fileChooser.setInitialDirectory(previousDirectory);
        }

        for (ExtensionFilter extensionFilter : extensionFilters) {
            fileChooser.getExtensionFilters().add(extensionFilter);
        }

        if (defaultExportType != null) {
            if (defaultExportType == ModelViewType.DECLARE) {
                fileChooser.setSelectedExtensionFilter(declExtensionFilter);
            } else if (defaultExportType == ModelViewType.TEXTUAL) {
                fileChooser.setSelectedExtensionFilter(txtExtensionFilter);
            } else if (defaultExportType == ModelViewType.AUTOMATON) {
                fileChooser.setSelectedExtensionFilter(dotExtensionFilter);
            }
        }

        File chosenFile = fileChooser.showSaveDialog(stage);

        //TODO: Could be refactored most likely
        if (chosenFile != null) { // If true then the user just closed the dialog without choosing a file
            previousDirectory = chosenFile.getParentFile();
            ModelViewType chosenExportType = null;
            if (fileChooser.getSelectedExtensionFilter() == declExtensionFilter) {
                chosenExportType = ModelViewType.DECLARE;
            } else if (fileChooser.getSelectedExtensionFilter() == txtExtensionFilter) {
                chosenExportType = ModelViewType.TEXTUAL;
            } else if (fileChooser.getSelectedExtensionFilter() == dotExtensionFilter) {
                chosenExportType = ModelViewType.AUTOMATON;
            } else if (fileChooser.getSelectedExtensionFilter() == xmlExtensionFilter) {
                chosenExportType = ModelViewType.XML_MODEL;
            }
            return new ModelExportChoice(chosenFile, chosenExportType);
        } else {
            return null;
        }
    }


    // Saving images

    public static File showImageSaveDialog(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        if (previousDirectory != null && previousDirectory.exists()) {
            fileChooser.setInitialDirectory(previousDirectory);
        }
        fileChooser.getExtensionFilters().add(pngExtensionFilter);

        File chosenFile = fileChooser.showSaveDialog(stage);
        if (chosenFile != null) {
            previousDirectory = chosenFile.getParentFile();
        }
        return chosenFile;
    }

    // Inventory items persistence

    public static void loadSavedElementsDataFromFile() {
        try {
            JAXBContext context = JAXBContext.newInstance(InventorySavedElementListWrapper.class);
            Unmarshaller um = context.createUnmarshaller();

            File file = new File("recentInventoryElements.xml");

            if (file.exists()) {
                // Reading XML from the file and unmarshalling.
                InventorySavedElementListWrapper wrapper = (InventorySavedElementListWrapper) um.unmarshal(file);

                if (wrapper.getSavedElements() != null) {
                    for (InventorySavedElement savedElement : wrapper.getSavedElements()) {
                        if (InventoryElementTypeEnum.PROCESS_MODEL.equals(savedElement.getTypeEnum())) {
                            Inventory.getSavedModelInstance().addFurtherElement(savedElement);
                        } else {
                            Inventory.getSavedEventLogInstance().addFurtherElement(savedElement);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Can not load saved inventory elements", e);
        }
    }

    public static void saveSavedElementsDataToFile() {
        try {
            JAXBContext context = JAXBContext.newInstance(InventorySavedElementListWrapper.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            File file = new File("recentInventoryElements.xml");

            InventorySavedElementListWrapper wrapper = new InventorySavedElementListWrapper();

            List<InventorySavedElement> savedElements = Inventory.getSavedModelInstance().getAvailableElements();
            savedElements.addAll(Inventory.getSavedEventLogInstance().getAvailableElements());

            wrapper.setSavedElements(savedElements);

            // Marshalling and saving XML to the file.
            m.marshal(wrapper, file);

        } catch (Exception e) {
            logger.error("Can not save inventory elements", e);
        }
    }

    // .decl file preprocessing

    public static String preprocessModel(File declModel, boolean cutTimeConds) {
        try {
            StringBuilder sb = new StringBuilder();
            Pattern constraintPattern = Pattern.compile("(.*)\\[(.*)\\]\\s*(.*)");

            Files.readAllLines(declModel.toPath()).forEach(line -> {
                String processedLine;
                Matcher mConstraint = constraintPattern.matcher(line);

                if (mConstraint.find()) {
                    String templateName = mConstraint.group(1);
                    String activities = mConstraint.group(2);
                    String dataAndTimeConditions = mConstraint.group(3);

                    processedLine = preprocessConstraintLine(templateName, activities, dataAndTimeConditions, cutTimeConds);

                } else
                    processedLine = line;

                sb.append(processedLine + "\n");
            });

            File tmpDeclModel = File.createTempFile("RuM_AlloyGen_model-", ".decl");
            tmpDeclModel.deleteOnExit();
            logger.debug("Created tmpDeclModel: {}", tmpDeclModel.getAbsolutePath());

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpDeclModel))) {
                bw.write(sb.toString());
            }

            return tmpDeclModel.getAbsolutePath();

        } catch (IOException e) {
            logger.error("Error in preprocessing declare model.");
            e.printStackTrace();

            return null;
        }
    }

    private static String preprocessConstraintLine(String templName, String activities, String conditions, boolean cutTimeConds) {

        // Alloy works with swapped activation and target activities for Precedence templates
        Matcher mActs = Pattern.compile("(.+), (.+)").matcher(activities);
        if (ConstraintTemplate.getByTemplateName(templName).getReverseActivationTarget())
            if (mActs.find())
                activities = mActs.group(2) + ", " + mActs.group(1);

        if (cutTimeConds) {
            // Removing time conditions from constraint line
            int lastPipeIndex = conditions.lastIndexOf("|");
            conditions = (lastPipeIndex >= 0 ? conditions.substring(0, lastPipeIndex) : conditions);
        }

        // Alloy works only with attributes preceded by "B." and not "T." as the notation of .decl files
        conditions = conditions.replace("T.", "B.");

        return translateConstraintForAlloy(templName, activities, conditions);
    }

    private static String translateConstraintForAlloy(String templName, String activitiesStr, String conds) {
        // Co-Existence is named CoExistence in Alloy
        if (templName.equals("Co-Existence"))
            templName = "CoExistence";

        // Alloy hasn't got blank spaces among template name words
        templName = templName.replaceAll("\\s+", "");

        // Alloy has a different notation for AbsenceN, ExistenceN and ExactlyN templates
        // e.g. Absence[Task] is expressed in Alloy as Absence[Task, 1]
        //		Exactly2[Task] is expressed in Alloy as Exactly[Task, 2]
        Matcher m = Pattern.compile("^(Absence|Existence|Exactly)(.*)$").matcher(templName);
        if (m.find()) {
            templName = m.group(1);
            activitiesStr += ", " + (m.group(2).isEmpty() ? "1" : m.group(2));
        }

        // Alloy doesn't supports some negative Declare templates... But they can be translated!
        switch (templName) {
            case "NotSuccession":
                return "NotResponse[" + activitiesStr + "]" + conds;

            case "NotChainSuccession":
                return "NotChainResponse[" + activitiesStr + "]" + conds;

            case "NotCo-Existence":
                return "NotRespondedExistence[" + activitiesStr + "]" + conds;
        }

        // No translation needed
        return templName + "[" + activitiesStr + "]" + conds;
    }


    public static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

}
