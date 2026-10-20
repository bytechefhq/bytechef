/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.EmbeddedResourceResolver;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.security.scope.ApiKeyPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ApiPlatformPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ConnectionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DataTablePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DeploymentPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ExecutionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.KnowledgeBasePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.McpPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ProjectPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkflowPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkspacePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.AppEventPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.ConnectedUserPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedSharedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationInstanceConfigurationPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.CustomRoleScopeResolverImpl;
import com.bytechef.ee.automation.configuration.service.EmbeddedScopeCacheService;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        EmbeddedHubGatesIntTest.RealPermissionConfiguration.class
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
    ClusterElementDefinitionService.class, TriggerDefinitionFacade.class, ComponentConnectionFacade.class,
    ComponentDefinitionService.class, ConnectedUserService.class, ConnectionDefinitionService.class,
    ConnectionFacade.class, ConnectionLifecycleFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class, GitHubProxyClient.class, JobFacade.class,
    JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class, TriggerDefinitionService.class,
    TriggerExecutionService.class, TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class, WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class EmbeddedHubGatesIntTest {

    private static final String EDITOR_IN_DEVELOPMENT = "hub-editor-in-development";
    private static final String EDITOR_IN_PRODUCTION = "hub-editor-in-production";
    private static final String EMBEDDED_ADMIN = "hub-embedded-admin";
    private static final String EMBEDDED_VIEWER = "hub-embedded-viewer";
    private static final String NON_MEMBER = "hub-non-member";
    private static final String TENANT_ADMIN = "hub-tenant-admin";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9600L, EMBEDDED_VIEWER, 9601L, EDITOR_IN_PRODUCTION, 9602L, EDITOR_IN_DEVELOPMENT, 9603L,
        EMBEDDED_ADMIN, 9604L, NON_MEMBER, 9605L);

    private final List<Long> createdProjectIds = new ArrayList<>();

    @Autowired
    private AutomationWorkflowProjectAdminFacade automationWorkflowProjectAdminFacade;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private AutomationWorkflowProjectService automationWorkflowProjectService;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        for (Map.Entry<String, Long> entry : USER_IDS_BY_LOGIN.entrySet()) {
            User user = new User();

            user.setId(entry.getValue());
            user.setLogin(entry.getKey());

            when(userService.fetchUserByLogin(entry.getKey())).thenReturn(Optional.of(user));
            when(userService.getUser(entry.getKey())).thenReturn(user);
            when(userService.getUser(entry.getValue())).thenReturn(user);
        }

        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_VIEWER), WorkspaceRole.VIEWER));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_PRODUCTION), WorkspaceRole.EDITOR, Environment.PRODUCTION));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(EDITOR_IN_DEVELOPMENT), WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_ADMIN), WorkspaceRole.ADMIN));

        permissionService.evictAllEmbeddedScopeCache();
    }

    @AfterEach
    void afterEach() {
        authenticate(TENANT_ADMIN, true);

        for (Long projectId : createdProjectIds) {
            if (automationWorkflowProjectService.fetchAutomationWorkflowProject(projectId)
                .isPresent()) {

                automationWorkflowProjectFacade.deleteProject(projectId);
            } else {
                projectService.delete(projectId);
            }
        }

        createdProjectIds.clear();

        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();
    }

    @Test
    void testAnEditorEditsHubWorkflowsButNotWhoSeesThem() {
        long projectId = createHubProject();
        String workflowUuid = createHubWorkflow(projectId);

        authenticate(EDITOR_IN_PRODUCTION, false);

        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.updateProjectWorkflow(workflowUuid, "x", "y"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.createProjectWorkflow(projectId, null, null))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(EDITOR_IN_DEVELOPMENT, false);

        assertThat(automationWorkflowProjectAdminFacade.getProjects()).isNotEmpty();
        assertThat(automationWorkflowProjectAdminFacade.getProjectVersions(projectId)).isNotEmpty();

        automationWorkflowProjectAdminFacade.updateProjectWorkflow(workflowUuid, "edited", "edited description");

        String createdWorkflowUuid = automationWorkflowProjectAdminFacade.createProjectWorkflow(
            projectId, null, "   ");
        String duplicatedWorkflowUuid = automationWorkflowProjectAdminFacade.duplicateProjectWorkflow(workflowUuid);

        automationWorkflowProjectAdminFacade.deleteProjectWorkflow(createdWorkflowUuid);
        automationWorkflowProjectAdminFacade.deleteProjectWorkflow(duplicatedWorkflowUuid);

        assertThatThrownBy(
            () -> automationWorkflowProjectAdminFacade.updateProjectWorkflowPermissionExpression(
                workflowUuid, "true"))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.createProjectWorkflow(projectId, null, "true"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> automationWorkflowProjectAdminFacade.updateProject(
                projectId, "renamed", "description", null, List.of(), null, null))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.deleteProject(projectId))
            .isInstanceOf(AccessDeniedException.class);

        automationWorkflowProjectAdminFacade.publishProject(projectId, "release");

        authenticate(EMBEDDED_VIEWER, false);

        assertThat(automationWorkflowProjectAdminFacade.getProjects()).isNotEmpty();
        assertThat(automationWorkflowProjectAdminFacade.getCategories()).isNotNull();
        assertThat(automationWorkflowProjectAdminFacade.getTags()).isNotNull();

        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.publishProject(projectId, "release"))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testCreatingWithAPermissionExpressionNeedsAnAdmin() {
        authenticate(EDITOR_IN_DEVELOPMENT, false);

        long blankProjectId = automationWorkflowProjectAdminFacade.createProject(
            "hub-" + UUID.randomUUID(), "", null, List.of(), "  ", null);

        createdProjectIds.add(blankProjectId);

        long nullProjectId = automationWorkflowProjectAdminFacade.createProject(
            "hub-" + UUID.randomUUID(), "", null, List.of(), null, null);

        createdProjectIds.add(nullProjectId);

        assertThatThrownBy(
            () -> automationWorkflowProjectAdminFacade.createProject(
                "hub-" + UUID.randomUUID(), "", null, List.of(), "true", null))
                    .isInstanceOf(AccessDeniedException.class);

        authenticate(EMBEDDED_ADMIN, false);

        long guardedProjectId = automationWorkflowProjectAdminFacade.createProject(
            "hub-" + UUID.randomUUID(), "", null, List.of(), "true", null);

        createdProjectIds.add(guardedProjectId);

        String guardedWorkflowUuid = automationWorkflowProjectAdminFacade.createProjectWorkflow(
            guardedProjectId, null, "true");

        assertThat(guardedWorkflowUuid).isNotBlank();
    }

    @Test
    void testDuplicatingAProjectNeedsTheRightToCreate() {
        long projectId = createHubProject();

        createHubWorkflow(projectId);

        authenticate(EMBEDDED_VIEWER, false);

        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.duplicateProject(projectId))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(EDITOR_IN_DEVELOPMENT, false);

        long duplicatedProjectId = automationWorkflowProjectAdminFacade.duplicateProject(projectId);

        createdProjectIds.add(duplicatedProjectId);

        authenticate(TENANT_ADMIN, true);

        assertThat(automationWorkflowProjectFacade.getProject(duplicatedProjectId)
            .workflowTemplates()).hasSize(1);
    }

    @Test
    void testATenantAdminWithoutARowStillPasses() {
        authenticate(TENANT_ADMIN, true);

        long projectId = automationWorkflowProjectAdminFacade.createProject(
            "hub-" + UUID.randomUUID(), "", null, List.of(), "true", null);

        createdProjectIds.add(projectId);

        String workflowUuid = automationWorkflowProjectAdminFacade.createProjectWorkflow(projectId, null, "true");

        automationWorkflowProjectAdminFacade.updateProjectWorkflow(workflowUuid, "edited", "description");
        automationWorkflowProjectAdminFacade.updateProjectWorkflowPermissionExpression(workflowUuid, "true");
        automationWorkflowProjectAdminFacade.updateProject(
            projectId, "hub-" + UUID.randomUUID(), "description", null, List.of(), "true", null);

        assertThat(automationWorkflowProjectAdminFacade.getProjects()).extracting(AutomationWorkflowProjectDTO::id)
            .contains(projectId);

        String duplicatedWorkflowUuid = automationWorkflowProjectAdminFacade.duplicateProjectWorkflow(workflowUuid);

        automationWorkflowProjectAdminFacade.deleteProjectWorkflow(duplicatedWorkflowUuid);
        automationWorkflowProjectAdminFacade.publishProject(projectId, "release");

        long duplicatedProjectId = automationWorkflowProjectAdminFacade.duplicateProject(projectId);

        createdProjectIds.add(duplicatedProjectId);

        automationWorkflowProjectAdminFacade.deleteProjectWorkflow(workflowUuid);
        automationWorkflowProjectAdminFacade.deleteProject(duplicatedProjectId);

        createdProjectIds.remove(duplicatedProjectId);
    }

    @Test
    void testANonMemberIsDenied() {
        long projectId = createHubProject();
        String workflowUuid = createHubWorkflow(projectId);

        authenticate(NON_MEMBER, false);

        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.getProjects())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.getProjectVersions(projectId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> automationWorkflowProjectAdminFacade.createProject(
                "hub-" + UUID.randomUUID(), "", null, List.of(), null, null))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.createProjectWorkflow(projectId, null, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.updateProjectWorkflow(workflowUuid, "x", "y"))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.deleteProjectWorkflow(workflowUuid))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> automationWorkflowProjectAdminFacade.updateProjectWorkflowPermissionExpression(
                workflowUuid, "true"))
                    .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.deleteProject(projectId))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(TENANT_ADMIN, true);

        assertThat(automationWorkflowProjectFacade.getProject(projectId)
            .workflowTemplates()).hasSize(1);
    }

    @Test
    void testANonHubProjectIsDeniedToAnEmbeddedAdmin() {
        authenticate(TENANT_ADMIN, true);

        Project project = new Project();

        project.setName("plain-" + UUID.randomUUID());
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

        project = projectService.create(project);

        long projectId = project.getId();

        createdProjectIds.add(projectId);

        authenticate(EMBEDDED_ADMIN, false);

        assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.deleteProject(projectId))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(TENANT_ADMIN, true);

        assertThat(projectService.fetchProject(projectId)).isPresent();
    }

    private long createHubProject() {
        authenticate(TENANT_ADMIN, true);

        long projectId = automationWorkflowProjectFacade.createProject(
            "hub-" + UUID.randomUUID(), "", null, List.of(), null, null);

        createdProjectIds.add(projectId);

        return projectId;
    }

    private String createHubWorkflow(long projectId) {
        authenticate(TENANT_ADMIN, true);

        return automationWorkflowProjectFacade.createProjectWorkflow(projectId, null, null);
    }

    private static long userId(String login) {
        return USER_IDS_BY_LOGIN.get(login);
    }

    private static void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    @TestConfiguration
    @EnableMethodSecurity
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        AppEventPermissionScopeProvider.class, ConnectedUserPermissionScopeProvider.class,
        ConnectionPermissionScopeProvider.class, CurrentUserResolver.class, CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        EmbeddedPermissionScopeProvider.class, EmbeddedScopeCacheService.class,
        EmbeddedSharedPermissionScopeProvider.class, ExecutionPermissionScopeProvider.class,
        IntegrationInstanceConfigurationPermissionScopeProvider.class, IntegrationPermissionScopeProvider.class,
        KnowledgeBasePermissionScopeProvider.class, McpPermissionScopeProvider.class, PermissionScopeRegistry.class,
        ProjectPermissionScopeProvider.class, WorkflowPermissionScopeProvider.class,
        WorkspacePermissionScopeProvider.class, WorkspaceScopeCacheService.class
    })
    static class RealPermissionConfiguration {

        @Bean
        AutomationPermissionEvaluator automationPermissionEvaluator(@Lazy PermissionService permissionService) {
            return new AutomationPermissionEvaluator(permissionService);
        }

        @Bean
        static RoleHierarchy roleHierarchy() {
            return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN")
                .implies("USER")
                .build();
        }

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService, PermissionEvaluator permissionEvaluator,
            RoleHierarchy roleHierarchy) {

            AutomationMethodSecurityExpressionHandler handler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            handler.setPermissionEvaluator(permissionEvaluator);
            handler.setRoleHierarchy(roleHierarchy);

            return handler;
        }

        @Bean
        @Primary
        PermissionServiceImpl realPermissionService(
            CurrentUserResolver currentUserResolver, PermissionScopeRegistry permissionScopeRegistry,
            ProjectRepository projectRepository,
            WorkspaceScopeCacheService workspaceScopeCacheService,
            WorkspaceUserRepository workspaceUserRepository,
            List<ResourceOwnershipResolver> ownershipResolvers,
            List<ResourceEnvironmentResolver> environmentResolvers,
            ObjectProvider<ConnectedUserAccessDecider> connectedUserAccessDeciderProvider,
            EmbeddedScopeCacheService embeddedScopeCacheService, EmbeddedUserRepository embeddedUserRepository,
            ObjectProvider<EmbeddedResourceResolver> embeddedResourceResolverProvider,
            ObjectProvider<ConnectionService> connectionServiceProvider) {

            return new PermissionServiceImpl(
                currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
                workspaceUserRepository, ownershipResolvers, List.of(),
                (resourceType, workspaceId, candidates) -> candidates.stream()
                    .map(VisibilityRecord::id)
                    .collect(Collectors.toSet()),
                environmentResolvers, connectedUserAccessDeciderProvider, embeddedScopeCacheService,
                embeddedUserRepository, embeddedResourceResolverProvider,
                connectionServiceProvider);
        }
    }
}
