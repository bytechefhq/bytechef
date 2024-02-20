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
import com.bytechef.platform.component.definition.DataStreamComponentDefinition;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.DataStream;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
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
            workflowTask.getName(), workflowTask.getType(), workflowTask.getExtensions(), true);
    }

    @Override
    public List<WorkflowConnection> getWorkflowConnections(WorkflowTrigger workflowTrigger) {
        return getWorkflowConnections(
            workflowTrigger.getName(), workflowTrigger.getType(), workflowTrigger.getExtensions(), false);
    }

    public List<WorkflowConnection> getWorkflowConnections(
        String workflowNodeName, WorkflowNodeType workflowNodeType, Map<String, ?> extensions, boolean workflowTask) {

        List<WorkflowConnection> workflowConnections = new ArrayList<>();

        if (workflowNodeType.componentOperationName() != null) {
            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion());

            if (workflowTask && componentDefinition instanceof DataStreamComponentDefinition) {
                workflowConnections.addAll(
                    getWorkflowConnections(
                        DataStream.of(extensions), workflowNodeName,
                        componentDefinitionService.getComponentDefinitions()));
            } else {
                workflowConnections.add(WorkflowConnection.of(workflowNodeName, workflowNodeType, componentDefinition));
            }
        }

        return workflowConnections;
    }

    private List<WorkflowConnection> getWorkflowConnections(
        String workflowNodeName, String type, Map<String, ?> extensions, boolean workflowTask) {

        List<WorkflowConnection> workflowConnections;
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        if (MapUtils.containsKey(extensions, WorkflowExtConstants.CONNECTIONS)) {
            workflowConnections = WorkflowConnection.of(
                MapUtils.getMap(extensions, WorkflowExtConstants.CONNECTIONS, new TypeReference<>() {}, Map.of()),
                workflowNodeType.componentName(), workflowNodeType.componentVersion(), workflowNodeName);
        } else {
            workflowConnections = getWorkflowConnections(workflowNodeName, workflowNodeType, extensions, workflowTask);
        }

        return workflowConnections;
    }

    private List<WorkflowConnection> getWorkflowConnections(
        DataStream dataStream, String workflowNodeName, List<ComponentDefinition> componentDefinitions) {

        List<WorkflowConnection> workflowConnections = new ArrayList<>();

        if (dataStream != null) {
            if (dataStream.source() != null) {
                workflowConnections.addAll(
                    WorkflowConnection.of(workflowNodeName, dataStream.source(), componentDefinitions));
            }

            if (dataStream.destination() != null) {
                workflowConnections.addAll(
                    WorkflowConnection.of(workflowNodeName, dataStream.destination(), componentDefinitions));
            }
        }

        return workflowConnections;
    }
}
