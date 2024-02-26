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
import com.bytechef.platform.component.constant.ScriptConstants;
import com.bytechef.platform.component.definition.DataStreamComponentDefinition;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.DataStream;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
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

    private List<WorkflowConnection> getWorkflowConnections(
        String workflowNodeName, String type, Map<String, ?> extensions, boolean workflowTask) {

        List<WorkflowConnection> workflowConnections = List.of();
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        if (workflowNodeType.componentOperationName() == null) {
            return Collections.emptyList();
        }

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            workflowNodeType.componentName(), workflowNodeType.componentVersion());

        if (workflowTask
            && StringUtils.startsWith(componentDefinition.getName(), DataStreamComponentDefinition.DATA_STREAM)) {
            workflowConnections = getWorkflowConnections(DataStream.of(extensions), workflowNodeName);
        } else if (workflowTask && StringUtils.startsWith(componentDefinition.getName(), ScriptConstants.SCRIPT)) {
            workflowConnections = WorkflowConnection.of(extensions, workflowNodeName);
        } else if (componentDefinition.getConnection() != null) {
            workflowConnections = List.of(
                WorkflowConnection.of(workflowNodeName, workflowNodeType, componentDefinition));
        }

        return workflowConnections;
    }

    private List<WorkflowConnection> getWorkflowConnections(DataStream dataStream, String workflowNodeName) {
        List<WorkflowConnection> workflowConnections = new ArrayList<>();

        if (dataStream != null) {
            if (dataStream.source() != null) {
                WorkflowConnection workflowConnection = getWorkflowConnection(
                    workflowNodeName,
                    StringUtils.lowerCase(DataStreamComponentDefinition.ComponentType.SOURCE.name()),
                    dataStream.source());

                if (workflowConnection != null) {
                    workflowConnections.add(workflowConnection);
                }
            }

            if (dataStream.destination() != null) {
                WorkflowConnection workflowConnection = getWorkflowConnection(
                    workflowNodeName,
                    StringUtils.lowerCase(DataStreamComponentDefinition.ComponentType.DESTINATION.name()),
                    dataStream.destination());

                if (workflowConnection != null) {
                    workflowConnections.add(workflowConnection);
                }
            }
        }

        return workflowConnections;
    }

    private WorkflowConnection getWorkflowConnection(
        String workflowNodeName, String workflowConnectionKey, DataStream.DataStreamComponent dataStreamComponent) {

        WorkflowConnection workflowConnection = null;

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            dataStreamComponent.componentName(), dataStreamComponent.componentVersion());

        if (componentDefinition.getConnection() != null) {
            workflowConnection = WorkflowConnection.of(workflowNodeName, workflowConnectionKey, componentDefinition);
        }

        return workflowConnection;
    }
}
