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
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private static final String REENTRANT_LOCK_CACHE = "TokenRefreshHelper.reentrantLock";

    @Bean
    @ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "redis")
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        Class<?> clazz = getClass();

        return (builder) -> {
            ClassLoader classLoader = clazz.getClassLoader();

            builder.cacheDefaults(getCacheConfiguration(10, classLoader))
                .withCacheConfiguration(REENTRANT_LOCK_CACHE, getCacheConfiguration(1, classLoader));
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "caffeine")
    public CacheManager cacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

        caffeineCacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES));

        caffeineCacheManager.registerCustomCache(
            REENTRANT_LOCK_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build());

        return caffeineCacheManager;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new TenantKeyGenerator();
    }

    private static RedisCacheConfiguration getCacheConfiguration(int minutes, ClassLoader clazz) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(minutes))
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(clazz)));
    }
}
