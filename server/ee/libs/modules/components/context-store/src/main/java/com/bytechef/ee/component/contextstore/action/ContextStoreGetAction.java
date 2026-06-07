/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.contextstore.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Workflow-step Action that fetches a single Context Store record by its {@code (sourceId, sourceRecordId)} natural
 * key. Returns an empty map when no row matches so callers can keep the workflow flowing without a null check.
 *
 * <p>
 * {@code workspaceId} is resolved internally from {@link ContextStoreSourceService#get(Long)} on the provided
 * {@code sourceId}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ContextStoreGetAction {

    public static final String SOURCE_ID = "sourceId";
    public static final String SOURCE_RECORD_ID = "sourceRecordId";

    private final ContextStoreQueryService contextStoreQueryService;
    private final ContextStoreSourceService contextStoreSourceService;

    @SuppressFBWarnings("EI")
    public static ModifiableActionDefinition of(
        ContextStoreQueryService contextStoreQueryService, ContextStoreSourceService contextStoreSourceService) {

        return new ContextStoreGetAction(contextStoreQueryService, contextStoreSourceService).build();
    }

    private ContextStoreGetAction(
        ContextStoreQueryService contextStoreQueryService, ContextStoreSourceService contextStoreSourceService) {

        this.contextStoreQueryService = contextStoreQueryService;
        this.contextStoreSourceService = contextStoreSourceService;
    }

    private ModifiableActionDefinition build() {
        return action("get")
            .title("Get Context Store Record")
            .description("Fetch a single record from the Context Store by its source record ID.")
            .properties(
                integer(SOURCE_ID)
                    .label("Context Source ID")
                    .description("ID of the ContextStoreSource that owns the record.")
                    .required(true),
                string(SOURCE_RECORD_ID)
                    .label("Source Record ID")
                    .description("Source-side identifier of the record to fetch.")
                    .required(true))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Long sourceId = inputParameters.getRequiredLong(SOURCE_ID);
        String sourceRecordId = inputParameters.getRequiredString(SOURCE_RECORD_ID);

        // Existence check: surfaces a clearer error than the optional below if the source row is missing.
        contextStoreSourceService.get(sourceId);

        Optional<ContextStoreRecord> recordOptional = contextStoreQueryService.get(sourceId, sourceRecordId);

        if (recordOptional.isEmpty()) {
            return Map.of();
        }

        ContextStoreRecord record = recordOptional.get();

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", record.getId());
        result.put("sourceRecordId", record.getSourceRecordId());
        result.put("payload", record.getPayload());
        result.put("lastSeenAt", record.getLastSeenAt());
        result.put("deletedAt", record.getDeletedAt());

        return result;
    }
}
