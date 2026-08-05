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
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuerySort;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
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
 * Spring AI {@link ToolCallback} that searches Context Store records via {@link ContextStoreQueryService#search}.
 * Translates a JSON tool input shape ({@code sourceId}, {@code entity}, optional {@code filters}, {@code sort},
 * {@code limit}, {@code cursor}) into a {@link ContextStoreQuery} and returns the search results as JSON. Read-only;
 * registered on both ASK and BUILD agents.
 *
 * <p>
 * Workspace scoping is enforced by validating that the requested {@code sourceId} belongs to the chat's workspace
 * before issuing the query — surfaces "not found" rather than leaking another workspace's data.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public class SearchContextStoreToolCallback implements ToolCallback {

    static final String TOOL_NAME = "searchContextStore";

    private static final String DESCRIPTION = """
        Search records in a Context Store source by sourceId. Filters apply to indexed fields only.
        Supported filter ops: EQ, NEQ, IN, CONTAINS, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN.
        Sort directions: ASC, DESC. Pagination: pass the returned nextCursor in a follow-up call. Limit defaults to
        50 and is clamped at 500. Use listContextSources first to discover available sources.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "sourceId": {"type": "integer", "description": "Context Store source id"},
                    "filters": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "field": {"type": "string"},
                                "op": {"type": "string", "enum": ["EQ","NEQ","IN","CONTAINS","STARTS_WITH","GT","GTE","LT","LTE","BETWEEN"]},
                                "value": {}
                            },
                            "required": ["field", "op", "value"]
                        }
                    },
                    "sort": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "field": {"type": "string"},
                                "dir": {"type": "string", "enum": ["ASC","DESC"]}
                            },
                            "required": ["field", "dir"]
                        }
                    },
                    "limit": {"type": "integer", "minimum": 1, "maximum": 500},
                    "cursor": {"type": "string"}
                },
                "required": ["sourceId"]
            }""";

    private final ContextStoreQueryService contextStoreQueryService;
    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SearchContextStoreToolCallback(
        ContextStoreQueryService contextStoreQueryService,
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService) {

        this.contextStoreQueryService = contextStoreQueryService;
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
            SearchContextStoreInput input = jsonMapper.readValue(toolInput, SearchContextStoreInput.class);

            if (input.sourceId() == null) {
                return toolError("sourceId is required");
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

            Integer inputLimit = input.limit();

            int limit = inputLimit == null ? ContextStoreQuery.DEFAULT_LIMIT : inputLimit;

            if (limit <= 0) {
                limit = ContextStoreQuery.DEFAULT_LIMIT;
            }

            if (limit > ContextStoreQuery.MAX_LIMIT) {
                limit = ContextStoreQuery.MAX_LIMIT;
            }

            List<ContextStoreQueryFilter> filters = toFilters(input.filters());
            List<ContextStoreQuerySort> sorts = toSorts(input.sort());

            ContextStoreQuery query = new ContextStoreQuery(
                input.sourceId(), filters, sorts, limit, input.cursor(), false, null);

            ContextStoreSearchResult result = contextStoreQueryService.search(query);

            List<Map<String, Object>> items = new ArrayList<>(result.items()
                .size());

            for (ContextStoreRecord record : result.items()) {
                items.add(toRecordMap(record));
            }

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("items", items);
            response.put("nextCursor", result.nextCursor());

            return jsonMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, SearchContextStoreToolCallback.class, TOOL_NAME, exception);
        }
    }

    private List<ContextStoreQueryFilter> toFilters(@Nullable List<FilterInput> filterInputs) {
        if (filterInputs == null || filterInputs.isEmpty()) {
            return List.of();
        }

        List<ContextStoreQueryFilter> filters = new ArrayList<>(filterInputs.size());

        for (FilterInput filterInput : filterInputs) {
            ContextStoreQueryFilter.FilterOp op = ContextStoreQueryFilter.FilterOp.valueOf(filterInput.op()
                .toUpperCase());

            filters.add(new ContextStoreQueryFilter(filterInput.field(), op, filterInput.value()));
        }

        return filters;
    }

    private List<ContextStoreQuerySort> toSorts(@Nullable List<SortInput> sortInputs) {
        if (sortInputs == null || sortInputs.isEmpty()) {
            return List.of();
        }

        List<ContextStoreQuerySort> sorts = new ArrayList<>(sortInputs.size());

        for (SortInput sortInput : sortInputs) {
            ContextStoreQuerySort.SortDirection dir = ContextStoreQuerySort.SortDirection.valueOf(sortInput.dir()
                .toUpperCase());

            sorts.add(new ContextStoreQuerySort(sortInput.field(), dir));
        }

        return sorts;
    }

    private Map<String, Object> toRecordMap(ContextStoreRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("id", record.getId());
        map.put("sourceRecordId", record.getSourceRecordId());
        map.put("payload", record.getPayload());
        map.put("lastSeenAt", record.getLastSeenAt() == null ? null : record.getLastSeenAt()
            .toString());

        return map;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record SearchContextStoreInput(
        Long sourceId, @Nullable List<FilterInput> filters, @Nullable List<SortInput> sort,
        @Nullable Integer limit, @Nullable String cursor) {
    }

    public record FilterInput(String field, String op, Object value) {
    }

    public record SortInput(String field, String dir) {
    }
}
