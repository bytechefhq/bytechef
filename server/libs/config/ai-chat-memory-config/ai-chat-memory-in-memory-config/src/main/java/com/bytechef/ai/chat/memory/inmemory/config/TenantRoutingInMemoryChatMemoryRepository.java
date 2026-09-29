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

package com.bytechef.ai.chat.memory.inmemory.config;

import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

/**
 * @author Marko Kriskovic
 */
final class TenantRoutingInMemoryChatMemoryRepository implements ChatMemoryRepository {

    private final Map<String, ChatMemoryRepository> repositories = new ConcurrentHashMap<>();

    @Override
    public List<String> findConversationIds() {
        return resolve().findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return resolve().findByConversationId(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        resolve().saveAll(conversationId, messages);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        resolve().deleteByConversationId(conversationId);
    }

    private ChatMemoryRepository resolve() {
        return repositories.computeIfAbsent(
            TenantContext.getCurrentTenantId(), tenantId -> new InMemoryChatMemoryRepository());
    }
}
