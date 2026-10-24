/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.event.BaseEvent;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Per-thread registry of AI Hub agent runs. Decouples the agent's reactive stream from any one HTTP {@code SseEmitter},
 * so a client refresh mid-stream does not kill the agent: the run keeps publishing into its {@link Sinks.Many replay
 * sink}, and a later attach sees both the buffered history and the live tail. It is also the one-turn-per-chat gate:
 * {@link #tryStart} refuses a second run while one is in flight for the thread. A cancelled run stays in flight while
 * its agent drains: until the agent reports that it stopped, or {@link #DRAIN_TIMEOUT} after the cancel, so a new turn
 * or a truncation never overlaps a stopped run that is still writing to the session.
 *
 * <p>
 * Backed by Caffeine directly: an entry holds a {@code Sinks.Many<BaseEvent>}, a stateful reactive primitive that
 * cannot be serialized or shared across JVMs, so the registry is single-instance.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class InFlightAiHubRunRegistry {

    private static final Logger log = LoggerFactory.getLogger(InFlightAiHubRunRegistry.class);

    /**
     * Number of recent {@link BaseEvent} instances retained per run for late attachers.
     */
    static final int REPLAY_BUFFER_LIMIT = 1000;

    /**
     * Backstop TTL for entries that never reach a terminal event. Normal completion terminates the run long before.
     */
    static final long ENTRY_TTL_MINUTES = 30L;

    /**
     * TTL for cancelled-run-id tombstones. A runId is single-use, so this TTL is only memory cleanup.
     */
    static final long CANCELLED_RUN_ID_TTL_MINUTES = 2L;

    /**
     * How long a cancelled run holds its thread when its agent never reports that it stopped.
     */
    static final Duration DRAIN_TIMEOUT = Duration.ofMinutes(2);

    private final Cache<String, InFlightRun> runs;
    private final Cache<String, Boolean> cancelledRunIds;
    private final Cache<String, Boolean> retiredThreadIds;
    private final Executor releaseActionExecutor;

    public InFlightAiHubRunRegistry() {
        this(buildDefaultCache(), Runnable::run);
    }

    @Autowired
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public InFlightAiHubRunRegistry(@Qualifier("workerExecutor") Executor releaseActionExecutor) {
        this(buildDefaultCache(), releaseActionExecutor);
    }

    InFlightAiHubRunRegistry(Cache<String, InFlightRun> runs) {
        this(runs, Runnable::run);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    InFlightAiHubRunRegistry(Cache<String, InFlightRun> runs, Executor releaseActionExecutor) {
        this.runs = runs;
        this.cancelledRunIds = buildCancelledRunIdCache();
        this.retiredThreadIds = buildRetiredThreadIdCache();
        this.releaseActionExecutor = releaseActionExecutor;
    }

    /**
     * Registers a new run for {@code threadId}, atomically with the check that no other run is in flight for it.
     * Returns empty when a run is still in flight, so the caller can refuse the turn. A terminated entry (kept for
     * late-attach replay) is replaced, so the new run starts with an empty buffer. When {@code runId} was cancelled
     * before it got here, the run is returned already terminated and the caller must not start the agent. A retired
     * thread ({@link #retireThread}) is always refused.
     */
    public Optional<InFlightRun> tryStart(String threadId, @Nullable String runId) {
        AtomicBoolean started = new AtomicBoolean();
        String cancelledRunKey = runId == null ? null : tenantKey(runId);

        InFlightRun run = runs.asMap()
            .compute(tenantKey(threadId), (key, existing) -> {
                if (isRetiredKey(key) || existing != null && existing.holdsThread(Instant.now())) {
                    return existing;
                }

                if (existing != null) {
                    existing.completeSink();
                }

                InFlightRun newRun = new InFlightRun(threadId, runId, releaseActionExecutor);

                if (cancelledRunKey != null && cancelledRunIds.asMap()
                    .remove(cancelledRunKey) != null) {

                    log.debug("tryStart for threadId={} runId={} matched a cancel tombstone; born terminated",
                        threadId, runId);

                    newRun.complete();
                }

                started.set(true);

                return newRun;
            });

        return started.get() ? Optional.of(run) : Optional.empty();
    }

    /**
     * Equivalent to {@link #cancel(String, String)} with no runId.
     */
    public boolean cancel(String threadId) {
        return cancel(threadId, null);
    }

    /**
     * Terminates the live run for {@code threadId}, completes its subscribers and signals its agent to stop (see
     * {@link #stopRequested}). Returns {@code true} when a live run was cancelled, {@code false} when none existed. The
     * run keeps holding the thread until its agent stops; events it emits meanwhile are dropped because the run's sink
     * is terminal.
     *
     * <p>
     * When {@code runId} is supplied it is recorded as a tombstone, so a Stop that reaches the server before the agent
     * POST registers the run still terminates it (see {@link #tryStart}), and only a registered run with that id is
     * cancelled, so a delayed Stop never ends the thread's next turn. Without a {@code runId} the thread's live run is
     * cancelled whatever its id.
     * </p>
     */
    public boolean cancel(String threadId, @Nullable String runId) {
        boolean runIdSupplied = runId != null && !runId.isBlank();

        if (runIdSupplied) {
            cancelledRunIds.put(tenantKey(runId), Boolean.TRUE);
        }

        InFlightRun run = runs.getIfPresent(tenantKey(threadId));

        if (run == null || run.isTerminated()) {
            return false;
        }

        if (runIdSupplied && !runId.equals(run.getRunId())) {
            log.debug(
                "Ignoring cancel for threadId={} runId={}: the registered run is runId={}", threadId, runId,
                run.getRunId());

            return false;
        }

        run.cancel();

        return true;
    }

    /**
     * Completes when the run registered for {@code threadId} is cancelled, at once when it already was; never for a
     * thread with no run. The agent ends its model stream on it.
     */
    public Mono<Void> stopRequested(String threadId) {
        InFlightRun run = runs.getIfPresent(tenantKey(threadId));

        return run == null ? Mono.never() : run.stopRequested();
    }

    /**
     * Returns a {@link Flux} that replays the run's buffered events and then follows the live tail. Empty when no run
     * is registered for {@code threadId}.
     */
    public Optional<Flux<BaseEvent>> attach(String threadId) {
        InFlightRun run = runs.getIfPresent(tenantKey(threadId));

        if (run == null) {
            return Optional.empty();
        }

        return Optional.of(run.asFlux());
    }

    /**
     * Runs {@code action} once no run holds {@code threadId}: at once when none does, otherwise on the release action
     * executor right after the holding run releases the thread. An action queued on a run that never reports that it
     * stopped never runs. For a retired thread ({@link #retireThread}) the action is refused: neither queued nor run.
     */
    public void runWhenReleased(String threadId, Runnable action) {
        String key = tenantKey(threadId);

        InFlightRun run = runs.getIfPresent(key);

        DeferResult deferResult = run == null
            ? DeferResult.NOT_HELD : run.deferUntilReleased(action, () -> isRetiredKey(key));

        if (deferResult == DeferResult.QUEUED) {
            return;
        }

        if (deferResult == DeferResult.REFUSED || isRetiredKey(key)) {
            log.debug("Refused a deferred action for retired threadId={}", threadId);

            return;
        }

        action.run();
    }

    /**
     * Retires {@code threadId} because its chat is being deleted, then cancels its live run. Drops the actions queued
     * with {@link #runWhenReleased} on the run holding it, refuses any later one, and refuses {@link #tryStart} for it.
     * The discard runs in a {@code compute} on the thread's entry, so it serializes with {@link #tryStart}: either that
     * {@code tryStart} sees the retirement, or the run it installed is the one cancelled here. Returns {@code true}
     * when a live run was cancelled. The retirement expires like the run entries; {@link #reinstateThread} lifts it
     * when the delete rolls back.
     */
    public boolean retireThread(String threadId) {
        String key = tenantKey(threadId);

        retiredThreadIds.put(key, Boolean.TRUE);

        AtomicReference<@Nullable InFlightRun> runReference = new AtomicReference<>();

        runs.asMap()
            .compute(key, (cacheKey, run) -> {
                if (run != null) {
                    run.discardReleaseActions();

                    runReference.set(run);
                }

                return run;
            });

        InFlightRun run = runReference.get();

        if (run == null || run.isTerminated()) {
            return false;
        }

        run.cancel();

        return true;
    }

    public void reinstateThread(String threadId) {
        retiredThreadIds.invalidate(tenantKey(threadId));
    }

    public boolean isRetired(String threadId) {
        return isRetiredKey(tenantKey(threadId));
    }

    /**
     * Returns {@code true} while a run holds {@code threadId}: it is live, or it was cancelled and its agent is still
     * draining.
     */
    public boolean isInFlight(String threadId) {
        InFlightRun run = runs.getIfPresent(tenantKey(threadId));

        return run != null && run.holdsThread(Instant.now());
    }

    private boolean isRetiredKey(String key) {
        return retiredThreadIds.getIfPresent(key) != null;
    }

    private static String tenantKey(String id) {
        return TenantCacheKeyUtils.getKey(id);
    }

    private static Cache<String, InFlightRun> buildDefaultCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(ENTRY_TTL_MINUTES, TimeUnit.MINUTES)
            .build();
    }

    private static Cache<String, Boolean> buildRetiredThreadIdCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(ENTRY_TTL_MINUTES, TimeUnit.MINUTES)
            .build();
    }

    private static Cache<String, Boolean> buildCancelledRunIdCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(CANCELLED_RUN_ID_TTL_MINUTES, TimeUnit.MINUTES)
            .build();
    }

    enum DeferResult {

        NOT_HELD, QUEUED, REFUSED
    }

    /**
     * One agent run on one thread: the replay sink producers push to and subscribers attach to. The producer holds the
     * run itself, not the thread id, so events of a cancelled run can never land in a later run on the same thread.
     */
    public static final class InFlightRun {

        private final String threadId;
        private final @Nullable String runId;
        private final Instant startedAt;
        private final Sinks.Many<BaseEvent> sink;
        private final Sinks.Empty<Void> stopSink = Sinks.empty();
        private final List<Runnable> releaseActions = new ArrayList<>();
        private final Executor releaseActionExecutor;
        private volatile boolean agentStopped;
        private volatile @Nullable Instant cancelledAt;
        private volatile boolean terminated;

        InFlightRun(String threadId, @Nullable String runId, Executor releaseActionExecutor) {
            this.releaseActionExecutor = releaseActionExecutor;
            this.threadId = threadId;
            this.runId = runId;
            this.startedAt = Instant.now();
            this.sink = Sinks.many()
                .replay()
                .limit(REPLAY_BUFFER_LIMIT);
        }

        public String getThreadId() {
            return threadId;
        }

        public @Nullable String getRunId() {
            return runId;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public boolean isTerminated() {
            return terminated;
        }

        void emit(BaseEvent event) {
            Sinks.EmitResult result = sink.tryEmitNext(event);

            if (result.isFailure()) {
                log.debug("Failed to emit event for threadId={} runId={}: {}", threadId, runId, result);
            }
        }

        /**
         * Whether the run still holds its thread at {@code now}: its agent has not stopped and, when the run was
         * cancelled, the drain timeout has not passed.
         */
        boolean holdsThread(Instant now) {
            if (agentStopped) {
                return false;
            }

            Instant runCancelledAt = cancelledAt;

            return runCancelledAt == null || now.isBefore(runCancelledAt.plus(DRAIN_TIMEOUT));
        }

        /**
         * Ends the run because its agent stopped, or never started: completes the subscribers and releases the thread.
         */
        public void complete() {
            agentStopped = true;
            terminated = true;

            completeSink();

            runReleaseActions();
        }

        void fail(Throwable error) {
            agentStopped = true;
            terminated = true;

            sink.tryEmitError(error);

            runReleaseActions();
        }

        DeferResult deferUntilReleased(Runnable action, BooleanSupplier retired) {
            synchronized (releaseActions) {
                if (retired.getAsBoolean()) {
                    return DeferResult.REFUSED;
                }

                if (!holdsThread(Instant.now())) {
                    return DeferResult.NOT_HELD;
                }

                releaseActions.add(action);

                return DeferResult.QUEUED;
            }
        }

        private void runReleaseActions() {
            List<Runnable> actions;

            synchronized (releaseActions) {
                actions = List.copyOf(releaseActions);

                releaseActions.clear();
            }

            for (Runnable action : actions) {
                try {
                    releaseActionExecutor.execute(() -> runReleaseAction(action));
                } catch (RuntimeException exception) {
                    log.warn("A deferred action for threadId={} could not be scheduled", threadId, exception);
                }
            }
        }

        private void runReleaseAction(Runnable action) {
            try {
                action.run();
            } catch (RuntimeException exception) {
                log.warn("A deferred action for threadId={} failed", threadId, exception);
            }
        }

        void discardReleaseActions() {
            synchronized (releaseActions) {
                releaseActions.clear();
            }
        }

        void cancel() {
            cancelledAt = Instant.now();
            terminated = true;

            completeSink();

            stopSink.tryEmitEmpty();
        }

        Mono<Void> stopRequested() {
            return stopSink.asMono();
        }

        Flux<BaseEvent> asFlux() {
            return sink.asFlux();
        }

        private void completeSink() {
            sink.tryEmitComplete();
        }
    }
}
