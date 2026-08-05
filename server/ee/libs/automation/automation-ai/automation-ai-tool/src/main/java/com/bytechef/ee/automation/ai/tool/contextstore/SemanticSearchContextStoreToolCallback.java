/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.contextstore;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService.SemanticHit;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that runs a semantic search against the Context Store via
 * {@link ContextStoreSemanticSearchService}. Translates a JSON tool input shape ({@code sourceId}, {@code entity},
 * {@code queryText}, optional {@code k}, optional {@code prefilter}) into the appropriate Mode 2 / Mode 3 call and
 * returns the ranked records as JSON.
 *
 * <p>
 * Read-only; registered on the ASK agent only. Workspace scoping is enforced by validating that the requested
 * {@code sourceId} belongs to the chat's workspace before issuing the query — surfaces "not found" rather than leaking
 * another workspace's data.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public class SemanticSearchContextStoreToolCallback implements ToolCallback {

    static final String TOOL_NAME = "semanticSearchContextStore";

    private static final int DEFAULT_TOP_K = 10;

    private static final String DESCRIPTION =
        """
            Run a semantic (similarity) search over Context Store records by sourceId. Records must have \
            semanticIndexFields populated on the source. Pass a natural-language queryText; results are ranked by \
            similarity. Optional `prefilter` restricts the candidate set before similarity ranking — same shape as \
            the structured-search filters: {field, op, value}. Use listContextSources first to discover sources.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "sourceId": {"type": "integer", "description": "Context Store source id"},
                    "queryText": {"type": "string", "description": "Natural-language query for similarity ranking"},
                    "k": {"type": "integer", "minimum": 1, "default": 10},
                    "prefilter": {
                        "type": "object",
                        "description": "Optional structured pre-filter applied before similarity ranking",
                        "properties": {
                            "field": {"type": "string"},
                            "op": {"type": "string", "enum": ["EQ","NEQ","IN","CONTAINS","STARTS_WITH","GT","GTE","LT","LTE","BETWEEN"]},
                            "value": {}
                        },
                        "required": ["field", "op", "value"]
                    }
                },
                "required": ["sourceId", "queryText"]
            }""";

    private final ContextStoreSemanticSearchService semanticSearchService;
    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SemanticSearchContextStoreToolCallback(
        ContextStoreSemanticSearchService semanticSearchService,
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService) {

        this.semanticSearchService = semanticSearchService;
        this.workspaceContextStoreSourceService = workspaceContextStoreSourceService;
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
            SemanticSearchContextStoreInput input =
                jsonMapper.readValue(toolInput, SemanticSearchContextStoreInput.class);

            if (input.sourceId() == null) {
                return toolError("sourceId is required");
            }

            if (input.queryText() == null || input.queryText()
                .isBlank()) {
                return toolError("queryText is required");
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            Long sourceWorkspaceId = workspaceContextStoreSourceService
                .fetchWorkspaceIdByContextStoreSourceId(input.sourceId())
                .orElse(null);

            if (sourceWorkspaceId == null || !workspaceId.equals(sourceWorkspaceId)) {
                return toolError("Context Store source " + input.sourceId() + " not found in the current workspace.");
            }

            Integer inputK = input.k();

            int k = inputK == null ? DEFAULT_TOP_K : inputK;

            if (k <= 0) {
                k = DEFAULT_TOP_K;
            }

            ContextStoreQueryFilter prefilter = toFilter(input.prefilter());

            List<SemanticHit> hits = prefilter == null
                ? semanticSearchService.searchSemantic(input.sourceId(), input.queryText(), k)
                : semanticSearchService.searchSemantic(input.sourceId(), input.queryText(), k, prefilter);

            List<Map<String, Object>> items = new ArrayList<>(hits.size());

            for (SemanticHit hit : hits) {
                items.add(toHitMap(hit));
            }

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("items", items);

            return jsonMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, SemanticSearchContextStoreToolCallback.class, TOOL_NAME, exception);
        }
    }

    @Nullable
    private static ContextStoreQueryFilter toFilter(@Nullable PrefilterInput prefilterInput) {
        if (prefilterInput == null) {
            return null;
        }

        if (prefilterInput.field() == null || prefilterInput.op() == null) {
            throw new IllegalArgumentException("prefilter requires 'field' and 'op'");
        }

        ContextStoreQueryFilter.FilterOp op = ContextStoreQueryFilter.FilterOp.valueOf(prefilterInput.op()
            .toUpperCase());

        return new ContextStoreQueryFilter(prefilterInput.field(), op, prefilterInput.value());
    }

    private static Map<String, Object> toHitMap(SemanticHit hit) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("id", hit.record()
            .getId());
        map.put("sourceRecordId", hit.record()
            .getSourceRecordId());
        map.put("payload", hit.record()
            .getPayload());
        map.put("similarityScore", hit.similarityScore());
        map.put("lastSeenAt", hit.record()
            .getLastSeenAt() == null ? null
                : hit.record()
                    .getLastSeenAt()
                    .toString());

        return map;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record SemanticSearchContextStoreInput(
        Long sourceId, String queryText, @Nullable Integer k, @Nullable PrefilterInput prefilter) {
    }

    public record PrefilterInput(String field, String op, Object value) {
    }
}
