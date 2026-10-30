/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.bytechef.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
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
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
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
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
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

    @MockitoBean
    private AutomationWorkflowProjectConfigurationUpdatedEventListener automationWorkflowProjectConfigurationUpdatedEventListener;

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectUserProjectRepository connectUserProjectRepository;

    @MockitoBean
    private CopilotWorkflowGenerator copilotWorkflowGenerator;

    @MockitoSpyBean
    private ProjectService projectService;

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

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}
