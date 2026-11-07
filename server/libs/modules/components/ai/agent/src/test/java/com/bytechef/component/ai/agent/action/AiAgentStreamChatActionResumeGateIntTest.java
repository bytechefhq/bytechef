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

package com.bytechef.component.ai.agent.action;

import static com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.tool.AgentToolSuspension;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsApprovalGate;
import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.ai.tool.AiAgentToolContext;
import com.bytechef.platform.ai.tool.AiAgentToolContext.SseTransport;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ActionContextAware.ApprovalLinks;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentStreamChatActionResumeGateIntTest {

    private static final String TOOL_NAME = "sendMessage";
    private static final String TOOL_CALL_ID = "call_gate_1";
    private static final String TOOL_CALL_ARGUMENTS = "{\"channel\": \"#general\", \"text\": \"hi\"}";
    private static final String TOOL_RESULT = "message sent: ts=1721";
    private static final String FINAL_ANSWER = "gated final answer";

    @Test
    void testGatedToolSuspendsAndApprovedResumeExecutesWithOriginalArguments() throws Exception {
        Harness harness = new Harness();

        ActionContext.Suspend suspend = harness.performUntilSuspend();

        assertThat(harness.toolInvocations.get()).isZero();

        verify(harness.clusterElementDefinitionService).executeApprovalChannel(
            eq("chat"), eq(1), eq("chat"), anyMap(), anyString(), isNull(), any());

        Map<String, Object> continueParameters = new HashMap<>(suspend.continueParameters());

        assertThat(continueParameters)
            .containsEntry(ToolSuspendConstants.GATED_TOOL_NAME, TOOL_NAME)
            .containsEntry(ToolSuspendConstants.GATED_TOOL_INPUT, TOOL_CALL_ARGUMENTS)
            .containsKey(AgentToolSuspension.CONTINUE_PARAMETER_KEY);

        AgentToolSuspension agentToolSuspension = (AgentToolSuspension) continueParameters.get(
            AgentToolSuspension.CONTINUE_PARAMETER_KEY);

        assertThat(agentToolSuspension.pendingToolCallId()).isEqualTo(TOOL_CALL_ID);

        List<Object> resumeEvents = harness.resume(suspend, Map.of("approved", true, "comment", "ship it"));

        assertThat(harness.toolInvocations.get()).isEqualTo(1);
        assertThat(harness.lastToolInput.get()).isEqualTo(TOOL_CALL_ARGUMENTS);

        ToolResponseMessage.ToolResponse patchedResponse = harness.findPatchedToolResponse();

        assertThat(patchedResponse.id()).isEqualTo(TOOL_CALL_ID);
        assertThat(patchedResponse.responseData())
            .contains("approvedByReviewer")
            .contains(TOOL_RESULT)
            .contains("ship it")
            .doesNotContain("__BYTECHEF_TOOL_SUSPENDED__");

        assertThat(resumeEvents).contains(FINAL_ANSWER);
    }

    @Test
    void testGatedToolRejectedResumePatchesDenialWithoutExecuting() throws Exception {
        Harness harness = new Harness();

        ActionContext.Suspend suspend = harness.performUntilSuspend();

        List<Object> resumeEvents = harness.resume(suspend, Map.of("approved", false, "comment", "not now"));

        assertThat(harness.toolInvocations.get()).isZero();

        ToolResponseMessage.ToolResponse patchedResponse = harness.findPatchedToolResponse();

        assertThat(patchedResponse.id()).isEqualTo(TOOL_CALL_ID);
        assertThat(patchedResponse.responseData())
            .contains("denied")
            .contains("Denied by reviewer: not now");

        assertThat(resumeEvents).contains(FINAL_ANSWER);
    }

    private static final class Harness {

        private final AtomicInteger toolInvocations = new AtomicInteger();
        private final AtomicReference<@Nullable String> lastToolInput = new AtomicReference<>();
        private final AtomicInteger modelCallCount = new AtomicInteger();
        private final AtomicReference<@Nullable Prompt> secondCallPrompt = new AtomicReference<>();
        private final AtomicReference<ActionContext.@Nullable Suspend> capturedSuspend = new AtomicReference<>();

        private final ClusterElementDefinitionService clusterElementDefinitionService;
        private final ActionContextAware actionContext;
        private final TestableStreamAction action;

        private final Parameters inputParameters = MockParametersFactory.create(Map.of(
            "format", ChatModel.Format.SIMPLE.name(),
            "userPrompt", "please send the message",
            "response", Map.of("responseFormat", "TEXT")));

        private final Parameters extensions = buildExtensions();

        private final Map<String, ComponentConnection> connectionParameters = buildConnectionParameters();

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        private Harness() throws Exception {
            org.springframework.ai.chat.model.ChatModel chatModel = buildChatModel();

            clusterElementDefinitionService = buildClusterElementDefinitionService(chatModel);
            actionContext = buildActionContext();

            ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder()
                .toolCallbackResolver(new DelegatingToolCallbackResolver(List.of()))
                .build();

            action = new TestableStreamAction(
                mock(AiAgentToolFacade.class), clusterElementDefinitionService, toolCallingManager);
        }

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        private ActionContext.Suspend performUntilSuspend() throws Exception {
            SseEmitterHandler performHandler = action.performStream(
                inputParameters, connectionParameters, extensions, actionContext);

            driveHandlerToCompletion(performHandler);

            ActionContext.Suspend suspend = capturedSuspend.get();

            assertThat(suspend).isNotNull();

            return suspend;
        }

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        private List<Object> resume(ActionContext.Suspend suspend, Map<String, Object> data) throws Exception {
            Parameters continueParameters = ParametersFactory.create(new HashMap<>(suspend.continueParameters()));

            capturedSuspend.set(null);

            SseEmitterHandler resumeHandler = action.performResumeStream(
                inputParameters, connectionParameters, extensions, continueParameters,
                MockParametersFactory.create(data), actionContext);

            return driveHandlerToCompletion(resumeHandler);
        }

        private ToolResponseMessage.ToolResponse findPatchedToolResponse() {
            Prompt capturedPrompt = secondCallPrompt.get();

            assertThat(capturedPrompt).isNotNull();

            for (Message message : capturedPrompt.getInstructions()) {
                if (message instanceof ToolResponseMessage toolResponseMessage) {
                    List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();

                    assertThat(responses).hasSize(1);

                    return responses.get(0);
                }
            }

            throw new AssertionError("No patched ToolResponseMessage found in the second model call");
        }

        private org.springframework.ai.chat.model.ChatModel buildChatModel() {
            return new org.springframework.ai.chat.model.ChatModel() {

                @Override
                public ChatResponse call(Prompt prompt) {
                    return respond(prompt);
                }

                @Override
                public Flux<ChatResponse> stream(Prompt prompt) {
                    return Flux.just(respond(prompt));
                }

                @Override
                public ToolCallingChatOptions getOptions() {
                    return ToolCallingChatOptions.builder()
                        .build();
                }

                private ChatResponse respond(Prompt prompt) {
                    int count = modelCallCount.incrementAndGet();

                    if (count == 1) {
                        AssistantMessage assistantMessage = AssistantMessage.builder()
                            .content("")
                            .toolCalls(
                                List.of(
                                    new AssistantMessage.ToolCall(
                                        TOOL_CALL_ID, "function", TOOL_NAME, TOOL_CALL_ARGUMENTS)))
                            .build();

                        return ChatResponse.builder()
                            .generations(List.of(new Generation(assistantMessage)))
                            .build();
                    }

                    secondCallPrompt.set(prompt);

                    return ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(FINAL_ANSWER))))
                        .build();
                }
            };
        }

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        private ClusterElementDefinitionService buildClusterElementDefinitionService(
            org.springframework.ai.chat.model.ChatModel chatModel) throws Exception {

            ClusterElementDefinitionService service = mock(ClusterElementDefinitionService.class);

            ModelFunction modelFunction = mock(ModelFunction.class);

            when(service.<ModelFunction>getClusterElement(eq("testComponent"), eq(1), eq("testModel")))
                .thenReturn(modelFunction);
            when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);

            ToolDefinition toolDefinition = DefaultToolDefinition.builder()
                .name(TOOL_NAME)
                .description("Send a message")
                .inputSchema("{}")
                .build();

            ToolCallback executingToolCallback = new ToolCallback() {

                @Override
                public ToolDefinition getToolDefinition() {
                    return toolDefinition;
                }

                @Override
                public String call(String toolInput) {
                    return call(toolInput, null);
                }

                @Override
                public String call(String toolInput, @Nullable ToolContext toolContext) {
                    toolInvocations.incrementAndGet();

                    lastToolInput.set(toolInput);

                    return TOOL_RESULT;
                }
            };

            ToolCallbackProviderFunction toolCallbackProviderFunction =
                (inputParams, connectionParams, context) -> (ToolCallbackProvider) () -> new ToolCallback[] {
                    executingToolCallback
                };

            when(service.<ToolCallbackProviderFunction>getClusterElement(
                eq("testComponent"), eq(1), eq(TOOL_NAME))).thenReturn(toolCallbackProviderFunction);

            AiAgentUtilsApprovalGate approvalGateTool = new AiAgentUtilsApprovalGate(
                new ClusterElementToolCallbacks(mock(AiAgentToolFacade.class), service), service);

            ClusterElementDefinition<MultipleConnectionsToolCallbackProviderFunction> gateDefinition =
                approvalGateTool.clusterElementDefinition;

            when(service.<MultipleConnectionsToolCallbackProviderFunction>getClusterElement(
                eq("aiAgentUtils"), eq(1), eq("approvalGate"))).thenReturn(gateDefinition.getElement());

            when(service.executeApprovalChannel(
                anyString(), anyInt(), anyString(), anyMap(), anyString(), isNull(), any())).thenReturn(null);

            return service;
        }

        private ActionContextAware buildActionContext() {
            ActionContextAware context = mock(ActionContextAware.class);

            doAnswer(invocation -> {
                capturedSuspend.set(invocation.getArgument(0));

                return null;
            }).when(context)
                .suspend(any());

            when(context.getSuspend()).thenAnswer(invocation -> capturedSuspend.get());
            when(context.isEditorEnvironment()).thenReturn(false);
            when(context.getApprovalLinks(any())).thenReturn(
                new ApprovalLinks(
                    "https://example.com", "tok123", "https://example.com/job/resume/tok123",
                    "https://example.com/resume/tok123"));

            return context;
        }
    }

    private static Parameters buildExtensions() {
        Map<String, Object> modelElementMap = new HashMap<>();

        modelElementMap.put("name", "model_1");
        modelElementMap.put("type", "testComponent/v1/testModel");
        modelElementMap.put("parameters", Map.of("model", "gpt-4o"));

        Map<String, Object> toolElementMap = new HashMap<>();

        toolElementMap.put("name", "tool_1");
        toolElementMap.put("type", "testComponent/v1/" + TOOL_NAME);
        toolElementMap.put("parameters", Map.of());

        Map<String, Object> gateElementMap = new HashMap<>();

        gateElementMap.put("name", "approvalGate_1");
        gateElementMap.put("type", "aiAgentUtils/v1/approvalGate");
        gateElementMap.put("parameters", Map.of("name", "Destructive"));
        gateElementMap.put("clusterElements", Map.of("tools", List.of(toolElementMap)));

        return MockParametersFactory.create(
            Map.of("clusterElements",
                Map.of(
                    "model", modelElementMap,
                    "tools", List.of(gateElementMap))));
    }

    private static Map<String, ComponentConnection> buildConnectionParameters() {
        Map<String, ComponentConnection> connectionParameters = new HashMap<>();

        connectionParameters.put("model_1", new ComponentConnection("testComponent", 1, 1L, Map.of(), null));
        connectionParameters.put("tool_1", new ComponentConnection("testComponent", 1, 2L, Map.of(), null));

        return connectionParameters;
    }

    private static List<Object> driveHandlerToCompletion(SseEmitterHandler handler) throws InterruptedException {
        List<Object> receivedEvents = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        SseEmitter emitter = new SseEmitter() {

            @Override
            public void send(Object data) {
                receivedEvents.add(data);
            }

            @Override
            public void complete() {
                latch.countDown();
            }

            @Override
            public void error(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void addTimeoutListener(Runnable runnable) {
            }
        };

        handler.handle(emitter);

        boolean completed = latch.await(10, TimeUnit.SECONDS);

        assertThat(completed).as("SSE handler did not complete within 10 seconds")
            .isTrue();

        return receivedEvents;
    }

    private static final class TestableStreamAction extends AbstractAiAgentChatAction {

        private TestableStreamAction(
            AiAgentToolFacade aiAgentToolFacade,
            ClusterElementDefinitionService clusterElementDefinitionService,
            ToolCallingManager toolCallingManager) {

            super(aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);
        }

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        public SseEmitterHandler performStream(
            Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
            Parameters extensions, ActionContext context) throws Exception {

            SseTransport sseTransport = new SseTransport();

            ChatClient.ChatClientRequestSpec chatClientRequestSpec =
                getChatClientRequestSpec(inputParameters, connectionParameters, extensions, null, context);

            chatClientRequestSpec.toolContext(new AiAgentToolContext(context, sseTransport).toMap());

            Flux<Object> contentFlux = chatClientRequestSpec.stream()
                .chatResponse()
                .concatMap(chatResponse -> Flux.fromIterable(toSseEvents(chatResponse)));

            return AiAgentStreamChatAction.createSseHandler(contentFlux, sseTransport, context);
        }

        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        public SseEmitterHandler performResumeStream(
            Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
            Parameters extensions, Parameters continueParameters, Parameters data,
            ActionContext context) throws Exception {

            SseTransport sseTransport = new SseTransport();

            ChatClient.ChatClientRequestSpec chatClientRequestSpec =
                buildResumedRequestSpec(
                    inputParameters, connectionParameters, extensions, continueParameters, data, null, context);

            if (chatClientRequestSpec == null) {
                throw new IllegalStateException("The resumed turn did not build a request");
            }

            chatClientRequestSpec.toolContext(new AiAgentToolContext(context, sseTransport).toMap());

            Flux<Object> contentFlux = chatClientRequestSpec.stream()
                .chatResponse()
                .concatMap(chatResponse -> Flux.fromIterable(toSseEvents(chatResponse)));

            return AiAgentStreamChatAction.createSseHandler(contentFlux, sseTransport, context);
        }

        private static List<Object> toSseEvents(ChatResponse chatResponse) {
            List<Object> events = new ArrayList<>();

            Generation result = chatResponse.getResult();

            if (result != null) {
                AssistantMessage output = result.getOutput();

                if (output != null) {
                    String text = output.getText();

                    if (text != null && !text.isEmpty()) {
                        events.add(text);
                    }
                }
            }

            return events;
        }
    }
}
