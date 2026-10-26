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
