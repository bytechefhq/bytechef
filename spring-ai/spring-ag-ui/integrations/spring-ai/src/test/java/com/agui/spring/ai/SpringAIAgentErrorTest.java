package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.core.type.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAIAgentErrorTest {

    @Test
    void shouldEmitRunErrorAndFinalizeWhenBuildingThePromptThrows() throws AGUIException {
        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new UnusedChatModel())
            .systemMessage("system")
            .state(new State());

        var agent = new SpringAIAgent(builder) {
            @Override
            protected SystemMessage createSystemMessage(State state, List<Context> context) {
                throw new IllegalStateException("Workflow with id: abc does not exist");
            }
        };

        var userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("Build a workflow");

        var input = new RunAgentInput(
            "thread", "run", new State(), List.of(userMessage), List.of(), new ArrayList<>(), null);

        var events = new ArrayList<BaseEvent>();
        var finalized = new AtomicBoolean(false);

        agent.run(input, new AgentSubscriber() {
            @Override
            public void onEvent(BaseEvent event) {
                events.add(event);
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                finalized.set(true);
            }
        });

        assertThat(events)
            .extracting(BaseEvent::getType)
            .containsExactly(EventType.RUN_STARTED, EventType.TEXT_MESSAGE_START, EventType.RUN_ERROR);
        assertThat(((RunErrorEvent) events.get(2)).getError()).isEqualTo("Workflow with id: abc does not exist");
        assertThat(finalized).isTrue();
    }

    @Test
    void shouldEmitRunErrorWithTheHookMessageAndFinalizeWhenTheChatStreamFails() throws AGUIException, InterruptedException {
        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new FailingStreamChatModel())
            .systemMessage("system")
            .state(new State());

        var agent = new SpringAIAgent(builder) {
            @Override
            protected String runErrorMessage(RunAgentInput input, Throwable throwable) {
                return "Run failed. Reference: " + input.runId();
            }
        };

        var events = new CopyOnWriteArrayList<BaseEvent>();
        var finalized = new CountDownLatch(1);

        agent.run(newInput(), new AgentSubscriber() {
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
        assertThat(events)
            .extracting(BaseEvent::getType)
            .containsExactly(EventType.RUN_STARTED, EventType.TEXT_MESSAGE_START, EventType.RUN_ERROR);
        assertThat(((RunErrorEvent) events.get(2)).getError()).isEqualTo("Run failed. Reference: run");
    }

    @Test
    void shouldStartTheRunBeforeReportingAMissingUserMessage() throws AGUIException {
        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new UnusedChatModel())
            .systemMessage("system")
            .state(new State());

        var agent = new SpringAIAgent(builder);

        var input = new RunAgentInput("thread", "run", new State(), List.of(), List.of(), new ArrayList<>(), null);

        var events = new ArrayList<BaseEvent>();
        var finalized = new AtomicBoolean(false);

        agent.run(input, new AgentSubscriber() {
            @Override
            public void onEvent(BaseEvent event) {
                events.add(event);
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                finalized.set(true);
            }
        });

        assertThat(events)
            .extracting(BaseEvent::getType)
            .containsExactly(EventType.RUN_STARTED, EventType.RUN_ERROR);
        assertThat(finalized).isTrue();
    }

    @Test
    void shouldFinalizeTheRunEvenWhenEmittingTheRunErrorThrows() throws AGUIException {
        var builder = SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new UnusedChatModel())
            .systemMessage("system")
            .state(new State());

        var agent = new SpringAIAgent(builder) {
            @Override
            protected SystemMessage createSystemMessage(State state, List<Context> context) {
                throw new IllegalStateException("prompt failed");
            }
        };

        var finalized = new AtomicBoolean(false);

        try {
            agent.run(newInput(), new AgentSubscriber() {
                @Override
                public void onRunErrorEvent(RunErrorEvent event) {
                    throw new IllegalStateException("client gone");
                }

                @Override
                public void onRunFinalized(AgentSubscriberParams params) {
                    finalized.set(true);
                }
            });
        } catch (IllegalStateException exception) {
            assertThat(exception).hasMessage("client gone");
        }

        assertThat(finalized).isTrue();
    }

    private static RunAgentInput newInput() {
        var userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("Build a workflow");

        return new RunAgentInput("thread", "run", new State(), List.of(userMessage), List.of(), new ArrayList<>(), null);
    }

    private static class FailingStreamChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The chat model must be streamed");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.error(new IllegalStateException("Incorrect API key provided: sk-proj-1234"));
        }
    }

    private static class UnusedChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The chat model must not be reached");
        }
    }
}
