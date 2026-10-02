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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Ivica Cardic
 */
class ResumedTurnChatMemoryAdvisorTest {

    private static final String CONVERSATION_ID = "conversation-1";

    private final AdvisorChain advisorChain = mock(AdvisorChain.class);

    @Test
    void testChatMemoryIsRequired() {
        assertThatThrownBy(() -> new ResumedTurnChatMemoryAdvisor((ChatMemory) null, 0))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testMemoryAdvisorIsRequired() {
        assertThatThrownBy(() -> new ResumedTurnChatMemoryAdvisor((BaseAdvisor) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testChatMemoryStoresOnlyTheNonBlankReplies() {
        ChatMemory chatMemory = mock(ChatMemory.class);

        new ResumedTurnChatMemoryAdvisor(chatMemory, 0).after(createResponse("", "Blue it is."), advisorChain);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> messagesArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(chatMemory).add(eq(CONVERSATION_ID), messagesArgumentCaptor.capture());

        assertThat(messagesArgumentCaptor.getValue())
            .extracting(Message::getText)
            .containsExactly("Blue it is.");
    }

    @Test
    void testChatMemoryIsNotWrittenWhenEveryReplyIsBlank() {
        ChatMemory chatMemory = mock(ChatMemory.class);

        new ResumedTurnChatMemoryAdvisor(chatMemory, 0).after(createResponse("", " "), advisorChain);

        verify(chatMemory, never()).add(anyString(), anyList());
    }

    @Test
    void testMemoryAdvisorReceivesOnlyTheNonBlankRepliesWithTheResponseMetadata() {
        BaseAdvisor memoryAdvisor = mock(BaseAdvisor.class);

        new ResumedTurnChatMemoryAdvisor(memoryAdvisor).after(createResponse("", "Blue it is."), advisorChain);

        ArgumentCaptor<ChatClientResponse> chatClientResponseArgumentCaptor =
            ArgumentCaptor.forClass(ChatClientResponse.class);

        verify(memoryAdvisor).after(chatClientResponseArgumentCaptor.capture(), eq(advisorChain));

        ChatResponse chatResponse = chatClientResponseArgumentCaptor.getValue()
            .chatResponse();

        assertThat(chatResponse).isNotNull();
        assertThat(chatResponse.getResults())
            .extracting(generation -> generation.getOutput()
                .getText())
            .containsExactly("Blue it is.");
        assertThat(chatResponse.getMetadata()
            .getId()).isEqualTo("response-1");
    }

    @Test
    void testMemoryAdvisorIsNotCalledWhenEveryReplyIsBlank() {
        BaseAdvisor memoryAdvisor = mock(BaseAdvisor.class);

        new ResumedTurnChatMemoryAdvisor(memoryAdvisor).after(createResponse("", " "), advisorChain);

        verify(memoryAdvisor, never()).after(any(), any());
    }

    @Test
    void testBeforeDoesNotStoreTheRequestThroughTheMemoryAdvisor() {
        BaseAdvisor memoryAdvisor = mock(BaseAdvisor.class);
        ChatClientRequest chatClientRequest = ChatClientRequest.builder()
            .prompt(new Prompt("Blue"))
            .build();

        ChatClientRequest result = new ResumedTurnChatMemoryAdvisor(memoryAdvisor).before(
            chatClientRequest, advisorChain);

        assertThat(result).isSameAs(chatClientRequest);

        verify(memoryAdvisor, never()).before(any(), any());
    }

    @Test
    void testOrderIsTheMemoryAdvisorOrder() {
        BaseAdvisor memoryAdvisor = mock(BaseAdvisor.class);

        when(memoryAdvisor.getOrder()).thenReturn(42);

        assertThat(new ResumedTurnChatMemoryAdvisor(memoryAdvisor).getOrder()).isEqualTo(42);
    }

    private static ChatClientResponse createResponse(String... replies) {
        List<Generation> generations = Arrays.stream(replies)
            .map(reply -> new Generation(new AssistantMessage(reply)))
            .toList();

        ChatResponse chatResponse = new ChatResponse(
            generations, ChatResponseMetadata.builder()
                .id("response-1")
                .build());

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(Map.of(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
            .build();
    }
}
