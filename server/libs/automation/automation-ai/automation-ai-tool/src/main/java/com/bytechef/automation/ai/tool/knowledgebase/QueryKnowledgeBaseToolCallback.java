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
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
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
 * Spring AI {@link ToolCallback} that performs a RAG (retrieval-augmented generation) search over a knowledge base.
 * Results are capped at {@value #MAX_LIMIT} chunks and include the document id and title, a short excerpt, the
 * similarity score, and the knowledge base id and name.
 *
 * <p>
 * The result is a {@code {"kind": "knowledge-base-citations", "hits": [...]}} envelope: the {@code kind} marker is
 * client-load-bearing — chat surfaces parse it (see {@code toToolResultDataPart.ts}) and render the hits as citation
 * chips under the assistant's answer, so the sources shown to the user come from the tool result itself rather than
 * from the model's prose. Do not rename the kind or the hit field names without updating the client.
 *
 * <p>
 * If the knowledge base does not exist or has no indexed documents the tool returns the envelope with empty hits
 * instead of an error, so the agent can gracefully report a lack of results.
 *
 *
 * @author Ivica Cardic
 */
public class QueryKnowledgeBaseToolCallback implements ToolCallback {

    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 20;

    static final String CITATIONS_KIND = "knowledge-base-citations";

    private static final Logger log = LoggerFactory.getLogger(QueryKnowledgeBaseToolCallback.class);

    private static final String DESCRIPTION = """
        Search a knowledge base using a natural-language question (RAG / vector similarity search).
        Returns {"kind": "knowledge-base-citations", "hits": [...]} where each hit carries docId,
        docTitle, excerpt, score, knowledgeBaseId, and knowledgeBaseName. Results are limited to 20
        hits maximum. Obtain the knowledgeBaseId from listKnowledgeBases first. Answer from the hit
        excerpts and cite the docTitle values as sources. Returns empty hits when the knowledge base
        has no matching content.""";

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

    private final KnowledgeBaseFacade knowledgeBaseFacade;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public QueryKnowledgeBaseToolCallback(
        KnowledgeBaseFacade knowledgeBaseFacade,
        KnowledgeBaseService knowledgeBaseService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService) {

        this.knowledgeBaseFacade = knowledgeBaseFacade;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("queryKnowledgeBase")
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

            KnowledgeBase knowledgeBase;

            try {
                knowledgeBase = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);
            } catch (RuntimeException exception) {
                if (isKnowledgeBaseNotFound(exception)) {
                    return jsonMapper.writeValueAsString(new QueryKnowledgeBaseResult(CITATIONS_KIND, List.of()));
                }

                // Anything that is not a "not found" — e.g. a connection pool exhaustion, a transaction failure or
                // a Spring-Data infrastructure error — must propagate so the LLM sees the real failure instead of
                // silently getting an empty result set.
                throw exception;
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
        }
    }

    /**
     * Resolves the display title for every distinct document referenced by the given chunks. A missing or unreadable
     * document row must not fail the whole search — the affected hit simply carries no title.
     */
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
                log.debug("Could not resolve title for knowledge base document {}", documentId, exception);
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

    /**
     * Returns true when the given exception was thrown by {@code KnowledgeBaseServiceImpl#getKnowledgeBase} to signal
     * that the requested knowledge base does not exist. The service throws a plain {@link RuntimeException} whose
     * message starts with {@code "KnowledgeBase not found"}, so we match by class + message rather than by a richer
     * type. Any other {@link RuntimeException} (DB connectivity, transaction errors, etc.) must propagate so the LLM
     * surfaces the real failure to the user instead of silently receiving an empty result set.
     */
    private boolean isKnowledgeBaseNotFound(RuntimeException exception) {
        if (exception.getClass() != RuntimeException.class) {
            return false;
        }

        String message = exception.getMessage();

        return message != null && message.startsWith("KnowledgeBase not found");
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
