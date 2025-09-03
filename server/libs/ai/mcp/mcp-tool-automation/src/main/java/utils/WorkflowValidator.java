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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        List<JsonNode> tasks, Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions,
        Map<String, ToolUtils.PropertyInfo> taskOutput,
        StringBuilder errors, StringBuilder warnings) {
        List<String> taskNames = new ArrayList<>();
        Map<String, String> taskNameToTypeMap = new java.util.HashMap<>();

        for (JsonNode task : tasks) {
            if (processTaskValidation(task, taskDefinitions, taskNames, taskNameToTypeMap, taskOutput, errors, warnings)) {
                break; // Stop if task order errors occurred
            }
        }
    }

    /**
     * Validates the structure of a single task JSON.
     *
     * @param task the task JSON string to validate
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
            String originalTaskDefinition = WorkflowParser.convertPropertyInfoToJson(taskDefinition);
            String processedTaskDefinition =
                WorkflowParser.processDisplayConditions(taskDefinition, currentTaskParameters);
            return validateProcessedTaskDefinitionWithArraySupport(currentPropsNode, processedTaskDefinition, originalTaskDefinition, errors, warnings,
                currentTaskParameters);

        } catch (RuntimeException e) {
            return handleDisplayConditionError(e, taskDefinition, currentTaskParameters, errors, warnings);
        }
    }

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

    private static StringBuilder validateProcessedTaskDefinitionWithArraySupport(
        JsonNode currentPropsNode, String processedTaskDefinition, String originalTaskDefinition,
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

            // For array validation with display conditions, use original definition
            PropertyValidator.validatePropertiesRecursivelyWithArraySupport(currentPropsNode, parametersNode, "",
                errors, warnings, processedTaskDefinition, originalTaskDefinition, currentTaskParameters);
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
        JsonNode task, Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions,
        List<String> taskNames, Map<String, String> taskNameToTypeMap,
        Map<String, ToolUtils.PropertyInfo> taskOutput,
        StringBuilder errors, StringBuilder warnings) {
        String taskType = task.get("type")
            .asText();
        List<ToolUtils.PropertyInfo> taskDefinition = taskDefinitions.get(taskType);

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

    /**
     * Validates a complete workflow including structure, tasks, and parameters.
     *
     * @param workflow the workflow JSON string to validate
     * @param taskDefinitionProvider function to get task definitions for a given task type and kind
     * @param taskOutputProvider function to get task output properties for a given task type and kind
     * @param errors StringBuilder to collect validation errors
     * @param warnings StringBuilder to collect validation warnings
     */
    public static void validateCompleteWorkflow(
            String workflow,
            TaskDefinitionProvider taskDefinitionProvider,
            TaskOutputProvider taskOutputProvider,
            StringBuilder errors,
            StringBuilder warnings) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // First validate the basic workflow structure
            validateWorkflowStructure(workflow, errors);

            // Extract task properties from the provided workflow JSON
            JsonNode workflowNode = objectMapper.readTree(workflow);

            java.util.List<JsonNode> tasks = new java.util.ArrayList<>();
            java.util.Map<String, java.util.List<ToolUtils.PropertyInfo>> taskDefinitions = new java.util.HashMap<>();
            java.util.Map<String, ToolUtils.PropertyInfo> taskOutputs = new java.util.HashMap<>();

            // Process triggers
            if (workflowNode.has("triggers") && workflowNode.get("triggers").isArray()) {
                workflowNode.get("triggers").elements().forEachRemaining(task -> {
                    if (tasks.isEmpty()) {
                        tasks.add(task);
                        String taskType = task.get("type").asText();
                        taskDefinitions.putIfAbsent(taskType, taskDefinitionProvider.getTaskProperties(taskType, "trigger"));
                        taskOutputs.putIfAbsent(taskType, taskOutputProvider.getTaskOutputProperty(taskType, "trigger", warnings));
                    } else {
                        errors.append("There can only be one trigger in the workflow");
                    }
                });
            }

            // Process tasks
            if (workflowNode.has("tasks") && workflowNode.get("tasks").isArray()) {
                workflowNode.get("tasks").elements().forEachRemaining(task -> {
                    if (tasks.stream().anyMatch(previousTask -> previousTask.get("name").equals(task.get("name")))) {
                        errors.append("Tasks cannot have repeating names: ").append(task.get("name").asText());
                    }
                    tasks.add(task);
                    String taskType = task.get("type").asText();
                    taskDefinitions.putIfAbsent(taskType, taskDefinitionProvider.getTaskProperties(taskType, ""));
                    taskOutputs.putIfAbsent(taskType, taskOutputProvider.getTaskOutputProperty(taskType, "", warnings));

                    // Handle nested TASK type properties by recursively processing them
                    processNestedTasks(task, taskDefinitions, taskDefinitions, taskOutputs, errors, warnings);
                });
            }

            validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

        } catch (Exception e) {
            errors.append("Failed to validate workflow: ").append(e.getMessage());
        }
    }

    /**
     * Validates a single task including structure and parameters.
     *
     * @param task the task JSON string to validate
     * @param taskDefinitionProvider function to get task definitions for a given task type and kind
     * @param errors StringBuilder to collect validation errors
     * @param warnings StringBuilder to collect validation warnings
     */
    public static void validateSingleTask(
            String task,
            TaskDefinitionProvider taskDefinitionProvider,
            StringBuilder errors,
            StringBuilder warnings) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // First validate the basic task structure
            validateTaskStructure(task, errors);

            // Extract task properties from the provided task JSON
            JsonNode taskNode = objectMapper.readTree(task);
            String taskType = taskNode.get("type").asText();

            // Get the task definition for property validation
            java.util.List<ToolUtils.PropertyInfo> taskDefinition =
                taskDefinitionProvider.getTaskProperties(taskType, "");

            String taskParameters = "{}";
            if (taskNode.has("parameters") && taskNode.get("parameters").isObject()) {
                taskParameters = objectMapper.writeValueAsString(taskNode.get("parameters"));
            }

            // Validate task properties against the definition
            validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        } catch (Exception e) {
            errors.append("Failed to validate task: ").append(e.getMessage());
        }
    }

    /**
     * Processes nested TASK type properties within a task, extracting and validating inner tasks.
     */
    private static void processNestedTasks(
            JsonNode task,
            java.util.Map<String, java.util.List<ToolUtils.PropertyInfo>> mainTaskDefinitions,
            java.util.Map<String, java.util.List<ToolUtils.PropertyInfo>> allTaskDefinitions,
            java.util.Map<String, ToolUtils.PropertyInfo> taskOutputs,
            StringBuilder errors,
            StringBuilder warnings) {

        if (!task.has("parameters")) {
            return;
        }

        JsonNode parameters = task.get("parameters");
        String taskType = task.get("type").asText();
        java.util.List<ToolUtils.PropertyInfo> taskDef = mainTaskDefinitions.get(taskType);

        if (taskDef != null) {
            extractNestedTasksFromParameters(parameters, taskDef, allTaskDefinitions, taskOutputs, errors, warnings);
        }
    }

    /**
     * Recursively extracts and processes nested tasks from parameters that have TASK type properties.
     */
    private static void extractNestedTasksFromParameters(
            JsonNode parameters,
            java.util.List<ToolUtils.PropertyInfo> taskDefinition,
            java.util.Map<String, java.util.List<ToolUtils.PropertyInfo>> allTaskDefinitions,
            java.util.Map<String, ToolUtils.PropertyInfo> taskOutputs,
            StringBuilder errors,
            StringBuilder warnings) {

        for (ToolUtils.PropertyInfo propertyInfo : taskDefinition) {
            String propertyName = propertyInfo.name();

            // Check if this is a TASK type array
            if ("ARRAY".equalsIgnoreCase(propertyInfo.type()) &&
                propertyInfo.nestedProperties() != null &&
                propertyInfo.nestedProperties().size() == 1 &&
                "TASK".equalsIgnoreCase(propertyInfo.nestedProperties().get(0).type())) {

                if (parameters.has(propertyName) && parameters.get(propertyName).isArray()) {
                    JsonNode taskArray = parameters.get(propertyName);

                    // Process each nested task in the array
                    for (int i = 0; i < taskArray.size(); i++) {
                        JsonNode nestedTask = taskArray.get(i);

                        if (nestedTask.has("type")) {
                            String nestedTaskType = nestedTask.get("type").asText();

                            // Add task definition for the nested task if not already present
                            if (!allTaskDefinitions.containsKey(nestedTaskType)) {
                                // This would need to be provided by the caller - for now we'll skip detailed validation
                                // allTaskDefinitions.put(nestedTaskType, getTaskDefinition(nestedTaskType));
                            }

                            // Validate the nested task structure
                            validateTaskStructure(nestedTask.toString(), errors);

                            // Recursively process nested tasks within this task
                            if (nestedTask.has("parameters")) {
                                java.util.List<ToolUtils.PropertyInfo> nestedTaskDef =
                                    allTaskDefinitions.get(nestedTaskType);
                                if (nestedTaskDef != null) {
                                    extractNestedTasksFromParameters(nestedTask.get("parameters"), nestedTaskDef,
                                        allTaskDefinitions, taskOutputs, errors, warnings);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Functional interface for providing task definitions.
     */
    @FunctionalInterface
    public interface TaskDefinitionProvider {
        java.util.List<ToolUtils.PropertyInfo> getTaskProperties(String taskType, String kind);
    }

    /**
     * Functional interface for providing task output properties.
     */
    @FunctionalInterface
    public interface TaskOutputProvider {
        ToolUtils.PropertyInfo getTaskOutputProperty(String taskType, String kind, StringBuilder warnings);
    }
}
