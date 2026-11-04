/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation.config;

import com.bytechef.cache.SharedCacheSupport;
import com.bytechef.cache.SharedCaches;
import com.bytechef.ee.cache.invalidation.JsonCacheValueRedisSerializer;
import com.bytechef.ee.cache.invalidation.SharedCacheRoutingCacheManager;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.time.Duration;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.cache.RedisCacheWriter.RedisCacheWriterConfigurer;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * Keeps the caches named by {@link SharedCaches} in Redis, so every node reads and writes the same entries.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "redis")
public class SharedCacheConfiguration {

    static final String REDIS_UNAVAILABLE_MESSAGE =
        "bytechef.cache.provider=redis needs a reachable Redis; check spring.data.redis.*";

    private static final String SHARED_CACHE_KEY_PREFIX = "bytechef:cache:";
    private static final Duration SHARED_CACHE_TIME_TO_LIVE = Duration.ofMinutes(10);

    @Bean
    static SharedCacheRoutingCacheManagerBeanPostProcessor sharedCacheRoutingCacheManagerBeanPostProcessor(
        ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
        ObjectProvider<SharedCaches> sharedCachesProvider) {

        return new SharedCacheRoutingCacheManagerBeanPostProcessor(
            redisConnectionFactoryProvider, sharedCachesProvider);
    }

    @Bean
    SharedCacheSupport sharedCacheSupport() {
        return new SharedCacheSupport() {};
    }

    private static RedisCacheManager createRedisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(SHARED_CACHE_TIME_TO_LIVE)
            .prefixCacheNameWith(SHARED_CACHE_KEY_PREFIX)
            .serializeValuesWith(SerializationPair.fromSerializer(new JsonCacheValueRedisSerializer()));

        RedisCacheManager redisCacheManager = RedisCacheManager
            .builder(RedisCacheWriter.create(redisConnectionFactory, RedisCacheWriterConfigurer::immediateWrites))
            .cacheDefaults(redisCacheConfiguration)
            .build();

        redisCacheManager.afterPropertiesSet();

        return redisCacheManager;
    }

    private static RedisConnectionFactory getReachableRedisConnectionFactory(
        ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider) {

        try {
            RedisConnectionFactory redisConnectionFactory = redisConnectionFactoryProvider.getObject();

            try (RedisConnection redisConnection = redisConnectionFactory.getConnection()) {
                redisConnection.ping();
            }

            return redisConnectionFactory;
        } catch (RuntimeException exception) {
            throw new IllegalStateException(REDIS_UNAVAILABLE_MESSAGE, exception);
        }
    }

    static final class SharedCacheRoutingCacheManagerBeanPostProcessor implements BeanPostProcessor, Ordered {

        private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
        private final ObjectProvider<SharedCaches> sharedCachesProvider;

        private SharedCacheRoutingCacheManagerBeanPostProcessor(
            ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
            ObjectProvider<SharedCaches> sharedCachesProvider) {

            this.redisConnectionFactoryProvider = redisConnectionFactoryProvider;
            this.sharedCachesProvider = sharedCachesProvider;
        }

        @Override
        public int getOrder() {
            return LOWEST_PRECEDENCE;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (!"cacheManager".equals(beanName) || !(bean instanceof CacheManager cacheManager) ||
                bean instanceof SharedCacheRoutingCacheManager) {

                return bean;
            }

            RedisConnectionFactory redisConnectionFactory = getReachableRedisConnectionFactory(
                redisConnectionFactoryProvider);

            return new SharedCacheRoutingCacheManager(
                cacheManager, createRedisCacheManager(redisConnectionFactory),
                () -> sharedCachesProvider.stream()
                    .map(SharedCaches::cacheNames)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toUnmodifiableSet()));
        }
    }
}
