/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.event.BaseEvent;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Per-thread registry of AI Hub agent runs. Decouples the agent's reactive stream from any one HTTP {@code SseEmitter},
 * so a client refresh mid-stream does not kill the agent: the run keeps publishing into its {@link Sinks.Many replay
 * sink}, and a later attach sees both the buffered history and the live tail. It is also the one-turn-per-chat gate:
 * {@link #tryStart} refuses a second run while one is in flight for the thread.
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

    private final Cache<String, InFlightRun> runs;
    private final Cache<String, Boolean> cancelledRunIds;

    public InFlightAiHubRunRegistry() {
        this(buildDefaultCache());
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    InFlightAiHubRunRegistry(Cache<String, InFlightRun> runs) {
        this.runs = runs;
        this.cancelledRunIds = buildCancelledRunIdCache();
    }

    /**
     * Registers a new run for {@code threadId}, atomically with the check that no other run is in flight for it.
     * Returns empty when a run is still in flight, so the caller can refuse the turn. A terminated entry (kept for
     * late-attach replay) is replaced, so the new run starts with an empty buffer. When {@code runId} was cancelled
     * before it got here, the run is returned already terminated and the caller must not start the agent.
     */
    public Optional<InFlightRun> tryStart(String threadId, @Nullable String runId) {
        AtomicBoolean started = new AtomicBoolean();

        InFlightRun run = runs.asMap()
            .compute(threadId, (key, existing) -> {
                if (existing != null && !existing.isTerminated()) {
                    return existing;
                }

                if (existing != null) {
                    existing.completeSink();
                }

                InFlightRun newRun = new InFlightRun(threadId, runId);

                if (runId != null && cancelledRunIds.asMap()
                    .remove(runId) != null) {

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
     * Terminates the live run for {@code threadId} and completes its subscribers. Returns {@code true} when a live run
     * was cancelled, {@code false} when none existed. The agent's own subscription may keep running server-side, but
     * its later events are dropped because the run's sink is terminal.
     *
     * <p>
     * When {@code runId} is supplied it is recorded as a tombstone, so a Stop that reaches the server before the agent
     * POST registers the run still terminates it (see {@link #tryStart}).
     * </p>
     */
    public boolean cancel(String threadId, @Nullable String runId) {
        if (runId != null && !runId.isBlank()) {
            cancelledRunIds.put(runId, Boolean.TRUE);
        }

        InFlightRun run = runs.getIfPresent(threadId);

        if (run == null || run.isTerminated()) {
            return false;
        }

        run.complete();

        return true;
    }

    /**
     * Returns a {@link Flux} that replays the run's buffered events and then follows the live tail. Empty when no run
     * is registered for {@code threadId}.
     */
    public Optional<Flux<BaseEvent>> attach(String threadId) {
        InFlightRun run = runs.getIfPresent(threadId);

        if (run == null) {
            return Optional.empty();
        }

        return Optional.of(run.asFlux());
    }

    /**
     * Returns {@code true} if a non-terminated run exists for {@code threadId}.
     */
    public boolean isInFlight(String threadId) {
        InFlightRun run = runs.getIfPresent(threadId);

        return run != null && !run.isTerminated();
    }

    private static Cache<String, InFlightRun> buildDefaultCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(ENTRY_TTL_MINUTES, TimeUnit.MINUTES)
            .build();
    }

    private static Cache<String, Boolean> buildCancelledRunIdCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(CANCELLED_RUN_ID_TTL_MINUTES, TimeUnit.MINUTES)
            .build();
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
        private volatile boolean terminated;

        InFlightRun(String threadId, @Nullable String runId) {
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

        void complete() {
            terminated = true;

            completeSink();
        }

        void fail(Throwable error) {
            terminated = true;

            sink.tryEmitError(error);
        }

        Flux<BaseEvent> asFlux() {
            return sink.asFlux();
        }

        private void completeSink() {
            sink.tryEmitComplete();
        }
    }
}
