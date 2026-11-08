/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserProjectWorkflowManagerIntTest.ConnectedUserServiceConfiguration.class
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
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, EmbeddedPermissionEvaluator.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserProjectWorkflowManagerIntTest {

    private static final String SLACK_WORKFLOW_DEFINITION = """
        {
            "label": "Auto Bind",
            "tasks": [
                {
                    "name": "slack_1",
                    "type": "slack/v1/sendChannelMessage",
                    "parameters": {}
                }
            ]
        }
        """;

    @MockitoBean
    private AutomationWorkflowProjectConfigurationUpdatedEventListener automationWorkflowProjectConfigurationUpdatedEventListener;

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectUserProjectRepository connectUserProjectRepository;

    @Autowired
    private ConnectionService connectionService;

    @MockitoBean
    private CopilotWorkflowGenerator copilotWorkflowGenerator;

    @MockitoSpyBean
    private ProjectService projectService;

    @Autowired
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Test
    void testCreateProjectWorkflowBindsOnlyAConnectionTheConnectedUserIsEntitledTo() {
        long ownConnectionId = 82001L;
        long foreignConnectionId = 82002L;

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "auto-bind-user-" + UUID.randomUUID(), Environment.PRODUCTION);
        ConnectedUser otherConnectedUser = connectedUserService.createConnectedUser(
            "auto-bind-other-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        connectedUserConnectionService.create(connectedUser.getId(), ownConnectionId);
        connectedUserConnectionService.create(otherConnectedUser.getId(), foreignConnectionId);

        Map<Long, Connection> connections = Map.of(
            ownConnectionId, createSlackConnection(ownConnectionId),
            foreignConnectionId, createSlackConnection(foreignConnectionId));

        when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of(connections.get(foreignConnectionId), connections.get(ownConnectionId)));
        when(connectionService.getConnections(anyList())).thenAnswer(invocation -> {
            List<Long> connectionIds = invocation.getArgument(0);

            return connectionIds.stream()
                .map(connections::get)
                .filter(Objects::nonNull)
                .toList();
        });

        connectedUserProjectWorkflowManager.createProjectWorkflow(
            connectedUser.getExternalId(), SLACK_WORKFLOW_DEFINITION, Environment.PRODUCTION);

        verify(workflowTestConfigurationFacade).saveWorkflowTestConfigurationConnection(
            anyString(), eq("slack_1"), eq("slack"), eq(ownConnectionId), eq((long) Environment.PRODUCTION.ordinal()));
        verify(workflowTestConfigurationFacade, never()).saveWorkflowTestConfigurationConnection(
            anyString(), anyString(), anyString(), eq(foreignConnectionId), anyLong());
    }

    @Test
    void testConcurrentFirstGetOrCreateConnectedUserProjectCreatesOneProject() throws Exception {
        String externalUserId = "project-workflow-manager-user-" + UUID.randomUUID();

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            externalUserId, Environment.PRODUCTION);

        CyclicBarrier projectCreationBarrier = new CyclicBarrier(2);

        doAnswer(invocation -> {
            awaitOtherCaller(projectCreationBarrier);

            return invocation.callRealMethod();
        }).when(projectService)
            .create(any());

        CompletableFuture<ConnectedUserProject> firstCall = CompletableFuture.supplyAsync(
            () -> connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
                externalUserId, Environment.PRODUCTION));
        CompletableFuture<ConnectedUserProject> secondCall = CompletableFuture.supplyAsync(
            () -> connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
                externalUserId, Environment.PRODUCTION));

        ConnectedUserProject firstConnectedUserProject = firstCall.get(30, TimeUnit.SECONDS);
        ConnectedUserProject secondConnectedUserProject = secondCall.get(30, TimeUnit.SECONDS);

        List<ConnectedUserProject> connectedUserProjects = connectUserProjectRepository.findAllByConnectedUserId(
            connectedUser.getId());

        assertThat(connectedUserProjects).hasSize(1);
        assertThat(secondConnectedUserProject.getId()).isEqualTo(firstConnectedUserProject.getId());
    }

    /**
     * Holds each caller at project creation until the other arrives, so both pass the existence check before either
     * creates. When the second caller never arrives because it is blocked on a lock the first holds, the wait times out
     * and the first proceeds alone.
     */
    private static void awaitOtherCaller(CyclicBarrier barrier) throws InterruptedException {
        try {
            barrier.await(2, TimeUnit.SECONDS);
        } catch (BrokenBarrierException | TimeoutException exception) {
            barrier.reset();
        }
    }

    private static Connection createSlackConnection(long id) {
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(id);
        connection.setType(PlatformType.EMBEDDED);

        return connection;
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}
