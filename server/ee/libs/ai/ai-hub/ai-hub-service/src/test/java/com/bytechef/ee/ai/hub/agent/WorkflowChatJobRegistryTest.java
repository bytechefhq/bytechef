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
 * Unit tests for {@link WorkflowChatJobRegistry}. The registry is the chatId to jobId mapping the cancel-turn mutation
 * reads to resolve which workflow execution to stop. Mirrors {@code WebhookResumeRegistry} for shape and is tested for
 * the same reasons: small contract surface, but a regression here breaks cancel silently, so a user clicking Stop on a
 * workflow chat would see the button do nothing because the registry couldn't resolve the chat back to its job.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowChatJobRegistryTest {

    @Test
    void testGetReturnsNullWhenNoEntry() {
        WorkflowChatJobRegistry registry = newRegistry();

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

        assertThat(registry.get(42L)).isEqualTo(7777L);
        assertThat(registry.get(42L)).isEqualTo(7777L);
    }

    @Test
    void testRegisterOverwritesPriorEntry() {
        WorkflowChatJobRegistry registry = newRegistry();

        registry.register(42L, 7777L);
        registry.register(42L, 8888L);

        assertThat(registry.get(42L)).isEqualTo(8888L);
    }

    @Test
    void testClearRemovesEntry() {
        WorkflowChatJobRegistry registry = newRegistry();

        registry.register(42L, 7777L);
        registry.clear(42L);

        assertThat(registry.get(42L)).isNull();
    }

    @Test
    void testClearIsIdempotent() {
        WorkflowChatJobRegistry registry = newRegistry();

        registry.clear(42L);
        registry.clear(42L);

        assertThat(registry.get(42L)).isNull();
    }

    @Test
    void testRegistryIsKeyedByChatId() {
        WorkflowChatJobRegistry registry = newRegistry();

        registry.register(1L, 100L);
        registry.register(2L, 200L);

        assertThat(registry.get(1L)).isEqualTo(100L);
        assertThat(registry.get(2L)).isEqualTo(200L);

        registry.clear(1L);

        assertThat(registry.get(1L)).isNull();
        assertThat(registry.get(2L)).isEqualTo(200L);
    }

    /**
     * Build a registry with a fresh in-memory {@link ConcurrentMapCacheManager} per test. Mirrors
     * {@code WebhookResumeRegistryTest.newRegistry}; the in-memory cache satisfies the same {@code Cache} contract
     * Caffeine and Redis use in production without spinning up either backend for tests.
     */
    private static WorkflowChatJobRegistry newRegistry() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(WorkflowChatJobRegistry.CACHE_NAME);

        return new WorkflowChatJobRegistry(cacheManager);
    }
}
