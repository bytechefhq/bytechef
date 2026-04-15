/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Verifies cache semantics of {@link ProjectScopeCacheService} without a real Spring cache infrastructure: the
 * transaction-synchronization routing, the self-injection for {@code @CacheEvict}, and the fail-safe behavior when
 * eviction throws.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectScopeCacheServiceTest {

    private static final long USER_ID = 42L;
    private static final long PROJECT_ID = 100L;

    private CustomRoleScopeResolver customRoleScopeResolver;
    private ProjectUserRepository projectUserRepository;
    private ProjectScopeCacheService service;
    private ProjectScopeCacheService self;

    @BeforeEach
    void setUp() {
        customRoleScopeResolver = mock(CustomRoleScopeResolver.class);
        projectUserRepository = mock(ProjectUserRepository.class);

        // ObjectProvider with null registry exercises the "lightweight EE app without actuator" code path.
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        // The self-reference normally routes through the Spring AOP proxy. In a unit test we substitute another mock
        // that we control, letting us verify evictions were invoked (or not) without Spring's @CacheEvict machinery.
        self = mock(ProjectScopeCacheService.class);

        service = new ProjectScopeCacheService(
            customRoleScopeResolver, projectUserRepository, meterRegistryProvider, self);
    }

    @AfterEach
    void tearDown() {
        // Ensure no leaked TransactionSynchronizationManager state bleeds between tests.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testGetProjectScopesResolvesBuiltInRole() {
        ProjectUser projectUser = ProjectUser.forBuiltInRole(PROJECT_ID, USER_ID, ProjectRole.VIEWER);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(projectUser));

        Set<PermissionScope> result = service.getProjectScopes(USER_ID, PROJECT_ID);

        assertThat(result).contains(PermissionScope.WORKFLOW_VIEW);
    }

    @Test
    void testGetProjectScopesResolvesCustomRole() {
        ProjectUser projectUser = ProjectUser.forCustomRole(PROJECT_ID, USER_ID, 900L);

        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.of(projectUser));
        when(customRoleScopeResolver.resolveScopes(900L))
            .thenReturn(Optional.of(EnumSet.of(PermissionScope.PROJECT_MANAGE_USERS)));

        Set<PermissionScope> result = service.getProjectScopes(USER_ID, PROJECT_ID);

        assertThat(result).containsExactly(PermissionScope.PROJECT_MANAGE_USERS);
    }

    @Test
    void testGetProjectScopesReturnsEmptyForUnknownMembership() {
        when(projectUserRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
            .thenReturn(Optional.empty());

        assertThat(service.getProjectScopes(USER_ID, PROJECT_ID)).isEmpty();
    }

    @Test
    void testEvictRunsImmediatelyWhenNoTransactionActive() {
        service.evictProjectScopeCache(USER_ID, PROJECT_ID);

        // No active transaction — eviction should be immediate (no synchronization registered).
        verify(self, times(1)).doEvictProjectScopeCache(USER_ID, PROJECT_ID);
    }

    @Test
    void testEvictIsDeferredToAfterCommitWhenTransactionActive() {
        TransactionSynchronizationManager.initSynchronization();

        try {
            service.evictProjectScopeCache(USER_ID, PROJECT_ID);

            // Immediately after the call, eviction has NOT yet fired — the sync is queued for afterCommit.
            verify(self, never()).doEvictProjectScopeCache(anyLong(), anyLong());

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {

                synchronization.afterCommit();
            }

            // Now that we've simulated commit, the eviction fires.
            verify(self, times(1)).doEvictProjectScopeCache(USER_ID, PROJECT_ID);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testEvictSwallowsExceptionsInsteadOfPropagating() {
        // Eviction failures (e.g., Redis unavailable) must not propagate back to the caller — the originating
        // transaction has already committed, so there is nothing to roll back and the user-visible flow has
        // already succeeded.
        org.mockito.Mockito.doThrow(new IllegalStateException("cache backend down"))
            .when(self)
            .doEvictProjectScopeCache(anyLong(), anyLong());

        service.evictProjectScopeCache(USER_ID, PROJECT_ID);
        // If we got here, the exception was swallowed as intended.
    }

    @Test
    void testSingleEvictionFailureIncrementsCounter() {
        // Wire a real registry so we can verify the counter actually increments. The unit test above only proves
        // exceptions are swallowed; this proves operators get observability.
        MeterRegistry registry = new SimpleMeterRegistry();

        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(registry);

        ProjectScopeCacheService instrumentedSelf = mock(ProjectScopeCacheService.class);
        ProjectScopeCacheService instrumentedService = new ProjectScopeCacheService(
            customRoleScopeResolver, projectUserRepository, provider, instrumentedSelf);

        org.mockito.Mockito.doThrow(new IllegalStateException("cache backend down"))
            .when(instrumentedSelf)
            .doEvictProjectScopeCache(anyLong(), anyLong());

        instrumentedService.evictProjectScopeCache(USER_ID, PROJECT_ID);

        assertThat(registry.counter("bytechef_permission_cache_eviction_failure", "scope", "single")
            .count()).isEqualTo(1.0);
        assertThat(registry.counter("bytechef_permission_cache_eviction_failure", "scope", "allEntries")
            .count()).isEqualTo(0.0);
    }

    @Test
    void testAllEntriesEvictionFailureIncrementsTaggedCounter() {
        MeterRegistry registry = new SimpleMeterRegistry();

        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(registry);

        ProjectScopeCacheService instrumentedSelf = mock(ProjectScopeCacheService.class);
        ProjectScopeCacheService instrumentedService = new ProjectScopeCacheService(
            customRoleScopeResolver, projectUserRepository, provider, instrumentedSelf);

        org.mockito.Mockito.doThrow(new IllegalStateException("cache backend down"))
            .when(instrumentedSelf)
            .doEvictAllProjectScopeCache();

        instrumentedService.evictAllProjectScopeCache();

        // The all-entries failure must NOT bleed into the single-key counter — operators page on the tagged metric.
        assertThat(registry.counter("bytechef_permission_cache_eviction_failure", "scope", "allEntries")
            .count()).isEqualTo(1.0);
        assertThat(registry.counter("bytechef_permission_cache_eviction_failure", "scope", "single")
            .count()).isEqualTo(0.0);
    }

    @Test
    void testEvictAllBundledIntoAfterCommitWhenTransactionActive() {
        TransactionSynchronizationManager.initSynchronization();

        try {
            service.evictAllProjectScopeCache();

            verify(self, never()).doEvictAllProjectScopeCache();

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {

                synchronization.afterCommit();
            }

            verify(self, times(1)).doEvictAllProjectScopeCache();
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}
