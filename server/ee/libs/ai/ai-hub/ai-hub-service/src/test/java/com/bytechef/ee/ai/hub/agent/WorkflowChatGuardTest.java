/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.agent.WorkflowChatGuard.AdmissionResult;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Unit tests for {@link WorkflowChatGuard}. The guard sits in front of every workflow-chat turn — getting the admission
 * decision wrong on either side has visible consequences:
 *
 * <ul>
 * <li>Falsely admitting back-to-back turns burns paid-LLM API quota and races two workflow runs whose outputs land in
 * the chat in non-deterministic order.</li>
 * <li>Falsely rejecting a legitimate retry locks the user out of their own task until the cache TTL expires.</li>
 * </ul>
 *
 * <p>
 * The tests cover the contract surface from {@code tryAdmit} / {@code release}: the cooldown window, the in-flight
 * lock, the order of checks (concurrency before rate limit), and the user-facing message rendering on rejection.
 * </p>
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
    void testSecondTurnAfterReleaseIsAdmittedWhenCooldownElapsed() throws InterruptedException {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);
        guard.release(1L);

        // Wait past the rate-limit window. 2.1s adds a small safety margin over the 2s cooldown to account for
        // wall-clock granularity on slow CI runners — flakier than ideal, but a fake-clock injection would
        // require widening the production API surface for a single test.
        Thread.sleep(WorkflowChatGuard.RATE_LIMIT_MIN_INTERVAL_MS + 100);

        AdmissionResult result = guard.tryAdmit(1L);

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
    }

    @Test
    void testSecondTurnDuringCooldownIsRateLimited() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);
        guard.release(1L);

        AdmissionResult result = guard.tryAdmit(1L);

        // Same task, immediate second call: rate-limit fires because the previous turn started < 2s ago.
        // Concurrency would NOT fire because release() cleared the in-flight slot.
        assertThat(result).isInstanceOf(AdmissionResult.RateLimited.class);

        AdmissionResult.RateLimited rateLimited = (AdmissionResult.RateLimited) result;

        assertThat(rateLimited.cooldownRemaining()).isLessThanOrEqualTo(
            Duration.ofMillis(WorkflowChatGuard.RATE_LIMIT_MIN_INTERVAL_MS));
        assertThat(rateLimited.cooldownRemaining()).isPositive();
    }

    @Test
    void testSecondTurnWhilePreviousInFlightIsConcurrencyBlocked() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);
        // No release() — simulates a turn still running. The next admission attempt should hit the
        // concurrency branch BEFORE the rate-limit branch, because concurrency is the more actionable failure.

        AdmissionResult result = guard.tryAdmit(1L);

        assertThat(result).isInstanceOf(AdmissionResult.ConcurrencyBlocked.class);
    }

    @Test
    void testConcurrencyChecksFirstEvenWhenRateLimitWouldAlsoFire() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);
        // Both gates would reject the next turn — concurrency (in-flight set) and rate-limit (just started).
        // The guard's contract is to return the more-actionable concurrency message rather than the
        // wait-N-seconds rate-limit message, so a button-mash gives the user "previous turn still running"
        // instead of a confusing cooldown countdown.
        AdmissionResult result = guard.tryAdmit(1L);

        assertThat(result).isInstanceOf(AdmissionResult.ConcurrencyBlocked.class);
    }

    @Test
    void testIndependentTasksDoNotInterfere() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);
        // AiHubTask 2 has no in-flight turn and no rate-limit history — both gates pass.
        AdmissionResult result = guard.tryAdmit(2L);

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
    }

    @Test
    void testReleaseIsIdempotent() {
        WorkflowChatGuard guard = newGuard();

        // Releasing a never-admitted task is a no-op rather than a throw — the bridge calls
        // release() unconditionally on terminal events, even paths that never admitted (early-error
        // branches that took a guard.release() short-circuit).
        guard.release(1L);
        guard.release(1L);

        AdmissionResult result = guard.tryAdmit(1L);

        assertThat(result).isInstanceOf(AdmissionResult.Admit.class);
    }

    @Test
    void testRateLimitedUserFacingMessageIncludesCountdown() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);
        guard.release(1L);

        AdmissionResult.RateLimited rateLimited = (AdmissionResult.RateLimited) guard.tryAdmit(1L);

        // The message renders the seconds-remaining +1 so the displayed value rounds up — sub-second
        // remaining still tells the user to wait at least one second rather than zero.
        assertThat(rateLimited.userFacingMessage()).contains("Wait");
        assertThat(rateLimited.userFacingMessage()).contains("seconds");
    }

    @Test
    void testConcurrencyBlockedUserFacingMessageMentionsPreviousTurn() {
        WorkflowChatGuard guard = newGuard();

        guard.tryAdmit(1L);

        AdmissionResult.ConcurrencyBlocked blocked = (AdmissionResult.ConcurrencyBlocked) guard.tryAdmit(1L);

        // The message tells the user that a previous turn is still running and offers Stop as a recovery
        // path — distinct from the rate-limit message so the client can branch on the result type rather
        // than parse the string.
        assertThat(blocked.userFacingMessage()).contains("previous turn");
        assertThat(blocked.userFacingMessage()).contains("Stop");
    }

    @Test
    void testAdmitResultUserFacingMessageIsNull() {
        // Admit carries no user-facing message — the bridge surfaces a RUN_ERROR only on rejection. Returning
        // null rather than an empty string lets the bridge use a simple null-check to decide whether to emit.
        assertThat(AdmissionResult.admit()
            .userFacingMessage()).isNull();
    }

    /**
     * Build a guard with a fresh in-memory {@link ConcurrentMapCacheManager} per test. Production picks Caffeine
     * (single-instance) or Redis (multi-instance) via {@code CacheConfiguration}; for unit tests the concurrent-map
     * manager satisfies the same {@code Cache} contract without the Caffeine dependency or TTL bookkeeping.
     */
    private static WorkflowChatGuard newGuard() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
            WorkflowChatGuard.LAST_TURN_CACHE_NAME, WorkflowChatGuard.IN_FLIGHT_CACHE_NAME);

        return new WorkflowChatGuard(cacheManager);
    }
}
