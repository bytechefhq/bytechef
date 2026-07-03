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
 * Unit tests for {@link WebhookResumeRegistry}. The registry's correctness lives almost entirely in the contract around
 * {@link WebhookResumeRegistry#consume(long)} — it must atomically remove-and-return so a duplicate delivery of the
 * same task turn doesn't fire the resume POST twice.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WebhookResumeRegistryTest {

    @Test
    void testConsumeReturnsNullWhenNoEntry() {
        WebhookResumeRegistry registry = newRegistry();

        assertThat(registry.consume(42L)).isNull();
    }

    @Test
    void testRegisterThenConsumeReturnsUrl() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register(42L, "https://example.com/resume/abc");

        assertThat(registry.consume(42L)).isEqualTo("https://example.com/resume/abc");
    }

    @Test
    void testConsumeIsAtomicallyRemoving() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register(42L, "https://example.com/resume/abc");

        // First consume returns the URL; second consume sees an empty slot. This is the contract that
        // prevents duplicate-resume-POST when a task receives two near-simultaneous turns.
        assertThat(registry.consume(42L)).isEqualTo("https://example.com/resume/abc");
        assertThat(registry.consume(42L)).isNull();
    }

    @Test
    void testRegisterOverwritesPriorEntry() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register(42L, "https://example.com/resume/old");
        registry.register(42L, "https://example.com/resume/new");

        // The bridge contract is "the latest URL wins" — a workflow that emits a second
        // ask_user_question on the same task should resume against the latest URL.
        assertThat(registry.consume(42L)).isEqualTo("https://example.com/resume/new");
    }

    @Test
    void testRegistryIsKeyedByTaskId() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register(1L, "https://example.com/resume/one");
        registry.register(2L, "https://example.com/resume/two");

        // Independent tasks don't pollute each other's slots — concurrent workflow chats
        // can each have their own pending resume URL.
        assertThat(registry.consume(1L)).isEqualTo("https://example.com/resume/one");
        assertThat(registry.consume(2L)).isEqualTo("https://example.com/resume/two");
    }

    /**
     * Build a registry with a fresh in-memory {@link ConcurrentMapCacheManager} per test. Production picks Caffeine
     * (single-instance) or Redis (multi-instance) via {@code CacheConfiguration} — for unit tests the concurrent-map
     * manager is the simplest in-memory cache that satisfies the same {@code Cache} contract, keeping each test
     * isolated without spinning up Caffeine.
     */
    private static WebhookResumeRegistry newRegistry() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME);

        return new WebhookResumeRegistry(cacheManager);
    }
}
