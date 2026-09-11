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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.definition.BaseProperty.ResourceType;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.FileEntryProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

/**
 * @author Marko Kriskovic
 */
@Service
public class WorkflowValidatorFacadeImpl implements WorkflowValidatorFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkflowValidatorFacadeImpl.class);

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ActionDefinitionService actionDefinitionService;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final Map<ResourceType, ResourceReferenceResolver> resourceReferenceResolverMap;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    private final WorkflowValidator.ClusterTypesProvider clusterTypesProvider =
        new WorkflowValidator.ClusterTypesProvider() {

            @Override
            @Nullable
            public List<String> getClusterElementTypes(String taskType) {
                return getClusterElementTypeKeys(taskType, false);
            }

            @Override
            @Nullable
            public List<String> getRequiredClusterElementTypes(String taskType) {
                return getClusterElementTypeKeys(taskType, true);
            }
        };

    @SuppressFBWarnings("EI2")
    public WorkflowValidatorFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, ActionDefinitionService actionDefinitionService,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        List<ResourceReferenceResolver> resourceReferenceResolvers) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.actionDefinitionService = actionDefinitionService;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.resourceReferenceResolverMap = resourceReferenceResolvers.stream()
            .collect(Collectors.toMap(ResourceReferenceResolver::getResourceType, Function.identity()));
    }

    @Override
    public WorkflowValidationResult validateWorkflow(String workflow) {
        return validateWorkflow(workflow, Environment.DEVELOPMENT.ordinal());
    }

    @Override
    public WorkflowValidationResult validateWorkflow(String workflow, long environmentId) {
        return validateWorkflow(workflow, null, environmentId);
    }

    @Override
    public WorkflowValidationResult validateWorkflow(
        String workflow, @Nullable String workflowId, long environmentId) {

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        NodeOutputMaps nodeOutputMaps = buildNodeOutputMaps(workflow, workflowId, environmentId);

        WorkflowValidator.validateWorkflow(
            workflow, this::getTaskProperties, this::getTaskOutputProperty, clusterTypesProvider,
            createResourceReferenceProvider(resourceReferenceResolverMap, environmentId), new HashMap<>(),
            new HashMap<>(), nodeOutputMaps.outputMap(), nodeOutputMaps.variableOutputMap(), new HashMap<>(), errors,
            warnings);

        if (workflowId != null) {
            appendMissingConnections(workflow, workflowId, environmentId, errors);
        }

        String errorsString = errors.toString();

        List<String> errorList = Arrays.stream(errorsString.split("\n"))
            .filter(line -> !line.isBlank())
            .toList();

        String warningsString = warnings.toString();

        List<String> warningList = Arrays.stream(warningsString.split("\n"))
            .filter(line -> !line.isBlank())
            .toList();

        return new WorkflowValidationResult(
            errorList, warningList, NodeValidationIssueParser.parse(errorList, warningList));
    }

    @Override
    public WorkflowValidationResult validateWorkflowById(String workflowId) {
        return validateWorkflowById(workflowId, Environment.DEVELOPMENT.ordinal());
    }

    @Override
    public WorkflowValidationResult validateWorkflowById(String workflowId, long environmentId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return validateWorkflow(workflow.getDefinition(), workflowId, environmentId);
    }

    static WorkflowValidator.ResourceReferenceProvider createResourceReferenceProvider(
        Map<ResourceType, ResourceReferenceResolver> resourceReferenceResolverMap, long environmentId) {

        return (resourceType, reference) -> {
            ResourceReferenceResolver resourceReferenceResolver =
                resourceReferenceResolverMap.get(ResourceType.valueOf(resourceType));

            return resourceReferenceResolver == null
                ? null : resourceReferenceResolver.findProblem(reference, environmentId);
        };
    }

    @Override
    public List<String> getDuplicateNodeNames(String workflow) {
        return WorkflowValidator.getDuplicateNodeNames(workflow);
    }

    @Override
    public List<String> getInvalidInputNames(String workflow) {
        return WorkflowValidator.getInvalidInputNames(workflow);
    }

    private List<PropertyInfo> getTaskProperties(String taskType, String kind) {
        try {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(taskType);

            if ("trigger".equals(kind)) {
                TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                    workflowNodeType.name(), workflowNodeType.version(),
                    Objects.requireNonNull(workflowNodeType.operation()));

                return triggerDefinition.getProperties()
                    .stream()
                    .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                    .toList();
            } else if ("clusterElement".equals(kind)) {
                com.bytechef.platform.component.domain.ClusterElementDefinition clusterElementDefinition =
                    clusterElementDefinitionService.getClusterElementDefinition(
                        workflowNodeType.name(), workflowNodeType.version(),
                        Objects.requireNonNull(workflowNodeType.operation()));

                return clusterElementDefinition.getProperties()
                    .stream()
                    .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                    .toList();
            } else if (workflowNodeType.operation() != null) {
                ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                    workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());

                return actionDefinition.getProperties()
                    .stream()
                    .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                    .toList();
            } else {
                TaskDispatcherDefinition taskDispatcherDefinition =
                    taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                        workflowNodeType.name(), workflowNodeType.version());

                return Stream.concat(
                    CollectionUtils.stream(taskDispatcherDefinition.getProperties()),
                    CollectionUtils.stream(taskDispatcherDefinition.getTaskProperties()))
                    .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                    .toList();
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    @Nullable
    private PropertyInfo getTaskOutputProperty(String taskType, String kind, StringBuilder warnings) {
        try {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(taskType);

            OutputResponse outputResponse;

            if ("trigger".equals(kind)) {
                TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                    workflowNodeType.name(), workflowNodeType.version(),
                    Objects.requireNonNull(workflowNodeType.operation()));

                outputResponse = triggerDefinition.getOutputResponse();
            } else if ("clusterElement".equals(kind)) {
                com.bytechef.platform.component.domain.ClusterElementDefinition clusterElementDefinition =
                    clusterElementDefinitionService.getClusterElementDefinition(
                        workflowNodeType.name(), workflowNodeType.version(),
                        Objects.requireNonNull(workflowNodeType.operation()));

                outputResponse = clusterElementDefinition.getOutputResponse();
            } else if (workflowNodeType.operation() != null) {
                ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                    workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());

                outputResponse = actionDefinition.getOutputResponse();
            } else {
                TaskDispatcherDefinition taskDispatcherDefinition =
                    taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                        workflowNodeType.name(), workflowNodeType.version());

                outputResponse = taskDispatcherDefinition.getOutputResponse();
            }

            if (outputResponse != null && outputResponse.outputSchema() != null) {
                return toPropertyInfo(outputResponse.outputSchema());
            }

            return null;
        } catch (Exception e) {
            warnings.append("Could not retrieve output for task type: ")
                .append(taskType)
                .append("\n");

            return null;
        }
    }

    private void
        appendMissingConnections(String workflow, String workflowId, long environmentId, StringBuilder errors) {
        try {
            JsonNode workflowJsonNode = JsonUtils.readTree(workflow);

            appendMissingNodeConnections(workflowJsonNode.get("triggers"), workflowId, environmentId, errors);
            appendMissingNodeConnections(workflowJsonNode.get("tasks"), workflowId, environmentId, errors);
        } catch (Exception e) {
            log.debug("Failed to check the connections of workflow '{}'", workflowId, e);
        }
    }

    private void appendMissingNodeConnections(
        @Nullable JsonNode nodesJsonNode, String workflowId, long environmentId, StringBuilder errors) {

        if (nodesJsonNode == null) {
            return;
        }

        if (!nodesJsonNode.isArray()) {
            appendMissingNodeConnection(nodesJsonNode, workflowId, environmentId, errors);

            return;
        }

        for (JsonNode nodeJsonNode : nodesJsonNode) {
            appendMissingNodeConnection(nodeJsonNode, workflowId, environmentId, errors);
        }
    }

    private void appendMissingNodeConnection(
        JsonNode nodeJsonNode, String workflowId, long environmentId, StringBuilder errors) {

        if (!nodeJsonNode.isObject() || !nodeJsonNode.has("name") || !nodeJsonNode.has("type")) {
            return;
        }

        String name = nodeJsonNode.get("name")
            .asString();
        String type = nodeJsonNode.get("type")
            .asString();

        List<WorkflowTestConfigurationConnection> connections = getTestConfigurationConnections(
            workflowId, name, environmentId);

        String componentTitle = getTitleOfComponentRequiringConnection(type);

        if (componentTitle != null && connections.isEmpty()) {
            StringUtils.appendWithNewline(
                "[" + name + "] " + ValidationErrorUtils.missingConnection(componentTitle), errors);
        }

        appendMissingClusterElementConnections(nodeJsonNode.get("clusterElements"), connections, errors);

        JsonNode parametersJsonNode = nodeJsonNode.get("parameters");

        if (parametersJsonNode == null || !parametersJsonNode.isObject()) {
            return;
        }

        for (String propertyName : WorkflowValidator.NESTED_TASK_PROPERTIES) {
            appendMissingNodeConnections(parametersJsonNode.get(propertyName), workflowId, environmentId, errors);
        }
    }

    private void appendMissingClusterElementConnections(
        @Nullable JsonNode clusterElementsJsonNode, List<WorkflowTestConfigurationConnection> connections,
        StringBuilder errors) {

        if (clusterElementsJsonNode == null || !clusterElementsJsonNode.isObject()) {
            return;
        }

        for (JsonNode clusterElementJsonNode : clusterElementsJsonNode.values()) {
            if (clusterElementJsonNode.isArray()) {
                for (JsonNode elementJsonNode : clusterElementJsonNode) {
                    appendMissingClusterElementConnection(elementJsonNode, connections, errors);
                }
            } else {
                appendMissingClusterElementConnection(clusterElementJsonNode, connections, errors);
            }
        }
    }

    private void appendMissingClusterElementConnection(
        JsonNode elementJsonNode, List<WorkflowTestConfigurationConnection> connections, StringBuilder errors) {

        if (!elementJsonNode.isObject() || !elementJsonNode.has("name") || !elementJsonNode.has("type")) {
            return;
        }

        String elementName = elementJsonNode.get("name")
            .asString();
        String componentTitle = getTitleOfComponentRequiringConnection(
            elementJsonNode.get("type")
                .asString());

        boolean connected = connections.stream()
            .anyMatch(connection -> Objects.equals(connection.getWorkflowConnectionKey(), elementName));

        if (componentTitle != null && !connected) {
            StringUtils.appendWithNewline(
                "[" + elementName + "] " + ValidationErrorUtils.missingConnection(componentTitle), errors);
        }
    }

    private List<WorkflowTestConfigurationConnection> getTestConfigurationConnections(
        String workflowId, String workflowNodeName, long environmentId) {

        try {
            return workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                workflowId, workflowNodeName, environmentId);
        } catch (Exception e) {
            log.debug("Failed to read the test connections of node '{}'", workflowNodeName, e);

            return List.of();
        }
    }

    private @Nullable String getTitleOfComponentRequiringConnection(String type) {
        try {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

            if (workflowNodeType.operation() == null) {
                return null;
            }

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.name(), workflowNodeType.version());

            if (!componentDefinition.isConnectionRequired()) {
                return null;
            }

            String title = componentDefinition.getTitle();

            return title == null ? componentDefinition.getName() : title;
        } catch (Exception e) {
            log.debug("Failed to resolve the component of node type '{}'", type, e);

            return null;
        }
    }

    private NodeOutputMaps buildNodeOutputMaps(String workflow, @Nullable String workflowId, long environmentId) {
        Map<String, PropertyInfo> nodeOutputMap = new HashMap<>();
        Map<String, PropertyInfo> nodeVariableOutputMap = new HashMap<>();

        try {
            JsonNode workflowJsonNode = JsonUtils.readTree(workflow);

            addNodeOutputs(
                workflowJsonNode.get("triggers"), true, workflowId, environmentId, nodeOutputMap,
                nodeVariableOutputMap);
            addNodeOutputs(
                workflowJsonNode.get("tasks"), false, workflowId, environmentId, nodeOutputMap,
                nodeVariableOutputMap);
        } catch (Exception e) {
            log.debug("Failed to build config-aware node output map; falling back to static validation", e);
        }

        return new NodeOutputMaps(nodeOutputMap, nodeVariableOutputMap);
    }

    private void addNodeOutputs(
        @Nullable JsonNode nodesJsonNode, boolean trigger, @Nullable String workflowId, long environmentId,
        Map<String, PropertyInfo> outputMap, Map<String, PropertyInfo> variableOutputMap) {

        if (nodesJsonNode == null || !nodesJsonNode.isArray()) {
            return;
        }

        for (JsonNode nodeJsonNode : nodesJsonNode) {
            addNodeOutput(nodeJsonNode, trigger, workflowId, environmentId, outputMap, variableOutputMap);
        }
    }

    private void addNodeOutput(
        JsonNode nodeJsonNode, boolean trigger, @Nullable String workflowId, long environmentId,
        Map<String, PropertyInfo> outputMap, Map<String, PropertyInfo> variableOutputMap) {

        if (!nodeJsonNode.isObject() || !nodeJsonNode.has("name") || !nodeJsonNode.has("type")) {
            return;
        }

        String name = nodeJsonNode.get("name")
            .asString();
        String type = nodeJsonNode.get("type")
            .asString();
        Map<String, ?> inputParameters = toInputParameters(nodeJsonNode.get("parameters"));

        PropertyInfo outputProperty = resolveTestOutput(name, type, workflowId, environmentId);

        if (outputProperty == null) {
            outputProperty = resolveDynamicOutput(type, trigger, inputParameters, workflowId, environmentId);
        }

        if (outputProperty != null) {
            outputMap.put(name, outputProperty);
        }

        PropertyInfo variableOutputProperty = resolveVariableOutput(type, trigger, inputParameters);

        if (variableOutputProperty != null) {
            variableOutputMap.put(name, variableOutputProperty);
        }

        if (!trigger) {
            addNestedNodeOutputs(nodeJsonNode, workflowId, environmentId, outputMap, variableOutputMap);
        }
    }

    private void addNestedNodeOutputs(
        JsonNode nodeJsonNode, @Nullable String workflowId, long environmentId, Map<String, PropertyInfo> outputMap,
        Map<String, PropertyInfo> variableOutputMap) {

        JsonNode parametersJsonNode = nodeJsonNode.get("parameters");

        if (parametersJsonNode == null || !parametersJsonNode.isObject()) {
            return;
        }

        for (String propertyName : WorkflowValidator.NESTED_TASK_PROPERTIES) {
            JsonNode nestedJsonNode = parametersJsonNode.get(propertyName);

            if (nestedJsonNode == null) {
                continue;
            }

            if (nestedJsonNode.isArray()) {
                addNodeOutputs(nestedJsonNode, false, workflowId, environmentId, outputMap, variableOutputMap);
            } else {
                addNodeOutput(nestedJsonNode, false, workflowId, environmentId, outputMap, variableOutputMap);
            }
        }
    }

    private @Nullable PropertyInfo resolveTestOutput(
        String name, String type, @Nullable String workflowId, long environmentId) {

        if (workflowId == null) {
            return null;
        }

        try {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

            Class<? extends BaseProperty> typeClass = workflowNodeType.operation() == null
                ? com.bytechef.platform.workflow.task.dispatcher.domain.Property.class : Property.class;

            return workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(workflowId, name, environmentId)
                .map(workflowNodeTestOutput -> toOutputPropertyInfo(workflowNodeTestOutput.getOutput(typeClass)))
                .orElse(null);
        } catch (Exception e) {
            log.debug("Failed to read the test output of node '{}'", name, e);
        }

        return null;
    }

    private @Nullable PropertyInfo resolveDynamicOutput(
        String type, boolean trigger, Map<String, ?> inputParameters, @Nullable String workflowId,
        long environmentId) {

        try {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

            OutputResponse outputResponse;

            if (trigger) {
                outputResponse = triggerDefinitionFacade.executeOutput(
                    workflowNodeType.name(), workflowNodeType.version(),
                    Objects.requireNonNull(workflowNodeType.operation()), inputParameters, null);
            } else if (workflowNodeType.operation() != null) {
                outputResponse = actionDefinitionFacade.executeOutput(
                    workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), inputParameters,
                    Map.of());
            } else {
                Map<String, Object> outputInputParameters = new HashMap<>(inputParameters);

                outputInputParameters.put(MapDataSource.ENVIRONMENT_ID, environmentId);

                if (workflowId != null) {
                    outputInputParameters.put(MapDataSource.WORKFLOW_ID, workflowId);
                }

                outputResponse = taskDispatcherDefinitionService.executeOutput(
                    workflowNodeType.name(), workflowNodeType.version(), outputInputParameters);
            }

            return toOutputPropertyInfo(outputResponse);
        } catch (Exception e) {
            // Best-effort: connection-needed or otherwise unresolvable dynamic output → fall back to the static path.
            log.debug("Failed to resolve dynamic output for node type '{}'; falling back to the static path", type, e);
        }

        return null;
    }

    private @Nullable PropertyInfo resolveVariableOutput(String type, boolean trigger, Map<String, ?> inputParameters) {
        if (trigger) {
            return null;
        }

        try {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

            if (workflowNodeType.operation() != null) {
                return null;
            }

            OutputResponse outputResponse = taskDispatcherDefinitionService.executeVariableProperties(
                workflowNodeType.name(), workflowNodeType.version(), inputParameters);

            return toOutputPropertyInfo(outputResponse);
        } catch (Exception e) {
            log.debug("Failed to resolve variable output for node type '{}'", type, e);
        }

        return null;
    }

    private static @Nullable PropertyInfo toOutputPropertyInfo(@Nullable OutputResponse outputResponse) {
        if (outputResponse == null || outputResponse.outputSchema() == null) {
            return null;
        }

        PropertyInfo propertyInfo = toPropertyInfo(outputResponse.outputSchema());

        List<PropertyInfo> nestedPropertyInfos = propertyInfo.nestedProperties();

        if ("OBJECT".equals(propertyInfo.type()) && (nestedPropertyInfos == null || nestedPropertyInfos.isEmpty())) {
            return null;
        }

        return propertyInfo;
    }

    private static Map<String, ?> toInputParameters(@Nullable JsonNode parametersJsonNode) {
        if (parametersJsonNode == null || !parametersJsonNode.isObject()) {
            return Map.of();
        }

        return JsonUtils.read(JsonUtils.write(parametersJsonNode), new TypeReference<Map<String, Object>>() {});
    }

    @Nullable
    private List<String> getClusterElementTypeKeys(String taskType, boolean requiredOnly) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(taskType);

        if (workflowNodeType.operation() != null) {
            ComponentDefinition componentDefinition =
                componentDefinitionService.getComponentDefinition(workflowNodeType.name(), workflowNodeType.version());

            if (componentDefinition.isClusterElement()) {
                return toClusterElementTypeKeys(componentDefinition.getClusterElementTypes(), requiredOnly);
            }
        }

        return null;
    }

    static List<String> toClusterElementTypeKeys(
        List<ClusterElementDefinition.ClusterElementType> clusterElementTypes, boolean requiredOnly) {

        return clusterElementTypes.stream()
            .filter(clusterElementType -> !requiredOnly || clusterElementType.required())
            .map(ClusterElementDefinition.ClusterElementType::key)
            .toList();
    }

    private static PropertyInfo toPropertyInfo(BaseProperty baseProperty) {
        String type;
        List<PropertyInfo> nestedPropertyInfos = null;
        String resourceType = null;

        switch (baseProperty) {
            case ObjectProperty objectProperty -> {
                type = "OBJECT";
                List<? extends Property> properties = objectProperty.getProperties();

                if (properties != null && !properties.isEmpty()) {
                    nestedPropertyInfos = properties.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case ArrayProperty arrayProperty -> {
                type = "ARRAY";
                List<? extends Property> items = arrayProperty.getItems();

                if (items != null && !items.isEmpty()) {
                    nestedPropertyInfos = items.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case FileEntryProperty fileEntryProperty -> {
                type = "FILE_ENTRY";
                List<? extends Property> properties = fileEntryProperty.getProperties();

                if (properties != null && !properties.isEmpty()) {
                    nestedPropertyInfos = properties.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case Property property -> {
                com.bytechef.component.definition.Property.Type propertyType = property.getType();

                type = propertyType.name();

                ResourceType propertyResourceType = property.getResourceType();

                if (propertyResourceType != null) {
                    resourceType = propertyResourceType.name();
                }
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty arrayProperty -> {
                type = "ARRAY";
                List<? extends com.bytechef.platform.workflow.task.dispatcher.domain.Property> items = arrayProperty
                    .getItems();

                if (items != null && !items.isEmpty()) {
                    nestedPropertyInfos = items.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.FileEntryProperty fileEntryProperty -> {
                type = "FILE_ENTRY";
                List<? extends com.bytechef.platform.workflow.task.dispatcher.domain.ValueProperty<?>> properties =
                    fileEntryProperty.getProperties();

                if (properties != null && !properties.isEmpty()) {
                    nestedPropertyInfos = properties.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty objectProperty -> {
                type = "OBJECT";
                List<? extends com.bytechef.platform.workflow.task.dispatcher.domain.Property> properties =
                    objectProperty.getProperties();

                if (properties != null && !properties.isEmpty()) {
                    nestedPropertyInfos = properties.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.Property property -> {
                com.bytechef.platform.workflow.task.dispatcher.definition.Property.Type propertyType =
                    property.getType();

                type = propertyType.name();
            }
            default -> type = "OBJECT";
        }

        return new PropertyInfo(
            baseProperty.getName(), type, baseProperty.getDescription(), baseProperty.getRequired(),
            baseProperty.getExpressionEnabled(), baseProperty.getDisplayCondition(), null, nestedPropertyInfos,
            resourceType);
    }

    private record NodeOutputMaps(Map<String, PropertyInfo> outputMap, Map<String, PropertyInfo> variableOutputMap) {
    }
}
