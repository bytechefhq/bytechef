/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.repository;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecordIndex;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for {@link ContextStoreRecordIndex}. Sidecar index rows are rebuilt on every record
 * upsert via delete-then-insert; the {@link #deleteAllByRecordId} method drives that wipe step.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Repository
public interface ContextStoreRecordIndexRepository extends ListCrudRepository<ContextStoreRecordIndex, Long> {

    List<ContextStoreRecordIndex> findAllByRecordId(Long recordId);

    @Modifying
    @Query("DELETE FROM context_store_record_index WHERE record_id = :recordId")
    void deleteAllByRecordId(@Param("recordId") Long recordId);

    @Modifying
    @Query("""
        DELETE FROM context_store_record_index
        WHERE record_id IN (
            SELECT id FROM context_store_record WHERE source_id = :sourceId AND deleted_at IS NOT NULL)
        """)
    int deleteAllForTombstonedRecords(@Param("sourceId") Long sourceId);
}
