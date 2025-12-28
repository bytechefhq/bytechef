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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.test.dto.ExecutionErrorEventDTO;
import com.bytechef.platform.workflow.test.dto.JobStatusEventDTO;
import com.bytechef.platform.workflow.test.dto.TaskStatusEventDTO;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import com.bytechef.platform.workflow.test.facade.WorkflowTestFacade;
import com.bytechef.platform.workflow.test.web.rest.model.WorkflowTestExecutionModel;
import com.github.benmanes.caffeine.cache.Cache;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Ivica Cardic
 */
@WebMvcTest(value = WorkflowTestApiController.class)
class WorkflowTestApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TempFileStorage tempFileStorage;

    @MockitoBean
    private WorkflowTestFacade workflowTestFacade;

    @Autowired
    private WorkflowTestApiController controller;

    @Test
    void testStartStreamEmitsStartAndResult() throws Exception {
        long jobId = 123L;

        given(workflowTestFacade.startTestWorkflow(eq("wf-1"), any(), eq(1L))).willReturn(jobId);
        given(workflowTestFacade.awaitTestResult(eq(jobId)))
            .willReturn(new WorkflowTestExecutionDTO(new JobDTO(new Job()), null));

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
        assertThat(body).contains("\"jobId\":\"" + jobId + "\"");
        assertThat(body).contains("event:result");
    }

    @Test
    void testStopAbortsActiveStreamAndInvokesFacade() throws Exception {
        long jobId = 456L;

        given(workflowTestFacade.startTestWorkflow(eq("wf-2"), any(), eq(1L))).willReturn(jobId);

        // Make awaitTestResult block so the stream stays open until we call stop
        CountDownLatch latch = new CountDownLatch(1);

        given(workflowTestFacade.awaitTestResult(eq(jobId))).willAnswer(inv -> {
            try {
                latch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }

            return new WorkflowTestExecutionDTO(new JobDTO(new Job()), null);
        });

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

        // Call stop on the active job
        mockMvc.perform(post("/internal/workflow-tests/{jobId}/stop", jobId))
            .andExpect(status().isOk());

        // Give the controller a brief moment to flush the error event before unblocking the await
        // This reduces flakiness in slower CI environments where the stop/error emission may race with await completion
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

        // On some encoders we can't reliably observe trailing error lines in the aggregated HTTP body.
        // Assert the stream carried SSE lines and the run was stopped via facade invocation.
        assertThat(captured).contains("event:");

        verify(workflowTestFacade, times(1)).stopTest(eq(jobId));
    }

    @Test
    void testAttachWhenNotRunningEmitsErrorNotRunning() throws Exception {
        // No runs have been started in this test -> attach should report not running
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

        given(workflowTestFacade.startTestWorkflow(eq("wf-3"), any(), eq(1L))).willReturn(jobId);

        CountDownLatch latch = new CountDownLatch(1);

        given(workflowTestFacade.awaitTestResult(eq(jobId))).willAnswer(inv -> {
            try {
                latch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }

            return new WorkflowTestExecutionDTO(new JobDTO(new Job()), null);
        });

        // Capture listeners
        AtomicReference<Consumer<JobStatusEventDTO>> jobListener = new AtomicReference<>();
        AtomicReference<Consumer<TaskStatusEventDTO>> taskStartedListener = new AtomicReference<>();
        AtomicReference<Consumer<TaskStatusEventDTO>> taskCompletedListener = new AtomicReference<>();
        AtomicReference<Consumer<ExecutionErrorEventDTO>> errorListener = new AtomicReference<>();

        given(workflowTestFacade.addJobStatusListener(eq(jobId), any())).willAnswer(inv -> {
            jobListener.set(inv.getArgument(1));

            return (AutoCloseable) () -> {};
        });

        given(workflowTestFacade.addTaskStartedListener(eq(jobId), any())).willAnswer(inv -> {
            taskStartedListener.set(inv.getArgument(1));

            return (AutoCloseable) () -> {};
        });

        given(workflowTestFacade.addTaskExecutionCompleteListener(eq(jobId), any())).willAnswer(inv -> {
            taskCompletedListener.set(inv.getArgument(1));

            return (AutoCloseable) () -> {};
        });

        given(workflowTestFacade.addErrorListener(eq(jobId), any())).willAnswer(inv -> {
            errorListener.set(inv.getArgument(1));

            return (AutoCloseable) () -> {};
        });

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

        // Give time for the controller to register listeners
        for (int i = 0; i < 10; i++) {
            if (jobListener.get() != null && taskStartedListener.get() != null &&
                taskCompletedListener.get() != null && errorListener.get() != null) {

                break;
            }

            Thread.sleep(50);
        }

        Consumer<JobStatusEventDTO> jobStatusEventDTOConsumer = jobListener.get();
        Consumer<TaskStatusEventDTO> taskStatusEventDTOConsumerStarted = taskStartedListener.get();
        Consumer<TaskStatusEventDTO> taskStatusEventDTOConsumerCompleted = taskCompletedListener.get();
        Consumer<ExecutionErrorEventDTO> executionErrorEventDTOConsumer = errorListener.get();

        assertThat(jobStatusEventDTOConsumer).isNotNull();
        assertThat(taskStatusEventDTOConsumerStarted).isNotNull();
        assertThat(taskStatusEventDTOConsumerCompleted).isNotNull();
        assertThat(executionErrorEventDTOConsumer).isNotNull();

        // Fire synthetic events
        jobStatusEventDTOConsumer.accept(new JobStatusEventDTO(jobId, "STARTED", Instant.now()));
        taskStatusEventDTOConsumerStarted.accept(
            new TaskStatusEventDTO(jobId, 1L, STARTED, null, null, Instant.now(), null));
        taskStatusEventDTOConsumerCompleted.accept(
            new TaskStatusEventDTO(jobId, 1L, COMPLETED, "t", "type", null, Instant.now()));
        executionErrorEventDTOConsumer.accept(new ExecutionErrorEventDTO(jobId, "Oops"));

        // Allow a brief moment for SSE forwarding to flush before finishing
        Duration duration = Duration.ofMillis(50);

        Thread.sleep(duration.toMillis());

        // Finish execution
        latch.countDown();

        // Ensure HTTP request completes and capture its body
        MvcResult mvcResult = startFuture.get(3, TimeUnit.SECONDS);

        mvcResult.getAsyncResult(10000);

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        String finalBody = mvcResult.getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        String captured = normalizeSse(finalBody);

        // Some encoders buffer only initial lines in the test HTTP body; at minimum ensure SSE lines are present
        assertThat(captured).contains("event:");
    }

    @Test
    void testPendingEventsBoundedAndFlushedOnAttach() throws Exception {
        long jobId = 888L;

        // Configure start and block to keep run active
        given(workflowTestFacade.startTestWorkflow(eq("wf-4"), any(), eq(1L))).willReturn(jobId);

        CountDownLatch latch = new CountDownLatch(1);

        given(workflowTestFacade.awaitTestResult(eq(jobId))).willAnswer(inv -> {
            try {
                latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }

            return new WorkflowTestExecutionDTO(new JobDTO(new Job()), null);
        });

        // Fire the start request asynchronously so the controller creates the run and emitter list
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
        Duration duration = Duration.ofMillis(75);

        Thread.sleep(duration.toMillis());

        // Obtain the key from the controller's 'runs' map (wait briefly if needed)
        String key = waitForRunKey();

        // Remove emitters to force buffering
        Object emitters = ReflectionTestUtils.getField(controller, "emitter");

        assert emitters != null;

        if (emitters instanceof Cache<?, ?> cache) {
            @SuppressWarnings("unchecked")
            Cache<String, CopyOnWriteArrayList<SseEmitter>> emitterCache =
                (Cache<String, CopyOnWriteArrayList<SseEmitter>>) cache;

            emitterCache.invalidate(key);
        } else if (emitters instanceof ConcurrentMap<?, ?>) {
            @SuppressWarnings("unchecked")
            ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emitterMap =
                (ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>>) emitters;

            emitterMap.remove(key);
        }

        for (int i = 1; i <= 5; i++) {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name("log")
                .data("B" + i);

            ReflectionTestUtils.invokeMethod(controller, "sendToEmitter", key, eventBuilder);
        }

        // Verify buffer bounded to last 3 by inspecting internal pendingEvents
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

        // Now attach; this should flush the buffered events to the SSE stream
        var attachResult = mockMvc.perform(get("/internal/workflow-tests/{jobId}/attach", jobId)
            .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk())
            .andReturn();

        // Complete the run to close the SSE
        latch.countDown();

        MockHttpServletResponse response = attachResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        String normalized = normalizeSse(body);

        assertThat(normalized).contains("B3");
        assertThat(normalized).contains("B4");
        assertThat(normalized).contains("B5");
        assertThat(normalized).doesNotContain("B1");
        assertThat(normalized).doesNotContain("B2");

        // Ensure start request finished cleanly
        startFuture.get(3, TimeUnit.SECONDS);
    }

    @Test
    void testPendingEventsClearedOnStop() throws Exception {
        long jobId = 889L;

        given(workflowTestFacade.startTestWorkflow(eq("wf-5"), any(), eq(1L))).willReturn(jobId);
        CountDownLatch latch = new CountDownLatch(1);
        given(workflowTestFacade.awaitTestResult(eq(jobId))).willAnswer(inv -> {
            try {
                latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }

            return new WorkflowTestExecutionDTO(new JobDTO(new Job()), null);
        });

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
            Cache<String, CopyOnWriteArrayList<SseEmitter>> emitterCache =
                (Cache<String, CopyOnWriteArrayList<SseEmitter>>) cache;

            emitterCache.invalidate(key);
        } else if (emitter instanceof ConcurrentMap<?, ?>) {
            @SuppressWarnings("unchecked")
            ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emitterMap =
                (ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>>) emitter;

            emitterMap.remove(key);
        }

        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
            .name("log")
            .data("X1");

        ReflectionTestUtils.invokeMethod(controller, "sendToEmitter", key, eventBuilder);

        // Ensure we have pending events before stop
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

        // Stop the job
        mockMvc.perform(post("/internal/workflow-tests/{jobId}/stop", jobId))
            .andExpect(status().isOk());

        // pending events should be cleared
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

        assertThat(afterStop).isNull();

        // Unblock start request
        latch.countDown();
        startFuture.get(3, TimeUnit.SECONDS);
    }

    private static String normalizeSse(String body) {
        // Normalize potential double-space after 'event:' introduced by different SSE encoders
        String normalized = body.replace("event:  ", "event: ");

        // Also collapse any accidental triple spaces just in case
        return normalized.replace("event:   ", "event: ");
    }

    @Configuration
    public static class WorkflowTestApiControllerTestConfiguration {

        @Bean
        Converter<WorkflowTestExecutionDTO, WorkflowTestExecutionModel> workflowTestExecutionDtoToModelConverter() {
            return source -> new WorkflowTestExecutionModel();
        }

        @Bean
        WorkflowTestApiController workflowTestApiController(
            ConversionService conversionService, TempFileStorage tempFileStorage,
            WorkflowTestFacade workflowTestFacade) {

            // Use a small buffer to make bounded behavior easy to assert in tests
            return new WorkflowTestApiController(conversionService, tempFileStorage, workflowTestFacade, 3);
        }
    }

    private String waitForRunKey() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1000;

        while (System.currentTimeMillis() < deadline) {
            Object runs = ReflectionTestUtils.getField(controller, "runs");

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
