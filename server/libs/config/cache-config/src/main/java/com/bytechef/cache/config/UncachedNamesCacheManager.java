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

package com.bytechef.cache.config;

import java.util.Collection;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;

/**
 * @author Ivica Cardic
 */
class UncachedNamesCacheManager implements CacheManager {

    private final CacheManager delegateCacheManager;
    private final Set<String> uncachedCacheNames;

    UncachedNamesCacheManager(CacheManager delegateCacheManager, Set<String> uncachedCacheNames) {
        this.delegateCacheManager = delegateCacheManager;
        this.uncachedCacheNames = Set.copyOf(uncachedCacheNames);
    }

    @Override
    public @Nullable Cache getCache(String name) {
        if (uncachedCacheNames.contains(name)) {
            return new NoOpCache(name);
        }

        return delegateCacheManager.getCache(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegateCacheManager.getCacheNames();
    }
}
