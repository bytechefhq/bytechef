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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Integration test for {@link JobResumeController}. Exercises the controller end-to-end through a standalone
 * {@code MockMvc} setup wired against a stubbed {@link JobResumeFacade} and a real {@link SseStreamBridgeRegistry}.
 *
 * <p>
 * The module's test harness cannot stand up a real suspended streaming-agent job — that requires a coordinator, a
 * worker, a message broker and a real AI agent task. The honest closest test the harness supports stubs the facade so
 * {@code resumeJobStreaming} resolves a job id and invokes the controller's {@code jobIdConsumer} callback (the window
 * in which the controller registers its SSE bridge), then drives the <i>real</i> {@link SseStreamBridgeRegistry} with
 * {@link SseStreamEvent}s exactly as the message-broker-backed {@code SseStreamBridgeRegistry.onSseStreamEvent} does
 * for a resumed turn — a couple of data events followed by a complete event. This verifies the controller's
 * content-negotiation branch and its registry-bridge-to-{@code SseEmitter} relay, which is the unit of behaviour this
 * task adds.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class JobResumeControllerIntTest {

    private static final String RESUME_ID = "resume-token-123";

    @Test
    public void testResumeWithEventStreamAcceptHeaderReturnsSseStream() throws Exception {
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
        SseStreamBridgeRegistry sseStreamBridgeRegistry = new SseStreamBridgeRegistry();

        long jobId = 77L;

        doAnswer(invocation -> {
            LongConsumer jobIdConsumer = invocation.getArgument(2);

            // Mirrors the facade contract: invoke the consumer (controller registers its bridge here) before the
            // resumed turn starts emitting events.
            jobIdConsumer.accept(jobId);

            // Simulates the worker's SseStreamTaskExecutionPostOutputProcessor publishing the resumed turn's events
            // onto the SSE_STREAM_EVENTS message route, consumed by SseStreamBridgeRegistry.
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

        JobResumeController controller = new JobResumeController(jobResumeFacade, sseStreamBridgeRegistry);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .build();

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

        assertThat(body).contains("event:delta");
        assertThat(body).contains("Hello");
        assertThat(body).contains("world");
    }

    @Test
    public void testResumeWithoutEventStreamAcceptHeaderReturnsNoContent() throws Exception {
        JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
        SseStreamBridgeRegistry sseStreamBridgeRegistry = new SseStreamBridgeRegistry();

        when(jobResumeFacade.resumeJob(eq(RESUME_ID), anyMap()))
            .thenReturn(JobResumeOutcome.OK);

        JobResumeController controller = new JobResumeController(jobResumeFacade, sseStreamBridgeRegistry);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .build();

        mockMvc
            .perform(
                post("/job/resume/" + RESUME_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isNoContent());
    }
}
