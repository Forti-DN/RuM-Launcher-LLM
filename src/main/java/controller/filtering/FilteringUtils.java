package controller.filtering;

import org.abego.treelayout.internal.util.java.util.IteratorUtil;
import org.apache.commons.io.FileUtils;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.plugins.declareminer.util.XLogReader;
import org.processmining.plugins.ltlchecker.InstanceModel;
import org.processmining.plugins.ltlchecker.LTLChecker;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class FilteringUtils {

    private static final List<String> DECLARE_LTL_FORMULAS = List.of("responded_existence", "not_responded_existence", "response", "not_response", "chain_response", "not_chain_response", "alternate_response", "not_chain_precedence", "not_precedence", "chain_precedence", "precedence", "alternate_precedence");

    protected static void applyFilterByAttributes_KeepSelectedMode(LinkedList<InstanceModel> traces, EventAttribute eventAttribute) {
        AtomicBoolean flag = new AtomicBoolean(true);

        for (InstanceModel currentTrace : traces) {
            Iterator<XEvent> iteratorEvents = currentTrace.getInstance().iterator();
            while (iteratorEvents.hasNext()) {
                XEvent currentEvent = iteratorEvents.next();
                XAttributeMap currentAttributes = currentEvent.getAttributes();

                for (String attributeValue : eventAttribute.attributeValue) {
                    if (currentAttributes.containsKey(eventAttribute.attributeName) && currentAttributes.get(eventAttribute.attributeName).toString().equals(attributeValue)) {
                        flag.set(false);
                    }
                }

                if (flag.get()) {
                    iteratorEvents.remove();
                }
                flag.set(true);
            }
        }

        traces.removeIf(currentTrace -> currentTrace.getInstance().isEmpty());
    }

    protected static void applyFilterByAttributes_MandatoryMode(LinkedList<InstanceModel> traces, EventAttribute eventAttribute) {
        AtomicBoolean flag = new AtomicBoolean(true);

        Iterator<InstanceModel> iteratorTrace = traces.iterator();
        while (iteratorTrace.hasNext()) {
            InstanceModel currentTrace = iteratorTrace.next();
            for (XEvent currentEvent : currentTrace.getInstance()) {
                XAttributeMap currentAttributes = currentEvent.getAttributes();

                for (String attributeValue : eventAttribute.attributeValue) {
                    if (currentAttributes.containsKey(eventAttribute.attributeName) && currentAttributes.get(eventAttribute.attributeName).toString().equals(attributeValue)) {
                        flag.set(false);
                        break;
                    }
                }
                if (!flag.get()) {
                    break;
                }
            }
            if (flag.get()) {
                iteratorTrace.remove();
            }
            flag.set(true);
        }
    }

    protected static void applyFilterByAttributes_ForbiddenMode(LinkedList<InstanceModel> traces, EventAttribute eventAttribute) {
        AtomicBoolean flag = new AtomicBoolean(true);

        Iterator<InstanceModel> iteratorTrace = traces.iterator();
        while (iteratorTrace.hasNext()) {
            InstanceModel currentTrace = iteratorTrace.next();
            for (XEvent currentEvent : currentTrace.getInstance()) {
                XAttributeMap currentAttributes = currentEvent.getAttributes();

                for (String attributeValue : eventAttribute.attributeValue) {
                    if (currentAttributes.containsKey(eventAttribute.attributeName) && currentAttributes.get(eventAttribute.attributeName).toString().equals(attributeValue)) {
                        flag.set(false);
                        break;
                    }
                }
                if (!flag.get()) {
                    break;
                }
            }
            if (!flag.get()) {
                iteratorTrace.remove();
            }
            flag.set(true);
        }
    }

    protected static void applyFilterByEndpoint_ModeTrimLongest(LinkedList<InstanceModel> traces, EventAttribute startValues, EventAttribute endValues) {

        if (startValues == null || endValues == null) {  // if one the start or end values is null
            traces.clear();
            return;
        }

        for (InstanceModel currentTrace : traces) {
            Iterator<XEvent> iteratorEvents = IteratorUtil.createReverseIterator(currentTrace.getInstance());

            while (iteratorEvents.hasNext()) {
                XEvent currentEvent = iteratorEvents.next();

                // if the current entry doesn't have that key
                if (!currentEvent.getAttributes().containsKey(endValues.attributeName)) {
                    iteratorEvents.remove();
                    continue;
                }

                // if the current entry is not equal to the start value
                if (!endValues.attributeValue.contains(currentEvent.getAttributes().get(endValues.attributeName).toString())) {
                    iteratorEvents.remove();
                    continue;
                }

                break;// if the current entry is the first occurrence of the end value in the map.
            }
        }


        for (InstanceModel currentTrace : traces) {
            Iterator<XEvent> iteratorEvents = currentTrace.getInstance().iterator();

            while (iteratorEvents.hasNext()) {
                XEvent currentEvent = iteratorEvents.next();

                // if the current entry doesn't have that key
                if (!currentEvent.getAttributes().containsKey(startValues.attributeName)) {
                    iteratorEvents.remove();
                    continue;
                }

                // if the current entry is not equal to the start value
                if (!startValues.attributeValue.contains(currentEvent.getAttributes().get(startValues.attributeName).toString())) {
                    iteratorEvents.remove();
                    continue;
                }

                break;// if the current entry is the first occurrence of the start value in the map.
            }
        }

        traces.removeIf(currentTrace -> currentTrace.getInstance().isEmpty());

    }


    protected static void applyFilterByEndpoint_ModeTrimFirst(LinkedList<InstanceModel> traces, EventAttribute startValues, EventAttribute endValues) {

        if (startValues == null || endValues == null) {  // if one the start or end values is null
            traces.clear();
            return;
        }

        for (InstanceModel currentTrace : traces) {
            boolean foundFirstAttribute = false;
            boolean foundSecondAttribute = false;

            Iterator<XEvent> iteratorEvents = currentTrace.getInstance().iterator();
            while (iteratorEvents.hasNext()) {
                XEvent currentEvent = iteratorEvents.next();

                // if the current entry is not equal to the start value
                if ((!foundFirstAttribute & currentEvent.getAttributes().containsKey(startValues.attributeName))) {
                    if (!startValues.attributeValue.contains(currentEvent.getAttributes().get(startValues.attributeName).toString())) {
                        iteratorEvents.remove();
                        continue;
                    }
                }

                if ((!foundFirstAttribute & !currentEvent.getAttributes().containsKey(startValues.attributeName))) {
                    iteratorEvents.remove();
                    continue;
                }

                // if the current entry is the first occurrence of the start value
                if (!foundFirstAttribute & currentEvent.getAttributes().containsKey(startValues.attributeName)) {
                    if (startValues.attributeValue.contains(currentEvent.getAttributes().get(startValues.attributeName).toString())) {
                        foundFirstAttribute = true;
                        foundSecondAttribute = endValues.attributeValue.contains(currentEvent.getAttributes().get(startValues.attributeName).toString());
                        continue;
                    }
                }

                // then we check if the current entry is the first occurrence of the end value
                if (!foundSecondAttribute & currentEvent.getAttributes().containsKey(endValues.attributeName)) {
                    if (endValues.attributeValue.contains(currentEvent.getAttributes().get(endValues.attributeName).toString())) {
                        foundSecondAttribute = true;
                        continue;
                    }
                }

                if (foundSecondAttribute) {
                    iteratorEvents.remove();
                }
            }
        }

        traces.removeIf(currentTrace -> currentTrace.getInstance().isEmpty());

    }


    protected static void applyFilterByEndpoint_ModeDiscardTraces(LinkedList<InstanceModel> traces, EventAttribute startValues, EventAttribute endValues) {
        Iterator<InstanceModel> iteratorTrace = traces.iterator();
        while (iteratorTrace.hasNext()) {
            InstanceModel currentTrace = iteratorTrace.next();

            if (endValues.attributeValue.isEmpty() || startValues.attributeValue.isEmpty()) {
                iteratorTrace.remove();
                continue;
            }


            if (!currentTrace.getInstance().get(0).getAttributes().containsKey(startValues.attributeName)) { // if first event does not contain the attribute
                iteratorTrace.remove();
                continue;
            }

            if (!currentTrace.getInstance().get(currentTrace.getInstance().size() - 1).getAttributes().containsKey(endValues.attributeName)) { // if last event does not contain the attribute
                iteratorTrace.remove();
                continue;
            }

            if (!startValues.attributeValue.contains(currentTrace.getInstance().get(0).getAttributes().get(startValues.attributeName).toString()) || !endValues.attributeValue.contains(currentTrace.getInstance().get(currentTrace.getInstance().size() - 1).getAttributes().get(endValues.attributeName).toString())) {
                iteratorTrace.remove();
            }
        }
    }

    protected static void applyFilterByTimeframe_ContainedInTimeframe(LinkedList<InstanceModel> traces, LocalDateTime minTimestamp, LocalDateTime maxTimestamp) {
        traces.removeIf(entry -> StatisticsUtils.getStartTimestampOfTrace(entry).isBefore(minTimestamp) || StatisticsUtils.getEndTimestampOfTrace(entry).isAfter(maxTimestamp));
    }

    protected static void applyFilterByTimeframe_StartedInTimeframe(LinkedList<InstanceModel> traces, LocalDateTime minTimestamp, LocalDateTime maxTimestamp) {
        traces.removeIf(entry -> StatisticsUtils.getStartTimestampOfTrace(entry).isBefore(minTimestamp) || StatisticsUtils.getStartTimestampOfTrace(entry).isAfter(maxTimestamp));
    }

    protected static void applyFilterByTimeframe_CompletedInTimeframe(LinkedList<InstanceModel> traces, LocalDateTime maxTimestamp) {
        traces.removeIf(entry -> StatisticsUtils.getEndTimestampOfTrace(entry).isAfter(maxTimestamp));
    }

    protected static void applyFilterByTimeframe_IntersectedInTimeframe(LinkedList<InstanceModel> traces, LocalDateTime minTimestamp, LocalDateTime maxTimestamp) {
        traces.removeIf(entry -> StatisticsUtils.getStartTimestampOfTrace(entry).isAfter(maxTimestamp) || StatisticsUtils.getEndTimestampOfTrace(entry).isBefore(minTimestamp));
    }

    protected static void applyFilterByTimeframe_TrimToTimeframe(LinkedList<InstanceModel> traces, LocalDateTime minTimestamp, LocalDateTime maxTimestamp) {
        for (InstanceModel currentTrace : traces) {
            Iterator<XEvent> iteratorEvents = IteratorUtil.createReverseIterator(currentTrace.getInstance());
            while (iteratorEvents.hasNext()) {
                XEvent currentEvent = iteratorEvents.next();

                if (!currentEvent.getAttributes().containsKey("time:timestamp")) {
                    iteratorEvents.remove();
                    continue;
                }

                LocalDateTime currentDate = ((XAttributeTimestampImpl) currentEvent.getAttributes().get("time:timestamp")).getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                if (currentDate.isBefore(minTimestamp) || currentDate.isAfter(maxTimestamp)) {
                    iteratorEvents.remove();
                }
            }
        }

        traces.removeIf(currentTrace -> currentTrace.getInstance().isEmpty());
    }

    protected static void applyFilterByPerformance_TraceDuration(LinkedList<InstanceModel> traces, double minDuration, double maxDuration) {
        traces.removeIf(instanceModel -> StatisticsUtils.getDurationOfTrace(instanceModel) < minDuration || StatisticsUtils.getDurationOfTrace(instanceModel) > maxDuration);
    }

    protected static void applyFilterByPerformance_NumberOfEvents(LinkedList<InstanceModel> traces, int minNumber, int maxNumber) {
        traces.removeIf(instanceModel -> StatisticsUtils.getNumberOfEventsInTrace(instanceModel) < minNumber || StatisticsUtils.getNumberOfEventsInTrace(instanceModel) > maxNumber);
    }

    public static void applyFilterByFollower(String formula, String attribute, List<String> referenceAttributeValues, List<String> followerAttributeValues, LinkedList<InstanceModel> traces) {

        // Create LTL formulas for Follower filers and add them inside an LTL File
        XLog logFile = FilteringUtils.createLogFromInstances(traces);
        LinkedHashSet<String> arguments = LTLFiltersUtils.generateUniqueWords(followerAttributeValues.size() + referenceAttributeValues.size());
        String ltlFileContent = LTLFiltersUtils.generateLTLFileContent(formula, attribute, referenceAttributeValues, followerAttributeValues, arguments);
        File ltlFile = LTLFiltersUtils.createTemporaryLTLFile(ltlFileContent);

        // Run LTLChecker
        Object[] analyseLtl = new Object[4];
        if (LTLFiltersUtils.isLTLFileValid(ltlFile)) {
            analyseLtl = initializeLtlChecker(formula + "_X_by_Y").analyse(logFile, ltlFile, LTLFiltersUtils.getParamTable(formula + "_X_by_Y", referenceAttributeValues, followerAttributeValues, arguments));
        }

        // Add result
        LinkedList<InstanceModel> currentResult = LTLFiltersUtils.convertXLogToInstanceModelList((XLogImpl) analyseLtl[1]);
        traces.clear();
        traces.addAll(currentResult);
    }


    public static void applyFilterByLTLFormulas(List<String> startValues, List<String> endValues, String mode, XLog logFile, LinkedList<InstanceModel> traces) {
        String ltlFormula = mode.contains(" ") ? mode.substring(0, mode.indexOf(" ")) : mode;

        if (DECLARE_LTL_FORMULAS.contains(ltlFormula)) {
            processBranchedDeclareFormulas(startValues, ltlFormula, logFile, traces);
        } else {
            runLtlCheckerAndUpdateTraces(ltlFormula, logFile, FilteringPageController.ltlFile, LTLFiltersUtils.createParamTable(ltlFormula, endValues, startValues), traces);
        }
    }


    public static void processBranchedDeclareFormulas(List<String> startValues, String ltlFormula, XLog logFile, LinkedList<InstanceModel> traces) {

        String activationConditionInput = startValues.get(0);
        String targetConditionInput = startValues.get(1);

        List<String> activationConditionArgs = parseString(activationConditionInput);
        List<String> targetConditionArgs = parseString(targetConditionInput);

        try {

            LinkedHashSet<String> arguments = LTLFiltersUtils.generateUniqueWords(activationConditionArgs.size() + targetConditionArgs.size());
            String formulaArgs = LTLFiltersUtils.generateFormulaArguments(arguments, "activity");

            // Create a temporary backup of the original file
            File backupFile = File.createTempFile("backup", ".tmp");
            FileUtils.copyFile(FilteringPageController.ltlFile, backupFile);

            // Step 2: Modify the original file
            String content = FileUtils.readFileToString(backupFile, "UTF-8");

            // Perform the replacement directly on the character sequence -- Formula signature
            StringBuilder modifiedContent = new StringBuilder(content);
            int index = modifiedContent.indexOf("formula " + ltlFormula);
            while (index != -1) {
                int lineStart = modifiedContent.lastIndexOf(System.lineSeparator(), index);
                int lineEnd = modifiedContent.indexOf(System.lineSeparator(), index);
                if (lineEnd == -1) lineEnd = modifiedContent.length();

                // Change the entire line to adapt to branched Declare formula
                String modifiedLine = "formula " + ltlFormula + "(" + formulaArgs + ") :=";
                modifiedContent.replace(lineStart, lineEnd, modifiedLine);

                int closingBracketIndex = modifiedContent.indexOf("}", index);
                if (closingBracketIndex != -1) {
                    // Find the start and end of the line after the closing bracket
                    int nextLineStart = modifiedContent.indexOf(System.lineSeparator(), closingBracketIndex);

                    nextLineStart += System.lineSeparator().length();  // Move to the start of the next line
                    int nextLineEnd = modifiedContent.indexOf(System.lineSeparator(), nextLineStart);
                    if (nextLineEnd == -1)
                        nextLineEnd = modifiedContent.length();  // If there's no more newline, the end is the end of the whole string

                    // Replace the entire line after the closing bracket with the new formula;
                    String branchedDeclareFormula = LTLFiltersUtils.generateLTLFormula_declareFormulas(ltlFormula, activationConditionArgs, targetConditionArgs, arguments);
                    modifiedContent.replace(nextLineStart, nextLineEnd, branchedDeclareFormula);
                }

                index = modifiedContent.indexOf("formula " + ltlFormula, lineStart + modifiedLine.length());
            }

            // Write the modified content back to the file (in order to match syntax for branched declare)
            FileUtils.writeStringToFile(backupFile, modifiedContent.toString());

            // Run LTLChecker and add result
            runLtlCheckerAndUpdateTraces(ltlFormula, logFile, backupFile, LTLFiltersUtils.getParamTable(ltlFormula, activationConditionArgs, targetConditionArgs, arguments), traces);

            // Delete the backup file manually. The file is set to be deleted on JVM exit, but you can delete it earlier if you no longer need it
            FileUtils.forceDelete(backupFile);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void runLtlCheckerAndUpdateTraces(String ltlFormula, XLog logFile, File ltlFile, Map<String, Map<String, String>> paramTable, LinkedList<InstanceModel> traces) {
        LTLChecker ltlChecker = initializeLtlChecker(ltlFormula);
        if (LTLFiltersUtils.isLTLFileValid(ltlFile)) {
            Object[] obj = ltlChecker.analyse(logFile, ltlFile, paramTable);LinkedList<InstanceModel> currentResult = LTLFiltersUtils.convertXLogToInstanceModelList((XLogImpl) obj[1]);
            traces.clear();
            traces.addAll(currentResult);
        }
    }

    private static LTLChecker initializeLtlChecker(String formula) {
        LTLChecker ltlChecker = new LTLChecker();
        ltlChecker.setSkipReady(true);
        ltlChecker.setSelectedRules(LTLFiltersUtils.getVector(formula));
        return ltlChecker;
    }


    public static XLog createLogFromInstances(LinkedList<InstanceModel> instanceModels) {
        XFactory factory = new XFactoryNaiveImpl();
        XLog newLog = factory.createLog();

        for (InstanceModel model : instanceModels) {
            XTrace trace = model.getInstance();
            newLog.add(trace);
        }

        return newLog;
    }

    protected static XLog getViolatedTraces(LinkedList<InstanceModel> satisfiedTraces, File logFile) throws Exception {
        XLog originalLog = XLogReader.openLog(logFile.getAbsolutePath());
        Set<String> satisfiedConceptNames = satisfiedTraces.stream().map(trace -> trace.getInstance().getAttributes().get("concept:name").toString()).collect(Collectors.toSet());
        XLog violatedTracesLog = XFactoryRegistry.instance().currentDefault().createLog(originalLog.getAttributes());

        for (XTrace trace : originalLog) {
            String conceptName = trace.getAttributes().get("concept:name").toString();
            if (!satisfiedConceptNames.contains(conceptName)) {
                violatedTracesLog.add(trace);
            }
        }

        return violatedTracesLog;
    }

    public static List<String> parseString(String input) {
        // Remove square brackets and quotes
        input = input.replace("[", "").replace("]", "").replace("\"", "");

        // Split on comma
        String[] parts = input.split(",");

        // Convert to list and remove leading/trailing whitespace from each element
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            list.add(part.trim());
        }
        return list;
    }
}