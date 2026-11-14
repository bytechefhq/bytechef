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
import static org.mockito.Mockito.doAnswer;
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
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRows;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationDeleteImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationProjectConfigurationUpdatedEvent;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.exception.ProvidedValueLockedException;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
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
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
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
        AutomationProjectFacadeIntTestConfiguration.class,
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
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpIntegrationInstanceWorkflowService.class, McpIntegrationInstanceRows.class,
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

    private static final String LABELED_SLACK_WORKFLOW_DEFINITION = """
        {"label":"%s","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}""";

    private static final String SLACK_TRIGGER_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[{"name":"newMessage1","type":"slack/v1/newMessage","parameters":{}}],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_TRIGGER_ONLY_WORKFLOW_DEFINITION = """
        {"label":"Watch","triggers":[{"name":"newMessage1","type":"slack/v1/newMessage","parameters":{}}],
         "tasks":[]}
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
    private AutomationProjectConfigurationAdminFacade automationProjectConfigurationAdminFacade;

    @Autowired
    private AutomationProjectConfigurationResolver automationProjectConfigurationResolver;

    @Autowired
    private AutomationProjectConfigurationService automationProjectConfigurationService;

    @Autowired
    private AutomationProjectFacade automationProjectFacade;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @MockitoBean
    private AutomationProjectConfigurationUpdatedEventListener automationProjectConfigurationUpdatedEventListener;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;

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
    void testUpdatingOneConfigurationLeavesUsersOnAnotherUntouched() {
        long automationProjectId = createAutomationProject("Two Configurations");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configurationA = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "A", null, 1, true, true);
        AutomationProjectConfiguration configurationB = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "B", null, 1, true, true);

        String firstExternalUserId = createConnectedUser();
        String secondExternalUserId = createConnectedUser();

        automationProjectConfigurationResolver.assign(
            connectedUserService.getConnectedUser(firstExternalUserId, Environment.PRODUCTION),
            automationProjectId, configurationA.getId());
        automationProjectConfigurationResolver.assign(
            connectedUserService.getConnectedUser(secondExternalUserId, Environment.PRODUCTION),
            automationProjectId, configurationB.getId());

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            firstExternalUserId, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow secondReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            secondExternalUserId, workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);

        automationProjectConfigurationService.update(
            configurationA.getId(), configurationA.getName(), configurationA.getPermissionExpression(), 2, true, true);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(firstReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentService.getProjectDeployment(secondReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testUnassignedDeploymentIsNotRolledOntoSelfServeConfiguration() {
        long automationProjectId = createAutomationProject("Unassigned Deployment");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration assignedConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Assigned", null, 1, true, true);

        String externalUserId = createConnectedUser();

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();

        connectedUserProjectConfigurationService.deleteByConfigurationId(assignedConfiguration.getId());

        automationProjectFacade.publishProject(automationProjectId);

        automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Newer", null, 2, true, true);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
    }

    @Test
    void testTwoTemplatesFromOneVisualProjectShareOneDeploymentAtThePublishedVersion() {
        long automationProjectId = createAutomationProject("Two Templates");

        String firstUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);
        publishAndPinProject(automationProjectId);

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
        long automationProjectId = createAutomationProject("Republish Enable");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, false, Environment.PRODUCTION);

        publishAndPinProject(automationProjectId);

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
        long automationProjectId = createAutomationProject("Delete Row");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.deleteReference(EXTERNAL_USER_ID, workflowUuid,
            Environment.PRODUCTION);

        assertThat(fetchReferenceDeployment(automationProjectId)).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isEmpty();
    }

    @Test
    void testChangingOneReferenceNeverStopsAnotherTemplatesRunningJobsOrTriggers() {
        long automationProjectId = createAutomationProject("Sibling Rows");

        String firstUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

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
    void testDeleteAutomationProjectRemovesTheReferenceDeploymentAndDanglesTheReference() {
        long automationProjectId = createAutomationProject("Delete Automation Workflow");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isTrue();

        long projectDeploymentId = reference.getProjectDeploymentId();
        String workflowId = projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);

        assertThatCode(() -> automationProjectFacade.deleteProject(automationProjectId))
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
        long automationProjectId = createAutomationProject("Atomic Provisioning");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenThrow(new IllegalStateException("Connection lookup failed"));

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(IllegalStateException.class);

        assertThat(connectedUserProjectWorkflowRepository.findAll())
            .noneMatch(reference -> workflowUuid.equals(reference.getAutomationUuid()));
        assertThat(fetchReferenceDeployment(automationProjectId)).isEmpty();
    }

    @Test
    void testEnableReferenceWithAMissingConnectionThrowsAndPausesTheRowButKeepsTheSwitchOn() {
        long automationProjectId = createAutomationProject("Missing Connection Enable");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

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
        long automationProjectId = createAutomationProject("Re-provision Missing Connection");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

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
        long automationProjectId = createAutomationProject("Save Pauses");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isEqualTo("MISSING_CONNECTION:slack");
    }

    @Test
    void testConfigurationSaveResumesAPausedWorkflowOnceItsConnectionIsAvailable() {
        long automationProjectId = createAutomationProject("Save Resumes");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();

        stubEntitledSlackConnection(778L);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isNull();
    }

    @Test
    void testInputsCanBeSetOnAReferenceAndAreListed() {
        long automationProjectId = createAutomationProject("Inputs");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        long environmentId = Environment.PRODUCTION.ordinal();

        when(environmentService.getEnvironment(environmentId)).thenReturn(Environment.PRODUCTION);

        connectedUserProjectFacade.updateProjectWorkflowInputs(
            EXTERNAL_USER_ID, workflowUuid, Map.of("channel", "#alerts"), environmentId);

        assertThat(
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(EXTERNAL_USER_ID, Environment.PRODUCTION))
                .filteredOn(workflow -> Objects.equals(workflow.automationUuid(), workflowUuid))
                .singleElement()
                .extracting(ConnectedUserProjectWorkflowDTO::inputValues)
                .isEqualTo(Map.of("channel", "#alerts"));
    }

    @Test
    void testProvisionWithAMissingRequiredInputSucceedsDisabledAndEnableRefusesUntilItIsSet() {
        long automationProjectId = createAutomationProject("Required Input");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

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
        long automationProjectId = createAutomationProject("Enabled Required Input");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

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
        long automationProjectId = createAutomationProject("Rollout");

        String firstUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);
        String secondUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        connectedUserReferenceDeploymentManager.updateInputs(
            first.getProjectDeploymentId(), firstUuid, Map.of("channel", "#alerts"));

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, firstUuid, true, Environment.PRODUCTION);

        publishAndPinProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

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
        long automationProjectId = createAutomationProject("Publish Keeps Pin");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testUpdatingTheConfigurationVersionRollsReferencesForward() {
        long automationProjectId = createAutomationProject("Pin Forward");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);

        updateProductionConfigurationVersion(automationProjectId, 2);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testRollOutMovesReferencesToAnOlderPinnedVersion() {
        long automationProjectId = createAutomationProject("Pin Back");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);
        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);

        updateProductionConfigurationVersion(automationProjectId, 1);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testRollOutToAVersionLackingATemplateDanglesItsReferencesWithTheReleasedVersionReason() {
        long automationProjectId = createAutomationProject("Roll Back Without Template");

        String keptUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        String addedUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow keptReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, keptUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow addedReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, addedUuid, Environment.PRODUCTION);

        updateProductionConfigurationVersion(automationProjectId, 1);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

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
        long automationProjectId = createAutomationProject("Pin Per Environment");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        automationProjectConfigurationService.create(automationProjectId,
            Environment.DEVELOPMENT.ordinal(), "Default", null, 1, true, true);

        String developmentExternalUserId = "rollout-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(developmentExternalUserId, Environment.DEVELOPMENT);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow productionReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow developmentReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            developmentExternalUserId, workflowUuid, Environment.DEVELOPMENT);

        automationProjectFacade.publishProject(automationProjectId);

        updateProductionConfigurationVersion(automationProjectId, 2);

        AutomationProjectConfiguration developmentConfiguration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, Environment.DEVELOPMENT.ordinal())
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.update(developmentConfiguration.getId(),
            developmentConfiguration.getName(), developmentConfiguration.getPermissionExpression(), 2, true, true);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(productionReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentService.getProjectDeployment(developmentReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testConcurrentRolloutsForTheSamePairCompleteWithoutRolloutFailures() throws InterruptedException {
        long automationProjectId = createAutomationProject("Concurrent Rollouts");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow secondReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            createConnectedUser(), workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);
        automationProjectFacade.publishProject(automationProjectId);

        updateProductionConfigurationVersion(automationProjectId, 2);
        updateProductionConfigurationVersion(automationProjectId, 3);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishedLatch = new CountDownLatch(2);
        AtomicReference<Throwable> rolloutFailure = new AtomicReference<>();

        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutManagerLogger();

        try {
            for (int index = 0; index < 2; index++) {
                startRolloutThread(automationProjectId, startLatch, finishedLatch, rolloutFailure);
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
                .isEqualTo(getWorkflowId(automationProjectId, 3, workflowUuid));
    }

    @Test
    void testRolloutWaitsForTheRolloutLockWhileAdministratorSavesDoNot() throws InterruptedException {
        long automationProjectId = createAutomationProject("Rollout Waits On Lock");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);
        automationProjectFacade.publishProject(automationProjectId);

        updateProductionConfigurationVersion(automationProjectId, 2);

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, PRODUCTION_ENVIRONMENT_ID)
            .stream()
            .findFirst()
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
            automationProjectConfigurationService.lockRollout(
                automationProjectId, PRODUCTION_ENVIRONMENT_ID);

            heldAdvisoryLockCount.set(countAdvisoryLocksHeldByThisSession());

            startRolloutThread(automationProjectId, startLatch, finishedLatch, rolloutFailure);

            startLatch.countDown();

            rolloutFinishedWhileLocked.set(awaitQuietly(finishedLatch, ROLLOUT_LOCK_WAIT_SECONDS));

            Thread saveThread = new Thread(() -> {
                try {
                    automationProjectConfigurationAdminFacade.updateConfiguration(
                        configuration.getId(), configuration.getName(), configuration.getPermissionExpression(), 3,
                        true, true);
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
        long lockedAutomationProjectId = createAutomationProject("Locked Pair");

        addWorkflow(lockedAutomationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(lockedAutomationProjectId);

        long automationProjectId = createAutomationProject("Free Pair");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);

        updateProductionConfigurationVersion(automationProjectId, 2);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishedLatch = new CountDownLatch(1);
        AtomicInteger heldAdvisoryLockCount = new AtomicInteger();
        AtomicBoolean finishedWhileTheOtherPairWasLocked = new AtomicBoolean();
        AtomicReference<Throwable> rolloutFailure = new AtomicReference<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            automationProjectConfigurationService.lockRollout(
                lockedAutomationProjectId, PRODUCTION_ENVIRONMENT_ID);

            heldAdvisoryLockCount.set(countAdvisoryLocksHeldByThisSession());

            startRolloutThread(automationProjectId, startLatch, finishedLatch, rolloutFailure);

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
        long automationProjectId = createAutomationProject("Rollout Missing");

        String slackUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String otherUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

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

        publishAndPinProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

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
        long automationProjectId = createAutomationProject("Rollout Adds Input");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();

        publishWithRequiredChannelInput(automationProjectId, workflowUuid);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isEqualTo("INPUT_REQUIRED:channel");
    }

    @Test
    void testUserDisabledWorkflowStaysDisabledAfterRolloutAndInputs() {
        long automationProjectId = createAutomationProject("Rollout User Off");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, false, Environment.PRODUCTION);

        publishWithRequiredChannelInput(automationProjectId, workflowUuid);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isFalse();

        connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            EXTERNAL_USER_ID, workflowUuid, Map.of("channel", "#alerts"), Environment.PRODUCTION);

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#alerts"));
        assertThat(getStoredReference(reference).isEnabled()).isFalse();
    }

    @Test
    void testRepublishWithoutATemplateMarksItsReferenceDangling() {
        long automationProjectId = createAutomationProject("Rollout Removed");

        String keptUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String removedUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow keptReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, keptUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow removedReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, removedUuid, Environment.PRODUCTION);

        automationProjectFacade.deleteProjectWorkflow(removedUuid);
        publishAndPinProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

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
        long automationProjectId = createAutomationProject("Rollout All Removed");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        String onlyReferencedUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, onlyReferencedUuid, Environment.PRODUCTION);

        automationProjectFacade.deleteProjectWorkflow(onlyReferencedUuid);
        publishAndPinProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isDangling)
            .isEqualTo(true);
        assertThat(fetchReferenceDeployment(automationProjectId)).isEmpty();
    }

    @Test
    void testEnableCatchesUpADeploymentLeftBehind() {
        long automationProjectId = createAutomationProject("Rollout Lazy");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testDeleteProjectWorkflowRemovesTheTemplateFromTheDraftByItsUuid() {
        long automationProjectId = createAutomationProject("Delete By Uuid");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        String removedUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        automationProjectFacade.deleteProjectWorkflow(removedUuid);

        publishAndPinProject(automationProjectId);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationProjectId, 1, removedUuid))
            .isPresent();
        assertThat(projectWorkflowService.fetchProjectWorkflow(automationProjectId, 2, removedUuid)).isEmpty();
    }

    @Test
    void testDeletingATemplateTwiceNeverTouchesThePublishedVersion() {
        long automationProjectId = createAutomationProject("Delete Twice");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        String publishedWorkflowId = getWorkflowId(automationProjectId, 1, workflowUuid);

        automationProjectFacade.deleteProjectWorkflow(workflowUuid);
        automationProjectFacade.deleteProjectWorkflow(workflowUuid);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationProjectId, 1, workflowUuid))
            .isPresent();
        assertThat(connectedUserReferenceDeploymentManager.fetchWorkflowRow(
            reference.getProjectDeploymentId(), publishedWorkflowId))
                .get()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(true);
    }

    @Test
    void testProvisioningATemplateNewerThanTheDeploymentCatchesTheDeploymentUp() {
        long automationProjectId = createAutomationProject("Rollout Newer Template");

        String firstUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        String secondUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow second = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        long projectDeploymentId = first.getProjectDeploymentId();

        assertThat(second.getProjectDeploymentId()).isEqualTo(projectDeploymentId);
        assertThat(projectDeploymentService.getProjectDeployment(projectDeploymentId)
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId))
            .extracting(ProjectDeploymentWorkflow::getWorkflowId)
            .containsExactlyInAnyOrder(
                getWorkflowId(automationProjectId, 2, firstUuid),
                getWorkflowId(automationProjectId, 2, secondUuid));
    }

    @Test
    void testRollOutContinuesWithTheNextDeploymentWhenOneFails() {
        long automationProjectId = createAutomationProject("Rollout Isolated Failure");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        String failingExternalUserId = createConnectedUser();
        String otherExternalUserId = createConnectedUser();

        ConnectedUserProjectWorkflow failingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            failingExternalUserId, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow otherReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            otherExternalUserId, workflowUuid, Environment.PRODUCTION);

        assertThat(otherReference.getProjectDeploymentId()).isNotEqualTo(failingReference.getProjectDeploymentId());

        publishAndPinProject(automationProjectId);

        ConnectedUser failingConnectedUser = connectedUserService.getConnectedUser(
            failingExternalUserId, Environment.PRODUCTION);

        when(connectedUserConnectionFacade.getConnections(failingConnectedUser.getId(), "slack", List.of()))
            .thenThrow(new IllegalStateException("Connection lookup failed"));

        assertThatCode(
            () -> connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID))
                .doesNotThrowAnyException();

        assertThat(projectDeploymentService.getProjectDeployment(failingReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            failingReference.getProjectDeploymentId()))
                .singleElement()
                .extracting(ProjectDeploymentWorkflow::getWorkflowId)
                .isEqualTo(getWorkflowId(automationProjectId, 1, workflowUuid));
        assertThat(projectDeploymentService.getProjectDeployment(otherReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            otherReference.getProjectDeploymentId()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getWorkflowId())
                        .isEqualTo(getWorkflowId(automationProjectId, 2, workflowUuid));
                    assertThat(row.isEnabled()).isTrue();
                });
    }

    @Test
    void testRollOutLogsAnOptimisticLockingFailureAtWarnAndOtherFailuresAtError() {
        long automationProjectId = createAutomationProject("Rollout Failure Log Levels");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        String concurrentExternalUserId = createConnectedUser();
        String failingExternalUserId = createConnectedUser();

        ConnectedUserProjectWorkflow concurrentReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            concurrentExternalUserId, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow failingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            failingExternalUserId, workflowUuid, Environment.PRODUCTION);

        publishAndPinProject(automationProjectId);

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
            connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

            assertThat(appender.list)
                .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
                .containsExactlyInAnyOrder(
                    tuple(
                        Level.WARN,
                        ("Rolling out automation project id=%d to deployment id=%d lost a concurrent update; "
                            +
                            "it will converge on the next publish or enable")
                                .formatted(automationProjectId, concurrentReference.getProjectDeploymentId())),
                    tuple(
                        Level.ERROR,
                        "Rolling out automation project id=%d to deployment id=%d failed"
                            .formatted(automationProjectId, failingReference.getProjectDeploymentId())));
        } finally {
            detachAppenderFromRolloutManagerLogger(appender);
        }
    }

    @Test
    void testRollOutDeletesADeploymentWhoseOnlyReferenceIsDangling() {
        long automationProjectId = createAutomationProject("Rollout Dangling Only");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        markDangling(reference);

        publishAndPinProject(automationProjectId);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationProjectId, 2, workflowUuid))
            .isPresent();

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

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
        long automationProjectId = createAutomationProject("Rollout Unpublished");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutManagerLogger();

        try {
            assertThatCode(() -> connectedUserReferenceRolloutManager.rollOut(automationProjectId,
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
        long automationProjectId = createAutomationProject("Rollout Unknown Environment");

        assertThatCode(() -> connectedUserReferenceRolloutManager.rollOut(automationProjectId, 99L))
            .doesNotThrowAnyException();
    }

    @Test
    void testEnableReferenceWhileTheConfigurationIsOffDoesNotThrow() {
        long automationProjectId = createAutomationProject("Rollout Configuration Off");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        setProductionConfigurationEnabled(automationProjectId, false);

        automationProjectFacade.publishProject(automationProjectId);

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
        long automationProjectId = createAutomationProject("Apply Keeps User Off");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, false, Environment.PRODUCTION);

        setProductionConfigurationEnabled(automationProjectId, false);

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();

        setProductionConfigurationEnabled(automationProjectId, true);

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())
            .orElseThrow()
            .isEnabled()).isFalse();
    }

    @Test
    void testDisablingAConfigurationStopsEveryReferenceInThatEnvironment() {
        long automationProjectId = createAutomationProject("Apply Disables All");

        String firstUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

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

        setProductionConfigurationEnabled(automationProjectId, false);

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        for (ConnectedUserProjectWorkflow reference : references) {
            assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
            assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())
                .orElseThrow()
                .isEnabled()).isTrue();
        }
    }

    @Test
    void testDisablingOneWorkflowStopsOnlyThatTemplatesReferences() {
        long automationProjectId = createAutomationProject("Apply Disables One");

        String firstUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow second = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, PRODUCTION_ENVIRONMENT_ID)
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.updateWorkflowEnabled(
            configuration.getId(), UUID.fromString(firstUuid), false);

        connectedUserReferenceRolloutManager.applyConfigurationState(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(first).isEnabled()).isFalse();
        assertThat(fetchDeploymentRow(second).isEnabled()).isTrue();
    }

    @Test
    void testDeleteConfigurationDeletesAssignedUsersReferencesAndKeepsCopies() {
        DeleteFixture fixture = createDeleteFixture("Destructive Delete");

        automationProjectConfigurationAdminFacade.deleteConfiguration(fixture.configurationA()
            .getId());

        long projectId = fixture.automationProjectId();

        assertThat(connectedUserProjectWorkflowRepository.findById(fixture.firstReference()
            .getId())).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(fixture.firstReference()
            .getProjectDeploymentId())).isEmpty();
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            fixture.firstConnectedUserId(), projectId)).isEmpty();
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(fixture.firstConnectedUserId(), projectId))
            .isEmpty();

        assertThat(connectedUserProjectWorkflowRepository.findById(fixture.copy()
            .getId()))
                .get()
                .satisfies(copy -> {
                    assertThat(copy.isEnabled()).isTrue();
                    assertThat(copy.getCopiedFromWorkflowUuid()).isEqualTo(fixture.workflowUuid());
                });
        assertThat(projectWorkflowService.getProjectWorkflow(fixture.copy()
            .getProjectWorkflowId())).isNotNull();
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(fixture.secondConnectedUserId(), projectId))
            .isEmpty();

        assertThat(connectedUserProjectWorkflowRepository.findById(fixture.thirdReference()
            .getId())).isPresent();
        assertThat(fetchDeploymentRow(fixture.thirdReference()).isEnabled()).isTrue();
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(fixture.thirdConnectedUserId(), projectId))
            .get()
            .extracting(ConnectedUserProjectConfiguration::getConfigurationId)
            .isEqualTo(fixture.configurationB()
                .getId());
        assertThat(automationProjectConfigurationService.getConfigurations(
            projectId, PRODUCTION_ENVIRONMENT_ID))
                .extracting(AutomationProjectConfiguration::getId)
                .containsExactly(fixture.configurationB()
                    .getId());
    }

    @Test
    void testDeleteKeepsReferencesOfUserMovedAwayAfterTheAssignmentsWereRead() {
        DeleteFixture fixture = createDeleteFixture("Delete Moved Away");

        long projectId = fixture.automationProjectId();
        long configurationAId = fixture.configurationA()
            .getId();

        List<Long> staleConnectedUserIds = connectedUserProjectConfigurationService.getAssignments(configurationAId)
            .stream()
            .map(ConnectedUserProjectConfiguration::getConnectedUserId)
            .toList();

        assertThat(staleConnectedUserIds).contains(fixture.firstConnectedUserId());

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationAdminFacade.moveConnectedUsers(
            configurationAId, fixture.configurationB()
                .getId(),
            List.of(fixture.firstConnectedUserId()));

        assertThat(moveResult.movedConnectedUserIds()).containsExactly(fixture.firstConnectedUserId());

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(
            status -> connectedUserReferenceRolloutManager.deleteReferenceDeployments(
                projectId, configurationAId, staleConnectedUserIds));

        assertThat(connectedUserProjectWorkflowRepository.findById(fixture.firstReference()
            .getId())).isPresent();
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            fixture.firstConnectedUserId(), projectId)).isPresent();
        assertThat(fetchDeploymentRow(fixture.firstReference()).isEnabled()).isTrue();
        assertThat(getAssignedConfigurationId(fixture.firstConnectedUserId(), projectId))
            .isEqualTo(fixture.configurationB()
                .getId());
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(fixture.secondConnectedUserId(), projectId))
            .isEmpty();
    }

    @Test
    void testSelfServePickedBeforeADeleteDisablesTheConfigurationLandsOnAnother() {
        long automationProjectId = createAutomationProject("Delete Self-Serve Race");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration remainingConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Remaining", null, 1, true, true);
        AutomationProjectConfiguration deletedConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Deleted", null, 1, true, true);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            createConnectedUser(), Environment.PRODUCTION);

        TransactionTemplate requiresNewTransactionTemplate = new TransactionTemplate(platformTransactionManager);

        requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        AtomicBoolean disableOnFirstEvaluation = new AtomicBoolean(true);

        doAnswer(invocation -> {
            if (disableOnFirstEvaluation.getAndSet(false)) {
                requiresNewTransactionTemplate.executeWithoutResult(
                    status -> automationProjectConfigurationService.updateEnabled(
                        deletedConfiguration.getId(), false));
            }

            return true;
        }).when(embeddedPermissionEvaluator)
            .evaluate(any(), any());

        assertThat(automationProjectConfigurationResolver.resolveAndAssign(
            connectedUser, automationProjectId))
                .get()
                .extracting(AutomationProjectConfiguration::getId)
                .isEqualTo(remainingConfiguration.getId());

        automationProjectConfigurationAdminFacade.deleteConfiguration(deletedConfiguration.getId());

        assertThat(getAssignedConfigurationId(connectedUser.getId(), automationProjectId))
            .isEqualTo(remainingConfiguration.getId());
        assertThat(automationProjectConfigurationService.getConfigurations(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID))
                .extracting(AutomationProjectConfiguration::getId)
                .containsExactly(remainingConfiguration.getId());
    }

    @Test
    void testGetDeleteImpact() {
        DeleteFixture fixture = createDeleteFixture("Delete Impact");

        assertThat(automationProjectConfigurationAdminFacade.getDeleteImpact(fixture.configurationA()
            .getId())).isEqualTo(new AutomationProjectConfigurationDeleteImpactDTO(2, 1));
        assertThat(automationProjectConfigurationAdminFacade.getDeleteImpact(fixture.configurationB()
            .getId())).isEqualTo(new AutomationProjectConfigurationDeleteImpactDTO(1, 1));
    }

    @Test
    void testVersionImpactCountsOnlyAssignedUsers() {
        long automationProjectId = createAutomationProject("Version Impact Assigned Users");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configurationA = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "A", null, 1, true, true);
        AutomationProjectConfiguration configurationB = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "B", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        createAssignedReference(automationProjectId, workflowUuid, configurationA);
        createAssignedReference(automationProjectId, workflowUuid, configurationB);

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        automationProjectFacade.publishProject(automationProjectId);

        assertThat(automationProjectConfigurationAdminFacade.getVersionImpact(configurationA.getId(), 2)
            .workflows())
                .singleElement()
                .satisfies(workflow -> {
                    assertThat(workflow.newRequiredInputs()).containsExactly("channel");
                    assertThat(workflow.affectedConnectedUserCount()).isEqualTo(1);
                });
    }

    @Test
    void testRemovingProvidedInputOnOneConfigurationKeepsItOnAnother() {
        long automationProjectId = createAutomationProject("Provided Input Per Configuration");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configurationA = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "A", null, 1, true, true);
        AutomationProjectConfiguration configurationB = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "B", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            configurationA.getId(), workflowUuid, Map.of(), Map.of("channel", "#administrator"));

        String providedExternalUserId = createConnectedUser();
        String ownValueExternalUserId = createConnectedUser();

        automationProjectConfigurationResolver.assign(
            connectedUserService.getConnectedUser(providedExternalUserId, Environment.PRODUCTION),
            automationProjectId, configurationA.getId());
        automationProjectConfigurationResolver.assign(
            connectedUserService.getConnectedUser(ownValueExternalUserId, Environment.PRODUCTION),
            automationProjectId, configurationB.getId());

        ConnectedUserProjectWorkflow providedReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            providedExternalUserId, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow ownValueReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            ownValueExternalUserId, workflowUuid, Environment.PRODUCTION, Map.of(), Map.of("channel", "#mine"));

        assertThat(fetchDeploymentRow(providedReference).getInputs()).isEqualTo(Map.of("channel", "#administrator"));
        assertThat(fetchDeploymentRow(ownValueReference).getInputs()).isEqualTo(Map.of("channel", "#mine"));

        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            configurationA.getId(), workflowUuid, Map.of(), Map.of());

        rollOutTheLastUpdatedEvent();

        assertThat(fetchDeploymentRow(providedReference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isFalse();
                assertThat(row.getInputs()).isEmpty();
            });
        assertThat(fetchDeploymentRow(ownValueReference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#mine"));
            });
    }

    @Test
    void testMoveRollsUsersToTargetVersion() {
        long automationProjectId = createAutomationProject("Move Rolls Forward");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser firstConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);
        ConnectedUser secondConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            firstConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow secondReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            secondConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        ConnectedUser targetConnectedUser = assignNewConnectedUser(automationProjectId, targetConfiguration);

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationAdminFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(),
            List.of(firstConnectedUser.getId(), secondConnectedUser.getId(), targetConnectedUser.getId()));

        assertThat(moveResult.movedConnectedUserIds()).containsExactly(
            firstConnectedUser.getId(), secondConnectedUser.getId());
        assertThat(moveResult.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                targetConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));
        assertThat(moveResult.needsAttentionConnectedUserIds()).isEmpty();

        for (ConnectedUserProjectWorkflow reference : List.of(firstReference, secondReference)) {
            assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
                .getProjectVersion()).isEqualTo(2);
            assertThat(fetchDeploymentRow(reference))
                .satisfies(row -> {
                    assertThat(row.getWorkflowId())
                        .isEqualTo(getWorkflowId(automationProjectId, 2, workflowUuid));
                    assertThat(row.isEnabled()).isTrue();
                });
        }

        assertThat(getAssignedConfigurationId(firstConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
        assertThat(getAssignedConfigurationId(secondConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
    }

    @Test
    void testMoveSkipsUserWhoseReferencedWorkflowIsMissingFromTarget() {
        long automationProjectId = createAutomationProject("Move Lacks Workflow");

        String firstWorkflowUuid = addWorkflow(
            automationProjectId, LABELED_SLACK_WORKFLOW_DEFINITION.formatted("Send Digest"));
        String secondWorkflowUuid = addWorkflow(
            automationProjectId, LABELED_SLACK_WORKFLOW_DEFINITION.formatted("Fetch Recent Emails"));

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser firstConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);
        ConnectedUser secondConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            firstConnectedUser.getExternalId(), secondWorkflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow secondReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            secondConnectedUser.getExternalId(), firstWorkflowUuid, Environment.PRODUCTION);

        automationProjectFacade.deleteProjectWorkflow(secondWorkflowUuid);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationAdminFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(),
            List.of(firstConnectedUser.getId(), secondConnectedUser.getId()));

        assertThat(moveResult.movedConnectedUserIds()).containsExactly(secondConnectedUser.getId());
        assertThat(moveResult.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                firstConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_REFERENCED_WORKFLOWS,
                List.of("Fetch Recent Emails")));

        assertThat(getAssignedConfigurationId(firstConnectedUser.getId(), automationProjectId))
            .isEqualTo(sourceConfiguration.getId());
        assertThat(getStoredReference(firstReference).isDangling()).isFalse();
        assertThat(projectDeploymentService.getProjectDeployment(firstReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);
        assertThat(fetchDeploymentRow(firstReference).isEnabled()).isTrue();

        assertThat(getAssignedConfigurationId(secondConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
        assertThat(projectDeploymentService.getProjectDeployment(secondReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testMoveOntoDisabledConfigurationTurnsReferencesOff() {
        long automationProjectId = createAutomationProject("Move Onto Disabled");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);
        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 1, false, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser connectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationAdminFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(), List.of(connectedUser.getId()));

        assertThat(moveResult.movedConnectedUserIds()).containsExactly(connectedUser.getId());
        assertThat(moveResult.skipped()).isEmpty();
        assertThat(moveResult.needsAttentionConnectedUserIds()).isEmpty();

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(getAssignedConfigurationId(connectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
    }

    @Test
    void testMoveAppliesTheTargetsProvidedInputs() {
        long automationProjectId = createAutomationProject("Move Target Provided Inputs");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);
        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            sourceConfiguration.getId(), workflowUuid, Map.of(), Map.of("channel", "#source"));
        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            targetConfiguration.getId(), workflowUuid, Map.of(), Map.of("channel", "#target"));

        ConnectedUser connectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#source"));

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationAdminFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(), List.of(connectedUser.getId()));

        assertThat(moveResult.movedConnectedUserIds()).containsExactly(connectedUser.getId());
        assertThat(moveResult.needsAttentionConnectedUserIds()).isEmpty();
        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#target"));
            });
    }

    @Test
    void testMoveImpactListsAnInputOnlyTheSourceProvides() {
        long automationProjectId = createAutomationProject("Move Impact Source Provided Input");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);
        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            sourceConfiguration.getId(), workflowUuid, Map.of(), Map.of("channel", "#source"));

        ConnectedUser firstConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);
        ConnectedUser secondConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            firstConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        ConfigurationMoveImpactDTO moveImpact = automationProjectConfigurationAdminFacade.getMoveImpact(
            sourceConfiguration.getId(), targetConfiguration.getId(),
            List.of(firstConnectedUser.getId(), secondConnectedUser.getId()));

        assertThat(moveImpact.skipped()).isEmpty();
        assertThat(moveImpact.workflows())
            .singleElement()
            .satisfies(workflow -> {
                assertThat(workflow.workflowUuid()).isEqualTo(workflowUuid);
                assertThat(workflow.label()).isEqualTo("Post");
                assertThat(workflow.newRequiredInputs()).containsExactly("channel");
                assertThat(workflow.newRequiredConnections()).isEmpty();
                assertThat(workflow.affectedConnectedUserCount()).isEqualTo(1);
            });
    }

    @Test
    void testMoveTakesOffAnInputOnlyTheSourceProvidesAndReportsAttention() {
        long automationProjectId = createAutomationProject("Move Source Provided Input");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);
        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            sourceConfiguration.getId(), workflowUuid, Map.of(), Map.of("channel", "#source"));

        ConnectedUser connectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationAdminFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(), List.of(connectedUser.getId()));

        assertThat(moveResult.movedConnectedUserIds()).containsExactly(connectedUser.getId());
        assertThat(moveResult.needsAttentionConnectedUserIds()).containsExactly(connectedUser.getId());
        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isFalse();
                assertThat(row.getInputs()).isEmpty();
            });
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
    }

    @Test
    void testMoveLeavesCopiesUntouched() {
        long automationProjectId = createAutomationProject("Move Keeps Copies");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);
        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 1, false, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser connectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        connectedUserProjectFacade.copyWorkflowTemplate(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow copy = connectedUserProjectWorkflowRepository
            .findAllByCopiedFromWorkflowUuid(workflowUuid)
            .getFirst();

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationAdminFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(), List.of(connectedUser.getId()));

        assertThat(moveResult.movedConnectedUserIds()).containsExactly(connectedUser.getId());
        assertThat(moveResult.needsAttentionConnectedUserIds()).isEmpty();
        assertThat(getAssignedConfigurationId(connectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
        assertThat(connectedUserProjectWorkflowRepository.findById(copy.getId()))
            .get()
            .satisfies(storedCopy -> {
                assertThat(storedCopy.isEnabled()).isEqualTo(copy.isEnabled());
                assertThat(storedCopy.getProjectWorkflowId()).isEqualTo(copy.getProjectWorkflowId());
                assertThat(storedCopy.getVersion()).isEqualTo(copy.getVersion());
            });
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUser.getId(), automationProjectId)).isEmpty();
    }

    @Test
    void testMoveToAnotherProjectThrows() {
        long automationProjectId = createAutomationProject("Move Source Project");
        long otherAutomationProjectId = createAutomationProject("Move Other Project");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        addWorkflow(otherAutomationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);
        automationProjectFacade.publishProject(otherAutomationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);
        AutomationProjectConfiguration otherProjectConfiguration =
            automationProjectConfigurationService.create(
                otherAutomationProjectId, PRODUCTION_ENVIRONMENT_ID, "Other", null, 1, true, true);

        ConnectedUser connectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        List<Long> connectedUserIds = List.of(connectedUser.getId());

        assertThatThrownBy(
            () -> automationProjectConfigurationAdminFacade.moveConnectedUsers(
                sourceConfiguration.getId(), otherProjectConfiguration.getId(), connectedUserIds))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(
                                AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH
                                    .getErrorKey()));
        assertThatThrownBy(
            () -> automationProjectConfigurationAdminFacade.getMoveImpact(
                sourceConfiguration.getId(), otherProjectConfiguration.getId(), connectedUserIds))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(
                                AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH
                                    .getErrorKey()));
        assertThat(getAssignedConfigurationId(connectedUser.getId(), automationProjectId))
            .isEqualTo(sourceConfiguration.getId());
    }

    @Test
    void testMoveFailureOfOneUserLeavesTheOthersMoved() {
        long automationProjectId = createAutomationProject("Move Isolated Failure");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser failingConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);
        ConnectedUser otherConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow failingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            failingConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow otherReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            otherConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        when(connectedUserConnectionFacade.getConnections(failingConnectedUser.getId(), "slack", List.of()))
            .thenThrow(new IllegalStateException("Connection lookup failed"));

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationAdminFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(),
            List.of(failingConnectedUser.getId(), otherConnectedUser.getId()));

        assertThat(moveResult.movedConnectedUserIds()).containsExactly(otherConnectedUser.getId());
        assertThat(moveResult.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                failingConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.MOVE_FAILED, List.of()));

        assertThat(getAssignedConfigurationId(failingConnectedUser.getId(), automationProjectId))
            .isEqualTo(sourceConfiguration.getId());
        assertThat(projectDeploymentService.getProjectDeployment(failingReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);

        assertThat(getAssignedConfigurationId(otherConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
        assertThat(projectDeploymentService.getProjectDeployment(otherReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testGetMoveImpactCountsOnlyTheGivenUsers() {
        long automationProjectId = createAutomationProject("Move Impact");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser movedConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);
        ConnectedUser stayingConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            movedConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);
        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            stayingConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        assertThat(automationProjectConfigurationAdminFacade.getMoveImpact(
            sourceConfiguration.getId(), targetConfiguration.getId(), List.of(movedConnectedUser.getId()))
            .workflows())
                .singleElement()
                .satisfies(workflow -> {
                    assertThat(workflow.newRequiredInputs()).containsExactly("channel");
                    assertThat(workflow.affectedConnectedUserCount()).isEqualTo(1);
                });
    }

    @Test
    void testMoveImpactListsUsersThatWouldBeSkipped() {
        long automationProjectId = createAutomationProject("Move Impact Skipped");

        String firstWorkflowUuid = addWorkflow(
            automationProjectId, LABELED_SLACK_WORKFLOW_DEFINITION.formatted("Send Digest"));
        String secondWorkflowUuid = addWorkflow(
            automationProjectId, LABELED_SLACK_WORKFLOW_DEFINITION.formatted("Fetch Recent Emails"));

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);
        AutomationProjectConfiguration otherConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Other", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser firstConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);
        ConnectedUser secondConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);
        ConnectedUser thirdConnectedUser = assignNewConnectedUser(automationProjectId, otherConfiguration);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            firstConnectedUser.getExternalId(), secondWorkflowUuid, Environment.PRODUCTION);
        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            secondConnectedUser.getExternalId(), firstWorkflowUuid, Environment.PRODUCTION);

        automationProjectFacade.deleteProjectWorkflow(secondWorkflowUuid);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        ConfigurationMoveImpactDTO moveImpact = automationProjectConfigurationAdminFacade.getMoveImpact(
            sourceConfiguration.getId(), targetConfiguration.getId(),
            List.of(firstConnectedUser.getId(), secondConnectedUser.getId(), thirdConnectedUser.getId()));

        assertThat(moveImpact.skipped()).containsExactlyInAnyOrder(
            new ConfigurationMoveResultDTO.Skipped(
                firstConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_REFERENCED_WORKFLOWS,
                List.of("Fetch Recent Emails")),
            new ConfigurationMoveResultDTO.Skipped(
                thirdConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));
        assertThat(moveImpact.skipped())
            .extracting(ConfigurationMoveResultDTO.Skipped::connectedUserId)
            .doesNotContain(secondConnectedUser.getId());
        assertThat(getAssignedConfigurationId(firstConnectedUser.getId(), automationProjectId))
            .isEqualTo(sourceConfiguration.getId());
    }

    @Test
    void testUpgradeConnectedUsersMovesUsersFromSeveralConfigurations() {
        long automationProjectId = createAutomationProject("Upgrade Several Sources");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration firstSourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "First Source", null, 1, true, true);
        AutomationProjectConfiguration secondSourceConfiguration =
            automationProjectConfigurationService.create(
                automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Second Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser firstConnectedUser = assignNewConnectedUser(
            automationProjectId, firstSourceConfiguration);
        ConnectedUser secondConnectedUser = assignNewConnectedUser(
            automationProjectId, secondSourceConfiguration);

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            firstConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow secondReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            secondConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        ConfigurationMoveResultDTO upgradeResult =
            automationProjectConfigurationAdminFacade.upgradeConnectedUsers(
                targetConfiguration.getId(), List.of(secondConnectedUser.getId(), firstConnectedUser.getId()));

        assertThat(upgradeResult.movedConnectedUserIds()).containsExactly(
            firstConnectedUser.getId(), secondConnectedUser.getId());
        assertThat(upgradeResult.skipped()).isEmpty();
        assertThat(upgradeResult.needsAttentionConnectedUserIds()).isEmpty();

        for (ConnectedUserProjectWorkflow reference : List.of(firstReference, secondReference)) {
            assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
                .getProjectVersion()).isEqualTo(2);
            assertThat(fetchDeploymentRow(reference).getWorkflowId())
                .isEqualTo(getWorkflowId(automationProjectId, 2, workflowUuid));
        }

        assertThat(getAssignedConfigurationId(firstConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
        assertThat(getAssignedConfigurationId(secondConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
    }

    @Test
    void testUpgradeConnectedUsersSkipsUsersAlreadyOnTargetOrWithoutTheProject() {
        long automationProjectId = createAutomationProject("Upgrade Skips Target Users");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);
        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 1, true, true);

        ConnectedUser sourceConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);
        ConnectedUser targetConnectedUser = assignNewConnectedUser(automationProjectId, targetConfiguration);
        ConnectedUser unassignedConnectedUser = connectedUserService.getConnectedUser(
            createConnectedUser(), Environment.PRODUCTION);

        ConfigurationMoveResultDTO upgradeResult =
            automationProjectConfigurationAdminFacade.upgradeConnectedUsers(
                targetConfiguration.getId(),
                List.of(
                    sourceConnectedUser.getId(), targetConnectedUser.getId(), unassignedConnectedUser.getId(),
                    sourceConnectedUser.getId()));

        assertThat(upgradeResult.movedConnectedUserIds()).containsExactly(sourceConnectedUser.getId());
        assertThat(upgradeResult.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                targetConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.ALREADY_ON_TARGET, List.of()),
            new ConfigurationMoveResultDTO.Skipped(
                unassignedConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));
        assertThat(upgradeResult.needsAttentionConnectedUserIds()).isEmpty();

        assertThat(getAssignedConfigurationId(sourceConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
        assertThat(getAssignedConfigurationId(targetConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(
            unassignedConnectedUser.getId(), automationProjectId)).isEmpty();
    }

    @Test
    void testUpgradeConnectedUsersIgnoresOtherEnvironmentsAndProjects() {
        long automationProjectId = createAutomationProject("Upgrade Environment Scope");
        long otherAutomationProjectId = createAutomationProject("Upgrade Other Project");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        addWorkflow(otherAutomationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);
        automationProjectFacade.publishProject(otherAutomationProjectId);

        AutomationProjectConfiguration stagingConfiguration = automationProjectConfigurationService
            .create(automationProjectId, Environment.STAGING.ordinal(), "Staging", null, 1, true, true);
        AutomationProjectConfiguration otherProjectConfiguration =
            automationProjectConfigurationService.create(
                otherAutomationProjectId, PRODUCTION_ENVIRONMENT_ID, "Other", null, 1, true, true);
        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 1, true, true);

        String stagingExternalUserId = "rollout-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(stagingExternalUserId, Environment.STAGING);

        ConnectedUser stagingConnectedUser = connectedUserService.getConnectedUser(
            stagingExternalUserId, Environment.STAGING);

        automationProjectConfigurationResolver.assign(
            stagingConnectedUser, automationProjectId, stagingConfiguration.getId());

        ConnectedUser otherProjectConnectedUser = assignNewConnectedUser(
            otherAutomationProjectId, otherProjectConfiguration);

        ConfigurationMoveResultDTO upgradeResult =
            automationProjectConfigurationAdminFacade.upgradeConnectedUsers(
                targetConfiguration.getId(),
                List.of(stagingConnectedUser.getId(), otherProjectConnectedUser.getId()));

        assertThat(upgradeResult.movedConnectedUserIds()).isEmpty();
        assertThat(upgradeResult.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                stagingConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()),
            new ConfigurationMoveResultDTO.Skipped(
                otherProjectConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.NOT_ON_SOURCE, List.of()));
        assertThat(upgradeResult.needsAttentionConnectedUserIds()).isEmpty();

        assertThat(getAssignedConfigurationId(stagingConnectedUser.getId(), automationProjectId))
            .isEqualTo(stagingConfiguration.getId());
        assertThat(getAssignedConfigurationId(otherProjectConnectedUser.getId(), otherAutomationProjectId))
            .isEqualTo(otherProjectConfiguration.getId());
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(
            otherProjectConnectedUser.getId(), automationProjectId)).isEmpty();
    }

    @Test
    void testUpgradeConnectedUsersSkipsTargetLackingReferencedWorkflows() {
        long automationProjectId = createAutomationProject("Upgrade Lacks Workflow");

        String firstWorkflowUuid = addWorkflow(
            automationProjectId, LABELED_SLACK_WORKFLOW_DEFINITION.formatted("Send Digest"));
        String secondWorkflowUuid = addWorkflow(
            automationProjectId, LABELED_SLACK_WORKFLOW_DEFINITION.formatted("Fetch Recent Emails"));

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration firstSourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "First Source", null, 1, true, true);
        AutomationProjectConfiguration secondSourceConfiguration =
            automationProjectConfigurationService.create(
                automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Second Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser firstConnectedUser = assignNewConnectedUser(
            automationProjectId, firstSourceConfiguration);
        ConnectedUser secondConnectedUser = assignNewConnectedUser(
            automationProjectId, secondSourceConfiguration);

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            firstConnectedUser.getExternalId(), secondWorkflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow secondReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            secondConnectedUser.getExternalId(), firstWorkflowUuid, Environment.PRODUCTION);

        automationProjectFacade.deleteProjectWorkflow(secondWorkflowUuid);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        List<Long> connectedUserIds = List.of(firstConnectedUser.getId(), secondConnectedUser.getId());

        ConfigurationMoveResultDTO.Skipped expectedSkipped = new ConfigurationMoveResultDTO.Skipped(
            firstConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_REFERENCED_WORKFLOWS,
            List.of("Fetch Recent Emails"));

        assertThat(automationProjectConfigurationAdminFacade.getUpgradeImpact(
            targetConfiguration.getId(), connectedUserIds)
            .skipped()).containsExactly(expectedSkipped);

        ConfigurationMoveResultDTO upgradeResult =
            automationProjectConfigurationAdminFacade.upgradeConnectedUsers(
                targetConfiguration.getId(), connectedUserIds);

        assertThat(upgradeResult.movedConnectedUserIds()).containsExactly(secondConnectedUser.getId());
        assertThat(upgradeResult.skipped()).containsExactly(expectedSkipped);
        assertThat(upgradeResult.needsAttentionConnectedUserIds()).isEmpty();

        assertThat(getAssignedConfigurationId(firstConnectedUser.getId(), automationProjectId))
            .isEqualTo(firstSourceConfiguration.getId());
        assertThat(projectDeploymentService.getProjectDeployment(firstReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(1);

        assertThat(getAssignedConfigurationId(secondConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
        assertThat(projectDeploymentService.getProjectDeployment(secondReference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testUpgradeConnectedUsersReportsUsersNeedingAttention() {
        long automationProjectId = createAutomationProject("Upgrade Attention");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration firstSourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "First Source", null, 1, true, true);
        AutomationProjectConfiguration secondSourceConfiguration =
            automationProjectConfigurationService.create(
                automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Second Source", null, 1, true, true);
        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            firstSourceConfiguration.getId(), workflowUuid, Map.of(), Map.of("channel", "#source"));

        ConnectedUser firstConnectedUser = assignNewConnectedUser(
            automationProjectId, firstSourceConfiguration);
        ConnectedUser secondConnectedUser = assignNewConnectedUser(
            automationProjectId, secondSourceConfiguration);

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            firstConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        ConfigurationMoveResultDTO upgradeResult =
            automationProjectConfigurationAdminFacade.upgradeConnectedUsers(
                targetConfiguration.getId(), List.of(firstConnectedUser.getId(), secondConnectedUser.getId()));

        assertThat(upgradeResult.movedConnectedUserIds()).containsExactly(
            firstConnectedUser.getId(), secondConnectedUser.getId());
        assertThat(upgradeResult.skipped()).isEmpty();
        assertThat(upgradeResult.needsAttentionConnectedUserIds()).containsExactly(firstConnectedUser.getId());
        assertThat(fetchDeploymentRow(firstReference).isEnabled()).isFalse();
        assertThat(getAssignedConfigurationId(secondConnectedUser.getId(), automationProjectId))
            .isEqualTo(targetConfiguration.getId());
    }

    @Test
    void testGetUpgradeImpactMergesSourcesAndCountsEachUserOnce() {
        long automationProjectId = createAutomationProject("Upgrade Impact");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration firstSourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "First Source", null, 1, true, true);
        AutomationProjectConfiguration secondSourceConfiguration =
            automationProjectConfigurationService.create(
                automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Second Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser firstConnectedUser = assignNewConnectedUser(
            automationProjectId, firstSourceConfiguration);
        ConnectedUser secondConnectedUser = assignNewConnectedUser(
            automationProjectId, secondSourceConfiguration);
        ConnectedUser stayingConnectedUser = assignNewConnectedUser(
            automationProjectId, firstSourceConfiguration);

        for (ConnectedUser connectedUser : List.of(firstConnectedUser, secondConnectedUser, stayingConnectedUser)) {
            connectedUserWorkflowReferenceFacade.getOrCreateReference(
                connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);
        }

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        ConnectedUser targetConnectedUser = assignNewConnectedUser(automationProjectId, targetConfiguration);

        ConfigurationMoveImpactDTO upgradeImpact = automationProjectConfigurationAdminFacade.getUpgradeImpact(
            targetConfiguration.getId(),
            List.of(
                secondConnectedUser.getId(), firstConnectedUser.getId(), firstConnectedUser.getId(),
                targetConnectedUser.getId()));

        assertThat(upgradeImpact.skipped()).containsExactly(
            new ConfigurationMoveResultDTO.Skipped(
                targetConnectedUser.getId(), ConfigurationMoveResultDTO.Skipped.ALREADY_ON_TARGET, List.of()));
        assertThat(upgradeImpact.workflows())
            .singleElement()
            .satisfies(workflow -> {
                assertThat(workflow.workflowUuid()).isEqualTo(workflowUuid);
                assertThat(workflow.newRequiredInputs()).containsExactly("channel");
                assertThat(workflow.newRequiredConnections()).isEmpty();
                assertThat(workflow.affectedConnectedUserCount()).isEqualTo(2);
            });
        assertThat(getAssignedConfigurationId(firstConnectedUser.getId(), automationProjectId))
            .isEqualTo(firstSourceConfiguration.getId());
        assertThat(getAssignedConfigurationId(secondConnectedUser.getId(), automationProjectId))
            .isEqualTo(secondSourceConfiguration.getId());
    }

    @Test
    void testGetConfigurationConnectedUsersListsAssignedUsersOnly() {
        long automationProjectId = createAutomationProject("Configuration Connected Users");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configurationA = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "A", null, 1, true, true);
        AutomationProjectConfiguration configurationB = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "B", null, 1, true, true);

        ConnectedUser firstConnectedUser = assignNewConnectedUser(automationProjectId, configurationA);
        ConnectedUser secondConnectedUser = assignNewConnectedUser(automationProjectId, configurationA);

        assignNewConnectedUser(automationProjectId, configurationB);

        assertThat(automationProjectConfigurationAdminFacade.getConfigurationConnectedUsers(
            configurationA.getId()))
                .containsExactly(
                    new AutomationProjectConfigurationConnectedUserDTO(
                        firstConnectedUser.getId(), firstConnectedUser.getExternalId(), firstConnectedUser.getName(),
                        firstConnectedUser.getEmail()),
                    new AutomationProjectConfigurationConnectedUserDTO(
                        secondConnectedUser.getId(), secondConnectedUser.getExternalId(),
                        secondConnectedUser.getName(), secondConnectedUser.getEmail()));
    }

    @Test
    void testRollOutDeletesAReferenceDeploymentWithoutReferencesAndRollsOutTheOthers() {
        long automationProjectId = createAutomationProject("Rollout Without References");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        String emptyExternalUserId = "rollout-empty-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(emptyExternalUserId, Environment.PRODUCTION);

        long emptyProjectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, emptyExternalUserId, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        publishAndPinProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.fetchProjectDeployment(emptyProjectDeploymentId)).isEmpty();
        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(reference.getProjectDeploymentId()))
            .singleElement()
            .extracting(ProjectDeploymentWorkflow::getWorkflowId)
            .isEqualTo(getWorkflowId(automationProjectId, 2, workflowUuid));
    }

    @Test
    void testRollOutLeavesADeploymentThatIsNotAReferenceDeploymentAlone() {
        long automationProjectId = createAutomationProject("Rollout Vendor Deployment");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ProjectDeployment vendorProjectDeployment = new ProjectDeployment();

        vendorProjectDeployment.setEnvironment(Environment.PRODUCTION);
        vendorProjectDeployment.setName("Vendor deployment");
        vendorProjectDeployment.setProjectId(automationProjectId);
        vendorProjectDeployment.setProjectVersion(1);

        long vendorProjectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            vendorProjectDeployment, List.of(), List.of());

        publishAndPinProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.fetchProjectDeployment(vendorProjectDeploymentId))
            .get()
            .extracting(ProjectDeployment::getProjectVersion)
            .isEqualTo(1);
    }

    @Test
    void testRollOutLeavesALegacyNamedMarkerDeploymentWithoutAConnectedUserReferenceDeploymentRelationAlone() {
        long automationProjectId = createAutomationProject("Rollout Marker Without Relation");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        long markerProjectDeploymentId = createProjectDeployment(
            automationProjectId, "__EMBEDDED__" + EXTERNAL_USER_ID + "__PRODUCTION");

        publishAndPinProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(projectDeploymentService.fetchProjectDeployment(markerProjectDeploymentId))
            .get()
            .extracting(ProjectDeployment::getProjectVersion)
            .isEqualTo(1);
    }

    @Test
    void testDeleteReferenceDeploymentsLeavesLegacyNamedMarkerDeploymentsWithoutARelationAndAdminDeploymentsAlone() {
        long automationProjectId = createAutomationProject("Delete Marker Deployment");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        long markerProjectDeploymentId = createProjectDeployment(
            automationProjectId, "__EMBEDDED__other-user__PRODUCTION");
        long adminProjectDeploymentId = createProjectDeployment(automationProjectId, "Admin deployment");

        connectedUserReferenceRolloutManager.deleteReferenceDeployments(automationProjectId);

        assertThat(projectDeploymentService.fetchProjectDeployment(markerProjectDeploymentId)).isPresent();
        assertThat(projectDeploymentService.fetchProjectDeployment(adminProjectDeploymentId)).isPresent();
    }

    @Test
    void testRollOutDeploymentIfBehindDeletesADeploymentWithoutReferences() {
        long automationProjectId = createAutomationProject("Rollout Lazy Without References");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        String emptyExternalUserId = "rollout-lazy-empty-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(emptyExternalUserId, Environment.PRODUCTION);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, emptyExternalUserId, Environment.PRODUCTION);

        publishAndPinProject(automationProjectId);

        assertThat(connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(projectDeploymentId)).isTrue();
        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isEmpty();
    }

    @Test
    void testRollOutDeploymentIfBehindReturnsFalseForALegacyNamedMarkerDeploymentWithoutReferences() {
        long automationProjectId = createAutomationProject("Rollout Lazy Marker");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        long markerProjectDeploymentId = createProjectDeployment(
            automationProjectId, "__EMBEDDED__" + EXTERNAL_USER_ID + "__PRODUCTION");

        publishAndPinProject(automationProjectId);

        assertThat(connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(markerProjectDeploymentId))
            .isFalse();
        assertThat(projectDeploymentService.getProjectDeployment(markerProjectDeploymentId)
            .getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testDeleteReferenceDeploymentsDeletesARenamedReferenceDeploymentAndDanglesItsReferences() {
        long automationProjectId = createAutomationProject("Delete Renamed Reference Deployment");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        long projectDeploymentId = reference.getProjectDeploymentId();

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        projectDeployment.setName("Renamed reference deployment");

        projectDeploymentService.update(projectDeployment);

        connectedUserReferenceRolloutManager.deleteReferenceDeployments(automationProjectId);

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
        long automationProjectId = createAutomationProject("Provided Connection Every User");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);
        stubNoEntitledSlackConnection();
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

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
        long automationProjectId = createAutomationProject("Provided Connection Overrides");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(getRowConnectionIds(reference)).containsExactly(777L);

        provideValuesAndRollOut(
            automationProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

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
        long automationProjectId = createAutomationProject("Provided Connection Removed Auto-wire");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(getRowConnectionIds(reference)).containsExactly(ADMINISTRATOR_CONNECTION_ID);

        provideValuesAndRollOut(automationProjectId, workflowUuid, Map.of(), Map.of());

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();
        assertThat(getRowConnectionIds(reference)).containsExactly(777L);
    }

    @Test
    void testRemovingAProvidedConnectionTheUserCannotReplacePausesTheWorkflowAndKeepsIntent() {
        long automationProjectId = createAutomationProject("Provided Connection Removed Missing");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);
        stubNoEntitledSlackConnection();
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();

        provideValuesAndRollOut(automationProjectId, workflowUuid, Map.of(), Map.of());

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getRowConnectionIds(reference)).isEmpty();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isEqualTo("MISSING_CONNECTION:slack");
    }

    @Test
    void testADeletedProvidedConnectionIsIgnoredAndTheSlotFallsBackToTheUser() {
        long automationProjectId = createAutomationProject("Provided Connection Deleted");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(getRowConnectionIds(reference)).containsExactly(ADMINISTRATOR_CONNECTION_ID);

        when(connectionService.fetchConnection(ADMINISTRATOR_CONNECTION_ID)).thenReturn(Optional.empty());

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isTrue();
        assertThat(getRowConnectionIds(reference)).containsExactly(777L);

        stubNoEntitledSlackConnection();

        connectedUserReferenceRolloutManager.rollOutAndApply(automationProjectId, PRODUCTION_ENVIRONMENT_ID);

        assertThat(fetchDeploymentRow(reference).isEnabled()).isFalse();
        assertThat(getStoredReference(reference).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getStoredReference(reference))
            .attentionReason()).isEqualTo("MISSING_CONNECTION:slack");
    }

    @Test
    void testProvidedInputsOverrideTheUsersValueAndNeverCountAsMissing() {
        long automationProjectId = createAutomationProject("Provided Inputs");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(automationProjectId, workflowUuid, Map.of(), Map.of("channel", "#administrator"));

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
        long automationProjectId = createAutomationProject("Provided Inputs Change");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION, Map.of(), Map.of("channel", "#mine"));

        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#mine"));

        provideValuesAndRollOut(automationProjectId, workflowUuid, Map.of(), Map.of("channel", "#first"));

        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#first"));

        provideValuesAndRollOut(automationProjectId, workflowUuid, Map.of(), Map.of("channel", "#second"));

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
        long automationProjectId = createAutomationProject("Provided Unknown Keys");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);
        stubNoEntitledSlackConnection();
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, PRODUCTION_ENVIRONMENT_ID)
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.updateWorkflowProvidedValues(
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
        long automationProjectId = createAutomationProject("Provided Connection Locked");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        provideValues(
            automationProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

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
        long automationProjectId = createAutomationProject("Provided Input Locked");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        provideValues(automationProjectId, workflowUuid, Map.of(), Map.of("channel", "#administrator"));

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
        long automationProjectId = createAutomationProject("Provided Reads");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

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
            automationProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID),
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
            .filteredOn(workflow -> Objects.equals(workflow.automationUuid(), workflowUuid))
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
                assertThat(workflow.connections()).doesNotContainKey("slack");
            });
    }

    @Test
    void testWorkflowReadLeavesOutTheConnectionOfAProvidedTriggerComponent() {
        long automationProjectId = createAutomationProject("Provided Trigger Connection");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_ONLY_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubConnectionRequiredSlackComponentDefinition();
        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        ComponentConnection triggerSlot = new ComponentConnection("slack", 1, "newMessage1", "slack", true);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTrigger.class)))
            .thenReturn(List.of(triggerSlot));
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("newMessage1"), eq("slack")))
            .thenReturn(triggerSlot);

        String externalUserId = createConnectedUser();

        connectedUserWorkflowReferenceFacade.getOrCreateReference(externalUserId, workflowUuid, Environment.PRODUCTION);

        assertThat(connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION))
            .filteredOn(workflow -> Objects.equals(workflow.automationUuid(), workflowUuid))
            .singleElement()
            .satisfies(workflow -> assertThat(workflow.connections()).isEqualTo(Map.of("slack", 777L)));

        provideValues(automationProjectId, workflowUuid, Map.of("slack", ADMINISTRATOR_CONNECTION_ID), Map.of());

        assertThat(connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION))
            .filteredOn(workflow -> Objects.equals(workflow.automationUuid(), workflowUuid))
            .singleElement()
            .satisfies(workflow -> assertThat(workflow.connections()).isEmpty());
    }

    @Test
    void testRemovingAProvidedInputTakesTheAdministratorsValueOffEveryRowAndPausesTheWorkflow() {
        long automationProjectId = createAutomationProject("Provided Input Removed");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        provideValues(automationProjectId, workflowUuid, Map.of(), Map.of("channel", "#administrator"));

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

        provideValuesAndRollOut(automationProjectId, workflowUuid, Map.of(), Map.of());

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
    void testSavingTheConfigurationAgainWithUnchangedValuesLeavesRowsAndTriggersAlone() {
        long automationProjectId = createAutomationProject("Unchanged Save");

        String workflowUuid = addWorkflow(
            automationProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);
        stubAdministratorSlackConnection(ADMINISTRATOR_CONNECTION_ID);

        Map<String, Long> providedConnections = Map.of("slack", ADMINISTRATOR_CONNECTION_ID);
        Map<String, Object> providedInputs = Map.of("channel", "#administrator");

        provideValuesAndRollOut(automationProjectId, workflowUuid, providedConnections, providedInputs);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            createConnectedUser(), workflowUuid, Environment.PRODUCTION);

        String workflowId = fetchDeploymentRow(reference).getWorkflowId();

        verify(triggerLifecycleFacade, times(1))
            .executeTriggerEnable(eq(workflowId), any(), any(), any(), any(), any(), anyLong());

        provideValuesAndRollOut(automationProjectId, workflowUuid, providedConnections, providedInputs);

        ProjectDeploymentWorkflow rowBeforeTheSecondSave = fetchDeploymentRow(reference);

        clearInvocations(triggerLifecycleFacade);

        provideValuesAndRollOut(automationProjectId, workflowUuid, providedConnections, providedInputs);

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, PRODUCTION_ENVIRONMENT_ID)
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationAdminFacade.updateConfiguration(
            configuration.getId(), configuration.getName(), configuration.getPermissionExpression(),
            configuration.getProjectVersion(), configuration.isEnabled(), configuration.isAutomationHubVisible());

        rollOutTheLastUpdatedEvent();

        verifyNoInteractions(triggerLifecycleFacade);
        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getVersion()).isEqualTo(rowBeforeTheSecondSave.getVersion());
                assertThat(row.getLastModifiedDate()).isEqualTo(rowBeforeTheSecondSave.getLastModifiedDate());
            });

        provideValuesAndRollOut(
            automationProjectId, workflowUuid, providedConnections, Map.of("channel", "#changed"));

        verify(triggerLifecycleFacade, times(1))
            .executeTriggerEnable(eq(workflowId), any(), any(), any(), any(), any(), anyLong());
        assertThat(fetchDeploymentRow(reference).getInputs()).isEqualTo(Map.of("channel", "#changed"));
    }

    @Test
    void testConcurrentRolloutRequestsForOnePairCoalesceIntoTwoPassesEndingInTheNewestState()
        throws InterruptedException {

        long automationProjectId = createAutomationProject("Coalesced Rollouts");

        ConnectedUserProjectWorkflow reference = createReferenceBehindTwoPublishedVersions(automationProjectId);

        CoalescedRollout coalescedRollout = runRequestsWhileARolloutWaitsForTheLock(
            automationProjectId, COALESCED_REQUEST_COUNT, 3);

        assertThat(coalescedRollout.rolloutFailure()).isNull();
        assertThat(coalescedRollout.requestsReturnedWhileTheRolloutWaited()).isTrue();
        assertThat(coalescedRollout.rolloutFinishedWhileLocked()).isFalse();
        assertThat(coalescedRollout.passCount()).isEqualTo(2);
        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(3);
    }

    @Test
    void testARequestDuringARunningRolloutRunsExactlyOneMorePass() throws InterruptedException {
        long automationProjectId = createAutomationProject("One More Pass");

        ConnectedUserProjectWorkflow reference = createReferenceBehindTwoPublishedVersions(automationProjectId);

        CoalescedRollout coalescedRollout = runRequestsWhileARolloutWaitsForTheLock(
            automationProjectId, 1, 3);

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
                automationProjectId, PRODUCTION_ENVIRONMENT_ID);
        } finally {
            logger.setLevel(previousLevel);

            detachAppenderFromRolloutManagerLogger(appender);
        }

        assertThat(countRolloutPasses(appender, automationProjectId)).isEqualTo(1);
    }

    @Test
    void testRolloutRequestsForDifferentPairsAreNotCoalesced() throws InterruptedException {
        long lockedAutomationProjectId = createAutomationProject("Coalescing Locked Pair");

        addWorkflow(lockedAutomationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(lockedAutomationProjectId);

        long automationProjectId = createAutomationProject("Coalescing Free Pair");

        ConnectedUserProjectWorkflow reference = createReferenceBehindTwoPublishedVersions(automationProjectId);

        updateProductionConfigurationVersion(automationProjectId, 2);

        CountDownLatch startedLatch = new CountDownLatch(0);
        CountDownLatch lockedFinishedLatch = new CountDownLatch(1);
        CountDownLatch freeFinishedLatch = new CountDownLatch(1);
        AtomicBoolean freeFinishedWhileTheOtherPairWaited = new AtomicBoolean();
        AtomicBoolean lockedFinishedWhileLocked = new AtomicBoolean();
        AtomicReference<Throwable> rolloutFailure = new AtomicReference<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            automationProjectConfigurationService.lockRollout(
                lockedAutomationProjectId, PRODUCTION_ENVIRONMENT_ID);

            startRolloutThread(lockedAutomationProjectId, startedLatch, lockedFinishedLatch, rolloutFailure);

            awaitAWaitingAdvisoryLock();

            startRolloutThread(automationProjectId, startedLatch, freeFinishedLatch, rolloutFailure);

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
        long automationProjectId, int requestCount, int newestProjectVersion) throws InterruptedException {

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
                automationProjectConfigurationService.lockRollout(
                    automationProjectId, PRODUCTION_ENVIRONMENT_ID);

                startRolloutThread(automationProjectId, startedLatch, rolloutFinishedLatch, rolloutFailure);

                awaitAWaitingAdvisoryLock();

                saveTransactionTemplate.executeWithoutResult(
                    saveStatus -> updateProductionConfigurationVersion(
                        automationProjectId, newestProjectVersion));

                for (int index = 0; index < requestCount; index++) {
                    startRolloutThread(automationProjectId, startedLatch, requestsFinishedLatch,
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
            countRolloutPasses(appender, automationProjectId), rolloutFailure.get());
    }

    private ConnectedUserProjectWorkflow createReferenceBehindTwoPublishedVersions(long automationProjectId) {
        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            createConnectedUser(), workflowUuid, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);
        automationProjectFacade.publishProject(automationProjectId);

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

    private static long countRolloutPasses(ListAppender<ILoggingEvent> appender, long automationProjectId) {
        String passMessagePrefix = "Running a rollout pass of automation project id=%d ".formatted(
            automationProjectId);

        return appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .filter(message -> message.startsWith(passMessagePrefix))
            .count();
    }

    private Map<String, ?> getInputValues(String externalUserId, String workflowUuid) {
        return connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION)
            .stream()
            .filter(workflow -> Objects.equals(workflow.automationUuid(), workflowUuid))
            .findFirst()
            .orElseThrow()
            .inputValues();
    }

    private ConnectedUserWorkflowTemplateDTO getTemplate(String externalUserId, String workflowUuid) {
        return automationProjectFacade.getPublishedProjects(externalUserId, Environment.PRODUCTION)
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
        long automationProjectId, String workflowUuid, Map<String, Long> providedConnections,
        Map<String, Object> providedInputs) {

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, PRODUCTION_ENVIRONMENT_ID)
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            configuration.getId(), workflowUuid, providedConnections, providedInputs);
    }

    private void provideValuesAndRollOut(
        long automationProjectId, String workflowUuid, Map<String, Long> providedConnections,
        Map<String, Object> providedInputs) {

        provideValues(automationProjectId, workflowUuid, providedConnections, providedInputs);

        rollOutTheLastUpdatedEvent();
    }

    private void rollOutTheLastUpdatedEvent() {
        ArgumentCaptor<AutomationProjectConfigurationUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(
            AutomationProjectConfigurationUpdatedEvent.class);

        verify(automationProjectConfigurationUpdatedEventListener, atLeastOnce())
            .onAutomationProjectConfigurationUpdated(eventCaptor.capture());

        AutomationProjectConfigurationUpdatedEvent event = eventCaptor.getValue();

        connectedUserReferenceRolloutManager.rollOutAndApply(
            event.projectId(), event.environmentId(), event.configurationId(), event.removedProvidedInputNames());
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

    private void publishWithRequiredChannelInput(long automationProjectId, String workflowUuid) {
        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        publishAndPinProject(automationProjectId);
    }

    private ProjectDeploymentWorkflow fetchDeploymentRow(ConnectedUserProjectWorkflow reference) {
        return connectedUserReferenceDeploymentManager
            .fetchRow(reference.getProjectDeploymentId(), reference.getAutomationUuid())
            .orElseThrow();
    }

    private void setProductionConfigurationEnabled(long automationProjectId, boolean enabled) {
        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, PRODUCTION_ENVIRONMENT_ID)
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.updateEnabled(configuration.getId(), enabled);
    }

    private long createProjectDeployment(long automationProjectId, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(automationProjectId);
        projectDeployment.setProjectVersion(1);

        return projectDeploymentFacade.createProjectDeployment(projectDeployment, List.of(), List.of());
    }

    private Optional<ConnectedUserReferenceDeployment> fetchReferenceDeployment(long automationProjectId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION);

        return connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUser.getId(), automationProjectId);
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

    private long createAutomationProject(String name) {
        return automationProjectFacade.createProject(name + " " + UUID.randomUUID(), "", null, List.of(), null);
    }

    private String createConnectedUser() {
        String externalUserId = "rollout-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        return externalUserId;
    }

    private String addWorkflow(long automationProjectId, String definition) {
        return automationProjectFacade.createProjectWorkflow(automationProjectId, definition, null);
    }

    private String getWorkflowId(long automationProjectId, int projectVersion, String workflowUuid) {
        return projectWorkflowService.fetchProjectWorkflow(automationProjectId, projectVersion, workflowUuid)
            .map(ProjectWorkflow::getWorkflowId)
            .orElseThrow();
    }

    private void startRolloutThread(
        long automationProjectId, CountDownLatch startLatch, CountDownLatch finishedLatch,
        AtomicReference<Throwable> rolloutFailure) {

        Thread rolloutThread = new Thread(() -> {
            try {
                if (!startLatch.await(ROLLOUT_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("The rollout was never released");
                }

                connectedUserReferenceRolloutManager.rollOutAndApply(
                    automationProjectId, PRODUCTION_ENVIRONMENT_ID);
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

    private record DeleteFixture(
        long automationProjectId, String workflowUuid, AutomationProjectConfiguration configurationA,
        AutomationProjectConfiguration configurationB, long firstConnectedUserId, long secondConnectedUserId,
        long thirdConnectedUserId, ConnectedUserProjectWorkflow firstReference, ConnectedUserProjectWorkflow copy,
        ConnectedUserProjectWorkflow thirdReference) {
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
        storedReference.setDanglingReason("Removed from the automation project on redeploy");
        storedReference.setEnabled(false);

        connectedUserProjectWorkflowRepository.save(storedReference);
    }

    private void updateProductionConfigurationVersion(long automationProjectId, int projectVersion) {
        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, PRODUCTION_ENVIRONMENT_ID)
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.update(configuration.getId(), configuration.getName(),
            configuration.getPermissionExpression(), projectVersion, true, true);
    }

    private long createAssignedReference(
        long automationProjectId, String workflowUuid, AutomationProjectConfiguration configuration) {

        String externalUserId = createConnectedUser();

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, Environment.PRODUCTION);

        automationProjectConfigurationResolver.assign(
            connectedUser, automationProjectId, configuration.getId());

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION);

        return connectedUser.getId();
    }

    private DeleteFixture createDeleteFixture(String name) {
        long automationProjectId = createAutomationProject(name);

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configurationA = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "A", null, 1, true, true);
        AutomationProjectConfiguration configurationB = automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, "B", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser firstConnectedUser = connectedUserService.getConnectedUser(
            createConnectedUser(), Environment.PRODUCTION);
        ConnectedUser secondConnectedUser = connectedUserService.getConnectedUser(
            createConnectedUser(), Environment.PRODUCTION);
        ConnectedUser thirdConnectedUser = connectedUserService.getConnectedUser(
            createConnectedUser(), Environment.PRODUCTION);

        automationProjectConfigurationResolver.assign(
            firstConnectedUser, automationProjectId, configurationA.getId());
        automationProjectConfigurationResolver.assign(
            secondConnectedUser, automationProjectId, configurationA.getId());
        automationProjectConfigurationResolver.assign(
            thirdConnectedUser, automationProjectId, configurationB.getId());

        ConnectedUserProjectWorkflow firstReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            firstConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.copyWorkflowTemplate(
            secondConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow copy = connectedUserProjectWorkflowRepository
            .findAllByCopiedFromWorkflowUuid(workflowUuid)
            .getFirst();

        ConnectedUserProjectWorkflow thirdReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            thirdConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        return new DeleteFixture(
            automationProjectId, workflowUuid, configurationA, configurationB, firstConnectedUser.getId(),
            secondConnectedUser.getId(), thirdConnectedUser.getId(), firstReference, copy, thirdReference);
    }

    private ConnectedUser assignNewConnectedUser(
        long automationProjectId, AutomationProjectConfiguration configuration) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            createConnectedUser(), Environment.PRODUCTION);

        automationProjectConfigurationResolver.assign(
            connectedUser, automationProjectId, configuration.getId());

        return connectedUser;
    }

    private long getAssignedConfigurationId(long connectedUserId, long automationProjectId) {
        return connectedUserProjectConfigurationService.fetchAssignment(connectedUserId, automationProjectId)
            .map(ConnectedUserProjectConfiguration::getConfigurationId)
            .orElseThrow();
    }

    private void publishAndPinProject(long projectId) {
        AutomationProjectConfigurationTestSupport.publishAndPinProject(
            automationProjectFacade, automationProjectConfigurationService, projectId);
    }
}
