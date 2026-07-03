/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.event.BaseEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Per-thread registry of AI Hub agent runs that are still producing events. Decouples the agent's reactive stream from
 * any one HTTP {@code SseEmitter} so that:
 *
 * <ol>
 * <li>A client refresh mid-stream doesn't kill the agent — the original emitter dies, the run keeps publishing into its
 * {@link Sinks.Many replay sink}, and a later attach call sees both the buffered history and the live tail.</li>
 * <li>Multiple AI Hub tasks can stream concurrently — each registered with its own {@code threadId} sink. Switching
 * tasks in the sidebar reattaches to whichever sink is currently in flight (or to none, if the run completed since the
 * last visit).</li>
 * </ol>
 *
 * <p>
 * <b>Direct Caffeine backing.</b> Unlike {@link WorkflowChatJobRegistry} / {@link WebhookResumeRegistry} which route
 * through Spring's {@link org.springframework.cache.CacheManager CacheManager} so they can swap to Redis for
 * multi-instance deployments, this registry holds a {@code Sinks.Many<BaseEvent>} per entry — a stateful reactive
 * primitive that cannot be serialized or transferred across JVMs. Multi-instance support for this feature requires a
 * different design (Redis pub/sub for event fan-out + per-thread instance routing), not a serialized-value cache. So we
 * drop the abstraction and use {@code Caffeine} directly: same TTL / stats semantics, less indirection.
 * </p>
 *
 * <p>
 * <b>Buffer bound:</b> {@link #REPLAY_BUFFER_LIMIT} caps the per-run event history at a level that comfortably covers
 * even multi-minute autonomous-attach turns (each tool call emits ~5-10 events; a 50-tool turn is well under 1000).
 * Runs that overflow the cap drop the oldest events; new attachers see best-effort replay rather than full history. The
 * terminal event always arrives because it's emitted strictly after the buffer overflow window.
 * </p>
 *
 * <p>
 * <b>Eviction:</b> {@code expireAfterWrite(30, MINUTES)} is a backstop for orphaned runs that never reach RUN_FINISHED
 * / RUN_ERROR — normal turns complete in well under 5 minutes and the natural completion flow disposes the sink long
 * before TTL fires.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class InFlightAiHubRunRegistry {

    private static final Logger log = LoggerFactory.getLogger(InFlightAiHubRunRegistry.class);

    /**
     * Number of recent {@link BaseEvent} instances retained per run for late attachers. Sized to comfortably hold even
     * the chattiest autonomous-tool-attach turn.
     */
    static final int REPLAY_BUFFER_LIMIT = 1000;

    /**
     * Backstop TTL for entries that never reach a terminal event. Normal completion disposes the sink long before this
     * fires. 30 minutes matches the TTL used for other AI Hub registries in {@code CacheConfiguration}.
     */
    static final long ENTRY_TTL_MINUTES = 30L;

    /**
     * TTL for cancelled-run-id tombstones. A runId is single-use, so a stale tombstone can never affect an unrelated
     * future turn — this TTL is purely memory cleanup. The race it covers (a Stop reaching the server before the agent
     * POST registers the run) resolves in well under a second.
     */
    static final long CANCELLED_RUN_ID_TTL_MINUTES = 2L;

    private final Cache<String, InFlightRun> runs;
    private final Cache<String, Boolean> cancelledRunIds;

    public InFlightAiHubRunRegistry() {
        this(buildDefaultCache());
    }

    /**
     * Test-only constructor that accepts a pre-built cache. Lets unit tests inject shorter TTLs or no-stats caches
     * without exposing the default Caffeine config as part of the public surface.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    InFlightAiHubRunRegistry(Cache<String, InFlightRun> runs) {
        this.runs = runs;
        this.cancelledRunIds = buildCancelledRunIdCache();
    }

    /**
     * Registers a new in-flight run for {@code threadId}, returning the sink the producer must push events to. If a run
     * already exists for this thread and is still active, the existing run is returned unchanged — the caller is
     * expected to skip starting a duplicate agent invocation. This is the linchpin of the "POST during in-flight run"
     * race protection: a refresh that retriggers the runtime's POST sees the active run and gets reattached instead of
     * starting a second concurrent agent that would interleave events on the same thread.
     */
    public InFlightRun startRun(String threadId, String runId) {
        // compute() is atomic per-key in Caffeine, so the "check existing → reuse-or-replace" sequence is
        // safe under concurrent startRun calls. The previous synchronized-on-this version had the same
        // intent; the compute closure gives us the same guarantee without serialising all callers.
        return runs.asMap()
            .compute(threadId, (key, existing) -> {
                if (existing != null && !existing.isTerminated()) {
                    log.debug(
                        "startRun called for threadId={} but a run is already in flight (runId={}); returning existing",
                        threadId, existing.getRunId());

                    return existing;
                }

                // A terminated entry (retained within TTL for late-attach replay) must be evicted before
                // we start a fresh run. Otherwise the new run would inherit the old buffer and a late
                // attacher would see the prior turn's events bolted onto the new run's start.
                if (existing != null) {
                    existing.dispose();
                }

                InFlightRun run = new InFlightRun(threadId, runId);

                // A Stop click can reach cancel() before the agent POST reaches startRun(). cancel()
                // records the runId as a tombstone; honor it here so the run is born terminated rather
                // than streaming on. Without this the attach-on-return probe would resurrect a turn the
                // user explicitly stopped. asMap().remove makes the tombstone single-use.
                if (runId != null && cancelledRunIds.asMap()
                    .remove(runId) != null) {

                    log.debug("startRun for threadId={} runId={} matched a cancel tombstone; born terminated",
                        threadId, runId);

                    run.markTerminated();
                    run.completeSink();
                }

                return run;
            });
    }

    /**
     * Pushes an event into the run's replay sink. No-op if the run was evicted (TTL expired while the agent was still
     * producing) — losing a stray event is safer than throwing back into the agent's subscriber loop.
     */
    public void onEvent(String threadId, BaseEvent event) {
        InFlightRun run = runs.getIfPresent(threadId);

        if (run == null) {
            log.debug("onEvent for threadId={} but no run is registered; dropping", threadId);

            return;
        }

        run.emit(event);
    }

    /**
     * Marks the run complete. Subscribers receive the sink's complete signal so their emitters close cleanly; the cache
     * entry stays put until {@code expireAfterWrite} TTL fires, so a slow attach right after completion still finds the
     * entry and sees the terminal signal via the replay buffer.
     */
    public void complete(String threadId) {
        InFlightRun run = runs.getIfPresent(threadId);

        if (run == null) {
            return;
        }

        run.markTerminated();
        run.completeSink();
    }

    /**
     * Marks the run failed and propagates the error to subscribers. Same retention semantics as {@link #complete} — the
     * cache entry survives until TTL so a late attacher sees the error event and can render it instead of staring at a
     * perpetual spinner.
     */
    public void fail(String threadId, Throwable error) {
        InFlightRun run = runs.getIfPresent(threadId);

        if (run == null) {
            return;
        }

        run.markTerminated();
        run.errorSink(error);
    }

    /**
     * Equivalent to {@link #cancel(String, String)} with no runId — terminates a registered run but cannot defend
     * against the Stop-before-registration race. Retained for callers that have no runId to supply.
     */
    public boolean cancel(String threadId) {
        return cancel(threadId, null);
    }

    /**
     * Marks the run cancelled at the user's request and emits a {@code complete} signal to subscribers. Returns
     * {@code true} when a non-terminated run was actually cancelled, {@code false} when no live run existed (already
     * completed / never started / terminal entry within TTL). The agent's reactive subscription may keep running
     * server-side — the AGUI {@code agent.runAgent} call doesn't expose a {@code Disposable} we can cancel — but
     * subsequent events are dropped by {@link #onEvent} because the sink is in a terminal state, and a re-mount probe
     * via {@link #isInFlight} will see the run as not-in-flight so the client stops showing the streaming UI.
     *
     * <p>
     * When {@code runId} is supplied it is recorded as a tombstone regardless of whether a run is registered yet. This
     * closes the race where the Stop click reaches the server before the agent POST registers the run: the later
     * {@link #startRun} sees the tombstone and starts the run already-terminated. A runId is single-use, so the
     * tombstone can never affect an unrelated future turn.
     * </p>
     */
    public boolean cancel(String threadId, String runId) {
        if (runId != null && !runId.isBlank()) {
            cancelledRunIds.put(runId, Boolean.TRUE);
        }

        InFlightRun run = runs.getIfPresent(threadId);

        if (run == null || run.isTerminated()) {
            return false;
        }

        run.markTerminated();
        run.completeSink();

        return true;
    }

    /**
     * Returns a {@link Flux} that replays buffered events and then receives any subsequent events for this run. Empty
     * when no run exists. Callers (the HTTP SSE bridge) subscribe and write each event to their {@code SseEmitter};
     * cancelling the subscription is the right thing to do on client disconnect — other subscribers and the run itself
     * are unaffected.
     */
    public Optional<Flux<BaseEvent>> attach(String threadId) {
        InFlightRun run = runs.getIfPresent(threadId);

        if (run == null) {
            return Optional.empty();
        }

        return Optional.of(run.asFlux());
    }

    /**
     * Returns {@code true} if a non-terminated run exists for {@code threadId}. Used by the client's mount-time probe
     * to decide between "attach to existing stream" vs "start a fresh turn." A terminated-but-still-cached entry
     * reports {@code false} so the sidebar's hydrate probe doesn't paint a stuck pulse on a task that already settled.
     */
    public boolean isInFlight(String threadId) {
        InFlightRun run = runs.getIfPresent(threadId);

        return run != null && !run.isTerminated();
    }

    /**
     * Returns the thread ids that currently have non-terminated runs. Used by the sidebar's hydrate-on-mount probe to
     * paint a "running" pulse on every task the user has actively streaming, not just the focused one.
     */
    public Collection<String> getInFlightThreadIds() {
        List<String> result = new ArrayList<>();

        runs.asMap()
            .forEach((threadId, run) -> {
                if (!run.isTerminated()) {
                    result.add(threadId);
                }
            });

        return result;
    }

    private static Cache<String, InFlightRun> buildDefaultCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(ENTRY_TTL_MINUTES, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }

    private static Cache<String, Boolean> buildCancelledRunIdCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(CANCELLED_RUN_ID_TTL_MINUTES, TimeUnit.MINUTES)
            .build();
    }

    /**
     * Per-thread run state. Holds the {@link Sinks.Many replay sink} that producers push to and subscribers attach to,
     * plus enough bookkeeping for the registry to decide when to evict.
     */
    public static class InFlightRun {

        private final String threadId;
        private final String runId;
        private final Instant startedAt;
        private final Sinks.Many<BaseEvent> sink;
        private volatile boolean terminated;

        InFlightRun(String threadId, String runId) {
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

        public String getRunId() {
            return runId;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public boolean isTerminated() {
            return terminated;
        }

        void emit(BaseEvent event) {
            // tryEmitNext returns FAIL on overflow / cancellation; we don't have a meaningful recovery and don't
            // want to throw back into the agent's reactive subscriber loop, so a debug log is the right escape.
            Sinks.EmitResult result = sink.tryEmitNext(event);

            if (result.isFailure()) {
                log.debug("Failed to emit event for threadId={} runId={}: {}", threadId, runId, result);
            }
        }

        void completeSink() {
            sink.tryEmitComplete();
        }

        void errorSink(Throwable error) {
            sink.tryEmitError(error);
        }

        void markTerminated() {
            terminated = true;
        }

        Flux<BaseEvent> asFlux() {
            return sink.asFlux();
        }

        void dispose() {
            // Replay sinks don't expose dispose() directly; emitting complete on an already-terminated sink is a
            // no-op and ensures any remaining subscribers receive the terminal signal before GC reclaims it.
            sink.tryEmitComplete();
        }
    }
}
