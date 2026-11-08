package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAIAgentRunIsolationTest {

    @Test
    void shouldAnswerEachRunFromItsOwnLatestUserMessage() throws AGUIException, InterruptedException {
        var agent = newAgent();

        assertThat(runToCompletion(agent, List.of(userMessage("message-1", "first user question"))))
            .isEqualTo("echo: first user question");
        assertThat(runToCompletion(agent, List.of(userMessage("message-2", "second user question"))))
            .isEqualTo("echo: second user question");
    }

    @Test
    void shouldAnswerAResentMessageInsteadOfAnEarlierRunsMessage() throws AGUIException, InterruptedException {
        var agent = newAgent();

        runToCompletion(agent, List.of(userMessage("message-1", "first user question")));
        runToCompletion(agent, List.of(userMessage("message-2", "second user question")));

        assertThat(runToCompletion(agent, List.of(userMessage("message-1", "first user question"))))
            .isEqualTo("echo: first user question");
    }

    @Test
    void shouldAnswerEachConcurrentRunFromItsOwnMessage() throws Exception {
        var agent = newAgent();
        int runCount = 64;

        ExecutorService executorService = Executors.newFixedThreadPool(8);

        try {
            var startSignal = new CountDownLatch(1);
            List<Future<String>> answerFutures = new ArrayList<>();

            for (int index = 0; index < runCount; index++) {
                var question = "question " + index;
                var messageId = "message-" + index;

                answerFutures.add(executorService.submit(() -> {
                    startSignal.await();

                    return runToCompletion(agent, List.of(userMessage(messageId, question)));
                }));
            }

            startSignal.countDown();

            for (int index = 0; index < runCount; index++) {
                assertThat(answerFutures.get(index).get(30, TimeUnit.SECONDS))
                    .isEqualTo("echo: question " + index);
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void shouldNotGrowTheAgentMessagesAcrossRuns() throws AGUIException, InterruptedException {
        var agent = newAgent();

        for (int index = 0; index < 5; index++) {
            runToCompletion(agent, List.of(userMessage("message-" + index, "question " + index)));
        }

        assertThat(agent.getMessages()).isEmpty();
    }

    @Test
    void shouldKeepTheBuilderMessagesUnchangedAcrossRuns() throws AGUIException, InterruptedException {
        var initialMessage = userMessage("initial", "initial question");
        var initialMessages = new ArrayList<BaseMessage>(List.of(initialMessage));

        var agent = new SpringAIAgent(SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new EchoLatestUserMessageChatModel())
            .systemMessage("system")
            .state(new State())
            .messages(initialMessages));

        assertThat(runToCompletion(agent, List.of(userMessage("message-1", "run question"))))
            .isEqualTo("echo: run question");
        assertThat(agent.getMessages()).containsExactly(initialMessage);
        assertThat(initialMessages).containsExactly(initialMessage);
    }

    @Test
    void shouldPassTheRunStateToTheFinalizedSubscriber() throws AGUIException, InterruptedException {
        var agent = newAgent();
        var runState = new State();

        assertThat(finalizedState(agent, runState)).isSameAs(runState);
    }

    @Test
    void shouldPassTheRunStateToTheFinalizedSubscriberOfAFailedRun() throws AGUIException, InterruptedException {
        var agent = new SpringAIAgent(SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new FailingChatModel())
            .systemMessage("system")
            .state(new State()));
        var runState = new State();

        assertThat(finalizedState(agent, runState)).isSameAs(runState);
    }

    private static State finalizedState(SpringAIAgent agent, State runState) throws InterruptedException {
        var input = new RunAgentInput(
            "thread", "run", runState, List.of(userMessage("message-1", "question")), List.of(), new ArrayList<>(),
            null);

        var finalizedState = new AtomicReference<State>();
        var finalized = new CountDownLatch(1);

        agent.run(input, new AgentSubscriber() {
            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                finalizedState.set(params.state());

                finalized.countDown();
            }
        });

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        return finalizedState.get();
    }

    private static SpringAIAgent newAgent() throws AGUIException {
        return new SpringAIAgent(SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new EchoLatestUserMessageChatModel())
            .systemMessage("system")
            .state(new State()));
    }

    private static UserMessage userMessage(String messageId, String content) {
        var userMessage = new UserMessage();

        userMessage.setId(messageId);
        userMessage.setContent(content);

        return userMessage;
    }

    private static String runToCompletion(SpringAIAgent agent, List<BaseMessage> messages)
        throws InterruptedException {

        var input = new RunAgentInput(
            "thread", "run", new State(), messages, List.of(), new ArrayList<>(), null);

        var deltas = new CopyOnWriteArrayList<String>();
        var finalized = new CountDownLatch(1);

        agent.run(input, new AgentSubscriber() {
            @Override
            public void onEvent(BaseEvent event) {
                if (event instanceof TextMessageContentEvent textMessageContentEvent) {
                    deltas.add(textMessageContentEvent.getDelta());
                }
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                finalized.countDown();
            }
        });

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();

        return String.join("", deltas);
    }

    private static class FailingChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The chat model must be streamed");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.error(new IllegalStateException("model failed"));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }

    private static class EchoLatestUserMessageChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The chat model must be streamed");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            String latestUserText = prompt.getInstructions()
                .stream()
                .filter(message -> message.getMessageType() == MessageType.USER)
                .map(Message::getText)
                .reduce((previous, next) -> next)
                .orElse("");

            return Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("echo: " + latestUserText)))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }
}
