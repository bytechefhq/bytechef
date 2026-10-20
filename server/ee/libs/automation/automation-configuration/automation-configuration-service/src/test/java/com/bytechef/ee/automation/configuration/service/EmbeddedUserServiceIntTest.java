/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        EmbeddedUserServiceIntTest.RealEmbeddedUserServiceConfiguration.class
    })
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class EmbeddedUserServiceIntTest {

    private static final String ADMIN = "tenant-admin";
    private static final String EMBEDDED_ADMIN = "embedded-admin";
    private static final String EMBEDDED_EDITOR = "embedded-editor";
    private static final String MEMBER = "member";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        ADMIN, 9300L, EMBEDDED_ADMIN, 9301L, EMBEDDED_EDITOR, 9302L, MEMBER, 9303L);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private EmbeddedUserService embeddedUserService;

    @Autowired
    private PermissionScopeRegistry permissionScopeRegistry;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void cleanUp() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS \"user\" (id BIGINT PRIMARY KEY)");

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            jdbcTemplate.update("INSERT INTO \"user\" (id) VALUES (?) ON CONFLICT DO NOTHING", userId);
        }

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAddingAMemberGivesThemTheRoleInEveryEnvironment() {
        authenticate(ADMIN, true);

        embeddedUserService.addEmbeddedUser(userId(MEMBER), WorkspaceRole.EDITOR);

        authenticate(MEMBER, false);

        for (Environment environment : Environment.values()) {
            assertThat(permissionService.getMyEmbeddedScopes(environment))
                .isEqualTo(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR, PermissionScopeSide.EMBEDDED));
        }

        assertThat(embeddedUserService.isEmbeddedMember(userId(MEMBER))).isTrue();

        assertThatThrownBy(() -> embeddedUserService.addEmbeddedUser(userId(MEMBER), WorkspaceRole.EDITOR))
            .hasMessageContaining("already");
    }

    @Test
    void testSettingAnEnvironmentRoleTurnsAnImplicitMemberExplicit() {
        authenticate(ADMIN, true);

        embeddedUserService.addEmbeddedUser(userId(MEMBER), WorkspaceRole.EDITOR);
        embeddedUserService.setEnvironmentRole(userId(MEMBER), Environment.PRODUCTION, WorkspaceRole.VIEWER);

        List<EmbeddedUser> embeddedUsers = embeddedUserService.getUserEmbeddedUsers(userId(MEMBER));

        assertThat(embeddedUsers).hasSize(3);
        assertThat(embeddedUsers).noneMatch(embeddedUser -> embeddedUser.getEnvironment() == null);
        assertThat(roleIn(embeddedUsers, Environment.DEVELOPMENT)).isEqualTo(WorkspaceRole.EDITOR);
        assertThat(roleIn(embeddedUsers, Environment.STAGING)).isEqualTo(WorkspaceRole.EDITOR);
        assertThat(roleIn(embeddedUsers, Environment.PRODUCTION)).isEqualTo(WorkspaceRole.VIEWER);

        assertThatThrownBy(() -> embeddedUserService.updateEmbeddedUserRole(userId(MEMBER), WorkspaceRole.ADMIN))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testRemovingTheLastEnvironmentRoleWidensItToAnEmbeddedWideRole() {
        authenticate(ADMIN, true);

        embeddedUserService.addEmbeddedUser(userId(MEMBER), WorkspaceRole.EDITOR);
        embeddedUserService.setEnvironmentRole(userId(MEMBER), Environment.PRODUCTION, WorkspaceRole.VIEWER);
        embeddedUserService.removeEnvironmentRole(userId(MEMBER), Environment.DEVELOPMENT);

        List<EmbeddedUser> embeddedUsers = embeddedUserService.getUserEmbeddedUsers(userId(MEMBER));

        assertThat(embeddedUsers).hasSize(2);

        embeddedUserService.removeEnvironmentRole(userId(MEMBER), Environment.STAGING);

        embeddedUsers = embeddedUserService.getUserEmbeddedUsers(userId(MEMBER));

        assertThat(embeddedUsers).hasSize(1);

        embeddedUserService.removeEnvironmentRole(userId(MEMBER), Environment.PRODUCTION);

        embeddedUsers = embeddedUserService.getUserEmbeddedUsers(userId(MEMBER));

        assertThat(embeddedUsers).hasSize(1);

        EmbeddedUser embeddedUser = embeddedUsers.get(0);

        assertThat(embeddedUser.getEnvironment()).isNull();
        assertThat(embeddedUser.getEmbeddedRole()).isEqualTo(WorkspaceRole.VIEWER);
    }

    @Test
    void testAnEmbeddedEditorCannotGrantAdmin() {
        authenticate(ADMIN, true);

        embeddedUserService.addEmbeddedUser(userId(EMBEDDED_EDITOR), WorkspaceRole.EDITOR);

        authenticate(EMBEDDED_EDITOR, false);

        assertThatThrownBy(() -> embeddedUserService.addEmbeddedUser(userId(MEMBER), WorkspaceRole.ADMIN))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(embeddedUserService.isEmbeddedMember(userId(MEMBER))).isFalse();
    }

    @Test
    void testAnEmbeddedAdminMayRemoveThemselves() {
        authenticate(ADMIN, true);

        embeddedUserService.addEmbeddedUser(userId(EMBEDDED_ADMIN), WorkspaceRole.ADMIN);

        authenticate(EMBEDDED_ADMIN, false);

        assertThatCode(() -> embeddedUserService.removeEmbeddedUser(userId(EMBEDDED_ADMIN)))
            .doesNotThrowAnyException();

        assertThat(embeddedUserService.isEmbeddedMember(userId(EMBEDDED_ADMIN))).isFalse();
        assertThat(embeddedUserService.removeEmbeddedUser(userId(EMBEDDED_ADMIN))).isFalse();
    }

    @Test
    void testRemovingATenantUserRemovesTheirEmbeddedRows() {
        authenticate(ADMIN, true);

        embeddedUserService.addEmbeddedUser(userId(MEMBER), WorkspaceRole.EDITOR);

        WorkspaceMembershipAssignerImpl workspaceMembershipAssigner = new WorkspaceMembershipAssignerImpl(
            embeddedUserRepository, permissionService, mock(WorkspaceService.class), workspaceUserRepository,
            mock(WorkspaceUserService.class));

        workspaceMembershipAssigner.removeMemberships(userId(MEMBER));

        assertThat(embeddedUserService.getUserEmbeddedUsers(userId(MEMBER))).isEmpty();

        authenticate(MEMBER, false);

        assertThat(permissionService.getMyEmbeddedScopes(Environment.DEVELOPMENT)).isEmpty();
    }

    @Test
    void testAnEmbeddedAdminCannotDemoteThemselves() {
        authenticate(ADMIN, true);

        embeddedUserService.addEmbeddedUser(userId(EMBEDDED_ADMIN), WorkspaceRole.ADMIN);

        authenticate(EMBEDDED_ADMIN, false);

        assertThatThrownBy(
            () -> embeddedUserService.updateEmbeddedUserRole(userId(EMBEDDED_ADMIN), WorkspaceRole.VIEWER))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Cannot withdraw your own ADMIN role");

        assertThatThrownBy(
            () -> embeddedUserService.setEnvironmentRole(
                userId(EMBEDDED_ADMIN), Environment.PRODUCTION, WorkspaceRole.VIEWER))
                    .isInstanceOf(ConfigurationException.class);

        assertThat(embeddedUserService.getUserEmbeddedUsers(userId(EMBEDDED_ADMIN))).hasSize(1);
    }

    @Test
    void testAProductionOnlyAdminCannotWidenAnotherMembersLastEnvironmentRole() {
        authenticate(ADMIN, true);

        seedProductionOnlyAdmin();

        embeddedUserService.addEmbeddedUser(userId(MEMBER), WorkspaceRole.EDITOR);
        embeddedUserService.setEnvironmentRole(userId(MEMBER), Environment.PRODUCTION, WorkspaceRole.EDITOR);
        embeddedUserService.removeEnvironmentRole(userId(MEMBER), Environment.DEVELOPMENT);
        embeddedUserService.removeEnvironmentRole(userId(MEMBER), Environment.STAGING);

        authenticate(EMBEDDED_ADMIN, false);

        assertThatThrownBy(() -> embeddedUserService.removeEnvironmentRole(userId(MEMBER), Environment.PRODUCTION))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("EMBEDDED_MEMBER_MANAGE");

        assertThat(embeddedUserService.getUserEmbeddedUsers(userId(MEMBER))).hasSize(1);
    }

    @Test
    void testSettingAnEnvironmentRoleIsDeniedWhereTheCallerLacksTheGrantedScopes() {
        authenticate(ADMIN, true);

        seedProductionOnlyAdmin();

        embeddedUserService.addEmbeddedUser(userId(MEMBER), WorkspaceRole.VIEWER);

        authenticate(EMBEDDED_ADMIN, false);

        assertThatThrownBy(
            () -> embeddedUserService.setEnvironmentRole(userId(MEMBER), Environment.DEVELOPMENT, WorkspaceRole.ADMIN))
                .isInstanceOf(AccessDeniedException.class);

        assertThatCode(
            () -> embeddedUserService.setEnvironmentRole(userId(MEMBER), Environment.PRODUCTION, WorkspaceRole.ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    void testARoleChangeIsVisibleImmediately() {
        authenticate(ADMIN, true);

        embeddedUserService.addEmbeddedUser(userId(MEMBER), WorkspaceRole.VIEWER);

        authenticate(MEMBER, false);

        Set<String> viewerScopes = permissionService.getMyEmbeddedScopes(Environment.DEVELOPMENT);

        authenticate(ADMIN, true);

        embeddedUserService.updateEmbeddedUserRole(userId(MEMBER), WorkspaceRole.ADMIN);

        authenticate(MEMBER, false);

        Set<String> adminScopes = permissionService.getMyEmbeddedScopes(Environment.DEVELOPMENT);

        assertThat(adminScopes).containsAll(viewerScopes);
        assertThat(adminScopes.size()).isGreaterThan(viewerScopes.size());
    }

    private void seedProductionOnlyAdmin() {
        embeddedUserService.addEmbeddedUser(userId(EMBEDDED_ADMIN), WorkspaceRole.VIEWER);
        embeddedUserService.setEnvironmentRole(userId(EMBEDDED_ADMIN), Environment.PRODUCTION, WorkspaceRole.ADMIN);
    }

    private static WorkspaceRole roleIn(List<EmbeddedUser> embeddedUsers, Environment environment) {
        return embeddedUsers.stream()
            .filter(embeddedUser -> embeddedUser.getEnvironment() == environment)
            .findFirst()
            .orElseThrow()
            .getEmbeddedRole();
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
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        AppEventPermissionScopeProvider.class, ConnectedUserPermissionScopeProvider.class,
        ConnectionPermissionScopeProvider.class, CurrentUserResolver.class, CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        EmbeddedPermissionScopeProvider.class, EmbeddedScopeCacheService.class,
        EmbeddedSharedPermissionScopeProvider.class, EmbeddedUserServiceImpl.class,
        ExecutionPermissionScopeProvider.class, IntegrationInstanceConfigurationPermissionScopeProvider.class,
        IntegrationPermissionScopeProvider.class, KnowledgeBasePermissionScopeProvider.class,
        McpPermissionScopeProvider.class, PermissionScopeRegistry.class, PermissionServiceImpl.class,
        ProjectDeploymentEnvironmentResolver.class, ProjectDeploymentOwnershipResolver.class,
        ProjectDeploymentServiceImpl.class, ProjectOwnershipResolver.class, ProjectPermissionScopeProvider.class,
        WorkflowPermissionScopeProvider.class, WorkspacePermissionScopeProvider.class,
        WorkspaceScopeCacheService.class
    })
    static class RealEmbeddedUserServiceConfiguration {

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
                when(userService.getUser(entry.getValue())).thenReturn(user);
            }

            return userService;
        }
    }
}
