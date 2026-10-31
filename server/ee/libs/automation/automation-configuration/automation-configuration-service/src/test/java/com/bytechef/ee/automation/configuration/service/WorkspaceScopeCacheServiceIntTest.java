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

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.PermissionService.UserWorkspacePair;
import com.bytechef.ee.automation.configuration.config.WorkspacePermissionIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs {@link WorkspaceScopeCacheService} through its Spring proxy with the production cache configuration (Caffeine
 * with the tenant key generator) over real {@code workspace_user} rows, so the cache key used by the programmatic
 * eviction is proven against the key {@code @Cacheable} stores under.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkspacePermissionIntTestConfiguration.class, properties = "bytechef.edition=ee")
class WorkspaceScopeCacheServiceIntTest {

    private static final long USER_ID = 2L;
    private static final String WORKSPACE_SCOPES_CACHE = "workspaceScopes";

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionScopeRegistry permissionScopeRegistry;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceScopeCacheService workspaceScopeCacheService;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("Workspace Scope Cache " + UUID.randomUUID()));

        workspaceId = workspace.getId();
    }

    @Test
    void testGetWorkspaceScopesResolvesBuiltInRole() {
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.EDITOR));

        Set<String> scopes = workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId);

        assertThat(scopes).isEqualTo(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR))
            .contains("WORKFLOW_VIEW", "WORKFLOW_EDIT");
    }

    @Test
    void testGetWorkspaceScopesResolvesCustomRole() {
        workspaceUserRepository.save(
            WorkspaceUser.forCustomRole(USER_ID, workspaceId, WorkspacePermissionIntTestConfiguration.CUSTOM_ROLE_ID));

        Set<String> scopes = workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId);

        assertThat(scopes).containsExactly(WorkspacePermissionIntTestConfiguration.CUSTOM_ROLE_SCOPE);
    }

    @Test
    void testGetWorkspaceScopesReturnsEmptyForUnknownMembership() {
        assertThat(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId)).isEmpty();
    }

    @Test
    void testGetWorkspaceScopesFailsClosedOnInvalidRoleOrdinal() {
        // A corrupted or legacy workspace_role ordinal hydrated by Spring Data JDBC (which bypasses the constructor's
        // range validation) must fail closed - deny all scopes - rather than surface an
        // ArrayIndexOutOfBoundsException as a 500 during permission evaluation.
        WorkspaceUser workspaceUser = workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.ADMIN));

        jdbcTemplate.update("UPDATE workspace_user SET workspace_role = 999 WHERE id = ?", workspaceUser.getId());

        assertThat(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId)).isEmpty();
    }

    @Test
    void testGetWorkspaceScopesThrowsOnXorCorruption() {
        // A row with neither a built-in nor a custom role violates chk_workspace_user_role_xor, so the constraint is
        // dropped for the duration of the test to persist the corrupted row the dispatch must reject loudly.
        WorkspaceUser workspaceUser = workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        jdbcTemplate.execute("ALTER TABLE workspace_user DROP CONSTRAINT chk_workspace_user_role_xor");

        try {
            jdbcTemplate.update(
                "UPDATE workspace_user SET workspace_role = NULL, custom_role_id = NULL WHERE id = ?",
                workspaceUser.getId());

            assertThatThrownBy(() -> workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chk_workspace_user_role_xor");
        } finally {
            workspaceUserRepository.deleteById(workspaceUser.getId());

            jdbcTemplate.execute(
                "ALTER TABLE workspace_user ADD CONSTRAINT chk_workspace_user_role_xor CHECK " +
                    "((workspace_role IS NOT NULL AND custom_role_id IS NULL) OR " +
                    "(workspace_role IS NULL AND custom_role_id IS NOT NULL))");
        }
    }

    @Test
    void testEvictWorkspaceScopeCacheTargetsTheCachedEntry() {
        WorkspaceUser workspaceUser = workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        Set<String> viewerScopes = permissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER);

        assertThat(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId)).isEqualTo(viewerScopes);
        assertThat(getCachedScopes()).isEqualTo(viewerScopes);

        changeRole(workspaceUser, WorkspaceRole.EDITOR);

        // The role changed in the database, but the @Cacheable entry still serves the old scopes.
        assertThat(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId)).isEqualTo(viewerScopes);

        workspaceScopeCacheService.evictWorkspaceScopeCache(USER_ID, workspaceId);

        // If the key built by the eviction did not match the @Cacheable key, the stale entry would survive here.
        assertThat(getCachedScopes()).isNull();
        assertThat(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId))
            .isEqualTo(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR));
    }

    @Test
    void testEvictAllWorkspaceScopeCacheClearsTheCachedEntry() {
        WorkspaceUser workspaceUser = workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId);

        changeRole(workspaceUser, WorkspaceRole.EDITOR);

        workspaceScopeCacheService.evictAllWorkspaceScopeCache();

        assertThat(getCachedScopes()).isNull();
        assertThat(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId))
            .isEqualTo(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR));
    }

    @Test
    void testEvictIsDeferredToAfterCommitWhenTransactionActive() {
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId);

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            workspaceScopeCacheService.evictWorkspaceScopeCache(USER_ID, workspaceId);

            // Inside the transaction the eviction is only queued for afterCommit.
            assertThat(getCachedScopes()).isNotNull();
        });

        assertThat(getCachedScopes()).isNull();
    }

    @Test
    void testEvictIsSkippedWhenTransactionRollsBack() {
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId);

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            workspaceScopeCacheService.evictWorkspaceScopeCache(USER_ID, workspaceId);

            transactionStatus.setRollbackOnly();
        });

        assertThat(getCachedScopes()).isNotNull();
    }

    @Test
    void testEvictAllBundledIntoAfterCommitWhenTransactionActive() {
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        workspaceScopeCacheService.getWorkspaceScopes(USER_ID, workspaceId);

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            workspaceScopeCacheService.evictAllWorkspaceScopeCache();

            assertThat(getCachedScopes()).isNotNull();
        });

        assertThat(getCachedScopes()).isNull();
    }

    @Test
    void testEvictSwallowsExceptionsInsteadOfPropagating() {
        // Eviction failures (e.g., Redis unavailable) must not propagate back to the caller - the originating
        // transaction has already committed, so there is nothing to roll back. The cache backend failure is the
        // only fake here: the service reads real rows through the real repository.
        WorkspaceScopeCacheService failingCacheService = new WorkspaceScopeCacheService(
            new FailingCacheManager(), null, permissionScopeRegistry, workspaceUserRepository);

        assertThatCode(() -> failingCacheService.evictWorkspaceScopeCache(USER_ID, workspaceId))
            .doesNotThrowAnyException();
        assertThatCode(() -> failingCacheService.evictWorkspaceScopeCaches(
            Set.of(new UserWorkspacePair(USER_ID, workspaceId))))
            .doesNotThrowAnyException();
    }

    @Test
    void testEvictAllSwallowsExceptionsInsteadOfPropagating() {
        WorkspaceScopeCacheService failingCacheService = new WorkspaceScopeCacheService(
            new FailingCacheManager(), null, permissionScopeRegistry, workspaceUserRepository);

        assertThatCode(failingCacheService::evictAllWorkspaceScopeCache).doesNotThrowAnyException();
    }

    private void changeRole(WorkspaceUser workspaceUser, WorkspaceRole workspaceRole) {
        WorkspaceUser persistedWorkspaceUser = workspaceUserRepository.findById(workspaceUser.getId())
            .orElseThrow();

        persistedWorkspaceUser.assignRole(workspaceRole);

        workspaceUserRepository.save(persistedWorkspaceUser);
    }

    @SuppressWarnings("unchecked")
    private @Nullable Set<String> getCachedScopes() {
        Cache cache = cacheManager.getCache(WORKSPACE_SCOPES_CACHE);

        assertThat(cache).isNotNull();

        Cache.ValueWrapper valueWrapper = cache.get(TenantCacheKeyUtils.getKey(USER_ID, workspaceId));

        return valueWrapper == null ? null : (Set<String>) valueWrapper.get();
    }

    private static class FailingCacheManager implements CacheManager {

        @Override
        public Cache getCache(String name) {
            return new ConcurrentMapCache(name) {

                @Override
                public void clear() {
                    throw new IllegalStateException("cache backend down");
                }

                @Override
                public void evict(Object key) {
                    throw new IllegalStateException("cache backend down");
                }
            };
        }

        @Override
        public Collection<String> getCacheNames() {
            return List.of(WORKSPACE_SCOPES_CACHE);
        }
    }
}
