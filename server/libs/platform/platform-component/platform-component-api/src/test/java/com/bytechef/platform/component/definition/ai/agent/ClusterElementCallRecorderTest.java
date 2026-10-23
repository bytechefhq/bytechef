/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.definition.ai.agent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.component.definition.ai.agent.ClusterElementCall.Status;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallEvent.Type;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClusterElementCallRecorderTest {

    private final ClusterElementCallRecorder recorder = new ClusterElementCallRecorder();

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testStartedThenCompleted() {
        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");

        Map<String, ClusterElementCall> calls = recorder.drain(10);

        assertEquals(1, calls.size());

        ClusterElementCall call = calls.get("httpClient_1");

        assertEquals(Status.COMPLETED, call.status());
        assertNotNull(call.startDate());
        assertNotNull(call.endDate());
        assertFalse(call.endDate()
            .isBefore(call.startDate()));
    }

    @Test
    void testFailedIsSticky() {
        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callFailed(1, 10, "httpClient_1", "boom");
        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");

        ClusterElementCall call = recorder.drain(10)
            .get("httpClient_1");

        assertEquals(Status.FAILED, call.status());
        assertEquals("boom", call.error());
    }

    @Test
    void testDrainRemoves() {
        recorder.callStarted(1, 10, "httpClient_1");

        recorder.drain(10);

        assertTrue(recorder.drain(10)
            .isEmpty());
    }

    @Test
    void testListenerReceivesTransitions() throws Exception {
        List<ClusterElementCallEvent> events = new CopyOnWriteArrayList<>();
        List<ClusterElementCallEvent> otherJobEvents = new CopyOnWriteArrayList<>();

        AutoCloseable handle = recorder.addListener(1, events::add);

        recorder.addListener(2, otherJobEvents::add);

        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");

        handle.close();

        recorder.callStarted(1, 10, "httpClient_1");

        assertEquals(
            List.of(Type.STARTED, Type.COMPLETED),
            events.stream()
                .map(ClusterElementCallEvent::type)
                .toList());

        for (ClusterElementCallEvent event : events) {
            assertEquals("httpClient_1", event.call()
                .workflowNodeName());
            assertEquals(10, event.taskExecutionId());
        }

        assertTrue(otherJobEvents.isEmpty());
    }

    @Test
    void testListenerFailureDoesNotPropagate() {
        recorder.addListener(1, event -> {
            throw new IllegalStateException("listener failed");
        });

        assertDoesNotThrow(() -> recorder.callStarted(1, 10, "httpClient_1"));
    }

    @Test
    void testConcurrentCallsOnOneElement() throws Exception {
        int threadCount = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        try {
            for (int index = 0; index < threadCount; index++) {
                executorService.execute(() -> {
                    recorder.callStarted(1, 10, "httpClient_1");
                    recorder.callCompleted(1, 10, "httpClient_1");

                    countDownLatch.countDown();
                });
            }

            assertTrue(countDownLatch.await(10, TimeUnit.SECONDS));
        } finally {
            executorService.shutdownNow();
        }

        assertEquals(Status.COMPLETED, recorder.drain(10)
            .get("httpClient_1")
            .status());
    }

    @Test
    void testOverlappingCallsStayStartedUntilTheLastOneEnds() {
        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");

        assertEquals(Status.STARTED, recorder.drain(10)
            .get("httpClient_1")
            .status());

        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");

        assertEquals(Status.COMPLETED, recorder.drain(10)
            .get("httpClient_1")
            .status());
    }

    @Test
    void testEventTypeFollowsTheMergedStatus() {
        List<ClusterElementCallEvent> events = new CopyOnWriteArrayList<>();

        recorder.addListener(1, events::add);

        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");
        recorder.callFailed(1, 10, "httpClient_1", "boom");
        recorder.callStarted(1, 10, "httpClient_1");
        recorder.callCompleted(1, 10, "httpClient_1");

        for (ClusterElementCallEvent event : events) {
            assertEquals(event.type()
                .name(),
                event.call()
                    .status()
                    .name());
        }

        assertEquals(
            List.of(Type.STARTED, Type.STARTED, Type.STARTED, Type.FAILED, Type.FAILED, Type.FAILED),
            events.stream()
                .map(ClusterElementCallEvent::type)
                .toList());
    }

    @Test
    void testListenersSeeTransitionsInMergeOrder() throws Exception {
        List<Type> types = new CopyOnWriteArrayList<>();
        CountDownLatch inStartedListener = new CountDownLatch(1);
        CountDownLatch releaseStartedListener = new CountDownLatch(1);

        recorder.addListener(1, event -> {
            if (event.type() == Type.STARTED) {
                inStartedListener.countDown();

                try {
                    if (!releaseStartedListener.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Started listener was never released");
                    }
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();
                }
            }

            types.add(event.type());
        });

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            executorService.execute(() -> recorder.callStarted(1, 10, "httpClient_1"));

            assertTrue(inStartedListener.await(10, TimeUnit.SECONDS));

            Future<?> completed = executorService.submit(() -> recorder.callCompleted(1, 10, "httpClient_1"));

            try {
                completed.get(300, TimeUnit.MILLISECONDS);
            } catch (TimeoutException timeoutException) {
                assertFalse(completed.isDone());
            }

            releaseStartedListener.countDown();

            completed.get(10, TimeUnit.SECONDS);
        } finally {
            executorService.shutdownNow();
        }

        assertEquals(List.of(Type.STARTED, Type.COMPLETED), types);
    }

    @Test
    void testTenantsAreSeparate() {
        TenantContext.setCurrentTenantId("tenantA");

        recorder.callStarted(1, 10, "httpClient_1");

        TenantContext.setCurrentTenantId("tenantB");

        assertTrue(recorder.drain(10)
            .isEmpty());

        TenantContext.setCurrentTenantId("tenantA");

        assertEquals(1, recorder.drain(10)
            .size());
    }

    @Test
    void testMapRoundTrip() {
        ClusterElementCall call = new ClusterElementCall(
            "httpClient_1", Status.FAILED, Instant.parse("2026-10-23T10:00:00Z"),
            Instant.parse("2026-10-23T10:00:00.412Z"), "boom");

        assertEquals(call, ClusterElementCall.fromMap(call.toMap()));
    }

    @Test
    void testMerge() {
        Instant firstStart = Instant.parse("2026-10-23T10:00:00Z");
        Instant secondEnd = Instant.parse("2026-10-23T10:00:05Z");

        Map<String, ClusterElementCall> existing = Map.of(
            "a", new ClusterElementCall("a", Status.FAILED, firstStart, firstStart.plusSeconds(1), "boom"));
        Map<String, ClusterElementCall> incoming = Map.of(
            "a", new ClusterElementCall("a", Status.COMPLETED, firstStart.plusSeconds(2), secondEnd, null),
            "b", new ClusterElementCall("b", Status.COMPLETED, firstStart.plusSeconds(3), secondEnd, null));

        Map<String, ClusterElementCall> merged = ClusterElementCallRecorder.merge(existing, incoming);

        ClusterElementCall mergedA = merged.get("a");

        assertEquals(Status.FAILED, mergedA.status());
        assertEquals(firstStart, mergedA.startDate());
        assertEquals(secondEnd, mergedA.endDate());
        assertEquals("boom", mergedA.error());
        assertEquals(Status.COMPLETED, merged.get("b")
            .status());
    }

    @Test
    void testUndrainedCallsExpire() {
        AtomicLong nanos = new AtomicLong();

        ClusterElementCallRecorder expiringRecorder = new ClusterElementCallRecorder(nanos::get);

        expiringRecorder.callStarted(1, 10, "httpClient_1");

        nanos.addAndGet(Duration.ofHours(2)
            .toNanos());

        assertTrue(expiringRecorder.drain(10)
            .isEmpty());
    }
}
