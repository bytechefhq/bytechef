/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ClusterInvalidatingCacheManagerTest {

    private List<CacheInvalidationEvent> nodeOnePublished;
    private ClusterInvalidatingCacheManager nodeOneCacheManager;
    private ClusterInvalidatingCacheManager nodeTwoCacheManager;

    @BeforeEach
    void beforeEach() {
        nodeOnePublished = new ArrayList<>();

        nodeOneCacheManager = new ClusterInvalidatingCacheManager(new CaffeineCacheManager(), nodeOnePublished::add);
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
