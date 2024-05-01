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
import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.domain.DynamicPropertiesProperty;
import com.bytechef.platform.component.registry.domain.OptionsDataSource;
import com.bytechef.platform.component.registry.domain.OptionsDataSourceAware;
import com.bytechef.platform.component.registry.domain.PropertiesDataSource;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.domain.TriggerDefinition;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeParameterFacadeImpl implements WorkflowNodeParameterFacade {

    private final ActionDefinitionService actionDefinitionService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeParameterFacadeImpl(
        ActionDefinitionService actionDefinitionService, TriggerDefinitionService triggerDefinitionService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionService = actionDefinitionService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> deleteParameter(
        String workflowId, String workflowNodeName, String path, String name, Integer arrayIndex) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        ParameterMapPropertiesResult result =
            getParameterMapProperties(workflowNodeName, (Map<String, Object>) definitionMap);

        Map<String, ?> parameterMap = result.parameterMap;

        deleteParameter(path, name, arrayIndex, (Map<String, Object>) parameterMap);

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());

        return parameterMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Boolean> getDisplayConditions(String workflowId, String workflowNodeName) {
        Map<String, Boolean> displayConditionMap = new HashMap<>();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        ParameterMapPropertiesResult result = getParameterMapProperties(
            workflowNodeName, (Map<String, Object>) definitionMap);

        for (String name : result.parameterMap.keySet()) {
            displayConditionMap.putAll(
                getDisplayConditions(
                    workflowNodeName, name, result.properties, workflow, result.parameterMap, result.taskParameters));
        }

        return displayConditionMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public UpdateParameterResult updateParameter(
        String workflowId, String workflowNodeName, String path, String name, Integer arrayIndex, Object value) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        ParameterMapPropertiesResult result = getParameterMapProperties(
            workflowNodeName, (Map<String, Object>) definitionMap);

        Map<String, Boolean> displayConditionMap = Map.of();

        updateParameter(path, name, arrayIndex, value, (Map<String, Object>) result.parameterMap);

        // dependOn list should not contain paths inside arrays

        if (arrayIndex == null) {
            checkDependOn(name, result.properties(), result.parameterMap);

            displayConditionMap = getDisplayConditions(
                workflowNodeName, name, result.properties, workflow, result.parameterMap, result.taskParameters);
        }

        workflowService.update(workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap),
            workflow.getVersion());

        return new UpdateParameterResult(displayConditionMap, result.parameterMap);
    }

    // For now only check the first, root level of properties on which other properties could depend on
    private void checkDependOn(String name, List<? extends Property> properties, Map<String, ?> parameterMap) {
        for (Property property : properties) {
            List<String> dependOnPropertyNames = List.of();

            if (property instanceof OptionsDataSourceAware optionsDataSourceAware) {
                OptionsDataSource optionsDataSource = optionsDataSourceAware.getOptionsDataSource();

                if (optionsDataSource == null) {
                    continue;
                }

                dependOnPropertyNames = optionsDataSource.getLoadOptionsDependsOn();
            } else if (property instanceof DynamicPropertiesProperty dynamicPropertiesProperty) {
                PropertiesDataSource propertiesDataSource = dynamicPropertiesProperty.getPropertiesDataSource();

                if (propertiesDataSource == null) {
                    continue;
                }

                dependOnPropertyNames = propertiesDataSource.getLoadPropertiesDependsOn();
            }

            if (dependOnPropertyNames.contains(name)) {
                parameterMap.remove(property.getName());
            }
        }
    }

    // For now only check the first, root level of properties on which other properties could depend on
    @SuppressWarnings("unchecked")
    private Map<String, Boolean> getDisplayConditions(
        String workflowNodeName, String name, List<? extends Property> properties, Workflow workflow,
        Map<String, ?> parameterMap, boolean taskParameters) {

        Map<String, Boolean> resultMap = new HashMap<>();

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflow.getId());

        for (Property property : properties) {
            if (property.getDisplayCondition() == null) {
                continue;
            }

            String displayCondition = property.getDisplayCondition();

            if (displayCondition != null && displayCondition.contains(name)) {
                parameterMap.remove(property.getName());
            }

            boolean result;

            if (taskParameters) {
                WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                Map<String, ?> outputs = workflowNodeOutputFacade.getWorkflowNodeSampleOutputs(
                    workflow.getId(), workflowTask.getName());

                result = evaluate(
                    displayCondition,
                    MapUtils.concat(
                        MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs),
                        (Map<String, Object>) parameterMap));
            } else {
                result = evaluate(
                    displayCondition,
                    MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) parameterMap));
            }

            resultMap.put(displayCondition, result);
        }

        return resultMap;
    }

    private boolean evaluate(String displayCondition, Map<String, ?> inputParameters) {
        Map<String, Object> result = Evaluator.evaluate(
            Map.of("displayCondition", "${" + displayCondition + "}"), inputParameters);

        Object displayConditionResult = result.get("displayCondition");

        return !(displayConditionResult instanceof String) && (boolean) displayConditionResult;
    }

    @SuppressWarnings("unchecked")
    private ParameterMapPropertiesResult getParameterMapProperties(
        String workflowNodeName, Map<String, Object> definitionMap) {

        Map<String, ?> parameterMap;
        List<? extends Property> properties;

        Map<String, ?> triggerMap = getTrigger(
            workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowExtConstants.TRIGGERS));

        if (triggerMap == null) {
            Map<String, ?> taskMap = getTask(
                workflowNodeName, (List<Map<String, ?>>) definitionMap.get(WorkflowConstants.TASKS));

            if (taskMap == null) {
                throw new IllegalArgumentException("Workflow node %s does not exist".formatted(workflowNodeName));
            }

            parameterMap = (Map<String, ?>) taskMap.get(WorkflowConstants.PARAMETERS);
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(
                (String) taskMap.get(WorkflowConstants.TYPE));

            ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName());

            properties = actionDefinition.getProperties();
        } else {
            parameterMap = (Map<String, ?>) triggerMap.get(WorkflowConstants.PARAMETERS);
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(
                (String) triggerMap.get(WorkflowConstants.TYPE));

            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName());

            properties = triggerDefinition.getProperties();
        }

        return new ParameterMapPropertiesResult(parameterMap, properties, triggerMap == null);
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
                } else if (entry.getValue() instanceof List<?> curList) {
                    if (!curList.isEmpty() && curList.getFirst() instanceof Map<?, ?>) {
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
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getTrigger(String workflowNodeName, List<Map<String, ?>> triggerMaps) {
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
    private void deleteParameter(String path, String name, Integer arrayIndex, Map<String, Object> parameterMap) {
        if (StringUtils.isEmpty(path)) {
            if (arrayIndex == null) {
                parameterMap.remove(name);
            } else {
                throw new IllegalStateException("Either path or name must be defined");
            }
        } else {
            String[] pathItems = path.split("\\.");

            Object subParameter = parameterMap;
            Object parentSubParameter;

            for (int i = 0; i < pathItems.length; i++) {
                String pathItem = pathItems[i];

                parentSubParameter = subParameter;
                subParameter = ((Map<String, Object>) subParameter).get(pathItem);

                if (subParameter == null) {
                    if (arrayIndex == null || i < pathItems.length - 1) {
                        subParameter = new HashMap<>();

                        ((Map<String, Object>) parentSubParameter).put(pathItem, subParameter);
                    } else {
                        subParameter = new ArrayList<>();

                        ((Map<String, Object>) parentSubParameter).put(pathItem, subParameter);
                    }
                }
            }

            if (arrayIndex == null) {
                ((Map<String, Object>) subParameter).remove(name);
            } else {
                List<Object> subParameters = (List<Object>) subParameter;

                if (name == null) {
                    subParameters.remove((int) arrayIndex);
                } else {
                    Map<String, Object> subParameterMap = (Map<String, Object>) subParameters.get(arrayIndex);

                    subParameterMap.remove(name);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateParameter(
        String path, String name, Integer arrayIndex, Object value, Map<String, Object> parameterMap) {

        if (StringUtils.isEmpty(path)) {
            if (arrayIndex == null) {
                parameterMap.put(name, value);
            } else {
                throw new IllegalStateException("Either path or name must be defined");
            }
        } else {
            String[] pathItems = path.split("\\.");

            Object subParameter = parameterMap;
            Object parentSubParameter;

            for (int i = 0; i < pathItems.length; i++) {
                String pathItem = pathItems[i];

                parentSubParameter = subParameter;
                subParameter = ((Map<String, Object>) subParameter).get(pathItem);

                if (subParameter == null) {
                    if (arrayIndex == null || i < pathItems.length - 1) {
                        subParameter = new HashMap<>();

                        ((Map<String, Object>) parentSubParameter).put(pathItem, subParameter);
                    } else {
                        subParameter = new ArrayList<>();

                        ((Map<String, Object>) parentSubParameter).put(pathItem, subParameter);
                    }
                }
            }

            if (arrayIndex == null) {
                ((Map<String, Object>) subParameter).put(name, value);
            } else {
                List<Object> subParameters = (List<Object>) subParameter;

                if (name == null) {
                    if (arrayIndex >= subParameters.size()) {
                        for (int i = subParameters.size() - 1; i < arrayIndex; i++) {
                            subParameters.add(null);
                        }
                    }

                    subParameters.set(arrayIndex, value);
                } else {
                    if (arrayIndex >= subParameters.size()) {
                        for (int i = subParameters.size() - 1; i < arrayIndex; i++) {
                            subParameters.add(new HashMap<>());
                        }
                    }

                    Map<String, Object> subParameterMap = (Map<String, Object>) subParameters.get(arrayIndex);

                    subParameterMap.put(name, value);
                }
            }
        }
    }

    @SuppressFBWarnings("EI")
    private record ParameterMapPropertiesResult(
        Map<String, ?> parameterMap, List<? extends Property> properties, boolean taskParameters) {
    }
}
