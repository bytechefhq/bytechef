package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.event.TextMessageEndEvent;
import com.agui.core.event.TextMessageStartEvent;
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
import static org.assertj.core.groups.Tuple.tuple;

class SpringAIAgentTextSplittingTest {

    @Test
    void shouldEndTheTextMessageAtAToolCallAndStartANewOneAfterIt() throws AGUIException, InterruptedException {
        var events = runToCompletion(newAgent(true));

        assertThat(events)
            .extracting(BaseEvent::getType)
            .containsExactly(
                EventType.RUN_STARTED,
                EventType.TEXT_MESSAGE_START, EventType.TEXT_MESSAGE_CONTENT, EventType.TEXT_MESSAGE_END,
                EventType.TOOL_CALL_START, EventType.TOOL_CALL_ARGS, EventType.TOOL_CALL_END,
                EventType.TOOL_CALL_RESULT,
                EventType.TEXT_MESSAGE_START, EventType.TEXT_MESSAGE_CONTENT, EventType.TEXT_MESSAGE_END,
                EventType.RUN_FINISHED);

        List<String> textMessageIds = events.stream()
            .filter(event -> event instanceof TextMessageStartEvent)
            .map(event -> ((TextMessageStartEvent) event).getMessageId())
            .toList();

        assertThat(textMessageIds).doesNotHaveDuplicates();

        assertThat(events)
            .filteredOn(event -> event instanceof TextMessageContentEvent)
            .extracting(
                event -> ((TextMessageContentEvent) event).getMessageId(),
                event -> ((TextMessageContentEvent) event).getDelta())
            .containsExactly(
                tuple(textMessageIds.get(0), "Let me look that up."),
                tuple(textMessageIds.get(1), "Found it."));

        assertThat(events)
            .filteredOn(event -> event instanceof TextMessageEndEvent)
            .extracting(event -> ((TextMessageEndEvent) event).getMessageId())
            .containsExactlyElementsOf(textMessageIds);

        assertThat(events)
            .filteredOn(event -> event instanceof ToolCallStartEvent)
            .extracting(event -> ((ToolCallStartEvent) event).getParentMessageId())
            .containsExactly(textMessageIds.get(0));
    }

    @Test
    void shouldKeepOneTextMessageAndDeferTheToolEventsByDefault() throws AGUIException, InterruptedException {
        var events = runToCompletion(newAgent(false));

        assertThat(events)
            .extracting(BaseEvent::getType)
            .containsExactly(
                EventType.RUN_STARTED,
                EventType.TEXT_MESSAGE_START, EventType.TEXT_MESSAGE_CONTENT, EventType.TEXT_MESSAGE_CONTENT,
                EventType.TEXT_MESSAGE_END,
                EventType.TOOL_CALL_START, EventType.TOOL_CALL_ARGS, EventType.TOOL_CALL_END,
                EventType.TOOL_CALL_RESULT,
                EventType.RUN_FINISHED);

        assertThat(events)
            .filteredOn(event -> event instanceof TextMessageContentEvent)
            .extracting(event -> ((TextMessageContentEvent) event).getMessageId())
            .containsOnly(((TextMessageStartEvent) events.get(1)).getMessageId());
    }

    private static SpringAIAgent newAgent(boolean splitsTextAtToolCalls) throws AGUIException {
        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new TextToolTextChatModel())
            .systemMessage("system")
            .state(new State())
            .toolCallback(new SearchToolCallback());

        return new SpringAIAgent(builder) {
            @Override
            protected boolean splitsTextAtToolCalls() {
                return splitsTextAtToolCalls;
            }
        };
    }

    private static List<BaseEvent> runToCompletion(SpringAIAgent agent) throws InterruptedException {
        var userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("Find an action that sends an email");

        var input = new RunAgentInput(
            "thread", "run", new State(), List.of(userMessage), List.of(), new ArrayList<>(), null);

        var events = new CopyOnWriteArrayList<BaseEvent>();
        var finalized = new CountDownLatch(1);

        agent.run(input, new AgentSubscriber() {
            @Override
            public void onEvent(BaseEvent event) {
                events.add(event);
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                finalized.countDown();
            }
        });

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        return events;
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
                var toolCall = new AssistantMessage.ToolCall("call-1", "function", "searchTasks", "{}");

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

    private static class SearchToolCallback implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("searchTasks")
                .description("Search the tasks")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return "[{\"name\":\"gmail/sendEmail\"}]";
        }
    }
}
