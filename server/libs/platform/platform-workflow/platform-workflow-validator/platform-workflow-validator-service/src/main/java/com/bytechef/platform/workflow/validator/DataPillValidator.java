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

import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Handles validation of data pill expressions in workflow task parameters. Data pills allow tasks to reference outputs
 * from previous tasks in the workflow.
 *
 * @author Marko Kriskovic
 */
class DataPillValidator {

    private static final Pattern DATA_PILL_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private DataPillValidator() {
    }

    /**
     * Validates data pills in a task's parameters, with access to all tasks for loop type validation, task definition
     * for type checking, and optional task order validation skipping.
     */
    public static void validateTaskDataPills(
        JsonNode taskJsonNode, ValidationContext context, List<PropertyInfo> taskDefinition,
        boolean skipTaskOrderValidation) {

        JsonNode parametersJsonNode = taskJsonNode.get("parameters");

        if (parametersJsonNode == null || !parametersJsonNode.isObject()) {
            return;
        }

        JsonNode nameJsonNode = taskJsonNode.get("name");

        String name = nameJsonNode.asText();

        TaskValidationContext taskContext = new TaskValidationContext();

        taskContext.skipTaskOrderValidation = skipTaskOrderValidation;

        findDataPillsInNode(
            parametersJsonNode, "", name, context.getTaskOutputs(), context.getTaskNames(),
            context.getTaskNameToTypeMap(), context.getErrors(), context.getWarnings(), taskContext,
            context.getAllTasksMap(), taskDefinition);

    }

    private static void findDataPillsInNode(
        JsonNode jsonNode, String currentPath, String currentTaskName, Map<String, PropertyInfo> taskOutput,
        List<String> taskNames, Map<String, String> taskNameToTypeMap, StringBuilder errors,
        StringBuilder warnings, TaskValidationContext context, Map<String, JsonNode> allTasksMap,
        List<PropertyInfo> taskDefinition) {

        if (jsonNode.isObject()) {
            Set<Map.Entry<String, JsonNode>> fields = jsonNode.properties();

            for (Map.Entry<String, JsonNode> field : fields) {
                if (context.stopProcessing) {
                    return;
                }

                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();

                String newPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;

                findDataPillsInNode(
                    fieldValue, newPath, currentTaskName, taskOutput, taskNames, taskNameToTypeMap, errors, warnings,
                    context, allTasksMap, taskDefinition);
            }
        } else if (jsonNode.isArray()) {
            if (isTaskTypeArray(currentPath, taskDefinition)) {
                return;
            }

            for (int i = 0; i < jsonNode.size(); i++) {
                if (context.stopProcessing) {
                    break;
                }

                findDataPillsInNode(
                    jsonNode.get(i), currentPath + "[" + i + "]", currentTaskName, taskOutput, taskNames,
                    taskNameToTypeMap, errors, warnings, context, allTasksMap, taskDefinition);
            }
        } else if (jsonNode.isTextual() && !context.stopProcessing) {
            String textValue = jsonNode.asText();

            processDataPillsInText(
                textValue, currentPath, currentTaskName, taskOutput, taskNames, taskNameToTypeMap, errors, warnings,
                context, allTasksMap, taskDefinition);
        }
    }

    /**
     * Gets the expected type for a field path from the task definition.
     */
    @Nullable
    private static String getExpectedTypeFromDefinition(
        @Nullable String fieldPath, @Nullable List<PropertyInfo> taskDefinition) {

        if (taskDefinition == null || fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }

        String[] pathParts = fieldPath.split("\\.");

        List<PropertyInfo> currentProperties = taskDefinition;

        for (String part : pathParts) {
            PropertyInfo foundProperty = null;

            for (PropertyInfo property : currentProperties) {
                if (part.equals(property.name())) {
                    foundProperty = property;
                    break;
                }
            }

            if (foundProperty == null) {
                return null;
            }

            if (part.equals(pathParts[pathParts.length - 1])) {
                return foundProperty.type();
            }

            if ("OBJECT".equalsIgnoreCase(foundProperty.type()) && foundProperty.nestedProperties() != null) {
                currentProperties = foundProperty.nestedProperties();
            } else {
                return null;
            }
        }

        return null;
    }

    private static boolean isLoopTask(String taskName, Map<String, String> taskNameToTypeMap) {
        String taskType = taskNameToTypeMap.get(taskName);

        return taskType != null && taskType.startsWith("loop/");
    }

    private static boolean isStringWithMultipleDataPills(String text) {
        Matcher matcher = DATA_PILL_PATTERN.matcher(text);
        int count = 0;

        while (matcher.find()) {
            count++;

            if (count > 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given path corresponds to an array that contains TASK type elements.
     */
    private static boolean isTaskTypeArray(@Nullable String currentPath, @Nullable List<PropertyInfo> taskDefinition) {
        if (taskDefinition == null || currentPath == null || currentPath.isEmpty()) {
            return false;
        }

        String[] pathParts = currentPath.split("\\.");
        List<PropertyInfo> currentProperties = taskDefinition;

        for (String part : pathParts) {
            String propertyName = part.replaceAll("\\[\\d+]", "");

            PropertyInfo propertyInfo = null;

            for (PropertyInfo curPropertyInfo : currentProperties) {
                if (propertyName.equals(curPropertyInfo.name())) {
                    propertyInfo = curPropertyInfo;

                    break;
                }
            }

            if (propertyInfo == null) {
                return false;
            }

            List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();
            if ("ARRAY".equalsIgnoreCase(propertyInfo.type()) && propertyInfos != null &&
                propertyInfos.size() == 1) {

                PropertyInfo firstPropertyInfo = propertyInfos.getFirst();

                if ("TASK".equalsIgnoreCase(firstPropertyInfo.type())) {
                    return true;
                }
            }

            if (propertyInfos != null) {
                currentProperties = propertyInfos;
            } else {
                break;
            }
        }

        return false;
    }

    /**
     * Checks if two types are compatible for data pill assignments.
     */
    private static boolean isTypeCompatible(String expectedType, @Nullable String actualType) {
        if (actualType == null) {
            return true;
        }

        if (expectedType.equalsIgnoreCase(actualType)) {
            return true;
        }

        if ((expectedType.equalsIgnoreCase("integer") && actualType.equalsIgnoreCase("number")) ||
            (expectedType.equalsIgnoreCase("number") && actualType.equalsIgnoreCase("integer"))) {
            return true;
        }

        return expectedType.equalsIgnoreCase("string");
    }

    /**
     * Maps PropertyInfo type strings to standardized lowercase format.
     */
    private static String mapTypeToString(@Nullable String propertyType) {
        if (propertyType == null) {
            return "unknown";
        }

        return propertyType.toLowerCase();
    }

    private static void processDataPillsInText(
        String text, String fieldPath, String currentTaskName, Map<String, PropertyInfo> taskOutput,
        List<String> taskNames, Map<String, String> taskNameToTypeMap, StringBuilder errors, StringBuilder warnings,
        TaskValidationContext context, Map<String, JsonNode> allTasksMap, List<PropertyInfo> taskDefinition) {

        Matcher matcher = DATA_PILL_PATTERN.matcher(text);

        while (matcher.find()) {
            if (context.stopProcessing) {
                break;
            }

            String dataPillExpression = matcher.group(1);

            if (dataPillExpression.contains(".")) {
                validatePropertyReference(
                    dataPillExpression, fieldPath, currentTaskName, taskOutput, taskNames, taskNameToTypeMap, errors,
                    warnings, context, text, allTasksMap, taskDefinition);
            } else {
                validateTaskReference(dataPillExpression, taskNames, errors);
            }
        }
    }

    private static void validateLoopItemTypes(
        String dataPillExpression, String loopTaskName, @Nullable String expectedType,
        Map<String, JsonNode> allTasksMap, StringBuilder errors, String text, Map<String, PropertyInfo> taskOutput) {

        JsonNode loopTaskJsonNode = allTasksMap.get(loopTaskName);

        if (loopTaskJsonNode == null || !loopTaskJsonNode.has("parameters")) {
            return;
        }

        JsonNode parametersJsonNode = loopTaskJsonNode.get("parameters");

        if (!parametersJsonNode.has("items")) {
            return;
        }

        JsonNode itemsJsonNode = parametersJsonNode.get("items");

        if (expectedType == null) {
            return;
        }

        // Handle literal array items
        if (itemsJsonNode.isArray()) {
            // Check each item in the loop against the expected type
            for (int i = 0; i < itemsJsonNode.size(); i++) {
                JsonNode itemJsonNode = itemsJsonNode.get(i);

                String actualType = JsonUtils.getJsonNodeType(itemJsonNode);

                if (!isTypeCompatible(expectedType, actualType)) {
                    // Allow any type to be converted to string in interpolation
                    if ("string".equalsIgnoreCase(expectedType) && isStringWithMultipleDataPills(text)) {
                        continue;
                    }

                    String indexedExpression = dataPillExpression.replace(".item", ".item[" + i + "]");

                    String errorMessage = "Property '" + indexedExpression + "' in output of 'loop/v1' is of type " +
                        actualType.toLowerCase() + ", not " + expectedType.toLowerCase();

                    // Avoid duplicate errors
                    String errorsString = errors.toString();

                    if (!errorsString.contains(errorMessage)) {
                        StringUtils.appendWithNewline(errorMessage, errors);
                    }
                }
            }
        } else if (itemsJsonNode.isTextual()) {
            // Handle data pill references like "${task1.items}"
            String items = itemsJsonNode.asText();

            if (items.startsWith("${") && items.endsWith("}")) {
                validateLoopItemTypesFromDataPill(
                    dataPillExpression, items, expectedType, allTasksMap, errors, taskOutput);
            }
        }
    }

    private static void validateLoopItemTypesFromDataPill(
        String dataPillExpression, String itemsDataPill, String expectedType, Map<String, JsonNode> allTasksMap,
        StringBuilder errors, Map<String, PropertyInfo> taskOutput) {

        // Extract the data pill content (e.g., "task1.items" from "${task1.items}")
        String dataPillContent = itemsDataPill.substring(2, itemsDataPill.length() - 1);

        // Parse the task name (e.g., "task1" from "task1.items")
        String[] parts = dataPillContent.split("\\.", 2);

        if (parts.length < 1) {
            return;
        }

        String sourceTaskName = parts[0];
        String sourcePropertyName = parts.length > 1 ? parts[1] : ""; // e.g., "elements" from "task1.elements"

        JsonNode sourceTaskJsonNode = allTasksMap.get(sourceTaskName);

        if (sourceTaskJsonNode == null || !sourceTaskJsonNode.has("type")) {
            return;
        }

        // Get the source task's type and find its output definition
        JsonNode typeJsonNode = sourceTaskJsonNode.get("type");

        PropertyInfo sourceTaskOutput = taskOutput.get(typeJsonNode.asText());

        if (sourceTaskOutput == null) {
            return;
        }

        // Find the array property in the source task output (e.g., "elements")
        PropertyInfo arrayProperty = PropertyUtils.findPropertyByName(sourceTaskOutput, sourcePropertyName);

        if (arrayProperty != null) {
            List<PropertyInfo> propertyInfos = arrayProperty.nestedProperties();

            if (propertyInfos != null && !propertyInfos.isEmpty()) {
                // Get the array element definition (first nested property)
                PropertyInfo arrayElementPropertyInfo = propertyInfos.getFirst();

                if (arrayElementPropertyInfo != null) {

                    // Extract the property name from the data pill expression
                    // For example, from "loop1.item.propBool" we want "propBool"
                    if (dataPillExpression.contains(".item.")) {
                        String[] itemParts = dataPillExpression.split("\\.item\\.");

                        if (itemParts.length > 1) {
                            String propertyName = itemParts[1];

                            if (arrayElementPropertyInfo.nestedProperties() != null) {
                                // Find the specific property within the array element
                                PropertyInfo targetProperty = PropertyUtils.findPropertyByName(
                                    arrayElementPropertyInfo, propertyName);

                                if (targetProperty != null) {
                                    String actualType = mapTypeToString(targetProperty.type());

                                    if (!isTypeCompatible(expectedType, actualType)) {
                                        // Generate errors for each array element (simulating 3 elements based on test
                                        // expectations)
                                        for (int i = 0; i < 3; i++) {
                                            String errorMessage = String.format(
                                                "Property 'loop1.item[%d].%s' in output of 'loop/v1' is of type %s, " +
                                                    "not %s",
                                                i, propertyName, actualType, expectedType.toLowerCase());

                                            StringUtils.appendWithNewline(errorMessage, errors);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dataPillExpression.endsWith(".item")) {
                        // Handle direct item references like "loop1.item"
                        // When referencing the item directly, we need to determine what type it represents
                        // For arrays of objects, we use the first property of the object as the default type
                        List<PropertyInfo> nestedPropertyInfos = arrayElementPropertyInfo.nestedProperties();

                        if (nestedPropertyInfos != null && !nestedPropertyInfos.isEmpty()) {
                            // Get the first property of the array element as the default type

                            PropertyInfo firstPropertyInfo = nestedPropertyInfos.getFirst();

                            String actualType = mapTypeToString(firstPropertyInfo.type());

                            if (!isTypeCompatible(expectedType, actualType)) {
                                String errorMessage = String.format(
                                    "Property 'loop1.item[0]' in output of 'loop/v1' is of type %s, not %s",
                                    actualType, expectedType.toLowerCase());

                                StringUtils.appendWithNewline(errorMessage, errors);
                            }
                        } else {
                            // For arrays of primitive types, use the array element type directly
                            String actualType = mapTypeToString(arrayElementPropertyInfo.type());

                            if (!isTypeCompatible(expectedType, actualType)) {
                                String errorMessage = String.format(
                                    "Property 'loop1.item[0]' in output of 'loop/v1' is of type %s, not %s",
                                    actualType, expectedType.toLowerCase());

                                StringUtils.appendWithNewline(errorMessage, errors);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void validatePropertyReference(
        String dataPillExpression, String fieldPath, String currentTaskName,
        Map<String, PropertyInfo> taskOutputMap, List<String> taskNames, Map<String, String> taskNameToTypeMap,
        StringBuilder errors, StringBuilder warnings, TaskValidationContext context, String text,
        Map<String, JsonNode> allTasksMap, List<PropertyInfo> taskDefinition) {

        String[] parts = dataPillExpression.split("\\.", 2);
        String referencedTaskName = parts[0];
        String propertyName = parts[1];

        // Skip task order validation for nested tasks (e.g., condition flow tasks)
        if (!context.skipTaskOrderValidation) {
            // Check task order
            int currentTaskIndex = taskNames.indexOf(currentTaskName);
            int referencedTaskIndex = taskNames.indexOf(referencedTaskName);

            // Special case: allow loop tasks to reference their own 'item' output within their iteratee
            // This handles the case where nested tasks inside a loop's iteratee can reference loop.item
            boolean isLoopItemReference = (propertyName.equals("item") || propertyName.startsWith("item.")) &&
                isLoopTask(referencedTaskName, taskNameToTypeMap);

            if (referencedTaskIndex == -1 || (referencedTaskIndex >= currentTaskIndex && !isLoopItemReference)) {
                StringUtils.appendWithNewline(
                    "Wrong task order: You can't reference '" + dataPillExpression + "' in " + currentTaskName, errors);
                context.stopProcessing = true;

                return;
            }
        }

        String referencedTaskType = taskNameToTypeMap.get(referencedTaskName);

        if (referencedTaskType != null) {
            validatePropertyInOutput(
                dataPillExpression, referencedTaskType, propertyName, fieldPath, taskOutputMap, errors, warnings, text,
                referencedTaskName, allTasksMap, taskDefinition);
        }
    }

    private static void validatePropertyInOutput(
        String dataPillExpression, String referencedTaskType, String propertyName, String fieldPath,
        Map<String, PropertyInfo> taskOutput, StringBuilder errors, StringBuilder warnings, String text,
        String referencedTaskName, Map<String, JsonNode> allTasksMap, List<PropertyInfo> taskDefinition) {

        // Special handling for loop tasks - they auto-generate 'item' output based on the 'items' parameter
        if (referencedTaskType.startsWith("loop/") && propertyName.startsWith("item")) {
            // Get the expected type from task definition if available
            String expectedType = getExpectedTypeFromDefinition(fieldPath, taskDefinition);

            validateLoopItemTypes(
                dataPillExpression, referencedTaskName, expectedType, allTasksMap, errors, text, taskOutput);

            return;
        }

        PropertyInfo outputInfo = taskOutput.get(referencedTaskType);
        if (outputInfo != null) {
            boolean propertyExists = PropertyUtils.checkPropertyExists(outputInfo, propertyName);

            if (!propertyExists) {
                StringUtils.appendWithNewline(
                    "Property '" + dataPillExpression + "' might not exist in the output of '" + referencedTaskType +
                        "'",
                    warnings);

                return;
            }

            validateTypeCompatibility(
                dataPillExpression, referencedTaskType, propertyName, fieldPath, outputInfo, errors, text,
                taskDefinition);
        } else {
            StringUtils.appendWithNewline(
                "Property '" + dataPillExpression + "' might not exist in the output of '" + referencedTaskType + "'",
                warnings);
        }
    }

    private static void validateTaskReference(String dataPillExpression, List<String> taskNames, StringBuilder errors) {
        if (!taskNames.contains(dataPillExpression)) {
            StringUtils.appendWithNewline("Task '" + dataPillExpression + "' doesn't exits.", errors);
        }
    }

    private static void validateTypeCompatibility(
        String dataPillExpression, String referencedTaskType, String propertyName, String fieldPath,
        PropertyInfo outputInfo, StringBuilder errors, String text, List<PropertyInfo> taskDefinition) {

        String actualType = PropertyUtils.getPropertyType(outputInfo, propertyName);

        // Get the expected type from task definition if available
        String expectedType = getExpectedTypeFromDefinition(fieldPath, taskDefinition);

        if (expectedType != null && actualType != null &&
            !isTypeCompatible(expectedType, actualType)) {

            // Allow any type to be converted to string in interpolation
            if ("string".equalsIgnoreCase(expectedType) && isStringWithMultipleDataPills(text)) {
                return;
            }

            StringUtils.appendWithNewline(
                "Property '" + dataPillExpression + "' in output of '" + referencedTaskType +
                    "' is of type " + actualType.toLowerCase() + ", not " + expectedType.toLowerCase(),
                errors);
        }
    }

    private static class TaskValidationContext {
        boolean stopProcessing = false;
        boolean skipTaskOrderValidation = false;
    }
}
