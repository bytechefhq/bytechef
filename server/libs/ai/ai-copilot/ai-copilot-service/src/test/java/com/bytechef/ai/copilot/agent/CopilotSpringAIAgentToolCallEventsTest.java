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

package com.bytechef.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.ToolCallResultEvent;
import com.agui.core.event.ToolCallStartEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.core.type.EventType;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CopilotSpringAIAgentToolCallEventsTest {

    @Test
    void testToolCallStartReachesTheClientWhileTheToolIsStillRunning() throws Exception {
        CountDownLatch toolEntered = new CountDownLatch(1);
        CountDownLatch releaseTool = new CountDownLatch(1);
        TextToolTextChatModel chatModel = new TextToolTextChatModel();

        ToolCallback buildWorkflowToolCallback = toolCallback(() -> {
            toolEntered.countDown();

            awaitRelease(releaseTool);

            return "{\"workflowId\":\"w1\"}";
        });

        List<BaseEvent> events = new CopyOnWriteArrayList<>();
        CountDownLatch finalized = new CountDownLatch(1);

        StreamingCopilotAgent agent = new StreamingCopilotAgent(newBuilder(chatModel, buildWorkflowToolCallback));

        Thread runThread = new Thread(() -> agent.run(newInput(), collector(events, finalized)));

        runThread.start();

        assertThat(toolEntered.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(events)
            .extracting(BaseEvent::getType)
            .containsSubsequence(
                EventType.TEXT_MESSAGE_START, EventType.TEXT_MESSAGE_CONTENT, EventType.TEXT_MESSAGE_END,
                EventType.TOOL_CALL_START, EventType.TOOL_CALL_ARGS, EventType.TOOL_CALL_END)
            .doesNotContain(EventType.TOOL_CALL_RESULT);

        releaseTool.countDown();

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        ToolCallStartEvent startEvent = firstOf(events, ToolCallStartEvent.class);
        ToolCallResultEvent resultEvent = firstOf(events, ToolCallResultEvent.class);

        assertThat(resultEvent.getToolCallId()).isEqualTo(startEvent.getToolCallId());
        assertThat(resultEvent.getContent()).isEqualTo("{\"workflowId\":\"w1\"}");
        assertThat(events.indexOf(resultEvent)).isGreaterThan(events.indexOf(startEvent));
        assertThat(events)
            .extracting(BaseEvent::getType)
            .endsWith(
                EventType.TOOL_CALL_RESULT, EventType.TEXT_MESSAGE_START, EventType.TEXT_MESSAGE_CONTENT,
                EventType.TEXT_MESSAGE_END, EventType.RUN_FINISHED);
    }

    @Test
    void testTurnEndingToolStartsBeforeItRunsAndEndsTheTurnAfterItsResult() throws Exception {
        TextToolTextChatModel chatModel = new TextToolTextChatModel();
        List<List<EventType>> eventTypesSeenByTheTool = new ArrayList<>();
        List<BaseEvent> events = new CopyOnWriteArrayList<>();

        ToolCallback askUserQuestionToolCallback = toolCallback(() -> {
            eventTypesSeenByTheTool.add(events.stream()
                .map(BaseEvent::getType)
                .toList());

            return "{\"kind\":\"ask-user-question\",\"awaitingAnswer\":true}";
        });

        CountDownLatch finalized = new CountDownLatch(1);

        new StreamingCopilotAgent(newBuilder(chatModel, askUserQuestionToolCallback))
            .run(newInput(), collector(events, finalized));

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(eventTypesSeenByTheTool.getFirst())
            .contains(EventType.TOOL_CALL_START)
            .doesNotContain(EventType.TOOL_CALL_RESULT);
        assertThat(chatModel.streamCalls.get()).isEqualTo(1);
        assertThat(events)
            .extracting(BaseEvent::getType)
            .containsSubsequence(EventType.TOOL_CALL_START, EventType.TOOL_CALL_RESULT, EventType.RUN_FINISHED);
    }

    @Test
    void testFailingToolStillAnswersItsStartedCallWithAnErrorResult() throws Exception {
        ToolCallback failingToolCallback = toolCallback(() -> {
            throw new IllegalStateException("catalog down");
        });

        List<BaseEvent> events = new CopyOnWriteArrayList<>();
        CountDownLatch finalized = new CountDownLatch(1);

        new StreamingCopilotAgent(newBuilder(new TextToolTextChatModel(), failingToolCallback))
            .run(newInput(), collector(events, finalized));

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        ToolCallStartEvent startEvent = firstOf(events, ToolCallStartEvent.class);
        ToolCallResultEvent resultEvent = firstOf(events, ToolCallResultEvent.class);

        assertThat(resultEvent.getToolCallId()).isEqualTo(startEvent.getToolCallId());
        assertThat(resultEvent.getContent()).contains("\"error\"")
            .contains("catalog down");
    }

    private static void awaitRelease(CountDownLatch releaseTool) {
        try {
            if (!releaseTool.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The test never released the tool");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(exception);
        }
    }

    private static <T extends BaseEvent> T firstOf(List<BaseEvent> events, Class<T> eventClass) {
        return events.stream()
            .filter(eventClass::isInstance)
            .map(eventClass::cast)
            .findFirst()
            .orElseThrow();
    }

    private static AgentSubscriber collector(List<BaseEvent> events, CountDownLatch finalized) {
        return new AgentSubscriber() {

            @Override
            public void onEvent(BaseEvent event) {
                events.add(event);
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                finalized.countDown();
            }
        };
    }

    private static RunAgentInput newInput() {
        UserMessage userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("build me a workflow");

        List<BaseMessage> messages = new ArrayList<>();

        messages.add(userMessage);

        State state = new State();

        state.set(CopilotConstants.STATE_TENANT_ID, "acme");

        return new RunAgentInput("thread", "run", state, messages, List.of(), List.of(), null);
    }

    private static SpringAIAgent.Builder newBuilder(ChatModel chatModel, ToolCallback toolCallback) {
        return SpringAIAgent.builder()
            .agentId("test")
            .chatModel(chatModel)
            .systemMessage("system")
            .state(new State())
            .toolCallback(toolCallback);
    }

    private static ToolCallback toolCallback(ToolBody toolBody) {
        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                    .name("buildWorkflow")
                    .description("Build a workflow")
                    .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                return toolBody.run();
            }
        };
    }

    @FunctionalInterface
    private interface ToolBody {

        String run();
    }

    private static final class StreamingCopilotAgent extends CopilotSpringAIAgent {

        private StreamingCopilotAgent(SpringAIAgent.Builder builder) throws AGUIException {
            super(builder, null);
        }
    }

    private static final class TextToolTextChatModel implements ChatModel {

        private final AtomicInteger streamCalls = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The chat model must be streamed");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            if (streamCalls.incrementAndGet() == 1) {
                AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                    "call-1", "function", "buildWorkflow", "{\"request\":\"post to slack\"}");

                AssistantMessage toolCallMessage = AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(toolCall))
                    .build();

                return Flux.just(
                    new ChatResponse(List.of(new Generation(new AssistantMessage("Building it now.")))),
                    new ChatResponse(List.of(new Generation(toolCallMessage))));
            }

            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Done.")))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }
}
