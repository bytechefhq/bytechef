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

package com.bytechef.platform.webhook.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry.Registration;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@WebMvcTest(JobResumeController.class)
class JobResumeControllerIntTest {

    private static final String RESUME_ID = "resume-token-123";

    @Autowired
    private JobResumeController jobResumeController;

    @MockitoBean
    private JobResumeFacade jobResumeFacade;

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private SseStreamBridgeRegistry sseStreamBridgeRegistry;

    @Test
    void testResumeWithEventStreamAcceptHeaderStreamsEventsAfterTheSuspendedTurnStopped() throws Exception {
        long jobId = 77L;

        doAnswer(invocation -> {
            LongConsumer jobIdConsumer = invocation.getArgument(2);

            jobIdConsumer.accept(jobId);

            sseStreamBridgeRegistry.onSseStreamEvent(
                new SseStreamEvent(
                    jobId, SseStreamEvent.EVENT_TYPE_DATA, Map.of("event", "delta", "payload", "stale")));
            sseStreamBridgeRegistry.onSseStreamEvent(
                new SseStreamEvent(jobId, SseStreamEvent.EVENT_TYPE_COMPLETE, null));
            sseStreamBridgeRegistry.onSseStreamEvent(
                new SseStreamEvent(jobId, SseStreamEvent.EVENT_TYPE_JOB_STATUS, "STOPPED"));

            sseStreamBridgeRegistry.onSseStreamEvent(
                new SseStreamEvent(
                    jobId, SseStreamEvent.EVENT_TYPE_DATA, Map.of("event", "delta", "payload", "Hello")));
            sseStreamBridgeRegistry.onSseStreamEvent(
                new SseStreamEvent(
                    jobId, SseStreamEvent.EVENT_TYPE_DATA, Map.of("event", "delta", "payload", " world")));
            sseStreamBridgeRegistry.onSseStreamEvent(
                new SseStreamEvent(jobId, SseStreamEvent.EVENT_TYPE_COMPLETE, null));

            return JobResumeOutcome.OK;
        })
            .when(jobResumeFacade)
            .resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));

        MvcResult mvcResult = mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();

        mvcResult.getAsyncResult(10000);

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

        MockHttpServletResponse response = mvcResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("event:delta")
            .contains("Hello")
            .contains("world")
            .doesNotContain("stale");
    }

    @Test
    void testResumeStreamingSendsAgentEventTypesAsNamedEventsAfterTheResumedTaskStarted() throws Exception {
        long jobId = 78L;

        doAnswer(invocation -> {
            LongConsumer jobIdConsumer = invocation.getArgument(2);

            jobIdConsumer.accept(jobId);

            sseStreamBridgeRegistry.onSseStreamEvent(
                new SseStreamEvent(jobId, SseStreamEvent.EVENT_TYPE_TASK_STARTED, 5L));
            sseStreamBridgeRegistry.onSseStreamEvent(
                new SseStreamEvent(
                    jobId, SseStreamEvent.EVENT_TYPE_DATA,
                    Map.of(
                        AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION,
                        "resumeUrl", "https://example.com/job/resume/next")));
            sseStreamBridgeRegistry.onSseStreamEvent(
                new SseStreamEvent(jobId, SseStreamEvent.EVENT_TYPE_COMPLETE, null));

            return JobResumeOutcome.OK;
        })
            .when(jobResumeFacade)
            .resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));

        MvcResult mvcResult = mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();

        mvcResult.getAsyncResult(10000);

        MockHttpServletResponse response = mvcResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("event:" + AiAgentSseEventType.ASK_USER_QUESTION)
            .contains("https://example.com/job/resume/next")
            .doesNotContain(AiAgentSseEventType.EVENT_TYPE);
    }

    @Test
    void testResumeStreamingWithAnInvalidIdReturnsBadRequest() throws Exception {
        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenReturn(JobResumeOutcome.INVALID_ID);

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(request().asyncNotStarted());
    }

    @Test
    void testResumeStreamingOfAJobThatCanNoLongerBeResumedReturnsGone() throws Exception {
        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenReturn(JobResumeOutcome.GONE);

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isGone())
            .andExpect(request().asyncNotStarted());
    }

    @Test
    void testResumeStreamingOfASuspendThatDoesNotAllowStreamingReturnsNotAcceptable() throws Exception {
        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenReturn(JobResumeOutcome.STREAMING_NOT_ALLOWED);

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isNotAcceptable())
            .andExpect(request().asyncNotStarted());

        verifyNoInteractions(sseStreamBridgeRegistry);
    }

    @Test
    void testResumeStreamingClosesTheRegistrationWhenTheFacadeThrowsAfterRegistering() throws Exception {
        AutoCloseable handle = mock(AutoCloseable.class);

        long jobId = 79L;

        doReturn(new Registration(handle, new CompletableFuture<>()))
            .when(sseStreamBridgeRegistry)
            .registerForResume(eq(jobId), any(SseStreamBridge.class));

        doAnswer(invocation -> {
            LongConsumer jobIdConsumer = invocation.getArgument(2);

            jobIdConsumer.accept(jobId);

            throw new IllegalStateException("Resume failed");
        })
            .when(jobResumeFacade)
            .resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));

        assertThatThrownBy(() -> jobResumeController.resumeStreaming(RESUME_ID, Map.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Resume failed");

        verify(handle).close();
    }

    @Test
    void testResumeStreamingClosesTheRegistrationWhenTheResumedJobCompletes() throws Exception {
        AutoCloseable handle = mock(AutoCloseable.class);
        CompletableFuture<Void> completion = new CompletableFuture<>();

        doReturn(new Registration(handle, completion))
            .when(sseStreamBridgeRegistry)
            .registerForResume(anyLong(), any(SseStreamBridge.class));

        doAnswer(invocation -> {
            LongConsumer jobIdConsumer = invocation.getArgument(2);

            jobIdConsumer.accept(80L);

            return JobResumeOutcome.OK;
        })
            .when(jobResumeFacade)
            .resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));

        ResponseEntity<SseEmitter> responseEntity = jobResumeController.resumeStreaming(RESUME_ID, null);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(handle, never()).close();

        completion.complete(null);

        verify(handle).close();
    }

    @Test
    void testResumeStreamingClosesTheRegistrationWhenTheClientConnectionCompletes() throws Exception {
        AutoCloseable handle = mock(AutoCloseable.class);

        MvcResult mvcResult = performStreamingResumeWithRegistration(handle);

        verify(handle, never()).close();

        MockAsyncContext mockAsyncContext = getMockAsyncContext(mvcResult);

        mockAsyncContext.complete();

        verify(handle).close();
    }

    @Test
    void testResumeStreamingClosesTheRegistrationWhenTheEmitterTimesOut() throws Exception {
        AutoCloseable handle = mock(AutoCloseable.class);

        MvcResult mvcResult = performStreamingResumeWithRegistration(handle);

        MockAsyncContext mockAsyncContext = getMockAsyncContext(mvcResult);

        for (AsyncListener asyncListener : mockAsyncContext.getListeners()) {
            asyncListener.onTimeout(new AsyncEvent(mockAsyncContext));
        }

        verify(handle, atLeastOnce()).close();
    }

    @Test
    void testResumeStreamingClosesTheRegistrationWhenTheEmitterFails() throws Exception {
        AutoCloseable handle = mock(AutoCloseable.class);

        MvcResult mvcResult = performStreamingResumeWithRegistration(handle);

        MockAsyncContext mockAsyncContext = getMockAsyncContext(mvcResult);

        for (AsyncListener asyncListener : mockAsyncContext.getListeners()) {
            asyncListener.onError(new AsyncEvent(mockAsyncContext, new IOException("Broken pipe")));
        }

        verify(handle, atLeastOnce()).close();
    }

    @Test
    void testResumeWithoutEventStreamAcceptHeaderReturnsNoContent() throws Exception {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), anyMap()))
            .thenReturn(JobResumeOutcome.OK);

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isNoContent());

        verify(jobResumeFacade, never()).resumeJobStreaming(any(), anyMap(), any(LongConsumer.class));
    }

    @Test
    void testResumeRetriesUntilTheJobIsSuspended() throws Exception {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), anyMap()))
            .thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED, JobResumeOutcome.NOT_YET_SUSPENDED, JobResumeOutcome.OK);

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isNoContent());

        verify(jobResumeFacade, times(3)).resumeJob(eq(RESUME_ID), anyMap());
    }

    @Test
    void testResumeStreamingRetriesUntilTheJobIsSuspendedAndRegistersOnce() throws Exception {
        AutoCloseable handle = mock(AutoCloseable.class);

        doReturn(new Registration(handle, new CompletableFuture<>()))
            .when(sseStreamBridgeRegistry)
            .registerForResume(anyLong(), any(SseStreamBridge.class));

        doAnswer(invocation -> JobResumeOutcome.NOT_YET_SUSPENDED)
            .doAnswer(invocation -> JobResumeOutcome.NOT_YET_SUSPENDED)
            .doAnswer(invocation -> {
                LongConsumer jobIdConsumer = invocation.getArgument(2);

                jobIdConsumer.accept(82L);

                return JobResumeOutcome.OK;
            })
            .when(jobResumeFacade)
            .resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));

        MvcResult mvcResult = mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();

        verify(jobResumeFacade, times(3)).resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));
        verify(sseStreamBridgeRegistry, times(1)).registerForResume(eq(82L), any(SseStreamBridge.class));
        verify(handle, never()).close();

        MockAsyncContext mockAsyncContext = getMockAsyncContext(mvcResult);

        mockAsyncContext.complete();

        verify(handle, times(1)).close();
    }

    @Test
    void testResumeOfAFailedJobReturnsUnprocessableContent() throws Exception {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), anyMap()))
            .thenReturn(JobResumeOutcome.JOB_FAILED);

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isUnprocessableContent());
    }

    @Test
    void testResumeStreamingOfAFailedJobReturnsUnprocessableContent() throws Exception {
        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenReturn(JobResumeOutcome.JOB_FAILED);

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(request().asyncNotStarted());
    }

    @Test
    void testResumeThatLosesAConcurrentResumeRaceReturnsGone() throws Exception {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), anyMap()))
            .thenThrow(new OptimisticLockingFailureException("stale job version"));

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isGone());
    }

    @Test
    void testResumeStreamingThatLosesAConcurrentResumeRaceReturnsGone() throws Exception {
        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenThrow(new OptimisticLockingFailureException("stale job version"));

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isGone())
            .andExpect(request().asyncNotStarted());

        verify(sseStreamBridgeRegistry, never()).registerForResume(anyLong(), any(SseStreamBridge.class));
    }

    @Test
    void testResumeThatLosesAConcurrentResumeRaceInsideTheTenantContextReturnsGone() throws Exception {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), anyMap()))
            .thenThrow(new RuntimeException(new OptimisticLockingFailureException("stale job version")));

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isGone());
    }

    @Test
    void testResumeStreamingThatLosesAConcurrentResumeRaceInsideTheTenantContextReturnsGone() throws Exception {
        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenThrow(new RuntimeException(new OptimisticLockingFailureException("stale job version")));

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isGone())
            .andExpect(request().asyncNotStarted());
    }

    @Test
    void testResumeOfAJobThatIsNeverSuspendedReturnsConflict() throws Exception {
        when(jobResumeFacade.resumeJob(eq(RESUME_ID), anyMap()))
            .thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isConflict());

        verify(jobResumeFacade, atLeast(2)).resumeJob(eq(RESUME_ID), anyMap());
    }

    @Test
    void testResumeStreamingOfAJobThatIsNeverSuspendedReturnsConflict() throws Exception {
        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenReturn(JobResumeOutcome.NOT_YET_SUSPENDED);

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isConflict())
            .andExpect(request().asyncNotStarted());

        verify(jobResumeFacade, atLeast(2)).resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));
        verify(sseStreamBridgeRegistry, never()).registerForResume(anyLong(), any(SseStreamBridge.class));
    }

    private static MockAsyncContext getMockAsyncContext(MvcResult mvcResult) {
        MockHttpServletRequest mockHttpServletRequest = mvcResult.getRequest();

        return (MockAsyncContext) Objects.requireNonNull(mockHttpServletRequest.getAsyncContext());
    }

    private MvcResult performStreamingResumeWithRegistration(AutoCloseable handle) throws Exception {
        doReturn(new Registration(handle, new CompletableFuture<>()))
            .when(sseStreamBridgeRegistry)
            .registerForResume(anyLong(), any(SseStreamBridge.class));

        doAnswer(invocation -> {
            LongConsumer jobIdConsumer = invocation.getArgument(2);

            jobIdConsumer.accept(81L);

            return JobResumeOutcome.OK;
        })
            .when(jobResumeFacade)
            .resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));

        return mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();
    }

    @Configuration
    static class JobResumeControllerIntTestConfiguration {

        @Bean
        JobResumeController jobResumeController(
            JobResumeFacade jobResumeFacade, SseStreamBridgeRegistry sseStreamBridgeRegistry) {

            return new JobResumeController(
                jobResumeFacade, sseStreamBridgeRegistry, Duration.ofMillis(300), Duration.ofMillis(10));
        }

        @Bean
        SseStreamBridgeRegistry sseStreamBridgeRegistry() {
            return new SseStreamBridgeRegistry();
        }
    }
}
