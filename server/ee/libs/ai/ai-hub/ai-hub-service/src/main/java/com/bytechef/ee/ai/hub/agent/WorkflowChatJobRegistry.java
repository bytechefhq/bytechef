/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Per-chat jobId registry for in-flight workflow-chat turns. It maps a chat id to the id of the workflow job running
 * its current turn, so a cancel request can resolve back to the right {@code JobFacade.stopJob(jobId)} call.
 *
 * <p>
 * Backed by Spring's {@link CacheManager} so multi-instance deployments work correctly: the thread that registers a
 * jobId and the request thread that cancels can land on different instances. The TTL matches the workflow engine's
 * job-execution timeout; a turn that outlives it is no longer cancellable through normal means anyway.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class WorkflowChatJobRegistry {

    public static final String CACHE_NAME =
        "com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry.jobIds";

    private final CacheManager cacheManager;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowChatJobRegistry(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Records the in-flight job id for the given chat, overwriting any prior entry.
     */
    public void register(long chatId, long jobId) {
        getCache().put(chatId, jobId);
    }

    /**
     * Clears the registered jobId for a chat when the turn finishes. Idempotent.
     */
    public void clear(long chatId) {
        getCache().evictIfPresent(chatId);
    }

    /**
     * Returns the current jobId for a chat, or {@code null} if none is registered. The entry is not removed on read so
     * a cancel request can be retried.
     */
    public @Nullable Long get(long chatId) {
        return getCache().get(chatId, Long.class);
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if (cache == null) {
            throw new IllegalStateException(
                "WorkflowChatJobRegistry cache '" + CACHE_NAME + "' is not configured. Add it to the "
                    + "CacheManager (Caffeine via registerCustomCache, Redis via withCacheConfiguration) so "
                    + "workflow-chat cancellation can resolve chatId to jobId.");
        }

        return cache;
    }
}
