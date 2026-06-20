package controller.filtering;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.ltlchecker.InstanceModel;
import org.processmining.plugins.ltlchecker.LTLChecker;
import org.processmining.plugins.ltlchecker.parser.FormulaParameter;
import org.processmining.plugins.ltlchecker.parser.LTLParser;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LTLFiltersUtils {

    private static final String CONCEPT_NAME = "concept:name";
    private static final String LIFECYCLE_TRANSITION = "lifecycle:transition";
    private static final String ORGANIZATIONAL_RESOURCE = "org:resource";
    private static final List<String> XES_STANDARD_EXTENSIONS = List.of(CONCEPT_NAME, LIFECYCLE_TRANSITION, ORGANIZATIONAL_RESOURCE);

    protected static boolean isLTLFileValid(File ltlModelFile) {
        try {
            LTLParser parser = new LTLParser(new FileInputStream(ltlModelFile.getAbsolutePath()));
            parser.setFilename(ltlModelFile.getAbsolutePath());
            parser.init();
            parser.parse();
            return true;
        } catch (Exception | Error e) {
            return false;
        }
    }

    public static LinkedList<InstanceModel> convertXLogToInstanceModelList(XLog log) {
        return log.stream()
                .map(instance -> {
                    InstanceModel instanceModel = new InstanceModel();
                    instanceModel.setInstance(instance);
                    return instanceModel;
                })
                .collect(Collectors.toCollection(LinkedList::new));
    }

    protected static Vector<String> getVector(String formulaName) {
        return new Vector<>(Collections.singletonList(formulaName));
    }

    private static boolean isAttributeNumeric(String attribute) {
        try {
            Float.parseFloat(attribute);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected static Map<String, Map<String, String>> getParamTable(String formulaName, List<String> referenceAttributeValues, List<String> followerAttributeValues, LinkedHashSet<String> arguments) {
        List<String> mergedAttributeValues = new ArrayList<>(referenceAttributeValues);
        mergedAttributeValues.addAll(followerAttributeValues);

        return createParamTable(formulaName, new ArrayList<>(arguments), mergedAttributeValues);
    }

    protected static Map<String, Map<String, String>> createParamTable(String key, List<String> keys, List<String> values) {
        Map<String, Map<String, String>> paramTable = new HashMap<>();
        Map<String, String> paramMap = new HashMap<>();

        IntStream.range(0, values.size()).forEach(i -> paramMap.put(keys.get(i), values.get(i).trim()));

        paramTable.put(key, Collections.unmodifiableMap(paramMap));

        return Collections.unmodifiableMap(paramTable);
    }


    public static File createTemporaryLTLFile(String ltlFileContent) {
        File temporaryLTLFile;
        try {
            temporaryLTLFile = File.createTempFile("RuM_ltlChecker_filterByFollower_formulas", ".ltl");
            try (FileWriter writer = new FileWriter(temporaryLTLFile); BufferedWriter bw = new BufferedWriter(writer)) {
                bw.write(ltlFileContent);
            }
        } catch (IOException e) {
            return null;
        }
        return temporaryLTLFile;
    }


    protected static String generateLTLFormulaSignature(String followerFormula, LinkedHashSet<String> argumentsName) {

        String argumentList = generateFormulaArguments(argumentsName, "renamedattribute");

        switch (followerFormula) {
            case "eventually_followed": {
                return "formula eventually_followed_X_by_Y( " + argumentList + " ):= {}\n";
            }
            case "directly_followed": {
                return "formula directly_followed_X_by_Y( " + argumentList + " ):= {}\n";
            }
            case "never_eventually_followed": {
                return "formula never_eventually_followed_X_by_Y( " + argumentList + " ):= {}\n";
            }
            case "never_directly_followed": {
                return "formula never_directly_followed_X_by_Y( " + argumentList + " ):= {}\n";
            }
            default: {
                throw new IllegalArgumentException("Invalid formula name: " + followerFormula);
            }
        }
    }

    static String generateFormulaArguments(LinkedHashSet<String> arguments, String renamedAttribute) {

        StringBuilder argumentsString = new StringBuilder();
        int argumentCount = 0;
        int argumentSize = arguments.size();

        for (String currentArgument : arguments) {
            argumentsString.append(currentArgument);
            argumentsString.append(" : ");
            argumentsString.append(renamedAttribute);  // "renamedattribute"

            if (argumentCount < argumentSize - 1) {
                argumentsString.append(" , ");
            }

            argumentCount++;
        }
        return argumentsString.toString();
    }

    public static LinkedHashSet<String> getSubset(LinkedHashSet<String> set, int fromIndex) {
        return new LinkedHashSet<>(new ArrayList<>(set).subList(fromIndex, set.size()));
    }

    protected static String generateLTLFormula_FollowerFormulas(String followerFormula, List<String> referenceAttributeValues, List<String> followerAttributeValues, LinkedHashSet<String> arguments) {

        String sourceAttributeCondition = generateAttributeCondition(referenceAttributeValues, arguments, "renamedattribute");
        String targetAttributeCondition = generateAttributeCondition(followerAttributeValues, getSubset(arguments, referenceAttributeValues.size()), "renamedattribute");

        switch (followerFormula) {
            case "eventually_followed": {
                return "[] ( ( " + sourceAttributeCondition + " -> <> ( " + targetAttributeCondition + " ) ) );";
            }
            case "directly_followed": {
                return "[] ( ( " + sourceAttributeCondition + " -> _O ( " + targetAttributeCondition + " ) ) );";
            }
            case "never_eventually_followed": {
                return "[] ( ( " + sourceAttributeCondition + " -> ! ( <> ( " + targetAttributeCondition + " ) ) ) );";
            }
            case "never_directly_followed": {
                return "[] ( ( " + sourceAttributeCondition + " -> ! ( _O ( " + targetAttributeCondition + " ) ) ) );";
            }
            default: {
                throw new IllegalArgumentException("Invalid formula name: " + followerFormula);
            }
        }
    }

    protected static String generateLTLFormula_declareFormulas(String declareLTLFormula, List<String> referenceAttributeValues, List<String> followerAttributeValues, LinkedHashSet<String> arguments) {

        String sourceAttributeCondition = generateAttributeCondition(referenceAttributeValues, arguments, "activity");
        String targetAttributeCondition = generateAttributeCondition(followerAttributeValues, getSubset(arguments, referenceAttributeValues.size()), "activity");


        switch (declareLTLFormula) {
            case "responded_existence": {
                return "( <> ( " + sourceAttributeCondition + " ) -> <> ( " + targetAttributeCondition + " ) ) ;";
            }
            case "not_responded_existence": {
                return "( <> ( " + sourceAttributeCondition + " ) -> ! ( <> ( " + targetAttributeCondition + " ) ) ) ;";
            }
            case "response": {
                return "[] ( ( " + sourceAttributeCondition + " ->  <> ( " + targetAttributeCondition + " ) ) );";
            }
            case "not_response": {
                return "[] ( ( " + sourceAttributeCondition + " -> ! ( <> ( " + targetAttributeCondition + " ) ) ) );";
            }
            case "chain_response": {
                return "[] ( ( " + sourceAttributeCondition + " -> _O ( " + targetAttributeCondition + " ) ) );";
            }
            case "not_chain_response": {
                return "[] ( ( " + sourceAttributeCondition + " -> ! ( _O ( " + targetAttributeCondition + " ) ) ) );";
            }
            case "alternate_response": {
                return "[] ( ( " + sourceAttributeCondition + " -> _O ( (! ( " + sourceAttributeCondition + ") _U " + targetAttributeCondition + " ) ) )) ;";
            }
            case "not_chain_precedence": {
                return "[] ( (  " + sourceAttributeCondition + "  -> ! ( _O (" + targetAttributeCondition + " ) ) ) );";
            }
            case "not_precedence": {
                return "[] ( ( " + sourceAttributeCondition + " ->  ! ( <> ( " + targetAttributeCondition + " ) ) ) );";
            }
            case "chain_precedence": {
              //  return "[] ((( _O( " + targetAttributeCondition + " ) -> " + sourceAttributeCondition + ")  /\\ ( ! ( " + targetAttributeCondition + " ) \\/ ( ! (_O ( " + sourceAttributeCondition + " )) /\\ ! ( _O ( ! ( " + sourceAttributeCondition + ")))))));";
               return "[] ( (_O( " + targetAttributeCondition + " ) -> " + sourceAttributeCondition + " ));";
            }
            case "precedence": {
                return "(( ! ( " + targetAttributeCondition + " ) _U " + sourceAttributeCondition + " )  \\/ ([] ( ! ( " + targetAttributeCondition + ")) /\\  (!( " + targetAttributeCondition + " )  \\/ (!(_O( " + sourceAttributeCondition + ")) /\\ !(_O(!( " + sourceAttributeCondition + "))))))) ;";
            }
            case "alternate_precedence": {
                return "(( ( ( !( " + targetAttributeCondition + " ) _U " + sourceAttributeCondition + ") \\/ []( !( " + targetAttributeCondition + "))) /\\ [](( " + targetAttributeCondition + " ->( (!(_O(" + sourceAttributeCondition + " )) /\\ !(_O(!( " + sourceAttributeCondition + " ))) ) \\/ _O((( !( " + targetAttributeCondition + ") _U " + sourceAttributeCondition + ") \\/ []( !( " + targetAttributeCondition + ")))))))) /\\ (  ! ( " + targetAttributeCondition + ") \\/ (!(_O( " + sourceAttributeCondition + ")) /\\ !(_O(!( " + sourceAttributeCondition + "))) ) ));";
            }
            default: {
                throw new IllegalArgumentException("Invalid formula name: " + declareLTLFormula);
            }
        }
    }

    private static String generateAttributeCondition(List<String> parameters, LinkedHashSet<String> arguments, String renamedAttribute) {
        StringBuilder attributeConditionBuilder = new StringBuilder();
        int openParenthesesCount = 0;
        List<String> argumentsList = new ArrayList<>(arguments);

        // Iterate through all attributes.
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.size() == 1) {
                attributeConditionBuilder.append(renamedAttribute);
                attributeConditionBuilder.append(" == ");
                attributeConditionBuilder.append(argumentsList.get(i));
            } else {

                if (parameters.size() - i > 1) {
                    attributeConditionBuilder.append("( ");
                    openParenthesesCount++;
                }

                attributeConditionBuilder.append(renamedAttribute);
                attributeConditionBuilder.append(" == ");
                attributeConditionBuilder.append(argumentsList.get(i));


                if (parameters.size() - i > 1) {
                    attributeConditionBuilder.append(" \\/ ");
                }

                if (parameters.size() - 1 == i && openParenthesesCount > 0) {
                    attributeConditionBuilder.append(" )".repeat(openParenthesesCount));
                    openParenthesesCount = 0;
                }
            }
        }
        return attributeConditionBuilder.toString();
    }

    public static LinkedHashSet<String> generateUniqueWords(int numWords) {
        LinkedHashSet<String> uniqueWordsSet = new LinkedHashSet<>();
        Random random = new Random();

        // Loop until the desired number of unique words is generated
        while (uniqueWordsSet.size() < numWords) {
            StringBuilder wordBuilder = new StringBuilder();

            // Generate a random two-letter word
            for (int i = 0; i < 2; i++) {
                char letter = (char) (random.nextInt(26) + 'a');
                wordBuilder.append(letter);
            }

            // Add the word to the set
            uniqueWordsSet.add(wordBuilder.toString());
        }

        return uniqueWordsSet;
    }

    public static String getStandardExtensionMapping(String attribute) {

        if (CONCEPT_NAME.equals(attribute)) {
            return "string ate.WorkflowModelElement;\n" + "rename ate.WorkflowModelElement as renamedattribute;\n\n";
        }

        if (LIFECYCLE_TRANSITION.equals(attribute)) {
            return "string ate.EventType;\n" + "rename ate.EventType as renamedattribute;\n\n";
        }

        if (ORGANIZATIONAL_RESOURCE.equals(attribute)) {
            return "string ate.Originator;\n" + "rename ate.Originator as renamedattribute;\n\n";
        }

        return "not parsable";
    }

    protected static String generateLTLFileContent(String formulaName, String attribute, List<String> referenceAttributeValues, List<String> followerAttributeValues, LinkedHashSet<String> argumentsName) {

        final String renamedAttribute = "renamedattribute";
        String ltlSignature = generateLTLFormulaSignature(formulaName, argumentsName);
        String ltlFormula = generateLTLFormula_FollowerFormulas(formulaName, referenceAttributeValues, followerAttributeValues, argumentsName);

        if (XES_STANDARD_EXTENSIONS.contains(attribute)) {
            return String.format(getStandardExtensionMapping(attribute) + "%s\n %s", ltlSignature, ltlFormula);
        }

        if (isAttributeNumeric(referenceAttributeValues.get(0))) {
            return String.format("number ate.%s;\n" + "rename ate.%s as %s;\n\n" + "%s%s", attribute, attribute, renamedAttribute, ltlSignature, ltlFormula);
        }

        return String.format("string ate.%s;\n" + "rename ate.%s as %s;\n\n" + "%s%s", attribute.replace(' ', '_'), attribute.replace(' ', '_'), renamedAttribute, ltlSignature, ltlFormula);
    }


    // needed for import
    public static LTLParser getLTLParser(XLog logFile) {
        Vector<String> currentVector = new Vector<>();
        currentVector.add("last");
        Map<String, Map<String, String>> currentParamTable = new HashMap<>();
        Map<String, String> newMap = new HashMap<>();
        currentParamTable.put("last", newMap);
        LTLChecker checker = new LTLChecker();
        checker.setSkipReady(true);
        checker.setSelectedRules(currentVector);
        Object[] analyseLtl = checker.analyse(logFile, FilteringPageController.ltlFile, currentParamTable);

        return (LTLParser) analyseLtl[3];
    }

    public static boolean parametersAndValuesAreCorrectForFormula(List<FormulaParameter> listOfParameters, ArrayNode endArrayNode) {
        return IntStream.range(0, listOfParameters.size()).allMatch(i -> {
            FormulaParameter parameter = listOfParameters.get(i);
            String expectedName = parameter.getParam().getValue();
            String importedName = endArrayNode.get(i).asText();
            return expectedName.equals(importedName);
        });
    }

}