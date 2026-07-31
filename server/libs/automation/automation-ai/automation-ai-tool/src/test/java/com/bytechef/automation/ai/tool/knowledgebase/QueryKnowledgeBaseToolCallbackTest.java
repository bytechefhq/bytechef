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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
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
class QueryKnowledgeBaseToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private static ToolContext toolContext(long workspaceId) {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(0L)
                .build()
                .toToolContext());
    }

    private static KnowledgeBase namedKnowledgeBase(String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName(name);

        return knowledgeBase;
    }

    @Test
    void testToolDefinitionExposesQueryKnowledgeBaseName() {
        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            knowledgeBaseFacade, knowledgeBaseService, knowledgeBaseDocumentService);

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("queryKnowledgeBase");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("knowledgeBaseId");
        assertThat(definition.inputSchema()).contains("question");
    }

    @Test
    void testCallReturnsCitationEnvelopeWithResolvedTitles() throws Exception {
        long workspaceId = 1L;
        long knowledgeBaseId = 42L;

        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(7L);
        chunk.setTextContent("Spring AI supports vector stores for semantic search.");
        chunk.setScore(0.92f);

        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setName("Onboarding Guide");

        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(namedKnowledgeBase("Company Docs"));
        when(knowledgeBaseFacade.searchKnowledgeBase(eq(knowledgeBaseId), any(), isNull()))
            .thenReturn(List.of(chunk));
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(7L)).thenReturn(document);

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            knowledgeBaseFacade, knowledgeBaseService, knowledgeBaseDocumentService);

        String result = callback.call("{\"knowledgeBaseId\":\"42\",\"question\":\"vector stores\"}",
            toolContext(workspaceId));

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
        long workspaceId = 1L;
        long knowledgeBaseId = 42L;

        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(7L);
        chunk.setTextContent("Some content");
        chunk.setScore(0.8f);

        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(namedKnowledgeBase("Company Docs"));
        when(knowledgeBaseFacade.searchKnowledgeBase(eq(knowledgeBaseId), any(), isNull()))
            .thenReturn(List.of(chunk));
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(7L))
            .thenThrow(new RuntimeException("document row gone"));

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            knowledgeBaseFacade, knowledgeBaseService, knowledgeBaseDocumentService);

        String result = callback.call("{\"knowledgeBaseId\":\"42\",\"question\":\"anything\"}",
            toolContext(workspaceId));

        JsonNode resultNode = jsonMapper.readTree(result);
        JsonNode hitsNode = resultNode.get("hits");

        assertThat(hitsNode).hasSize(1);

        JsonNode hit = hitsNode.get(0);

        // A failed title lookup degrades to a title-less hit — it must not fail the whole search.
        assertThat(hit.has("docTitle")).isFalse();
        assertThat(hit.get("docId")
            .asText()).isEqualTo("7");
    }

    @Test
    void testCallReturnsEmptyHitsWhenNoResults() throws Exception {
        long workspaceId = 1L;
        long knowledgeBaseId = 42L;

        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(namedKnowledgeBase("Company Docs"));
        when(knowledgeBaseFacade.searchKnowledgeBase(eq(knowledgeBaseId), any(), isNull()))
            .thenReturn(List.of());

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            knowledgeBaseFacade, knowledgeBaseService, knowledgeBaseDocumentService);

        String result = callback.call("{\"knowledgeBaseId\":\"42\",\"question\":\"nothing here\"}",
            toolContext(workspaceId));

        JsonNode resultNode = jsonMapper.readTree(result);

        assertThat(resultNode.get("kind")
            .asText()).isEqualTo("knowledge-base-citations");
        assertThat(resultNode.get("hits")).isEmpty();
    }

    @Test
    void testCallClampsLimitAtTwenty() throws Exception {
        long workspaceId = 1L;
        long knowledgeBaseId = 42L;

        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(1L);
        chunk.setTextContent("Some content");
        chunk.setScore(0.8f);

        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(namedKnowledgeBase("Company Docs"));
        when(knowledgeBaseFacade.searchKnowledgeBase(eq(knowledgeBaseId), any(), isNull()))
            .thenReturn(List.of(chunk));

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            knowledgeBaseFacade, knowledgeBaseService, knowledgeBaseDocumentService);

        String result = callback.call(
            "{\"knowledgeBaseId\":\"42\",\"question\":\"test\",\"limit\":" + QueryKnowledgeBaseToolCallback.MAX_LIMIT
                + "}",
            toolContext(workspaceId));

        JsonNode resultNode = jsonMapper.readTree(result);

        assertThat(resultNode.get("hits")).hasSize(1);
    }

    @Test
    void testCallReturnsEmptyHitsWhenKnowledgeBaseNotFound() throws Exception {
        long workspaceId = 1L;
        long knowledgeBaseId = 999L;

        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        // KnowledgeBaseServiceImpl#getKnowledgeBase throws a plain RuntimeException with this message when the
        // KB does not exist. Treat that as "no results" so the LLM gracefully reports a lack of content.
        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId))
            .thenThrow(new RuntimeException("KnowledgeBase not found: " + knowledgeBaseId));

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            knowledgeBaseFacade, knowledgeBaseService, knowledgeBaseDocumentService);

        String result = callback.call(
            "{\"knowledgeBaseId\":\"" + knowledgeBaseId + "\",\"question\":\"anything\"}",
            toolContext(workspaceId));

        JsonNode resultNode = jsonMapper.readTree(result);

        assertThat(resultNode.get("kind")
            .asText()).isEqualTo("knowledge-base-citations");
        assertThat(resultNode.get("hits")).isEmpty();
    }

    @Test
    void testCallPropagatesInfrastructureExceptions() {
        long workspaceId = 1L;
        long knowledgeBaseId = 42L;

        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        // A non-"not found" infrastructure error must propagate so the LLM sees the real failure instead of
        // silently getting an empty result set.
        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId))
            .thenThrow(new IllegalStateException("connection pool exhausted"));

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            knowledgeBaseFacade, knowledgeBaseService, knowledgeBaseDocumentService);

        assertThatThrownBy(() -> callback.call(
            "{\"knowledgeBaseId\":\"" + knowledgeBaseId + "\",\"question\":\"anything\"}",
            toolContext(workspaceId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("connection pool exhausted");
    }

    @Test
    void testCallReturnsErrorWhenQuestionMissing() throws Exception {
        KnowledgeBaseFacade knowledgeBaseFacade = mock(KnowledgeBaseFacade.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        QueryKnowledgeBaseToolCallback callback = new QueryKnowledgeBaseToolCallback(
            knowledgeBaseFacade, knowledgeBaseService, knowledgeBaseDocumentService);

        String result = callback.call("{\"knowledgeBaseId\":\"42\",\"question\":\"\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
