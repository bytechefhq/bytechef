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
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * @author Ivica Cardic
 */
class InMemoryDataStorage {

    private static final String CACHE = InMemoryDataStorage.class.getName() + ".dataStorage";

    private final CacheManager cacheManager;
    @Nullable
    private final String workflowUuid;

    InMemoryDataStorage(@Nullable String workflowUuid, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.workflowUuid = workflowUuid;
    }

    public void delete(String componentName, DataStorageScope scope, String scopeId, String key) {
        Map<String, Object> map = getValueMap(componentName, scope, scopeId);

        map.remove(key);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetch(String componentName, DataStorageScope scope, String scopeId, String key) {
        Map<String, Object> map = getValueMap(componentName, scope, scopeId);

        return Optional.ofNullable((T) map.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String componentName, DataStorageScope scope, String scopeId, String key) {
        Map<String, Object> map = getValueMap(componentName, scope, scopeId);

        return (T) map.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAll(String componentName, DataStorageScope scope, String scopeId) {
        return (Map<String, T>) getValueMap(componentName, scope, scopeId);
    }

    public void put(String componentName, DataStorageScope scope, String scopeId, String key, Object value) {
        int size = getSizeInBytes(value);

        if (size > 409600) {
            throw new IllegalArgumentException("Value size exceeds 400KB limit per key. Actual: " + size + " bytes)");
        }

        Map<String, Object> map = getValueMap(componentName, scope, scopeId);

        map.put(key, value);
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

    private Map<String, Object> getValueMap(
        String componentName, DataStorageScope scope, String scopeId) {

        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE), CACHE);

        return Objects.requireNonNull(
            cache.get(TenantCacheKeyUtils.getKey(workflowUuid, componentName, scope, scopeId), HashMap::new));
    }
}
