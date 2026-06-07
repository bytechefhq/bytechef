/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.repository;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * SPI for the Context Store record repository. Phase 16 extracted this interface from what used to be a single Spring
 * Data JDBC repository so that an alternative backend (ClickHouse) can plug in without leaking Postgres-specific types
 * into the service layer.
 *
 * <p>
 * Surface is deliberately narrow — the wider inherited surface of Spring Data JDBC (paging, sorting, count, deleteAll,
 * etc.) is not used externally. With Source absorbing the former Entity layer, every record-routing call now keys off
 * {@code sourceId} alone: source is 1:1 with its record shape, so the entity-name parameter is redundant.
 * </p>
 *
 * <p>
 * Query-path methods used by {@code ContextStoreQueryServiceImpl.search()} stay Postgres-only at the implementation
 * layer — that surface (filtering against {@code context_store_record_index}, semantic search, etc.) reaches into
 * Postgres-specific column shapes and is not part of this abstraction.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreRecordRepository {

    /**
     * Persists a record. Insert or update depending on whether {@link ContextStoreRecord#getId()} is set — Postgres
     * impl delegates to Spring Data JDBC's save semantics. ClickHouse impl always inserts (the RMT engine deduplicates
     * on the {@code _id} merge key in the background).
     *
     * <p>
     * <b>Backend asymmetry on the returned id:</b> the Postgres adapter populates {@code getId()} from the
     * autoincrement sequence on insert and returns the saved aggregate. The ClickHouse impl has no autoincrement column
     * and returns the input record unchanged — {@code getId()} stays {@code null} on the way out for newly inserted
     * ClickHouse-backed records. Callers that need a stable {@code Long} handle for ClickHouse records derive one via
     * {@code findTombstonedRecordIdsBySourceId} (which returns the {@code cityHash64(_id)} cast) or address records by
     * their natural {@code (sourceId, sourceRecordId)} key. None of today's callers depend on the post-save {@code id}
     * for ClickHouse-backed sources — the chunker pipeline and admin paths stay Postgres-only.
     * </p>
     */
    ContextStoreRecord save(ContextStoreRecord contextStoreRecord);

    /**
     * Composite-key lookup hit on every sync write. Returns the existing row when the (sourceId, sourceRecordId) pair
     * has been synced before; empty when this is a fresh record.
     */
    Optional<ContextStoreRecord> findBySourceIdAndSourceRecordId(Long sourceId, String sourceRecordId);

    /**
     * Soft-deletes rows for the given source whose {@code sourceRecordId} is not in {@code seenIds} and which are not
     * already tombstoned. Called by {@code ContextStoreSyncJobListener} at the end of every FULL_REPLACE run to catch
     * upstream deletions. Returns the number of rows tombstoned.
     */
    int tombstoneUnseen(Long sourceId, Collection<String> seenIds, Instant deletedAt);

    /**
     * Bulk fetch by ids — used by the chunker pipeline.
     */
    List<ContextStoreRecord> findAllById(Iterable<Long> ids);

    /**
     * Returns the ids of currently-tombstoned records for the given source. Used by the chunker pipeline to find rows
     * whose downstream indices need cleaning.
     */
    List<Long> findTombstonedRecordIdsBySourceId(Long sourceId);

    /**
     * Hard-delete by id. Administrative — not used by the sync hot path.
     */
    void deleteById(Long id);
}
