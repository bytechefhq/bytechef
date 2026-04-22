/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * In-memory sliding window rate limiter using ConcurrentHashMap.
 *
 * <p>
 * Counters are scoped by {@code (workspace, project, user)} tuples so the map grows with distinct callers over time. A
 * scheduled sweep every 5 minutes evicts entries whose window has long since expired to prevent unbounded memory growth
 * on long-lived instances with many users.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
// Activates when rate limiting is enabled AND provider != "redis"; this keeps InMemory and Redis limiter beans
// mutually exclusive so AiGatewayRateLimiter injection never hits NoUniqueBeanDefinitionException.
@ConditionalOnExpression("'${bytechef.ai.gateway.rate-limiting.enabled:false}'=='true' "
    + "and '${bytechef.ai.gateway.rate-limiting.provider:in-memory}'!='redis'")
public class InMemoryAiGatewayRateLimiter implements AiGatewayRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryAiGatewayRateLimiter.class);

    // Entries whose last window ended more than this long ago are safe to evict — no in-flight tryAcquire can still
    // be comparing against them. Picked to be much larger than any reasonable rate-limit window.
    private static final long EVICTION_AFTER_MILLIS = 15 * 60 * 1000L;

    private final ConcurrentMap<String, SlidingWindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public AiGatewayRateLimitResult tryAcquire(String key, int limit, int windowSeconds) {
        long windowMillis = windowSeconds * 1000L;
        long now = System.currentTimeMillis();

        // Snapshot the counter state inside the compute lambda — reading counter fields after
        // the lambda returns is racy because another thread may mutate them via a concurrent compute.
        int[] snapshot = new int[1];
        long[] windowStartSnapshot = new long[1];

        counters.compute(key, (counterKey, existing) -> {
            if (existing == null || now - existing.windowStart >= windowMillis) {
                SlidingWindowCounter fresh = new SlidingWindowCounter(now, 1);

                snapshot[0] = fresh.count;
                windowStartSnapshot[0] = fresh.windowStart;

                return fresh;
            }

            existing.count++;

            snapshot[0] = existing.count;
            windowStartSnapshot[0] = existing.windowStart;

            return existing;
        });

        long resetAtEpochMs = windowStartSnapshot[0] + windowMillis;
        int count = snapshot[0];
        int remaining = Math.max(0, limit - count);

        if (count > limit) {
            return AiGatewayRateLimitResult.rejected(remaining, resetAtEpochMs);
        }

        return AiGatewayRateLimitResult.allowed(remaining, resetAtEpochMs);
    }

    @Override
    public void reset(String key) {
        counters.remove(key);
    }

    /**
     * Periodic sweep that removes counter entries whose window started more than {@link #EVICTION_AFTER_MILLIS} ago.
     * Runs every 5 minutes. The cutoff is intentionally much larger than any reasonable rate-limit window so there is
     * no risk of evicting an entry a concurrent tryAcquire might still be comparing against — the worst case is a
     * freshly evicted caller starts the next window from scratch, which is the same as a new caller.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    void evictStaleCounters() {
        long cutoff = System.currentTimeMillis() - EVICTION_AFTER_MILLIS;
        int sizeBefore = counters.size();

        // Read each counter's windowStart through computeIfPresent so the load happens under the same
        // ConcurrentHashMap bin lock as the concurrent tryAcquire write — avoids a data race on the non-volatile
        // long field (torn reads on 32-bit JVMs; stale reads otherwise).
        for (String key : counters.keySet()) {
            counters.computeIfPresent(key, (counterKey, counter) -> counter.windowStart < cutoff ? null : counter);
        }

        int evicted = sizeBefore - counters.size();

        if (evicted > 0) {
            logger.debug(
                "InMemoryAiGatewayRateLimiter evicted {} stale counters (size: {} -> {})",
                evicted, sizeBefore, counters.size());
        }
    }

    private static class SlidingWindowCounter {

        int count;
        long windowStart;

        SlidingWindowCounter(long windowStart, int count) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}
