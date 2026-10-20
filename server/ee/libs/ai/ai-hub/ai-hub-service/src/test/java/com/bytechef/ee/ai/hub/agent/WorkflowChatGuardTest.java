/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.agent.WorkflowChatGuard.AdmissionResult;
import java.lang.reflect.Field;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Unit tests for {@link WorkflowChatGuard}: the per-chat cooldown window and the user-facing rejection message.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowChatGuardTest {

    @Test
    void testFirstTurnIsAdmitted() {
        WorkflowChatGuard guard = newGuard();

        AdmissionResult result = guard.tryAdmit(1L);

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
    }

    @Test
    void testSecondTurnAfterCooldownElapsedIsAdmitted() throws InterruptedException {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);

        Thread.sleep(WorkflowChatGuard.RATE_LIMIT_MIN_INTERVAL_MS + 100);

        AdmissionResult result = guard.tryAdmit(1L);

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
    }

    @Test
    void testSecondTurnDuringCooldownIsRateLimited() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);

        AdmissionResult result = guard.tryAdmit(1L);

        assertThat(result).isInstanceOf(AdmissionResult.RateLimited.class);

        AdmissionResult.RateLimited rateLimited = (AdmissionResult.RateLimited) result;

        assertThat(rateLimited.cooldownRemaining()).isLessThanOrEqualTo(
            Duration.ofMillis(WorkflowChatGuard.RATE_LIMIT_MIN_INTERVAL_MS));
        assertThat(rateLimited.cooldownRemaining()).isPositive();
    }

    @Test
    void testIndependentChatsDoNotInterfere() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);

        AdmissionResult result = guard.tryAdmit(2L);

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
    }

    @Test
    void testRateLimitedUserFacingMessageIncludesCountdown() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);

        AdmissionResult.RateLimited rateLimited = (AdmissionResult.RateLimited) guard.tryAdmit(1L);

        assertThat(rateLimited.userFacingMessage()).contains("Wait");
        assertThat(rateLimited.userFacingMessage()).contains("seconds");
    }

    @Test
    void testAdmitResultUserFacingMessageIsNull() {
        assertThat(AdmissionResult.admit()
            .userFacingMessage()).isNull();
    }

    @Test
    void testRegistryHasNoInFlightCache() {
        assertThat(WorkflowChatGuard.class.getDeclaredFields())
            .extracting(Field::getName)
            .doesNotContain("IN_FLIGHT_CACHE_NAME");
    }

    private static WorkflowChatGuard newGuard() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(WorkflowChatGuard.LAST_TURN_CACHE_NAME);

        return new WorkflowChatGuard(cacheManager);
    }
}
