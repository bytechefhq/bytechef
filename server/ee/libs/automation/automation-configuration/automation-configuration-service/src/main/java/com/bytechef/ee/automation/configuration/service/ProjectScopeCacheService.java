/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService.UserProjectPair;
import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.BuiltInRoleScopes;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Separate bean for project scope caching to avoid Spring AOP self-invocation bypass. {@code @Cacheable} only works
 * when called through the proxy, which requires an external bean call.
 *
 * <p>
 * <b>Cache semantics</b>
 *
 * <p>
 * The cache is local to each JVM. In a multi-instance EE deployment, an eviction triggered on instance A will NOT
 * invalidate instance B's local cache &mdash; instance B keeps the stale entry until the cache TTL expires (10 minutes
 * by default in {@code CacheConfiguration}). For deployments where role changes must propagate immediately across all
 * instances, switch the cache provider to a distributed implementation (Redis, Hazelcast) via
 * {@code spring.cache.type}. Also see {@code CacheConfiguration} for the TTL configuration.
 *
 * <p>
 * <b>Eviction transactional ordering</b>
 *
 * <p>
 * Calling {@link #evictProjectScopeCache(long, long)} or {@link #evictAllProjectScopeCache()} schedules the eviction to
 * run on transaction commit (rather than immediately). This avoids a race where:
 * <ol>
 * <li>Mutation method runs, writes to the database, schedules cache eviction.</li>
 * <li>Eviction fires immediately (under classic {@code @CacheEvict}).</li>
 * <li>Concurrent reader misses the cache, queries the database, sees the pre-commit state, repopulates the cache with
 * the stale value.</li>
 * <li>Original transaction commits.</li>
 * <li>The cache now serves stale data until TTL.</li>
 * </ol>
 * By deferring the eviction to {@code AFTER_COMMIT}, concurrent readers between the write and the commit see the old
 * value (correct &mdash; the write isn't visible yet) AND the cache is invalidated only once the new value is visible
 * to subsequent reads.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional(readOnly = true)
public class ProjectScopeCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectScopeCacheService.class);

    private static final String PROJECT_SCOPES_CACHE = "projectScopes";

    private final @Nullable CustomRoleScopeResolver customRoleScopeResolver;
    private final @Nullable Counter singleEvictionFailureCounter;
    private final @Nullable Counter allEntriesEvictionFailureCounter;
    private final ProjectUserRepository projectUserRepository;

    /**
     * Self-reference resolved through Spring's proxy so that internal calls to {@code doEvict*} run through the
     * {@code @CacheEvict} interceptor. {@code @Lazy} is required because the proxy only exists once the bean is fully
     * initialized; without it Spring would refuse to inject the bean into itself during construction. Injected via
     * constructor (not setter) so the reference is {@code final} and cannot be reassigned post-construction — the
     * setter-injection variant previously used here left the field mutable and package-visible.
     */
    private final ProjectScopeCacheService self;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public ProjectScopeCacheService(
        @Nullable CustomRoleScopeResolver customRoleScopeResolver, ProjectUserRepository projectUserRepository,
        ObjectProvider<MeterRegistry> meterRegistryProvider, @Lazy ProjectScopeCacheService self) {

        this.customRoleScopeResolver = customRoleScopeResolver;
        this.projectUserRepository = projectUserRepository;
        this.self = self;

        // Lightweight EE apps (e.g., runtime-job-app) may start without actuator — ObjectProvider lets us resolve the
        // registry if present, fall back to null otherwise. When null, eviction failures are logged but not counted.
        // CT_CONSTRUCTOR_THROW is suppressed because Counter.builder().register() can theoretically throw and the
        // class is not exposed via Finalizer attack surface (it's a singleton bean instantiated by Spring).
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        this.singleEvictionFailureCounter = createFailureCounter(meterRegistry, "single");
        this.allEntriesEvictionFailureCounter = createFailureCounter(meterRegistry, "allEntries");
    }

    @Nullable
    private static Counter createFailureCounter(@Nullable MeterRegistry meterRegistry, String scopeTag) {
        if (meterRegistry == null) {
            return null;
        }

        return Counter.builder("bytechef_permission_cache_eviction_failure")
            .description(
                "Number of permission cache evictions that failed after the originating transaction committed. "
                    + "Non-zero values indicate stale project scopes may be served until TTL expires.")
            .tag("scope", scopeTag)
            .register(meterRegistry);
    }

    /**
     * Resolves the user's project scopes. Empty results ARE cached to short-circuit DENY paths under attack load (a
     * request spray against projects the caller is not a member of would otherwise hit the DB on every request).
     * Caching empty results is only safe because every legitimate state change that could flip an empty result into a
     * non-empty one explicitly evicts the cache: {@code addProjectUser}, {@code updateProjectUserRole},
     * {@code deleteProjectUser}, {@code removeWorkspaceUser}, and custom-role scope mutations (see
     * {@code CustomRoleServiceImpl} — full eviction). That eviction coverage is load-bearing: if a new mutation path
     * introduces grants without calling {@code evictProjectScopeCache}, a previously-cached empty set will pin the user
     * as "no access" until the TTL expires.
     */
    @Cacheable(value = PROJECT_SCOPES_CACHE)
    public Set<PermissionScope> getProjectScopes(long userId, long projectId) {
        return projectUserRepository.findByProjectIdAndUserId(projectId, userId)
            .map(projectUser -> dispatchScopes(projectUser, userId, projectId))
            .orElse(Collections.emptySet());
    }

    /**
     * Three branches: built-in role, custom role, or corrupted (XOR violation). Each branch is extracted to its own
     * method so this top-level dispatch stays a one-liner and each branch can be reasoned about \u2014 and tested
     * \u2014 in isolation. Important for an audit-grade hot path: every {@code @PreAuthorize} request reads uncached
     * scopes through here on cache miss, and the per-branch logging differs (data corruption vs. missing resolver bean
     * vs. CHECK violation), so reviewers should be able to scan each case without untangling the dispatch.
     */
    private Set<PermissionScope> dispatchScopes(ProjectUser projectUser, long userId, long projectId) {
        if (projectUser.getProjectRole() != null) {
            return resolveBuiltInScopes(projectUser.getProjectRole(), userId, projectId);
        }

        if (projectUser.getCustomRoleId() != null) {
            return resolveCustomRoleScopes(projectUser.getCustomRoleId(), userId, projectId);
        }

        // Both projectRole and customRoleId are null \u2014 the chk_project_user_role_xor CHECK constraint should
        // have prevented this row. Indicates corruption (manual SQL edit, failed migration).
        logger.error(
            "ProjectUser for userId={}, projectId={} has neither a built-in role nor a custom role \u2014 "
                + "returning empty permissions. This violates the chk_project_user_role_xor CHECK constraint and "
                + "indicates corrupted state.",
            userId, projectId);

        return Collections.emptySet();
    }

    private Set<PermissionScope> resolveBuiltInScopes(int ordinal, long userId, long projectId) {
        ProjectRole[] values = ProjectRole.values();

        if (ordinal < 0 || ordinal >= values.length) {
            // A stored ordinal outside the current enum range means corruption (manual SQL, skewed binary). Fail
            // closed rather than throw ArrayIndexOutOfBoundsException, which would surface as HTTP 500 and mask the
            // underlying data problem.
            logger.error(
                "project_user.project_role ordinal {} out of range [0,{}) for userId={}, projectId={} "
                    + "\u2014 returning empty permissions.",
                ordinal, values.length, userId, projectId);

            return Collections.emptySet();
        }

        return BuiltInRoleScopes.getScopesForRole(values[ordinal]);
    }

    private Set<PermissionScope> resolveCustomRoleScopes(long customRoleId, long userId, long projectId) {
        if (customRoleScopeResolver == null) {
            // Row is well-formed (custom role id present) but the resolver bean is missing \u2014 typical of
            // lightweight EE app variants that skip the custom-role module. Distinct from a CHECK constraint
            // violation, so keep operators from chasing a phantom DB-corruption alert.
            logger.error(
                "ProjectUser for userId={}, projectId={} carries customRoleId={} but no "
                    + "CustomRoleScopeResolver bean is available \u2014 returning empty permissions. "
                    + "Either deploy a build that includes the custom-role module or remove the custom "
                    + "role from this membership.",
                userId, projectId, customRoleId);

            return Collections.emptySet();
        }

        // Optional.empty() from the resolver means the custom_role_id is orphaned. The resolver has already logged
        // at ERROR; here we fail closed so the orphaned user is simply denied everything rather than carrying any
        // phantom permissions.
        return customRoleScopeResolver.resolveScopes(customRoleId)
            .orElseGet(Collections::emptySet);
    }

    /**
     * Schedules cache eviction for {@code (userId, projectId)} to run after the current transaction commits. If there
     * is no active transaction, evicts immediately so that callers outside a transaction (e.g., admin tooling) still
     * get the eviction. Eviction failures are logged and counted (see
     * {@code bytechef_permission_cache_eviction_failure{scope="single"}} metric) — they cannot abort the
     * already-committed transaction, but operators must be able to detect sustained failures.
     */
    public void evictProjectScopeCache(long userId, long projectId) {
        Runnable eviction = () -> self.doEvictProjectScopeCache(userId, projectId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeEvictSingle(eviction, userId, projectId);
                }
            });
        } else {
            safeEvictSingle(eviction, userId, projectId);
        }
    }

    /**
     * Batch variant of {@link #evictProjectScopeCache(long, long)}. Registers ONE
     * {@code TransactionSynchronization.afterCommit} callback that iterates over all supplied pairs. Workspace-wide
     * operations (e.g., {@code WorkspaceServiceImpl.delete}) would otherwise schedule one synchronization per (member
     * \u00d7 project) pair \u2014 with dozens of members and hundreds of projects, that's thousands of allocations on
     * the tx commit path. Batching keeps both the synchronization list and the closure count at O(1) per transaction
     * regardless of pair count.
     *
     * <p>
     * Each individual eviction inside the loop is still wrapped by {@link #safeEvictSingle} so a single failure does
     * not abort the rest. Empty collections are a no-op (avoids registering a synchronization that does nothing).
     *
     * <p>
     * The pair collection is snapshotted at call time \u2014 callers are free to clear or mutate their source
     * collection between call and commit without affecting which pairs get evicted.
     */
    public void evictProjectScopeCaches(Collection<UserProjectPair> userProjectPairs) {
        if (userProjectPairs.isEmpty()) {
            return;
        }

        List<UserProjectPair> snapshot = List.copyOf(userProjectPairs);

        Runnable eviction = () -> {
            for (UserProjectPair pair : snapshot) {
                safeEvictSingle(
                    () -> self.doEvictProjectScopeCache(pair.userId(), pair.projectId()),
                    pair.userId(), pair.projectId());
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

    /**
     * Schedules a full cache eviction to run after the current transaction commits. See
     * {@link #evictProjectScopeCache(long, long)} for the rationale. Failures increment
     * {@code bytechef_permission_cache_eviction_failure{scope="allEntries"}}.
     */
    public void evictAllProjectScopeCache() {
        Runnable eviction = () -> self.doEvictAllProjectScopeCache();

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

    /**
     * Runs a single-key eviction, logging and counting failures instead of propagating them. Propagating would be
     * pointless from {@code afterCommit} (Spring catches and swallows) and harmful from the immediate path (it would
     * obscure the already-successful database mutation with a cache-layer exception). Operators rely on the counter
     * (tagged {@code scope=single}) and the ERROR log to detect sustained outages.
     */
    private void safeEvictSingle(Runnable eviction, long userId, long projectId) {
        try {
            eviction.run();
        } catch (RuntimeException exception) {
            if (singleEvictionFailureCounter != null) {
                singleEvictionFailureCounter.increment();
            }

            logger.error(
                "PERMISSION_CACHE_EVICTION_FAILURE[single]: Failed to evict project scopes for userId={}, "
                    + "projectId={}. Stale permissions may be served until TTL expires.",
                userId, projectId, exception);
        }
    }

    /**
     * Runs an all-entries eviction with the same swallow-and-log discipline as
     * {@link #safeEvictSingle(Runnable, long, long)}. Logged separately (no userId/projectId placeholders) so operators
     * paging on the {@code allEntries}-tagged counter see only eviction-scope context.
     */
    private void safeEvictAllEntries(Runnable eviction) {
        try {
            eviction.run();
        } catch (RuntimeException exception) {
            if (allEntriesEvictionFailureCounter != null) {
                allEntriesEvictionFailureCounter.increment();
            }

            logger.error(
                "PERMISSION_CACHE_EVICTION_FAILURE[allEntries]: Failed to evict the entire project scopes cache. "
                    + "Stale permissions may be served until TTL expires.",
                exception);
        }
    }

    @CacheEvict(value = PROJECT_SCOPES_CACHE)
    public void doEvictProjectScopeCache(long userId, long projectId) {
    }

    @CacheEvict(value = PROJECT_SCOPES_CACHE, allEntries = true)
    public void doEvictAllProjectScopeCache() {
    }
}
