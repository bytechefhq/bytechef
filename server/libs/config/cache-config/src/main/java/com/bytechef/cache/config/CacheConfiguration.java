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
import com.bytechef.cache.invalidation.CacheInvalidation;
import com.bytechef.cache.invalidation.ClusterInvalidatingCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    private static final String CACHE_INVALIDATION_CHANNEL = "bytechef:cache-invalidation";
    private static final String REENTRANT_LOCK_CACHE =
        "com.bytechef.platform.component.aspect.TokenRefreshHandler.reentrantLock";

    @Bean
    @ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "caffeine")
    public CacheManager cacheManager() {
        return createCaffeineCacheManager();
    }

    /**
     * Caches stay local to each node; Redis only carries evictions, so every node drops an entry another node changed.
     */
    @Bean("cacheManager")
    @ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "redis")
    public ClusterInvalidatingCacheManager clusterInvalidatingCacheManager(
        JsonMapper jsonMapper, StringRedisTemplate stringRedisTemplate) {

        return new ClusterInvalidatingCacheManager(
            createCaffeineCacheManager(),
            cacheInvalidation -> publish(cacheInvalidation, jsonMapper, stringRedisTemplate));
    }

    @Bean(autowireCandidate = false)
    @ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "redis")
    RedisMessageListenerContainer cacheInvalidationRedisMessageListenerContainer(
        ClusterInvalidatingCacheManager clusterInvalidatingCacheManager, JsonMapper jsonMapper,
        RedisConnectionFactory redisConnectionFactory) {

        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();

        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        redisMessageListenerContainer.addMessageListener(
            (message, pattern) -> clusterInvalidatingCacheManager.onInvalidation(
                jsonMapper.readValue(message.getBody(), CacheInvalidation.class)),
            new ChannelTopic(CACHE_INVALIDATION_CHANNEL));

        return redisMessageListenerContainer;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new TenantKeyGenerator();
    }

    private static CaffeineCacheManager createCaffeineCacheManager() {
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

        return caffeineCacheManager;
    }

    private static void publish(
        CacheInvalidation cacheInvalidation, JsonMapper jsonMapper, StringRedisTemplate stringRedisTemplate) {

        try {
            stringRedisTemplate.convertAndSend(
                CACHE_INVALIDATION_CHANNEL, jsonMapper.writeValueAsString(cacheInvalidation));
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to publish invalidation of cache {}, other nodes keep the entry until it expires",
                cacheInvalidation.cacheName(), exception);
        }
    }
}
