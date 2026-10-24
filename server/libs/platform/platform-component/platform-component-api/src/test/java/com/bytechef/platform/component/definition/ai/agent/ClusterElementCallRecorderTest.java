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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.component.definition.ai.agent.ClusterElementCall.Status;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallEvent.Type;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        ClusterElementCallRef ref = start(recorder, null);

        recorder.callCompleted(1, 10, ref, null);

        Map<String, ClusterElementCall> calls = recorder.drain(10)
            .calls();

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
        recorder.callFailed(1, 10, start(recorder, null), "boom");

        ClusterElementCallRef second = start(recorder, null);

        recorder.callCompleted(1, 10, second, null);

        ClusterElementCall call = recorder.drain(10)
            .calls()
            .get("httpClient_1");

        assertEquals(Status.FAILED, call.status());
        assertEquals("boom", call.error());
    }

    @Test
    void testDrainRemoves() {
        start(recorder, null);

        recorder.drain(10);

        ClusterElementCallDrain drain = recorder.drain(10);

        assertTrue(drain.calls()
            .isEmpty());
        assertTrue(drain.records()
            .isEmpty());
    }

    @Test
    void testListenerReceivesTransitions() throws Exception {
        List<ClusterElementCallEvent> events = new CopyOnWriteArrayList<>();
        List<ClusterElementCallEvent> otherJobEvents = new CopyOnWriteArrayList<>();

        AutoCloseable handle = recorder.addListener(1, events::add);

        recorder.addListener(2, otherJobEvents::add);

        recorder.callCompleted(1, 10, start(recorder, null), null);

        handle.close();

        start(recorder, null);

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

        assertDoesNotThrow(() -> start(recorder, null));
    }

    @Test
    void testConcurrentCallsOnOneElement() throws Exception {
        int threadCount = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        try {
            for (int index = 0; index < threadCount; index++) {
                executorService.execute(() -> {
                    recorder.callCompleted(1, 10, start(recorder, null), null);

                    countDownLatch.countDown();
                });
            }

            assertTrue(countDownLatch.await(10, TimeUnit.SECONDS));
        } finally {
            executorService.shutdownNow();
        }

        ClusterElementCallDrain drain = recorder.drain(10);

        assertEquals(Status.COMPLETED, drain.calls()
            .get("httpClient_1")
            .status());
        assertEquals(threadCount, drain.records()
            .size());
    }

    @Test
    void testOverlappingCallsStayStartedUntilTheLastOneEnds() {
        start(recorder, null);

        ClusterElementCallRef second = start(recorder, null);

        recorder.callCompleted(1, 10, second, null);

        assertEquals(Status.STARTED, recorder.drain(10)
            .calls()
            .get("httpClient_1")
            .status());

        ClusterElementCallRef third = start(recorder, null);
        ClusterElementCallRef fourth = start(recorder, null);

        recorder.callCompleted(1, 10, third, null);
        recorder.callCompleted(1, 10, fourth, null);

        assertEquals(Status.COMPLETED, recorder.drain(10)
            .calls()
            .get("httpClient_1")
            .status());
    }

    @Test
    void testEventTypeFollowsTheMergedStatus() {
        List<ClusterElementCallEvent> events = new CopyOnWriteArrayList<>();

        recorder.addListener(1, events::add);

        ClusterElementCallRef first = start(recorder, null);
        ClusterElementCallRef second = start(recorder, null);

        recorder.callCompleted(1, 10, first, null);
        recorder.callFailed(1, 10, second, "boom");

        ClusterElementCallRef third = start(recorder, null);

        recorder.callCompleted(1, 10, third, null);

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
        ClusterElementCallRef ref = new ClusterElementCallRef("id-1", "httpClient_1");

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
            executorService.execute(() -> start(recorder, null));

            assertTrue(inStartedListener.await(10, TimeUnit.SECONDS));

            Future<?> completed = executorService.submit(() -> recorder.callCompleted(1, 10, ref, null));

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

        start(recorder, null);

        TenantContext.setCurrentTenantId("tenantB");

        assertTrue(recorder.drain(10)
            .calls()
            .isEmpty());

        TenantContext.setCurrentTenantId("tenantA");

        assertEquals(1, recorder.drain(10)
            .calls()
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

        start(expiringRecorder, null);

        nanos.addAndGet(Duration.ofHours(2)
            .toNanos());

        assertTrue(expiringRecorder.drain(10)
            .records()
            .isEmpty());
    }

    @Test
    void testRecordsEachCallWithItsInputAndOutput() {
        ClusterElementCallRef first =
            recorder.callStarted(1, 10, "httpClient_1", "httpClient", null, null, "{\"a\":1}");
        ClusterElementCallRef second =
            recorder.callStarted(1, 10, "httpClient_1", "httpClient", null, null, "{\"a\":2}");

        recorder.callCompleted(1, 10, first, "out1");
        recorder.callCompleted(1, 10, second, "out2");

        List<ClusterElementCallRecord> records = recorder.drain(10)
            .records();

        assertEquals(2, records.size());
        assertNotEquals(records.get(0)
            .id(),
            records.get(1)
                .id());
        assertEquals(first.id(), records.get(0)
            .id());
        assertEquals(second.id(), records.get(1)
            .id());
        assertEquals(UUID.fromString(first.id())
            .toString(), first.id());

        assertEquals("{\"a\":1}", records.get(0)
            .input());
        assertEquals("out1", records.get(0)
            .output());
        assertEquals("{\"a\":2}", records.get(1)
            .input());
        assertEquals("out2", records.get(1)
            .output());

        for (ClusterElementCallRecord callRecord : records) {
            assertEquals("httpClient", callRecord.componentName());
            assertEquals(ClusterElementCallRecord.Status.COMPLETED, callRecord.status());
        }

        assertTrue(recorder.drain(10)
            .records()
            .isEmpty());
    }

    @Test
    void testFailedCallKeepsItsError() {
        recorder.callFailed(1, 10, start(recorder, null), "boom");

        ClusterElementCallRecord callRecord = recorder.drain(10)
            .records()
            .getFirst();

        assertEquals(ClusterElementCallRecord.Status.FAILED, callRecord.status());
        assertEquals("boom", callRecord.error());
        assertNotNull(callRecord.endDate());
    }

    @Test
    void testRecordsTheParentCall() {
        ClusterElementCallRef parent = start(recorder, null);

        recorder.callStarted(1, 10, "httpClient_2", "httpClient", "get", parent.id(), null);

        List<ClusterElementCallRecord> records = recorder.drain(10)
            .records();

        assertNull(records.get(0)
            .parentId());
        assertEquals(parent.id(), records.get(1)
            .parentId());
        assertEquals("get", records.get(1)
            .toolName());
    }

    @Test
    void testSuspendedCallIsFoundAndStoredAsSuspended() {
        ClusterElementCallRef ref = start(recorder, null);

        recorder.callSuspended(1, 10, ref);

        assertEquals(ref, recorder.findSuspendedCall(10)
            .orElseThrow());
        assertTrue(recorder.findSuspendedCall(11)
            .isEmpty());

        assertEquals(ClusterElementCallRecord.Status.SUSPENDED, recorder.drain(10)
            .records()
            .getFirst()
            .status());
    }

    @Test
    void testCompletingAnUnknownCallRecordsAPartialRecord() {
        recorder.callCompleted(1, 10, new ClusterElementCallRef("id-1", "httpClient_1"), "out");

        ClusterElementCallRecord callRecord = recorder.drain(10)
            .records()
            .getFirst();

        assertEquals("id-1", callRecord.id());
        assertEquals("httpClient_1", callRecord.workflowNodeName());
        assertEquals(ClusterElementCallRecord.Status.COMPLETED, callRecord.status());
        assertEquals("out", callRecord.output());
        assertNotNull(callRecord.endDate());
        assertNull(callRecord.startDate());
    }

    @Test
    void testEventsCarryTheCallRecord() {
        List<ClusterElementCallEvent> events = new CopyOnWriteArrayList<>();

        recorder.addListener(1, events::add);

        ClusterElementCallRef ref = recorder.callStarted(1, 10, "httpClient_1", "httpClient", null, null, "in");

        recorder.callCompleted(1, 10, ref, "out");

        assertEquals("in", events.get(0)
            .record()
            .input());
        assertEquals("out", events.get(1)
            .record()
            .output());
        assertEquals(events.get(0)
            .record()
            .id(),
            events.get(1)
                .record()
                .id());
    }

    @Test
    void testCapsRecordedValues() {
        recorder.callStarted(1, 10, "httpClient_1", "httpClient", null, null, "x".repeat(70_000));

        assertEquals(
            ClusterElementCallRecord.cap("x".repeat(70_000)), recorder.drain(10)
                .records()
                .getFirst()
                .input());
    }

    private static ClusterElementCallRef start(ClusterElementCallRecorder recorder, String input) {
        return recorder.callStarted(1, 10, "httpClient_1", "httpClient", null, null, input);
    }
}
