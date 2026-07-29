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

/**
 * Keyed token-bucket rate limiter SPI. The default implementation is in-process (Bucket4j local buckets in a Caffeine
 * cache); a distributed deployment replaces the bean with a Redis/JCache-backed Bucket4j {@code ProxyManager}
 * implementation without touching callers.
 *
 * @author Ivica Cardic
 */
public interface RateLimiter {

    /**
     * Consumes one permit from the bucket identified by {@code key} under {@code policy}. Returns {@code false} when
     * the bucket is exhausted — the caller decides the rejection semantics (HTTP 429, queue, drop).
     */
    boolean tryConsume(String key, RateLimitPolicy policy);
}
