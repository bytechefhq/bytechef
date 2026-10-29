/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.exception.ProvidedValueLockedException;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.domain.ComponentDefinition;
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
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserReferenceRolloutManagerIntTest.ConnectedUserServiceConfiguration.class
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
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserReferenceRolloutManagerIntTest {

    private static final long ADMINISTRATOR_CONNECTION_ID = 900L;
    private static final long PRODUCTION_ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();
    private static final String EXTERNAL_USER_ID = "rollout-user-1";
    private static final long ROLLOUT_COMPLETION_TIMEOUT_SECONDS = 60;
    private static final int COALESCED_REQUEST_COUNT = 5;
    private static final long ROLLOUT_LOCK_WAIT_SECONDS = 5;
    private static final long RUNNING_FIRST_TEMPLATE_JOB_ID = 4242L;

    private static final String SLACK_AND_JIRA_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}},
         {"name":"createIssue1","type":"jira/v1/createIssue","parameters":{}}]}
        """;

    private static final String SLACK_TRIGGER_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[{"name":"newMessage1","type":"slack/v1/newMessage","parameters":{}}],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[{"name":"newMessage1","type":"slack/v1/newMessage","parameters":{}}],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    @Autowired
    private AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade;

    @Autowired
    private AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @MockitoBean
    private AutomationWorkflowProjectConfigurationUpdatedEventListener automationWorkflowProjectConfigurationUpdatedEventListener;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserReferenceAttentionResolver connectedUserReferenceAttentionResolver;

    @Autowired
    private ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;

    @Autowired
    private ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;

    @Autowired
    private ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobFacade jobFacade;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        if (connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION)
            .isEmpty()) {

            connectedUserService.createConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION);
        }

        when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
            .thenReturn(new ComponentDefinition("slack"));
        when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of());

        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                .thenReturn(Page.empty());

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(true);
    }

    @Test
    void testTwoTemplatesFromOneVisualProjectShareOneDeploymentAtThePublishedVersion() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Two Templates");

        String firstUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);
        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow second = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        assertThat(second.getProjectDeploymentId()).isEqualTo(first.getProjectDeploymentId());

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            first.getProjectDeploymentId());

        assertThat(projectDeployment.getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(first.getProjectDeploymentId()))
            .hasSize(2)
            .allSatisfy(row -> assertThat(row.getConnections())
                .containsExactly(new ProjectDeploymentWorkflowConnection(777L, "slack", "postMessage1")));
    }

    @Test
    void testEnableReferenceAfterRepublishDoesNotFail() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Republish Enable");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, false, Environment.PRODUCTION);

        publishAndPinProject(automationWorkflowProjectId);

        String versionTwoWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);

        assertThatCode(() -> connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION))
                .doesNotThrowAnyException();

        ConnectedUserProjectWorkflow enabledReference = connectedUserProjectWorkflowRepository
            .findById(reference.getId())
            .orElseThrow();

        assertThat(enabledReference.isEnabled()).isTrue();
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(reference.getProjectDeploymentId()))
            .singleElement()
            .satisfies(row -> {
                assertThat(row.getWorkflowId()).isEqualTo(versionTwoWorkflowId);
                assertThat(row.isEnabled()).isTrue();
            });
    }

    @Test
    void testDeleteLastReferenceRemovesItsRowAndTheDeployment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Row");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.deleteReference(EXTERNAL_USER_ID, workflowUuid,
            Environment.PRODUCTION);

        assertThat(fetchReferenceDeployment(automationWorkflowProjectId)).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isEmpty();
    }

    @Test
    void testChangingOneReferenceNeverStopsAnotherTemplatesRunningJobsOrTriggers() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Sibling Rows");

        String firstUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        assertThat(first.isEnabled()).isTrue();

        String firstWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(firstUuid);

        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), argThat(workflowIds -> workflowIds.contains(firstWorkflowId)),
            anyBoolean(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(RUNNING_FIRST_TEMPLATE_JOB_ID)));

        ConnectedUserProjectWorkflow second = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, secondUuid, false, Environment.PRODUCTION);
        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, secondUuid, true, Environment.PRODUCTION);

        assertThat(second.getProjectDeploymentId()).isEqualTo(first.getProjectDeploymentId());

        verify(jobFacade, never()).stopJob(RUNNING_FIRST_TEMPLATE_JOB_ID);
        verify(triggerLifecycleFacade, times(1))
            .executeTriggerEnable(eq(firstWorkflowId), any(), any(), any(), any(), any(), anyLong());
        verify(triggerLifecycleFacade, never())
            .executeTriggerDisable(eq(firstWorkflowId), any(), any(), any(), any());
    }

    @Test
    void testDeleteAutomationWorkflowProjectRemovesTheReferenceDeploymentAndDanglesTheReference() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Automation Workflow");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isTrue();

        long projectDeploymentId = reference.getProjectDeploymentId();
        String workflowId = projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);

        assertThatCode(() -> automationWorkflowProjectFacade.deleteProject(automationWorkflowProjectId))
            .doesNotThrowAnyException();

        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isEmpty();
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId)).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .satisfies(deletedReference -> {
                assertThat(deletedReference.isDangling()).isTrue();
                assertThat(deletedReference.isEnabled()).isFalse();
            });

        verify(triggerLifecycleFacade).executeTriggerDisable(eq(workflowId), any(), any(), any(), any());
    }

    @Test
    void testFailedProvisioningLeavesNoReferenceAndNoDeployment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Atomic Provisioning");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenThrow(new IllegalStateException("Connection lookup failed"));

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(IllegalStateException.class);

        assertThat(connectedUserProjectWorkflowRepository.findAll())
            .noneMatch(reference -> workflowUuid.equals(reference.getAutomationWorkflowUuid()));
        assertThat(fetchReferenceDeployment(automationWorkflowProjectId)).isEmpty();
    }

    @Test
    void testEnableReferenceWithAMissingConnectionThrowsAndPausesTheRowButKeepsTheSwitchOn() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Missing Connection Enable");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isTrue();

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class);

        ConnectedUserProjectWorkflow reloadedReference = connectedUserProjectWorkflowRepository
            .findById(reference.getId())
            .orElseThrow();

        assertThat(reloadedReference.isEnabled()).isTrue();
        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
    }

    @Test
    void testReProvisioningAnExistingReferenceWithAMissingConnectionKeepsIntentAndThrows() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Re-provision Missing Connection");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isTrue();

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION, Map.of(), Map.of()))
                .isInstanceOf(MissingConnectionException.class);

        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
    }

    @Test
    void testConfigurationSavePausesAWorkflowWhoseConnectionIsGoneAndKeepsIntent() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Save Pauses");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        connectedUserReferenceRolloutManager.rollOutAndApply(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isEqualTo("MISSING_CONNECTION:slack");
    }

    @Test
    void testConfigurationSaveResumesAPausedWorkflowOnceItsConnectionIsAvailable() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Save Resumes");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        connectedUserReferenceRolloutManager.rollOutAndApply(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();

        stubEntitledSlackConnection(778L);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isNull();
    }

    @Test
    void testInputsCanBeSetOnAReferenceAndAreListed() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Inputs");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        long environmentId = Environment.PRODUCTION.ordinal();

        when(environmentService.getEnvironment(environmentId)).thenReturn(Environment.PRODUCTION);

        connectedUserProjectFacade.updateProjectWorkflowInputs(
            EXTERNAL_USER_ID, workflowUuid, Map.of("channel", "#alerts"), environmentId);

        assertThat(
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(EXTERNAL_USER_ID, Environment.PRODUCTION))
                .filteredOn(workflow -> Objects.equals(workflow.automationWorkflowUuid(), workflowUuid))
                .singleElement()
                .extracting(ConnectedUserProjectWorkflowDTO::inputValues)
                .isEqualTo(Map.of("channel", "#alerts"));
    }

    @Test
    void testProvisionWithAMissingRequiredInputSucceedsDisabledAndEnableRefusesUntilItIsSet() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Required Input");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isFalse();

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION))
                .isInstanceOf(MissingInputException.class)
                .extracting("inputName")
                .isEqualTo("channel");

        connectedUserReferenceDeploymentManager.updateInputs(
            reference.getProjectDeploymentId(), workflowUuid, Map.of("channel", "#alerts"));

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isEnabled)
            .isEqualTo(true);
    }

    @Test
    void testUpdatingInputsOnAnEnabledReferenceRefusesToDropARequiredValue() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Enabled Required Input");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserReferenceDeploymentManager.updateInputs(
            reference.getProjectDeploymentId(), workflowUuid, Map.of("channel", "#alerts"));

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION);

        long environmentId = Environment.PRODUCTION.ordinal();

        when(environmentService.getEnvironment(environmentId)).thenReturn(Environment.PRODUCTION);

        assertThatThrownBy(() -> connectedUserProjectFacade.updateProjectWorkflowInputs(
            EXTERNAL_USER_ID, workflowUuid, Map.of("note", "no channel here"), environmentId))
                .isInstanceOf(MissingInputException.class)
                .extracting("inputName")
                .isEqualTo("channel");

        assertThat(connectedUserReferenceDeploymentManager.getInputs(reference.getProjectDeploymentId(), workflowUuid))
            .isEqualTo(Map.of("channel", "#alerts"));

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isEnabled)
            .isEqualTo(true);
    }

    @Test
    void testRepublishMovesReferencesToTheNewVersionAndKeepsInputs() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout");

        String firstUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);
        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        connectedUserReferenceDeploymentManager.updateInputs(
            first.getProjectDeploymentId(), firstUuid, Map.of("channel", "#alerts"));

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, firstUuid, true, Environment.PRODUCTION);

        publishAndPinProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        long projectDeploymentId = first.getProjectDeploymentId();

        assertThat(projectDeploymentService.getProjectDeployment(projectDeploymentId)
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId)).hasSize(2);
        assertThat(connectedUserReferenceDeploymentManager.getInputs(projectDeploymentId, firstUuid))
            .isEqualTo(Map.of("channel", "#alerts"));
        assertThat(connectedUserProjectWorkflowRepository.findById(first.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isEnabled)
            .isEqualTo(true);
    }

    @Test
    void testPublishingDoesNotMoveReferences() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Publish Keeps Pin");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testUpdatingTheConfigurationVersionRollsReferencesForward() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Pin Forward");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        updateProductionConfigurationVersion(automationWorkflowProjectId, 2);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testRollOutMovesReferencesToAnOlderPinnedVersion() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Pin Back");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);
        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);

        updateProductionConfigurationVersion(automationWorkflowProjectId, 1);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testRollOutToAVersionLackingATemplateDanglesItsReferencesWithTheReleasedVersionReason() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Roll Back Without Template");

        String keptUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        String addedUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow keptReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, keptUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow addedReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, addedUuid, Environment.PRODUCTION);

        updateProductionConfigurationVersion(automationWorkflowProjectId, 1);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(connectedUserProjectWorkflowRepository.findById(addedReference.getId()))
            .get()
            .satisfies(reference -> {
                assertThat(reference.isDangling()).isTrue();
                assertThat(reference.isEnabled()).isFalse();
                assertThat(reference.getDanglingReason())
                    .isEqualTo("Not in the version released to this environment");
            });
        assertThat(connectedUserProjectWorkflowRepository.findById(keptReference.getId()))
            .get()
            .satisfies(reference -> assertThat(reference.isDangling()).isFalse());
    }

    @Test
    void testRollOutOnlyTouchesTheConfiguredEnvironment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Pin Per Environment");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        automationWorkflowProjectConfigurationService.create(
            automationWorkflowProjectId, Environment.DEVELOPMENT.ordinal(), 1, true, true);

        String developmentExternalUserId = "rollout-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(developmentExternalUserId, Environment.DEVELOPMENT);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow productionReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow developmentReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            developmentExternalUserId, workflowUuid, Environment.DEVELOPMENT);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        updateProductionConfigurationVersion(automationWorkflowProjectId, 2);

        AutomationWorkflowProjectConfiguration developmentConfiguration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, Environment.DEVELOPMENT.ordinal())
            .orElseThrow();

        automationWorkflowProjectConfigurationService.update(developmentConfiguration.getId(), 2, true, true);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(productionReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentService.getProjectDeployment(developmentReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testConcurrentRolloutsForTheSamePairCompleteWithoutRolloutFailures() throws InterruptedException {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Concurrent Rollouts");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow secondReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            createConnectedUser(), workflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        updateProductionConfigurationVersion(automationWorkflowProjectId, 2);
        updateProductionConfigurationVersion(automationWorkflowProjectId, 3);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishedLatch = new CountDownLatch(2);
        AtomicReference<Throwable> rolloutFailure = new AtomicReference<>();

        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutManagerLogger();

        try {
            for (int index = 0; index < 2; index++) {
                startRolloutThread(automationWorkflowProjectId, startLatch, finishedLatch, rolloutFailure);
            }

            startLatch.countDown();

            boolean finished = finishedLatch.await(ROLLOUT_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(finished).isTrue();
        } finally {
            detachAppenderFromRolloutManagerLogger(appender);
        }

        assertThat(rolloutFailure.get()).isNull();
        assertThat(appender.list)
            .noneSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.ERROR));
        assertThat(List.of(firstReference.getProjectDeploymentId(), secondReference.getProjectDeploymentId()))
            .allSatisfy(projectDeploymentId -> assertThat(
                projectDeploymentService.getProjectDeployment(projectDeploymentId)
                    .getProjectVersion()).isEqualTo(3));
        assertThat(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(firstReference.getProjectDeploymentId()))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::getWorkflowId)
                .isEqualTo(getWorkflowId(automationWorkflowProjectId, 3, workflowUuid));
    }

    @Test
    void testRolloutWaitsForTheRolloutLockWhileAdministratorSavesDoNot() throws InterruptedException {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Waits On Lock");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        updateProductionConfigurationVersion(automationWorkflowProjectId, 2);

        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID)
            .orElseThrow();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishedLatch = new CountDownLatch(1);
        CountDownLatch saveFinishedLatch = new CountDownLatch(1);
        AtomicInteger heldAdvisoryLockCount = new AtomicInteger();
        AtomicBoolean rolloutFinishedWhileLocked = new AtomicBoolean();
        AtomicBoolean rolloutFinishedAfterTheSave = new AtomicBoolean();
        AtomicBoolean saveFinishedWhileLocked = new AtomicBoolean();
        AtomicReference<Throwable> rolloutFailure = new AtomicReference<>();
        AtomicReference<Throwable> saveFailure = new AtomicReference<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            automationWorkflowProjectConfigurationService.lockRollout(
                automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

            heldAdvisoryLockCount.set(countAdvisoryLocksHeldByThisSession());

            startRolloutThread(automationWorkflowProjectId, startLatch, finishedLatch, rolloutFailure);

            startLatch.countDown();

            rolloutFinishedWhileLocked.set(awaitQuietly(finishedLatch, ROLLOUT_LOCK_WAIT_SECONDS));

            Thread saveThread = new Thread(() -> {
                try {
                    automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(
                        configuration.getId(), 3, true, true);
                } catch (RuntimeException | AssertionError throwable) {
                    saveFailure.set(throwable);
                } finally {
                    saveFinishedLatch.countDown();
                }
            });

            saveThread.setDaemon(true);
            saveThread.start();

            saveFinishedWhileLocked.set(awaitQuietly(saveFinishedLatch, ROLLOUT_COMPLETION_TIMEOUT_SECONDS));
            rolloutFinishedAfterTheSave.set(finishedLatch.getCount() == 0);
        });

        boolean finished = finishedLatch.await(ROLLOUT_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(heldAdvisoryLockCount.get()).isEqualTo(1);
        assertThat(saveFinishedWhileLocked.get()).isTrue();
        assertThat(saveFailure.get()).isNull();
        assertThat(finished).isTrue();
        assertThat(rolloutFailure.get()).isNull();
        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(3);
        assertThat(rolloutFinishedWhileLocked.get()).isFalse();
        assertThat(rolloutFinishedAfterTheSave.get()).isFalse();
    }

    @Test
    void testRolloutsForDifferentPairsDoNotWaitOnEachOther() throws InterruptedException {
        long lockedAutomationWorkflowProjectId = createAutomationWorkflowProject("Locked Pair");

        addWorkflow(lockedAutomationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(lockedAutomationWorkflowProjectId);

        long automationWorkflowProjectId = createAutomationWorkflowProject("Free Pair");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        updateProductionConfigurationVersion(automationWorkflowProjectId, 2);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishedLatch = new CountDownLatch(1);
        AtomicInteger heldAdvisoryLockCount = new AtomicInteger();
        AtomicBoolean finishedWhileTheOtherPairWasLocked = new AtomicBoolean();
        AtomicReference<Throwable> rolloutFailure = new AtomicReference<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            automationWorkflowProjectConfigurationService.lockRollout(
                lockedAutomationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

            heldAdvisoryLockCount.set(countAdvisoryLocksHeldByThisSession());

            startRolloutThread(automationWorkflowProjectId, startLatch, finishedLatch, rolloutFailure);

            startLatch.countDown();

            finishedWhileTheOtherPairWasLocked.set(awaitQuietly(finishedLatch, ROLLOUT_COMPLETION_TIMEOUT_SECONDS));
        });

        assertThat(heldAdvisoryLockCount.get()).isEqualTo(1);
        assertThat(finishedWhileTheOtherPairWasLocked.get()).isTrue();
        assertThat(rolloutFailure.get()).isNull();
        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testRepublishAddingAConnectionTheUserLacksPausesOnlyThatReferenceAndKeepsIntent() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Missing");

        String slackUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String otherUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow slackReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, slackUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow otherReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, otherUuid, Environment.PRODUCTION);

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(slackUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(draftWorkflowId, SLACK_AND_JIRA_WORKFLOW_DEFINITION, draftWorkflow.getVersion());

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenAnswer(invocation -> switch (invocation.<WorkflowTask>getArgument(0)
                .getName()) {
                case "postMessage1" -> List.of(new ComponentConnection("slack", 1, "postMessage1", "slack", true));
                case "createIssue1" -> List.of(new ComponentConnection("jira", 1, "createIssue1", "jira", true));
                default -> List.of();
            });
        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("jira"), eq(List.of()))).thenReturn(List.of());

        publishAndPinProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(slackReference).isEnabled()).isFalse();
        assertThat(fetchDeploymentRow(otherReference).isEnabled()).isTrue();
        assertThat(connectedUserProjectWorkflowRepository.findById(slackReference.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isEnabled)
            .isEqualTo(true);
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(slackReference))
            .attentionReason()).isEqualTo("MISSING_CONNECTION:jira");
    }

    @Test
    void testRolloutAddingRequiredInputPausesButKeepsIntent() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Adds Input");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();

        publishWithRequiredChannelInput(automationWorkflowProjectId, workflowUuid);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isEqualTo("INPUT_REQUIRED:channel");
    }

    @Test
    void testUserDisabledWorkflowStaysDisabledAfterRolloutAndInputs() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout User Off");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, false, Environment.PRODUCTION);

        publishWithRequiredChannelInput(automationWorkflowProjectId, workflowUuid);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isFalse();

        connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            EXTERNAL_USER_ID, workflowUuid, Map.of("channel", "#alerts"), Environment.PRODUCTION);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#alerts"));
        assertThat(getStoredReference(reference).isEnabled()).isFalse();
    }

    @Test
    void testRepublishWithoutATemplateMarksItsReferenceDangling() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Removed");

        String keptUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String removedUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow keptReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, keptUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow removedReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, removedUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.deleteProjectWorkflow(removedUuid);
        publishAndPinProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(connectedUserProjectWorkflowRepository.findById(removedReference.getId()))
            .get()
            .satisfies(reference -> {
                assertThat(reference.isDangling()).isTrue();
                assertThat(reference.isEnabled()).isFalse();
            });
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            keptReference.getProjectDeploymentId())).hasSize(1);
    }

    @Test
    void testRepublishWithoutAnyReferencedTemplateDeletesTheDeployment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout All Removed");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        String onlyReferencedUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, onlyReferencedUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.deleteProjectWorkflow(onlyReferencedUuid);
        publishAndPinProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isDangling)
            .isEqualTo(true);
        assertThat(fetchReferenceDeployment(automationWorkflowProjectId)).isEmpty();
    }

    @Test
    void testEnableCatchesUpADeploymentLeftBehind() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Lazy");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        publishAndPinProject(automationWorkflowProjectId);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testDeleteProjectWorkflowRemovesTheTemplateFromTheDraftByItsUuid() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete By Uuid");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        String removedUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        automationWorkflowProjectFacade.deleteProjectWorkflow(removedUuid);

        publishAndPinProject(automationWorkflowProjectId);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 1, removedUuid))
            .isPresent();
        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 2, removedUuid)).isEmpty();
    }

    @Test
    void testDeletingATemplateTwiceNeverTouchesThePublishedVersion() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Twice");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        String publishedWorkflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);

        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);
        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 1, workflowUuid))
            .isPresent();
        assertThat(connectedUserReferenceDeploymentManager.fetchWorkflowRow(
            reference.getProjectDeploymentId(), publishedWorkflowId))
                .get()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(true);
    }

    @Test
    void testProvisioningATemplateNewerThanTheDeploymentCatchesTheDeploymentUp() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Newer Template");

        String firstUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow second = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        long projectDeploymentId = first.getProjectDeploymentId();

        assertThat(second.getProjectDeploymentId()).isEqualTo(projectDeploymentId);
        assertThat(projectDeploymentService.getProjectDeployment(projectDeploymentId)
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId))
            .extracting(ProjectDeploymentWorkflow::getWorkflowId)
            .containsExactlyInAnyOrder(
                getWorkflowId(automationWorkflowProjectId, 2, firstUuid),
                getWorkflowId(automationWorkflowProjectId, 2, secondUuid));
    }

    @Test
    void testRollOutContinuesWithTheNextDeploymentWhenOneFails() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Isolated Failure");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        String failingExternalUserId = createConnectedUser();
        String otherExternalUserId = createConnectedUser();

        ConnectedUserProjectWorkflow failingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            failingExternalUserId, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow otherReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            otherExternalUserId, workflowUuid, Environment.PRODUCTION);

        assertThat(otherReference.getProjectDeploymentId()).isNotEqualTo(failingReference.getProjectDeploymentId());

        publishAndPinProject(automationWorkflowProjectId);

        ConnectedUser failingConnectedUser = connectedUserService.getConnectedUser(
            failingExternalUserId, Environment.PRODUCTION);

        when(connectedUserConnectionFacade.getConnections(failingConnectedUser.getId(), "slack", List.of()))
            .thenThrow(new IllegalStateException("Connection lookup failed"));

        assertThatCode(
            () -> connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID))
                .doesNotThrowAnyException();

        assertThat(projectDeploymentService.getProjectDeployment(failingReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            failingReference.getProjectDeploymentId()))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::getWorkflowId)
                .isEqualTo(getWorkflowId(automationWorkflowProjectId, 1, workflowUuid));
        assertThat(projectDeploymentService.getProjectDeployment(otherReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            otherReference.getProjectDeploymentId()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getWorkflowId())
                        .isEqualTo(getWorkflowId(automationWorkflowProjectId, 2, workflowUuid));
                    assertThat(row.isEnabled()).isTrue();
                });
    }

    @Test
    void testRollOutLogsAnOptimisticLockingFailureAtWarnAndOtherFailuresAtError() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Failure Log Levels");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        String concurrentExternalUserId = createConnectedUser();
        String failingExternalUserId = createConnectedUser();

        ConnectedUserProjectWorkflow concurrentReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            concurrentExternalUserId, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow failingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            failingExternalUserId, workflowUuid, Environment.PRODUCTION);

        publishAndPinProject(automationWorkflowProjectId);

        ConnectedUser concurrentConnectedUser = connectedUserService.getConnectedUser(
            concurrentExternalUserId, Environment.PRODUCTION);
        ConnectedUser failingConnectedUser = connectedUserService.getConnectedUser(
            failingExternalUserId, Environment.PRODUCTION);

        when(connectedUserConnectionFacade.getConnections(concurrentConnectedUser.getId(), "slack", List.of()))
            .thenThrow(new IllegalStateException(
                "Failed to execute DbAction", new OptimisticLockingFailureException("Row was updated concurrently")));
        when(connectedUserConnectionFacade.getConnections(failingConnectedUser.getId(), "slack", List.of()))
            .thenThrow(new IllegalStateException("Connection lookup failed"));

        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutManagerLogger();

        try {
            connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

            assertThat(appender.list)
                .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
                .containsExactlyInAnyOrder(
                    tuple(
                        Level.WARN,
                        ("Rolling out automation workflow project id=%d to deployment id=%d lost a concurrent update; "
                            +
                            "it will converge on the next publish or enable")
                                .formatted(automationWorkflowProjectId, concurrentReference.getProjectDeploymentId())),
                    tuple(
                        Level.ERROR,
                        "Rolling out automation workflow project id=%d to deployment id=%d failed"
                            .formatted(automationWorkflowProjectId, failingReference.getProjectDeploymentId())));
        } finally {
            detachAppenderFromRolloutManagerLogger(appender);
        }
    }

    @Test
    void testRollOutDeletesADeploymentWhoseOnlyReferenceIsDangling() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Dangling Only");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        markDangling(reference);

        publishAndPinProject(automationWorkflowProjectId);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 2, workflowUuid))
            .isPresent();

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.fetchProjectDeployment(reference.getProjectDeploymentId())).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .satisfies(danglingReference -> {
                assertThat(danglingReference.isDangling()).isTrue();
                assertThat(danglingReference.isEnabled()).isFalse();
            });
    }

    @Test
    void testRollOutIsAQuietNoOpWhenNothingIsReleased() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Unpublished");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutManagerLogger();

        try {
            assertThatCode(() -> connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId,
                PRODUCTION_ENVIRONMENT_ID))
                    .doesNotThrowAnyException();

            assertThat(appender.list)
                .noneSatisfy(event -> assertThat(event.getLevel()
                    .isGreaterOrEqual(Level.WARN)).isTrue());
            assertThat(appender.list)
                .allSatisfy(event -> assertThat(event.getThrowableProxy()).isNull());
        } finally {
            detachAppenderFromRolloutManagerLogger(appender);
        }
    }

    @Test
    void testRollOutWithAnUnknownEnvironmentIdDoesNotThrow() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Unknown Environment");

        assertThatCode(() -> connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, 99L))
            .doesNotThrowAnyException();
    }

    @Test
    void testEnableReferenceWhileTheConfigurationIsOffDoesNotThrow() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Configuration Off");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        setProductionConfigurationEnabled(automationWorkflowProjectId, false);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        assertThatCode(() -> connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION)).doesNotThrowAnyException();
        assertThatCode(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION)).doesNotThrowAnyException();

        assertThat(connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(reference.getProjectDeploymentId()))
            .isFalse();
        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
    }

    @Test
    void testApplyConfigurationStateKeepsTheUsersOwnOffSwitch() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Apply Keeps User Off");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, false, Environment.PRODUCTION);

        setProductionConfigurationEnabled(automationWorkflowProjectId, false);

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();

        setProductionConfigurationEnabled(automationWorkflowProjectId, true);

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())
            .orElseThrow()
            .isEnabled()).isFalse();
    }

    @Test
    void testDisablingAConfigurationStopsEveryReferenceInThatEnvironment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Apply Disables All");

        String firstUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        String otherExternalUserId = createConnectedUser();

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow second = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow third = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            otherExternalUserId, firstUuid, Environment.PRODUCTION);

        List<ConnectedUserProjectWorkflow> references = List.of(first, second, third);

        for (ConnectedUserProjectWorkflow reference : references) {
            assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();
        }

        setProductionConfigurationEnabled(automationWorkflowProjectId, false);

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        for (ConnectedUserProjectWorkflow reference : references) {
            assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
            assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())
                .orElseThrow()
                .isEnabled()).isTrue();
        }
    }

    @Test
    void testDisablingOneWorkflowStopsOnlyThatTemplatesReferences() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Apply Disables One");

        String firstUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow second = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID)
            .orElseThrow();

        automationWorkflowProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), UUID.fromString(firstUuid), false);

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(first).isEnabled()).isFalse();
        assertThat(fetchDeploymentRow(second).isEnabled()).isTrue();
    }

    @Test
    void testDeleteConfigurationKeepsReferenceDeployments() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Apply After Delete");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID)
            .orElseThrow();

        automationWorkflowProjectConfigurationAdminFacade.deleteConfiguration(configuration.getId());

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchReferenceDeployment(automationWorkflowProjectId)).isPresent();
        assertThat(projectDeploymentService.fetchProjectDeployment(reference.getProjectDeploymentId())).isPresent();
        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())
            .orElseThrow()
            .isEnabled()).isTrue();

        automationWorkflowProjectConfigurationService.create(
            automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID, 1, true, true);

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();
    }

    @Test
    void testRollOutDeletesAReferenceDeploymentWithoutReferencesAndRollsOutTheOthers() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Without References");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        String emptyExternalUserId = "rollout-empty-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(emptyExternalUserId, Environment.PRODUCTION);

        long emptyProjectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProjectId, emptyExternalUserId, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        publishAndPinProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.fetchProjectDeployment(emptyProjectDeploymentId)).isEmpty();
        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(reference.getProjectDeploymentId()))
            .singleElement()
            .extracting(ProjectDeploymentWorkflow::getWorkflowId)
            .isEqualTo(getWorkflowId(automationWorkflowProjectId, 2, workflowUuid));
    }

    @Test
    void testRollOutLeavesADeploymentThatIsNotAReferenceDeploymentAlone() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Vendor Deployment");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        ProjectDeployment vendorProjectDeployment = new ProjectDeployment();

        vendorProjectDeployment.setEnvironment(Environment.PRODUCTION);
        vendorProjectDeployment.setName("Vendor deployment");
        vendorProjectDeployment.setProjectId(automationWorkflowProjectId);
        vendorProjectDeployment.setProjectVersion(1);

        long vendorProjectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            vendorProjectDeployment, List.of(), List.of());

        publishAndPinProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.fetchProjectDeployment(vendorProjectDeploymentId))
            .get()
            .extracting(ProjectDeployment::getProjectVersion)
            .isEqualTo(1);
    }

    @Test
    void testRollOutLeavesALegacyNamedMarkerDeploymentWithoutAConnectedUserReferenceDeploymentRelationAlone() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Marker Without Relation");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        long markerProjectDeploymentId = createProjectDeployment(
            automationWorkflowProjectId, "__EMBEDDED__" + EXTERNAL_USER_ID + "__PRODUCTION");

        publishAndPinProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.fetchProjectDeployment(markerProjectDeploymentId))
            .get()
            .extracting(ProjectDeployment::getProjectVersion)
            .isEqualTo(1);
    }

    @Test
    void testDeleteReferenceDeploymentsLeavesLegacyNamedMarkerDeploymentsWithoutARelationAndAdminDeploymentsAlone() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Marker Deployment");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        long markerProjectDeploymentId = createProjectDeployment(
            automationWorkflowProjectId, "__EMBEDDED__other-user__PRODUCTION");
        long adminProjectDeploymentId = createProjectDeployment(automationWorkflowProjectId, "Admin deployment");

        connectedUserReferenceRolloutManager.deleteReferenceDeployments(automationWorkflowProjectId);

        assertThat(projectDeploymentService.fetchProjectDeployment(markerProjectDeploymentId)).isPresent();
        assertThat(projectDeploymentService.fetchProjectDeployment(adminProjectDeploymentId)).isPresent();
    }

    @Test
    void testRollOutDeploymentIfBehindDeletesADeploymentWithoutReferences() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Lazy Without References");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        String emptyExternalUserId = "rollout-lazy-empty-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(emptyExternalUserId, Environment.PRODUCTION);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProjectId, emptyExternalUserId, Environment.PRODUCTION);

        publishAndPinProject(automationWorkflowProjectId);

        assertThat(connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(projectDeploymentId)).isTrue();
        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isEmpty();
    }

    @Test
    void testRollOutDeploymentIfBehindReturnsFalseForALegacyNamedMarkerDeploymentWithoutReferences() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Lazy Marker");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        long markerProjectDeploymentId = createProjectDeployment(
            automationWorkflowProjectId, "__EMBEDDED__" + EXTERNAL_USER_ID + "__PRODUCTION");

        publishAndPinProject(automationWorkflowProjectId);

        assertThat(connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(markerProjectDeploymentId))
            .isFalse();
        assertThat(projectDeploymentService.getProjectDeployment(markerProjectDeploymentId)
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testDeleteReferenceDeploymentsDeletesARenamedReferenceDeploymentAndDanglesItsReferences() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Renamed Reference Deployment");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        long projectDeploymentId = reference.getProjectDeploymentId();

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        projectDeployment.setName("Renamed reference deployment");

        projectDeploymentService.update(projectDeployment);

        connectedUserReferenceRolloutManager.deleteReferenceDeployments(automationWorkflowProjectId);

        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .satisfies(storedReference -> {
                assertThat(storedReference.isDangling()).isTrue();
                assertThat(storedReference.isEnabled()).isFalse();
            });
    }

    @Test
    void testAProvidedConnectionIsUsedForEveryUsersWorkflowWithoutEntitlement() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Connection Every User");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);
        stubNoEntitledSlackConnection();
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationWorkflowProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow secondReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            createConnectedUser(), workflowUuid, Environment.PRODUCTION);

        for (ConnectedUserProjectWorkflow reference : List.of(firstReference, secondReference)) {
            assertThat(getStoredReference(reference).isEnabled()).isTrue();
            assertThat(fetchDeploymentRow(reference))
                .satisfies(row -> {
                    assertThat(row.isEnabled()).isTrue();
                    assertThat(row.getConnections())
                        .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                        .containsExactly(ADMINISTRATOR_CONNECTION_ID);
                });
        }

        assertThat(firstReference.getProjectDeploymentId()).isNotEqualTo(secondReference.getProjectDeploymentId());
    }

    @Test
    void testAUsersOwnConnectionIsIgnoredWhileAConnectionIsProvidedAndChangesReachExistingReferences() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Connection Overrides");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(getRowConnectionIds(reference)).containsExactly(777L);

        provideValuesAndRollOut(
            automationWorkflowProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();
        assertThat(getRowConnectionIds(reference)).containsExactly(ADMINISTRATOR_CONNECTION_ID);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION);

        assertThat(getRowConnectionIds(reference)).containsExactly(ADMINISTRATOR_CONNECTION_ID);
    }

    @Test
    void testRemovingAProvidedConnectionAutoWiresTheUsersOwnConnection() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Connection Removed Auto-wire");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationWorkflowProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(getRowConnectionIds(reference)).containsExactly(ADMINISTRATOR_CONNECTION_ID);

        provideValuesAndRollOut(automationWorkflowProjectId, workflowUuid, Map.of(), Map.of());

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();
        assertThat(getRowConnectionIds(reference)).containsExactly(777L);
    }

    @Test
    void testRemovingAProvidedConnectionTheUserCannotReplacePausesTheWorkflowAndKeepsIntent() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Connection Removed Missing");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);
        stubNoEntitledSlackConnection();
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationWorkflowProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();

        provideValuesAndRollOut(automationWorkflowProjectId, workflowUuid, Map.of(), Map.of());

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getRowConnectionIds(reference)).isEmpty();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isEqualTo("MISSING_CONNECTION:slack");
    }

    @Test
    void testADeletedProvidedConnectionIsIgnoredAndTheSlotFallsBackToTheUser() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Connection Deleted");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationWorkflowProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(getRowConnectionIds(reference)).containsExactly(ADMINISTRATOR_CONNECTION_ID);

        when(connectionService.fetchConnection(ADMINISTRATOR_CONNECTION_ID)).thenReturn(Optional.empty());

        connectedUserReferenceRolloutManager.rollOutAndApply(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();
        assertThat(getRowConnectionIds(reference)).containsExactly(777L);

        stubNoEntitledSlackConnection();

        connectedUserReferenceRolloutManager.rollOutAndApply(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isEqualTo("MISSING_CONNECTION:slack");
    }

    @Test
    void testProvidedInputsOverrideTheUsersValueAndNeverCountAsMissing() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Inputs");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(automationWorkflowProjectId, workflowUuid, Map.of(), Map.of("channel", "#administrator"));

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(getStoredReference(firstReference).isEnabled()).isTrue();
        assertThat(fetchDeploymentRow(firstReference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#administrator"));
            });

        String secondExternalUserId = createConnectedUser();

        ConnectedUserProjectWorkflow secondReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            secondExternalUserId, workflowUuid, Environment.PRODUCTION, Map.of(), Map.of("note", "kept"));

        assertThat(fetchDeploymentRow(secondReference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#administrator", "note", "kept"));
            });

        connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            secondExternalUserId, workflowUuid, Map.of(), Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(secondReference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#administrator"));
            });
    }

    @Test
    void testChangingProvidedInputsReachesExistingReferencesWithoutAVersionChange() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Inputs Change");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION, Map.of(), Map.of("channel", "#mine"));

        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#mine"));

        provideValuesAndRollOut(automationWorkflowProjectId, workflowUuid, Map.of(), Map.of("channel", "#first"));

        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#first"));

        provideValuesAndRollOut(automationWorkflowProjectId, workflowUuid, Map.of(), Map.of("channel", "#second"));

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#second"));
            });
    }

    @Test
    void testProvidedKeysThatAreNotSlotsOrInputsOfTheVersionAreIgnored() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Unknown Keys");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);
        stubNoEntitledSlackConnection();
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID)
            .orElseThrow();

        automationWorkflowProjectConfigurationService.updateWorkflowProvidedValues(
            configuration.getId(), UUID.fromString(workflowUuid),
            Map.of("jira", 901L, "slack", ADMINISTRATOR_CONNECTION_ID),
            Map.of("channel", "#administrator", "unknown", "ignored"));

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getConnections())
                    .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                    .containsExactly(ADMINISTRATOR_CONNECTION_ID);
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#administrator"));
            });
    }

    @Test
    void testRequestingAConnectionForAProvidedComponentIsRejected() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Connection Locked");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationWorkflowProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        String externalUserId = createConnectedUser();

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION, Map.of("slack", 777L)))
                .isInstanceOfSatisfying(ProvidedValueLockedException.class, exception -> {
                    assertThat(exception.getKind()).isEqualTo(ProvidedValueLockedException.Kind.CONNECTION);
                    assertThat(exception.getName()).isEqualTo("slack");
                });

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION);

        assertThat(getRowConnectionIds(reference)).containsExactly(ADMINISTRATOR_CONNECTION_ID);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION, Map.of("slack", 777L)))
                .isInstanceOfSatisfying(
                    ProvidedValueLockedException.class,
                    exception -> assertThat(exception.getName()).isEqualTo("slack"));

        assertThat(getRowConnectionIds(reference)).containsExactly(ADMINISTRATOR_CONNECTION_ID);
    }

    @Test
    void testSettingAProvidedInputIsRejectedOnProvisioningAndOnTheInputsUpdate() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Input Locked");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        provideValues(automationWorkflowProjectId, workflowUuid, Map.of(), Map.of("channel", "#administrator"));

        String externalUserId = createConnectedUser();

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION, Map.of(), Map.of("channel", "#mine")))
                .isInstanceOfSatisfying(ProvidedValueLockedException.class, exception -> {
                    assertThat(exception.getKind()).isEqualTo(ProvidedValueLockedException.Kind.INPUT);
                    assertThat(exception.getName()).isEqualTo("channel");
                });

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION, Map.of(), Map.of("note", "kept"));

        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#administrator", "note", "kept"));
            });

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            externalUserId, workflowUuid, Map.of("channel", "#mine", "note", "changed"), Environment.PRODUCTION))
                .isInstanceOfSatisfying(ProvidedValueLockedException.class, exception -> {
                    assertThat(exception.getKind()).isEqualTo(ProvidedValueLockedException.Kind.INPUT);
                    assertThat(exception.getName()).isEqualTo("channel");
                });

        assertThat(fetchDeploymentRow(reference).getInputs())
            .isEqualTo(Map.of("channel", "#administrator", "note", "kept"));

        connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            externalUserId, workflowUuid, Map.of("note", "changed"), Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#administrator", "note", "changed"));
            });
    }

    @Test
    void testTemplateAndWorkflowReadsMarkProvidedValuesAndLeaveOutProvidedInputValues() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Reads");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubConnectionRequiredSlackComponentDefinition();
        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        String externalUserId = createConnectedUser();

        assertThat(getTemplate(externalUserId, workflowUuid))
            .satisfies(template -> {
                assertThat(template.components())
                    .extracting(
                        ConnectedUserWorkflowTemplateDTO.Component::name,
                        ConnectedUserWorkflowTemplateDTO.Component::provided)
                    .containsExactly(tuple("slack", false));
                assertThat(template.inputs())
                    .extracting(ConnectedUserWorkflowTemplateDTO.Input::name,
                        ConnectedUserWorkflowTemplateDTO.Input::provided)
                    .containsExactly(tuple("channel", false));
            });

        provideValues(
            automationWorkflowProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID),
            Map.of("channel", "#administrator"));

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION, Map.of(), Map.of("note", "kept"));

        assertThat(getTemplate(externalUserId, workflowUuid))
            .satisfies(template -> {
                assertThat(template.components())
                    .extracting(
                        ConnectedUserWorkflowTemplateDTO.Component::name,
                        ConnectedUserWorkflowTemplateDTO.Component::provided)
                    .containsExactly(tuple("slack", true));
                assertThat(template.inputs())
                    .extracting(ConnectedUserWorkflowTemplateDTO.Input::name,
                        ConnectedUserWorkflowTemplateDTO.Input::provided)
                    .containsExactly(tuple("channel", true));
            });

        assertThat(connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION))
            .filteredOn(workflow -> Objects.equals(workflow.automationWorkflowUuid(), workflowUuid))
            .singleElement()
            .satisfies(workflow -> {
                assertThat(workflow.components())
                    .extracting(
                        ConnectedUserWorkflowTemplateDTO.Component::name,
                        ConnectedUserWorkflowTemplateDTO.Component::provided)
                    .containsExactly(tuple("slack", true));
                assertThat(workflow.inputs())
                    .extracting(ConnectedUserWorkflowTemplateDTO.Input::name,
                        ConnectedUserWorkflowTemplateDTO.Input::provided)
                    .containsExactly(tuple("channel", true));
                assertThat(workflow.inputValues()).isEqualTo(Map.of("note", "kept"));
            });
    }

    @Test
    void testRemovingAProvidedInputTakesTheAdministratorsValueOffEveryRowAndPausesTheWorkflow() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Input Removed");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        provideValues(automationWorkflowProjectId, workflowUuid, Map.of(), Map.of("channel", "#administrator"));

        String runningExternalUserId = createConnectedUser();
        String stoppedExternalUserId = createConnectedUser();

        ConnectedUserProjectWorkflow runningReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            runningExternalUserId, workflowUuid, Environment.PRODUCTION, Map.of(), Map.of("note", "kept"));
        ConnectedUserProjectWorkflow stoppedReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            stoppedExternalUserId, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            stoppedExternalUserId, workflowUuid, false, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(runningReference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#administrator", "note", "kept"));
            });
        assertThat(fetchDeploymentRow(stoppedReference).getInputs()).isEqualTo(Map.of("channel", "#administrator"));

        provideValuesAndRollOut(automationWorkflowProjectId, workflowUuid, Map.of(), Map.of());

        assertThat(fetchDeploymentRow(runningReference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isFalse();
                assertThat(row.getInputs()).isEqualTo(Map.of("note", "kept"));
            });
        assertThat(getStoredReference(runningReference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(runningReference))
            .attentionReason()).isEqualTo("INPUT_REQUIRED:channel");
        assertThat(getInputValues(runningExternalUserId, workflowUuid)).isEqualTo(Map.of("note", "kept"));

        assertThat(fetchDeploymentRow(stoppedReference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isFalse();
                assertThat(row.getInputs()).isEmpty();
            });
        assertThat(getStoredReference(stoppedReference).isEnabled()).isFalse();
        assertThat(getInputValues(stoppedExternalUserId, workflowUuid)).isEmpty();
    }

    @Test
    void testDeletingTheConfigurationTakesTheAdministratorsProvidedValuesOffEveryRow() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Provided Input Configuration Deleted");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        provideValues(automationWorkflowProjectId, workflowUuid, Map.of(), Map.of("channel", "#administrator"));

        String externalUserId = createConnectedUser();

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#administrator"));

        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID)
            .orElseThrow();

        automationWorkflowProjectConfigurationAdminFacade.deleteConfiguration(configuration.getId());

        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isFalse();
                assertThat(row.getInputs()).isEmpty();
            });
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(getInputValues(externalUserId, workflowUuid)).isEmpty();
    }

    @Test
    void testSavingTheConfigurationAgainWithUnchangedValuesLeavesRowsAndTriggersAlone() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Unchanged Save");

        String workflowUuid = addWorkflow(
            automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        Map<String, Long> providedConnections = Map.of("slack", ADMINISTRATOR_CONNECTION_ID);
        Map<String, Object> providedInputs = Map.of("channel", "#administrator");

        provideValuesAndRollOut(automationWorkflowProjectId, workflowUuid, providedConnections, providedInputs);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            createConnectedUser(), workflowUuid, Environment.PRODUCTION);

        String workflowId = fetchDeploymentRow(reference).getWorkflowId();

        verify(triggerLifecycleFacade, times(1))
            .executeTriggerEnable(eq(workflowId), any(), any(), any(), any(), any(), anyLong());

        provideValuesAndRollOut(automationWorkflowProjectId, workflowUuid, providedConnections, providedInputs);

        ProjectDeploymentWorkflow rowBeforeTheSecondSave = fetchDeploymentRow(reference);

        clearInvocations(triggerLifecycleFacade);

        provideValuesAndRollOut(automationWorkflowProjectId, workflowUuid, providedConnections, providedInputs);

        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID)
            .orElseThrow();

        automationWorkflowProjectConfigurationAdminFacade.updateConfiguration(
            configuration.getId(), configuration.getProjectVersion(), configuration.isEnabled(),
            configuration.isAutomationHubVisible());

        rollOutTheLastUpdatedEvent();

        verifyNoInteractions(triggerLifecycleFacade);
        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getVersion()).isEqualTo(rowBeforeTheSecondSave.getVersion());
                assertThat(row.getLastModifiedDate()).isEqualTo(rowBeforeTheSecondSave.getLastModifiedDate());
            });

        provideValuesAndRollOut(
            automationWorkflowProjectId, workflowUuid, providedConnections, Map.of("channel", "#changed"));

        verify(triggerLifecycleFacade, times(1))
            .executeTriggerEnable(eq(workflowId), any(), any(), any(), any(), any(), anyLong());
        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#changed"));
    }

    @Test
    void testConcurrentRolloutRequestsForOnePairCoalesceIntoTwoPassesEndingInTheNewestState()
        throws InterruptedException {

        long automationWorkflowProjectId = createAutomationWorkflowProject("Coalesced Rollouts");

        ConnectedUserProjectWorkflow reference = createReferenceBehindTwoPublishedVersions(automationWorkflowProjectId);

        CoalescedRollout coalescedRollout = runRequestsWhileARolloutWaitsForTheLock(
            automationWorkflowProjectId, COALESCED_REQUEST_COUNT, 3);

        assertThat(coalescedRollout.rolloutFailure()).isNull();
        assertThat(coalescedRollout.requestsReturnedWhileTheRolloutWaited()).isTrue();
        assertThat(coalescedRollout.rolloutFinishedWhileLocked()).isFalse();
        assertThat(coalescedRollout.passCount()).isEqualTo(2);
        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(3);
    }

    @Test
    void testARequestDuringARunningRolloutRunsExactlyOneMorePass() throws InterruptedException {
        long automationWorkflowProjectId = createAutomationWorkflowProject("One More Pass");

        ConnectedUserProjectWorkflow reference = createReferenceBehindTwoPublishedVersions(automationWorkflowProjectId);

        CoalescedRollout coalescedRollout = runRequestsWhileARolloutWaitsForTheLock(
            automationWorkflowProjectId, 1, 3);

        assertThat(coalescedRollout.rolloutFailure()).isNull();
        assertThat(coalescedRollout.requestsReturnedWhileTheRolloutWaited()).isTrue();
        assertThat(coalescedRollout.passCount()).isEqualTo(2);
        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(3);

        Logger logger = getRolloutManagerLogger();
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutManagerLogger();

        logger.setLevel(Level.DEBUG);

        try {
            connectedUserReferenceRolloutManager.rollOutAndApply(
                automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);
        } finally {
            logger.setLevel(previousLevel);

            detachAppenderFromRolloutManagerLogger(appender);
        }

        assertThat(countRolloutPasses(appender, automationWorkflowProjectId)).isEqualTo(1);
    }

    @Test
    void testRolloutRequestsForDifferentPairsAreNotCoalesced() throws InterruptedException {
        long lockedAutomationWorkflowProjectId = createAutomationWorkflowProject("Coalescing Locked Pair");

        addWorkflow(lockedAutomationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(lockedAutomationWorkflowProjectId);

        long automationWorkflowProjectId = createAutomationWorkflowProject("Coalescing Free Pair");

        ConnectedUserProjectWorkflow reference = createReferenceBehindTwoPublishedVersions(automationWorkflowProjectId);

        updateProductionConfigurationVersion(automationWorkflowProjectId, 2);

        CountDownLatch startedLatch = new CountDownLatch(0);
        CountDownLatch lockedFinishedLatch = new CountDownLatch(1);
        CountDownLatch freeFinishedLatch = new CountDownLatch(1);
        AtomicBoolean freeFinishedWhileTheOtherPairWaited = new AtomicBoolean();
        AtomicBoolean lockedFinishedWhileLocked = new AtomicBoolean();
        AtomicReference<Throwable> rolloutFailure = new AtomicReference<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            automationWorkflowProjectConfigurationService.lockRollout(
                lockedAutomationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

            startRolloutThread(lockedAutomationWorkflowProjectId, startedLatch, lockedFinishedLatch, rolloutFailure);

            awaitAWaitingAdvisoryLock();

            startRolloutThread(automationWorkflowProjectId, startedLatch, freeFinishedLatch, rolloutFailure);

            freeFinishedWhileTheOtherPairWaited.set(
                awaitQuietly(freeFinishedLatch, ROLLOUT_COMPLETION_TIMEOUT_SECONDS));
            lockedFinishedWhileLocked.set(lockedFinishedLatch.getCount() == 0);
        });

        boolean lockedFinished = lockedFinishedLatch.await(ROLLOUT_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(freeFinishedWhileTheOtherPairWaited.get()).isTrue();
        assertThat(lockedFinishedWhileLocked.get()).isFalse();
        assertThat(lockedFinished).isTrue();
        assertThat(rolloutFailure.get()).isNull();
        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    private CoalescedRollout runRequestsWhileARolloutWaitsForTheLock(
        long automationWorkflowProjectId, int requestCount, int newestProjectVersion) throws InterruptedException {

        CountDownLatch startedLatch = new CountDownLatch(0);
        CountDownLatch rolloutFinishedLatch = new CountDownLatch(1);
        CountDownLatch requestsFinishedLatch = new CountDownLatch(requestCount);
        AtomicBoolean requestsReturnedWhileTheRolloutWaited = new AtomicBoolean();
        AtomicBoolean rolloutFinishedWhileLocked = new AtomicBoolean();
        AtomicReference<Throwable> rolloutFailure = new AtomicReference<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        TransactionTemplate saveTransactionTemplate = new TransactionTemplate(platformTransactionManager);

        saveTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Logger logger = getRolloutManagerLogger();
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutManagerLogger();

        logger.setLevel(Level.DEBUG);

        boolean rolloutFinished;

        try {
            transactionTemplate.executeWithoutResult(status -> {
                automationWorkflowProjectConfigurationService.lockRollout(
                    automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);

                startRolloutThread(automationWorkflowProjectId, startedLatch, rolloutFinishedLatch, rolloutFailure);

                awaitAWaitingAdvisoryLock();

                saveTransactionTemplate.executeWithoutResult(
                    saveStatus -> updateProductionConfigurationVersion(
                        automationWorkflowProjectId, newestProjectVersion));

                for (int index = 0; index < requestCount; index++) {
                    startRolloutThread(automationWorkflowProjectId, startedLatch, requestsFinishedLatch,
                        rolloutFailure);
                }

                requestsReturnedWhileTheRolloutWaited.set(
                    awaitQuietly(requestsFinishedLatch, ROLLOUT_COMPLETION_TIMEOUT_SECONDS));
                rolloutFinishedWhileLocked.set(rolloutFinishedLatch.getCount() == 0);
            });

            rolloutFinished = rolloutFinishedLatch.await(ROLLOUT_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            logger.setLevel(previousLevel);

            detachAppenderFromRolloutManagerLogger(appender);
        }

        assertThat(rolloutFinished).isTrue();

        return new CoalescedRollout(
            requestsReturnedWhileTheRolloutWaited.get(), rolloutFinishedWhileLocked.get(),
            countRolloutPasses(appender, automationWorkflowProjectId), rolloutFailure.get());
    }

    private ConnectedUserProjectWorkflow createReferenceBehindTwoPublishedVersions(long automationWorkflowProjectId) {
        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            createConnectedUser(), workflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        return reference;
    }

    private void awaitAWaitingAdvisoryLock() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(ROLLOUT_COMPLETION_TIMEOUT_SECONDS);

        while (countWaitingAdvisoryLocks() == 0) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("No rollout started waiting for the rollout lock");
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                throw new IllegalStateException(interruptedException);
            }
        }
    }

    private int countWaitingAdvisoryLocks() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_locks WHERE locktype = 'advisory' AND NOT granted", Integer.class));
    }

    private static long countRolloutPasses(ListAppender<ILoggingEvent> appender, long automationWorkflowProjectId) {
        String passMessagePrefix = "Running a rollout pass of automation workflow project id=%d ".formatted(
            automationWorkflowProjectId);

        return appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .filter(message -> message.startsWith(passMessagePrefix))
            .count();
    }

    private Map<String, ?> getInputValues(String externalUserId, String workflowUuid) {
        return connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION)
            .stream()
            .filter(workflow -> Objects.equals(workflow.automationWorkflowUuid(), workflowUuid))
            .findFirst()
            .orElseThrow()
            .inputValues();
    }

    private ConnectedUserWorkflowTemplateDTO getTemplate(String externalUserId, String workflowUuid) {
        return automationWorkflowProjectFacade.getPublishedProjects(externalUserId, Environment.PRODUCTION)
            .stream()
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .filter(template -> Objects.equals(template.workflowUuid(), workflowUuid))
            .findFirst()
            .orElseThrow();
    }

    private void stubConnectionRequiredSlackComponentDefinition() {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn("slack");
        when(componentDefinition.getTitle()).thenReturn("Slack");
        when(componentDefinition.isConnectionRequired()).thenReturn(true);
        when(componentDefinitionService.fetchComponentDefinition("slack", 1))
            .thenReturn(Optional.of(componentDefinition));
    }

    private List<Long> getRowConnectionIds(ConnectedUserProjectWorkflow reference) {
        return fetchDeploymentRow(reference).getConnections()
            .stream()
            .map(ProjectDeploymentWorkflowConnection::getConnectionId)
            .toList();
    }

    private void provideValues(
        long automationWorkflowProjectId, String workflowUuid, Map<String, Long> providedConnections,
        Map<String, Object> providedInputs) {

        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID)
            .orElseThrow();

        automationWorkflowProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            configuration.getId(), workflowUuid, providedConnections, providedInputs);
    }

    private void provideValuesAndRollOut(
        long automationWorkflowProjectId, String workflowUuid, Map<String, Long> providedConnections,
        Map<String, Object> providedInputs) {

        provideValues(automationWorkflowProjectId, workflowUuid, providedConnections, providedInputs);

        rollOutTheLastUpdatedEvent();
    }

    private void rollOutTheLastUpdatedEvent() {
        ArgumentCaptor<AutomationWorkflowProjectConfigurationUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(
            AutomationWorkflowProjectConfigurationUpdatedEvent.class);

        verify(automationWorkflowProjectConfigurationUpdatedEventListener, atLeastOnce())
            .onAutomationWorkflowProjectConfigurationUpdated(eventCaptor.capture());

        AutomationWorkflowProjectConfigurationUpdatedEvent event = eventCaptor.getValue();

        connectedUserReferenceRolloutManager.rollOutAndApply(
            event.projectId(), event.environmentId(), event.removedProvidedInputNames());
    }

    private void stubAdministratorSlackConnection(long connectionId) {
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(connectionId);
        connection.setType(PlatformType.EMBEDDED);

        when(connectionService.fetchConnection(connectionId)).thenReturn(Optional.of(connection));
        when(connectionService.getConnection(connectionId)).thenReturn(connection);
    }

    private void stubNoEntitledSlackConnection() {
        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());
    }

    private ConnectedUserProjectWorkflow getStoredReference(ConnectedUserProjectWorkflow reference) {
        return connectedUserProjectWorkflowRepository.findById(reference.getId())
            .orElseThrow();
    }

    private void publishWithRequiredChannelInput(long automationWorkflowProjectId, String workflowUuid) {
        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        publishAndPinProject(automationWorkflowProjectId);
    }

    private ProjectDeploymentWorkflow fetchDeploymentRow(ConnectedUserProjectWorkflow reference) {
        return connectedUserReferenceDeploymentManager
            .fetchRow(reference.getProjectDeploymentId(), reference.getAutomationWorkflowUuid())
            .orElseThrow();
    }

    private void setProductionConfigurationEnabled(long automationWorkflowProjectId, boolean enabled) {
        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID)
            .orElseThrow();

        automationWorkflowProjectConfigurationService.updateEnabled(configuration.getId(), enabled);
    }

    private long createProjectDeployment(long automationWorkflowProjectId, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(automationWorkflowProjectId);
        projectDeployment.setProjectVersion(1);

        return projectDeploymentFacade.createProjectDeployment(projectDeployment, List.of(), List.of());
    }

    private Optional<ConnectedUserReferenceDeployment> fetchReferenceDeployment(long automationWorkflowProjectId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION);

        return connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUser.getId(), automationWorkflowProjectId);
    }

    private static Logger getRolloutManagerLogger() {
        return (Logger) LoggerFactory.getLogger(ConnectedUserReferenceRolloutManager.class);
    }

    private static ListAppender<ILoggingEvent> attachAppenderToRolloutManagerLogger() {
        Logger logger = getRolloutManagerLogger();

        ListAppender<ILoggingEvent> appender = new ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        return appender;
    }

    private static void detachAppenderFromRolloutManagerLogger(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(ConnectedUserReferenceRolloutManager.class);

        logger.detachAppender(appender);
    }

    private static boolean awaitQuietly(CountDownLatch latch, long timeoutSeconds) {
        try {
            return latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private int countAdvisoryLocksHeldByThisSession() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_locks WHERE locktype = 'advisory' AND granted AND pid = pg_backend_pid()",
                Integer.class));
    }

    private long createAutomationWorkflowProject(String name) {
        return automationWorkflowProjectFacade.createProject(name + " " + UUID.randomUUID(), "", null, List.of(), null);
    }

    private String createConnectedUser() {
        String externalUserId = "rollout-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        return externalUserId;
    }

    private String addWorkflow(long automationWorkflowProjectId, String definition) {
        return automationWorkflowProjectFacade.createProjectWorkflow(automationWorkflowProjectId, definition, null);
    }

    private String getWorkflowId(long automationWorkflowProjectId, int projectVersion, String workflowUuid) {
        return projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, projectVersion, workflowUuid)
            .map(ProjectWorkflow::getWorkflowId)
            .orElseThrow();
    }

    private void startRolloutThread(
        long automationWorkflowProjectId, CountDownLatch startLatch, CountDownLatch finishedLatch,
        AtomicReference<Throwable> rolloutFailure) {

        Thread rolloutThread = new Thread(() -> {
            try {
                if (!startLatch.await(ROLLOUT_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("The rollout was never released");
                }

                connectedUserReferenceRolloutManager.rollOutAndApply(
                    automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                rolloutFailure.compareAndSet(null, interruptedException);
            } catch (RuntimeException | AssertionError throwable) {
                rolloutFailure.compareAndSet(null, throwable);
            } finally {
                finishedLatch.countDown();
            }
        });

        rolloutThread.setDaemon(true);
        rolloutThread.start();
    }

    private void stubEntitledSlackConnection(long connectionId) {
        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(connectionId);

        ComponentConnection slot = new ComponentConnection("slack", 1, "postMessage1", "slack", true);

        when(connectionDTO.id()).thenReturn(connectionId);
        when(connectionDTO.componentName()).thenReturn("slack");
        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of(connectionDTO));
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class))).thenReturn(List.of(slot));
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("postMessage1"), eq("slack")))
            .thenReturn(slot);
        when(connectionService.getConnection(connectionId)).thenReturn(connection);
    }

    private record CoalescedRollout(
        boolean requestsReturnedWhileTheRolloutWaited, boolean rolloutFinishedWhileLocked, long passCount,
        Throwable rolloutFailure) {
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }

    private void markDangling(ConnectedUserProjectWorkflow reference) {
        ConnectedUserProjectWorkflow storedReference =
            connectedUserProjectWorkflowRepository.findById(reference.getId())
                .orElseThrow();

        storedReference.setDangling(true);
        storedReference.setDanglingReason("Removed from the automation workflow project on redeploy");
        storedReference.setEnabled(false);

        connectedUserProjectWorkflowRepository.save(storedReference);
    }

    private void updateProductionConfigurationVersion(long automationWorkflowProjectId, int projectVersion) {
        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(automationWorkflowProjectId, PRODUCTION_ENVIRONMENT_ID)
            .orElseThrow();

        automationWorkflowProjectConfigurationService.update(configuration.getId(), projectVersion, true, true);
    }

    private void publishAndPinProject(long projectId) {
        AutomationWorkflowProjectConfigurationTestSupport.publishAndPinProject(
            automationWorkflowProjectFacade, automationWorkflowProjectConfigurationService, projectId);
    }
}
