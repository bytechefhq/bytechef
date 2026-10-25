/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.function.SingletonSupplier;

/**
 * Serves the caches named by {@link com.bytechef.cache.SharedCaches} from a cache manager every node shares and all
 * other caches from the node-local cache manager.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SharedCacheRoutingCacheManager implements CacheManager {

    private final CacheManager localCacheManager;
    private final CacheManager sharedCacheManager;
    private final SingletonSupplier<Set<String>> sharedCacheNamesSupplier;

    public SharedCacheRoutingCacheManager(
        CacheManager localCacheManager, CacheManager sharedCacheManager,
        Supplier<Set<String>> sharedCacheNamesSupplier) {

        this.localCacheManager = localCacheManager;
        this.sharedCacheManager = sharedCacheManager;
        this.sharedCacheNamesSupplier = SingletonSupplier.of(sharedCacheNamesSupplier);
    }

    @Override
    public @Nullable Cache getCache(String name) {
        Set<String> sharedCacheNames = sharedCacheNamesSupplier.obtain();

        if (sharedCacheNames.contains(name)) {
            return sharedCacheManager.getCache(name);
        }

        return localCacheManager.getCache(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> cacheNames = new LinkedHashSet<>(localCacheManager.getCacheNames());

        cacheNames.addAll(sharedCacheManager.getCacheNames());

        return cacheNames;
    }

    public CacheManager getLocalCacheManager() {
        return localCacheManager;
    }
}
