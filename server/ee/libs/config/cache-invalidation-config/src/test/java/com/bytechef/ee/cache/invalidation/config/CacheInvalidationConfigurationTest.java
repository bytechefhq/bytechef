/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation.config;

import static com.bytechef.ee.cache.invalidation.CacheInvalidationMessageRoute.CACHE_INVALIDATION_EVENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.cache.invalidation.CacheInvalidationEvent;
import com.bytechef.ee.cache.invalidation.ClusterInvalidatingCacheManager;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CacheInvalidationConfigurationTest {

    private final MessageBroker messageBroker = mock(MessageBroker.class);

    @Test
    void testWrapsCacheManagerInEeWithAmqpBroker() {
        contextRunner("bytechef.edition=ee", "bytechef.message-broker.provider=amqp")
            .run(context -> assertThat(context.getBean(CacheManager.class))
                .isInstanceOf(ClusterInvalidatingCacheManager.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWrapsCacheManagerAndRegistersListenerOnAWebhookNode() {
        contextRunner(
            "bytechef.edition=ee", "bytechef.message-broker.provider=amqp", "bytechef.coordinator.enabled=false",
            "bytechef.worker.enabled=false")
                .run(context -> {
                    assertThat(context.getBean(CacheManager.class)).isInstanceOf(ClusterInvalidatingCacheManager.class);

                    MessageBrokerConfigurer<Object> messageBrokerConfigurer =
                        (MessageBrokerConfigurer<Object>) context.getBean("cacheInvalidationMessageBrokerConfigurer");
                    MessageBrokerListenerRegistrar<Object> messageBrokerListenerRegistrar =
                        mock(MessageBrokerListenerRegistrar.class);

                    messageBrokerConfigurer.configure(new Object(), messageBrokerListenerRegistrar);

                    verify(messageBrokerListenerRegistrar).registerListenerEndpoint(
                        any(), eq(CACHE_INVALIDATION_EVENTS), eq(1), isA(ClusterInvalidatingCacheManager.class),
                        eq("onInvalidation"));
                });
    }

    @Test
    void testWrapsCacheManagerForUppercaseBrokerValue() {
        contextRunner("bytechef.edition=ee", "bytechef.message-broker.provider=REDIS")
            .run(context -> assertThat(context.getBean(CacheManager.class))
                .isInstanceOf(ClusterInvalidatingCacheManager.class));
    }

    @Test
    void testLeavesCacheManagerWithMemoryBroker() {
        contextRunner("bytechef.edition=ee", "bytechef.message-broker.provider=memory")
            .run(context -> assertThat(context.getBean(CacheManager.class)).isInstanceOf(CaffeineCacheManager.class));
    }

    @Test
    void testLeavesCacheManagerWhenBrokerIsUnset() {
        contextRunner("bytechef.edition=ee")
            .run(context -> assertThat(context.getBean(CacheManager.class))
                .isNotInstanceOf(ClusterInvalidatingCacheManager.class));
    }

    @Test
    void testLeavesCacheManagerInCe() {
        contextRunner("bytechef.edition=ce", "bytechef.message-broker.provider=amqp")
            .run(context -> assertThat(context.getBean(CacheManager.class))
                .isNotInstanceOf(ClusterInvalidatingCacheManager.class));
    }

    @Test
    void testEvictPublishesOnCacheInvalidationRoute() {
        contextRunner("bytechef.edition=ee", "bytechef.message-broker.provider=amqp")
            .run(context -> {
                CacheManager cacheManager = context.getBean(CacheManager.class);

                Objects.requireNonNull(cacheManager.getCache("test"))
                    .evict("key");

                verify(messageBroker).send(eq(CACHE_INVALIDATION_EVENTS), any(CacheInvalidationEvent.class));
            });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRegistersListenerOnCacheInvalidationRoute() {
        contextRunner("bytechef.edition=ee", "bytechef.message-broker.provider=amqp")
            .run(context -> {
                MessageBrokerConfigurer<Object> messageBrokerConfigurer =
                    (MessageBrokerConfigurer<Object>) context.getBean("cacheInvalidationMessageBrokerConfigurer");
                MessageBrokerListenerRegistrar<Object> messageBrokerListenerRegistrar =
                    mock(MessageBrokerListenerRegistrar.class);

                messageBrokerConfigurer.configure(new Object(), messageBrokerListenerRegistrar);

                verify(messageBrokerListenerRegistrar).registerListenerEndpoint(
                    any(), eq(CACHE_INVALIDATION_EVENTS), eq(1), isA(ClusterInvalidatingCacheManager.class),
                    eq("onInvalidation"));
            });
    }

    private ApplicationContextRunner contextRunner(String... properties) {
        return new ApplicationContextRunner()
            .withUserConfiguration(CaffeineCacheManagerConfiguration.class, CacheInvalidationConfiguration.class)
            .withBean(MessageBroker.class, () -> messageBroker)
            .withPropertyValues(properties);
    }

    @Configuration
    static class CaffeineCacheManagerConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager();
        }
    }
}
