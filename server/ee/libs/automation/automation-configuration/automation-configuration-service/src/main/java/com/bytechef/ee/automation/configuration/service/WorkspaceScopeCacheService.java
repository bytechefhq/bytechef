/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService.UserWorkspacePair;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Workspace scope caching service. Reads are cached declaratively via {@link Cacheable}; evictions are performed
 * programmatically through the {@link CacheManager} so that internally-invoked eviction is not silently dropped by
 * Spring AOP self-invocation bypass.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional(readOnly = true)
public class WorkspaceScopeCacheService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceScopeCacheService.class);

    private static final String WORKSPACE_SCOPES_CACHE = "workspaceScopes";

    private final CacheManager cacheManager;
    private final @Nullable CustomRoleScopeResolver customRoleScopeResolver;
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public WorkspaceScopeCacheService(
        CacheManager cacheManager, @Nullable CustomRoleScopeResolver customRoleScopeResolver,
        PermissionScopeRegistry permissionScopeRegistry, WorkspaceUserRepository workspaceUserRepository) {

        this.cacheManager = cacheManager;
        this.customRoleScopeResolver = customRoleScopeResolver;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    public void evictAllWorkspaceScopeCache() {
        Runnable eviction = this::evictAllEntries;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeEvictAllEntries(eviction);
                }
            });
        } else {
            safeEvictAllEntries(eviction);
        }
    }

    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
        Runnable eviction = () -> evictSingleEntry(userId, workspaceId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeEvictSingle(eviction, userId, workspaceId);
                }
            });
        } else {
            safeEvictSingle(eviction, userId, workspaceId);
        }
    }

    public void evictWorkspaceScopeCaches(Collection<UserWorkspacePair> userWorkspacePairs) {
        if (userWorkspacePairs.isEmpty()) {
            return;
        }

        List<UserWorkspacePair> snapshotUserWorkspacePairs = List.copyOf(userWorkspacePairs);

        Runnable eviction = () -> {
            for (UserWorkspacePair pair : snapshotUserWorkspacePairs) {
                safeEvictSingle(
                    () -> evictSingleEntry(pair.userId(), pair.workspaceId()),
                    pair.userId(), pair.workspaceId());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }

    @Cacheable(value = WORKSPACE_SCOPES_CACHE)
    public Set<String> getWorkspaceScopes(long userId, long workspaceId) {
        return workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .map(workspaceUser -> dispatchScopes(workspaceUser, userId, workspaceId))
            .orElse(Collections.emptySet());
    }

    private void evictSingleEntry(long userId, long workspaceId) {
        Cache cache = cacheManager.getCache(WORKSPACE_SCOPES_CACHE);

        if (cache != null) {
            // Key must mirror the @Cacheable read path. That path declares no explicit key, so it falls through to the
            // globally-configured TenantKeyGenerator (CacheConfiguration), which stores under the tenant-prefixed
            // "<tenantId>_<userId>_<workspaceId>". A bare SimpleKey(userId, workspaceId) would never match it, so the
            // eviction would silently no-op and stale scopes would be served until the TTL expires.
            cache.evict(TenantCacheKeyUtils.getKey(userId, workspaceId));
        }
    }

    private void evictAllEntries() {
        Cache cache = cacheManager.getCache(WORKSPACE_SCOPES_CACHE);

        if (cache != null) {
            cache.clear();
        }
    }

    private Set<String> dispatchScopes(WorkspaceUser workspaceUser, long userId, long workspaceId) {
        if (workspaceUser.getWorkspaceRole() != null) {
            return resolveBuiltInScopes(workspaceUser.getWorkspaceRole());
        }

        if (workspaceUser.getCustomRoleId() != null) {
            return resolveCustomRoleScopes(workspaceUser.getCustomRoleId(), userId, workspaceId);
        }

        throw new IllegalStateException(
            ("WorkspaceUser for userId=%d, workspaceId=%d has neither a built-in role nor a custom role. This " +
                "violates the chk_workspace_user_role_xor CHECK constraint and indicates corrupted state.")
                    .formatted(userId, workspaceId));
    }

    private Set<String> resolveBuiltInScopes(int ordinal) {
        WorkspaceRole[] values = WorkspaceRole.values();

        return permissionScopeRegistry.getScopeNames(values[ordinal]);
    }

    private Set<String> resolveCustomRoleScopes(long customRoleId, long userId, long workspaceId) {
        if (customRoleScopeResolver == null) {
            log.error(
                "WorkspaceUser for userId={}, workspaceId={} carries customRoleId={} but no " +
                    "CustomRoleScopeResolver bean is available — returning empty permissions. " +
                    "Either deploy a build that includes the custom-role module or remove the custom " +
                    "role from this membership.",
                userId, workspaceId, customRoleId);

            return Collections.emptySet();
        }

        return customRoleScopeResolver.resolveScopes(customRoleId)
            .orElseGet(Collections::emptySet);
    }

    private void safeEvictAllEntries(Runnable eviction) {
        try {
            eviction.run();
        } catch (RuntimeException exception) {
            log.error(
                "PERMISSION_CACHE_EVICTION_FAILURE[allEntries]: Failed to evict the entire workspace scopes cache. " +
                    "Stale permissions may be served until TTL expires.",
                exception);
        }
    }

    private void safeEvictSingle(Runnable eviction, long userId, long workspaceId) {
        try {
            eviction.run();
        } catch (RuntimeException exception) {
            log.error(
                "PERMISSION_CACHE_EVICTION_FAILURE[single]: Failed to evict workspace scopes for userId={}, " +
                    "workspaceId={}. Stale permissions may be served until TTL expires.",
                userId, workspaceId, exception);
        }
    }
}
