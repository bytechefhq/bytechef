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
import com.bytechef.commons.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 */
@Component
public class WorkflowValidator {

    /**
     * Validates all tasks in a workflow, including their structure, parameters, and data pill references. Enhanced with
     * Command Pattern and Observer Pattern for better architecture.
     *
     * @param taskJsonNodes     list of task JsonNodes to validate
     * @param taskDefinitionMap map of task types to their definitions
     * @param taskOutput        map of task types to their output PropertyInfo
     * @param errors            StringBuilder to collect validation errors
     * @param warnings          StringBuilder to collect validation warnings
     */
    public static void validateWorkflowTasks(
        List<JsonNode> taskJsonNodes, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutput, StringBuilder errors, StringBuilder warnings) {

        // Use the existing Template Method Pattern implementation to maintain exact backward compatibility
        ValidationContext context = ValidationContext.builder()
            .withTasks(taskJsonNodes)
            .withTaskDefinitionMap(taskDefinitionMap)
            .withTaskOutputMap(taskOutput)
            .withErrors(errors)
            .withWarnings(warnings)
            .build();

        TaskValidator.validateAllTasks(context);
    }

    /**
     * Validates task parameters against a single PropertyInfo task definition.
     *
     * @param currentTaskParameters      the current task parameters JSON
     * @param taskDefinitionPropertyInfo the PropertyInfo representing the task definition
     * @param errors                     StringBuilder to collect validation errors
     * @param warnings                   StringBuilder to collect validation warnings
     */
    public static void validateTaskParameters(
        String currentTaskParameters, PropertyInfo taskDefinitionPropertyInfo, StringBuilder errors,
        StringBuilder warnings) {

        if (taskDefinitionPropertyInfo == null) {
            errors.append("Task definition must not be null");

            return;
        }

        if (!"OBJECT".equalsIgnoreCase(taskDefinitionPropertyInfo.type())) {
            errors.append("Task definition must be an object");

            return;
        }

        List<PropertyInfo> taskDefinitionList = List.of(taskDefinitionPropertyInfo);
        TaskValidator.validateTaskParameters(currentTaskParameters, taskDefinitionList, errors, warnings);
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
        String workflow, TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        StringBuilder errors, StringBuilder warnings) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // First, validate the basic workflow structure
            validateWorkflowStructure(workflow, errors);

            // Extract task properties from the provided workflow JSON
            JsonNode workflowNode = objectMapper.readTree(workflow);

            List<JsonNode> taskJsonNodes = new ArrayList<>();

            // Process triggers
            JsonNode triggersJsonNode = workflowNode.get("triggers");
            if (triggersJsonNode != null && triggersJsonNode.isArray()) {
                Iterator<JsonNode> elements = triggersJsonNode.elements();

                elements.forEachRemaining(triggerJsonNode -> {
                    if (taskJsonNodes.isEmpty()) {
                        taskJsonNodes.add(triggerJsonNode);

                        JsonNode typeJsonNode = triggerJsonNode.get("type");

                        String taskType = typeJsonNode.asText();

                        taskDefinitionMap.putIfAbsent(
                            taskType, taskDefinitionProvider.getTaskProperties(taskType, "trigger"));
                        taskOutputMap.putIfAbsent(
                            taskType, taskOutputProvider.getTaskOutputProperty(taskType, "trigger", warnings));
                    } else {
                        errors.append("There can only be one trigger in the workflow");
                    }
                });
            }

            // Process tasks
            JsonNode tasksJsonNode = workflowNode.get("tasks");

            if (tasksJsonNode != null && tasksJsonNode.isArray()) {
                Iterator<JsonNode> elements = tasksJsonNode.elements();

                elements.forEachRemaining(taskJsonNode -> {
                    Stream<JsonNode> stream = taskJsonNodes.stream();

                    JsonNode nameJsonNode = taskJsonNode.get("name");

                    if (stream.anyMatch(previousTask -> Objects.equals(
                        previousTask.get("name"), nameJsonNode))) {

                        errors.append("Tasks cannot have repeating names: ");
                        errors.append(nameJsonNode.asText());
                    }

                    taskJsonNodes.add(taskJsonNode);

                    JsonNode typeJsonNode = taskJsonNode.get("type");

                    String taskType = typeJsonNode.asText();

                    taskDefinitionMap.putIfAbsent(taskType, taskDefinitionProvider.getTaskProperties(taskType, ""));
                    taskOutputMap.putIfAbsent(
                        taskType, taskOutputProvider.getTaskOutputProperty(taskType, "", warnings));

                    // Handle nested TASK type properties by recursively processing them
                    processNestedTasks(
                        taskJsonNode, taskDefinitionMap, taskDefinitionMap, taskOutputMap, taskJsonNodes,
                        taskDefinitionProvider, taskOutputProvider, errors, warnings);
                });
            }

            validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

        } catch (Exception e) {
            errors.append("Failed to validate workflow: ");
            errors.append(e.getMessage());
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
        String task, TaskDefinitionProvider taskDefinitionProvider, StringBuilder errors, StringBuilder warnings) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // First, validate the basic task structure
            TaskValidator.validateTask(task, errors);

            // Extract task properties from the provided task JSON
            JsonNode taskJsonNode = objectMapper.readTree(task);
            JsonNode typeJsonNode = taskJsonNode.get("type");

            String taskType = typeJsonNode.asText();

            // Get the task definition for property validation
            List<PropertyInfo> taskDefinition =
                taskDefinitionProvider.getTaskProperties(taskType, "");

            String taskParameters = "{}";
            JsonNode parametersJsonNode = taskJsonNode.get("parameters");

            if (parametersJsonNode != null && parametersJsonNode.isObject()) {
                taskParameters = objectMapper.writeValueAsString(parametersJsonNode);
            }

            // Validate task properties against the definition
            TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        } catch (Exception e) {
            errors.append("Failed to validate task: ");
            errors.append(e.getMessage());
        }
    }

    /**
     * Validates the overall structure of a workflow JSON.
     *
     * @param workflow the workflow JSON string to validate
     * @param errors   StringBuilder to collect validation errors
     */
    static void validateWorkflowStructure(String workflow, StringBuilder errors) {
        JsonNode workflowNodeJsonNode = JsonUtils.parseJsonWithErrorHandling(workflow, errors);

        if (workflowNodeJsonNode == null) {
            return;
        }

        if (!JsonUtils.validateNodeIsObject(workflowNodeJsonNode, "Workflow", errors)) {
            return;
        }

        // Validate required workflow fields
        FieldValidator.validateRequiredStringField(workflowNodeJsonNode, "label", errors);
        FieldValidator.validateRequiredStringField(workflowNodeJsonNode, "description", errors);
        validateWorkflowTriggerFields(workflowNodeJsonNode, errors);
        validateRequiredArrayField(workflowNodeJsonNode, "tasks", errors);

    }

    /**
     * Processes nested TASK type properties within a task, extracting and validating inner tasks.
     */
    private static void processNestedTasks(
        JsonNode taskJsonNode, Map<String, List<PropertyInfo>> mainTaskDefinitionMap,
        Map<String, List<PropertyInfo>> allTaskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        List<JsonNode> allTaskJsonNode, TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider, StringBuilder errors, StringBuilder warnings) {

        if (!taskJsonNode.has("parameters")) {
            return;
        }

        JsonNode parameters = taskJsonNode.get("parameters");
        JsonNode typeJsonNode = taskJsonNode.get("type");

        String taskType = typeJsonNode.asText();

        List<PropertyInfo> taskDef = mainTaskDefinitionMap.get(taskType);

        if (taskDef != null && !taskDef.isEmpty()) {
            extractNestedTasksFromParameters(
                parameters, taskDef, allTaskDefinitionMap, taskOutputMap, allTaskJsonNode, taskDefinitionProvider,
                taskOutputProvider, errors, warnings);
        } else {
            // If no task definition is available, look for common nested task patterns directly
            discoverNestedTasksFromJsonStructure(
                parameters, allTaskDefinitionMap, taskOutputMap, allTaskJsonNode, taskDefinitionProvider,
                taskOutputProvider, errors, warnings);
        }
    }

    /**
     * Discovers nested tasks by looking for common patterns in JSON structure when task definitions are not available.
     */
    private static void discoverNestedTasksFromJsonStructure(
        JsonNode parametersJsonNode, Map<String, List<PropertyInfo>> allTaskDefinitionMap,
        Map<String, PropertyInfo> taskOutputMap, List<JsonNode> allTaskJsonNodes,
        TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider, StringBuilder errors,
        StringBuilder warnings) {

        // Common nested task property names in different task types
        String[] nestedTaskProperties = {
            "caseTrue", "caseFalse", "iteratee", "tasks"
        };

        for (String propertyName : nestedTaskProperties) {
            if (parametersJsonNode.has(propertyName)) {
                JsonNode propertyValue = parametersJsonNode.get(propertyName);

                if (propertyValue.isArray()) {
                    // Process each task in the array
                    for (int i = 0; i < propertyValue.size(); i++) {
                        JsonNode nestedTaskJsonNode = propertyValue.get(i);

                        if (nestedTaskJsonNode.isObject() && nestedTaskJsonNode.has("type")) {
                            JsonNode typeJsonNode = nestedTaskJsonNode.get("type");

                            String nestedTaskType = typeJsonNode.asText();

                            // Add the nested task to the main tasks list for validation
                            allTaskJsonNodes.add(nestedTaskJsonNode);

                            // Add task definition for the nested task if not already present
                            if (!allTaskDefinitionMap.containsKey(nestedTaskType)) {
                                List<PropertyInfo> nestedTaskDefinition = taskDefinitionProvider.getTaskProperties(
                                    nestedTaskType, "");

                                allTaskDefinitionMap.put(
                                    nestedTaskType, nestedTaskDefinition != null ? nestedTaskDefinition : List.of());
                            }

                            // Add task output for the nested task if not already present
                            if (!taskOutputMap.containsKey(nestedTaskType)) {
                                PropertyInfo nestedTaskOutput = taskOutputProvider.getTaskOutputProperty(
                                    nestedTaskType, "", warnings);

                                taskOutputMap.put(nestedTaskType, nestedTaskOutput);
                            }

                            // Validate the nested task structure
                            TaskValidator.validateTask(nestedTaskJsonNode.toString(), errors);

                            // Recursively process nested tasks within this task
                            if (nestedTaskJsonNode.has("parameters")) {
                                List<PropertyInfo> nestedTaskDefinition = allTaskDefinitionMap.get(nestedTaskType);

                                if (nestedTaskDefinition != null && !nestedTaskDefinition.isEmpty()) {
                                    extractNestedTasksFromParameters(
                                        nestedTaskJsonNode.get("parameters"), nestedTaskDefinition,
                                        allTaskDefinitionMap, taskOutputMap, allTaskJsonNodes, taskDefinitionProvider,
                                        taskOutputProvider, errors, warnings);
                                } else {
                                    // Recursively discover more nested tasks
                                    discoverNestedTasksFromJsonStructure(
                                        nestedTaskJsonNode.get("parameters"), allTaskDefinitionMap, taskOutputMap,
                                        allTaskJsonNodes, taskDefinitionProvider, taskOutputProvider, errors,
                                        warnings);
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
        JsonNode parameters, List<PropertyInfo> taskDefinition, Map<String, List<PropertyInfo>> allTaskDefinitionMap,
        Map<String, PropertyInfo> taskOutputMap, List<JsonNode> allTasks, TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider, StringBuilder errors, StringBuilder warnings) {

        for (PropertyInfo propertyInfo : taskDefinition) {
            String propertyName = propertyInfo.name();

            // Check if this is a TASK type array
            List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

            if ("ARRAY".equalsIgnoreCase(propertyInfo.type()) &&
                propertyInfos != null && propertyInfos.size() == 1) {
                PropertyInfo propertyInfosFirst = propertyInfos.getFirst();

                JsonNode jsonNode = parameters.get(propertyName);

                if ("TASK".equalsIgnoreCase(propertyInfosFirst.type()) && jsonNode != null && jsonNode.isArray()) {
                    // Process each nested task in the array
                    for (int i = 0; i < jsonNode.size(); i++) {
                        JsonNode nestedTask = jsonNode.get(i);

                        if (nestedTask.has("type")) {
                            JsonNode typeJsonNode = nestedTask.get("type");

                            String nestedTaskType = typeJsonNode.asText();

                            // Add the nested task to the main tasks list for validation
                            allTasks.add(nestedTask);

                            // Add task definition for the nested task if not already present
                            if (!allTaskDefinitionMap.containsKey(nestedTaskType)) {
                                List<PropertyInfo> nestedTaskProperties = taskDefinitionProvider.getTaskProperties(
                                    nestedTaskType, "");

                                // Always add the nested task type to the map, even if the provider returns null or
                                // empty
                                allTaskDefinitionMap.put(
                                    nestedTaskType, nestedTaskProperties != null ? nestedTaskProperties : List.of());
                            }

                            // Add task output for the nested task if not already present
                            if (!taskOutputMap.containsKey(nestedTaskType)) {
                                PropertyInfo nestedTaskOutput = taskOutputProvider.getTaskOutputProperty(
                                    nestedTaskType, "", warnings);

                                // Always add the nested task type to the map, even if the provider returns null
                                taskOutputMap.put(nestedTaskType, nestedTaskOutput);
                            }

                            // Validate the nested task structure
                            TaskValidator.validateTask(nestedTask.toString(), errors);

                            // Recursively process nested tasks within this task
                            if (nestedTask.has("parameters")) {
                                List<PropertyInfo> nestedTaskDefinition = allTaskDefinitionMap.get(nestedTaskType);

                                if (nestedTaskDefinition != null) {
                                    extractNestedTasksFromParameters(nestedTask.get("parameters"), nestedTaskDefinition,
                                        allTaskDefinitionMap, taskOutputMap, allTasks, taskDefinitionProvider,
                                        taskOutputProvider, errors, warnings);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Validates workflow triggers field structure and constraints.
     */
    private static void validateWorkflowTriggerFields(JsonNode workflowJsonNode, StringBuilder errors) {
        if (!workflowJsonNode.has("triggers")) {
            StringUtils.appendWithNewline(errors, "Missing required field: triggers");
        } else {
            JsonNode triggersJsonNode = workflowJsonNode.get("triggers");

            if (!triggersJsonNode.isArray()) {
                StringUtils.appendWithNewline(errors, "Field 'triggers' must be an array");
            } else if (triggersJsonNode.size() > 1) {
                StringUtils.appendWithNewline(errors, "Field 'triggers' must contain one or less objects");
            } else {
                JsonNode jsonNode = triggersJsonNode.get(0);

                if (triggersJsonNode.size() == 1 && !jsonNode.isObject()) {
                    StringUtils.appendWithNewline(errors, "Trigger must be an object");
                }
            }
        }
    }

    /**
     * Validates that a required array field exists and is of the correct type.
     */
    private static void validateRequiredArrayField(JsonNode jsonNode, String fieldName, StringBuilder errors) {
        if (!jsonNode.has(fieldName)) {
            StringUtils.appendWithNewline(errors, "Missing required field: " + fieldName);
        } else {
            JsonNode fieldJsonNode = jsonNode.get(fieldName);

            if (!fieldJsonNode.isArray()) {
                StringUtils.appendWithNewline(errors, "Field '" + fieldName + "' must be an array");
            }
        }
    }

    /**
     * Functional interface for providing task definitions.
     */
    @FunctionalInterface
    public interface TaskDefinitionProvider {
        List<PropertyInfo> getTaskProperties(String taskType, String kind);
    }

    /**
     * Functional interface for providing task output properties.
     */
    @FunctionalInterface
    public interface TaskOutputProvider {
        PropertyInfo getTaskOutputProperty(String taskType, String kind, StringBuilder warnings);
    }
}
