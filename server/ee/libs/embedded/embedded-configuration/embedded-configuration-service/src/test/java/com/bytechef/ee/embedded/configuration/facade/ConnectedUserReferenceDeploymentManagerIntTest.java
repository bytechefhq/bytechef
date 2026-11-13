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
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ReferenceResolution;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.RowSpec;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
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
class ConnectedUserReferenceDeploymentManagerIntTest {

    private static final long JIRA_CONNECTION_ID = 888L;
    private static final long OTHER_SLACK_CONNECTION_ID = 778L;
    private static final long SLACK_CONNECTION_ID = 777L;
    private static final long UNCONFIGURED_PROJECT_ID = 0L;
    private static final String UNCONFIGURED_WORKFLOW_UUID = "00000000-0000-0000-0000-000000000000";

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
    private AutomationProjectConfigurationService automationProjectConfigurationService;

    @Autowired
    private AutomationProjectFacade automationProjectFacade;

    @MockitoBean
    private AutomationProjectConfigurationUpdatedEventListener automationProjectConfigurationUpdatedEventListener;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;

    @Autowired
    private ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;

    @Autowired
    private ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;

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
            connectedUserId, UNCONFIGURED_PROJECT_ID, UNCONFIGURED_WORKFLOW_UUID, workflowId,
            true, Map.of(), List.of(), Map.of("channel", "general"));

        RowSpec rowSpec = resolution.rowSpec();

        assertThat(rowSpec.enabled()).isTrue();
        assertThat(rowSpec.inputs()).isEqualTo(Map.of("channel", "general"));
        assertThat(rowSpec.resolved()
            .connections()).containsExactly(slackConnection(SLACK_CONNECTION_ID));
        assertThat(resolution.missingComponentName()).isNull();
        assertThat(resolution.missingInputName()).isNull();
    }

    @Test
    void testResolveReferenceKeepsTheRowDisabledWhenEnablingWasNotAskedFor() {
        String workflowId = createDraftWorkflow("Resolve Not Asked", SLACK_WORKFLOW_DEFINITION);

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUserId, UNCONFIGURED_PROJECT_ID, UNCONFIGURED_WORKFLOW_UUID, workflowId,
            false, Map.of(), List.of(), Map.of());

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
            connectedUserId, UNCONFIGURED_PROJECT_ID, UNCONFIGURED_WORKFLOW_UUID, workflowId,
            true, Map.of(), List.of(), Map.of());

        assertThat(resolution.rowSpec()
            .enabled()).isFalse();
        assertThat(resolution.missingComponentName()).isEqualTo("slack");
    }

    @Test
    void testResolveReferenceKeepsTheRowDisabledWhenARequiredInputIsMissing() {
        String workflowId = createDraftWorkflow("Resolve Missing Input", SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUserId, UNCONFIGURED_PROJECT_ID, UNCONFIGURED_WORKFLOW_UUID, workflowId,
            true, Map.of(), List.of(), Map.of());

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
    void testGetOrCreateDeploymentKeepsDevelopmentAndProductionConnectedUsersApart() {
        long automationProjectId = createAutomationProject("Deployment Name");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        automationProjectConfigurationService.create(automationProjectId,
            Environment.DEVELOPMENT.ordinal(), "Default", null, 1, true, true);

        ConnectedUser developmentConnectedUser = connectedUserService.createConnectedUser(
            externalUserId, Environment.DEVELOPMENT);

        long productionDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);
        long developmentDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.DEVELOPMENT);

        assertThat(developmentDeploymentId).isNotEqualTo(productionDeploymentId);
        assertThat(projectDeploymentService.getProjectDeployment(productionDeploymentId))
            .satisfies(projectDeployment -> {
                assertThat(projectDeployment.getName()).isEqualTo(externalUserId);
                assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
            });
        assertThat(projectDeploymentService.getProjectDeployment(developmentDeploymentId))
            .satisfies(projectDeployment -> {
                assertThat(projectDeployment.getName()).isEqualTo(externalUserId);
                assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
            });
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUserId, automationProjectId))
                .map(ConnectedUserReferenceDeployment::getProjectDeploymentId)
                .contains(productionDeploymentId);
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            developmentConnectedUser.getId(), automationProjectId))
                .map(ConnectedUserReferenceDeployment::getProjectDeploymentId)
                .contains(developmentDeploymentId);
    }

    @Test
    void testFirstActivationAssignsSelfServeConfiguration() {
        long automationProjectId = createAutomationProject("Self Serve Assignment");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);
        automationProjectFacade.publishProject(automationProjectId);

        automationProjectConfigurationService.create(
            automationProjectId, Environment.PRODUCTION.ordinal(), "Newest", null, 2, true, true);
        automationProjectConfigurationService.create(
            automationProjectId, Environment.PRODUCTION.ordinal(), "Oldest", null, 1, true, true);

        AutomationProjectConfiguration selfServeConfiguration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, Environment.PRODUCTION.ordinal())
            .stream()
            .filter(configuration -> configuration.getProjectVersion() == 2)
            .findFirst()
            .orElseThrow();

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);

        assertThat(
            connectedUserProjectConfigurationService.fetchAssignment(connectedUserId, automationProjectId))
                .map(ConnectedUserProjectConfiguration::getConfigurationId)
                .contains(selfServeConfiguration.getId());
        assertThat(projectDeploymentService.getProjectDeployment(projectDeploymentId)
            .getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testGetOrCreateDeploymentRecordsTheConnectedUserReferenceDeploymentOnce() {
        long automationProjectId = createAutomationProject("Record Relation");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);

        ConnectedUserReferenceDeployment connectedUserReferenceDeployment = connectedUserReferenceDeploymentService
            .fetchConnectedUserReferenceDeployment(connectedUserId, automationProjectId)
            .orElseThrow();

        assertThat(connectedUserReferenceDeployment.getProjectDeploymentId()).isEqualTo(projectDeploymentId);
        assertThat(connectedUserReferenceDeployment.getProjectId()).isEqualTo(automationProjectId);
        assertThat(connectedUserReferenceDeploymentManager.isReferenceDeployment(projectDeploymentId)).isTrue();

        assertThat(connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION)).isEqualTo(projectDeploymentId);
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUserId, automationProjectId))
                .map(ConnectedUserReferenceDeployment::getId)
                .contains(connectedUserReferenceDeployment.getId());
    }

    @Test
    void testGetOrCreateDeploymentFindsTheDeploymentByItsRelationAfterARename() {
        long automationProjectId = createAutomationProject("Renamed Deployment");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        projectDeployment.setName("__EMBEDDED__renamed");

        projectDeploymentService.update(projectDeployment);

        assertThat(connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION)).isEqualTo(projectDeploymentId);
        assertThat(projectDeploymentService.getAllProjectDeployments(automationProjectId))
            .singleElement()
            .extracting(ProjectDeployment::getId)
            .isEqualTo(projectDeploymentId);
    }

    @Test
    void testGetOrCreateDeploymentDoesNotAdoptALegacyNamedDeploymentThatOnlyMatchesByName() {
        long automationProjectId = createAutomationProject("Name Only Deployment");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ProjectDeployment nameOnlyProjectDeployment = new ProjectDeployment();

        nameOnlyProjectDeployment.setEnabled(false);
        nameOnlyProjectDeployment.setEnvironment(Environment.PRODUCTION);
        nameOnlyProjectDeployment.setName("__EMBEDDED__" + externalUserId + "__PRODUCTION");
        nameOnlyProjectDeployment.setProjectId(automationProjectId);
        nameOnlyProjectDeployment.setProjectVersion(1);

        long nameOnlyProjectDeploymentId = projectDeploymentService.create(nameOnlyProjectDeployment)
            .getId();

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);

        assertThat(projectDeploymentId).isNotEqualTo(nameOnlyProjectDeploymentId);
        assertThat(connectedUserReferenceDeploymentManager.isReferenceDeployment(nameOnlyProjectDeploymentId))
            .isFalse();
        assertThat(connectedUserReferenceDeploymentManager.isReferenceDeployment(projectDeploymentId)).isTrue();
    }

    @Test
    void testDeleteDeploymentRemovesTheConnectedUserReferenceDeployment() {
        long automationProjectId = createAutomationProject("Delete Relation");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);

        connectedUserReferenceDeploymentManager.deleteDeployment(projectDeploymentId);

        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUserId, automationProjectId)).isEmpty();
        assertThat(connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION)).isNotEqualTo(projectDeploymentId);
    }

    @Test
    void testGetOrCreateDeploymentCreatesAnEnabledDeploymentAtTheLastPublishedVersion() {
        long automationProjectId = createAutomationProject("Create Deployment");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);
        publishAndPinProject(automationProjectId);
        publishAndPinProject(automationProjectId);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        assertThat(projectDeployment.getProjectVersion()).isEqualTo(3);
        assertThat(projectDeployment.getProjectId()).isEqualTo(automationProjectId);
        assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(projectDeployment.getName()).isEqualTo(externalUserId);
        assertThat(projectDeployment.isEnabled()).isTrue();
    }

    @Test
    void testGetOrCreateDeploymentReusesTheExistingDeployment() {
        long automationProjectId = createAutomationProject("Reuse Deployment");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION)).isEqualTo(projectDeploymentId);
        assertThat(projectDeploymentService.getAllProjectDeployments(automationProjectId))
            .singleElement()
            .extracting(ProjectDeployment::getId)
            .isEqualTo(projectDeploymentId);
    }

    @Test
    void testGetReleasedVersionRejectsAProjectWithoutAConfiguration() {
        long automationProjectId = createAutomationProject("Unreleased");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        assertThatThrownBy(
            () -> connectedUserReferenceDeploymentManager.getReleasedVersion(
                connectedUserId, automationProjectId))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED
                                .getErrorKey()));
    }

    @Test
    void testGetReleasedVersionRejectsADisabledConfiguration() {
        long automationProjectId = createAutomationProject("Disabled Release");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, Environment.PRODUCTION.ordinal())
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.updateEnabled(configuration.getId(), false);

        assertThatThrownBy(
            () -> connectedUserReferenceDeploymentManager.getReleasedVersion(
                connectedUserId, automationProjectId))
                    .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testGetReleasedVersionReturnsThePinnedVersionOfTheEnvironment() {
        long automationProjectId = createAutomationProject("Pinned Release");

        addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            automationProjectId, externalUserId, Environment.PRODUCTION);

        automationProjectFacade.publishProject(automationProjectId);

        assertThat(connectedUserReferenceDeploymentManager.getReleasedVersion(
            connectedUserId, automationProjectId)).isEqualTo(1);
    }

    @Test
    void testPutWorkflowsAtTheSameVersionRewritesAnEnabledRowAloneDisablingItFirst() {
        long automationProjectId = createAutomationProject("Rewrite Enabled Row");

        String siblingUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid =
            addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        provision(siblingUuid);

        long projectDeploymentId = provisionEnabled(workflowUuid, Map.of("channel", "general"));

        String workflowId = getWorkflowId(automationProjectId, 1, workflowUuid);
        String siblingWorkflowId = getWorkflowId(automationProjectId, 1, siblingUuid);

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
        long automationProjectId = createAutomationProject("Rewrite Disabled Row");

        String siblingUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid =
            addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        provision(siblingUuid);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();

        connectedUserReferenceDeploymentManager.updateInputs(
            projectDeploymentId, workflowUuid, Map.of("channel", "general"));

        String workflowId = getWorkflowId(automationProjectId, 1, workflowUuid);
        String siblingWorkflowId = getWorkflowId(automationProjectId, 1, siblingUuid);

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
        long automationProjectId = createAutomationProject("Unchanged Row");

        String workflowUuid = addWorkflow(
            automationProjectId, SLACK_AND_JIRA_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        long projectDeploymentId = provisionEnabled(workflowUuid, Map.of("channel", "general"));

        String workflowId = getWorkflowId(automationProjectId, 1, workflowUuid);

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
        long automationProjectId = createAutomationProject("Create Row");

        String siblingUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow siblingReference = provision(siblingUuid);

        long projectDeploymentId = siblingReference.getProjectDeploymentId();

        String workflowId = getWorkflowId(automationProjectId, 1, workflowUuid);
        String siblingWorkflowId = getWorkflowId(automationProjectId, 1, siblingUuid);

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
        long automationProjectId = createAutomationProject("New Version");

        String otherUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        provision(otherUuid);

        long projectDeploymentId = provisionEnabled(workflowUuid, Map.of("channel", "general"));

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId)).hasSize(2);

        publishAndPinProject(automationProjectId);

        RowSpec rowSpec = new RowSpec(new ResolvedWorkflowConnections(List.of(), List.of()), false, null);

        connectedUserReferenceDeploymentManager.putWorkflows(projectDeploymentId, 2, Map.of(workflowUuid, rowSpec));

        assertThat(projectDeploymentService.getProjectDeployment(projectDeploymentId)
            .getProjectVersion()).isEqualTo(2);
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId))
            .singleElement()
            .satisfies(writtenRow -> {
                assertThat(writtenRow.getWorkflowId())
                    .isEqualTo(getWorkflowId(automationProjectId, 2, workflowUuid));
                assertThat(writtenRow.getInputs()).isEqualTo(Map.of("channel", "general"));
                assertThat(writtenRow.isEnabled()).isFalse();
            });
    }

    @Test
    void testUpdateInputsRefusesAnEnabledRowMissingARequiredInput() {
        long automationProjectId = createAutomationProject("Refuse Inputs");

        String workflowUuid =
            addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        long projectDeploymentId = provisionEnabled(workflowUuid, Map.of("channel", "general"));

        String workflowId = getWorkflowId(automationProjectId, 1, workflowUuid);

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
        long automationProjectId = createAutomationProject("Incomplete Inputs");

        String workflowUuid =
            addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();
        String workflowId = getWorkflowId(automationProjectId, 1, workflowUuid);

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
        long automationProjectId = createAutomationProject("Remove Enabled Row");

        String siblingUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        provision(siblingUuid);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();

        String workflowId = getWorkflowId(automationProjectId, 1, workflowUuid);
        String siblingWorkflowId = getWorkflowId(automationProjectId, 1, siblingUuid);

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
        long automationProjectId = createAutomationProject("Remove Disabled Row");

        String siblingUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);
        String workflowUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        provision(siblingUuid);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        connectedUserWorkflowReferenceFacade.enableReference(
            externalUserId, workflowUuid, false, Environment.PRODUCTION);

        long projectDeploymentId = reference.getProjectDeploymentId();
        String workflowId = getWorkflowId(automationProjectId, 1, workflowUuid);

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
        long automationProjectId = createAutomationProject("Remove Missing Deployment");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

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
        long automationProjectId = createAutomationProject("Remove Last Row");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow reference = provision(workflowUuid);

        long projectDeploymentId = reference.getProjectDeploymentId();
        String workflowId = getWorkflowId(automationProjectId, 1, workflowUuid);

        assertThat(getRow(projectDeploymentId, workflowId).isEnabled()).isTrue();

        clearInvocations(triggerLifecycleFacade);

        connectedUserReferenceDeploymentManager.removeWorkflow(projectDeploymentId, workflowUuid);

        verify(triggerLifecycleFacade).executeTriggerDisable(eq(workflowId), any(), any(), any(), any());

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId)).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(projectDeploymentId)).isEmpty();
    }

    private String addWorkflow(long automationProjectId, String definition) {
        return automationProjectFacade.createProjectWorkflow(automationProjectId, definition, null);
    }

    private long createAutomationProject(String name) {
        return automationProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null);
    }

    private String createDraftWorkflow(String projectName, String definition) {
        long automationProjectId = createAutomationProject(projectName);

        String workflowUuid = addWorkflow(automationProjectId, definition);

        return projectWorkflowService.getLastWorkflowId(workflowUuid);
    }

    private ProjectDeploymentWorkflow getRow(long projectDeploymentId, String workflowId) {
        return projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentId, workflowId);
    }

    private String getWorkflowId(long automationProjectId, int projectVersion, String workflowUuid) {
        return projectWorkflowService.fetchProjectWorkflow(automationProjectId, projectVersion, workflowUuid)
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

    private void publishAndPinProject(long projectId) {
        AutomationProjectConfigurationTestSupport.publishAndPinProject(
            automationProjectFacade, automationProjectConfigurationService, projectId);
    }
}
