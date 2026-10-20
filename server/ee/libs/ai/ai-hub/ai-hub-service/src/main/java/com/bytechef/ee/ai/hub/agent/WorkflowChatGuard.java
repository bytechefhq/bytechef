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

    public AdmissionResult tryAdmit(long chatId) {
        Cache lastTurn = getCache();
        Long lastTurnMillis = lastTurn.get(chatId, Long.class);
        long now = Instant.now()
            .toEpochMilli();

        if (lastTurnMillis != null) {
            long elapsed = now - lastTurnMillis;

            if (elapsed < RATE_LIMIT_MIN_INTERVAL_MS) {
                return AdmissionResult.rateLimited(Duration.ofMillis(RATE_LIMIT_MIN_INTERVAL_MS - elapsed));
            }
        }

        lastTurn.put(chatId, now);

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
