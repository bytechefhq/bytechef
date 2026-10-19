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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    static final int REPLAY_BUFFER_LIMIT = 1000;

    static final long ENTRY_TTL_MINUTES = 30L;

    static final long CANCELLED_RUN_ID_TTL_MINUTES = 2L;

    static final Duration DRAIN_TIMEOUT = Duration.ofMinutes(2);

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

    public Optional<InFlightRun> tryStart(String threadId, @Nullable String runId) {
        AtomicBoolean started = new AtomicBoolean();

        InFlightRun run = runs.asMap()
            .compute(threadId, (key, existing) -> {
                if (existing != null && existing.holdsThread(Instant.now())) {
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

    public boolean cancel(String threadId) {
        return cancel(threadId, null);
    }

    public boolean cancel(String threadId, @Nullable String runId) {
        if (runId != null && !runId.isBlank()) {
            cancelledRunIds.put(runId, Boolean.TRUE);
        }

        InFlightRun run = runs.getIfPresent(threadId);

        if (run == null || run.isTerminated()) {
            return false;
        }

        run.cancel();

        return true;
    }

    public Mono<Void> stopRequested(String threadId) {
        InFlightRun run = runs.getIfPresent(threadId);

        return run == null ? Mono.never() : run.stopRequested();
    }

    public Optional<Flux<BaseEvent>> attach(String threadId) {
        InFlightRun run = runs.getIfPresent(threadId);

        if (run == null) {
            return Optional.empty();
        }

        return Optional.of(run.asFlux());
    }

    public void runWhenReleased(String threadId, Runnable action) {
        InFlightRun run = runs.getIfPresent(threadId);

        if (run != null && run.deferUntilReleased(action)) {
            return;
        }

        action.run();
    }

    public boolean isInFlight(String threadId) {
        InFlightRun run = runs.getIfPresent(threadId);

        return run != null && run.holdsThread(Instant.now());
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
        private final Sinks.Empty<Void> stopSink = Sinks.empty();
        private final List<Runnable> releaseActions = new ArrayList<>();
        private volatile boolean agentStopped;
        private volatile @Nullable Instant cancelledAt;
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

        boolean holdsThread(Instant now) {
            if (agentStopped) {
                return false;
            }

            Instant runCancelledAt = cancelledAt;

            return runCancelledAt == null || now.isBefore(runCancelledAt.plus(DRAIN_TIMEOUT));
        }

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

        boolean deferUntilReleased(Runnable action) {
            synchronized (releaseActions) {
                if (!holdsThread(Instant.now())) {
                    return false;
                }

                releaseActions.add(action);

                return true;
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
                    action.run();
                } catch (RuntimeException exception) {
                    log.warn("A deferred action for threadId={} failed", threadId, exception);
                }
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
