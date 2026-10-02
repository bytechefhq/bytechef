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

import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.LOOK_UP_TOOL_NAME;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.LOOK_UP_TOOL_RESULT;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.SUSPENDING_TOOL_NAME;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.SUSPEND_THEN_FAIL_TOOL_NAME;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.USER_PROMPT;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createActionContext;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createChatModel;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createClusterElementDefinitionService;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createConnectionParameters;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createExtensions;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createInputParameters;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createToolCallingManager;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.findLastToolResponseMessage;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.textResponse;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.toPersistedContinueParameters;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.toolCallResponse;
import static com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.component.ai.agent.action.event.ToolExecutionEvent;
import com.bytechef.component.ai.agent.action.event.listener.ToolExecutionListener;
import com.bytechef.component.ai.agent.tool.AgentToolSuspension;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.ai.tool.AiAgentToolContext.SseTransport;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.definition.MultipleConnectionsResumePerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.TenantContextThreadLocalAccessor;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.micrometer.context.ContextRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * Tests for {@link AiAgentStreamChatAction#createSseHandler} covering buffered-event replay, cancel-on-send-failure,
 * upstream-error propagation, and timeout-cancel wiring.
 *
 * @author Ivica Cardic
 */
class AiAgentStreamChatActionTest {

    @Test
    void testExecutionContextCarriesTenantToSchedulerThreads() {
        ContextRegistry.getInstance()
            .registerThreadLocalAccessor(new TenantContextThreadLocalAccessor());

        Hooks.enableAutomaticContextPropagation();

        try {
            Flux<Object> tenantIdFlux = TenantContext.callWithTenantId(
                "tenantA", () -> AiAgentStreamChatAction.withExecutionContext(
                    Flux.<Object>just("event")
                        .publishOn(Schedulers.boundedElastic())
                        .map(event -> TenantContext.getCurrentTenantId())));

            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
            assertThat(tenantIdFlux.blockLast()).isEqualTo("tenantA");
        } finally {
            Hooks.disableAutomaticContextPropagation();

            ContextRegistry.getInstance()
                .removeThreadLocalAccessor(TenantContextThreadLocalAccessor.KEY);
        }
    }

    @Test
    void testBufferedEventsAreReplayedOnceEmitterBinds() {
        SseTransport sseTransport = new SseTransport();

        Map<String, @Nullable Object> firstBuffered = new LinkedHashMap<>();
        firstBuffered.put("__eventType", "tool_execution");
        firstBuffered.put("toolName", "lookupCustomer");

        Map<String, @Nullable Object> secondBuffered = new LinkedHashMap<>();
        secondBuffered.put("__eventType", "tool_execution");
        secondBuffered.put("toolName", "createTicket");

        sseTransport.send(firstBuffered);
        sseTransport.send(secondBuffered);

        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        Flux<Object> emptyUpstream = Flux.empty();

        AiAgentStreamChatAction
            .createSseHandler(emptyUpstream, sseTransport, context)
            .handle(emitter);

        InOrder inOrder = inOrder(emitter);

        inOrder.verify(emitter)
            .send(firstBuffered);
        inOrder.verify(emitter)
            .send(secondBuffered);

        Map<String, @Nullable Object> afterBinding = Map.of("toolName", "sendEmail");

        sseTransport.send(afterBinding);

        verify(emitter).send(afterBinding);
    }

    @Test
    void testBufferedEventThatFailsToSendIsLoggedAndTheRestAreStillSent() {
        SseTransport sseTransport = new SseTransport();

        Map<String, @Nullable Object> failingBuffered = Map.of("toolName", "lookupCustomer");
        Map<String, @Nullable Object> nextBuffered = Map.of("toolName", "createTicket");

        sseTransport.send(failingBuffered);
        sseTransport.send(nextBuffered);

        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        doThrow(new RuntimeException("client disconnected"))
            .when(emitter)
            .send(failingBuffered);

        AiAgentStreamChatAction
            .createSseHandler(Flux.empty(), sseTransport, context)
            .handle(emitter);

        verify(emitter).send(nextBuffered);
        verify(context, atLeastOnce()).log(any());
    }

    @Test
    void testSendFailureOnStreamItemCancelsSubscriptionAndLogsAtWarn() {
        SseTransport sseTransport = new SseTransport();
        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        Sinks.Many<Object> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

        doThrow(new RuntimeException("client disconnected"))
            .when(emitter)
            .send("first");

        AiAgentStreamChatAction
            .createSseHandler(sink.asFlux(), sseTransport, context)
            .handle(emitter);

        sink.tryEmitNext("first");
        sink.tryEmitNext("second");

        verify(emitter).send("first");
        verify(emitter, never()).send("second");
        verify(context, atLeastOnce()).log(any());
    }

    @Test
    void testUpstreamErrorPropagatesToEmitterError() {
        SseTransport sseTransport = new SseTransport();
        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        RuntimeException upstreamFailure = new RuntimeException("LLM provider timeout");

        Flux<Object> failingUpstream = Flux.error(upstreamFailure);

        AiAgentStreamChatAction
            .createSseHandler(failingUpstream, sseTransport, context)
            .handle(emitter);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(emitter).error(throwableCaptor.capture());
        verify(emitter, never()).complete();

        assertThat(throwableCaptor.getValue()).isSameAs(upstreamFailure);
    }

    @Test
    void testTimeoutListenerCancelsUpstreamSubscription() {
        SseTransport sseTransport = new SseTransport();
        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        Sinks.Many<Object> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

        ArgumentCaptor<Runnable> timeoutListenerCaptor = ArgumentCaptor.forClass(Runnable.class);

        AiAgentStreamChatAction
            .createSseHandler(sink.asFlux(), sseTransport, context)
            .handle(emitter);

        verify(emitter).addTimeoutListener(timeoutListenerCaptor.capture());

        sink.tryEmitNext("before-timeout");

        verify(emitter).send("before-timeout");

        timeoutListenerCaptor.getValue()
            .run();

        sink.tryEmitNext("after-timeout");

        verify(emitter, times(1)).send("before-timeout");
        verify(emitter, never()).send("after-timeout");
    }

    @Test
    void testStreamCompletionCallsEmitterComplete() {
        SseTransport sseTransport = new SseTransport();
        ActionContext context = mock(ActionContext.class);
        SseEmitter emitter = mock(SseEmitter.class);

        Flux<Object> finiteUpstream = Flux.just("chunk-1", "chunk-2");

        AiAgentStreamChatAction
            .createSseHandler(finiteUpstream, sseTransport, context)
            .handle(emitter);

        verify(emitter).send("chunk-1");
        verify(emitter).send("chunk-2");
        verify(emitter).complete();
        verify(emitter, never()).error(any());
    }

    @Test
    void testTurnTextSeparatorSeparatesTextAcrossToolExecution() {
        AiAgentStreamChatAction.TurnTextSeparator turnTextSeparator = new AiAgentStreamChatAction.TurnTextSeparator();

        assertThat(turnTextSeparator.apply("I'll load the skill first.")).isEqualTo("I'll load the skill first.");

        turnTextSeparator.markToolExecuted();

        assertThat(turnTextSeparator.apply("**New**")).isEqualTo("\n\n**New**");
        assertThat(turnTextSeparator.apply(" items")).isEqualTo(" items");
    }

    @Test
    void testTurnTextSeparatorDoesNotPrefixFirstText() {
        AiAgentStreamChatAction.TurnTextSeparator turnTextSeparator = new AiAgentStreamChatAction.TurnTextSeparator();

        turnTextSeparator.markToolExecuted();

        assertThat(turnTextSeparator.apply("**New**")).isEqualTo("**New**");
        assertThat(turnTextSeparator.apply(" items")).isEqualTo(" items");
    }

    @Test
    void testToolExecutionSeparatesStreamedTextOfNextTurn() {
        SseTransport sseTransport = new SseTransport();
        ActionContext context = mock(ActionContext.class);
        AiAgentStreamChatAction.TurnTextSeparator turnTextSeparator = new AiAgentStreamChatAction.TurnTextSeparator();

        ToolExecutionListener toolExecutionListener = AiAgentStreamChatAction.createToolExecutionListener(
            sseTransport, turnTextSeparator, context);

        assertThat(
            AiAgentStreamChatAction.toSseEvents(chatResponse("I'll load the skill first."), turnTextSeparator, context))
                .containsExactly("I'll load the skill first.");

        toolExecutionListener.onToolExecution(
            new ToolExecutionEvent("release_notes", Map.of(), "skill instructions", null, null));

        assertThat(AiAgentStreamChatAction.toSseEvents(chatResponse("**New**"), turnTextSeparator, context))
            .containsExactly("\n\n**New**");
        assertThat(AiAgentStreamChatAction.toSseEvents(chatResponse(" items"), turnTextSeparator, context))
            .containsExactly(" items");

        SseEmitter emitter = mock(SseEmitter.class);

        sseTransport.attach(emitter, exception -> {
            throw new AssertionError(exception);
        });

        verify(emitter, times(1)).send(any());
    }

    private static ChatResponse chatResponse(String text) {
        return ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();
    }

    @Nested
    @ExtendWith(ObjectMapperSetupExtension.class)
    class ResumeTests {

        private static final String FINAL_ANSWER = "streaming final answer";
        private static final String FIRST_TOOL_CALL_ID = "call_stream_1";
        private static final String SECOND_TOOL_CALL_ID = "call_stream_2";

        @Test
        void testPerformStreamSuspendsAndResumeContinuesFromPersistedState() throws Exception {
            List<Prompt> prompts = new CopyOnWriteArrayList<>();

            ActionDefinition streamChatActionDefinition = createStreamChatActionDefinition(
                prompts,
                callNumber -> callNumber == 1 ? suspendingToolCallResponse(FIRST_TOOL_CALL_ID) : finalAnswer());

            SseEmitterHandler performHandler = getPerformFunction(streamChatActionDefinition).apply(
                createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
                createActionContext(new AtomicReference<>()));

            assertThat(performHandler).isInstanceOf(SuspendAwareSseEmitterHandler.class);

            StreamResult performResult = driveHandlerToCompletion(performHandler);

            assertThat(performResult.failed()).isFalse();

            ActionContext.Suspend suspend = ((SuspendAwareSseEmitterHandler) performHandler).getSuspendOrThrow(1L);

            assertThat(suspend).isNotNull();

            String suspendedToolResult = ToolSuspension.getSuspendedToolResult(suspend);

            assertThat(suspendedToolResult).isNotNull();
            assertThat(performResult.events()).doesNotContain(suspendedToolResult);
            assertThat(performResult.events())
                .filteredOn(event -> event instanceof Map<?, ?> eventMap &&
                    AiAgentSseEventType.TOOL_EXECUTION.equals(eventMap.get(AiAgentSseEventType.EVENT_TYPE)))
                .noneMatch(
                    event -> ToolSuspension.isSuspendedToolResult(
                        String.valueOf(((Map<?, ?>) event).get("output")), suspendedToolResult));
            assertThat(getAgentToolSuspension(suspend).pendingToolCallId()).isEqualTo(FIRST_TOOL_CALL_ID);

            SseEmitterHandler resumeHandler = resume(streamChatActionDefinition, suspend, new AtomicReference<>());

            assertThat(resumeHandler).isInstanceOf(SuspendAwareSseEmitterHandler.class);

            StreamResult resumeResult = driveHandlerToCompletion(resumeHandler);

            assertThat(resumeResult.failed()).isFalse();
            assertThat(resumeResult.events()).contains(FINAL_ANSWER);
            assertThat(((SuspendAwareSseEmitterHandler) resumeHandler).getSuspendOrThrow(1L)).isNull();
            assertThat(prompts).hasSize(2);

            ToolResponseMessage resumedToolResponseMessage = findLastToolResponseMessage(prompts.get(1)
                .getInstructions());

            assertThat(resumedToolResponseMessage).isNotNull();
            assertThat(resumedToolResponseMessage.getResponses()).containsExactly(
                new ToolResponseMessage.ToolResponse(FIRST_TOOL_CALL_ID, SUSPENDING_TOOL_NAME, "{\"approved\":true}"));
        }

        @Test
        void testResumeStreamEmitsToolExecutionEventsForToolsCalledAfterResume() throws Exception {
            List<Prompt> prompts = new CopyOnWriteArrayList<>();

            ActionDefinition streamChatActionDefinition = createStreamChatActionDefinition(
                prompts, callNumber -> switch (callNumber) {
                    case 1 -> suspendingToolCallResponse(FIRST_TOOL_CALL_ID);
                    case 2 -> toolCallResponse(LOOK_UP_TOOL_NAME, SECOND_TOOL_CALL_ID);
                    default -> finalAnswer();
                });

            ActionContext.Suspend suspend = performUntilSuspended(streamChatActionDefinition);

            SseEmitterHandler resumeHandler = resume(streamChatActionDefinition, suspend, new AtomicReference<>());

            StreamResult resumeResult = driveHandlerToCompletion(resumeHandler);

            assertThat(resumeResult.failed()).isFalse();
            assertThat(resumeResult.events()).contains(FINAL_ANSWER);

            List<Map<String, Object>> toolExecutionEvents = resumeResult.events()
                .stream()
                .filter(event -> event instanceof Map<?, ?> map &&
                    AiAgentSseEventType.TOOL_EXECUTION.equals(map.get(AiAgentSseEventType.EVENT_TYPE)))
                .map(ResumeTests::toMap)
                .toList();

            assertThat(toolExecutionEvents).hasSize(1);
            assertThat(toolExecutionEvents.getFirst()).containsEntry("toolName", LOOK_UP_TOOL_NAME)
                .containsEntry("output", LOOK_UP_TOOL_RESULT);
        }

        @Test
        void testResumeStreamThatSuspendsAgainRecordsTheNewPendingToolCall() throws Exception {
            List<Prompt> prompts = new CopyOnWriteArrayList<>();

            ActionDefinition streamChatActionDefinition = createStreamChatActionDefinition(
                prompts,
                callNumber -> suspendingToolCallResponse(callNumber == 1 ? FIRST_TOOL_CALL_ID : SECOND_TOOL_CALL_ID));

            ActionContext.Suspend firstSuspend = performUntilSuspended(streamChatActionDefinition);

            SseEmitterHandler resumeHandler = resume(streamChatActionDefinition, firstSuspend, new AtomicReference<>());

            StreamResult resumeResult = driveHandlerToCompletion(resumeHandler);

            assertThat(resumeResult.failed()).isFalse();

            ActionContext.Suspend secondSuspend = ((SuspendAwareSseEmitterHandler) resumeHandler).getSuspendOrThrow(1L);

            assertThat(secondSuspend).isNotNull();
            assertThat(getAgentToolSuspension(secondSuspend).pendingToolCallId()).isEqualTo(SECOND_TOOL_CALL_ID);
        }

        @Test
        void testResumeStreamStoresOnlyTheFinalAssistantMessageInChatMemory() throws Exception {
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();

            chatMemory.add(
                CONVERSATION_ID, List.of(new UserMessage("earlier question"), new AssistantMessage("earlier answer")));

            ChatMemoryFunction.Result chatMemoryResult = ChatMemoryFunction.Result.of(
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .build(),
                chatMemory);

            List<Prompt> prompts = new CopyOnWriteArrayList<>();

            ActionDefinition streamChatActionDefinition = AiAgentStreamChatAction.of(
                mock(AiAgentToolFacade.class),
                createClusterElementDefinitionService(
                    createChatModel(
                        prompts,
                        callNumber -> callNumber == 1 ? suspendingToolCallResponse(FIRST_TOOL_CALL_ID) : finalAnswer()),
                    chatMemoryResult),
                createToolCallingManager());

            SseEmitterHandler performHandler = getPerformFunction(streamChatActionDefinition).apply(
                createInputParameters("TEXT"), createConnectionParameters(), createExtensions(true),
                createActionContext(new AtomicReference<>()));

            assertThat(driveHandlerToCompletion(performHandler).failed()).isFalse();

            ActionContext.Suspend suspend = ((SuspendAwareSseEmitterHandler) performHandler).getSuspendOrThrow(1L);

            assertThat(suspend).isNotNull();

            int memorySizeAfterSuspend = chatMemory.get(CONVERSATION_ID)
                .size();

            MultipleConnectionsResumePerformFunction resumePerformFunction =
                (MultipleConnectionsResumePerformFunction) streamChatActionDefinition.getResumePerform()
                    .orElseThrow();

            SseEmitterHandler resumeHandler = (SseEmitterHandler) resumePerformFunction.apply(
                createInputParameters("TEXT"), createConnectionParameters(), createExtensions(true),
                toPersistedContinueParameters(suspend), MockParametersFactory.create(Map.of("approved", true)),
                createActionContext(new AtomicReference<>()));

            StreamResult resumeResult = driveHandlerToCompletion(resumeHandler);

            assertThat(resumeResult.failed()).isFalse();
            assertThat(resumeResult.events()).contains(FINAL_ANSWER);

            List<Message> memoryAfterResume = chatMemory.get(CONVERSATION_ID);

            assertThat(memoryAfterResume).hasSize(memorySizeAfterSuspend + 1)
                .noneMatch(message -> message instanceof ToolResponseMessage);
            assertThat(memoryAfterResume.getLast()
                .getText()).isEqualTo(FINAL_ANSWER);

            List<String> resumedPromptTexts = prompts.get(1)
                .getInstructions()
                .stream()
                .map(Message::getText)
                .filter(text -> text != null && !text.isEmpty())
                .toList();

            assertThat(resumedPromptTexts).containsOnlyOnce("earlier question", "earlier answer", USER_PROMPT);
        }

        @Test
        void testSuspendedStreamsStoreNoBlankAssistantMessageInChatMemory() throws Exception {
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();

            ChatMemoryFunction.Result chatMemoryResult = ChatMemoryFunction.Result.of(
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .build(),
                chatMemory);

            List<Prompt> prompts = new CopyOnWriteArrayList<>();

            ActionDefinition streamChatActionDefinition = AiAgentStreamChatAction.of(
                mock(AiAgentToolFacade.class),
                createClusterElementDefinitionService(
                    createChatModel(
                        prompts,
                        callNumber -> callNumber == 1
                            ? suspendingToolCallResponse(FIRST_TOOL_CALL_ID)
                            : callNumber == 2 ? suspendingToolCallResponse(SECOND_TOOL_CALL_ID) : finalAnswer()),
                    chatMemoryResult),
                createToolCallingManager());

            SseEmitterHandler performHandler = getPerformFunction(streamChatActionDefinition).apply(
                createInputParameters("TEXT"), createConnectionParameters(), createExtensions(true),
                createActionContext(new AtomicReference<>()));

            assertThat(driveHandlerToCompletion(performHandler).failed()).isFalse();

            ActionContext.Suspend suspend = ((SuspendAwareSseEmitterHandler) performHandler).getSuspendOrThrow(1L);

            assertThat(suspend).isNotNull();
            assertThat(chatMemory.get(CONVERSATION_ID))
                .noneMatch(ResumeTests::isBlankAssistantMessage);

            MultipleConnectionsResumePerformFunction resumePerformFunction =
                (MultipleConnectionsResumePerformFunction) streamChatActionDefinition.getResumePerform()
                    .orElseThrow();

            SseEmitterHandler resumeHandler = (SseEmitterHandler) resumePerformFunction.apply(
                createInputParameters("TEXT"), createConnectionParameters(), createExtensions(true),
                toPersistedContinueParameters(suspend), MockParametersFactory.create(Map.of("approved", true)),
                createActionContext(new AtomicReference<>()));

            assertThat(driveHandlerToCompletion(resumeHandler).failed()).isFalse();
            assertThat(((SuspendAwareSseEmitterHandler) resumeHandler).getSuspendOrThrow(1L)).isNotNull();
            assertThat(chatMemory.get(CONVERSATION_ID))
                .noneMatch(ResumeTests::isBlankAssistantMessage);
        }

        private static boolean isBlankAssistantMessage(Message message) {
            if (!(message instanceof AssistantMessage assistantMessage) || assistantMessage.hasToolCalls()) {
                return false;
            }

            String text = assistantMessage.getText();

            return text == null || text.isBlank();
        }

        @Test
        void testFailedStreamDoesNotReportTheSuspend() throws Exception {
            List<Prompt> prompts = new CopyOnWriteArrayList<>();

            ActionDefinition streamChatActionDefinition = createStreamChatActionDefinition(
                prompts, callNumber -> toolCallResponse(SUSPEND_THEN_FAIL_TOOL_NAME, FIRST_TOOL_CALL_ID));

            AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

            SseEmitterHandler performHandler = getPerformFunction(streamChatActionDefinition).apply(
                createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
                createActionContext(suspendReference));

            StreamResult performResult = driveHandlerToCompletion(performHandler);

            assertThat(performResult.failed()).isTrue();
            assertThat(suspendReference.get()).isNotNull();
            assertThatThrownBy(() -> ((SuspendAwareSseEmitterHandler) performHandler).getSuspendOrThrow(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed");
        }

        private static ActionDefinition createStreamChatActionDefinition(
            List<Prompt> prompts, Function<Integer, ChatResponse> responseFunction) throws Exception {

            return AiAgentStreamChatAction.of(
                mock(AiAgentToolFacade.class),
                createClusterElementDefinitionService(createChatModel(prompts, responseFunction), null),
                createToolCallingManager());
        }

        private static MultipleConnectionsStreamPerformFunction getPerformFunction(ActionDefinition actionDefinition) {
            return (MultipleConnectionsStreamPerformFunction) actionDefinition.getPerform()
                .orElseThrow();
        }

        private static ActionContext.Suspend performUntilSuspended(ActionDefinition streamChatActionDefinition)
            throws Exception {

            SseEmitterHandler performHandler = getPerformFunction(streamChatActionDefinition).apply(
                createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
                createActionContext(new AtomicReference<>()));

            StreamResult performResult = driveHandlerToCompletion(performHandler);

            assertThat(performResult.failed()).isFalse();

            ActionContext.Suspend suspend = ((SuspendAwareSseEmitterHandler) performHandler).getSuspendOrThrow(1L);

            assertThat(suspend).isNotNull();

            return suspend;
        }

        private static SseEmitterHandler resume(
            ActionDefinition streamChatActionDefinition, ActionContext.Suspend suspend,
            AtomicReference<ActionContext.Suspend> suspendReference) throws Exception {

            MultipleConnectionsResumePerformFunction resumePerformFunction =
                (MultipleConnectionsResumePerformFunction) streamChatActionDefinition.getResumePerform()
                    .orElseThrow();

            return (SseEmitterHandler) resumePerformFunction.apply(
                createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
                toPersistedContinueParameters(suspend), MockParametersFactory.create(Map.of("approved", true)),
                createActionContext(suspendReference));
        }

        private static AgentToolSuspension getAgentToolSuspension(ActionContext.Suspend suspend) {
            Map<String, ?> continueParameters = suspend.continueParameters();

            assertThat(continueParameters).containsKey(AgentToolSuspension.CONTINUE_PARAMETER_KEY);

            return (AgentToolSuspension) continueParameters.get(AgentToolSuspension.CONTINUE_PARAMETER_KEY);
        }

        private static StreamResult driveHandlerToCompletion(SseEmitterHandler sseEmitterHandler)
            throws InterruptedException {

            List<Object> events = new CopyOnWriteArrayList<>();
            AtomicReference<Throwable> errorReference = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            List<Runnable> timeoutListeners = new CopyOnWriteArrayList<>();

            SseEmitter sseEmitter = new SseEmitter() {

                @Override
                public void send(Object data) {
                    events.add(data);
                }

                @Override
                public void complete() {
                    latch.countDown();
                }

                @Override
                public void error(Throwable throwable) {
                    errorReference.set(throwable);

                    latch.countDown();
                }

                @Override
                public void addTimeoutListener(Runnable timeoutListener) {
                    timeoutListeners.add(timeoutListener);
                }
            };

            sseEmitterHandler.handle(sseEmitter);

            boolean completed = latch.await(10, TimeUnit.SECONDS);

            assertThat(completed).as("SSE handler did not complete within 10 seconds")
                .isTrue();

            return new StreamResult(events, errorReference.get() != null);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> toMap(Object value) {
            return (Map<String, Object>) value;
        }

        private static ChatResponse finalAnswer() {
            return textResponse(FINAL_ANSWER);
        }

        private static ChatResponse suspendingToolCallResponse(String toolCallId) {
            return toolCallResponse(SUSPENDING_TOOL_NAME, toolCallId);
        }

        private record StreamResult(List<Object> events, boolean failed) {
        }
    }
}
