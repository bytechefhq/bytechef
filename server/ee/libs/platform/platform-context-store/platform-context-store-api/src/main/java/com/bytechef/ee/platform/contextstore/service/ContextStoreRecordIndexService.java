/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecordIndex;

/**
 * CRUD service for {@link ContextStoreRecordIndex}. Index rows are sidecar entries rebuilt on every record upsert via
 * delete-then-insert; consumers (e.g. the sync writer) call {@link #deleteAllByRecordId} before re-inserting fresh rows
 * via {@link #save}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreRecordIndexService {

    ContextStoreRecordIndex save(ContextStoreRecordIndex index);

    void deleteAllByRecordId(Long recordId);

    /**
     * Deletes the index rows of every tombstoned ({@code deleted_at IS NOT NULL}) record of the given source. Called by
     * the sync listener after the FULL_REPLACE tombstone sweep so a deleted upstream record does not leak its sidecar
     * index rows; running it on every sweep also self-heals rows left behind by earlier runs. Returns the number of
     * index rows deleted.
     */
    int deleteAllForTombstonedRecords(Long sourceId);
}
