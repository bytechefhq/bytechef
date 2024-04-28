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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.workflow.connection.WorkflowConnectionFactoryResolver;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowConnectionFacadeImpl implements WorkflowConnectionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final WorkflowConnectionFactoryResolver workflowConnectionFactoryResolver;

    public WorkflowConnectionFacadeImpl(
        ComponentDefinitionService componentDefinitionService,
        WorkflowConnectionFactoryResolver workflowConnectionFactoryResolver) {

        this.componentDefinitionService = componentDefinitionService;
        this.workflowConnectionFactoryResolver = workflowConnectionFactoryResolver;
    }

    @Override
    public WorkflowConnection getWorkflowConnection(Workflow workflow, String workflowNodeName, String key) {
        return WorkflowTrigger.fetch(workflow, workflowNodeName)
            .map(this::getWorkflowConnections)
            .orElseGet(() -> {
                WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                return getWorkflowConnections(workflowTask);
            })
            .stream()
            .filter(workflowConnection -> Objects.equals(workflowConnection.key(), key))
            .findFirst()
            .orElseThrow();
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
        String workflowNodeName, String type, Map<String, ?> extensions) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        if (workflowNodeType.componentOperationName() == null) {
            return Collections.emptyList();
        }

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            workflowNodeType.componentName(), workflowNodeType.componentVersion());

        return workflowConnectionFactoryResolver
            .resolve(componentDefinition)
            .map(workflowConnectionFactory -> workflowConnectionFactory.create(
                workflowNodeName, extensions, componentDefinition))
            .orElse(List.of());
    }
}
