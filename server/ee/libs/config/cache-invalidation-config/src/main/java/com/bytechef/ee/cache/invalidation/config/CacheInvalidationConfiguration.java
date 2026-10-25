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
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnExpression("!'${bytechef.message-broker.provider:memory}'.equalsIgnoreCase('memory')")
public class CacheInvalidationConfiguration {

    @Bean
    static BeanPostProcessor clusterInvalidatingCacheManagerBeanPostProcessor(
        ObjectProvider<MessageBroker> messageBrokerProvider) {

        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if ("cacheManager".equals(beanName) && bean instanceof CacheManager cacheManager &&
                    !(bean instanceof ClusterInvalidatingCacheManager)) {

                    return new ClusterInvalidatingCacheManager(
                        cacheManager,
                        (CacheInvalidationEvent cacheInvalidationEvent) -> messageBrokerProvider.getObject()
                            .send(CacheInvalidationMessageRoute.CACHE_INVALIDATION_EVENTS, cacheInvalidationEvent));
                }

                return bean;
            }
        };
    }

    @Bean
    MessageBrokerConfigurer<?> cacheInvalidationMessageBrokerConfigurer(CacheManager cacheManager) {
        ClusterInvalidatingCacheManager clusterInvalidatingCacheManager =
            (ClusterInvalidatingCacheManager) cacheManager;

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> messageBrokerListenerRegistrar
            .registerListenerEndpoint(
                listenerEndpointRegistrar, CacheInvalidationMessageRoute.CACHE_INVALIDATION_EVENTS, 1,
                clusterInvalidatingCacheManager, "onInvalidation");
    }
}
