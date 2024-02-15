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
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.definition.WorkflowNodeType;
import com.bytechef.platform.component.registry.domain.ConnectionDefinition;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowConnectionFacadeImpl implements WorkflowConnectionFacade {

    private final ConnectionDefinitionService connectionDefinitionService;

    public WorkflowConnectionFacadeImpl(ConnectionDefinitionService connectionDefinitionService) {
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @Override
    public List<WorkflowConnection> getWorkflowConnections(WorkflowTask workflowTask) {
        return getWorkflowConnections(
            workflowTask.getType(), (connectionRequired) -> getWorkflowConnections(
                workflowTask.getName(), workflowTask.getType(), workflowTask.getExtensions(), connectionRequired));
    }

    @Override
    public List<WorkflowConnection> getWorkflowConnections(WorkflowTrigger workflowTrigger) {
        return getWorkflowConnections(
            workflowTrigger.getType(),
            (connectionRequired) -> getWorkflowConnections(
                workflowTrigger.getName(), workflowTrigger.getType(), workflowTrigger.getExtensions(),
                connectionRequired));
    }

    private static List<WorkflowConnection> getWorkflowConnections(
        String name, WorkflowNodeType workflowNodeType, boolean connectionRequired) {

        return List.of(
            new WorkflowConnection(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(), name,
                workflowNodeType.componentName(),
                null, connectionRequired));
    }

    private static List<WorkflowConnection> getWorkflowConnections(
        String workflowNodeName, String type, Map<String, Object> extensions, boolean connectionRequired) {

        List<WorkflowConnection> workflowConnections;
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        if (MapUtils.containsKey(extensions, WorkflowConnection.CONNECTIONS)) {
            workflowConnections = toList(
                MapUtils.getMap(extensions, WorkflowConnection.CONNECTIONS, new TypeReference<>() {}, Map.of()),
                workflowNodeType.componentName(), workflowNodeType.componentVersion(), workflowNodeName,
                connectionRequired);
        } else {
            workflowConnections = getWorkflowConnections(workflowNodeName, workflowNodeType, connectionRequired);
        }

        return workflowConnections;
    }

    private static List<WorkflowConnection> toList(
        Map<String, Map<String, Object>> connections, String componentName, int componentVersion,
        String workflowNodeName, boolean connectionRequired) {

        return connections
            .entrySet()
            .stream()
            .map(entry -> {
                Map<String, Object> connectionMap = entry.getValue();

                if (!connectionMap.containsKey(WorkflowConnection.ID) &&
                    (!connectionMap.containsKey(WorkflowConnection.COMPONENT_NAME) ||
                        !connectionMap.containsKey(WorkflowConnection.COMPONENT_VERSION))) {

                    throw new IllegalStateException(
                        "%s and %s must be set".formatted(
                            WorkflowConnection.COMPONENT_NAME, WorkflowConnection.COMPONENT_VERSION));
                }

                return new WorkflowConnection(
                    MapUtils.getString(connectionMap, WorkflowConnection.COMPONENT_NAME, componentName),
                    MapUtils.getInteger(connectionMap, WorkflowConnection.COMPONENT_VERSION, componentVersion),
                    workflowNodeName, entry.getKey(), MapUtils.getLong(connectionMap, WorkflowConnection.ID),
                    MapUtils.getBoolean(
                        connectionMap, WorkflowConnection.AUTHORIZATION_REQUIRED, false) || connectionRequired);
            })
            .toList();
    }

    private List<WorkflowConnection> getWorkflowConnections(
        String type, Function<Boolean, List<WorkflowConnection>> workflowConnectionsFunction) {

        List<WorkflowConnection> workflowConnections = List.of();

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        if (connectionDefinitionService.containsConnection(workflowNodeType.componentName())) {
            ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion());

            boolean propertiesRequired = CollectionUtils.anyMatch(
                connectionDefinition.getProperties(), Property::getRequired);

            workflowConnections = workflowConnectionsFunction.apply(
                connectionDefinition.isAuthorizationRequired() || propertiesRequired);
        }
        return workflowConnections;
    }
}
