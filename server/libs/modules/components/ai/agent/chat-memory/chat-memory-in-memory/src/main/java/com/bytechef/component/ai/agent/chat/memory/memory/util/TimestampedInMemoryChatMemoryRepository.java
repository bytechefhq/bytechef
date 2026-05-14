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

package com.bytechef.component.ai.agent.chat.memory.memory.util;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

/**
 * Wraps a {@link ChatMemoryRepository} and tracks the last-save timestamp per conversation so that
 * {@link #findConversationIds()} returns IDs ordered by most recent activity (DESC).
 *
 * @author ByteChef
 */
class TimestampedInMemoryChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryRepository delegate;
    private final Map<String, Instant> lastSavedAt = new ConcurrentHashMap<>();

    TimestampedInMemoryChatMemoryRepository(ChatMemoryRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<String> findConversationIds() {
        return delegate.findConversationIds()
            .stream()
            .sorted(Comparator.comparing(
                id -> lastSavedAt.getOrDefault(id, Instant.EPOCH), Comparator.reverseOrder()))
            .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return delegate.findByConversationId(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        delegate.saveAll(conversationId, messages);

        lastSavedAt.put(conversationId, Instant.now());
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        delegate.deleteByConversationId(conversationId);

        lastSavedAt.remove(conversationId);
    }
}
