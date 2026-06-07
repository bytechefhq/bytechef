/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * CRUD service for {@link ContextStoreRecord}. Records are the high-throughput artefact of a sync run — they are
 * upserted on every read and tombstoned (soft-deleted) when a sync sees the source no longer expose them.
 *
 * <p>
 * With Source absorbing the former Entity layer, every record-routing call keys off {@code sourceId} alone: a source is
 * 1:1 with its record shape.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreRecordService {

    ContextStoreRecord save(ContextStoreRecord record);

    Optional<ContextStoreRecord> fetchByKey(Long sourceId, String sourceRecordId);

    /**
     * Soft-deletes (sets {@code deletedAt}) every record for {@code sourceId} whose {@code sourceRecordId} is not in
     * the {@code seenIds} collection and whose {@code deletedAt} is currently null.
     *
     * <p>
     * If {@code seenIds} is null or empty, this method short-circuits and returns 0 rather than emitting a
     * {@code NOT IN ()} clause (Postgres rejects empty {@code NOT IN}). A future refinement can add a
     * "tombstone-everything" code path; until then a sync run that yields zero records is a no-op rather than a
     * full-source wipe.
     * </p>
     */
    int tombstoneUnseen(Long sourceId, Collection<String> seenIds, Instant deletedAt);

    /**
     * Returns every record matching the given ids. Used by the semantic embedding listener to batch-load payloads for a
     * chunk of seen records before (re-)embedding. Records that don't exist (for example because they were tombstoned +
     * hard-deleted between the writer pass and the listener pass) are silently omitted.
     */
    List<ContextStoreRecord> findAllById(Collection<Long> ids);

    /**
     * Returns the {@code id} column of every soft-deleted ({@code deleted_at IS NOT NULL}) record for the given source.
     * Used by the semantic listener's FULL_REPLACE tombstone sweep to identify vector-store rows whose backing record
     * was just tombstoned.
     */
    List<Long> findTombstonedRecordIdsBySourceId(Long sourceId);

    void delete(Long id);
}
