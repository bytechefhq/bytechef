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

package com.bytechef.cache.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Policy;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
class CacheConfigurationTest {

    private static final String CLAIM_CACHE =
        "com.bytechef.platform.coordinator.notification.NotificationThrottle.claim";
    private static final String SUPPRESSED_COUNT_CACHE =
        "com.bytechef.platform.coordinator.notification.NotificationThrottle.suppressedCount";

    private final CacheConfiguration cacheConfiguration = new CacheConfiguration();

    @Test
    void testCaffeineNotificationThrottleCachesFollowTheDefaultWindow() {
        CacheManager cacheManager = cacheConfiguration.cacheManager(new MockEnvironment());

        Policy<Object, Object> claimPolicy = getPolicy(cacheManager, CLAIM_CACHE);

        assertThat(getExpiresAfter(claimPolicy)).isEqualTo(Duration.ofHours(1));
        assertThat(claimPolicy.eviction()
            .orElseThrow()
            .getMaximum()).isEqualTo(10_000);

        Policy<Object, Object> suppressedCountPolicy = getPolicy(cacheManager, SUPPRESSED_COUNT_CACHE);

        assertThat(getExpiresAfter(suppressedCountPolicy)).isEqualTo(Duration.ofHours(2));
        assertThat(suppressedCountPolicy.eviction()
            .orElseThrow()
            .getMaximum()).isEqualTo(10_000);
    }

    @Test
    void testCaffeineNotificationThrottleCachesFollowTheConfiguredWindow() {
        CacheManager cacheManager = cacheConfiguration.cacheManager(
            new MockEnvironment().withProperty("bytechef.mail.failure-notification-throttle-window", "3h"));

        assertThat(getExpiresAfter(getPolicy(cacheManager, CLAIM_CACHE))).isEqualTo(Duration.ofHours(3));
        assertThat(getExpiresAfter(getPolicy(cacheManager, SUPPRESSED_COUNT_CACHE))).isEqualTo(Duration.ofHours(6));
    }

    @Test
    void testCaffeineNotificationThrottleCachesKeepAMinimumExpiryWhenDisabled() {
        CacheManager cacheManager = cacheConfiguration.cacheManager(
            new MockEnvironment().withProperty("bytechef.mail.failure-notification-throttle-window", "0"));

        assertThat(getExpiresAfter(getPolicy(cacheManager, CLAIM_CACHE))).isEqualTo(Duration.ofMinutes(10));
        assertThat(getExpiresAfter(getPolicy(cacheManager, SUPPRESSED_COUNT_CACHE)))
            .isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void testRedisNotificationThrottleCachesFollowTheConfiguredWindow() {
        RedisCacheManager.RedisCacheManagerBuilder redisCacheManagerBuilder = RedisCacheManager.builder();

        cacheConfiguration
            .redisCacheManagerBuilderCustomizer(
                new MockEnvironment().withProperty("bytechef.mail.failure-notification-throttle-window", "90m"))
            .customize(redisCacheManagerBuilder);

        assertThat(getRedisTtl(redisCacheManagerBuilder, CLAIM_CACHE)).isEqualTo(Duration.ofMinutes(90));
        assertThat(getRedisTtl(redisCacheManagerBuilder, SUPPRESSED_COUNT_CACHE)).isEqualTo(Duration.ofHours(3));
    }

    private static Duration getExpiresAfter(Policy<Object, Object> policy) {
        return policy.expireAfterWrite()
            .orElseThrow()
            .getExpiresAfter();
    }

    private static Policy<Object, Object> getPolicy(CacheManager cacheManager, String name) {
        Cache cache = cacheManager.getCache(name);

        assertThat(cache).isInstanceOf(CaffeineCache.class);

        CaffeineCache caffeineCache = (CaffeineCache) cache;

        return caffeineCache.getNativeCache()
            .policy();
    }

    private static Duration
        getRedisTtl(RedisCacheManager.RedisCacheManagerBuilder redisCacheManagerBuilder, String name) {
        return redisCacheManagerBuilder.getCacheConfigurationFor(name)
            .orElseThrow()
            .getTtlFunction()
            .getTimeToLive("key", "value");
    }
}
