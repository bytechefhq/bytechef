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

package com.bytechef.component.ai.agent.chat.memory.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
class InMemoryChatMemoryV2ComponentHandlerTest {

    @Test
    void testRecursiveSummarizationBuildsTheSummarizerFromTheModelChild() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        ModelFunction modelFunction = mock(ModelFunction.class);

        doReturn(mock(ChatModel.class)).when(modelFunction)
            .apply(any(), any(), anyBoolean());

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(eq("openAi"), eq(1), eq("model")))
            .thenReturn(modelFunction);

        ComponentDefinition componentDefinition =
            new InMemoryChatMemoryV2ComponentHandler(clusterElementDefinitionService).getDefinition();

        List<? extends ClusterElementDefinition<?>> clusterElementDefinitions = componentDefinition.getClusterElements()
            .orElseThrow();

        ChatMemoryFunction chatMemoryFunction = (ChatMemoryFunction) clusterElementDefinitions.getFirst()
            .getElement();

        chatMemoryFunction.apply(
            MockParametersFactory.create(
                Map.of(
                    "conversationId", "conversation-1", "compactionStrategy", "RECURSIVE_SUMMARIZATION",
                    "maxEvents", 20, "maxEventsToKeep", 10, "overlapSize", 2)),
            MockParametersFactory.create(Map.of()),
            MockParametersFactory.create(
                Map.of(
                    "clusterElements",
                    Map.of("model", Map.of("name", "model_1", "type", "openAi/v1/model", "parameters", Map.of())))),
            Map.of("model_1", new ComponentConnection("openAi", 1, 2L, Map.of(), null)));

        verify(modelFunction).apply(any(), any(), eq(false));
    }

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/in-memory-chat-memory_v2.json",
            new InMemoryChatMemoryV2ComponentHandler(mock(ClusterElementDefinitionService.class)).getDefinition());
    }

    @Test
    void testIsVersionTwoOfInMemoryChatMemoryWithAnOptionalSummarizerModel() {
        ComponentDefinition componentDefinition =
            new InMemoryChatMemoryV2ComponentHandler(mock(ClusterElementDefinitionService.class)).getDefinition();

        assertThat(componentDefinition.getName()).isEqualTo("inMemoryChatMemory");
        assertThat(componentDefinition.getVersion()).isEqualTo(2);
        assertThat(((SessionChatMemoryComponentDefinition) componentDefinition).getClusterElementTypes())
            .containsExactly(SessionChatMemoryComponentDefinition.SUMMARIZER_MODEL);
    }

    @Test
    void testSessionsAreIsolatedPerTenant() throws Exception {
        InMemoryChatMemoryV2ComponentHandler componentHandler =
            new InMemoryChatMemoryV2ComponentHandler(mock(ClusterElementDefinitionService.class));

        TenantContext.callWithTenantId("tenanta", () -> addMessage(componentHandler, "conversation-1"));

        assertThat(listConversations(componentHandler, "tenanta"))
            .containsExactly("conversation-1");
        assertThat(listConversations(componentHandler, "tenantb"))
            .isEmpty();
    }

    @Test
    void testComponentHandlersDoNotShareSessions() throws Exception {
        InMemoryChatMemoryV2ComponentHandler firstComponentHandler =
            new InMemoryChatMemoryV2ComponentHandler(mock(ClusterElementDefinitionService.class));
        InMemoryChatMemoryV2ComponentHandler secondComponentHandler =
            new InMemoryChatMemoryV2ComponentHandler(mock(ClusterElementDefinitionService.class));

        TenantContext.callWithTenantId("tenanta", () -> addMessage(firstComponentHandler, "conversation-1"));

        assertThat(listConversations(secondComponentHandler, "tenanta"))
            .isEmpty();
    }

    private static Object addMessage(ComponentHandler componentHandler, String conversationId) throws Exception {
        return getPerformFunction(componentHandler, "addMessages").apply(
            MockParametersFactory.create(
                Map.of(
                    "conversationId", conversationId,
                    "messages", List.of(Map.of("role", "user", "content", "hello")))),
            MockParametersFactory.create(Map.of()), mock(ActionContext.class));
    }

    @SuppressWarnings("unchecked")
    private static List<String> listConversations(ComponentHandler componentHandler, String tenantId) {
        Map<?, ?> result = TenantContext.callWithTenantId(
            tenantId, () -> (Map<?, ?>) getPerformFunction(componentHandler, "listConversations").apply(
                MockParametersFactory.create(Map.of()), MockParametersFactory.create(Map.of()),
                mock(ActionContext.class)));

        return (List<String>) result.get("conversationIds");
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
