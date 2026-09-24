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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectPublishedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

    private static final String EXTERNAL_USER_ID = "rollout-user-1";
    private static final long RUNNING_FIRST_TEMPLATE_JOB_ID = 4242L;

    private static final String SLACK_AND_JIRA_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}},
         {"name":"createIssue1","type":"jira/v1/createIssue","parameters":{}}]}
        """;

    private static final String SLACK_TRIGGER_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[{"name":"newMessage1","type":"slack/v1/newMessage","parameters":{}}],
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
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private AutomationWorkflowProjectPublishedEventListener automationWorkflowProjectPublishedEventListener;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;

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
    private JobFacade jobFacade;

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, false, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.deleteReference(EXTERNAL_USER_ID, workflowUuid,
            Environment.PRODUCTION);

        assertThat(projectDeploymentService.fetchProjectDeploymentByName(
            automationWorkflowProjectId, "__EMBEDDED__" + EXTERNAL_USER_ID + "__PRODUCTION")).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isEmpty();
    }

    @Test
    void testChangingOneReferenceNeverStopsAnotherTemplatesRunningJobsOrTriggers() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Sibling Rows");

        String firstUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenThrow(new IllegalStateException("Connection lookup failed"));

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(IllegalStateException.class);

        assertThat(connectedUserProjectWorkflowRepository.findAll())
            .noneMatch(reference -> workflowUuid.equals(reference.getAutomationWorkflowUuid()));
        assertThat(projectDeploymentService.fetchProjectDeploymentByName(
            automationWorkflowProjectId, "__EMBEDDED__" + EXTERNAL_USER_ID + "__PRODUCTION")).isEmpty();
    }

    @Test
    void testEnableReferenceWithAMissingConnectionThrowsAndKeepsTheReferenceDisabled() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Missing Connection Enable");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        assertThat(reloadedReference.isEnabled()).isFalse();
    }

    @Test
    void testInputsCanBeSetOnAReferenceAndAreListed() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Inputs");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        connectedUserReferenceDeploymentManager.updateInputs(
            first.getProjectDeploymentId(), firstUuid, Map.of("channel", "#alerts"));

        connectedUserWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, firstUuid, true, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId);

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
    void testRepublishAddingAConnectionTheUserLacksDisablesOnlyThatReference() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Missing");

        String slackUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String otherUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId);

        assertThat(connectedUserProjectWorkflowRepository.findById(slackReference.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isEnabled)
            .isEqualTo(false);
        assertThat(connectedUserProjectWorkflowRepository.findById(otherReference.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isEnabled)
            .isEqualTo(true);
    }

    @Test
    void testRepublishWithoutATemplateMarksItsReferenceDangling() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Removed");

        String keptUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String removedUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow keptReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, keptUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow removedReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, removedUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.deleteProjectWorkflow(removedUuid);
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, onlyReferencedUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.deleteProjectWorkflow(onlyReferencedUuid);
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId);

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isDangling)
            .isEqualTo(true);
        assertThat(projectDeploymentService.fetchProjectDeploymentByName(
            automationWorkflowProjectId, "__EMBEDDED__" + EXTERNAL_USER_ID + "__PRODUCTION")).isEmpty();
    }

    @Test
    void testEnableCatchesUpADeploymentLeftBehind() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Lazy");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        automationWorkflowProjectFacade.deleteProjectWorkflow(removedUuid);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 1, removedUuid))
            .isPresent();
        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 2, removedUuid)).isEmpty();
    }

    @Test
    void testDeletingATemplateTwiceNeverTouchesThePublishedVersion() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Twice");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        String failingExternalUserId = createConnectedUser();
        String otherExternalUserId = createConnectedUser();

        ConnectedUserProjectWorkflow failingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            failingExternalUserId, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow otherReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            otherExternalUserId, workflowUuid, Environment.PRODUCTION);

        assertThat(otherReference.getProjectDeploymentId()).isNotEqualTo(failingReference.getProjectDeploymentId());

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser failingConnectedUser = connectedUserService.getConnectedUser(
            failingExternalUserId, Environment.PRODUCTION);

        when(connectedUserConnectionFacade.getConnections(failingConnectedUser.getId(), "slack", List.of()))
            .thenThrow(new IllegalStateException("Connection lookup failed"));

        assertThatCode(() -> connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId))
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
    void testRollOutDeletesADeploymentWhoseOnlyReferenceIsDangling() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Dangling Only");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        markDangling(reference);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 2, workflowUuid))
            .isPresent();

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId);

        assertThat(projectDeploymentService.fetchProjectDeployment(reference.getProjectDeploymentId())).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .satisfies(danglingReference -> {
                assertThat(danglingReference.isDangling()).isTrue();
                assertThat(danglingReference.isEnabled()).isFalse();
            });
    }

    @Test
    void testRollOutLogsAndReturnsWhenTheLastPublishedVersionCannotBeRead() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Unpublished");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        ListAppender<ILoggingEvent> appender = attachAppenderToRolloutManagerLogger();

        try {
            assertThatCode(() -> connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId))
                .doesNotThrowAnyException();

            assertThat(appender.list)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(event.getFormattedMessage())
                        .contains("automation workflow project id=" + automationWorkflowProjectId);
                });
        } finally {
            detachAppenderFromRolloutManagerLogger(appender);
        }
    }

    @Test
    void testRollOutDeletesAReferenceDeploymentWithoutReferencesAndRollsOutTheOthers() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Without References");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        long emptyProjectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProjectId, "rollout-empty-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId);

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

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ProjectDeployment vendorProjectDeployment = new ProjectDeployment();

        vendorProjectDeployment.setEnvironment(Environment.PRODUCTION);
        vendorProjectDeployment.setName("Vendor deployment");
        vendorProjectDeployment.setProjectId(automationWorkflowProjectId);
        vendorProjectDeployment.setProjectVersion(1);

        long vendorProjectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            vendorProjectDeployment, List.of(), List.of());

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserReferenceRolloutManager.rollOut(automationWorkflowProjectId);

        assertThat(projectDeploymentService.fetchProjectDeployment(vendorProjectDeploymentId))
            .get()
            .extracting(ProjectDeployment::getProjectVersion)
            .isEqualTo(1);
    }

    @Test
    void testRollOutDeploymentIfBehindDeletesADeploymentWithoutReferences() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Lazy Without References");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProjectId, "rollout-lazy-empty-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        assertThat(connectedUserReferenceRolloutManager.rollOutDeploymentIfBehind(projectDeploymentId)).isTrue();
        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isEmpty();
    }

    private static ListAppender<ILoggingEvent> attachAppenderToRolloutManagerLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(ConnectedUserReferenceRolloutManager.class);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        return appender;
    }

    private static void detachAppenderFromRolloutManagerLogger(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(ConnectedUserReferenceRolloutManager.class);

        logger.detachAppender(appender);
    }

    private long createAutomationWorkflowProject(String name) {
        return automationWorkflowProjectFacade.createProject(name + " " + UUID.randomUUID(), "", null, List.of(), null,
            null);
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
}
