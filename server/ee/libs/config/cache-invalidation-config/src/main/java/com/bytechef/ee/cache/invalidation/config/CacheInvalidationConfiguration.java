/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation.config;

import com.bytechef.ee.cache.invalidation.CacheInvalidationEvent;
import com.bytechef.ee.cache.invalidation.CacheInvalidationMessageRoute;
import com.bytechef.ee.cache.invalidation.ClusterInvalidatingCacheManager;
import com.bytechef.ee.cache.invalidation.SharedCacheRoutingCacheManager;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Wraps the default cache manager so evictions reach every node.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnExpression("!'${bytechef.message-broker.provider}'.equalsIgnoreCase('MEMORY') && " +
    "!('${bytechef.message-broker.provider}'.equalsIgnoreCase('AWS') && " +
    "!'${bytechef.message-broker.aws.control-provider}'.equalsIgnoreCase('REDIS'))")
public class CacheInvalidationConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationConfiguration.class);

    @Bean
    static ClusterInvalidatingCacheManagerBeanPostProcessor clusterInvalidatingCacheManagerBeanPostProcessor(
        ObjectProvider<MessageBroker> messageBrokerProvider) {

        return new ClusterInvalidatingCacheManagerBeanPostProcessor(messageBrokerProvider);
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

    @Bean
    SmartInitializingSingleton sharedCacheProviderWarning(
        Environment environment, ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider) {

        return () -> {
            if (!"redis".equalsIgnoreCase(environment.getProperty("bytechef.cache.provider")) &&
                redisConnectionFactoryProvider.getIfAvailable() != null) {

                log.warn(
                    "Redis is configured but bytechef.cache.provider is caffeine, so shared caches stay on each " +
                        "node; set bytechef.cache.provider=redis to share them");
            }
        };
    }

    private static ClusterInvalidatingCacheManager getClusterInvalidatingCacheManager(CacheManager cacheManager) {
        if (cacheManager instanceof SharedCacheRoutingCacheManager sharedCacheRoutingCacheManager) {
            return (ClusterInvalidatingCacheManager) sharedCacheRoutingCacheManager.getLocalCacheManager();
        }

        return (ClusterInvalidatingCacheManager) cacheManager;
    }

    static final class ClusterInvalidatingCacheManagerBeanPostProcessor
        implements BeanPostProcessor, Ordered {

        private final ObjectProvider<MessageBroker> messageBrokerProvider;

        private ClusterInvalidatingCacheManagerBeanPostProcessor(ObjectProvider<MessageBroker> messageBrokerProvider) {
            this.messageBrokerProvider = messageBrokerProvider;
        }

        @Override
        public int getOrder() {
            return LOWEST_PRECEDENCE - 100;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (!"cacheManager".equals(beanName) || !(bean instanceof CacheManager cacheManager) ||
                bean instanceof ClusterInvalidatingCacheManager || bean instanceof SharedCacheRoutingCacheManager) {

                return bean;
            }

            return new ClusterInvalidatingCacheManager(
                cacheManager,
                (CacheInvalidationEvent cacheInvalidationEvent) -> messageBrokerProvider.getObject()
                    .send(CacheInvalidationMessageRoute.CACHE_INVALIDATION_EVENTS, cacheInvalidationEvent));
        }
    }
}
