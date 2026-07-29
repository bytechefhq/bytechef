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

package com.bytechef.platform.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;

/**
 * In-process token-bucket limiter: one Bucket4j local bucket per (key, policy), held in a Caffeine cache so idle
 * tenants expire instead of leaking. Semantics follow Sim: capacity = sustained rate x burst multiplier, refilled
 * greedily at the sustained per-minute rate — a quiet tenant can burst to capacity, a busy one converges on the
 * sustained rate. The policy is part of the cache key, so a plan change simply starts a fresh bucket.
 *
 * <p>
 * Per-node enforcement only: in an HA deployment each node grants the full budget. That over-admits by the node count
 * at worst — acceptable for the abuse-control goal; swap the {@code RateLimiter} bean for a Bucket4j
 * {@code ProxyManager} (Redis/JCache) implementation when strict global limits are required.
 * </p>
 *
 * @author Ivica Cardic
 */
public class Bucket4jRateLimiter implements RateLimiter {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(100_000)
        .build();

    @Override
    public boolean tryConsume(String key, RateLimitPolicy policy) {
        Bucket bucket = buckets.get(
            key + "|" + policy.permitsPerMinute() + "x" + policy.burstMultiplier(),
            cacheKey -> Bucket.builder()
                .addLimit(
                    Bandwidth.builder()
                        .capacity(policy.capacity())
                        .refillGreedy(policy.permitsPerMinute(), Duration.ofMinutes(1))
                        .build())
                .build());

        return bucket.tryConsume(1);
    }
}
