/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SharedCacheRoutingCacheManagerTest {

    private final CacheManager localCacheManager = new ConcurrentMapCacheManager();
    private final CacheManager sharedCacheManager = new ConcurrentMapCacheManager();

    @Test
    void testRoutesByCacheName() {
        SharedCacheRoutingCacheManager sharedCacheRoutingCacheManager = new SharedCacheRoutingCacheManager(
            localCacheManager, sharedCacheManager, () -> Set.of("shared"));

        Objects.requireNonNull(sharedCacheRoutingCacheManager.getCache("shared"))
            .put("key", "shared value");
        Objects.requireNonNull(sharedCacheRoutingCacheManager.getCache("local"))
            .put("key", "local value");

        assertThat(Objects.requireNonNull(sharedCacheManager.getCache("shared"))
            .get("key", String.class)).isEqualTo("shared value");
        assertThat(Objects.requireNonNull(localCacheManager.getCache("local"))
            .get("key", String.class)).isEqualTo("local value");
        assertThat(sharedCacheRoutingCacheManager.getCacheNames()).containsExactlyInAnyOrder("shared", "local");
    }

    @Test
    void testResolvesSharedCacheNamesOnce() {
        AtomicInteger resolutions = new AtomicInteger();

        SharedCacheRoutingCacheManager sharedCacheRoutingCacheManager = new SharedCacheRoutingCacheManager(
            localCacheManager, sharedCacheManager, () -> {
                resolutions.incrementAndGet();

                return Set.of("shared");
            });

        sharedCacheRoutingCacheManager.getCache("shared");
        sharedCacheRoutingCacheManager.getCache("local");

        assertThat(resolutions).hasValue(1);
        assertThat(sharedCacheRoutingCacheManager.getLocalCacheManager()).isSameAs(localCacheManager);
    }
}
