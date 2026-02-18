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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@WebMvcTest(value = AiAgentTestApiController.class)
class AiAgentTestApiControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActionDefinitionFacade actionDefinitionFacade;

    @MockitoBean
    private Evaluator evaluator;

    @MockitoBean
    private TempFileStorage tempFileStorage;

    @MockitoBean
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @BeforeEach
    void beforeEach() {
        JsonMapper objectMapper = JsonMapper.builder()
            .build();

        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);
    }

    @Test
    void testAiAgentEmitsStartAndResult() throws Exception {
        setupWorkflowMocks();

        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), isNull(), isNull(), isNull(), anyString(), anyMap(), anyMap(),
                anyMap(), anyLong(), isNull(), anyBoolean()))
                    .thenReturn("Test response");

        MvcResult mvcResult = mockMvc.perform(
            post("/internal/ai-agent-tests")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(createRequestJson("wf-1", "node-1", 1L, "conv-1", "Hello")))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();

        mvcResult.getAsyncResult(10000);

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        MockHttpServletResponse response = mvcResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("event:start");
        assertThat(body).contains("testId");
        assertThat(body).contains("event:result");
        assertThat(body).contains("Test response");
    }

    @Test
    void testAiAgentStreamingEmitsStreamEventsAndResult() throws Exception {
        setupWorkflowMocks();

        ActionDefinition.SseEmitterHandler sseEmitterHandler = sseEmitter -> {
            sseEmitter.send("chunk1");
            sseEmitter.send("chunk2");
            sseEmitter.complete();
        };

        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), isNull(), isNull(), isNull(), anyString(), anyMap(), anyMap(),
                anyMap(), anyLong(), isNull(), anyBoolean()))
                    .thenReturn(sseEmitterHandler);

        MvcResult mvcResult = mockMvc.perform(
            post("/internal/ai-agent-tests")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(createRequestJson("wf-1", "node-1", 1L, "conv-1", "Hello")))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();

        mvcResult.getAsyncResult(10000);

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        MockHttpServletResponse response = mvcResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("event:start");
        assertThat(body).contains("event:stream");
        assertThat(body).contains("event:result");
        assertThat(body).contains("chunk1chunk2");
    }

    @Test
    void testAiAgentEmitsErrorOnException() throws Exception {
        setupWorkflowMocks();

        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), isNull(), isNull(), isNull(), anyString(), anyMap(), anyMap(),
                anyMap(), anyLong(), isNull(), anyBoolean()))
                    .thenThrow(new RuntimeException("Something went wrong"));

        MvcResult mvcResult = mockMvc.perform(
            post("/internal/ai-agent-tests")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(createRequestJson("wf-1", "node-1", 1L, "conv-1", "Hello")))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();

        mvcResult.getAsyncResult(10000);

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        MockHttpServletResponse response = mvcResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("event:start");
        assertThat(body).contains("event:error");
        assertThat(body).contains("Something went wrong");
    }

    @Test
    void testAiAgentStreamingEmitsErrorOnHandlerFailure() throws Exception {
        setupWorkflowMocks();

        ActionDefinition.SseEmitterHandler sseEmitterHandler = sseEmitter -> {
            sseEmitter.send("partial");
            sseEmitter.error(new RuntimeException("Stream failed"));
        };

        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), isNull(), isNull(), isNull(), anyString(), anyMap(), anyMap(),
                anyMap(), anyLong(), isNull(), anyBoolean()))
                    .thenReturn(sseEmitterHandler);

        MvcResult mvcResult = mockMvc.perform(
            post("/internal/ai-agent-tests")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(createRequestJson("wf-1", "node-1", 1L, "conv-1", "Hello")))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();

        mvcResult.getAsyncResult(10000);

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        MockHttpServletResponse response = mvcResult.getResponse();

        String body = response.getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("event:stream");
        assertThat(body).contains("event:error");
        assertThat(body).contains("Stream failed");
    }

    @Test
    void testStopReturnsOk() throws Exception {
        mockMvc.perform(post("/internal/ai-agent-tests/{testId}/stop", "non-existent-test-id"))
            .andExpect(status().isOk());
    }

    private String createRequestJson(
        String workflowId, String workflowNodeName, long environmentId, String conversationId, String message) {

        return "{" +
            "\"workflowId\": \"" + workflowId + "\"," +
            "\"workflowNodeName\": \"" + workflowNodeName + "\"," +
            "\"environmentId\": " + environmentId + "," +
            "\"conversationId\": \"" + conversationId + "\"," +
            "\"message\": \"" + message + "\"" +
            "}";
    }

    private void setupWorkflowMocks() {
        Workflow workflow = new Workflow(
            """
                {
                    "tasks": [
                        {
                            "name": "node-1",
                            "type": "testComponent/v1/testAction",
                            "parameters": {}
                        }
                    ]
                }
                """, Workflow.Format.JSON);

        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);

        when(
            workflowTestConfigurationService.getWorkflowTestConfigurationInputs(anyString(), anyLong()))
                .thenReturn(Map.of());

        when(
            workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(anyString(), anyString(), anyLong()))
                .thenReturn(Map.of());

        when(evaluator.evaluate(any(), anyMap())).thenAnswer(invocation -> invocation.getArgument(0));

        when(
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                anyString(), anyString(), anyLong()))
                    .thenReturn(List.of());
    }

    @Configuration
    static class AiAgentTestApiControllerTestConfiguration {

        @Bean
        AiAgentTestApiController aiAgentTestApiController(
            ActionDefinitionFacade actionDefinitionFacade, Evaluator evaluator, TempFileStorage tempFileStorage,
            WorkflowNodeOutputFacade workflowNodeOutputFacade, WorkflowService workflowService,
            WorkflowTestConfigurationService workflowTestConfigurationService) {

            return new AiAgentTestApiController(
                actionDefinitionFacade, evaluator, Runnable::run, tempFileStorage, workflowNodeOutputFacade,
                workflowService, workflowTestConfigurationService);
        }
    }
}
