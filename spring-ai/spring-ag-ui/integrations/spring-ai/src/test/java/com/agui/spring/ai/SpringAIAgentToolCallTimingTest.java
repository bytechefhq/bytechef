package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.ToolCallArgsEvent;
import com.agui.core.event.ToolCallEndEvent;
import com.agui.core.event.ToolCallResultEvent;
import com.agui.core.event.ToolCallStartEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.core.type.EventType;
import org.junit.jupiter.api.Test;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAIAgentToolCallTimingTest {

    @Test
    void shouldEmitToolCallStartArgsAndEndBeforeTheToolRunsWhenSplittingText() throws Exception {
        var toolEntered = new CountDownLatch(1);
        var releaseTool = new CountDownLatch(1);
        var events = new CopyOnWriteArrayList<BaseEvent>();
        var finalized = new CountDownLatch(1);

        var agent = newAgent(true, new BlockingToolCallback(toolEntered, releaseTool));

        var runThread = new Thread(() -> agent.run(newInput(), collector(events, finalized)));

        runThread.start();

        assertThat(toolEntered.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(events)
            .extracting(BaseEvent::getType)
            .contains(EventType.TOOL_CALL_START, EventType.TOOL_CALL_ARGS, EventType.TOOL_CALL_END)
            .doesNotContain(EventType.TOOL_CALL_RESULT);

        releaseTool.countDown();

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        assertToolCallEventsShareOneIdInOrder(events, "{\"tasks\":[]}");
    }

    @Test
    void shouldStillDeferEveryToolCallEventInOrderWhenNotSplittingText() throws Exception {
        var events = new CopyOnWriteArrayList<BaseEvent>();
        var finalized = new CountDownLatch(1);
        var toolEntered = new CountDownLatch(1);
        var releaseTool = new CountDownLatch(0);

        var agent = newAgent(false, new BlockingToolCallback(toolEntered, releaseTool));

        agent.run(newInput(), collector(events, finalized));

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(events)
            .extracting(BaseEvent::getType)
            .containsExactly(
                EventType.RUN_STARTED,
                EventType.TEXT_MESSAGE_START, EventType.TEXT_MESSAGE_CONTENT, EventType.TEXT_MESSAGE_CONTENT,
                EventType.TEXT_MESSAGE_END,
                EventType.TOOL_CALL_START, EventType.TOOL_CALL_ARGS, EventType.TOOL_CALL_END,
                EventType.TOOL_CALL_RESULT,
                EventType.RUN_FINISHED);

        assertToolCallEventsShareOneIdInOrder(events, "{\"tasks\":[]}");
    }

    @Test
    void shouldEmitAnErrorResultForTheStartedCallWhenTheToolFails() throws Exception {
        var events = new CopyOnWriteArrayList<BaseEvent>();
        var finalized = new CountDownLatch(1);

        var agent = newAgent(true, new FailingToolCallback());

        agent.run(newInput(), collector(events, finalized));

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        var startEvent = (ToolCallStartEvent) events.stream()
            .filter(event -> event instanceof ToolCallStartEvent)
            .findFirst()
            .orElseThrow();

        var resultEvent = (ToolCallResultEvent) events.stream()
            .filter(event -> event instanceof ToolCallResultEvent)
            .findFirst()
            .orElseThrow();

        assertThat(resultEvent.getToolCallId()).isEqualTo(startEvent.getToolCallId());
        assertThat(resultEvent.getContent()).contains("\"error\"").contains("search index unavailable");
        assertThat(events.indexOf(resultEvent)).isGreaterThan(events.indexOf(startEvent));
    }

    private static void assertToolCallEventsShareOneIdInOrder(List<BaseEvent> events, String expectedResult) {
        var startEvent = (ToolCallStartEvent) single(events, ToolCallStartEvent.class);
        var argsEvent = (ToolCallArgsEvent) single(events, ToolCallArgsEvent.class);
        var endEvent = (ToolCallEndEvent) single(events, ToolCallEndEvent.class);
        var resultEvent = (ToolCallResultEvent) single(events, ToolCallResultEvent.class);

        assertThat(startEvent.getToolCallName()).isEqualTo("searchTasks");
        assertThat(List.of(argsEvent.getToolCallId(), endEvent.getToolCallId(), resultEvent.getToolCallId()))
            .containsOnly(startEvent.getToolCallId());
        assertThat(resultEvent.getMessageId()).isEqualTo(startEvent.getParentMessageId());
        assertThat(resultEvent.getContent()).isEqualTo(expectedResult);
        assertThat(List.of(
            events.indexOf(startEvent), events.indexOf(argsEvent), events.indexOf(endEvent),
            events.indexOf(resultEvent)))
            .isSorted();
    }

    private static BaseEvent single(List<BaseEvent> events, Class<? extends BaseEvent> eventClass) {
        var matches = events.stream()
            .filter(eventClass::isInstance)
            .toList();

        assertThat(matches).hasSize(1);

        return matches.getFirst();
    }

    private static SpringAIAgent newAgent(boolean splitsTextAtToolCalls, ToolCallback toolCallback)
        throws AGUIException {

        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new TextToolTextChatModel())
            .systemMessage("system")
            .state(new State())
            .toolCallback(toolCallback);

        return new SpringAIAgent(builder) {
            @Override
            protected boolean splitsTextAtToolCalls() {
                return splitsTextAtToolCalls;
            }
        };
    }

    private static RunAgentInput newInput() {
        var userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("Find an action that sends an email");

        return new RunAgentInput(
            "thread", "run", new State(), List.of(userMessage), List.of(), new ArrayList<>(), null);
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

    private static ToolDefinition searchTasksDefinition() {
        return ToolDefinition.builder()
            .name("searchTasks")
            .description("Search the tasks")
            .inputSchema("{\"type\":\"object\",\"properties\":{}}")
            .build();
    }

    private static class TextToolTextChatModel implements ChatModel {

        private final AtomicInteger streamCalls = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The chat model must be streamed");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            if (streamCalls.incrementAndGet() == 1) {
                var toolCall = new AssistantMessage.ToolCall("call-1", "function", "searchTasks", "{\"query\":\"email\"}");

                var toolCallMessage = AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(toolCall))
                    .build();

                return Flux.just(
                    new ChatResponse(List.of(new Generation(new AssistantMessage("Let me look that up.")))),
                    new ChatResponse(List.of(new Generation(toolCallMessage))));
            }

            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Found it.")))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }

    private static class BlockingToolCallback implements ToolCallback {

        private final CountDownLatch toolEntered;
        private final CountDownLatch releaseTool;

        private BlockingToolCallback(CountDownLatch toolEntered, CountDownLatch releaseTool) {
            this.toolEntered = toolEntered;
            this.releaseTool = releaseTool;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return searchTasksDefinition();
        }

        @Override
        public String call(String toolInput) {
            toolEntered.countDown();

            try {
                if (!releaseTool.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("The test never released the tool");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException(exception);
            }

            return "{\"tasks\":[]}";
        }
    }

    private static class FailingToolCallback implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return searchTasksDefinition();
        }

        @Override
        public String call(String toolInput) {
            throw new IllegalStateException("search index unavailable");
        }
    }
}
