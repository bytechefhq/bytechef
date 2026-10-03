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

package com.bytechef.automation.ai.a2a.server.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.ai.a2a.server.facade.AutomationA2AServerFacade;
import com.bytechef.automation.ai.a2a.server.web.rest.config.AutomationA2AServerRestTestConfiguration;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor.A2ASkill;
import com.bytechef.platform.ai.a2a.A2AAgentRequest;
import com.bytechef.platform.ai.a2a.A2AAgentResult;
import com.bytechef.platform.ai.a2a.A2AAgentRun;
import com.bytechef.platform.ai.a2a.A2AProtocolHandler;
import com.bytechef.platform.ai.a2a.A2ATaskReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.util.Utils;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    A2AServerController.class, AutomationA2AServerRestTestConfiguration.class
})
@WebMvcTest(controllers = A2AServerController.class)
class A2AServerControllerIntTest {

    private static final String AGENT_CARD_PATH = "/api/automation/a2a/{secretKey}/.well-known/agent-card.json";
    private static final String JSON_RPC_PATH = "/api/automation/a2a/{secretKey}";
    private static final String MESSAGE_PARAMS = """
        {"message": {"kind": "message", "role": "user", "messageId": "message-1",
        "parts": [{"kind": "text", "text": "hi"}]}}""";
    private static final String PUBLIC_URL = "https://public.example";
    private static final String SECRET_KEY = "server-secret";
    private static final A2ATaskReference TASK_REFERENCE = new A2ATaskReference("durable-task", "durable-context");

    @MockitoBean
    private AutomationA2AServerFacade automationA2AServerFacade;

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private A2AProtocolHandler protocolHandler;

    @Test
    void testStreamMessageIsHandedToTheProtocolHandler() throws Exception {
        when(automationA2AServerFacade.start(any(A2AAgentRequest.class)))
            .thenReturn(A2AAgentRun.started(TASK_REFERENCE, new CompletableFuture<>()));

        performStream();

        verify(protocolHandler).handleStream(eq(SECRET_KEY), eq(1), eq("message/stream"), any(), any());
    }

    @Test
    void testStreamWritesTheWorkingAndTheFinalEventAsServerSentEvents() throws Exception {
        CompletableFuture<A2AAgentResult> result = new CompletableFuture<>();

        when(automationA2AServerFacade.start(any(A2AAgentRequest.class)))
            .thenReturn(A2AAgentRun.started(TASK_REFERENCE, result));

        MvcResult mvcResult = performStream();

        result.complete(A2AAgentResult.ofCompleted("the answer", TASK_REFERENCE));

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk());

        String body = mvcResult.getResponse()
            .getContentAsString();

        assertThat(body.split("data:", -1)).hasSize(3);
        assertThat(body).contains("\"working\"")
            .contains("\"completed\"")
            .contains("the answer");
    }

    @Test
    void testStreamWritesNothingAfterTheClientDisconnects() throws Exception {
        CompletableFuture<A2AAgentResult> result = new CompletableFuture<>();

        when(automationA2AServerFacade.start(any(A2AAgentRequest.class)))
            .thenReturn(A2AAgentRun.started(TASK_REFERENCE, result));

        MvcResult mvcResult = performStream();

        MockAsyncContext mockAsyncContext = (MockAsyncContext) Objects.requireNonNull(
            mvcResult.getRequest()
                .getAsyncContext());

        for (AsyncListener asyncListener : mockAsyncContext.getListeners()) {
            asyncListener.onError(new AsyncEvent(mockAsyncContext, new IOException("broken pipe")));
        }

        String bodyBeforeTheRunCompleted = mvcResult.getResponse()
            .getContentAsString();

        result.complete(A2AAgentResult.ofCompleted("the answer", TASK_REFERENCE));

        assertThat(bodyBeforeTheRunCompleted).contains("\"working\"");
        assertThat(mvcResult.getResponse()
            .getContentAsString()).isEqualTo(bodyBeforeTheRunCompleted);
    }

    @Test
    void testGetAndCancelTaskAreScopedToTheServerInThePath() throws Exception {
        performJsonRpc(jsonRpcRequest("\"r1\"", "tasks/get", "{\"id\": \"t1\"}"));
        performJsonRpc(jsonRpcRequest("\"r2\"", "tasks/cancel", "{\"id\": \"t2\"}"));

        verify(protocolHandler).handleGetTask(SECRET_KEY, "r1", "t1");
        verify(protocolHandler).handleCancelTask(SECRET_KEY, "r2", "t2");
        verify(automationA2AServerFacade).pollTask(SECRET_KEY, "t1");
        verify(automationA2AServerFacade).pollTask(SECRET_KEY, "t2");
    }

    @Nested
    class TaskParams {

        @Test
        void testNonTextualTaskIdsReturnAJsonRpcInvalidParamsError() throws Exception {
            List<String> invalidTaskIds = List.of("true", "{\"value\": \"t1\"}", "[\"t1\"]", "42", "null");

            for (String method : List.of("tasks/get", "tasks/cancel")) {
                for (String invalidTaskId : invalidTaskIds) {
                    JsonNode body = performJsonRpc(
                        jsonRpcRequest("\"r4\"", method, "{\"id\": " + invalidTaskId + "}"));

                    assertThat(body.at("/error/code")
                        .asInt())
                            .as(method + " " + invalidTaskId)
                            .isEqualTo(-32602);
                    assertThat(body.get("id")
                        .asText())
                            .as(method + " " + invalidTaskId)
                            .isEqualTo("r4");
                }
            }

            verifyNoInteractions(protocolHandler, automationA2AServerFacade);
        }

        @Test
        void testNonObjectTaskParamsReturnAJsonRpcInvalidParamsError() throws Exception {
            List<String> invalidParams = List.of("\"t1\"", "[\"t1\"]", "42", "true", "null");

            for (String method : List.of("tasks/get", "tasks/cancel")) {
                for (String invalidParam : invalidParams) {
                    JsonNode body = performJsonRpc(jsonRpcRequest("\"r5\"", method, invalidParam));

                    assertThat(body.at("/error/code")
                        .asInt())
                            .as(method + " " + invalidParam)
                            .isEqualTo(-32602);
                }
            }

            verifyNoInteractions(protocolHandler, automationA2AServerFacade);
        }

        @Test
        void testTextualTaskIdReachesTheProtocolHandler() throws Exception {
            performJsonRpc(jsonRpcRequest("\"r6\"", "tasks/get", "{\"id\": \"t6\"}"));
            performJsonRpc(jsonRpcRequest("\"r7\"", "tasks/cancel", "{\"id\": \"t7\"}"));

            verify(protocolHandler).handleGetTask(SECRET_KEY, "r6", "t6");
            verify(protocolHandler).handleCancelTask(SECRET_KEY, "r7", "t7");
        }
    }

    @Test
    void testMalformedJsonReturnsAJsonRpcParseError() throws Exception {
        JsonNode body = performJsonRpc("{not json");

        assertThat(body.at("/error/code")
            .asInt()).isEqualTo(-32700);
    }

    @Test
    void testStructurallyInvalidRequestsReturnAJsonRpcInvalidRequestError() throws Exception {
        List<String> invalidRequests = List.of(
            " ",
            "[{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"tasks/get\"}]",
            "{\"id\": 1, \"method\": \"tasks/get\", \"params\": {\"id\": \"t1\"}}",
            "{\"jsonrpc\": \"1.0\", \"id\": 1, \"method\": \"tasks/get\", \"params\": {\"id\": \"t1\"}}",
            "{\"jsonrpc\": 2.0, \"id\": 1, \"method\": \"tasks/get\", \"params\": {\"id\": \"t1\"}}",
            "{\"jsonrpc\": \"2.0\", \"id\": 1, \"params\": {\"id\": \"t1\"}}",
            "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": 42}");

        for (String invalidRequest : invalidRequests) {
            JsonNode body = performJsonRpc(invalidRequest);

            assertThat(body.at("/error/code")
                .asInt())
                    .as(invalidRequest)
                    .isEqualTo(-32600);
        }

        verifyNoInteractions(protocolHandler, automationA2AServerFacade);
    }

    @Test
    void testNonScalarIdsReturnAJsonRpcInvalidRequestErrorWithANullId() throws Exception {
        List<String> invalidIds = List.of("true", "{\"value\": 1}", "[1]");

        for (String invalidId : invalidIds) {
            JsonNode body = performJsonRpc(jsonRpcRequest(invalidId, "tasks/get", "{\"id\": \"t1\"}"));

            assertThat(body.at("/error/code")
                .asInt())
                    .as(invalidId)
                    .isEqualTo(-32600);
            assertThat(body.hasNonNull("id"))
                .as(invalidId)
                .isFalse();
        }

        verifyNoInteractions(protocolHandler, automationA2AServerFacade);
    }

    @Test
    void testMalformedMessageParamsReturnAJsonRpcInvalidParamsError() throws Exception {
        JsonNode body = performJsonRpc(jsonRpcRequest("\"r3\"", "message/send", "{\"message\": 42}"));

        assertThat(body.at("/error/code")
            .asInt()).isEqualTo(-32602);
        assertThat(body.get("id")
            .asText()).isEqualTo("r3");
        verifyNoInteractions(automationA2AServerFacade);
    }

    @Test
    void testSendMessageReturnsTheSerializedTaskOverTheWire() throws Exception {
        when(automationA2AServerFacade.start(any(A2AAgentRequest.class)))
            .thenReturn(A2AAgentRun.of(A2AAgentResult.ofInputRequired("approve at the link", TASK_REFERENCE)));

        JsonNode body = performJsonRpc(jsonRpcRequest(7, "message/send", MESSAGE_PARAMS));

        assertThat(body.get("jsonrpc")
            .asText()).isEqualTo("2.0");
        assertThat(body.get("id")
            .isNumber()).isTrue();
        assertThat(body.get("id")
            .asInt()).isEqualTo(7);
        assertThat(body.at("/result/kind")
            .asText()).isEqualTo("task");
        assertThat(body.at("/result/id")
            .asText()).isEqualTo("durable-task");
        assertThat(body.at("/result/contextId")
            .asText()).isEqualTo("durable-context");
        assertThat(body.at("/result/status/state")
            .asText()).isEqualTo("input-required");
        assertThat(body.at("/result/status/message/parts/0/text")
            .asText()).isEqualTo("approve at the link");
    }

    @Test
    void testUnknownTaskIsATaskNotFoundErrorOverTheWire() throws Exception {
        when(automationA2AServerFacade.pollTask(SECRET_KEY, "missing")).thenReturn(null);

        JsonNode body = performJsonRpc(jsonRpcRequest(9, "tasks/get", "{\"id\": \"missing\"}"));

        assertThat(body.get("id")
            .asInt()).isEqualTo(9);
        assertThat(body.at("/error/code")
            .asInt()).isEqualTo(-32001);
        verify(automationA2AServerFacade).pollTask(SECRET_KEY, "missing");
    }

    @Test
    void testAgentCardSerializesARealCard() throws Exception {
        stubAgentDescriptor(automationA2AServerFacade);

        JsonNode agentCard = getAgentCard(mockMvc);

        assertThat(agentCard.get("protocolVersion")
            .asText()).isEqualTo("0.3.0");
        assertThat(agentCard.get("url")
            .asText()).isEqualTo("http://localhost/api/automation/a2a/server-secret");
        assertThat(agentCard.at("/capabilities/streaming")
            .asBoolean()).isTrue();
        assertThat(agentCard.at("/skills/0/id")
            .asText()).isEqualTo("wf1");
    }

    @Test
    void testAgentCardUrlFallsBackToTheRequestUrlWithoutAPublicUrl() throws Exception {
        stubAgentDescriptor(automationA2AServerFacade);

        getAgentCard(mockMvc);

        verify(automationA2AServerFacade).getAgentDescriptor(
            SECRET_KEY, "http://localhost/api/automation/a2a/server-secret");
    }

    @Nested
    @TestPropertySource(properties = "bytechef.public-url=" + PUBLIC_URL)
    class WithPublicUrl {

        @Autowired
        private MockMvc publicUrlMockMvc;

        @Autowired
        private AutomationA2AServerFacade publicUrlAutomationA2AServerFacade;

        @Test
        void testAgentCardAdvertisesThePublicUrl() throws Exception {
            stubAgentDescriptor(publicUrlAutomationA2AServerFacade);

            JsonNode agentCard = getAgentCard(publicUrlMockMvc);

            assertThat(agentCard.get("url")
                .asText()).isEqualTo(PUBLIC_URL + "/api/automation/a2a/server-secret");
            verify(publicUrlAutomationA2AServerFacade).getAgentDescriptor(
                SECRET_KEY, PUBLIC_URL + "/api/automation/a2a/server-secret");
        }
    }

    private MvcResult performStream() throws Exception {
        return mockMvc
            .perform(
                post(JSON_RPC_PATH, SECRET_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRpcRequest(1, "message/stream", MESSAGE_PARAMS)))
            .andExpect(request().asyncStarted())
            .andReturn();
    }

    private JsonNode performJsonRpc(String content) throws Exception {
        MvcResult mvcResult = mockMvc
            .perform(
                post(JSON_RPC_PATH, SECRET_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content))
            .andExpect(status().isOk())
            .andReturn();

        return Utils.OBJECT_MAPPER.readTree(
            mvcResult.getResponse()
                .getContentAsString());
    }

    private static JsonNode getAgentCard(MockMvc agentCardMockMvc) throws Exception {
        MvcResult mvcResult = agentCardMockMvc.perform(get(AGENT_CARD_PATH, SECRET_KEY))
            .andExpect(status().isOk())
            .andReturn();

        return Utils.OBJECT_MAPPER.readTree(
            mvcResult.getResponse()
                .getContentAsString());
    }

    private static void stubAgentDescriptor(AutomationA2AServerFacade stubbedAutomationA2AServerFacade) {
        when(stubbedAutomationA2AServerFacade.getAgentDescriptor(eq(SECRET_KEY), anyString()))
            .thenAnswer(invocation -> new A2AAgentDescriptor(
                "Support Agent", "Answers questions", invocation.getArgument(1), "1.0.0",
                List.of(new A2ASkill("wf1", "Q&A", "Answers", List.of("support")))));
    }

    private static String jsonRpcRequest(Object id, String method, String params) {
        return "{\"jsonrpc\": \"2.0\", \"id\": " + id + ", \"method\": \"" + method + "\", \"params\": " + params + "}";
    }
}
