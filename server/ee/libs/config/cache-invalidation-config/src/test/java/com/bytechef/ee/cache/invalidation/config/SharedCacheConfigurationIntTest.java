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

import com.bytechef.cache.SharedCacheSupport;
import com.bytechef.cache.SharedCaches;
import com.bytechef.ee.cache.invalidation.CacheInvalidationEvent;
import com.bytechef.ee.cache.invalidation.ClusterInvalidatingCacheManager;
import com.bytechef.ee.cache.invalidation.SharedCacheRoutingCacheManager;
import com.bytechef.ee.cache.invalidation.boot.CacheProviderEnvironmentPostProcessor;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
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
class SharedCacheConfigurationIntTest {

    private static final String LOCAL_CACHE = "local";
    private static final String REDIS_UNAVAILABLE_MESSAGE =
        "bytechef.cache.provider=redis needs a reachable Redis; check spring.data.redis.*";
    private static final String SHARED_CACHE = "shared";

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private final MessageBroker messageBroker = mock(MessageBroker.class);

    @Test
    void testRoutesSharedCachesToRedisWithAmqpBroker() {
        redisContextRunner("bytechef.message-broker.provider=AMQP")
            .run(context -> {
                CacheManager cacheManager = context.getBean(CacheManager.class);

                assertThat(cacheManager).isInstanceOf(SharedCacheRoutingCacheManager.class);
                assertThat(((SharedCacheRoutingCacheManager) cacheManager).getLocalCacheManager())
                    .isInstanceOf(ClusterInvalidatingCacheManager.class);
                assertThat(cacheManager.getCache(SHARED_CACHE)).isInstanceOf(RedisCache.class);
                assertThat(cacheManager.getCache(LOCAL_CACHE)).isNotInstanceOf(RedisCache.class);
                assertThat(context).hasSingleBean(SharedCacheSupport.class);
            });
    }

    @Test
    void testRoutesSharedCachesToRedisWithMemoryBroker() {
        redisContextRunner("bytechef.message-broker.provider=MEMORY")
            .run(context -> {
                CacheManager cacheManager = context.getBean(CacheManager.class);

                assertThat(cacheManager).isInstanceOf(SharedCacheRoutingCacheManager.class);
                assertThat(((SharedCacheRoutingCacheManager) cacheManager).getLocalCacheManager())
                    .isInstanceOf(CaffeineCacheManager.class);
                assertThat(cacheManager.getCache(SHARED_CACHE)).isInstanceOf(RedisCache.class);
            });
    }

    @Test
    void testRoutesSharedCachesToRedisOnAWebhookNode() {
        redisContextRunner(
            "bytechef.message-broker.provider=AMQP", "bytechef.coordinator.enabled=false",
            "bytechef.worker.enabled=false")
                .run(context -> assertThat(getCache(context, SHARED_CACHE)).isInstanceOf(RedisCache.class));
    }

    @Test
    void testRoutesSharedCachesToRedisWithUpperCaseProvider() {
        redisContextRunner("bytechef.message-broker.provider=AMQP", "bytechef.cache.provider=REDIS")
            .run(context -> assertThat(getCache(context, SHARED_CACHE)).isInstanceOf(RedisCache.class));
    }

    @Test
    void testSharedCacheEntryWrittenOnOneNodeIsReadOnTheOther() {
        redisContextRunner("bytechef.message-broker.provider=AMQP")
            .run(nodeOneContext -> redisContextRunner("bytechef.message-broker.provider=AMQP")
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
    void testSharedCacheReadsTheValueJustWritten() {
        redisContextRunner("bytechef.message-broker.provider=AMQP")
            .run(context -> {
                Cache cache = getCache(context, SHARED_CACHE);

                for (int count = 1; count <= 200; count++) {
                    Integer previousCount = cache.get("count", Integer.class);

                    cache.put("count", previousCount == null ? 1 : previousCount + 1);

                    assertThat(cache.get("count", Integer.class)).isEqualTo(count);
                }
            });
    }

    @Test
    void testSharedCacheKeepsNullValues() {
        redisContextRunner("bytechef.message-broker.provider=AMQP")
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
        redisContextRunner("bytechef.message-broker.provider=AMQP")
            .run(nodeOneContext -> redisContextRunner("bytechef.message-broker.provider=AMQP")
                .run(nodeTwoContext -> {
                    getCache(nodeOneContext, LOCAL_CACHE).put("key", "value");

                    assertThat(getCache(nodeTwoContext, LOCAL_CACHE).get("key")).isNull();
                }));
    }

    @Test
    void testSharedCacheEvictionIsNotPublished() {
        redisContextRunner("bytechef.message-broker.provider=AMQP")
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
        redisContextRunner("bytechef.message-broker.provider=AMQP")
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
    void testFailsWhenRedisIsUnreachable() {
        contextRunner(
            "bytechef.message-broker.provider=AMQP", "spring.data.redis.host=localhost",
            "spring.data.redis.port=1", "spring.data.redis.timeout=1s",
            "spring.data.redis.connect-timeout=1s")
                .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class))
                .run(context -> assertThat(context).getFailure()
                    .hasStackTraceContaining(REDIS_UNAVAILABLE_MESSAGE));
    }

    @Test
    void testFailsWithoutRedisConnectionFactory() {
        contextRunner("bytechef.message-broker.provider=AMQP")
            .run(context -> assertThat(context).getFailure()
                .hasStackTraceContaining(REDIS_UNAVAILABLE_MESSAGE));
    }

    private ApplicationContextRunner contextRunner(String... properties) {
        return new ApplicationContextRunner()
            .withInitializer(context -> new CacheProviderEnvironmentPostProcessor()
                .postProcessEnvironment(context.getEnvironment(), new SpringApplication()))
            .withUserConfiguration(
                CaffeineCacheManagerConfiguration.class, SharedCacheConfiguration.class,
                CacheInvalidationConfiguration.class)
            .withBean(MessageBroker.class, () -> messageBroker)
            .withBean("sharedCaches", SharedCaches.class, () -> SharedCaches.of(SHARED_CACHE))
            .withPropertyValues("bytechef.edition=EE", "bytechef.cache.provider=redis")
            .withPropertyValues(properties);
    }

    private ApplicationContextRunner redisContextRunner(String... properties) {
        return contextRunner(properties)
            .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class))
            .withPropertyValues(
                "spring.data.redis.host=" + redis.getHost(), "spring.data.redis.port=" + redis.getMappedPort(6379));
    }

    private static Cache getCache(AssertableApplicationContext context, String name) {
        CacheManager cacheManager = context.getBean(CacheManager.class);

        return Objects.requireNonNull(cacheManager.getCache(name));
    }

    @Configuration
    static class CaffeineCacheManagerConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager();
        }
    }
}
