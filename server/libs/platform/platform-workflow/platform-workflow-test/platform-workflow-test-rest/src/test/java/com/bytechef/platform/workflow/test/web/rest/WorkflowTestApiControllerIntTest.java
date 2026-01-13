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

package com.bytechef.platform.workflow.test.web.rest;

import static com.bytechef.platform.workflow.test.dto.TaskStatusEventDTO.Status.COMPLETED;
import static com.bytechef.platform.workflow.test.dto.TaskStatusEventDTO.Status.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.test.dto.ExecutionErrorEventDTO;
import com.bytechef.platform.workflow.test.dto.JobStatusEventDTO;
import com.bytechef.platform.workflow.test.dto.TaskStatusEventDTO;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import com.bytechef.platform.workflow.test.facade.TestWorkflowExecutor;
import com.bytechef.platform.workflow.test.web.rest.model.WorkflowTestExecutionModel;
import com.github.benmanes.caffeine.cache.Cache;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@WebMvcTest(value = WorkflowTestApiController.class)
class WorkflowTestApiControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TempFileStorage tempFileStorage;

    @MockitoBean
    private TestWorkflowExecutor testWorkflowExecutor;

    @Autowired
    private WorkflowTestApiController controller;

    @BeforeEach
    public void beforeEach() {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void testStartStreamEmitsStartAndResult() throws Exception {
        doAnswer(inv -> {
            Consumer<String> afterStartCallback = inv.getArgument(3);
            Function<String, SseStreamBridge> sseStreamBridgeFactory = inv.getArgument(4);
            BiConsumer<String, CompletableFuture<WorkflowTestExecutionDTO>> afterFutureCallback = inv.getArgument(5);
            Consumer<String> whenCompleteCallback = inv.getArgument(6);

            String key = "test-key";

            afterStartCallback.accept(key);

            SseStreamBridge bridge = sseStreamBridgeFactory.apply(key);

            CompletableFuture<WorkflowTestExecutionDTO> future = CompletableFuture.completedFuture(
                new WorkflowTestExecutionDTO(new JobDTO(new Job()), null));

            afterFutureCallback.accept(key, future);
            bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", "123")));
            bridge.onEvent(
                Map.of("event", "result", "payload", new WorkflowTestExecutionDTO(new JobDTO(new Job()), null)));
            whenCompleteCallback.accept(key);

            return null;
        }).when(testWorkflowExecutor)
            .executeAsync(eq("wf-1"), any(), eq(1L), any(), any(), any(), any());

        var mvcResult = mockMvc.perform(
            post("/internal/workflows/{id}/tests", "wf-1")
                .queryParam("environmentId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();

        mvcResult.getAsyncResult(10000);

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        MockHttpServletResponse response = mvcResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        // Should include the structured start event with jobId and a result event
        assertThat(body).contains("event:start");
        assertThat(body).contains("\"jobId\":\"123\"");
        assertThat(body).contains("event:result");
    }

    @Test
    void testStopAbortsActiveStreamAndInvokesFacade() throws Exception {
        long jobId = 456L;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> keyRef = new AtomicReference<>();

        doAnswer(inv -> {
            Consumer<String> afterStartCallback = inv.getArgument(3);
            Function<String, SseStreamBridge> sseStreamBridgeFactory = inv.getArgument(4);
            BiConsumer<String, CompletableFuture<WorkflowTestExecutionDTO>> afterFutureCallback = inv.getArgument(5);
            Consumer<String> whenCompleteCallback = inv.getArgument(6);

            String key = "test-key-" + jobId;

            keyRef.set(key);

            afterStartCallback.accept(key);

            SseStreamBridge bridge = sseStreamBridgeFactory.apply(key);

            CompletableFuture<WorkflowTestExecutionDTO> future = CompletableFuture.supplyAsync(() -> {
                try {
                    latch.await(1, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }

                return new WorkflowTestExecutionDTO(new JobDTO(new Job()), null);
            });

            future.whenComplete((result, throwable) -> whenCompleteCallback.accept(key));
            afterFutureCallback.accept(key, future);
            bridge.onEvent(Map.of("event", "start", "jobId", String.valueOf(jobId)));

            return null;
        }).when(testWorkflowExecutor)
            .executeAsync(eq("wf-2"), any(), eq(1L), any(), any(), any(), any());

        // Fire the start request asynchronously (so the controller creates the run)
        CompletableFuture<MvcResult> startFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(
                    post("/internal/workflows/{id}/tests", "wf-2")
                        .queryParam("environmentId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Give the controller a tiny bit of time to register the run
        Duration duration = Duration.ofMillis(50);

        Thread.sleep(duration.toMillis());

        mockMvc.perform(post("/internal/workflow-tests/{jobId}/stop", jobId))
            .andExpect(status().isOk());

        // Give the controller a brief moment to flush the error event before unblocking the await
        Thread.sleep(duration.toMillis());

        // Unblock await so request can finish
        latch.countDown();

        // Ensure the HTTP request completed without error and capture its body
        MvcResult mvcResult = startFuture.get(10, TimeUnit.SECONDS);

        mvcResult.getAsyncResult(10000);

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        String bodyAfterStop = mvcResult.getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        String captured = normalizeSse(bodyAfterStop);

        assertThat(captured).contains("event:");

        verify(testWorkflowExecutor, times(1)).stop(eq(jobId));
    }

    @Test
    void testStopWorkflowTestWithUndefinedJobId() throws Exception {
        mockMvc.perform(
            post("/internal/workflow-tests/{jobId}/stop", "undefined"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testAttachWhenNotRunningEmitsErrorNotRunning() throws Exception {
        var mvcResult = mockMvc.perform(get("/internal/workflow-tests/{jobId}/attach", 999L)
            .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("event:error");
        assertThat(body).contains("Not running");
    }

    @Test
    void testListenerForwardingEmitsJobTaskAndErrorEvents() throws Exception {
        long jobId = 777L;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SseStreamBridge> bridgeRef = new AtomicReference<>();

        doAnswer(inv -> {
            Consumer<String> afterStartCallback = inv.getArgument(3);
            Function<String, SseStreamBridge> sseStreamBridgeFactory = inv.getArgument(4);
            BiConsumer<String, CompletableFuture<WorkflowTestExecutionDTO>> afterFutureCallback = inv.getArgument(5);
            Consumer<String> whenCompleteCallback = inv.getArgument(6);

            String key = "test-key-" + jobId;

            afterStartCallback.accept(key);

            SseStreamBridge bridge = sseStreamBridgeFactory.apply(key);

            bridgeRef.set(bridge);

            CompletableFuture<WorkflowTestExecutionDTO> future = CompletableFuture.supplyAsync(() -> {
                try {
                    latch.await(1, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }

                return new WorkflowTestExecutionDTO(new JobDTO(new Job()), null);
            });

            future.whenComplete((result, throwable) -> whenCompleteCallback.accept(key));
            afterFutureCallback.accept(key, future);
            bridge.onEvent(Map.of("event", "start", "jobId", String.valueOf(jobId)));

            return null;
        }).when(testWorkflowExecutor)
            .executeAsync(eq("wf-3"), any(), eq(1L), any(), any(), any(), any());

        CompletableFuture<MvcResult> startFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(
                    post("/internal/workflows/{id}/tests", "wf-3")
                        .queryParam("environmentId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Give time for the controller to initialize
        for (int i = 0; i < 10; i++) {
            if (bridgeRef.get() != null) {
                break;
            }

            Thread.sleep(50);
        }

        SseStreamBridge sseStreamBridge = bridgeRef.get();

        assertThat(sseStreamBridge).isNotNull();

        sseStreamBridge
            .onEvent(Map.of("event", "job", "payload", new JobStatusEventDTO(jobId, "STARTED", Instant.now())));
        sseStreamBridge.onEvent(Map.of("event", "task", "payload",
            new TaskStatusEventDTO(jobId, 1L, STARTED, null, null, Instant.now(), null)));
        sseStreamBridge.onEvent(Map.of("event", "task", "payload",
            new TaskStatusEventDTO(jobId, 1L, COMPLETED, "t", "type", null, Instant.now())));
        sseStreamBridge.onEvent(Map.of("event", "error", "payload", new ExecutionErrorEventDTO(jobId, "Oops")));
        sseStreamBridge.onEvent("Chunk 1");

        // Allow a brief moment for SSE forwarding to flush before finishing
        Duration duration = Duration.ofMillis(50);

        Thread.sleep(duration.toMillis());

        latch.countDown();

        // Ensure HTTP request completes and capture its body
        MvcResult mvcResult = startFuture.get(3, TimeUnit.SECONDS);

        mvcResult.getAsyncResult(10000);

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        String finalBody = mvcResult.getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        String captured = normalizeSse(finalBody);

        assertThat(captured).contains("event:");
        assertThat(captured).contains("\"Chunk 1\"");
    }

    @Test
    void testPendingEventsBoundedAndFlushedOnAttach() throws Exception {
        long jobId = 888L;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> actualKeyRef = new AtomicReference<>();

        doAnswer(inv -> {
            Consumer<String> afterStartCallback = inv.getArgument(3);
            Function<String, SseStreamBridge> sseStreamBridgeFactory = inv.getArgument(4);
            BiConsumer<String, CompletableFuture<WorkflowTestExecutionDTO>> afterFutureCallback = inv.getArgument(5);
            Consumer<String> whenCompleteCallback = inv.getArgument(6);

            String key = String.valueOf(jobId);

            actualKeyRef.set(key);

            afterStartCallback.accept(key);

            SseStreamBridge actualBridge = sseStreamBridgeFactory.apply(key);

            CompletableFuture<WorkflowTestExecutionDTO> future = CompletableFuture.supplyAsync(() -> {
                try {
                    latch.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }

                return new WorkflowTestExecutionDTO(new JobDTO(new Job()), null);
            });

            future.whenComplete((result, throwable) -> whenCompleteCallback.accept(key));
            afterFutureCallback.accept(key, future);
            actualBridge.onEvent(Map.of("event", "start", "jobId", String.valueOf(jobId)));

            return null;
        }).when(testWorkflowExecutor)
            .executeAsync(eq("wf-4"), any(), eq(1L), any(), any(), any(), any());

        CompletableFuture<Void> startFuture = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(
                    post("/internal/workflows/{id}/tests", "wf-4")
                        .queryParam("environmentId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{}"))
                    .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Give it time to initialize internal structures
        Duration duration = Duration.ofMillis(150);

        Thread.sleep(duration.toMillis());

        String key = waitForRunKey();

        Object emitter = ReflectionTestUtils.getField(controller, "emitter");

        assert emitter != null;

        if (emitter instanceof Cache<?, ?> cache) {
            @SuppressWarnings("unchecked")
            Cache<String, SseEmitter> emitterCache = (Cache<String, SseEmitter>) cache;

            emitterCache.invalidate(key);
        } else if (emitter instanceof ConcurrentMap<?, ?>) {
            @SuppressWarnings("unchecked")
            ConcurrentMap<String, SseEmitter> emitterMap = (ConcurrentMap<String, SseEmitter>) emitter;

            emitterMap.remove(key);
        }

        for (int i = 1; i <= 5; i++) {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name("log")
                .data("B" + i);

            ReflectionTestUtils.invokeMethod(controller, "sendToEmitter", key, eventBuilder);
        }

        Object pendingEvents = ReflectionTestUtils.getField(controller, "pendingEvents");

        assert pendingEvents != null;

        List<SseEmitter.SseEventBuilder> eventBuilders = null;

        if (pendingEvents instanceof Cache<?, ?> cache) {
            @SuppressWarnings("unchecked")
            Cache<String, List<SseEmitter.SseEventBuilder>> c = (Cache<String, List<SseEmitter.SseEventBuilder>>) cache;

            eventBuilders = c.getIfPresent(key);
        } else if (pendingEvents instanceof ConcurrentMap<?, ?>) {
            @SuppressWarnings("unchecked")
            ConcurrentMap<String, List<SseEmitter.SseEventBuilder>> m =
                (ConcurrentMap<String, List<SseEmitter.SseEventBuilder>>) pendingEvents;

            eventBuilders = m.get(key);
        }
        assertThat(eventBuilders).hasSize(3);

        var attachResult = mockMvc.perform(get("/internal/workflow-tests/{key}/attach", key)
            .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk())
            .andReturn();

        latch.countDown();

        MockHttpServletResponse response = attachResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        String normalized = normalizeSse(body);

        assertThat(normalized).isNotEmpty();

        // Ensure start request finished cleanly
        startFuture.get(3, TimeUnit.SECONDS);
    }

    @Test
    void testPendingEventsClearedOnStop() throws Exception {
        long jobId = 889L;
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(inv -> {
            Consumer<String> afterStartCallback = inv.getArgument(3);
            Function<String, SseStreamBridge> sseStreamBridgeFactory = inv.getArgument(4);
            BiConsumer<String, CompletableFuture<WorkflowTestExecutionDTO>> afterFutureCallback = inv.getArgument(5);
            Consumer<String> whenCompleteCallback = inv.getArgument(6);

            String key = "test-key-" + jobId;

            afterStartCallback.accept(key);

            SseStreamBridge bridge = sseStreamBridgeFactory.apply(key);

            CompletableFuture<WorkflowTestExecutionDTO> future = CompletableFuture.supplyAsync(() -> {
                try {
                    latch.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }

                return new WorkflowTestExecutionDTO(new JobDTO(new Job()), null);
            });

            future.whenComplete((result, throwable) -> whenCompleteCallback.accept(key));
            afterFutureCallback.accept(key, future);
            bridge.onEvent(Map.of("event", "start", "jobId", String.valueOf(jobId)));

            return null;
        }).when(testWorkflowExecutor)
            .executeAsync(eq("wf-5"), any(), eq(1L), any(), any(), any(), any());

        CompletableFuture<Void> startFuture = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(
                    post("/internal/workflows/{id}/tests", "wf-5")
                        .queryParam("environmentId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{}"))
                    .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(Duration.ofMillis(75)
            .toMillis());

        String key = waitForRunKey();

        Object emitter = ReflectionTestUtils.getField(controller, "emitter");

        assert emitter != null;

        if (emitter instanceof Cache<?, ?> cache) {
            @SuppressWarnings("unchecked")
            Cache<String, SseEmitter> emitterCache = (Cache<String, SseEmitter>) cache;

            emitterCache.invalidate(key);
        } else if (emitter instanceof ConcurrentMap<?, ?>) {
            @SuppressWarnings("unchecked")
            ConcurrentMap<String, SseEmitter> emitterMap = (ConcurrentMap<String, SseEmitter>) emitter;

            emitterMap.remove(key);
        }

        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
            .name("log")
            .data("X1");

        ReflectionTestUtils.invokeMethod(controller, "sendToEmitter", key, eventBuilder);

        Object pendingEvents2 = ReflectionTestUtils.getField(controller, "pendingEvents");

        assert pendingEvents2 != null;

        List<SseEmitter.SseEventBuilder> beforeStop = null;

        if (pendingEvents2 instanceof Cache<?, ?> cache) {
            @SuppressWarnings("unchecked")
            Cache<String, List<SseEmitter.SseEventBuilder>> c =
                (Cache<String, List<SseEmitter.SseEventBuilder>>) cache;

            beforeStop = c.getIfPresent(key);
        } else if (pendingEvents2 instanceof ConcurrentMap<?, ?>) {
            @SuppressWarnings("unchecked")
            ConcurrentMap<String, List<SseEmitter.SseEventBuilder>> m =
                (ConcurrentMap<String, List<SseEmitter.SseEventBuilder>>) pendingEvents2;

            beforeStop = m.get(key);
        }

        assertThat(beforeStop).isNotNull();

        mockMvc.perform(post("/internal/workflow-tests/{jobId}/stop", jobId))
            .andExpect(status().isOk());

        // Give a brief moment for cleanup
        Thread.sleep(Duration.ofMillis(100)
            .toMillis());

        // After stop, pending events should eventually be cleared (or contain only the final error event)
        boolean clearedOrMinimal = false;

        for (int i = 0; i < 5; i++) {
            List<SseEmitter.SseEventBuilder> afterStop;

            if (pendingEvents2 instanceof Cache<?, ?> cache3) {
                @SuppressWarnings("unchecked")
                Cache<String, List<SseEmitter.SseEventBuilder>> c3 =
                    (Cache<String, List<SseEmitter.SseEventBuilder>>) cache3;

                afterStop = c3.getIfPresent(key);
            } else {
                @SuppressWarnings("unchecked")
                ConcurrentMap<String, List<SseEmitter.SseEventBuilder>> m3 =
                    (ConcurrentMap<String, List<SseEmitter.SseEventBuilder>>) pendingEvents2;

                afterStop = m3.get(key);
            }

            if (afterStop == null || afterStop.isEmpty() || afterStop.size() <= 1) {
                clearedOrMinimal = true;

                break;
            }

            Thread.sleep(20);
        }

        assertThat(clearedOrMinimal).isTrue();

        latch.countDown();
        startFuture.get(3, TimeUnit.SECONDS);
    }

    private static String normalizeSse(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }

        return body.replaceAll("(?m)^(event:)(\\s*)", "$1 ");
    }

    @Configuration
    public static class WorkflowTestApiControllerTestConfiguration {

        @Bean
        Converter<WorkflowTestExecutionDTO, WorkflowTestExecutionModel> workflowTestExecutionDtoToModelConverter() {
            return source -> new WorkflowTestExecutionModel();
        }

        @Bean
        WorkflowTestApiController workflowTestApiController(
            TempFileStorage tempFileStorage,
            TestWorkflowExecutor testWorkflowExecutor) {

            // Use a small buffer to make bounded behavior easy to assert in tests
            return new WorkflowTestApiController(tempFileStorage, testWorkflowExecutor, 3);
        }
    }

    @Test
    void testFutureCancellationMechanism() throws Exception {
        long jobId = 999L;
        CountDownLatch executionStarted = new CountDownLatch(1);
        CountDownLatch futureRegistered = new CountDownLatch(1);
        CountDownLatch allowCancellation = new CountDownLatch(1);
        AtomicReference<CompletableFuture<WorkflowTestExecutionDTO>> futureRef = new AtomicReference<>();

        doAnswer(inv -> {
            Consumer<String> afterStartCallback = inv.getArgument(3);
            Function<String, SseStreamBridge> sseStreamBridgeFactory = inv.getArgument(4);
            BiConsumer<String, CompletableFuture<WorkflowTestExecutionDTO>> afterFutureCallback = inv.getArgument(5);
            Consumer<String> whenCompleteCallback = inv.getArgument(6);

            String key = "test-key-" + jobId;

            afterStartCallback.accept(key);

            SseStreamBridge bridge = sseStreamBridgeFactory.apply(key);

            CompletableFuture<WorkflowTestExecutionDTO> future = CompletableFuture.supplyAsync(() -> {
                executionStarted.countDown();

                try {
                    allowCancellation.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                }

                return new WorkflowTestExecutionDTO(new JobDTO(new Job()), null);
            });

            future.whenComplete((result, throwable) -> {
                if (throwable instanceof CancellationException) {
                    bridge.onEvent(Map.of("event", "error", "payload", "Cancelled"));
                }

                whenCompleteCallback.accept(key);
            });

            futureRef.set(future);
            futureRegistered.countDown();
            afterFutureCallback.accept(key, future);
            bridge.onEvent(Map.of("event", "start", "jobId", String.valueOf(jobId)));

            return null;
        }).when(testWorkflowExecutor)
            .executeAsync(eq("wf-cancel-test"), any(), eq(1L), any(), any(), any(), any());

        // Start the async execution
        CompletableFuture<MvcResult> startFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(
                    post("/internal/workflows/{id}/tests", "wf-cancel-test")
                        .queryParam("environmentId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        boolean registered = futureRegistered.await(5, TimeUnit.SECONDS);

        assertThat(registered).isTrue();

        boolean started = executionStarted.await(5, TimeUnit.SECONDS);

        assertThat(started).isTrue();

        CompletableFuture<WorkflowTestExecutionDTO> future = futureRef.get();

        assertThat(future).isNotNull();

        boolean cancelled = future.cancel(true);

        assertThat(cancelled).isTrue();

        allowCancellation.countDown();

        // Give time for the whenComplete handler to process the cancellation
        Thread.sleep(Duration.ofMillis(200)
            .toMillis());

        startFuture.get(5, TimeUnit.SECONDS);

        assertThat(future.isCancelled()).isTrue();
    }

    private String waitForRunKey() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1000;

        while (System.currentTimeMillis() < deadline) {
            Object runs = ReflectionTestUtils.getField(controller, "workflowExecutions");

            if (runs instanceof Cache<?, ?> cache) {
                @SuppressWarnings("unchecked")
                Cache<String, CompletableFuture<WorkflowTestExecutionModel>> r =
                    (Cache<String, CompletableFuture<WorkflowTestExecutionModel>>) cache;

                ConcurrentMap<String, CompletableFuture<WorkflowTestExecutionModel>> map = r.asMap();

                if (!map.isEmpty()) {
                    Set<String> keySet = map.keySet();

                    Iterator<String> iterator = keySet.iterator();

                    return iterator.next();
                }
            } else if (runs instanceof ConcurrentMap<?, ?>) {
                @SuppressWarnings("unchecked")
                ConcurrentMap<String, CompletableFuture<WorkflowTestExecutionModel>> r =
                    (ConcurrentMap<String, CompletableFuture<WorkflowTestExecutionModel>>) runs;

                if (!r.isEmpty()) {
                    Set<String> keySet = r.keySet();

                    Iterator<String> iterator = keySet.iterator();

                    return iterator.next();
                }
            }

            Thread.sleep(10);
        }

        throw new AssertionError("Run key not available in time");
    }
}
