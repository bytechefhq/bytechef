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

package com.bytechef.platform.component.definition.ai.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.Ordered;

/**
 * @author Ivica Cardic
 */
class ChatMemoryFunctionResultTest {

    @Test
    void testOfRunsOutsideTheToolLoopWithoutToolCallbacks() {
        ChatMemoryFunction.Result result = ChatMemoryFunction.Result.of(
            mock(BaseAdvisor.class), mock(ChatMemory.class));

        assertThat(result.toolCallbacks()).isEmpty();
        assertThat(result.supportsToolMessagePersistence()).isFalse();
    }

    @Test
    void testPersistingToolMessagesRunsInsideTheToolLoopWithItsToolCallbacks() {
        ToolCallback toolCallback = mock(ToolCallback.class);

        ChatMemoryFunction.Result result = ChatMemoryFunction.Result.persistingToolMessages(
            toolMessagePersistenceAdvisor(), conversationId -> List.of(), List.of(toolCallback));

        assertThat(result.toolCallbacks()).containsExactly(toolCallback);
        assertThat(result.supportsToolMessagePersistence()).isTrue();
    }

    @Test
    void testPersistingToolMessagesRejectsAnAdvisorOutsideTheToolMessagePersistenceOrder() {
        BaseAdvisor advisor = mock(BaseAdvisor.class);

        when(advisor.getOrder()).thenReturn(Ordered.HIGHEST_PRECEDENCE);

        assertThatThrownBy(
            () -> ChatMemoryFunction.Result.persistingToolMessages(advisor, conversationId -> List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER));
    }

    @Test
    void testWithoutToolMessagePersistenceAcceptsAnAdvisorAtTheToolMessagePersistenceOrder() {
        ChatMemoryFunction.Result result = new ChatMemoryFunction.Result(
            toolMessagePersistenceAdvisor(), conversationId -> List.of(), List.of(), false);

        assertThat(result.supportsToolMessagePersistence()).isFalse();
    }

    @Test
    void testToolCallbacksAreCopied() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        ChatMemoryFunction.Result result = ChatMemoryFunction.Result.persistingToolMessages(
            toolMessagePersistenceAdvisor(), conversationId -> List.of(), toolCallbacks);

        toolCallbacks.add(mock(ToolCallback.class));

        assertThat(result.toolCallbacks()).isEmpty();
    }

    @Test
    void testOfReadsHistoryFromTheChatMemory() {
        ChatMemory chatMemory = mock(ChatMemory.class);
        List<Message> messages = List.of(new UserMessage("hello"));

        when(chatMemory.get("conversation-1")).thenReturn(messages);

        ChatMemoryFunction.Result result = ChatMemoryFunction.Result.of(mock(BaseAdvisor.class), chatMemory);

        ConversationHistoryReader conversationHistoryReader = result.conversationHistoryReader();

        assertThat(conversationHistoryReader.read("conversation-1")).isEqualTo(messages);
    }

    @Test
    void testOfWithoutAChatMemoryReadsNoHistory() {
        ChatMemoryFunction.Result result = ChatMemoryFunction.Result.of(mock(BaseAdvisor.class), null);

        ConversationHistoryReader conversationHistoryReader = result.conversationHistoryReader();

        assertThat(conversationHistoryReader.read("conversation-1")).isEmpty();
    }

    @Test
    void testConversationHistoryReaderIsRequired() {
        assertThatThrownBy(
            () -> ChatMemoryFunction.Result.persistingToolMessages(mock(BaseAdvisor.class), null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("conversationHistoryReader");
    }

    @Test
    void testToolCallbacksAreRequired() {
        assertThatThrownBy(
            () -> ChatMemoryFunction.Result.persistingToolMessages(
                mock(BaseAdvisor.class), conversationId -> List.of(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("toolCallbacks");
    }

    @Test
    void testAdvisorIsRequired() {
        assertThatThrownBy(() -> ChatMemoryFunction.Result.of(null, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("advisor");
    }

    private static BaseAdvisor toolMessagePersistenceAdvisor() {
        BaseAdvisor advisor = mock(BaseAdvisor.class);

        when(advisor.getOrder()).thenReturn(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER);

        return advisor;
    }
}
