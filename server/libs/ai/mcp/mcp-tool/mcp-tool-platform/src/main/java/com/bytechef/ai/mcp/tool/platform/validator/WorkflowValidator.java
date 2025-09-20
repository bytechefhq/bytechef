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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WorkflowValidator {

    private final WorkflowStructureValidator structureValidator;
    private final TaskValidator taskValidator;

    public WorkflowValidator() {
        this.structureValidator = new WorkflowStructureValidator();
        this.taskValidator = new TaskValidator();
    }

    /**
     * Validates the overall structure of a workflow JSON. Enhanced with design patterns for improved maintainability.
     * Now uses enhanced pattern-based implementation internally.
     *
     * @param workflow the workflow JSON string to validate
     * @param errors   StringBuilder to collect validation errors
     * @return the errors StringBuilder for method chaining
     */
    public static StringBuilder validateWorkflowStructure(String workflow, StringBuilder errors) {
        WorkflowValidator validator = new WorkflowValidator();

        try {
            return validator.structureValidator.validateStructure(workflow, errors);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Validates all tasks in a workflow, including their structure, parameters, and data pill references. Enhanced with
     * Command Pattern and Observer Pattern for better architecture.
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

        // Use the existing Template Method Pattern implementation to maintain exact backward compatibility
        ValidationContext context = ValidationContext.builder()
            .withTasks(tasks)
            .withTaskDefinitions(taskDefinitions)
            .withTaskOutputs(taskOutput)
            .withErrors(errors)
            .withWarnings(warnings)
            .build();

        WorkflowValidator validator = new WorkflowValidator();

        try {
            validator.taskValidator.validateAllTasks(context);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Validates the structure of a single task JSON. Enhanced with Observer Pattern for validation event tracking. Now
     * uses enhanced pattern-based implementation internally.
     *
     * @param task   the task JSON string to validate
     * @param errors StringBuilder to collect validation errors
     * @return the errors StringBuilder for method chaining
     */
    public static StringBuilder validateTaskStructure(String task, StringBuilder errors) {
        try {
            return new TaskStructureValidator().validate(task, errors);
        } catch (Exception e) {
            throw e;
        }
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
        JsonNode currentPropsNode = JsonUtils.parseJsonWithErrorHandling(currentTaskParameters, errors);
        if (currentPropsNode == null
            || !JsonUtils.validateNodeIsObject(currentPropsNode, "Current task parameters", errors)) {
            return errors;
        }

        try {
            String originalTaskDefinition = WorkflowParser.convertPropertyInfoToJson(taskDefinition);
            String processedTaskDefinition =
                WorkflowParser.processDisplayConditions(taskDefinition, currentTaskParameters);
            return validateProcessedTaskDefinition(currentPropsNode, processedTaskDefinition, originalTaskDefinition,
                errors, warnings,
                currentTaskParameters);

        } catch (RuntimeException e) {
            return handleDisplayConditionError(e, taskDefinition, currentTaskParameters, errors, warnings);
        }
    }

    /**
     * Validates data pill expressions in task parameters with access to all tasks for loop type validation, task
     * definition for type checking, and optional task order validation skipping.
     *
     * @param task                    the task JsonNode containing parameters to validate
     * @param taskOutput              map of task types to their output PropertyInfo
     * @param taskNames               list of task names in workflow order
     * @param taskNameToTypeMap       mapping from task names to task types
     * @param errors                  StringBuilder to collect validation errors
     * @param warnings                StringBuilder to collect validation warnings
     * @param allTasksMap             map of all task names to their JsonNode for loop validation
     * @param taskDefinition          list of PropertyInfo representing the task definition for type checking
     * @param skipTaskOrderValidation whether to skip task order validation (useful for nested tasks)
     * @return true if task order errors were found (indicating processing should stop)
     */
    public static boolean validateTaskDataPills(
        JsonNode task, Map<String, ToolUtils.PropertyInfo> taskOutput,
        List<String> taskNames, Map<String, String> taskNameToTypeMap,
        StringBuilder errors, StringBuilder warnings, Map<String, JsonNode> allTasksMap,
        List<ToolUtils.PropertyInfo> taskDefinition, boolean skipTaskOrderValidation) {
        return DataPillValidator.validateTaskDataPills(task, taskOutput, taskNames, taskNameToTypeMap, errors,
            warnings, allTasksMap, taskDefinition, skipTaskOrderValidation);
    }

    /**
     * Validates data pill expressions in task parameters with access to all tasks for loop type validation, task
     * definition for type checking, optional task order validation skipping, and optional nested task validation
     * skipping.
     *
     * @param task                     the task JsonNode containing parameters to validate
     * @param taskOutput               map of task types to their output PropertyInfo
     * @param taskNames                list of task names in workflow order
     * @param taskNameToTypeMap        mapping from task names to task types
     * @param errors                   StringBuilder to collect validation errors
     * @param warnings                 StringBuilder to collect validation warnings
     * @param allTasksMap              map of all task names to their JsonNode for loop validation
     * @param taskDefinition           list of PropertyInfo representing the task definition for type checking
     * @param skipTaskOrderValidation  whether to skip task order validation (useful for nested tasks)
     * @param skipNestedTaskValidation whether to skip validation of nested TASK type arrays
     * @return true if task order errors were found (indicating processing should stop)
     */
    public static boolean validateTaskDataPills(
        JsonNode task, Map<String, ToolUtils.PropertyInfo> taskOutput,
        List<String> taskNames, Map<String, String> taskNameToTypeMap,
        StringBuilder errors, StringBuilder warnings, Map<String, JsonNode> allTasksMap,
        List<ToolUtils.PropertyInfo> taskDefinition, boolean skipTaskOrderValidation,
        boolean skipNestedTaskValidation) {
        return DataPillValidator.validateTaskDataPills(task, taskOutput, taskNames, taskNameToTypeMap, errors,
            warnings, allTasksMap, taskDefinition, skipTaskOrderValidation, skipNestedTaskValidation);
    }

    private static StringBuilder validateProcessedTaskDefinition(
        JsonNode currentPropsNode, String processedTaskDefinition, String originalTaskDefinition,
        StringBuilder errors, StringBuilder warnings, String currentTaskParameters) {
        try {
            JsonNode taskDefNode = WorkflowParser.parseJsonString(processedTaskDefinition);
            if (!JsonUtils.validateNodeIsObject(taskDefNode, "Task definition", errors)) {
                return errors;
            }

            JsonNode parametersNode = taskDefNode.get("parameters");
            if (parametersNode == null || !parametersNode.isObject()) {
                errors.append("Task definition must have a 'parameters' object");
                return errors;
            }

            // For array validation with display conditions, use original definition
            PropertyValidator.validatePropertiesRecursively(currentPropsNode, parametersNode, "",
                errors, warnings, processedTaskDefinition, originalTaskDefinition, currentTaskParameters);
            return errors;
        } catch (JsonProcessingException e) {
            JsonUtils.handleJsonProcessingException(e, processedTaskDefinition, errors);
            return errors;
        }
    }

    private static StringBuilder handleDisplayConditionError(
        RuntimeException e, List<ToolUtils.PropertyInfo> taskDefinition,
        String currentTaskParameters, StringBuilder errors, StringBuilder warnings) {
        if (e.getMessage() != null && e.getMessage()
            .startsWith("Invalid logic for display condition:")) {
            try {
                String cleanedTaskDefinition = JsonUtils.removeObjectsWithInvalidConditions(
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

    /**
     * Validates a complete workflow including structure, tasks, and parameters.
     *
     * @param workflow               the workflow JSON string to validate
     * @param taskDefinitionProvider function to get task definitions for a given task type and kind
     * @param taskOutputProvider     function to get task output properties for a given task type and kind
     * @param errors                 StringBuilder to collect validation errors
     * @param warnings               StringBuilder to collect validation warnings
     */
    public static void validateCompleteWorkflow(
        String workflow,
        TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider,
        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions,
        Map<String, ToolUtils.PropertyInfo> taskOutputs,
        StringBuilder errors,
        StringBuilder warnings) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // First validate the basic workflow structure
            validateWorkflowStructure(workflow, errors);

            // Extract task properties from the provided workflow JSON
            JsonNode workflowNode = objectMapper.readTree(workflow);

            List<JsonNode> tasks = new ArrayList<>();

            // Process triggers
            if (workflowNode.has("triggers") && workflowNode.get("triggers")
                .isArray()) {
                workflowNode.get("triggers")
                    .elements()
                    .forEachRemaining(task -> {
                        if (tasks.isEmpty()) {
                            tasks.add(task);
                            String taskType = task.get("type")
                                .asText();
                            taskDefinitions.putIfAbsent(taskType,
                                taskDefinitionProvider.getTaskProperties(taskType, "trigger"));
                            taskOutputs.putIfAbsent(taskType,
                                taskOutputProvider.getTaskOutputProperty(taskType, "trigger", warnings));
                        } else {
                            errors.append("There can only be one trigger in the workflow");
                        }
                    });
            }

            // Process tasks
            if (workflowNode.has("tasks") && workflowNode.get("tasks")
                .isArray()) {
                workflowNode.get("tasks")
                    .elements()
                    .forEachRemaining(task -> {
                        if (tasks.stream()
                            .anyMatch(previousTask -> previousTask.get("name")
                                .equals(task.get("name")))) {
                            errors.append("Tasks cannot have repeating names: ")
                                .append(task.get("name")
                                    .asText());
                        }
                        tasks.add(task);
                        String taskType = task.get("type")
                            .asText();
                        taskDefinitions.putIfAbsent(taskType, taskDefinitionProvider.getTaskProperties(taskType, ""));
                        taskOutputs.putIfAbsent(taskType,
                            taskOutputProvider.getTaskOutputProperty(taskType, "", warnings));

                        // Handle nested TASK type properties by recursively processing them
                        processNestedTasks(task, taskDefinitions, taskDefinitions, taskOutputs,
                            tasks, taskDefinitionProvider, taskOutputProvider, errors, warnings);
                    });
            }

            validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

        } catch (Exception e) {
            errors.append("Failed to validate workflow: ")
                .append(e.getMessage());
        }
    }

    /**
     * Validates a single task including structure and parameters.
     *
     * @param task                   the task JSON string to validate
     * @param taskDefinitionProvider function to get task definitions for a given task type and kind
     * @param errors                 StringBuilder to collect validation errors
     * @param warnings               StringBuilder to collect validation warnings
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
            String taskType = taskNode.get("type")
                .asText();

            // Get the task definition for property validation
            List<ToolUtils.PropertyInfo> taskDefinition =
                taskDefinitionProvider.getTaskProperties(taskType, "");

            String taskParameters = "{}";
            if (taskNode.has("parameters") && taskNode.get("parameters")
                .isObject()) {
                taskParameters = objectMapper.writeValueAsString(taskNode.get("parameters"));
            }

            // Validate task properties against the definition
            validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        } catch (Exception e) {
            errors.append("Failed to validate task: ")
                .append(e.getMessage());
        }
    }

    /**
     * Processes nested TASK type properties within a task, extracting and validating inner tasks.
     */
    private static void processNestedTasks(
        JsonNode task,
        Map<String, List<ToolUtils.PropertyInfo>> mainTaskDefinitions,
        Map<String, List<ToolUtils.PropertyInfo>> allTaskDefinitions,
        Map<String, ToolUtils.PropertyInfo> taskOutputs,
        List<JsonNode> allTasks,
        TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider,
        StringBuilder errors,
        StringBuilder warnings) {

        if (!task.has("parameters")) {
            return;
        }

        JsonNode parameters = task.get("parameters");
        String taskType = task.get("type")
            .asText();
        List<ToolUtils.PropertyInfo> taskDef = mainTaskDefinitions.get(taskType);

        if (taskDef != null && !taskDef.isEmpty()) {
            extractNestedTasksFromParameters(parameters, taskDef, allTaskDefinitions, taskOutputs,
                allTasks, taskDefinitionProvider, taskOutputProvider, errors, warnings);
        } else {
            // If no task definition is available, look for common nested task patterns directly
            discoverNestedTasksFromJsonStructure(parameters, allTaskDefinitions, taskOutputs,
                allTasks, taskDefinitionProvider, taskOutputProvider, errors, warnings);
        }
    }

    /**
     * Discovers nested tasks by looking for common patterns in JSON structure when task definitions are not available.
     */
    private static void discoverNestedTasksFromJsonStructure(
        JsonNode parameters,
        Map<String, List<ToolUtils.PropertyInfo>> allTaskDefinitions,
        Map<String, ToolUtils.PropertyInfo> taskOutputs,
        List<JsonNode> allTasks,
        TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider,
        StringBuilder errors,
        StringBuilder warnings) {

        // Common nested task property names in different task types
        String[] nestedTaskProperties = {
            "caseTrue", "caseFalse", "iteratee", "tasks"
        };

        for (String propertyName : nestedTaskProperties) {
            if (parameters.has(propertyName)) {
                JsonNode propertyValue = parameters.get(propertyName);

                if (propertyValue.isArray()) {
                    // Process each task in the array
                    for (int i = 0; i < propertyValue.size(); i++) {
                        JsonNode nestedTask = propertyValue.get(i);

                        if (nestedTask.isObject() && nestedTask.has("type")) {
                            String nestedTaskType = nestedTask.get("type")
                                .asText();

                            // Add the nested task to the main tasks list for validation
                            allTasks.add(nestedTask);

                            // Add task definition for the nested task if not already present
                            if (!allTaskDefinitions.containsKey(nestedTaskType)) {
                                List<ToolUtils.PropertyInfo> nestedTaskDefinition =
                                    taskDefinitionProvider.getTaskProperties(nestedTaskType, "");
                                allTaskDefinitions.put(nestedTaskType,
                                    nestedTaskDefinition != null ? nestedTaskDefinition : List.of());
                            }

                            // Add task output for the nested task if not already present
                            if (!taskOutputs.containsKey(nestedTaskType)) {
                                ToolUtils.PropertyInfo nestedTaskOutput =
                                    taskOutputProvider.getTaskOutputProperty(nestedTaskType, "", warnings);
                                taskOutputs.put(nestedTaskType, nestedTaskOutput);
                            }

                            // Validate the nested task structure
                            validateTaskStructure(nestedTask.toString(), errors);

                            // Recursively process nested tasks within this task
                            if (nestedTask.has("parameters")) {
                                List<ToolUtils.PropertyInfo> nestedTaskDef = allTaskDefinitions.get(nestedTaskType);
                                if (nestedTaskDef != null && !nestedTaskDef.isEmpty()) {
                                    extractNestedTasksFromParameters(nestedTask.get("parameters"), nestedTaskDef,
                                        allTaskDefinitions, taskOutputs, allTasks, taskDefinitionProvider,
                                        taskOutputProvider, errors, warnings);
                                } else {
                                    // Recursively discover more nested tasks
                                    discoverNestedTasksFromJsonStructure(nestedTask.get("parameters"),
                                        allTaskDefinitions, taskOutputs,
                                        allTasks, taskDefinitionProvider, taskOutputProvider, errors, warnings);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Recursively extracts and processes nested tasks from parameters that have TASK type properties.
     */
    private static void extractNestedTasksFromParameters(
        JsonNode parameters,
        List<ToolUtils.PropertyInfo> taskDefinition,
        Map<String, List<ToolUtils.PropertyInfo>> allTaskDefinitions,
        Map<String, ToolUtils.PropertyInfo> taskOutputs,
        List<JsonNode> allTasks,
        TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider,
        StringBuilder errors,
        StringBuilder warnings) {

        for (ToolUtils.PropertyInfo propertyInfo : taskDefinition) {
            String propertyName = propertyInfo.name();

            // Check if this is a TASK type array
            if ("ARRAY".equalsIgnoreCase(propertyInfo.type()) &&
                propertyInfo.nestedProperties() != null &&
                propertyInfo.nestedProperties()
                    .size() == 1
                &&
                "TASK".equalsIgnoreCase(propertyInfo.nestedProperties()
                    .get(0)
                    .type())
                &&
                parameters.has(propertyName) && parameters.get(propertyName)
                    .isArray()) {
                JsonNode taskArray = parameters.get(propertyName);

                // Process each nested task in the array
                for (int i = 0; i < taskArray.size(); i++) {
                    JsonNode nestedTask = taskArray.get(i);

                    if (nestedTask.has("type")) {
                        String nestedTaskType = nestedTask.get("type")
                            .asText();

                        // Add the nested task to the main tasks list for validation
                        allTasks.add(nestedTask);

                        // Add task definition for the nested task if not already present
                        if (!allTaskDefinitions.containsKey(nestedTaskType)) {
                            List<ToolUtils.PropertyInfo> nestedTaskProperties =
                                taskDefinitionProvider.getTaskProperties(nestedTaskType, "");
                            // Always add the nested task type to the map, even if the provider returns null or empty
                            allTaskDefinitions.put(nestedTaskType,
                                nestedTaskProperties != null ? nestedTaskProperties : List.of());
                        }

                        // Add task output for the nested task if not already present
                        if (!taskOutputs.containsKey(nestedTaskType)) {
                            ToolUtils.PropertyInfo nestedTaskOutput =
                                taskOutputProvider.getTaskOutputProperty(nestedTaskType, "", warnings);
                            // Always add the nested task type to the map, even if the provider returns null
                            taskOutputs.put(nestedTaskType, nestedTaskOutput);
                        }

                        // Validate the nested task structure
                        validateTaskStructure(nestedTask.toString(), errors);

                        // Recursively process nested tasks within this task
                        if (nestedTask.has("parameters")) {
                            List<ToolUtils.PropertyInfo> nestedTaskDef =
                                allTaskDefinitions.get(nestedTaskType);
                            if (nestedTaskDef != null) {
                                extractNestedTasksFromParameters(nestedTask.get("parameters"), nestedTaskDef,
                                    allTaskDefinitions, taskOutputs, allTasks, taskDefinitionProvider,
                                    taskOutputProvider, errors, warnings);
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
        List<ToolUtils.PropertyInfo> getTaskProperties(String taskType, String kind);
    }

    /**
     * Functional interface for providing task output properties.
     */
    @FunctionalInterface
    public interface TaskOutputProvider {
        ToolUtils.PropertyInfo getTaskOutputProperty(String taskType, String kind, StringBuilder warnings);
    }
}
