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

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.platform.knowledgebase.event.KnowledgeBaseDocumentEvent;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseDocumentNotFoundException;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
class KnowledgeBaseDocumentServiceImpl implements KnowledgeBaseDocumentService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;
    private final KnowledgeBaseFileStorage knowledgeBaseFileStorage;

    @SuppressFBWarnings("EI")
    KnowledgeBaseDocumentServiceImpl(
        ApplicationEventPublisher applicationEventPublisher,
        KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.knowledgeBaseDocumentRepository = knowledgeBaseDocumentRepository;
        this.knowledgeBaseFileStorage = knowledgeBaseFileStorage;
    }

    @Override
    public void delete(long id) {
        knowledgeBaseDocumentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseDocument getKnowledgeBaseDocument(long id) {
        return knowledgeBaseDocumentRepository.findById(id)
            .orElseThrow(() -> new KnowledgeBaseDocumentNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDocument> getKnowledgeBaseDocuments(long knowledgeBaseId) {
        return knowledgeBaseDocumentRepository.findAllByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentStatusUpdate getKnowledgeBaseDocumentStatus(long id) {
        KnowledgeBaseDocument document = getKnowledgeBaseDocument(id);

        return DocumentStatusUpdate.of(document.getId(), document.getStatus());
    }

    @Override
    public KnowledgeBaseDocument saveKnowledgeBaseDocument(KnowledgeBaseDocument knowledgeBaseDocument) {
        return knowledgeBaseDocumentRepository.save(knowledgeBaseDocument);
    }

    /**
     * Creates a new synced document tied to a Knowledge Base source. Persists {@code text} to platform file storage as
     * {@code <sourceRecordId>.md}, populates the five sync columns ({@code source_id}, {@code source_record_id},
     * {@code synced_payload_hash}, {@code last_seen_at}, {@code deleted_at = NULL}), saves the document, and publishes
     * {@link KnowledgeBaseDocumentEvent} to kick the chunker pipeline. Used by the DESTINATION cluster element writer
     * in Phase 13 Task 31.
     */
    @Override
    public KnowledgeBaseDocument createSyncedDocument(
        long kbId, long sourceId, String sourceRecordId, String name, String text, Map<String, ?> metadata,
        @Nullable Map<String, ?> metadataFieldsWhitelist, String payloadHash, Instant now) {

        String filename = sourceRecordId + ".md";
        FileEntry fileEntry = storeText(filename, text);

        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setKnowledgeBaseId(kbId);
        document.setSourceId(sourceId);
        document.setSourceRecordId(sourceRecordId);
        document.setName(name);
        document.setDocument(fileEntry);
        document.setSyncedPayloadHash(payloadHash);
        document.setLastSeenAt(now);
        document.setStatus(KnowledgeBaseDocument.STATUS_UPLOADED);
        document.setTagNames(applyWhitelistAndFlatten(metadata, metadataFieldsWhitelist));

        try {
            document = knowledgeBaseDocumentRepository.save(document);
        } catch (RuntimeException ex) {
            // Best-effort cleanup of the orphaned FileEntry if the DB save fails. The platform-KB file storage write
            // happens before the DB write because FileEntry persistence is external to the JDBC transaction; if the
            // save throws (e.g. unique constraint, connection drop), delete the orphan.
            safelyDeleteFileEntry(fileEntry);

            throw ex;
        }

        applicationEventPublisher.publishEvent(new KnowledgeBaseDocumentEvent(document.getId()));

        return document;
    }

    /**
     * Replaces the content of an existing synced document. Idempotent fast path: if the new {@code payloadHash} matches
     * the stored one and the row is not tombstoned, just bumps {@code last_seen_at} (no file rewrite, no chunker
     * re-run, no event publication). Otherwise: writes the new {@code text} to a new {@code FileEntry}, swaps the
     * document's pointer, eagerly deletes the old {@code FileEntry}, clears {@code deleted_at}, sets status to
     * {@code STATUS_UPLOADED} (which re-triggers the chunker pipeline), saves, and publishes
     * {@link KnowledgeBaseDocumentEvent}.
     *
     * <p>
     * Eager file-storage cleanup ordering: the new file is written first, then the DB row is updated; if the DB save
     * fails, the new file is cleaned up (preserving the old file). Once the DB save succeeds, the old file is deleted
     * as a best-effort cleanup — if that delete fails, the new file pointer is already committed and the old file
     * becomes an orphan (acceptable trade-off for MVP; the alternative is a transactional outbox pattern).
     * </p>
     */
    @Override
    @SuppressFBWarnings("UNSAFE_HASH_EQUALS")
    public KnowledgeBaseDocument replaceSyncedDocument(
        long documentId, String name, String text, Map<String, ?> metadata,
        @Nullable Map<String, ?> metadataFieldsWhitelist, String payloadHash, Instant now) {

        KnowledgeBaseDocument document = getKnowledgeBaseDocument(documentId);

        // Idempotent fast path: same hash + not tombstoned ⇒ just bump last_seen_at and return. Catches Spring Batch
        // retries and double-deliveries upstream of the writer's own fast path. The payloadHash is a content-change-
        // detection digest, not a secret — String.equals here is safe; suppress UNSAFE_HASH_EQUALS.
        if (payloadHash.equals(document.getSyncedPayloadHash()) && document.getDeletedAt() == null) {
            document.setLastSeenAt(now);

            return knowledgeBaseDocumentRepository.save(document);
        }

        String filename = document.getSourceRecordId() + ".md";
        FileEntry oldFileEntry = document.getDocument();
        FileEntry newFileEntry = storeText(filename, text);

        document.setName(name);
        document.setDocument(newFileEntry);
        document.setSyncedPayloadHash(payloadHash);
        document.setLastSeenAt(now);
        document.setDeletedAt(null);
        document.setStatus(KnowledgeBaseDocument.STATUS_UPLOADED);
        document.setTagNames(applyWhitelistAndFlatten(metadata, metadataFieldsWhitelist));

        KnowledgeBaseDocument saved;

        try {
            saved = knowledgeBaseDocumentRepository.save(document);
        } catch (RuntimeException ex) {
            // DB save failed — clean up the new orphan file and leave the old one (still referenced by the
            // un-modified DB row in the rolled-back transaction).
            safelyDeleteFileEntry(newFileEntry);

            throw ex;
        }

        // DB save succeeded — best-effort delete of the old file. Failures here leave an orphan but don't break
        // correctness; the new pointer is already persisted.
        safelyDeleteFileEntry(oldFileEntry);

        applicationEventPublisher.publishEvent(new KnowledgeBaseDocumentEvent(saved.getId()));

        return saved;
    }

    @Override
    public int tombstoneUnseen(long sourceId, Set<String> seenSourceRecordIds, Instant now) {
        return knowledgeBaseDocumentRepository.tombstoneUnseen(sourceId, seenSourceRecordIds, now);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KnowledgeBaseDocument> findSyncedDocument(long sourceId, String sourceRecordId) {
        return knowledgeBaseDocumentRepository.findBySourceIdAndSourceRecordId(sourceId, sourceRecordId);
    }

    @Override
    public KnowledgeBaseDocument bumpLastSeenAt(KnowledgeBaseDocument document, Instant now) {
        document.setLastSeenAt(now);

        return knowledgeBaseDocumentRepository.save(document);
    }

    private FileEntry storeText(String filename, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        return knowledgeBaseFileStorage.storeDocument(filename, new ByteArrayInputStream(bytes));
    }

    private void safelyDeleteFileEntry(@Nullable FileEntry fileEntry) {
        if (fileEntry == null) {
            return;
        }

        try {
            knowledgeBaseFileStorage.deleteDocument(fileEntry);
        } catch (RuntimeException ignored) {
            // Best-effort cleanup: swallow to avoid masking the original error or breaking the happy path.
        }
    }

    /**
     * Flattens {@code metadata} to a list of {@code key=value} tag strings, optionally narrowed to the field names
     * listed in {@code whitelist}. The whitelist payload is the {@code KnowledgeBaseSource.metadata_fields} JSONB — by
     * convention shaped as {@code {fields: ["fieldA", "fieldB"]}}, mirroring the CS {@code stored_fields} contract. A
     * null or empty {@code fields} list (or a null whitelist altogether) preserves MVP behavior: every metadata key
     * becomes a tag.
     *
     * <p>
     * Package-private so the branching logic (null whitelist, empty fields list, missing keys) can be unit-tested
     * directly without spinning up Testcontainers + file storage just to exercise a pure function.
     * </p>
     */
    static List<String> applyWhitelistAndFlatten(
        @Nullable Map<String, ?> metadata, @Nullable Map<String, ?> whitelist) {

        if (metadata == null || metadata.isEmpty()) {
            return List.of();
        }

        Set<String> allowed = extractAllowedFields(whitelist);

        Collection<? extends Map.Entry<String, ?>> entries = metadata.entrySet();
        List<String> tagNames = new ArrayList<>(allowed == null ? entries.size() : allowed.size());

        for (Map.Entry<String, ?> entry : entries) {
            if (allowed != null && !allowed.contains(entry.getKey())) {
                continue;
            }

            Object value = entry.getValue();

            tagNames.add(entry.getKey() + "=" + (value == null ? "" : value.toString()));
        }

        return tagNames;
    }

    /**
     * Returns the whitelist's {@code fields} list as a Set, or {@code null} when the whitelist is absent / malformed /
     * empty (meaning "include all" — MVP behavior). Null-vs-empty-Set matters here: an empty Set would drop every tag.
     */
    @Nullable
    static Set<String> extractAllowedFields(@Nullable Map<String, ?> whitelist) {
        if (whitelist == null || whitelist.isEmpty()) {
            return null;
        }

        Object fieldsValue = whitelist.get("fields");

        if (!(fieldsValue instanceof Collection<?> fields) || fields.isEmpty()) {
            return null;
        }

        Set<String> allowed = new HashSet<>(fields.size());

        for (Object field : fields) {
            if (field != null) {
                allowed.add(field.toString());
            }
        }

        return allowed.isEmpty() ? null : allowed;
    }
}
