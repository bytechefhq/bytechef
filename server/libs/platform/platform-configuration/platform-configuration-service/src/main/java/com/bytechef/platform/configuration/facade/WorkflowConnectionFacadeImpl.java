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

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.registry.definition.WorkflowNodeType;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowConnectionFacadeImpl implements WorkflowConnectionFacade {

    private final ComponentDefinitionService componentDefinitionService;

    public WorkflowConnectionFacadeImpl(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public List<WorkflowConnection> getWorkflowConnections(WorkflowTask workflowTask) {
        return getWorkflowConnections(
            workflowTask.getName(), workflowTask.getType(), workflowTask.getExtensions());
    }

    @Override
    public List<WorkflowConnection> getWorkflowConnections(WorkflowTrigger workflowTrigger) {
        return getWorkflowConnections(
            workflowTrigger.getName(), workflowTrigger.getType(), workflowTrigger.getExtensions());
    }

    private List<WorkflowConnection> getWorkflowConnections(
        String name, WorkflowNodeType workflowNodeType) {

        List<WorkflowConnection> workflowConnections = new ArrayList<>();

        if (workflowNodeType.componentOperationName() != null) {
            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion());

            for (String workflowConnectionKey : componentDefinition.getWorkflowConnectionKeys()) {
                workflowConnections.add(new WorkflowConnection(
                    workflowNodeType.componentName(), workflowNodeType.componentVersion(), name,
                    workflowConnectionKey, componentDefinition.isConnectionRequired()));
            }
        }

        return workflowConnections;
    }

    private List<WorkflowConnection> getWorkflowConnections(
        String workflowNodeName, String type, Map<String, Object> extensions) {

        List<WorkflowConnection> workflowConnections;
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        if (MapUtils.containsKey(extensions, WorkflowExtConstants.CONNECTIONS)) {
            workflowConnections = toList(
                MapUtils.getMap(extensions, WorkflowExtConstants.CONNECTIONS, new TypeReference<>() {}, Map.of()),
                workflowNodeType.componentName(), workflowNodeType.componentVersion(), workflowNodeName);
        } else {
            workflowConnections = getWorkflowConnections(workflowNodeName, workflowNodeType);
        }

        return workflowConnections;
    }

    private List<WorkflowConnection> toList(
        Map<String, Map<String, Object>> connections, String componentName, int componentVersion,
        String workflowNodeName) {

        return connections
            .entrySet()
            .stream()
            .map(entry -> {
                Map<String, Object> connectionMap = entry.getValue();

                if ((!connectionMap.containsKey(WorkflowExtConstants.COMPONENT_NAME) ||
                    !connectionMap.containsKey(WorkflowExtConstants.COMPONENT_VERSION))) {

                    throw new IllegalStateException(
                        "%s and %s must be set".formatted(
                            WorkflowExtConstants.COMPONENT_NAME, WorkflowExtConstants.COMPONENT_VERSION));
                }

                return new WorkflowConnection(
                    MapUtils.getString(connectionMap, WorkflowExtConstants.COMPONENT_NAME, componentName),
                    MapUtils.getInteger(connectionMap, WorkflowExtConstants.COMPONENT_VERSION, componentVersion),
                    workflowNodeName, entry.getKey(),
                    MapUtils.getBoolean(connectionMap, WorkflowExtConstants.AUTHORIZATION_REQUIRED, false));
            })
            .toList();
    }
}
