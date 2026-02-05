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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactoryResolver;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
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

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ComponentConnectionFactoryResolver componentConnectionFactoryResolver;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ComponentConnectionFacadeImpl(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService,
        ComponentConnectionFactoryResolver componentConnectionFactoryResolver, WorkflowService workflowService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.componentConnectionFactoryResolver = componentConnectionFactoryResolver;
        this.workflowService = workflowService;
    }

    @Override
    public List<ComponentConnection> getClusterElementComponentConnections(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        ClusterElementMap clusterElementMap = ClusterElementMap.of(workflowTask.getExtensions());

        ClusterElementType clusterElementType = clusterElementDefinitionService.getClusterElementType(
            workflowNodeType.name(), workflowNodeType.version(), clusterElementTypeName.toUpperCase());

        ClusterElement clusterElement = clusterElementMap.getClusterElement(
            clusterElementType, clusterElementWorkflowNodeName);

        List<ComponentConnection> componentConnections = new ArrayList<>();

        ComponentConnection clusterElementConnection = getClusterElementConnection(clusterElement);

        if (clusterElementConnection != null) {
            componentConnections.add(clusterElementConnection);
        }

        componentConnections.addAll(
            ComponentConnection.of(
                clusterElement.getExtensions(), clusterElementWorkflowNodeName,
                (name, version) -> {
                    return componentDefinitionService.getComponentDefinition(name, version)
                        .isConnectionRequired();
                }));

        return componentConnections;
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

    @Override
    public List<ComponentConnection> getWorkflowNodeComponentConnections(String workflowId, String workflowNodeName) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return WorkflowTrigger.fetch(workflow, workflowNodeName)
            .map(this::getComponentConnections)
            .orElseGet(() -> {
                WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                return getComponentConnections(workflowTask);
            });
    }

    private ComponentConnection getClusterElementConnection(ClusterElement clusterElement) {
        return componentDefinitionService.fetchComponentDefinition(
            clusterElement.getComponentName(), clusterElement.getComponentVersion())
            .filter(componentDefinition -> componentDefinition.getConnection() != null)
            .map(componentDefinition -> ComponentConnection.of(
                clusterElement.getWorkflowNodeName(), clusterElement.getWorkflowNodeName(),
                componentDefinition.getName(), componentDefinition.getVersion(),
                componentDefinition.isConnectionRequired()))
            .orElse(null);
    }

    private List<ComponentConnection> getComponentConnections(
        String workflowNodeName, String type, Map<String, ?> extensions) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        if (workflowNodeType.operation() == null) {
            return Collections.emptyList();
        }

        return componentDefinitionService
            .fetchComponentDefinition(workflowNodeType.name(), workflowNodeType.version())
            .map(componentDefinition -> componentConnectionFactoryResolver.resolve(componentDefinition)
                .map(workflowConnectionFactory -> workflowConnectionFactory.create(
                    workflowNodeName, extensions, componentDefinition))
                .orElse(List.of()))
            .orElse(List.of());
    }
}
