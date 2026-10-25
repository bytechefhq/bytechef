/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cache.invalidation;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Keeps caches local to each node and publishes every eviction to the other nodes of a cluster. Puts stay local: a node
 * that fills its cache after a miss must not evict the copies the other nodes hold, and values such as locks only make
 * sense inside one JVM. A write that replaces a cached value must evict it first.
 *
 * @author Ivica Cardic
 */
public class ClusterInvalidatingCacheManager implements CacheManager {

    private final Map<String, Cache> caches = new ConcurrentHashMap<>();
    private final CacheManager delegateCacheManager;
    private final Consumer<CacheInvalidation> invalidationPublisher;
    private final String nodeId = UUID.randomUUID()
        .toString();

    public ClusterInvalidatingCacheManager(
        CacheManager delegateCacheManager, Consumer<CacheInvalidation> invalidationPublisher) {

        this.delegateCacheManager = delegateCacheManager;
        this.invalidationPublisher = invalidationPublisher;
    }

    @Override
    public @Nullable Cache getCache(String name) {
        Cache cache = delegateCacheManager.getCache(name);

        if (cache == null) {
            return null;
        }

        return caches.computeIfAbsent(name, cacheName -> new ClusterInvalidatingCache(cache));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegateCacheManager.getCacheNames();
    }

    public void onInvalidation(CacheInvalidation cacheInvalidation) {
        if (nodeId.equals(cacheInvalidation.nodeId())) {
            return;
        }

        Cache cache = delegateCacheManager.getCache(cacheInvalidation.cacheName());

        if (cache == null) {
            return;
        }

        String key = cacheInvalidation.key();

        if (key == null) {
            cache.clear();
        } else {
            cache.evict(key);
        }
    }

    private void publish(String cacheName, @Nullable Object key) {
        CacheInvalidation cacheInvalidation = new CacheInvalidation(
            nodeId, cacheName, key instanceof String stringKey ? stringKey : null);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    invalidationPublisher.accept(cacheInvalidation);
                }
            });
        } else {
            invalidationPublisher.accept(cacheInvalidation);
        }
    }

    private class ClusterInvalidatingCache implements Cache {

        private final Cache cache;

        private ClusterInvalidatingCache(Cache cache) {
            this.cache = cache;
        }

        @Override
        public String getName() {
            return cache.getName();
        }

        @Override
        public Object getNativeCache() {
            return cache.getNativeCache();
        }

        @Override
        public @Nullable ValueWrapper get(Object key) {
            return cache.get(key);
        }

        @Override
        public <T> @Nullable T get(Object key, @Nullable Class<T> type) {
            return cache.get(key, type);
        }

        @Override
        public <T> @Nullable T get(Object key, Callable<T> valueLoader) {
            return cache.get(key, valueLoader);
        }

        @Override
        public @Nullable CompletableFuture<?> retrieve(Object key) {
            return cache.retrieve(key);
        }

        @Override
        public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
            return cache.retrieve(key, valueLoader);
        }

        @Override
        public void put(Object key, @Nullable Object value) {
            cache.put(key, value);
        }

        @Override
        public @Nullable ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
            return cache.putIfAbsent(key, value);
        }

        @Override
        public void evict(Object key) {
            cache.evict(key);

            publish(getName(), key);
        }

        @Override
        public boolean evictIfPresent(Object key) {
            boolean present = cache.evictIfPresent(key);

            publish(getName(), key);

            return present;
        }

        @Override
        public void clear() {
            cache.clear();

            publish(getName(), null);
        }

        @Override
        public boolean invalidate() {
            boolean present = cache.invalidate();

            publish(getName(), null);

            return present;
        }
    }
}
