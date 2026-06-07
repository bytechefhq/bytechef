/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.tool;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuerySort;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService.SemanticHit;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.ai.tool.facade.AbstractToolFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mints typed per-source {@link FunctionToolCallback} instances for the {@code McpServer} enumeration path. Each
 * callback's name is {@code CONTEXT_STORE_<SOURCE_COMPONENT>_<ENTITY>_SEARCH}; the input schema is generated from the
 * source's {@code indexedFields} so the LLM sees the valid filter-field names as a JSON Schema {@code enum}.
 *
 * <p>
 * The {@link ContextStoreSource} reference is bound at callback-mint time (closure-captured); the LLM-supplied filters
 * / sort / limit / cursor flow through at call time. If a source's indexable shape changes after a callback is minted,
 * the existing callback continues to reflect the old shape until the {@code McpServer} re-enumerates by calling
 * {@link #getFunctionToolCallbacks(Long)} again.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI2")
public class ContextStoreToolCallbackFacadeImpl extends AbstractToolFacade implements ContextStoreToolCallbackFacade {

    private static final int DEFAULT_SEMANTIC_TOP_K = 10;

    private final ContextStoreQueryService contextStoreQueryService;
    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;
    private final ObjectProvider<ContextStoreSemanticSearchService> semanticSearchServiceProvider;

    public ContextStoreToolCallbackFacadeImpl(
        ContextStoreQueryService contextStoreQueryService,
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService, Evaluator evaluator,
        ObjectProvider<ContextStoreSemanticSearchService> semanticSearchServiceProvider) {

        super(evaluator);

        this.contextStoreQueryService = contextStoreQueryService;
        this.workspaceContextStoreSourceService = workspaceContextStoreSourceService;
        this.semanticSearchServiceProvider = semanticSearchServiceProvider;
    }

    @Override
    public List<ToolCallback> getFunctionToolCallbacks(Long workspaceId) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        List<ContextStoreSource> contextStoreSources =
            workspaceContextStoreSourceService.getAllEnabledSourcesByWorkspaceId(workspaceId);

        for (ContextStoreSource contextStoreSource : contextStoreSources) {
            toolCallbacks.add(buildCallback(contextStoreSource));
        }

        toolCallbacks.addAll(getSemanticFunctionToolCallbacks(workspaceId));

        return toolCallbacks;
    }

    @Override
    public List<ToolCallback> getSemanticFunctionToolCallbacks(Long workspaceId) {
        ContextStoreSemanticSearchService semanticSearchService = semanticSearchServiceProvider.getIfAvailable();

        if (semanticSearchService == null) {
            return List.of();
        }

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        List<ContextStoreSource> contextStoreSources =
            workspaceContextStoreSourceService.getAllEnabledSourcesByWorkspaceId(workspaceId);

        for (ContextStoreSource contextStoreSource : contextStoreSources) {
            if (!hasSemanticIndexFields(contextStoreSource)) {
                continue;
            }

            toolCallbacks.add(buildSemanticCallback(contextStoreSource, semanticSearchService));
        }

        return toolCallbacks;
    }

    private static boolean hasSemanticIndexFields(ContextStoreSource source) {
        Map<String, ?> semanticIndexFields = source.getSemanticIndexFields();

        if (semanticIndexFields == null || semanticIndexFields.isEmpty()) {
            return false;
        }

        Object fieldsValue = semanticIndexFields.get("fields");

        if (fieldsValue instanceof List<?> list) {
            return !list.isEmpty();
        }

        return false;
    }

    private ToolCallback buildCallback(ContextStoreSource source) {
        String toolName = buildToolName(source.getSourceComponentName(), source.getEntityName());
        String description = buildDescription(source);
        String inputSchema = buildInputSchema(source);

        Function<Map<String, Object>, Object> function = arguments -> executeSearch(source, arguments);

        FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
            .builder(toolName, function)
            .inputType(Map.class)
            .inputSchema(inputSchema)
            .description(description);

        return builder.build();
    }

    private static String buildToolName(String sourceComponentName, String entityName) {
        String sanitizedComponent = sourceComponentName.toUpperCase()
            .replaceAll("[^A-Z0-9_]", "_");
        String sanitizedEntity = entityName.toUpperCase()
            .replaceAll("[^A-Z0-9_]", "_");

        return "CONTEXT_STORE_" + sanitizedComponent + "_" + sanitizedEntity + "_SEARCH";
    }

    private static String buildSemanticToolName(String sourceComponentName, String entityName) {
        String sanitizedComponent = sourceComponentName.toUpperCase()
            .replaceAll("[^A-Z0-9_]", "_");
        String sanitizedEntity = entityName.toUpperCase()
            .replaceAll("[^A-Z0-9_]", "_");

        return "CONTEXT_STORE_" + sanitizedComponent + "_" + sanitizedEntity + "_SEMANTIC_SEARCH";
    }

    private ToolCallback buildSemanticCallback(
        ContextStoreSource source, ContextStoreSemanticSearchService semanticSearchService) {

        String toolName = buildSemanticToolName(source.getSourceComponentName(), source.getEntityName());
        String description = buildSemanticDescription(source);
        String inputSchema = buildSemanticInputSchema(source);

        Function<Map<String, Object>, Object> function =
            arguments -> executeSemanticSearch(source, semanticSearchService, arguments);

        FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
            .builder(toolName, function)
            .inputType(Map.class)
            .inputSchema(inputSchema)
            .description(description);

        return builder.build();
    }

    private static String buildSemanticDescription(ContextStoreSource source) {
        String description = source.getDescription();

        if (description != null && !description.isBlank()) {
            return String.format(
                "Semantic search of %s records from Context Store source '%s' (component: %s) by natural-language "
                    + "query. Optional structured 'prefilter' restricts the candidate set before similarity ranking. "
                    + "%s",
                source.getEntityName(), source.getName(), source.getSourceComponentName(), description);
        }

        return String.format(
            "Semantic search of %s records from Context Store source '%s' (component: %s) by natural-language "
                + "query. Optional structured 'prefilter' restricts the candidate set before similarity ranking.",
            source.getEntityName(), source.getName(), source.getSourceComponentName());
    }

    @SuppressWarnings("unchecked")
    private Object executeSemanticSearch(
        ContextStoreSource source, ContextStoreSemanticSearchService semanticSearchService,
        Map<String, Object> arguments) {

        String queryText = (String) arguments.get("queryText");

        if (queryText == null || queryText.isBlank()) {
            throw new IllegalArgumentException("queryText is required");
        }

        int k = parseTopK(arguments.get("k"));

        Object rawPrefilter = arguments.get("prefilter");
        ContextStoreQueryFilter prefilter = null;

        if (rawPrefilter instanceof Map<?, ?> prefilterMap) {
            prefilter = parseFilter((Map<String, Object>) prefilterMap);
        }

        List<SemanticHit> hits = prefilter == null
            ? semanticSearchService.searchSemantic(source.getId(), queryText, k)
            : semanticSearchService.searchSemantic(source.getId(), queryText, k, prefilter);

        List<Map<String, Object>> items = new ArrayList<>(hits.size());

        for (SemanticHit hit : hits) {
            Map<String, Object> item = new LinkedHashMap<>();

            item.put("record", hit.record());
            item.put("similarityScore", hit.similarityScore());

            items.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("items", items);

        return response;
    }

    private static int parseTopK(@Nullable Object rawK) {
        if (rawK instanceof Number number) {
            int value = number.intValue();

            return value <= 0 ? DEFAULT_SEMANTIC_TOP_K : value;
        }

        if (rawK instanceof String string && !string.isBlank()) {
            try {
                int value = Integer.parseInt(string);

                return value <= 0 ? DEFAULT_SEMANTIC_TOP_K : value;
            } catch (NumberFormatException ignored) {
                return DEFAULT_SEMANTIC_TOP_K;
            }
        }

        return DEFAULT_SEMANTIC_TOP_K;
    }

    private static ContextStoreQueryFilter parseFilter(Map<String, Object> rawFilter) {
        String field = (String) rawFilter.get("field");
        String op = (String) rawFilter.get("op");
        Object value = rawFilter.get("value");

        if (field == null || op == null) {
            throw new IllegalArgumentException("prefilter requires 'field' and 'op'");
        }

        return new ContextStoreQueryFilter(field, ContextStoreQueryFilter.FilterOp.valueOf(op.toUpperCase()), value);
    }

    private static String buildDescription(ContextStoreSource source) {
        String description = source.getDescription();

        if (description != null && !description.isBlank()) {
            return String.format(
                "Search %s records from Context Store source '%s' (component: %s) by structured filters. %s",
                source.getEntityName(), source.getName(), source.getSourceComponentName(), description);
        }

        return String.format(
            "Search %s records from Context Store source '%s' (component: %s) by structured filters. "
                + "Returns matching records with their full payload.",
            source.getEntityName(), source.getName(), source.getSourceComponentName());
    }

    @SuppressWarnings("unchecked")
    private Object executeSearch(ContextStoreSource source, Map<String, Object> arguments) {
        List<ContextStoreQueryFilter> filters = parseFilters(
            (List<Map<String, Object>>) arguments.getOrDefault("filters", List.of()));
        List<ContextStoreQuerySort> sort = parseSort(
            (List<Map<String, Object>>) arguments.getOrDefault("sort", List.of()));

        int limit = parseLimit(arguments.get("limit"));
        String cursor = (String) arguments.get("cursor");

        ContextStoreSearchResult result = contextStoreQueryService.search(
            new ContextStoreQuery(source.getId(), filters, sort, limit, cursor, false, null));

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("items", result.items());
        response.put("nextCursor", result.nextCursor() == null ? "" : result.nextCursor());

        return response;
    }

    private static List<ContextStoreQueryFilter> parseFilters(@Nullable List<Map<String, Object>> rawFilters) {
        List<ContextStoreQueryFilter> filters = new ArrayList<>();

        if (rawFilters == null) {
            return filters;
        }

        for (Map<String, Object> rawFilter : rawFilters) {
            String field = (String) rawFilter.get("field");
            String op = (String) rawFilter.get("op");
            Object value = rawFilter.get("value");

            filters.add(
                new ContextStoreQueryFilter(field, ContextStoreQueryFilter.FilterOp.valueOf(op.toUpperCase()), value));
        }

        return filters;
    }

    private static List<ContextStoreQuerySort> parseSort(@Nullable List<Map<String, Object>> rawSort) {
        List<ContextStoreQuerySort> sort = new ArrayList<>();

        if (rawSort == null) {
            return sort;
        }

        for (Map<String, Object> rawSortItem : rawSort) {
            String field = (String) rawSortItem.get("field");
            String dir = (String) rawSortItem.getOrDefault("dir", "ASC");

            sort.add(new ContextStoreQuerySort(field, ContextStoreQuerySort.SortDirection.valueOf(dir.toUpperCase())));
        }

        return sort;
    }

    private static int parseLimit(@Nullable Object rawLimit) {
        if (rawLimit instanceof Number number) {
            return number.intValue();
        }

        if (rawLimit instanceof String string && !string.isBlank()) {
            return Integer.parseInt(string);
        }

        return ContextStoreQuery.DEFAULT_LIMIT;
    }

    private static String buildSemanticInputSchema(ContextStoreSource source) {
        Map<String, ?> indexedFields = source.getIndexedFields();

        List<String> fieldNames = indexedFields == null ? List.of() : new ArrayList<>(indexedFields.keySet());

        Map<String, Object> queryTextSchema = new LinkedHashMap<>();

        queryTextSchema.put("type", "string");
        queryTextSchema.put("description", "Natural-language query for similarity ranking.");

        Map<String, Object> kSchema = new LinkedHashMap<>();

        kSchema.put("type", "integer");
        kSchema.put("default", DEFAULT_SEMANTIC_TOP_K);
        kSchema.put("minimum", 1);

        Map<String, Object> filterFieldProperty = new LinkedHashMap<>();

        filterFieldProperty.put("type", "string");
        filterFieldProperty.put("description", "Indexed field to pre-filter on.");

        if (!fieldNames.isEmpty()) {
            filterFieldProperty.put("enum", fieldNames);
        }

        Map<String, Object> filterOpProperty = Map.of(
            "type", "string",
            "enum",
            List.of("EQ", "NEQ", "IN", "CONTAINS", "STARTS_WITH", "GT", "GTE", "LT", "LTE", "BETWEEN"));

        Map<String, Object> filterValueProperty = Map.of(
            "description", "Filter value. List for IN / BETWEEN, primitive otherwise.");

        Map<String, Object> prefilterProperties = new LinkedHashMap<>();

        prefilterProperties.put("field", filterFieldProperty);
        prefilterProperties.put("op", filterOpProperty);
        prefilterProperties.put("value", filterValueProperty);

        Map<String, Object> prefilterSchema = new LinkedHashMap<>();

        prefilterSchema.put("type", "object");
        prefilterSchema.put(
            "description",
            "Optional structured pre-filter. The candidate set is restricted before similarity ranking.");
        prefilterSchema.put("properties", prefilterProperties);
        prefilterSchema.put("required", List.of("field", "op", "value"));

        Map<String, Object> properties = new LinkedHashMap<>();

        properties.put("queryText", queryTextSchema);
        properties.put("k", kSchema);
        properties.put("prefilter", prefilterSchema);

        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("queryText"));

        return JsonUtils.write(schema);
    }

    private static String buildInputSchema(ContextStoreSource source) {
        Map<String, ?> indexedFields = source.getIndexedFields();

        List<String> fieldNames = indexedFields == null ? List.of() : new ArrayList<>(indexedFields.keySet());

        Map<String, Object> filterFieldProperty = new LinkedHashMap<>();

        filterFieldProperty.put("type", "string");
        filterFieldProperty.put("description", "Indexed field to filter on.");

        if (!fieldNames.isEmpty()) {
            filterFieldProperty.put("enum", fieldNames);
        }

        Map<String, Object> filterOpProperty = Map.of(
            "type", "string",
            "enum",
            List.of("EQ", "NEQ", "IN", "CONTAINS", "STARTS_WITH", "GT", "GTE", "LT", "LTE", "BETWEEN"));

        Map<String, Object> filterValueProperty = Map.of(
            "description", "Filter value. List for IN / BETWEEN, primitive otherwise.");

        Map<String, Object> filterItemProperties = new LinkedHashMap<>();

        filterItemProperties.put("field", filterFieldProperty);
        filterItemProperties.put("op", filterOpProperty);
        filterItemProperties.put("value", filterValueProperty);

        Map<String, Object> filterItemSchema = new LinkedHashMap<>();

        filterItemSchema.put("type", "object");
        filterItemSchema.put("properties", filterItemProperties);
        filterItemSchema.put("required", List.of("field", "op", "value"));

        Map<String, Object> filtersSchema = new LinkedHashMap<>();

        filtersSchema.put("type", "array");
        filtersSchema.put("description", "List of filter clauses ANDed together.");
        filtersSchema.put("items", filterItemSchema);

        Map<String, Object> sortFieldProperty = new LinkedHashMap<>();

        sortFieldProperty.put("type", "string");

        if (!fieldNames.isEmpty()) {
            sortFieldProperty.put("enum", fieldNames);
        }

        Map<String, Object> sortItemProperties = new LinkedHashMap<>();

        sortItemProperties.put("field", sortFieldProperty);
        sortItemProperties.put("dir", Map.of("type", "string", "enum", List.of("ASC", "DESC")));

        Map<String, Object> sortItemSchema = new LinkedHashMap<>();

        sortItemSchema.put("type", "object");
        sortItemSchema.put("properties", sortItemProperties);

        Map<String, Object> sortSchema = new LinkedHashMap<>();

        sortSchema.put("type", "array");
        sortSchema.put("description", "Optional ordering. First entry has highest precedence.");
        sortSchema.put("items", sortItemSchema);

        Map<String, Object> limitSchema = new LinkedHashMap<>();

        limitSchema.put("type", "integer");
        limitSchema.put("default", ContextStoreQuery.DEFAULT_LIMIT);
        limitSchema.put("minimum", 1);
        limitSchema.put("maximum", ContextStoreQuery.MAX_LIMIT);

        Map<String, Object> cursorSchema = Map.of(
            "type", "string",
            "description", "Opaque cursor from a previous response's nextCursor for pagination.");

        Map<String, Object> properties = new LinkedHashMap<>();

        properties.put("filters", filtersSchema);
        properties.put("sort", sortSchema);
        properties.put("limit", limitSchema);
        properties.put("cursor", cursorSchema);

        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        schema.put("properties", properties);

        return JsonUtils.write(schema);
    }
}
