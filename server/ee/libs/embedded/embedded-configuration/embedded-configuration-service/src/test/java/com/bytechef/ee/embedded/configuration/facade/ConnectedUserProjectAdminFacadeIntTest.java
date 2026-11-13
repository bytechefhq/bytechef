/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRows;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.evaluator.SpelEvaluator;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationProjectFacadeIntTestConfiguration.class,
        ConnectedUserProjectAdminFacadeIntTest.ConnectedUserServiceConfiguration.class
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
    PrincipalJobFacade.class, PrincipalJobService.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserProjectAdminFacadeIntTest {

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    private static final AtomicLong NEXT_CONNECTION_ID = new AtomicLong(1000L);

    private static final String CHANNEL_INPUT_WORKFLOW_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String NO_CONNECTION_WORKFLOW_DEFINITION = """
        {"label":"Standalone","triggers":[],"tasks":[]}
        """;

    private static final String SLACK_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
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

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;

    @MockitoSpyBean
    private ConnectedUserProjectService connectedUserProjectService;

    @MockitoSpyBean
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserProjectAdminFacade connectedUserProjectAdminFacade;

    @Autowired
    private AutomationProjectConfigurationAdminFacade automationProjectConfigurationAdminFacade;

    @MockitoSpyBean
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    private final Map<Long, Connection> embeddedConnections = new ConcurrentHashMap<>();

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private PrincipalJobService principalJobService;

    @Test
    void testGetProvisionableProjectsListsReleasedProjectsWithTheirEnabledWorkflows() {
        givenSlackAutomationEnvironment();

        long projectId = createProjectWithWorkflows("Provisionable Listed", 2);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        AutomationProjectDTO publishedProject = findPublishedProject(connectedUser, projectId);

        List<ProvisionableAutomationProjectDTO> result =
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId());

        ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(result, projectId);

        assertThat(provisionableProject.name()).isEqualTo(publishedProject.name());
        assertThat(provisionableProject.projectVersion()).isEqualTo(publishedProject.lastPublishedVersion());
        assertThat(provisionableProject.workflows())
            .extracting(
                ProvisionableAutomationProjectDTO.Workflow::workflowUuid,
                ProvisionableAutomationProjectDTO.Workflow::label,
                ProvisionableAutomationProjectDTO.Workflow::inputs,
                ProvisionableAutomationProjectDTO.Workflow::provisioned,
                ProvisionableAutomationProjectDTO.Workflow::dangling)
            .containsExactlyInAnyOrderElementsOf(
                publishedProject.workflowTemplates()
                    .stream()
                    .map(template -> tuple(
                        template.workflowUuid(), template.label(), template.inputs(), false, false))
                    .toList());
        assertThat(provisionableProject.workflows()).hasSize(2);
    }

    @Test
    void testGetProvisionableProjectsSkipsProjectsWithoutConfigurationInTheUserEnvironment() {
        givenSlackAutomationEnvironment();

        long unconfiguredProjectId = createProjectWithWorkflows("Provisionable Unconfigured", 1);

        automationProjectFacade.publishProject(unconfiguredProjectId);

        long configuredProjectId = createProjectWithWorkflows("Provisionable Configured", 1);

        publishAndPinProject(configuredProjectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        List<ProvisionableAutomationProjectDTO> result =
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId());

        assertThat(result)
            .extracting(ProvisionableAutomationProjectDTO::projectId)
            .contains(configuredProjectId)
            .doesNotContain(unconfiguredProjectId);
    }

    @Test
    void testGetProvisionableProjectsSkipsWorkflowsTurnedOffInTheConfiguration() {
        givenSlackAutomationEnvironment();

        long projectId = createProjectWithWorkflows("Provisionable Turned Off", 2);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
            .workflowTemplates();

        String turnedOffWorkflowUuid = templates.getFirst()
            .workflowUuid();
        String remainingWorkflowUuid = templates.getLast()
            .workflowUuid();

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(projectId, Environment.PRODUCTION.ordinal())
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationAdminFacade.enableConfigurationWorkflow(
            configuration.getId(), turnedOffWorkflowUuid, false);

        ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId);

        assertThat(provisionableProject.workflows())
            .extracting(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
            .containsExactly(remainingWorkflowUuid);
    }

    @Test
    void testGetProvisionableProjectsMarksProvisionedAndDanglingWorkflows() {
        givenSlackAutomationEnvironment();

        long projectId = createProjectWithWorkflows("Provisionable Marked", 3);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
            .workflowTemplates();

        String provisionedWorkflowUuid = templates.get(0)
            .workflowUuid();
        String danglingWorkflowUuid = templates.get(1)
            .workflowUuid();
        String untouchedWorkflowUuid = templates.get(2)
            .workflowUuid();

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), provisionedWorkflowUuid, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow danglingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), danglingWorkflowUuid, Environment.PRODUCTION);

        markDangling(danglingReference);

        ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId);

        assertThat(provisionableProject.workflows())
            .extracting(
                ProvisionableAutomationProjectDTO.Workflow::workflowUuid,
                ProvisionableAutomationProjectDTO.Workflow::provisioned,
                ProvisionableAutomationProjectDTO.Workflow::dangling)
            .containsExactlyInAnyOrder(
                tuple(provisionedWorkflowUuid, true, false),
                tuple(danglingWorkflowUuid, true, true),
                tuple(untouchedWorkflowUuid, false, false));
    }

    @Test
    void testProvisionProjectWorkflowsCreatesReferencesLikeThePublicPath() {
        givenSlackAutomationEnvironment();

        long projectId = createProjectWithWorkflows("Provision Created", 2);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();
        ConnectedUser directConnectedUser = createConnectedUserWithConnection();

        List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
            .workflowTemplates();

        String firstWorkflowUuid = templates.getFirst()
            .workflowUuid();
        String secondWorkflowUuid = templates.getLast()
            .workflowUuid();

        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), getConfigurationId(projectId),
            List.of(
                new ProvisionConnectedUserProjectWorkflowDTO(firstWorkflowUuid, null),
                new ProvisionConnectedUserProjectWorkflowDTO(secondWorkflowUuid, null)));

        ConnectedUserProjectWorkflow directReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            directConnectedUser.getExternalId(), firstWorkflowUuid, Environment.PRODUCTION);

        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
            .extracting(
                ConnectedUserProjectWorkflow::getAutomationUuid, ConnectedUserProjectWorkflow::isEnabled,
                ConnectedUserProjectWorkflow::isDangling,
                reference -> reference.getProjectDeploymentId() != null)
            .containsExactlyInAnyOrder(
                tuple(firstWorkflowUuid, true, false, directReference.getProjectDeploymentId() != null),
                tuple(secondWorkflowUuid, true, false, directReference.getProjectDeploymentId() != null));
    }

    @Test
    void testProvisionProjectWorkflowsRejectsUnprovisionableUuidBeforeWriting() {
        givenSlackAutomationEnvironment();

        long projectId = createProjectWithWorkflows("Provision Rejected", 2);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
            .workflowTemplates();

        String validWorkflowUuid = templates.getFirst()
            .workflowUuid();
        String turnedOffWorkflowUuid = templates.getLast()
            .workflowUuid();

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(projectId, Environment.PRODUCTION.ordinal())
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationAdminFacade.enableConfigurationWorkflow(
            configuration.getId(), turnedOffWorkflowUuid, false);

        assertThatThrownBy(() -> connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), configuration.getId(),
            List.of(
                new ProvisionConnectedUserProjectWorkflowDTO(validWorkflowUuid, null),
                new ProvisionConnectedUserProjectWorkflowDTO(turnedOffWorkflowUuid, null))))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE
                                .getErrorKey()));

        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();
        assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
            .isEmpty();
    }

    @Test
    void testProvisionProjectWorkflowsKeepsEarlierReferencesWhenALaterWorkflowFails() {
        givenSlackAutomationEnvironment();

        long projectId = createProjectWithWorkflows("Provision Partial", 2);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
            .workflowTemplates();

        String firstWorkflowUuid = templates.getFirst()
            .workflowUuid();
        String failingWorkflowUuid = templates.getLast()
            .workflowUuid();

        doThrow(new IllegalStateException("Trigger registration failed"))
            .when(connectedUserWorkflowReferenceFacade)
            .getOrCreateReference(
                anyString(), eq(failingWorkflowUuid), any(Environment.class), anyMap(), any());

        assertThatThrownBy(() -> connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), getConfigurationId(projectId),
            List.of(
                new ProvisionConnectedUserProjectWorkflowDTO(firstWorkflowUuid, null),
                new ProvisionConnectedUserProjectWorkflowDTO(failingWorkflowUuid, null))))
                    .isInstanceOf(IllegalStateException.class);

        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
            .extracting(ConnectedUserProjectWorkflow::getAutomationUuid,
                ConnectedUserProjectWorkflow::isEnabled)
            .containsExactly(tuple(firstWorkflowUuid, true));
    }

    @Test
    void testProvisionProjectWorkflowsKeepsReferenceWhenConnectionIsMissing() {
        givenSlackAutomationEnvironment();

        long needsConnectionProjectId =
            createProjectWithDefinition("Provision Needs Connection", SLACK_WORKFLOW_DEFINITION);
        long standaloneProjectId = createProjectWithDefinition(
            "Provision Standalone", NO_CONNECTION_WORKFLOW_DEFINITION);

        publishAndPinProject(needsConnectionProjectId);
        publishAndPinProject(standaloneProjectId);

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "provisionable-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        String needsConnectionWorkflowUuid = findPublishedProject(connectedUser, needsConnectionProjectId)
            .workflowTemplates()
            .getFirst()
            .workflowUuid();
        String standaloneWorkflowUuid = findPublishedProject(connectedUser, standaloneProjectId)
            .workflowTemplates()
            .getFirst()
            .workflowUuid();

        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), getConfigurationId(needsConnectionProjectId),
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(needsConnectionWorkflowUuid, null)));
        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), getConfigurationId(standaloneProjectId),
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(standaloneWorkflowUuid, null)));

        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
            .extracting(ConnectedUserProjectWorkflow::getAutomationUuid)
            .containsExactlyInAnyOrder(needsConnectionWorkflowUuid, standaloneWorkflowUuid);

        ConnectedUserProjectWorkflowDTO needsConnectionWorkflow = findProjectWorkflow(
            connectedUser, needsConnectionWorkflowUuid);

        assertThat(needsConnectionWorkflow.attentionReason())
            .isEqualTo(ConnectedUserReferenceAttentionResolver.MISSING_CONNECTION_PREFIX + "slack");
    }

    @Test
    void testGetProvisionableProjectsListsTheComponentsAWorkflowNeedsAConnectionFor() {
        givenSlackAutomationEnvironment();

        ComponentDefinition slackComponentDefinition = mock(ComponentDefinition.class);

        when(slackComponentDefinition.getName()).thenReturn("slack");
        when(slackComponentDefinition.getTitle()).thenReturn("Slack");
        when(slackComponentDefinition.isConnectionRequired()).thenReturn(true);
        when(componentDefinitionService.fetchComponentDefinition(eq("slack"), anyInt()))
            .thenReturn(Optional.of(slackComponentDefinition));

        long projectId = createProjectWithDefinition("Provisionable Components", SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId);

        assertThat(provisionableProject.workflows())
            .singleElement()
            .satisfies(workflow -> assertThat(workflow.components())
                .extracting(ConnectedUserWorkflowTemplateDTO.Component::name,
                    ConnectedUserWorkflowTemplateDTO.Component::title)
                .containsExactly(tuple("slack", "Slack")));
    }

    @Test
    void testGetProvisionableProjectsOmitsTheConnectionsAndInputsTheConfigurationProvides() {
        givenSlackAutomationEnvironment();

        ComponentDefinition slackComponentDefinition = mock(ComponentDefinition.class);

        when(slackComponentDefinition.getName()).thenReturn("slack");
        when(slackComponentDefinition.getTitle()).thenReturn("Slack");
        when(slackComponentDefinition.isConnectionRequired()).thenReturn(true);
        when(componentDefinitionService.fetchComponentDefinition(eq("slack"), anyInt()))
            .thenReturn(Optional.of(slackComponentDefinition));

        long projectId = createProjectWithDefinition("Provisionable Provided", CHANNEL_INPUT_WORKFLOW_DEFINITION);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        long administratorConnectionId = NEXT_CONNECTION_ID.getAndIncrement();
        Connection administratorConnection = new Connection();

        administratorConnection.setComponentName("slack");
        administratorConnection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        administratorConnection.setId(administratorConnectionId);
        administratorConnection.setType(PlatformType.EMBEDDED);

        when(connectionService.fetchConnection(administratorConnectionId))
            .thenReturn(Optional.of(administratorConnection));

        String workflowUuid = findPublishedProject(connectedUser, projectId).workflowTemplates()
            .getFirst()
            .workflowUuid();

        automationProjectConfigurationAdminFacade.updateConfigurationWorkflow(
            getConfigurationId(projectId), workflowUuid, Map.of("slack", administratorConnectionId),
            Map.of("channel", "#provided"));

        ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId);

        assertThat(provisionableProject.workflows())
            .singleElement()
            .satisfies(workflow -> {
                assertThat(workflow.components()).isEmpty();
                assertThat(workflow.inputs()).isEmpty();
            });
    }

    @Test
    void testProvisionProjectWorkflowsWiresTheConnectionChosenForAComponent() {
        givenSlackAutomationEnvironment();

        long projectId = createProjectWithDefinition("Provision Chosen Connection", SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        long connectedUserId = Objects.requireNonNull(connectedUser.getId());
        long chosenConnectionId = connectedUserConnectionService.getConnectionIds(connectedUserId)
            .getFirst();
        long newerConnectionId = NEXT_CONNECTION_ID.getAndIncrement();

        registerEmbeddedConnection(newerConnectionId, Environment.PRODUCTION);

        connectedUserConnectionService.create(connectedUserId, newerConnectionId);

        String workflowUuid = findPublishedProject(connectedUser, projectId).workflowTemplates()
            .getFirst()
            .workflowUuid();

        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUserId, getConfigurationId(projectId),
            List.of(
                new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null, Map.of("slack", chosenConnectionId))));

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade
            .getConnectedUserWorkflows(connectedUserId)
            .stream()
            .filter(
                connectedUserProjectWorkflow -> workflowUuid.equals(connectedUserProjectWorkflow.getAutomationUuid()))
            .findFirst()
            .orElseThrow();

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
            Objects.requireNonNull(reference.getProjectDeploymentId())))
                .flatExtracting(ProjectDeploymentWorkflow::getConnections)
                .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                .containsExactly(chosenConnectionId);
    }

    @Test
    void testProvisionProjectWorkflowsPassesInputsThroughAndLeavesThemAloneWhenOmitted() {
        givenSlackAutomationEnvironment();

        long projectId = createProjectWithDefinition("Provision Inputs", CHANNEL_INPUT_WORKFLOW_DEFINITION);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        String workflowUuid = findPublishedProject(connectedUser, projectId).workflowTemplates()
            .getFirst()
            .workflowUuid();

        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), getConfigurationId(projectId),
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, Map.of("channel", "#alerts"))));

        assertThat(findProjectWorkflow(connectedUser, workflowUuid).inputValues())
            .isEqualTo(Map.of("channel", "#alerts"));

        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), getConfigurationId(projectId),
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null)));

        assertThat(findProjectWorkflow(connectedUser, workflowUuid).inputValues())
            .isEqualTo(Map.of("channel", "#alerts"));
    }

    @Test
    void testProvisionAssignsTheChosenConfigurationIgnoringItsExpression() {
        givenSlackAutomationEnvironment();
        givenRealPermissionEvaluator();

        long projectId = createProjectWithWorkflows("Provision Beta", 1);

        publishAndPinProject(projectId);

        AutomationProjectConfiguration betaConfiguration = createBetaConfiguration(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        String workflowUuid = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId)
                .workflows()
                .getFirst()
                .workflowUuid();

        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), betaConfiguration.getId(),
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null)));

        assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
            .map(ConnectedUserProjectConfiguration::getConfigurationId)
            .contains(betaConfiguration.getId());
        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
            .extracting(ConnectedUserProjectWorkflow::getAutomationUuid,
                ConnectedUserProjectWorkflow::isEnabled)
            .containsExactly(tuple(workflowUuid, true));
        assertThat(connectedUserProjectAdminFacade.getConnectedUserProjectConfigurations(connectedUser.getId()))
            .containsExactly(
                new ConnectedUserAutomationProjectConfigurationDTO(
                    projectId, betaConfiguration.getId(), "Beta", betaConfiguration.getProjectVersion()));
    }

    @Test
    void testProvisionWithDifferentConfigurationForAssignedUserThrows() {
        givenSlackAutomationEnvironment();
        givenRealPermissionEvaluator();

        long projectId = createProjectWithWorkflows("Provision Reassign", 1);

        publishAndPinProject(projectId);

        long alphaConfigurationId = getConfigurationId(projectId);

        AutomationProjectConfiguration betaConfiguration = createBetaConfiguration(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        String workflowUuid = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId)
                .workflows()
                .getFirst()
                .workflowUuid();

        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), alphaConfigurationId,
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null)));

        assertThatThrownBy(() -> connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), betaConfiguration.getId(),
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null))))
                .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                    configurationException.getErrorKey())
                        .isEqualTo(AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH
                            .getErrorKey()));

        assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
            .map(ConnectedUserProjectConfiguration::getConfigurationId)
            .contains(alphaConfigurationId);

        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), alphaConfigurationId,
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null)));

        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
            .extracting(ConnectedUserProjectWorkflow::getAutomationUuid)
            .containsExactly(workflowUuid);
    }

    @Test
    void testProvisionableProjectsListEveryEnabledConfigurationAndMarkSelfServe() {
        givenSlackAutomationEnvironment();
        givenRealPermissionEvaluator();

        long projectId = createProjectWithWorkflows("Provisionable Configurations", 2);

        publishAndPinProject(projectId);

        long alphaConfigurationId = getConfigurationId(projectId);

        AutomationProjectConfiguration betaConfiguration = createBetaConfiguration(projectId);

        automationProjectConfigurationService.create(
            projectId, Environment.PRODUCTION.ordinal(), "Disabled", null, betaConfiguration.getProjectVersion(),
            false, true);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        List<String> workflowUuids = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId)
                .workflows()
                .stream()
                .map(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
                .toList();

        String betaTurnedOffWorkflowUuid = workflowUuids.getFirst();
        String betaReleasedWorkflowUuid = workflowUuids.getLast();

        automationProjectConfigurationAdminFacade.enableConfigurationWorkflow(
            betaConfiguration.getId(), betaTurnedOffWorkflowUuid, false);

        ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId);

        assertThat(provisionableProject.assignedConfigurationId()).isNull();
        assertThat(provisionableProject.configurations())
            .extracting(
                ProvisionableAutomationProjectDTO.Configuration::id,
                ProvisionableAutomationProjectDTO.Configuration::selfServe)
            .containsExactly(tuple(alphaConfigurationId, true), tuple(betaConfiguration.getId(), false));
        assertThat(provisionableProject.configurations()
            .getLast()
            .workflows())
                .extracting(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
                .containsExactly(betaReleasedWorkflowUuid);
        assertThat(provisionableProject.workflows())
            .extracting(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
            .containsExactlyInAnyOrderElementsOf(workflowUuids);

        ConnectedUser betaConnectedUser = connectedUserService.createConnectedUser(
            "beta-user-" + UUID.randomUUID(), Environment.PRODUCTION, null, null, Map.of("beta", "true"));

        assertThat(
            findProvisionableProject(
                connectedUserProjectAdminFacade.getProvisionableProjects(betaConnectedUser.getId()), projectId)
                    .configurations())
                        .extracting(
                            ProvisionableAutomationProjectDTO.Configuration::id,
                            ProvisionableAutomationProjectDTO.Configuration::selfServe)
                        .containsExactly(tuple(alphaConfigurationId, false), tuple(betaConfiguration.getId(), true));

        connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), betaConfiguration.getId(),
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(betaReleasedWorkflowUuid, null)));

        ProvisionableAutomationProjectDTO assignedProvisionableProject = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId);

        assertThat(assignedProvisionableProject.assignedConfigurationId()).isEqualTo(betaConfiguration.getId());
        assertThat(assignedProvisionableProject.workflows())
            .extracting(
                ProvisionableAutomationProjectDTO.Workflow::workflowUuid,
                ProvisionableAutomationProjectDTO.Workflow::provisioned)
            .containsExactly(tuple(betaReleasedWorkflowUuid, true));
    }

    @Test
    void testProvisionOntoDisabledConfigurationThrowsAndAssignsNothing() {
        givenSlackAutomationEnvironment();
        givenRealPermissionEvaluator();

        long projectId = createProjectWithWorkflows("Provision Disabled", 1);

        publishAndPinProject(projectId);

        AutomationProjectConfiguration alphaConfiguration = automationProjectConfigurationService
            .getConfiguration(getConfigurationId(projectId));

        AutomationProjectConfiguration disabledConfiguration = automationProjectConfigurationService
            .create(
                projectId, Environment.PRODUCTION.ordinal(), "Disabled", null, alphaConfiguration.getProjectVersion(),
                false, true);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        String workflowUuid = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId)
                .workflows()
                .getFirst()
                .workflowUuid();

        assertThatThrownBy(() -> connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), disabledConfiguration.getId(),
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null))))
                .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                    configurationException.getErrorKey())
                        .isEqualTo(AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH
                            .getErrorKey()));

        assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
            .isEmpty();
        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();
    }

    @Test
    void testProvisionIntoProjectWhoseExpressionFailsThrowsAndAssignsNothing() {
        givenSlackAutomationEnvironment();
        givenRealPermissionEvaluator();

        long projectId = automationProjectFacade.createProject(
            "Provision Gated " + UUID.randomUUID(), "", null, List.of(), "metadata['beta'] == 'true'");

        String workflowUuid = automationProjectFacade.createProjectWorkflow(
            projectId, SLACK_WORKFLOW_DEFINITION, null);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        assertThat(connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()))
            .extracting(ProvisionableAutomationProjectDTO::projectId)
            .doesNotContain(projectId);

        assertThatThrownBy(() -> connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), getConfigurationId(projectId),
            List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null))))
                .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                    configurationException.getErrorKey())
                        .isEqualTo(AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH
                            .getErrorKey()));

        assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
            .isEmpty();
        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();
    }

    @Test
    void testProvisionWithNoWorkflowsThrows() {
        givenSlackAutomationEnvironment();
        givenRealPermissionEvaluator();

        long projectId = createProjectWithWorkflows("Provision Nothing", 1);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        assertThatThrownBy(() -> connectedUserProjectAdminFacade.provisionProjectWorkflows(
            connectedUser.getId(), getConfigurationId(projectId), List.of()))
                .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                    configurationException.getErrorKey())
                        .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE.getErrorKey()));

        assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
            .isEmpty();
    }

    private AutomationProjectConfiguration createBetaConfiguration(long projectId) {
        AutomationProjectConfiguration alphaConfiguration = automationProjectConfigurationService
            .getConfiguration(getConfigurationId(projectId));

        return automationProjectConfigurationService.create(
            projectId, Environment.PRODUCTION.ordinal(), "Beta", "metadata['beta'] == 'true'",
            alphaConfiguration.getProjectVersion(), true, true);
    }

    private void givenRealPermissionEvaluator() {
        EmbeddedPermissionEvaluator realEmbeddedPermissionEvaluator = new EmbeddedPermissionEvaluator(
            SpelEvaluator.create());

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenAnswer(invocation -> realEmbeddedPermissionEvaluator.evaluate(
                invocation.getArgument(0), invocation.getArgument(1)));
    }

    private long getConfigurationId(long projectId) {
        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(projectId, Environment.PRODUCTION.ordinal())
            .getFirst();

        return Objects.requireNonNull(configuration.getId());
    }

    private ConnectedUserProjectWorkflowDTO findProjectWorkflow(ConnectedUser connectedUser, String workflowUuid) {
        return connectedUserProjectAdminFacade.getConnectedUserProjects(connectedUser.getId(), Environment.PRODUCTION)
            .stream()
            .map(ConnectedUserProjectDTO::connectedUserProjectWorkflows)
            .flatMap(List::stream)
            .filter(workflow -> workflowUuid.equals(workflow.automationUuid()))
            .findFirst()
            .orElseThrow();
    }

    private long createProjectWithDefinition(String name, String definition) {
        long projectId = automationProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null);

        automationProjectFacade.createProjectWorkflow(projectId, definition, null);

        return projectId;
    }

    private long createProjectWithWorkflows(String name, int workflowCount) {
        long projectId = automationProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null);

        for (int index = 0; index < workflowCount; index++) {
            automationProjectFacade.createProjectWorkflow(projectId, SLACK_WORKFLOW_DEFINITION, null);
        }

        return projectId;
    }

    private AutomationProjectDTO findPublishedProject(ConnectedUser connectedUser, long projectId) {
        return automationProjectFacade
            .getPublishedProjects(connectedUser.getExternalId(), Environment.PRODUCTION)
            .stream()
            .filter(project -> project.id() == projectId)
            .findFirst()
            .orElseThrow();
    }

    private ProvisionableAutomationProjectDTO findProvisionableProject(
        List<ProvisionableAutomationProjectDTO> provisionableProjects, long projectId) {

        return provisionableProjects.stream()
            .filter(project -> project.projectId() == projectId)
            .findFirst()
            .orElseThrow();
    }

    private ConnectedUser createConnectedUserWithConnection() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "provisionable-user-" + UUID.randomUUID(), Environment.PRODUCTION);

        long connectionId = NEXT_CONNECTION_ID.getAndIncrement();

        registerEmbeddedConnection(connectionId, Environment.PRODUCTION);

        connectedUserConnectionService.create(Objects.requireNonNull(connectedUser.getId()), connectionId);

        return connectedUser;
    }

    private void registerEmbeddedConnection(long connectionId, Environment environment) {
        Connection connection = new Connection();

        connection.setEnvironmentId(environment.ordinal());
        connection.setId(connectionId);
        connection.setType(PlatformType.EMBEDDED);

        embeddedConnections.put(connectionId, connection);
    }

    private void givenSlackAutomationEnvironment() {
        ComponentConnection slot = new ComponentConnection("slack", 1, "postMessage1", "slack", true);

        when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
            .thenReturn(new ComponentDefinition("slack"));
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(List.of(slot));
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("postMessage1"), eq("slack")))
            .thenReturn(slot);
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
            .thenAnswer(invocation -> {
                List<Long> connectionIds = invocation.getArgument(0);

                return connectionIds.stream()
                    .map(connectionId -> ConnectionDTO.builder()
                        .componentName("slack")
                        .id(connectionId)
                        .build())
                    .toList();
            });
        when(connectionService.getConnections(anyList()))
            .thenAnswer(invocation -> {
                List<Long> connectionIds = invocation.getArgument(0);

                return connectionIds.stream()
                    .map(embeddedConnections::get)
                    .filter(Objects::nonNull)
                    .toList();
            });
        when(connectionService.getConnection(anyLong()))
            .thenAnswer(invocation -> {
                Connection connection = new Connection();

                connection.setComponentName("slack");
                connection.setId(invocation.getArgument(0));

                return connection;
            });
        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(true);
        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                .thenReturn(Page.empty());
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

    private void publishAndPinProject(long projectId) {
        AutomationProjectConfigurationTestSupport.publishAndPinProject(
            automationProjectFacade, automationProjectConfigurationService, projectId);
    }
}
