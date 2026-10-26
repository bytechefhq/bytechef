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
