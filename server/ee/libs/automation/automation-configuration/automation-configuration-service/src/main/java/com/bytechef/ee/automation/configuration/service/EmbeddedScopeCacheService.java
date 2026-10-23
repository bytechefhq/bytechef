/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
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
 * Embedded scope caching service. Reads are cached declaratively via {@link Cacheable}; evictions are performed
 * programmatically through the {@link CacheManager} so that internally-invoked eviction is not silently dropped by
 * Spring AOP self-invocation bypass.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional(readOnly = true)
public class EmbeddedScopeCacheService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedScopeCacheService.class);

    private static final String EMBEDDED_SCOPES_CACHE = "embeddedScopes";

    private final CacheManager cacheManager;
    private final @Nullable CustomRoleScopeResolver customRoleScopeResolver;
    private final EmbeddedUserRepository embeddedUserRepository;
    private final PermissionScopeRegistry permissionScopeRegistry;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public EmbeddedScopeCacheService(
        CacheManager cacheManager, @Nullable CustomRoleScopeResolver customRoleScopeResolver,
        PermissionScopeRegistry permissionScopeRegistry, EmbeddedUserRepository embeddedUserRepository) {

        this.cacheManager = cacheManager;
        this.customRoleScopeResolver = customRoleScopeResolver;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.embeddedUserRepository = embeddedUserRepository;
    }

    public void evictAllEmbeddedScopeCache() {
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

    public void evictEmbeddedScopeCache(long userId) {
        Runnable eviction = () -> evictSingleEntry(userId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeEvictSingle(eviction, userId);
                }
            });
        } else {
            safeEvictSingle(eviction, userId);
        }
    }

    @Cacheable(value = EMBEDDED_SCOPES_CACHE)
    public Set<String> getEmbeddedScopes(long userId) {
        Optional<EmbeddedUser> implicitEmbeddedUser = embeddedUserRepository
            .findByUserIdAndEnvironmentIsNull(userId);

        if (implicitEmbeddedUser.isPresent() && !embeddedUserRepository.existsByUserIdAndEnvironmentIsNotNull(userId)) {
            return dispatchScopes(implicitEmbeddedUser.get(), userId);
        }

        Set<String> unionScopes = new HashSet<>();

        for (EmbeddedUser embeddedUser : embeddedUserRepository.findAllByUserId(userId)) {
            if (embeddedUser.getEnvironment() != null) {
                unionScopes.addAll(dispatchScopes(embeddedUser, userId));
            }
        }

        return unionScopes;
    }

    @Cacheable(value = EMBEDDED_SCOPES_CACHE)
    public Set<String> getEmbeddedScopes(long userId, Environment environment) {
        return fetchEmbeddedUser(userId, environment)
            .map(embeddedUser -> dispatchScopes(embeddedUser, userId))
            .orElse(Collections.emptySet());
    }

    private Optional<EmbeddedUser> fetchEmbeddedUser(long userId, Environment environment) {
        Optional<EmbeddedUser> environmentEmbeddedUser = embeddedUserRepository
            .findByUserIdAndEnvironment(userId, environment.ordinal());

        if (environmentEmbeddedUser.isPresent()) {
            return environmentEmbeddedUser;
        }

        if (embeddedUserRepository.existsByUserIdAndEnvironmentIsNotNull(userId)) {
            return Optional.empty();
        }

        return embeddedUserRepository.findByUserIdAndEnvironmentIsNull(userId);
    }

    private void evictSingleEntry(long userId) {
        Cache cache = cacheManager.getCache(EMBEDDED_SCOPES_CACHE);

        if (cache != null) {
            cache.evict(TenantCacheKeyUtils.getKey(userId));

            for (Environment environment : Environment.values()) {
                cache.evict(TenantCacheKeyUtils.getKey(userId, environment));
            }
        }
    }

    private void evictAllEntries() {
        Cache cache = cacheManager.getCache(EMBEDDED_SCOPES_CACHE);

        if (cache != null) {
            cache.clear();
        }
    }

    private Set<String> dispatchScopes(EmbeddedUser embeddedUser, long userId) {
        WorkspaceRole embeddedRole = embeddedUser.getEmbeddedRole();

        if (embeddedRole != null) {
            return permissionScopeRegistry.getScopeNames(embeddedRole, PermissionScopeSide.EMBEDDED);
        }

        Long customRoleId = embeddedUser.getCustomRoleId();

        if (customRoleId != null) {
            return resolveCustomRoleScopes(customRoleId, userId);
        }

        throw new IllegalStateException(
            ("EmbeddedUser for userId=%d has neither a built-in role nor a custom role. This violates the " +
                "role XOR CHECK constraint and indicates corrupted state.").formatted(userId));
    }

    private Set<String> resolveCustomRoleScopes(long customRoleId, long userId) {
        if (customRoleScopeResolver == null) {
            log.error(
                "EmbeddedUser for userId={} carries customRoleId={} but no CustomRoleScopeResolver bean is " +
                    "available — returning empty permissions. Either deploy a build that includes the custom-role " +
                    "module or remove the custom role from this membership.",
                userId, customRoleId);

            return Collections.emptySet();
        }

        Set<String> scopes = new HashSet<>(
            customRoleScopeResolver.resolveScopes(customRoleId)
                .orElseGet(Collections::emptySet));

        scopes.retainAll(permissionScopeRegistry.getAllScopeNames(PermissionScopeSide.EMBEDDED));

        return scopes;
    }

    private void safeEvictAllEntries(Runnable eviction) {
        try {
            eviction.run();
        } catch (RuntimeException exception) {
            log.error(
                "PERMISSION_CACHE_EVICTION_FAILURE[allEntries]: Failed to evict the entire embedded scopes cache. " +
                    "Stale permissions may be served until TTL expires.",
                exception);
        }
    }

    private void safeEvictSingle(Runnable eviction, long userId) {
        try {
            eviction.run();
        } catch (RuntimeException exception) {
            log.error(
                "PERMISSION_CACHE_EVICTION_FAILURE[single]: Failed to evict embedded scopes for userId={}. Stale " +
                    "permissions may be served until TTL expires.",
                userId, exception);
        }
    }
}
