/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class ConnectionAuditMapper implements AuditMapper {

    private final ConnectionService connectionService;
    private final UserService userService;
    private final WorkspaceConnectionService workspaceConnectionService;

    @SuppressFBWarnings("EI")
    public ConnectionAuditMapper(
        ConnectionService connectionService, UserService userService,
        WorkspaceConnectionService workspaceConnectionService) {

        this.connectionService = connectionService;
        this.userService = userService;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    @Override
    public Set<String> events() {
        return Set.of(
            ConnectionAuditEvents.CONNECTION_ACCESS_GRANTED, ConnectionAuditEvents.CONNECTION_ACCESS_REVOKED,
            ConnectionAuditEvents.CONNECTION_CREATED, ConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED,
            ConnectionAuditEvents.CONNECTION_DELETED, ConnectionAuditEvents.CONNECTION_REASSIGNED,
            ConnectionAuditEvents.CONNECTION_VISIBILITY_CHANGED);
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        return switch (auditInvocation.event()) {
            case ConnectionAuditEvents.CONNECTION_VISIBILITY_CHANGED -> connectionService
                .fetchConnection((Long) auditInvocation.requireArgument("connectionId"))
                .map(Connection::getVisibility)
                .orElse(null);
            case ConnectionAuditEvents.CONNECTION_REASSIGNED -> auditInvocation.arguments()
                .containsKey("connectionId") ? null : getOwnedConnectionIds(auditInvocation);
            default -> null;
        };
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        Object connectionId = resolveConnectionId(auditInvocation);

        if (connectionId != null) {
            data.put("connectionId", String.valueOf(connectionId));
        }

        Object workspaceId = auditInvocation.argument("workspaceId");

        if (workspaceId != null) {
            data.put("workspaceId", String.valueOf(workspaceId));
        }

        switch (auditInvocation.event()) {
            case ConnectionAuditEvents.CONNECTION_CREATED -> putCreatedVisibility(data, auditInvocation, connectionId);
            case ConnectionAuditEvents.CONNECTION_VISIBILITY_CHANGED -> {
                if (auditInvocation.captured() instanceof ResourceVisibility fromVisibility) {
                    data.put("fromVisibility", fromVisibility.name());
                }

                data.put(
                    "toVisibility", ((ResourceVisibility) auditInvocation.requireArgument("visibility")).name());
            }
            case ConnectionAuditEvents.CONNECTION_ACCESS_GRANTED, ConnectionAuditEvents.CONNECTION_ACCESS_REVOKED ->
                data.put("targetUserId", String.valueOf(auditInvocation.requireArgument("userId")));
            case ConnectionAuditEvents.CONNECTION_REASSIGNED -> putReassignment(data, auditInvocation);
            default -> {
            }
        }

        return data;
    }

    private List<Long> getOwnedConnectionIds(AuditInvocation auditInvocation) {
        String userLogin = (String) auditInvocation.requireArgument("userLogin");

        List<Long> workspaceConnectionIds = workspaceConnectionService
            .getWorkspaceConnections((Long) auditInvocation.requireArgument("workspaceId"))
            .stream()
            .map(WorkspaceConnection::getConnectionId)
            .toList();

        if (workspaceConnectionIds.isEmpty()) {
            return List.of();
        }

        return connectionService.getConnections(workspaceConnectionIds)
            .stream()
            .filter(connection -> userLogin.equals(connection.getCreatedBy()))
            .map(Connection::getId)
            .toList();
    }

    private void putCreatedVisibility(
        Map<String, String> data, AuditInvocation auditInvocation, @Nullable Object connectionId) {

        if (connectionId instanceof Long id) {
            connectionService.fetchConnection(id)
                .map(Connection::getVisibility)
                .ifPresent(visibility -> data.put("visibility", visibility.name()));

            return;
        }

        if (auditInvocation.argument("connectionDTO") instanceof ConnectionDTO connectionDTO &&
            connectionDTO.visibility() != null) {

            data.put("visibility", connectionDTO.visibility()
                .name());
        }
    }

    private void putReassignment(Map<String, String> data, AuditInvocation auditInvocation) {
        putUserId(data, "newOwnerUserId", (String) auditInvocation.requireArgument("newOwnerLogin"));

        Object userLogin = auditInvocation.argument("userLogin");

        if (userLogin != null) {
            putUserId(data, "previousOwnerUserId", (String) userLogin);
        }

        if (auditInvocation.captured() instanceof List<?> connectionIds) {
            data.put("connectionIds", AuditValues.joinBounded(connectionIds));
            data.put("connectionCount", String.valueOf(connectionIds.size()));
        }
    }

    private void putUserId(Map<String, String> data, String key, String login) {
        userService.fetchUserByLogin(login)
            .map(User::getId)
            .ifPresent(userId -> data.put(key, String.valueOf(userId)));
    }

    private static @Nullable Object resolveConnectionId(AuditInvocation auditInvocation) {
        Object connectionId = auditInvocation.argument("connectionId");

        if (connectionId != null) {
            return connectionId;
        }

        if (auditInvocation.result() instanceof Long id) {
            return id;
        }

        return null;
    }
}
