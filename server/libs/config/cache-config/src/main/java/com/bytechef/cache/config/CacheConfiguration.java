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

import com.bytechef.cache.interceptor.TenantKeyGenerator;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {

    private static final Duration MINIMUM_NOTIFICATION_THROTTLE_TTL = Duration.ofMinutes(10);
    private static final String NOTIFICATION_THROTTLE_CLAIM_CACHE =
        "com.bytechef.platform.coordinator.notification.NotificationThrottle.claim";
    private static final String NOTIFICATION_THROTTLE_SUPPRESSED_COUNT_CACHE =
        "com.bytechef.platform.coordinator.notification.NotificationThrottle.suppressedCount";
    private static final String NOTIFICATION_THROTTLE_WINDOW_PROPERTY =
        "bytechef.mail.failure-notification-throttle-window";
    private static final String REENTRANT_LOCK_CACHE =
        "com.bytechef.platform.component.aspect.TokenRefreshHandler.reentrantLock";

    @Bean
    @ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "redis")
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(Environment environment) {
        Class<?> clazz = getClass();
        Duration notificationThrottleWindow = getNotificationThrottleWindow(environment);

        return (builder) -> {
            ClassLoader classLoader = clazz.getClassLoader();

            builder.cacheDefaults(getCacheConfiguration(Duration.ofMinutes(10), classLoader))
                .withCacheConfiguration(REENTRANT_LOCK_CACHE, getCacheConfiguration(Duration.ofMinutes(1), classLoader))
                .withCacheConfiguration(
                    NOTIFICATION_THROTTLE_CLAIM_CACHE,
                    getCacheConfiguration(getClaimTtl(notificationThrottleWindow), classLoader))
                .withCacheConfiguration(
                    NOTIFICATION_THROTTLE_SUPPRESSED_COUNT_CACHE,
                    getCacheConfiguration(getSuppressedCountTtl(notificationThrottleWindow), classLoader));
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "caffeine")
    public CacheManager cacheManager(Environment environment) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

        caffeineCacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(10, TimeUnit.MINUTES));

        caffeineCacheManager.registerCustomCache(
            REENTRANT_LOCK_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build());

        Duration notificationThrottleWindow = getNotificationThrottleWindow(environment);

        caffeineCacheManager.registerCustomCache(
            NOTIFICATION_THROTTLE_CLAIM_CACHE,
            Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(getClaimTtl(notificationThrottleWindow))
                .build());

        caffeineCacheManager.registerCustomCache(
            NOTIFICATION_THROTTLE_SUPPRESSED_COUNT_CACHE,
            Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(getSuppressedCountTtl(notificationThrottleWindow))
                .build());

        return caffeineCacheManager;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new TenantKeyGenerator();
    }

    private static RedisCacheConfiguration getCacheConfiguration(Duration ttl, ClassLoader clazz) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(clazz)));
    }

    private static Duration getClaimTtl(Duration window) {
        return window.isZero() || window.isNegative() ? MINIMUM_NOTIFICATION_THROTTLE_TTL : window;
    }

    private static Duration getNotificationThrottleWindow(Environment environment) {
        Binder binder = Binder.get(environment);

        BindResult<Duration> bindResult = binder.bind(NOTIFICATION_THROTTLE_WINDOW_PROPERTY, Duration.class);

        return bindResult.isBound() ? bindResult.get() : Duration.ofHours(1);
    }

    private static Duration getSuppressedCountTtl(Duration window) {
        Duration ttl = window.multipliedBy(2);

        return ttl.compareTo(MINIMUM_NOTIFICATION_THROTTLE_TTL) < 0 ? MINIMUM_NOTIFICATION_THROTTLE_TTL : ttl;
    }
}
