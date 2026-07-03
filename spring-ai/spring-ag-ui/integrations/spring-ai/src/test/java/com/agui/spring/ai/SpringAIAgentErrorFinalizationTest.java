package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.type.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a chat stream which errors mid-flight still finalizes the run. Before the fix, the error branch of
 * {@link SpringAIAgent#run} emitted a {@code RUN_ERROR} event but never invoked {@code onRunFinalized}, so a host
 * subscriber that owns an event sink (the AI Hub in-flight run registry) never completed that sink after a failure and
 * leaned on a 30-minute TTL backstop instead. This test pins the contract: a streaming failure emits exactly one
 * {@code RUN_ERROR} carrying the upstream message AND finalizes the run exactly once, mirroring the successful path's
 * terminal sequence.
 *
 * @author Ivica Cardic
 */
class SpringAIAgentErrorFinalizationTest {

    @Test
    void testStreamErrorEmitsRunErrorAndFinalizes() throws Exception {
        ChatModel erroringChatModel = new ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException("call() is not exercised by the streaming path");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.error(new RuntimeException("upstream boom"));
            }
        };

        SpringAIAgent agent = SpringAIAgent.builder()
            .chatModel(erroringChatModel)
            .agentId("test-agent")
            .systemMessage("You are a test agent.")
            .messages(new ArrayList<>())
            .build();

        UserMessage userMessage = new UserMessage();

        userMessage.setId(UUID.randomUUID()
            .toString());
        userMessage.setContent("hi");

        RecordingSubscriber subscriber = new RecordingSubscriber();

        RunAgentParameters parameters = RunAgentParameters.builder()
            .threadId("thread-1")
            .runId("run-1")
            .messages(List.of(userMessage))
            .build();

        agent.runAgent(parameters, subscriber);

        // The run executes on a CompletableFuture worker thread; await the terminal signal rather than racing it.
        boolean finalized = subscriber.terminalLatch.await(5, TimeUnit.SECONDS);

        assertThat(finalized)
            .as("run should reach a terminal lifecycle signal")
            .isTrue();
        assertThat(subscriber.runErrorMessages)
            .containsExactly("upstream boom");
        assertThat(subscriber.finalizedCount.get())
            .as("onRunFinalized must fire exactly once after the error event")
            .isEqualTo(1);
        assertThat(subscriber.runFailedCount.get())
            .as("the in-band RUN_ERROR event replaces an onRunFailed sink-error")
            .isZero();
    }

    private static final class RecordingSubscriber implements AgentSubscriber {

        private final List<String> runErrorMessages = new ArrayList<>();
        private final AtomicInteger finalizedCount = new AtomicInteger();
        private final AtomicInteger runFailedCount = new AtomicInteger();
        private final CountDownLatch terminalLatch = new CountDownLatch(1);

        @Override
        public void onEvent(BaseEvent event) {
            if (event.getType() == EventType.RUN_ERROR) {
                runErrorMessages.add(((RunErrorEvent) event).getError());
            }
        }

        @Override
        public void onRunFinalized(AgentSubscriberParams params) {
            finalizedCount.incrementAndGet();

            terminalLatch.countDown();
        }

        @Override
        public void onRunFailed(AgentSubscriberParams params, Throwable error) {
            runFailedCount.incrementAndGet();

            terminalLatch.countDown();
        }
    }
}
