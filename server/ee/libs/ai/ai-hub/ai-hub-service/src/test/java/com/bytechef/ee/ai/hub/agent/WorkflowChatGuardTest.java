/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.agent.WorkflowChatGuard.AdmissionResult;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    private static final String OTHER_THREAD_ID = "00000000-0000-0000-0000-000000000002";
    private static final String THREAD_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void testFirstTurnIsAdmitted() {
        WorkflowChatGuard guard = newGuard();

        AdmissionResult result = guard.tryAdmit(THREAD_ID);

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
    }

    @Test
    void testSecondTurnAfterCooldownElapsedIsAdmitted() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-20T10:00:00Z"));
        WorkflowChatGuard guard = new WorkflowChatGuard(
            new ConcurrentMapCacheManager(WorkflowChatGuard.LAST_TURN_CACHE_NAME), clock);

        guard.tryAdmit(THREAD_ID);

        clock.advance(Duration.ofMillis(WorkflowChatGuard.RATE_LIMIT_MIN_INTERVAL_MS - 1));

        assertThat(guard.tryAdmit(THREAD_ID)).isInstanceOf(AdmissionResult.RateLimited.class);

        clock.advance(Duration.ofMillis(1));

        AdmissionResult result = guard.tryAdmit(THREAD_ID);

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
    }

    @Test
    void testSecondTurnDuringCooldownIsRateLimited() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(THREAD_ID);

        AdmissionResult result = guard.tryAdmit(THREAD_ID);

        assertThat(result).isInstanceOf(AdmissionResult.RateLimited.class);

        AdmissionResult.RateLimited rateLimited = (AdmissionResult.RateLimited) result;

        assertThat(rateLimited.cooldownRemaining()).isLessThanOrEqualTo(
            Duration.ofMillis(WorkflowChatGuard.RATE_LIMIT_MIN_INTERVAL_MS));
        assertThat(rateLimited.cooldownRemaining()).isPositive();
    }

    @Test
    void testIndependentChatsDoNotInterfere() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(THREAD_ID);

        AdmissionResult result = guard.tryAdmit(OTHER_THREAD_ID);

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
    }

    @Test
    void testChatsSharingANumericIdAcrossTenantsDoNotShareACooldown() {
        WorkflowChatGuard guard = newGuard();

        AiHubChat tenantOneChat = newChat(1L, THREAD_ID);
        AiHubChat tenantTwoChat = newChat(1L, OTHER_THREAD_ID);

        guard.tryAdmit(tenantOneChat.getThreadId());

        AdmissionResult result = guard.tryAdmit(tenantTwoChat.getThreadId());

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
        assertThat(guard.tryAdmit(tenantOneChat.getThreadId())).isInstanceOf(AdmissionResult.RateLimited.class);
    }

    @Test
    void testRateLimitedUserFacingMessageIncludesCountdown() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(THREAD_ID);

        AdmissionResult.RateLimited rateLimited = (AdmissionResult.RateLimited) guard.tryAdmit(THREAD_ID);

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

    private static AiHubChat newChat(long chatId, String threadId) {
        AiHubChat chat = new AiHubChat(1L);

        chat.setId(chatId);
        chat.setThreadId(threadId);

        return chat;
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static WorkflowChatGuard newGuard() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(WorkflowChatGuard.LAST_TURN_CACHE_NAME);

        return new WorkflowChatGuard(cacheManager);
    }
}
