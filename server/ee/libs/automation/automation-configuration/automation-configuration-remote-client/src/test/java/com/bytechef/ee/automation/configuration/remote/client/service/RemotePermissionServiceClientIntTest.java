/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationIntTestData;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.automation.configuration.remote.grpc.PermissionRemoteServiceGrpcServer;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.InProcessGrpc;
import com.bytechef.ee.remote.grpc.interceptor.UserContextClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.UserContextServerInterceptor;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.user.domain.User;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationConfigurationRemoteClientIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true",
        "spring.liquibase.contexts=configuration,connection,execution,user"
    })
class RemotePermissionServiceClientIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final String GHOST_LOGIN = "ghost";
    private static final String USER_LOGIN = "user";

    @Autowired
    private AutomationConfigurationIntTestData automationConfigurationIntTestData;

    @Autowired
    private PermissionScopeRegistry permissionScopeRegistry;

    @Autowired
    private PermissionService permissionService;

    private User adminUser;
    private InProcessGrpc inProcessGrpc;
    private McpServer mcpServer;
    private Project project;
    private ProjectWorkflow projectWorkflow;
    private RemotePermissionServiceClient remotePermissionServiceClient;
    private User user;
    private Workspace workspace;

    @BeforeEach
    void beforeEach() throws IOException {
        inProcessGrpc = new InProcessGrpc(
            new PermissionRemoteServiceGrpcServer(permissionService), List.of(new UserContextServerInterceptor()),
            List.of(new UserContextClientInterceptor()));

        remotePermissionServiceClient = new RemotePermissionServiceClient(
            new FixedGrpcChannels(inProcessGrpc.channel()));

        workspace = automationConfigurationIntTestData.createWorkspace("workspace");

        adminUser = automationConfigurationIntTestData.createUser(ADMIN_LOGIN);
        user = automationConfigurationIntTestData.createUser(USER_LOGIN);

        project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());

        projectWorkflow = automationConfigurationIntTestData.createProjectWorkflow(
            project.getId(), 1, "Live Workflow", UUID.randomUUID());

        mcpServer = automationConfigurationIntTestData.createMcpServer("mcp server", workspace.getId());
    }

    @AfterEach
    void afterEach() {
        inProcessGrpc.close();

        SecurityContextHolder.clearContext();

        automationConfigurationIntTestData.deleteAll();
    }

    @Test
    void testIsTenantAdminIsEvaluatedForTheCaller() {
        authenticate(ADMIN_LOGIN, "ROLE_ADMIN", "ROLE_USER");

        assertThat(remotePermissionServiceClient.isTenantAdmin()).isTrue();

        authenticate(USER_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.isTenantAdmin()).isFalse();

        SecurityContextHolder.clearContext();

        assertThat(remotePermissionServiceClient.isTenantAdmin()).isFalse();
    }

    @Test
    void testIsCurrentUserResolvesTheCallerFromTheDatabase() {
        authenticate(USER_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.isCurrentUser(user.getId())).isTrue();
        assertThat(remotePermissionServiceClient.isCurrentUser(adminUser.getId())).isFalse();

        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.isCurrentUser(user.getId())).isFalse();

        SecurityContextHolder.clearContext();

        assertThat(remotePermissionServiceClient.isCurrentUser(user.getId())).isFalse();
    }

    @Test
    void testHasWorkspaceRoleGrantsTenantAdminsAndFailsClosedForUnknownCallersAndRoles() {
        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThat(remotePermissionServiceClient.hasWorkspaceRole(workspace.getId(), "ADMIN")).isTrue();

        authenticate(USER_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasWorkspaceRole(workspace.getId(), "OWNER")).isFalse();

        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasWorkspaceRole(workspace.getId(), "VIEWER")).isFalse();

        SecurityContextHolder.clearContext();

        assertThat(remotePermissionServiceClient.hasWorkspaceRole(workspace.getId(), "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeGrantsTenantAdminsAndFailsClosedForUnknownCallers() {
        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThat(remotePermissionServiceClient.hasWorkspaceScope(workspace.getId(), "PROJECT_DELETE")).isTrue();

        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasWorkspaceScope(workspace.getId(), "WORKFLOW_VIEW")).isFalse();

        SecurityContextHolder.clearContext();

        assertThat(remotePermissionServiceClient.hasWorkspaceScope(workspace.getId(), "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectResolvesTheProjectWorkspace() {
        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThat(remotePermissionServiceClient.hasWorkspaceScopeForProject(project.getId(), "PROJECT_DELETE"))
            .isTrue();

        authenticate(USER_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasWorkspaceScopeForProject(Long.MAX_VALUE, "PROJECT_CREATE"))
            .isFalse();

        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasWorkspaceScopeForProject(project.getId(), "PROJECT_CREATE"))
            .isFalse();
    }

    @Test
    void testHasResourceScopeResolvesTheResourceWorkspace() {
        long mcpServerId = mcpServer.getId();

        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThat(remotePermissionServiceClient.hasResourceScope(mcpServerId, "McpServer", "MCP_EDIT")).isTrue();

        authenticate(USER_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasResourceScope(Long.MAX_VALUE, "McpServer", "MCP_VIEW"))
            .isFalse();
        assertThat(remotePermissionServiceClient.hasResourceScope((int) Long.MAX_VALUE, "McpServer", "MCP_VIEW"))
            .isFalse();
        assertThat(remotePermissionServiceClient.hasResourceScope(mcpServerId, "Connection", "MCP_VIEW")).isFalse();
        assertThat(
            remotePermissionServiceClient.hasResourceScope(String.valueOf(mcpServerId), "McpServer", "MCP_VIEW"))
                .isFalse();

        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasResourceScope(mcpServerId, "McpServer", "MCP_VIEW")).isFalse();
        assertThat(remotePermissionServiceClient.hasResourceScope((int) mcpServerId, "McpServer", "MCP_VIEW"))
            .isFalse();
    }

    @Test
    void testHasResourceScopeRejectsAnIdTypeTheWireCannotCarry() {
        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThatThrownBy(
            () -> remotePermissionServiceClient.hasResourceScope(UUID.randomUUID(), "McpServer", "MCP_VIEW"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testHasResourceRoleResolvesTheResourceWorkspace() {
        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThat(remotePermissionServiceClient.hasResourceRole(mcpServer.getId(), "McpServer", "ADMIN")).isTrue();

        authenticate(USER_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasResourceRole(Long.MAX_VALUE, "McpServer", "VIEWER")).isFalse();
        assertThat(remotePermissionServiceClient.hasResourceRole(mcpServer.getId(), "Connection", "VIEWER"))
            .isFalse();

        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasResourceRole(mcpServer.getId(), "McpServer", "VIEWER"))
            .isFalse();
    }

    @Test
    void testIsResourceOwnerGrantsOnlyTenantAdminsForWorkspaceMappedResources() {
        authenticate(USER_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.isResourceOwner("McpServer", mcpServer.getId())).isFalse();
        assertThat(remotePermissionServiceClient.isResourceOwner("Connection", mcpServer.getId())).isFalse();

        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThat(remotePermissionServiceClient.isResourceOwner("McpServer", mcpServer.getId())).isTrue();
    }

    @Test
    void testHasWorkflowScopeResolvesTheWorkflowProjectWorkspace() {
        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThat(remotePermissionServiceClient.hasWorkflowScope(projectWorkflow.getWorkflowId(), "WORKFLOW_EDIT"))
            .isTrue();

        authenticate(USER_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasWorkflowScope("unknown-workflow", "WORKFLOW_VIEW")).isFalse();

        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasWorkflowScope(projectWorkflow.getWorkflowId(), "WORKFLOW_VIEW"))
            .isFalse();
    }

    @Test
    void testGetMyWorkspaceScopesReturnsEveryScopeToTenantAdmins() {
        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThat(remotePermissionServiceClient.getMyWorkspaceScopes(workspace.getId()))
            .containsExactlyInAnyOrderElementsOf(permissionScopeRegistry.getAllScopeNames())
            .contains("MCP_VIEW", "PROJECT_DELETE", "WORKFLOW_VIEW");

        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.getMyWorkspaceScopes(workspace.getId())).isEmpty();
    }

    @Test
    void testGetMyWorkspaceRoleReturnsAdminToTenantAdmins() {
        authenticate(ADMIN_LOGIN, "ROLE_ADMIN");

        assertThat(remotePermissionServiceClient.getMyWorkspaceRole(workspace.getId())).isEqualTo("ADMIN");

        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.getMyWorkspaceRole(workspace.getId())).isNull();
    }

    @Test
    void testSkippedChecksAreGrantedWithoutACall() throws Throwable {
        authenticate(GHOST_LOGIN, "ROLE_USER");

        assertThat(remotePermissionServiceClient.hasWorkspaceScope(workspace.getId(), "WORKFLOW_VIEW")).isFalse();

        Boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> remotePermissionServiceClient.hasWorkspaceScope(workspace.getId(), "WORKFLOW_VIEW"));

        assertThat(granted).isTrue();
    }

    private static void authenticate(String login, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
            .map(SimpleGrantedAuthority::new)
            .toList();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(login, null, grantedAuthorities));
    }
}
