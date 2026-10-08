/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectPublishedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserFacade;
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
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserFacadeIntTest.ConnectedUserConfiguration.class
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
    PrincipalJobFacade.class, PrincipalJobService.class, TaskDispatcherDefinitionService.class,
    TaskExecutionService.class, TriggerDefinitionFacade.class, TriggerDefinitionService.class,
    TriggerExecutionService.class, TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class, WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserFacadeIntTest {

    private static final long ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();
    private static final long INSTANCE_CONNECTION_ID = 9102L;
    private static final long OWN_CONNECTION_ID = 9101L;

    private static final String SLACK_TRIGGER_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[{"name":"newMessage1","type":"slack/v1/newMessage","parameters":{}}],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
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
    private ConnectUserProjectRepository connectUserProjectRepository;

    @Autowired
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserConnectionRepository connectedUserConnectionRepository;

    @Autowired
    private ConnectedUserFacade connectedUserFacade;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Test
    void testDeleteConnectedUserRemovesItsInstancesProjectsAndConnections() {
        String externalUserId = "delete-user-" + UUID.randomUUID();

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        long connectedUserId = connectedUser.getId();

        stubCollaborators();

        connectedUserConnectionFacade.createConnectedUserConnection(
            connectedUserId, ConnectionDTO.builder()
                .componentName("slack")
                .name("Slack")
                .build());

        long automationWorkflowProjectId = automationWorkflowProjectFacade.createProject(
            "Delete Connected User " + UUID.randomUUID(), "", null, List.of(), null, null);

        String automationWorkflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            automationWorkflowProjectId, SLACK_TRIGGER_WORKFLOW_DEFINITION, null);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isTrue();

        String referenceWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(automationWorkflowUuid);

        String copyWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.publishProjectWorkflow(externalUserId, copyWorkflowUuid, "v1", ENVIRONMENT_ID);
        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, copyWorkflowUuid, true, ENVIRONMENT_ID);

        ConnectedUserProject connectedUserProject = connectUserProjectRepository.findByConnectedUserId(connectedUserId)
            .orElseThrow();

        long copyProjectId = connectedUserProject.getProjectId();
        long copyProjectDeploymentId = projectDeploymentService.getProjectDeploymentId(
            copyProjectId, Environment.PRODUCTION);
        String copyWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(copyWorkflowUuid);

        projectDeploymentFacade.enableProjectDeployment(copyProjectDeploymentId, true);

        createIntegrationInstance(connectedUserId);

        ArgumentCaptor<String> enabledWorkflowIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(triggerLifecycleFacade, atLeastOnce()).executeTriggerEnable(
            enabledWorkflowIdCaptor.capture(), any(), any(), any(), any(), any(), anyLong());

        assertThat(enabledWorkflowIdCaptor.getAllValues()).contains(referenceWorkflowId, copyWorkflowId);

        assertThatCode(() -> connectedUserFacade.deleteConnectedUser(connectedUserId))
            .doesNotThrowAnyException();

        assertThat(connectedUserService.fetchConnectedUser(connectedUserId)).isEmpty();
        assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId)).isEmpty();
        assertThat(connectedUserConnectionRepository.findAllByConnectedUserId(connectedUserId)).isEmpty();
        assertThat(connectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUserId)).isEmpty();
        assertThat(connectUserProjectRepository.findByConnectedUserId(connectedUserId)).isEmpty();
        assertThat(projectService.fetchProject(copyProjectId)).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(copyProjectDeploymentId)).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(reference.getProjectDeploymentId())).isEmpty();

        for (String enabledWorkflowId : enabledWorkflowIdCaptor.getAllValues()) {
            verify(triggerLifecycleFacade, atLeastOnce())
                .executeTriggerDisable(eq(enabledWorkflowId), any(), any(), any(), any());
        }

        verify(connectionFacade).delete(OWN_CONNECTION_ID);
        verify(connectionFacade).delete(INSTANCE_CONNECTION_ID);
    }

    private void createIntegrationInstance(long connectedUserId) {
        Integration integration = new Integration();

        integration.setComponentName("jira");
        integration.setName("Jira " + UUID.randomUUID());

        integration = integrationRepository.save(integration);

        Long integrationInstanceConfigurationId = jdbcTemplate.queryForObject(
            """
                INSERT INTO integration_instance_configuration (
                    integration_id, integration_version, name, enabled, environment, authorization_type,
                    connection_parameters, created_by, created_date, last_modified_by, last_modified_date, version)
                VALUES (?, 1, 'Jira', true, ?, 0, '{}', 'system', now(), 'system', now(), 0)
                RETURNING id
                """,
            Long.class, integration.getId(), Environment.PRODUCTION.ordinal());

        integrationInstanceService.create(
            connectedUserId, INSTANCE_CONNECTION_ID, Objects.requireNonNull(integrationInstanceConfigurationId));
    }

    private void stubCollaborators() {
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(OWN_CONNECTION_ID);

        ComponentConnection slot = new ComponentConnection("slack", 1, "postMessage1", "slack", true);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class))).thenReturn(List.of(slot));
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("postMessage1"), eq("slack")))
            .thenReturn(slot);
        when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
            .thenReturn(new ComponentDefinition("slack"));
        when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
            .thenReturn(OWN_CONNECTION_ID);
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
            .thenReturn(List.of(
                ConnectionDTO.builder()
                    .componentName("slack")
                    .id(OWN_CONNECTION_ID)
                    .build()));
        when(connectionService.getConnection(OWN_CONNECTION_ID)).thenReturn(connection);
        when(connectionService.getConnections(PlatformType.EMBEDDED)).thenReturn(List.of());
        when(embeddedPermissionEvaluator.evaluate(any(), any())).thenReturn(true);
        when(environmentService.getEnvironment(ENVIRONMENT_ID)).thenReturn(Environment.PRODUCTION);
        when(principalJobService.getJobIds(any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(OWN_CONNECTION_ID, "slack", "postMessage1")));

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(anyString(), eq(ENVIRONMENT_ID)))
            .thenReturn(Optional.of(workflowTestConfiguration));
    }

    @Configuration
    @ComponentScan({
        "com.bytechef.ee.embedded.connected.user.facade", "com.bytechef.ee.embedded.connected.user.service"
    })
    static class ConnectedUserConfiguration {
    }
}
