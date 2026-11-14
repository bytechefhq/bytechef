/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRows;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
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
        AutomationProjectFacadeIntTestConfiguration.class,
        ConnectedUserProjectWorkflowManagerIntTest.ConnectedUserServiceConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,connection,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpIntegrationInstanceWorkflowService.class, McpIntegrationInstanceRows.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserProjectWorkflowManagerIntTest {

    @MockitoBean
    private AutomationProjectConfigurationUpdatedEventListener automationProjectConfigurationUpdatedEventListener;

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

    @MockitoBean
    private CopilotWorkflowGenerator copilotWorkflowGenerator;

    @Autowired
    private ConnectionService connectionService;

    @MockitoSpyBean
    private ProjectService projectService;

    @Autowired
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Test
    void testCreateProjectWorkflowDoesNotBindAnotherConnectedUsersConnection() {
        String componentName = createComponentName();

        ConnectedUser otherConnectedUser = connectedUserService.createConnectedUser(
            "other-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        long otherConnectionId = createConnection(componentName, Environment.PRODUCTION);

        connectedUserConnectionService.create(otherConnectedUser.getId(), otherConnectionId, null);

        String externalUserId = "user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        connectedUserProjectWorkflowManager.createProjectWorkflow(
            externalUserId, createDefinition(componentName), Environment.PRODUCTION);

        verify(workflowTestConfigurationFacade, never()).saveWorkflowTestConfigurationConnection(
            anyString(), anyString(), anyString(), anyLong(), anyLong());
    }

    @Test
    void testCreateProjectWorkflowBindsOwnConnectionInTheSameEnvironment() {
        String componentName = createComponentName();
        String externalUserId = "user-" + UUID.randomUUID();

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            externalUserId, Environment.PRODUCTION);

        long developmentConnectionId = createConnection(componentName, Environment.DEVELOPMENT);

        connectedUserConnectionService.create(connectedUser.getId(), developmentConnectionId, null);

        ConnectedUser otherConnectedUser = connectedUserService.createConnectedUser(
            "other-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        long otherConnectionId = createConnection(componentName, Environment.PRODUCTION);

        connectedUserConnectionService.create(otherConnectedUser.getId(), otherConnectionId, null);

        long ownConnectionId = createConnection(componentName, Environment.PRODUCTION);

        connectedUserConnectionService.create(connectedUser.getId(), ownConnectionId, null);

        connectedUserProjectWorkflowManager.createProjectWorkflow(
            externalUserId, createDefinition(componentName), Environment.PRODUCTION);

        verify(workflowTestConfigurationFacade).saveWorkflowTestConfigurationConnection(
            anyString(), eq("trigger_1"), eq(componentName), eq(ownConnectionId),
            eq((long) Environment.PRODUCTION.ordinal()));
        verify(workflowTestConfigurationFacade, never()).saveWorkflowTestConfigurationConnection(
            anyString(), anyString(), anyString(), eq(otherConnectionId), anyLong());
        verify(workflowTestConfigurationFacade, never()).saveWorkflowTestConfigurationConnection(
            anyString(), anyString(), anyString(), eq(developmentConnectionId), anyLong());
    }

    @Test
    void testCreateProjectWorkflowBindsSharedConnection() {
        String componentName = createComponentName();
        String externalUserId = "user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        long sharedConnectionId = createConnection(componentName, Environment.PRODUCTION);

        connectedUserConnectionService.updateShared(sharedConnectionId, true);

        connectedUserProjectWorkflowManager.createProjectWorkflow(
            externalUserId, createDefinition(componentName), Environment.PRODUCTION);

        verify(workflowTestConfigurationFacade).saveWorkflowTestConfigurationConnection(
            anyString(), eq("trigger_1"), eq(componentName), eq(sharedConnectionId),
            eq((long) Environment.PRODUCTION.ordinal()));
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

    private static String createComponentName() {
        return "component" + UUID.randomUUID()
            .toString()
            .replace("-", "");
    }

    private static String createDefinition(String componentName) {
        return """
            {
                "label": "Workflow",
                "description": "",
                "triggers": [{"label": "Trigger", "name": "trigger_1", "parameters": {}, "type": "COMPONENT_NAME/v1/trigger"}],
                "tasks": []
            }
            """
            .replace("COMPONENT_NAME", componentName);
    }

    private long createConnection(String componentName, Environment environment) {
        Connection connection = connectionService.create(
            null, componentName, 1, environment.ordinal(), componentName + " connection", Map.of(),
            PlatformType.EMBEDDED);

        return connection.getId();
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

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}
