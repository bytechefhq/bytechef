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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.tool.AgentToolCallingManagers;
import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.constant.AiAgentToolContextKey;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.component.definition.SuspendUtils;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
 * End-to-end integration test for the streaming AI agent suspend/resume flow. Exercises the real
 * {@link com.bytechef.component.ai.agent.tool.SuspendableToolCallingManager} inside the {@link AiAgentStreamChatAction}
 * advisor chain: a stub {@link org.springframework.ai.chat.model.ChatModel} whose first streaming response is a tool
 * call triggers a suspend; the test then calls {@link AiAgentStreamChatAction#resumePerform} with the captured
 * continuation parameters and verifies the second model call receives the patched tool response and the streamed
 * continuation carries the stub model's final-text response.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentStreamChatActionResumeIntTest {

    private static final String TOOL_NAME = "requestApproval";
    private static final String TOOL_CALL_ID = "call_stream_1";
    private static final String FINAL_ANSWER = "streaming final answer";

    @Test
    void testStreamingSuspendOnFirstCallAndResumeWithPatchedToolResponse() throws Exception {
        Parameters inputParameters = buildInputParameters();
        Parameters extensions = buildExtensions();
        Map<String, ComponentConnection> connectionParameters = buildConnectionParameters();

        AtomicReference<ActionContext.Suspend> capturedSuspend = new AtomicReference<>();
        ActionContextAware actionContext = buildActionContext(capturedSuspend);

        AtomicInteger callCount = new AtomicInteger(0);
        AtomicReference<Prompt> secondCallPrompt = new AtomicReference<>();

        org.springframework.ai.chat.model.ChatModel chatModel = buildChatModel(callCount, secondCallPrompt);
        ToolCallbackProvider toolCallbackProvider = buildSuspendingToolCallbackProvider();
        ClusterElementDefinitionService clusterElementDefinitionService =
            buildClusterElementDefinitionService(chatModel, toolCallbackProvider);

        ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder()
            .toolCallbackResolver(new DelegatingToolCallbackResolver(List.of()))
            .build();

        TestableStreamAction testAction = new TestableStreamAction(
            mock(AiAgentToolFacade.class), clusterElementDefinitionService, toolCallingManager);

        // === Assertion A: perform() returns a handler and stream processing causes a suspend ===

        SseEmitterHandler performHandler = testAction.performStream(
            inputParameters, connectionParameters, extensions, actionContext);

        assertThat(performHandler).isNotNull();

        List<Object> performEvents = driveHandlerToCompletion(performHandler);

        assertThat(performEvents).isNotNull();

        ActionContext.Suspend suspend = capturedSuspend.get();

        assertThat(suspend).isNotNull();
        assertThat(suspend.continueParameters()).containsKey(ToolSuspendConstants.CONVERSATION_STATE);
        assertThat(suspend.continueParameters()).containsKey(ToolSuspendConstants.PENDING_TOOL_CALL_ID);
        assertThat(suspend.continueParameters()
            .get(ToolSuspendConstants.PENDING_TOOL_CALL_ID)).isEqualTo(TOOL_CALL_ID);

        // === Assertion B: SuspendUtils would finalize the suspend (simulating post-output processor) ===

        ActionContext.Suspend finalizedSuspend = SuspendUtils.finalizeSuspend(actionContext);

        assertThat(finalizedSuspend).isNotNull();
        assertThat(finalizedSuspend.continueParameters()).containsKey(ToolSuspendConstants.CONVERSATION_STATE);
        assertThat(finalizedSuspend.continueParameters()).containsKey(ToolSuspendConstants.PENDING_TOOL_CALL_ID);

        // === Assertion C: resumePerform() returns an SseEmitterHandler that streams the patched continuation ===

        Parameters continueParameters = ParametersFactory.create(new HashMap<>(suspend.continueParameters()));
        Parameters resumeData = MockParametersFactory.create(Map.of("approved", true));

        SseEmitterHandler resumeHandler = testAction.performResumeStream(
            inputParameters, connectionParameters, extensions, continueParameters, resumeData, actionContext);

        assertThat(resumeHandler).isNotNull();

        // The service layer wraps a streaming resume result in a SuspendAwareSseEmitterHandler so the SSE
        // post-output processor can consume it; mirror that wrap here and drive the wrapped handler.

        SuspendAwareSseEmitterHandler wrappedResumeHandler =
            new SuspendAwareSseEmitterHandler(resumeHandler, actionContext);

        assertThat(wrappedResumeHandler.getActionContext()).isSameAs(actionContext);

        List<Object> resumeEvents = driveHandlerToCompletion(wrappedResumeHandler);

        assertThat(callCount.get()).isEqualTo(2);

        Prompt capturedPrompt = secondCallPrompt.get();

        assertThat(capturedPrompt).isNotNull();

        ToolResponseMessage patchedToolResponse = findToolResponseMessage(capturedPrompt.getInstructions());

        assertThat(patchedToolResponse).isNotNull();

        List<ToolResponseMessage.ToolResponse> responses = patchedToolResponse.getResponses();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .id()).isEqualTo(TOOL_CALL_ID);
        assertThat(responses.get(0)
            .responseData()).contains("approved");
        assertThat(responses.get(0)
            .responseData()).doesNotContain(ToolSuspendConstants.SUSPENDED_SENTINEL);

        assertThat(resumeEvents).contains(FINAL_ANSWER);
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

    private static Parameters buildInputParameters() {
        return MockParametersFactory.create(Map.of(
            "format", ChatModel.Format.SIMPLE.name(),
            "userPrompt", "please get approval",
            "response", Map.of("responseFormat", "TEXT")));
    }

    private static Parameters buildExtensions() {
        Map<String, Object> modelElementMap = new HashMap<>();

        modelElementMap.put("name", "model_1");
        modelElementMap.put("type", "testComponent/v1/testModel");
        modelElementMap.put("parameters", Map.of("model", "gpt-4o"));

        Map<String, Object> toolElementMap = new HashMap<>();

        toolElementMap.put("name", "tool_1");
        toolElementMap.put("type", "testComponent/v1/requestApproval");
        toolElementMap.put("parameters", Map.of());

        return MockParametersFactory.create(
            Map.of("clusterElements",
                Map.of(
                    "model", modelElementMap,
                    "tools", List.of(toolElementMap))));
    }

    private static Map<String, ComponentConnection> buildConnectionParameters() {
        Map<String, ComponentConnection> connectionParameters = new HashMap<>();

        connectionParameters.put("model_1", new ComponentConnection("testComponent", 1, 1L, Map.of(), null));
        connectionParameters.put("tool_1", new ComponentConnection("testComponent", 1, 2L, Map.of(), null));

        return connectionParameters;
    }

    private static ActionContextAware buildActionContext(AtomicReference<ActionContext.Suspend> capturedSuspend) {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        doAnswer(invocation -> {
            capturedSuspend.set(invocation.getArgument(0));

            return null;
        }).when(actionContext)
            .suspend(any());

        when(actionContext.getSuspend()).thenAnswer(invocation -> capturedSuspend.get());
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        return actionContext;
    }

    private static org.springframework.ai.chat.model.ChatModel buildChatModel(
        AtomicInteger callCount, AtomicReference<Prompt> secondCallPrompt) {

        return new org.springframework.ai.chat.model.ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                int count = callCount.incrementAndGet();

                if (count == 1) {
                    return buildToolCallResponse();
                }

                secondCallPrompt.set(prompt);

                return buildFinalAnswerResponse();
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                int count = callCount.incrementAndGet();

                if (count == 1) {
                    return Flux.just(buildToolCallResponse());
                }

                secondCallPrompt.set(prompt);

                return Flux.just(buildFinalAnswerResponse());
            }

            @Override
            public ToolCallingChatOptions getOptions() {
                return ToolCallingChatOptions.builder()
                    .build();
            }
        };
    }

    private static ChatResponse buildToolCallResponse() {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(
                List.of(new AssistantMessage.ToolCall(TOOL_CALL_ID, "function", TOOL_NAME, "{}")))
            .build();

        return ChatResponse.builder()
            .generations(List.of(new Generation(assistantMessage)))
            .build();
    }

    private static ChatResponse buildFinalAnswerResponse() {
        return ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(FINAL_ANSWER))))
            .build();
    }

    private static ToolCallbackProvider buildSuspendingToolCallbackProvider() {
        ToolDefinition toolDefinition = DefaultToolDefinition.builder()
            .name(TOOL_NAME)
            .description("request approval from a human")
            .inputSchema("{}")
            .build();

        ToolCallback suspendingToolCallback = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            @Override
            public String call(String toolInput) {
                throw new UnsupportedOperationException("must be called with ToolContext");
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                if (toolContext == null) {
                    throw new IllegalStateException("toolContext is required");
                }

                ActionContext context =
                    (ActionContext) toolContext.getContext()
                        .get(AiAgentToolContextKey.ACTION_CONTEXT);

                context.suspend(new ActionContext.Suspend(new HashMap<>(), Instant.now()
                    .plusSeconds(60)));

                return ToolSuspendConstants.SUSPENDED_SENTINEL;
            }
        };

        return () -> new ToolCallback[] {
            suspendingToolCallback
        };
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private static ClusterElementDefinitionService buildClusterElementDefinitionService(
        org.springframework.ai.chat.model.ChatModel chatModel, ToolCallbackProvider toolCallbackProvider)
        throws Exception {

        ClusterElementDefinitionService clusterElementDefinitionService =
            mock(ClusterElementDefinitionService.class);

        ModelFunction modelFunction = mock(ModelFunction.class);

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testModel"))).thenReturn(modelFunction);
        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);

        ToolCallbackProviderFunction toolCallbackProviderFunction =
            (inputParams, connectionParams, ctx) -> toolCallbackProvider;

        when(clusterElementDefinitionService.<ToolCallbackProviderFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("requestApproval"))).thenReturn(toolCallbackProviderFunction);

        return clusterElementDefinitionService;
    }

    private static ToolResponseMessage findToolResponseMessage(List<Message> messages) {
        for (Message message : messages) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                return toolResponseMessage;
            }
        }

        return null;
    }

    /**
     * Subclass of {@link AbstractAiAgentChatAction} that exposes streaming perform and resume methods for testing,
     * mirroring the logic in {@link AiAgentStreamChatAction} using the inherited protected helpers.
     */
    private static final class TestableStreamAction extends AbstractAiAgentChatAction {

        private TestableStreamAction(
            AiAgentToolFacade aiAgentToolFacade,
            ClusterElementDefinitionService clusterElementDefinitionService,
            ToolCallingManager toolCallingManager) {

            super(aiAgentToolFacade, clusterElementDefinitionService, new AgentToolCallingManagers(toolCallingManager));
        }

        public SseEmitterHandler performStream(
            Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
            Parameters extensions, ActionContext context) throws Exception {

            AtomicReference<@Nullable SseEmitter> emitterReference = new AtomicReference<>();
            Queue<Map<String, @Nullable Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

            ChatClient.ChatClientRequestSpec chatClientRequestSpec =
                getChatClientRequestSpec(inputParameters, connectionParameters, extensions, null, context);

            chatClientRequestSpec.toolContext(
                Map.of(
                    AiAgentToolContextKey.ACTION_CONTEXT, context,
                    AiAgentToolContextKey.SSE_EMITTER_REFERENCE, emitterReference,
                    AiAgentToolContextKey.SSE_BUFFERED_EVENTS, bufferedEvents));

            Flux<Object> contentFlux = chatClientRequestSpec.stream()
                .chatResponse()
                .concatMap(chatResponse -> Flux.fromIterable(toSseEvents(chatResponse, context)));

            return AiAgentStreamChatAction.createSseHandler(
                contentFlux, emitterReference, bufferedEvents, context);
        }

        public SseEmitterHandler performResumeStream(
            Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
            Parameters extensions, Parameters continueParameters, Parameters data,
            ActionContext context) throws Exception {

            AtomicReference<@Nullable SseEmitter> emitterReference = new AtomicReference<>();
            Queue<Map<String, @Nullable Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

            ChatClient.ChatClientRequestSpec chatClientRequestSpec =
                buildPatchedRequestSpec(
                    inputParameters, connectionParameters, extensions, continueParameters, data, context);

            chatClientRequestSpec.toolContext(
                Map.of(
                    AiAgentToolContextKey.ACTION_CONTEXT, context,
                    AiAgentToolContextKey.SSE_EMITTER_REFERENCE, emitterReference,
                    AiAgentToolContextKey.SSE_BUFFERED_EVENTS, bufferedEvents));

            Flux<Object> contentFlux = chatClientRequestSpec.stream()
                .chatResponse()
                .concatMap(chatResponse -> Flux.fromIterable(toSseEvents(chatResponse, context)));

            return AiAgentStreamChatAction.createSseHandler(
                contentFlux, emitterReference, bufferedEvents, context);
        }

        @SuppressWarnings("PMD.UnusedFormalParameter")
        private static List<Object> toSseEvents(ChatResponse chatResponse, ActionContext context) {
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
