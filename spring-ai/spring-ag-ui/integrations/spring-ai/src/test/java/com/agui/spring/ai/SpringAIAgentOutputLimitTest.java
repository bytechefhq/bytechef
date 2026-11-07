package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.message.UserMessage;
import com.agui.core.type.EventType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
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
 * @author Ivica Cardic
 */
class SpringAIAgentOutputLimitTest {

    @ParameterizedTest
    @ValueSource(strings = {"max_tokens", "MAX_TOKENS", "length"})
    void testOutputLimitFinishReasonEmitsRunErrorAndFinalizes(String finishReason) throws Exception {
        RecordingSubscriber subscriber = runWithFinishReason(finishReason);

        assertThat(subscriber.runErrorMessages)
            .singleElement()
            .asString()
            .contains("output token limit");
        assertThat(subscriber.runFinishedCount.get()).isZero();
        assertThat(subscriber.finalizedCount.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"end_turn", "stop", ""})
    void testRegularFinishReasonFinishesRun(String finishReason) throws Exception {
        RecordingSubscriber subscriber = runWithFinishReason(finishReason);

        assertThat(subscriber.runErrorMessages).isEmpty();
        assertThat(subscriber.runFinishedCount.get()).isEqualTo(1);
        assertThat(subscriber.finalizedCount.get()).isEqualTo(1);
    }

    private static RecordingSubscriber runWithFinishReason(String finishReason) throws Exception {
        ChatModel chatModel = new ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException("call() is not exercised by the streaming path");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                ChatResponse textChunk = new ChatResponse(
                    List.of(new Generation(AssistantMessage.builder()
                        .content("Changing the sort comparator now.")
                        .build())));
                ChatResponse finalChunk = new ChatResponse(
                    List.of(new Generation(
                        AssistantMessage.builder()
                            .content("")
                            .build(),
                        ChatGenerationMetadata.builder()
                            .finishReason(finishReason)
                            .build())));

                return Flux.just(textChunk, finalChunk);
            }
        };

        SpringAIAgent agent = SpringAIAgent.builder()
            .chatModel(chatModel)
            .agentId("test-agent")
            .systemMessage("You are a test agent.")
            .messages(new ArrayList<>())
            .build();

        UserMessage userMessage = new UserMessage();

        userMessage.setId(UUID.randomUUID()
            .toString());
        userMessage.setContent("change the sort");

        RecordingSubscriber subscriber = new RecordingSubscriber();

        RunAgentParameters parameters = RunAgentParameters.builder()
            .threadId("thread-1")
            .runId("run-1")
            .messages(List.of(userMessage))
            .build();

        agent.runAgent(parameters, subscriber);

        assertThat(subscriber.terminalLatch.await(5, TimeUnit.SECONDS)).isTrue();

        return subscriber;
    }

    private static final class RecordingSubscriber implements AgentSubscriber {

        private final List<String> runErrorMessages = new ArrayList<>();
        private final AtomicInteger finalizedCount = new AtomicInteger();
        private final AtomicInteger runFinishedCount = new AtomicInteger();
        private final CountDownLatch terminalLatch = new CountDownLatch(1);

        @Override
        public void onEvent(BaseEvent event) {
            if (event.getType() == EventType.RUN_ERROR) {
                runErrorMessages.add(((RunErrorEvent) event).getError());
            }

            if (event.getType() == EventType.RUN_FINISHED) {
                runFinishedCount.incrementAndGet();
            }
        }

        @Override
        public void onRunFinalized(AgentSubscriberParams params) {
            finalizedCount.incrementAndGet();

            terminalLatch.countDown();
        }

        @Override
        public void onRunFailed(AgentSubscriberParams params, Throwable error) {
            terminalLatch.countDown();
        }
    }
}
