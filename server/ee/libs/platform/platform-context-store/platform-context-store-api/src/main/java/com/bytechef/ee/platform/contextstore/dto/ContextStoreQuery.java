/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record ContextStoreQuery(
    Long sourceId,
    List<ContextStoreQueryFilter> filters,
    List<ContextStoreQuerySort> sort,
    int limit,
    @Nullable String cursor,
    boolean includeDeleted,
    @Nullable List<String> fields) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    public ContextStoreQuery {
        if (limit <= 0 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be in (0, " + MAX_LIMIT + "]");
        }

        filters = filters == null ? List.of() : List.copyOf(filters);
        sort = sort == null ? List.of() : List.copyOf(sort);
        fields = fields == null ? null : List.copyOf(fields);
    }
}
