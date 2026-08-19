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
 * Per-chat resume URL registry. Workflow chats that emit {@code ask_user_question} pause execution and hand the client
 * a {@code resumeUrl} to POST the user's answer to. {@link AgUiStreamBridge} captures the URL on the way through;
 * {@link WebhookBridgeAgent} consults this registry on the next user turn to decide whether to start a fresh workflow
 * run (the default) or resume the paused one.
 *
 * <p>
 * Backed by Spring's {@link CacheManager} so multi-instance ai-hub deployments work correctly — the AG-UI subscriber
 * that registers a resume URL and the next-turn request that consumes it can land on different instances. With the
 * Caffeine backend (single-instance dev / monolith) the cache is in-memory; with the Redis backend (the EE microservice
 * topology) the entry is shared across all ai-hub instances. The cache name ({@value #CACHE_NAME}) is configured by the
 * platform's central {@code CacheConfiguration} — the same place that sets per-cache TTL when finer control than the
 * default is needed.
 * </p>
 *
 * <p>
 * <b>TTL is correctness, not just hygiene.</b> A paused workflow that exceeds the cache TTL also likely exceeds the
 * workflow engine's server-side timeout — a resume attempt after that boundary would fail anyway. Letting the cache
 * naturally evict aligned the registry's lifecycle with the workflow engine's own pause-window.
 * </p>
 *
 * <p>
 * <b>consume() is not strictly atomic across instances.</b> Spring Cache exposes {@link Cache#get} and
 * {@link Cache#evictIfPresent} as separate operations; with Redis there's a single network round-trip between them. The
 * worst-case race is a duplicate resume POST when two near-simultaneous turns from the same chat hit two different
 * instances and both observe the entry before either evicts. That requires (a) a buggy or retrying client AND (b)
 * cross-instance dispatch on the same chatId in the same millisecond — narrow enough that we accept it. If production
 * traces show this happening, swap in {@code RedisTemplate.opsForValue().getAndDelete} for true atomicity at the cost
 * of a Redis-specific dependency.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class WebhookResumeRegistry {

    /**
     * The named Spring cache that holds resume URLs. Registered on first access — the platform's
     * {@code CacheConfiguration} (Caffeine / Redis) handles eviction and TTL. Picking a fully qualified, ByteChef-
     * scoped name avoids collisions with other cache regions when the Redis backend is shared across teams.
     */
    public static final String CACHE_NAME =
        "com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry.resumeUrls";

    private final CacheManager cacheManager;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WebhookResumeRegistry(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Records a pending resume URL for the given chat. Called by {@link AgUiStreamBridge} when the underlying workflow
     * emits an {@code ask_user_question} event with a resume URL. Overwrites any prior entry — the workflow can issue a
     * new question on the same chat and the latest URL wins.
     */
    public void register(long chatId, String resumeUrl) {
        getCache().put(chatId, resumeUrl);
    }

    /**
     * Removes and returns any pending resume URL for the given chat. Called by {@link WebhookBridgeAgent} at the top of
     * {@code run()}: a non-null return means "this is a resume turn, POST the user's input to the URL"; a null return
     * means "regular new-execution turn."
     *
     * <p>
     * Uses Spring Cache's {@link Cache#get} + {@link Cache#evictIfPresent} pair. Not strictly atomic across instances —
     * see the class javadoc's "consume() is not strictly atomic" note for the acceptance rationale.
     * </p>
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
     * Resolve the cache lazily. Pulling it via {@code cacheManager.getCache(CACHE_NAME)} on each call lets the
     * platform's CacheConfiguration drop in or swap out the backing store (Caffeine ↔ Redis) without recycling this
     * bean. Throws when the cache hasn't been configured — that's an explicit deployment error rather than a silent
     * "resume URLs vanish" symptom.
     */
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
