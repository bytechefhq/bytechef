/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.connection;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectionEnvironmentMapper {

    private static final String NOT_VISIBLE_MESSAGE = "Target connection is not visible in this workspace";

    private final ConnectionService connectionService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectionEnvironmentMapper(
        ConnectionService connectionService, WorkspaceConnectionFacade workspaceConnectionFacade) {

        this.connectionService = connectionService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    public Map<Long, Long> suggest(long workspaceId, Set<Long> sourceConnectionIds, Environment targetEnvironment) {
        if (sourceConnectionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> suggestedConnectionMappings = new HashMap<>();

        for (Connection sourceConnection : connectionService.getConnections(sortedIds(sourceConnectionIds))) {
            List<ConnectionDTO> targetEnvironmentCandidates = workspaceConnectionFacade.getConnections(
                workspaceId, sourceConnection.getComponentName(), sourceConnection.getConnectionVersion(),
                (long) targetEnvironment.ordinal(), null);

            List<ConnectionDTO> sameNameCandidates = targetEnvironmentCandidates.stream()
                .filter(candidate -> Objects.equals(candidate.name(), sourceConnection.getName()))
                .toList();

            if (sameNameCandidates.size() == 1) {
                ConnectionDTO matchedCandidate = sameNameCandidates.getFirst();

                suggestedConnectionMappings.put(sourceConnection.getId(), matchedCandidate.id());
            }
        }

        return suggestedConnectionMappings;
    }

    public Map<Long, Long> validate(long workspaceId, Environment targetEnvironment, Map<Long, Long> requested) {
        if (requested.isEmpty()) {
            return Map.of();
        }

        Map<Long, Connection> sourceConnectionsById =
            toConnectionsById(connectionService.getConnections(sortedIds(requested.keySet())));
        Map<Long, Connection> targetConnectionsById =
            toConnectionsById(connectionService.getConnections(sortedIds(requested.values())));

        for (Map.Entry<Long, Long> requestedConnectionMapping : requested.entrySet()) {
            Connection sourceConnection = sourceConnectionsById.get(requestedConnectionMapping.getKey());
            Connection targetConnection = targetConnectionsById.get(requestedConnectionMapping.getValue());

            validateConnectionMapping(workspaceId, targetEnvironment, sourceConnection, targetConnection);
        }

        return Map.copyOf(requested);
    }

    private void validateConnectionMapping(
        long workspaceId, Environment targetEnvironment, Connection sourceConnection, Connection targetConnection) {

        if (sourceConnection == null) {
            throw invalidTargetConnection("Connection mapping references a source connection that does not exist");
        }

        if (!isVisibleInWorkspace(workspaceId, targetEnvironment, targetConnection)) {
            throw invalidTargetConnection(NOT_VISIBLE_MESSAGE);
        }

        if (targetConnection.getEnvironmentId() != targetEnvironment.ordinal()) {
            throw invalidTargetConnection(
                "Target connection %s is not in environment %s".formatted(
                    targetConnection.getId(), targetEnvironment));
        }

        if (!Objects.equals(sourceConnection.getComponentName(), targetConnection.getComponentName()) ||
            sourceConnection.getConnectionVersion() != targetConnection.getConnectionVersion()) {

            throw invalidTargetConnection(
                "Target connection %s (%s v%d) does not match source connection %s (%s v%d)".formatted(
                    targetConnection.getId(), targetConnection.getComponentName(),
                    targetConnection.getConnectionVersion(), sourceConnection.getId(),
                    sourceConnection.getComponentName(), sourceConnection.getConnectionVersion()));
        }
    }

    private boolean isVisibleInWorkspace(
        long workspaceId, Environment targetEnvironment, @Nullable Connection targetConnection) {

        if (targetConnection == null) {
            return false;
        }

        List<ConnectionDTO> visibleTargetEnvironmentConnections = workspaceConnectionFacade.getConnections(
            workspaceId, targetConnection.getComponentName(), targetConnection.getConnectionVersion(),
            (long) targetEnvironment.ordinal(), null);

        return visibleTargetEnvironmentConnections.stream()
            .anyMatch(candidate -> Objects.equals(candidate.id(), targetConnection.getId()));
    }

    private static ConfigurationException invalidTargetConnection(String message) {
        return new ConfigurationException(message, EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID);
    }

    private static Map<Long, Connection> toConnectionsById(List<Connection> connections) {
        return connections.stream()
            .collect(Collectors.toMap(Connection::getId, connection -> connection, (first, second) -> first));
    }

    private static List<Long> sortedIds(Collection<Long> connectionIds) {
        return connectionIds.stream()
            .distinct()
            .sorted()
            .toList();
    }
}
