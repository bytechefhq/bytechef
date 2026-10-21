/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.ownership;

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
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.ownership.OwnerContributorMissingException;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectOwnershipServiceImpl;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacadeIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectPublishedEventListener;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        EmbeddedOwnerContributorIntTest.ConnectedUserServiceConfiguration.class
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
class EmbeddedOwnerContributorIntTest {

    private static final String SLACK_WORKFLOW_DEFINITION = """
        {"label":"Sync leads","description":"Posts new leads","triggers":[],
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

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    private String externalUserId;

    @BeforeEach
    void setUp() {
        externalUserId = "owner-contributor-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
            .thenReturn(new ComponentDefinition("slack"));
        when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of());
        when(environmentService.getEnvironment(Environment.PRODUCTION.ordinal()))
            .thenReturn(Environment.PRODUCTION);
        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                .thenReturn(Page.empty());
        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(true);

        stubEntitledSlackConnection(777L);
    }

    @Test
    void testEmbeddedCatalogProjectsAreHiddenFromWorkspaceProjects() {
        long catalogProjectId = automationWorkflowProjectFacade.createProject(
            "Plain Catalog " + UUID.randomUUID(), "", null, List.of(), null, null);

        long plainProjectId = createPlainProject();

        assertThat(projectService.getProjects(null, null, null, null, null, null))
            .extracting(Project::getId)
            .contains(plainProjectId)
            .doesNotContain(catalogProjectId);
    }

    @Test
    void testConnectedUserProjectsAreHiddenFromProjectListsAndWorkspaceProjectIds() {
        String copyWorkflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, SLACK_WORKFLOW_DEFINITION, Environment.PRODUCTION);

        connectedUserProjectFacade.publishProjectWorkflow(externalUserId, copyWorkflowUuid, "published", null);

        Project connectedUserProject = projectService.getProject(
            connectedUserProjectService.getConnectUserProject(externalUserId, Environment.PRODUCTION)
                .getProjectId());

        long plainProjectId = createPlainProject();

        assertThat(projectService.getProjects(null, null, null, null, null, null))
            .extracting(Project::getId)
            .contains(plainProjectId)
            .doesNotContain(connectedUserProject.getId());
        assertThat(projectService.getWorkspaceProjectIds(connectedUserProject.getWorkspaceId()))
            .contains(plainProjectId)
            .doesNotContain(connectedUserProject.getId());
    }

    @Test
    void testWorkspaceProjectIdsExcludingConnectedUserProjectsKeepCatalogProjects() {
        long catalogProjectId = automationWorkflowProjectFacade.createProject(
            "Reachable Catalog " + UUID.randomUUID(), "", null, List.of(), null, null);

        String copyWorkflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, SLACK_WORKFLOW_DEFINITION, Environment.PRODUCTION);

        connectedUserProjectFacade.publishProjectWorkflow(externalUserId, copyWorkflowUuid, "published", null);

        Project catalogProject = projectService.getProject(catalogProjectId);
        Project connectedUserProject = projectService.getProject(
            connectedUserProjectService.getConnectUserProject(externalUserId, Environment.PRODUCTION)
                .getProjectId());

        assertThat(
            projectService.getWorkspaceProjectIds(
                catalogProject.getWorkspaceId(), Set.of(ProjectOwner.EMBEDDED_CONNECTED_USER)))
                    .contains(catalogProjectId);
        assertThat(
            projectService.getWorkspaceProjectIds(
                connectedUserProject.getWorkspaceId(), Set.of(ProjectOwner.EMBEDDED_CONNECTED_USER)))
                    .doesNotContain(connectedUserProject.getId());
        assertThat(projectService.getWorkspaceProjectIds(catalogProject.getWorkspaceId()))
            .doesNotContain(catalogProjectId);
    }

    @Test
    void testEmbeddedFilterSelectsConnectedUserAndReferenceDeployments() {
        long catalogProjectId = automationWorkflowProjectFacade.createProject(
            "Reference Catalog " + UUID.randomUUID(), "", null, List.of(), null, null);

        String catalogWorkflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            catalogProjectId, SLACK_WORKFLOW_DEFINITION, null);

        automationWorkflowProjectFacade.publishProject(catalogProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, catalogWorkflowUuid, Environment.PRODUCTION);

        String copyWorkflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, SLACK_WORKFLOW_DEFINITION, Environment.PRODUCTION);

        connectedUserProjectFacade.publishProjectWorkflow(externalUserId, copyWorkflowUuid, "published", null);

        long connectedUserProjectId = connectedUserProjectService.getConnectUserProject(
            externalUserId, Environment.PRODUCTION)
            .getProjectId();
        long connectedUserDeploymentId = projectDeploymentService.getProjectDeploymentId(
            connectedUserProjectId, Environment.PRODUCTION);
        long referenceDeploymentId = reference.getProjectDeploymentId();

        long plainDeploymentId = createPlainDeployment();

        List<ProjectDeployment> embeddedDeployments = projectDeploymentService.getProjectDeployments(
            true, null, null, null, null);
        List<ProjectDeployment> nonEmbeddedDeployments = projectDeploymentService.getProjectDeployments(
            false, null, null, null, null);

        assertThat(embeddedDeployments)
            .filteredOn(projectDeployment -> projectDeployment.getId() == connectedUserDeploymentId ||
                projectDeployment.getId() == referenceDeploymentId)
            .extracting(ProjectDeployment::getName)
            .containsExactly(externalUserId, externalUserId);
        assertThat(embeddedDeployments)
            .extracting(ProjectDeployment::getId)
            .doesNotContain(plainDeploymentId);
        assertThat(nonEmbeddedDeployments)
            .extracting(ProjectDeployment::getId)
            .contains(plainDeploymentId)
            .doesNotContain(connectedUserDeploymentId, referenceDeploymentId);
    }

    @Test
    void testAiAutoMemoryOwnerKindsMatchToday() {
        long catalogProjectId = automationWorkflowProjectFacade.createProject(
            "Owner Kinds Catalog " + UUID.randomUUID(), "", null, List.of(), null, null);

        String copyWorkflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, SLACK_WORKFLOW_DEFINITION, Environment.PRODUCTION);

        connectedUserProjectFacade.publishProjectWorkflow(externalUserId, copyWorkflowUuid, "published", null);

        long connectedUserProjectId = connectedUserProjectService.getConnectUserProject(
            externalUserId, Environment.PRODUCTION)
            .getProjectId();

        Set<Long> ownedProjectIds = projectFacade.getOwnedProjectIds(
            List.of(catalogProjectId, connectedUserProjectId), ProjectOwner.EMBEDDED_CONNECTED_USER);

        assertThat(ownedProjectIds)
            .contains(connectedUserProjectId)
            .doesNotContain(catalogProjectId);

        Set<Long> catalogProjectIds = projectFacade.getOwnedProjectIds(
            List.of(catalogProjectId, connectedUserProjectId), ProjectOwner.EMBEDDED_CATALOG);

        assertThat(catalogProjectIds)
            .contains(catalogProjectId)
            .doesNotContain(connectedUserProjectId);
    }

    @Test
    void testStartupCheckFindsTheEmbeddedRowsInTheOwnerTables() {
        long catalogProjectId = automationWorkflowProjectFacade.createProject(
            "Startup Check Catalog " + UUID.randomUUID(), "", null, List.of(), null, null);

        String catalogWorkflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(
            catalogProjectId, SLACK_WORKFLOW_DEFINITION, null);

        automationWorkflowProjectFacade.publishProject(catalogProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, catalogWorkflowUuid, Environment.PRODUCTION);

        String copyWorkflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, SLACK_WORKFLOW_DEFINITION, Environment.PRODUCTION);

        connectedUserProjectFacade.publishProjectWorkflow(externalUserId, copyWorkflowUuid, "published", null);

        ProjectOwnershipServiceImpl projectOwnershipService = new ProjectOwnershipServiceImpl(
            jdbcClient, List.of(), List.of());

        assertThatThrownBy(projectOwnershipService::afterSingletonsInstantiated)
            .isInstanceOfSatisfying(OwnerContributorMissingException.class, exception -> {
                assertThat(exception.getProjectOwners())
                    .containsExactly(ProjectOwner.EMBEDDED_CATALOG, ProjectOwner.EMBEDDED_CONNECTED_USER);
                assertThat(exception.getProjectDeploymentOwners()).contains(ProjectDeploymentOwner.EMBEDDED);
            });
        assertThat(ProjectDeploymentOwner.EMBEDDED.getOwnerTables())
            .allSatisfy(ownerTable -> assertThat(
                jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM " + ownerTable + ")")
                    .query(Boolean.class)
                    .single())
                        .isTrue());
    }

    private long createPlainDeployment() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setName("plain-" + UUID.randomUUID());
        projectDeployment.setProjectId(createPlainProject());
        projectDeployment.setProjectVersion(1);

        return projectDeploymentService.create(projectDeployment)
            .getId();
    }

    private long createPlainProject() {
        Project project = projectService.create(
            Project.builder()
                .name("Plain " + UUID.randomUUID())
                .workspaceId(Workspace.DEFAULT_WORKSPACE_ID)
                .build());

        return project.getId();
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
