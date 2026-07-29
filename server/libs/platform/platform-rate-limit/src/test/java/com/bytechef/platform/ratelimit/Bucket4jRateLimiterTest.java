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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class Bucket4jRateLimiterTest {

    private final Bucket4jRateLimiter rateLimiter = new Bucket4jRateLimiter();

    @Test
    public void testBurstCapacityIsPermitsTimesMultiplier() {
        RateLimitPolicy policy = new RateLimitPolicy(5, 2);

        for (int attempt = 0; attempt < 10; attempt++) {
            assertThat(rateLimiter.tryConsume("tenant-a", policy)).isTrue();
        }

        assertThat(rateLimiter.tryConsume("tenant-a", policy)).isFalse();
    }

    @Test
    public void testKeysAreIndependent() {
        RateLimitPolicy policy = new RateLimitPolicy(2, 1);

        assertThat(rateLimiter.tryConsume("tenant-a", policy)).isTrue();
        assertThat(rateLimiter.tryConsume("tenant-a", policy)).isTrue();
        assertThat(rateLimiter.tryConsume("tenant-a", policy)).isFalse();

        assertThat(rateLimiter.tryConsume("tenant-b", policy)).isTrue();
    }

    @Test
    public void testPolicyChangeCreatesFreshBucket() {
        RateLimitPolicy smallPolicy = new RateLimitPolicy(1, 1);

        assertThat(rateLimiter.tryConsume("tenant-a", smallPolicy)).isTrue();
        assertThat(rateLimiter.tryConsume("tenant-a", smallPolicy)).isFalse();

        RateLimitPolicy largerPolicy = new RateLimitPolicy(3, 1);

        assertThat(rateLimiter.tryConsume("tenant-a", largerPolicy)).isTrue();
    }
}
