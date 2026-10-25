/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation.config;

import com.bytechef.cache.SharedCaches;
import com.bytechef.ee.cache.invalidation.CacheInvalidationEvent;
import com.bytechef.ee.cache.invalidation.CacheInvalidationMessageRoute;
import com.bytechef.ee.cache.invalidation.ClusterInvalidatingCacheManager;
import com.bytechef.ee.cache.invalidation.JsonCacheValueRedisSerializer;
import com.bytechef.ee.cache.invalidation.SharedCacheRoutingCacheManager;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.time.Duration;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * Wraps the default cache manager so evictions reach every node. When Redis is configured, the caches named by
 * {@link SharedCaches} are kept in Redis instead, so every node reads and writes the same entries.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnExpression("!'${bytechef.message-broker.provider:memory}'.equalsIgnoreCase('memory') && " +
    "!('${bytechef.message-broker.provider:memory}'.equalsIgnoreCase('aws') && " +
    "!'${bytechef.message-broker.aws.control-provider:sqs}'.equalsIgnoreCase('redis'))")
public class CacheInvalidationConfiguration {

    private static final String SHARED_CACHE_KEY_PREFIX = "bytechef:cache:";
    private static final Duration SHARED_CACHE_TIME_TO_LIVE = Duration.ofMinutes(10);

    @Bean
    static BeanPostProcessor clusterInvalidatingCacheManagerBeanPostProcessor(
        ObjectProvider<MessageBroker> messageBrokerProvider,
        ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
        ObjectProvider<SharedCaches> sharedCachesProvider) {

        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!"cacheManager".equals(beanName) || !(bean instanceof CacheManager cacheManager) ||
                    bean instanceof ClusterInvalidatingCacheManager || bean instanceof SharedCacheRoutingCacheManager) {

                    return bean;
                }

                ClusterInvalidatingCacheManager clusterInvalidatingCacheManager = new ClusterInvalidatingCacheManager(
                    cacheManager,
                    (CacheInvalidationEvent cacheInvalidationEvent) -> messageBrokerProvider.getObject()
                        .send(CacheInvalidationMessageRoute.CACHE_INVALIDATION_EVENTS, cacheInvalidationEvent));

                RedisConnectionFactory redisConnectionFactory = redisConnectionFactoryProvider.getIfAvailable();

                if (redisConnectionFactory == null) {
                    return clusterInvalidatingCacheManager;
                }

                return new SharedCacheRoutingCacheManager(
                    clusterInvalidatingCacheManager, createRedisCacheManager(redisConnectionFactory),
                    () -> sharedCachesProvider.stream()
                        .map(SharedCaches::cacheNames)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toUnmodifiableSet()));
            }
        };
    }

    @Bean
    MessageBrokerConfigurer<?> cacheInvalidationMessageBrokerConfigurer(CacheManager cacheManager) {
        ClusterInvalidatingCacheManager clusterInvalidatingCacheManager = getClusterInvalidatingCacheManager(
            cacheManager);

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> messageBrokerListenerRegistrar
            .registerListenerEndpoint(
                listenerEndpointRegistrar, CacheInvalidationMessageRoute.CACHE_INVALIDATION_EVENTS, 1,
                clusterInvalidatingCacheManager, "onInvalidation");
    }

    private static RedisCacheManager createRedisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(SHARED_CACHE_TIME_TO_LIVE)
            .prefixCacheNameWith(SHARED_CACHE_KEY_PREFIX)
            .serializeValuesWith(SerializationPair.fromSerializer(new JsonCacheValueRedisSerializer()));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(redisCacheConfiguration)
            .build();

        redisCacheManager.afterPropertiesSet();

        return redisCacheManager;
    }

    private static ClusterInvalidatingCacheManager getClusterInvalidatingCacheManager(CacheManager cacheManager) {
        if (cacheManager instanceof SharedCacheRoutingCacheManager sharedCacheRoutingCacheManager) {
            return (ClusterInvalidatingCacheManager) sharedCacheRoutingCacheManager.getLocalCacheManager();
        }

        return (ClusterInvalidatingCacheManager) cacheManager;
    }
}
