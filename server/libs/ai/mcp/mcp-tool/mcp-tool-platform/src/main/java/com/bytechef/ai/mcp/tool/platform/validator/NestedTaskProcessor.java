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

package com.bytechef.ai.mcp.tool.platform.validator;

import com.bytechef.ai.mcp.tool.platform.util.ToolUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Strategy pattern implementation for processing different types of nested tasks. Handles the complexity of nested task
 * validation with different strategies based on task types and structures.
 */
public class NestedTaskProcessor {

    /**
     * Main entry point for nested task processing. Uses strategy pattern to handle different nested task scenarios.
     */
    public void processNestedTaskValidation(JsonNode task, ValidationContext context) {
        if (!task.has("parameters")) {
            return;
        }

        JsonNode parameters = task.get("parameters");
        String taskType = task.get("type")
            .asText();

        if (!isLoopTaskType(taskType)) {
            return;
        }

        List<ToolUtils.PropertyInfo> taskDefinition = context.getTaskDefinitions()
            .get(taskType);
        if (taskDefinition != null) {
            findAndValidateNestedTasks(parameters, taskDefinition, context);
        }
    }

    /**
     * Strategy to determine if a task type supports nested tasks.
     */
    private boolean isLoopTaskType(String taskType) {
        return taskType.matches("^\\w+/\\w+$");
    }

    /**
     * Finds and validates nested tasks within task parameters. Uses Chain of Responsibility pattern to process
     * different property types.
     */
    private void findAndValidateNestedTasks(
        JsonNode parameters,
        List<ToolUtils.PropertyInfo> taskDefinition,
        ValidationContext context) {

        for (ToolUtils.PropertyInfo propertyInfo : taskDefinition) {
            processTaskArrayProperty(parameters, propertyInfo, context);
        }
    }

    /**
     * Processes a single property that might contain nested tasks. Part of Chain of Responsibility pattern.
     */
    private void processTaskArrayProperty(
        JsonNode parameters,
        ToolUtils.PropertyInfo propertyInfo,
        ValidationContext context) {

        String propertyName = propertyInfo.name();

        // Check if this is a TASK type array
        if (!isTaskTypeArray(propertyInfo)) {
            return;
        }

        if (!parameters.has(propertyName) || !parameters.get(propertyName)
            .isArray()) {
            return;
        }

        JsonNode taskArray = parameters.get(propertyName);
        processNestedTaskArray(taskArray, context);
    }

    /**
     * Strategy to identify TASK type arrays.
     */
    private boolean isTaskTypeArray(ToolUtils.PropertyInfo propertyInfo) {
        return "ARRAY".equalsIgnoreCase(propertyInfo.type()) &&
            propertyInfo.nestedProperties() != null &&
            propertyInfo.nestedProperties()
                .size() == 1
            &&
            "TASK".equalsIgnoreCase(propertyInfo.nestedProperties()
                .get(0)
                .type());
    }

    /**
     * Processes an array of nested tasks. Template method for nested task processing.
     */
    private void processNestedTaskArray(JsonNode taskArray, ValidationContext context) {
        for (int i = 0; i < taskArray.size(); i++) {
            JsonNode nestedTask = taskArray.get(i);
            if (nestedTask.has("type")) {
                processIndividualNestedTask(nestedTask, context);
            }
        }
    }

    /**
     * Processes a single nested task. Template method defining the nested task validation steps.
     */
    private void processIndividualNestedTask(JsonNode nestedTask, ValidationContext context) {
        addNestedTaskToContext(nestedTask, context);

        validateNestedTaskStructure(nestedTask, context);

        validateNestedTaskParameters(nestedTask, context);

        validateNestedTaskDataPills(nestedTask, context);

        processNestedTaskValidation(nestedTask, context);
    }

    /**
     * Adds nested task to validation context for proper tracking.
     */
    private void addNestedTaskToContext(JsonNode nestedTask, ValidationContext context) {
        if (nestedTask.has("name")) {
            String nestedTaskName = nestedTask.get("name")
                .asText();
            String nestedTaskType = nestedTask.get("type")
                .asText();

            if (!context.getTaskNames()
                .contains(nestedTaskName)) {
                context.getTaskNames()
                    .add(nestedTaskName);
            }
            context.getAllTasksMap()
                .put(nestedTaskName, nestedTask);
            context.getTaskNameToTypeMap()
                .put(nestedTaskName, nestedTaskType);
        }
    }

    /**
     * Validates the structure of a nested task.
     */
    private void validateNestedTaskStructure(JsonNode nestedTask, ValidationContext context) {
        WorkflowValidator.validateTaskStructure(nestedTask.toString(), context.getErrors());
    }

    /**
     * Validates parameters of a nested task.
     */
    private void validateNestedTaskParameters(JsonNode nestedTask, ValidationContext context) {
        String nestedTaskType = nestedTask.get("type")
            .asText();
        List<ToolUtils.PropertyInfo> nestedTaskDefinition = context.getTaskDefinitions()
            .get(nestedTaskType);

        if (nestedTaskDefinition != null) {
            String nestedTaskParameters = "{}";
            if (nestedTask.has("parameters") && nestedTask.get("parameters")
                .isObject()) {
                nestedTaskParameters = nestedTask.get("parameters")
                    .toString();
            }
            WorkflowValidator.validateTaskParameters(nestedTaskParameters, nestedTaskDefinition,
                context.getErrors(), context.getWarnings());
        }
    }

    /**
     * Validates data pills in nested task parameters.
     */
    private void validateNestedTaskDataPills(JsonNode nestedTask, ValidationContext context) {
        String nestedTaskType = nestedTask.get("type")
            .asText();
        List<ToolUtils.PropertyInfo> nestedTaskDefinition = context.getTaskDefinitions()
            .get(nestedTaskType);

        // Skip task order validation for nested tasks
        WorkflowValidator.validateTaskDataPills(nestedTask, context.getTaskOutputs(),
            context.getTaskNames(), context.getTaskNameToTypeMap(),
            context.getErrors(), context.getWarnings(), context.getAllTasksMap(),
            nestedTaskDefinition, true);
    }
}
