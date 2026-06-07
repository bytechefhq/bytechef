/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import java.util.List;

/**
 * SPI for applying {@code indexedFields} edits to an existing per-source ClickHouse table. The Postgres-only deployment
 * doesn't register an implementation; the source-service injects this via {@code ObjectProvider} and skips it when the
 * bean is absent.
 *
 * <p>
 * Sister SPI to {@link ClickHouseTableProvisioner} (which handles table creation at source-creation time). This one
 * handles in-place schema mutation when an existing source's {@code indexedFields} map changes.
 * </p>
 *
 * <p>
 * Implementations must be idempotent on no-op (empty change list) and idempotent on retry (e.g. partial failure
 * mid-flight needs to succeed on re-application without manual cleanup). The ClickHouse impl uses {@code IF EXISTS} /
 * {@code IF NOT EXISTS} clauses to achieve this.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ClickHouseTableMigrator {

    /**
     * Applies the given list of changes to the source's ClickHouse table. Empty list is a no-op that returns
     * {@link MigrationResult#NONE}. Throws if the source's {@code clickhouseTableName} is null — the deployment was
     * either never opted in to ClickHouse, in which case the caller shouldn't reach this method, or the source-creation
     * flow had a bug and didn't populate the column.
     */
    MigrationResult applyChanges(ContextStoreSource source, List<IndexedFieldChange> changes);
}
