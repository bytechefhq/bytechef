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

package com.bytechef.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
class ChatMemoryRewinderTest {

    private static final String CONVERSATION_ID = "conversation";

    private ChatMemory chatMemory;

    @BeforeEach
    void beforeEach() {
        chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(100)
            .build();

        chatMemory.add(
            CONVERSATION_ID,
            List.of(
                new UserMessage("Post a standup reminder"), new AssistantMessage("Which channel?"),
                new UserMessage("User picked: #general"), new AssistantMessage("Done, posting to #general."),
                new UserMessage("Make it 10am"), new AssistantMessage("Moved it to 10am.")));
    }

    @Test
    void testKeepsTheMemoryWhenTheClientShowsEveryTurn() {
        ChatMemoryRewinder.rewind(
            chatMemory, CONVERSATION_ID,
            List.of("Post a standup reminder", "User picked: #general", "Make it 10am"));

        assertThat(texts()).hasSize(6);
    }

    @Test
    void testDropsTheTurnsAfterAnEditedMessage() {
        ChatMemoryRewinder.rewind(chatMemory, CONVERSATION_ID, List.of("Post a standup reminder"));

        assertThat(texts()).containsExactly("Post a standup reminder", "Which channel?");
    }

    @Test
    void testDropsTheLastTurnWhenItIsRegenerated() {
        ChatMemoryRewinder.rewind(
            chatMemory, CONVERSATION_ID, List.of("Post a standup reminder", "User picked: #general"));

        assertThat(texts()).containsExactly(
            "Post a standup reminder", "Which channel?", "User picked: #general", "Done, posting to #general.");
    }

    @Test
    void testClearsTheMemoryWhenTheFirstMessageIsEdited() {
        ChatMemoryRewinder.rewind(chatMemory, CONVERSATION_ID, List.of());

        assertThat(texts()).isEmpty();
    }

    @Test
    void testLeavesTheMemoryAloneWhenTheClientHistoryDoesNotMatch() {
        ChatMemoryRewinder.rewind(chatMemory, CONVERSATION_ID, List.of("Something the server never saw"));

        assertThat(texts()).hasSize(6);
    }

    @Test
    void testNeverAddsMessages() {
        ChatMemoryRewinder.rewind(
            chatMemory, CONVERSATION_ID,
            List.of("Post a standup reminder", "User picked: #general", "Make it 10am", "An extra turn"));

        assertThat(texts()).hasSize(6);
    }

    @Test
    void testDropsARegeneratedTurnThatRepeatsAnEarlierText() {
        chatMemory.clear(CONVERSATION_ID);

        chatMemory.add(
            CONVERSATION_ID,
            List.of(
                new UserMessage("yes"), new AssistantMessage("Which channel?"), new UserMessage("no"),
                new AssistantMessage("Which team?"), new UserMessage("yes"), new AssistantMessage("Done.")));

        ChatMemoryRewinder.rewind(chatMemory, CONVERSATION_ID, List.of("yes"));

        assertThat(texts()).containsExactly("yes", "Which channel?");
    }

    @Test
    void testRewindsAMemoryWhoseWindowDroppedTheFirstTurns() {
        chatMemory.clear(CONVERSATION_ID);

        chatMemory.add(
            CONVERSATION_ID,
            List.of(
                new UserMessage("User picked: #general"), new AssistantMessage("Done, posting to #general."),
                new UserMessage("Make it 10am"), new AssistantMessage("Moved it to 10am.")));

        ChatMemoryRewinder.rewind(
            chatMemory, CONVERSATION_ID, List.of("Post a standup reminder", "User picked: #general"));

        assertThat(texts()).containsExactly("User picked: #general", "Done, posting to #general.");
    }

    private List<String> texts() {
        return chatMemory.get(CONVERSATION_ID)
            .stream()
            .map(Message::getText)
            .toList();
    }
}
