/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.definition;

import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Ivica Cardic
 */
class InMemoryDataStorage implements DataStorage {

    private final String workflowReference;

    private static final Cache<String, Map<String, Object>> VALUES =
        Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(100000)
            .build();

    InMemoryDataStorage(String workflowReference) {
        this.workflowReference = workflowReference;
    }

    @Override
    public void delete(String componentName, DataStorageScope scope, String scopeId, String key, AppType type) {
        Map<String, Object> map = getValueMap(componentName, scope, scopeId, type);

        map.remove(key);
    }

    @Override
    public <T> Optional<T> fetch(
        String componentName, DataStorageScope scope, String scopeId, String key, AppType type) {

        Map<String, Object> map = getValueMap(componentName, scope, scopeId, type);

        return Optional.ofNullable((T) map.get(key));
    }

    @Override
    public <T> T get(String componentName, DataStorageScope scope, String scopeId, String key, AppType type) {
        Map<String, Object> map = getValueMap(componentName, scope, scopeId, type);

        return (T) map.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAll(String componentName, DataStorageScope scope, String scopeId, AppType type) {
        return (Map<String, T>) getValueMap(componentName, scope, scopeId, type);
    }

    @Override
    public void put(
        String componentName, DataStorageScope scope, String scopeId, String key, AppType type, Object value) {

        Map<String, Object> map = getValueMap(componentName, scope, scopeId, type);

        map.put(key, value);
    }

    private String getValueKey(String componentName, DataStorageScope scope, String scopeId, AppType type) {
        return workflowReference + "_" + componentName + "_" + scope + "_" + scopeId + "_" + type;
    }

    private Map<String, Object> getValueMap(
        String componentName, DataStorageScope scope, String scopeId, AppType type) {

        return VALUES.get(getValueKey(componentName, scope, scopeId, type), s -> new HashMap<>());
    }
}
