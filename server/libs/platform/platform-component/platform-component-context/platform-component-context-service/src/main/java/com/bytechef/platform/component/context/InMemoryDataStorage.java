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

package com.bytechef.platform.component.context;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.ValueTagUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.exception.ComponentErrorType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * @author Ivica Cardic
 */
class InMemoryDataStorage {

    static final String CACHE = InMemoryDataStorage.class.getName() + ".dataStorage";

    private static final int LOCK_STRIPES = 64;
    private static final ReentrantLock[] LOCKS = IntStream.range(0, LOCK_STRIPES)
        .mapToObj(index -> new ReentrantLock())
        .toArray(ReentrantLock[]::new);

    private final CacheManager cacheManager;

    @Nullable
    private final String workflowUuid;

    InMemoryDataStorage(@Nullable String workflowUuid, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.workflowUuid = workflowUuid;
    }

    public void delete(String componentName, DataStorageScope scope, String scopeId, String key) {
        ReentrantLock lock = getLock(componentName, scope, scopeId);

        lock.lock();

        try {
            Map<String, Object> valueMap = getValueMap(componentName, scope, scopeId);

            if (valueMap.containsKey(key)) {
                valueMap.remove(key);

                putValueMap(componentName, scope, scopeId, valueMap);
            }
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetch(String componentName, DataStorageScope scope, String scopeId, String key) {
        Map<String, Object> valueMap = getValueMap(componentName, scope, scopeId);

        return Optional.ofNullable((T) valueMap.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String componentName, DataStorageScope scope, String scopeId, String key) {
        Map<String, Object> valueMap = getValueMap(componentName, scope, scopeId);

        return (T) valueMap.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAll(String componentName, DataStorageScope scope, String scopeId) {
        return (Map<String, T>) getValueMap(componentName, scope, scopeId);
    }

    public void put(String componentName, DataStorageScope scope, String scopeId, String key, Object value) {
        checkSize(value);

        ReentrantLock lock = getLock(componentName, scope, scopeId);

        lock.lock();

        try {
            Map<String, Object> valueMap = getValueMap(componentName, scope, scopeId);

            valueMap.put(key, value);

            putValueMap(componentName, scope, scopeId, valueMap);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T update(
        String componentName, DataStorageScope scope, String scopeId, String key, Function<Optional<T>, T> updater) {

        ReentrantLock lock = getLock(componentName, scope, scopeId);

        lock.lock();

        try {
            Map<String, Object> valueMap = getValueMap(componentName, scope, scopeId);

            T value = updater.apply(Optional.ofNullable((T) valueMap.get(key)));

            if (value == null) {
                throw new IllegalArgumentException("A data storage update must not return null");
            }

            checkSize(value);

            valueMap.put(key, value);

            putValueMap(componentName, scope, scopeId, valueMap);

            return value;
        } finally {
            lock.unlock();
        }
    }

    private void checkSize(Object value) {
        int size = getSizeInBytes(value);

        if (size > 409600) {
            throw new ConfigurationException(
                "Value size exceeds 400KB limit per key. Actual: " + size + " bytes",
                ComponentErrorType.DATA_STORAGE_SIZE_EXCEEDED);
        }
    }

    private Cache getCache() {
        return Objects.requireNonNull(cacheManager.getCache(CACHE), CACHE);
    }

    private String getCacheKey(String componentName, DataStorageScope scope, String scopeId) {
        return TenantCacheKeyUtils.getKey(workflowUuid, componentName, scope, scopeId);
    }

    private ReentrantLock getLock(String componentName, DataStorageScope scope, String scopeId) {
        String cacheKey = getCacheKey(componentName, scope, scopeId);

        return LOCKS[Math.floorMod(cacheKey.hashCode(), LOCK_STRIPES)];
    }

    private int getSizeInBytes(Object value) {
        if (value instanceof byte[] bytes) {
            return bytes.length;
        }

        if (value instanceof String string) {
            return string.getBytes(StandardCharsets.UTF_8).length;
        }

        return JsonUtils.writeValueAsBytes(value).length;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getValueMap(String componentName, DataStorageScope scope, String scopeId) {
        Cache cache = getCache();

        Map<String, ?> taggedValueMap = cache.get(getCacheKey(componentName, scope, scopeId), Map.class);

        if (taggedValueMap == null) {
            return new HashMap<>();
        }

        return new HashMap<>(ValueTagUtils.untagMap(taggedValueMap));
    }

    private void putValueMap(
        String componentName, DataStorageScope scope, String scopeId, Map<String, Object> valueMap) {

        Cache cache = getCache();

        cache.put(getCacheKey(componentName, scope, scopeId), ValueTagUtils.tag(valueMap));
    }
}
