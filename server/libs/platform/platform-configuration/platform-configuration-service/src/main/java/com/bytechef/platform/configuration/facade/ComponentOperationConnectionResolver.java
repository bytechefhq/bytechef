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

import com.bytechef.platform.configuration.domain.ClusterElementNode;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class ComponentOperationConnectionResolver {

    private final ComponentConnectionFacade componentConnectionFacade;
    private final ConnectionService connectionService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    ComponentOperationConnectionResolver(
        ComponentConnectionFacade componentConnectionFacade, ConnectionService connectionService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.componentConnectionFacade = componentConnectionFacade;
        this.connectionService = connectionService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    Optional<ResolvedConnection> resolve(
        String workflowId, String workflowNodeName, @Nullable ClusterElementNode clusterElementNode,
        String componentName, @Nullable String connectionKey, long environmentId) {

        List<ComponentConnection> declaredComponentConnections;

        try {
            declaredComponentConnections = getDeclaredComponentConnections(
                workflowId, workflowNodeName, clusterElementNode);
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }

        List<ComponentConnection> componentConnections = declaredComponentConnections.stream()
            .filter(componentConnection -> Objects.equals(componentConnection.componentName(), componentName))
            .filter(componentConnection -> connectionKey == null || Objects.equals(
                componentConnection.key(), connectionKey))
            .toList();

        if (componentConnections.isEmpty()) {
            return connectionKey == null ? Optional.of(new ResolvedConnection(null)) : Optional.empty();
        }

        if (componentConnections.size() > 1) {
            return Optional.empty();
        }

        ComponentConnection componentConnection = componentConnections.getFirst();

        return workflowTestConfigurationService
            .getWorkflowTestConfigurationConnections(workflowId, workflowNodeName, environmentId)
            .stream()
            .filter(workflowTestConfigurationConnection -> Objects.equals(
                workflowTestConfigurationConnection.getWorkflowConnectionKey(), componentConnection.key()))
            .map(WorkflowTestConfigurationConnection::getConnectionId)
            .filter(Objects::nonNull)
            .findFirst()
            .filter(connectionId -> isConnectionOfComponent(connectionId, componentName))
            .map(ResolvedConnection::new);
    }

    private List<ComponentConnection> getDeclaredComponentConnections(
        String workflowId, String workflowNodeName, @Nullable ClusterElementNode clusterElementNode) {

        if (clusterElementNode == null) {
            return componentConnectionFacade.getWorkflowNodeComponentConnections(workflowId, workflowNodeName);
        }

        return componentConnectionFacade.getClusterElementComponentConnections(
            workflowId, workflowNodeName, clusterElementNode.clusterElementTypeName(),
            clusterElementNode.clusterElementWorkflowNodeName());
    }

    private boolean isConnectionOfComponent(long connectionId, String componentName) {
        return connectionService.fetchConnection(connectionId)
            .map(Connection::getComponentName)
            .filter(connectionComponentName -> Objects.equals(connectionComponentName, componentName))
            .isPresent();
    }

    record ResolvedConnection(@Nullable Long connectionId) {
    }
}
