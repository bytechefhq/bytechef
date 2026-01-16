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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Feb, 21 2020
 */
public class InMemoryCounterRepository implements CounterRepository {

    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public void deleteById(Long id) {
        String key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        try {
            lock.lock();

            cache.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Long> findValueByIdForUpdate(Long id) {
        String key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        if (!lock.isHeldByCurrentThread()) {
            lock.lock();
        }

        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public void unlockForUpdate(Long id) {
        String key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = locks.get(key);

        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public long decrementAndGet(Long id) {
        String key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        try {
            lock.lock();

            Long current = cache.get(key);

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
        String key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        try {
            lock.lock();

            cache.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Counter save(Counter counter) {
        String key = TenantCacheKeyUtils.getKey(counter.getId());

        ReentrantLock lock = lockFor(key);

        try {
            lock.lock();

            cache.put(key, counter.getValue());
        } finally {
            lock.unlock();
        }

        return counter;
    }

    @Override
    public void update(Long id, long value) {
        String key = TenantCacheKeyUtils.getKey(id);

        ReentrantLock lock = lockFor(key);

        try {
            lock.lock();

            cache.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock lockFor(String key) {
        return locks.computeIfAbsent(key, (key1) -> new ReentrantLock());
    }
}
