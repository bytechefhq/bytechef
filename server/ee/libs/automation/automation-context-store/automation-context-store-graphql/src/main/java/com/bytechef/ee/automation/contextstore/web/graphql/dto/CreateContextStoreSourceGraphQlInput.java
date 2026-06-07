/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.web.graphql.dto;

import com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput;
import com.bytechef.ee.platform.contextstore.domain.TombstoneStrategy;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * GraphQL input shape for creating a Context Store source. Mirrors the schema's {@code CreateContextStoreSourceInput};
 * a controller adapter translates this to the facade-level
 * {@link com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput}.
 *
 * <p>
 * The translation is needed because the facade DTO uses nullable {@link Long} for {@code connectionId} and primitive
 * types where GraphQL serializes maps via the {@code Map} scalar.
 * </p>
 *
 * <p>
 * {@code workspaceId} is read off the GraphQL input by the controller and passed as the first argument to
 * {@link com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacade#create}; it is not part of
 * the facade-level DTO.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record CreateContextStoreSourceGraphQlInput(
    Long workspaceId,
    @Nullable Long environmentId,
    Long contextStoreId,
    String name,
    String entityName,
    @Nullable String description,
    String idField,
    @Nullable Map<String, ?> storedFields,
    Map<String, ?> indexedFields,
    @Nullable Map<String, ?> semanticIndexFields,
    @Nullable Map<String, ?> parameters,
    String sourceComponentName,
    int sourceComponentVersion,
    @Nullable String sourceClusterElementName,
    @Nullable Long connectionId,
    String cadence,
    @Nullable String fullReplaceCadence,
    @Nullable TombstoneStrategy tombstoneStrategy) {

    public CreateContextStoreSourceInput toFacadeInput() {
        return new CreateContextStoreSourceInput(
            contextStoreId, name, entityName, description, sourceComponentName, sourceComponentVersion,
            sourceClusterElementName, connectionId, cadence, fullReplaceCadence, tombstoneStrategy, idField,
            storedFields, indexedFields, semanticIndexFields, parameters);
    }
}
