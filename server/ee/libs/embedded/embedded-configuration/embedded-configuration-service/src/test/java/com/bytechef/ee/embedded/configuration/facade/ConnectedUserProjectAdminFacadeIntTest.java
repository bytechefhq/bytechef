/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
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
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
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
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
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

    private static final AtomicLong NEXT_CONNECTION_ID = new AtomicLong(1000L);

    private static final String SLACK_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    @Autowired
    private AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private AutomationWorkflowProjectConfigurationUpdatedEventListener automationWorkflowProjectConfigurationUpdatedEventListener;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @MockitoSpyBean
    private ConnectedUserProjectService connectedUserProjectService;

    @MockitoSpyBean
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserProjectAdminFacade connectedUserProjectAdminFacade;

    @Autowired
    private AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade;

    @Autowired
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
        givenSlackAutomationWorkflowEnvironment();

        long projectId = createProjectWithWorkflows("Provisionable Listed", 2);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        AutomationWorkflowProjectDTO publishedProject = findPublishedProject(connectedUser, projectId);

        List<ProvisionableAutomationWorkflowProjectDTO> result =
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId());

        ProvisionableAutomationWorkflowProjectDTO provisionableProject = findProvisionableProject(result, projectId);

        assertThat(provisionableProject.name()).isEqualTo(publishedProject.name());
        assertThat(provisionableProject.projectVersion()).isEqualTo(publishedProject.lastPublishedVersion());
        assertThat(provisionableProject.workflows())
            .extracting(
                ProvisionableAutomationWorkflowProjectDTO.Workflow::workflowUuid,
                ProvisionableAutomationWorkflowProjectDTO.Workflow::label,
                ProvisionableAutomationWorkflowProjectDTO.Workflow::inputs,
                ProvisionableAutomationWorkflowProjectDTO.Workflow::provisioned,
                ProvisionableAutomationWorkflowProjectDTO.Workflow::dangling)
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
        givenSlackAutomationWorkflowEnvironment();

        long unconfiguredProjectId = createProjectWithWorkflows("Provisionable Unconfigured", 1);

        automationWorkflowProjectFacade.publishProject(unconfiguredProjectId);

        long configuredProjectId = createProjectWithWorkflows("Provisionable Configured", 1);

        publishAndPinProject(configuredProjectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        List<ProvisionableAutomationWorkflowProjectDTO> result =
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId());

        assertThat(result)
            .extracting(ProvisionableAutomationWorkflowProjectDTO::projectId)
            .contains(configuredProjectId)
            .doesNotContain(unconfiguredProjectId);
    }

    @Test
    void testGetProvisionableProjectsSkipsWorkflowsTurnedOffInTheConfiguration() {
        givenSlackAutomationWorkflowEnvironment();

        long projectId = createProjectWithWorkflows("Provisionable Turned Off", 2);

        publishAndPinProject(projectId);

        ConnectedUser connectedUser = createConnectedUserWithConnection();

        List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
            .workflowTemplates();

        String turnedOffWorkflowUuid = templates.getFirst()
            .workflowUuid();
        String remainingWorkflowUuid = templates.getLast()
            .workflowUuid();

        AutomationWorkflowProjectConfiguration configuration = automationWorkflowProjectConfigurationService
            .fetchConfiguration(projectId, Environment.PRODUCTION.ordinal())
            .orElseThrow();

        automationWorkflowProjectConfigurationAdminFacade.enableConfigurationWorkflow(
            configuration.getId(), turnedOffWorkflowUuid, false);

        ProvisionableAutomationWorkflowProjectDTO provisionableProject = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId);

        assertThat(provisionableProject.workflows())
            .extracting(ProvisionableAutomationWorkflowProjectDTO.Workflow::workflowUuid)
            .containsExactly(remainingWorkflowUuid);
    }

    @Test
    void testGetProvisionableProjectsMarksProvisionedAndDanglingWorkflows() {
        givenSlackAutomationWorkflowEnvironment();

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

        ProvisionableAutomationWorkflowProjectDTO provisionableProject = findProvisionableProject(
            connectedUserProjectAdminFacade.getProvisionableProjects(connectedUser.getId()), projectId);

        assertThat(provisionableProject.workflows())
            .extracting(
                ProvisionableAutomationWorkflowProjectDTO.Workflow::workflowUuid,
                ProvisionableAutomationWorkflowProjectDTO.Workflow::provisioned,
                ProvisionableAutomationWorkflowProjectDTO.Workflow::dangling)
            .containsExactlyInAnyOrder(
                tuple(provisionedWorkflowUuid, true, false),
                tuple(danglingWorkflowUuid, true, true),
                tuple(untouchedWorkflowUuid, false, false));
    }

    private long createProjectWithWorkflows(String name, int workflowCount) {
        long projectId = automationWorkflowProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null);

        for (int index = 0; index < workflowCount; index++) {
            automationWorkflowProjectFacade.createProjectWorkflow(projectId, SLACK_WORKFLOW_DEFINITION, null);
        }

        return projectId;
    }

    private AutomationWorkflowProjectDTO findPublishedProject(ConnectedUser connectedUser, long projectId) {
        return automationWorkflowProjectFacade
            .getPublishedProjects(connectedUser.getExternalId(), Environment.PRODUCTION)
            .stream()
            .filter(project -> project.id() == projectId)
            .findFirst()
            .orElseThrow();
    }

    private ProvisionableAutomationWorkflowProjectDTO findProvisionableProject(
        List<ProvisionableAutomationWorkflowProjectDTO> provisionableProjects, long projectId) {

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

    private void givenSlackAutomationWorkflowEnvironment() {
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
        storedReference.setDanglingReason("Removed from the automation workflow project on redeploy");
        storedReference.setEnabled(false);

        connectedUserProjectWorkflowRepository.save(storedReference);
    }

    private void publishAndPinProject(long projectId) {
        AutomationWorkflowProjectConfigurationTestSupport.publishAndPinProject(
            automationWorkflowProjectFacade, automationWorkflowProjectConfigurationService, projectId);
    }
}
