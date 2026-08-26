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
 * Re-points connections across environments for
 * {@link com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler} implementations. Connections are
 * environment-scoped, so promoting a resource from one environment to another requires every connection its workflows
 * bind to be swapped for a counterpart connection that lives in the target environment.
 *
 * <p>
 * {@link #suggest(long, Set, Environment)} pre-selects an unambiguous target-environment counterpart for each source
 * connection, for the promotion dialog to offer as a default. {@link #validate(long, Environment, Map)} then checks
 * whatever mapping the caller actually submits, since a client-supplied mapping cannot be trusted to bind a connection
 * that is in the right environment, of the matching component and version, and visible to the requesting workspace.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectionEnvironmentMapper {

    // Deliberately static and detail-free: a target the caller cannot see must be rejected the same way whether it
    // does not exist, belongs to another workspace, or belongs to a different component/version/environment than
    // what was requested — see the visibility-gate note on #validate.
    private static final String NOT_VISIBLE_MESSAGE = "Target connection is not visible in this workspace";

    private final ConnectionService connectionService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectionEnvironmentMapper(
        ConnectionService connectionService, WorkspaceConnectionFacade workspaceConnectionFacade) {

        this.connectionService = connectionService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    /**
     * @return source connection id to target connection id, for every source id that has exactly one visible
     *         {@code targetEnvironment} connection sharing its component, connection version and name
     */
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

    /**
     * Rejects any requested target connection that does not exist, is not visible to {@code workspaceId}, is not in
     * {@code targetEnvironment}, or belongs to a different component or connection version than its source. The
     * target's name is deliberately not compared against the source's: {@link #suggest(long, Set, Environment)} uses a
     * name match only to guess a default, but the caller is free to pick a differently-named target connection of the
     * same component and version.
     *
     * <p>
     * Visibility is checked FIRST, ahead of the environment and component/version checks, and is deliberately the only
     * check whose rejection message and control flow do not depend on which of "does not exist" / "not visible to this
     * workspace" is true — a target id the caller cannot see must be rejected identically whether it names no
     * connection at all or a real connection in another workspace, otherwise {@code validate} becomes an oracle a
     * caller can use to probe for connections outside their workspace. Only once a target has passed the visibility
     * gate — meaning the caller is entitled to know it exists — do the environment and component/version checks run and
     * report their more specific reasons.
     * </p>
     *
     * @return {@code requested}, unchanged, once every mapping has passed validation
     */
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

        // isVisibleInWorkspace only returns true for a non-null targetConnection, so every check below may
        // dereference it freely, and every message from here on may safely name details of a connection the caller
        // has already been confirmed able to see.

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
