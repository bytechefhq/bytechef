/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedScopeCacheKeyConsistencyTest {

    private static final long USER_ID = 42L;
    private static final String EMBEDDED_SCOPES_CACHE = "embeddedScopes";

    private AnnotationConfigApplicationContext applicationContext;
    private EmbeddedScopeCacheService embeddedScopeCacheService;
    private EmbeddedUserRepository embeddedUserRepository;
    private PermissionScopeRegistry permissionScopeRegistry;

    @BeforeEach
    void setUp() {
        applicationContext = new AnnotationConfigApplicationContext(CacheTestConfiguration.class);
        embeddedScopeCacheService = applicationContext.getBean(EmbeddedScopeCacheService.class);
        embeddedUserRepository = applicationContext.getBean(EmbeddedUserRepository.class);
        permissionScopeRegistry = applicationContext.getBean(PermissionScopeRegistry.class);

        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.of(EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR)));
        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR, PermissionScopeSide.EMBEDDED))
            .thenReturn(Set.of("WORKFLOW_VIEW"));
    }

    @AfterEach
    void tearDown() {
        applicationContext.close();
    }

    @Test
    void testEvictEmbeddedScopeCacheTargetsTheCachedEntry() {
        embeddedScopeCacheService.getEmbeddedScopes(USER_ID);
        embeddedScopeCacheService.getEmbeddedScopes(USER_ID);

        verify(embeddedUserRepository, times(1)).findByUserIdAndEnvironmentIsNull(USER_ID);

        embeddedScopeCacheService.evictEmbeddedScopeCache(USER_ID);

        embeddedScopeCacheService.getEmbeddedScopes(USER_ID);

        verify(embeddedUserRepository, times(2)).findByUserIdAndEnvironmentIsNull(USER_ID);
    }

    @Test
    void testEvictAllEmbeddedScopeCacheClearsTheCachedEntry() {
        embeddedScopeCacheService.getEmbeddedScopes(USER_ID);

        embeddedScopeCacheService.evictAllEmbeddedScopeCache();

        Set<String> scopes = embeddedScopeCacheService.getEmbeddedScopes(USER_ID);

        assertThat(scopes).containsExactly("WORKFLOW_VIEW");
        verify(embeddedUserRepository, times(2)).findByUserIdAndEnvironmentIsNull(USER_ID);
    }

    @Test
    void testTwoEnvironmentsDoNotShareACacheEntry() {
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.DEVELOPMENT.ordinal()))
            .thenReturn(Optional.of(EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT)));
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(EmbeddedUser.forRole(USER_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION)));
        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER, PermissionScopeSide.EMBEDDED))
            .thenReturn(Set.of("EMBEDDED_VIEW"));

        Set<String> developmentScopes = embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.DEVELOPMENT);
        Set<String> productionScopes = embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.PRODUCTION);

        assertThat(developmentScopes).isNotEqualTo(productionScopes);
    }

    @Test
    void testEvictionClearsEveryEnvironmentEntry() {
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(EmbeddedUser.forRole(USER_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION)));

        embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.PRODUCTION);
        embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.PRODUCTION);

        verify(embeddedUserRepository, times(1)).findByUserIdAndEnvironment(
            USER_ID, Environment.PRODUCTION.ordinal());

        embeddedScopeCacheService.evictEmbeddedScopeCache(USER_ID);

        embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.PRODUCTION);

        verify(embeddedUserRepository, times(2)).findByUserIdAndEnvironment(
            USER_ID, Environment.PRODUCTION.ordinal());
    }

    @Test
    void testEnvironmentUnawareReadUnionsTheEnvironmentRoles() {
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.empty());
        when(embeddedUserRepository.findAllByUserId(USER_ID))
            .thenReturn(
                List.of(
                    EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT),
                    EmbeddedUser.forRole(USER_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION)));
        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER, PermissionScopeSide.EMBEDDED))
            .thenReturn(Set.of("EMBEDDED_VIEW"));

        assertThat(embeddedScopeCacheService.getEmbeddedScopes(USER_ID))
            .containsExactlyInAnyOrder("WORKFLOW_VIEW", "EMBEDDED_VIEW");
    }

    @EnableCaching
    @Configuration
    static class CacheTestConfiguration implements CachingConfigurer {

        @Bean
        CacheManager concurrentMapCacheManager() {
            return new ConcurrentMapCacheManager(EMBEDDED_SCOPES_CACHE);
        }

        @Override
        public KeyGenerator keyGenerator() {
            return (target, method, params) -> TenantCacheKeyUtils.getKey(params);
        }

        @Bean
        CustomRoleScopeResolver customRoleScopeResolver() {
            return mock(CustomRoleScopeResolver.class);
        }

        @Bean
        PermissionScopeRegistry permissionScopeRegistry() {
            return mock(PermissionScopeRegistry.class);
        }

        @Bean
        EmbeddedUserRepository embeddedUserRepository() {
            return mock(EmbeddedUserRepository.class);
        }

        @Bean
        EmbeddedScopeCacheService embeddedScopeCacheService(
            CacheManager cacheManager, CustomRoleScopeResolver customRoleScopeResolver,
            PermissionScopeRegistry permissionScopeRegistry, EmbeddedUserRepository embeddedUserRepository) {

            return new EmbeddedScopeCacheService(
                cacheManager, customRoleScopeResolver, permissionScopeRegistry, embeddedUserRepository);
        }
    }
}
