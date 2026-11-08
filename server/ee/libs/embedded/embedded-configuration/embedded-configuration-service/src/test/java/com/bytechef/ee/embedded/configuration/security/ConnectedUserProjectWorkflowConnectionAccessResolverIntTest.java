/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacadeIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacadeImpl;
import com.bytechef.platform.configuration.security.WorkflowConnectionAccessResolver;
import com.bytechef.platform.configuration.security.WorkflowConnectionAccessResolver.ConnectionAccess;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserProjectWorkflowConnectionAccessResolverIntTest.ConnectedUserServiceConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    AutomationWorkflowProjectConfigurationUpdatedEventListener.class, ClusterElementDefinitionService.class,
    ComponentConnectionFacade.class, ComponentDefinitionService.class, ConnectedUserConnectionFacade.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, CopilotWorkflowGenerator.class, EmbeddedPermissionEvaluator.class,
    EnvironmentService.class, GitHubProxyClient.class, JobFacade.class, JobService.class, McpComponentService.class,
    McpIntegrationComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, TaskDispatcherDefinitionService.class,
    TaskExecutionService.class, TriggerDefinitionFacade.class, TriggerDefinitionService.class,
    TriggerExecutionService.class, TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class, WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserProjectWorkflowConnectionAccessResolverIntTest {

    private static final long FOREIGN_CONNECTION_ID = 83002L;
    private static final long OWN_CONNECTION_ID = 83001L;
    private static final long SHARED_CONNECTION_ID = 83003L;
    private static final String WORKFLOW_DEFINITION = """
        {
            "label": "Connection Access",
            "tasks": [
                {
                    "name": "slack_1",
                    "type": "slack/v1/sendChannelMessage",
                    "parameters": {}
                }
            ]
        }
        """;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private ConnectedUserProjectWorkflowConnectionAccessResolver connectedUserProjectWorkflowConnectionAccessResolver;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ObjectProvider<WorkflowConnectionAccessResolver> workflowConnectionAccessResolvers;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private final Map<Long, Connection> connections = new HashMap<>();

    private String automationWorkflowId;
    private String connectedUserWorkflowId;

    @BeforeEach
    void beforeEach() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "connection-access-user-" + UUID.randomUUID(), Environment.PRODUCTION);
        ConnectedUser otherConnectedUser = connectedUserService.createConnectedUser(
            "connection-access-other-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        registerConnection(OWN_CONNECTION_ID);
        registerConnection(FOREIGN_CONNECTION_ID);
        registerConnection(SHARED_CONNECTION_ID);

        connectedUserConnectionService.create(connectedUser.getId(), OWN_CONNECTION_ID);
        connectedUserConnectionService.create(otherConnectedUser.getId(), FOREIGN_CONNECTION_ID);
        connectedUserConnectionService.updateShared(SHARED_CONNECTION_ID, true);

        Project connectedUserProject = createProject(connectedUser.getExternalId());

        connectedUserProjectService.create(connectedUser.getId(), connectedUserProject.getId());

        connectedUserWorkflowId = addWorkflow(connectedUserProject);
        automationWorkflowId = addWorkflow(createProject("automation-" + UUID.randomUUID()));

        when(connectionService.getConnections(anyList())).thenAnswer(invocation -> {
            List<Long> connectionIds = invocation.getArgument(0);

            return connectionIds.stream()
                .map(connections::get)
                .filter(Objects::nonNull)
                .toList();
        });
        when(connectionService.getConnection(anyLong())).thenAnswer(
            invocation -> connections.get(invocation.<Long>getArgument(0)));
        when(componentConnectionFacade.getComponentConnection(connectedUserWorkflowId, "slack_1", "slack"))
            .thenReturn(new ComponentConnection("slack", 1, "slack_1", "slack", true));
    }

    @Test
    void testResolveConnectionAccessGrantsAConnectionTheConnectedUserOwns() {
        assertThat(
            connectedUserProjectWorkflowConnectionAccessResolver.resolveConnectionAccess(
                connectedUserWorkflowId, OWN_CONNECTION_ID, Environment.PRODUCTION.ordinal()))
                    .isEqualTo(ConnectionAccess.GRANTED);
    }

    @Test
    void testResolveConnectionAccessGrantsASharedConnection() {
        assertThat(
            connectedUserProjectWorkflowConnectionAccessResolver.resolveConnectionAccess(
                connectedUserWorkflowId, SHARED_CONNECTION_ID, Environment.PRODUCTION.ordinal()))
                    .isEqualTo(ConnectionAccess.GRANTED);
    }

    @Test
    void testResolveConnectionAccessDeniesAConnectionOfAnotherConnectedUser() {
        assertThat(
            connectedUserProjectWorkflowConnectionAccessResolver.resolveConnectionAccess(
                connectedUserWorkflowId, FOREIGN_CONNECTION_ID, Environment.PRODUCTION.ordinal()))
                    .isEqualTo(ConnectionAccess.DENIED);
    }

    @Test
    void testResolveConnectionAccessAbstainsForAProjectNoConnectedUserOwns() {
        assertThat(
            connectedUserProjectWorkflowConnectionAccessResolver.resolveConnectionAccess(
                automationWorkflowId, OWN_CONNECTION_ID, Environment.PRODUCTION.ordinal()))
                    .isEqualTo(ConnectionAccess.ABSTAIN);
    }

    @Test
    void testResolveConnectionAccessAbstainsForAWorkflowOutsideAnyProject() {
        assertThat(
            connectedUserProjectWorkflowConnectionAccessResolver.resolveConnectionAccess("unknown", OWN_CONNECTION_ID,
                Environment.PRODUCTION.ordinal()))
                    .isEqualTo(ConnectionAccess.ABSTAIN);
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionBindsAConnectionTheConnectedUserOwns() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade();

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            connectedUserWorkflowId, "slack_1", "slack", OWN_CONNECTION_ID, Environment.PRODUCTION.ordinal());

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationConnection(
            connectedUserWorkflowId, "slack_1", "slack", OWN_CONNECTION_ID, false, Environment.PRODUCTION.ordinal());
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionRejectsAConnectionOfAnotherConnectedUser() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade();

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            connectedUserWorkflowId, "slack_1", "slack", FOREIGN_CONNECTION_ID, Environment.PRODUCTION.ordinal()))
                .isInstanceOf(AccessDeniedException.class);

        verify(workflowTestConfigurationService, never()).saveWorkflowTestConfigurationConnection(
            anyString(), anyString(), anyString(), anyLong(), anyBoolean(), anyLong());
    }

    private String addWorkflow(Project project) {
        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(project.getId(), WORKFLOW_DEFINITION);

        return projectWorkflow.getWorkflowId();
    }

    private WorkflowTestConfigurationFacade createWorkflowTestConfigurationFacade() {
        return new WorkflowTestConfigurationFacadeImpl(
            connectionService, componentConnectionFacade, workflowConnectionAccessResolvers, workflowService,
            workflowTestConfigurationService);
    }

    private Project createProject(String name) {
        Project project = new Project();

        project.setName(name);
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

        return projectService.create(project);
    }

    private void registerConnection(long connectionId) {
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(connectionId);
        connection.setType(PlatformType.EMBEDDED);

        connections.put(connectionId, connection);
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}
