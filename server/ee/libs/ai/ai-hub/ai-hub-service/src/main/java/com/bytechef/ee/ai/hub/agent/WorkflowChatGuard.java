/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Per-chat rate limit for workflow-chat turns: at most one turn per {@value #RATE_LIMIT_MIN_INTERVAL_MS} milliseconds
 * on a given chat. A user who spams Enter or a client that retries on every keystroke cannot fire dozens of workflow
 * executions per second, which would otherwise be a denial-of-wallet vector for workflows that call paid APIs. The
 * cooldown is per chat rather than per user so parallel workflow chats stay independently usable. It is keyed by the
 * chat's thread id, a UUID, because numeric chat ids repeat across tenants in schema-per-tenant mode.
 *
 * <p>
 * Backed by Spring's {@link CacheManager} so multi-instance deployments share state. The check returns a typed result
 * rather than throwing so the caller can surface a friendly message.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class WorkflowChatGuard {

    public static final long RATE_LIMIT_MIN_INTERVAL_MS = 2_000L;

    public static final String LAST_TURN_CACHE_NAME =
        "com.bytechef.ee.ai.hub.agent.WorkflowChatGuard.lastTurn";

    private final CacheManager cacheManager;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowChatGuard(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Admits the turn unless the previous turn on the same chat started less than {@value #RATE_LIMIT_MIN_INTERVAL_MS}
     * milliseconds ago. An admitted turn records its start time so the cooldown is measured against the start of the
     * previous turn, not its end.
     */
    public AdmissionResult tryAdmit(String threadId) {
        Cache lastTurn = getCache();
        Long lastTurnMillis = lastTurn.get(threadId, Long.class);
        long now = Instant.now()
            .toEpochMilli();

        if (lastTurnMillis != null) {
            long elapsed = now - lastTurnMillis;

            if (elapsed < RATE_LIMIT_MIN_INTERVAL_MS) {
                return AdmissionResult.rateLimited(Duration.ofMillis(RATE_LIMIT_MIN_INTERVAL_MS - elapsed));
            }
        }

        lastTurn.put(threadId, now);

        return AdmissionResult.admit();
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache(LAST_TURN_CACHE_NAME);

        if (cache == null) {
            throw new IllegalStateException(
                "WorkflowChatGuard cache '" + LAST_TURN_CACHE_NAME + "' is not configured. Add it to the "
                    + "CacheManager (Caffeine via registerCustomCache, Redis via withCacheConfiguration).");
        }

        return cache;
    }

    /**
     * Result of a turn-admission check.
     */
    public sealed interface AdmissionResult {

        static AdmissionResult admit() {
            return Admit.INSTANCE;
        }

        static AdmissionResult rateLimited(Duration cooldownRemaining) {
            return new RateLimited(cooldownRemaining);
        }

        record Admit() implements AdmissionResult {

            static final Admit INSTANCE = new Admit();
        }

        record RateLimited(Duration cooldownRemaining) implements AdmissionResult {

            public String userFacingMessage() {
                long seconds = cooldownRemaining.toSeconds();

                if (seconds == 0) {
                    return "You're sending messages too quickly. Wait a moment and try again.";
                }

                return "You're sending messages too quickly. Wait " + (seconds + 1) + " seconds and try again.";
            }
        }

        @Nullable
        default String userFacingMessage() {
            return switch (this) {
                case Admit ignored -> null;
                case RateLimited rateLimited -> rateLimited.userFacingMessage();
            };
        }
    }
}
