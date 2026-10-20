/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

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
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
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
import com.bytechef.ee.automation.configuration.service.EmbeddedUserServiceImpl;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        EmbeddedUserFacadeIntTest.RealEmbeddedUserFacadeConfiguration.class
    })
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class EmbeddedUserFacadeIntTest {

    private static final String ADMIN = "tenant-admin";
    private static final String EMBEDDED_ADMIN = "embedded-admin";
    private static final String EMBEDDED_VIEWER = "embedded-viewer";
    private static final String MEMBER = "member";
    private static final String NON_MEMBER = "non-member";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        ADMIN, 9400L, EMBEDDED_ADMIN, 9401L, EMBEDDED_VIEWER, 9402L, MEMBER, 9403L, NON_MEMBER, 9404L);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EmbeddedUserFacade embeddedUserFacade;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private PermissionService permissionService;

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
    void testANonMemberIsRefusedTheMemberList() {
        authenticate(NON_MEMBER, false);

        assertThatThrownBy(() -> embeddedUserFacade.getEmbeddedUsers())
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAnEmbeddedViewerListsMembersButCannotAdd() {
        authenticate(ADMIN, true);

        embeddedUserFacade.addEmbeddedUser(userId(EMBEDDED_VIEWER), WorkspaceRole.VIEWER);

        authenticate(EMBEDDED_VIEWER, false);

        assertThat(embeddedUserFacade.getEmbeddedUsers()).hasSize(1);

        assertThatThrownBy(() -> embeddedUserFacade.addEmbeddedUser(userId(MEMBER), WorkspaceRole.VIEWER))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(embeddedUserRepository.existsByUserId(userId(MEMBER))).isFalse();
    }

    @Test
    void testAnEmbeddedAdminAddsAMember() {
        authenticate(ADMIN, true);

        embeddedUserFacade.addEmbeddedUser(userId(EMBEDDED_ADMIN), WorkspaceRole.ADMIN);

        authenticate(EMBEDDED_ADMIN, false);

        embeddedUserFacade.addEmbeddedUser(userId(MEMBER), WorkspaceRole.EDITOR);

        assertThat(embeddedUserRepository.existsByUserId(userId(MEMBER))).isTrue();
    }

    @Test
    void testATenantAdminWithNoRowAddsAMember() {
        authenticate(ADMIN, true);

        embeddedUserFacade.addEmbeddedUser(userId(MEMBER), WorkspaceRole.VIEWER);

        assertThat(embeddedUserRepository.existsByUserId(userId(MEMBER))).isTrue();
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
        EmbeddedSharedPermissionScopeProvider.class, EmbeddedUserFacadeImpl.class, EmbeddedUserServiceImpl.class,
        ExecutionPermissionScopeProvider.class, IntegrationInstanceConfigurationPermissionScopeProvider.class,
        IntegrationPermissionScopeProvider.class, KnowledgeBasePermissionScopeProvider.class,
        McpPermissionScopeProvider.class, PermissionScopeRegistry.class, PermissionServiceImpl.class,
        ProjectDeploymentEnvironmentResolver.class, ProjectDeploymentOwnershipResolver.class,
        ProjectDeploymentServiceImpl.class, ProjectOwnershipResolver.class, ProjectPermissionScopeProvider.class,
        WorkflowPermissionScopeProvider.class, WorkspacePermissionScopeProvider.class,
        WorkspaceScopeCacheService.class
    })
    static class RealEmbeddedUserFacadeConfiguration {

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
