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
    void testConsumeRemovesTheEntry() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register(42L, "https://example.com/resume/abc");

        assertThat(registry.consume(42L)).isEqualTo("https://example.com/resume/abc");
        assertThat(registry.consume(42L)).isNull();
    }

    @Test
    void testRegisterOverwritesPriorEntry() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register(42L, "https://example.com/resume/old");
        registry.register(42L, "https://example.com/resume/new");

        assertThat(registry.consume(42L)).isEqualTo("https://example.com/resume/new");
    }

    @Test
    void testRegistryIsKeyedByChatId() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register(1L, "https://example.com/resume/one");
        registry.register(2L, "https://example.com/resume/two");

        assertThat(registry.consume(1L)).isEqualTo("https://example.com/resume/one");
        assertThat(registry.consume(2L)).isEqualTo("https://example.com/resume/two");
    }

    @Test
    void testClearDropsThePendingResumeUrl() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register(5L, "http://resume");
        registry.clear(5L);

        assertThat(registry.consume(5L)).isNull();
    }

    private static WebhookResumeRegistry newRegistry() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME);

        return new WebhookResumeRegistry(cacheManager);
    }
}
