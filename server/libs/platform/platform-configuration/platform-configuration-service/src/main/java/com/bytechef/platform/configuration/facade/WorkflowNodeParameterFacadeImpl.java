/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.DynamicPropertiesProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.OptionsDataSource;
import com.bytechef.platform.component.domain.OptionsDataSourceAware;
import com.bytechef.platform.component.domain.PropertiesDataSource;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.dto.UpdateParameterResultDTO;
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
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeParameterFacadeImpl implements WorkflowNodeParameterFacade {

    protected static final Pattern ARRAY_INDEX_VALUE_PATTERN = Pattern.compile("\\[(\\d+)]");
    private static final Pattern ARRAY_INDEXES_PATTERN =
        Pattern.compile("^.*(\\b\\w+\\b)((\\[index])+)(\\.\\b\\w+\\b.*)|.*((\\[index])+)(\\.\\b\\w+\\b.*)$");
    private static final String DYNAMIC_PROPERTY_TYPES = "dynamicPropertyTypes";
    private static final String METADATA = "metadata";
    private static final String UI = "ui";

    private final ActionDefinitionService actionDefinitionService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeParameterFacadeImpl(
        ActionDefinitionService actionDefinitionService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionService triggerDefinitionService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionService = actionDefinitionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public Map<String, ?> deleteParameter(String workflowId, String workflowNodeName, String path) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        WorkflowNodeStructure workflowNodeStructure = getWorkflowNodeStructure(workflowNodeName, definitionMap);

        String[] pathItems = path.split("\\.");

        setDynamicPropertyTypeItem(path, null, getMetadataMap(workflowNodeName, definitionMap));
        setParameter(pathItems, null, true, workflowNodeStructure.parameterMap);

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap, true), workflow.getVersion());

        return workflowNodeStructure.parameterMap;
    }

    @Override
    public Map<String, Boolean> getDisplayConditions(String workflowId, String workflowNodeName) {
        Map<String, Boolean> displayConditionMap = new HashMap<>();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        WorkflowNodeStructure parameterMapProperties = getWorkflowNodeStructure(
            workflowNodeName, definitionMap);

        Set<String> keySet = parameterMapProperties.parameterMap.keySet();

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflow.getId());

        for (String parameterName : new HashSet<>(keySet)) {
            displayConditionMap.putAll(
                checkDisplayConditionsAndParameters(
                    parameterName, workflowNodeName, workflow, parameterMapProperties.operationType,
                    parameterMapProperties.parameterMap, inputMap, Map.of(), parameterMapProperties.properties, false));
        }

        return displayConditionMap;
    }

    @Override
    public UpdateParameterResultDTO updateParameter(
        String workflowId, String workflowNodeName, String parameterPath, Object value, String type,
        boolean includeInMetadata) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        WorkflowNodeStructure workflowNodeStructure = getWorkflowNodeStructure(workflowNodeName, definitionMap);

        Map<String, Object> metadataMap = getMetadataMap(workflowNodeName, definitionMap);

        Map<String, ?> dynamicPropertyTypesMap = getDynamicPropertyTypesMap(metadataMap);

        String[] parameterPathParts = parameterPath.split("\\.");

        setParameter(parameterPathParts, value, false, workflowNodeStructure.parameterMap);

        // For now only check the first, root level of properties on which other properties could depend on

        checkDependOn(parameterPathParts[0], workflowNodeStructure.properties(), workflowNodeStructure.parameterMap,
            dynamicPropertyTypesMap);

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflow.getId());

        Map<String, Boolean> displayConditionMap = checkDisplayConditionsAndParameters(
            parameterPath, workflowNodeName, workflow, workflowNodeStructure.operationType,
            workflowNodeStructure.parameterMap, inputMap, dynamicPropertyTypesMap, workflowNodeStructure.properties,
            true);

        if (includeInMetadata) {
            setDynamicPropertyTypeItem(parameterPath, type, metadataMap);
        }

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap, true), workflow.getVersion());

        return new UpdateParameterResultDTO(displayConditionMap, metadataMap, workflowNodeStructure.parameterMap);
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

    protected static boolean evaluate(
        String displayCondition, Map<String, Object> inputMap, Map<String, Object> outputs,
        Map<String, ?> parameterMap) {

        return evaluate(
            displayCondition,
            MapUtils.concat(
                MapUtils.concat(inputMap, outputs),
                MapUtils.toMap(
                    Evaluator.evaluate(parameterMap, outputs),
                    Map.Entry::getKey, entry -> entry.getValue() == null ? "" : entry.getValue())));
    }

    protected static void evaluateArray(
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
        List<? extends BaseProperty> properties, boolean removeParameters) {

        Map<String, String> displayConditionMap = new HashMap<>();

        checkDisplayConditionsAndParameters(
            parameterPath, workflowNodeName, workflow, operationType, parameterMap, inputMap,
            displayConditionMap, dynamicPropertyTypesMap, properties, removeParameters);

        return MapUtils.toMap(displayConditionMap, Map.Entry::getKey, entry -> true);
    }

    @SuppressWarnings("unchecked")
    private void checkDisplayConditionsAndParameters(
        String parameterPath, String workflowNodeName, Workflow workflow,
        WorkflowNodeStructure.OperationType operationType,
        Map<String, ?> parameterMap, Map<String, ?> inputMap, Map<String, String> displayConditionMap,
        Map<String, ?> dynamicPropertyTypesMap, List<? extends BaseProperty> properties, boolean removeParameters) {

        for (BaseProperty property : properties) {
            switch (property) {
                case ArrayProperty arrayProperty -> {
                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, arrayProperty,
                        removeParameters);

                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        inputMap, displayConditionMap, dynamicPropertyTypesMap, arrayProperty.getItems(),
                        removeParameters);
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty arrayProperty -> {
                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, arrayProperty,
                        removeParameters);

                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        inputMap, displayConditionMap, dynamicPropertyTypesMap, arrayProperty.getItems(),
                        removeParameters);
                }
                case ObjectProperty objectProperty -> {
                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, objectProperty,
                        removeParameters);

                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        inputMap, displayConditionMap, dynamicPropertyTypesMap, objectProperty.getProperties(),
                        removeParameters);
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty objectProperty -> {
                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, objectProperty,
                        removeParameters);

                    checkDisplayConditionsAndParameters(
                        parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                        inputMap, displayConditionMap, dynamicPropertyTypesMap, objectProperty.getProperties(),
                        removeParameters);
                }
                default -> checkDisplayConditionsAndParameters(
                    parameterPath, workflowNodeName, workflow, operationType, parameterMap,
                    (Map<String, Object>) inputMap, displayConditionMap, dynamicPropertyTypesMap, property,
                    removeParameters);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void checkDisplayConditionsAndParameters(
        String parameterPath, String workflowNodeName, Workflow workflow,
        WorkflowNodeStructure.OperationType operationType,
        Map<String, ?> parameterMap, Map<String, Object> inputMap, Map<String, String> displayConditionMap,
        Map<String, ?> dynamicPropertyTypesMap, BaseProperty property, boolean removeParameters) {

        if (property.getDisplayCondition() == null) {
            return;
        }

        String displayCondition = property.getDisplayCondition();

        if (operationType == WorkflowNodeStructure.OperationType.TASK ||
            operationType == WorkflowNodeStructure.OperationType.TASK_DISPATCHER) {

            WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

            Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
                workflow.getId(), workflowTask.getName());

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
            boolean result = evaluate(displayCondition, MapUtils.concat(inputMap, (Map<String, Object>) parameterMap));

            if (result) {
                displayConditionMap.put(displayCondition, property.getName());
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

    private static void removeParameter(String parameterName, List<Integer> indexes, Map<?, ?> parameterMap) {
        for (Map.Entry<?, ?> entry : parameterMap.entrySet()) {
            String key = (String) entry.getKey();

            if (key.equals(parameterName)) {
                parameterMap.remove(key);

                return;
            }

            if (parameterMap.get(key) instanceof List<?> subList && indexes != null) {
                int index = indexes.getFirst();

                if (subList.size() <= index) {
                    continue;
                }

                if (subList.get(index) instanceof Map<?, ?> subParameterMap) {
                    indexes.removeFirst();

                    removeParameter(parameterName, indexes, subParameterMap);
                } else if (subList.get(index) instanceof List<?> subList2) {
                    indexes.removeFirst();

                    if (subList2.get(indexes.getFirst()) instanceof Map<?, ?> subParameterMap) {
                        indexes.removeFirst();

                        removeParameter(parameterName, indexes, subParameterMap);
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

    private static boolean evaluate(String displayCondition, Map<String, ?> inputParameters) {
        Map<String, Object> result = Evaluator.evaluate(
            Map.of("displayCondition", "${" + displayCondition + "}"), inputParameters);

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

        Matcher matcher = ARRAY_INDEXES_PATTERN.matcher(displayCondition);

        if (matcher.find()) {
            String key = matcher.group(1);
            String indexGroup = matcher.group(2);
            String remainingExpression = matcher.group(4);

            if (currentParameters instanceof Map<?, ?> currentParameterMap) {
                if (currentParameterMap.containsKey(key)) {
                    Object nextParameters = currentParameterMap.get(key);

                    if (indexGroup != null && nextParameters instanceof List<?> nextList) {
                        for (int i = 0; i < nextList.size(); i++) {
                            currentIndexes.add(i);

                            findIndexes(
                                (indexGroup + remainingExpression).replaceFirst("\\[index]", ""), nextList.get(i),
                                currentIndexes, allIndexes);

                            currentIndexes.removeLast();
                        }
                    } else {
                        findIndexes(indexGroup + remainingExpression, nextParameters, currentIndexes, allIndexes);
                    }
                }
            } else if (currentParameters instanceof List<?> currentList) {
                for (int i = 0; i < currentList.size(); i++) {
                    currentIndexes.add(i);

                    findIndexes(
                        (indexGroup + remainingExpression).replaceFirst("\\[index]", ""), currentList.get(i),
                        currentIndexes, allIndexes);

                    currentIndexes.removeLast();
                }
            }
        } else {
            if (!currentIndexes.isEmpty()) {
                allIndexes.add(new ArrayList<>(currentIndexes));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMetadataMap(String workflowNodeName, Map<String, ?> definitionMap) {
        Map<String, Object> metadataMap;

        Map<String, ?> triggerMap = getTrigger(
            workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowExtConstants.TRIGGERS));

        if (triggerMap == null) {
            Map<String, ?> taskMap = getTask(
                workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowConstants.TASKS));

            if (taskMap == null) {
                throw new IllegalArgumentException("Workflow node %s does not exist".formatted(workflowNodeName));
            }

            metadataMap = (Map<String, Object>) taskMap.get(METADATA);

            if (metadataMap == null) {
                metadataMap = new HashMap<>();

                ((Map<String, Object>) taskMap).put(METADATA, metadataMap);
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
    private static Map<String, ?> getTask(String workflowNodeName, List<Map<String, ?>> tasksMaps) {
        for (Map<String, ?> taskMap : tasksMaps) {
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
                if (entry.getValue() instanceof Map<?, ?> curMap) {
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
                } else if (entry.getValue() instanceof List<?> curList && !curList.isEmpty() &&
                    curList.getFirst() instanceof Map<?, ?>) {

                    for (Object item : curList) {
                        Map<String, ?> curTask = (Map<String, ?>) item;

                        if (curTask == null || !curTask.containsKey(WorkflowConstants.NAME) &&
                            !curTask.containsKey(WorkflowConstants.PARAMETERS)) {

                            continue;
                        }

                        Map<String, ?> curTaskMap = getTask(workflowNodeName, List.of(curTask));

                        if (curTaskMap != null) {
                            return curTaskMap;
                        }
                    }
                }
            }
        }

        return null;
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
    private WorkflowNodeStructure getWorkflowNodeStructure(String workflowNodeName, Map<String, ?> definitionMap) {
        Map<String, ?> parameterMap;
        List<? extends BaseProperty> properties;

        Map<String, ?> triggerMap = getTrigger(
            workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowExtConstants.TRIGGERS));

        WorkflowNodeStructure.OperationType operationType;

        if (triggerMap == null) {
            Map<String, ?> taskMap = getTask(
                workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowConstants.TASKS));

            if (taskMap == null) {
                throw new IllegalArgumentException("Workflow node %s does not exist".formatted(workflowNodeName));
            }

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
            operationType = WorkflowNodeStructure.OperationType.TRIGGER;
            parameterMap = (Map<String, ?>) triggerMap.get(WorkflowConstants.PARAMETERS);
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(
                (String) triggerMap.get(WorkflowConstants.TYPE));

            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                workflowNodeType.name(), workflowNodeType.version(),
                workflowNodeType.operation());

            properties = triggerDefinition.getProperties();
        }

        return new WorkflowNodeStructure(operationType, parameterMap, properties);
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

    private void setDynamicPropertyTypeItem(String path, String type, Map<String, Object> metadataMap) {
        Map<String, Object> dynamicPropertyTypesMap = getDynamicPropertyTypesMap(metadataMap);

        if (type == null) {
            dynamicPropertyTypesMap.remove(path);

            if (path.contains("[")) {
                List<Integer> pathIndexes = extractIndexes(path);
                String pathPrefix = path.substring(0, path.lastIndexOf("["));

                for (String key : new HashSet<>(dynamicPropertyTypesMap.keySet())) {
                    List<Integer> keyIndexes = extractIndexes(key);

                    if (key.startsWith(pathPrefix) && pathIndexes.getLast() < keyIndexes.getLast()) {
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
        for (Integer index : indexes) {
            expression = expression.replaceFirst("\\[index]", "[" + index + "]");
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
                    if (map.containsKey(pathItem)) {
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

    @SuppressFBWarnings("EI")
    private record WorkflowNodeStructure(
        OperationType operationType, Map<String, ?> parameterMap, List<? extends BaseProperty> properties) {

        enum OperationType {
            TASK,
            TASK_DISPATCHER,
            TRIGGER
        }
    }
}
