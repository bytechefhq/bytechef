/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.knowledgebase.repository;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
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

@Repository
public interface KnowledgeBaseDocumentRepository
    extends PagingAndSortingRepository<KnowledgeBaseDocument, Long>, ListCrudRepository<KnowledgeBaseDocument, Long> {

    List<KnowledgeBaseDocument> findAllByKnowledgeBaseId(Long knowledgeBaseId);

    List<KnowledgeBaseDocument> findAllBySourceIdAndDeletedAtIsNotNull(Long sourceId);

    Optional<KnowledgeBaseDocument> findBySourceIdAndSourceRecordId(Long sourceId, String sourceRecordId);

    /**
     * Soft-deletes (tombstones) every synced document tied to the given source whose {@code source_record_id} is not
     * present in the {@code seenSourceRecordIds} collection. Manual uploads (where {@code source_id IS NULL}) are
     * untouched. Returns the count of rows tombstoned.
     *
     * <p>
     * {@code @Modifying} is required for Spring Data JDBC string {@code @Query} UPDATE/DELETE — without it, JDBC tries
     * {@code executeQuery()} on the UPDATE and fails with a misleading {@code DataIntegrityViolationException}.
     * </p>
     */
    @Modifying
    @Query("""
        UPDATE knowledge_base_document
        SET deleted_at = :deletedAt, last_modified_date = :deletedAt
        WHERE source_id = :sourceId
          AND source_record_id NOT IN (:seenIds)
          AND deleted_at IS NULL
        """)
    int tombstoneUnseen(
        @Param("sourceId") Long sourceId,
        @Param("seenIds") Collection<String> seenIds,
        @Param("deletedAt") Instant deletedAt);
}
