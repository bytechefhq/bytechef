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

package com.bytechef.ai.mcp.tool.validator;

import com.bytechef.ai.mcp.tool.model.PropertyInfo;
import com.bytechef.ai.mcp.tool.util.JsonUtils;
import com.bytechef.ai.mcp.tool.util.ValidationErrorUtils;
import com.bytechef.ai.mcp.tool.util.WorkflowUtils;
import com.bytechef.commons.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template Method pattern for task validation. Defines the skeleton of a task validation algorithm while letting
 * subclasses override specific steps of the algorithm without changing its structure.
 *
 * @author Marko Kriskovic
 */
class TaskValidator {

    private static final Pattern TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9]+/v[0-9]+(/[a-zA-Z0-9]+)?$");

    private TaskValidator() {
    }

    /**
     * Template method defining the validation algorithm for all tasks. Works directly with the legacy StringBuilder
     * approach for full backward compatibility.
     */
    public static void validateAllTasks(ValidationContext context) {
        for (JsonNode taskJsonNode : context.getTasks()) {
            validateTask(taskJsonNode.toString(), context.getErrors());

            List<PropertyInfo> taskDefinition = validateTaskParameters(taskJsonNode, context);

            processNestedTaskValidation(taskJsonNode, context);
            validateDataPills(taskJsonNode, taskDefinition, context);
        }
    }

    /**
     * Validates the structure of a single task JSON.
     *
     * @param taskJson the task JSON string to validate
     * @param errors   StringBuilder to collect validation errors
     */
    public static void validateTask(String taskJson, StringBuilder errors) {
        JsonNode taskJsonNode = JsonUtils.parseJsonWithErrorHandling(taskJson, errors);

        if (taskJsonNode == null) {
            return;
        }

        if (!JsonUtils.validateNodeIsObject(taskJsonNode, "Task", errors)) {
            return;
        }

        // Validate required task fields
        FieldValidator.validateRequiredStringField(taskJsonNode, "label", errors);
        FieldValidator.validateRequiredStringField(taskJsonNode, "name", errors);
        validateTaskTypeField(taskJsonNode, errors);
        validateRequiredObjectField(taskJsonNode, "parameters", errors);

    }

    /**
     * Validates an array containing TASK objects.
     */
    public static void validateTaskArray(JsonNode arrayValueJsonNode, String propertyPath, StringBuilder errors) {
        for (int i = 0; i < arrayValueJsonNode.size(); i++) {
            JsonNode taskJsonNode = arrayValueJsonNode.get(i);
            String path = propertyPath + "[" + i + "]";

            if (!taskJsonNode.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(taskJsonNode);

                StringUtils.appendWithNewline(errors, ValidationErrorUtils.typeError(path, "object", actualType));

                continue;
            }

            validateTask(taskJsonNode.toString(), errors);

            // If task has parameters, validate them recursively if we have the task type
            if (taskJsonNode.has("parameters") && taskJsonNode.has("type")) {
                JsonNode parametersJsonNode = taskJsonNode.get("parameters");

                // Basic parameter structure validation
                if (!parametersJsonNode.isObject()) {
                    String actualType = JsonUtils.getJsonNodeType(parametersJsonNode);

                    StringUtils.appendWithNewline(
                        errors, ValidationErrorUtils.typeError(path + ".parameters", "object", actualType));
                }
            }
        }
    }

    /**
     * Validates task parameters against a list of PropertyInfo definitions with display condition processing.
     *
     * @param currentTaskParameters the current task parameters JSON
     * @param taskDefinition        list of PropertyInfo representing the task definition
     * @param errors                StringBuilder to collect validation errors
     * @param warnings              StringBuilder to collect validation warnings
     */
    public static void validateTaskParameters(
        String currentTaskParameters, List<PropertyInfo> taskDefinition,
        StringBuilder errors, StringBuilder warnings) {
        JsonNode currentPropsNode = JsonUtils.parseJsonWithErrorHandling(currentTaskParameters, errors);

        if (!JsonUtils.validateNodeIsObject(currentPropsNode, "Current task parameters", errors)) {
            return;
        }

        try {
            String originalTaskDefinition = WorkflowUtils.convertPropertyInfoToJson(taskDefinition);
            String processedTaskDefinition =
                WorkflowUtils.processDisplayConditions(taskDefinition, currentTaskParameters);
            validateProcessedTaskDefinition(
                currentPropsNode, processedTaskDefinition, originalTaskDefinition, errors, warnings,
                currentTaskParameters);

        } catch (RuntimeException e) {
            handleDisplayConditionError(e, taskDefinition, currentTaskParameters, errors, warnings);
        }
    }

    /**
     * Adds nested task to validation context for proper tracking.
     */
    private static void addNestedTaskToContext(JsonNode nestedTaskJsonNode, ValidationContext context) {
        if (nestedTaskJsonNode.has("name")) {
            JsonNode nameJsonNode = nestedTaskJsonNode.get("name");

            String nestedTaskName = nameJsonNode.asText();

            JsonNode typeJsonNode = nestedTaskJsonNode.get("type");

            String nestedTaskType = typeJsonNode.asText();

            List<String> taskNames = context.getTaskNames();

            if (!taskNames.contains(nestedTaskName)) {
                taskNames.add(nestedTaskName);
            }

            Map<String, JsonNode> allTasksMap = context.getAllTasksMap();

            allTasksMap.put(nestedTaskName, nestedTaskJsonNode);

            Map<String, String> taskNameToTypeMap = context.getTaskNameToTypeMap();

            taskNameToTypeMap.put(nestedTaskName, nestedTaskType);
        }
    }

    /**
     * Finds and validates nested tasks within task parameters. Uses Chain of Responsibility pattern to process
     * different property types.
     */
    private static void findAndValidateNestedTasks(
        JsonNode parametersJsonNode, List<PropertyInfo> taskDefinition, ValidationContext context) {

        for (PropertyInfo propertyInfo : taskDefinition) {
            processTaskArrayProperty(parametersJsonNode, propertyInfo, context);
        }
    }

    private static void handleDisplayConditionError(
        RuntimeException exception, List<PropertyInfo> taskDefinition, String currentTaskParameters,
        StringBuilder errors, StringBuilder warnings) {

        String message = exception.getMessage();

        if (message != null && message.startsWith("Invalid logic for display condition:")) {
            try {
                String cleanedTaskDefinition = removeObjectsWithInvalidConditions(
                    WorkflowUtils.convertPropertyInfoToJson(taskDefinition));

                JsonNode taskDefinitionJsonNode = com.bytechef.commons.util.JsonUtils.readTree(cleanedTaskDefinition);
                JsonNode parametersJsonNode = taskDefinitionJsonNode.get("parameters");

                if (parametersJsonNode != null && parametersJsonNode.isObject()) {
                    JsonNode currentTaskParametersJsonNode = com.bytechef.commons.util.JsonUtils.readTree(
                        currentTaskParameters);

                    PropertyValidator.validatePropertiesRecursively(
                        currentTaskParametersJsonNode, parametersJsonNode, "", errors, warnings, cleanedTaskDefinition,
                        currentTaskParameters);
                }

                StringUtils.appendWithNewline(warnings, message);
            } catch (Exception ignored) {
                StringUtils.appendWithNewline(warnings, message);
            }
        } else {
            throw exception;
        }
    }

    /**
     * Handles JsonProcessingException with context-aware error messages.
     */
    private static void handleJsonProcessingException(
        JsonProcessingException exception, String json, StringBuilder errors) {

        if (exception.getMessage() != null && json.contains("\"type\":") && json.contains("triggers")) {
            errors.append("Trigger must be an object\n");
        } else {
            errors.append("Invalid JSON format: ")
                .append(exception.getMessage())
                .append("\n");
        }
    }

    /**
     * Strategy to determine if a task type supports nested tasks.
     */
    private static boolean isLoopTaskType(String taskType) {
        return taskType.matches("^\\w+/\\w+$");
    }

    /**
     * Strategy to identify TASK type arrays.
     */
    private static boolean isTaskTypeArray(PropertyInfo propertyInfo) {
        List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

        if (!"ARRAY".equalsIgnoreCase(propertyInfo.type()) || propertyInfos == null || propertyInfos.size() != 1) {
            return false;
        }

        PropertyInfo propertyInfosFirst = propertyInfos.getFirst();

        return "TASK".equalsIgnoreCase(propertyInfosFirst.type());
    }

    /**
     * Processes a single nested task. Template method defining the nested task validation steps.
     */
    private static void processIndividualNestedTask(JsonNode nestedTask, ValidationContext context) {
        addNestedTaskToContext(nestedTask, context);
        validateNestedTaskStructure(nestedTask, context);
        validateNestedTaskParameters(nestedTask, context);
        validateNestedTaskDataPills(nestedTask, context);
        processNestedTaskValidation(nestedTask, context);
    }

    /**
     * Processes an array of nested tasks. Template method for nested task processing.
     */
    private static void processNestedTaskArray(JsonNode taskArrayJsonNode, ValidationContext context) {
        for (int i = 0; i < taskArrayJsonNode.size(); i++) {
            JsonNode nestedTaskJsonNode = taskArrayJsonNode.get(i);

            if (nestedTaskJsonNode.has("type")) {
                processIndividualNestedTask(nestedTaskJsonNode, context);
            }
        }
    }

    /**
     * Main entry point for nested task processing. Uses the strategy pattern to handle different nested task scenarios.
     */
    private static void processNestedTaskValidation(JsonNode task, ValidationContext context) {
        if (!task.has("parameters")) {
            return;
        }

        JsonNode parametersJsonNode = task.get("parameters");

        String taskType = task.get("type")
            .asText();

        if (!isLoopTaskType(taskType)) {
            return;
        }

        Map<String, List<PropertyInfo>> taskDefinitionsMap = context.getTaskDefinitions();

        List<PropertyInfo> taskDefinition = taskDefinitionsMap.get(taskType);

        if (taskDefinition != null) {
            findAndValidateNestedTasks(parametersJsonNode, taskDefinition, context);
        }
    }

    /**
     * Processes a single property that might contain nested tasks. Part of Chain of Responsibility pattern.
     */
    private static void processTaskArrayProperty(
        JsonNode parametersJsonNode, PropertyInfo propertyInfo, ValidationContext context) {

        String propertyName = propertyInfo.name();

        // Check if this is a TASK type array
        if (!isTaskTypeArray(propertyInfo)) {
            return;
        }

        JsonNode jsonNode = parametersJsonNode.get(propertyName);

        if (jsonNode == null || !jsonNode.isArray()) {
            return;
        }

        processNestedTaskArray(jsonNode, context);
    }

    /**
     * Removes objects with invalid display conditions from JSON.
     */
    private static String removeObjectsWithInvalidConditions(String json) {
        try {
            String result = json;

            // Remove objects that have metadata (display conditions)
            result = result.replaceAll("\"[^\"]+\"\\s*:\\s*\\{[^{}]*\"metadata\"\\s*:\\s*\"[^\"]*\"[^{}]*}", "");

            // Clean up any resulting JSON syntax issues
            result = JsonUtils.cleanupJsonSyntax(result);

            return result;
        } catch (Exception e) {
            return json;
        }
    }

    private static void validateDataPills(
        JsonNode task, List<PropertyInfo> taskDefinition, ValidationContext context) {

        if (taskDefinition != null && !taskDefinition.isEmpty()) {
            DataPillValidator.validateTaskDataPills(
                task, context.getTaskOutputs(), context.getTaskNames(), context.getTaskNameToTypeMap(),
                context.getErrors(), context.getWarnings(), context.getAllTasksMap(), taskDefinition, false, true);
        }
    }

    /**
     * Validates the structure of a nested task.
     */
    private static void validateNestedTaskStructure(JsonNode nestedTask, ValidationContext context) {
        validateTask(nestedTask.toString(), context.getErrors());
    }

    /**
     * Validates parameters of a nested task.
     */
    private static void validateNestedTaskParameters(JsonNode nestedTaskJsonNode, ValidationContext context) {
        JsonNode typeJsonNode = nestedTaskJsonNode.get("type");

        String nestedTaskType = typeJsonNode.asText();

        Map<String, List<PropertyInfo>> taskDefinitionsMap = context.getTaskDefinitions();

        List<PropertyInfo> nestedTaskDefinition = taskDefinitionsMap.get(nestedTaskType);

        if (nestedTaskDefinition != null) {
            String nestedTaskParameters = "{}";
            JsonNode jsonNode = nestedTaskJsonNode.get("parameters");

            if (jsonNode != null && jsonNode.isObject()) {
                nestedTaskParameters = jsonNode.toString();
            }

            validateTaskParameters(
                nestedTaskParameters, nestedTaskDefinition, context.getErrors(), context.getWarnings());
        }
    }

    /**
     * Validates data pills in nested task parameters.
     */
    private static void validateNestedTaskDataPills(JsonNode nestedTaskJsonNode, ValidationContext context) {
        JsonNode typeJsonNode = nestedTaskJsonNode.get("type");

        String nestedTaskType = typeJsonNode.asText();

        Map<String, List<PropertyInfo>> taskDefinitionsMap = context.getTaskDefinitions();

        List<PropertyInfo> nestedTaskDefinition = taskDefinitionsMap.get(nestedTaskType);

        // Skip task order validation for nested tasks
        DataPillValidator.validateTaskDataPills(
            nestedTaskJsonNode, context.getTaskOutputs(), context.getTaskNames(), context.getTaskNameToTypeMap(),
            context.getErrors(), context.getWarnings(), context.getAllTasksMap(), nestedTaskDefinition, true);
    }

    private static void validateProcessedTaskDefinition(
        JsonNode currentPropsNode, String processedTaskDefinition, String originalTaskDefinition,
        StringBuilder errors, StringBuilder warnings, String currentTaskParameters) {
        try {
            JsonNode taskDefNode = com.bytechef.commons.util.JsonUtils.readTree(processedTaskDefinition);
            if (!JsonUtils.validateNodeIsObject(taskDefNode, "Task definition", errors)) {
                return;
            }

            JsonNode parametersNode = taskDefNode.get("parameters");
            if (parametersNode == null || !parametersNode.isObject()) {
                errors.append("Task definition must have a 'parameters' object");
                return;
            }

            // For array validation with display conditions, use original definition
            PropertyValidator.validatePropertiesRecursively(currentPropsNode, parametersNode, "",
                errors, warnings, processedTaskDefinition, originalTaskDefinition, currentTaskParameters);
        } catch (Exception e) {
            handleJsonProcessingException(
                (JsonProcessingException) e.getCause(), processedTaskDefinition, errors);

        }
    }

    /**
     * Validates that a required object field exists and is of correct type.
     */
    private static void validateRequiredObjectField(JsonNode jsonNode, String fieldName, StringBuilder errors) {
        if (!jsonNode.has(fieldName)) {
            StringUtils.appendWithNewline(errors, "Missing required field: " + fieldName);
        } else {
            JsonNode fieldJsonNode = jsonNode.get(fieldName);

            if (!fieldJsonNode.isObject()) {
                StringUtils.appendWithNewline(
                    errors, "Field '" + fieldName + "' must be an object");
            }
        }
    }

    private static List<PropertyInfo> validateTaskParameters(JsonNode taskJsonNode, ValidationContext context) {
        JsonNode typeJsonNode = taskJsonNode.get("type");

        String taskType = typeJsonNode.asText();

        Map<String, List<PropertyInfo>> taskDefinitionsMap = context.getTaskDefinitions();

        List<PropertyInfo> taskDefinition = taskDefinitionsMap.get(taskType);

        if (taskDefinition != null && !taskDefinition.isEmpty()) {
            String taskParameters = "{}";
            JsonNode jsonNode = taskJsonNode.get("parameters");

            if (jsonNode != null && jsonNode.isObject()) {
                taskParameters = jsonNode.toString();
            }

            validateTaskParameters(
                taskParameters, taskDefinition, context.getErrors(), context.getWarnings());
        }
        return taskDefinition;
    }

    /**
     * Validates task type field against required pattern.
     */
    private static void validateTaskTypeField(JsonNode taskJsonNode, StringBuilder errors) {
        if (!taskJsonNode.has("type")) {
            StringUtils.appendWithNewline(errors, "Missing required field: type");
        } else {
            JsonNode typeJsonNode = taskJsonNode.get("type");

            if (!typeJsonNode.isTextual()) {
                StringUtils.appendWithNewline(errors, "Field 'type' must be a string");
            } else {
                String typeValue = typeJsonNode.asText();

                Matcher matcher = TYPE_PATTERN.matcher(typeValue);

                if (!matcher.matches()) {
                    StringUtils.appendWithNewline(
                        errors,
                        "Field 'type' must match pattern: (alphanumeric)+/v(numeric)+(/(alphanumeric)+)?");
                }
            }
        }
    }
}
