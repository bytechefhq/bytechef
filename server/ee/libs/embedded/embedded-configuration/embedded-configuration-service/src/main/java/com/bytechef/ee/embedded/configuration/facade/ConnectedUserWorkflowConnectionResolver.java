/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.ee.embedded.configuration.exception.ConnectionNotEntitledException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserWorkflowConnectionResolver {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;
    private final ConnectionService connectionService;
    private final WorkflowConnectionSlot workflowConnectionSlot;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowConnectionResolver(
        ConnectedUserConnectionFacade connectedUserConnectionFacade, ConnectionService connectionService,
        WorkflowConnectionSlot workflowConnectionSlot) {

        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
        this.connectionService = connectionService;
        this.workflowConnectionSlot = workflowConnectionSlot;
    }

    public ResolvedWorkflowConnections resolve(
        String workflowId, long connectedUserId, Map<String, Long> requestedConnectionIds,
        List<ProjectDeploymentWorkflowConnection> currentConnections, Map<String, Long> providedConnectionIds) {

        Map<String, List<Long>> entitledConnectionIdsByComponentName = new HashMap<>();
        Map<String, Optional<Long>> providedConnectionIdsByComponentName = new HashMap<>();
        List<ProjectDeploymentWorkflowConnection> connections = new ArrayList<>();
        Set<String> missingComponentNames = new LinkedHashSet<>();

        for (ComponentConnection slot : workflowConnectionSlot.getSlots(workflowId)) {
            String componentName = slot.componentName();

            Optional<Long> providedConnectionId = providedConnectionIdsByComponentName.computeIfAbsent(
                componentName, name -> fetchExistingConnectionId(providedConnectionIds.get(name)));

            Long connectionId;

            if (providedConnectionId.isPresent()) {
                connectionId = providedConnectionId.get();
            } else {
                List<Long> entitledConnectionIds = entitledConnectionIdsByComponentName.computeIfAbsent(
                    componentName, name -> getEntitledConnectionIds(connectedUserId, name));

                connectionId = selectConnectionId(
                    componentName, entitledConnectionIds, requestedConnectionIds.get(componentName),
                    currentConnections);
            }

            if (connectionId == null) {
                if (slot.required()) {
                    missingComponentNames.add(componentName);
                }

                continue;
            }

            connections.add(new ProjectDeploymentWorkflowConnection(connectionId, slot.key(), slot.workflowNodeName()));
        }

        return new ResolvedWorkflowConnections(connections, List.copyOf(missingComponentNames));
    }

    public Set<String> getProvidedComponentNames(Map<String, Long> providedConnectionIds) {
        Set<String> providedComponentNames = new LinkedHashSet<>();

        for (Map.Entry<String, Long> entry : providedConnectionIds.entrySet()) {
            Optional<Long> connectionId = fetchExistingConnectionId(entry.getValue());

            if (connectionId.isPresent()) {
                providedComponentNames.add(entry.getKey());
            }
        }

        return providedComponentNames;
    }

    private Optional<Long> fetchExistingConnectionId(@Nullable Long connectionId) {
        if (connectionId == null) {
            return Optional.empty();
        }

        return connectionService.fetchConnection(connectionId)
            .map(Connection::getId);
    }

    private List<Long> getEntitledConnectionIds(long connectedUserId, String componentName) {
        return connectedUserConnectionFacade.getConnections(connectedUserId, componentName, List.of())
            .stream()
            .map(ConnectionDTO::id)
            .toList();
    }

    @Nullable
    private static Long selectConnectionId(
        String componentName, List<Long> entitledConnectionIds, @Nullable Long requestedConnectionId,
        List<ProjectDeploymentWorkflowConnection> currentConnections) {
        if (requestedConnectionId != null) {
            if (!entitledConnectionIds.contains(requestedConnectionId)) {
                throw new ConnectionNotEntitledException(componentName, requestedConnectionId);
            }

            return requestedConnectionId;
        }

        for (ProjectDeploymentWorkflowConnection currentConnection : currentConnections) {
            if (entitledConnectionIds.contains(currentConnection.getConnectionId())) {
                return currentConnection.getConnectionId();
            }
        }

        return entitledConnectionIds.isEmpty() ? null : entitledConnectionIds.getFirst();
    }
}
