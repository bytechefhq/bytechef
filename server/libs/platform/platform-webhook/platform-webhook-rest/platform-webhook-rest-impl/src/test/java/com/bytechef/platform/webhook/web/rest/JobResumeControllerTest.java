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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class JobResumeControllerTest {

    private static final String RESUME_ID = "resume-token-123";

    @Test
    void testResumeWithEventStreamAcceptHeaderStreamsEventsAfterTheSuspendedTurnStopped() throws Exception {
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
        SseStreamBridgeRegistry sseStreamBridgeRegistry = new SseStreamBridgeRegistry();

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

        MockMvc mockMvc = createMockMvc(jobResumeFacade, sseStreamBridgeRegistry);

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
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
        SseStreamBridgeRegistry sseStreamBridgeRegistry = new SseStreamBridgeRegistry();

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

        MockMvc mockMvc = createMockMvc(jobResumeFacade, sseStreamBridgeRegistry);

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
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);

        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenReturn(JobResumeOutcome.INVALID_ID);

        MockMvc mockMvc = createMockMvc(jobResumeFacade, new SseStreamBridgeRegistry());

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
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);

        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenReturn(JobResumeOutcome.GONE);

        MockMvc mockMvc = createMockMvc(jobResumeFacade, new SseStreamBridgeRegistry());

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
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
        SseStreamBridgeRegistry sseStreamBridgeRegistry = mock(SseStreamBridgeRegistry.class);

        when(jobResumeFacade.resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class)))
            .thenReturn(JobResumeOutcome.STREAMING_NOT_ALLOWED);

        MockMvc mockMvc = createMockMvc(jobResumeFacade, sseStreamBridgeRegistry);

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
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
        SseStreamBridgeRegistry sseStreamBridgeRegistry = mock(SseStreamBridgeRegistry.class);
        AutoCloseable handle = mock(AutoCloseable.class);

        long jobId = 79L;

        when(sseStreamBridgeRegistry.registerForResume(eq(jobId), any(SseStreamBridge.class)))
            .thenReturn(new Registration(handle, new CompletableFuture<>()));

        doAnswer(invocation -> {
            LongConsumer jobIdConsumer = invocation.getArgument(2);

            jobIdConsumer.accept(jobId);

            throw new IllegalStateException("Resume failed");
        })
            .when(jobResumeFacade)
            .resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));

        JobResumeController jobResumeController = new JobResumeController(jobResumeFacade, sseStreamBridgeRegistry);

        assertThatThrownBy(() -> jobResumeController.resumeStreaming(RESUME_ID, Map.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Resume failed");

        verify(handle).close();
    }

    @Test
    void testResumeStreamingClosesTheRegistrationWhenTheResumedJobCompletes() throws Exception {
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
        SseStreamBridgeRegistry sseStreamBridgeRegistry = mock(SseStreamBridgeRegistry.class);
        AutoCloseable handle = mock(AutoCloseable.class);
        CompletableFuture<Void> completion = new CompletableFuture<>();

        when(sseStreamBridgeRegistry.registerForResume(anyLong(), any(SseStreamBridge.class)))
            .thenReturn(new Registration(handle, completion));

        doAnswer(invocation -> {
            LongConsumer jobIdConsumer = invocation.getArgument(2);

            jobIdConsumer.accept(80L);

            return JobResumeOutcome.OK;
        })
            .when(jobResumeFacade)
            .resumeJobStreaming(eq(RESUME_ID), anyMap(), any(LongConsumer.class));

        JobResumeController jobResumeController = new JobResumeController(jobResumeFacade, sseStreamBridgeRegistry);

        ResponseEntity<SseEmitter> responseEntity = jobResumeController.resumeStreaming(RESUME_ID, null);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(handle, never()).close();

        completion.complete(null);

        verify(handle).close();
    }

    @Test
    void testResumeWithoutEventStreamAcceptHeaderReturnsNoContent() throws Exception {
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), anyMap()))
            .thenReturn(JobResumeOutcome.OK);

        MockMvc mockMvc = createMockMvc(jobResumeFacade, new SseStreamBridgeRegistry());

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isNoContent());

        verify(jobResumeFacade, never()).resumeJobStreaming(any(), anyMap(), any(LongConsumer.class));
    }

    private static MockMvc createMockMvc(
        JobResumeFacade jobResumeFacade, SseStreamBridgeRegistry sseStreamBridgeRegistry) {

        return MockMvcBuilders.standaloneSetup(new JobResumeController(jobResumeFacade, sseStreamBridgeRegistry))
            .build();
    }
}
