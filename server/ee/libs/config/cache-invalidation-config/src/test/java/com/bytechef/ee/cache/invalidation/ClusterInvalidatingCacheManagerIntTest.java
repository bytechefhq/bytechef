/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation;

import static com.bytechef.ee.cache.invalidation.CacheInvalidationMessageRoute.CACHE_INVALIDATION_EVENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.message.broker.redis.RedisMessageBroker;
import com.bytechef.message.broker.redis.control.RedisControlChannel;
import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.broker.redis.serializer.RedisMessageSerializer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
class ClusterInvalidatingCacheManagerIntTest {

    @Nested
    class LocalPublisher {

        private List<CacheInvalidationEvent> nodeOnePublished;
        private ClusterInvalidatingCacheManager nodeOneCacheManager;
        private ClusterInvalidatingCacheManager nodeTwoCacheManager;

        @BeforeEach
        void beforeEach() {
            nodeOnePublished = new ArrayList<>();

            nodeOneCacheManager =
                new ClusterInvalidatingCacheManager(new CaffeineCacheManager(), nodeOnePublished::add);
            nodeTwoCacheManager =
                new ClusterInvalidatingCacheManager(new CaffeineCacheManager(), cacheInvalidationEvent -> {});
        }

        @AfterEach
        void afterEach() {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        void testPutStaysLocal() {
            getCache(nodeOneCacheManager).put("key", "value");

            assertThat(nodeOnePublished).isEmpty();
            assertThat(getCache(nodeOneCacheManager).get("key", String.class)).isEqualTo("value");
        }

        @Test
        void testEvictOnOneNodeEvictsTheKeyOnTheOtherNode() {
            getCache(nodeOneCacheManager).put("key", "value");
            getCache(nodeTwoCacheManager).put("key", "stale");
            getCache(nodeTwoCacheManager).put("other", "kept");

            getCache(nodeOneCacheManager).evict("key");

            nodeOnePublished.forEach(nodeTwoCacheManager::onInvalidation);

            assertThat(getCache(nodeOneCacheManager).get("key")).isNull();
            assertThat(getCache(nodeTwoCacheManager).get("key")).isNull();
            assertThat(getCache(nodeTwoCacheManager).get("other", String.class)).isEqualTo("kept");
        }

        @Test
        void testClearOnOneNodeClearsTheCacheOnTheOtherNode() {
            getCache(nodeTwoCacheManager).put("key", "stale");

            getCache(nodeOneCacheManager).clear();

            nodeOnePublished.forEach(nodeTwoCacheManager::onInvalidation);

            assertThat(getCache(nodeTwoCacheManager).get("key")).isNull();
        }

        @Test
        void testEvictOfNonStringKeyClearsTheCacheOnTheOtherNode() {
            getCache(nodeTwoCacheManager).put(42L, "stale");

            getCache(nodeOneCacheManager).evict(42L);

            nodeOnePublished.forEach(nodeTwoCacheManager::onInvalidation);

            assertThat(getCache(nodeTwoCacheManager).get(42L)).isNull();
        }

        @Test
        void testOwnInvalidationIsIgnored() {
            getCache(nodeOneCacheManager).evict("key");

            getCache(nodeOneCacheManager).put("key", "value");

            nodeOnePublished.forEach(nodeOneCacheManager::onInvalidation);

            assertThat(getCache(nodeOneCacheManager).get("key", String.class)).isEqualTo("value");
        }

        @Test
        void testValueLoaderStaysLocalAndKeepsTheSameInstance() {
            Cache cache = getCache(nodeOneCacheManager);

            ReentrantLock reentrantLock = cache.get("lock", ReentrantLock::new);
            Object cachedReentrantLock = cache.get("lock", ReentrantLock::new);

            assertThat(cachedReentrantLock).isSameAs(reentrantLock);
            assertThat(nodeOnePublished).isEmpty();
        }

        @Test
        void testEvictInsideTransactionIsPublishedAfterCommit() {
            TransactionSynchronizationManager.initSynchronization();

            getCache(nodeOneCacheManager).evict("key");

            assertThat(nodeOnePublished).isEmpty();

            for (TransactionSynchronization transactionSynchronization : TransactionSynchronizationManager
                .getSynchronizations()) {

                transactionSynchronization.afterCommit();
            }

            assertThat(nodeOnePublished).hasSize(1);
        }

        @Test
        void testEvictInsideRolledBackTransactionIsNotPublished() {
            TransactionSynchronizationManager.initSynchronization();

            getCache(nodeOneCacheManager).evict("key");

            for (TransactionSynchronization transactionSynchronization : TransactionSynchronizationManager
                .getSynchronizations()) {

                transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }

            assertThat(nodeOnePublished).isEmpty();
        }

        @Test
        void testEvictAppliesLocallyWhenPublishingFails() {
            ClusterInvalidatingCacheManager failingCacheManager = new ClusterInvalidatingCacheManager(
                new CaffeineCacheManager(), cacheInvalidationEvent -> {
                    throw new IllegalStateException("broker down");
                });

            Cache cache = getCache(failingCacheManager);

            cache.put("key", "value");

            cache.evict("key");

            assertThat(cache.get("key")).isNull();
        }

        private static Cache getCache(ClusterInvalidatingCacheManager cacheManager) {
            return Objects.requireNonNull(cacheManager.getCache("test"));
        }
    }

    @Nested
    class RedisBroker {

        @Container
        private static final GenericContainer<?> redis =
            new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

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
}
