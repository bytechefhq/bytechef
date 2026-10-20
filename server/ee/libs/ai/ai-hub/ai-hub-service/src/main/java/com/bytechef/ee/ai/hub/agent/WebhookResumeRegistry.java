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
public class WebhookResumeRegistry {

    public static final String CACHE_NAME =
        "com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry.resumeUrls";

    private final CacheManager cacheManager;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WebhookResumeRegistry(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void register(long chatId, String resumeUrl) {
        getCache().put(chatId, resumeUrl);
    }

    public @Nullable String consume(long chatId) {
        Cache cache = getCache();

        String resumeUrl = cache.get(chatId, String.class);

        if (resumeUrl != null) {
            cache.evictIfPresent(chatId);
        }

        return resumeUrl;
    }

    public void clear(long chatId) {
        getCache().evictIfPresent(chatId);
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if (cache == null) {
            throw new IllegalStateException(
                "WebhookResumeRegistry cache '" + CACHE_NAME + "' is not configured. Add it to the CacheManager "
                    + "(Caffeine via registerCustomCache, Redis via withCacheConfiguration) so workflow-chat "
                    + "ask_user_question pause/resume can persist between turns.");
        }

        return cache;
    }
}
