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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
class InMemoryChatMemoryTenantTeardownContributorTest {

    private final TenantRoutingInMemoryChatMemoryRepository repository =
        new TenantRoutingInMemoryChatMemoryRepository();
    private final InMemoryChatMemoryTenantTeardownContributor contributor =
        new InMemoryChatMemoryTenantTeardownContributor(repository);

    @Test
    void testRemovesOnlyTheDeletedTenantsMemory() {
        TenantContext.runWithTenantId("000001", () -> repository.saveAll("c1", List.of(new UserMessage("one"))));
        TenantContext.runWithTenantId("000002", () -> repository.saveAll("c1", List.of(new UserMessage("two"))));

        contributor.afterSchemaDrop("000001");
        contributor.afterSchemaDrop("000001");

        TenantContext.runWithTenantId("000001", () -> assertThat(repository.findConversationIds()).isEmpty());
        TenantContext.runWithTenantId("000002",
            () -> assertThat(repository.findConversationIds()).containsExactly("c1"));
    }

    @Test
    void testRejectsInvalidTenantId() {
        assertThatThrownBy(() -> contributor.afterSchemaDrop("a:b"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
