package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.event.ToolCallResultEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.core.type.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
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

class SpringAIAgentTurnEndingTest {

    private static final String AWAITING_RESULT = "{\"kind\":\"ask-user-question\",\"awaitingAnswer\":true}";

    @Test
    void shouldEndTheTurnWhenAToolResultAwaitsTheUser() throws AGUIException, InterruptedException {
        var chatModel = new AskThenAnswerChatModel();

        var agent = newAgent(chatModel, true);

        var events = runToCompletion(agent);

        assertThat(chatModel.streamCalls).hasValue(1);
        assertThat(events)
            .filteredOn(event -> event instanceof ToolCallResultEvent)
            .extracting(event -> ((ToolCallResultEvent) event).getContent())
            .containsExactly(AWAITING_RESULT);
        assertThat(events).noneMatch(event -> event instanceof TextMessageContentEvent);
        assertThat(events)
            .extracting(BaseEvent::getType)
            .last()
            .isEqualTo(EventType.RUN_FINISHED);
    }

    @Test
    void shouldReturnTheToolResultToTheModelWhenNoResultEndsTheTurn() throws AGUIException, InterruptedException {
        var chatModel = new AskThenAnswerChatModel();

        var agent = newAgent(chatModel, false);

        var events = runToCompletion(agent);

        assertThat(chatModel.streamCalls).hasValue(2);
        assertThat(events)
            .filteredOn(event -> event instanceof TextMessageContentEvent)
            .extracting(event -> ((TextMessageContentEvent) event).getDelta())
            .containsExactly("Great choices!");
    }

    private static SpringAIAgent newAgent(ChatModel chatModel, boolean endsTurn) throws AGUIException {
        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(chatModel)
            .systemMessage("system")
            .state(new State())
            .toolCallback(new AskUserToolCallback());

        return new SpringAIAgent(builder) {
            @Override
            protected boolean endsTurn(ToolResponseMessage.ToolResponse toolResponse) {
                return endsTurn && toolResponse.responseData().contains("\"awaitingAnswer\":true");
            }
        };
    }

    private static List<BaseEvent> runToCompletion(SpringAIAgent agent) throws InterruptedException {
        var userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("Remind the team about standup");

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

    private static class AskThenAnswerChatModel implements ChatModel {

        private final AtomicInteger streamCalls = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The chat model must be streamed");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            if (streamCalls.incrementAndGet() == 1) {
                var toolCall = new AssistantMessage.ToolCall("call-1", "function", "askUserQuestion", "{}");

                var assistantMessage = AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(toolCall))
                    .build();

                return Flux.just(new ChatResponse(List.of(new Generation(assistantMessage))));
            }

            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Great choices!")))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }

    private static class AskUserToolCallback implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("askUserQuestion")
                .description("Ask the user")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return AWAITING_RESULT;
        }
    }
}
