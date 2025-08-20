package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkflowParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static JsonNode parseJsonString(String jsonString) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(jsonString);
    }

    public static String processDisplayConditions(String taskDefinition, String taskParameters) {
        if (taskDefinition == null || taskParameters == null) {
            return taskDefinition;
        }

        try {
            JsonNode parametersNode = OBJECT_MAPPER.readTree(taskParameters);
            return processMetadataObjectsRecursively(taskDefinition, parametersNode);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions from invalid display conditions
            if (e.getMessage() != null && e.getMessage().startsWith("Invalid logic for display condition:")) {
                throw e;
            }
            return taskDefinition;
        } catch (Exception e) {
            return taskDefinition;
        }
    }

    public static String cleanupJsonSyntax(String jsonString) {
        return jsonString
            .replaceAll(",\\s*,", ",")
            .replaceAll(",\\s*}", "}")
            .replaceAll(",\\s*]", "]")
            .replaceAll("\\{\\s*,", "{")
            .replaceAll("\\[\\s*,", "[");
    }

    public static JsonNode getNestedField(JsonNode node, String fieldPath) {
        if (node == null || fieldPath == null) {
            return null;
        }

        String[] pathParts = fieldPath.split("\\.");
        JsonNode current = node;

        for (String part : pathParts) {
            if (current == null || !current.isObject() || !current.has(part)) {
                return null;
            }
            current = current.get(part);
        }

        return current;
    }

    public static JsonNode findFieldRecursively(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }

        if (node.has(fieldName)) {
            return node.get(fieldName);
        }

        for (JsonNode child : node) {
            if (child.isObject()) {
                JsonNode found = findFieldRecursively(child, fieldName);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    public static String getJsonNodeType(JsonNode node) {
        if (node.isTextual()) return "string";
        if (node.isFloatingPointNumber()) return "float";
        if (node.isIntegralNumber()) return "integer";
        if (node.isBoolean()) return "boolean";
        if (node.isArray()) return "array";
        if (node.isObject()) return "object";
        if (node.isNull()) return "null";
        return "unknown";
    }

    public static boolean isEmptyContainer(JsonNode node) {
        return (node.isObject() && node.isEmpty()) || (node.isArray() && node.isEmpty());
    }

    public static String buildPropertyPath(String parentPath, String propertyName) {
        return parentPath.isEmpty() ? propertyName : parentPath + "." + propertyName;
    }

    public static String extractPropertyNameFromPath(String arrayPath) {
        return arrayPath.contains(".")
            ? arrayPath.substring(arrayPath.lastIndexOf(".") + 1)
            : arrayPath;
    }

    public static String formatElementValue(JsonNode element) {
        return element.isTextual() ? "'" + element.asText() + "'" : element.toString();
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private static String processMetadataObjectsRecursively(String jsonString, JsonNode actualParameters) {
        Pattern combinedPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:(\\{(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})*\"metadata\"(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})*\\})|\"([^\"]*?@[^@]+@[^\"]*?)\")");
        Matcher matcher = combinedPattern.matcher(jsonString);

        Map<String, List<PropertyMatch>> propertyGroups = new HashMap<>();
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String objectValue = matcher.group(2);
            String inlineValue = matcher.group(3);

            String value = objectValue != null ? objectValue : inlineValue;

            PropertyMatch match = new PropertyMatch(matcher.start(), matcher.end(), propertyName, value);
            propertyGroups.computeIfAbsent(propertyName, k -> new ArrayList<>()).add(match);
        }

        if (propertyGroups.isEmpty()) {
            return jsonString;
        }

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        List<PropertyMatch> allMatches = new ArrayList<>();
        for (List<PropertyMatch> group : propertyGroups.values()) {
            allMatches.addAll(group);
        }
        allMatches.sort(Comparator.comparingInt(a -> a.start));

        Set<String> processedProperties = new HashSet<>();

        for (PropertyMatch match : allMatches) {
            if (processedProperties.contains(match.propertyName)) {
                continue;
            }

            result.append(jsonString.substring(lastEnd, match.start));

            List<PropertyMatch> duplicates = propertyGroups.get(match.propertyName);
            PropertyMatch selectedMatch;
            try {
                selectedMatch = selectBestMatch(duplicates, actualParameters);
            } catch (RuntimeException e) {
                // Re-throw runtime exceptions from invalid display conditions
                throw e;
            }

            if (selectedMatch != null) {
                if (selectedMatch.objectValue.startsWith("{")) {
                    String processedContent = processMetadataObjectsRecursively(selectedMatch.objectValue, actualParameters);
                    String cleanedObject = removeMetadataFromObjectString(processedContent);
                    result.append("\"").append(selectedMatch.propertyName).append("\": ").append(cleanedObject);
                } else {
                    String cleanedValue = selectedMatch.objectValue.replaceAll("@[^@]+@", "").trim();
                    result.append("\"").append(selectedMatch.propertyName).append("\": \"").append(cleanedValue).append("\"");
                }
                lastEnd = selectedMatch.end;
            } else {
                lastEnd = getLastEndOfDuplicates(duplicates);
            }

            processedProperties.add(match.propertyName);
            for (PropertyMatch duplicate : duplicates) {
                if (duplicate.end > lastEnd) {
                    lastEnd = duplicate.end;
                }
            }
        }

        result.append(jsonString.substring(lastEnd));
        return cleanupJsonSyntax(result.toString());
    }

    private static PropertyMatch selectBestMatch(List<PropertyMatch> duplicates, JsonNode actualParameters) {
        for (PropertyMatch match : duplicates) {
            try {
                boolean shouldInclude;
                if (match.objectValue.startsWith("{")) {
                    shouldInclude = shouldIncludeObjectString(match.objectValue, actualParameters);
                } else {
                    shouldInclude = shouldIncludePropertyWithCondition(match.objectValue, actualParameters);
                }

                if (shouldInclude) {
                    return match;
                }
            } catch (RuntimeException e) {
                // If there's an invalid logic error, throw it to be handled by the caller
                throw e;
            }
        }

        return null;
    }

    private static int getLastEndOfDuplicates(List<PropertyMatch> duplicates) {
        return duplicates.stream().mapToInt(m -> m.end).max().orElse(0);
    }

    private static boolean shouldIncludePropertyWithCondition(String propertyValue, JsonNode actualParameters) {
        return extractAndEvaluateCondition(propertyValue, actualParameters);
    }

    public static boolean extractAndEvaluateCondition(String text, JsonNode actualParameters) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }

        Pattern pattern = Pattern.compile("@([^@]+)@");
        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            return true;
        }

        String condition = matcher.group(1).trim();
        try {
            return evaluateCondition(condition, actualParameters);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid logic for display condition: '@" + condition + "@'");
        }
    }

    private static boolean shouldIncludeObjectString(String objectString, JsonNode actualParameters) {
        Pattern metadataPattern = Pattern.compile("\"metadata\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = metadataPattern.matcher(objectString);

        if (!matcher.find()) {
            return true;
        }

        String metadata = matcher.group(1);
        try {
            return shouldIncludePropertyWithCondition(metadata, actualParameters);
        } catch (RuntimeException e) {
            throw e; // Re-throw to let the caller handle it
        }
    }

    private static String removeMetadataFromObjectString(String objectString) {
        String result = objectString.replaceAll("\"metadata\"\\s*:\\s*\"[^\"]*\"\\s*,?\\s*", "");
        return cleanupJsonSyntax(result).replaceAll("\\{\\s*\\}", "{}");
    }

    private static boolean evaluateCondition(String condition, JsonNode actualParameters) {
        try {
            // Check for contains function first
            if (condition.contains("contains(")) {
                return evaluateContainsFunction(condition, actualParameters);
            }

            String[] operatorChecks = {"<=", ">=", "==", "<", ">"};
            String operator = null;
            String[] parts = null;

            for (String op : operatorChecks) {
                if (condition.contains(op)) {
                    parts = condition.split("\\s*" + Pattern.quote(op) + "\\s*");
                    operator = op;
                    break;
                }
            }

            if (parts == null || parts.length != 2) {
                // If condition is not empty and doesn't have valid operators, it's likely invalid
                if (condition.trim().length() > 0) {
                    throw new IllegalArgumentException("Invalid condition syntax");
                }
                return true;
            }

            String leftSide = parts[0].trim();
            String rightSide = parts[1].trim();
            String cleanLeftSide = cleanQuotes(leftSide);
            String cleanRightSide = cleanQuotes(rightSide);

            JsonNode fieldNode = findField(actualParameters, leftSide);
            if (fieldNode != null && !fieldNode.isNull()) {
                return performComparison(fieldNode, cleanRightSide, operator, true);
            }

            fieldNode = findField(actualParameters, rightSide);
            if (fieldNode != null && !fieldNode.isNull()) {
                return performComparison(fieldNode, cleanLeftSide, operator, false);
            }

            return performLiteralComparison(cleanLeftSide, cleanRightSide, operator);
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw IllegalArgumentException to be caught by caller
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid condition syntax");
        }
    }

    private static boolean evaluateContainsFunction(String condition, JsonNode actualParameters) {
        // Pattern to match contains({'val1','val2'}, fieldName) or contains({'val1','val2'}, 'literal')
        Pattern containsPattern = Pattern.compile("contains\\s*\\(\\s*\\{([^}]+)\\}\\s*,\\s*([^)]+)\\s*\\)");
        Matcher matcher = containsPattern.matcher(condition);
        
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid contains function syntax");
        }
        
        String arrayValues = matcher.group(1).trim();
        String checkValue = matcher.group(2).trim();
        
        // Parse the array values
        String[] values = arrayValues.split(",");
        for (int i = 0; i < values.length; i++) {
            values[i] = cleanQuotes(values[i].trim());
        }
        
        // Get the value to check
        String valueToCheck;
        JsonNode fieldNode = findField(actualParameters, checkValue);
        if (fieldNode != null && !fieldNode.isNull()) {
            valueToCheck = fieldNode.asText();
        } else {
            valueToCheck = cleanQuotes(checkValue);
        }
        
        // Check if valueToCheck is in the array
        for (String value : values) {
            if (value.equals(valueToCheck)) {
                return true;
            }
        }
        return false;
    }

    private static String cleanQuotes(String value) {
        return (value.startsWith("'") && value.endsWith("'")) ?
               value.substring(1, value.length() - 1) : value;
    }

    private static JsonNode findField(JsonNode parameters, String fieldName) {
        JsonNode field = getNestedField(parameters, fieldName);
        if ((field == null || field.isNull()) && !fieldName.contains(".")) {
            field = findFieldRecursively(parameters, fieldName);
        }
        return field;
    }

    private static boolean performComparison(JsonNode fieldNode, String expectedValue, String operator, boolean fieldOnLeft) {
        try {
            if ("==".equals(operator) && ("true".equalsIgnoreCase(expectedValue) || "false".equalsIgnoreCase(expectedValue))) {
                boolean expectedBool = Boolean.parseBoolean(expectedValue);
                boolean actualBool = fieldNode.isBoolean() ? fieldNode.asBoolean() : Boolean.parseBoolean(fieldNode.asText());
                return expectedBool == actualBool;
            }

            if (isNumeric(expectedValue) && fieldNode.isNumber()) {
                double actualNum = fieldNode.asDouble();
                double expectedNum = Double.parseDouble(expectedValue);

                return switch (operator) {
                    case "==" -> actualNum == expectedNum;
                    case "<=" -> fieldOnLeft ? actualNum <= expectedNum : expectedNum <= actualNum;
                    case ">=" -> fieldOnLeft ? actualNum >= expectedNum : expectedNum >= actualNum;
                    case "<" -> fieldOnLeft ? actualNum < expectedNum : expectedNum < actualNum;
                    case ">" -> fieldOnLeft ? actualNum > expectedNum : expectedNum > actualNum;
                    default -> false;
                };
            }

            String actualValue = fieldNode.asText();
            if ("==".equals(operator)) {
                return expectedValue.equals(actualValue);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean performLiteralComparison(String leftValue, String rightValue, String operator) {
        try {
            if (isNumeric(leftValue) && isNumeric(rightValue)) {
                double leftNum = Double.parseDouble(leftValue);
                double rightNum = Double.parseDouble(rightValue);

                return switch (operator) {
                    case "==" -> leftNum == rightNum;
                    case "<=" -> leftNum <= rightNum;
                    case ">=" -> leftNum >= rightNum;
                    case "<" -> leftNum < rightNum;
                    case ">" -> leftNum > rightNum;
                    default -> false;
                };
            }

            if ("==".equals(operator)) {
                return leftValue.equals(rightValue);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private record PropertyMatch(int start, int end, String propertyName, String objectValue) {}
}
