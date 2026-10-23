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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.Option;
import com.bytechef.platform.component.definition.ai.agent.ConversationFirstMessageReader.ConversationFirstMessage;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
class ChatMemoryConversationOptionsTest {

    @Test
    void testGetOptionsLoadsOnlyTheFirstHundredConversations() {
        ChatMemoryRepository chatMemoryRepository = mock(ChatMemoryRepository.class);

        List<String> conversationIds = IntStream.range(0, 150)
            .mapToObj(index -> "conversation-" + index)
            .toList();

        when(chatMemoryRepository.findConversationIds()).thenReturn(conversationIds);
        when(chatMemoryRepository.findByConversationId(anyString()))
            .thenAnswer(invocation -> List.of(new UserMessage("first " + invocation.getArgument(0))));

        List<Option<String>> options = ChatMemoryConversationOptions.getOptions(chatMemoryRepository, null);

        assertThat(options).hasSize(ChatMemoryConversationOptions.MAX_OPTIONS);
        assertThat(options.getFirst()
            .getValue()).isEqualTo("conversation-0");
        assertThat(options.getLast()
            .getValue()).isEqualTo("conversation-99");
        assertThat(options.getFirst()
            .getDescription()).contains("first conversation-0");

        verify(chatMemoryRepository, times(ChatMemoryConversationOptions.MAX_OPTIONS)).findByConversationId(
            anyString());
        verify(chatMemoryRepository, never()).findByConversationId("conversation-100");
    }

    @Test
    void testGetOptionsFiltersConversationIdsBySearchTextIgnoringCase() {
        ChatMemoryRepository chatMemoryRepository = mock(ChatMemoryRepository.class);

        when(chatMemoryRepository.findConversationIds()).thenReturn(
            List.of("Support-1", "sales-1", "support-2", "billing"));
        when(chatMemoryRepository.findByConversationId(anyString()))
            .thenAnswer(invocation -> List.of(new UserMessage("hello")));

        List<Option<String>> options = ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "SUPPORT");

        assertThat(options).extracting(Option::getValue)
            .containsExactly("Support-1", "support-2");

        verify(chatMemoryRepository, never()).findByConversationId("sales-1");
        verify(chatMemoryRepository, never()).findByConversationId("billing");
    }

    @Test
    void testGetOptionsAppliesTheLimitAfterSearchTextFiltering() {
        ChatMemoryRepository chatMemoryRepository = mock(ChatMemoryRepository.class);

        List<String> conversationIds = IntStream.range(0, 300)
            .mapToObj(index -> (index % 2 == 0 ? "even-" : "odd-") + index)
            .toList();

        when(chatMemoryRepository.findConversationIds()).thenReturn(conversationIds);
        when(chatMemoryRepository.findByConversationId(anyString()))
            .thenAnswer(invocation -> List.of(new UserMessage("hello")));

        List<Option<String>> options = ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "odd");

        assertThat(options).hasSize(ChatMemoryConversationOptions.MAX_OPTIONS);
        assertThat(options).extracting(Option::getValue)
            .allMatch(value -> value.startsWith("odd-"));
    }

    @Test
    void testGetOptionsSkipsConversationsWithoutMessages() {
        ChatMemoryRepository chatMemoryRepository = mock(ChatMemoryRepository.class);

        when(chatMemoryRepository.findConversationIds()).thenReturn(List.of("empty", "filled"));
        when(chatMemoryRepository.findByConversationId("empty")).thenReturn(List.of());
        when(chatMemoryRepository.findByConversationId("filled")).thenReturn(List.of(new UserMessage("hello")));

        List<Option<String>> options = ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "");

        assertThat(options).extracting(Option::getValue)
            .containsExactly("filled");
    }

    @Test
    void testGetOptionsUsesConversationFirstMessageReader() {
        ChatMemoryRepository chatMemoryRepository = mock(
            ChatMemoryRepository.class, withSettings().extraInterfaces(ConversationFirstMessageReader.class));

        ConversationFirstMessageReader conversationFirstMessageReader =
            (ConversationFirstMessageReader) chatMemoryRepository;

        when(conversationFirstMessageReader.findFirstMessages("abc", ChatMemoryConversationOptions.MAX_OPTIONS))
            .thenReturn(List.of(new ConversationFirstMessage("abc-1", "hello")));

        List<Option<String>> options = ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "abc");

        assertThat(options).extracting(Option::getValue)
            .containsExactly("abc-1");
        assertThat(options.getFirst()
            .getDescription()).contains("hello");

        verify(chatMemoryRepository, never()).findConversationIds();
        verify(chatMemoryRepository, never()).findByConversationId(anyString());
    }

    @Test
    void testGetOptionsPassesNullSearchTextToReaderWhenBlank() {
        ChatMemoryRepository chatMemoryRepository = mock(
            ChatMemoryRepository.class, withSettings().extraInterfaces(ConversationFirstMessageReader.class));

        ChatMemoryConversationOptions.getOptions(chatMemoryRepository, "  ");

        verify((ConversationFirstMessageReader) chatMemoryRepository).findFirstMessages(
            null, ChatMemoryConversationOptions.MAX_OPTIONS);
    }
}
