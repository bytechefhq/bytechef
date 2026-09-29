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

package com.bytechef.ai.chat.memory.redis.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryConfig;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
class TenantRoutingRedisChatMemoryRepositoryTest {

    private static final String CONVERSATION_ID = "shared-conversation-id";

    private final List<String> createdTenantIds = new ArrayList<>();
    private final TenantRoutingRedisChatMemoryRepository repository = new TenantRoutingRedisChatMemoryRepository(
        tenantId -> {
            createdTenantIds.add(tenantId);

            return new InMemoryChatMemoryRepository();
        });

    @Test
    void testConversationsAreIsolatedPerTenant() {
        TenantContext.runWithTenantId(
            "000001", () -> repository.saveAll(CONVERSATION_ID, List.of(new UserMessage("tenant 1 message"))));
        TenantContext.runWithTenantId(
            "000002", () -> repository.saveAll(CONVERSATION_ID, List.of(new UserMessage("tenant 2 message"))));

        List<Message> firstTenantMessages = TenantContext.callWithTenantId(
            "000001", () -> repository.findByConversationId(CONVERSATION_ID));
        List<Message> secondTenantMessages = TenantContext.callWithTenantId(
            "000002", () -> repository.findByConversationId(CONVERSATION_ID));

        assertThat(firstTenantMessages).extracting(Message::getText)
            .containsExactly("tenant 1 message");
        assertThat(secondTenantMessages).extracting(Message::getText)
            .containsExactly("tenant 2 message");
        assertThat(repository.findConversationIds()).isEmpty();
        assertThat(createdTenantIds).containsExactly("000001", "000002", TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testDefaultTenantKeepsTheLibraryKeyPrefixAndIndex() {
        assertThat(TenantRoutingRedisChatMemoryRepository.getKeyPrefix(TenantContext.DEFAULT_TENANT_ID))
            .isEqualTo(RedisChatMemoryConfig.DEFAULT_KEY_PREFIX);
        assertThat(TenantRoutingRedisChatMemoryRepository.getIndexName(TenantContext.DEFAULT_TENANT_ID))
            .isEqualTo(RedisChatMemoryConfig.DEFAULT_INDEX_NAME);
    }

    @Test
    void testTenantKeyPrefixIsOutsideTheDefaultTenantsIndexPrefix() {
        String keyPrefix = TenantRoutingRedisChatMemoryRepository.getKeyPrefix("000001");

        assertThat(keyPrefix).isEqualTo("chat-memory-000001:")
            .doesNotStartWith(RedisChatMemoryConfig.DEFAULT_KEY_PREFIX);
        assertThat(TenantRoutingRedisChatMemoryRepository.getIndexName("000001"))
            .isEqualTo("chat-memory-idx-000001");
    }
}
