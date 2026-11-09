/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * @author Ivica Cardic
 */
class ChatMemoryRedactingAdvisorTest {

    private static final String EMAIL = "grace@example.com";
    private static final String PLACEHOLDER = "[REDACTED_EMAIL_ADDRESS]";

    private final AdvisorChain advisorChain = mock(AdvisorChain.class);
    private final BaseAdvisor memoryAdvisor = mock(BaseAdvisor.class);
    private ChatMemoryRedactingAdvisor chatMemoryRedactingAdvisor;

    @BeforeEach
    void beforeEach() {
        when(memoryAdvisor.getOrder()).thenReturn(7);
        when(memoryAdvisor.getScheduler()).thenReturn(Schedulers.immediate());
        when(memoryAdvisor.before(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memoryAdvisor.after(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        chatMemoryRedactingAdvisor = new ChatMemoryRedactingAdvisor(
            memoryAdvisor, text -> text.replace(EMAIL, PLACEHOLDER));
    }

    @Test
    void testAfterStoresTheRedactedReplyAndReturnsTheOriginal() {
        ChatClientResponse chatClientResponse = createChatClientResponse("Write to " + EMAIL);

        assertThat(chatMemoryRedactingAdvisor.after(chatClientResponse, advisorChain)).isSameAs(chatClientResponse);

        assertThat(storedText()).isEqualTo("Write to " + PLACEHOLDER);
        assertThat(chatMemoryRedactingAdvisor.getOrder()).isEqualTo(7);
    }

    @Test
    void testAfterStoresAReplyWithNothingToRedactUnchanged() {
        ChatClientResponse chatClientResponse = createChatClientResponse("Nothing sensitive");

        chatMemoryRedactingAdvisor.after(chatClientResponse, advisorChain);

        verify(memoryAdvisor).after(chatClientResponse, advisorChain);
    }

    @Test
    void testAfterKeepsTheToolCalls() {
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "function", "lookup", "{}");

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("Write to " + EMAIL)
            .toolCalls(List.of(toolCall))
            .build();

        chatMemoryRedactingAdvisor.after(createChatClientResponse(assistantMessage), advisorChain);

        AssistantMessage storedAssistantMessage = storedAssistantMessage();

        assertThat(storedAssistantMessage.getText()).isEqualTo("Write to " + PLACEHOLDER);
        assertThat(storedAssistantMessage.getToolCalls()).containsExactly(toolCall);
    }

    @Test
    void testAdviseStreamStoresTheRedactedAggregateAndEmitsTheOriginalChunks() {
        StreamAdvisorChain streamAdvisorChain = mock(StreamAdvisorChain.class);

        when(streamAdvisorChain.nextStream(any())).thenReturn(
            Flux.just(createChatClientResponse("Write to grace@exa"), createChatClientResponse("mple.com")));

        List<ChatClientResponse> emitted = Objects.requireNonNull(
            chatMemoryRedactingAdvisor.adviseStream(createChatClientRequest(), streamAdvisorChain)
                .collectList()
                .block());

        assertThat(emitted)
            .extracting(ChatMemoryRedactingAdvisorTest::textOf)
            .containsExactly("Write to grace@exa", "mple.com");
        assertThat(storedText()).isEqualTo("Write to " + PLACEHOLDER);
    }

    private String storedText() {
        return storedAssistantMessage().getText();
    }

    private AssistantMessage storedAssistantMessage() {
        ArgumentCaptor<ChatClientResponse> chatClientResponseArgumentCaptor =
            ArgumentCaptor.forClass(ChatClientResponse.class);

        verify(memoryAdvisor).after(chatClientResponseArgumentCaptor.capture(), any());

        ChatClientResponse storedChatClientResponse = chatClientResponseArgumentCaptor.getValue();

        ChatResponse storedChatResponse = Objects.requireNonNull(storedChatClientResponse.chatResponse());

        Generation storedGeneration = storedChatResponse.getResult();

        return Objects.requireNonNull(storedGeneration)
            .getOutput();
    }

    private static String textOf(ChatClientResponse chatClientResponse) {
        ChatResponse chatResponse = Objects.requireNonNull(chatClientResponse.chatResponse());

        Generation generation = Objects.requireNonNull(chatResponse.getResult());

        return generation.getOutput()
            .getText();
    }

    private static ChatClientRequest createChatClientRequest() {
        return ChatClientRequest.builder()
            .prompt(new Prompt("Who should I contact?"))
            .context(Map.of())
            .build();
    }

    private static ChatClientResponse createChatClientResponse(String text) {
        return createChatClientResponse(new AssistantMessage(text));
    }

    private static ChatClientResponse createChatClientResponse(AssistantMessage assistantMessage) {
        return ChatClientResponse.builder()
            .chatResponse(new ChatResponse(List.of(new Generation(assistantMessage))))
            .context(Map.of())
            .build();
    }
}
