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

package com.bytechef.platform.configuration.facade;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.DynamicPropertiesProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.OptionsDataSource;
import com.bytechef.platform.component.domain.OptionsDataSourceAware;
import com.bytechef.platform.component.domain.PropertiesDataSource;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.dto.DisplayConditionResultDTO;
import com.bytechef.platform.configuration.dto.ParameterResultDTO;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeParameterFacadeImpl implements WorkflowNodeParameterFacade {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowNodeParameterFacadeImpl.class);

    protected static final Pattern ARRAY_INDEX_VALUE_PATTERN = Pattern.compile("\\[(\\d+)]");
    private static final Pattern ARRAY_INDEXES_PATTERN =
        Pattern.compile("(\\b\\w+\\b)?((\\[index])+)");
    private static final String DYNAMIC_PROPERTY_TYPES = "dynamicPropertyTypes";
    private static final String FROM_AI = "fromAi";
    private static final String METADATA = "metadata";
    private static final String UI = "ui";

    private final ActionDefinitionService actionDefinitionService;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final Evaluator evaluator;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeParameterFacadeImpl(
        ActionDefinitionService actionDefinitionService,
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionService triggerDefinitionService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionService = actionDefinitionService;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.evaluator = evaluator;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public ParameterResultDTO deleteClusterElementParameter(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String parameterPath, long environmentId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        WorkflowNodeStructure workflowNodeStructure = getWorkflowNodeStructure(
            workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, definitionMap);

        Map<String, Object> metadataMap = getMetadataMap(
            workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, definitionMap);

        Map<String, ?> dynamicPropertyTypesMap = getDynamicPropertyTypesMap(metadataMap);

        String[] parameterPathParts = parameterPath.split("\\.");

        setParameter(parameterPathParts, null, true, workflowNodeStructure.parameterMap);

        // For now only check the first, root level of properties on which other properties could depend on

        checkDependOn(
            parameterPathParts[0], workflowNodeStructure.properties(), workflowNodeStructure.parameterMap,
            dynamicPropertyTypesMap);

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflow.getId(), environmentId);

        Map<String, Boolean> displayConditionMap = checkDisplayConditionsAndParameters(
            parameterPath, workflowNodeName, workflow, workflowNodeStructure.operationType,
            workflowNodeStructure.parameterMap, inputMap, dynamicPropertyTypesMap, workflowNodeStructure.properties,
            true, environmentId);

        updateFromAiMetadataPaths(false, getFromAiPaths(metadataMap), parameterPath);

        setDynamicPropertyTypeItem(parameterPath, null, metadataMap);

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());

        return new ParameterResultDTO(
            displayConditionMap, metadataMap, workflowNodeStructure.missingRequiredProperties,
            workflowNodeStructure.parameterMap);
    }

    @Override
    public ParameterResultDTO deleteWorkflowNodeParameter(
        String workflowId, String workflowNodeName, String parameterPath,
        long environmentId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        WorkflowNodeStructure workflowNodeStructure = getWorkflowNodeStructure(
            workflowNodeName, null, null, definitionMap);

        Map<String, Object> metadataMap = getMetadataMap(workflowNodeName, null, null, definitionMap);

        Map<String, ?> dynamicPropertyTypesMap = getDynamicPropertyTypesMap(metadataMap);

        String[] parameterPathParts = parameterPath.split("\\.");

        setParameter(parameterPathParts, null, true, workflowNodeStructure.parameterMap);

        // For now only check the first, root level of properties on which other properties could depend on

        checkDependOn(
            parameterPathParts[0], workflowNodeStructure.properties(), workflowNodeStructure.parameterMap,
            dynamicPropertyTypesMap);

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflow.getId(), environmentId);

        Map<String, Boolean> displayConditionMap = checkDisplayConditionsAndParameters(
            parameterPath, workflowNodeName, workflow, workflowNodeStructure.operationType,
            workflowNodeStructure.parameterMap, inputMap, dynamicPropertyTypesMap, workflowNodeStructure.properties,
            true, environmentId);

        setDynamicPropertyTypeItem(parameterPath, null, metadataMap);

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());

        return new ParameterResultDTO(
            displayConditionMap, metadataMap, workflowNodeStructure.missingRequiredProperties,
            workflowNodeStructure.parameterMap);
    }

    @Override
    public DisplayConditionResultDTO getClusterElementDisplayConditions(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, long environmentId) {

        Map<String, Boolean> displayConditionMap = new HashMap<>();
        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        WorkflowNodeStructure workflowNodeStructure = getWorkflowNodeStructure(
            workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, definitionMap);

        Set<String> keySet = workflowNodeStructure.parameterMap.keySet();

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflow.getId(), environmentId);

        for (String parameterName : new HashSet<>(keySet)) {
            displayConditionMap.putAll(
                checkDisplayConditionsAndParameters(
                    parameterName, workflowNodeName, workflow, workflowNodeStructure.operationType,
                    workflowNodeStructure.parameterMap, inputMap, Map.of(), workflowNodeStructure.properties, false,
                    environmentId));
        }

        return new DisplayConditionResultDTO(
            displayConditionMap, new ArrayList<>(workflowNodeStructure.missingRequiredProperties));
    }

    @Override
    public DisplayConditionResultDTO getWorkflowNodeDisplayConditions(
        String workflowId, String workflowNodeName, long environmentId) {

        Map<String, Boolean> displayConditionMap = new HashMap<>();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        WorkflowNodeStructure workflowNodeStructure = getWorkflowNodeStructure(
            workflowNodeName, null, null, definitionMap);

        Set<String> keySet = workflowNodeStructure.parameterMap.keySet();

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflow.getId(), environmentId);

        for (String parameterName : new HashSet<>(keySet)) {
            displayConditionMap.putAll(
                checkDisplayConditionsAndParameters(
                    parameterName, workflowNodeName, workflow, workflowNodeStructure.operationType,
                    workflowNodeStructure.parameterMap, inputMap, Map.of(), workflowNodeStructure.properties, false,
                    environmentId));
        }

        return new DisplayConditionResultDTO(
            displayConditionMap, new ArrayList<>(workflowNodeStructure.missingRequiredProperties));
    }

    @Override
    public ParameterResultDTO updateClusterElementParameter(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String parameterPath, Object value, String type,
        boolean fromAiInMetadata, boolean includeInMetadata, long environmentId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        WorkflowNodeStructure workflowNodeStructure = getWorkflowNodeStructure(
            workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, definitionMap);

        Map<String, Object> metadataMap = getMetadataMap(
            workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName, definitionMap);

        Map<String, ?> dynamicPropertyTypesMap = getDynamicPropertyTypesMap(metadataMap);

        String[] parameterPathParts = parameterPath.split("\\.");

        setParameter(parameterPathParts, value, false, workflowNodeStructure.parameterMap);

        // For now only check the first, root level of properties on which other properties could depend on

        checkDependOn(
            parameterPathParts[0], workflowNodeStructure.properties(), workflowNodeStructure.parameterMap,
            dynamicPropertyTypesMap);

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflow.getId(), environmentId);

        Map<String, Boolean> displayConditionMap = checkDisplayConditionsAndParameters(
            parameterPath, workflowNodeName, workflow, workflowNodeStructure.operationType,
            workflowNodeStructure.parameterMap, inputMap, dynamicPropertyTypesMap, workflowNodeStructure.properties,
            true, environmentId);

        updateFromAiMetadataPaths(fromAiInMetadata, getFromAiPaths(metadataMap), parameterPath);

        if (includeInMetadata) {
            setDynamicPropertyTypeItem(parameterPath, type, metadataMap);
        }

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());

        return new ParameterResultDTO(
            displayConditionMap, metadataMap, workflowNodeStructure.missingRequiredProperties,
            workflowNodeStructure.parameterMap);
    }

    @Override
    public ParameterResultDTO updateWorkflowNodeParameter(
        String workflowId, String workflowNodeName, String parameterPath, Object value, String type,
        boolean includeInMetadata, long environmentId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        WorkflowNodeStructure workflowNodeStructure = getWorkflowNodeStructure(
            workflowNodeName, null, null, definitionMap);

        Map<String, Object> metadataMap = getMetadataMap(workflowNodeName, null, null, definitionMap);

        Map<String, ?> dynamicPropertyTypesMap = getDynamicPropertyTypesMap(metadataMap);

        String[] parameterPathParts = parameterPath.split("\\.");

        setParameter(parameterPathParts, value, false, workflowNodeStructure.parameterMap);

        // For now only check the first, root level of properties on which other properties could depend on

        checkDependOn(
            parameterPathParts[0], workflowNodeStructure.properties(), workflowNodeStructure.parameterMap,
            dynamicPropertyTypesMap);

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflow.getId(), environmentId);

        Map<String, Boolean> displayConditionMap = checkDisplayConditionsAndParameters(
            parameterPath, workflowNodeName, workflow, workflowNodeStructure.operationType,
            workflowNodeStructure.parameterMap, inputMap, dynamicPropertyTypesMap, workflowNodeStructure.properties,
            true, environmentId);

        if (includeInMetadata) {
            setDynamicPropertyTypeItem(parameterPath, type, metadataMap);
        }

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());

        return new ParameterResultDTO(
            displayConditionMap, metadataMap, workflowNodeStructure.missingRequiredProperties,
            workflowNodeStructure.parameterMap);
    }

    protected static boolean hasExpressionVariable(String displayCondition, String parameterPath) {
        if ((displayCondition == null) || displayCondition.isEmpty()) {
            return false;
        }

        String regex = "(^|.*\\W)" + parameterPath + "(\\W.*|$)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(displayCondition);

        return matcher.find();
    }

    protected static boolean hasExpressionVariable(
        String displayCondition, String parameterPath, List<Integer> parameterPathIndexes) {

        if ((displayCondition == null) || displayCondition.isEmpty()) {
            return false;
        }

        if (!displayCondition.contains("[index]")) {
            return false;
        }

        displayCondition = replaceIndexes(displayCondition, parameterPathIndexes);

        return displayCondition.contains(parameterPath);
    }

    protected boolean evaluate(
        String displayCondition, Map<String, Object> inputMap, Map<String, Object> outputs,
        Map<String, ?> parameterMap) {

        Map<String, Object> evaluate;

        try {
            evaluate = evaluator.evaluate(parameterMap, outputs);
        } catch (Exception e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage());
            }

            evaluate = new HashMap<>(parameterMap);
        }

        return evaluate(
            displayCondition,
            MapUtils.concat(
                MapUtils.concat(inputMap, outputs),
                MapUtils.toMap(
                    evaluate, Map.Entry::getKey, entry -> entry.getValue() == null ? "" : entry.getValue())));
    }

    protected void evaluateArray(
        String propertyName, String displayCondition, Map<String, String> displayConditionMap,
        Map<String, Object> inputMap, Map<String, Object> outputs, Map<String, ?> parameterMap) {

        List<List<Integer>> indexesList = findIndexes(displayCondition, parameterMap);

        if (indexesList.isEmpty()) {
            boolean result = evaluate(displayCondition, inputMap, outputs, parameterMap);

            if (result) {
                displayConditionMap.put(displayCondition, propertyName);
            }
        } else {
            for (List<Integer> indexes : indexesList) {
                String updatedDisplayCondition = displayCondition.contains("[index]")
                    ? replaceIndexes(displayCondition, indexes) : displayCondition;

                if (displayConditionMap.containsKey(updatedDisplayCondition)) {
                    continue;
                }

                boolean result = evaluate(updatedDisplayCondition, inputMap, outputs, parameterMap);

                if (result) {
                    String indexesString = indexes.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining("_"));

                    displayConditionMap.put(updatedDisplayCondition, String.join("_", indexesString, propertyName));
                }
            }
        }
    }

    private void updateFromAiMetadataPaths(boolean fromAiInMetadata, List<String> fromAiPaths, String parameterPath) {
        if (fromAiInMetadata) {
            fromAiPaths.add(parameterPath);
        } else {
            fromAiPaths.remove(parameterPath);
        }
    }

    private void checkDependOn(
        String name, List<? extends BaseProperty> properties, Map<String, ?> parameterMap,
        Map<String, ?> dynamicPropertyTypesMap) {

        for (BaseProperty property : properties) {
            List<String> dependOnPropertyNames = List.of();

            if (property instanceof OptionsDataSourceAware optionsDataSourceAware) {
                OptionsDataSource optionsDataSource = optionsDataSourceAware.getOptionsDataSource();

                if (optionsDataSource == null) {
                    continue;
                }

                dependOnPropertyNames = optionsDataSource.getOptionsLookupDependsOn();
            } else if (property instanceof DynamicPropertiesProperty dynamicPropertiesProperty) {
                PropertiesDataSource propertiesDataSource = dynamicPropertiesProperty.getPropertiesDataSource();

                if (propertiesDataSource == null) {
                    continue;
                }

                dependOnPropertyNames = propertiesDataSource.getPropertiesLookupDependsOn();
            }

            if (dependOnPropertyNames.contains(name)) {
                parameterMap.remove(property.getName());

                checkDynamicPropertyType(property.getName(), dynamicPropertyTypesMap);
            }
        }
    }

    private Map<String, Boolean> checkDisplayConditionsAndParameters(
        String parameterPath, String workflowNodeName, Workflow workflow,
        WorkflowNodeStructure.OperationType operationType,
        Map<String, ?> parameterMap, Map<String, ?> inputMap, Map<String, ?> dynamicPropertyTypesMap,
        List<? extends BaseProperty> properties, boolean removeParameters, long environmentId) {

        Map<String, String> displayConditionMap = new HashMap<>();

        checkDisplayConditionsAndParameters(
            parameterPath, workflowNodeName, workflow, operationType, parameterMap, inputMap,
            displayConditionMap, dynamicPropertyTypesMap, properties, removeParameters, environmentId);

        return MapUtils.toMap(displayConditionMap, Map.Entry::getKey, entry -> true);
    }

    @SuppressWarnings("unchecked")
    private void checkDisplayConditionsAndParameters(
        String parameterPath, String workflowNodeName, Workflow workflow,
        WorkflowNodeStructure.OperationType operationType,
        Map<String, ?> parameterMap, Map<String, ?> inputMap, Map<String, String> displayConditionMap,
        Map<String, ?> dynamicPropertyTypesMap, List<? extends BaseProperty> properties, boolean removeParameters,
        long environmentId) {

        for (BaseProperty property : properties) {
            switch (property) {
                case ArrayProperty arrayProperty -> {
                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, arrayProperty,
                        removeParameters, environmentId);

                    List<? extends BaseProperty> itemsToCheck = filterArrayItems(
                        arrayProperty.getItems(), parameterPath, dynamicPropertyTypesMap);

                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        inputMap, displayConditionMap, dynamicPropertyTypesMap, itemsToCheck,
                        removeParameters, environmentId);
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty arrayProperty -> {
                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, arrayProperty,
                        removeParameters, environmentId);

                    List<? extends BaseProperty> itemsToCheck = filterArrayItems(
                        arrayProperty.getItems(), parameterPath, dynamicPropertyTypesMap);

                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap, inputMap,
                        displayConditionMap, dynamicPropertyTypesMap, itemsToCheck, removeParameters, environmentId);
                }
                case ObjectProperty objectProperty -> {
                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, objectProperty,
                        removeParameters, environmentId);

                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        inputMap, displayConditionMap, dynamicPropertyTypesMap, objectProperty.getProperties(),
                        removeParameters, environmentId);
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty objectProperty -> {
                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, objectProperty,
                        removeParameters, environmentId);

                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        inputMap, displayConditionMap, dynamicPropertyTypesMap, objectProperty.getProperties(),
                        removeParameters, environmentId);
                }
                default -> checkDisplayConditionsAndParameters(
                    parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                    (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, property,
                    removeParameters, environmentId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void checkDisplayConditionsAndParameters(
        String parameterPath, String workflowNodeName, Workflow workflow,
        WorkflowNodeStructure.OperationType operationType,
        Map<String, ?> parameterMap, Map<String, Object> inputMap, Map<String, String> displayConditionMap,
        Map<String, ?> dynamicPropertyTypesMap, BaseProperty property, boolean removeParameters, long environmentId) {

        if (property.getDisplayCondition() == null) {
            return;
        }

        String displayCondition = property.getDisplayCondition();

        if (operationType == WorkflowNodeStructure.OperationType.CLUSTER_ELEMENT ||
            operationType == WorkflowNodeStructure.OperationType.TASK ||
            operationType == WorkflowNodeStructure.OperationType.TASK_DISPATCHER) {

            WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

            Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
                workflow.getId(), workflowTask.getName(), environmentId);

            if (displayCondition.contains("[index]")) {
                evaluateArray(
                    property.getName(), displayCondition, displayConditionMap, inputMap, (Map<String, Object>) outputs,
                    parameterMap);
            } else {
                boolean result = evaluate(displayCondition, inputMap, (Map<String, Object>) outputs, parameterMap);

                if (result) {
                    displayConditionMap.put(displayCondition, property.getName());
                }
            }
        } else {
            if (displayCondition.contains("[index]")) {
                evaluateArray(
                    property.getName(), displayCondition, displayConditionMap, inputMap, Map.of(),
                    parameterMap);
            } else {
                boolean result = evaluate(
                    displayCondition, MapUtils.concat(inputMap, (Map<String, Object>) parameterMap));

                if (result) {
                    displayConditionMap.put(displayCondition, property.getName());
                }
            }
        }

        if (parameterPath.contains("[")) {
            if (removeParameters) {
                List<Integer> parameterPathIndexes = extractIndexes(parameterPath);

                String indexesString = parameterPathIndexes.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("_"));

                if (hasExpressionVariable(displayCondition, parameterPath, parameterPathIndexes) &&
                    !displayConditionMap.containsValue(String.join("_", indexesString, property.getName()))) {

                    removeParameter(property.getName(), parameterPathIndexes, parameterMap);

                    checkDynamicPropertyType(property.getName(), dynamicPropertyTypesMap);
                }
            }
        } else {
            if (removeParameters && hasExpressionVariable(displayCondition, parameterPath)) {
                removeParameter(property.getName(), null, parameterMap);

                checkDynamicPropertyType(property.getName(), dynamicPropertyTypesMap);
            }
        }
    }

    /**
     * Filters array items based on dynamic property types stored in metadata. For array paths like "conditions[0][0]",
     * only returns the object variant that matches the stored type in dynamicPropertyTypesMap. <br/>
     * In the condition component, array items are object variants with names like "boolean", "number", "string". We
     * match the object name against the stored type. <br/>
     * The type can be stored either: 1. Directly as "conditions[0][0]" -> "STRING" 2. Or inferred from child properties
     * like "conditions[0][0].value1" -> "STRING"
     *
     * @param items                   the list of properties to be filtered, may be null or empty
     * @param parameterPath           the parameter path that may contain array indices and property accessors
     * @param dynamicPropertyTypesMap a map containing parameter paths as keys and their corresponding type names as
     *                                values
     * @return the filtered list of properties matching the stored type, or the original list if no filtering applies
     */
    private List<? extends BaseProperty> filterArrayItems(
        List<? extends BaseProperty> items, String parameterPath, Map<String, ?> dynamicPropertyTypesMap) {

        if (items == null || items.isEmpty() || !parameterPath.contains("[")) {
            return items;
        }

        // Extract parent path (e.g., "conditions[0][0]" from "conditions[0][0].value2")
        int lastDotIndex = parameterPath.lastIndexOf('.');

        String parentPath = lastDotIndex > 0 ? parameterPath.substring(0, lastDotIndex) : parameterPath;

        String storedType = (String) dynamicPropertyTypesMap.get(parentPath);

        // If not found directly, try to infer from child properties
        if (storedType == null) {
            String prefix = parentPath + ".";

            for (Map.Entry<String, ?> entry : dynamicPropertyTypesMap.entrySet()) {
                String key = entry.getKey();

                if (key.startsWith(prefix) && entry.getValue() instanceof String) {
                    storedType = (String) entry.getValue();

                    break;
                }
            }
        }

        if (storedType == null) {
            return items;
        }

        List<BaseProperty> filteredProperties = new ArrayList<>();

        for (BaseProperty item : items) {
            if (item instanceof ObjectProperty objectProperty) {
                String name = objectProperty.getName();

                if (name != null && name.equalsIgnoreCase(storedType)) {
                    filteredProperties.add(item);
                }
            } else if (item instanceof com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty objectProperty) {

                String name = objectProperty.getName();

                if (name != null && name.equalsIgnoreCase(storedType)) {
                    filteredProperties.add(item);
                }
            } else {
                filteredProperties.add(item);
            }
        }

        return filteredProperties.isEmpty() ? items : filteredProperties;
    }

    private static Map<String, ?> getClusterElementMap(
        String clusterElementTypeName, String clusterElementWorkflowNodeName, Map<String, ?> taskMap) {

        return getClusterElementMap(clusterElementTypeName, clusterElementWorkflowNodeName, taskMap, true);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> getClusterElementMap(
        String clusterElementTypeName, String clusterElementWorkflowNodeName, Map<String, ?> taskMap,
        boolean nullCheck) {

        Map<String, ?> clusterElementMap = null;
        Map<String, Map<String, ?>> clusterElementsMap = (Map<String, Map<String, ?>>) taskMap.get(
            WorkflowExtConstants.CLUSTER_ELEMENTS);

        for (Map.Entry<String, ?> entry : clusterElementsMap.entrySet()) {
            if (clusterElementTypeName.equalsIgnoreCase(entry.getKey())) {
                if (entry.getValue() instanceof Map<?, ?> map &&
                    Objects.equals(map.get(WorkflowConstants.NAME), clusterElementWorkflowNodeName)) {

                    clusterElementMap = (Map<String, ?>) map;
                } else if (entry.getValue() instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            String name = (String) map.get(WorkflowConstants.NAME);

                            if (name.equals(clusterElementWorkflowNodeName)) {
                                clusterElementMap = (Map<String, ?>) map;

                                break;
                            }
                        }
                    }
                }
            } else {
                if (entry.getValue() instanceof Map<?, ?> map) {
                    if (map.containsKey(WorkflowExtConstants.CLUSTER_ELEMENTS)) {
                        clusterElementMap = getClusterElementMap(
                            clusterElementTypeName, clusterElementWorkflowNodeName, (Map<String, ?>) map, false);
                    }
                } else if (entry.getValue() instanceof List<?> list) {
                    for (Object item : list) {
                        if ((item instanceof Map<?, ?> map) && map.containsKey(WorkflowExtConstants.CLUSTER_ELEMENTS)) {
                            clusterElementMap = getClusterElementMap(
                                clusterElementTypeName, clusterElementWorkflowNodeName, (Map<String, ?>) map, false);

                            if (clusterElementMap != null && Objects.equals(
                                clusterElementMap.get(WorkflowConstants.NAME), clusterElementWorkflowNodeName)) {

                                break;
                            }
                        }
                    }

                }
            }

            if (clusterElementMap != null) {
                break;
            }
        }

        if (nullCheck && clusterElementMap == null) {
            throw new ConfigurationException(
                "Cluster element with name: %s does not exist".formatted(clusterElementWorkflowNodeName),
                WorkflowErrorType.CLUSTER_ELEMENT_NOT_FOUND);
        }

        return clusterElementMap;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> getTask(String workflowNodeName, List<Map<String, ?>> taskMaps) {
        for (Map<String, ?> taskMap : taskMaps) {
            if (Objects.equals(taskMap.get(WorkflowConstants.NAME), workflowNodeName)) {
                if (!taskMap.containsKey(WorkflowConstants.PARAMETERS)) {
                    ((Map<String, Object>) taskMap).put(WorkflowConstants.PARAMETERS, new HashMap<>());
                }

                return taskMap;
            }

            Map<String, ?> parameters = (Map<String, ?>) taskMap.get(WorkflowConstants.PARAMETERS);

            if (parameters == null) {
                continue;
            }

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Map<?, ?> curMap) {
                    if (curMap.containsKey(WorkflowConstants.NAME)) {
                        Map<String, ?> curTaskMap = getTask(workflowNodeName, List.of((Map<String, ?>) curMap));

                        if (curTaskMap != null) {
                            return curTaskMap;
                        }
                    } else {
                        for (Map.Entry<?, ?> curMapEntry : curMap.entrySet()) {
                            if (curMapEntry.getValue() instanceof Map<?, ?> curTask) {
                                if (!curTask.containsKey(WorkflowConstants.NAME) &&
                                    !curTask.containsKey(WorkflowConstants.PARAMETERS)) {

                                    continue;
                                }

                                Map<String, ?> curTaskMap = getTask(
                                    workflowNodeName, List.of((Map<String, ?>) curTask));

                                if (curTaskMap != null) {
                                    return curTaskMap;
                                }
                            }
                        }
                    }
                } else if (value instanceof List<?> curList && !curList.isEmpty()) {
                    if (curList.getFirst() instanceof Map<?, ?>) {
                        for (Object curItem : curList) {
                            if (curItem instanceof Map<?, ?> curTask) {
                                if (curTask.containsKey(WorkflowConstants.TASKS)) {
                                    Map<String, ?> curTaskMap = getTask(
                                        workflowNodeName, (List<Map<String, ?>>) curTask.get(WorkflowConstants.TASKS));

                                    if (curTaskMap != null) {
                                        return curTaskMap;
                                    }
                                }

                                if (!curTask.containsKey(WorkflowConstants.NAME) &&
                                    !curTask.containsKey(WorkflowConstants.PARAMETERS)) {

                                    continue;
                                }

                                Map<String, ?> curTaskMap = getTask(
                                    workflowNodeName, List.of((Map<String, ?>) curTask));

                                if (curTaskMap != null) {
                                    return curTaskMap;
                                }
                            }
                        }
                    } else if (curList.getFirst() instanceof List<?>) {
                        for (Object curListItem : curList) {
                            for (Object curItem : (List<?>) curListItem) {
                                if (curItem instanceof Map<?, ?> curTask) {
                                    if (curTask.containsKey(WorkflowConstants.TASKS)) {
                                        Map<String, ?> curTaskMap = getTask(
                                            workflowNodeName,
                                            (List<Map<String, ?>>) curTask.get(WorkflowConstants.TASKS));

                                        if (curTaskMap != null) {
                                            return curTaskMap;
                                        }
                                    }

                                    if (!curTask.containsKey(WorkflowConstants.NAME) &&
                                        !curTask.containsKey(WorkflowConstants.PARAMETERS)) {

                                        continue;
                                    }

                                    Map<String, ?> curTaskMap = getTask(
                                        workflowNodeName, List.of((Map<String, ?>) curTask));

                                    if (curTaskMap != null) {
                                        return curTaskMap;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static void removeParameter(String parameterName, List<Integer> indexes, Map<?, ?> parameterMap) {
        for (Map.Entry<?, ?> entry : parameterMap.entrySet()) {
            String key = (String) entry.getKey();

            if (key.equals(parameterName)) {
                parameterMap.remove(key);

                return;
            }

            if (parameterMap.get(key) instanceof List<?> subList && indexes != null && !indexes.isEmpty()) {
                int index = indexes.getFirst();

                if (subList.size() <= index) {
                    continue;
                }

                if (subList.get(index) instanceof Map<?, ?> subParameterMap) {
                    indexes.removeFirst();

                    removeParameter(parameterName, indexes, subParameterMap);
                } else if (subList.get(index) instanceof List<?> subList2) {
                    if (indexes.getFirst() < subList2.size() &&
                        subList2.get(indexes.getFirst()) instanceof Map<?, ?> subParameterMap) {

                        indexes.removeFirst();

                        removeParameter(parameterName, indexes, subParameterMap);
                    } else {
                        indexes.removeFirst();
                    }
                }
            } else if (parameterMap.get(key) instanceof Map<?, ?> subParameterMap) {
                removeParameter(parameterName, indexes, subParameterMap);
            }
        }
    }

    private static void checkDynamicPropertyType(String propertyName, Map<String, ?> dynamicPropertyTypesMap) {
        for (String key : new HashSet<>(dynamicPropertyTypesMap.keySet())) {
            if (key.equals(propertyName) || key.contains("." + propertyName) || key.startsWith(propertyName + ".") ||
                key.startsWith(propertyName + "[")) {

                dynamicPropertyTypesMap.remove(key);
            }
        }
    }

    private boolean evaluate(String displayCondition, Map<String, ?> inputParameters) {
        Map<String, Object> result = evaluator.evaluate(
            Map.of("displayCondition", "=" + displayCondition), inputParameters);

        Object displayConditionResult = result.get("displayCondition");

        return !(displayConditionResult instanceof String) && (boolean) displayConditionResult;
    }

    private static List<Integer> extractIndexes(String expression) {
        List<Integer> indexes = new ArrayList<>();

        Matcher matcher = ARRAY_INDEX_VALUE_PATTERN.matcher(expression);

        while (matcher.find()) {
            indexes.add(Integer.parseInt(matcher.group(1)));
        }

        return indexes;
    }

    private static List<List<Integer>> findIndexes(String displayCondition, Map<String, ?> parameterMap) {
        List<List<Integer>> allIndexes = new ArrayList<>();

        if (displayCondition == null || displayCondition.isEmpty()) {
            return List.of();
        }

        findIndexes(displayCondition, parameterMap, new ArrayList<>(), allIndexes);

        return allIndexes;
    }

    private static void findIndexes(
        String displayCondition, Object currentParameters, List<Integer> currentIndexes,
        List<List<Integer>> allIndexes) {

        if (displayCondition.startsWith(".")) {
            displayCondition = displayCondition.substring(1);
        }

        Matcher matcher = ARRAY_INDEXES_PATTERN.matcher(displayCondition);

        if (matcher.find()) {
            String key = matcher.group(1);
            String indexGroup = matcher.group(2);
            int matchEnd = matcher.end();
            String remainingExpression = displayCondition.substring(matchEnd);

            if (key != null) {
                if (currentParameters instanceof Map<?, ?> currentParameterMap) {
                    if (currentParameterMap.containsKey(key)) {
                        Object nextParameters = currentParameterMap.get(key);

                        if (nextParameters instanceof List<?> nextList) {
                            for (int i = 0; i < nextList.size(); i++) {
                                currentIndexes.add(i);

                                findIndexes(
                                    indexGroup.replaceFirst("\\[index]", "") + remainingExpression, nextList.get(i),
                                    currentIndexes, allIndexes);

                                currentIndexes.removeLast();
                            }
                        } else {
                            findIndexes(indexGroup + remainingExpression, nextParameters, currentIndexes, allIndexes);
                        }
                    } else {
                        findIndexes(remainingExpression, currentParameters, currentIndexes, allIndexes);
                    }
                }
            } else {
                if (currentParameters instanceof List<?> currentList) {
                    for (int i = 0; i < currentList.size(); i++) {
                        currentIndexes.add(i);

                        findIndexes(
                            indexGroup.replaceFirst("\\[index]", "") + remainingExpression, currentList.get(i),
                            currentIndexes, allIndexes);

                        currentIndexes.removeLast();
                    }
                }
            }
        } else {
            if (!currentIndexes.isEmpty()) {
                allIndexes.add(new ArrayList<>(currentIndexes));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMetadataMap(
        String workflowNodeName, String clusterElementTypeName, String clusterElementWorkflowNodeName,
        Map<String, ?> definitionMap) {

        Map<String, Object> metadataMap;

        Map<String, ?> triggerMap = getTrigger(
            workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowExtConstants.TRIGGERS));

        if (triggerMap == null) {
            Map<String, ?> taskMap = getTask(
                workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowConstants.TASKS));

            if (taskMap == null) {
                throw new ConfigurationException(
                    "Workflow node with name: %s does not exist".formatted(workflowNodeName),
                    WorkflowErrorType.WORKFLOW_NODE_NOT_FOUND);
            }

            if (clusterElementTypeName == null) {
                metadataMap = (Map<String, Object>) taskMap.get(METADATA);

                if (metadataMap == null) {
                    metadataMap = new HashMap<>();

                    ((Map<String, Object>) taskMap).put(METADATA, metadataMap);
                }
            } else {
                Map<String, ?> clusterElementMap = getClusterElementMap(
                    clusterElementTypeName, clusterElementWorkflowNodeName, taskMap);

                metadataMap = (Map<String, Object>) clusterElementMap.get(METADATA);
            }
        } else {
            metadataMap = (Map<String, Object>) triggerMap.get(METADATA);

            if (metadataMap == null) {
                metadataMap = new HashMap<>();

                ((Map<String, Object>) triggerMap).put(METADATA, metadataMap);
            }
        }

        return metadataMap;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getTrigger(String workflowNodeName, List<Map<String, ?>> triggerMaps) {
        if (triggerMaps == null) {
            return null;
        }

        if (Objects.equals(workflowNodeName, "manual")) {
            return Map.of("type", "manual/v1/manual", "parameters", Map.of());
        }

        for (Map<String, ?> triggerMap : triggerMaps) {
            if (Objects.equals(triggerMap.get(WorkflowConstants.NAME), workflowNodeName)) {
                if (!triggerMap.containsKey(WorkflowConstants.PARAMETERS)) {
                    ((Map<String, Object>) triggerMap).put(WorkflowConstants.PARAMETERS, new HashMap<>());
                }

                return triggerMap;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private WorkflowNodeStructure getWorkflowNodeStructure(
        String workflowNodeName, String clusterElementTypeName, String clusterElementWorkflowNodeName,
        Map<String, ?> definitionMap) {

        Map<String, ?> parameterMap;
        List<? extends BaseProperty> properties;

        // We need a mutable map
        Map<String, ?> triggerMap = getTrigger(
            workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowExtConstants.TRIGGERS));

        WorkflowNodeStructure.OperationType operationType;

        if (triggerMap == null) {
            // We need a mutable map

            Map<String, ?> taskMap = getTask(
                workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowConstants.TASKS));

            if (taskMap == null) {
                throw new ConfigurationException(
                    "Workflow node with name: %s does not exist".formatted(workflowNodeName),
                    WorkflowErrorType.WORKFLOW_NODE_NOT_FOUND);
            }

            if (clusterElementTypeName == null) {
                parameterMap = (Map<String, ?>) taskMap.get(WorkflowConstants.PARAMETERS);
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(
                    (String) taskMap.get(WorkflowConstants.TYPE));

                if (workflowNodeType.operation() == null) {
                    operationType = WorkflowNodeStructure.OperationType.TASK_DISPATCHER;

                    TaskDispatcherDefinition taskDispatcherDefinition =
                        taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                            workflowNodeType.name(), workflowNodeType.version());

                    properties = taskDispatcherDefinition.getProperties();
                } else {
                    operationType = WorkflowNodeStructure.OperationType.TASK;

                    ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                        workflowNodeType.name(), workflowNodeType.version(),
                        workflowNodeType.operation());

                    properties = actionDefinition.getProperties();
                }
            } else {
                Map<String, ?> clusterElementMap = getClusterElementMap(
                    clusterElementTypeName, clusterElementWorkflowNodeName, taskMap);

                parameterMap = (Map<String, ?>) clusterElementMap.get(WorkflowConstants.PARAMETERS);

                operationType = WorkflowNodeStructure.OperationType.CLUSTER_ELEMENT;

                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(
                    (String) clusterElementMap.get(WorkflowConstants.TYPE));

                ClusterElementDefinition clusterElementDefinition = clusterElementDefinitionService
                    .getClusterElementDefinition(
                        workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());

                properties = clusterElementDefinition.getProperties();
            }
        } else {
            operationType = WorkflowNodeStructure.OperationType.TRIGGER;
            parameterMap = (Map<String, ?>) triggerMap.get(WorkflowConstants.PARAMETERS);
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(
                (String) triggerMap.get(WorkflowConstants.TYPE));

            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());

            properties = triggerDefinition.getProperties();
        }

        Set<String> missingRequiredProperties = new HashSet<>();

        if (parameterMap != null && properties != null) {
            checkRequiredProperties(properties, parameterMap, "", missingRequiredProperties);
        }

        return new WorkflowNodeStructure(operationType, parameterMap, properties, missingRequiredProperties);
    }

    private void checkRequiredProperties(
        List<?> properties, Map<?, ?> parameterMap, String prefix, Set<String> missingRequiredProperties) {

        for (Object prop : properties) {
            if (!(prop instanceof BaseProperty property)) {
                continue;
            }

            String propertyName = property.getName();

            String propertyPath = prefix.isEmpty() ? propertyName : prefix + "." + propertyName;

            if (property.getRequired() && !parameterMap.containsKey(propertyName)) {
                missingRequiredProperties.add(propertyPath);
            } else if (parameterMap.containsKey(propertyName)) {
                // Check nested properties
                if (property instanceof ObjectProperty objectProperty) {
                    List<?> nestedProperties = objectProperty.getProperties();

                    if (nestedProperties != null && !nestedProperties.isEmpty()) {
                        Object value = parameterMap.get(propertyName);

                        if (value instanceof Map) {
                            checkRequiredProperties(
                                nestedProperties, (Map<?, ?>) value, propertyPath, missingRequiredProperties);
                        }
                    }
                } else if (property instanceof ArrayProperty arrayProperty) {
                    List<?> items = arrayProperty.getItems();
                    if (items != null && !items.isEmpty()) {
                        Object value = parameterMap.get(propertyName);

                        if (value instanceof List<?> list) {
                            for (int i = 0; i < list.size(); i++) {
                                Object item = list.get(i);

                                if (item instanceof Map) {
                                    checkRequiredProperties(
                                        items, (Map<?, ?>) item, propertyPath + "[" + i + "]",
                                        missingRequiredProperties);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getDynamicPropertyTypesMap(Map<String, Object> metadataMap) {
        Map<String, Object> uiMap = (Map<String, Object>) metadataMap.get(UI);

        if (uiMap == null) {
            uiMap = new HashMap<>();

            metadataMap.put(UI, uiMap);
        }

        Map<String, Object> dynamicPropertyTypesMap = (Map<String, Object>) uiMap.get(DYNAMIC_PROPERTY_TYPES);

        if (dynamicPropertyTypesMap == null) {
            dynamicPropertyTypesMap = new HashMap<>();

            uiMap.put(DYNAMIC_PROPERTY_TYPES, dynamicPropertyTypesMap);
        }

        return dynamicPropertyTypesMap;
    }

    @SuppressWarnings("unchecked")
    private List<String> getFromAiPaths(Map<String, Object> metadataMap) {
        Map<String, Object> uiMap = (Map<String, Object>) metadataMap.get(UI);

        if (uiMap == null) {
            uiMap = new HashMap<>();

            metadataMap.put(UI, uiMap);
        }

        List<String> fromAi = (List<String>) uiMap.get(FROM_AI);

        if (fromAi == null) {
            fromAi = new ArrayList<>();

            uiMap.put(FROM_AI, fromAi);
        }

        return fromAi;
    }

    private void setDynamicPropertyTypeItem(String path, String type, Map<String, Object> metadataMap) {
        Map<String, Object> dynamicPropertyTypesMap = getDynamicPropertyTypesMap(metadataMap);

        if (type == null) {
            dynamicPropertyTypesMap.remove(path);

            if (path.contains("[")) {
                List<Integer> pathIndexes = extractIndexes(path);
                String pathPrefix = path.substring(0, path.lastIndexOf("["));

                for (String key : new HashSet<>(dynamicPropertyTypesMap.keySet())) {
                    List<Integer> keyIndexes = extractIndexes(key);

                    if (key.startsWith(pathPrefix) && !keyIndexes.isEmpty() &&
                        pathIndexes.getLast() < keyIndexes.getLast()) {

                        dynamicPropertyTypesMap.put(
                            pathPrefix + "[" + (keyIndexes.getLast() - 1) +
                                path.substring(path.lastIndexOf("[") + 2, path.lastIndexOf("]") + 1),
                            dynamicPropertyTypesMap.remove(key));
                    }
                }
            }
        } else {
            dynamicPropertyTypesMap.put(path, type);
        }
    }

    private static String replaceIndexes(String expression, List<Integer> indexes) {
        Integer lastIndex = null;

        for (Integer index : indexes) {
            expression = expression.replaceFirst("\\[index]", "[" + index + "]");
            lastIndex = index;
        }

        if (lastIndex != null && expression.contains("[index]")) {
            expression = expression.replace("[index]", "[" + lastIndex + "]");
        }

        return expression;
    }

    @SuppressWarnings("unchecked")
    private void setParameter(
        String[] parameterPathParts, Object value, boolean removeValue, Map<String, ?> parameterMap) {

        Map<String, Object> map = (Map<String, Object>) parameterMap;

        for (int i = 0; i < parameterPathParts.length; i++) {
            String pathItem = parameterPathParts[i];

            if (pathItem.endsWith("]")) {
                String name = pathItem.substring(0, pathItem.indexOf("["));
                String arrays = pathItem.substring(pathItem.indexOf("["));

                List<Object> list;

                if (Objects.nonNull(map.get(name))) {
                    list = (List<Object>) map.get(name);
                } else {
                    list = new ArrayList<>();

                    map.put(name, list);
                }

                Matcher matcher = ARRAY_INDEX_VALUE_PATTERN.matcher(arrays);

                List<Integer> arrayIndexes = new ArrayList<>();

                while (matcher.find()) {
                    arrayIndexes.add(Integer.parseInt(matcher.group(1)));
                }

                for (int j = 0; j < arrayIndexes.size(); j++) {
                    int arrayIndex = arrayIndexes.get(j);

                    if (list.size() < (arrayIndex + 1)) {
                        for (int k = list.size(); k < (arrayIndex + 1); k++) {
                            list.add(null);
                        }
                    }

                    if (j == arrayIndexes.size() - 1) {
                        if (i == parameterPathParts.length - 1) {
                            if (removeValue) {
                                list.remove(arrayIndex);
                            } else {
                                list.set(arrayIndex, value);
                            }
                        } else {
                            if (list.get(arrayIndex) == null) {
                                map = new HashMap<>();

                                list.set(arrayIndex, map);
                            } else {
                                map = (Map<String, Object>) list.get(arrayIndex);
                            }
                        }
                    } else {
                        if (list.get(arrayIndex) == null) {
                            list.set(arrayIndex, new ArrayList<>());
                        }

                        list = (List<Object>) list.get(arrayIndex);
                    }
                }
            } else {
                if (i < parameterPathParts.length - 1) {
                    if (map.containsKey(pathItem) && map.get(pathItem) != null) {
                        map = (Map<String, Object>) map.get(pathItem);
                    } else {
                        Map<String, Object> subParameterMap = new HashMap<>();

                        map.put(pathItem, subParameterMap);

                        map = subParameterMap;
                    }
                } else {
                    if (removeValue) {
                        map.remove(pathItem);
                    } else {
                        map.put(pathItem, value);
                    }
                }
            }
        }
    }

    private record WorkflowNodeStructure(
        OperationType operationType, Map<String, ?> parameterMap, List<? extends BaseProperty> properties,
        Set<String> missingRequiredProperties) {

        enum OperationType {
            CLUSTER_ELEMENT,
            TASK,
            TASK_DISPATCHER,
            TRIGGER
        }
    }
}
