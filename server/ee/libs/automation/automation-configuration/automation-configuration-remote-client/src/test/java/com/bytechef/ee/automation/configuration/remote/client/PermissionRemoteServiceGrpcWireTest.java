/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.remote.client.service.RemotePermissionServiceClient;
import com.bytechef.ee.automation.configuration.remote.grpc.PermissionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.InProcessGrpc;
import com.bytechef.ee.remote.grpc.interceptor.UserContextClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.UserContextServerInterceptor;
import io.grpc.StatusRuntimeException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionRemoteServiceGrpcWireTest {

    private final PermissionService permissionService = mock(PermissionService.class);

    private InProcessGrpc inProcessGrpc;
    private RemotePermissionServiceClient remotePermissionServiceClient;

    @BeforeEach
    void beforeEach() throws Exception {
        inProcessGrpc = new InProcessGrpc(
            new PermissionRemoteServiceGrpcServer(permissionService), List.of(new UserContextServerInterceptor()),
            List.of(new UserContextClientInterceptor()));

        remotePermissionServiceClient = new RemotePermissionServiceClient(
            new FixedGrpcChannels(inProcessGrpc.channel()));
    }

    @AfterEach
    void afterEach() {
        inProcessGrpc.close();

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsTenantAdminIsEvaluatedForTheCaller() {
        when(permissionService.isTenantAdmin()).thenAnswer(invocation -> hasServerSideAuthority("ROLE_ADMIN"));

        authenticate("admin@localhost.com", "ROLE_ADMIN", "ROLE_USER");

        assertThat(remotePermissionServiceClient.isTenantAdmin()).isTrue();

        authenticate("user@localhost.com", "ROLE_USER");

        assertThat(remotePermissionServiceClient.isTenantAdmin()).isFalse();
    }

    @Test
    void testHasWorkspaceRole() {
        when(permissionService.hasWorkspaceRole(3L, "EDITOR")).thenReturn(true);

        assertThat(remotePermissionServiceClient.hasWorkspaceRole(3L, "EDITOR")).isTrue();
        assertThat(remotePermissionServiceClient.hasWorkspaceRole(3L, "ADMIN")).isFalse();
    }

    @Test
    void testHasWorkspaceScope() {
        when(permissionService.hasWorkspaceScope(3L, "PROJECT_VIEW")).thenReturn(true);

        assertThat(remotePermissionServiceClient.hasWorkspaceScope(3L, "PROJECT_VIEW")).isTrue();
        assertThat(remotePermissionServiceClient.hasWorkspaceScope(4L, "PROJECT_VIEW")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProject() {
        when(permissionService.hasWorkspaceScopeForProject(9L, "PROJECT_EDIT")).thenReturn(true);

        assertThat(remotePermissionServiceClient.hasWorkspaceScopeForProject(9L, "PROJECT_EDIT")).isTrue();
        assertThat(remotePermissionServiceClient.hasWorkspaceScopeForProject(9L, "PROJECT_DELETE")).isFalse();
    }

    @Test
    void testHasResourceScopeCarriesANumericId() {
        when(permissionService.hasResourceScope(5L, "Connection", "CONNECTION_DELETE")).thenReturn(true);

        assertThat(remotePermissionServiceClient.hasResourceScope(5L, "Connection", "CONNECTION_DELETE")).isTrue();
        assertThat(remotePermissionServiceClient.hasResourceScope(5, "Connection", "CONNECTION_DELETE")).isTrue();
        assertThat(remotePermissionServiceClient.hasResourceScope("5", "Connection", "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testHasResourceScopeCarriesAStringId() {
        when(permissionService.hasResourceScope("workflow-uuid", "Workflow", "WORKFLOW_VIEW")).thenReturn(true);

        assertThat(remotePermissionServiceClient.hasResourceScope("workflow-uuid", "Workflow", "WORKFLOW_VIEW"))
            .isTrue();
    }

    @Test
    void testHasResourceScopeRejectsAnIdTypeTheWireCannotCarry() {
        assertThatThrownBy(() -> remotePermissionServiceClient.hasResourceScope(
            UUID.randomUUID(), "Workflow", "WORKFLOW_VIEW"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasResourceRole() {
        when(permissionService.hasResourceRole(7L, "McpServer", "VIEWER")).thenReturn(true);

        assertThat(remotePermissionServiceClient.hasResourceRole(7L, "McpServer", "VIEWER")).isTrue();
        assertThat(remotePermissionServiceClient.hasResourceRole(7L, "McpServer", "ADMIN")).isFalse();
    }

    @Test
    void testHasWorkflowScope() {
        when(permissionService.hasWorkflowScope("workflow-1", "WORKFLOW_EXECUTE")).thenReturn(true);

        assertThat(remotePermissionServiceClient.hasWorkflowScope("workflow-1", "WORKFLOW_EXECUTE")).isTrue();
        assertThat(remotePermissionServiceClient.hasWorkflowScope("workflow-2", "WORKFLOW_EXECUTE")).isFalse();
    }

    @Test
    void testGetMyWorkspaceScopes() {
        when(permissionService.getMyWorkspaceScopes(3L)).thenReturn(Set.of("PROJECT_VIEW", "PROJECT_EDIT"));

        assertThat(remotePermissionServiceClient.getMyWorkspaceScopes(3L))
            .containsExactlyInAnyOrder("PROJECT_VIEW", "PROJECT_EDIT");
        assertThat(remotePermissionServiceClient.getMyWorkspaceScopes(4L)).isEmpty();
    }

    @Test
    void testGetMyWorkspaceRole() {
        when(permissionService.getMyWorkspaceRole(3L)).thenReturn("EDITOR");

        assertThat(remotePermissionServiceClient.getMyWorkspaceRole(3L)).isEqualTo("EDITOR");
        assertThat(remotePermissionServiceClient.getMyWorkspaceRole(4L)).isNull();
    }

    @Test
    void testIsCurrentUser() {
        when(permissionService.isCurrentUser(11L)).thenReturn(true);

        assertThat(remotePermissionServiceClient.isCurrentUser(11L)).isTrue();
        assertThat(remotePermissionServiceClient.isCurrentUser(12L)).isFalse();
    }

    @Test
    void testIsResourceOwner() {
        when(permissionService.isResourceOwner("McpServer", 7L)).thenReturn(true);

        assertThat(remotePermissionServiceClient.isResourceOwner("McpServer", 7L)).isTrue();
        assertThat(remotePermissionServiceClient.isResourceOwner("McpServer", 8L)).isFalse();
    }

    @Test
    void testSkippedChecksAreGrantedWithoutACall() throws Throwable {
        Boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> remotePermissionServiceClient.hasWorkspaceScope(3L, "PROJECT_VIEW"));

        assertThat(granted).isTrue();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testServerFailurePropagatesInsteadOfDenying() {
        when(permissionService.hasWorkspaceScope(3L, "PROJECT_VIEW")).thenThrow(new IllegalStateException("down"));

        assertThatThrownBy(() -> remotePermissionServiceClient.hasWorkspaceScope(3L, "PROJECT_VIEW"))
            .isInstanceOf(StatusRuntimeException.class);

        verify(permissionService).hasWorkspaceScope(3L, "PROJECT_VIEW");
    }

    @Test
    void testEvictionsStayLocal() {
        remotePermissionServiceClient.evictWorkspaceScopeCache(1L, 2L);
        remotePermissionServiceClient.evictAllWorkspaceScopeCache();

        verifyNoInteractions(permissionService);
    }

    private static void authenticate(String login, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
            .map(SimpleGrantedAuthority::new)
            .toList();

        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(login, null, grantedAuthorities));
    }

    private static boolean hasServerSideAuthority(String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority::equals);
    }
}
