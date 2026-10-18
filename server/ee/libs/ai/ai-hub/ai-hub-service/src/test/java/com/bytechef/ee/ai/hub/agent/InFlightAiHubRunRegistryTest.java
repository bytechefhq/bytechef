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
 * @version ee
 *
 * @author Ivica Cardic
 */
class InFlightAiHubRunRegistryTest {

    @Test
    void testAttachAfterFirstEventReplaysHistoryThenReceivesLiveTail() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        run.emit(new RunStartedEvent());
        run.emit(textContent("hel"));

        Flux<BaseEvent> attached = registry.attach("thread-1")
            .orElseThrow();

        StepVerifier.create(attached)
            .then(() -> {
                run.emit(textContent("lo"));
                run.complete();
            })
            .assertNext(event -> assertThat(event.getType()).isEqualTo(EventType.RUN_STARTED))
            .assertNext(event -> assertThat(((TextMessageContentEvent) event).getDelta()).isEqualTo("hel"))
            .assertNext(event -> assertThat(((TextMessageContentEvent) event).getDelta()).isEqualTo("lo"))
            .verifyComplete();
    }

    @Test
    void testSecondTryStartForTheSameThreadIsRefusedWhileTheFirstIsInFlight() {
        InFlightAiHubRunRegistry registry = newRegistry();

        Optional<InFlightRun> first = registry.tryStart("thread-1", "run-1");
        Optional<InFlightRun> second = registry.tryStart("thread-1", "run-2");

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(registry.isInFlight("thread-1")).isTrue();
    }

    @Test
    void testTryStartOnAnotherThreadIsIndependent() {
        InFlightAiHubRunRegistry registry = newRegistry();

        registry.tryStart("thread-1", "run-1");

        assertThat(registry.tryStart("thread-2", "run-2")).isPresent();
    }

    @Test
    void testTryStartReplacesATerminatedEntrySoTheNewBufferStartsEmpty() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun first = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        first.emit(textContent("stale"));
        first.complete();

        InFlightRun second = registry.tryStart("thread-1", "run-2")
            .orElseThrow();

        assertThat(second).isNotSameAs(first);
        assertThat(second.getRunId()).isEqualTo("run-2");

        second.emit(textContent("fresh"));

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
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        assertThat(registry.isInFlight("thread-1")).isTrue();

        run.complete();

        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    @Test
    void testAttachReturnsEmptyWhenNoRunRegistered() {
        InFlightAiHubRunRegistry registry = newRegistry();

        Optional<Flux<BaseEvent>> attached = registry.attach("ghost-thread");

        assertThat(attached).isEmpty();
    }

    @Test
    void testCompletePropagatesTerminalSignalToActiveSubscribers() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        StepVerifier.create(run.asFlux())
            .then(() -> {
                run.emit(textContent("partial"));
                run.complete();
            })
            .assertNext(event -> assertThat(((TextMessageContentEvent) event).getDelta()).isEqualTo("partial"))
            .verifyComplete();
    }

    @Test
    void testFailPropagatesErrorToActiveSubscribers() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        IllegalStateException failure = new IllegalStateException("boom");

        StepVerifier.create(run.asFlux())
            .then(() -> run.fail(failure))
            .verifyErrorMatches(throwable -> throwable == failure);

        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    @Test
    void testCancelMarksRunTerminatedAndCompletesSubscribers() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        StepVerifier.create(run.asFlux())
            .then(() -> {
                run.emit(textContent("partial"));

                assertThat(registry.cancel("thread-1")).isTrue();
            })
            .assertNext(event -> assertThat(((TextMessageContentEvent) event).getDelta()).isEqualTo("partial"))
            .verifyComplete();

        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    @Test
    void testCancelBeforeTryStartTombstonesRunIdSoTheRaceLosingRunIsBornTerminated() {
        InFlightAiHubRunRegistry registry = newRegistry();

        assertThat(registry.cancel("thread-1", "run-1")).isFalse();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        assertThat(run.isTerminated()).isTrue();
        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    @Test
    void testCancelTombstoneIsRunIdScopedSoADeliberateNewTurnStillStarts() {
        InFlightAiHubRunRegistry registry = newRegistry();

        registry.cancel("thread-1", "run-1");

        InFlightRun run = registry.tryStart("thread-1", "run-2")
            .orElseThrow();

        assertThat(run.isTerminated()).isFalse();
        assertThat(registry.isInFlight("thread-1")).isTrue();
    }

    @Test
    void testCancelWithRunIdStillTerminatesAnAlreadyRegisteredRun() {
        InFlightAiHubRunRegistry registry = newRegistry();

        registry.tryStart("thread-1", "run-1");

        assertThat(registry.cancel("thread-1", "run-1")).isTrue();
        assertThat(registry.isInFlight("thread-1")).isFalse();
    }

    @Test
    void testCancelReturnsFalseForUnknownOrAlreadyTerminatedRun() {
        InFlightAiHubRunRegistry registry = newRegistry();

        assertThat(registry.cancel("unknown-thread")).isFalse();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        run.complete();

        assertThat(registry.cancel("thread-1")).isFalse();
    }

    @Test
    void testEventsOfACancelledRunDoNotReachTheNextRunOnTheSameThread() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun cancelledRun = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        registry.cancel("thread-1");

        InFlightRun nextRun = registry.tryStart("thread-1", "run-2")
            .orElseThrow();

        cancelledRun.emit(textContent("late"));
        nextRun.emit(textContent("current"));

        List<BaseEvent> replayed = new ArrayList<>();

        registry.attach("thread-1")
            .orElseThrow()
            .take(Duration.ofMillis(100))
            .doOnNext(replayed::add)
            .blockLast(Duration.ofMillis(200));

        assertThat(replayed)
            .extracting(event -> ((TextMessageContentEvent) event).getDelta())
            .containsExactly("current");
    }

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
