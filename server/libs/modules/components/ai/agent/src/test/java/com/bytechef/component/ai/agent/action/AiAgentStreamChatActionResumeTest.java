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
import static org.mockito.Mockito.mock;

import com.bytechef.component.ai.agent.facade.AiAgentToolFacade;
import com.bytechef.component.ai.agent.tool.AgentToolSuspension;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.definition.MultipleConnectionsResumePerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentStreamChatActionResumeTest {

    private static final String FINAL_ANSWER = "streaming final answer";
    private static final String FIRST_TOOL_CALL_ID = "call_stream_1";
    private static final String SECOND_TOOL_CALL_ID = "call_stream_2";

    @Test
    void testPerformStreamSuspendsAndResumeContinuesFromPersistedState() throws Exception {
        List<Prompt> prompts = new CopyOnWriteArrayList<>();

        ActionDefinition streamChatActionDefinition = createStreamChatActionDefinition(
            prompts, callNumber -> callNumber == 1 ? suspendingToolCallResponse(FIRST_TOOL_CALL_ID) : finalAnswer());

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
            .map(AiAgentStreamChatActionResumeTest::toMap)
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

        ChatMemoryFunction.Result chatMemoryResult = new ChatMemoryFunction.Result(
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

        ChatMemoryFunction.Result chatMemoryResult = new ChatMemoryFunction.Result(
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
            .noneMatch(AiAgentStreamChatActionResumeTest::isBlankAssistantMessage);

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
            .noneMatch(AiAgentStreamChatActionResumeTest::isBlankAssistantMessage);
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
