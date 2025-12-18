/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.execution.repository.memory;

import com.bytechef.atlas.execution.domain.Counter;
import com.bytechef.atlas.execution.repository.CounterRepository;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Feb, 21 2020
 */
public class InMemoryCounterRepository implements CounterRepository {

    private static final String CACHE = InMemoryCounterRepository.class.getName() + ".counter";

    private final CacheManager cacheManager;

    private final com.github.benmanes.caffeine.cache.Cache<Object, ReentrantLock> locks =
        Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public InMemoryCounterRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void deleteById(Long id) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

        Object key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        boolean needAcquire = !lock.isHeldByCurrentThread();

        if (needAcquire) {
            lock.lock();
        }

        try {
            cache.evict(key);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Optional<Long> findValueByIdForUpdate(Long id) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));
        Object key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);
        boolean acquiredHere = false;

        if (!lock.isHeldByCurrentThread()) {
            lock.lock();
            acquiredHere = true;
        }

        Long value = cache.get(key, Long.class);

        if (value == null && acquiredHere && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }

        return Optional.ofNullable(value);
    }

    @Override
    public void unlockForUpdate(Long id) {
        Object key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public long decrementAndGet(Long id) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));
        Object key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        lock.lock();

        try {
            Long current = cache.get(key, Long.class);

            if (current == null) {
                throw new IllegalArgumentException("Unable to locate counter with id: %s".formatted(id));
            }

            long next = current - 1;

            cache.put(key, next);

            return next;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setAtomic(Long id, long value) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));
        Object key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        lock.lock();

        try {
            cache.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Counter save(Counter counter) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

        Object key = TenantCacheKeyUtils.getKey(counter.getId());

        ReentrantLock lock = lockFor(key);

        boolean needAcquire = !lock.isHeldByCurrentThread();

        if (needAcquire) {
            lock.lock();
        }

        try {
            cache.put(key, counter.getValue());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return counter;
    }

    @Override
    public void update(Long id, long value) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

        Object key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        boolean needAcquire = !lock.isHeldByCurrentThread();

        if (needAcquire) {
            lock.lock();
        }

        try {
            cache.put(key, value);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private ReentrantLock lockFor(Object key) {
        return locks.get(key, k -> new ReentrantLock());
    }
}
