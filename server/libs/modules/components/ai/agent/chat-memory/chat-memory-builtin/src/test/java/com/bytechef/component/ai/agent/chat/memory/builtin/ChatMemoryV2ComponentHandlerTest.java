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

package com.bytechef.component.ai.agent.chat.memory.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
class ChatMemoryV2ComponentHandlerTest {

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/chat-memory_v2.json", createComponentHandler().getDefinition());
    }

    @Test
    void testIsVersionTwoOfChatMemoryWithAnOptionalSummarizerModelChild() {
        ComponentDefinition componentDefinition = createComponentHandler().getDefinition();

        assertThat(componentDefinition.getName()).isEqualTo("chatMemory");
        assertThat(componentDefinition.getVersion()).isEqualTo(2);
        assertThat(((SessionChatMemoryComponentDefinition) componentDefinition).getClusterElementTypes())
            .containsExactly(SessionChatMemoryComponentDefinition.SUMMARIZER_MODEL);
    }

    @Test
    void testActionsUseTheBuiltInSessionStorePerTenant() throws Exception {
        ChatMemoryV2ComponentHandler componentHandler = createComponentHandler();

        PerformFunction addMessages = getPerformFunction(componentHandler, "addMessages");
        PerformFunction listConversations = getPerformFunction(componentHandler, "listConversations");

        TenantContext.callWithTenantId(
            "tenanta", () -> addMessages.apply(
                MockParametersFactory.create(
                    Map.of(
                        "conversationId", "conversation-1",
                        "messages", List.of(Map.of("role", "user", "content", "hello")))),
                MockParametersFactory.create(Map.of()), mock(ActionContext.class)));

        Object tenantAConversations = TenantContext.callWithTenantId(
            "tenanta", () -> listConversations.apply(
                MockParametersFactory.create(Map.of()), MockParametersFactory.create(Map.of()),
                mock(ActionContext.class)));
        Object tenantBConversations = TenantContext.callWithTenantId(
            "tenantb", () -> listConversations.apply(
                MockParametersFactory.create(Map.of()), MockParametersFactory.create(Map.of()),
                mock(ActionContext.class)));

        assertThat(tenantAConversations).isEqualTo(Map.of("conversationIds", List.of("conversation-1"), "count", 1));
        assertThat(tenantBConversations).isEqualTo(Map.of("conversationIds", List.of(), "count", 0));
    }

    private static ChatMemoryV2ComponentHandler createComponentHandler() {
        return new ChatMemoryV2ComponentHandler(
            null, new MockEnvironment().withProperty("bytechef.ai.memory.provider", "in_memory"),
            mock(ClusterElementDefinitionService.class));
    }

    private static PerformFunction getPerformFunction(ComponentHandler componentHandler, String actionName) {
        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        List<? extends ActionDefinition> actionDefinitions = componentDefinition.getActions()
            .orElseThrow();

        return actionDefinitions.stream()
            .filter(actionDefinition -> actionName.equals(actionDefinition.getName()))
            .findFirst()
            .flatMap(ActionDefinition::getPerform)
            .map(PerformFunction.class::cast)
            .orElseThrow();
    }
}
