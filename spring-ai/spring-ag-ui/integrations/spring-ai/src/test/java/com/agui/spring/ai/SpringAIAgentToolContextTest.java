package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.ToolCallStartEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAIAgentToolContextTest {

    @Test
    void shouldHandTheRunSubscriberToTheToolContextHookAndReachTheToolOnItsOwnThread() throws Exception {
        var events = new CopyOnWriteArrayList<BaseEvent>();
        var finalized = new CountDownLatch(1);
        var receivedContext = new AtomicReference<ToolContext>();
        var toolThread = new AtomicReference<Thread>();

        AgentSubscriber subscriber = new AgentSubscriber() {
            @Override
            public void onEvent(BaseEvent event) {
                events.add(event);
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                finalized.countDown();
            }
        };

        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new ToolThenTextChatModel())
            .systemMessage("system")
            .state(new State())
            .toolCallback(new RecordingToolCallback(receivedContext, toolThread));

        var agent = new SpringAIAgent(builder) {
            @Override
            protected Map<String, Object> toolContext(RunAgentInput input, AgentSubscriber runSubscriber) {
                return Map.of("subscriber", runSubscriber);
            }
        };

        agent.run(newInput(), subscriber);

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        var startEvent = (ToolCallStartEvent) events.stream()
            .filter(ToolCallStartEvent.class::isInstance)
            .findFirst()
            .orElseThrow();

        assertThat(receivedContext.get().getContext())
            .containsEntry("subscriber", subscriber)
            .containsEntry(AgUiFunctionToolCallback.TOOL_CALL_ID_KEY, startEvent.getToolCallId());
        assertThat(toolThread.get()).isNotEqualTo(Thread.currentThread());
    }

    @Test
    void shouldDefaultTheSubscriberHookToTheInputOnlyHook() throws AGUIException {
        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new ToolThenTextChatModel())
            .systemMessage("system")
            .state(new State());

        var agent = new SpringAIAgent(builder) {
            @Override
            protected Map<String, Object> toolContext(RunAgentInput input) {
                return Map.of("runId", input.runId());
            }
        };

        assertThat(agent.toolContext(newInput(), new AgentSubscriber() {})).containsEntry("runId", "run");
    }

    private static RunAgentInput newInput() {
        var userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("Find an action that sends an email");

        return new RunAgentInput(
            "thread", "run", new State(), List.of(userMessage), List.of(), new ArrayList<>(), null);
    }

    private static class ToolThenTextChatModel implements ChatModel {

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

                return Flux.just(new ChatResponse(List.of(new Generation(toolCallMessage))));
            }

            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Found it.")))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }

    private record RecordingToolCallback(AtomicReference<ToolContext> receivedContext, AtomicReference<Thread> toolThread)
        implements ToolCallback {

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
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            receivedContext.set(toolContext);
            toolThread.set(Thread.currentThread());

            return "{\"tasks\":[]}";
        }
    }
}
