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

import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Builder pattern for managing validation context and parameters. Reduces parameter passing complexity and improves
 * code maintainability.
 *
 * @author Marko Kriskovic
 */
class ValidationContext {

    private final List<JsonNode> taskJsonNodes;
    private final List<JsonNode> inputJsonNodes;
    private final Map<String, List<PropertyInfo>> taskDefinitionMap;
    private final Map<String, PropertyInfo> taskOutputMap;
    private final Map<String, PropertyInfo> nodeOutputMap;
    private final Map<String, PropertyInfo> nodeVariableOutputMap;
    private final Map<String, List<String>> clusterTypesProviderMap;
    private final WorkflowValidator.@Nullable ClusterTypesProvider clusterTypesProvider;
    private final WorkflowValidator.ResourceReferenceProvider resourceReferenceProvider;
    private final StringBuilder errors;
    private final StringBuilder warnings;
    private final List<String> taskNames = new ArrayList<>();
    private final Map<String, String> taskNameToTypeMap = new HashMap<>();
    private final Map<String, JsonNode> allTasksMap = new HashMap<>();

    private ValidationContext(
        List<JsonNode> taskJsonNodes, List<JsonNode> inputJsonNodes,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        Map<String, PropertyInfo> nodeOutputMap, Map<String, PropertyInfo> nodeVariableOutputMap,
        Map<String, List<String>> clusterTypesProviderMap,
        WorkflowValidator.@Nullable ClusterTypesProvider clusterTypesProvider,
        WorkflowValidator.ResourceReferenceProvider resourceReferenceProvider, StringBuilder errors,
        StringBuilder warnings) {

        this.taskJsonNodes = taskJsonNodes;
        this.inputJsonNodes = inputJsonNodes;
        this.taskDefinitionMap = taskDefinitionMap;
        this.taskOutputMap = taskOutputMap;
        this.nodeOutputMap = nodeOutputMap;
        this.nodeVariableOutputMap = nodeVariableOutputMap;
        this.clusterTypesProviderMap = clusterTypesProviderMap;
        this.clusterTypesProvider = clusterTypesProvider;
        this.resourceReferenceProvider = resourceReferenceProvider;
        this.errors = errors;
        this.warnings = warnings;

        buildTaskMaps();
    }

    public static ValidationContext of(
        List<JsonNode> taskJsonNodes, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutputMap, Map<String, List<String>> clusterTypesProviderMap,
        StringBuilder errors, StringBuilder warnings) {

        return of(taskJsonNodes, taskDefinitionMap, taskOutputMap, Map.of(), clusterTypesProviderMap, errors, warnings);
    }

    public static ValidationContext of(
        List<JsonNode> taskJsonNodes, Map<String, List<PropertyInfo>> taskDefinitionMap,
        Map<String, PropertyInfo> taskOutputMap, Map<String, PropertyInfo> nodeOutputMap,
        Map<String, List<String>> clusterTypesProviderMap, StringBuilder errors, StringBuilder warnings) {

        return of(
            taskJsonNodes, List.of(), taskDefinitionMap, taskOutputMap, nodeOutputMap, clusterTypesProviderMap,
            WorkflowValidator.NO_RESOURCE_REFERENCE_PROVIDER, errors, warnings);
    }

    public static ValidationContext of(
        List<JsonNode> taskJsonNodes, List<JsonNode> inputJsonNodes,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        Map<String, PropertyInfo> nodeOutputMap, Map<String, List<String>> clusterTypesProviderMap,
        WorkflowValidator.ResourceReferenceProvider resourceReferenceProvider, StringBuilder errors,
        StringBuilder warnings) {

        return of(
            taskJsonNodes, inputJsonNodes, taskDefinitionMap, taskOutputMap, nodeOutputMap, clusterTypesProviderMap,
            null, resourceReferenceProvider, errors, warnings);
    }

    public static ValidationContext of(
        List<JsonNode> taskJsonNodes, List<JsonNode> inputJsonNodes,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        Map<String, PropertyInfo> nodeOutputMap, Map<String, List<String>> clusterTypesProviderMap,
        WorkflowValidator.@Nullable ClusterTypesProvider clusterTypesProvider,
        WorkflowValidator.ResourceReferenceProvider resourceReferenceProvider, StringBuilder errors,
        StringBuilder warnings) {

        return of(
            taskJsonNodes, inputJsonNodes, taskDefinitionMap, taskOutputMap, nodeOutputMap, Map.of(),
            clusterTypesProviderMap, clusterTypesProvider, resourceReferenceProvider, errors, warnings);
    }

    public static ValidationContext of(
        List<JsonNode> taskJsonNodes, List<JsonNode> inputJsonNodes,
        Map<String, List<PropertyInfo>> taskDefinitionMap, Map<String, PropertyInfo> taskOutputMap,
        Map<String, PropertyInfo> nodeOutputMap, Map<String, PropertyInfo> nodeVariableOutputMap,
        Map<String, List<String>> clusterTypesProviderMap,
        WorkflowValidator.@Nullable ClusterTypesProvider clusterTypesProvider,
        WorkflowValidator.ResourceReferenceProvider resourceReferenceProvider, StringBuilder errors,
        StringBuilder warnings) {

        return new ValidationContext(
            taskJsonNodes, inputJsonNodes, taskDefinitionMap, taskOutputMap, nodeOutputMap, nodeVariableOutputMap,
            clusterTypesProviderMap, clusterTypesProvider, resourceReferenceProvider, errors, warnings);
    }

    private void buildTaskMaps() {
        for (JsonNode inputJsonNode : inputJsonNodes) {
            addNodeNameAndType(inputJsonNode);
        }

        for (JsonNode taskJsonNode : taskJsonNodes) {
            addNodeNameAndType(taskJsonNode);
        }
    }

    private void addNodeNameAndType(JsonNode nodeJsonNode) {
        if (nodeJsonNode.has("name")) {
            JsonNode nameJsonNode = nodeJsonNode.get("name");

            String nodeName = nameJsonNode.asText();

            taskNames.add(nodeName);
            allTasksMap.put(nodeName, nodeJsonNode);

            if (nodeJsonNode.has("type")) {
                JsonNode typeJsonNode = nodeJsonNode.get("type");

                taskNameToTypeMap.put(nodeName, typeJsonNode.asText());
            }
        }
    }

    public List<JsonNode> getTasks() {
        return new ArrayList<>(taskJsonNodes);
    }

    public Map<String, List<PropertyInfo>> getTaskDefinitions() {
        return new HashMap<>(taskDefinitionMap);
    }

    public Map<String, PropertyInfo> getTaskOutputs() {
        return new HashMap<>(taskOutputMap);
    }

    public Map<String, PropertyInfo> getNodeOutputMap() {
        return nodeOutputMap;
    }

    public Map<String, PropertyInfo> getNodeVariableOutputMap() {
        return nodeVariableOutputMap;
    }

    public StringBuilder getErrors() {
        return errors;
    }

    public StringBuilder getWarnings() {
        return warnings;
    }

    public List<String> getTaskNames() {
        return new ArrayList<>(taskNames);
    }

    public Map<String, String> getTaskNameToTypeMap() {
        return taskNameToTypeMap;
    }

    public Map<String, JsonNode> getAllTasksMap() {
        return allTasksMap;
    }

    public Map<String, List<String>> getClusterTypesProviderMap() {
        return clusterTypesProviderMap;
    }

    public List<String> getRequiredClusterElementTypes(String taskType) {
        List<String> clusterElementTypes = clusterTypesProviderMap.getOrDefault(taskType, List.of());

        if (clusterTypesProvider == null) {
            return clusterElementTypes;
        }

        List<String> requiredClusterElementTypes = clusterTypesProvider.getRequiredClusterElementTypes(taskType);

        return requiredClusterElementTypes == null ? clusterElementTypes : requiredClusterElementTypes;
    }

    public WorkflowValidator.ResourceReferenceProvider getResourceReferenceProvider() {
        return resourceReferenceProvider;
    }
}
