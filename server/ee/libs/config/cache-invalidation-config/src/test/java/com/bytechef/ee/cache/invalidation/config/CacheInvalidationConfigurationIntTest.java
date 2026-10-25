/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation.config;

import static com.bytechef.ee.cache.invalidation.CacheInvalidationMessageRoute.CACHE_INVALIDATION_EVENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.ee.cache.invalidation.ClusterInvalidatingCacheManager;
import com.bytechef.message.broker.redis.RedisMessageBroker;
import com.bytechef.message.broker.redis.control.RedisControlChannel;
import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.broker.redis.serializer.RedisMessageSerializer;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Testcontainers
class CacheInvalidationConfigurationIntTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .build();
    private LettuceConnectionFactory lettuceConnectionFactory;
    private ClusterInvalidatingCacheManager nodeACacheManager;
    private ClusterInvalidatingCacheManager nodeBCacheManager;
    private RedisControlChannel nodeBControlChannel;

    @BeforeEach
    void beforeEach() {
        lettuceConnectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));

        lettuceConnectionFactory.afterPropertiesSet();
        lettuceConnectionFactory.start();

        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(lettuceConnectionFactory);

        stringRedisTemplate.afterPropertiesSet();

        RedisMessageBroker redisMessageBroker = new RedisMessageBroker(
            new RedisMessageSerializer(objectMapper), stringRedisTemplate);

        nodeACacheManager = new ClusterInvalidatingCacheManager(
            new CaffeineCacheManager(),
            cacheInvalidationEvent -> redisMessageBroker.send(CACHE_INVALIDATION_EVENTS, cacheInvalidationEvent));
        nodeBCacheManager = new ClusterInvalidatingCacheManager(new CaffeineCacheManager(), event -> {});

        nodeBControlChannel = new RedisControlChannel(
            lettuceConnectionFactory, new RedisMessageDeserializer(objectMapper));

        nodeBControlChannel.register(CACHE_INVALIDATION_EVENTS, nodeBCacheManager, "onInvalidation");
        nodeBControlChannel.start();
    }

    @AfterEach
    void afterEach() {
        nodeBControlChannel.stop();

        lettuceConnectionFactory.destroy();
    }

    @Test
    void testEvictOnNodeAReachesNodeB() {
        Cache nodeACache = Objects.requireNonNull(nodeACacheManager.getCache("test"));
        Cache nodeBCache = Objects.requireNonNull(nodeBCacheManager.getCache("test"));

        nodeACache.put("key", "value");
        nodeBCache.put("key", "value");

        await().atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> {
                nodeACache.evict("key");

                assertThat(nodeBCache.get("key")).isNull();
            });
    }
}
