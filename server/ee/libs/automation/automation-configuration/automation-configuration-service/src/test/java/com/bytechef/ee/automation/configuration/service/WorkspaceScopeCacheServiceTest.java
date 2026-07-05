/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Verifies cache semantics of {@link WorkspaceScopeCacheService} without a real Spring cache infrastructure: the
 * transaction-synchronization routing, the programmatic {@link CacheManager} eviction, and the fail-safe behavior when
 * eviction throws. The actual cache key used by eviction is pinned end-to-end against {@code @Cacheable} in
 * {@link WorkspaceScopeCacheKeyConsistencyTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceScopeCacheServiceTest {

    private static final long USER_ID = 42L;
    private static final long WORKSPACE_ID = 100L;
    private static final String WORKSPACE_SCOPES_CACHE = "workspaceScopes";

    private Cache cache;
    private CacheManager cacheManager;
    private CustomRoleScopeResolver customRoleScopeResolver;
    private PermissionScopeRegistry permissionScopeRegistry;
    private WorkspaceUserRepository workspaceUserRepository;
    private WorkspaceScopeCacheService service;

    @BeforeEach
    void setUp() {
        cache = mock(Cache.class);
        cacheManager = mock(CacheManager.class);
        customRoleScopeResolver = mock(CustomRoleScopeResolver.class);
        permissionScopeRegistry = mock(PermissionScopeRegistry.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        // Eviction now drives the CacheManager directly instead of bouncing through a self-proxy. Wiring the mock
        // manager to a mock cache lets us verify evict/clear calls without Spring's @CacheEvict machinery.
        when(cacheManager.getCache(WORKSPACE_SCOPES_CACHE)).thenReturn(cache);

        service = new WorkspaceScopeCacheService(
            cacheManager, customRoleScopeResolver, permissionScopeRegistry, workspaceUserRepository);
    }

    @AfterEach
    void tearDown() {
        // Ensure no leaked TransactionSynchronizationManager state bleeds between tests.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testGetWorkspaceScopesResolvesBuiltInRole() {
        WorkspaceUser workspaceUser = WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR);

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR))
            .thenReturn(Set.of("WORKFLOW_VIEW", "WORKFLOW_EDIT"));

        Set<String> result = service.getWorkspaceScopes(USER_ID, WORKSPACE_ID);

        assertThat(result).contains("WORKFLOW_VIEW");
    }

    @Test
    void testGetWorkspaceScopesResolvesCustomRole() {
        WorkspaceUser workspaceUser = WorkspaceUser.forCustomRole(USER_ID, WORKSPACE_ID, 900L);

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(workspaceUser));
        when(customRoleScopeResolver.resolveScopes(900L))
            .thenReturn(Optional.of(Set.of("PROJECT_SETTINGS")));

        Set<String> result = service.getWorkspaceScopes(USER_ID, WORKSPACE_ID);

        assertThat(result).containsExactly("PROJECT_SETTINGS");
    }

    @Test
    void testGetWorkspaceScopesReturnsEmptyForUnknownMembership() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThat(service.getWorkspaceScopes(USER_ID, WORKSPACE_ID)).isEmpty();
    }

    @Test
    void testGetWorkspaceScopesFailsClosedOnInvalidRoleOrdinal() {
        // A corrupted or legacy workspace_role ordinal hydrated via Spring Data JDBC (which bypasses the constructor's
        // range validation) must fail closed — deny all scopes — rather than surface an ArrayIndexOutOfBoundsException
        // as a 500 during permission evaluation.
        WorkspaceUser corrupted = mock(WorkspaceUser.class);

        when(corrupted.getWorkspaceRole()).thenReturn(999);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(corrupted));

        assertThat(service.getWorkspaceScopes(USER_ID, WORKSPACE_ID)).isEmpty();

        verify(permissionScopeRegistry, never()).getScopeNames(any());
    }

    @Test
    void testGetWorkspaceScopesThrowsOnXorCorruption() {
        // Both workspaceRole and customRoleId null violates the chk_workspace_user_role_xor CHECK constraint. The XOR
        // constructor forbids constructing such a row, so mock the getters to simulate a corrupted DB read — the
        // dispatch surfaces the corruption as an IllegalStateException rather than silently returning empty scopes.
        WorkspaceUser corrupted = mock(WorkspaceUser.class);

        when(corrupted.getWorkspaceRole()).thenReturn(null);
        when(corrupted.getCustomRoleId()).thenReturn(null);
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(corrupted));

        assertThatThrownBy(() -> service.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chk_workspace_user_role_xor");
    }

    @Test
    void testEvictRunsImmediatelyWhenNoTransactionActive() {
        service.evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);

        // No active transaction — eviction should be immediate (no synchronization registered).
        verify(cache, times(1)).evict(TenantCacheKeyUtils.getKey(USER_ID, WORKSPACE_ID));
    }

    @Test
    void testEvictIsDeferredToAfterCommitWhenTransactionActive() {
        TransactionSynchronizationManager.initSynchronization();

        try {
            service.evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);

            // Immediately after the call, eviction has NOT yet fired — the sync is queued for afterCommit.
            verify(cache, never()).evict(any());

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {

                synchronization.afterCommit();
            }

            // Now that we've simulated commit, the eviction fires.
            verify(cache, times(1)).evict(TenantCacheKeyUtils.getKey(USER_ID, WORKSPACE_ID));
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testEvictSwallowsExceptionsInsteadOfPropagating() {
        // Eviction failures (e.g., Redis unavailable) must not propagate back to the caller — the originating
        // transaction has already committed, so there is nothing to roll back and the user-visible flow has
        // already succeeded.
        doThrow(new IllegalStateException("cache backend down"))
            .when(cache)
            .evict(any());

        service.evictWorkspaceScopeCache(USER_ID, WORKSPACE_ID);
        // If we got here, the exception was swallowed as intended.
    }

    @Test
    void testEvictAllSwallowsExceptionsInsteadOfPropagating() {
        // Symmetric to the single-key swallow test: an all-entries eviction failure (e.g., Redis unavailable) must
        // not propagate — the originating transaction has already committed, so there is nothing to roll back.
        doThrow(new IllegalStateException("cache backend down"))
            .when(cache)
            .clear();

        service.evictAllWorkspaceScopeCache();
        // If we got here, the exception was swallowed as intended.
    }

    @Test
    void testEvictAllBundledIntoAfterCommitWhenTransactionActive() {
        TransactionSynchronizationManager.initSynchronization();

        try {
            service.evictAllWorkspaceScopeCache();

            verify(cache, never()).clear();

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {

                synchronization.afterCommit();
            }

            verify(cache, times(1)).clear();
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}
