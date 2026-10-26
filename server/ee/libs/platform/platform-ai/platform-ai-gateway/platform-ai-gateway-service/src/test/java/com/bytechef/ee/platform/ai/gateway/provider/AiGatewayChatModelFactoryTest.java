/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayChatModelFactoryTest.CacheTestConfiguration.class)
class AiGatewayChatModelFactoryTest {

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

        ChatModel stable = aiGatewayChatModelFactory.getChatModel(provider);

        for (int i = 0; i < 5; i++) {
            assertThat(aiGatewayChatModelFactory.getChatModel(provider))
                .as("Post-stampede, every call must return the same cached instance")
                .isSameAs(stable);
        }
    }

    @Test
    void testEvictAfterProviderMutationForcesFreshInstance() {
        long providerId = 40L;
        AiGatewayProvider original = buildProvider(providerId);

        ChatModel beforeMutation = aiGatewayChatModelFactory.getChatModel(original);

        AiGatewayProvider mutated = new AiGatewayProvider(
            "cache-test-provider-" + providerId, AiGatewayProviderType.OPENAI, "sk-rotated-key-" + providerId);

        try {
            Field idField = AiGatewayProvider.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(mutated, providerId);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError(reflectiveOperationException);
        }

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
    @Import(AiGatewayChatModelFactoryImpl.class)
    static class CacheTestConfiguration {

        @Bean
        org.springframework.cache.CacheManager cacheManager() {
            org.springframework.cache.concurrent.ConcurrentMapCacheManager manager =
                new org.springframework.cache.concurrent.ConcurrentMapCacheManager(
                    AiGatewayChatModelFactory.AI_GATEWAY_CHAT_MODEL_CACHE);

            return manager;
        }
    }

    @Nested
    class BaseUrlSsrf {

        @Test
        void testCustomerSuppliedBaseUrlIsValidated() {
            AiGatewayProvider customerProvider = newProvider(1L, AiGatewayProviderType.OPENAI);

            customerProvider.setBaseUrl("http://169.254.169.254/latest/meta-data/");

            AiGatewayChatModelFactory aiGatewayChatModelFactory = new AiGatewayChatModelFactoryImpl();

            assertThatThrownBy(() -> aiGatewayChatModelFactory.getChatModel(customerProvider))
                .isInstanceOf(IllegalArgumentException.class);
        }

        private static AiGatewayProvider newProvider(long id, AiGatewayProviderType type) {
            AiGatewayProvider provider = new AiGatewayProvider("byok-ssrf-test-provider", type, "sk-test-key");

            try {
                Field idField = AiGatewayProvider.class.getDeclaredField("id");

                idField.setAccessible(true);
                idField.set(provider, id);
            } catch (ReflectiveOperationException reflectiveOperationException) {
                throw new AssertionError("failed to seed id on test provider", reflectiveOperationException);
            }

            return provider;
        }
    }
}
