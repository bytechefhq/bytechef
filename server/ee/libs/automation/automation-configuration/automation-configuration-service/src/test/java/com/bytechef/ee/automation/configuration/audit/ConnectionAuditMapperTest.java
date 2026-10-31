/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionAuditMapperTest {

    private ConnectionAuditMapper connectionAuditMapper;
    private ConnectionService connectionService;
    private UserService userService;
    private WorkspaceConnectionService workspaceConnectionService;

    @BeforeEach
    void beforeEach() {
        connectionService = mock(ConnectionService.class);
        userService = mock(UserService.class);
        workspaceConnectionService = mock(WorkspaceConnectionService.class);

        connectionAuditMapper = new ConnectionAuditMapper(connectionService, userService, workspaceConnectionService);
    }

    @Test
    void testCreatedRecordsThePersistedVisibility() {
        when(connectionService.fetchConnection(5L)).thenReturn(Optional.of(connection(5L, ResourceVisibility.PRIVATE)));

        AuditInvocation auditInvocation = new AuditInvocation(
            ConnectionAuditEvents.CONNECTION_CREATED, Map.of("workspaceId", 1L), 5L, null, AuditOutcome.SUCCESS,
            null);

        assertThat(connectionAuditMapper.map(auditInvocation))
            .containsExactlyInAnyOrderEntriesOf(
                Map.of("connectionId", "5", "workspaceId", "1", "visibility", "PRIVATE"));
    }

    @Test
    void testVisibilityChangedRecordsBothRungs() {
        when(connectionService.fetchConnection(5L))
            .thenReturn(Optional.of(connection(5L, ResourceVisibility.WORKSPACE)));

        Map<String, Object> arguments = Map.of(
            "workspaceId", 1L, "connectionId", 5L, "visibility", ResourceVisibility.PRIVATE);

        Object captured = connectionAuditMapper.capture(
            invocation(ConnectionAuditEvents.CONNECTION_VISIBILITY_CHANGED, arguments, null));

        assertThat(connectionAuditMapper.map(
            invocation(ConnectionAuditEvents.CONNECTION_VISIBILITY_CHANGED, arguments, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                        "connectionId", "5", "workspaceId", "1", "fromVisibility", "WORKSPACE",
                        "toVisibility", "PRIVATE"));
    }

    @Test
    void testAccessGrantedRecordsTheTargetUser() {
        Map<String, Object> arguments = Map.of("workspaceId", 1L, "connectionId", 5L, "userId", 9L);

        assertThat(connectionAuditMapper.map(
            invocation(ConnectionAuditEvents.CONNECTION_ACCESS_GRANTED, arguments, null)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of("connectionId", "5", "workspaceId", "1", "targetUserId", "9"));
    }

    @Test
    void testCredentialsUpdatedRecordsNoCredentialMaterial() {
        Map<String, Object> arguments = Map.of(
            "connectionId", 5L, "parameters", Map.of("token", "secret"), "version", 2);

        assertThat(connectionAuditMapper.map(
            invocation(ConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED, arguments, null)))
                .containsExactlyEntriesOf(Map.of("connectionId", "5"));
    }

    @Test
    void testReassignedRecordsUserIdsRatherThanLogins() {
        when(userService.fetchUserByLogin("new@localhost.com")).thenReturn(Optional.of(user(8L)));

        Map<String, Object> arguments = Map.of(
            "workspaceId", 1L, "connectionId", 5L, "newOwnerLogin", "new@localhost.com");

        assertThat(connectionAuditMapper.map(invocation(ConnectionAuditEvents.CONNECTION_REASSIGNED, arguments, null)))
            .containsExactlyInAnyOrderEntriesOf(
                Map.of("connectionId", "5", "workspaceId", "1", "newOwnerUserId", "8"));
    }

    @Test
    void testBulkReassignedRecordsTheOwnedConnections() {
        Connection owned = connection(5L, ResourceVisibility.WORKSPACE);

        owned.setCreatedBy("old@localhost.com");

        Connection other = connection(6L, ResourceVisibility.WORKSPACE);

        other.setCreatedBy("someone@localhost.com");

        when(workspaceConnectionService.getWorkspaceConnections(1L))
            .thenReturn(List.of(new WorkspaceConnection(5L, 1L), new WorkspaceConnection(6L, 1L)));
        when(connectionService.getConnections(List.of(5L, 6L))).thenReturn(List.of(owned, other));
        when(userService.fetchUserByLogin("new@localhost.com")).thenReturn(Optional.of(user(8L)));
        when(userService.fetchUserByLogin("old@localhost.com")).thenReturn(Optional.of(user(3L)));

        Map<String, Object> arguments = Map.of(
            "workspaceId", 1L, "userLogin", "old@localhost.com", "newOwnerLogin", "new@localhost.com");

        Object captured = connectionAuditMapper.capture(
            invocation(ConnectionAuditEvents.CONNECTION_REASSIGNED, arguments, null));

        assertThat(connectionAuditMapper.map(
            invocation(ConnectionAuditEvents.CONNECTION_REASSIGNED, arguments, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                        "workspaceId", "1", "newOwnerUserId", "8", "previousOwnerUserId", "3",
                        "connectionIds", "5", "connectionCount", "1"));
    }

    private static Connection connection(long id, ResourceVisibility visibility) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setVisibility(visibility);

        return connection;
    }

    private static AuditInvocation invocation(String event, Map<String, Object> arguments, Object captured) {
        return new AuditInvocation(event, arguments, null, captured, AuditOutcome.SUCCESS, null);
    }

    private static User user(long id) {
        User user = new User();

        user.setId(id);

        return user;
    }
}
