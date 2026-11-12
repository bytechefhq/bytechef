/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.exception.ProjectDeploymentErrorType;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionErrorCode;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionEnableException;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionErrorType;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.ee.embedded.configuration.trigger.subscription.TriggerSubscriptionEventSender;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.exception.ConnectionDefinitionErrorType;
import com.bytechef.platform.component.exception.TriggerDefinitionErrorType;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.event.ConnectionCredentialsInvalidatedEvent;
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
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationProjectFacadeIntTestConfiguration.class,
        TriggerSubscriptionManagerIntTest.ConnectedUserServiceConfiguration.class
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
class TriggerSubscriptionManagerIntTest {

    private static final long CONNECTION_ID = 777L;
    private static final String INTERNAL_ERROR_MESSAGE = "Internal error while enabling the trigger subscription";
    private static final String WEBHOOK_COMPONENT_NAME = "webhook";
    private static final String WEBHOOK_TRIGGER_NAME = "autoRespondWithHTTP200";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration SHORT_REGISTRATION_LEASE = Duration.ofMillis(500);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @MockitoBean
    private AutomationProjectConfigurationUpdatedEventListener automationProjectConfigurationUpdatedEventListener;

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @MockitoBean
    private CopilotWorkflowGenerator copilotWorkflowGenerator;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @MockitoBean
    private TriggerSubscriptionEventSender triggerSubscriptionEventSender;

    @Autowired
    private TriggerSubscriptionFacade triggerSubscriptionFacade;

    @Autowired
    private TriggerSubscriptionManager triggerSubscriptionManager;

    @MockitoSpyBean
    private TriggerSubscriptionService triggerSubscriptionService;

    private ConnectedUser connectedUser;

    @BeforeEach
    void setUp() {
        connectedUser = connectedUserService.createConnectedUser(
            "trigger-subscription-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getName()).thenReturn(WEBHOOK_TRIGGER_NAME);
        when(triggerDefinition.getType()).thenReturn(TriggerType.STATIC_WEBHOOK);

        when(triggerDefinitionService.getTriggerDefinition(WEBHOOK_COMPONENT_NAME, 1, WEBHOOK_TRIGGER_NAME))
            .thenReturn(triggerDefinition);
        when(environmentService.getEnvironment(Environment.PRODUCTION.ordinal()))
            .thenReturn(Environment.PRODUCTION);
        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                .thenReturn(Page.empty());
        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(true);
    }

    @Test
    void testCreateEnablesTriggerOnHiddenProject() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "ok"));

        assertThat(triggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(triggerSubscription.getErrorCode()).isNull();
        assertThat(triggerSubscription.getProjectId()).isNotNull();
        assertThat(triggerSubscription.getProjectDeploymentId()).isNotNull();
        assertThat(triggerSubscription.getWorkflowUuid()).isNotNull();

        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        long projectId = Validate.notNull(triggerSubscription.getProjectId(), "projectId");

        Project project = projectService.getProject(projectId);

        assertThat(project.getName()).isEqualTo(
            connectedUser.getExternalId() + " - " + WEBHOOK_COMPONENT_NAME + "/" + WEBHOOK_TRIGGER_NAME);
        assertThat(projectService.getWorkspaceProjectIds(Workspace.DEFAULT_WORKSPACE_ID)).doesNotContain(projectId);
        assertThat(projectService.getProjects(null, null, null, null, null, null))
            .extracting(Project::getId)
            .doesNotContain(projectId);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId"));

        assertThat(projectDeployment.isEnabled()).isTrue();
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeployment.getId()))
            .singleElement()
            .satisfies(projectDeploymentWorkflow -> {
                assertThat(projectDeploymentWorkflow.isEnabled()).isTrue();
                assertThat(projectDeploymentWorkflow.getConnections()).isEmpty();
            });

        assertThat(triggerSubscriptionManager.getStaticWebhookUrl(triggerSubscription)).contains("/webhooks/");
    }

    @Test
    void testCreateSetsTheConnectionExplicitlyOnTheTriggerNode() {
        TriggerSubscription triggerSubscription = triggerSubscriptionManager.create(
            connectedUser, null, CONNECTION_ID, WEBHOOK_COMPONENT_NAME, 1, WEBHOOK_TRIGGER_NAME, Map.of(), null, null,
            null, 0);

        assertThat(triggerSubscription.getConnectionId()).isEqualTo(CONNECTION_ID);
        assertThat(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId")))
                    .singleElement()
                    .extracting(ProjectDeploymentWorkflow::getConnections)
                    .satisfies(connections -> assertThat(connections)
                        .containsExactly(
                            new ProjectDeploymentWorkflowConnection(
                                CONNECTION_ID, WEBHOOK_COMPONENT_NAME,
                                TriggerSubscriptionWorkflowDefinitionFactory.TRIGGER_NAME)));
    }

    @Test
    void testCreateRejectsAnUnknownTrigger() {
        when(triggerDefinitionService.getTriggerDefinition(WEBHOOK_COMPONENT_NAME, 1, "missing"))
            .thenThrow(new IllegalArgumentException("The component 'webhook' does not contain the 'missing' trigger."));

        assertThatThrownBy(() -> triggerSubscriptionManager.create(
            connectedUser, null, null, WEBHOOK_COMPONENT_NAME, 1, "missing", Map.of(), null, null, null, 0))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> {
                        assertThat(exception.getEntityClass()).isEqualTo(TriggerSubscription.class);
                        assertThat(exception.getErrorKey())
                            .isEqualTo(TriggerSubscriptionErrorType.NO_TRIGGER.getErrorKey());
                    });

        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId())).isEmpty();
    }

    @Test
    void testCreateOfAnUnknownComponentIsRejectedAsNoTrigger() {
        when(componentDefinitionService.getComponentDefinition("unknownComponent", 1))
            .thenThrow(new IllegalArgumentException("Component definition unknownComponent/1 does not exist"));
        when(triggerDefinitionService.getTriggerDefinition("unknownComponent", 1, "newEvent"))
            .thenThrow(new IllegalArgumentException("Component definition unknownComponent/1 does not exist"));

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            connectedUser.getExternalId(), "unknownComponent", 1, "newEvent", Map.of(), null, null, null, null,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> {
                        assertThat(exception.getEntityClass()).isEqualTo(TriggerSubscription.class);
                        assertThat(exception.getErrorKey())
                            .isEqualTo(TriggerSubscriptionErrorType.NO_TRIGGER.getErrorKey());
                    });

        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId())).isEmpty();
    }

    @Test
    void testGetStaticWebhookUrlOfANonStaticWebhookTriggerDoesNotLoadTheDeployment() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "ok"));

        TriggerDefinition dynamicWebhookTriggerDefinition = mock(TriggerDefinition.class);

        when(dynamicWebhookTriggerDefinition.getType()).thenReturn(TriggerType.DYNAMIC_WEBHOOK);
        when(triggerDefinitionService.getTriggerDefinition(WEBHOOK_COMPONENT_NAME, 1, WEBHOOK_TRIGGER_NAME))
            .thenReturn(dynamicWebhookTriggerDefinition);

        clearInvocations(principalJobService);

        assertThat(triggerSubscriptionManager.getStaticWebhookUrl(triggerSubscription)).isNull();

        verify(principalJobService, never()).fetchLastWorkflowJobId(anyLong(), anyList(), any());
    }

    @Test
    void testUpdateKeepsStaticWebhookUrl() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "first"));

        String staticWebhookUrl = triggerSubscriptionManager.getStaticWebhookUrl(triggerSubscription);

        clearInvocations(triggerLifecycleFacade);

        TriggerSubscription updatedTriggerSubscription = triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "second"), "Renamed", "external-1",
            "https://example.com/hook");

        assertThat(updatedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(updatedTriggerSubscription.getParameters()).isEqualTo(Map.of("respondWith", "second"));
        assertThat(updatedTriggerSubscription.getName()).isEqualTo("Renamed");
        assertThat(updatedTriggerSubscription.getExternalId()).isEqualTo("external-1");
        assertThat(updatedTriggerSubscription.getWebhookUrl()).isEqualTo("https://example.com/hook");
        assertThat(updatedTriggerSubscription.getProjectDeploymentId())
            .isEqualTo(triggerSubscription.getProjectDeploymentId());
        assertThat(triggerSubscriptionManager.getStaticWebhookUrl(updatedTriggerSubscription))
            .isNotNull()
            .isEqualTo(staticWebhookUrl);

        InOrder inOrder = inOrder(triggerLifecycleFacade);

        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                anyString(), any(), any(), eq(Map.of("respondWith", "second")), any(), anyString(), anyLong());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void testCreateDoesNotTouchOtherSubscriptions() {
        TriggerSubscription firstTriggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));
        TriggerSubscription secondTriggerSubscription = createWebhookSubscription(Map.of("respondWith", "b"));

        assertThat(secondTriggerSubscription.getProjectId()).isNotEqualTo(firstTriggerSubscription.getProjectId());
        assertThat(secondTriggerSubscription.getProjectDeploymentId())
            .isNotEqualTo(firstTriggerSubscription.getProjectDeploymentId());

        verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerLifecycleFacade, times(2)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testEnableFailureMarksFailed() {
        ExecutionException enableFailure = createProviderFailure("Webhook registration rejected");

        doThrow(enableFailure).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        Throwable thrown = catchThrowable(() -> createWebhookSubscription(Map.of()));

        TriggerSubscription triggerSubscription = triggerSubscriptionService
            .getConnectedUserTriggerSubscriptions(connectedUser.getId())
            .getFirst();

        assertThat(thrown).isInstanceOfSatisfying(
            TriggerSubscriptionEnableException.class,
            exception -> {
                assertThat(exception.getCause()).isSameAs(enableFailure);
                assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.API_ERROR);
                assertThat(exception.getMessage()).isEqualTo("Webhook registration rejected");
                assertThat(exception.getTriggerSubscriptionId()).isEqualTo(triggerSubscription.getId());
            });

        assertThat(triggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.FAILED);
        assertThat(triggerSubscription.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.API_ERROR);
        assertThat(triggerSubscription.getErrorMessage()).isEqualTo("Webhook registration rejected");

        long projectDeploymentId =
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId");

        assertThat(projectDeploymentService.getProjectDeployment(projectDeploymentId)
            .isEnabled()).isFalse();
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId))
            .singleElement()
            .extracting(ProjectDeploymentWorkflow::isEnabled)
            .isEqualTo(false);
    }

    @Test
    void testEnableFailureWithTimeoutCauseMarksTimeoutError() {
        doThrow(new IllegalStateException("Registration failed", new SocketTimeoutException("Read timed out")))
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        assertThatThrownBy(() -> createWebhookSubscription(Map.of()))
            .isInstanceOf(TriggerSubscriptionEnableException.class)
            .hasCauseInstanceOf(IllegalStateException.class);

        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId()))
            .singleElement()
            .extracting(TriggerSubscription::getErrorCode)
            .isEqualTo(TriggerSubscriptionErrorCode.TIMEOUT_ERROR);
    }

    @Test
    void testEnableFailureFromOAuthTokenRefreshMarksOAuthRefresh() {
        assertEnableFailureErrorCode(
            new ConfigurationException(
                "Unable to perform oauth token refresh", new IllegalStateException("invalid_grant"),
                ConnectionDefinitionErrorType.OAUTH_TOKEN_REFRESH_FAILED),
            TriggerSubscriptionErrorCode.OAUTH_REFRESH);
    }

    @Test
    void testEnableFailureFromRefreshUrlResolutionMarksOAuthRefresh() {
        assertEnableFailureErrorCode(
            new ConfigurationException(
                new IllegalStateException("Missing token url"), ConnectionDefinitionErrorType.TOKEN_REFRESH_FAILED),
            TriggerSubscriptionErrorCode.OAUTH_REFRESH);
    }

    @Test
    void testEnableFailureFromCredentialAcquireMarksOAuthRefresh() {
        assertEnableFailureErrorCode(
            new IllegalStateException(
                "Trigger enable failed",
                new ConfigurationException(
                    new IllegalStateException("401 Unauthorized"), ConnectionDefinitionErrorType.ACQUIRE_FAILED)),
            TriggerSubscriptionErrorCode.OAUTH_REFRESH);
    }

    @Test
    void testEnableFailureFromOtherConnectionDefinitionErrorMarksApiError() {
        assertEnableFailureErrorCode(
            new ConfigurationException(
                new IllegalStateException("Bad header"), ConnectionDefinitionErrorType.AUTHORIZATION_APPLY_FAILED),
            TriggerSubscriptionErrorCode.API_ERROR);
    }

    @Test
    void testEnableFailureFromALocalErrorMarksInternalErrorWithoutItsMessage() {
        IllegalStateException enableFailure = new IllegalStateException("relation \"secret\" does not exist");

        doThrow(enableFailure).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        Throwable thrown = catchThrowable(() -> createWebhookSubscription(Map.of()));

        assertThat(thrown).isInstanceOfSatisfying(
            TriggerSubscriptionEnableException.class,
            exception -> {
                assertThat(exception.getCause()).isSameAs(enableFailure);
                assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.INTERNAL_ERROR);
                assertThat(exception.getMessage()).isEqualTo(INTERNAL_ERROR_MESSAGE);
            });

        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId()))
            .singleElement()
            .satisfies(triggerSubscription -> {
                assertThat(triggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.FAILED);
                assertThat(triggerSubscription.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.INTERNAL_ERROR);
                assertThat(triggerSubscription.getErrorMessage()).isEqualTo(INTERNAL_ERROR_MESSAGE);
            });
    }

    @Test
    void testEnableFailureFromMissingWorkflowConnectionsMarksUserInputError() {
        assertEnableFailureErrorCode(
            new ConfigurationException(
                "Not all required connections are set for a workflow with id=x",
                ProjectDeploymentErrorType.WORKFLOW_CONNECTIONS_NOT_FOUND),
            TriggerSubscriptionErrorCode.USER_INPUT_ERROR);
    }

    @Test
    void testEnableFailureWritingTheStatusDisablesTheTriggerAndKeepsTheOriginalError() {
        doAnswer(invocation -> {
            jdbcClient.sql("DELETE FROM connected_user_trigger_subscription WHERE connected_user_id = :id")
                .param("id", connectedUser.getId())
                .update();

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        assertThatThrownBy(() -> createWebhookSubscription(Map.of()))
            .isInstanceOfSatisfying(
                TriggerSubscriptionEnableException.class,
                exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.INTERNAL_ERROR);
                    assertThat(exception.getMessage()).isEqualTo(INTERNAL_ERROR_MESSAGE);
                    assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
                    assertThat(exception.getCause()
                        .getSuppressed()).isNotEmpty();
                });

        verify(triggerLifecycleFacade, times(1)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testEnableFailureLogsAFailedDeploymentDisableAndKeepsTheOriginalError() {
        IllegalStateException deploymentDisableFailure = new IllegalStateException("Deployment disable failed");

        doAnswer(invocation -> {
            jdbcClient.sql("DELETE FROM connected_user_trigger_subscription WHERE connected_user_id = :id")
                .param("id", connectedUser.getId())
                .update();

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
        doThrow(deploymentDisableFailure).when(triggerLifecycleFacade)
            .executeTriggerDisable(anyString(), any(), any(), anyMap(), any());

        Logger logger = (Logger) LoggerFactory.getLogger(TriggerSubscriptionManager.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        Throwable throwable;

        try {
            throwable = catchThrowable(() -> createWebhookSubscription(Map.of()));
        } finally {
            logger.detachAppender(listAppender);
        }

        assertThat(throwable).isInstanceOfSatisfying(
            TriggerSubscriptionEnableException.class,
            exception -> {
                assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
                assertThat(exception.getCause()
                    .getSuppressed()).contains(deploymentDisableFailure);
            });

        assertThat(listAppender.list).anySatisfy(loggingEvent -> {
            assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
            assertThat(loggingEvent.getFormattedMessage()).contains("project deployment id=");

            IThrowableProxy throwableProxy = loggingEvent.getThrowableProxy();

            assertThat(throwableProxy).isNotNull();
            assertThat(throwableProxy.getMessage()).isEqualTo("Deployment disable failed");
        });
    }

    @Test
    void testEnableAfterFailureEnablesTheTrigger() {
        doThrow(new IllegalStateException("Webhook registration rejected"))
            .doNothing()
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        assertThatThrownBy(() -> createWebhookSubscription(Map.of()))
            .isInstanceOf(TriggerSubscriptionEnableException.class)
            .hasCauseInstanceOf(IllegalStateException.class);

        TriggerSubscription failedTriggerSubscription = triggerSubscriptionService
            .getConnectedUserTriggerSubscriptions(connectedUser.getId())
            .getFirst();

        triggerSubscriptionManager.enable(failedTriggerSubscription.getId());

        TriggerSubscription triggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            failedTriggerSubscription.getId());

        assertThat(triggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(triggerSubscription.getErrorCode()).isNull();
        assertThat(triggerSubscription.getErrorMessage()).isNull();
        assertThat(projectDeploymentService
            .getProjectDeployment(Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId"))
            .isEnabled()).isTrue();
    }

    @Test
    void testEnableOfADisabledDeploymentWithAnEnabledWorkflowRegistersTheTriggerOnce() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());

        projectDeploymentService.updateEnabled(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId"), false);

        jdbcClient.sql("UPDATE connected_user_trigger_subscription SET status = :status WHERE id = :id")
            .param("status", TriggerSubscriptionStatus.FAILED.ordinal())
            .param("id", triggerSubscription.getId())
            .update();

        clearInvocations(triggerLifecycleFacade);

        triggerSubscriptionManager.enable(triggerSubscription.getId());

        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);

        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
        verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testConcurrentEnablesRegisterTheTriggerOnceAndKeepItEnabled() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());
        AtomicBoolean enabled = new AtomicBoolean();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        clearInvocations(triggerLifecycleFacade);

        doAnswer(invocation -> {
            if (enabled.compareAndSet(false, true)) {
                Thread thread = Thread.ofPlatform()
                    .start(() -> {
                        try {
                            triggerSubscriptionManager.enable(triggerSubscription.getId());
                        } catch (RuntimeException exception) {
                            failures.add(exception);
                        }
                    });

                if (!thread.join(Duration.ofSeconds(30))) {
                    failures.add(new IllegalStateException("Concurrent enable did not finish"));
                }
            }

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        triggerSubscriptionManager.enable(triggerSubscription.getId());

        assertThat(failures).isEmpty();
        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId")))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(true);

        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
        verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testEnableIgnoresAnExpiredRegistrationLease() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        setRegistrationLeaseUntil(triggerSubscription, Instant.now()
            .minus(Duration.ofMinutes(1)));

        clearInvocations(triggerLifecycleFacade);

        triggerSubscriptionManager.enable(triggerSubscription.getId());

        TriggerSubscription enabledTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(enabledTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(getRegistrationLeaseUntil(triggerSubscription)).isNull();

        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testEnableLeavesTheRegistrationToTheHolderOfAnActiveLease() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        Instant registrationLeaseUntil = Instant.now()
            .plus(Duration.ofMinutes(5));

        setRegistrationLeaseUntil(triggerSubscription, registrationLeaseUntil);

        clearInvocations(triggerLifecycleFacade);

        triggerSubscriptionManager.enable(triggerSubscription.getId());

        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.DISABLED);
        assertThat(getRegistrationLeaseUntil(triggerSubscription)).isNotNull();

        verify(triggerLifecycleFacade, never()).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testEnableRenewsTheRegistrationLeaseWhileARegistrationOutlastsIt() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());
        AtomicBoolean registering = new AtomicBoolean();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        clearInvocations(triggerLifecycleFacade);

        doAnswer(invocation -> {
            if (registering.compareAndSet(false, true)) {
                Thread.sleep(SHORT_REGISTRATION_LEASE.multipliedBy(2));

                Thread thread = Thread.ofPlatform()
                    .start(() -> {
                        try {
                            triggerSubscriptionManager.enable(triggerSubscription.getId());
                        } catch (RuntimeException exception) {
                            failures.add(exception);
                        }
                    });

                if (!thread.join(Duration.ofSeconds(30))) {
                    failures.add(new IllegalStateException("Concurrent enable did not finish"));
                }

                Thread.sleep(SHORT_REGISTRATION_LEASE);
            }

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        runWithRegistrationLease(
            SHORT_REGISTRATION_LEASE, () -> triggerSubscriptionManager.enable(triggerSubscription.getId()));

        assertThat(failures).isEmpty();
        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(getRegistrationLeaseUntil(triggerSubscription)).isNull();

        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
        verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testEnableThatLostItsRegistrationLeaseLeavesTheSubscriptionToTheNewHolder() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());
        Instant takenOverRegistrationLeaseUntil = Instant.now()
            .plus(Duration.ofMinutes(5))
            .truncatedTo(ChronoUnit.MILLIS);

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        clearInvocations(triggerLifecycleFacade);

        doAnswer(invocation -> {
            setRegistrationLeaseUntil(triggerSubscription, takenOverRegistrationLeaseUntil);

            Thread.sleep(SHORT_REGISTRATION_LEASE.multipliedBy(2));

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        Logger logger = (Logger) LoggerFactory.getLogger(TriggerSubscriptionManager.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            runWithRegistrationLease(
                SHORT_REGISTRATION_LEASE, () -> triggerSubscriptionManager.enable(triggerSubscription.getId()));
        } finally {
            logger.detachAppender(listAppender);
        }

        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.DISABLED);
        assertThat(getRegistrationLeaseUntil(triggerSubscription)).isEqualTo(takenOverRegistrationLeaseUntil);
        assertThat(listAppender.list).anySatisfy(loggingEvent -> {
            assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
            assertThat(loggingEvent.getFormattedMessage())
                .contains("id=" + triggerSubscription.getId())
                .contains("project deployment id=" + triggerSubscription.getProjectDeploymentId())
                .contains("may be orphaned");
        });

        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
        verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testRenewalOfARegistrationLeaseIsNotBlockedByTheRowLockOfAnotherSubscription() {
        TriggerSubscription lockedTriggerSubscription = createWebhookSubscription(Map.of());
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());
        String lockedWorkflowId = getWorkflowId(lockedTriggerSubscription);
        String workflowId = getWorkflowId(triggerSubscription);
        CountDownLatch lockedRegistrationReleased = new CountDownLatch(1);
        CountDownLatch lockedRegistrationStarted = new CountDownLatch(1);
        CountDownLatch rowLocked = new CountDownLatch(1);
        CountDownLatch rowLockReleased = new CountDownLatch(1);
        AtomicBoolean registering = new AtomicBoolean();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        triggerSubscriptionManager.disable(lockedTriggerSubscription.getId(), null);
        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        clearInvocations(triggerLifecycleFacade);

        doAnswer(invocation -> {
            lockedRegistrationStarted.countDown();

            awaitLatch(lockedRegistrationReleased);

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(eq(lockedWorkflowId), any(), any(), anyMap(), any(), anyString(), anyLong());

        doAnswer(invocation -> {
            if (registering.compareAndSet(false, true)) {
                Thread.sleep(SHORT_REGISTRATION_LEASE.multipliedBy(2));

                Thread thread = Thread.ofPlatform()
                    .start(() -> {
                        try {
                            triggerSubscriptionManager.enable(triggerSubscription.getId());
                        } catch (RuntimeException exception) {
                            failures.add(exception);
                        }
                    });

                joinThread(thread, failures);

                Thread.sleep(SHORT_REGISTRATION_LEASE);
            }

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(eq(workflowId), any(), any(), anyMap(), any(), anyString(), anyLong());

        runWithRegistrationLease(SHORT_REGISTRATION_LEASE, () -> {
            Thread lockedEnableThread = Thread.ofPlatform()
                .start(() -> {
                    try {
                        triggerSubscriptionManager.enable(lockedTriggerSubscription.getId());
                    } catch (RuntimeException exception) {
                        failures.add(exception);
                    }
                });
            Thread rowLockThread = null;

            try {
                awaitLatch(lockedRegistrationStarted);

                rowLockThread = holdRowLock(lockedTriggerSubscription, rowLocked, rowLockReleased, failures);

                awaitLatch(rowLocked);

                sleep(SHORT_REGISTRATION_LEASE);

                triggerSubscriptionManager.enable(triggerSubscription.getId());
            } finally {
                rowLockReleased.countDown();

                if (rowLockThread != null) {
                    joinThread(rowLockThread, failures);
                }

                lockedRegistrationReleased.countDown();

                joinThread(lockedEnableThread, failures);
            }
        });

        assertThat(failures).isEmpty();
        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(getRegistrationLeaseUntil(triggerSubscription)).isNull();
        assertThat(getStatus(lockedTriggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(getRegistrationLeaseUntil(lockedTriggerSubscription)).isNull();

        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            eq(workflowId), any(), any(), anyMap(), any(), anyString(), anyLong());
        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            eq(lockedWorkflowId), any(), any(), anyMap(), any(), anyString(), anyLong());
        verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testRenewalThatFindsTheRowLockedKeepsTheRegistrationLease() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());
        CountDownLatch rowLocked = new CountDownLatch(1);
        CountDownLatch rowLockReleased = new CountDownLatch(1);
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        clearInvocations(triggerLifecycleFacade);

        doAnswer(invocation -> {
            Thread rowLockThread = holdRowLock(triggerSubscription, rowLocked, rowLockReleased, failures);

            try {
                awaitLatch(rowLocked);

                Thread.sleep(SHORT_REGISTRATION_LEASE.multipliedBy(2));
            } finally {
                rowLockReleased.countDown();

                joinThread(rowLockThread, failures);
            }

            Thread.sleep(SHORT_REGISTRATION_LEASE);

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        Logger logger = (Logger) LoggerFactory.getLogger(TriggerSubscriptionManager.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            runWithRegistrationLease(
                SHORT_REGISTRATION_LEASE, () -> triggerSubscriptionManager.enable(triggerSubscription.getId()));
        } finally {
            logger.detachAppender(listAppender);
        }

        assertThat(failures).isEmpty();
        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(getRegistrationLeaseUntil(triggerSubscription)).isNull();
        assertThat(listAppender.list).noneSatisfy(
            loggingEvent -> assertThat(loggingEvent.getFormattedMessage()).contains("was taken over"));

        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
        verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testEnableHoldsNoLockOrTransactionOfItsOwnWhileTheTriggerIsRegistered() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());
        List<Integer> advisoryLockCounts = new CopyOnWriteArrayList<>();
        List<Integer> transactionConnectionCounts = new CopyOnWriteArrayList<>();

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        doAnswer(invocation -> {
            advisoryLockCounts.add(
                jdbcClient.sql("SELECT COUNT(*) FROM pg_locks WHERE locktype = 'advisory'")
                    .query(Integer.class)
                    .single());
            transactionConnectionCounts.add(
                jdbcClient.sql(
                    """
                        SELECT COUNT(*) FROM pg_stat_activity
                        WHERE datname = current_database() AND state LIKE 'idle in transaction%'
                        """)
                    .query(Integer.class)
                    .single());

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        triggerSubscriptionManager.enable(triggerSubscription.getId());

        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(transactionConnectionCounts).containsExactly(0);
        assertThat(advisoryLockCounts).containsExactly(0);
    }

    @Test
    void testUpdateWhileAnotherUpdateReRegistersTheTriggerLeavesTheNewDefinitionRegistered() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));
        AtomicBoolean updated = new AtomicBoolean();
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        List<Map<String, ?>> registeredTriggerParameters = new CopyOnWriteArrayList<>();

        clearInvocations(triggerLifecycleFacade);

        doAnswer(invocation -> {
            registeredTriggerParameters.add(invocation.getArgument(3));

            if (updated.compareAndSet(false, true)) {
                Thread thread = Thread.ofPlatform()
                    .start(() -> {
                        try {
                            triggerSubscriptionManager.update(
                                triggerSubscription.getId(), Map.of("respondWith", "c"), null, null, null);
                        } catch (RuntimeException exception) {
                            failures.add(exception);
                        }
                    });

                if (!thread.join(Duration.ofSeconds(30))) {
                    failures.add(new IllegalStateException("Concurrent update did not finish"));
                }
            }

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        TriggerSubscription updatedTriggerSubscription = triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), null, null, null);

        assertThat(failures).isEmpty();
        assertThat(updatedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(updatedTriggerSubscription.getParameters()).isEqualTo(Map.of("respondWith", "c"));
        assertThat(getRegistrationLeaseUntil(triggerSubscription)).isNull();
        assertThat(registeredTriggerParameters)
            .last()
            .isEqualTo(Map.of("respondWith", "c"));
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId")))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(true);

        verify(triggerLifecycleFacade, times(2)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
        verify(triggerLifecycleFacade, times(2)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testEnableFailureOfAnExistingSubscriptionCarriesItsId() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        ExecutionException enableFailure = createProviderFailure("Webhook registration rejected");

        doThrow(enableFailure).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        assertThatThrownBy(() -> triggerSubscriptionManager.enable(triggerSubscription.getId()))
            .isInstanceOfSatisfying(
                TriggerSubscriptionEnableException.class,
                exception -> {
                    assertThat(exception.getCause()).isSameAs(enableFailure);
                    assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.API_ERROR);
                    assertThat(exception.getTriggerSubscriptionId()).isEqualTo(triggerSubscription.getId());
                });

        TriggerSubscription failedTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(failedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.FAILED);
        assertThat(failedTriggerSubscription.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.API_ERROR);
        assertThat(failedTriggerSubscription.getErrorMessage()).isEqualTo("Webhook registration rejected");
    }

    @Test
    void testUpdateEnableFailureMarksFailedAndCarriesTheId() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());

        doThrow(new IllegalStateException("Registration failed", new SocketTimeoutException("Read timed out")))
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        assertThatThrownBy(() -> triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), null, null, null))
                .isInstanceOfSatisfying(
                    TriggerSubscriptionEnableException.class,
                    exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.TIMEOUT_ERROR);
                        assertThat(exception.getTriggerSubscriptionId()).isEqualTo(triggerSubscription.getId());
                    });

        TriggerSubscription failedTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(failedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.FAILED);
        assertThat(failedTriggerSubscription.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.TIMEOUT_ERROR);
    }

    @Test
    void testUpdateFailureAfterTheTriggerWasDisabledRestoresTheOldTrigger() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));

        deleteDraftProjectWorkflow(triggerSubscription);

        assertThatThrownBy(() -> triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), "renamed", null, null))
                .isInstanceOfSatisfying(
                    TriggerSubscriptionEnableException.class,
                    exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.INTERNAL_ERROR);
                        assertThat(exception.getTriggerSubscriptionId()).isEqualTo(triggerSubscription.getId());
                    });

        TriggerSubscription restoredTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(restoredTriggerSubscription.getParameters()).isEqualTo(Map.of("respondWith", "a"));
        assertThat(restoredTriggerSubscription.getName()).isNull();
        assertThat(restoredTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(restoredTriggerSubscription.getErrorCode()).isNull();
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId")))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(true);

        verify(triggerLifecycleFacade, times(1)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerLifecycleFacade, times(2)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testUpdateFailureWhoseTriggerCannotBeRestoredMarksFailed() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));

        deleteDraftProjectWorkflow(triggerSubscription);

        doThrow(new IllegalStateException("Registration failed", new SocketTimeoutException("Read timed out")))
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        assertThatThrownBy(() -> triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), "renamed", null, null))
                .isInstanceOfSatisfying(
                    TriggerSubscriptionEnableException.class,
                    exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.TIMEOUT_ERROR);
                        assertThat(exception.getTriggerSubscriptionId()).isEqualTo(triggerSubscription.getId());
                    });

        TriggerSubscription failedTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(failedTriggerSubscription.getParameters()).isEqualTo(Map.of("respondWith", "a"));
        assertThat(failedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.FAILED);
        assertThat(failedTriggerSubscription.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.TIMEOUT_ERROR);
    }

    @Test
    void testUpdateWhoseTriggerCannotBeUnregisteredMarksFailed() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));

        doThrow(createProviderFailure("Webhook removal rejected"))
            .when(triggerLifecycleFacade)
            .executeTriggerDisable(anyString(), any(), any(), anyMap(), any());

        assertThatThrownBy(() -> triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), null, null, null))
                .isInstanceOfSatisfying(
                    TriggerSubscriptionEnableException.class,
                    exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.API_ERROR);
                        assertThat(exception.getTriggerSubscriptionId()).isEqualTo(triggerSubscription.getId());
                    });

        TriggerSubscription failedTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(failedTriggerSubscription.getParameters()).isEqualTo(Map.of("respondWith", "a"));
        assertThat(failedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.FAILED);
        assertThat(failedTriggerSubscription.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.API_ERROR);
    }

    @Test
    void testUpdateOfADisabledSubscriptionStoresTheValuesWithoutRegisteringTheTrigger() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        TriggerSubscription updatedTriggerSubscription = triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), "renamed", "ext-2", null);

        assertThat(updatedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.DISABLED);
        assertThat(updatedTriggerSubscription.getParameters()).isEqualTo(Map.of("respondWith", "b"));
        assertThat(updatedTriggerSubscription.getName()).isEqualTo("renamed");
        assertThat(updatedTriggerSubscription.getExternalId()).isEqualTo("ext-2");
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId")))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(false);

        verify(triggerLifecycleFacade, times(1)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerLifecycleFacade, times(1)).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testUpdateFailureOfADisabledSubscriptionKeepsItsStatusAndCarriesNoId() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        deleteDraftProjectWorkflow(triggerSubscription);

        assertThatThrownBy(() -> triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), null, null, null))
                .isInstanceOfSatisfying(
                    TriggerSubscriptionEnableException.class,
                    exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.INTERNAL_ERROR);
                        assertThat(exception.getMessage()).isEqualTo(INTERNAL_ERROR_MESSAGE);
                        assertThat(exception.getTriggerSubscriptionId()).isNull();
                    });

        TriggerSubscription currentTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(currentTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.DISABLED);
        assertThat(currentTriggerSubscription.getErrorCode()).isNull();
        assertThat(currentTriggerSubscription.getParameters()).isEqualTo(Map.of("respondWith", "a"));
    }

    @Test
    void testUpdateDoesNotReEnableASubscriptionDisabledWhileItsTriggerWasUnregistered() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        disableOnAnotherThreadDuringFirstTriggerDisable(triggerSubscription.getId(), failures);

        clearInvocations(triggerLifecycleFacade);

        TriggerSubscription updatedTriggerSubscription = triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), "renamed", null, null);

        assertThat(failures).isEmpty();
        assertThat(updatedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.DISABLED);
        assertThat(updatedTriggerSubscription.getParameters()).isEqualTo(Map.of("respondWith", "b"));
        assertThat(updatedTriggerSubscription.getName()).isEqualTo("renamed");
        assertConcurrentlyDisabled(triggerSubscription);

        verify(triggerLifecycleFacade, never()).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testUpdateFailureDoesNotRestoreTheTriggerOfASubscriptionDisabledWhileItWasUnregistered() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        deleteDraftProjectWorkflow(triggerSubscription);

        disableOnAnotherThreadDuringFirstTriggerDisable(triggerSubscription.getId(), failures);

        clearInvocations(triggerLifecycleFacade);

        assertThatThrownBy(() -> triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), null, null, null))
                .isInstanceOf(TriggerSubscriptionEnableException.class);

        assertThat(failures).isEmpty();
        assertThat(triggerSubscriptionService.getTriggerSubscription(triggerSubscription.getId()))
            .extracting(TriggerSubscription::getParameters)
            .isEqualTo(Map.of("respondWith", "a"));
        assertConcurrentlyDisabled(triggerSubscription);

        verify(triggerLifecycleFacade, never()).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testUpdateUnregistersTheTriggerOfASubscriptionDisabledWhileItWasReRegistered() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));
        AtomicBoolean disabled = new AtomicBoolean();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        doAnswer(invocation -> {
            if (disabled.compareAndSet(false, true)) {
                disableOnAnotherThread(triggerSubscription.getId(), failures);
            }

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        TriggerSubscription updatedTriggerSubscription = triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "b"), null, null, null);

        assertThat(failures).isEmpty();
        assertThat(updatedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.DISABLED);
        assertConcurrentlyDisabled(triggerSubscription);
    }

    @Test
    void testEnableKeepsADisableThatCommittedWhileItsTriggerWasRegistered() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        disableOnAnotherThreadDuringFirstTriggerEnable(triggerSubscription.getId(), failures, null);

        triggerSubscriptionManager.enable(triggerSubscription.getId());

        assertThat(failures).isEmpty();
        assertConcurrentlyDisabled(triggerSubscription);
    }

    @Test
    void testEnableFailureKeepsADisableThatCommittedWhileItsTriggerWasRegistered() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of("respondWith", "a"));
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        disableOnAnotherThreadDuringFirstTriggerEnable(
            triggerSubscription.getId(), failures, createProviderFailure("Webhook registration rejected"));

        assertThatThrownBy(() -> triggerSubscriptionManager.enable(triggerSubscription.getId()))
            .isInstanceOf(TriggerSubscriptionEnableException.class);

        assertThat(failures).isEmpty();
        assertThat(triggerSubscriptionService.getTriggerSubscription(triggerSubscription.getId()))
            .extracting(TriggerSubscription::getErrorCode)
            .isNull();
        assertConcurrentlyDisabled(triggerSubscription);
    }

    @Test
    void testEnableAndDisablePassTheConnectionAndTheEnvironmentToTheTrigger() {
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTrigger.class)))
            .thenReturn(
                List.of(
                    ComponentConnection.of(
                        TriggerSubscriptionWorkflowDefinitionFactory.TRIGGER_NAME, WEBHOOK_COMPONENT_NAME,
                        WEBHOOK_COMPONENT_NAME, 1, false)));

        TriggerSubscription triggerSubscription = createSubscription(connectedUser, null, CONNECTION_ID);

        verify(triggerLifecycleFacade).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), eq(CONNECTION_ID), anyString(),
            eq((long) Environment.PRODUCTION.ordinal()));

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        verify(triggerLifecycleFacade).executeTriggerDisable(anyString(), any(), any(), anyMap(), eq(CONNECTION_ID));
    }

    @Test
    void testDisableWithoutReasonSetsDisabled() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        assertThat(triggerSubscriptionService.getTriggerSubscription(triggerSubscription.getId())
            .getStatus()).isEqualTo(TriggerSubscriptionStatus.DISABLED);

        verify(triggerLifecycleFacade).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testDisableWithConnectionReasonSetsConnectionError() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());

        triggerSubscriptionManager.disable(
            triggerSubscription.getId(), TriggerSubscriptionDisableReason.CONNECTION_INVALID);

        TriggerSubscription disabledTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(disabledTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.CONNECTION_ERROR);
        assertThat(disabledTriggerSubscription.getErrorCode()).isNull();
        assertThat(disabledTriggerSubscription.getErrorMessage()).isNull();
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId")))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(false);

        verify(triggerLifecycleFacade).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());

        triggerSubscriptionManager.disable(
            triggerSubscription.getId(), TriggerSubscriptionDisableReason.CONNECTED_USER_DISABLED);

        assertThat(triggerSubscriptionService.getTriggerSubscription(triggerSubscription.getId())
            .getStatus()).isEqualTo(TriggerSubscriptionStatus.DISABLED);

        verify(triggerLifecycleFacade, times(2)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testDisableClearsTheErrorOfAFailedSubscription() {
        doThrow(createProviderFailure("Webhook registration rejected"))
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        assertThatThrownBy(() -> createWebhookSubscription(Map.of()))
            .isInstanceOf(TriggerSubscriptionEnableException.class);

        TriggerSubscription failedTriggerSubscription = triggerSubscriptionService
            .getConnectedUserTriggerSubscriptions(connectedUser.getId())
            .getFirst();

        assertThat(failedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.FAILED);
        assertThat(failedTriggerSubscription.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.API_ERROR);
        assertThat(failedTriggerSubscription.getErrorMessage()).isEqualTo("Webhook registration rejected");

        triggerSubscriptionManager.disable(failedTriggerSubscription.getId(), null);

        TriggerSubscription disabledTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            failedTriggerSubscription.getId());

        assertThat(disabledTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.DISABLED);
        assertThat(disabledTriggerSubscription.getErrorCode()).isNull();
        assertThat(disabledTriggerSubscription.getErrorMessage()).isNull();
    }

    @Test
    void testDeleteRemovesProjectAndRow() {
        TriggerSubscription triggerSubscription = createWebhookSubscription(Map.of());

        long projectId = Validate.notNull(triggerSubscription.getProjectId(), "projectId");
        long projectDeploymentId =
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId");

        triggerSubscriptionManager.delete(triggerSubscription.getId());

        assertThat(triggerSubscriptionService.fetchTriggerSubscription(triggerSubscription.getId())).isEmpty();
        assertThat(projectService.fetchProject(projectId)).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isEmpty();

        verify(triggerLifecycleFacade).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    @Test
    void testDeleteOfAFailedSubscriptionRemovesProjectAndRow() {
        doThrow(new IllegalStateException("Webhook registration rejected"))
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        assertThatThrownBy(() -> createWebhookSubscription(Map.of()))
            .isInstanceOf(TriggerSubscriptionEnableException.class)
            .hasCauseInstanceOf(IllegalStateException.class);

        TriggerSubscription triggerSubscription = triggerSubscriptionService
            .getConnectedUserTriggerSubscriptions(connectedUser.getId())
            .getFirst();

        triggerSubscriptionManager.delete(triggerSubscription.getId());

        assertThat(triggerSubscriptionService.fetchTriggerSubscription(triggerSubscription.getId())).isEmpty();
        assertThat(projectService.fetchProject(triggerSubscription.getProjectId())).isEmpty();
    }

    @Test
    void testConnectionInvalidatedDisablesTriggerAndNotifies() {
        long connectionId = createUniqueId();

        TriggerSubscription triggerSubscription = createSubscription(connectedUser, null, connectionId);
        TriggerSubscription otherTriggerSubscription = createSubscription(connectedUser, null, createUniqueId());

        clearInvocations(triggerLifecycleFacade);

        applicationEventPublisher.publishEvent(new ConnectionCredentialsInvalidatedEvent(connectionId));

        verify(triggerLifecycleFacade, times(1)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());

        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.CONNECTION_ERROR);
        assertThat(getStatus(otherTriggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId")))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(false);

        verify(triggerSubscriptionEventSender, times(1)).sendLifecycleEvent(
            argThat(curTriggerSubscription -> triggerSubscription.getId()
                .equals(curTriggerSubscription.getId())),
            eq("subscription.disabled"), eq(TriggerSubscriptionDisableReason.CONNECTION_INVALID));
        verify(triggerSubscriptionEventSender, times(1)).sendLifecycleEvent(any(), any(), any());
    }

    @Test
    void testDisableByConnectionIdSkipsSubscriptionsThatAreNotEnabled() {
        long connectionId = createUniqueId();

        TriggerSubscription enabledTriggerSubscription = createSubscription(connectedUser, null, connectionId);
        TriggerSubscription disabledTriggerSubscription = createSubscription(connectedUser, null, connectionId);

        triggerSubscriptionManager.disable(disabledTriggerSubscription.getId(), null);

        clearInvocations(triggerLifecycleFacade);

        triggerSubscriptionManager.disableByConnectionId(
            connectionId, TriggerSubscriptionDisableReason.CONNECTION_DELETED);

        assertThat(getStatus(enabledTriggerSubscription)).isEqualTo(TriggerSubscriptionStatus.CONNECTION_ERROR);
        assertThat(getStatus(disabledTriggerSubscription)).isEqualTo(TriggerSubscriptionStatus.DISABLED);

        verify(triggerLifecycleFacade, times(1)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerSubscriptionEventSender, times(1)).sendLifecycleEvent(
            argThat(curTriggerSubscription -> enabledTriggerSubscription.getId()
                .equals(curTriggerSubscription.getId())),
            eq("subscription.disabled"), eq(TriggerSubscriptionDisableReason.CONNECTION_DELETED));
        verify(triggerSubscriptionEventSender, times(1)).sendLifecycleEvent(any(), any(), any());
    }

    @Test
    void testDisableByIntegrationInstanceIdInsideATransactionKeepsTheTransactionAndRecordsTheFailure() {
        long integrationInstanceId = createIntegrationInstance();

        TriggerSubscription failingTriggerSubscription = createSubscription(
            connectedUser, integrationInstanceId, null);
        TriggerSubscription triggerSubscription = createSubscription(connectedUser, integrationInstanceId, null);

        doThrow(new IllegalStateException("Trigger disable failed"))
            .when(triggerLifecycleFacade)
            .executeTriggerDisable(eq(getWorkflowId(failingTriggerSubscription)), any(), any(), anyMap(), any());

        String connectedUserName = "renamed-" + UUID.randomUUID();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            renameConnectedUser(connectedUserName);

            triggerSubscriptionManager.disableByIntegrationInstanceId(
                integrationInstanceId, TriggerSubscriptionDisableReason.INTEGRATION_INSTANCE_DISABLED);
        });

        assertThat(getConnectedUserName()).isEqualTo(connectedUserName);
        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.DISABLED);

        TriggerSubscription currentFailingTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            failingTriggerSubscription.getId());

        assertThat(currentFailingTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.FAILED);
        assertThat(currentFailingTriggerSubscription.getErrorCode()).isNotNull();
        assertThat(currentFailingTriggerSubscription.getErrorMessage()).isNotBlank();

        verify(triggerLifecycleFacade, times(2)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerSubscriptionEventSender, times(1)).sendLifecycleEvent(
            argThat(curTriggerSubscription -> triggerSubscription.getId()
                .equals(curTriggerSubscription.getId())),
            eq("subscription.disabled"), eq(TriggerSubscriptionDisableReason.INTEGRATION_INSTANCE_DISABLED));
        verify(triggerSubscriptionEventSender, times(1)).sendLifecycleEvent(any(), any(), any());
    }

    @Test
    void testDisableByIntegrationInstanceIdInsideARolledBackTransactionDisablesNothing() {
        long integrationInstanceId = createIntegrationInstance();

        TriggerSubscription triggerSubscription = createSubscription(connectedUser, integrationInstanceId, null);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            triggerSubscriptionManager.disableByIntegrationInstanceId(
                integrationInstanceId, TriggerSubscriptionDisableReason.INTEGRATION_INSTANCE_DISABLED);

            transactionStatus.setRollbackOnly();
        });

        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);

        verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerSubscriptionEventSender, never()).sendLifecycleEvent(any(), any(), any());
    }

    @Test
    void testDisableByConnectedUserIdDisablesOnlyThatUsersSubscriptions() {
        ConnectedUser otherConnectedUser = connectedUserService.createConnectedUser(
            "trigger-subscription-other-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        TriggerSubscription triggerSubscription = createSubscription(connectedUser, null, null);
        TriggerSubscription otherTriggerSubscription = createSubscription(otherConnectedUser, null, null);

        triggerSubscriptionManager.disableByConnectedUserId(
            connectedUser.getId(), TriggerSubscriptionDisableReason.CONNECTED_USER_DISABLED);

        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.DISABLED);
        assertThat(getStatus(otherTriggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);

        verify(triggerSubscriptionEventSender, times(1)).sendLifecycleEvent(
            argThat(curTriggerSubscription -> triggerSubscription.getId()
                .equals(curTriggerSubscription.getId())),
            eq("subscription.disabled"), eq(TriggerSubscriptionDisableReason.CONNECTED_USER_DISABLED));
    }

    @Test
    void testDeleteByConnectedUserIdDeletesEverySubscriptionAndNotifies() {
        TriggerSubscription enabledTriggerSubscription = createSubscription(connectedUser, null, null);
        TriggerSubscription disabledTriggerSubscription = createSubscription(connectedUser, null, null);

        triggerSubscriptionManager.disable(disabledTriggerSubscription.getId(), null);

        triggerSubscriptionManager.deleteByConnectedUserId(connectedUser.getId());

        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId())).isEmpty();
        assertThat(projectService.fetchProject(enabledTriggerSubscription.getProjectId())).isEmpty();
        assertThat(projectService.fetchProject(disabledTriggerSubscription.getProjectId())).isEmpty();

        verify(triggerSubscriptionEventSender, times(2)).sendLifecycleEvent(
            any(), eq("subscription.deleted"), eq(TriggerSubscriptionDisableReason.CONNECTED_USER_DELETED));
    }

    @Test
    void testDeleteByConnectedUserIdContinuesAfterARemoteUnregisterFailureAndKeepsTheTransaction() {
        TriggerSubscription failingTriggerSubscription = createSubscription(connectedUser, null, null);
        TriggerSubscription triggerSubscription = createSubscription(connectedUser, null, null);

        doThrow(new IllegalStateException("Trigger disable failed"))
            .when(triggerLifecycleFacade)
            .executeTriggerDisable(eq(getWorkflowId(failingTriggerSubscription)), any(), any(), anyMap(), any());

        String connectedUserName = "renamed-" + UUID.randomUUID();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            renameConnectedUser(connectedUserName);

            triggerSubscriptionManager.deleteByConnectedUserId(connectedUser.getId());
        });

        assertThat(getConnectedUserName()).isEqualTo(connectedUserName);
        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId())).isEmpty();
        assertThat(projectService.fetchProject(failingTriggerSubscription.getProjectId())).isEmpty();
        assertThat(projectService.fetchProject(triggerSubscription.getProjectId())).isEmpty();

        verify(triggerLifecycleFacade, times(2)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerSubscriptionEventSender, times(2)).sendLifecycleEvent(
            any(), eq("subscription.deleted"), eq(TriggerSubscriptionDisableReason.CONNECTED_USER_DELETED));
    }

    @Test
    void testDeleteByConnectedUserIdInsideARolledBackTransactionMarksTheUnregisteredSubscriptionsFailed() {
        TriggerSubscription triggerSubscription = createSubscription(connectedUser, null, null);
        TriggerSubscription otherTriggerSubscription = createSubscription(connectedUser, null, null);
        TriggerSubscription disabledTriggerSubscription = createSubscription(connectedUser, null, null);

        triggerSubscriptionManager.disable(disabledTriggerSubscription.getId(), null);

        IllegalStateException parentDeleteFailure = new IllegalStateException("Parent delete failed");
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(transactionStatus -> {
            triggerSubscriptionManager.deleteByConnectedUserId(connectedUser.getId());

            throw parentDeleteFailure;
        })).isSameAs(parentDeleteFailure);

        for (TriggerSubscription unregisteredTriggerSubscription : List.of(
            triggerSubscription, otherTriggerSubscription)) {

            TriggerSubscription currentTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
                unregisteredTriggerSubscription.getId());

            assertThat(currentTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.FAILED);
            assertThat(currentTriggerSubscription.getErrorCode()).isEqualTo(
                TriggerSubscriptionErrorCode.INTERNAL_ERROR);
            assertThat(currentTriggerSubscription.getErrorMessage()).isEqualTo(
                "Trigger unregistered; delete was rolled back");
        }

        assertThat(getStatus(disabledTriggerSubscription)).isEqualTo(TriggerSubscriptionStatus.DISABLED);
    }

    @Test
    void testDeleteByConnectionIdDeletesEverySubscriptionOfTheConnectionAndNotifies() {
        long connectionId = createUniqueId();

        TriggerSubscription enabledTriggerSubscription = createSubscription(connectedUser, null, connectionId);
        TriggerSubscription disabledTriggerSubscription = createSubscription(connectedUser, null, connectionId);
        TriggerSubscription otherTriggerSubscription = createSubscription(connectedUser, null, createUniqueId());

        triggerSubscriptionManager.disable(
            disabledTriggerSubscription.getId(), TriggerSubscriptionDisableReason.INTEGRATION_INSTANCE_DELETED);

        clearInvocations(triggerLifecycleFacade, triggerSubscriptionEventSender);

        triggerSubscriptionManager.deleteByConnectionId(connectionId);

        assertThat(triggerSubscriptionService.getConnectionTriggerSubscriptions(connectionId)).isEmpty();
        assertThat(projectService.fetchProject(enabledTriggerSubscription.getProjectId())).isEmpty();
        assertThat(projectService.fetchProject(disabledTriggerSubscription.getProjectId())).isEmpty();
        assertThat(getStatus(otherTriggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);

        verify(triggerLifecycleFacade, times(1)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerSubscriptionEventSender, times(2)).sendLifecycleEvent(
            any(), eq("subscription.deleted"), eq(TriggerSubscriptionDisableReason.CONNECTION_DELETED));
        verify(triggerSubscriptionEventSender, times(2)).sendLifecycleEvent(any(), any(), any());
    }

    @Test
    void testDeleteByConnectionIdContinuesAfterARemoteUnregisterFailure() {
        long connectionId = createUniqueId();

        TriggerSubscription failingTriggerSubscription = createSubscription(connectedUser, null, connectionId);
        TriggerSubscription triggerSubscription = createSubscription(connectedUser, null, connectionId);

        doThrow(new IllegalStateException("Trigger disable failed"))
            .when(triggerLifecycleFacade)
            .executeTriggerDisable(eq(getWorkflowId(failingTriggerSubscription)), any(), any(), anyMap(), any());

        triggerSubscriptionManager.deleteByConnectionId(connectionId);

        assertThat(triggerSubscriptionService.getConnectionTriggerSubscriptions(connectionId)).isEmpty();
        assertThat(projectService.fetchProject(failingTriggerSubscription.getProjectId())).isEmpty();
        assertThat(projectService.fetchProject(triggerSubscription.getProjectId())).isEmpty();

        verify(triggerSubscriptionEventSender, times(2)).sendLifecycleEvent(
            any(), eq("subscription.deleted"), eq(TriggerSubscriptionDisableReason.CONNECTION_DELETED));
    }

    @Test
    void testCreateDisablesTheSubscriptionWhenItsConnectedUserIsDisabledWhileItsTriggerIsRegistered() {
        doAnswer(invocation -> {
            runInNewTransaction(() -> {
                connectedUserService.enableConnectedUser(connectedUser.getId(), false);

                triggerSubscriptionManager.disableByConnectedUserId(
                    connectedUser.getId(), TriggerSubscriptionDisableReason.CONNECTED_USER_DISABLED);
            });

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        TriggerSubscription triggerSubscription = createSubscription(connectedUser, null, null);

        assertThat(triggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.DISABLED);

        assertOwnerDisabled(
            triggerSubscription, TriggerSubscriptionStatus.DISABLED,
            TriggerSubscriptionDisableReason.CONNECTED_USER_DISABLED);
    }

    @Test
    void testEnableDisablesTheSubscriptionWhenItsIntegrationInstanceIsDisabledWhileItsTriggerIsRegistered() {
        long integrationInstanceId = createIntegrationInstance();

        TriggerSubscription triggerSubscription = createSubscription(connectedUser, integrationInstanceId, null);

        triggerSubscriptionManager.disable(triggerSubscription.getId(), null);

        clearInvocations(triggerLifecycleFacade);

        doAnswer(invocation -> {
            runInNewTransaction(() -> {
                integrationInstanceService.updateEnabled(integrationInstanceId, false);

                triggerSubscriptionManager.disableByIntegrationInstanceId(
                    integrationInstanceId, TriggerSubscriptionDisableReason.INTEGRATION_INSTANCE_DISABLED);
            });

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        triggerSubscriptionManager.enable(triggerSubscription.getId());

        assertOwnerDisabled(
            triggerSubscription, TriggerSubscriptionStatus.DISABLED,
            TriggerSubscriptionDisableReason.INTEGRATION_INSTANCE_DISABLED);
    }

    @Test
    void testUpdateDisablesTheSubscriptionWhenItsConnectionBecomesInvalidWhileItsTriggerIsRegistered() {
        TriggerSubscription triggerSubscription = createSubscription(connectedUser, null, CONNECTION_ID);

        clearInvocations(triggerLifecycleFacade);

        Connection invalidConnection = new Connection();

        invalidConnection.setCredentialStatus(Connection.CredentialStatus.INVALID);

        doAnswer(invocation -> {
            when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(invalidConnection));

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        TriggerSubscription updatedTriggerSubscription = triggerSubscriptionManager.update(
            triggerSubscription.getId(), Map.of("respondWith", "updated"), null, null, null);

        assertThat(updatedTriggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.CONNECTION_ERROR);

        assertOwnerDisabled(
            triggerSubscription, TriggerSubscriptionStatus.CONNECTION_ERROR,
            TriggerSubscriptionDisableReason.CONNECTION_INVALID);
    }

    @Test
    void testCreateWithAnInstanceIdOfAnotherComponentIsRejected() {
        long integrationInstanceId = createIntegrationInstance();

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            connectedUser.getExternalId(), "github", 1, "newIssue", Map.of(), null, null, null, integrationInstanceId,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> {
                        assertThat(exception.getErrorKey())
                            .isEqualTo(TriggerSubscriptionErrorType.USER_INPUT_ERROR.getErrorKey());
                        assertThat(exception.getMessage()).contains("github");
                    });

        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId())).isEmpty();

        verify(triggerLifecycleFacade, never()).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testCreateWithAnInstanceIdOfAnotherEnvironmentIsRejected() {
        long integrationInstanceId = createIntegrationInstance();

        jdbcClient
            .sql(
                """
                    UPDATE integration_instance_configuration SET environment = :environment
                    WHERE id = (
                        SELECT integration_instance_configuration_id FROM integration_instance WHERE id = :id)
                    """)
            .param("environment", Environment.DEVELOPMENT.ordinal())
            .param("id", integrationInstanceId)
            .update();

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            connectedUser.getExternalId(), WEBHOOK_COMPONENT_NAME, 1, WEBHOOK_TRIGGER_NAME, Map.of(), null, null,
            null, integrationInstanceId, Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> {
                        assertThat(exception.getErrorKey())
                            .isEqualTo(TriggerSubscriptionErrorType.USER_INPUT_ERROR.getErrorKey());
                        assertThat(exception.getMessage()).contains(Environment.PRODUCTION.name());
                    });

        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId())).isEmpty();

        verify(triggerLifecycleFacade, never()).executeTriggerEnable(
            anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testConcurrentCreatesBeyondTheSubscriptionLimitKeepOnlyOne() throws InterruptedException {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.isConnectionRequired()).thenReturn(false);
        when(componentDefinitionService.getComponentDefinition(WEBHOOK_COMPONENT_NAME, 1))
            .thenReturn(componentDefinition);

        CountDownLatch insertedCountDownLatch = new CountDownLatch(2);
        List<Boolean> overlappingInserts = new CopyOnWriteArrayList<>();

        doAnswer(invocation -> {
            Object triggerSubscription = invocation.callRealMethod();

            insertedCountDownLatch.countDown();

            overlappingInserts.add(insertedCountDownLatch.await(5, TimeUnit.SECONDS));

            return triggerSubscription;
        }).when(triggerSubscriptionService)
            .create(any(TriggerSubscription.class));

        Object triggerSubscriptionFacadeTarget = AopTestUtils.getUltimateTargetObject(triggerSubscriptionFacade);

        ReflectionTestUtils.setField(triggerSubscriptionFacadeTarget, "maxTriggerSubscriptionsPerConnectedUser", 1);

        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            List<Thread> threads = new ArrayList<>();

            for (int index = 0; index < 2; index++) {
                threads.add(Thread.ofPlatform()
                    .start(() -> {
                        try {
                            triggerSubscriptionFacade.createTriggerSubscription(
                                connectedUser.getExternalId(), WEBHOOK_COMPONENT_NAME, 1, WEBHOOK_TRIGGER_NAME,
                                Map.of(), null, null, null, null, Environment.PRODUCTION);
                        } catch (RuntimeException exception) {
                            failures.add(exception);
                        }
                    }));
            }

            for (Thread thread : threads) {
                thread.join(Duration.ofSeconds(60));
            }
        } finally {
            ReflectionTestUtils.setField(
                triggerSubscriptionFacadeTarget, "maxTriggerSubscriptionsPerConnectedUser", 0);
        }

        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId()))
            .hasSize(1);
        assertThat(overlappingInserts).containsOnly(false);
        assertThat(failures)
            .singleElement()
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(TriggerSubscriptionErrorType.USER_INPUT_ERROR.getErrorKey()));
    }

    private void assertOwnerDisabled(
        TriggerSubscription triggerSubscription, TriggerSubscriptionStatus expectedStatus,
        TriggerSubscriptionDisableReason expectedReason) {

        assertThat(getStatus(triggerSubscription)).isEqualTo(expectedStatus);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId")))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(false);

        verify(triggerLifecycleFacade).executeTriggerDisable(
            eq(getWorkflowId(triggerSubscription)), any(), any(), anyMap(), any());
        verify(triggerSubscriptionEventSender).sendLifecycleEvent(
            argThat(curTriggerSubscription -> triggerSubscription.getId()
                .equals(curTriggerSubscription.getId())),
            eq("subscription.disabled"), eq(expectedReason));
    }

    private void assertConcurrentlyDisabled(TriggerSubscription triggerSubscription) {
        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.DISABLED);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId")))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(false);
    }

    private void disableOnAnotherThread(long id, List<Throwable> failures) throws InterruptedException {
        Thread thread = Thread.ofPlatform()
            .start(() -> {
                try {
                    triggerSubscriptionManager.disable(id, null);
                } catch (RuntimeException exception) {
                    failures.add(exception);
                }
            });

        if (!thread.join(Duration.ofSeconds(60))) {
            failures.add(new IllegalStateException("Concurrent disable did not finish"));
        }
    }

    private void disableOnAnotherThreadDuringFirstTriggerDisable(long id, List<Throwable> failures) {
        AtomicBoolean disabled = new AtomicBoolean();

        doAnswer(invocation -> {
            if (disabled.compareAndSet(false, true)) {
                disableOnAnotherThread(id, failures);
            }

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
    }

    private void disableOnAnotherThreadDuringFirstTriggerEnable(
        long id, List<Throwable> failures, @Nullable RuntimeException enableFailure) {

        AtomicBoolean disabled = new AtomicBoolean();

        doAnswer(invocation -> {
            if (disabled.compareAndSet(false, true)) {
                disableOnAnotherThread(id, failures);

                if (enableFailure != null) {
                    throw enableFailure;
                }
            }

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());
    }

    private long createIntegrationInstance() {
        return TriggerSubscriptionLifecycleTestSupport.createIntegrationInstance(
            integrationInstanceService, integrationRepository, jdbcClient, connectedUser.getId(), createUniqueId());
    }

    private Thread holdRowLock(
        TriggerSubscription triggerSubscription, CountDownLatch rowLocked, CountDownLatch rowLockReleased,
        List<Throwable> failures) {

        return Thread.ofPlatform()
            .start(() -> {
                try {
                    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

                    transactionTemplate.executeWithoutResult(transactionStatus -> {
                        jdbcClient.sql("SELECT id FROM connected_user_trigger_subscription WHERE id = :id FOR UPDATE")
                            .param("id", triggerSubscription.getId())
                            .query(Long.class)
                            .single();

                        rowLocked.countDown();

                        awaitLatch(rowLockReleased);
                    });
                } catch (RuntimeException exception) {
                    failures.add(exception);
                }
            });
    }

    private static void awaitLatch(CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Latch was not released in time");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private static void joinThread(Thread thread, List<Throwable> failures) {
        try {
            if (!thread.join(Duration.ofSeconds(60))) {
                failures.add(new IllegalStateException("Thread " + thread.getName() + " did not finish"));
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            failures.add(interruptedException);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private void runWithRegistrationLease(Duration registrationLease, Runnable runnable) {
        Object triggerSubscriptionManagerTarget = AopTestUtils.getUltimateTargetObject(triggerSubscriptionManager);

        Object defaultRegistrationLease = ReflectionTestUtils.getField(
            triggerSubscriptionManagerTarget, "registrationLease");

        ReflectionTestUtils.setField(triggerSubscriptionManagerTarget, "registrationLease", registrationLease);

        try {
            runnable.run();
        } finally {
            ReflectionTestUtils.setField(
                triggerSubscriptionManagerTarget, "registrationLease", defaultRegistrationLease);
        }
    }

    private void runInNewTransaction(Runnable runnable) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplate.executeWithoutResult(transactionStatus -> runnable.run());
    }

    private void assertEnableFailureErrorCode(
        RuntimeException enableFailure, TriggerSubscriptionErrorCode expectedErrorCode) {

        doThrow(enableFailure).when(triggerLifecycleFacade)
            .executeTriggerEnable(anyString(), any(), any(), anyMap(), any(), anyString(), anyLong());

        assertThatThrownBy(() -> triggerSubscriptionManager.create(
            connectedUser, null, CONNECTION_ID, WEBHOOK_COMPONENT_NAME, 1, WEBHOOK_TRIGGER_NAME, Map.of(), null, null,
            null, 0))
                .isInstanceOfSatisfying(
                    TriggerSubscriptionEnableException.class,
                    exception -> {
                        assertThat(exception.getCause()).isSameAs(enableFailure);
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
                    });

        assertThat(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUser.getId()))
            .singleElement()
            .extracting(TriggerSubscription::getErrorCode)
            .isEqualTo(expectedErrorCode);
    }

    private static ExecutionException createProviderFailure(String message) {
        return new ExecutionException(message, TriggerDefinitionErrorType.DYNAMIC_WEBHOOK_ENABLE_FAILED);
    }

    private TriggerSubscription createSubscription(
        ConnectedUser subscriptionConnectedUser, Long integrationInstanceId, Long connectionId) {

        return triggerSubscriptionManager.create(
            subscriptionConnectedUser, integrationInstanceId, connectionId, WEBHOOK_COMPONENT_NAME, 1,
            WEBHOOK_TRIGGER_NAME, Map.of(), null, null, null, 0);
    }

    private static long createUniqueId() {
        return SECURE_RANDOM
            .nextLong(1_000_000L, Long.MAX_VALUE);
    }

    private String getConnectedUserName() {
        return jdbcClient.sql("SELECT name FROM connected_user WHERE id = :id")
            .param("id", connectedUser.getId())
            .query(String.class)
            .single();
    }

    private @Nullable Instant getRegistrationLeaseUntil(TriggerSubscription triggerSubscription) {
        TriggerSubscription currentTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        return currentTriggerSubscription.getRegistrationLeaseUntil();
    }

    private void setRegistrationLeaseUntil(TriggerSubscription triggerSubscription, Instant registrationLeaseUntil) {
        jdbcClient.sql(
            "UPDATE connected_user_trigger_subscription SET registration_lease_until = :leaseUntil WHERE id = :id")
            .param("leaseUntil", Timestamp.from(registrationLeaseUntil))
            .param("id", triggerSubscription.getId())
            .update();
    }

    private TriggerSubscriptionStatus getStatus(TriggerSubscription triggerSubscription) {
        TriggerSubscription currentTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        return currentTriggerSubscription.getStatus();
    }

    private TriggerSubscription createWebhookSubscription(Map<String, ?> parameters) {
        return triggerSubscriptionManager.create(
            connectedUser, null, null, WEBHOOK_COMPONENT_NAME, 1, WEBHOOK_TRIGGER_NAME, parameters, null, null, null,
            0);
    }

    private String getWorkflowId(TriggerSubscription triggerSubscription) {
        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId"));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflows.getFirst();

        return projectDeploymentWorkflow.getWorkflowId();
    }

    private void deleteDraftProjectWorkflow(TriggerSubscription triggerSubscription) {
        jdbcClient.sql(
            """
                DELETE FROM project_workflow
                WHERE project_id = :projectId
                    AND project_version = (
                        SELECT MAX(project_version) FROM project_workflow WHERE project_id = :projectId)
                """)
            .param("projectId", triggerSubscription.getProjectId())
            .update();
    }

    private void renameConnectedUser(String name) {
        jdbcClient.sql("UPDATE connected_user SET name = :name WHERE id = :id")
            .param("name", name)
            .param("id", connectedUser.getId())
            .update();
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}
