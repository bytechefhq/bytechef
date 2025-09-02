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
     * Validates data pills in a task's parameters, checking task order and property references.
     */
    public static boolean validateTaskDataPills(
        JsonNode task, Map<String, ToolUtils.PropertyInfo> taskOutput,
        List<String> taskNames, Map<String, String> taskNameToTypeMap,
        StringBuilder errors, StringBuilder warnings) {
        if (!task.has("parameters") || !task.get("parameters")
            .isObject()) {
            return false;
        }

        String currentTaskName = task.get("name")
            .asText();
        int errorCountBefore = errors.length();

        TaskValidationContext context = new TaskValidationContext();
        findDataPillsInNode(task.get("parameters"), "", currentTaskName, taskOutput,
            taskNames, taskNameToTypeMap, errors, warnings, context);

        return errors.length() > errorCountBefore;
    }

    private static void findDataPillsInNode(
        JsonNode node, String currentPath, String currentTaskName,
        Map<String, ToolUtils.PropertyInfo> taskOutput, List<String> taskNames,
        Map<String, String> taskNameToTypeMap, StringBuilder errors,
        StringBuilder warnings, TaskValidationContext context) {
        if (node.isObject()) {
            node.fields()
                .forEachRemaining(entry -> {
                    if (context.stopProcessing)
                        return;
                    String fieldName = entry.getKey();
                    JsonNode fieldValue = entry.getValue();
                    String newPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
                    findDataPillsInNode(fieldValue, newPath, currentTaskName, taskOutput,
                        taskNames, taskNameToTypeMap, errors, warnings, context);
                });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                if (context.stopProcessing)
                    break;
                findDataPillsInNode(node.get(i), currentPath + "[" + i + "]", currentTaskName,
                    taskOutput, taskNames, taskNameToTypeMap, errors, warnings, context);
            }
        } else if (node.isTextual()) {
            if (!context.stopProcessing) {
                String textValue = node.asText();
                processDataPillsInText(textValue, currentPath, currentTaskName, taskOutput,
                    taskNames, taskNameToTypeMap, errors, warnings, context);
            }
        }
    }

    private static void processDataPillsInText(
        String text, String fieldPath, String currentTaskName,
        Map<String, ToolUtils.PropertyInfo> taskOutput, List<String> taskNames,
        Map<String, String> taskNameToTypeMap, StringBuilder errors,
        StringBuilder warnings, TaskValidationContext context) {
        Matcher matcher = DATA_PILL_PATTERN.matcher(text);

        while (matcher.find()) {
            if (context.stopProcessing)
                break;

            String dataPillExpression = matcher.group(1);

            if (dataPillExpression.contains(".")) {
                validatePropertyReference(dataPillExpression, fieldPath, currentTaskName, taskOutput,
                    taskNames, taskNameToTypeMap, errors, warnings, context, text);
            } else {
                validateTaskReference(dataPillExpression, taskNames, errors);
            }
        }
    }

    private static void validatePropertyReference(
        String dataPillExpression, String fieldPath, String currentTaskName,
        Map<String, ToolUtils.PropertyInfo> taskOutput, List<String> taskNames,
        Map<String, String> taskNameToTypeMap, StringBuilder errors,
        StringBuilder warnings, TaskValidationContext context, String text) {
        String[] parts = dataPillExpression.split("\\.", 2);
        String referencedTaskName = parts[0];
        String propertyName = parts[1];

        // Check task order
        int currentTaskIndex = taskNames.indexOf(currentTaskName);
        int referencedTaskIndex = taskNames.indexOf(referencedTaskName);

        if (referencedTaskIndex == -1 || referencedTaskIndex >= currentTaskIndex) {
            ValidationErrorBuilder.append(errors,
                "Wrong task order: You can't reference '" + dataPillExpression + "' in " + currentTaskName + ".");
            context.stopProcessing = true;
            return;
        }

        String referencedTaskType = taskNameToTypeMap.get(referencedTaskName);
        if (referencedTaskType != null) {
            validatePropertyInOutput(dataPillExpression, referencedTaskType, propertyName, fieldPath,
                taskOutput, errors, warnings, text);
        }
    }

    private static void validatePropertyInOutput(
        String dataPillExpression, String referencedTaskType,
        String propertyName, String fieldPath,
        Map<String, ToolUtils.PropertyInfo> taskOutput,
        StringBuilder errors, StringBuilder warnings, String text) {
        ToolUtils.PropertyInfo outputInfo = taskOutput.get(referencedTaskType);
        if (outputInfo != null) {
            boolean propertyExists = PropertyNavigator.checkPropertyExists(outputInfo, propertyName);

            if (!propertyExists) {
                ValidationErrorBuilder.append(warnings,
                    "Property '" + dataPillExpression + "' might not exist in the output of '" + referencedTaskType
                        + "'");
                return;
            }

            validateTypeCompatibility(dataPillExpression, referencedTaskType, propertyName, fieldPath,
                outputInfo, errors, text);
        } else {
            ValidationErrorBuilder.append(warnings,
                "Property '" + dataPillExpression + "' might not exist in the output of '" + referencedTaskType + "'");
        }
    }

    private static void validateTypeCompatibility(
        String dataPillExpression, String referencedTaskType,
        String propertyName, String fieldPath,
        ToolUtils.PropertyInfo outputInfo, StringBuilder errors, String text) {
        String expectedType = TypeCompatibilityChecker.getExpectedTypeFromFieldPath(fieldPath);
        String actualType = PropertyNavigator.getPropertyType(outputInfo, propertyName);

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

    static class TaskValidationContext {
        boolean stopProcessing = false;
    }
}
