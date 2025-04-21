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
import java.util.Objects;
import java.util.Optional;
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

    public InMemoryCounterRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void deleteById(Long id) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

        cache.evict(TenantCacheKeyUtils.getKey(id));
    }

    @Override
    public Optional<Long> findValueByIdForUpdate(Long id) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

        return Optional.ofNullable(cache.get(TenantCacheKeyUtils.getKey(id), Long.class));
    }

    @Override
    public Counter save(Counter counter) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

        cache.put(TenantCacheKeyUtils.getKey(counter.getId()), counter.getValue());

        return counter;
    }

    @Override
    public void update(Long id, long value) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

        cache.put(TenantCacheKeyUtils.getKey(id), value);
    }
}
