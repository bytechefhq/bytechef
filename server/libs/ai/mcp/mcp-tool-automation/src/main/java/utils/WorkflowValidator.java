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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Provides high-level validation functionality for workflows, tasks, and task parameters. This class orchestrates
 * validation using specialized utility classes for different concerns.
 *
 * Key responsibilities: - Workflow structure validation (triggers, tasks, metadata) - Task structure validation (type,
 * parameters, labels) - Task parameter validation with PropertyInfo support - Coordination of data pill validation
 * through DataPillValidator
 *
 * @see DataPillValidator for data pill expression validation
 * @see FieldValidator for basic field validation
 * @see PropertyValidator for property-specific validation
 * @see JsonProcessingHelper for JSON processing utilities
 */
@Component
public class WorkflowValidator {

    // ==================== WORKFLOW VALIDATION ====================

    /**
     * Validates the overall structure of a workflow JSON.
     *
     * @param workflow the workflow JSON string to validate
     * @param errors   StringBuilder to collect validation errors
     * @return the errors StringBuilder for method chaining
     */
    public static StringBuilder validateWorkflowStructure(String workflow, StringBuilder errors) {
        JsonNode workflowNode = JsonProcessingHelper.parseJsonWithErrorHandling(workflow, errors);
        if (workflowNode == null) {
            return errors;
        }

        if (!JsonProcessingHelper.validateNodeIsObject(workflowNode, "Workflow", errors)) {
            return errors;
        }

        // Validate required workflow fields
        FieldValidator.validateRequiredStringField(workflowNode, "label", errors);
        FieldValidator.validateRequiredStringField(workflowNode, "description", errors);
        FieldValidator.validateWorkflowTriggers(workflowNode, errors);
        FieldValidator.validateRequiredArrayField(workflowNode, "tasks", errors);

        return errors;
    }

    /**
     * Validates all tasks in a workflow, including their structure, parameters, and data pill references.
     *
     * @param tasks           list of task JsonNodes to validate
     * @param taskDefinitions map of task types to their definitions
     * @param taskOutput      map of task types to their output PropertyInfo
     * @param errors          StringBuilder to collect validation errors
     * @param warnings        StringBuilder to collect validation warnings
     */
    public static void validateWorkflowTasks(
        List<JsonNode> tasks, Map<String, String> taskDefinitions,
        Map<String, ToolUtils.PropertyInfo> taskOutput,
        StringBuilder errors, StringBuilder warnings) {
        List<String> taskNames = new ArrayList<>();
        Map<String, String> taskNameToTypeMap = new java.util.HashMap<>();

        for (JsonNode task : tasks) {
            if (processTaskValidation(task, taskDefinitions, taskNames, taskNameToTypeMap, taskOutput, errors,
                warnings)) {
                break; // Stop if task order errors occurred
            }
        }
    }

    // ==================== TASK VALIDATION ====================

    /**
     * Validates the structure of a single task JSON.
     *
     * @param task   the task JSON string to validate
     * @param errors StringBuilder to collect validation errors
     * @return the errors StringBuilder for method chaining
     */
    public static StringBuilder validateTaskStructure(String task, StringBuilder errors) {
        JsonNode taskNode = JsonProcessingHelper.parseJsonWithErrorHandling(task, errors);
        if (taskNode == null) {
            return errors;
        }

        if (!JsonProcessingHelper.validateNodeIsObject(taskNode, "Task", errors)) {
            return errors;
        }

        // Validate required task fields
        FieldValidator.validateRequiredStringField(taskNode, "label", errors);
        FieldValidator.validateRequiredStringField(taskNode, "name", errors);
        FieldValidator.validateTaskType(taskNode, errors);
        FieldValidator.validateRequiredObjectField(taskNode, "parameters", errors);

        return errors;
    }

    // ==================== TASK PARAMETER VALIDATION ====================

    /**
     * Validates task parameters against a string-based task definition.
     *
     * @param currentTaskParameters the current task parameters JSON
     * @param taskDefinition        the task definition JSON
     * @param errors                StringBuilder to collect validation errors
     * @param warnings              StringBuilder to collect validation warnings
     * @return the errors StringBuilder for method chaining
     */
    public static StringBuilder validateTaskParameters(
        String currentTaskParameters, String taskDefinition,
        StringBuilder errors, StringBuilder warnings) {
        JsonNode currentPropsNode = JsonProcessingHelper.parseJsonWithErrorHandling(currentTaskParameters, errors);
        if (currentPropsNode == null
            || !JsonProcessingHelper.validateNodeIsObject(currentPropsNode, "Current task parameters", errors)) {
            return errors;
        }

        JsonNode taskDefNode = JsonProcessingHelper.parseJsonWithErrorHandling(taskDefinition, errors);
        if (taskDefNode == null || !JsonProcessingHelper.validateNodeIsObject(taskDefNode, "Task definition", errors)) {
            return errors;
        }

        JsonNode parametersNode = taskDefNode.get("parameters");
        if (parametersNode == null || !parametersNode.isObject()) {
            errors.append("Task definition must have a 'parameters' object");
            return errors;
        }

        PropertyValidator.validatePropertiesRecursively(currentPropsNode, parametersNode, "",
            errors, warnings, taskDefinition, currentTaskParameters);
        return errors;
    }

    /**
     * Validates task parameters against a single PropertyInfo task definition.
     *
     * @param currentTaskParameters the current task parameters JSON
     * @param taskDefinition        the PropertyInfo representing the task definition
     * @param errors                StringBuilder to collect validation errors
     * @param warnings              StringBuilder to collect validation warnings
     * @return the errors StringBuilder for method chaining
     */
    public static StringBuilder validateTaskParameters(
        String currentTaskParameters, ToolUtils.PropertyInfo taskDefinition,
        StringBuilder errors, StringBuilder warnings) {
        if (taskDefinition == null) {
            errors.append("Task definition must not be null");
            return errors;
        }

        if (!"OBJECT".equalsIgnoreCase(taskDefinition.type())) {
            errors.append("Task definition must be an object");
            return errors;
        }

        List<ToolUtils.PropertyInfo> taskDefinitionList = List.of(taskDefinition);
        return validateTaskParameters(currentTaskParameters, taskDefinitionList, errors, warnings);
    }

    /**
     * Validates task parameters against a list of PropertyInfo definitions with display condition processing.
     *
     * @param currentTaskParameters the current task parameters JSON
     * @param taskDefinition        list of PropertyInfo representing the task definition
     * @param errors                StringBuilder to collect validation errors
     * @param warnings              StringBuilder to collect validation warnings
     * @return the errors StringBuilder for method chaining
     */
    public static StringBuilder validateTaskParameters(
        String currentTaskParameters, List<ToolUtils.PropertyInfo> taskDefinition,
        StringBuilder errors, StringBuilder warnings) {
        JsonNode currentPropsNode = JsonProcessingHelper.parseJsonWithErrorHandling(currentTaskParameters, errors);
        if (currentPropsNode == null
            || !JsonProcessingHelper.validateNodeIsObject(currentPropsNode, "Current task parameters", errors)) {
            return errors;
        }

        try {
            String processedTaskDefinition =
                WorkflowParser.processDisplayConditions(taskDefinition, currentTaskParameters);
            return validateProcessedTaskDefinition(currentPropsNode, processedTaskDefinition, errors, warnings,
                currentTaskParameters);

        } catch (RuntimeException e) {
            return handleDisplayConditionError(e, taskDefinition, currentTaskParameters, errors, warnings);
        }
    }

    // ==================== DATA PILL VALIDATION ====================

    /**
     * Validates data pill expressions in task parameters. Delegates to DataPillValidator for the actual validation
     * logic.
     *
     * @param task              the task JsonNode containing parameters to validate
     * @param taskOutput        map of task types to their output PropertyInfo
     * @param taskNames         list of task names in workflow order
     * @param taskNameToTypeMap mapping from task names to task types
     * @param errors            StringBuilder to collect validation errors
     * @param warnings          StringBuilder to collect validation warnings
     * @return true if task order errors were found (indicating processing should stop)
     */
    public static boolean validateTaskDataPills(
        JsonNode task, Map<String, ToolUtils.PropertyInfo> taskOutput,
        List<String> taskNames, Map<String, String> taskNameToTypeMap,
        StringBuilder errors, StringBuilder warnings) {
        return DataPillValidator.validateTaskDataPills(task, taskOutput, taskNames, taskNameToTypeMap, errors,
            warnings);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Handles JsonProcessingException with context-aware error messages. Delegates to JsonProcessingHelper for
     * standardized error handling.
     */
    public static void handleJsonProcessingException(JsonProcessingException e, String workflow, StringBuilder errors) {
        JsonProcessingHelper.handleJsonProcessingException(e, workflow, errors);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private static StringBuilder validateProcessedTaskDefinition(
        JsonNode currentPropsNode, String processedTaskDefinition,
        StringBuilder errors, StringBuilder warnings, String currentTaskParameters) {
        try {
            JsonNode taskDefNode = WorkflowParser.parseJsonString(processedTaskDefinition);
            if (!JsonProcessingHelper.validateNodeIsObject(taskDefNode, "Task definition", errors)) {
                return errors;
            }

            JsonNode parametersNode = taskDefNode.get("parameters");
            if (parametersNode == null || !parametersNode.isObject()) {
                errors.append("Task definition must have a 'parameters' object");
                return errors;
            }

            PropertyValidator.validatePropertiesRecursively(currentPropsNode, parametersNode, "",
                errors, warnings, processedTaskDefinition, currentTaskParameters);
            return errors;
        } catch (JsonProcessingException e) {
            JsonProcessingHelper.handleJsonProcessingException(e, processedTaskDefinition, errors);
            return errors;
        }
    }

    private static StringBuilder handleDisplayConditionError(
        RuntimeException e, List<ToolUtils.PropertyInfo> taskDefinition,
        String currentTaskParameters, StringBuilder errors, StringBuilder warnings) {
        if (e.getMessage() != null && e.getMessage()
            .startsWith("Invalid logic for display condition:")) {
            try {
                String cleanedTaskDefinition = JsonProcessingHelper.removeObjectsWithInvalidConditions(
                    WorkflowParser.convertPropertyInfoToJson(taskDefinition));

                JsonNode taskDefNode = WorkflowParser.parseJsonString(cleanedTaskDefinition);
                JsonNode parametersNode = taskDefNode.get("parameters");

                if (parametersNode != null && parametersNode.isObject()) {
                    JsonNode currentPropsNode = WorkflowParser.parseJsonString(currentTaskParameters);
                    PropertyValidator.validatePropertiesRecursively(currentPropsNode, parametersNode, "",
                        errors, warnings, cleanedTaskDefinition, currentTaskParameters);
                }
                ValidationErrorBuilder.append(warnings, e.getMessage());
            } catch (Exception ignored) {
                ValidationErrorBuilder.append(warnings, e.getMessage());
            }
        } else {
            throw e;
        }
        return errors;
    }

    private static boolean processTaskValidation(
        JsonNode task, Map<String, String> taskDefinitions,
        List<String> taskNames, Map<String, String> taskNameToTypeMap,
        Map<String, ToolUtils.PropertyInfo> taskOutput,
        StringBuilder errors, StringBuilder warnings) {
        String taskType = task.get("type")
            .asText();
        String taskDefinition = taskDefinitions.get(taskType);

        validateTaskStructure(task.toString(), errors);

        String taskParameters = "{}";
        if (task.has("parameters") && task.get("parameters")
            .isObject()) {
            taskParameters = task.get("parameters")
                .toString();
        }
        validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        String taskName = task.get("name")
            .asText();
        taskNames.add(taskName);
        taskNameToTypeMap.put(taskName, taskType);

        return validateTaskDataPills(task, taskOutput, taskNames, taskNameToTypeMap, errors, warnings);
    }
}
