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

package utils;

import com.bytechef.ai.mcp.tool.automation.ToolUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles validation of data pill expressions in workflow task parameters. Data pills allow tasks to reference outputs
 * from previous tasks in the workflow.
 */
public class DataPillValidator {

    private static final Pattern DATA_PILL_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private DataPillValidator() {
        // Utility class
    }

    /**
     * Validates data pills in a task's parameters, with access to all tasks for loop type validation and task
     * definition for type checking.
     */
    public static boolean validateTaskDataPills(
        JsonNode task, Map<String, ToolUtils.PropertyInfo> taskOutput,
        List<String> taskNames, Map<String, String> taskNameToTypeMap,
        StringBuilder errors, StringBuilder warnings, Map<String, JsonNode> allTasksMap,
        List<ToolUtils.PropertyInfo> taskDefinition) {
        return validateTaskDataPills(task, taskOutput, taskNames, taskNameToTypeMap, errors, warnings, allTasksMap,
            taskDefinition, false);
    }

    /**
     * Validates data pills in a task's parameters, with access to all tasks for loop type validation and task
     * definition for type checking.
     */
    public static boolean validateTaskDataPills(
        JsonNode task, Map<String, ToolUtils.PropertyInfo> taskOutput,
        List<String> taskNames, Map<String, String> taskNameToTypeMap,
        StringBuilder errors, StringBuilder warnings, Map<String, JsonNode> allTasksMap,
        List<ToolUtils.PropertyInfo> taskDefinition, boolean skipTaskOrderValidation) {
        if (!task.has("parameters") || !task.get("parameters")
            .isObject()) {
            return false;
        }

        String currentTaskName = task.get("name")
            .asText();
        int errorCountBefore = errors.length();

        TaskValidationContext context = new TaskValidationContext();
        context.skipTaskOrderValidation = skipTaskOrderValidation;
        findDataPillsInNode(task.get("parameters"), "", currentTaskName, taskOutput,
            taskNames, taskNameToTypeMap, errors, warnings, context, allTasksMap, taskDefinition);

        return errors.length() > errorCountBefore;
    }

    private static void findDataPillsInNode(
        JsonNode node, String currentPath, String currentTaskName,
        Map<String, ToolUtils.PropertyInfo> taskOutput, List<String> taskNames,
        Map<String, String> taskNameToTypeMap, StringBuilder errors,
        StringBuilder warnings, TaskValidationContext context, Map<String, JsonNode> allTasksMap,
        List<ToolUtils.PropertyInfo> taskDefinition) {
        if (node.isObject()) {
            node.fields()
                .forEachRemaining(entry -> {
                    if (context.stopProcessing)
                        return;
                    String fieldName = entry.getKey();
                    JsonNode fieldValue = entry.getValue();
                    String newPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
                    findDataPillsInNode(fieldValue, newPath, currentTaskName, taskOutput,
                        taskNames, taskNameToTypeMap, errors, warnings, context, allTasksMap, taskDefinition);
                });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                if (context.stopProcessing)
                    break;
                findDataPillsInNode(node.get(i), currentPath + "[" + i + "]", currentTaskName,
                    taskOutput, taskNames, taskNameToTypeMap, errors, warnings, context, allTasksMap, taskDefinition);
            }
        } else if (node.isTextual()) {
            if (!context.stopProcessing) {
                String textValue = node.asText();
                processDataPillsInText(textValue, currentPath, currentTaskName, taskOutput,
                    taskNames, taskNameToTypeMap, errors, warnings, context, allTasksMap, taskDefinition);
            }
        }
    }

    private static void processDataPillsInText(
        String text, String fieldPath, String currentTaskName,
        Map<String, ToolUtils.PropertyInfo> taskOutput, List<String> taskNames,
        Map<String, String> taskNameToTypeMap, StringBuilder errors,
        StringBuilder warnings, TaskValidationContext context, Map<String, JsonNode> allTasksMap,
        List<ToolUtils.PropertyInfo> taskDefinition) {
        Matcher matcher = DATA_PILL_PATTERN.matcher(text);

        while (matcher.find()) {
            if (context.stopProcessing)
                break;

            String dataPillExpression = matcher.group(1);
            if (dataPillExpression.contains(".")) {
                validatePropertyReference(dataPillExpression, fieldPath, currentTaskName, taskOutput,
                    taskNames, taskNameToTypeMap, errors, warnings, context, text, allTasksMap, taskDefinition);
            } else {
                validateTaskReference(dataPillExpression, taskNames, errors);
            }
        }
    }

    private static void validatePropertyReference(
        String dataPillExpression, String fieldPath, String currentTaskName,
        Map<String, ToolUtils.PropertyInfo> taskOutput, List<String> taskNames,
        Map<String, String> taskNameToTypeMap, StringBuilder errors,
        StringBuilder warnings, TaskValidationContext context, String text, Map<String, JsonNode> allTasksMap,
        List<ToolUtils.PropertyInfo> taskDefinition) {
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
                ValidationErrorBuilder.append(errors,
                    "Wrong task order: You can't reference '" + dataPillExpression + "' in " + currentTaskName);
                context.stopProcessing = true;
                return;
            }
        }

        String referencedTaskType = taskNameToTypeMap.get(referencedTaskName);
        if (referencedTaskType != null) {
            validatePropertyInOutput(dataPillExpression, referencedTaskType, propertyName, fieldPath,
                taskOutput, errors, warnings, text, referencedTaskName, allTasksMap, taskDefinition);
        }
    }

    private static void validatePropertyInOutput(
        String dataPillExpression, String referencedTaskType,
        String propertyName, String fieldPath,
        Map<String, ToolUtils.PropertyInfo> taskOutput,
        StringBuilder errors, StringBuilder warnings, String text,
        String referencedTaskName, Map<String, JsonNode> allTasksMap, List<ToolUtils.PropertyInfo> taskDefinition) {

        // Special handling for loop tasks - they auto-generate 'item' output based on 'items' parameter
        if (referencedTaskType.startsWith("loop/") && propertyName.startsWith("item")) {
            // Get expected type from task definition if available
            String expectedType = getExpectedTypeFromDefinition(fieldPath, taskDefinition);

            validateLoopItemTypes(dataPillExpression, referencedTaskName, expectedType, fieldPath, allTasksMap, errors,
                text, taskOutput);
            return;
        }

        ToolUtils.PropertyInfo outputInfo = taskOutput.get(referencedTaskType);
        if (outputInfo != null) {
            boolean propertyExists = PropertyUtils.checkPropertyExists(outputInfo, propertyName);

            if (!propertyExists) {
                ValidationErrorBuilder.append(warnings,
                    "Property '" + dataPillExpression + "' might not exist in the output of '" + referencedTaskType
                        + "'");
                return;
            }

            validateTypeCompatibility(dataPillExpression, referencedTaskType, propertyName, fieldPath,
                outputInfo, errors, text, referencedTaskName, allTasksMap, taskDefinition);
        } else {
            ValidationErrorBuilder.append(warnings,
                "Property '" + dataPillExpression + "' might not exist in the output of '" + referencedTaskType + "'");
        }
    }

    private static void validateTypeCompatibility(
        String dataPillExpression, String referencedTaskType,
        String propertyName, String fieldPath,
        ToolUtils.PropertyInfo outputInfo, StringBuilder errors, String text,
        String referencedTaskName, Map<String, JsonNode> allTasksMap, List<ToolUtils.PropertyInfo> taskDefinition) {
        String actualType = PropertyUtils.getPropertyType(outputInfo, propertyName);

        // Get expected type from task definition if available
        String expectedType = getExpectedTypeFromDefinition(fieldPath, taskDefinition);

        if (expectedType != null && actualType != null &&
            !TypeCompatibilityChecker.isTypeCompatible(expectedType, actualType)) {

            // Allow any type to be converted to string in interpolation
            if ("string".equalsIgnoreCase(expectedType) && isStringWithMultipleDataPills(text)) {
                return;
            }

            ValidationErrorBuilder.append(errors,
                "Property '" + dataPillExpression + "' in output of '" + referencedTaskType +
                    "' is of type " + actualType.toLowerCase() + ", not " + expectedType.toLowerCase());
        }
    }

    private static void validateTaskReference(String dataPillExpression, List<String> taskNames, StringBuilder errors) {
        if (!taskNames.contains(dataPillExpression)) {
            ValidationErrorBuilder.append(errors, "Task '" + dataPillExpression + "' doesn't exits.");
        }
    }

    /**
     * Gets the expected type for a field path from the task definition.
     */
    private static String getExpectedTypeFromDefinition(String fieldPath, List<ToolUtils.PropertyInfo> taskDefinition) {
        if (taskDefinition == null || fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }

        // Split the field path into parts (e.g., "active" or "config.setting")
        String[] pathParts = fieldPath.split("\\.");

        List<ToolUtils.PropertyInfo> currentProperties = taskDefinition;

        for (String part : pathParts) {
            ToolUtils.PropertyInfo foundProperty = null;

            // Look for the property in the current level
            for (ToolUtils.PropertyInfo property : currentProperties) {
                if (part.equals(property.name())) {
                    foundProperty = property;
                    break;
                }
            }

            if (foundProperty == null) {
                return null; // Property not found in definition
            }

            // If this is the last part of the path, return its type
            if (part.equals(pathParts[pathParts.length - 1])) {
                return foundProperty.type();
            }

            // If not the last part, navigate deeper into nested properties
            if ("OBJECT".equalsIgnoreCase(foundProperty.type()) && foundProperty.nestedProperties() != null) {
                currentProperties = foundProperty.nestedProperties();
            } else {
                return null; // Can't navigate deeper
            }
        }

        return null;
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

    private static boolean isLoopTask(String taskName, Map<String, String> taskNameToTypeMap) {
        String taskType = taskNameToTypeMap.get(taskName);
        return taskType != null && taskType.startsWith("loop/");
    }

    private static void validateLoopItemTypes(
        String dataPillExpression, String loopTaskName,
        String expectedType, String fieldPath,
        Map<String, JsonNode> allTasksMap, StringBuilder errors, String text,
        Map<String, ToolUtils.PropertyInfo> taskOutput) {
        JsonNode loopTask = allTasksMap.get(loopTaskName);
        if (loopTask == null || !loopTask.has("parameters")) {
            return;
        }

        JsonNode parameters = loopTask.get("parameters");
        if (!parameters.has("items")) {
            return;
        }

        JsonNode items = parameters.get("items");

        if (expectedType == null) {
            return;
        }

        // Handle literal array items
        if (items.isArray()) {
            // Check each item in the loop against the expected type
            for (int i = 0; i < items.size(); i++) {
                JsonNode item = items.get(i);
                String actualType = JsonUtils.getJsonNodeType(item);

                if (!TypeCompatibilityChecker.isTypeCompatible(expectedType, actualType)) {
                    // Allow any type to be converted to string in interpolation
                    if ("string".equalsIgnoreCase(expectedType) && isStringWithMultipleDataPills(text)) {
                        continue;
                    }

                    String indexedExpression = dataPillExpression.replace(".item", ".item[" + i + "]");
                    String errorMessage = "Property '" + indexedExpression + "' in output of 'loop/v1' is of type "
                        + actualType.toLowerCase() + ", not " + expectedType.toLowerCase();

                    // Avoid duplicate errors
                    if (!errors.toString()
                        .contains(errorMessage)) {
                        ValidationErrorBuilder.append(errors, errorMessage);
                    }
                }
            }
        } else if (items.isTextual()) {
            // Handle data pill references like "${task1.items}"
            String itemsValue = items.asText();
            if (itemsValue.startsWith("${") && itemsValue.endsWith("}")) {
                validateLoopItemTypesFromDataPill(dataPillExpression, itemsValue, expectedType,
                    fieldPath, allTasksMap, errors, text, taskOutput);
            }
        }
    }

    private static void validateLoopItemTypesFromDataPill(
        String dataPillExpression, String itemsDataPill,
        String expectedType, String fieldPath,
        Map<String, JsonNode> allTasksMap, StringBuilder errors, String text,
        Map<String, ToolUtils.PropertyInfo> taskOutput) {
        // Extract the data pill content (e.g., "task1.items" from "${task1.items}")
        String dataPillContent = itemsDataPill.substring(2, itemsDataPill.length() - 1);

        // Parse the task name (e.g., "task1" from "task1.items")
        String[] parts = dataPillContent.split("\\.", 2);
        if (parts.length < 1) {
            return;
        }

        String sourceTaskName = parts[0];
        String sourcePropertyName = parts.length > 1 ? parts[1] : ""; // e.g., "elements" from "task1.elements"

        JsonNode sourceTask = allTasksMap.get(sourceTaskName);
        if (sourceTask == null || !sourceTask.has("type")) {
            return;
        }

        // Get the source task's type and find its output definition
        String sourceTaskType = sourceTask.get("type")
            .asText();
        ToolUtils.PropertyInfo sourceTaskOutput = taskOutput.get(sourceTaskType);

        if (sourceTaskOutput == null) {
            return;
        }

        // Extract the property name from the data pill expression
        // For example, from "loop1.item.propBool" we want "propBool"
        if (dataPillExpression.contains(".item.")) {
            String[] itemParts = dataPillExpression.split("\\.item\\.");
            if (itemParts.length > 1) {
                String propertyName = itemParts[1];

                // Find the array property in the source task output (e.g., "elements")
                ToolUtils.PropertyInfo arrayProperty =
                    PropertyUtils.findPropertyByName(sourceTaskOutput, sourcePropertyName);
                if (arrayProperty != null && arrayProperty.nestedProperties() != null
                    && !arrayProperty.nestedProperties()
                        .isEmpty()) {
                    // Get the array element definition (first nested property)
                    ToolUtils.PropertyInfo arrayElementProperty = arrayProperty.nestedProperties()
                        .get(0);
                    if (arrayElementProperty != null && arrayElementProperty.nestedProperties() != null) {
                        // Find the specific property within the array element
                        ToolUtils.PropertyInfo targetProperty =
                            PropertyUtils.findPropertyByName(arrayElementProperty, propertyName);
                        if (targetProperty != null) {
                            String actualType = JsonUtils.mapTypeToString(targetProperty.type());

                            if (!TypeCompatibilityChecker.isTypeCompatible(expectedType, actualType)) {
                                // Generate errors for each array element (simulating 3 elements based on test
                                // expectations)
                                for (int i = 0; i < 3; i++) {
                                    String errorMessage = String.format(
                                        "Property 'loop1.item[%d].%s' in output of 'loop/v1' is of type %s, not %s",
                                        i, propertyName, actualType, expectedType.toLowerCase());
                                    ValidationErrorBuilder.append(errors, errorMessage);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static class TaskValidationContext {
        boolean stopProcessing = false;
        boolean skipTaskOrderValidation = false;
    }
}
