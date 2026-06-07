/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.dto;

import com.bytechef.ee.platform.contextstore.domain.TombstoneStrategy;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Input DTO for creating a {@link com.bytechef.ee.platform.contextstore.domain.ContextStoreSource}. Consumed by
 * {@link com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade#create(Long, CreateContextStoreSourceInput)}
 * which in turn auto-generates and persists the source-bound workflow.
 *
 * <p>
 * Source absorbed the former Entity layer — the record-shape fields ({@code entityName}, {@code idField},
 * {@code indexedFields}, {@code semanticIndexFields}, {@code storedFields}, {@code parameters}, {@code description})
 * live inline on this DTO and on the persisted source row.
 * </p>
 *
 * <p>
 * {@code sourceClusterElementName} is optional: when omitted, the facade auto-picks the first {@code ItemReader}
 * cluster element on the source component. An explicit name lets the caller pin a specific reader when the component
 * defines more than one.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record CreateContextStoreSourceInput(
    Long contextStoreId,
    String name,
    String entityName,
    @Nullable String description,
    String sourceComponentName,
    int sourceComponentVersion,
    @Nullable String sourceClusterElementName,
    @Nullable Long connectionId,
    String cadence,
    @Nullable String fullReplaceCadence,
    @Nullable TombstoneStrategy tombstoneStrategy,
    String idField,
    @Nullable Map<String, ?> storedFields,
    Map<String, ?> indexedFields,
    @Nullable Map<String, ?> semanticIndexFields,
    @Nullable Map<String, ?> parameters) {

    public CreateContextStoreSourceInput {
        Objects.requireNonNull(contextStoreId, "contextStoreId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(entityName, "entityName");
        Objects.requireNonNull(sourceComponentName, "sourceComponentName");
        Objects.requireNonNull(cadence, "cadence");
        Objects.requireNonNull(idField, "idField");
        Objects.requireNonNull(indexedFields, "indexedFields");
    }
}
