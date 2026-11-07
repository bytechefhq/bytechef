package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import io.micrometer.context.ContextRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
class SpringAIAgentContextPropagationTest {

    private static final String ACCESSOR_KEY = "test.environment";
    private static final ThreadLocal<String> ENVIRONMENT_HOLDER = new ThreadLocal<>();

    @BeforeAll
    static void enableContextPropagation() {
        ContextRegistry.getInstance()
            .registerThreadLocalAccessor(
                ACCESSOR_KEY, ENVIRONMENT_HOLDER::get, ENVIRONMENT_HOLDER::set, ENVIRONMENT_HOLDER::remove);

        Hooks.enableAutomaticContextPropagation();
    }

    @AfterAll
    static void disableContextPropagation() {
        Hooks.disableAutomaticContextPropagation();

        ContextRegistry.getInstance()
            .removeThreadLocalAccessor(ACCESSOR_KEY);
    }

    @Test
    void testThreadLocalBoundAroundRunIsVisibleInsideStreamOnAnotherScheduler() throws Exception {
        AtomicReference<String> observedEnvironment = new AtomicReference<>();

        ChatModel threadHoppingChatModel = new ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException("call() is not exercised by the streaming path");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux
                    .defer(() -> {
                        observedEnvironment.set(ENVIRONMENT_HOLDER.get());

                        return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
                    })
                    .subscribeOn(Schedulers.boundedElastic());
            }
        };

        SpringAIAgent agent = SpringAIAgent.builder()
            .chatModel(threadHoppingChatModel)
            .agentId("test-agent")
            .systemMessage("You are a test agent.")
            .messages(new ArrayList<>())
            .build();

        UserMessage userMessage = new UserMessage();

        userMessage.setId(UUID.randomUUID()
            .toString());
        userMessage.setContent("hi");

        RunAgentInput input = new RunAgentInput(
            "thread-1", "run-1", new State(), List.of(userMessage), List.of(), List.of(), null);

        TerminalLatchSubscriber subscriber = new TerminalLatchSubscriber();

        ENVIRONMENT_HOLDER.set("development");

        try {
            agent.run(input, subscriber);
        } finally {
            ENVIRONMENT_HOLDER.remove();
        }

        boolean finished = subscriber.terminalLatch.await(5, TimeUnit.SECONDS);

        assertThat(finished)
            .as("run should reach a terminal lifecycle signal")
            .isTrue();
        assertThat(observedEnvironment.get())
            .as("ThreadLocal bound around run() must be restored inside the stream on the other scheduler")
            .isEqualTo("development");
    }

    private static final class TerminalLatchSubscriber implements AgentSubscriber {

        private final CountDownLatch terminalLatch = new CountDownLatch(1);

        @Override
        public void onRunFinalized(AgentSubscriberParams params) {
            terminalLatch.countDown();
        }

        @Override
        public void onRunFailed(AgentSubscriberParams params, Throwable error) {
            terminalLatch.countDown();
        }
    }
}
