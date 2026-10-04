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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class QueryKnowledgeBaseToolCallbackTest {

    private static final long KNOWLEDGE_BASE_ID = 42L;
    private static final long WORKSPACE_ID = 1L;

    private final JsonMapper jsonMapper = new JsonMapper();

    private static ToolContext toolContext(long workspaceId) {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(0L)
                .build()
                .toToolContext());
    }

    private static KnowledgeBase knowledgeBase(long knowledgeBaseId, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(knowledgeBaseId);
        knowledgeBase.setName(name);

        return knowledgeBase;
    }

    private static KnowledgeBaseDocumentChunk chunk(long documentId, String textContent, float score) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(documentId);
        chunk.setTextContent(textContent);
        chunk.setScore(score);

        return chunk;
    }

    @Test
    void testToolDefinitionExposesQueryKnowledgeBaseName() {
        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("queryKnowledgeBase");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("knowledgeBaseId");
        assertThat(definition.inputSchema()).contains("question");
    }

    @Test
    void testCallReturnsCitationEnvelopeWithResolvedTitles() throws Exception {
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);
        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setName("Onboarding Guide");

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenReturn(List.of(knowledgeBase(KNOWLEDGE_BASE_ID, "Company Docs")));
        when(knowledgeBaseFacade.searchKnowledgeBase(eq(KNOWLEDGE_BASE_ID), any(), isNull()))
            .thenReturn(List.of(chunk(7L, "Spring AI supports vector stores for semantic search.", 0.92f)));
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(7L)).thenReturn(document);

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            workspaceKnowledgeBaseFacade, knowledgeBaseFacade, knowledgeBaseDocumentService);

        String result = callback.call(
            "{\"knowledgeBaseId\":\"42\",\"question\":\"vector stores\"}", toolContext(WORKSPACE_ID));

        JsonNode resultNode = jsonMapper.readTree(result);

        assertThat(resultNode.get("kind")
            .asText()).isEqualTo("knowledge-base-citations");

        JsonNode hitsNode = resultNode.get("hits");

        assertThat(hitsNode.isArray()).isTrue();
        assertThat(hitsNode).hasSize(1);

        JsonNode hit = hitsNode.get(0);

        assertThat(hit.get("docId")
            .asText()).isEqualTo("7");
        assertThat(hit.get("docTitle")
            .asText()).isEqualTo("Onboarding Guide");
        assertThat(hit.get("excerpt")
            .asText()).contains("Spring AI");
        assertThat(hit.get("score")
            .floatValue()).isEqualTo(0.92f);
        assertThat(hit.get("knowledgeBaseId")
            .asText()).isEqualTo("42");
        assertThat(hit.get("knowledgeBaseName")
            .asText()).isEqualTo("Company Docs");
    }

    @Test
    void testCallOmitsTitleWhenDocumentLookupFails() throws Exception {
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);
        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenReturn(List.of(knowledgeBase(KNOWLEDGE_BASE_ID, "Company Docs")));
        when(knowledgeBaseFacade.searchKnowledgeBase(eq(KNOWLEDGE_BASE_ID), any(), isNull()))
            .thenReturn(List.of(chunk(7L, "Some content", 0.8f)));
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(7L))
            .thenThrow(new RuntimeException("document row gone"));

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            workspaceKnowledgeBaseFacade, knowledgeBaseFacade, knowledgeBaseDocumentService);

        String result = callback.call(
            "{\"knowledgeBaseId\":\"42\",\"question\":\"anything\"}", toolContext(WORKSPACE_ID));

        JsonNode resultNode = jsonMapper.readTree(result);
        JsonNode hitsNode = resultNode.get("hits");

        assertThat(hitsNode).hasSize(1);

        JsonNode hit = hitsNode.get(0);

        assertThat(hit.has("docTitle")).isFalse();
        assertThat(hit.get("docId")
            .asText()).isEqualTo("7");
    }

    @Test
    void testCallReturnsEmptyHitsWhenNoResults() throws Exception {
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);
        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenReturn(List.of(knowledgeBase(KNOWLEDGE_BASE_ID, "Company Docs")));
        when(knowledgeBaseFacade.searchKnowledgeBase(eq(KNOWLEDGE_BASE_ID), any(), isNull()))
            .thenReturn(List.of());

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            workspaceKnowledgeBaseFacade, knowledgeBaseFacade, mock(KnowledgeBaseDocumentService.class));

        String result = callback.call(
            "{\"knowledgeBaseId\":\"42\",\"question\":\"nothing here\"}", toolContext(WORKSPACE_ID));

        JsonNode resultNode = jsonMapper.readTree(result);

        assertThat(resultNode.get("kind")
            .asText()).isEqualTo("knowledge-base-citations");
        assertThat(resultNode.get("hits")).isEmpty();
    }

    @Test
    void testCallClampsLimitAtTwenty() throws Exception {
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);
        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenReturn(List.of(knowledgeBase(KNOWLEDGE_BASE_ID, "Company Docs")));
        when(knowledgeBaseFacade.searchKnowledgeBase(eq(KNOWLEDGE_BASE_ID), any(), isNull()))
            .thenReturn(List.of(chunk(1L, "Some content", 0.8f)));

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            workspaceKnowledgeBaseFacade, knowledgeBaseFacade, mock(KnowledgeBaseDocumentService.class));

        String result = callback.call(
            "{\"knowledgeBaseId\":\"42\",\"question\":\"test\",\"limit\":" + QueryKnowledgeBaseToolCallback.MAX_LIMIT
                + "}",
            toolContext(WORKSPACE_ID));

        JsonNode resultNode = jsonMapper.readTree(result);

        assertThat(resultNode.get("hits")).hasSize(1);
    }

    @Test
    void testCallReturnsErrorWhenKnowledgeBaseIsOutsideTheWorkspace() throws Exception {
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);
        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenReturn(List.of(knowledgeBase(KNOWLEDGE_BASE_ID, "Company Docs")));

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            workspaceKnowledgeBaseFacade, knowledgeBaseFacade, mock(KnowledgeBaseDocumentService.class));

        String result = callback.call(
            "{\"knowledgeBaseId\":\"999\",\"question\":\"anything\"}", toolContext(WORKSPACE_ID));

        JsonNode resultNode = jsonMapper.readTree(result);

        assertThat(resultNode.has("error")).isTrue();
        assertThat(resultNode.has("hits")).isFalse();

        verify(knowledgeBaseFacade, never()).searchKnowledgeBase(any(), any(), any());
    }

    @Test
    void testCallReturnsToolErrorOnInfrastructureFailure() throws Exception {
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);
        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenThrow(new IllegalStateException("connection pool exhausted"));

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            workspaceKnowledgeBaseFacade, knowledgeBaseFacade, mock(KnowledgeBaseDocumentService.class));

        String result = callback.call(
            "{\"knowledgeBaseId\":\"42\",\"question\":\"anything\"}", toolContext(WORKSPACE_ID));

        JsonNode resultNode = jsonMapper.readTree(result);

        assertThat(resultNode.has("error")).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenQuestionMissing() throws Exception {
        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentService.class));

        String result = callback.call("{\"knowledgeBaseId\":\"42\",\"question\":\"\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
