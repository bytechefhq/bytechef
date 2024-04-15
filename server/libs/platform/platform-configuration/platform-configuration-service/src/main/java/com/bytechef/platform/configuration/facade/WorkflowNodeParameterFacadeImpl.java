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
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
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
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeParameterFacadeImpl(
        ActionDefinitionService actionDefinitionService, TriggerDefinitionService triggerDefinitionService,
        WorkflowService workflowService) {

        this.actionDefinitionService = actionDefinitionService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowService = workflowService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> updateParameter(
        String workflowId, String workflowNodeName, String path, String name, Integer arrayIndex, Object value) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, ?> definitionMap = JsonUtils.readMap(workflow.getDefinition());

        Result result = getParameterMapProperties(workflowNodeName, (Map<String, Object>) definitionMap);

        Map<String, ?> parameterMap = result.parameterMap;

        updateParameter(path, name, arrayIndex, value, (Map<String, Object>) parameterMap);

        // dependOn list should not contain paths inside arrays

        if (arrayIndex == null) {
            checkDependOn(name, result.properties(), result.parameterMap());
        }

        workflowService.update(workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap),
            workflow.getVersion());

        return parameterMap;
    }

    // for now only check the first, root level of properties on which other properties could depend on
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

    @SuppressWarnings("unchecked")
    private Result getParameterMapProperties(
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

        return new Result(parameterMap, properties);
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

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> curMap) {
                    if (curMap.containsKey(WorkflowConstants.NAME)) {
                        return getTask(workflowNodeName, List.of((Map<String, ?>) curMap));
                    } else {
                        for (Map.Entry<?, ?> curMapEntry : curMap.entrySet()) {
                            if (curMapEntry.getValue() instanceof Map<?, ?> curTask) {
                                if (!curTask.containsKey(WorkflowConstants.NAME) &&
                                    !curTask.containsKey(WorkflowConstants.PARAMETERS)) {

                                    continue;
                                }

                                return getTask(workflowNodeName, List.of((Map<String, ?>) curTask));
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
                            return getTask(workflowNodeName, List.of(curTask));
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

    private record Result(Map<String, ?> parameterMap, List<? extends Property> properties) {
    }
}
