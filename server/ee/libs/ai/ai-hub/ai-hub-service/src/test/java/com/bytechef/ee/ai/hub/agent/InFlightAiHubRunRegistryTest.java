/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.event.BaseEvent;
import com.agui.core.event.RunStartedEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.type.EventType;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry.InFlightRun;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Pin the behaviours the AI Hub resume / multi-stream features depend on. Each test exercises one user-visible scenario
 * named in the assertion comment.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class InFlightAiHubRunRegistryTest {

    @Test
    void testAttachAfterFirstEventReplaysHistoryThenReceivesLiveTail() {
        // Scenario: user refreshes mid-stream. The original emitter is dead; the second emitter via /attach
        // must see (a) the events that already happened, and (b) every subsequent event.
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.startRun("thread-1", "run-1");

        BaseEvent first = new RunStartedEvent();
        BaseEvent second = textContent("hel");

        run.emit(first);
        run.emit(second);

        Flux<BaseEvent> attached = registry.attach("thread-1")
            .orElseThrow();

        BaseEvent third = textContent("lo");

        StepVerifier.create(attached)
            .then(() -> {
                run.emit(third);
                registry.complete("thread-1");
                run.completeSink();
            })
            .assertNext(event -> assertThat(event.getType()).isEqualTo(EventType.RUN_STARTED))
            .assertNext(event -> assertThat(((TextMessageContentEvent) event).getDelta()).isEqualTo("hel"))
            .assertNext(event -> assertThat(((TextMessageContentEvent) event).getDelta()).isEqualTo("lo"))
            .verifyComplete();
    }

    @Test
    void testSecondStartRunForSameThreadReturnsExistingRunNotDuplicate() {
        // Scenario: refresh fires POST again while a run is still in flight. The registry must hand back the
        // existing run unchanged — otherwise we'd start a second concurrent agent invocation and interleave
        // events on the same thread.
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun first = registry.startRun("thread-1", "run-1");
        InFlightRun second = registry.startRun("thread-1", "run-2");

        assertThat(second).isSameAs(first);
        assertThat(second.getRunId()).isEqualTo("run-1");
    }

    @Test
    void testStartRunReplacesTerminatedEntryEvictedSoNewBufferStartsEmpty() {
        // Scenario: a turn completed, the entry is retained briefly for late attaches; THEN the user sends a
        // new message. The new run must start with a clean buffer — otherwise a late attacher on the new run
        // would see the prior turn's events bolted onto the new run's start.
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun first = registry.startRun("thread-1", "run-1");

        first.emit(textContent("stale"));
        registry.complete("thread-1");

        InFlightRun second = registry.startRun("thread-1", "run-2");

        assertThat(second).isNotSameAs(first);
        assertThat(second.getRunId()).isEqualTo("run-2");

        BaseEvent fresh = textContent("fresh");

        second.emit(fresh);

        List<BaseEvent> replayed = new ArrayList<>();

        registry.attach("thread-1")
            .orElseThrow()
            .take(Duration.ofMillis(100))
            .doOnNext(replayed::add)
            .blockLast(Duration.ofMillis(200));

        assertThat(replayed)
            .extracting(event -> ((TextMessageContentEvent) event).getDelta())
            .containsExactly("fresh");
    }

    @Test
    void testIsInFlightFalseAfterCompletion() {
        // Scenario: the sidebar's hydrate probe runs after the turn completed. It must report false so the
        // running pulse clears. Retention of the registry entry for late-attach replay must not lie to the
        // probe.
        InFlightAiHubRunRegistry registry = newRegistry();

        registry.startRun("thread-1", "run-1");

        assertThat(registry.isInFlight("thread-1")).isTrue();

        registry.complete("thread-1");

        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    @Test
    void testGetInFlightThreadIdsReturnsOnlyActiveRuns() {
        // Scenario: the sidebar wants to paint a pulse on each task currently streaming. A finished thread
        // that's still in the retention window must NOT appear — otherwise the user sees a stuck running
        // pulse on a task that already settled.
        InFlightAiHubRunRegistry registry = newRegistry();

        registry.startRun("thread-1", "run-1");
        registry.startRun("thread-2", "run-2");
        registry.startRun("thread-3", "run-3");

        registry.complete("thread-2");

        assertThat(registry.getInFlightThreadIds()).containsExactlyInAnyOrder("thread-1", "thread-3");
    }

    @Test
    void testAttachReturnsEmptyWhenNoRunRegistered() {
        // Scenario: client mounts and probes for a thread that has no run. The attach endpoint must surface
        // this as not-found so the client falls back to history-only rendering, not as an empty stream that
        // never produces a terminal signal.
        InFlightAiHubRunRegistry registry = newRegistry();

        Optional<Flux<BaseEvent>> attached = registry.attach("ghost-thread");

        assertThat(attached).isEmpty();
    }

    @Test
    void testEmitForUnknownThreadIsSilentNoOp() {
        // Scenario: agent emits an event for a thread that's already been evicted (slow producer + eager
        // cleanup race). Throwing back into the agent's reactive subscriber would tear down the run
        // pointlessly — drop the event silently.
        InFlightAiHubRunRegistry registry = newRegistry();

        registry.onEvent("unknown-thread", textContent("orphan"));

        assertThat(registry.isInFlight("unknown-thread")).isFalse();
    }

    @Test
    void testCompletePropagatesTerminalSignalToActiveSubscribers() {
        // Scenario: run finishes while an attach subscriber is reading the stream. The subscriber must
        // receive Reactor's onComplete so its emitter closes cleanly — otherwise the client's EventSource
        // hangs open until the OS times out the socket.
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.startRun("thread-1", "run-1");

        StepVerifier.create(run.asFlux())
            .then(() -> {
                run.emit(textContent("partial"));
                registry.complete("thread-1");
            })
            .assertNext(event -> assertThat(((TextMessageContentEvent) event).getDelta()).isEqualTo("partial"))
            .verifyComplete();
    }

    @Test
    void testFailPropagatesErrorToActiveSubscribers() {
        // Scenario: agent threw mid-stream. Subscribers see the error event so the client can render it
        // instead of a perpetual spinner.
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.startRun("thread-1", "run-1");

        IllegalStateException failure = new IllegalStateException("boom");

        StepVerifier.create(run.asFlux())
            .then(() -> registry.fail("thread-1", failure))
            .verifyErrorMatches(throwable -> throwable == failure);
    }

    @Test
    void testCancelMarksRunTerminatedAndCompletesSubscribers() {
        // Scenario: user clicks Stop in the chat composer. The registry must mark the entry terminated
        // (so a subsequent isInFlight probe returns false and the client stops showing the streaming UI)
        // and emit complete to any live subscribers (so their SSE emitters close cleanly).
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.startRun("thread-1", "run-1");

        StepVerifier.create(run.asFlux())
            .then(() -> {
                run.emit(textContent("partial"));
                boolean cancelled = registry.cancel("thread-1");

                assertThat(cancelled).isTrue();
            })
            .assertNext(event -> assertThat(((TextMessageContentEvent) event).getDelta()).isEqualTo("partial"))
            .verifyComplete();

        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    @Test
    void testCancelBeforeStartRunTombstonesRunIdSoTheRaceLosingRunIsBornTerminated() {
        // Scenario: user clicks Stop before the agent POST registered its run. cancel() finds no run
        // but tombstones the runId; the racing startRun() must then produce an already-terminated run
        // so the attach-on-return probe won't resurrect a turn the user stopped.
        InFlightAiHubRunRegistry registry = newRegistry();

        boolean cancelled = registry.cancel("thread-1", "run-1");

        assertThat(cancelled).isFalse();

        InFlightRun run = registry.startRun("thread-1", "run-1");

        assertThat(run.isTerminated()).isTrue();
        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    @Test
    void testCancelTombstoneIsRunIdScopedSoADeliberateNewTurnStillStarts() {
        // Scenario: user stops a turn, then sends a fresh message on the same thread. The new turn
        // carries a different runId and must NOT be killed by the previous turn's tombstone.
        InFlightAiHubRunRegistry registry = newRegistry();

        registry.cancel("thread-1", "run-1");

        InFlightRun run = registry.startRun("thread-1", "run-2");

        assertThat(run.isTerminated()).isFalse();
        assertThat(registry.isInFlight("thread-1")).isTrue();
    }

    @Test
    void testCancelWithRunIdStillTerminatesAnAlreadyRegisteredRun() {
        // Scenario: the common case — Stop arrives after the run registered. cancel(threadId, runId)
        // terminates the live run exactly as the single-arg overload does.
        InFlightAiHubRunRegistry registry = newRegistry();

        registry.startRun("thread-1", "run-1");

        boolean cancelled = registry.cancel("thread-1", "run-1");

        assertThat(cancelled).isTrue();
        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    @Test
    void testCancelReturnsFalseForUnknownOrAlreadyTerminatedRun() {
        // Idempotent cancel — clicking Stop after the run has already finished must be a clean no-op,
        // not a registry-state mutation. Returning false lets the client disambiguate "we cancelled
        // something" from "there was nothing to cancel" without exception-handling gymnastics.
        InFlightAiHubRunRegistry registry = newRegistry();

        assertThat(registry.cancel("unknown-thread")).isFalse();

        registry.startRun("thread-1", "run-1");
        registry.complete("thread-1");

        assertThat(registry.cancel("thread-1")).isFalse();
    }

    /**
     * Builds a registry backed by a freshly-constructed Caffeine cache. Injecting a fresh cache per test keeps each
     * test isolated from the default-cache state that would persist across tests if we used the no-arg constructor. The
     * 30-minute TTL is irrelevant within a single test method — no test runs long enough to exercise it.
     */
    private static InFlightAiHubRunRegistry newRegistry() {
        Cache<String, InFlightRun> caffeineCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

        return new InFlightAiHubRunRegistry(caffeineCache);
    }

    private static TextMessageContentEvent textContent(String delta) {
        TextMessageContentEvent event = new TextMessageContentEvent();

        event.setMessageId("m-1");
        event.setDelta(delta);

        return event;
    }
}
