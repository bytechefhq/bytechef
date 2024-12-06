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
import com.bytechef.platform.registry.domain.BaseProperty;
import com.bytechef.platform.workflow.task.dispatcher.registry.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.registry.service.TaskDispatcherDefinitionService;
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
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeParameterFacadeImpl implements WorkflowNodeParameterFacade {

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

        ParameterMapPropertiesResult result =
            getParameterMapProperties(workflowNodeName, definitionMap);

        String[] pathItems = path.split("\\.");

        setDynamicPropertyTypeItem(path, null, getMetadataMap(workflowNodeName, definitionMap));
        setParameter(pathItems, null, true, result.parameterMap);

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap, true), workflow.getVersion());

        return result.parameterMap;
    }

    @Override
    public Map<String, Boolean> getDisplayConditions(String workflowId, String workflowNodeName) {
        Map<String, Boolean> displayConditionMap = new HashMap<>();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        ParameterMapPropertiesResult parameterMapProperties = getParameterMapProperties(
            workflowNodeName, definitionMap);

        Set<String> keySet = parameterMapProperties.parameterMap.keySet();

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflow.getId());

        for (String name : new HashSet<>(keySet)) {
            displayConditionMap.putAll(
                checkDisplayConditionsParameters(
                    workflowNodeName, name, parameterMapProperties.properties, workflow,
                    parameterMapProperties.parameterMap, inputMap, parameterMapProperties.parameterType, Map.of()));
        }

        return displayConditionMap;
    }

    @Override
    public UpdateParameterResultDTO updateParameter(
        String workflowId, String workflowNodeName, String path, Object value, String type, boolean includeInMetadata) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        ParameterMapPropertiesResult result = getParameterMapProperties(workflowNodeName, definitionMap);

        String[] pathItems = path.split("\\.");

        setParameter(pathItems, value, false, result.parameterMap);

        Map<String, Object> metadataMap = getMetadataMap(workflowNodeName, definitionMap);

        Map<String, ?> dynamicPropertyTypesMap = getDynamicPropertyTypesMap(metadataMap);

        // For now only check the first, root level of properties on which other properties could depend on

        checkDependOn(pathItems[0], result.properties(), result.parameterMap, dynamicPropertyTypesMap);

        Map<String, ?> inputMap = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflow.getId());

        Map<String, Boolean> displayConditionMap = checkDisplayConditionsParameters(
            workflowNodeName, pathItems[0], result.properties, workflow, result.parameterMap, inputMap,
            result.parameterType, dynamicPropertyTypesMap);

        if (includeInMetadata) {
            setDynamicPropertyTypeItem(path, type, metadataMap);
        }

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap, true), workflow.getVersion());

        return new UpdateParameterResultDTO(displayConditionMap, metadataMap, result.parameterMap);
    }

    protected static boolean hasExpressionVariable(String expression, String variableName) {
        if ((expression == null) || expression.isEmpty()) {
            return false;
        }

        String regex = "(^|.*\\W)" + variableName + "(\\W.*|$)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(expression);

        return matcher.find();
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

    protected static Map<String, Boolean> evaluateArray(
        String displayCondition, Map<String, Object> inputMap, Map<String, Object> outputs,
        Map<String, ?> parameterMap) {

        Map<String, Boolean> displayConditionMap = new HashMap<>();

        List<List<Integer>> indexesList = findIndexes(parameterMap, displayCondition);

        for (List<Integer> indexes : indexesList) {
            String updatedDisplayCondition = replaceIndexes(displayCondition, indexes);

            boolean result = evaluate(updatedDisplayCondition, inputMap, outputs, parameterMap);

            if (result) {
                displayConditionMap.put(updatedDisplayCondition, result);
            }
        }

        return displayConditionMap;
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

                checkDynamicPropertyTypeItem(property.getName(), dynamicPropertyTypesMap);
            }
        }
    }

    private Map<String, Boolean> checkDisplayConditionsParameters(
        String workflowNodeName, String name, List<? extends BaseProperty> properties, Workflow workflow,
        Map<String, ?> parameterMap, Map<String, ?> inputMap, ParameterMapPropertiesResult.ParameterType parameterType,
        Map<String, ?> dynamicPropertyTypesMap) {

        Map<String, Boolean> displayConditionMap = new HashMap<>();

        checkDisplayConditionsParameters(
            workflowNodeName, name, properties, workflow, parameterMap, inputMap, parameterType,
            dynamicPropertyTypesMap, displayConditionMap);

        return displayConditionMap;
    }

    @SuppressWarnings("unchecked")
    private void checkDisplayConditionsParameters(
        String workflowNodeName, String name, List<? extends BaseProperty> properties, Workflow workflow,
        Map<String, ?> parameterMap, Map<String, ?> inputMap, ParameterMapPropertiesResult.ParameterType parameterType,
        Map<String, ?> dynamicPropertyTypesMap, Map<String, Boolean> displayConditionMap) {

        for (BaseProperty property : properties) {
            if (property instanceof ArrayProperty arrayProperty) {
                checkDisplayConditionsParameters(
                    workflowNodeName, name, arrayProperty.getItems(), workflow, parameterMap, inputMap,
                    parameterType, dynamicPropertyTypesMap, displayConditionMap);
            }
            if (property instanceof com.bytechef.platform.workflow.task.dispatcher.registry.domain.ArrayProperty arrayProperty) {
                checkDisplayConditionsParameters(
                    workflowNodeName, name, arrayProperty.getItems(), workflow, parameterMap, inputMap,
                    parameterType, dynamicPropertyTypesMap, displayConditionMap);
            } else if (property instanceof ObjectProperty objectProperty) {
                checkDisplayConditionsParameters(
                    workflowNodeName, name, objectProperty.getProperties(), workflow, parameterMap, inputMap,
                    parameterType, dynamicPropertyTypesMap, displayConditionMap);
            } else if (property instanceof com.bytechef.platform.workflow.task.dispatcher.registry.domain.ObjectProperty objectProperty) {
                checkDisplayConditionsParameters(
                    workflowNodeName, name, objectProperty.getProperties(), workflow, parameterMap, inputMap,
                    parameterType, dynamicPropertyTypesMap, displayConditionMap);
            }

            if (property.getDisplayCondition() == null) {
                continue;
            }

            String displayCondition = property.getDisplayCondition();

            if (hasExpressionVariable(displayCondition, name)) {
                parameterMap.remove(property.getName());

                checkDynamicPropertyTypeItem(property.getName(), dynamicPropertyTypesMap);
            }

            boolean result;

            if (parameterType == ParameterMapPropertiesResult.ParameterType.TASK ||
                parameterType == ParameterMapPropertiesResult.ParameterType.TASK_DISPATCHER) {

                WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
                    workflow.getId(), workflowTask.getName());

                if (displayCondition.contains("[index]")) {
                    displayConditionMap.putAll(
                        evaluateArray(
                            displayCondition, (Map<String, Object>) inputMap, (Map<String, Object>) outputs,
                            parameterMap));
                } else {
                    result = evaluate(
                        displayCondition, (Map<String, Object>) inputMap, (Map<String, Object>) outputs, parameterMap);

                    if (result) {
                        displayConditionMap.put(displayCondition, result);
                    }
                }
            } else {
                result = evaluate(
                    displayCondition,
                    MapUtils.concat((Map<String, Object>) inputMap, (Map<String, Object>) parameterMap));

                if (result) {
                    displayConditionMap.put(displayCondition, result);
                }
            }
        }
    }

    private static void checkDynamicPropertyTypeItem(String name, Map<String, ?> dynamicPropertyTypesMap) {
        Set<String> keySet = new HashSet<>(dynamicPropertyTypesMap.keySet());

        for (String key : keySet) {
            if (key.equals(name) || key.startsWith(name + ".") || key.startsWith(name + "[")) {
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

    private static List<List<Integer>> findIndexes(Map<String, ?> map, String expression) {
        List<List<Integer>> allIndexes = new ArrayList<>();

        findIndexes(map, expression, new ArrayList<>(), allIndexes);

        return allIndexes;
    }

    private static void findIndexes(
        Object current, String expression, List<Integer> currentIndexes, List<List<Integer>> allIndexes) {

        if (expression.isEmpty()) {
            allIndexes.add(new ArrayList<>(currentIndexes));

            return;
        }

        Matcher matcher = ARRAY_INDEXES_PATTERN.matcher(expression);

        if (matcher.find()) {
            String key = matcher.group(1);
            String indexGroup = matcher.group(2);
            String remainingExpression = matcher.group(4);

            if (current instanceof Map<?, ?> currentMap) {
                if (currentMap.containsKey(key)) {
                    Object next = currentMap.get(key);

                    if (indexGroup != null && next instanceof List<?> nextList) {
                        for (int i = 0; i < nextList.size(); i++) {
                            currentIndexes.add(i);

                            findIndexes(
                                nextList.get(i), (indexGroup + remainingExpression).replaceFirst("\\[index]", ""),
                                currentIndexes, allIndexes);

                            currentIndexes.removeLast();
                        }
                    } else {
                        findIndexes(next, indexGroup + remainingExpression, currentIndexes, allIndexes);
                    }
                }
            } else if (current instanceof List<?> currentList) {
                for (int i = 0; i < currentList.size(); i++) {
                    currentIndexes.add(i);

                    findIndexes(
                        currentList.get(i), (indexGroup + remainingExpression).replaceFirst("\\[index]", ""),
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
    private ParameterMapPropertiesResult getParameterMapProperties(
        String workflowNodeName, Map<String, ?> definitionMap) {

        Map<String, ?> parameterMap;
        List<? extends BaseProperty> properties;

        Map<String, ?> triggerMap = getTrigger(
            workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowExtConstants.TRIGGERS));

        ParameterMapPropertiesResult.ParameterType parameterType;

        if (triggerMap == null) {
            Map<String, ?> taskMap = getTask(
                workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowConstants.TASKS));

            if (taskMap == null) {
                throw new IllegalArgumentException("Workflow node %s does not exist".formatted(workflowNodeName));
            }

            parameterMap = (Map<String, ?>) taskMap.get(WorkflowConstants.PARAMETERS);
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(
                (String) taskMap.get(WorkflowConstants.TYPE));

            if (workflowNodeType.componentOperationName() == null) {
                parameterType = ParameterMapPropertiesResult.ParameterType.TASK_DISPATCHER;

                TaskDispatcherDefinition taskDispatcherDefinition =
                    taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                        workflowNodeType.componentName(), workflowNodeType.componentVersion());

                properties = taskDispatcherDefinition.getProperties();
            } else {
                parameterType = ParameterMapPropertiesResult.ParameterType.TASK;

                ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                    workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                    workflowNodeType.componentOperationName());

                properties = actionDefinition.getProperties();
            }
        } else {
            parameterType = ParameterMapPropertiesResult.ParameterType.TRIGGER;
            parameterMap = (Map<String, ?>) triggerMap.get(WorkflowConstants.PARAMETERS);
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(
                (String) triggerMap.get(WorkflowConstants.TYPE));

            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName());

            properties = triggerDefinition.getProperties();
        }

        return new ParameterMapPropertiesResult(parameterMap, properties, parameterType);
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

                        if (!curTask.containsKey(WorkflowConstants.NAME) &&
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

    private void setDynamicPropertyTypeItem(
        String path, String type, Map<String, Object> metadataMap) {

        Map<String, Object> dynamicPropertyTypesMap = getDynamicPropertyTypesMap(metadataMap);

        if (type == null) {
            dynamicPropertyTypesMap.remove(path);
        } else {
            dynamicPropertyTypesMap.put(path, type);
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

    private static String replaceIndexes(String expression, List<Integer> indexes) {
        for (Integer index : indexes) {
            expression = expression.replaceFirst("\\[index]", "[" + index + "]");
        }

        return expression;
    }

    @SuppressWarnings("unchecked")
    private void setParameter(String[] pathItems, Object value, boolean removeValue, Map<String, ?> parameterMap) {
        Map<String, Object> map = (Map<String, Object>) parameterMap;

        for (int i = 0; i < pathItems.length; i++) {
            String pathItem = pathItems[i];

            if (pathItem.endsWith("]")) {
                String name = pathItem.substring(0, pathItem.indexOf("["));
                String arrays = pathItem.substring(pathItem.indexOf("["));

                List<Object> list;

                if (map.containsKey(name)) {
                    list = (List<Object>) map.get(name);
                } else {
                    list = new ArrayList<>();

                    map.put(name, list);
                }

                Pattern pattern = Pattern.compile("\\[(\\d+)]");
                Matcher matcher = pattern.matcher(arrays);

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
                        if (i == pathItems.length - 1) {
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
                if (i < pathItems.length - 1) {
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
    private record ParameterMapPropertiesResult(
        Map<String, ?> parameterMap, List<? extends BaseProperty> properties, ParameterType parameterType) {

        enum ParameterType {
            TASK,
            TASK_DISPATCHER,
            TRIGGER
        }
    }
}
