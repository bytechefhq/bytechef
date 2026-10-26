/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface ClickHouseTableProvisioner {

    /**
     * Provisions the per-source ClickHouse table for the given (workspaceId, contextStoreId, source) tuple. Returns the
     * sanitised table name actually used; the caller persists this on
     * {@link ContextStoreSource#setClickhouseTableName(String)}.
     *
     * <p>
     * The {@code contextStoreId} segment ensures DEV/STAGING/PROD stores of the same logical source get physically
     * separate ClickHouse tables — environment isolation flows transitively through the parent ContextStore.
     * </p>
     *
     * <p>
     * Implementations must be idempotent on table existence ({@code CREATE TABLE IF NOT EXISTS} or pre-check) so a
     * partial source-creation that fails mid-flight can be retried without manual cleanup of the ClickHouse side.
     * </p>
     */
    String provisionTable(long workspaceId, long contextStoreId, ContextStoreSource source);
}
