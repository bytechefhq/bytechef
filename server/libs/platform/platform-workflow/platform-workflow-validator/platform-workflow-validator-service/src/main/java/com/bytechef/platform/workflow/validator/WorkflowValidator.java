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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * @author Marko Kriskovic
 */
public class WorkflowValidator {

    static final String[] NESTED_TASK_PROPERTIES = new String[] {
        "caseTrue", "caseFalse", "iteratee", "tasks"
    };

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
        @Nullable ClusterTypesProvider clusterTypesProvider, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutputMap, Map<String, List<String>> clusterTypesMap,
        StringBuilder errors, StringBuilder warnings) {

        validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, taskDefinitionMap,
            taskOutputMap, Map.of(), clusterTypesMap, errors, warnings);
    }

    /**
     * Same as {@link #validateWorkflow}, plus a config-aware per-node output map (node name to the schema the node
     * produces for its configured input parameters) used to hard-fail references that don't exist in a dynamic output's
     * config-resolved shape.
     */
    public static void validateWorkflow(
        String workflow, TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        @Nullable ClusterTypesProvider clusterTypesProvider, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutputMap, Map<String, PropertyInfo> nodeOutputMap,
        Map<String, List<String>> clusterTypesMap, StringBuilder errors, StringBuilder warnings) {

        try {
            validateWorkflowStructure(workflow, errors);

            JsonNode workflowJsonNode = com.bytechef.commons.util.JsonUtils.readTree(workflow);

            for (String duplicateNodeName : getDuplicateNodeNames(workflowJsonNode)) {
                StringUtils.appendWithNewline(
                    "Node names must be unique. Duplicate node name: " + duplicateNodeName, errors);
            }

            List<JsonNode> taskJsonNodes = new ArrayList<>();

            processTriggers(
                taskDefinitionProvider, taskOutputProvider, taskDefinitionMap, taskOutputMap, warnings,
                workflowJsonNode, taskJsonNodes);
            processTasks(
                taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, taskDefinitionMap,
                taskOutputMap, clusterTypesMap, workflowJsonNode, taskJsonNodes, errors, warnings);
            validateWorkflowTasks(
                taskJsonNodes, taskDefinitionMap, taskOutputMap, nodeOutputMap, clusterTypesMap, errors, warnings);
        } catch (Exception e) {
            errors.append("Failed to validate workflow: ");
            errors.append(e.getMessage()
                .replace("\n", " "));
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
        Map<String, PropertyInfo> taskOutput, Map<String, List<String>> clusterTypesProviderMap, StringBuilder errors,
        StringBuilder warnings) {

        validateWorkflowTasks(
            taskJsonNodes, taskDefinitionMap, taskOutput, Map.of(), clusterTypesProviderMap, errors, warnings);
    }

    public static void validateWorkflowTasks(
        List<JsonNode> taskJsonNodes, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutput, Map<String, PropertyInfo> nodeOutputMap,
        Map<String, List<String>> clusterTypesProviderMap, StringBuilder errors, StringBuilder warnings) {

        ValidationContext context = ValidationContext.of(
            taskJsonNodes, taskDefinitionMap, taskOutput, nodeOutputMap, clusterTypesProviderMap, errors, warnings);

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

            String taskName = "";

            if (taskJsonNode.has("name")) {
                JsonNode nameJsonNode = taskJsonNode.get("name");

                if (nameJsonNode.isString()) {
                    taskName = nameJsonNode.asString();
                }
            }

            JsonNode typeJsonNode = taskJsonNode.get("type");

            String type = typeJsonNode.asString();

            List<PropertyInfo> taskDefinition = taskDefinitionProvider.getTaskProperties(type, "");

            String taskParameters = "{}";
            JsonNode parametersJsonNode = taskJsonNode.get("parameters");

            if (parametersJsonNode != null && parametersJsonNode.isObject()) {
                taskParameters = com.bytechef.commons.util.JsonUtils.write(parametersJsonNode);
            }

            TaskValidator.validateTaskParameters(taskName, taskParameters, taskDefinition, errors, warnings);

        } catch (Exception e) {
            errors.append("Failed to validate task: ");
            errors.append(e.getMessage());
        }
    }

    /**
     * Discovers nested tasks by looking for common patterns in JSON structure when task definitions are not available.
     */
    private static void discoverNestedTasksFromJsonStructure(
        JsonNode parametersJsonNode, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, @Nullable PropertyInfo> taskOutputMap, Map<String, List<String>> clusterTypesMap,
        List<JsonNode> taskJsonNodes, TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider, ClusterTypesProvider clusterTypesProvider, StringBuilder errors,
        StringBuilder warnings) {

        for (String propertyName : NESTED_TASK_PROPERTIES) {
            if (parametersJsonNode.has(propertyName)) {
                JsonNode jsonNode = parametersJsonNode.get(propertyName);

                if (jsonNode.isArray()) {
                    for (int i = 0; i < jsonNode.size(); i++) {
                        JsonNode nestedTaskJsonNode = jsonNode.get(i);

                        if (nestedTaskJsonNode.isObject() && nestedTaskJsonNode.has("type")) {
                            String type =
                                getType(
                                    taskDefinitionMap, taskOutputMap, taskJsonNodes, taskDefinitionProvider,
                                    taskOutputProvider, errors, warnings, nestedTaskJsonNode);

                            if (nestedTaskJsonNode.has("clusterElements")) {
                                List<String> clusterElementTypes =
                                    clusterTypesProvider.getClusterElementTypes(type);

                                if (clusterElementTypes != null) {
                                    clusterTypesMap.putIfAbsent(type, clusterElementTypes);
                                }

                                processClusterElements(
                                    nestedTaskJsonNode, taskDefinitionMap, taskOutputMap, clusterTypesMap,
                                    taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, warnings);
                            }

                            if (nestedTaskJsonNode.has("parameters")) {
                                List<PropertyInfo> nestedTaskDefinition = taskDefinitionMap.get(type);

                                if (nestedTaskDefinition != null && !nestedTaskDefinition.isEmpty()) {
                                    extractNestedTasksFromParameters(
                                        nestedTaskJsonNode.get("parameters"), nestedTaskDefinition,
                                        taskDefinitionMap, taskOutputMap, taskJsonNodes,
                                        taskDefinitionProvider, taskOutputProvider, errors,
                                        warnings);
                                } else {
                                    // Recursively discover more nested tasks
                                    discoverNestedTasksFromJsonStructure(
                                        nestedTaskJsonNode.get("parameters"), taskDefinitionMap, taskOutputMap,
                                        clusterTypesMap, taskJsonNodes, taskDefinitionProvider, taskOutputProvider,
                                        clusterTypesProvider, errors, warnings);
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
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, @Nullable PropertyInfo> taskOutputMap,
        List<JsonNode> taskJsonNodes, TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider, StringBuilder errors, StringBuilder warnings) {

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
                            String type = getType(
                                taskDefinitionMap, taskOutputMap, taskJsonNodes,
                                taskDefinitionProvider, taskOutputProvider, errors, warnings, nestedTaskJsonNode);

                            if (nestedTaskJsonNode.has("parameters")) {
                                List<PropertyInfo> nestedTaskDefinition = taskDefinitionMap.get(type);

                                if (nestedTaskDefinition != null) {
                                    extractNestedTasksFromParameters(
                                        nestedTaskJsonNode.get("parameters"), nestedTaskDefinition, taskDefinitionMap,
                                        taskOutputMap, taskJsonNodes, taskDefinitionProvider, taskOutputProvider,
                                        errors, warnings);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static String getType(
        Map<String, List<PropertyInfo>> allTaskDefinitionPropertyInfosMap,
        Map<String, @Nullable PropertyInfo> taskOutputPropertyInfoMap, List<JsonNode> allTaskJsonNodes,
        TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider, StringBuilder errors,
        StringBuilder warnings, JsonNode nestedTaskJsonNode) {

        JsonNode typeJsonNode = nestedTaskJsonNode.get("type");

        String type = typeJsonNode.asString();

        allTaskJsonNodes.add(nestedTaskJsonNode);

        if (!allTaskDefinitionPropertyInfosMap.containsKey(type)) {
            List<PropertyInfo> nestedTaskProperties = taskDefinitionProvider.getTaskProperties(type, "");

            allTaskDefinitionPropertyInfosMap.put(type, nestedTaskProperties);
        }

        if (!taskOutputPropertyInfoMap.containsKey(type)) {
            PropertyInfo nestedTaskOutput = taskOutputProvider.getTaskOutputProperty(type, "", warnings);

            taskOutputPropertyInfoMap.put(type, nestedTaskOutput);
        }

        TaskValidator.validateTaskStructure(nestedTaskJsonNode.toString(), errors);

        return type;
    }

    private static void processClusterElements(
        JsonNode taskJsonNode, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, @Nullable PropertyInfo> taskOutputMap, Map<String, List<String>> clusterTypesMap,
        TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        ClusterTypesProvider clusterTypesProvider, StringBuilder warnings) {

        if (!taskJsonNode.has("clusterElements")) {
            return;
        }

        JsonNode clusterElementsJsonNode = taskJsonNode.get("clusterElements");

        if (!clusterElementsJsonNode.isObject()) {
            return;
        }

        for (String fieldName : clusterElementsJsonNode.propertyNames()) {
            JsonNode clusterElementJsonNode = clusterElementsJsonNode.get(fieldName);

            if (clusterElementJsonNode == null) {
                continue;
            }

            if (clusterElementJsonNode.isArray()) {
                for (int i = 0; i < clusterElementJsonNode.size(); i++) {
                    JsonNode arrayItemJsonNode = clusterElementJsonNode.get(i);

                    if (arrayItemJsonNode.isObject() && arrayItemJsonNode.has("type")) {
                        JsonNode typeJsonNode = arrayItemJsonNode.get("type");

                        String type = typeJsonNode.asString();

                        taskDefinitionMap.putIfAbsent(type,
                            taskDefinitionProvider.getTaskProperties(type, "clusterElement"));
                        taskOutputMap.putIfAbsent(type,
                            taskOutputProvider.getTaskOutputProperty(type, "clusterElement", warnings));
                    }
                }
            } else if (clusterElementJsonNode.isObject() &&
                clusterElementJsonNode.has("clusterElements") && clusterElementJsonNode.has("type")) {

                JsonNode typeJsonNode = clusterElementJsonNode.get("type");

                String type = typeJsonNode.asString();

                taskDefinitionMap.putIfAbsent(type, taskDefinitionProvider.getTaskProperties(type, "clusterElement"));
                taskOutputMap.putIfAbsent(
                    type, taskOutputProvider.getTaskOutputProperty(type, "clusterElement", warnings));

                List<String> clusterElementTypes = clusterTypesProvider.getClusterElementTypes(type);

                if (clusterElementTypes != null) {
                    clusterTypesMap.putIfAbsent(type, clusterElementTypes);
                }

                processClusterElements(
                    clusterElementJsonNode, taskDefinitionMap, taskOutputMap, clusterTypesMap, taskDefinitionProvider,
                    taskOutputProvider, clusterTypesProvider, warnings);
            }
        }
    }

    private static void processNestedTasks(
        JsonNode taskJsonNode, Map<String, List<PropertyInfo>> taskDefinitionsMap,
        Map<String, @Nullable PropertyInfo> taskOutputMap, Map<String, List<String>> clusterTypesMap,
        List<JsonNode> taskJsonNodes, TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider, ClusterTypesProvider clusterTypesProvider,
        StringBuilder errors, StringBuilder warnings) {

        processClusterElements(taskJsonNode, taskDefinitionsMap, taskOutputMap, clusterTypesMap,
            taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, warnings);

        if (!taskJsonNode.has("parameters")) {
            return;
        }

        JsonNode parametersJsonNode = taskJsonNode.get("parameters");

        discoverNestedTasksFromJsonStructure(
            parametersJsonNode, taskDefinitionsMap, taskOutputMap, clusterTypesMap, taskJsonNodes,
            taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, errors, warnings);
    }

    private static void processTasks(
        TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        @Nullable ClusterTypesProvider clusterTypesProvider, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, @Nullable PropertyInfo> taskOutputMap, Map<String, List<String>> clusterTypesMap,
        JsonNode workflowJsonNode, List<JsonNode> taskJsonNodes, StringBuilder errors, StringBuilder warnings) {

        JsonNode tasksJsonNode = workflowJsonNode.get("tasks");

        if (tasksJsonNode != null && tasksJsonNode.isArray()) {
            Iterator<JsonNode> iterator = tasksJsonNode.iterator();

            iterator.forEachRemaining(taskJsonNode -> {
                taskJsonNodes.add(taskJsonNode);

                JsonNode typeJsonNode = taskJsonNode.get("type");

                String type = typeJsonNode.asString();

                taskDefinitionMap.putIfAbsent(type, taskDefinitionProvider.getTaskProperties(type, ""));
                taskOutputMap.putIfAbsent(type, taskOutputProvider.getTaskOutputProperty(type, "", warnings));

                if (taskJsonNode.has("clusterElements")) {
                    List<String> clusterElementTypes = clusterTypesProvider.getClusterElementTypes(type);

                    if (clusterElementTypes != null) {
                        clusterTypesMap.putIfAbsent(type, clusterElementTypes);
                    }
                }

                processNestedTasks(
                    taskJsonNode, taskDefinitionMap, taskOutputMap, clusterTypesMap, taskJsonNodes,
                    taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, errors, warnings);
            });
        }
    }

    private static void processTriggers(
        TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        Map<String, List<PropertyInfo>> taskDefinitionPropertyInfosMap,
        Map<String, @Nullable PropertyInfo> taskOutputPropertyInfoMap, StringBuilder warnings,
        JsonNode workflowJsonNode, List<JsonNode> taskJsonNodes) {

        JsonNode triggersJsonNode = workflowJsonNode.get("triggers");

        if (triggersJsonNode != null && triggersJsonNode.isArray()) {
            Iterator<JsonNode> iterator = triggersJsonNode.iterator();

            iterator.forEachRemaining(triggerJsonNode -> {
                taskJsonNodes.add(triggerJsonNode);

                JsonNode typeJsonNode = triggerJsonNode.get("type");

                String type = typeJsonNode.asString();

                taskDefinitionPropertyInfosMap.putIfAbsent(
                    type, taskDefinitionProvider.getTaskProperties(type, "trigger"));
                taskOutputPropertyInfoMap.putIfAbsent(
                    type, taskOutputProvider.getTaskOutputProperty(type, "trigger", warnings));
            });
        }
    }

    /**
     * Returns the node names (the trigger plus all tasks, including tasks nested inside condition, loop, branch,
     * parallel, each, fork-join and on-error dispatchers) that occur more than once in the given workflow JSON. Node
     * names are global ids of workflow nodes, so a duplicate name produces two nodes with the same id and a broken,
     * unrenderable graph. A malformed workflow yields an empty list (it is reported by structure validation, not here).
     */
    public static List<String> getDuplicateNodeNames(String workflow) {
        try {
            return getDuplicateNodeNames(com.bytechef.commons.util.JsonUtils.readTree(workflow));
        } catch (Exception e) {
            return List.of();
        }
    }

    static List<String> getDuplicateNodeNames(JsonNode workflowJsonNode) {
        List<String> nodeNames = new ArrayList<>();

        JsonNode triggersJsonNode = workflowJsonNode.get("triggers");

        if (triggersJsonNode != null && triggersJsonNode.isArray()) {
            for (JsonNode triggerJsonNode : triggersJsonNode) {
                collectNodeName(triggerJsonNode, nodeNames);
            }
        }

        JsonNode tasksJsonNode = workflowJsonNode.get("tasks");

        if (tasksJsonNode != null && tasksJsonNode.isArray()) {
            collectTaskNames(tasksJsonNode, nodeNames);
        }

        Set<String> seenNames = new HashSet<>();
        Set<String> duplicateNames = new LinkedHashSet<>();

        for (String nodeName : nodeNames) {
            if (!seenNames.add(nodeName)) {
                duplicateNames.add(nodeName);
            }
        }

        return new ArrayList<>(duplicateNames);
    }

    /**
     * Recursively collects the names of the given tasks and of any tasks nested within their parameters.
     */
    private static void collectTaskNames(JsonNode tasksJsonNode, List<String> nodeNames) {
        for (JsonNode taskJsonNode : tasksJsonNode) {
            if (!taskJsonNode.isObject()) {
                continue;
            }

            collectNodeName(taskJsonNode, nodeNames);

            JsonNode parametersJsonNode = taskJsonNode.get("parameters");

            if (parametersJsonNode != null && parametersJsonNode.isObject()) {
                collectNestedTaskNames(parametersJsonNode, nodeNames);
            }
        }
    }

    /**
     * Collects task names from the task-dispatcher nesting shapes: condition caseTrue/caseFalse, branch default/cases,
     * parallel/on-error task arrays, loop/each/map iteratee (array or single object) and fork-join branches.
     */
    private static void collectNestedTaskNames(JsonNode parametersJsonNode, List<String> nodeNames) {
        for (String key : new String[] {
            "caseTrue", "caseFalse", "default", "main-branch", "on-error-branch", "tasks"
        }) {

            JsonNode nestedTasksJsonNode = parametersJsonNode.get(key);

            if (nestedTasksJsonNode != null && nestedTasksJsonNode.isArray()) {
                collectTaskNames(nestedTasksJsonNode, nodeNames);
            }
        }

        JsonNode iterateeJsonNode = parametersJsonNode.get("iteratee");

        if (iterateeJsonNode != null) {
            if (iterateeJsonNode.isArray()) {
                collectTaskNames(iterateeJsonNode, nodeNames);
            } else if (iterateeJsonNode.isObject() && iterateeJsonNode.has("name")) {
                collectNodeName(iterateeJsonNode, nodeNames);

                JsonNode iterateeParametersJsonNode = iterateeJsonNode.get("parameters");

                if (iterateeParametersJsonNode != null && iterateeParametersJsonNode.isObject()) {
                    collectNestedTaskNames(iterateeParametersJsonNode, nodeNames);
                }
            }
        }

        JsonNode casesJsonNode = parametersJsonNode.get("cases");

        if (casesJsonNode != null && casesJsonNode.isArray()) {
            for (JsonNode caseJsonNode : casesJsonNode) {
                JsonNode caseTasksJsonNode = caseJsonNode.get("tasks");

                if (caseTasksJsonNode != null && caseTasksJsonNode.isArray()) {
                    collectTaskNames(caseTasksJsonNode, nodeNames);
                }
            }
        }

        JsonNode branchesJsonNode = parametersJsonNode.get("branches");

        if (branchesJsonNode != null && branchesJsonNode.isArray()) {
            for (JsonNode branchJsonNode : branchesJsonNode) {
                if (branchJsonNode.isArray()) {
                    collectTaskNames(branchJsonNode, nodeNames);
                }
            }
        }
    }

    private static void collectNodeName(JsonNode nodeJsonNode, List<String> nodeNames) {
        JsonNode nameJsonNode = nodeJsonNode.get("name");

        if (nameJsonNode != null && nameJsonNode.isString()) {
            nodeNames.add(nameJsonNode.asString());
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
     * Validates the overall structure of a workflow JSON.
     *
     * @param workflow the workflow JSON string to validate
     * @param errors   StringBuilder to collect validation errors
     */
    static void validateWorkflowStructure(String workflow, StringBuilder errors) {
        JsonNode workflowJsonNode = JsonNodeUtils.parseJsonWithErrorHandling(workflow, errors);

        if (workflowJsonNode == null) {
            return;
        }

        if (!JsonNodeUtils.appendErrorNodeIsObject(workflowJsonNode, "Workflow", errors)) {
            return;
        }

        FieldValidator.appendErrorRequiredStringField(workflowJsonNode, "label", errors);
        FieldValidator.appendErrorRequiredStringField(workflowJsonNode, "description", errors);
        validateWorkflowTriggerFields(workflowJsonNode, errors);
        validateRequiredArrayField(workflowJsonNode, errors);

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
            } else {
                for (JsonNode triggerJsonNode : triggersJsonNode) {
                    if (!triggerJsonNode.isObject()) {
                        StringUtils.appendWithNewline("Trigger must be an object", errors);
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
        List<PropertyInfo> getTaskProperties(String taskType, String kind);
    }

    /**
     * Functional interface for providing task output properties.
     */
    @FunctionalInterface
    public interface TaskOutputProvider {
        @Nullable
        PropertyInfo getTaskOutputProperty(String taskType, String kind, StringBuilder warnings);
    }

    @FunctionalInterface
    public interface ClusterTypesProvider {
        @Nullable
        List<String> getClusterElementTypes(String taskType);
    }
}
