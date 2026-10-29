/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Unit tests for {@link WebhookResumeRegistry}. The registry's correctness lives almost entirely in the contract around
 * {@link WebhookResumeRegistry#consume(String)}; it must remove and return the entry so a duplicate delivery of the
 * same chat turn doesn't fire the resume POST twice.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WebhookResumeRegistryTest {

    @Test
    void testConsumeReturnsNullWhenNoEntry() {
        WebhookResumeRegistry registry = newRegistry();

        assertThat(registry.consume("thread-42")).isNull();
    }

    @Test
    void testRegisterThenConsumeReturnsUrl() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register("thread-42", "https://example.com/resume/abc");

        assertThat(registry.consume("thread-42")).isEqualTo("https://example.com/resume/abc");
    }

    @Test
    void testConsumeRemovesTheEntry() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register("thread-42", "https://example.com/resume/abc");

        assertThat(registry.consume("thread-42")).isEqualTo("https://example.com/resume/abc");
        assertThat(registry.consume("thread-42")).isNull();
    }

    @Test
    void testRegisterOverwritesPriorEntry() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register("thread-42", "https://example.com/resume/old");
        registry.register("thread-42", "https://example.com/resume/new");

        assertThat(registry.consume("thread-42")).isEqualTo("https://example.com/resume/new");
    }

    @Test
    void testRegistryIsKeyedByThreadId() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register("thread-1", "https://example.com/resume/one");
        registry.register("thread-2", "https://example.com/resume/two");

        assertThat(registry.consume("thread-1")).isEqualTo("https://example.com/resume/one");
        assertThat(registry.consume("thread-2")).isEqualTo("https://example.com/resume/two");
    }

    @Test
    void testChatsWithTheSameNumericIdInDifferentTenantsDoNotShareASlot() {
        WebhookResumeRegistry registry = newRegistry();
        AiHubChat tenantAChat = newChat(11L, "a1b2c3d4-0000-4000-8000-00000000000a");
        AiHubChat tenantBChat = newChat(11L, "a1b2c3d4-0000-4000-8000-00000000000b");

        registry.register(tenantAChat.getThreadId(), "https://tenant-a.example.com/resume");

        assertThat(registry.consume(tenantBChat.getThreadId())).isNull();
        assertThat(registry.consume(tenantAChat.getThreadId())).isEqualTo("https://tenant-a.example.com/resume");
    }

    @Test
    void testClearDropsThePendingResumeUrl() {
        WebhookResumeRegistry registry = newRegistry();

        registry.register("thread-5", "http://resume");
        registry.clear("thread-5");

        assertThat(registry.consume("thread-5")).isNull();
    }

    /**
     * Build a registry with a fresh in-memory {@link ConcurrentMapCacheManager} per test. Production picks Caffeine
     * (single-instance) or Redis (multi-instance) via {@code CacheConfiguration}; for unit tests the concurrent-map
     * manager is the simplest in-memory cache that satisfies the same {@code Cache} contract, keeping each test
     * isolated without spinning up Caffeine.
     */
    private static AiHubChat newChat(long chatId, String threadId) {
        AiHubChat chat = new AiHubChat(3L);

        chat.setId(chatId);
        chat.setThreadId(threadId);

        return chat;
    }

    private static WebhookResumeRegistry newRegistry() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME);

        return new WebhookResumeRegistry(cacheManager);
    }
}
