/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Per-task jobId registry for in-flight workflow-chat turns. The streaming executor emits a
 * {@code event=start, payload.jobId=...} event when a workflow run begins; {@link AgUiStreamBridge} captures that jobId
 * and registers it here so a cancel request from the user can map back to the right {@code JobFacade.stopJob(jobId)}
 * call. Without the registry the cancel mutation has no way to resolve taskId to jobId — the AG-UI per-turn runId isn't
 * the same as the executor's job identifier, and the task row itself doesn't track in-flight job ids.
 *
 * <p>
 * Backed by Spring's {@link CacheManager} so multi-instance ai-hub deployments work correctly: the subscriber thread
 * that registers a jobId and the GraphQL request thread that cancels can land on different instances. Same backing
 * store as {@link WebhookResumeRegistry} (Caffeine for single-instance dev, Redis for the EE microservice topology) —
 * see that class for the cross-instance atomicity caveats which apply identically here.
 * </p>
 *
 * <p>
 * <b>TTL aligns with the workflow engine's job-execution timeout.</b> A turn that runs longer than the cache TTL has,
 * by definition, exceeded the workflow engine's own timeout and is no longer cancellable through normal means anyway.
 * Letting the entry naturally evict matches the engine's own lifecycle.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class WorkflowChatJobRegistry {

    /**
     * Named Spring cache that holds in-flight job ids. Registered by {@code CacheConfiguration} (Caffeine / Redis) —
     * same TTL as the resume URL cache (30 minutes), since both protect ephemeral per-turn state with identical
     * lifetime requirements.
     */
    public static final String CACHE_NAME =
        "com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry.jobIds";

    private final CacheManager cacheManager;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowChatJobRegistry(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Records the in-flight job id for the given task. Called by {@link AgUiStreamBridge} when the executor emits
     * {@code event=start, payload.jobId=...}. Overwrites any prior entry — a task with a previous jobId still in the
     * cache (e.g. the previous turn's job that finished but whose entry hasn't yet been evicted) gets its slot replaced
     * by the current turn.
     */
    public void register(long taskId, long jobId) {
        getCache().put(taskId, jobId);
    }

    /**
     * Atomically clears the registered jobId for a task when the turn finishes. Idempotent — calling with no entry
     * present is a no-op. Called by {@link AgUiStreamBridge#onComplete} and {@link AgUiStreamBridge#onError} so a
     * finished turn doesn't leave a stale jobId lying around for the cancel mutation to fire against.
     */
    public void clear(long taskId) {
        getCache().evictIfPresent(taskId);
    }

    /**
     * Returns the current jobId for a task, or {@code null} if none is registered. Called by the cancel-mutation
     * handler. We deliberately do NOT remove on read — the cancel call needs to be idempotent (the caller may retry on
     * transient failure) and racing the eviction with the cancel-followed-by-RUN_FINISHED order isn't worth the
     * complexity.
     */
    public @Nullable Long get(long taskId) {
        return getCache().get(taskId, Long.class);
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if (cache == null) {
            throw new IllegalStateException(
                "WorkflowChatJobRegistry cache '" + CACHE_NAME + "' is not configured. Add it to the "
                    + "CacheManager (Caffeine via registerCustomCache, Redis via withCacheConfiguration) so "
                    + "workflow-chat cancellation can resolve taskId to jobId.");
        }

        return cache;
    }
}
