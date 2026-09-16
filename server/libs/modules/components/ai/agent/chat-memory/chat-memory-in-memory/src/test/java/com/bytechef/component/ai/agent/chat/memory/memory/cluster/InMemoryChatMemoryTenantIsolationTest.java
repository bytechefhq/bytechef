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

package com.bytechef.component.ai.agent.chat.memory.memory.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.agent.chat.memory.memory.util.InMemoryChatMemoryRepositoryHolder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ConversationHistoryReader;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
class InMemoryChatMemoryTenantIsolationTest {

    private static final String CONVERSATION_ID = "tenant-isolation-conversation";

    @Test
    void testHolderReturnsDistinctRepositoriesPerTenant() {
        ChatMemoryRepository tenantA =
            TenantContext.callWithTenantId("tenantA", InMemoryChatMemoryRepositoryHolder::getInstance);
        ChatMemoryRepository tenantB =
            TenantContext.callWithTenantId("tenantB", InMemoryChatMemoryRepositoryHolder::getInstance);

        assertThat(tenantA).isNotSameAs(tenantB);
    }

    @Test
    void testApplyResolvesRepositoryPerInvocation() {
        TenantContext.runWithTenantId(
            "isolationTenantA", () -> saveMessage(new UserMessage("tenant A message")));

        List<Message> tenantAMessages = TenantContext.callWithTenantId(
            "isolationTenantA", InMemoryChatMemoryTenantIsolationTest::readHistory);
        List<Message> tenantBMessages = TenantContext.callWithTenantId(
            "isolationTenantB", InMemoryChatMemoryTenantIsolationTest::readHistory);

        assertThat(tenantAMessages)
            .extracting(Message::getText)
            .containsExactly("tenant A message");
        assertThat(tenantBMessages).isEmpty();
    }

    private static List<Message> readHistory() {
        Parameters parameters = Mockito.mock(Parameters.class);

        ChatMemoryFunction.Result result = InMemoryChatMemory.apply(parameters, parameters, parameters, Map.of());

        ConversationHistoryReader conversationHistoryReader = result.conversationHistoryReader();

        return conversationHistoryReader.read(CONVERSATION_ID);
    }

    private static void saveMessage(Message message) {
        ChatMemoryRepository chatMemoryRepository = InMemoryChatMemoryRepositoryHolder.getInstance();

        chatMemoryRepository.saveAll(CONVERSATION_ID, List.of(message));
    }
}
