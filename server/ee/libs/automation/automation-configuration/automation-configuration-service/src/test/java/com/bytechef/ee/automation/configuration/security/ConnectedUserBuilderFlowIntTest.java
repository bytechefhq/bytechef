/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.callback.ProjectCallback;
import com.bytechef.automation.configuration.callback.ProjectWorkflowCallback;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectFacadeImpl;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacadeImpl;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.ProjectDeploymentEnvironmentResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentOwnershipResolver;
import com.bytechef.automation.configuration.security.ProjectOwnershipResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
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
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.CustomRoleScopeResolverImpl;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacadeImpl;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectWorkflowManager;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserAccessDeciderImpl;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserServiceImpl;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationProvider;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EeAutomationConfigurationIntTestConfiguration.class,
        ConnectedUserBuilderFlowIntTest.ConnectedUserBuilderFlowConfiguration.class
    },
    properties = "spring.liquibase.contexts=configuration,user")
@ActiveProfiles("testint")
@ExtendWith(ObjectMapperSetupExtension.class)
@Import(PostgreSQLContainerConfiguration.class)
class ConnectedUserBuilderFlowIntTest {

    private static final String DEFINITION = "{\"label\":\"Flow\",\"triggers\":[],\"tasks\":[]}";
    private static final Environment ENVIRONMENT = Environment.PRODUCTION;
    private static final long ENVIRONMENT_ID = ENVIRONMENT.ordinal();

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private EmbeddedApiKeyAuthenticationProvider embeddedApiKeyAuthenticationProvider;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowFacade workflowFacade;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void stubTheWorkflowStore() {
        reset(workflowFacade, workflowService);

        when(workflowFacade.getWorkflow(anyString())).thenReturn(mock(WorkflowDTO.class));
        when(workflowService.create(anyString(), any(), any()))
            .thenAnswer(invocation -> new Workflow(
                String.valueOf(UUID.randomUUID()), invocation.getArgument(0), Workflow.Format.JSON));
        when(workflowService.duplicateWorkflow(anyString()))
            .thenAnswer(
                invocation -> new Workflow(String.valueOf(UUID.randomUUID()), DEFINITION, Workflow.Format.JSON));
        when(workflowService.getWorkflow(anyString()))
            .thenAnswer(invocation -> new Workflow(invocation.getArgument(0), DEFINITION, Workflow.Format.JSON));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testBrandNewConnectedUserCreatesPublishesAndEnablesAWorkflow() {
        String externalUserId = "newcomer-" + UUID.randomUUID();

        assertThat(connectedUserService.fetchConnectedUser(externalUserId, ENVIRONMENT_ID)).isEmpty();

        authenticateFirstRequest(externalUserId);

        assertThat(connectedUserProjectService.fetchConnectUserProject(externalUserId, ENVIRONMENT)).isEmpty();

        String workflowUuid = connectedUserProjectFacade.createProjectWorkflow(externalUserId, DEFINITION, ENVIRONMENT);

        ConnectedUserProject connectedUserProject = connectedUserProjectService
            .fetchConnectUserProject(externalUserId, ENVIRONMENT)
            .orElseThrow();

        long projectId = connectedUserProject.getProjectId();

        Project project = projectService.getProject(projectId);

        assertThat(project.getWorkspaceId()).isEqualTo(Workspace.DEFAULT_WORKSPACE_ID);

        connectedUserProjectFacade.publishProjectWorkflow(externalUserId, workflowUuid, "first", ENVIRONMENT_ID);

        Optional<ProjectDeployment> projectDeployment = projectDeploymentService.fetchProjectDeployment(
            projectId, ENVIRONMENT);

        assertThat(projectDeployment).isPresent();

        long projectDeploymentId = projectDeployment.get()
            .getId();

        String deployedWorkflowId = projectWorkflowService.fetchProjectWorkflowWorkflowId(
            projectDeploymentId, workflowUuid)
            .orElseThrow();

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, workflowUuid, false, ENVIRONMENT_ID);

        assertThat(projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
            projectDeploymentId, deployedWorkflowId)).isFalse();

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, workflowUuid, true, ENVIRONMENT_ID);

        assertThat(projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
            projectDeploymentId, deployedWorkflowId)).isTrue();
    }

    @Test
    void testConnectedUserIsDeniedOpeningEditingAndTestingAnotherConnectedUsersWorkflow() {
        String aliceExternalUserId = "alice-" + UUID.randomUUID();

        authenticateFirstRequest(aliceExternalUserId);

        String aliceWorkflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            aliceExternalUserId, DEFINITION, ENVIRONMENT);
        String aliceWorkflowId = projectWorkflowService.getLastWorkflowId(aliceWorkflowUuid);

        assertThat(permissionService.hasWorkflowScope(aliceWorkflowId, "WORKFLOW_VIEW")).isTrue();

        String bobExternalUserId = "bob-" + UUID.randomUUID();

        authenticateFirstRequest(bobExternalUserId);

        connectedUserProjectFacade.createProjectWorkflow(bobExternalUserId, DEFINITION, ENVIRONMENT);

        assertThatThrownBy(() -> projectWorkflowFacade.getProjectWorkflow(aliceWorkflowId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> projectWorkflowFacade.updateWorkflow(aliceWorkflowId, DEFINITION, 0))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow(aliceWorkflowId, "WORKFLOW_EDIT", ENVIRONMENT))
                .isFalse();
        assertThatThrownBy(
            () -> connectedUserProjectFacade.publishProjectWorkflow(
                bobExternalUserId, aliceWorkflowUuid, "hijack", ENVIRONMENT_ID))
                    .isInstanceOf(ConfigurationException.class);

        verify(workflowFacade, never()).update(anyString(), anyString(), anyInt());

        long aliceProjectId = connectedUserProjectService.fetchConnectUserProject(aliceExternalUserId, ENVIRONMENT)
            .map(ConnectedUserProject::getProjectId)
            .orElseThrow();

        assertThat(projectDeploymentService.fetchProjectDeployment(aliceProjectId, ENVIRONMENT)).isEmpty();
    }

    private void authenticateFirstRequest(String externalUserId) {
        SecurityContextHolder.clearContext();

        Authentication authentication = embeddedApiKeyAuthenticationProvider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, externalUserId, null, "public"));

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    @TestConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        ConnectedUserAccessDeciderImpl.class, ConnectedUserConnectionMembership.class,
        ConnectedUserConnectionServiceImpl.class, ConnectedUserProjectServiceImpl.class,
        ConnectedUserProjectWorkflowServiceImpl.class, ConnectedUserServiceImpl.class,
        ConnectionPermissionScopeProvider.class, CurrentUserResolver.class, CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        ExecutionPermissionScopeProvider.class, IntegrationInstanceServiceImpl.class,
        KnowledgeBasePermissionScopeProvider.class, McpPermissionScopeProvider.class, PermissionScopeRegistry.class,
        PermissionServiceImpl.class, ProjectDeploymentEnvironmentResolver.class,
        ProjectDeploymentOwnershipResolver.class, ProjectDeploymentServiceImpl.class,
        ProjectDeploymentWorkflowServiceImpl.class, ProjectOwnershipResolver.class,
        ProjectCallback.class, ProjectPermissionScopeProvider.class, ProjectServiceImpl.class,
        ProjectWorkflowCallback.class, ProjectWorkflowServiceImpl.class, WorkflowPermissionScopeProvider.class,
        WorkspacePermissionScopeProvider.class, WorkspaceScopeCacheService.class
    })
    static class ConnectedUserBuilderFlowConfiguration {

        @Bean
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade() {
            return mock(AutomationWorkflowProjectFacade.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        ConnectedUserProjectFacade connectedUserProjectFacade(
            ConnectedUserProjectService connectedUserProjectService,
            ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager,
            ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
            ConnectedUserService connectedUserService, EnvironmentService environmentService,
            ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
            ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectFacade projectFacade,
            ProjectService projectService, ProjectWorkflowFacade projectWorkflowFacade,
            ProjectWorkflowService projectWorkflowService, WorkflowFacade workflowFacade,
            WorkflowService workflowService)
            throws ReflectiveOperationException {

            return instantiate(
                ConnectedUserProjectFacadeImpl.class,
                Map.ofEntries(
                    Map.entry(ConnectedUserProjectService.class, connectedUserProjectService),
                    Map.entry(ConnectedUserProjectWorkflowManager.class, connectedUserProjectWorkflowManager),
                    Map.entry(ConnectedUserProjectWorkflowService.class, connectedUserProjectWorkflowService),
                    Map.entry(ConnectedUserService.class, connectedUserService),
                    Map.entry(EnvironmentService.class, environmentService),
                    Map.entry(ProjectDeploymentFacade.class, projectDeploymentFacade),
                    Map.entry(ProjectDeploymentService.class, projectDeploymentService),
                    Map.entry(ProjectDeploymentWorkflowService.class, projectDeploymentWorkflowService),
                    Map.entry(ProjectFacade.class, projectFacade),
                    Map.entry(ProjectService.class, projectService),
                    Map.entry(ProjectWorkflowFacade.class, projectWorkflowFacade),
                    Map.entry(ProjectWorkflowService.class, projectWorkflowService),
                    Map.entry(WorkflowFacade.class, workflowFacade),
                    Map.entry(WorkflowService.class, workflowService)));
        }

        @Bean
        ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager(
            ConnectedUserConnectionMembership connectedUserConnectionMembership,
            ConnectedUserProjectService connectedUserProjectService,
            ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
            ConnectedUserService connectedUserService, ConnectionService connectionService,
            ProjectService projectService, ProjectWorkflowFacade projectWorkflowFacade,
            ProjectWorkflowService projectWorkflowService, WorkflowService workflowService)
            throws ReflectiveOperationException {

            return instantiate(
                ConnectedUserProjectWorkflowManager.class,
                Map.of(
                    ConnectedUserConnectionMembership.class, connectedUserConnectionMembership,
                    ConnectedUserProjectService.class, connectedUserProjectService,
                    ConnectedUserProjectWorkflowService.class, connectedUserProjectWorkflowService,
                    ConnectedUserService.class, connectedUserService, ConnectionService.class, connectionService,
                    ProjectService.class, projectService, ProjectWorkflowFacade.class, projectWorkflowFacade,
                    ProjectWorkflowService.class, projectWorkflowService, WorkflowService.class, workflowService));
        }

        @Bean
        ConnectionService connectionService() {
            return mock(ConnectionService.class);
        }

        @Bean
        EmbeddedApiKeyAuthenticationProvider embeddedApiKeyAuthenticationProvider(
            ConnectedUserService connectedUserService) {

            return new EmbeddedApiKeyAuthenticationProvider(mock(ApiKeyService.class), connectedUserService);
        }

        @Bean
        EnvironmentService environmentService() {
            EnvironmentService environmentService = mock(EnvironmentService.class);

            when(environmentService.getEnvironment(anyLong()))
                .thenAnswer(invocation -> Environment.values()[(int) (long) invocation.getArgument(0, Long.class)]);

            return environmentService;
        }

        @Bean
        ProjectDeploymentFacade projectDeploymentFacade(
            ApplicationProperties applicationProperties, PermissionService permissionService,
            ProjectDeploymentService projectDeploymentService,
            ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
            ProjectWorkflowService projectWorkflowService, WorkflowService workflowService)
            throws ReflectiveOperationException {

            return instantiate(
                ProjectDeploymentFacadeImpl.class,
                Map.of(
                    ApplicationProperties.class, applicationProperties, PermissionService.class, permissionService,
                    ProjectDeploymentService.class, projectDeploymentService,
                    ProjectDeploymentWorkflowService.class, projectDeploymentWorkflowService,
                    ProjectService.class, projectService, ProjectWorkflowService.class, projectWorkflowService,
                    WorkflowService.class, workflowService));
        }

        @Bean
        ProjectFacade projectFacade(
            ApplicationProperties applicationProperties, ProjectDeploymentFacade projectDeploymentFacade,
            ProjectDeploymentService projectDeploymentService, ProjectService projectService,
            ProjectWorkflowFacade projectWorkflowFacade, ProjectWorkflowService projectWorkflowService,
            WorkflowService workflowService)
            throws ReflectiveOperationException {

            return instantiate(
                ProjectFacadeImpl.class,
                Map.of(
                    ApplicationProperties.class, applicationProperties,
                    ProjectDeploymentFacade.class, projectDeploymentFacade,
                    ProjectDeploymentService.class, projectDeploymentService, ProjectService.class, projectService,
                    ProjectWorkflowFacade.class, projectWorkflowFacade,
                    ProjectWorkflowService.class, projectWorkflowService, WorkflowService.class, workflowService));
        }

        @Bean
        ProjectWorkflowFacade projectWorkflowFacade(
            EnvironmentService environmentService, ProjectDeploymentService projectDeploymentService,
            ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
            ProjectWorkflowService projectWorkflowService, WorkflowFacade workflowFacade,
            WorkflowService workflowService)
            throws ReflectiveOperationException {

            return instantiate(
                ProjectWorkflowFacadeImpl.class,
                Map.of(
                    EnvironmentService.class, environmentService,
                    ProjectDeploymentService.class, projectDeploymentService,
                    ProjectDeploymentWorkflowService.class, projectDeploymentWorkflowService,
                    ProjectService.class, projectService, ProjectWorkflowService.class, projectWorkflowService,
                    WorkflowFacade.class, workflowFacade, WorkflowService.class, workflowService));
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        WorkflowFacade workflowFacade() {
            return mock(WorkflowFacade.class);
        }

        @Bean
        WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }

        private static <T> T instantiate(Class<T> type, Map<Class<?>, Object> collaborators)
            throws ReflectiveOperationException {

            Constructor<?>[] constructors = type.getConstructors();

            Constructor<?> constructor = constructors[0];

            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];

            for (int index = 0; index < parameterTypes.length; index++) {
                Class<?> parameterType = parameterTypes[index];

                arguments[index] = collaborators.containsKey(parameterType)
                    ? collaborators.get(parameterType) : mock(parameterType);
            }

            return type.cast(constructor.newInstance(arguments));
        }
    }
}
