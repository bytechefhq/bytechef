/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationRowFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRowFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionReader;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ConnectedUserConnectionFacade.class,
    ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, EmbeddedPermissionEvaluator.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpIntegrationInstanceWorkflowService.class, McpAutomationRowFacade.class, McpIntegrationInstanceRowFacade.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class AutomationProjectConfigurationMoveWorkflowInputsIntTest {

    private static final long PRODUCTION_ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();

    private static final String SLACK_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WORKFLOW_WITH_INTERNAL_ONLY_CHANNEL_INPUT_DEFINITION = """
        {"label":"Post","inputs":[
         {"name":"channel","label":"Channel","type":"string","required":true,"internalOnly":true}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WORKFLOW_WITH_OPTIONAL_CHANNEL_INPUT_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":false}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WORKFLOW_WITH_TYPED_INPUTS_DEFINITION = """
        {"label":"Post","inputs":[
         {"name":"notify","label":"Notify","type":"boolean","required":true},
         {"name":"limit","label":"Limit","type":"integer","required":true},
         {"name":"ratio","label":"Ratio","type":"number","required":true},
         {"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    @Autowired
    private AutomationProjectConfigurationFacade automationProjectConfigurationFacade;

    @Autowired
    private AutomationProjectConfigurationResolver automationProjectConfigurationResolver;

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
    private ConnectedUserConnectionReader connectedUserConnectionReader;

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
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
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
    void testGetMoveImpactLeavesOutANewRequiredInputTheTargetProvides() {
        long automationProjectId = createAutomationProject("Move Impact Target Provided Input");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId, null);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser connectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_INTERNAL_ONLY_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        automationProjectFacade.publishProject(automationProjectId, null);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        automationProjectConfigurationFacade.updateConfigurationWorkflow(
            targetConfiguration.getId(), workflowUuid, Map.of(), Map.of("channel", "#target"));

        assertThat(reference.getId()).isNotNull();
        assertThat(automationProjectConfigurationFacade.getMoveImpact(
            sourceConfiguration.getId(), targetConfiguration.getId(), List.of(connectedUser.getId()))
            .workflows()).isEmpty();
    }

    @Test
    void testMoveFillsANewRequiredInputWithTheGivenValue() {
        long automationProjectId = createAutomationProject("Move Fills New Required Input");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId, null);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser connectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        automationProjectFacade.publishProject(automationProjectId, null);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(), List.of(connectedUser.getId()),
            Map.of(workflowUuid, Map.of("channel", "#mine")));

        assertThat(moveResult.movedConnectedUserIds()).containsExactly(connectedUser.getId());
        assertThat(moveResult.needsAttentionConnectedUserIds()).isEmpty();
        assertThat(fetchDeploymentRow(reference))
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#mine"));
            });
    }

    @Test
    void testMoveSavesTheGivenInputValuesWithTheirDeclaredTypes() {
        long automationProjectId = createAutomationProject("Move Typed Inputs");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId, null);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser connectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_TYPED_INPUTS_DEFINITION, draftWorkflow.getVersion());

        automationProjectFacade.publishProject(automationProjectId, null);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        automationProjectConfigurationFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(), List.of(connectedUser.getId()),
            Map.of(workflowUuid, Map.of("notify", "false", "limit", "5", "ratio", "0.5", "channel", "42")));

        Map<String, ?> inputs = fetchDeploymentRow(reference).getInputs();

        assertThat(inputs.get("notify")).isEqualTo(false);
        assertThat(inputs.get("limit")).isInstanceOf(Number.class)
            .satisfies(limit -> assertThat(((Number) limit).longValue()).isEqualTo(5L));
        assertThat(inputs.get("ratio")).isInstanceOf(Number.class)
            .satisfies(ratio -> assertThat(((Number) ratio).doubleValue()).isEqualTo(0.5));
        assertThat(inputs.get("channel")).isEqualTo("42");
    }

    @Test
    void testMoveKeepsTheInputValueAUserAlreadyHasAndFillsOnlyTheMissingOne() {
        long automationProjectId = createAutomationProject("Move Keeps Existing Input");

        String workflowUuid = addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_OPTIONAL_CHANNEL_INPUT_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId, null);

        AutomationProjectConfiguration sourceConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Source", null, 1, true, true);

        stubEntitledSlackConnection(777L);

        ConnectedUser ownValueConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);
        ConnectedUser missingValueConnectedUser = assignNewConnectedUser(automationProjectId, sourceConfiguration);

        ConnectedUserProjectWorkflow ownValueReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            ownValueConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow missingValueReference =
            connectedUserWorkflowReferenceFacade.getOrCreateReference(
                missingValueConnectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            ownValueConnectedUser.getExternalId(), workflowUuid, Map.of("channel", "#own"), Environment.PRODUCTION);

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        automationProjectFacade.publishProject(automationProjectId, null);

        AutomationProjectConfiguration targetConfiguration = automationProjectConfigurationService
            .create(automationProjectId, PRODUCTION_ENVIRONMENT_ID, "Target", null, 2, true, true);

        ConfigurationMoveResultDTO moveResult = automationProjectConfigurationFacade.moveConnectedUsers(
            sourceConfiguration.getId(), targetConfiguration.getId(),
            List.of(ownValueConnectedUser.getId(), missingValueConnectedUser.getId()),
            Map.of(workflowUuid, Map.of("channel", "#batch")));

        assertThat(moveResult.movedConnectedUserIds())
            .containsExactlyInAnyOrder(ownValueConnectedUser.getId(), missingValueConnectedUser.getId());
        assertThat(fetchDeploymentRow(ownValueReference).getInputs()).isEqualTo(Map.of("channel", "#own"));
        assertThat(fetchDeploymentRow(missingValueReference).getInputs()).isEqualTo(Map.of("channel", "#batch"));
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

    private ProjectDeploymentWorkflow fetchDeploymentRow(ConnectedUserProjectWorkflow reference) {
        return connectedUserReferenceDeploymentManager
            .fetchRow(reference.getProjectDeploymentId(), reference.getAutomationUuid())
            .orElseThrow();
    }

    private ConnectedUser assignNewConnectedUser(
        long automationProjectId, AutomationProjectConfiguration configuration) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            createConnectedUser(), Environment.PRODUCTION);

        automationProjectConfigurationResolver.assign(
            connectedUser, automationProjectId, configuration.getId());

        return connectedUser;
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
        when(connectedUserConnectionReader.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of(connectionDTO));
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class))).thenReturn(List.of(slot));
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("postMessage1"), eq("slack")))
            .thenReturn(slot);
        when(connectionService.getConnection(connectionId)).thenReturn(connection);
    }
}
