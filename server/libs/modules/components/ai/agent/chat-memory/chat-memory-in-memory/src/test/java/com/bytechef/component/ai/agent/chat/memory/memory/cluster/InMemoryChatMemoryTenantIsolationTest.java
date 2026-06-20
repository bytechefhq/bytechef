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
import com.bytechef.tenant.TenantContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;

/**
 * @author Ivica Cardic
 */
class InMemoryChatMemoryTenantIsolationTest {

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
        ChatMemory tenantA =
            TenantContext.callWithTenantId("tenantA", InMemoryChatMemoryTenantIsolationTest::applyChatMemory);
        ChatMemory tenantB =
            TenantContext.callWithTenantId("tenantB", InMemoryChatMemoryTenantIsolationTest::applyChatMemory);

        // With the previous static-final capture, apply() returned the same instance for every tenant.
        // Building per-invocation means each tenant resolves its own (tenant-scoped) repository.
        assertThat(tenantA).isNotSameAs(tenantB);
    }

    private static ChatMemory applyChatMemory() {
        Parameters parameters = Mockito.mock(Parameters.class);

        ChatMemoryFunction.Result result = InMemoryChatMemory.apply(parameters, parameters, parameters, Map.of());

        return result.chatMemory();
    }
}
