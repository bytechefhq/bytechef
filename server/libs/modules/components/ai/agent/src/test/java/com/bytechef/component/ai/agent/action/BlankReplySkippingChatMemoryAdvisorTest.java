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

package com.bytechef.component.ai.agent.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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
class BlankReplySkippingChatMemoryAdvisorTest {

    private final AdvisorChain advisorChain = mock(AdvisorChain.class);
    private final BaseAdvisor memoryAdvisor = mock(BaseAdvisor.class);
    private BlankReplySkippingChatMemoryAdvisor blankReplySkippingChatMemoryAdvisor;

    @BeforeEach
    void beforeEach() {
        when(memoryAdvisor.getOrder()).thenReturn(7);
        when(memoryAdvisor.getScheduler()).thenReturn(Schedulers.immediate());
        when(memoryAdvisor.before(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memoryAdvisor.after(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        blankReplySkippingChatMemoryAdvisor = new BlankReplySkippingChatMemoryAdvisor(memoryAdvisor);
    }

    @Test
    void testBeforeDelegatesSoTheUserMessageIsStored() {
        ChatClientRequest chatClientRequest = createChatClientRequest();

        assertThat(blankReplySkippingChatMemoryAdvisor.before(chatClientRequest, advisorChain))
            .isSameAs(chatClientRequest);

        verify(memoryAdvisor).before(chatClientRequest, advisorChain);
        assertThat(blankReplySkippingChatMemoryAdvisor.getOrder()).isEqualTo(7);
    }

    @Test
    void testAfterDoesNotStoreABlankReply() {
        ChatClientResponse chatClientResponse = createChatClientResponse(new AssistantMessage(""));

        assertThat(blankReplySkippingChatMemoryAdvisor.after(chatClientResponse, advisorChain))
            .isSameAs(chatClientResponse);

        verify(memoryAdvisor, never()).after(any(), any());
    }

    @Test
    void testAfterStoresANonBlankReply() {
        ChatClientResponse chatClientResponse = createChatClientResponse(new AssistantMessage("Answer"));

        blankReplySkippingChatMemoryAdvisor.after(chatClientResponse, advisorChain);

        verify(memoryAdvisor).after(chatClientResponse, advisorChain);
    }

    @Test
    void testAfterStoresOnlyTheNonBlankGenerations() {
        ChatClientResponse chatClientResponse = createChatClientResponse(
            new AssistantMessage(""), new AssistantMessage("Answer"));

        blankReplySkippingChatMemoryAdvisor.after(chatClientResponse, advisorChain);

        ArgumentCaptor<ChatClientResponse> chatClientResponseArgumentCaptor =
            ArgumentCaptor.forClass(ChatClientResponse.class);

        verify(memoryAdvisor).after(chatClientResponseArgumentCaptor.capture(), any());

        ChatClientResponse storedChatClientResponse = chatClientResponseArgumentCaptor.getValue();

        ChatResponse storedChatResponse = Objects.requireNonNull(storedChatClientResponse.chatResponse());

        List<Generation> storedGenerations = storedChatResponse.getResults();

        assertThat(storedGenerations).hasSize(1);

        Generation storedGeneration = storedGenerations.getFirst();

        assertThat(storedGeneration.getOutput()
            .getText()).isEqualTo("Answer");
    }

    @Test
    void testAdviseStreamStoresTheUserMessageButNotAStreamedBlankReply() {
        StreamAdvisorChain streamAdvisorChain = mock(StreamAdvisorChain.class);

        when(streamAdvisorChain.nextStream(any()))
            .thenReturn(Flux.just(createChatClientResponse(new AssistantMessage(""))));

        ChatClientRequest chatClientRequest = createChatClientRequest();

        List<ChatClientResponse> chatClientResponses = blankReplySkippingChatMemoryAdvisor
            .adviseStream(chatClientRequest, streamAdvisorChain)
            .collectList()
            .block();

        assertThat(chatClientResponses).hasSize(1);

        verify(memoryAdvisor).before(chatClientRequest, streamAdvisorChain);
        verify(memoryAdvisor, never()).after(any(), any());
    }

    private static ChatClientRequest createChatClientRequest() {
        return ChatClientRequest.builder()
            .prompt(new Prompt("Question"))
            .context(Map.of())
            .build();
    }

    private static ChatClientResponse createChatClientResponse(AssistantMessage... assistantMessages) {
        List<Generation> generations = Arrays.stream(assistantMessages)
            .map(Generation::new)
            .toList();

        return ChatClientResponse.builder()
            .chatResponse(new ChatResponse(generations))
            .context(Map.of())
            .build();
    }
}
