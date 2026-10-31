/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.config;

import com.bytechef.automation.configuration.callback.ProjectCallback;
import com.bytechef.automation.configuration.callback.ProjectWorkflowCallback;
import com.bytechef.cache.config.CacheConfiguration;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditMapper;
import com.bytechef.ee.automation.configuration.security.scope.A2aPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ApiKeyPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DeploymentPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.McpPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ProjectPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkflowPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.CustomRoleScopeResolver;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.service.UserServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared context for the workspace permission IntTests: the real permission, scope-cache and audit-mapper beans over
 * the real repositories, the production cache configuration (Caffeine with the tenant key generator) and the Postgres
 * Testcontainer migrated by the real Liquibase changelog.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@EnableMethodSecurity
@Import({
    A2aPermissionScopeProvider.class, ApiKeyPermissionScopeProvider.class, CacheConfiguration.class,
    CurrentUserResolver.class, DeploymentPermissionScopeProvider.class,
    EmbeddedOwnerContributorsTestConfiguration.class, LiquibaseConfiguration.class, McpPermissionScopeProvider.class,
    PermissionScopeRegistry.class, PermissionServiceImpl.class, PostgreSQLContainerConfiguration.class,
    ProjectCallback.class, ProjectPermissionScopeProvider.class, ProjectWorkflowCallback.class,
    WorkflowPermissionScopeProvider.class,
    WorkspacePermissionIntTestConfiguration.WorkspacePermissionJdbcConfiguration.class,
    WorkspaceScopeCacheService.class, WorkspaceUserAuditMapper.class
})
@SpringBootConfiguration
public class WorkspacePermissionIntTestConfiguration {

    public static final long CUSTOM_ROLE_ID = 900L;
    public static final String CUSTOM_ROLE_SCOPE = "PROJECT_SETTINGS";

    @Bean
    CustomRoleScopeResolver customRoleScopeResolver() {
        return customRoleId -> customRoleId == CUSTOM_ROLE_ID ? Optional.of(Set.of(CUSTOM_ROLE_SCOPE))
            : Optional.empty();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserService userService(
        AuthorityRepository authorityRepository, CacheManager cacheManager, PasswordEncoder passwordEncoder,
        PersistentTokenRepository persistentTokenRepository, UserRepository userRepository) {

        return new UserServiceImpl(
            authorityRepository, cacheManager, passwordEncoder, persistentTokenRepository, userRepository);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class WorkspacePermissionJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        WorkspacePermissionJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new MapWrapperToStringConverter(objectMapper), new StringToMapWrapperConverter(objectMapper));
        }
    }
}
