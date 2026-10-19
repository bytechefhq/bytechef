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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
        ChatMemoryRewinder.rewind(
            chatMemory, CONVERSATION_ID, List.of("Post a standup reminder"));

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
    void testResetsTheMemoryWhenTheHistoryRestartsAtTheFirstMessage() {
        ChatMemoryRewinder.rewind(chatMemory, CONVERSATION_ID, List.of());

        assertThat(texts()).isEmpty();
    }

    @Test
    void testLeavesTheMemoryAloneWhenTheClientHistoryDoesNotMatch() {
        ChatMemoryRewinder.rewind(
            chatMemory, CONVERSATION_ID, List.of("Something the server never saw"));

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

    @Test
    void testConcurrentRewindsOfOneConversationDoNotDuplicateTheKeptTurns() throws Exception {
        InterleavingChatMemory interleavingChatMemory = new InterleavingChatMemory(chatMemory.get(CONVERSATION_ID));

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            List<Future<?>> rewindFutures = List.of(
                executorService.submit(() -> ChatMemoryRewinder.rewind(
                    interleavingChatMemory, CONVERSATION_ID, List.of("Post a standup reminder"))),
                executorService.submit(() -> ChatMemoryRewinder.rewind(
                    interleavingChatMemory, CONVERSATION_ID, List.of("Post a standup reminder"))));

            for (Future<?> rewindFuture : rewindFutures) {
                rewindFuture.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executorService.shutdownNow();
        }

        assertThat(interleavingChatMemory.get(CONVERSATION_ID))
            .extracting(Message::getText)
            .containsExactly("Post a standup reminder", "Which channel?");
    }

    private static final class InterleavingChatMemory implements ChatMemory {

        private final List<Message> storedMessages;
        private final AtomicInteger addCount = new AtomicInteger();
        private final AtomicInteger clearCount = new AtomicInteger();
        private final CountDownLatch bothReadLatch = new CountDownLatch(2);
        private final CountDownLatch secondClearLatch = new CountDownLatch(1);

        private InterleavingChatMemory(List<Message> initialMessages) {
            this.storedMessages = new CopyOnWriteArrayList<>(initialMessages);
        }

        @Override
        public void add(String conversationId, List<Message> messages) {
            if (addCount.incrementAndGet() == 1) {
                awaitQuietly(secondClearLatch);
            }

            storedMessages.addAll(messages);
        }

        @Override
        public List<Message> get(String conversationId) {
            List<Message> messages = List.copyOf(storedMessages);

            bothReadLatch.countDown();

            return messages;
        }

        @Override
        public void clear(String conversationId) {
            if (clearCount.incrementAndGet() == 1) {
                awaitQuietly(bothReadLatch);
            } else {
                secondClearLatch.countDown();
            }

            storedMessages.clear();
        }

        private static boolean awaitQuietly(CountDownLatch latch) {
            try {
                return latch.await(300, TimeUnit.MILLISECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                return false;
            }
        }
    }

    private List<String> texts() {
        return chatMemory.get(CONVERSATION_ID)
            .stream()
            .map(Message::getText)
            .toList();
    }
}
