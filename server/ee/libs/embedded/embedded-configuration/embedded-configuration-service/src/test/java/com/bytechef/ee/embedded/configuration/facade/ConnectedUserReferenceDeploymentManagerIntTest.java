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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
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
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ReferenceResolution;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.RowSpec;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectPublishedEventListener;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
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
class ConnectedUserReferenceDeploymentManagerIntTest {

    private static final long JIRA_CONNECTION_ID = 888L;
    private static final long OTHER_SLACK_CONNECTION_ID = 778L;
    private static final long SLACK_CONNECTION_ID = 777L;

    private static final String SLACK_AND_JIRA_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[{"name":"newMessage1","type":"slack/v1/newMessage","parameters":{}}],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}},
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

    private static final String SLACK_WORKFLOW_WITH_NOTE_AND_CHANNEL_INPUTS_DEFINITION = """
        {"label":"Post","inputs":[{"name":"note","label":"Note","type":"string","required":false},
         {"name":"channel","label":"Channel","type":"string","required":true}],
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

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

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

    private long connectedUserId;
    private String externalUserId;

    @BeforeEach
    void setUp() {
        externalUserId = "deployment-manager-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, Environment.PRODUCTION);

        connectedUserId = connectedUser.getId();

        when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
            .thenReturn(new ComponentDefinition("slack"));
        when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of());

        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                .thenReturn(Page.empty());

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(true);

        stubEntitledConnections();
    }

    @Test
    void testResolveReferenceEnablesTheRowWhenConnectionsAndInputsAreComplete() {
        String workflowId = createDraftWorkflow("Resolve Complete", SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUserId, workflowId, true, Map.of(), List.of(), Map.of("channel", "general"));

        RowSpec rowSpec = resolution.rowSpec();

        assertThat(rowSpec.enabled()).isTrue();
        assertThat(rowSpec.inputs()).isNull();
        assertThat(rowSpec.resolved()
            .connections()).containsExactly(slackConnection(SLACK_CONNECTION_ID));
        assertThat(resolution.missingComponentName()).isNull();
        assertThat(resolution.missingInputName()).isNull();
    }

    @Test
    void testResolveReferenceKeepsTheRowDisabledWhenEnablingWasNotAskedFor() {
        String workflowId = createDraftWorkflow("Resolve Not Asked", SLACK_WORKFLOW_DEFINITION);

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUserId, workflowId, false, Map.of(), List.of(), Map.of());

        assertThat(resolution.rowSpec()
            .enabled()).isFalse();
        assertThat(resolution.missingComponentName()).isNull();
        assertThat(resolution.missingInputName()).isNull();
    }

    @Test
    void testResolveReferenceKeepsTheRowDisabledWhenAConnectionIsMissing() {
        String workflowId = createDraftWorkflow("Resolve Missing Connection", SLACK_WORKFLOW_DEFINITION);

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUserId, workflowId, true, Map.of(), List.of(), Map.of());

        assertThat(resolution.rowSpec()
            .enabled()).isFalse();
        assertThat(resolution.missingComponentName()).isEqualTo("slack");
    }

    @Test
    void testResolveReferenceKeepsTheRowDisabledWhenARequiredInputIsMissing() {
        String workflowId = createDraftWorkflow("Resolve Missing Input", SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUserId, workflowId, true, Map.of(), List.of(), Map.of());

        assertThat(resolution.rowSpec()
            .enabled()).isFalse();
        assertThat(resolution.missingComponentName()).isNull();
        assertThat(resolution.missingInputName()).isEqualTo("channel");
    }

    @Test
    void testFindMissingRequiredInputCountsABlankValueAsMissing() {
        String workflowId = createDraftWorkflow("Blank Input", SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        assertThat(connectedUserReferenceDeploymentManager.findMissingRequiredInput(
            workflowId, Map.of("channel", "  "))).isEqualTo("channel");
    }

    @Test
    void testFindMissingRequiredInputIgnoresOptionalInputs() {
        String workflowId = createDraftWorkflow(
            "Optional Input", SLACK_WORKFLOW_WITH_NOTE_AND_CHANNEL_INPUTS_DEFINITION);

        Map<String, Object> inputs = new HashMap<>();

        inputs.put("channel", "general");
        inputs.put("note", null);

        assertThat(connectedUserReferenceDeploymentManager.findMissingRequiredInput(workflowId, inputs)).isNull();
    }

    @Test
    void testGetDeploymentNameIsDistinctPerEnvironment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Deployment Name");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        long productionDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProjectId, externalUserId, Environment.PRODUCTION);
        long developmentDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProjectId, externalUserId, Environment.DEVELOPMENT);

        assertThat(developmentDeploymentId).isNotEqualTo(productionDeploymentId);
        assertThat(projectDeploymentService.getProjectDeployment(productionDeploymentId))
            .satisfies(projectDeployment -> {
                assertThat(projectDeployment.getName()).isEqualTo("__EMBEDDED__" + externalUserId + "__PRODUCTION");
                assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
            });
        assertThat(projectDeploymentService.getProjectDeployment(developmentDeploymentId))
            .satisfies(projectDeployment -> {
                assertThat(projectDeployment.getName()).isEqualTo("__EMBEDDED__" + externalUserId + "__DEVELOPMENT");
                assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
            });
        assertThat(connectedUserReferenceDeploymentManager.getDeploymentName(externalUserId, Environment.PRODUCTION))
            .isEqualTo("__EMBEDDED__" + externalUserId + "__PRODUCTION");
    }

    @Test
    void testGetOrCreateDeploymentCreatesAnEnabledDeploymentAtTheLastPublishedVersion() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Create Deployment");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProjectId, externalUserId, Environment.PRODUCTION);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        assertThat(projectDeployment.getProjectVersion()).isEqualTo(3);
        assertThat(projectDeployment.getProjectId()).isEqualTo(automationWorkflowProjectId);
        assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(projectDeployment.getName()).isEqualTo("__EMBEDDED__" + externalUserId + "__PRODUCTION");
        assertThat(projectDeployment.isEnabled()).isTrue();
    }

    @Test
    void testGetOrCreateDeploymentReusesTheExistingDeployment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Reuse Deployment");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProjectId, externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationWorkflowProjectId, externalUserId, Environment.PRODUCTION)).isEqualTo(projectDeploymentId);
        assertThat(projectDeploymentService.getAllProjectDeployments(automationWorkflowProjectId))
            .singleElement()
            .extracting(ProjectDeployment::getId)
            .isEqualTo(projectDeploymentId);
    }

    @Test
    void testGetLastPublishedVersionRejectsAnUnpublishedProject() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Unpublished");

        addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        assertThatThrownBy(
            () -> connectedUserReferenceDeploymentManager.getLastPublishedVersion(automationWorkflowProjectId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPutWorkflowsAtTheSameVersionRewritesAnEnabledRowAloneDisablingItFirst() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rewrite Enabled Row");

        String siblingUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid =
            addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        provision(siblingUuid);

        long projectDeploymentId = provisionEnabled(workflowUuid, Map.of("channel", "general"));

        String workflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);
        String siblingWorkflowId = getWorkflowId(automationWorkflowProjectId, 1, siblingUuid);

        ProjectDeploymentWorkflow existingRow = getRow(projectDeploymentId, workflowId);
        ProjectDeploymentWorkflow existingSiblingRow = getRow(projectDeploymentId, siblingWorkflowId);

        assertThat(existingRow.isEnabled()).isTrue();

        clearInvocations(triggerLifecycleFacade);

        connectedUserReferenceDeploymentManager.putWorkflows(
            projectDeploymentId, 1, Map.of(workflowUuid, rowSpec(OTHER_SLACK_CONNECTION_ID, true, null)));

        InOrder inOrder = inOrder(triggerLifecycleFacade);

        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(eq(workflowId), any(), any(), any(), any());
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(eq(workflowId), any(), any(), any(), any(), any(), anyLong());

        assertThat(getRow(projectDeploymentId, workflowId)).satisfies(writtenRow -> {
            assertThat(writtenRow.getId()).isEqualTo(existingRow.getId());
            assertThat(writtenRow.isEnabled()).isTrue();
            assertThat(writtenRow.getConnections()).containsExactly(slackConnection(OTHER_SLACK_CONNECTION_ID));
            assertThat(writtenRow.getInputs()).isEqualTo(Map.of("channel", "general"));
        });

        verifySiblingRowUntouched(projectDeploymentId, existingSiblingRow);
    }

    @Test
    void testPutWorkflowsAtTheSameVersionWritesADisabledRowWithoutTogglingTriggers() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rewrite Disabled Row");

        String siblingUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid =
            addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        provision(siblingUuid);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();

        connectedUserReferenceDeploymentManager.updateInputs(
            projectDeploymentId, workflowUuid, Map.of("channel", "general"));

        String workflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);
        String siblingWorkflowId = getWorkflowId(automationWorkflowProjectId, 1, siblingUuid);

        ProjectDeploymentWorkflow existingRow = getRow(projectDeploymentId, workflowId);
        ProjectDeploymentWorkflow existingSiblingRow = getRow(projectDeploymentId, siblingWorkflowId);

        assertThat(existingRow.isEnabled()).isFalse();

        clearInvocations(triggerLifecycleFacade, principalJobService);

        connectedUserReferenceDeploymentManager.putWorkflows(
            projectDeploymentId, 1,
            Map.of(workflowUuid, rowSpec(OTHER_SLACK_CONNECTION_ID, false, Map.of("channel", "random"))));

        assertThat(getRow(projectDeploymentId, workflowId)).satisfies(writtenRow -> {
            assertThat(writtenRow.getId()).isEqualTo(existingRow.getId());
            assertThat(writtenRow.isEnabled()).isFalse();
            assertThat(writtenRow.getConnections()).containsExactly(slackConnection(OTHER_SLACK_CONNECTION_ID));
            assertThat(writtenRow.getInputs()).isEqualTo(Map.of("channel", "random"));
        });

        verifyNoInteractions(triggerLifecycleFacade);
        verifyRunningJobsNeverStopped(projectDeploymentId, workflowId);
        verifySiblingRowUntouched(projectDeploymentId, existingSiblingRow);
    }

    @Test
    void testPutWorkflowsAtTheSameVersionLeavesAnUnchangedRowAlone() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Unchanged Row");

        String workflowUuid = addWorkflow(
            automationWorkflowProjectId, SLACK_AND_JIRA_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        long projectDeploymentId = provisionEnabled(workflowUuid, Map.of("channel", "general"));

        String workflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);

        ProjectDeploymentWorkflow existingRow = getRow(projectDeploymentId, workflowId);

        assertThat(existingRow.isEnabled()).isTrue();
        assertThat(existingRow.getConnections()).containsExactlyInAnyOrder(
            slackConnection(SLACK_CONNECTION_ID), jiraConnection(JIRA_CONNECTION_ID));

        clearInvocations(triggerLifecycleFacade, principalJobService);

        List<ProjectDeploymentWorkflowConnection> reversedConnections = new ArrayList<>(existingRow.getConnections());

        Collections.reverse(reversedConnections);

        RowSpec rowSpec = new RowSpec(
            new ResolvedWorkflowConnections(reversedConnections, List.of()), true, Map.of("channel", "general"));

        connectedUserReferenceDeploymentManager.putWorkflows(projectDeploymentId, 1, Map.of(workflowUuid, rowSpec));

        verifyNoInteractions(triggerLifecycleFacade);
        verifyRunningJobsNeverStopped(projectDeploymentId, workflowId);

        assertThat(getRow(projectDeploymentId, workflowId)).satisfies(row -> {
            assertThat(row.getId()).isEqualTo(existingRow.getId());
            assertThat(row.getVersion()).isEqualTo(existingRow.getVersion());
            assertThat(row.isEnabled()).isTrue();
        });
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId)).hasSize(1);
    }

    @Test
    void testPutWorkflowsAtTheSameVersionCreatesANewRowDisabledThenEnablesIt() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Create Row");

        String siblingUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow siblingReference = provision(siblingUuid);

        long projectDeploymentId = siblingReference.getProjectDeploymentId();

        String workflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);
        String siblingWorkflowId = getWorkflowId(automationWorkflowProjectId, 1, siblingUuid);

        ProjectDeploymentWorkflow existingSiblingRow = getRow(projectDeploymentId, siblingWorkflowId);

        assertThat(connectedUserReferenceDeploymentManager.fetchWorkflowRow(projectDeploymentId, workflowId)).isEmpty();

        clearInvocations(triggerLifecycleFacade, principalJobService);

        connectedUserReferenceDeploymentManager.putWorkflows(
            projectDeploymentId, 1, Map.of(workflowUuid, rowSpec(SLACK_CONNECTION_ID, true, null)));

        assertThat(getRow(projectDeploymentId, workflowId)).satisfies(createdRow -> {
            assertThat(createdRow.getWorkflowId()).isEqualTo(workflowId);
            assertThat(createdRow.getProjectDeploymentId()).isEqualTo(projectDeploymentId);
            assertThat(createdRow.getConnections()).containsExactly(slackConnection(SLACK_CONNECTION_ID));
            assertThat(createdRow.isEnabled()).isTrue();
        });

        verify(triggerLifecycleFacade, times(1))
            .executeTriggerEnable(eq(workflowId), any(), any(), any(), any(), any(), anyLong());
        verify(triggerLifecycleFacade, never())
            .executeTriggerDisable(eq(workflowId), any(), any(), any(), any());
        verifyRunningJobsNeverStopped(projectDeploymentId, workflowId);
        verifySiblingRowUntouched(projectDeploymentId, existingSiblingRow);
    }

    @Test
    void testPutWorkflowsAtANewVersionWritesTheCompleteListAndDropsRowsNotPassed() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("New Version");

        String otherUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        provision(otherUuid);

        long projectDeploymentId = provisionEnabled(workflowUuid, Map.of("channel", "general"));

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId)).hasSize(2);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        RowSpec rowSpec = new RowSpec(new ResolvedWorkflowConnections(List.of(), List.of()), false, null);

        connectedUserReferenceDeploymentManager.putWorkflows(projectDeploymentId, 2, Map.of(workflowUuid, rowSpec));

        assertThat(projectDeploymentService.getProjectDeployment(projectDeploymentId)
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId))
            .singleElement()
            .satisfies(writtenRow -> {
                assertThat(writtenRow.getWorkflowId())
                    .isEqualTo(getWorkflowId(automationWorkflowProjectId, 2, workflowUuid));
                assertThat(writtenRow.getInputs()).isEqualTo(Map.of("channel", "general"));
                assertThat(writtenRow.isEnabled()).isFalse();
            });
    }

    @Test
    void testUpdateInputsRefusesAnEnabledRowMissingARequiredInput() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Refuse Inputs");

        String workflowUuid =
            addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        long projectDeploymentId = provisionEnabled(workflowUuid, Map.of("channel", "general"));

        String workflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);

        ProjectDeploymentWorkflow existingRow = getRow(projectDeploymentId, workflowId);

        clearInvocations(triggerLifecycleFacade, principalJobService);

        assertThatThrownBy(
            () -> connectedUserReferenceDeploymentManager.updateInputs(projectDeploymentId, workflowUuid, Map.of()))
                .isInstanceOf(MissingInputException.class)
                .extracting("inputName")
                .isEqualTo("channel");

        assertThat(getRow(projectDeploymentId, workflowId)).satisfies(row -> {
            assertThat(row.getVersion()).isEqualTo(existingRow.getVersion());
            assertThat(row.isEnabled()).isTrue();
            assertThat(row.getInputs()).isEqualTo(Map.of("channel", "general"));
        });

        verifyNoInteractions(triggerLifecycleFacade);
        verifyRunningJobsNeverStopped(projectDeploymentId, workflowId);
    }

    @Test
    void testUpdateInputsSavesADisabledRowWithIncompleteInputs() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Incomplete Inputs");

        String workflowUuid =
            addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();
        String workflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);

        clearInvocations(triggerLifecycleFacade);

        connectedUserReferenceDeploymentManager.updateInputs(
            projectDeploymentId, workflowUuid, Map.of("note", "partial"));

        assertThat(getRow(projectDeploymentId, workflowId)).satisfies(row -> {
            assertThat(row.getInputs()).isEqualTo(Map.of("note", "partial"));
            assertThat(row.isEnabled()).isFalse();
        });

        verifyNoInteractions(triggerLifecycleFacade);
    }

    @Test
    void testRemoveWorkflowDisablesAndDeletesAnEnabledRowAloneKeepingTheDeployment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Remove Enabled Row");

        String siblingUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        provision(siblingUuid);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();

        String workflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);
        String siblingWorkflowId = getWorkflowId(automationWorkflowProjectId, 1, siblingUuid);

        ProjectDeploymentWorkflow existingSiblingRow = getRow(projectDeploymentId, siblingWorkflowId);

        assertThat(getRow(projectDeploymentId, workflowId).isEnabled()).isTrue();

        clearInvocations(triggerLifecycleFacade, principalJobService);

        connectedUserReferenceDeploymentManager.removeWorkflow(projectDeploymentId, workflowUuid);

        verify(triggerLifecycleFacade).executeTriggerDisable(eq(workflowId), any(), any(), any(), any());

        assertThat(connectedUserReferenceDeploymentManager.fetchWorkflowRow(projectDeploymentId, workflowId)).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isPresent();

        verifySiblingRowUntouched(projectDeploymentId, existingSiblingRow);
    }

    @Test
    void testRemoveWorkflowOfADisabledRowSkipsDisabling() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Remove Disabled Row");

        String siblingUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        provision(siblingUuid);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        connectedUserWorkflowReferenceFacade.enableReference(
            externalUserId, workflowUuid, false, Environment.PRODUCTION);

        long projectDeploymentId = reference.getProjectDeploymentId();
        String workflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);

        assertThat(getRow(projectDeploymentId, workflowId).isEnabled()).isFalse();

        clearInvocations(triggerLifecycleFacade, principalJobService);

        connectedUserReferenceDeploymentManager.removeWorkflow(projectDeploymentId, workflowUuid);

        assertThat(connectedUserReferenceDeploymentManager.fetchWorkflowRow(projectDeploymentId, workflowId)).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isPresent();

        verifyNoInteractions(triggerLifecycleFacade);
        verifyRunningJobsNeverStopped(projectDeploymentId, workflowId);
    }

    @Test
    void testRemoveWorkflowOfADeploymentThatNoLongerExistsDoesNothing() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Remove Missing Deployment");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();

        connectedUserReferenceDeploymentManager.deleteDeployment(projectDeploymentId);

        clearInvocations(triggerLifecycleFacade);

        assertThatCode(() -> connectedUserReferenceDeploymentManager.removeWorkflow(projectDeploymentId, workflowUuid))
            .doesNotThrowAnyException();

        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isEmpty();

        verifyNoInteractions(triggerLifecycleFacade);
    }

    @Test
    void testRemoveWorkflowDeletesTheDeploymentWithItsLastRow() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Remove Last Row");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();
        String workflowId = getWorkflowId(automationWorkflowProjectId, 1, workflowUuid);

        assertThat(getRow(projectDeploymentId, workflowId).isEnabled()).isTrue();

        clearInvocations(triggerLifecycleFacade);

        connectedUserReferenceDeploymentManager.removeWorkflow(projectDeploymentId, workflowUuid);

        verify(triggerLifecycleFacade).executeTriggerDisable(eq(workflowId), any(), any(), any(), any());

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId)).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isEmpty();
    }

    private String addWorkflow(long automationWorkflowProjectId, String definition) {
        return automationWorkflowProjectFacade.createProjectWorkflow(automationWorkflowProjectId, definition, null);
    }

    private long createAutomationWorkflowProject(String name) {
        return automationWorkflowProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null, null);
    }

    private String createDraftWorkflow(String projectName, String definition) {
        long automationWorkflowProjectId = createAutomationWorkflowProject(projectName);

        String workflowUuid = addWorkflow(automationWorkflowProjectId, definition);

        return projectWorkflowService.getLastWorkflowId(workflowUuid);
    }

    private ProjectDeploymentWorkflow getRow(long projectDeploymentId, String workflowId) {
        return projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentId, workflowId);
    }

    private String getWorkflowId(long automationWorkflowProjectId, int projectVersion, String workflowUuid) {
        return projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, projectVersion, workflowUuid)
            .map(ProjectWorkflow::getWorkflowId)
            .orElseThrow();
    }

    private ConnectedUserProjectWorkflow provision(String workflowUuid) {
        return connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION);
    }

    private long provisionEnabled(String workflowUuid, Map<String, ?> inputs) {
        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();

        connectedUserReferenceDeploymentManager.updateInputs(projectDeploymentId, workflowUuid, inputs);

        connectedUserWorkflowReferenceFacade.enableReference(
            externalUserId, workflowUuid, true, Environment.PRODUCTION);

        return projectDeploymentId;
    }

    private void stubEntitledConnection(long connectionId, String componentName) {
        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);

        when(connectionDTO.id()).thenReturn(connectionId);
        when(connectionDTO.componentName()).thenReturn(componentName);
        when(connectedUserConnectionFacade.getConnections(anyLong(), eq(componentName), eq(List.of())))
            .thenReturn(List.of(connectionDTO));

        stubConnection(connectionId, componentName);
    }

    private void stubConnection(long connectionId, String componentName) {
        Connection connection = new Connection();

        connection.setComponentName(componentName);
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(connectionId);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);
    }

    private void stubEntitledConnections() {
        ComponentConnection slackSlot = new ComponentConnection("slack", 1, "postMessage1", "slack", true);
        ComponentConnection jiraSlot = new ComponentConnection("jira", 1, "createIssue1", "jira", true);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenAnswer(invocation -> switch (invocation.<WorkflowTask>getArgument(0)
                .getName()) {
                case "postMessage1" -> List.of(slackSlot);
                case "createIssue1" -> List.of(jiraSlot);
                default -> List.of();
            });
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("postMessage1"), eq("slack")))
            .thenReturn(slackSlot);
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("createIssue1"), eq("jira")))
            .thenReturn(jiraSlot);

        stubEntitledConnection(SLACK_CONNECTION_ID, "slack");
        stubEntitledConnection(JIRA_CONNECTION_ID, "jira");
        stubConnection(OTHER_SLACK_CONNECTION_ID, "slack");
    }

    private void verifySiblingRowUntouched(long projectDeploymentId, ProjectDeploymentWorkflow existingSiblingRow) {
        String siblingWorkflowId = existingSiblingRow.getWorkflowId();

        assertThat(getRow(projectDeploymentId, siblingWorkflowId)).satisfies(siblingRow -> {
            assertThat(siblingRow.getId()).isEqualTo(existingSiblingRow.getId());
            assertThat(siblingRow.getVersion()).isEqualTo(existingSiblingRow.getVersion());
            assertThat(siblingRow.isEnabled()).isEqualTo(existingSiblingRow.isEnabled());
        });

        verify(triggerLifecycleFacade, never())
            .executeTriggerDisable(eq(siblingWorkflowId), any(), any(), any(), any());
        verify(triggerLifecycleFacade, never())
            .executeTriggerEnable(eq(siblingWorkflowId), any(), any(), any(), any(), any(), anyLong());
    }

    private void verifyRunningJobsNeverStopped(long projectDeploymentId, String workflowId) {
        verify(principalJobService, never()).getJobIds(
            any(), any(), any(), eq(List.of(projectDeploymentId)), any(), eq(List.of(workflowId)), anyBoolean(),
            anyInt());
    }

    private static ProjectDeploymentWorkflowConnection jiraConnection(long connectionId) {
        return new ProjectDeploymentWorkflowConnection(connectionId, "jira", "createIssue1");
    }

    private static RowSpec rowSpec(long connectionId, boolean enabled, Map<String, ?> inputs) {
        return new RowSpec(
            new ResolvedWorkflowConnections(List.of(slackConnection(connectionId)), List.of()), enabled, inputs);
    }

    private static ProjectDeploymentWorkflowConnection slackConnection(long connectionId) {
        return new ProjectDeploymentWorkflowConnection(connectionId, "slack", "postMessage1");
    }
}
