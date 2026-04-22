/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that {@link AiGatewayChatModelFactory#getChatModel} actually hits the Spring cache through the AOP proxy —
 * not just on the source-annotation level. Without this test, a refactor that accidentally calls
 * {@code this.createChatModel(...)} instead of routing through the bean (or that removes {@code @EnableCaching} from
 * the consuming app) silently degrades to a fresh HTTP client per request with no signal.
 *
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayChatModelFactoryCacheTest.CacheTestConfiguration.class)
class AiGatewayChatModelFactoryCacheTest {

    @Autowired
    private AiGatewayChatModelFactory aiGatewayChatModelFactory;

    @Test
    void testSecondCallReturnsCachedInstance() {
        AiGatewayProvider provider = buildProvider(10L);

        ChatModel first = aiGatewayChatModelFactory.getChatModel(provider);
        ChatModel second = aiGatewayChatModelFactory.getChatModel(provider);

        assertThat(second)
            .as("Second invocation must return the cached ChatModel instance; otherwise @Cacheable is a no-op because "
                + "callers are bypassing the Spring proxy, which defeats the factory's entire purpose")
            .isSameAs(first);
    }

    @Test
    void testEvictForcesFreshModelOnNextCall() {
        AiGatewayProvider provider = buildProvider(11L);

        ChatModel first = aiGatewayChatModelFactory.getChatModel(provider);

        aiGatewayChatModelFactory.evict(11L);

        ChatModel afterEvict = aiGatewayChatModelFactory.getChatModel(provider);

        assertThat(afterEvict)
            .as("After evict(), the factory must rebuild the ChatModel so a rotated API key or changed baseUrl takes "
                + "effect on the next call; a stale-cache hit here means a rotated credential keeps working unchanged")
            .isNotSameAs(first);
    }

    @Test
    void testConcurrentGetChatModelStampedeYieldsAtMostTwoInstances() throws InterruptedException {
        // Under Spring's default ConcurrentMapCache the @Cacheable lookup is not strictly once-and-only-once: two
        // threads can race past the cache miss before either writes. What we guarantee is the map converges — once
        // the first write lands, every subsequent caller sees the cached instance. This test asserts bounded
        // duplication (<= thread count, in practice much less) rather than exactly-one; an unbounded explosion
        // (one instance per caller) means the cache is completely broken by a refactor or a missing @EnableCaching.
        AiGatewayProvider provider = buildProvider(30L);

        int threadCount = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        Set<ChatModel> distinctInstances = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();

                try {
                    startLatch.await();
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();

                    return;
                }

                distinctInstances.add(aiGatewayChatModelFactory.getChatModel(provider));
            });
        }

        assertThat(readyLatch.await(5, TimeUnit.SECONDS))
            .as("All worker threads must reach the barrier")
            .isTrue();

        startLatch.countDown();

        executor.shutdown();

        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
            .as("Concurrent getChatModel calls must complete within timeout")
            .isTrue();

        assertThat(distinctInstances)
            .as("With @Cacheable(sync=true), concurrent misses on the same key must collapse to exactly one " +
                "cache-populating invocation — N distinct instances would mean sync was removed and every thread " +
                "paid the ChatModel construction cost.")
            .hasSize(1);

        // After the stampede resolves, subsequent calls must all hit the cached instance.
        ChatModel stable = aiGatewayChatModelFactory.getChatModel(provider);

        for (int i = 0; i < 5; i++) {
            assertThat(aiGatewayChatModelFactory.getChatModel(provider))
                .as("Post-stampede, every call must return the same cached instance")
                .isSameAs(stable);
        }
    }

    @Test
    void testEvictAfterProviderMutationForcesFreshInstance() {
        // Simulates the admin workflow: a provider's baseUrl/apiKey is updated, the service calls evict(providerId),
        // and the next getChatModel must build a fresh ChatModel with the new config. A regression where evict is
        // skipped means rotated credentials silently keep working against stale cached clients — a security hole.
        long providerId = 40L;
        AiGatewayProvider original = buildProvider(providerId);

        ChatModel beforeMutation = aiGatewayChatModelFactory.getChatModel(original);

        // Mutate the stored provider fields as if an admin updated them.
        AiGatewayProvider mutated = new AiGatewayProvider(
            "cache-test-provider-" + providerId, AiGatewayProviderType.OPENAI, "sk-rotated-key-" + providerId);

        try {
            Field idField = AiGatewayProvider.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(mutated, providerId);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError(reflectiveOperationException);
        }

        // Without calling evict(), the cache still returns the old instance — demonstrates why evict is mandatory.
        assertThat(aiGatewayChatModelFactory.getChatModel(mutated))
            .as("Cache returns the stale instance until evict() is called — test documents the contract every " +
                "provider-write path must honor")
            .isSameAs(beforeMutation);

        aiGatewayChatModelFactory.evict(providerId);

        ChatModel afterEvict = aiGatewayChatModelFactory.getChatModel(mutated);

        assertThat(afterEvict)
            .as("After the write path calls evict(providerId), the next getChatModel must rebuild with the " +
                "current (rotated) credentials")
            .isNotSameAs(beforeMutation);
    }

    @Test
    void testEvictAllClearsEveryEntry() {
        AiGatewayProvider providerA = buildProvider(20L);
        AiGatewayProvider providerB = buildProvider(21L);

        ChatModel cachedA = aiGatewayChatModelFactory.getChatModel(providerA);
        ChatModel cachedB = aiGatewayChatModelFactory.getChatModel(providerB);

        aiGatewayChatModelFactory.evictAll();

        assertThat(aiGatewayChatModelFactory.getChatModel(providerA)).isNotSameAs(cachedA);
        assertThat(aiGatewayChatModelFactory.getChatModel(providerB)).isNotSameAs(cachedB);
    }

    /**
     * Builds a provider stub bypassing the public constructor (which insists on a non-blank api key) because these
     * cache tests deliberately don't want a real SDK to be initialized. The returned {@link ChatModel} is a valid
     * Spring AI object but is never asked to make an HTTP call.
     */
    private static AiGatewayProvider buildProvider(long id) {
        AiGatewayProvider provider = new AiGatewayProvider(
            "cache-test-provider-" + id, AiGatewayProviderType.OPENAI, "sk-test-key-" + id);

        try {
            Field idField = AiGatewayProvider.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(provider, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id on test provider", reflectiveOperationException);
        }

        return provider;
    }

    @Configuration
    @EnableCaching
    @Import(AiGatewayChatModelFactory.class)
    static class CacheTestConfiguration {

        @Bean
        org.springframework.cache.CacheManager cacheManager() {
            org.springframework.cache.concurrent.ConcurrentMapCacheManager manager =
                new org.springframework.cache.concurrent.ConcurrentMapCacheManager(
                    AiGatewayChatModelFactory.AI_GATEWAY_CHAT_MODEL_CACHE);

            return manager;
        }
    }
}
