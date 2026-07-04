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

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
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
class WorkspaceScopeCacheKeyConsistencyTest {

    private static final long USER_ID = 42L;
    private static final long WORKSPACE_ID = 100L;
    private static final String WORKSPACE_SCOPES_CACHE = "workspaceScopes";

    private AnnotationConfigApplicationContext applicationContext;
    private WorkspaceScopeCacheService workspaceScopeCacheService;
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void setUp() {
        applicationContext = new AnnotationConfigApplicationContext(CacheTestConfiguration.class);
        workspaceScopeCacheService = applicationContext.getBean(WorkspaceScopeCacheService.class);
        workspaceUserRepository = applicationContext.getBean(WorkspaceUserRepository.class);

        PermissionScopeRegistry permissionScopeRegistry = applicationContext.getBean(PermissionScopeRegistry.class);

        WorkspaceUser workspaceUser = WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR))
            .thenReturn(Set.of("WORKFLOW_VIEW"));
    }

    @AfterEach
    void tearDown() {
        applicationContext.close();
    }

    @Test
    void testEvictWorkspaceScopeCacheTargetsTheCachedEntry() {
        // Two reads, one repository hit — the @Cacheable entry is populated and then served from cache.
        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID);
        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID);

        verify(workspaceUserRepository, times(1)).findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);

        // No active transaction, so eviction runs immediately.
        workspaceScopeCacheService.evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);

        // A third read must MISS the cache — proving the SimpleKey built in eviction matched the @Cacheable key. If it
        // did not match, this read would still be served from cache and the repository would stay at a single hit.
        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID);

        verify(workspaceUserRepository, times(2)).findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @Test
    void testEvictAllWorkspaceScopeCacheClearsTheCachedEntry() {
        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID);

        verify(workspaceUserRepository, times(1)).findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);

        workspaceScopeCacheService.evictAllWorkspaceScopeCache();

        Set<String> scopes = workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID);

        assertThat(scopes).containsExactly("WORKFLOW_VIEW");
        verify(workspaceUserRepository, times(2)).findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID);
    }

    @EnableCaching
    @Configuration
    static class CacheTestConfiguration implements CachingConfigurer {

        // Named to avoid overriding CachingConfigurer.cacheManager() (Spring injects this bean by type anyway).
        @Bean
        CacheManager concurrentMapCacheManager() {
            return new ConcurrentMapCacheManager(WORKSPACE_SCOPES_CACHE);
        }

        // Mirror production: CacheConfiguration installs TenantKeyGenerator as the default key generator, so the
        // @Cacheable read path (which declares no explicit key) stores under the tenant-prefixed String
        // "<tenantId>_<userId>_<workspaceId>". Without this override the test would fall back to SimpleKeyGenerator and
        // both read and evict would agree on a SimpleKey — masking the very mismatch this test exists to catch.
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
        WorkspaceUserRepository workspaceUserRepository() {
            return mock(WorkspaceUserRepository.class);
        }

        @Bean
        WorkspaceScopeCacheService workspaceScopeCacheService(
            CacheManager cacheManager, CustomRoleScopeResolver customRoleScopeResolver,
            PermissionScopeRegistry permissionScopeRegistry, WorkspaceUserRepository workspaceUserRepository) {

            return new WorkspaceScopeCacheService(
                cacheManager, customRoleScopeResolver, permissionScopeRegistry, workspaceUserRepository);
        }
    }
}
