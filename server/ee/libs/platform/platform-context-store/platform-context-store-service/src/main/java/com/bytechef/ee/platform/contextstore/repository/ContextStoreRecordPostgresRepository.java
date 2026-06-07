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
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for {@link ContextStoreRecord}, Postgres-backed. The {@link ContextStoreRecordRepository}
 * SPI in {@code platform-context-store-api} is the public abstraction over this interface; the
 * {@link ContextStoreRecordPostgresRepositoryAdapter} bridges Spring Data's surface into the SPI. A parallel
 * ClickHouse-backed implementation of the SPI lives in {@code platform-context-store-clickhouse-service}.
 *
 * <p>
 * Records are inserted/updated on every sync run; the {@link #tombstoneUnseen} method soft-deletes rows that were not
 * encountered during the current run window.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Repository
public interface ContextStoreRecordPostgresRepository
    extends PagingAndSortingRepository<ContextStoreRecord, Long>, ListCrudRepository<ContextStoreRecord, Long> {

    Optional<ContextStoreRecord> findBySourceIdAndSourceRecordId(Long sourceId, String sourceRecordId);

    @Modifying
    @Query("""
        UPDATE context_store_record
        SET deleted_at = :deletedAt, last_modified_date = :deletedAt
        WHERE source_id = :sourceId
          AND source_record_id NOT IN (:seenIds)
          AND deleted_at IS NULL
        """)
    int tombstoneUnseen(
        @Param("sourceId") Long sourceId,
        @Param("seenIds") Collection<String> seenIds,
        @Param("deletedAt") Instant deletedAt);

    @Query("""
        SELECT id FROM context_store_record
        WHERE source_id = :sourceId
          AND deleted_at IS NOT NULL
        """)
    List<Long> findTombstonedRecordIdsBySourceId(@Param("sourceId") Long sourceId);
}
