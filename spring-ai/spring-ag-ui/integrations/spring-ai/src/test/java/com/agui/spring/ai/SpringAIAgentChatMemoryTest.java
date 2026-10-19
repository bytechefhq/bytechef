package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAIAgentChatMemoryTest {

    @Test
    void shouldWriteTheRunsTurnThroughTheRunChatMemory() throws AGUIException, InterruptedException {
        var builderChatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();

        var runChatMemory = new RecordingChatMemory(builderChatMemory);

        var agent = new SpringAIAgent(SpringAIAgent.builder()
            .agentId("test")
            .chatModel(new ReplyingChatModel())
            .chatMemory(builderChatMemory)
            .systemMessage("system")
            .state(new State())) {

            @Override
            protected ChatMemory chatMemoryForRun(RunAgentInput input, ChatMemory chatMemory) {
                return runChatMemory;
            }
        };

        var userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("question");

        var input = new RunAgentInput(
            "thread", "run", new State(), List.of(userMessage), List.of(), new ArrayList<>(), null);

        var finalized = new CountDownLatch(1);

        agent.run(input, new AgentSubscriber() {
            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                finalized.countDown();
            }
        });

        assertThat(finalized.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(runChatMemory.addedTexts).containsExactly("question", "answer");
    }

    private static final class RecordingChatMemory implements ChatMemory {

        private final List<String> addedTexts = new CopyOnWriteArrayList<>();
        private final ChatMemory delegate;

        private RecordingChatMemory(ChatMemory delegate) {
            this.delegate = delegate;
        }

        @Override
        public void add(String conversationId, List<Message> messages) {
            messages.forEach(message -> addedTexts.add(message.getText()));

            delegate.add(conversationId, messages);
        }

        @Override
        public List<Message> get(String conversationId) {
            return delegate.get(conversationId);
        }

        @Override
        public void clear(String conversationId) {
            delegate.clear(conversationId);
        }
    }

    private static final class ReplyingChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The chat model must be streamed");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("answer")))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }
}
