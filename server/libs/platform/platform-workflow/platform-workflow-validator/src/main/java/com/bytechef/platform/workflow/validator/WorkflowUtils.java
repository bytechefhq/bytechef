/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.workflow.validator;

import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
class WorkflowUtils {

    private static final Pattern COMBINED_PATTERN = Pattern.compile(
        "\"([^\"]+)\"\\s*:\\s*(?:(\\{(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*})*})*\"metadata\"(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*})*})*})|\"([^\"]*?@[^@]+@[^\"]*?)\")");

    private static final Evaluator EVALUATOR = SpelEvaluator.builder()
        .build();

    public static String convertPropertyInfoToJson(@Nullable List<PropertyInfo> propertyInfos) {
        if (propertyInfos == null || propertyInfos.isEmpty()) {
            return "{ \"parameters\": {} }";
        }

        // Remove the special case that flattens single object properties

        StringBuilder json = new StringBuilder();

        json.append("{ \"parameters\": {");

        for (int i = 0; i < propertyInfos.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }

            json.append(convertSinglePropertyToJson(propertyInfos.get(i)));
        }

        json.append("} }");

        return json.toString();
    }

    public static boolean extractAndEvaluateCondition(String condition, JsonNode actualParameters) {
        if (StringUtils.isBlank(condition)) {
            return true;
        }

        try {
            return evaluateCondition(condition, actualParameters);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid logic for display condition: '" + condition + "'");
        }
    }

    @Nullable
    public static JsonNode getNestedField(@Nullable JsonNode jsonNode, @Nullable String fieldPath) {
        if (jsonNode == null || fieldPath == null) {
            return null;
        }

        String[] pathParts = fieldPath.split("\\.");
        JsonNode currentJsonNode = jsonNode;

        for (String part : pathParts) {
            if (currentJsonNode == null) {
                return null;
            }

            // Check if part contains array index like "items[1]" or nested indices like "items[1][2]"
            if (part.contains("[") && part.contains("]")) {
                String remainingPart = part;

                // Handle multiple array indices like "conditions[index][index]"
                while (remainingPart.contains("[") && remainingPart.contains("]")) {
                    int bracketStart = remainingPart.indexOf('[');
                    int bracketEnd = remainingPart.indexOf(']');

                    if (bracketStart < 0 || bracketEnd <= bracketStart) {
                        return null;
                    }

                    String fieldName = remainingPart.substring(0, bracketStart);
                    String indexStr = remainingPart.substring(bracketStart + 1, bracketEnd);

                    // If this is the first array access and fieldName is not empty
                    if (!fieldName.isEmpty()) {
                        if (!currentJsonNode.isObject() || !currentJsonNode.has(fieldName)) {
                            return null;
                        }

                        currentJsonNode = currentJsonNode.get(fieldName);
                    }

                    // Handle array access
                    try {
                        // Handle placeholder values like "index" by returning null
                        if ("index".equals(indexStr)) {
                            return null;
                        }

                        int index = Integer.parseInt(indexStr);

                        if (currentJsonNode == null || !currentJsonNode.isArray() || index < 0 ||
                            index >= currentJsonNode.size()) {

                            return null;
                        }

                        currentJsonNode = currentJsonNode.get(index);
                    } catch (NumberFormatException e) {
                        return null;
                    }

                    // Move to the remaining part after this bracket pair
                    remainingPart = remainingPart.substring(bracketEnd + 1);

                    // If there are more brackets, continue with the current node
                    if (remainingPart.startsWith("[")) {
                        // Continue to process the next array index
                        remainingPart = "temp" + remainingPart; // Add dummy field name for next iteration
                    } else {
                        // No more brackets, we're done with array processing
                        break;
                    }
                }
            } else {
                // Regular field access
                if (!currentJsonNode.isObject() || !currentJsonNode.has(part)) {
                    return null;
                }

                currentJsonNode = currentJsonNode.get(part);
            }
        }

        return currentJsonNode;
    }

    public static String processDisplayConditions(String taskDefinition, @Nullable String taskParameters) {
        if (taskParameters == null) {
            return taskDefinition;
        }

        try {
            JsonNode parametersNode = com.bytechef.commons.util.JsonUtils.readTree(taskParameters);

            return processMetadataObjectsRecursively(
                taskDefinition,
                parametersNode != null ? parametersNode : com.bytechef.commons.util.JsonUtils.readTree("{}"));
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions from invalid display conditions
            String message = e.getMessage();

            if (message != null && message.startsWith("Invalid logic for display condition:")) {
                throw e;
            }

            return taskDefinition;
        } catch (Exception e) {
            return taskDefinition;
        }
    }

    private static String buildComplexArrayContent(PropertyInfo propertyInfo) {
        List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

        if (propertyInfos.size() == 1) {
            return buildSingleComplexArrayItem(propertyInfos.getFirst());
        } else {
            return buildNestedObjectContent(propertyInfos);
        }
    }

    private static String buildNestedArrayContent(PropertyInfo arrayProperty) {
        StringBuilder json = new StringBuilder("[ ");

        boolean hasNestedComplexStructure = arrayProperty.nestedProperties()
            .stream()
            .anyMatch(prop -> {
                List<PropertyInfo> propertyInfos = prop.nestedProperties();

                return propertyInfos != null && !propertyInfos.isEmpty();
            });

        if (hasNestedComplexStructure) {
            json.append(buildNestedObjectContent(arrayProperty.nestedProperties()));
        } else {
            json.append(buildSimpleArrayContent(arrayProperty));
        }

        json.append(" ]");
        return json.toString();
    }

    private static String buildNestedObjectContent(List<PropertyInfo> nestedProperties) {
        StringBuilder json = new StringBuilder("{ ");

        for (int i = 0; i < nestedProperties.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }

            json.append(convertSinglePropertyToJson(nestedProperties.get(i)));
        }

        json.append(" }");

        return json.toString();
    }

    private static String buildSimpleArrayContent(PropertyInfo propertyInfo) {
        StringBuilder json = new StringBuilder();
        List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

        for (int i = 0; i < propertyInfos.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }

            PropertyInfo itemInfo = propertyInfos.get(i);

            String typeString = buildTypeString(itemInfo);

            json.append("\"")
                .append(typeString)
                .append("\"");
        }

        return json.toString();
    }

    private static String buildSingleComplexArrayItem(PropertyInfo singleProperty) {
        if ("OBJECT".equalsIgnoreCase(singleProperty.type()) && hasNestedProperties(singleProperty)) {
            return buildNestedObjectContent(singleProperty.nestedProperties());
        } else if ("ARRAY".equalsIgnoreCase(singleProperty.type()) && hasNestedProperties(singleProperty)) {
            return buildNestedArrayContent(singleProperty);
        } else {
            return "{ " + convertSinglePropertyToJson(singleProperty) + " }";
        }
    }

    private static String buildTypeString(PropertyInfo propertyInfo) {
        StringBuilder typeString = new StringBuilder();

        String type = propertyInfo.type();

        typeString.append(type.toLowerCase());

        // Add a display condition if present (inline condition)
        String displayCondition = propertyInfo.displayCondition();

        if (displayCondition != null && !displayCondition.isEmpty()) {
            typeString.append(" @")
                .append(displayCondition)
                .append("@");
        }

        // Add the required indicator
        if (propertyInfo.required()) {
            typeString.append(" (required)");
        }

        return typeString.toString();
    }

    private static String convertArrayPropertyToJson(PropertyInfo propertyInfo) {
        if (isTaskTypeArray(propertyInfo)) {
            return "[ \"object\" ]";
        }

        StringBuilder json = new StringBuilder("[ ");

        if (hasComplexArrayStructure(propertyInfo)) {
            json.append(buildComplexArrayContent(propertyInfo));
        } else {
            json.append(buildSimpleArrayContent(propertyInfo));
        }

        json.append(" ]");

        return json.toString();
    }

    private static String convertObjectPropertyToJson(PropertyInfo propertyInfo) {
        StringBuilder json = new StringBuilder("{");

        String displayCondition = propertyInfo.displayCondition();

        if (displayCondition != null && !displayCondition.isEmpty()) {
            json.append("\"metadata\": \"");
            json.append(displayCondition);
            json.append("\", ");
        }

        List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

        for (int i = 0; i < propertyInfos.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }

            json.append(convertSinglePropertyToJson(propertyInfos.get(i)));
        }

        json.append("}");

        return json.toString();
    }

    private static String convertSimplePropertyToJson(PropertyInfo propertyInfo) {
        if ("TASK".equalsIgnoreCase(propertyInfo.type())) {
            return "{}";
        } else {
            String typeString = buildTypeString(propertyInfo);

            return "\"" + typeString + "\"";
        }
    }

    private static String convertSinglePropertyToJson(PropertyInfo propertyInfo) {
        StringBuilder json = new StringBuilder();
        String propertyName = propertyInfo.name();

        json.append("\"");
        json.append(propertyName);
        json.append("\": ");

        if (hasNestedProperties(propertyInfo)) {
            if (isArrayType(propertyInfo)) {
                json.append(convertArrayPropertyToJson(propertyInfo));
            } else {
                json.append(convertObjectPropertyToJson(propertyInfo));
            }
        } else {
            json.append(convertSimplePropertyToJson(propertyInfo));
        }

        return json.toString();
    }

    private static boolean evaluateCondition(String condition, JsonNode actualParameters) {
        Map<String, String> map = new HashMap<>();
        // convert condition to SpEL condition
        map.put("convertedExpression", "=" + condition);

        Map<String, Object> actualParametersMap = actualParameters != null
            ? com.bytechef.commons.util.JsonUtils.read(actualParameters.toString(), new TypeReference<>() {})
            : Map.of();

        try {
            Map<String, Object> evaluated = EVALUATOR.evaluate(map, actualParametersMap);

            Object convertedExpression = evaluated.get("convertedExpression");

            return parseBoolean(convertedExpression.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static int getLastEndOfDuplicates(List<PropertyMatch> duplicates) {
        return duplicates.stream()
            .mapToInt(m -> m.end)
            .max()
            .orElse(0);
    }

    private static Map<String, List<PropertyMatch>> getPropertyWithCondition(String json) {
        Matcher matcher = COMBINED_PATTERN.matcher(json);

        Map<String, List<PropertyMatch>> propertyMatchesMap = new HashMap<>();

        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String objectValue = matcher.group(2);
            String inlineValue = matcher.group(3);

            String value = objectValue != null ? objectValue : inlineValue;

            PropertyMatch propertyMatch = new PropertyMatch(matcher.start(), matcher.end(), propertyName, value);

            List<PropertyMatch> propertyMatches = propertyMatchesMap.computeIfAbsent(
                propertyName, k -> new ArrayList<>());

            propertyMatches.add(propertyMatch);
        }

        return propertyMatchesMap;
    }

    private static boolean hasComplexArrayStructure(PropertyInfo propertyInfo) {
        List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

        boolean hasComplexItemStructure = propertyInfos.stream()
            .anyMatch(prop -> {
                List<PropertyInfo> curPropertyInfos = prop.nestedProperties();

                return curPropertyInfos != null && !curPropertyInfos.isEmpty();
            });

        boolean allMatch = propertyInfos
            .stream()
            .allMatch(curPropertyInfo -> {
                List<PropertyInfo> curPropertyInfos = curPropertyInfo.nestedProperties();

                return (curPropertyInfos == null || curPropertyInfos.isEmpty()) && !curPropertyInfo.required();
            });

        boolean isUnionType = propertyInfos.size() > 1 && allMatch;

        return hasComplexItemStructure || (propertyInfos.size() > 1 && !isUnionType);
    }

    private static boolean hasNestedProperties(PropertyInfo propertyInfo) {
        List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

        return propertyInfos != null && !propertyInfos.isEmpty();
    }

    private static boolean isArrayType(PropertyInfo propertyInfo) {
        return "ARRAY".equalsIgnoreCase(propertyInfo.type());
    }

    private static boolean isTaskTypeArray(PropertyInfo propertyInfo) {
        List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

        if (propertyInfos.size() == 1) {
            PropertyInfo propertyInfosFirst = propertyInfos.getFirst();

            return "TASK".equalsIgnoreCase(propertyInfosFirst.type());
        } else {
            return false;
        }
    }

    private static boolean parseBoolean(String string) {
        if (string == null) {
            throw new IllegalArgumentException("String cannot be null");
        }

        if ("true".equalsIgnoreCase(string)) {
            return true;
        } else if ("false".equalsIgnoreCase(string)) {
            return false;
        } else {
            throw new IllegalArgumentException(
                "Invalid boolean value: '" + string + "'. Expected 'true' or 'false'");
        }
    }

    private static String processMetadataObjectsRecursively(String json, JsonNode actualParameters) {
        Map<String, List<PropertyMatch>> propertyWithCondition = getPropertyWithCondition(json);

        if (propertyWithCondition.isEmpty()) {
            return json;
        }

        List<PropertyMatch> allPropertyMatches = new ArrayList<>();

        for (List<PropertyMatch> propertyMatches : propertyWithCondition.values()) {
            allPropertyMatches.addAll(propertyMatches);
        }

        allPropertyMatches.sort(Comparator.comparingInt(a -> a.start));

        int lastEnd = 0;
        Set<String> processedProperties = new HashSet<>();
        StringBuilder result = new StringBuilder();

        for (PropertyMatch match : allPropertyMatches) {
            if (processedProperties.contains(match.propertyName)) {
                continue;
            }

            result.append(json, lastEnd, match.start);

            List<PropertyMatch> duplicatePropertyMatches = propertyWithCondition.get(match.propertyName);
            PropertyMatch selectedPropertyMatch;

            selectedPropertyMatch = selectBestMatch(duplicatePropertyMatches, actualParameters);

            if (selectedPropertyMatch != null) {
                if (selectedPropertyMatch.objectValue.startsWith("{")) {
                    String processedContent = processMetadataObjectsRecursively(
                        selectedPropertyMatch.objectValue, actualParameters);

                    String cleanedObject = removeMetadataFromObjectString(processedContent);

                    result.append("\"")
                        .append(selectedPropertyMatch.propertyName)
                        .append("\": ")
                        .append(cleanedObject);
                } else {
                    String cleanedValue = StringUtils.trim(selectedPropertyMatch.objectValue.replaceAll("@[^@]+@", ""));

                    // If the property had an inline condition and we got here, it means the condition was true
                    // So we should mark it as required
                    if (selectedPropertyMatch.objectValue.contains("@") && !cleanedValue.contains("(required)")) {
                        cleanedValue += " (required)";
                    }

                    result.append("\"")
                        .append(selectedPropertyMatch.propertyName)
                        .append("\": \"")
                        .append(cleanedValue)
                        .append("\"");
                }

                lastEnd = selectedPropertyMatch.end;
            } else {
                lastEnd = getLastEndOfDuplicates(duplicatePropertyMatches);
            }

            processedProperties.add(match.propertyName);

            for (PropertyMatch duplicate : duplicatePropertyMatches) {
                if (duplicate.end > lastEnd) {
                    lastEnd = duplicate.end;
                }
            }
        }

        result.append(json.substring(lastEnd));

        return JsonUtils.cleanupJsonSyntax(result.toString());
    }

    private static String removeMetadataFromObjectString(String objectString) {
        String result = objectString.replaceAll("\"metadata\"\\s*:\\s*\"[^\"]*\"\\s*,?\\s*", "");

        result = JsonUtils.cleanupJsonSyntax(result);

        return result.replaceAll("\\{\\s*}", "{}");
    }

    @Nullable
    private static PropertyMatch selectBestMatch(List<PropertyMatch> duplicates, JsonNode actualParameters) {
        for (PropertyMatch propertyMatch : duplicates) {
            boolean shouldInclude;

            if (propertyMatch.objectValue.startsWith("{")) {
                shouldInclude = shouldIncludeObjectString(propertyMatch.objectValue, actualParameters);
            } else {
                shouldInclude = shouldIncludePropertyWithCondition(propertyMatch.objectValue, actualParameters);
            }

            if (shouldInclude) {
                return propertyMatch;
            }
        }

        return null;
    }

    private static boolean shouldIncludePropertyWithCondition(String condition, JsonNode actualParameters) {
        try {
            return evaluateCondition(condition, actualParameters);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid logic for display condition: '" + condition + "'");
        }
    }

    private static boolean shouldIncludeObjectString(String objectString, JsonNode actualParameters) {
        Pattern metadataPattern = Pattern.compile("\"metadata\"\\s*:\\s*\"([^\"]*)\"");

        Matcher matcher = metadataPattern.matcher(objectString);

        if (!matcher.find()) {
            return true;
        }

        String metadata = matcher.group(1);

        return shouldIncludePropertyWithCondition(metadata, actualParameters);
    }

    private record PropertyMatch(int start, int end, String propertyName, String objectValue) {
    }
}
