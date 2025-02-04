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
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactoryResolver;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ComponentConnectionFacadeImpl implements ComponentConnectionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ComponentConnectionFactoryResolver componentConnectionFactoryResolver;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ComponentConnectionFacadeImpl(
        ComponentDefinitionService componentDefinitionService,
        ComponentConnectionFactoryResolver componentConnectionFactoryResolver, WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.componentConnectionFactoryResolver = componentConnectionFactoryResolver;
        this.workflowService = workflowService;
    }

    @Override
    public ComponentConnection getComponentConnection(String workflowId, String workflowNodeName, String key) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return WorkflowTrigger.fetch(workflow, workflowNodeName)
            .map(this::getComponentConnections)
            .orElseGet(() -> {
                WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                return getComponentConnections(workflowTask);
            })
            .stream()
            .filter(workflowConnection -> Objects.equals(workflowConnection.key(), key))
            .findFirst()
            .orElseThrow();
    }

    @Override
    public List<ComponentConnection> getComponentConnections(WorkflowTask workflowTask) {
        return getComponentConnections(workflowTask.getName(), workflowTask.getType(), workflowTask.getExtensions());
    }

    @Override
    public List<ComponentConnection> getComponentConnections(WorkflowTrigger workflowTrigger) {
        return getComponentConnections(
            workflowTrigger.getName(), workflowTrigger.getType(), workflowTrigger.getExtensions());
    }

    private List<ComponentConnection> getComponentConnections(
        String workflowNodeName, String type, Map<String, ?> extensions) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        if (workflowNodeType.componentOperationName() == null) {
            return Collections.emptyList();
        }

        return componentDefinitionService
            .fetchComponentDefinition(workflowNodeType.componentName(), workflowNodeType.componentVersion())
            .map(componentDefinition -> componentConnectionFactoryResolver.resolve(componentDefinition)
                .map(workflowConnectionFactory -> workflowConnectionFactory.create(
                    workflowNodeName, extensions, componentDefinition))
                .orElse(List.of()))
            .orElse(List.of());
    }
}
