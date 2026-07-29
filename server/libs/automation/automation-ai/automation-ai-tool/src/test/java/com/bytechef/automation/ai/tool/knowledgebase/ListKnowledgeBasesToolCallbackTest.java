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

package com.bytechef.automation.ai.tool.knowledgebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class ListKnowledgeBasesToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesListKnowledgeBasesName() {
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);

        ListKnowledgeBasesToolCallback callback = new ListKnowledgeBasesToolCallback(
            workspaceKnowledgeBaseFacade);

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("listKnowledgeBases");
        assertThat(definition.description()).isNotBlank();
    }

    @Test
    void testCallReturnsKnowledgeBaseListFromWorkspace() throws Exception {
        long workspaceId = 1L;

        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);

        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(10L);
        knowledgeBase.setName("Product Docs");

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(workspaceId, 0L))
            .thenReturn(List.of(knowledgeBase));

        ListKnowledgeBasesToolCallback callback = new ListKnowledgeBasesToolCallback(
            workspaceKnowledgeBaseFacade);

        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(0L)
                .build()
                .toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).hasSize(1);

        JsonNode first = arrayNode.get(0);

        assertThat(first.get("id")
            .asText()).isEqualTo("10");
        assertThat(first.get("name")
            .asText()).isEqualTo("Product Docs");
    }

    @Test
    void testCallReturnsErrorWhenWorkspaceIdMissing() throws Exception {
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);

        ListKnowledgeBasesToolCallback callback = new ListKnowledgeBasesToolCallback(
            workspaceKnowledgeBaseFacade);

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testCallReturnsEmptyArrayWhenNoKnowledgeBases() throws Exception {
        long workspaceId = 1L;

        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(workspaceId, 0L)).thenReturn(List.of());

        ListKnowledgeBasesToolCallback callback = new ListKnowledgeBasesToolCallback(
            workspaceKnowledgeBaseFacade);

        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(0L)
                .build()
                .toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).isEmpty();
    }

    @Test
    void testCallPassesEnvironmentIdToFacade() throws Exception {
        long workspaceId = 1L;
        long environmentId = 2L;

        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(workspaceId, environmentId))
            .thenReturn(List.of());

        ListKnowledgeBasesToolCallback callback = new ListKnowledgeBasesToolCallback(
            workspaceKnowledgeBaseFacade);

        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(environmentId)
                .build()
                .toToolContext());

        callback.call("{}", toolContext);

        verify(workspaceKnowledgeBaseFacade).getWorkspaceKnowledgeBases(workspaceId, environmentId);
    }
}
