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
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuerySort;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import com.bytechef.ee.platform.contextstore.service.ContextStoreNameLookupService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.component.definition.ActionContextAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Store-level variant of {@link ContextStoreSearchAction}. Takes a {@code contextStoreId} (or, for env-portable
 * workflows, a {@code contextStoreName}) + {@code entity} pair and fans the structured query out across every source
 * under that store whose {@code entityName} matches, returning a merged result. The point is to let a workflow target
 * "all records of this entity in this store" without hardcoding individual {@code sourceId}s — useful when a store has
 * multiple sources contributing the same logical entity (e.g. HubSpot + Salesforce both populating {@code contacts})
 * and the workflow author wants a single search across them.
 *
 * <p>
 * Env-portable variant: when {@code contextStoreId} is absent and {@code contextStoreName} is supplied, the action
 * resolves the id at perform-time using the running workflow's workspace + environment. {@code environmentId} comes
 * from {@link ActionContextAware#getEnvironmentId()}, which is already plumbed by the platform; {@code workspaceId} is
 * read from the parent Atlas Job's {@code __jobParameters.contextStore.workspaceId} entry, injected at trigger
 * fire-time by {@code ContextStoreWorkflowContextJobParameterContributor}. Result: a single workflow definition can
 * promote across DEVELOPMENT / STAGING / PRODUCTION targeting the same logical store in each env without ever touching
 * the id.
 *
 * <p>
 * Sort/cursor caveats: each underlying source has its own cursor, so the merged result cannot be cursored across
 * sources in a single token. This action exposes a single {@code limit} that caps the union, runs each source query
 * with the same {@code limit}, then truncates the concatenation. For MVP this is intentionally simple — heavier
 * use-cases that need true sorted pagination across sources should fall back to {@link ContextStoreSearchAction} with
 * an explicit {@code sourceId}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ContextStoreSearchByStoreAction {

    public static final String CONTEXT_STORE_ID = "contextStoreId";
    public static final String CONTEXT_STORE_NAME = "contextStoreName";
    public static final String ENTITY = "entity";
    public static final String FILTERS = "filters";
    public static final String SORT = "sort";
    public static final String LIMIT = "limit";
    public static final String INCLUDE_DELETED = "includeDeleted";

    /**
     * Reserved metadata key the platform coordinator stores trigger-time JobParameter overrides under. Matches
     * {@code JobMetadataKeys.JOB_PARAMETERS} in {@code platform-workflow-coordinator-api}; inlined here to avoid
     * pulling a coordinator-api dep into this component module — same precedent as
     * {@code DataStreamStreamActionDefinition}.
     */
    private static final String JOB_PARAMETERS_METADATA_KEY = "__jobParameters";

    /**
     * JobParameter key set by {@code ContextStoreWorkflowContextJobParameterContributor}. Kept in sync via review;
     * inlined for the same module-layering reason as above.
     */
    private static final String WORKSPACE_ID_JOB_PARAM_KEY = "contextStore.workspaceId";

    private static final String FIELD = "field";
    private static final String OP = "op";
    private static final String VALUE = "value";
    private static final String DIR = "dir";

    private final ObjectProvider<ContextStoreNameLookupService> contextStoreNameLookupServiceProvider;
    private final ContextStoreQueryService contextStoreQueryService;
    private final ContextStoreSourceService contextStoreSourceService;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        ObjectProvider<ContextStoreNameLookupService> contextStoreNameLookupServiceProvider,
        ContextStoreQueryService contextStoreQueryService, ContextStoreSourceService contextStoreSourceService) {

        return new ContextStoreSearchByStoreAction(
            contextStoreNameLookupServiceProvider, contextStoreQueryService, contextStoreSourceService).build();
    }

    // Package-private so the in-package test can construct an instance directly and call perform(); the public
    // entry-point remains the of(...) factory.
    @SuppressFBWarnings("EI2")
    ContextStoreSearchByStoreAction(
        ObjectProvider<ContextStoreNameLookupService> contextStoreNameLookupServiceProvider,
        ContextStoreQueryService contextStoreQueryService, ContextStoreSourceService contextStoreSourceService) {

        this.contextStoreNameLookupServiceProvider = contextStoreNameLookupServiceProvider;
        this.contextStoreQueryService = contextStoreQueryService;
        this.contextStoreSourceService = contextStoreSourceService;
    }

    private ModifiableActionDefinition build() {
        return action("searchByStore")
            .title("Search Context Store by Store")
            .description(
                "Run a structured query across every source in a Context Store. Identify the store by id, or by name "
                    + "for env-portable workflows that resolve the right store per environment at run time.")
            .properties(
                integer(CONTEXT_STORE_ID)
                    .label("Context Store ID")
                    .description(
                        "ID of the parent ContextStore. Required unless {@code contextStoreName} is supplied; when "
                            + "both are present, id wins.")
                    .required(false),
                string(CONTEXT_STORE_NAME)
                    .label("Context Store Name")
                    .description(
                        "Workspace-stable name of the ContextStore. Resolved to an id at run time using the workflow's "
                            + "current workspace + environment, so the same workflow definition works across DEVELOPMENT "
                            + "/ STAGING / PRODUCTION without hardcoded ids.")
                    .required(false),
                string(ENTITY)
                    .label("Entity Name")
                    .description("Logical entity name shared across sources (e.g. \"contacts\").")
                    .required(true),
                array(FILTERS)
                    .label("Filters")
                    .description(
                        "List of {field, op, value} filter objects. op is one of EQ, NEQ, IN, "
                            + "CONTAINS, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN."),
                array(SORT)
                    .label("Sort")
                    .description("List of {field, dir} sort objects applied per-source before merging."),
                integer(LIMIT)
                    .label("Limit")
                    .description(
                        "Maximum number of items in the merged result. Each source is queried with this same limit "
                            + "and the concatenation is truncated to it.")
                    .defaultValue(ContextStoreQuery.DEFAULT_LIMIT),
                bool(INCLUDE_DELETED)
                    .label("Include Deleted")
                    .description("If true, tombstoned records are included from each source.")
                    .defaultValue(false))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Long contextStoreId = resolveContextStoreId(inputParameters, actionContext);
        String entity = inputParameters.getRequiredString(ENTITY);

        List<ContextStoreSource> sources = contextStoreSourceService.findAllByContextStoreId(contextStoreId);

        List<ContextStoreQueryFilter> filters = parseFilters(inputParameters);
        List<ContextStoreQuerySort> sort = parseSort(inputParameters);

        int limit = inputParameters.getInteger(LIMIT, ContextStoreQuery.DEFAULT_LIMIT);
        boolean includeDeleted = inputParameters.getBoolean(INCLUDE_DELETED, false);

        List<Map<String, Object>> merged = new ArrayList<>();

        for (ContextStoreSource source : sources) {
            if (merged.size() >= limit) {
                break;
            }

            if (!entity.equals(source.getEntityName())) {
                // Source is 1:1 with its entityName; sources whose entityName doesn't match the requested entity are
                // skipped entirely rather than queried for an empty set.
                continue;
            }

            // No cross-source cursor: each fan-out call runs as a first page. The per-source result honours the
            // shared filter/sort; the merge stops at the union limit.
            ContextStoreSearchResult result = contextStoreQueryService.search(new ContextStoreQuery(
                source.getId(), filters, sort, limit, null, includeDeleted, null));

            for (ContextStoreRecord record : result.items()) {
                if (merged.size() >= limit) {
                    break;
                }

                merged.add(toItemMap(record, source));
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("items", merged);
        // Cursor pagination across sources is not supported; document the absence so callers don't expect a token.
        response.put("nextCursor", "");

        return response;
    }

    /**
     * Picks an explicit {@code contextStoreId} when provided, otherwise resolves the {@code contextStoreName} input via
     * the workflow's current workspace + environment. Throws with a precise message when neither input is set, when the
     * workspace cannot be resolved from job metadata (e.g. ad-hoc REST invocation with no trigger-injected context), or
     * when the name has no matching store in this environment.
     */
    private Long resolveContextStoreId(Parameters inputParameters, ActionContext actionContext) {
        Long contextStoreId = inputParameters.getLong(CONTEXT_STORE_ID);

        if (contextStoreId != null) {
            return contextStoreId;
        }

        String contextStoreName = inputParameters.getString(CONTEXT_STORE_NAME);

        if (contextStoreName == null || contextStoreName.isBlank()) {
            throw new IllegalArgumentException(
                "searchByStore requires either '" + CONTEXT_STORE_ID + "' or '" + CONTEXT_STORE_NAME + "'");
        }

        ContextStoreNameLookupService lookupService = contextStoreNameLookupServiceProvider.getIfAvailable();

        if (lookupService == null) {
            throw new IllegalStateException(
                "Cannot resolve Context Store by name: no ContextStoreNameLookupService bean is wired. This deployment "
                    + "lacks the automation-context-store module — use '" + CONTEXT_STORE_ID + "' instead.");
        }

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;
        Long environmentId = actionContextAware.getEnvironmentId();
        Long workspaceId = extractWorkspaceId(actionContextAware.getJobMetadata());

        if (workspaceId == null || environmentId == null) {
            throw new IllegalStateException(
                "Cannot resolve Context Store '" + contextStoreName + "' by name: workspace or environment is not "
                    + "available in the current action context. The name-based path requires a trigger-initiated "
                    + "workflow run; pass '" + CONTEXT_STORE_ID + "' for ad-hoc invocations.");
        }

        Optional<Long> resolved = lookupService.findIdByName(workspaceId, contextStoreName, environmentId);

        return resolved.orElseThrow(() -> new IllegalArgumentException(
            "No Context Store named '" + contextStoreName + "' found in workspace " + workspaceId
                + " at environment " + environmentId));
    }

    @SuppressWarnings("unchecked")
    private static Long extractWorkspaceId(Map<String, Object> jobMetadata) {
        Object jobParameters = jobMetadata.get(JOB_PARAMETERS_METADATA_KEY);

        if (!(jobParameters instanceof Map<?, ?> map)) {
            return null;
        }

        Object value = ((Map<String, ?>) map).get(WORKSPACE_ID_JOB_PARAM_KEY);

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException numberFormatException) {
                return null;
            }
        }

        return null;
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

    private static Map<String, Object> toItemMap(ContextStoreRecord record, ContextStoreSource source) {
        Map<String, Object> itemMap = new LinkedHashMap<>();

        itemMap.put("id", record.getId());
        // sourceId is added here (vs. the single-source action) because the merged result mixes records from N
        // sources; without it the caller can't tell which source a record originated from.
        itemMap.put("sourceId", source.getId());
        itemMap.put("sourceRecordId", record.getSourceRecordId());
        // entityName is denormalised from the source for caller convenience — it doesn't exist on the record itself
        // anymore now that source is 1:1 with entity.
        itemMap.put("entityName", source.getEntityName());
        itemMap.put("payload", record.getPayload());
        itemMap.put("lastSeenAt", record.getLastSeenAt());
        itemMap.put("deletedAt", record.getDeletedAt());

        return itemMap;
    }
}
