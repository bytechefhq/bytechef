/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationRowFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRowFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.CallableAutomationDTO;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
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
import java.util.Optional;
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
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
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
class ConnectedUserMcpAutomationResolverIntTest {

    private static final int PRODUCTION_ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();

    private static final long SLACK_CONNECTION_ID = 777L;

    private static final String SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WORKFLOW_WITH_WORKFLOW_CALL_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[{"name":"newWorkflowCall_1","type":"workflow/v1/newWorkflowCall","parameters":{}}],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

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
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserMcpAutomationResolver connectedUserMcpAutomationResolver;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

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
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

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

        stubEntitledSlackConnection();
    }

    @Test
    void testResolveReturnsDeploymentWorkflow() {
        long automationProjectId = createAutomationProject("Resolver Returns");

        String workflowUuid = addWorkflow(automationProjectId);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configuration = createConfiguration(automationProjectId, "A");

        String externalUserId = createAssignedConnectedUser(automationProjectId, configuration);

        ConnectedUserProjectWorkflow reference = provisionWithInputs(
            externalUserId, workflowUuid, Map.of("channel", "#a"));

        automationProjectFacade.publishProject(automationProjectId);

        String deployedWorkflowId = connectedUserReferenceDeploymentManager.getWorkflowId(
            automationProjectId, 1, workflowUuid);

        assertThat(connectedUserReferenceDeploymentManager.fetchWorkflowId(automationProjectId, 2, workflowUuid))
            .get()
            .isNotEqualTo(deployedWorkflowId);

        Optional<CallableAutomationDTO> callableAutomation = connectedUserMcpAutomationResolver
            .fetchCallableAutomation(getConnectedUserId(externalUserId), configuration.getId(), workflowUuid);

        assertThat(callableAutomation).contains(
            new CallableAutomationDTO(
                reference.getId(), reference.getProjectDeploymentId(), deployedWorkflowId, Map.of("channel", "#a"),
                null, null));
    }

    @Test
    void testResolveReportsNotToolCallableWithoutWorkflowCallTrigger() {
        Fixture fixture = provisionFixture("Resolver Not Tool Callable");

        assertThat(fetchCallableAutomation(fixture))
            .get()
            .satisfies(callableAutomation -> {
                assertThat(callableAutomation.workflowCallTriggerName()).isNull();
                assertThat(callableAutomation.isToolCallable()).isFalse();
            });
    }

    @Test
    void testResolveReportsToolCallableWithWorkflowCallTrigger() {
        Fixture fixture = provisionFixture("Resolver Tool Callable", SLACK_WORKFLOW_WITH_WORKFLOW_CALL_DEFINITION);

        assertThat(fetchCallableAutomation(fixture))
            .get()
            .satisfies(callableAutomation -> {
                assertThat(callableAutomation.workflowCallTriggerName()).isEqualTo("newWorkflowCall_1");
                assertThat(callableAutomation.isToolCallable()).isTrue();
            });
    }

    @Test
    void testResolveEmptyWhenConfigurationDisabled() {
        Fixture fixture = provisionFixture("Resolver Configuration Disabled");

        automationProjectConfigurationService.updateEnabled(fixture.configurationId(), false);

        assertThat(fetchCallableAutomation(fixture)).isEmpty();
    }

    @Test
    void testResolveEmptyWhenConfigurationWorkflowDisabledButDeploymentStillEnabled() {
        Fixture fixture = provisionFixture("Resolver Configuration Workflow Disabled");

        automationProjectConfigurationService.updateWorkflowEnabled(
            fixture.configurationId(), UUID.fromString(fixture.workflowUuid()), false);

        assertThat(connectedUserReferenceDeploymentManager.fetchRow(
            fixture.reference()
                .getProjectDeploymentId(),
            fixture.workflowUuid()))
                .get()
                .satisfies(row -> assertThat(row.isEnabled()).isTrue());

        assertThat(fetchCallableAutomation(fixture)).isEmpty();
    }

    @Test
    void testResolveEmptyWhenUserAssignedElsewhere() {
        Fixture fixture = provisionFixture("Resolver Assigned Elsewhere");

        AutomationProjectConfiguration otherConfiguration = createConfiguration(fixture.automationProjectId(), "B");

        assertThat(connectedUserMcpAutomationResolver.fetchCallableAutomation(
            fixture.connectedUserId(), otherConfiguration.getId(), fixture.workflowUuid())).isEmpty();
        assertThat(connectedUserMcpAutomationResolver.fetchConnectedUserProjectWorkflowId(
            fixture.connectedUserId(), otherConfiguration.getId(), fixture.workflowUuid())).isEmpty();
        assertThat(fetchCallableAutomation(fixture)).isPresent();
    }

    @Test
    void testResolveEmptyWhenReferenceDangling() {
        Fixture fixture = provisionFixture("Resolver Dangling");

        markDangling(fixture.reference());

        assertThat(fetchCallableAutomation(fixture)).isEmpty();
    }

    @Test
    void testResolveEmptyWhenNoDeploymentWorkflow() {
        Fixture fixture = provisionFixture("Resolver No Deployment Workflow");

        ProjectDeploymentWorkflow row = connectedUserReferenceDeploymentManager.fetchRow(
            fixture.reference()
                .getProjectDeploymentId(),
            fixture.workflowUuid())
            .orElseThrow();

        projectDeploymentWorkflowService.delete(row.getId());

        assertThat(fetchCallableAutomation(fixture)).isEmpty();
    }

    @Test
    void testResolveReturnsAutomationWhenDeploymentWorkflowDisabled() {
        Fixture fixture = provisionFixture("Resolver Deployment Workflow Disabled");

        connectedUserWorkflowReferenceFacade.enableReference(
            fixture.externalUserId(), fixture.workflowUuid(), false, Environment.PRODUCTION);

        assertThat(connectedUserReferenceDeploymentManager.fetchRow(
            fixture.reference()
                .getProjectDeploymentId(),
            fixture.workflowUuid()))
                .get()
                .satisfies(row -> assertThat(row.isEnabled()).isFalse());

        assertThat(fetchCallableAutomation(fixture))
            .get()
            .satisfies(callableAutomation -> {
                assertThat(callableAutomation.connectedUserProjectWorkflowId()).isEqualTo(
                    fixture.reference()
                        .getId());
                assertThat(callableAutomation.missingConnectionComponentName()).isNull();
            });
    }

    @Test
    void testResolveReportsMissingConnection() {
        long automationProjectId = createAutomationProject("Resolver Missing Connection");

        String workflowUuid = addWorkflow(automationProjectId);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configuration = createConfiguration(automationProjectId, "A");

        String externalUserId = createAssignedConnectedUser(automationProjectId, configuration);

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class);

        long connectedUserId = getConnectedUserId(externalUserId);

        assertThat(connectedUserMcpAutomationResolver.fetchCallableAutomation(
            connectedUserId, configuration.getId(), workflowUuid))
                .get()
                .satisfies(callableAutomation -> assertThat(callableAutomation.missingConnectionComponentName())
                    .isEqualTo("slack"));
    }

    @Test
    void testResolveOnlyCallersOwnDeployment() {
        long automationProjectId = createAutomationProject("Resolver Own Deployment");

        String workflowUuid = addWorkflow(automationProjectId);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configuration = createConfiguration(automationProjectId, "A");

        String firstExternalUserId = createAssignedConnectedUser(automationProjectId, configuration);
        String secondExternalUserId = createAssignedConnectedUser(automationProjectId, configuration);

        ConnectedUserProjectWorkflow firstReference = provisionWithInputs(
            firstExternalUserId, workflowUuid, Map.of("channel", "#first"));
        ConnectedUserProjectWorkflow secondReference = provisionWithInputs(
            secondExternalUserId, workflowUuid, Map.of("channel", "#second"));

        assertThat(firstReference.getProjectDeploymentId()).isNotEqualTo(secondReference.getProjectDeploymentId());

        CallableAutomationDTO firstCallableAutomation = connectedUserMcpAutomationResolver
            .fetchCallableAutomation(getConnectedUserId(firstExternalUserId), configuration.getId(), workflowUuid)
            .orElseThrow();
        CallableAutomationDTO secondCallableAutomation = connectedUserMcpAutomationResolver
            .fetchCallableAutomation(getConnectedUserId(secondExternalUserId), configuration.getId(), workflowUuid)
            .orElseThrow();

        assertThat(firstCallableAutomation.connectedUserProjectWorkflowId()).isEqualTo(firstReference.getId());
        assertThat(firstCallableAutomation.projectDeploymentId()).isEqualTo(firstReference.getProjectDeploymentId());
        assertThat(firstCallableAutomation.inputs()).isEqualTo(Map.of("channel", "#first"));
        assertThat(secondCallableAutomation.connectedUserProjectWorkflowId()).isEqualTo(secondReference.getId());
        assertThat(secondCallableAutomation.projectDeploymentId()).isEqualTo(
            secondReference.getProjectDeploymentId());
        assertThat(secondCallableAutomation.inputs()).isEqualTo(Map.of("channel", "#second"));
    }

    @Test
    void testFetchConnectedUserProjectWorkflowIdIgnoresOnOff() {
        Fixture fixture = provisionFixture("Resolver Reference Id");

        automationProjectConfigurationService.updateEnabled(fixture.configurationId(), false);

        connectedUserWorkflowReferenceFacade.enableReference(
            fixture.externalUserId(), fixture.workflowUuid(), false, Environment.PRODUCTION);

        assertThat(connectedUserMcpAutomationResolver.fetchConnectedUserProjectWorkflowId(
            fixture.connectedUserId(), fixture.configurationId(), fixture.workflowUuid()))
                .contains(fixture.reference()
                    .getId());
        assertThat(connectedUserMcpAutomationResolver.fetchConnectedUserProjectWorkflowId(
            fixture.connectedUserId(), fixture.configurationId(), UUID.randomUUID()
                .toString())).isEmpty();
    }

    private String addWorkflow(long automationProjectId) {
        return addWorkflow(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);
    }

    private String addWorkflow(long automationProjectId, String definition) {
        return automationProjectFacade.createProjectWorkflow(automationProjectId, definition, null);
    }

    private String createAssignedConnectedUser(
        long automationProjectId, AutomationProjectConfiguration configuration) {

        String externalUserId = "mcp-automation-resolver-user-" + UUID.randomUUID();

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        automationProjectConfigurationResolver.assign(connectedUser, automationProjectId, configuration.getId());

        return externalUserId;
    }

    private long createAutomationProject(String name) {
        return automationProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null);
    }

    private AutomationProjectConfiguration createConfiguration(long automationProjectId, String name) {
        return automationProjectConfigurationService.create(
            automationProjectId, PRODUCTION_ENVIRONMENT_ID, name, null, 1, true, true);
    }

    private Optional<CallableAutomationDTO> fetchCallableAutomation(Fixture fixture) {
        return connectedUserMcpAutomationResolver.fetchCallableAutomation(
            fixture.connectedUserId(), fixture.configurationId(), fixture.workflowUuid());
    }

    private long getConnectedUserId(String externalUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, Environment.PRODUCTION);

        return connectedUser.getId();
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

    private Fixture provisionFixture(String name) {
        return provisionFixture(name, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);
    }

    private Fixture provisionFixture(String name, String definition) {
        long automationProjectId = createAutomationProject(name);

        String workflowUuid = addWorkflow(automationProjectId, definition);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configuration = createConfiguration(automationProjectId, "A");

        String externalUserId = createAssignedConnectedUser(automationProjectId, configuration);

        ConnectedUserProjectWorkflow reference = provisionWithInputs(
            externalUserId, workflowUuid, Map.of("channel", "#a"));

        return new Fixture(
            automationProjectId, configuration.getId(), getConnectedUserId(externalUserId), externalUserId,
            reference, workflowUuid);
    }

    private ConnectedUserProjectWorkflow provisionWithInputs(
        String externalUserId, String workflowUuid, Map<String, ?> inputs) {

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION);

        connectedUserReferenceDeploymentManager.updateInputs(reference.getProjectDeploymentId(), workflowUuid, inputs);

        connectedUserWorkflowReferenceFacade.enableReference(
            externalUserId, workflowUuid, true, Environment.PRODUCTION);

        return connectedUserProjectWorkflowRepository.findById(reference.getId())
            .orElseThrow();
    }

    private void stubEntitledSlackConnection() {
        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(PRODUCTION_ENVIRONMENT_ID);
        connection.setId(SLACK_CONNECTION_ID);

        ComponentConnection slot = new ComponentConnection("slack", 1, "postMessage1", "slack", true);

        when(connectionDTO.id()).thenReturn(SLACK_CONNECTION_ID);
        when(connectionDTO.componentName()).thenReturn("slack");
        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of(connectionDTO));
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class))).thenReturn(List.of(slot));
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("postMessage1"), eq("slack")))
            .thenReturn(slot);
        when(connectionService.getConnection(SLACK_CONNECTION_ID)).thenReturn(connection);
    }

    private record Fixture(
        long automationProjectId, long configurationId, long connectedUserId, String externalUserId,
        ConnectedUserProjectWorkflow reference, String workflowUuid) {
    }
}
