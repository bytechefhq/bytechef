/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.contextstore.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuerySort;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow-step Action that runs a structured query against the workspace's Context Store replica via
 * {@link ContextStoreQueryService}. Inputs accept {@code filters} / {@code sort} as lists of {@code {field, op, value}}
 * / {@code {field, dir}} maps so the workflow editor can author them as arrays of nested-property objects (the same
 * shape an LLM emits when this Action is exposed as a TOOLS cluster element on an AI Agent step).
 *
 * <p>
 * The {@code sourceId} input parameter is the authoritative scope: workspace ownership is resolved upstream by whoever
 * built the workflow definition, so the action does not look up the workspace at runtime. The query service trusts the
 * sourceId to identify the workspace's replica.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ContextStoreSearchAction {

    public static final String SOURCE_ID = "sourceId";
    public static final String FILTERS = "filters";
    public static final String SORT = "sort";
    public static final String LIMIT = "limit";
    public static final String CURSOR = "cursor";
    public static final String INCLUDE_DELETED = "includeDeleted";

    private static final String FIELD = "field";
    private static final String OP = "op";
    private static final String VALUE = "value";
    private static final String DIR = "dir";

    private final ContextStoreQueryService contextStoreQueryService;
    private final ContextStoreSourceService contextStoreSourceService;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        ContextStoreQueryService contextStoreQueryService, ContextStoreSourceService contextStoreSourceService) {

        return new ContextStoreSearchAction(contextStoreQueryService, contextStoreSourceService).build();
    }

    private ContextStoreSearchAction(
        ContextStoreQueryService contextStoreQueryService, ContextStoreSourceService contextStoreSourceService) {

        this.contextStoreQueryService = contextStoreQueryService;
        this.contextStoreSourceService = contextStoreSourceService;
    }

    private ModifiableActionDefinition build() {
        return action("search")
            .title("Search Context Store")
            .description(
                "Query the workspace's replicated source data with structured filters, sort and cursor pagination.")
            .properties(
                integer(SOURCE_ID)
                    .label("Context Source ID")
                    .description("ID of the ContextStoreSource whose replica to query.")
                    .required(true),
                array(FILTERS)
                    .label("Filters")
                    .description(
                        "List of {field, op, value} filter objects. op is one of EQ, NEQ, IN, "
                            + "CONTAINS, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN."),
                array(SORT)
                    .label("Sort")
                    .description("List of {field, dir} sort objects. dir is ASC or DESC; defaults to ASC."),
                integer(LIMIT)
                    .label("Limit")
                    .description("Maximum number of items to return per page. Defaults to 50.")
                    .defaultValue(ContextStoreQuery.DEFAULT_LIMIT),
                string(CURSOR)
                    .label("Cursor")
                    .description("Opaque cursor returned as nextCursor by a previous page; omit for the first page."),
                bool(INCLUDE_DELETED)
                    .label("Include Deleted")
                    .description("If true, tombstoned (soft-deleted) records are included in the result.")
                    .defaultValue(false))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Long sourceId = inputParameters.getRequiredLong(SOURCE_ID);

        // Existence check: surfaces a clearer error than the query below if the source row is missing.
        contextStoreSourceService.get(sourceId);

        List<ContextStoreQueryFilter> filters = parseFilters(inputParameters);
        List<ContextStoreQuerySort> sort = parseSort(inputParameters);

        int limit = inputParameters.getInteger(LIMIT, ContextStoreQuery.DEFAULT_LIMIT);
        String cursor = inputParameters.getString(CURSOR);
        boolean includeDeleted = inputParameters.getBoolean(INCLUDE_DELETED, false);

        ContextStoreSearchResult result = contextStoreQueryService.search(new ContextStoreQuery(
            sourceId, filters, sort, limit, cursor, includeDeleted, null));

        List<Map<String, Object>> items = new ArrayList<>(result.items()
            .size());

        for (ContextStoreRecord record : result.items()) {
            items.add(toItemMap(record));
        }

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("items", items);
        response.put("nextCursor", result.nextCursor() == null ? "" : result.nextCursor());

        return response;
    }

    private static List<ContextStoreQueryFilter> parseFilters(Parameters inputParameters) {
        List<Map<String, Object>> rawFilters = inputParameters.getList(
            FILTERS, new TypeReference<>() {}, List.of());

        List<ContextStoreQueryFilter> filters = new ArrayList<>(rawFilters.size());

        for (Map<String, Object> rawFilter : rawFilters) {
            Object fieldObj = rawFilter.get(FIELD);
            Object opObj = rawFilter.get(OP);

            if (fieldObj == null || opObj == null) {
                throw new IllegalArgumentException(
                    "Filter requires 'field' and 'op': " + rawFilter);
            }

            ContextStoreQueryFilter.FilterOp op =
                ContextStoreQueryFilter.FilterOp.valueOf(opObj.toString()
                    .toUpperCase());

            filters.add(new ContextStoreQueryFilter(fieldObj.toString(), op, rawFilter.get(VALUE)));
        }

        return filters;
    }

    private static List<ContextStoreQuerySort> parseSort(Parameters inputParameters) {
        List<Map<String, Object>> rawSort = inputParameters.getList(SORT, new TypeReference<>() {}, List.of());

        List<ContextStoreQuerySort> sort = new ArrayList<>(rawSort.size());

        for (Map<String, Object> rawEntry : rawSort) {
            Object fieldObj = rawEntry.get(FIELD);

            if (fieldObj == null) {
                throw new IllegalArgumentException("Sort entry requires 'field': " + rawEntry);
            }

            Object dirObj = rawEntry.getOrDefault(DIR, "ASC");

            ContextStoreQuerySort.SortDirection direction =
                ContextStoreQuerySort.SortDirection.valueOf(dirObj.toString()
                    .toUpperCase());

            sort.add(new ContextStoreQuerySort(fieldObj.toString(), direction));
        }

        return sort;
    }

    private static Map<String, Object> toItemMap(ContextStoreRecord record) {
        Map<String, Object> itemMap = new LinkedHashMap<>();

        itemMap.put("id", record.getId());
        itemMap.put("sourceRecordId", record.getSourceRecordId());
        itemMap.put("payload", record.getPayload());
        itemMap.put("lastSeenAt", record.getLastSeenAt());
        itemMap.put("deletedAt", record.getDeletedAt());

        return itemMap;
    }
}
