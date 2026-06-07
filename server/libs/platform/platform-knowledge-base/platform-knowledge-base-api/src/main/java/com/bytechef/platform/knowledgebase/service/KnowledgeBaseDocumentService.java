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

package com.bytechef.platform.knowledgebase.service;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public interface KnowledgeBaseDocumentService {

    /**
     * Deletes the knowledge base document identified by the specified ID.
     *
     * @param id the unique identifier of the knowledge base document to be deleted
     */
    void delete(long id);

    /**
     * Retrieves the knowledge base document for the specified unique identifier.
     *
     * @param id the unique identifier of the knowledge base document to retrieve
     * @return the {@code KnowledgeBaseDocument} associated with the given ID, or {@code null} if no such document
     *         exists
     */
    KnowledgeBaseDocument getKnowledgeBaseDocument(long id);

    /**
     * Retrieves a list of knowledge base documents associated with the specified knowledge base ID.
     *
     * @param knowledgeBaseId the unique identifier of the knowledge base
     * @return a list of {@code KnowledgeBaseDocument} objects associated with the provided knowledge base ID
     */
    List<KnowledgeBaseDocument> getKnowledgeBaseDocuments(long knowledgeBaseId);

    /**
     * Retrieves the status of the knowledge base document identified by the specified ID.
     *
     * @param id the unique identifier of the knowledge base document whose status is to be retrieved
     * @return a {@code DocumentStatusUpdate} object containing the status information of the specified knowledge base
     *         document
     */
    DocumentStatusUpdate getKnowledgeBaseDocumentStatus(long id);

    /**
     * Saves the provided {@code KnowledgeBaseDocument} to the underlying data store. If the document already exists
     * (based on its unique identifier), it will be updated; otherwise, a new document will be created.
     *
     * @param knowledgeBaseDocument the {@code KnowledgeBaseDocument} to be saved or updated
     * @return the saved or updated {@code KnowledgeBaseDocument} instance, including any modifications made during the
     *         save operation (e.g., generated IDs, timestamps)
     */
    KnowledgeBaseDocument saveKnowledgeBaseDocument(KnowledgeBaseDocument knowledgeBaseDocument);

    /**
     * Creates a new synced document tied to a Knowledge Base source. Persists {@code text} to platform file storage as
     * {@code <sourceRecordId>.md}, populates the five sync columns ({@code source_id}, {@code source_record_id},
     * {@code synced_payload_hash}, {@code last_seen_at}, {@code deleted_at = NULL}), saves the document, and publishes
     * {@code KnowledgeBaseDocumentEvent} to kick the chunker pipeline. Used by the DESTINATION cluster element writer
     * in Phase 13 Task 31.
     *
     * <p>
     * {@code metadataFieldsWhitelist} — when non-null, narrows {@code metadata} to the listed field names before
     * flattening to {@code key=value} tags. Mirrors {@code KnowledgeBaseSource.metadataFields}. Null preserves MVP
     * behavior: every metadata key becomes a tag.
     * </p>
     */
    KnowledgeBaseDocument createSyncedDocument(
        long kbId, long sourceId, String sourceRecordId, String name, String text, Map<String, ?> metadata,
        @Nullable Map<String, ?> metadataFieldsWhitelist, String payloadHash, Instant now);

    /**
     * Replaces the content of an existing synced document. Idempotent fast path: if the new {@code payloadHash} matches
     * the stored one and the row is not tombstoned, just bumps {@code last_seen_at} (no file rewrite, no chunker
     * re-run, no event publication). Otherwise: writes the new {@code text} to a new {@code FileEntry}, swaps the
     * document's pointer, eagerly deletes the old {@code FileEntry}, clears {@code deleted_at}, sets status to
     * {@code STATUS_UPLOADED} (which re-triggers the chunker pipeline), saves, and publishes
     * {@code KnowledgeBaseDocumentEvent}.
     *
     * <p>
     * {@code metadataFieldsWhitelist} — same semantics as on {@link #createSyncedDocument}.
     * </p>
     */
    KnowledgeBaseDocument replaceSyncedDocument(
        long documentId, String name, String text, Map<String, ?> metadata,
        @Nullable Map<String, ?> metadataFieldsWhitelist, String payloadHash, Instant now);

    /**
     * Soft-deletes synced documents whose {@code source_record_id} is not in {@code seenSourceRecordIds} for the given
     * source by setting {@code deleted_at = now}. Manual uploads ({@code source_id IS NULL}) are unaffected. Returns
     * the number of rows tombstoned. Called by the {@code KnowledgeBaseSourceSyncJobListener} after each FULL_REPLACE
     * sync run completes.
     */
    int tombstoneUnseen(long sourceId, Set<String> seenSourceRecordIds, Instant now);

    /**
     * Looks up an existing synced document by its {@code (source_id, source_record_id)} sync key. Used by the
     * DESTINATION cluster element writer (Phase 13 Task 31) to decide between create / unchanged-fast-path / replace
     * paths per record.
     */
    Optional<KnowledgeBaseDocument> findSyncedDocument(long sourceId, String sourceRecordId);

    /**
     * Bumps {@code last_seen_at} on the given synced document and saves it. Used by the DESTINATION cluster element
     * writer's unchanged-record fast path (Phase 13 Task 31): when a record's payload hash matches the stored hash and
     * the row is not tombstoned, neither the file nor the chunker pipeline needs to be re-run — only the heartbeat
     * needs to be refreshed so the post-job tombstone sweep doesn't reap the row. Avoids the extra
     * {@code findById}-then-save round-trip that {@link #replaceSyncedDocument} would incur on its own service-side
     * fast path.
     */
    KnowledgeBaseDocument bumpLastSeenAt(KnowledgeBaseDocument document, Instant now);
}
