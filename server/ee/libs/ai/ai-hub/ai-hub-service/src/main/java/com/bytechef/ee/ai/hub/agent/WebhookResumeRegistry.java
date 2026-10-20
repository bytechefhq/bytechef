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
 * Per-chat resume URL registry. Workflow chats that emit {@code ask_user_question} pause execution and hand the client
 * a {@code resumeUrl} to POST the user's answer to. The URL is registered here on the way through and consumed on the
 * next user turn to decide whether to start a fresh workflow run (the default) or resume the paused one.
 *
 * <p>
 * Backed by Spring's {@link CacheManager} so multi-instance deployments work correctly. The cache name
 * ({@value #CACHE_NAME}) is configured by the platform's central {@code CacheConfiguration}, which also sets the TTL.
 * </p>
 *
 * <p>
 * <b>TTL is correctness, not just hygiene.</b> A paused workflow that exceeds the cache TTL also likely exceeds the
 * workflow engine's server-side timeout, so a resume attempt after that boundary would fail anyway.
 * </p>
 *
 * <p>
 * <b>consume() is not strictly atomic across instances.</b> Spring Cache exposes {@link Cache#get} and
 * {@link Cache#evictIfPresent} as separate operations. The worst case is a duplicate resume POST when two
 * near-simultaneous turns from the same chat hit two different instances and both observe the entry before either
 * evicts; that is narrow enough to accept.
 * </p>
 *
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

    /**
     * Records a pending resume URL for the given chat. Overwrites any prior entry; the latest URL wins.
     */
    public void register(long chatId, String resumeUrl) {
        getCache().put(chatId, resumeUrl);
    }

    /**
     * Removes and returns any pending resume URL for the given chat. A non-null return means the turn is a resume turn;
     * {@code null} means a regular new-execution turn.
     */
    public @Nullable String consume(long chatId) {
        Cache cache = getCache();

        String resumeUrl = cache.get(chatId, String.class);

        if (resumeUrl != null) {
            cache.evictIfPresent(chatId);
        }

        return resumeUrl;
    }

    /**
     * Drops any pending resume URL for the given chat. Idempotent.
     */
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
