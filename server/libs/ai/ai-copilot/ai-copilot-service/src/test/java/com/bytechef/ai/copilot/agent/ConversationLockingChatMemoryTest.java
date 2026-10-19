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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
class ConversationLockingChatMemoryTest {

    private static final String CONCURRENT_TURN = "A turn written by a concurrent run";

    private static final String CONVERSATION_ID = "conversation";

    @Test
    void testAWriteDuringARewindLandsAfterTheKeptTurns() throws Exception {
        RecordingChatMemory recordingChatMemory = new RecordingChatMemory(
            List.of(
                new UserMessage("Post a standup reminder"), new AssistantMessage("Which channel?"),
                new UserMessage("Use #general"), new AssistantMessage("Done, posting to #general.")));

        ChatMemory conversationLockingChatMemory = new ConversationLockingChatMemory(recordingChatMemory);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            Future<?> rewindFuture = executorService.submit(() -> ChatMemoryRewinder.rewind(
                conversationLockingChatMemory, CONVERSATION_ID, List.of("Post a standup reminder")));

            Future<?> writeFuture = executorService.submit(() -> {
                if (awaitQuietly(recordingChatMemory.rewindReadLatch)) {
                    conversationLockingChatMemory.add(CONVERSATION_ID, new UserMessage(CONCURRENT_TURN));
                }
            });

            rewindFuture.get(10, TimeUnit.SECONDS);
            writeFuture.get(10, TimeUnit.SECONDS);
        } finally {
            executorService.shutdownNow();
        }

        assertThat(recordingChatMemory.concurrentWriteLandedDuringRewind).isFalse();
        assertThat(recordingChatMemory.get(CONVERSATION_ID))
            .extracting(Message::getText)
            .containsExactly("Post a standup reminder", "Which channel?", CONCURRENT_TURN);
    }

    private static boolean awaitQuietly(CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            return false;
        }
    }

    private static final class RecordingChatMemory implements ChatMemory {

        private final CountDownLatch concurrentWriteLatch = new CountDownLatch(1);
        private final AtomicBoolean concurrentWriteLandedDuringRewind = new AtomicBoolean();
        private final CountDownLatch rewindReadLatch = new CountDownLatch(1);
        private final List<Message> storedMessages;

        private RecordingChatMemory(List<Message> initialMessages) {
            this.storedMessages = new CopyOnWriteArrayList<>(initialMessages);
        }

        @Override
        public void add(String conversationId, List<Message> messages) {
            storedMessages.addAll(messages);

            if (messages.stream()
                .anyMatch(message -> CONCURRENT_TURN.equals(message.getText()))) {

                concurrentWriteLatch.countDown();
            }
        }

        @Override
        public List<Message> get(String conversationId) {
            List<Message> messages = List.copyOf(storedMessages);

            rewindReadLatch.countDown();

            return messages;
        }

        @Override
        public void clear(String conversationId) {
            if (awaitConcurrentWrite()) {
                concurrentWriteLandedDuringRewind.set(true);
            }

            storedMessages.clear();
        }

        private boolean awaitConcurrentWrite() {
            try {
                return concurrentWriteLatch.await(300, TimeUnit.MILLISECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                return false;
            }
        }
    }
}
