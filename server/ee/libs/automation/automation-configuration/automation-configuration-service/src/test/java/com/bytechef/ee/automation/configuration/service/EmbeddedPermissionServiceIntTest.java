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

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.ProjectDeploymentEnvironmentResolver;
import com.bytechef.automation.configuration.security.ProjectDeploymentOwnershipResolver;
import com.bytechef.automation.configuration.security.ProjectOwnershipResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
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
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
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
        EmbeddedPermissionServiceIntTest.RealEmbeddedPermissionServiceConfiguration.class
    })
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class EmbeddedPermissionServiceIntTest {

    private static final String EMBEDDED_ADMIN = "embedded-only-admin";
    private static final String EXPLICIT_MEMBER = "explicit-development-editor";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final String WORKSPACE_EDITOR = "workspace-only-editor";
    private static final String EMBEDDED_VIEW_SCOPE = "WORKFLOW_VIEW";
    private static final String EMBEDDED_EDIT_SCOPE = "WORKFLOW_EDIT";
    private static final String WORKSPACE_VIEW_SCOPE = "WORKSPACE_VIEW";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        EMBEDDED_ADMIN, 9401L, EXPLICIT_MEMBER, 9402L, TENANT_ADMIN, 9403L, WORKSPACE_EDITOR, 9404L);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private PermissionScopeRegistry permissionScopeRegistry;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    private long workspaceId;

    @BeforeEach
    void seedMembers() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            jdbcTemplate.update("DELETE FROM workspace_user WHERE user_id = ?", userId);

            embeddedUserRepository.deleteByUserId(userId);
        }

        workspaceId = insertWorkspace("independence-" + UUID.randomUUID());

        workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_IDS_BY_LOGIN.get(WORKSPACE_EDITOR), workspaceId, WorkspaceRole.EDITOR));

        embeddedUserRepository.save(EmbeddedUser.forRole(USER_IDS_BY_LOGIN.get(EMBEDDED_ADMIN), WorkspaceRole.ADMIN));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(USER_IDS_BY_LOGIN.get(EXPLICIT_MEMBER), WorkspaceRole.EDITOR,
                Environment.DEVELOPMENT));

        permissionService.evictAllEmbeddedScopeCache();
        permissionService.evictAllWorkspaceScopeCache();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAWorkspaceEditorHoldsNoEmbeddedScope() {
        authenticate(WORKSPACE_EDITOR, false);

        assertThat(permissionService.hasWorkspaceScope(workspaceId, EMBEDDED_VIEW_SCOPE)).isTrue();
        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW_SCOPE)).isFalse();
        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW_SCOPE, Environment.DEVELOPMENT)).isFalse();
        assertThat(permissionService.getMyEmbeddedScopes(null)).isEmpty();
    }

    @Test
    void testAnEmbeddedAdminHoldsNoWorkspaceScope() {
        authenticate(EMBEDDED_ADMIN, false);

        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW_SCOPE)).isTrue();
        assertThat(permissionService.hasWorkspaceScope(workspaceId, WORKSPACE_VIEW_SCOPE)).isFalse();
        assertThat(permissionService.getMyWorkspaceScopes(workspaceId)).isEmpty();
    }

    @Test
    void testAnExplicitMemberIsDeniedOutsideTheirEnvironments() {
        authenticate(EXPLICIT_MEMBER, false);

        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_EDIT_SCOPE, Environment.DEVELOPMENT)).isTrue();
        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_EDIT_SCOPE, Environment.STAGING)).isFalse();
        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_EDIT_SCOPE, Environment.PRODUCTION)).isFalse();
        assertThat(permissionService.hasEmbeddedScopeInEveryEnvironment(EMBEDDED_EDIT_SCOPE)).isFalse();
        assertThat(permissionService.getMyEmbeddedScopes(Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testATenantAdminHoldsEveryEmbeddedScopeInEveryEnvironment() {
        authenticate(TENANT_ADMIN, true);

        Set<String> allScopeNames = permissionScopeRegistry.getAllScopeNames(PermissionScopeSide.EMBEDDED);

        assertThat(allScopeNames).contains(EMBEDDED_VIEW_SCOPE, EMBEDDED_EDIT_SCOPE);
        assertThat(permissionService.getMyEmbeddedScopes(null)).isEqualTo(allScopeNames);

        for (Environment environment : Environment.values()) {
            assertThat(permissionService.getMyEmbeddedScopes(environment)).isEqualTo(allScopeNames);

            for (String scopeName : allScopeNames) {
                assertThat(permissionService.hasEmbeddedScope(scopeName, environment)).isTrue();
            }
        }

        for (String scopeName : allScopeNames) {
            assertThat(permissionService.hasEmbeddedScopeInEveryEnvironment(scopeName)).isTrue();
        }
    }

    @Test
    void testAnUnauthenticatedCallerGetsNoEmbeddedScopes() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> permissionService.getMyEmbeddedScopes(null))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> permissionService.getMyEmbeddedScopes(Environment.DEVELOPMENT))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private static void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private long insertWorkspace(String name) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Long insertedWorkspaceId = jdbcTemplate.queryForObject(
            "INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, NOW(), 'test', NOW(), 'test', 0) RETURNING id",
            Long.class, name);

        assertThat(insertedWorkspaceId).isNotNull();

        return insertedWorkspaceId;
    }

    @TestConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        AppEventPermissionScopeProvider.class, ConnectedUserPermissionScopeProvider.class,
        ConnectionPermissionScopeProvider.class, CurrentUserResolver.class, CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        EmbeddedPermissionScopeProvider.class, EmbeddedScopeCacheService.class,
        EmbeddedSharedPermissionScopeProvider.class,
        ExecutionPermissionScopeProvider.class, IntegrationInstanceConfigurationPermissionScopeProvider.class,
        IntegrationPermissionScopeProvider.class, KnowledgeBasePermissionScopeProvider.class,
        McpPermissionScopeProvider.class, PermissionScopeRegistry.class, PermissionServiceImpl.class,
        ProjectDeploymentEnvironmentResolver.class, ProjectDeploymentOwnershipResolver.class,
        ProjectDeploymentServiceImpl.class, ProjectOwnershipResolver.class, ProjectPermissionScopeProvider.class,
        WorkflowPermissionScopeProvider.class, WorkspacePermissionScopeProvider.class,
        WorkspaceScopeCacheService.class
    })
    static class RealEmbeddedPermissionServiceConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        UserService userService() {
            UserService userService = mock(UserService.class);

            for (Map.Entry<String, Long> entry : USER_IDS_BY_LOGIN.entrySet()) {
                User user = new User();

                user.setId(entry.getValue());
                user.setLogin(entry.getKey());

                when(userService.fetchUserByLogin(entry.getKey())).thenReturn(Optional.of(user));
                when(userService.getUser(entry.getKey())).thenReturn(user);
            }

            return userService;
        }
    }
}
