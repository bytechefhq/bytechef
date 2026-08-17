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
import com.bytechef.platform.configuration.domain.HostedChatTriggers;
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

    static final List<String> VALID_INPUT_TYPES = List.of(
        "boolean", "date", "date_time", "time", "integer", "number", "string");

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
            validateChatOnlyApprovalChannels(workflowJsonNode, taskJsonNodes, warnings);
            validateWorkflowTasks(
                taskJsonNodes, inputJsonNodes, taskDefinitionMap, taskOutputMap, nodeOutputMap, clusterTypesMap,
                errors, warnings);
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
            taskJsonNodes, List.of(), taskDefinitionMap, taskOutput, nodeOutputMap, clusterTypesProviderMap, errors,
            warnings);
    }

    private static void validateWorkflowTasks(
        List<JsonNode> taskJsonNodes, List<JsonNode> inputJsonNodes, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutput, Map<String, PropertyInfo> nodeOutputMap,
        Map<String, List<String>> clusterTypesProviderMap, StringBuilder errors, StringBuilder warnings) {

        ValidationContext context = ValidationContext.of(
            taskJsonNodes, inputJsonNodes, taskDefinitionMap, taskOutput, nodeOutputMap, clusterTypesProviderMap,
            errors, warnings);

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

    /**
     * Warns about tasks whose approval requests can only be delivered to the chat channel while the workflow does not
     * start from a chat trigger. The chat channel publishes the approval card onto the run's live chat stream; a run
     * started by webhook or schedule has no chat listener, so the run pauses with no live card and is only resolvable
     * from the pending-approvals inbox or the hosted form. Covers both explicitly configured chat-only
     * {@code approvalChannels} and AI agent approval gates with chat-only or no configured channels, which default to
     * the chat channel.
     */
    private static void validateChatOnlyApprovalChannels(
        JsonNode workflowJsonNode, List<JsonNode> taskJsonNodes, StringBuilder warnings) {

        if (hasChatCapableTrigger(workflowJsonNode)) {
            return;
        }

        for (JsonNode taskJsonNode : taskJsonNodes) {
            if (!taskJsonNode.isObject() || !taskJsonNode.has("clusterElements")) {
                continue;
            }

            JsonNode clusterElementsJsonNode = taskJsonNode.get("clusterElements");

            if (!clusterElementsJsonNode.isObject()) {
                continue;
            }

            // Task-level channels belong to the standalone Approval action, which delivers nothing at all when
            // none are configured -- so only a configured-but-chat-only list is worth warning about here.
            List<String> approvalChannelTypes = getApprovalChannelTypes(clusterElementsJsonNode);

            if (!approvalChannelTypes.isEmpty() && isChatOnly(approvalChannelTypes)) {
                appendChatOnlyWarning("Task '" + getNodeName(taskJsonNode) + "'", false, warnings);
            }

            // An approval gate owns its own channels, and an empty list falls back to the chat channel, so an
            // unconfigured gate is exactly the case worth warning about.
            for (JsonNode approvalGateJsonNode : getApprovalGates(clusterElementsJsonNode)) {
                List<String> gateApprovalChannelTypes = getApprovalChannelTypes(
                    approvalGateJsonNode.get("clusterElements"));

                boolean implicitChatDefault = gateApprovalChannelTypes.isEmpty();

                if (implicitChatDefault || isChatOnly(gateApprovalChannelTypes)) {
                    appendChatOnlyWarning(
                        "Approval gate '" + getNodeName(approvalGateJsonNode) + "' in task '" +
                            getNodeName(taskJsonNode) + "'",
                        implicitChatDefault, warnings);
                }
            }
        }
    }

    private static boolean hasChatCapableTrigger(JsonNode workflowJsonNode) {
        JsonNode triggersJsonNode = workflowJsonNode.get("triggers");

        if (triggersJsonNode == null || !triggersJsonNode.isArray()) {
            return false;
        }

        for (JsonNode triggerJsonNode : triggersJsonNode) {
            if (!triggerJsonNode.isObject() || !triggerJsonNode.has("type")) {
                continue;
            }

            JsonNode typeJsonNode = triggerJsonNode.get("type");

            String type = typeJsonNode.asString();

            if (HostedChatTriggers.isChatTriggerType(type)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isChatOnly(List<String> approvalChannelTypes) {
        return approvalChannelTypes.stream()
            .allMatch(WorkflowValidator::isChatApprovalChannelType);
    }

    private static void appendChatOnlyWarning(String subject, boolean implicitChatDefault, StringBuilder warnings) {
        StringUtils.appendWithNewline(
            subject + " delivers approval requests only to the chat channel" +
                (implicitChatDefault ? " (the default when no approval channels are configured)" : "") +
                ", but the workflow does not start from a chat trigger. Runs started by webhook or " +
                "schedule will pause without a live approval card and are only resolvable from the " +
                "pending run approvals list or the hosted form. Configure a fallback approval channel " +
                "(Slack, email, approval task) or use a chat trigger.",
            warnings);
    }

    /**
     * Collects the approval gates among a node's TOOLS children. A gate's presence is what signals that approvals
     * happen at all, replacing the per-tool requiresApproval flag.
     */
    private static List<JsonNode> getApprovalGates(JsonNode clusterElementsJsonNode) {
        JsonNode toolsJsonNode = clusterElementsJsonNode.get("tools");

        if (toolsJsonNode == null || !toolsJsonNode.isArray()) {
            return List.of();
        }

        List<JsonNode> approvalGateJsonNodes = new ArrayList<>();

        for (JsonNode toolJsonNode : toolsJsonNode) {
            if (!toolJsonNode.isObject() || !toolJsonNode.has("type")) {
                continue;
            }

            JsonNode typeJsonNode = toolJsonNode.get("type");

            String[] typeParts = typeJsonNode.asString()
                .split("/");

            if (typeParts.length == 3 && "approvalGateTool".equals(typeParts[2])) {
                approvalGateJsonNodes.add(toolJsonNode);
            }
        }

        return approvalGateJsonNodes;
    }

    private static List<String> getApprovalChannelTypes(@Nullable JsonNode clusterElementsJsonNode) {
        if (clusterElementsJsonNode == null || !clusterElementsJsonNode.isObject()) {
            return List.of();
        }

        JsonNode approvalChannelsJsonNode = clusterElementsJsonNode.get("approvalChannels");

        if (approvalChannelsJsonNode == null || !approvalChannelsJsonNode.isArray()) {
            return List.of();
        }

        List<String> approvalChannelTypes = new ArrayList<>();

        for (JsonNode approvalChannelJsonNode : approvalChannelsJsonNode) {
            if (approvalChannelJsonNode.isObject() && approvalChannelJsonNode.has("type")) {
                JsonNode typeJsonNode = approvalChannelJsonNode.get("type");

                approvalChannelTypes.add(typeJsonNode.asString());
            }
        }

        return approvalChannelTypes;
    }

    private static boolean isChatApprovalChannelType(String type) {
        String[] typeParts = type.split("/");

        return typeParts.length == 3 && "chat".equals(typeParts[0]) && "chat".equals(typeParts[2]);
    }

    private static String getNodeName(JsonNode taskJsonNode) {
        JsonNode nameJsonNode = taskJsonNode.get("name");

        if (nameJsonNode != null && nameJsonNode.isString()) {
            return nameJsonNode.asString();
        }

        JsonNode typeJsonNode = taskJsonNode.get("type");

        return typeJsonNode != null ? typeJsonNode.asString() : "unknown";
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
    }
}
