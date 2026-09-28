/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacadeImpl;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacadeImpl;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.ProjectDeploymentEnvironmentResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentOwnershipResolver;
import com.bytechef.automation.configuration.security.ProjectOwnershipResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.automation.data.table.web.graphql.DataTableGraphQlController;
import com.bytechef.automation.data.table.web.graphql.DataTableGraphQlController.RemoveTableInput;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security.ConnectedUserAuthorizationMode;
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
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserAccessDeciderImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionServiceImpl;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EeAutomationConfigurationIntTestConfiguration.class,
        ConnectedUserOwnershipIntTest.ConnectedUserOwnershipConfiguration.class
    },
    properties = {
        "bytechef.security.connected-user-authorization-mode=ENFORCE",
        "spring.liquibase.contexts=configuration,user"
    })
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class ConnectedUserOwnershipIntTest {

    private static final long DATA_TABLE_ID = 4242L;
    private static final String DEFINITION = "{\"tasks\":[]}";

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DataTableGraphQlController dataTableGraphQlController;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkflowFacade workflowFacade;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    private ConnectedUserFixture alice;
    private ConnectedUserFixture bob;
    private ListAppender<ILoggingEvent> logAppender;
    private long otherWorkspaceProjectId;
    private Logger permissionServiceLogger;

    @BeforeEach
    void seedConnectedUsersAndAWorkspaceMembersProject() {
        String suffix = String.valueOf(UUID.randomUUID());

        long defaultWorkspaceId = insertWorkspace("default-" + suffix);
        long otherWorkspaceId = insertWorkspace("other-" + suffix);

        alice = seedConnectedUser("alice-" + suffix, defaultWorkspaceId);
        bob = seedConnectedUser("bob-" + suffix, defaultWorkspaceId);

        otherWorkspaceProjectId = saveProject("automation-" + suffix, otherWorkspaceId);

        reset(connectionFacade, dataTableService, workflowFacade);

        when(workflowFacade.getWorkflow(anyString())).thenReturn(mock(WorkflowDTO.class));

        logAppender = new ListAppender<>();

        logAppender.start();

        permissionServiceLogger = (Logger) LoggerFactory.getLogger(PermissionServiceImpl.class);

        permissionServiceLogger.addAppender(logAppender);

        authenticate(alice);
    }

    @AfterEach
    void restoreEnforceModeAndClearSecurityContext() {
        ApplicationProperties.Security security = applicationProperties.getSecurity();

        security.setConnectedUserAuthorizationMode(ConnectedUserAuthorizationMode.ENFORCE);

        permissionServiceLogger.detachAppender(logAppender);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectedUserUpdatesOwnWorkflowAndEnablesOwnDeployment() {
        ProjectWorkflowDTO projectWorkflowDTO = projectWorkflowFacade.updateWorkflow(alice.workflowId(), DEFINITION, 0);

        assertThat(projectWorkflowDTO.getProjectWorkflowId()).isEqualTo(alice.projectWorkflowId());

        verify(workflowFacade).update(alice.workflowId(), DEFINITION, 0);

        projectDeploymentFacade.enableProjectDeployment(alice.projectDeploymentId(), true);

        assertThat(isDeploymentEnabled(alice.projectDeploymentId())).isTrue();
    }

    @Test
    void testConnectedUserIsDeniedAnotherConnectedUsersWorkflowAndDeployment() {
        assertThatThrownBy(() -> projectWorkflowFacade.updateWorkflow(bob.workflowId(), DEFINITION, 0))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> projectDeploymentFacade.enableProjectDeployment(bob.projectDeploymentId(), true))
            .isInstanceOf(AccessDeniedException.class);

        verify(workflowFacade, never()).update(anyString(), anyString(), anyInt());

        assertThat(isDeploymentEnabled(bob.projectDeploymentId())).isFalse();
    }

    @Test
    void testConnectedUserIsDeniedCreatingADeploymentOfOwnProjectInAnotherEnvironment() {
        ProjectDeploymentDTO developmentDeployment = new ProjectDeploymentDTO(
            newDeployment(alice.projectId(), Environment.DEVELOPMENT));

        assertThatThrownBy(() -> projectDeploymentFacade.createProjectDeployment(developmentDeployment))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(projectDeploymentRepository.findByProjectIdAndEnvironment(
            alice.projectId(), Environment.DEVELOPMENT.ordinal())).isEmpty();
        assertThat(
            permissionService.hasWorkspaceScopeForProject(
                alice.projectId(), "DEPLOYMENT_CREATE", Environment.PRODUCTION))
                    .isTrue();
    }

    @Test
    void testConnectedUserIsDeniedAnExistingDeploymentOfOwnProjectInAnotherEnvironment() {
        ProjectDeployment developmentDeployment = projectDeploymentRepository.save(
            newDeployment(alice.projectId(), Environment.DEVELOPMENT));

        long developmentDeploymentId = developmentDeployment.getId();

        assertThatThrownBy(() -> projectDeploymentFacade.enableProjectDeployment(developmentDeploymentId, true))
            .isInstanceOf(AccessDeniedException.class);

        ProjectDeploymentDTO projectDeploymentDTO = new ProjectDeploymentDTO(developmentDeployment);

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeployment(projectDeploymentDTO))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(isDeploymentEnabled(developmentDeploymentId)).isFalse();
    }

    @Test
    void testConnectedUserIsDeniedAWorkspaceMembersProject() {
        assertThatThrownBy(() -> projectWorkflowFacade.getProjectWorkflows(otherWorkspaceProjectId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testConnectedUserIsDeniedDroppingADataTable() {
        RemoveTableInput removeTableInput = new RemoveTableInput(
            (long) Environment.PRODUCTION.ordinal(), DATA_TABLE_ID);

        assertThatThrownBy(() -> dataTableGraphQlController.dropDataTable(removeTableInput))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(dataTableService);
    }

    @Test
    void testConnectedUserUsesOwnConnectionButIsDeniedAnotherConnectedUsersConnection() {
        workspaceConnectionFacade.getConnection(alice.connectionId());

        verify(connectionFacade).getConnection(alice.connectionId());

        assertThatThrownBy(() -> workspaceConnectionFacade.getConnection(bob.connectionId()))
            .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).getConnection(bob.connectionId());

        assertThat(
            permissionService.canUseConnectionInWorkflow(
                alice.connectionId(), alice.workflowId(), Environment.PRODUCTION))
                    .isTrue();
        assertThat(
            permissionService.canUseConnectionInWorkflow(
                bob.connectionId(), alice.workflowId(), Environment.PRODUCTION))
                    .isFalse();
        assertThat(
            permissionService.canUseConnectionInWorkflow(
                alice.connectionId(), bob.workflowId(), Environment.PRODUCTION))
                    .isFalse();
    }

    @Test
    void testConnectedUserIsStillDeniedUnderSkippedChecksInEnforceMode() {
        RemoveTableInput removeTableInput = new RemoveTableInput(
            (long) Environment.PRODUCTION.ordinal(), DATA_TABLE_ID);

        assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(() -> {
            projectDeploymentFacade.enableProjectDeployment(bob.projectDeploymentId(), true);

            return null;
        })).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(
            () -> projectWorkflowFacade.updateWorkflow(bob.workflowId(), DEFINITION, 0)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(
            () -> projectWorkflowFacade.getProjectWorkflows(otherWorkspaceProjectId)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(
            () -> dataTableGraphQlController.dropDataTable(removeTableInput)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(
            () -> workspaceConnectionFacade.getConnection(bob.connectionId())))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(isDeploymentEnabled(bob.projectDeploymentId())).isFalse();

        verify(workflowFacade, never()).update(anyString(), anyString(), anyInt());
        verify(connectionFacade, never()).getConnection(anyLong());
        verifyNoInteractions(dataTableService);
    }

    @Test
    void testLogModeAllowsAnotherConnectedUsersDeploymentUnderSkippedChecksAndLogsWouldDeny() throws Throwable {
        ApplicationProperties.Security security = applicationProperties.getSecurity();

        security.setConnectedUserAuthorizationMode(ConnectedUserAuthorizationMode.LOG);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            projectDeploymentFacade.enableProjectDeployment(bob.projectDeploymentId(), true);

            return null;
        });

        assertThat(isDeploymentEnabled(bob.projectDeploymentId())).isTrue();
        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .anyMatch(message -> message.contains("would deny ProjectDeployment:DEPLOYMENT_EDIT"));
    }

    private static void authenticate(ConnectedUserFixture connectedUserFixture) {
        User user = new User(connectedUserFixture.externalUserId(), "n/a", List.of());

        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    Environment.PRODUCTION.ordinal(), connectedUserFixture.connectedUserId(), user));
    }

    private long insertConnectedUser(String externalUserId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Long connectedUserId = jdbcTemplate.queryForObject(
            "INSERT INTO connected_user (external_id, name, enabled, environment, created_date, created_by, "
                + "last_modified_date, last_modified_by, version) "
                + "VALUES (?, ?, true, ?, NOW(), 'test', NOW(), 'test', 0) RETURNING id",
            Long.class, externalUserId, externalUserId, Environment.PRODUCTION.ordinal());

        assertThat(connectedUserId).isNotNull();

        return connectedUserId;
    }

    private long insertWorkspace(String name) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Long workspaceId = jdbcTemplate.queryForObject(
            "INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, NOW(), 'test', NOW(), 'test', 0) RETURNING id",
            Long.class, name);

        assertThat(workspaceId).isNotNull();

        return workspaceId;
    }

    private boolean isDeploymentEnabled(long projectDeploymentId) {
        ProjectDeployment projectDeployment = projectDeploymentRepository.findById(projectDeploymentId)
            .orElseThrow();

        return projectDeployment.isEnabled();
    }

    private static ProjectDeployment newDeployment(long projectId, Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(false);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setName("deployment-" + UUID.randomUUID());
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(1);

        return projectDeployment;
    }

    private long saveDeployment(long projectId) {
        ProjectDeployment projectDeployment = projectDeploymentRepository.save(
            newDeployment(projectId, Environment.PRODUCTION));

        return projectDeployment.getId();
    }

    private long saveProject(String name, long workspaceId) {
        Project project = Project.builder()
            .name(name)
            .workspaceId(workspaceId)
            .build();

        project.setUuid(UUID.randomUUID());

        project = projectRepository.save(project);

        return project.getId();
    }

    private ConnectedUserFixture seedConnectedUser(String externalUserId, long workspaceId) {
        long connectedUserId = insertConnectedUser(externalUserId);

        long projectId = saveProject("__EMBEDDED__" + externalUserId, workspaceId);

        connectedUserProjectService.create(connectedUserId, projectId);

        String workflowId = String.valueOf(UUID.randomUUID());

        ProjectWorkflow projectWorkflow = projectWorkflowRepository.save(
            new ProjectWorkflow(projectId, 1, workflowId, UUID.randomUUID()));

        long connectionId = connectedUserId * 1000;

        connectedUserConnectionService.create(connectedUserId, connectionId);

        return new ConnectedUserFixture(
            connectedUserId, connectionId, externalUserId, saveDeployment(projectId), projectId,
            projectWorkflow.getId(), workflowId);
    }

    private record ConnectedUserFixture(
        long connectedUserId, long connectionId, String externalUserId, long projectDeploymentId, long projectId,
        long projectWorkflowId, String workflowId) {
    }

    @TestConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        ConnectedUserAccessDeciderImpl.class, ConnectedUserConnectionServiceImpl.class,
        ConnectedUserProjectServiceImpl.class, ConnectionPermissionScopeProvider.class, CurrentUserResolver.class,
        CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        ExecutionPermissionScopeProvider.class, IntegrationInstanceServiceImpl.class,
        KnowledgeBasePermissionScopeProvider.class, McpPermissionScopeProvider.class, PermissionScopeRegistry.class,
        PermissionServiceImpl.class, ProjectDeploymentEnvironmentResolver.class,
        ProjectDeploymentOwnershipResolver.class, ProjectDeploymentServiceImpl.class, ProjectOwnershipResolver.class,
        ProjectPermissionScopeProvider.class, ProjectServiceImpl.class, ProjectWorkflowServiceImpl.class,
        WorkflowPermissionScopeProvider.class, WorkspacePermissionScopeProvider.class,
        WorkspaceScopeCacheService.class
    })
    static class ConnectedUserOwnershipConfiguration {

        @Bean
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade() {
            return mock(AutomationWorkflowProjectFacade.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        ConnectionFacade connectionFacade() {
            return mock(ConnectionFacade.class);
        }

        @Bean
        DataTableGraphQlController dataTableGraphQlController(DataTableService dataTableService)
            throws ReflectiveOperationException {

            return instantiate(DataTableGraphQlController.class, Map.of(DataTableService.class, dataTableService));
        }

        @Bean
        DataTableService dataTableService() {
            return mock(DataTableService.class);
        }

        @Bean
        ProjectDeploymentFacade projectDeploymentFacade(
            PermissionService permissionService, ProjectDeploymentService projectDeploymentService)
            throws ReflectiveOperationException {

            return instantiate(
                ProjectDeploymentFacadeImpl.class,
                Map.of(
                    PermissionService.class, permissionService,
                    ProjectDeploymentService.class, projectDeploymentService));
        }

        @Bean
        ProjectWorkflowFacade projectWorkflowFacade(
            ProjectDeploymentService projectDeploymentService, ProjectService projectService,
            ProjectWorkflowService projectWorkflowService, WorkflowFacade workflowFacade)
            throws ReflectiveOperationException {

            return instantiate(
                ProjectWorkflowFacadeImpl.class,
                Map.of(
                    ProjectDeploymentService.class, projectDeploymentService, ProjectService.class, projectService,
                    ProjectWorkflowService.class, projectWorkflowService, WorkflowFacade.class, workflowFacade));
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
        WorkspaceConnectionFacade workspaceConnectionFacade(ConnectionFacade connectionFacade)
            throws ReflectiveOperationException {

            return instantiate(
                WorkspaceConnectionFacadeImpl.class, Map.of(ConnectionFacade.class, connectionFacade));
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
