/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Per-task guard for workflow-chat turns. Enforces two production-safety invariants:
 *
 * <ul>
 * <li><b>Rate limit:</b> at most {@value #RATE_LIMIT_MIN_INTERVAL_MS} milliseconds between turns. A user who spams
 * Enter or a buggy client that retries on every keystroke can't fire 100 workflow executions per second — workflows
 * that hit paid APIs (LLM, OpenAI, external services) would otherwise be a denial-of-wallet vector. The cooldown is
 * per-task rather than per-user so multiple parallel workflow chats stay independently usable.</li>
 *
 * <li><b>Concurrency:</b> at most one in-flight turn per task. Two near-simultaneous user messages (mis-click on the
 * send button, network retry) would otherwise race two workflow executions whose outputs land in the chat in
 * non-deterministic order. The lock is acquired on turn start and released on turn complete; a request that finds a
 * held lock surfaces a "previous turn still running" error to the user.</li>
 * </ul>
 *
 * <p>
 * Backed by Spring's {@link CacheManager} so multi-instance deployments share state. The cache is the same
 * Caffeine/Redis dual backend that {@link WebhookResumeRegistry} and {@link WorkflowChatJobRegistry} use, configured by
 * {@link com.bytechef.cache.config.CacheConfiguration}. Caching is acceptable here despite the eventual-consistency
 * window because the worst case is a single duplicate turn slipping through right as a previous one finishes — not a
 * correctness disaster.
 * </p>
 *
 * <p>
 * Both checks return a typed result rather than throwing; the bridge surfaces them as friendly RUN_ERROR events with a
 * recovery hint. Throwing would force the bridge to wrap-and-rewrap, surface generic 500s to the client, and lose the
 * cooldown/concurrency distinction.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class WorkflowChatGuard {

    /**
     * Cooldown between successive turns on the same task. 2 seconds is comfortable for a normal user (faster than
     * typing-then-clicking-send takes anyway) but high enough to bound a runaway client to ~30 turns/min instead of
     * unbounded.
     */
    public static final long RATE_LIMIT_MIN_INTERVAL_MS = 2_000L;

    /**
     * Cache name for last-turn timestamps (rate limit). Key: task id. Value: epoch millis of the last turn that
     * started.
     */
    public static final String LAST_TURN_CACHE_NAME =
        "com.bytechef.ee.ai.hub.agent.WorkflowChatGuard.lastTurn";

    /**
     * Cache name for in-flight task locks (concurrency). Key: task id. Value: epoch millis of when the lock was
     * acquired (used for diagnostics — never read for correctness).
     */
    public static final String IN_FLIGHT_CACHE_NAME =
        "com.bytechef.ee.ai.hub.agent.WorkflowChatGuard.inFlight";

    private final CacheManager cacheManager;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowChatGuard(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Attempts to acquire the per-task turn slot. Returns {@link AdmissionResult#admit()} on success (the bridge
     * proceeds with the turn and must call {@link #release(long)} on completion);
     * {@link AdmissionResult#rateLimited(Duration)} or {@link AdmissionResult#concurrencyBlocked()} on rejection (the
     * bridge surfaces a RUN_ERROR with the result's message).
     *
     * <p>
     * Order of checks: concurrency first, then rate limit. A user trying to fire a second turn while the first is still
     * running gets the more actionable "previous turn still running" message; rate limit applies only to genuinely
     * back-to-back attempts where the previous turn completed quickly.
     * </p>
     */
    public AdmissionResult tryAdmit(long taskId) {
        Cache inFlight = getCache(IN_FLIGHT_CACHE_NAME);

        // Concurrency check: refuse if a turn is already in flight for this task. The cache entry
        // is set by acquire() below (this method) and cleared by release() at turn end.
        if (inFlight.get(taskId) != null) {
            return AdmissionResult.concurrencyBlocked();
        }

        // Rate-limit check: a turn that just finished is fine to retry, but two starts within the cooldown
        // window suggest a runaway client. Read the last-turn timestamp; if within RATE_LIMIT_MIN_INTERVAL_MS,
        // refuse with the time remaining so the client can show a "wait X seconds" message.
        Cache lastTurn = getCache(LAST_TURN_CACHE_NAME);
        Long lastTurnMillis = lastTurn.get(taskId, Long.class);
        long now = Instant.now()
            .toEpochMilli();

        if (lastTurnMillis != null) {
            long elapsed = now - lastTurnMillis;

            if (elapsed < RATE_LIMIT_MIN_INTERVAL_MS) {
                return AdmissionResult.rateLimited(Duration.ofMillis(RATE_LIMIT_MIN_INTERVAL_MS - elapsed));
            }
        }

        // Admit the turn. Set both the in-flight marker (for concurrency) and the last-turn timestamp (for
        // rate limit on the next attempt). The in-flight marker stays until release() fires; the last-turn
        // timestamp is set BEFORE the workflow runs so even a turn that takes 30 seconds enforces the
        // cooldown against the start time, not the end time.
        inFlight.put(taskId, now);
        lastTurn.put(taskId, now);

        return AdmissionResult.admit();
    }

    /**
     * Releases the per-task turn slot. Idempotent — calling release() on a task with no held lock is a no-op. The
     * bridge calls this in onComplete / onError paths so a finished turn frees the slot for the next message.
     */
    public void release(long taskId) {
        getCache(IN_FLIGHT_CACHE_NAME).evictIfPresent(taskId);
    }

    private Cache getCache(String name) {
        Cache cache = cacheManager.getCache(name);

        if (cache == null) {
            throw new IllegalStateException(
                "WorkflowChatGuard cache '" + name + "' is not configured. Add it to the CacheManager "
                    + "(Caffeine via registerCustomCache, Redis via withCacheConfiguration).");
        }

        return cache;
    }

    /**
     * Result of a turn-admission check. Returned by {@link #tryAdmit} so the bridge can branch on the specific failure
     * mode and surface the matching user-facing error message.
     */
    public sealed interface AdmissionResult {

        static AdmissionResult admit() {
            return Admit.INSTANCE;
        }

        static AdmissionResult rateLimited(Duration cooldownRemaining) {
            return new RateLimited(cooldownRemaining);
        }

        static AdmissionResult concurrencyBlocked() {
            return ConcurrencyBlocked.INSTANCE;
        }

        record Admit() implements AdmissionResult {

            static final Admit INSTANCE = new Admit();
        }

        record RateLimited(Duration cooldownRemaining) implements AdmissionResult {

            public String userFacingMessage() {
                long seconds = cooldownRemaining.toSeconds();

                if (seconds == 0) {
                    return "You're sending messages too quickly. Wait a moment and try again.";
                }

                return "You're sending messages too quickly. Wait " + (seconds + 1) + " seconds and try again.";
            }
        }

        record ConcurrencyBlocked() implements AdmissionResult {

            static final ConcurrencyBlocked INSTANCE = new ConcurrencyBlocked();

            public String userFacingMessage() {
                return "The previous turn is still running. Wait for it to finish — or click Stop to cancel — "
                    + "before sending another message.";
            }
        }

        @Nullable
        default String userFacingMessage() {
            return switch (this) {
                case Admit ignored -> null;
                case RateLimited rateLimited -> rateLimited.userFacingMessage();
                case ConcurrencyBlocked concurrencyBlocked -> concurrencyBlocked.userFacingMessage();
            };
        }
    }
}
