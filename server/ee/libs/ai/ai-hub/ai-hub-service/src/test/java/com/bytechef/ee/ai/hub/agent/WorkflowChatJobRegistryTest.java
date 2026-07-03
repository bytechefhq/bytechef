/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Unit tests for {@link WorkflowChatJobRegistry}. The registry is the taskId → jobId mapping the cancel-turn mutation
 * reads to resolve which workflow execution to stop. Mirrors {@code WebhookResumeRegistry} for shape and is tested for
 * the same reasons: small contract surface, but a regression here breaks cancel silently — a user clicking Stop on a
 * workflow chat would see the button do nothing because the registry couldn't resolve the task back to its job.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowChatJobRegistryTest {

    @Test
    void testGetReturnsNullWhenNoEntry() {
        WorkflowChatJobRegistry registry = newRegistry();

        // No entry — the cancel-turn mutation interprets null as "no job to cancel" and surfaces a clean
        // false to the client. Without this null return, a freshly-loaded task would surface a
        // confusing NullPointerException on the first cancel attempt.
        assertThat(registry.get(42L)).isNull();
    }

    @Test
    void testRegisterThenGetReturnsJobId() {
        WorkflowChatJobRegistry registry = newRegistry();

        registry.register(42L, 7777L);

        assertThat(registry.get(42L)).isEqualTo(7777L);
    }

    @Test
    void testGetIsNonAtomicallyReading() {
        WorkflowChatJobRegistry registry = newRegistry();

        registry.register(42L, 7777L);

        // Deliberately non-atomic on read — the cancel mutation needs to be idempotent (the caller may
        // retry on transient failure) and racing the eviction with cancel-followed-by-RUN_FINISHED isn't
        // worth the complexity. Pin: two reads return the same value, neither removes the entry.
        assertThat(registry.get(42L)).isEqualTo(7777L);
        assertThat(registry.get(42L)).isEqualTo(7777L);
    }

    @Test
    void testRegisterOverwritesPriorEntry() {
        WorkflowChatJobRegistry registry = newRegistry();

        registry.register(42L, 7777L);
        registry.register(42L, 8888L);

        // The bridge contract is "the latest jobId wins" — a task whose previous turn finished but
        // whose cache entry hasn't yet been evicted gets its slot replaced when the next turn's start
        // event fires. Pin: register-then-register-then-get returns the second value.
        assertThat(registry.get(42L)).isEqualTo(8888L);
    }

    @Test
    void testClearRemovesEntry() {
        WorkflowChatJobRegistry registry = newRegistry();

        registry.register(42L, 7777L);
        registry.clear(42L);

        // Clear is the bridge's onComplete/onError finaliser — without it, a cancel attempt fired AFTER
        // the workflow legitimately finished would either fail with "job already done" or, worse, cancel
        // an unrelated job that landed in the same slot before the cache TTL expired.
        assertThat(registry.get(42L)).isNull();
    }

    @Test
    void testClearIsIdempotent() {
        WorkflowChatJobRegistry registry = newRegistry();

        // Idempotent clear — the bridge's onComplete and onError both fire clear() under racing-error
        // conditions (the executor's whenComplete chain can deliver both stream-level and future-level
        // errors). Without idempotency a second clear would either NPE or evict an unrelated entry.
        registry.clear(42L);
        registry.clear(42L);

        assertThat(registry.get(42L)).isNull();
    }

    @Test
    void testRegistryIsKeyedByTaskId() {
        WorkflowChatJobRegistry registry = newRegistry();

        registry.register(1L, 100L);
        registry.register(2L, 200L);

        // Independent tasks don't pollute each other's slots — concurrent workflow chats can each
        // have their own pending jobId, and cancelling task 1 doesn't touch task 2.
        assertThat(registry.get(1L)).isEqualTo(100L);
        assertThat(registry.get(2L)).isEqualTo(200L);

        registry.clear(1L);

        // Pin the cross-key isolation: clearing task 1 leaves task 2's entry intact.
        assertThat(registry.get(1L)).isNull();
        assertThat(registry.get(2L)).isEqualTo(200L);
    }

    /**
     * Build a registry with a fresh in-memory {@link ConcurrentMapCacheManager} per test. Mirrors
     * {@code WebhookResumeRegistryTest.newRegistry} — the in-memory cache satisfies the same {@code Cache} contract
     * Caffeine and Redis use in production without spinning up either backend for tests.
     */
    private static WorkflowChatJobRegistry newRegistry() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(WorkflowChatJobRegistry.CACHE_NAME);

        return new WorkflowChatJobRegistry(cacheManager);
    }
}
