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

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class QueryKnowledgeBaseToolCallback implements ToolCallback {

    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 20;

    static final String CITATIONS_KIND = "knowledge-base-citations";

    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;
    private static final String TOOL_NAME = "queryKnowledgeBase";

    private static final Logger log = LoggerFactory.getLogger(QueryKnowledgeBaseToolCallback.class);

    private static final String DESCRIPTION = """
        Search a knowledge base using a natural-language question (RAG / vector similarity search).
        Returns {"kind": "knowledge-base-citations", "hits": [...]} where each hit carries docId,
        docTitle, excerpt, score, knowledgeBaseId, and knowledgeBaseName. Results are limited to 20
        hits maximum. Obtain the knowledgeBaseId from listKnowledgeBases first. Answer from the hit
        excerpts and cite the docTitle values as sources. Returns empty hits when the knowledge base
        has no matching content, and an {"error": ...} payload when the knowledgeBaseId is unknown
        or outside the current workspace.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "knowledgeBaseId": {"type": "string", "description": "Knowledge base id obtained from listKnowledgeBases"},
                    "question": {"type": "string", "description": "Natural-language question to search for"},
                    "limit": {"type": "integer", "description": "Maximum number of chunks to return (capped at 20, default 5)"}
                },
                "required": ["knowledgeBaseId", "question"]
            }""";

    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;
    private final KnowledgeBaseFacade knowledgeBaseFacade;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public QueryKnowledgeBaseToolCallback(
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade, KnowledgeBaseFacade knowledgeBaseFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService) {

        this.workspaceKnowledgeBaseFacade = workspaceKnowledgeBaseFacade;
        this.knowledgeBaseFacade = knowledgeBaseFacade;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            QueryKnowledgeBaseInput input = jsonMapper.readValue(toolInput, QueryKnowledgeBaseInput.class);

            String knowledgeBaseIdString = input.knowledgeBaseId();

            if (knowledgeBaseIdString == null || knowledgeBaseIdString.isBlank()) {
                return toolError("knowledgeBaseId is required");
            }

            String question = input.question();

            if (question == null || question.isBlank()) {
                return toolError("question is required and must not be blank");
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            long knowledgeBaseId;

            try {
                knowledgeBaseId = Long.parseLong(knowledgeBaseIdString);
            } catch (NumberFormatException exception) {
                return toolError("Invalid knowledgeBaseId - must be a numeric id obtained from listKnowledgeBases");
            }

            long environmentId = resolveEnvironmentId(invocationContext);

            KnowledgeBase knowledgeBase =
                resolveKnowledgeBaseInWorkspace(knowledgeBaseId, workspaceId, environmentId);

            if (knowledgeBase == null) {
                return toolError(
                    "Knowledge base " + knowledgeBaseIdString + " not found in the current workspace.");
            }

            int fetchLimit = resolveLimit(input.limit());

            List<KnowledgeBaseDocumentChunk> chunks = knowledgeBaseFacade.searchKnowledgeBase(
                knowledgeBaseId, question, null);

            List<KnowledgeBaseDocumentChunk> limitedChunks = chunks.stream()
                .limit(fetchLimit)
                .toList();

            Map<Long, String> documentTitlesById = resolveDocumentTitles(limitedChunks);

            List<SearchHit> hits = limitedChunks.stream()
                .map(chunk -> new SearchHit(
                    chunk.getKnowledgeBaseDocumentId() != null
                        ? chunk.getKnowledgeBaseDocumentId()
                            .toString()
                        : null,
                    documentTitlesById.get(chunk.getKnowledgeBaseDocumentId()),
                    truncateExcerpt(chunk.getTextContent()),
                    chunk.getScore(),
                    String.valueOf(knowledgeBaseId),
                    knowledgeBase.getName()))
                .toList();

            return jsonMapper.writeValueAsString(new QueryKnowledgeBaseResult(CITATIONS_KIND, hits));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, QueryKnowledgeBaseToolCallback.class, TOOL_NAME, exception);
        }
    }

    private @Nullable KnowledgeBase resolveKnowledgeBaseInWorkspace(
        long knowledgeBaseId, long workspaceId, long environmentId) {

        List<KnowledgeBase> workspaceKnowledgeBases =
            workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(workspaceId, environmentId);

        return workspaceKnowledgeBases.stream()
            .filter(knowledgeBase -> knowledgeBase.getId() != null && knowledgeBase.getId() == knowledgeBaseId)
            .findFirst()
            .orElse(null);
    }

    private long resolveEnvironmentId(AgentToolInvocationContext invocationContext) {
        Long environmentId = invocationContext.environmentId();

        return environmentId != null ? environmentId : DEFAULT_ENVIRONMENT_ORDINAL;
    }

    private Map<Long, String> resolveDocumentTitles(List<KnowledgeBaseDocumentChunk> chunks) {
        Map<Long, String> documentTitlesById = new HashMap<>();

        for (KnowledgeBaseDocumentChunk chunk : chunks) {
            Long documentId = chunk.getKnowledgeBaseDocumentId();

            if (documentId == null || documentTitlesById.containsKey(documentId)) {
                continue;
            }

            try {
                KnowledgeBaseDocument document = knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId);

                if (document != null && document.getName() != null) {
                    documentTitlesById.put(documentId, document.getName());
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "Could not resolve title for knowledge base document {}; its citations are returned without a "
                        + "docTitle",
                    documentId, exception);
            }
        }

        return documentTitlesById;
    }

    private int resolveLimit(@Nullable Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private @Nullable String truncateExcerpt(@Nullable String text) {
        if (text == null) {
            return null;
        }

        int maxLength = 500;

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "…";
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record QueryKnowledgeBaseInput(
        String knowledgeBaseId, String question, @Nullable Integer limit) {
    }

    public record QueryKnowledgeBaseResult(String kind, List<SearchHit> hits) {

        public QueryKnowledgeBaseResult {
            hits = List.copyOf(hits);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SearchHit(
        String docId, String docTitle, String excerpt, Float score, String knowledgeBaseId, String knowledgeBaseName) {
    }
}
