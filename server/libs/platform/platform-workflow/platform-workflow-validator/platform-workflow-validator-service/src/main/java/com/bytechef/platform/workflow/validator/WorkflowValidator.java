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
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * @author Marko Kriskovic
 */
public class WorkflowValidator {

    private static final String[] NESTED_TASK_ARRAY_PROPERTIES = new String[] {
        "caseTrue", "caseFalse", "default", "main-branch", "on-error-branch", "tasks"
    };

    static final List<String> VALID_INPUT_TYPES = List.of(
        "boolean", "date", "date_time", "time", "integer", "number", "string");

    private static final Pattern INPUT_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

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

        validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, NO_RESOURCE_REFERENCE_PROVIDER,
            taskDefinitionMap, taskOutputMap, nodeOutputMap, clusterTypesMap, errors, warnings);
    }

    public static void validateWorkflow(
        String workflow, TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        @Nullable ClusterTypesProvider clusterTypesProvider, ResourceReferenceProvider resourceReferenceProvider,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        Map<String, PropertyInfo> nodeOutputMap, Map<String, List<String>> clusterTypesMap, StringBuilder errors,
        StringBuilder warnings) {

        validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, resourceReferenceProvider,
            taskDefinitionMap, taskOutputMap, nodeOutputMap, Map.of(), clusterTypesMap, errors, warnings);
    }

    /**
     * Same as {@link #validateWorkflow}, plus a per-node variable output map (node name to the schema a task dispatcher
     * exposes to the tasks nested inside it, such as the current item of a loop or map).
     */
    public static void validateWorkflow(
        String workflow, TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider,
        @Nullable ClusterTypesProvider clusterTypesProvider, ResourceReferenceProvider resourceReferenceProvider,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        Map<String, PropertyInfo> nodeOutputMap, Map<String, PropertyInfo> nodeVariableOutputMap,
        Map<String, List<String>> clusterTypesMap, StringBuilder errors, StringBuilder warnings) {

        try {
            validateWorkflowStructure(workflow, errors, warnings);

            JsonNode workflowJsonNode = com.bytechef.commons.util.JsonUtils.readTree(workflow);

            for (String duplicateNodeName : getDuplicateNodeNames(workflowJsonNode)) {
                StringUtils.appendWithNewline(
                    "Node names must be unique. Duplicate node name: " + duplicateNodeName, errors);
            }

            List<JsonNode> inputJsonNodes = new ArrayList<>();
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            processInputs(taskOutputMap, workflowJsonNode, inputJsonNodes, errors, warnings);
            processTriggers(
                taskDefinitionProvider, taskOutputProvider, taskDefinitionMap, taskOutputMap, warnings,
                workflowJsonNode, taskJsonNodes);
            processTasks(
                taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, taskDefinitionMap,
                taskOutputMap, clusterTypesMap, workflowJsonNode, taskJsonNodes, errors, warnings);
            validateWorkflowTasks(
                taskJsonNodes, inputJsonNodes, taskDefinitionMap, taskOutputMap, nodeOutputMap, nodeVariableOutputMap,
                clusterTypesMap, clusterTypesProvider, resourceReferenceProvider, errors, warnings);
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

        validateWorkflowTasks(
            taskJsonNodes, List.of(), taskDefinitionMap, taskOutput, nodeOutputMap, Map.of(), clusterTypesProviderMap,
            null, NO_RESOURCE_REFERENCE_PROVIDER, errors, warnings);
    }

    private static void validateWorkflowTasks(
        List<JsonNode> taskJsonNodes, List<JsonNode> inputJsonNodes, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutput, Map<String, PropertyInfo> nodeOutputMap,
        Map<String, PropertyInfo> nodeVariableOutputMap, Map<String, List<String>> clusterTypesProviderMap,
        @Nullable ClusterTypesProvider clusterTypesProvider, ResourceReferenceProvider resourceReferenceProvider,
        StringBuilder errors, StringBuilder warnings) {

        ValidationContext context = ValidationContext.of(
            taskJsonNodes, inputJsonNodes, taskDefinitionMap, taskOutput, nodeOutputMap, nodeVariableOutputMap,
            clusterTypesProviderMap, clusterTypesProvider, resourceReferenceProvider, errors, warnings);

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
            TaskValidator.validateTaskStructure(task, errors, warnings);

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

        forEachNestedTask(
            parametersJsonNode,
            nestedTaskJsonNode -> discoverNestedTask(
                nestedTaskJsonNode, taskDefinitionMap, taskOutputMap, clusterTypesMap, taskJsonNodes,
                taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, errors, warnings));
    }

    private static void discoverNestedTask(
        JsonNode nestedTaskJsonNode, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, @Nullable PropertyInfo> taskOutputMap, Map<String, List<String>> clusterTypesMap,
        List<JsonNode> taskJsonNodes, TaskDefinitionProvider taskDefinitionProvider,
        TaskOutputProvider taskOutputProvider, ClusterTypesProvider clusterTypesProvider, StringBuilder errors,
        StringBuilder warnings) {

        if (!nestedTaskJsonNode.isObject() || !nestedTaskJsonNode.has("type")) {
            return;
        }

        String type = getType(
            taskDefinitionMap, taskOutputMap, taskJsonNodes, taskDefinitionProvider, taskOutputProvider, errors,
            warnings, nestedTaskJsonNode);

        if (nestedTaskJsonNode.has("clusterElements")) {
            List<String> clusterElementTypes = clusterTypesProvider.getClusterElementTypes(type);

            if (clusterElementTypes != null) {
                clusterTypesMap.putIfAbsent(type, clusterElementTypes);
            }

            processClusterElements(
                nestedTaskJsonNode, taskDefinitionMap, taskOutputMap, clusterTypesMap, taskDefinitionProvider,
                taskOutputProvider, clusterTypesProvider, warnings);
        }

        if (!nestedTaskJsonNode.has("parameters")) {
            return;
        }

        List<PropertyInfo> nestedTaskDefinition = taskDefinitionMap.get(type);

        if (nestedTaskDefinition != null && !nestedTaskDefinition.isEmpty()) {
            extractNestedTasksFromParameters(
                nestedTaskJsonNode.get("parameters"), nestedTaskDefinition, taskDefinitionMap, taskOutputMap,
                taskJsonNodes, taskDefinitionProvider, taskOutputProvider, errors, warnings);
        } else {
            // Recursively discover more nested tasks
            discoverNestedTasksFromJsonStructure(
                nestedTaskJsonNode.get("parameters"), taskDefinitionMap, taskOutputMap, clusterTypesMap,
                taskJsonNodes, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, errors, warnings);
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
            if (!PropertyUtils.isNestedTaskProperty(propertyInfo)) {
                continue;
            }

            JsonNode jsonNode = parametersJsonNode.get(propertyInfo.name());

            if (jsonNode == null) {
                continue;
            }

            if (jsonNode.isArray()) {
                for (int i = 0; i < jsonNode.size(); i++) {
                    extractNestedTask(
                        jsonNode.get(i), taskDefinitionMap, taskOutputMap, taskJsonNodes, taskDefinitionProvider,
                        taskOutputProvider, errors, warnings);
                }
            } else if (jsonNode.isObject()) {
                extractNestedTask(
                    jsonNode, taskDefinitionMap, taskOutputMap, taskJsonNodes, taskDefinitionProvider,
                    taskOutputProvider, errors, warnings);
            }
        }
    }

    private static void extractNestedTask(
        JsonNode nestedTaskJsonNode, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, @Nullable PropertyInfo> taskOutputMap, List<JsonNode> taskJsonNodes,
        TaskDefinitionProvider taskDefinitionProvider, TaskOutputProvider taskOutputProvider, StringBuilder errors,
        StringBuilder warnings) {

        if (!nestedTaskJsonNode.has("type")) {
            return;
        }

        String type = getType(
            taskDefinitionMap, taskOutputMap, taskJsonNodes, taskDefinitionProvider, taskOutputProvider, errors,
            warnings, nestedTaskJsonNode);

        if (!nestedTaskJsonNode.has("parameters")) {
            return;
        }

        List<PropertyInfo> nestedTaskDefinition = taskDefinitionMap.get(type);

        if (nestedTaskDefinition != null) {
            extractNestedTasksFromParameters(
                nestedTaskJsonNode.get("parameters"), nestedTaskDefinition, taskDefinitionMap, taskOutputMap,
                taskJsonNodes, taskDefinitionProvider, taskOutputProvider, errors, warnings);
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

        TaskValidator.validateTaskStructure(nestedTaskJsonNode.toString(), errors, warnings);

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
            } else if (clusterElementJsonNode.isObject() && clusterElementJsonNode.has("type")) {
                JsonNode typeJsonNode = clusterElementJsonNode.get("type");

                String type = typeJsonNode.asString();

                taskDefinitionMap.putIfAbsent(type, taskDefinitionProvider.getTaskProperties(type, "clusterElement"));
                taskOutputMap.putIfAbsent(
                    type, taskOutputProvider.getTaskOutputProperty(type, "clusterElement", warnings));

                if (!clusterElementJsonNode.has("clusterElements")) {
                    continue;
                }

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

    private static void processInputs(
        Map<String, @Nullable PropertyInfo> taskOutputMap, JsonNode workflowJsonNode, List<JsonNode> inputJsonNodes,
        StringBuilder errors, StringBuilder warnings) {

        JsonNode inputsJsonNode = workflowJsonNode.get("inputs");

        if (inputsJsonNode == null) {
            return;
        }

        if (!inputsJsonNode.isArray()) {
            StringUtils.appendWithNewline("Field 'inputs' must be an array", errors);

            return;
        }

        for (JsonNode inputJsonNode : inputsJsonNode) {
            if (!inputJsonNode.isObject()) {
                StringUtils.appendWithNewline("Input must be an object", errors);

                continue;
            }

            inputJsonNodes.add(inputJsonNode);

            validateInputFields(inputJsonNode, errors, warnings);

            JsonNode typeJsonNode = inputJsonNode.get("type");
            JsonNode nameJsonNode = inputJsonNode.get("name");

            if (typeJsonNode != null && typeJsonNode.isString() && nameJsonNode != null && nameJsonNode.isString()) {
                String type = typeJsonNode.asString();
                String name = nameJsonNode.asString();

                JsonNode requiredJsonNode = inputJsonNode.get("required");
                boolean required = requiredJsonNode != null && requiredJsonNode.isBoolean() &&
                    requiredJsonNode.asBoolean();

                taskOutputMap.putIfAbsent(name, new PropertyInfo(name, type.toUpperCase(), null, required, false,
                    null, null));
            }
        }
    }

    private static void validateInputFields(
        JsonNode inputJsonNode, StringBuilder errors, StringBuilder warnings) {

        String name = "";

        if (inputJsonNode.has("name")) {
            JsonNode nameJsonNode = inputJsonNode.get("name");

            if (nameJsonNode.isString()) {
                name = nameJsonNode.asString();
            }
        }

        String prefix = name.isEmpty() ? "" : "[" + name + "] ";

        FieldValidator.validateRequiredStringField(inputJsonNode, "name", errors);
        FieldValidator.validateOptionalStringField(inputJsonNode, "label", errors, warnings);

        if (!name.isEmpty() && !isValidInputName(name)) {
            StringUtils.appendWithNewline(
                prefix +
                    "Field 'name' must start with a letter or underscore and contain only letters, digits and " +
                    "underscores",
                errors);
        }

        if (!inputJsonNode.has("type")) {
            StringUtils.appendWithNewline(prefix + "Missing required field: type", errors);
        } else {
            JsonNode typeJsonNode = inputJsonNode.get("type");

            if (!typeJsonNode.isString()) {
                StringUtils.appendWithNewline(prefix + "Field 'type' must be a string", errors);
            } else if (!VALID_INPUT_TYPES.contains(typeJsonNode.asString())) {
                StringUtils.appendWithNewline(
                    prefix + "Field 'type' must be one of: " + String.join(", ", VALID_INPUT_TYPES), errors);
            }
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

    public static List<String> getInvalidInputNames(String workflow) {
        try {
            return getInvalidInputNames(com.bytechef.commons.util.JsonUtils.readTree(workflow));
        } catch (Exception e) {
            return List.of();
        }
    }

    static List<String> getInvalidInputNames(JsonNode workflowJsonNode) {
        List<String> invalidInputNames = new ArrayList<>();

        JsonNode inputsJsonNode = workflowJsonNode.get("inputs");

        if (inputsJsonNode == null || !inputsJsonNode.isArray()) {
            return invalidInputNames;
        }

        for (JsonNode inputJsonNode : inputsJsonNode) {
            if (!inputJsonNode.isObject() || !inputJsonNode.has("name")) {
                continue;
            }

            JsonNode nameJsonNode = inputJsonNode.get("name");

            if (!nameJsonNode.isString()) {
                continue;
            }

            String name = nameJsonNode.asString();

            if (!name.isBlank() && !isValidInputName(name)) {
                invalidInputNames.add(name);
            }
        }

        return invalidInputNames;
    }

    private static boolean isValidInputName(String name) {
        return INPUT_NAME_PATTERN.matcher(name)
            .matches();
    }

    public static List<String> getDuplicateNodeNames(String workflow) {
        try {
            return getDuplicateNodeNames(com.bytechef.commons.util.JsonUtils.readTree(workflow));
        } catch (Exception e) {
            return List.of();
        }
    }

    static List<String> getDuplicateNodeNames(JsonNode workflowJsonNode) {
        List<String> nodeNames = new ArrayList<>();

        JsonNode inputsJsonNode = workflowJsonNode.get("inputs");

        if (inputsJsonNode != null && inputsJsonNode.isArray()) {
            for (JsonNode inputJsonNode : inputsJsonNode) {
                if (inputJsonNode.isObject()) {
                    collectNodeName(inputJsonNode, nodeNames);
                }
            }
        }

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
            collectTaskName(taskJsonNode, nodeNames);
        }
    }

    private static void collectTaskName(JsonNode taskJsonNode, List<String> nodeNames) {
        if (!taskJsonNode.isObject()) {
            return;
        }

        collectNodeName(taskJsonNode, nodeNames);

        JsonNode parametersJsonNode = taskJsonNode.get("parameters");

        if (parametersJsonNode != null && parametersJsonNode.isObject()) {
            collectNestedTaskNames(parametersJsonNode, nodeNames);
        }
    }

    /**
     * Collects task names from the task-dispatcher nesting shapes: condition caseTrue/caseFalse, branch default/cases,
     * parallel/on-error task arrays, loop/each/map iteratee (array or single object) and fork-join branches.
     */
    private static void collectNestedTaskNames(JsonNode parametersJsonNode, List<String> nodeNames) {
        forEachNestedTask(parametersJsonNode, taskJsonNode -> collectTaskName(taskJsonNode, nodeNames));
    }

    /**
     * Passes each task nested directly inside the given parameters to the consumer, covering the task-dispatcher
     * nesting shapes: condition caseTrue/caseFalse, branch default/cases, parallel/on-error task arrays, loop/each/map
     * iteratee (array or single object) and fork-join branches. Tasks nested deeper are left to the consumer.
     */
    static void forEachNestedTask(@Nullable JsonNode parametersJsonNode, Consumer<JsonNode> consumer) {
        if (parametersJsonNode == null || !parametersJsonNode.isObject()) {
            return;
        }

        for (String key : NESTED_TASK_ARRAY_PROPERTIES) {
            acceptTasks(parametersJsonNode.get(key), consumer);
        }

        JsonNode iterateeJsonNode = parametersJsonNode.get("iteratee");

        if (iterateeJsonNode != null) {
            if (iterateeJsonNode.isArray()) {
                acceptTasks(iterateeJsonNode, consumer);
            } else if (iterateeJsonNode.isObject()) {
                consumer.accept(iterateeJsonNode);
            }
        }

        JsonNode casesJsonNode = parametersJsonNode.get("cases");

        if (casesJsonNode != null && casesJsonNode.isArray()) {
            for (JsonNode caseJsonNode : casesJsonNode) {
                acceptTasks(caseJsonNode.get("tasks"), consumer);
            }
        }

        JsonNode branchesJsonNode = parametersJsonNode.get("branches");

        if (branchesJsonNode != null && branchesJsonNode.isArray()) {
            for (JsonNode branchJsonNode : branchesJsonNode) {
                acceptTasks(branchJsonNode, consumer);
            }
        }
    }

    private static void acceptTasks(@Nullable JsonNode tasksJsonNode, Consumer<JsonNode> consumer) {
        if (tasksJsonNode == null || !tasksJsonNode.isArray()) {
            return;
        }

        for (JsonNode taskJsonNode : tasksJsonNode) {
            if (taskJsonNode.isObject()) {
                consumer.accept(taskJsonNode);
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
     * @param warnings StringBuilder to collect validation warnings
     */
    static void validateWorkflowStructure(String workflow, StringBuilder errors, StringBuilder warnings) {
        JsonNode workflowJsonNode = JsonNodeUtils.parseJsonWithErrorHandling(workflow, errors);

        if (workflowJsonNode == null) {
            return;
        }

        if (!JsonNodeUtils.appendErrorNodeIsObject(workflowJsonNode, "Workflow", errors)) {
            return;
        }

        FieldValidator.validateOptionalStringField(workflowJsonNode, "label", errors, warnings);
        FieldValidator.validateRequiredStringField(workflowJsonNode, "description", errors);
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

        @Nullable
        default List<String> getRequiredClusterElementTypes(String taskType) {
            return getClusterElementTypes(taskType);
        }
    }

    @FunctionalInterface
    public interface ResourceReferenceProvider {

        @Nullable
        String findProblem(String resourceType, String reference);
    }

    public static final ResourceReferenceProvider NO_RESOURCE_REFERENCE_PROVIDER = (resourceType, reference) -> null;
}
