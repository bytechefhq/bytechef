/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration;

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
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacadeIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceRolloutManager;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectPublishedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
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
        ConnectedUserReferenceRolloutIntTest.ConnectedUserServiceConfiguration.class
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
    ClusterElementDefinitionService.class, CodeWorkflowContainerFacade.class, CodeWorkflowContainerService.class,
    CodeWorkflowFileStorage.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
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
class ConnectedUserReferenceRolloutIntTest {

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

    // Publishing would otherwise run the rollout from the after-commit listener, racing the direct calls under test.
    @MockitoBean
    private AutomationWorkflowProjectPublishedEventListener automationWorkflowProjectPublishedEventListener;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

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

        // The per-user deployment is enabled, so rewriting or removing an enabled row stops that row's running jobs;
        // the mocked job service answers "none running", as an empty job table would.
        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                .thenReturn(Page.empty());

        // getOrCreateReference validates the automation workflow uuid against the permission-FILTERED automation workflows, which consults this
        // (mocked) evaluator; the default mock answer of false would hide every template from every connected user.
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

        ConnectedUserProjectWorkflow first = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow second = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
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

        ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserCodeWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, false, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        // Enabling catches the deployment up to the last published version first, so the row ends on that version.
        String versionTwoWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);

        assertThatCode(() -> connectedUserCodeWorkflowReferenceFacade.enableReference(
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

        ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserCodeWorkflowReferenceFacade.deleteReference(EXTERNAL_USER_ID, workflowUuid,
            Environment.PRODUCTION);

        assertThat(projectDeploymentService.fetchProjectDeploymentByName(
            automationWorkflowProjectId, "__EMBEDDED__" + EXTERNAL_USER_ID + "__PRODUCTION")).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isEmpty();
    }

    /**
     * Changing one template's reference must write only that template's row: disabling a row stops its running jobs, so
     * re-sending a sibling row would stop the sibling template's in-flight runs and re-register its triggers.
     */
    @Test
    void testChangingOneReferenceNeverStopsAnotherTemplatesRunningJobsOrTriggers() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Sibling Rows");

        String firstUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        assertThat(first.isEnabled()).isTrue();

        String firstWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(firstUuid);

        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), argThat(workflowIds -> workflowIds.contains(firstWorkflowId)),
            anyBoolean(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(RUNNING_FIRST_TEMPLATE_JOB_ID)));

        ConnectedUserProjectWorkflow second = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        connectedUserCodeWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, secondUuid, false, Environment.PRODUCTION);
        connectedUserCodeWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, secondUuid, true, Environment.PRODUCTION);

        assertThat(second.getProjectDeploymentId()).isEqualTo(first.getProjectDeploymentId());

        verify(jobFacade, never()).stopJob(RUNNING_FIRST_TEMPLATE_JOB_ID);
        verify(triggerLifecycleFacade, times(1))
            .executeTriggerEnable(eq(firstWorkflowId), any(), any(), any(), any(), any(), anyLong());
        verify(triggerLifecycleFacade, never())
            .executeTriggerDisable(eq(firstWorkflowId), any(), any(), any(), any());
    }

    /**
     * Provisioning through the three-argument overload is atomic: a failure after the deployment was created and the
     * reference saved -- here the entitlement lookup failing with a non-409 exception -- leaves neither behind, so the
     * next call cannot return an orphan.
     */
    @Test
    void testFailedProvisioningLeavesNoReferenceAndNoDeployment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Atomic Provisioning");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenThrow(new IllegalStateException("Connection lookup failed"));

        assertThatThrownBy(() -> connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(IllegalStateException.class);

        assertThat(connectedUserProjectWorkflowRepository.findAll())
            .noneMatch(reference -> workflowUuid.equals(reference.getAutomationWorkflowUuid()));
        assertThat(projectDeploymentService.fetchProjectDeploymentByName(
            automationWorkflowProjectId, "__EMBEDDED__" + EXTERNAL_USER_ID + "__PRODUCTION")).isEmpty();
    }

    /**
     * Enabling refused with a 409 keeps what it wrote: the reference, enabled before, is saved disabled in the real
     * transaction rather than rolled back to its enabled state.
     */
    @Test
    void testEnableReferenceWithAMissingConnectionThrowsAndKeepsTheReferenceDisabled() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Missing Connection Enable");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isTrue();

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        assertThatThrownBy(() -> connectedUserCodeWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class);

        ConnectedUserProjectWorkflow reloadedReference = connectedUserProjectWorkflowRepository
            .findById(reference.getId())
            .orElseThrow();

        assertThat(reloadedReference.isEnabled()).isFalse();
    }

    /**
     * A connected user's input values for a reference land on the deployment row through
     * {@link ConnectedUserProjectFacade#updateProjectWorkflowInputs} the same way copy-mode inputs do, and are reported
     * back by {@link ConnectedUserProjectFacade#getConnectedUserProjectWorkflows}.
     */
    @Test
    void testInputsCanBeSetOnAReferenceAndAreListed() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Inputs");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
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

    /**
     * Provisioning with a missing required input succeeds and leaves the reference disabled rather than aborting;
     * enabling is refused with {@link MissingInputException} until the input is set, mirroring
     * {@link #testEnableReferenceWithAMissingConnectionThrowsAndKeepsTheReferenceDisabled} for inputs instead of
     * connections.
     */
    @Test
    void testProvisionWithAMissingRequiredInputSucceedsDisabledAndEnableRefusesUntilItIsSet() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Required Input");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isFalse();

        assertThatThrownBy(() -> connectedUserCodeWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION))
                .isInstanceOf(MissingInputException.class)
                .extracting("inputName")
                .isEqualTo("channel");

        connectedUserReferenceDeploymentManager.updateInputs(
            reference.getProjectDeploymentId(), workflowUuid, Map.of("channel", "#alerts"));

        connectedUserCodeWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId()))
            .get()
            .extracting(ConnectedUserProjectWorkflow::isEnabled)
            .isEqualTo(true);
    }

    /**
     * Updating an ENABLED reference's inputs must not drop a required value out from under a running automation:
     * {@link ConnectedUserReferenceDeploymentManager#updateInputs} refuses with {@link MissingInputException} before
     * writing anything, rather than saving the incomplete inputs and then failing to re-enable the row with a raw
     * {@code IllegalArgumentException} from {@code ProjectDeploymentFacadeImpl}'s own validation.
     */
    @Test
    void testUpdatingInputsOnAnEnabledReferenceRefusesToDropARequiredValue() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Enabled Required Input");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        connectedUserReferenceDeploymentManager.updateInputs(
            reference.getProjectDeploymentId(), workflowUuid, Map.of("channel", "#alerts"));

        connectedUserCodeWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION);

        long environmentId = Environment.PRODUCTION.ordinal();

        when(environmentService.getEnvironment(environmentId)).thenReturn(Environment.PRODUCTION);

        // Non-empty but missing "channel": an empty map is a no-op (ProjectDeploymentWorkflow#setInputs ignores it),
        // which would let this call through even without the fix and hide the bug entirely.
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

        ConnectedUserProjectWorkflow first = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        connectedUserReferenceDeploymentManager.updateInputs(
            first.getProjectDeploymentId(), firstUuid, Map.of("channel", "#alerts"));

        // The required input was missing at provisioning, which left the reference disabled; rollout never enables a
        // disabled reference, so it is enabled here now that the input is set.
        connectedUserCodeWorkflowReferenceFacade.enableReference(
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

        ConnectedUserProjectWorkflow slackReference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, slackUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow otherReference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
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

        ConnectedUserProjectWorkflow keptReference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, keptUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow removedReference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
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

        // Never referenced: it only keeps the automation workflow project publishable once the referenced template is gone.
        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        String onlyReferencedUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
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

        ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserCodeWorkflowReferenceFacade.enableReference(
            EXTERNAL_USER_ID, workflowUuid, true, Environment.PRODUCTION);

        assertThat(projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId())
            .getProjectVersion()).isEqualTo(2);
    }

    /**
     * The editor deletes a template by its uuid. Only the draft loses it: a published version keeps its row, because
     * the deployments still on that version point at its workflow until the next publish rolls them forward.
     */
    @Test
    void testDeleteProjectWorkflowRemovesTheTemplateFromTheDraftByItsUuid() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete By Uuid");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        String removedUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        automationWorkflowProjectFacade.deleteProjectWorkflow(removedUuid);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 1, removedUuid)).isPresent();
        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 2, removedUuid)).isEmpty();
    }

    /**
     * Deleting a template the draft no longer holds -- a double submit or a retried mutation -- does nothing. It must
     * never fall through to the last PUBLISHED version, whose workflow the connected users' deployment rows still run.
     */
    @Test
    void testDeletingATemplateTwiceNeverTouchesThePublishedVersion() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Twice");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, workflowUuid, Environment.PRODUCTION);

        String publishedWorkflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);

        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);
        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);

        assertThat(projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, 1, workflowUuid)).isPresent();
        assertThat(connectedUserReferenceDeploymentManager.fetchWorkflowRow(
            reference.getProjectDeploymentId(), publishedWorkflowId))
                .get()
                .extracting(ProjectDeploymentWorkflow::isEnabled)
                .isEqualTo(true);
    }

    /**
     * A template first published in a newer version than the connected user's deployment can only be written once the
     * deployment has caught up: at the deployment's old version the template has no workflow to point a row at.
     */
    @Test
    void testProvisioningATemplateNewerThanTheDeploymentCatchesTheDeploymentUp() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rollout Newer Template");

        String firstUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        stubEntitledSlackConnection(777L);

        ConnectedUserProjectWorkflow first = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, firstUuid, Environment.PRODUCTION);

        String secondUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow second = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            EXTERNAL_USER_ID, secondUuid, Environment.PRODUCTION);

        long projectDeploymentId = first.getProjectDeploymentId();

        assertThat(second.getProjectDeploymentId()).isEqualTo(projectDeploymentId);
        assertThat(projectDeploymentService.getProjectDeployment(projectDeploymentId)
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId))
            .extracting(ProjectDeploymentWorkflow::getWorkflowId)
            .containsExactlyInAnyOrder(
                getWorkflowId(automationWorkflowProjectId, 2, firstUuid), getWorkflowId(automationWorkflowProjectId, 2, secondUuid));
    }

    private long createAutomationWorkflowProject(String name) {
        return automationWorkflowProjectFacade.createProject(name + " " + UUID.randomUUID(), "", null, List.of(), null,
            null);
    }

    private String addWorkflow(long automationWorkflowProjectId, String definition) {
        String workflowId = automationWorkflowProjectFacade.createProjectWorkflow(automationWorkflowProjectId, definition, null);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        return projectWorkflow.getUuidAsString();
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
}
