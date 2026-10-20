/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ConfigurationException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserReferenceRolloutManagerIntTest.ConnectedUserServiceConfiguration.class,
        AutomationWorkflowProjectConfigurationResolverIntTest.EmbeddedPermissionEvaluatorTestConfiguration.class
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
    ConnectionService.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class AutomationWorkflowProjectConfigurationResolverIntTest {

    private static final String WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String BETA_EXPRESSION = "metadata['beta'] == 'true'";
    private static final int ROUND_COUNT = 20;
    private static final int PRODUCTION_ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();

    @Autowired
    private AutomationWorkflowProjectConfigurationResolver automationWorkflowProjectConfigurationResolver;

    @Autowired
    private AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private AutomationWorkflowProjectConfigurationUpdatedEventListener automationWorkflowProjectConfigurationUpdatedEventListener;

    @Autowired
    private ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Test
    void testSelfServePicksHighestEnabledVersion() {
        long projectId = createPublishedProject(2);

        createConfiguration(projectId, "Default", null, 1, true);
        createConfiguration(projectId, "Next", null, 2, false);
        createConfiguration(projectId, "Beta", null, 2, true);

        ConnectedUser connectedUser = createConnectedUser(Map.of());

        assertThat(automationWorkflowProjectConfigurationResolver.resolve(connectedUser, projectId))
            .map(AutomationWorkflowProjectConfiguration::getName)
            .contains("Beta");
    }

    @Test
    void testSelfServeTieGoesToHighestId() {
        long projectId = createPublishedProject(1);

        createConfiguration(projectId, "First", null, 1, true);

        AutomationWorkflowProjectConfiguration second = createConfiguration(projectId, "Second", null, 1, true);

        ConnectedUser connectedUser = createConnectedUser(Map.of());

        assertThat(automationWorkflowProjectConfigurationResolver.resolve(connectedUser, projectId))
            .map(AutomationWorkflowProjectConfiguration::getId)
            .contains(second.getId());
    }

    @Test
    void testSelfServeSkipsConfigurationWhoseExpressionFails() {
        long projectId = createPublishedProject(3);

        createConfiguration(projectId, "Default", "", 2, true);
        createConfiguration(projectId, "Beta", BETA_EXPRESSION, 3, true);

        ConnectedUser plainConnectedUser = createConnectedUser(Map.of());
        ConnectedUser betaConnectedUser = createConnectedUser(Map.of("beta", "true"));

        assertThat(automationWorkflowProjectConfigurationResolver.resolve(plainConnectedUser, projectId))
            .map(AutomationWorkflowProjectConfiguration::getName)
            .contains("Default");
        assertThat(automationWorkflowProjectConfigurationResolver.resolve(betaConnectedUser, projectId))
            .map(AutomationWorkflowProjectConfiguration::getName)
            .contains("Beta");
    }

    @Test
    void testResolveReturnsAssignmentEvenWhenAHigherVersionExists() {
        long projectId = createPublishedProject(3);

        createConfiguration(projectId, "Version One", null, 1, true);

        AutomationWorkflowProjectConfiguration versionTwo = createConfiguration(
            projectId, "Version Two", null, 2, true);

        ConnectedUser connectedUser = createConnectedUser(Map.of());

        assertThat(automationWorkflowProjectConfigurationResolver.resolveAndAssign(connectedUser, projectId))
            .map(AutomationWorkflowProjectConfiguration::getId)
            .contains(versionTwo.getId());

        createConfiguration(projectId, "Version Three", null, 3, true);

        assertThat(automationWorkflowProjectConfigurationResolver.resolve(connectedUser, projectId))
            .map(AutomationWorkflowProjectConfiguration::getId)
            .contains(versionTwo.getId());
    }

    @Test
    void testResolveAndAssignWritesNothingWhenNoConfigurationQualifies() {
        long projectId = createPublishedProject(1);

        createConfiguration(projectId, "Disabled", null, 1, false);
        createConfiguration(projectId, "Beta", BETA_EXPRESSION, 1, true);

        ConnectedUser connectedUser = createConnectedUser(Map.of());

        assertThat(automationWorkflowProjectConfigurationResolver.resolveAndAssign(connectedUser, projectId))
            .isEmpty();
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
            .isEmpty();
    }

    @Test
    void testAssignRejectsAConfigurationOfAnotherProject() {
        long projectId = createPublishedProject(1);
        long otherProjectId = createPublishedProject(1);

        AutomationWorkflowProjectConfiguration otherConfiguration = createConfiguration(
            otherProjectId, "Other", null, 1, true);

        ConnectedUser connectedUser = createConnectedUser(Map.of());

        assertThatThrownBy(
            () -> automationWorkflowProjectConfigurationResolver.assign(
                connectedUser, projectId, otherConfiguration.getId()))
                    .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testAssignIgnoresThePermissionExpression() {
        long projectId = createPublishedProject(1);

        AutomationWorkflowProjectConfiguration betaConfiguration = createConfiguration(
            projectId, "Beta", BETA_EXPRESSION, 1, true);

        ConnectedUser connectedUser = createConnectedUser(Map.of());

        automationWorkflowProjectConfigurationResolver.assign(connectedUser, projectId, betaConfiguration.getId());

        assertThat(automationWorkflowProjectConfigurationResolver.resolve(connectedUser, projectId))
            .map(AutomationWorkflowProjectConfiguration::getId)
            .contains(betaConfiguration.getId());
    }

    @Test
    void testConcurrentResolveAndAssignCreatesOneRow() throws Exception {
        long projectId = createPublishedProject(1);

        AutomationWorkflowProjectConfiguration configuration = createConfiguration(
            projectId, "Default", null, 1, true);

        for (int round = 0; round < ROUND_COUNT; round++) {
            ConnectedUser connectedUser = createConnectedUser(Map.of());

            CountDownLatch startLatch = new CountDownLatch(1);
            ExecutorService executorService = Executors.newFixedThreadPool(2);

            try {
                List<Future<Optional<AutomationWorkflowProjectConfiguration>>> futures = new ArrayList<>();

                for (int index = 0; index < 2; index++) {
                    futures.add(executorService.submit(() -> {
                        awaitLatch(startLatch);

                        return automationWorkflowProjectConfigurationResolver.resolveAndAssign(
                            connectedUser, projectId);
                    }));
                }

                startLatch.countDown();

                for (Future<Optional<AutomationWorkflowProjectConfiguration>> future : futures) {
                    Optional<AutomationWorkflowProjectConfiguration> resolvedOptional =
                        future.get(60, TimeUnit.SECONDS);

                    assertThat(resolvedOptional)
                        .map(AutomationWorkflowProjectConfiguration::getId)
                        .contains(configuration.getId());
                }
            } finally {
                startLatch.countDown();

                executorService.shutdownNow();
            }
        }

        assertThat(connectedUserProjectConfigurationService.getAssignments(configuration.getId()))
            .hasSize(ROUND_COUNT);
    }

    private static void awaitLatch(CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the start signal");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private AutomationWorkflowProjectConfiguration createConfiguration(
        long projectId, String name, String permissionExpression, int projectVersion, boolean enabled) {

        return automationWorkflowProjectConfigurationService.create(
            projectId, PRODUCTION_ENVIRONMENT_ID, name, permissionExpression, projectVersion, enabled, true);
    }

    private ConnectedUser createConnectedUser(Map<String, String> metadata) {
        String externalUserId = "resolver-user-" + UUID.randomUUID();

        return connectedUserService.createConnectedUser(
            externalUserId, Environment.PRODUCTION, null, null, metadata);
    }

    private long createPublishedProject(int versionCount) {
        long projectId = automationWorkflowProjectFacade.createProject(
            "Resolver " + UUID.randomUUID(), "", null, List.of(), null);

        automationWorkflowProjectFacade.createProjectWorkflow(projectId, WORKFLOW_DEFINITION, null);

        for (int version = 0; version < versionCount; version++) {
            automationWorkflowProjectFacade.publishProject(projectId, null);
        }

        return projectId;
    }

    @Configuration
    static class EmbeddedPermissionEvaluatorTestConfiguration {

        @Bean
        EmbeddedPermissionEvaluator embeddedPermissionEvaluator() {
            return new EmbeddedPermissionEvaluator(SpelEvaluator.create());
        }
    }
}
