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
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceAttentionResolver.ReferenceState;
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
class ConnectedUserReferenceAttentionResolverIntTest {

    private static final long SLACK_CONNECTION_ID = 777L;

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

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserReferenceAttentionResolver connectedUserReferenceAttentionResolver;

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

    private String externalUserId;

    @BeforeEach
    void setUp() {
        externalUserId = "attention-resolver-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

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
    void testDanglingReferenceHasNoInputsAndNoAttentionReason() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Attention Dangling");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = provisionWithInputs(workflowUuid, Map.of("channel", "#a"));

        markDangling(reference);

        ConnectedUserProjectWorkflow danglingReference = reload(reference);

        assertThat(danglingReference.isDangling()).isTrue();
        assertThat(connectedUserReferenceDeploymentManager.getInputs(
            danglingReference.getProjectDeploymentId(), workflowUuid)).isEqualTo(Map.of("channel", "#a"));

        ReferenceState referenceState = connectedUserReferenceAttentionResolver.resolve(danglingReference);

        assertThat(referenceState.inputs()).isEmpty();
        assertThat(referenceState.attentionReason()).isNull();
    }

    @Test
    void testDeploymentBehindPublishedVersionIsUpdatePendingAndKeepsItsInputs() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Attention Update Pending");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = provisionWithInputs(workflowUuid, Map.of("channel", "#a"));

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ReferenceState referenceState = connectedUserReferenceAttentionResolver.resolve(reload(reference));

        assertThat(referenceState.attentionReason()).isEqualTo("UPDATE_PENDING");
        assertThat(referenceState.inputs()).isEqualTo(Map.of("channel", "#a"));
    }

    @Test
    void testRequiredSlotWithoutConnectionIsMissingConnection() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Attention Missing Connection");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of());

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class);

        ReferenceState referenceState = connectedUserReferenceAttentionResolver.resolve(findReference(workflowUuid));

        assertThat(referenceState.attentionReason()).isEqualTo("MISSING_CONNECTION:slack");
    }

    @Test
    void testMissingRequiredInputIsInputRequired() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Attention Missing Input");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION);

        assertThat(connectedUserReferenceDeploymentManager.fetchRow(reference.getProjectDeploymentId(), workflowUuid))
            .get()
            .satisfies(row -> assertThat(row.getConnections()).isNotEmpty());

        ReferenceState referenceState = connectedUserReferenceAttentionResolver.resolve(reload(reference));

        assertThat(referenceState.attentionReason()).isEqualTo("INPUT_REQUIRED:channel");
    }

    @Test
    void testMissingDeploymentHasNoInputsAndNoAttentionReason() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Attention Missing Deployment");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = provisionWithInputs(workflowUuid, Map.of("channel", "#a"));

        connectedUserReferenceDeploymentManager.deleteDeployment(reference.getProjectDeploymentId());

        ReferenceState referenceState = connectedUserReferenceAttentionResolver.resolve(reload(reference));

        assertThat(referenceState.inputs()).isEmpty();
        assertThat(referenceState.attentionReason()).isNull();
    }

    @Test
    void testCompleteReferenceHasNoAttentionReason() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Attention Complete");

        String workflowUuid = addWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = provisionWithInputs(workflowUuid, Map.of("channel", "#a"));

        ReferenceState referenceState = connectedUserReferenceAttentionResolver.resolve(reload(reference));

        assertThat(referenceState.attentionReason()).isNull();
        assertThat(referenceState.inputs()).isEqualTo(Map.of("channel", "#a"));
    }

    private String addWorkflow(long automationWorkflowProjectId, String definition) {
        return automationWorkflowProjectFacade.createProjectWorkflow(automationWorkflowProjectId, definition, null);
    }

    private long createAutomationWorkflowProject(String name) {
        return automationWorkflowProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null, null);
    }

    private ConnectedUserProjectWorkflow findReference(String workflowUuid) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, Environment.PRODUCTION);

        return connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())
            .stream()
            .filter(reference -> Objects.equals(reference.getAutomationWorkflowUuid(), workflowUuid))
            .findFirst()
            .orElseThrow();
    }

    private ConnectedUserProjectWorkflow provisionWithInputs(String workflowUuid, Map<String, ?> inputs) {
        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION);

        connectedUserReferenceDeploymentManager.updateInputs(reference.getProjectDeploymentId(), workflowUuid, inputs);

        connectedUserWorkflowReferenceFacade.enableReference(
            externalUserId, workflowUuid, true, Environment.PRODUCTION);

        return reload(reference);
    }

    private ConnectedUserProjectWorkflow reload(ConnectedUserProjectWorkflow reference) {
        return connectedUserProjectWorkflowRepository.findById(reference.getId())
            .orElseThrow();
    }

    private void stubEntitledSlackConnection() {
        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
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
