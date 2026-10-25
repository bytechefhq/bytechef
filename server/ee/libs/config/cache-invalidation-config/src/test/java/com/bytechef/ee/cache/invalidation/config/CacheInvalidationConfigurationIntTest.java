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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.cache.SharedCaches;
import com.bytechef.ee.cache.invalidation.CacheInvalidationEvent;
import com.bytechef.ee.cache.invalidation.ClusterInvalidatingCacheManager;
import com.bytechef.ee.cache.invalidation.SharedCacheRoutingCacheManager;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Testcontainers
class CacheInvalidationConfigurationIntTest {

    private static final String LOCAL_CACHE = "local";
    private static final String SHARED_CACHE = "shared";

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private final MessageBroker messageBroker = mock(MessageBroker.class);

    @Test
    void testWrapsCacheManagerInEeWithAmqpBroker() {
        contextRunner("bytechef.edition=ee", "bytechef.message-broker.provider=amqp")
            .run(context -> assertThat(context.getBean(CacheManager.class))
                .isInstanceOf(ClusterInvalidatingCacheManager.class));
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
    void testWrapsCacheManagerWithAwsBrokerAndRedisControlProvider() {
        contextRunner(
            "bytechef.edition=ee", "bytechef.message-broker.provider=aws",
            "bytechef.message-broker.aws.control-provider=redis")
                .run(context -> assertThat(context.getBean(CacheManager.class))
                    .isInstanceOf(ClusterInvalidatingCacheManager.class));
    }

    @Test
    void testLeavesCacheManagerWithAwsBrokerAndSqsControlProvider() {
        contextRunner(
            "bytechef.edition=ee", "bytechef.message-broker.provider=aws",
            "bytechef.message-broker.aws.control-provider=sqs")
                .run(context -> assertThat(context.getBean(CacheManager.class))
                    .isInstanceOf(CaffeineCacheManager.class));
    }

    @Test
    void testLeavesCacheManagerWithAwsBrokerWhenControlProviderIsUnset() {
        contextRunner("bytechef.edition=ee", "bytechef.message-broker.provider=aws")
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

    @Nested
    class WithRedis {

        @Test
        void testRoutesSharedCachesToRedisOnAWebhookNode() {
            redisContextRunner()
                .withPropertyValues("bytechef.coordinator.enabled=false", "bytechef.worker.enabled=false")
                .run(context -> {
                    CacheManager cacheManager = context.getBean(CacheManager.class);

                    assertThat(cacheManager).isInstanceOf(SharedCacheRoutingCacheManager.class);
                    assertThat(cacheManager.getCache(SHARED_CACHE)).isInstanceOf(RedisCache.class);
                    assertThat(cacheManager.getCache(LOCAL_CACHE)).isNotInstanceOf(RedisCache.class);
                });
        }

        @Test
        void testRoutesSharedCachesToRedis() {
            redisContextRunner()
                .run(context -> {
                    CacheManager cacheManager = context.getBean(CacheManager.class);

                    assertThat(cacheManager).isInstanceOf(SharedCacheRoutingCacheManager.class);
                    assertThat(cacheManager.getCache(SHARED_CACHE)).isInstanceOf(RedisCache.class);
                    assertThat(cacheManager.getCache(LOCAL_CACHE)).isNotInstanceOf(RedisCache.class);
                });
        }

        @Test
        void testSharedCacheEntryWrittenOnOneNodeIsReadOnTheOther() {
            redisContextRunner()
                .run(nodeOneContext -> redisContextRunner()
                    .run(nodeTwoContext -> {
                        getCache(nodeOneContext, SHARED_CACHE).put("enabled", true);
                        getCache(nodeOneContext, SHARED_CACHE).put("output", Map.of("id", "webhook-1", "count", 2));

                        assertThat(getCache(nodeTwoContext, SHARED_CACHE).get("enabled", Boolean.class)).isTrue();
                        assertThat(getCache(nodeTwoContext, SHARED_CACHE).get("output"))
                            .isNotNull()
                            .extracting(Cache.ValueWrapper::get)
                            .isEqualTo(Map.of("id", "webhook-1", "count", 2));

                        getCache(nodeTwoContext, SHARED_CACHE).evict("enabled");

                        assertThat(getCache(nodeOneContext, SHARED_CACHE).get("enabled")).isNull();
                    }));
        }

        @Test
        void testSharedCacheKeepsNullValues() {
            redisContextRunner()
                .run(context -> {
                    getCache(context, SHARED_CACHE).put("missing", null);

                    assertThat(getCache(context, SHARED_CACHE).get("missing"))
                        .isNotNull()
                        .extracting(Cache.ValueWrapper::get)
                        .isNull();
                });
        }

        @Test
        void testLocalCacheEntryStaysOnItsNode() {
            redisContextRunner()
                .run(nodeOneContext -> redisContextRunner()
                    .run(nodeTwoContext -> {
                        getCache(nodeOneContext, LOCAL_CACHE).put("key", "value");

                        assertThat(getCache(nodeTwoContext, LOCAL_CACHE).get("key")).isNull();
                    }));
        }

        @Test
        void testSharedCacheEvictionIsNotPublished() {
            redisContextRunner()
                .run(context -> {
                    getCache(context, SHARED_CACHE).evict("key");

                    verify(messageBroker, never()).send(any(), any());

                    getCache(context, LOCAL_CACHE).evict("key");

                    verify(messageBroker).send(eq(CACHE_INVALIDATION_EVENTS), any(CacheInvalidationEvent.class));
                });
        }

        @Test
        @SuppressWarnings("unchecked")
        void testRegistersListenerOnTheLocalCacheManager() {
            redisContextRunner()
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

        private ApplicationContextRunner redisContextRunner() {
            return contextRunner("bytechef.edition=ee", "bytechef.message-broker.provider=redis")
                .withBean(
                    RedisConnectionFactory.class,
                    () -> new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379)))
                .withBean("sharedCaches", SharedCaches.class, () -> SharedCaches.of(SHARED_CACHE));
        }

        private static Cache getCache(AssertableApplicationContext context, String name) {
            CacheManager cacheManager = context.getBean(CacheManager.class);

            return Objects.requireNonNull(cacheManager.getCache(name));
        }
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
