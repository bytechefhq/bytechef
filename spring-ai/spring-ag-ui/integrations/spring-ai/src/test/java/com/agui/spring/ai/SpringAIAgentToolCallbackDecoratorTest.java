package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.ToolCallResultEvent;
import com.agui.core.event.ToolCallStartEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.core.type.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAIAgentToolCallbackDecoratorTest {

    @Test
    void shouldEmitToolEventsForAToolTheAdvisorAddsThroughTheDecorator() throws AGUIException, InterruptedException {
        var chatModel = new CallThenAnswerChatModel();

        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(chatModel)
            .systemMessage("system")
            .state(new State())
            .advisor(new CatalogToolAdvisor(new ListComponentsToolCallback()));

        var events = runToCompletion(new SpringAIAgent(builder));

        assertThat(events).noneMatch(event -> event instanceof RunErrorEvent);
        assertThat(events)
            .extracting(BaseEvent::getType)
            .containsSubsequence(
                EventType.TOOL_CALL_START, EventType.TOOL_CALL_ARGS, EventType.TOOL_CALL_END,
                EventType.TOOL_CALL_RESULT);
        assertThat(events)
            .filteredOn(event -> event instanceof ToolCallStartEvent)
            .extracting(event -> ((ToolCallStartEvent) event).getToolCallName())
            .containsExactly("listComponents");
        assertThat(events)
            .filteredOn(event -> event instanceof ToolCallResultEvent)
            .extracting(event -> ((ToolCallResultEvent) event).getContent())
            .containsExactly("[\"slack\"]");
    }

    private static List<BaseEvent> runToCompletion(SpringAIAgent agent) throws InterruptedException {
        var userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("Which components exist?");

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

    private static final class CatalogToolAdvisor extends ToolCallingAdvisor {

        private final ToolCallback catalogToolCallback;

        private CatalogToolAdvisor(ToolCallback catalogToolCallback) {
            super(
                ToolCallingManager.builder()
                    .build(),
                DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER, DEFAULT_ORDER, true);

            this.catalogToolCallback = catalogToolCallback;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected ChatClientRequest doBeforeStream(
            ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

            Object decorator = chatClientRequest.context()
                .get(SpringAIAgent.TOOL_CALLBACK_DECORATOR_KEY);

            ToolCallback toolCallback = decorator instanceof UnaryOperator<?> unaryOperator
                ? ((UnaryOperator<ToolCallback>) unaryOperator).apply(catalogToolCallback)
                : catalogToolCallback;

            ToolCallingChatOptions toolOptions = (ToolCallingChatOptions) chatClientRequest.prompt()
                .getOptions();

            ToolCallingChatOptions toolOptionsCopy = ((ToolCallingChatOptions.Builder<?>) toolOptions.mutate())
                .toolCallbacks(List.of(toolCallback))
                .build();

            return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt()
                    .mutate()
                    .chatOptions(toolOptionsCopy)
                    .build())
                .build();
        }
    }

    private static final class CallThenAnswerChatModel implements ChatModel {

        private final AtomicInteger streamCalls = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The chat model must be streamed");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            if (streamCalls.incrementAndGet() == 1) {
                var toolCall = new AssistantMessage.ToolCall("call-1", "function", "listComponents", "{}");

                var assistantMessage = AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(toolCall))
                    .build();

                return Flux.just(new ChatResponse(List.of(new Generation(assistantMessage))));
            }

            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Slack is available.")))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }

    private static final class ListComponentsToolCallback implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("listComponents")
                .description("List the components")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return "[\"slack\"]";
        }
    }
}
