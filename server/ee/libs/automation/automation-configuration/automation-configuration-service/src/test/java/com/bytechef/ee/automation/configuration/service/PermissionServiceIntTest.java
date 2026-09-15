/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.ProjectDeploymentEnvironmentResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentOwnershipResolver;
import com.bytechef.automation.configuration.security.ProjectOwnershipResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
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
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        PermissionServiceIntTest.RealPermissionServiceConfiguration.class
    })
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class PermissionServiceIntTest {

    private static final String DEVELOPMENT_EDITOR = "development-editor";
    private static final String DEVELOPMENT_VIEWER = "development-viewer";
    private static final String OTHER_WORKSPACE_EDITOR = "other-workspace-editor";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        DEVELOPMENT_EDITOR, 9102L, DEVELOPMENT_VIEWER, 9101L, OTHER_WORKSPACE_EDITOR, 9103L);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    private long developmentDeploymentId;
    private long productionDeploymentId;

    @BeforeEach
    void seedWorkspacesProjectDeploymentsAndMembers() {
        String suffix = String.valueOf(UUID.randomUUID());

        long workspaceId = insertWorkspace("enforced-" + suffix);
        long otherWorkspaceId = insertWorkspace("other-" + suffix);

        Project project = Project.builder()
            .name("enforced-project-" + suffix)
            .workspaceId(workspaceId)
            .build();

        project.setUuid(UUID.randomUUID());

        project = projectRepository.save(project);

        developmentDeploymentId = saveDeployment(project, Environment.DEVELOPMENT);
        productionDeploymentId = saveDeployment(project, Environment.PRODUCTION);

        workspaceUserRepository.save(
            WorkspaceUser.forRole(
                USER_IDS_BY_LOGIN.get(DEVELOPMENT_VIEWER), workspaceId, WorkspaceRole.VIEWER,
                Environment.DEVELOPMENT));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(
                USER_IDS_BY_LOGIN.get(DEVELOPMENT_EDITOR), workspaceId, WorkspaceRole.EDITOR,
                Environment.DEVELOPMENT));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_IDS_BY_LOGIN.get(OTHER_WORKSPACE_EDITOR), otherWorkspaceId,
                WorkspaceRole.EDITOR));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDevelopmentViewerIsDeniedADeploymentWriteInDevelopment() {
        authenticate(DEVELOPMENT_VIEWER);

        ProjectDeployment projectDeployment = renamedDeployment(developmentDeploymentId);

        assertThatThrownBy(() -> projectDeploymentService.update(projectDeployment))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(storedDeploymentName(developmentDeploymentId)).isNotEqualTo(projectDeployment.getName());
    }

    @Test
    void testDevelopmentEditorIsAllowedTheDeploymentWriteInDevelopment() {
        authenticate(DEVELOPMENT_EDITOR);

        ProjectDeployment projectDeployment = renamedDeployment(developmentDeploymentId);

        projectDeploymentService.update(projectDeployment);

        assertThat(storedDeploymentName(developmentDeploymentId)).isEqualTo(projectDeployment.getName());
    }

    @Test
    void testDevelopmentViewerIsDeniedReadingAProductionDeploymentById() {
        authenticate(DEVELOPMENT_VIEWER);

        assertThatThrownBy(() -> projectDeploymentFacade.getProjectDeployment(productionDeploymentId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testDevelopmentViewerIsAllowedReadingADevelopmentDeploymentById() {
        authenticate(DEVELOPMENT_VIEWER);

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacade.getProjectDeployment(
            developmentDeploymentId);

        assertThat(projectDeploymentDTO.id()).isEqualTo(developmentDeploymentId);
    }

    @Test
    void testMemberOfAnotherWorkspaceIsDeniedTheDeploymentById() {
        authenticate(OTHER_WORKSPACE_EDITOR);

        ProjectDeployment projectDeployment = renamedDeployment(developmentDeploymentId);

        assertThatThrownBy(() -> projectDeploymentService.update(projectDeployment))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> projectDeploymentFacade.getProjectDeployment(developmentDeploymentId))
            .isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticate(String login) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    login, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
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

    private ProjectDeployment renamedDeployment(long projectDeploymentId) {
        ProjectDeployment projectDeployment = projectDeploymentRepository.findById(projectDeploymentId)
            .orElseThrow();

        projectDeployment.setName("renamed-" + UUID.randomUUID());

        return projectDeployment;
    }

    private long saveDeployment(Project project, Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);
        projectDeployment.setName(environment.name() + "-" + UUID.randomUUID());
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        return projectDeployment.getId();
    }

    private String storedDeploymentName(long projectDeploymentId) {
        ProjectDeployment projectDeployment = projectDeploymentRepository.findById(projectDeploymentId)
            .orElseThrow();

        return projectDeployment.getName();
    }

    @TestConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        ConnectionPermissionScopeProvider.class, CurrentUserResolver.class, CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        ExecutionPermissionScopeProvider.class, KnowledgeBasePermissionScopeProvider.class,
        McpPermissionScopeProvider.class, PermissionScopeRegistry.class, PermissionServiceImpl.class,
        ProjectDeploymentEnvironmentResolver.class, ProjectDeploymentOwnershipResolver.class,
        ProjectDeploymentServiceImpl.class, ProjectOwnershipResolver.class, ProjectPermissionScopeProvider.class,
        WorkflowPermissionScopeProvider.class, WorkspacePermissionScopeProvider.class,
        WorkspaceScopeCacheService.class
    })
    static class RealPermissionServiceConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        ProjectDeploymentFacade projectDeploymentFacade(
            PermissionService permissionService, ProjectDeploymentService projectDeploymentService)
            throws ReflectiveOperationException {

            Constructor<?>[] constructors = ProjectDeploymentFacadeImpl.class.getConstructors();

            Constructor<?> constructor = constructors[0];

            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];

            for (int index = 0; index < parameterTypes.length; index++) {
                Class<?> parameterType = parameterTypes[index];

                if (parameterType == PermissionService.class) {
                    arguments[index] = permissionService;
                } else if (parameterType == ProjectDeploymentService.class) {
                    arguments[index] = projectDeploymentService;
                } else {
                    arguments[index] = mock(parameterType);
                }
            }

            return (ProjectDeploymentFacade) constructor.newInstance(arguments);
        }

        @Bean
        UserService userService() {
            UserService userService = mock(UserService.class);

            for (Map.Entry<String, Long> entry : USER_IDS_BY_LOGIN.entrySet()) {
                User user = new User();

                user.setId(entry.getValue());
                user.setLogin(entry.getKey());

                when(userService.getUser(entry.getKey())).thenReturn(user);
            }

            return userService;
        }
    }
}
