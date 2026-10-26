/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.platform.ai.gateway.ratelimit;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnEEVersion
@ConditionalOnExpression("'${bytechef.ai.gateway.rate-limiting.enabled:false}'=='true' "
    + "and '${bytechef.ai.gateway.rate-limiting.provider:in-memory}'!='redis'")
public class InMemoryAiGatewayRateLimiter implements AiGatewayRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(InMemoryAiGatewayRateLimiter.class);

    private static final long EVICTION_AFTER_MILLIS = 15 * 60 * 1000L;

    private final ConcurrentMap<String, SlidingWindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public AiGatewayRateLimitResult tryAcquire(String key, int limit, int windowSeconds) {
        long windowMillis = windowSeconds * 1000L;
        long now = System.currentTimeMillis();

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

    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    void evictStaleCounters() {
        long cutoff = System.currentTimeMillis() - EVICTION_AFTER_MILLIS;
        int sizeBefore = counters.size();

        for (String key : counters.keySet()) {
            counters.computeIfPresent(key, (counterKey, counter) -> counter.windowStart < cutoff ? null : counter);
        }

        int evicted = sizeBefore - counters.size();

        if (evicted > 0) {
            log.debug(
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
