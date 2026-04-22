/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitScope;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitType;
import com.bytechef.ee.automation.ai.gateway.ratelimit.InMemoryAiGatewayRateLimiter;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class AiGatewayRateLimitServiceIntTest {

    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private AiGatewayRateLimitService aiGatewayRateLimitService;

    @Autowired
    private InMemoryAiGatewayRateLimiter inMemoryAiGatewayRateLimiter;

    @Test
    public void testCreateRateLimitRule() {
        AiGatewayRateLimit rateLimit = new AiGatewayRateLimit(
            WORKSPACE_ID, "rule-1", AiGatewayRateLimitScope.GLOBAL,
            AiGatewayRateLimitType.REQUESTS, 5, 60);

        AiGatewayRateLimit created = aiGatewayRateLimitService.create(rateLimit);

        Long id = Validate.notNull(created.getId(), "id");

        AiGatewayRateLimit retrieved = aiGatewayRateLimitService.getRateLimit(id);

        assertThat(retrieved)
            .hasFieldOrPropertyWithValue("name", "rule-1")
            .hasFieldOrPropertyWithValue("limitValue", 5)
            .hasFieldOrPropertyWithValue("windowSeconds", 60);
    }

    @Test
    public void testInMemoryRateLimiterAllowsUpToLimit() {
        String key = "test-key-allow-" + System.nanoTime();
        int limit = 3;
        int windowSeconds = 60;

        AiGatewayRateLimitResult first = inMemoryAiGatewayRateLimiter.tryAcquire(key, limit, windowSeconds);
        AiGatewayRateLimitResult second = inMemoryAiGatewayRateLimiter.tryAcquire(key, limit, windowSeconds);
        AiGatewayRateLimitResult third = inMemoryAiGatewayRateLimiter.tryAcquire(key, limit, windowSeconds);

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isTrue();
        assertThat(third.allowed()).isTrue();

        AiGatewayRateLimitResult fourth = inMemoryAiGatewayRateLimiter.tryAcquire(key, limit, windowSeconds);

        assertThat(fourth.allowed()).isFalse();
    }

    @Test
    public void testPerUserScopeIsolatesKeysSoSeparateUsersGetIndependentCounters() {
        // Per-user scope keys the limiter by user so two distinct users each get their own window, even under
        // the same rule id. Catches regressions where key derivation drops the user discriminator and collapses
        // every request to a single shared counter.
        String ruleKeyForUserA = "rule#42#user#alice";
        String ruleKeyForUserB = "rule#42#user#bob";
        int limit = 2;
        int windowSeconds = 60;

        assertThat(inMemoryAiGatewayRateLimiter.tryAcquire(ruleKeyForUserA, limit, windowSeconds)
            .allowed()).isTrue();
        assertThat(inMemoryAiGatewayRateLimiter.tryAcquire(ruleKeyForUserA, limit, windowSeconds)
            .allowed()).isTrue();
        assertThat(inMemoryAiGatewayRateLimiter.tryAcquire(ruleKeyForUserA, limit, windowSeconds)
            .allowed()).isFalse();

        assertThat(inMemoryAiGatewayRateLimiter.tryAcquire(ruleKeyForUserB, limit, windowSeconds)
            .allowed())
                .as("Per-user scope: user B must not be blocked by user A's exhausted window")
                .isTrue();
    }

    @Test
    public void testPerPropertyScopeIsolatesKeysSoDifferentPropertyValuesGetIndependentCounters() {
        // Per-property scope keys by a user-supplied property (e.g. customer_id tag). Distinct property values
        // must not share a counter.
        String ruleKeyForTenantA = "rule#7#property#customer_id=acme";
        String ruleKeyForTenantB = "rule#7#property#customer_id=globex";
        int limit = 1;
        int windowSeconds = 60;

        assertThat(inMemoryAiGatewayRateLimiter.tryAcquire(ruleKeyForTenantA, limit, windowSeconds)
            .allowed()).isTrue();
        assertThat(inMemoryAiGatewayRateLimiter.tryAcquire(ruleKeyForTenantA, limit, windowSeconds)
            .allowed()).isFalse();

        assertThat(inMemoryAiGatewayRateLimiter.tryAcquire(ruleKeyForTenantB, limit, windowSeconds)
            .allowed())
                .as("Per-property scope: distinct values must not share a counter")
                .isTrue();
    }

    /**
     * Under concurrent tryAcquire calls against the same key, the total number of "allowed" results must equal the
     * configured limit — no more, no less. A non-atomic counter would let two threads both read the pre-increment value
     * and both grant permits, silently over-admitting. This is the whole value proposition of an in-memory limiter; a
     * regression here cannot be caught by the serial tests above.
     */
    @Test
    public void testInMemoryRateLimiterIsRaceSafeUnderConcurrentTryAcquire() throws InterruptedException {
        String key = "test-key-concurrent-" + System.nanoTime();
        int limit = 10;
        int windowSeconds = 60;
        int threadCount = 32;
        int attemptsPerThread = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger();

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();

                        for (int attempt = 0; attempt < attemptsPerThread; attempt++) {
                            AiGatewayRateLimitResult result =
                                inMemoryAiGatewayRateLimiter.tryAcquire(key, limit, windowSeconds);

                            if (result.allowed()) {
                                allowedCount.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread()
                            .interrupt();
                    } finally {
                        doneGate.countDown();
                    }
                });
            }

            // Fire all workers at roughly the same instant to maximize contention on the counter.
            startGate.countDown();

            boolean completed = doneGate.await(10, TimeUnit.SECONDS);

            assertThat(completed)
                .as("All concurrent tryAcquire workers should complete within 10s")
                .isTrue();

            assertThat(allowedCount.get())
                .as("Race-safe limiter must admit EXACTLY limit=%d across %d concurrent attempts", limit,
                    threadCount * attemptsPerThread)
                .isEqualTo(limit);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testInMemoryRateLimiterWindowExpires() throws InterruptedException {
        String key = "test-key-window-" + System.nanoTime();
        int limit = 2;
        int windowSeconds = 2;

        inMemoryAiGatewayRateLimiter.tryAcquire(key, limit, windowSeconds);
        inMemoryAiGatewayRateLimiter.tryAcquire(key, limit, windowSeconds);

        AiGatewayRateLimitResult denied = inMemoryAiGatewayRateLimiter.tryAcquire(key, limit, windowSeconds);

        assertThat(denied.allowed()).isFalse();

        Thread.sleep(2100L);

        AiGatewayRateLimitResult allowedAfterWindow =
            inMemoryAiGatewayRateLimiter.tryAcquire(key, limit, windowSeconds);

        assertThat(allowedAfterWindow.allowed()).isTrue();
    }
}
