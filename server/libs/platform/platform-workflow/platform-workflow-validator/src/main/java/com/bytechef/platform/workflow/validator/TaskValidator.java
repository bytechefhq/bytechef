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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

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
            validateTaskStructure(taskJsonNode.toString(), context.getErrors());

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
    public static void validateTaskStructure(String taskJson, StringBuilder errors) {
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
        validateRequiredObjectField(taskJsonNode, errors);
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

                StringUtils.appendWithNewline(ValidationErrorUtils.typeError(path, "object", actualType), errors);
            }
            else {
                validateTaskStructure(taskJsonNode.toString(), errors);

                // If task has parameters, validate them recursively if we have the task type
                if (taskJsonNode.has("parameters") && taskJsonNode.has("type")) {
                    JsonNode parametersJsonNode = taskJsonNode.get("parameters");

                    // Basic parameter structure validation
                    if (!parametersJsonNode.isObject()) {
                        String actualType = JsonUtils.getJsonNodeType(parametersJsonNode);

                        StringUtils.appendWithNewline(
                            ValidationErrorUtils.typeError(path + ".parameters", "object", actualType), errors);
                    }
                }
            }
        }
    }

    /**
     * Validates task parameters against a list of PropertyInfo definitions with display condition processing.
     *
     * @param taskParameters the task parameters JSON
     * @param taskDefinition list of PropertyInfo representing the task definition
     * @param errors         StringBuilder to collect validation errors
     * @param warnings       StringBuilder to collect validation warnings
     */
    public static void validateTaskParameters(
        String taskParameters, List<PropertyInfo> taskDefinition, StringBuilder errors, StringBuilder warnings) {

        JsonNode taskParametersJsonNode = JsonUtils.parseJsonWithErrorHandling(taskParameters, errors);

        if (!JsonUtils.validateNodeIsObject(taskParametersJsonNode, "Current task parameters", errors)) {
            return;
        }

        PropertyValidator.validatePropertiesFromPropertyInfo(
            taskParametersJsonNode, taskDefinition, "", taskParameters, errors, warnings);
    }

    /**
     * Adds nested task to validation context for proper tracking.
     */
    private static void addNestedTaskToContext(JsonNode nestedTaskJsonNode, ValidationContext context) {
        if (nestedTaskJsonNode.has("name")) {
            JsonNode nameJsonNode = nestedTaskJsonNode.get("name");

            String name = nameJsonNode.asText();

            JsonNode typeJsonNode = nestedTaskJsonNode.get("type");

            String type = typeJsonNode.asText();

            List<String> taskNames = context.getTaskNames();

            if (!taskNames.contains(name)) {
                taskNames.add(name);
            }

            Map<String, JsonNode> allTasksMap = context.getAllTasksMap();

            allTasksMap.put(name, nestedTaskJsonNode);

            Map<String, String> taskNameToTypeMap = context.getTaskNameToTypeMap();

            taskNameToTypeMap.put(name, type);
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
        validateTaskStructure(nestedTask, context);
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

    private static void validateDataPills(
        JsonNode task, @Nullable List<PropertyInfo> taskDefinition, ValidationContext context) {

        if (taskDefinition != null && !taskDefinition.isEmpty()) {
            DataPillValidator.validateTaskDataPills(
                task, context.getTaskOutputs(), context.getTaskNames(), context.getTaskNameToTypeMap(),
                context.getErrors(), context.getWarnings(), context.getAllTasksMap(), taskDefinition, false);
        }
    }

    /**
     * Validates the structure of a nested task.
     */
    private static void validateTaskStructure(JsonNode nestedTaskJsonNode, ValidationContext context) {
        validateTaskStructure(nestedTaskJsonNode.toString(), context.getErrors());
    }

    /**
     * Validates parameters of a nested task.
     */
    private static void validateNestedTaskParameters(JsonNode taskJsonNode, ValidationContext context) {
        JsonNode typeJsonNode = taskJsonNode.get("type");

        String type = typeJsonNode.asText();

        Map<String, List<PropertyInfo>> taskDefinitionsMap = context.getTaskDefinitions();

        List<PropertyInfo> nestedTaskDefinition = taskDefinitionsMap.get(type);

        if (nestedTaskDefinition != null) {
            String nestedTaskParameters = "{}";
            JsonNode parametersJsonNode = taskJsonNode.get("parameters");

            if (parametersJsonNode != null && parametersJsonNode.isObject()) {
                nestedTaskParameters = parametersJsonNode.toString();
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

        String type = typeJsonNode.asText();

        Map<String, List<PropertyInfo>> taskDefinitionsMap = context.getTaskDefinitions();

        List<PropertyInfo> nestedTaskDefinition = taskDefinitionsMap.get(type);

        DataPillValidator.validateTaskDataPills(
            nestedTaskJsonNode, context.getTaskOutputs(), context.getTaskNames(), context.getTaskNameToTypeMap(),
            context.getErrors(), context.getWarnings(), context.getAllTasksMap(), nestedTaskDefinition, true);
    }

    /**
     * Validates that a required object field exists and is of correct type.
     */
    private static void
        validateRequiredObjectField(JsonNode jsonNode, StringBuilder errors) {
        if (!jsonNode.has("parameters")) {
            StringUtils.appendWithNewline("Missing required field: " + "parameters", errors);
        } else {
            JsonNode fieldJsonNode = jsonNode.get("parameters");

            if (!fieldJsonNode.isObject()) {
                StringUtils.appendWithNewline(
                    "Field '" + "parameters" + "' must be an object", errors);
            }
        }
    }

    @Nullable
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

            validateTaskParameters(taskParameters, taskDefinition, context.getErrors(), context.getWarnings());
        }

        return taskDefinition;
    }

    /**
     * Validates task type field against required pattern.
     */
    private static void validateTaskTypeField(JsonNode taskJsonNode, StringBuilder errors) {
        if (!taskJsonNode.has("type")) {
            StringUtils.appendWithNewline("Missing required field: type", errors);
        } else {
            JsonNode typeJsonNode = taskJsonNode.get("type");

            if (!typeJsonNode.isTextual()) {
                StringUtils.appendWithNewline("Field 'type' must be a string", errors);
            } else {
                String typeValue = typeJsonNode.asText();

                Matcher matcher = TYPE_PATTERN.matcher(typeValue);

                if (!matcher.matches()) {
                    StringUtils.appendWithNewline(
                        "Field 'type' must match pattern: (alphanumeric)+/v(numeric)+(/(alphanumeric)+)?", errors);
                }
            }
        }
    }
}
