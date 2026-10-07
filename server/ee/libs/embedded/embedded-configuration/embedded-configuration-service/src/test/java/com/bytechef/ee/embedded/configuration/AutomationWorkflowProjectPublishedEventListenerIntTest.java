/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacadeIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceRolloutManager;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationWorkflowProjectFacadeIntTestConfiguration.class,
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import({
    AutomationWorkflowProjectPublishedEventListenerIntTest.AsyncTestConfiguration.class,
    PostgreSQLContainerConfiguration.class
})
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, TriggerDefinitionFacade.class,
    ComponentConnectionFacade.class,
    ComponentDefinitionService.class, ConnectedUserService.class, ConnectionDefinitionService.class,
    ConnectionFacade.class, ConnectionLifecycleFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class,
    GitHubProxyClient.class, JobFacade.class, JobService.class, McpComponentService.class,
    McpIntegrationInstanceConfigurationService.class, McpIntegrationInstanceConfigurationWorkflowService.class,
    McpIntegrationInstanceToolService.class, McpServerService.class, McpToolService.class,
    OAuth2ParametersFacade.class,
    OAuth2Service.class, PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class, TriggerDefinitionService.class,
    TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class,
    WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class AutomationWorkflowProjectPublishedEventListenerIntTest {

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Test
    void testPublishProjectHandsTheRolloutToTheListenerAfterCommit() {
        long automationWorkflowProjectId = automationWorkflowProjectFacade.createProject(
            "Listener " + UUID.randomUUID(), "", null, List.of(), null, null);

        automationWorkflowProjectFacade.createProjectWorkflow(
            automationWorkflowProjectId, "{\"label\":\"x\",\"triggers\":[],\"tasks\":[]}", null);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        verify(connectedUserReferenceRolloutManager, timeout(5000)).rollOut(automationWorkflowProjectId);
    }

    @Test
    void testRolloutWaitsForTheCommitAndRunsOffThePublishingThread() {
        long automationWorkflowProjectId = automationWorkflowProjectFacade.createProject(
            "Listener " + UUID.randomUUID(), "", null, List.of(), null, null);

        automationWorkflowProjectFacade.createProjectWorkflow(
            automationWorkflowProjectId, "{\"label\":\"x\",\"triggers\":[],\"tasks\":[]}", null);

        AtomicReference<Thread> rolloutThread = new AtomicReference<>();

        doAnswer(invocation -> {
            rolloutThread.set(Thread.currentThread());

            return null;
        }).when(connectedUserReferenceRolloutManager)
            .rollOut(automationWorkflowProjectId);

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

            verify(connectedUserReferenceRolloutManager, after(500).never()).rollOut(automationWorkflowProjectId);
        });

        verify(connectedUserReferenceRolloutManager, timeout(5000)).rollOut(automationWorkflowProjectId);

        assertThat(rolloutThread.get()).isNotSameAs(Thread.currentThread());
    }

    @EnableAsync
    @TestConfiguration
    static class AsyncTestConfiguration {

        @Bean
        TaskExecutor workerExecutor() {
            return new SimpleAsyncTaskExecutor("rollout-listener-");
        }
    }
}
