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

import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
class InMemoryDataStorage implements DataStorage {

    private static final String CACHE = InMemoryDataStorage.class.getName() + ".dataStorage";

    private final CacheManager cacheManager;
    private final String workflowUuid;

    InMemoryDataStorage(String workflowUuid, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.workflowUuid = workflowUuid;
    }

    @Override
    public void delete(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull ModeType type) {

        Map<String, Object> map = getValueMap(componentName, scope, scopeId, type);

        map.remove(key);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetch(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull ModeType type) {

        Map<String, Object> map = getValueMap(componentName, scope, scopeId, type);

        return Optional.ofNullable((T) map.get(key));
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId, @NonNull String key,
        @NonNull ModeType type) {

        Map<String, Object> map = getValueMap(componentName, scope, scopeId, type);

        return (T) map.get(key);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAll(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
        @NonNull ModeType type) {

        return (Map<String, T>) getValueMap(componentName, scope, scopeId, type);
    }

    @Override
    public void put(
        @NonNull String componentName, @NonNull DataStorageScope scope, @NonNull String scopeId,
        @NonNull String key, @NonNull ModeType type, @NonNull Object value) {

        Map<String, Object> map = getValueMap(componentName, scope, scopeId, type);

        map.put(key, value);
    }

    private Map<String, Object> getValueMap(
        String componentName, DataStorageScope scope, String scopeId, ModeType type) {

        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE), CACHE);

        return cache.get(
            TenantCacheKeyUtils.getKey(workflowUuid, componentName, scope, scopeId, type), HashMap::new);
    }
}
