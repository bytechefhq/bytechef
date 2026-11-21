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
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
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
    void testRetireThreadCancelsTheRunATryStartInstallsWhileTheRetireRaces() throws Exception {
        CountDownLatch insideComputeLatch = new CountDownLatch(1);
        CountDownLatch releaseComputeLatch = new CountDownLatch(1);

        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry(
            blockingFirstComputeCache(insideComputeLatch, releaseComputeLatch));

        AtomicReference<Optional<InFlightRun>> startedRunReference = new AtomicReference<>();

        Thread tryStartThread = new Thread(
            () -> startedRunReference.set(registry.tryStart("thread-1", "run-1")), "try-start");
        Thread retireThread = new Thread(() -> registry.retireThread("thread-1"), "retire");

        tryStartThread.start();

        assertThat(insideComputeLatch.await(5, TimeUnit.SECONDS)).isTrue();

        retireThread.start();

        assertThat(awaitBlocked(retireThread))
            .as("the retire thread must block on the entry tryStart holds before the race is released")
            .isTrue();

        releaseComputeLatch.countDown();

        tryStartThread.join(5_000);
        retireThread.join(5_000);

        Optional<InFlightRun> startedRun = startedRunReference.get();

        assertThat(startedRun).isPresent();
        assertThat(startedRun.get()
            .isTerminated()).isTrue();
        assertThat(registry.tryStart("thread-1", "run-2")).isEmpty();
    }

    @Test
    void testAThreadIdOfAnotherTenantNeitherAttachesToNorBlocksItsRun() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();

        Optional<InFlightRun> tenantRun = TenantContext.callWithTenantId(
            "tenant_a", () -> registry.tryStart("thread-1", "run-1"));

        assertThat(tenantRun).isPresent();

        TenantContext.runWithTenantId("tenant_b", () -> {
            assertThat(registry.attach("thread-1")).isEmpty();
            assertThat(registry.isInFlight("thread-1")).isFalse();
            assertThat(registry.cancel("thread-1")).isFalse();
            assertThat(registry.tryStart("thread-1", "run-2")).isPresent();
        });

        assertThat(tenantRun.get()
            .isTerminated()).isFalse();
        assertThat(TenantContext.callWithTenantId("tenant_a", () -> registry.isInFlight("thread-1"))).isTrue();
    }

    @Test
    void testReleaseActionsRunOnTheReleaseExecutorNotOnTheReleasingThread() {
        List<Runnable> scheduledActions = new ArrayList<>();
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry(scheduledActions::add);
        List<String> ranActions = new ArrayList<>();

        InFlightRun run = registry.tryStart("thread-held", "run-1")
            .orElseThrow();

        registry.runWhenReleased("thread-held", () -> ranActions.add("held"));

        run.complete();

        assertThat(ranActions).isEmpty();
        assertThat(scheduledActions).hasSize(1);

        scheduledActions.getFirst()
            .run();

        assertThat(ranActions).containsExactly("held");
    }

    @Test
    void testAReleaseActionQueuedAfterTheThreadRetiredIsRefused() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        List<String> ranActions = new ArrayList<>();

        InFlightRun run = registry.tryStart("thread-held", "run-1")
            .orElseThrow();

        registry.retireThread("thread-held");
        registry.runWhenReleased("thread-held", () -> ranActions.add("held"));

        run.complete();

        registry.runWhenReleased("thread-held", () -> ranActions.add("after-release"));
        registry.runWhenReleased("thread-free", () -> ranActions.add("free"));

        assertThat(ranActions).containsExactly("free");
    }

    @Test
    void testTryStartOnARetiredThreadFailsUntilItIsReinstated() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();

        registry.retireThread("thread-1");

        assertThat(registry.tryStart("thread-1", "run-1")).isEmpty();
        assertThat(registry.isInFlight("thread-1")).isFalse();

        registry.reinstateThread("thread-1");

        assertThat(registry.tryStart("thread-1", "run-2")).isPresent();
    }

    @Test
    void testDiscardedReleaseActionsNeverRun() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        List<String> ranActions = new ArrayList<>();

        InFlightRun run = registry.tryStart("thread-held", "run-1")
            .orElseThrow();

        registry.runWhenReleased("thread-held", () -> ranActions.add("held"));
        registry.retireThread("thread-held");

        run.complete();

        assertThat(ranActions).isEmpty();
    }

    @Test
    void testRunWhenReleasedRunsAtOnceForAFreeThreadAndOtherwiseWhenTheHoldingRunEnds() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        List<String> ranActions = new ArrayList<>();

        registry.runWhenReleased("thread-free", () -> ranActions.add("free"));

        assertThat(ranActions).containsExactly("free");

        InFlightRun run = registry.tryStart("thread-held", "run-1")
            .orElseThrow();

        registry.runWhenReleased("thread-held", () -> ranActions.add("held"));

        assertThat(ranActions).containsExactly("free");

        run.complete();

        assertThat(ranActions).containsExactly("free", "held");
    }

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
    }

    @Test
    void testACancelledRunHoldsTheThreadUntilItsAgentStops() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        registry.cancel("thread-1");

        assertThat(registry.isInFlight("thread-1")).isTrue();
        assertThat(registry.tryStart("thread-1", "run-2")).isEmpty();

        run.complete();

        assertThat(registry.isInFlight("thread-1")).isFalse();
        assertThat(registry.tryStart("thread-1", "run-2")).isPresent();
    }

    @Test
    void testACancelledRunWhoseAgentNeverStopsReleasesTheThreadAfterTheDrainTimeout() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        registry.cancel("thread-1");

        assertThat(run.holdsThread(Instant.now()
            .plus(InFlightAiHubRunRegistry.DRAIN_TIMEOUT)
            .plusSeconds(1))).isFalse();
    }

    @Test
    void testCancelSignalsTheStopOfTheRunsAgent() {
        InFlightAiHubRunRegistry registry = newRegistry();

        registry.tryStart("thread-1", "run-1");

        StepVerifier.create(registry.stopRequested("thread-1"))
            .then(() -> registry.cancel("thread-1"))
            .verifyComplete();
    }

    @Test
    void testStopIsNeverRequestedForAThreadWithNoRun() {
        StepVerifier.create(newRegistry().stopRequested("ghost-thread"))
            .expectSubscription()
            .expectNoEvent(Duration.ofMillis(50))
            .thenCancel()
            .verify();
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
        assertThat(registry.isInFlight("thread-1")).isTrue();
    }

    @Test
    void testADelayedCancelOfAFinishedRunDoesNotCancelTheThreadsNextRun() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun firstRun = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        firstRun.complete();

        InFlightRun nextRun = registry.tryStart("thread-1", "run-2")
            .orElseThrow();

        assertThat(registry.cancel("thread-1", "run-1")).isFalse();
        assertThat(nextRun.isTerminated()).isFalse();
        assertThat(registry.isInFlight("thread-1")).isTrue();
    }

    @Test
    void testCancelWithoutRunIdCancelsTheRegisteredRunWhateverItsId() {
        InFlightAiHubRunRegistry registry = newRegistry();

        InFlightRun run = registry.tryStart("thread-1", "run-1")
            .orElseThrow();

        assertThat(registry.cancel("thread-1", null)).isTrue();
        assertThat(run.isTerminated()).isTrue();
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

        cancelledRun.complete();

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

    @SuppressWarnings("unchecked")
    private static Cache<String, InFlightRun> blockingFirstComputeCache(
        CountDownLatch insideComputeLatch, CountDownLatch releaseComputeLatch) {

        Cache<String, InFlightRun> cache = Caffeine.newBuilder()
            .build();
        ConcurrentMap<String, InFlightRun> map = cache.asMap();
        AtomicBoolean blocked = new AtomicBoolean();

        ConcurrentMap<String, InFlightRun> blockingMap = (ConcurrentMap<String, InFlightRun>) Proxy.newProxyInstance(
            InFlightAiHubRunRegistryTest.class.getClassLoader(), new Class<?>[] {
                ConcurrentMap.class
            }, (proxy, method, arguments) -> {
                if ("compute".equals(method.getName())) {
                    BiFunction<String, InFlightRun, InFlightRun> remapping =
                        (BiFunction<String, InFlightRun, InFlightRun>) arguments[1];

                    return map.compute((String) arguments[0], (key, value) -> {
                        InFlightRun result = remapping.apply(key, value);

                        if (blocked.compareAndSet(false, true)) {
                            insideComputeLatch.countDown();

                            awaitLatch(releaseComputeLatch);
                        }

                        return result;
                    });
                }

                return method.invoke(map, arguments);
            });

        return (Cache<String, InFlightRun>) Proxy.newProxyInstance(
            InFlightAiHubRunRegistryTest.class.getClassLoader(), new Class<?>[] {
                Cache.class
            }, (proxy, method, arguments) -> "asMap".equals(method.getName())
                ? blockingMap : method.invoke(cache, arguments));
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(exception);
        }
    }

    private static boolean awaitBlocked(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while (thread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }

        return thread.getState() == Thread.State.BLOCKED;
    }
}
