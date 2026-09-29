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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Marko Kriskovic
 */
class TenantRoutingInMemoryChatMemoryRepositoryTest {

    private static final String CONVERSATION_ID = "shared-conversation-id";

    private final TenantRoutingInMemoryChatMemoryRepository repository =
        new TenantRoutingInMemoryChatMemoryRepository();

    @Test
    void testConversationsAreIsolatedPerTenant() {
        TenantContext.runWithTenantId(
            "tenantA", () -> repository.saveAll(CONVERSATION_ID, List.of(new UserMessage("tenant A message"))));
        TenantContext.runWithTenantId(
            "tenantB", () -> repository.saveAll(CONVERSATION_ID, List.of(new UserMessage("tenant B message"))));

        List<Message> tenantAMessages = TenantContext.callWithTenantId(
            "tenantA", () -> repository.findByConversationId(CONVERSATION_ID));
        List<Message> tenantBMessages = TenantContext.callWithTenantId(
            "tenantB", () -> repository.findByConversationId(CONVERSATION_ID));

        assertThat(tenantAMessages).extracting(Message::getText)
            .containsExactly("tenant A message");
        assertThat(tenantBMessages).extracting(Message::getText)
            .containsExactly("tenant B message");
        assertThat(repository.findConversationIds()).isEmpty();
    }

    @Test
    void testDeleteOnlyAffectsTheCurrentTenant() {
        TenantContext.runWithTenantId(
            "tenantA", () -> repository.saveAll(CONVERSATION_ID, List.of(new UserMessage("tenant A message"))));
        TenantContext.runWithTenantId(
            "tenantB", () -> repository.saveAll(CONVERSATION_ID, List.of(new UserMessage("tenant B message"))));

        TenantContext.runWithTenantId("tenantA", () -> repository.deleteByConversationId(CONVERSATION_ID));

        assertThat(TenantContext.callWithTenantId("tenantA", repository::findConversationIds)).isEmpty();
        assertThat(TenantContext.callWithTenantId("tenantB", repository::findConversationIds))
            .containsExactly(CONVERSATION_ID);
    }
}
