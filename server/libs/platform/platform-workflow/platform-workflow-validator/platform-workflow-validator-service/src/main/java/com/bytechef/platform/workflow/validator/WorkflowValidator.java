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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * @author Marko Kriskovic
 */
public class WorkflowValidator {

    /**
     * Validates task parameters against a single PropertyInfo task definition.
     *
     * @param taskParameters             the current task parameters JSON
     * @param taskDefinitionPropertyInfo the PropertyInfo representing the task definition
     * @param errors                     StringBuilder to collect validation errors
     * @param warnings                   StringBuilder to collect validation warnings
     */
    public static void validateTaskParameters(
        String taskParameters, @Nullable PropertyInfo taskDefinitionPropertyInfo, StringBuilder errors,
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

        TaskValidator.validateTaskParameters(taskParameters, taskDefinitionList, errors, warnings);
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
    public static void validateWorkflow(
        String workflow, TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        StringBuilder errors, StringBuilder warnings) {

        try {
            validateWorkflowStructure(workflow, errors);

            JsonNode workflowJsonNode = com.bytechef.commons.util.JsonUtils.readTree(workflow);

            List<JsonNode> taskJsonNodes = new ArrayList<>();

            processTriggers(
                taskDefinitionProvider, taskOutputProvider, taskDefinitionMap, taskOutputMap, errors, warnings,
                workflowJsonNode, taskJsonNodes);
            processTasks(taskDefinitionProvider, taskOutputProvider, taskDefinitionMap, taskOutputMap, errors, warnings,
                workflowJsonNode, taskJsonNodes);
            validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);
        } catch (Exception e) {
            errors.append("Failed to validate workflow: ");
            errors.append(e.getMessage());
        }
    }

    /**
     * Validates all tasks in a workflow, including their structure, parameters, and data pill references.
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

        ValidationContext context = ValidationContext.of(
            taskJsonNodes, taskDefinitionMap, taskOutput, errors, warnings);

        TaskValidator.validateAllTasks(context);
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
            TaskValidator.validateTaskStructure(task, errors);

            JsonNode taskJsonNode = com.bytechef.commons.util.JsonUtils.readTree(task);

            JsonNode typeJsonNode = taskJsonNode.get("type");

            String type = typeJsonNode.asText();

            List<PropertyInfo> taskDefinition = taskDefinitionProvider.getTaskProperties(type, "");

            String taskParameters = "{}";
            JsonNode parametersJsonNode = taskJsonNode.get("parameters");

            if (parametersJsonNode != null && parametersJsonNode.isObject()) {
                taskParameters = com.bytechef.commons.util.JsonUtils.write(parametersJsonNode);
            }

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

        if (!JsonUtils.appendErrorNodeIsObject(workflowNodeJsonNode, "Workflow", errors)) {
            return;
        }

        FieldValidator.appendErrorRequiredStringField(workflowNodeJsonNode, "label", errors);
        FieldValidator.appendErrorRequiredStringField(workflowNodeJsonNode, "description", errors);
        validateWorkflowTriggerFields(workflowNodeJsonNode, errors);
        validateRequiredArrayField(workflowNodeJsonNode, errors);

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

        JsonNode parametersJsonNode = taskJsonNode.get("parameters");
        JsonNode typeJsonNode = taskJsonNode.get("type");

        String type = typeJsonNode.asText();

        List<PropertyInfo> taskDef = mainTaskDefinitionMap.get(type);

        if (taskDef != null && !taskDef.isEmpty()) {
            extractNestedTasksFromParameters(
                parametersJsonNode, taskDef, allTaskDefinitionMap, taskOutputMap, allTaskJsonNode,
                taskDefinitionProvider, taskOutputProvider, errors, warnings);
        } else {
            discoverNestedTasksFromJsonStructure(
                parametersJsonNode, allTaskDefinitionMap, taskOutputMap, allTaskJsonNode, taskDefinitionProvider,
                taskOutputProvider, errors, warnings);
        }
    }

    private static void processTasks(
        TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        StringBuilder errors, StringBuilder warnings, JsonNode workflowNode, List<JsonNode> taskJsonNodes) {
        JsonNode tasksJsonNode = workflowNode.get("tasks");

        if (tasksJsonNode != null && tasksJsonNode.isArray()) {
            Iterator<JsonNode> elements = tasksJsonNode.iterator();

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

                String type = typeJsonNode.asText();

                taskDefinitionMap.putIfAbsent(type, taskDefinitionProvider.getTaskProperties(type, ""));
                taskOutputMap.putIfAbsent(type, taskOutputProvider.getTaskOutputProperty(type, "", warnings));

                processNestedTasks(
                    taskJsonNode, taskDefinitionMap, taskDefinitionMap, taskOutputMap, taskJsonNodes,
                    taskDefinitionProvider, taskOutputProvider, errors, warnings);
            });
        }
    }

    private static void processTriggers(
        TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        StringBuilder errors, StringBuilder warnings, JsonNode workflowNode, List<JsonNode> taskJsonNodes) {
        JsonNode triggersJsonNode = workflowNode.get("triggers");

        if (triggersJsonNode != null && triggersJsonNode.isArray()) {
            Iterator<JsonNode> triggersJsonNodeIterator = triggersJsonNode.iterator();

            triggersJsonNodeIterator.forEachRemaining(triggerJsonNode -> {
                if (taskJsonNodes.isEmpty()) {
                    taskJsonNodes.add(triggerJsonNode);

                    JsonNode typeJsonNode = triggerJsonNode.get("type");

                    String type = typeJsonNode.asText();

                    taskDefinitionMap.putIfAbsent(
                        type, taskDefinitionProvider.getTaskProperties(type, "trigger"));
                    taskOutputMap.putIfAbsent(
                        type, taskOutputProvider.getTaskOutputProperty(type, "trigger", warnings));
                } else {
                    errors.append("There can only be one trigger in the workflow");
                }
            });
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

        String[] nestedTaskProperties = {
            "caseTrue", "caseFalse", "iteratee", "tasks"
        };

        for (String propertyName : nestedTaskProperties) {
            if (parametersJsonNode.has(propertyName)) {
                JsonNode jsonNode = parametersJsonNode.get(propertyName);

                if (jsonNode.isArray()) {
                    for (int i = 0; i < jsonNode.size(); i++) {
                        JsonNode nestedTaskJsonNode = jsonNode.get(i);

                        if (nestedTaskJsonNode.isObject() && nestedTaskJsonNode.has("type")) {
                            JsonNode typeJsonNode = nestedTaskJsonNode.get("type");

                            String type = typeJsonNode.asText();

                            allTaskJsonNodes.add(nestedTaskJsonNode);

                            if (!allTaskDefinitionMap.containsKey(type)) {
                                List<PropertyInfo> nestedTaskDefinition = taskDefinitionProvider.getTaskProperties(
                                    type, "");

                                allTaskDefinitionMap.put(type, nestedTaskDefinition);
                            }

                            if (!taskOutputMap.containsKey(type)) {
                                PropertyInfo nestedTaskOutput = taskOutputProvider.getTaskOutputProperty(
                                    type, "", warnings);

                                taskOutputMap.put(type, nestedTaskOutput);
                            }

                            TaskValidator.validateTaskStructure(nestedTaskJsonNode.toString(), errors);

                            if (nestedTaskJsonNode.has("parameters")) {
                                List<PropertyInfo> nestedTaskDefinition = allTaskDefinitionMap.get(type);

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
        JsonNode parametersJsonNode, List<PropertyInfo> taskDefinition,
        Map<String, List<PropertyInfo>> allTaskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        List<JsonNode> allTasks,
        TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider, StringBuilder errors,
        StringBuilder warnings) {

        for (PropertyInfo propertyInfo : taskDefinition) {
            String propertyName = propertyInfo.name();

            List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

            if ("ARRAY".equalsIgnoreCase(propertyInfo.type()) &&
                propertyInfos != null && propertyInfos.size() == 1) {
                PropertyInfo propertyInfosFirst = propertyInfos.getFirst();

                JsonNode jsonNode = parametersJsonNode.get(propertyName);

                if ("TASK".equalsIgnoreCase(propertyInfosFirst.type()) && jsonNode != null && jsonNode.isArray()) {
                    for (int i = 0; i < jsonNode.size(); i++) {
                        JsonNode nestedTaskJsonNode = jsonNode.get(i);

                        if (nestedTaskJsonNode.has("type")) {
                            JsonNode typeJsonNode = nestedTaskJsonNode.get("type");

                            String type = typeJsonNode.asText();

                            allTasks.add(nestedTaskJsonNode);

                            if (!allTaskDefinitionMap.containsKey(type)) {
                                List<PropertyInfo> nestedTaskProperties = taskDefinitionProvider.getTaskProperties(
                                    type, "");

                                allTaskDefinitionMap.put(type, nestedTaskProperties);
                            }

                            if (!taskOutputMap.containsKey(type)) {
                                PropertyInfo nestedTaskOutput = taskOutputProvider.getTaskOutputProperty(
                                    type, "", warnings);

                                taskOutputMap.put(type, nestedTaskOutput);
                            }

                            TaskValidator.validateTaskStructure(nestedTaskJsonNode.toString(), errors);

                            if (nestedTaskJsonNode.has("parameters")) {
                                List<PropertyInfo> nestedTaskDefinition = allTaskDefinitionMap.get(type);

                                if (nestedTaskDefinition != null) {
                                    extractNestedTasksFromParameters(nestedTaskJsonNode.get("parameters"),
                                        nestedTaskDefinition, allTaskDefinitionMap, taskOutputMap, allTasks,
                                        taskDefinitionProvider, taskOutputProvider, errors, warnings);
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
            StringUtils.appendWithNewline("Missing required field: triggers", errors);
        } else {
            JsonNode triggersJsonNode = workflowJsonNode.get("triggers");

            if (!triggersJsonNode.isArray()) {
                StringUtils.appendWithNewline("Field 'triggers' must be an array", errors);
            } else if (triggersJsonNode.size() > 1) {
                StringUtils.appendWithNewline("Field 'triggers' must contain one or less objects", errors);
            } else {
                JsonNode jsonNode = triggersJsonNode.get(0);

                if (triggersJsonNode.size() == 1 && !jsonNode.isObject()) {
                    StringUtils.appendWithNewline("Trigger must be an object", errors);
                }
            }
        }
    }

    /**
     * Validates that a required array field exists and is of the correct type.
     */
    private static void validateRequiredArrayField(JsonNode jsonNode, StringBuilder errors) {
        if (!jsonNode.has("tasks")) {
            StringUtils.appendWithNewline("Missing required field: " + "tasks", errors);
        } else {
            JsonNode fieldJsonNode = jsonNode.get("tasks");

            if (!fieldJsonNode.isArray()) {
                StringUtils.appendWithNewline("Field '" + "tasks" + "' must be an array", errors);
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
