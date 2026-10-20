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

    public void register(long chatId, long jobId) {
        getCache().put(chatId, jobId);
    }

    public void clear(long chatId) {
        getCache().evictIfPresent(chatId);
    }

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
