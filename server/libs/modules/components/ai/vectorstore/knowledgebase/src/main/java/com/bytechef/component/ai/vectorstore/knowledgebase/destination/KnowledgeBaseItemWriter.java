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

package com.bytechef.component.ai.vectorstore.knowledgebase.destination;

import com.bytechef.commons.util.PayloadHashUtil;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.component.definition.datastream.ItemWriter;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * DESTINATION cluster element implementation for the {@code knowledgeBase} component. Upserts source records into the
 * target Knowledge Base as documents and tracks {@code seenRecordIds} per job for the post-step tombstone sweep
 * performed by the {@code KnowledgeBaseSourceSyncJobListener} (Phase 13 Task 32).
 *
 * <p>
 * Lifecycle: {@link #open} loads the {@link KnowledgeBaseSource} (failing fast if it was deleted before the job
 * started), validates the {@code mode} parameter, and initializes per-job state. {@link #write} hashes each record's
 * payload via the shared {@link PayloadHashUtil}, looks up the existing document by {@code (sourceId, sourceRecordId)},
 * and routes to one of three paths: unchanged-record fast path (bump {@code lastSeenAt} only — no chunker re-run),
 * changed-record replace, or new-record create. {@link #update} flushes {@code seenRecordIds} and {@code mode} to the
 * {@link ExecutionContext} so the listener can decide whether to run the tombstone sweep. {@link #close} is a no-op.
 *
 * <p>
 * Instances are created fresh per batch job by a {@code Supplier} captured in the cluster element definition (see
 * {@link com.bytechef.component.ai.vectorstore.knowledgebase.KnowledgeBaseComponentHandler}). Each instance carries
 * per-job mutable state ({@code sourceId}, {@code mode}, {@code kbId}, {@code seenRecordIds}) and is therefore not safe
 * to share across concurrent jobs.
 *
 * @author Ivica Cardic
 */
public class KnowledgeBaseItemWriter implements ItemWriter {

    public static final String SOURCE_ID = "sourceId";
    public static final String MODE = "mode";

    public static final String MODE_FULL_REPLACE = "FULL_REPLACE";
    public static final String MODE_PARTIAL = "PARTIAL";

    public static final String SEEN_RECORD_IDS_KEY = "knowledgeBaseSource.seenRecordIds";
    public static final String MODE_KEY = "knowledgeBaseSource.mode";

    private final KnowledgeBaseSourceService knowledgeBaseSourceService;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    private Long sourceId;
    private String mode;
    private Long kbId;
    private Set<String> seenRecordIds;

    @Nullable
    private Map<String, ?> metadataFieldsWhitelist;

    @SuppressFBWarnings("EI2")
    public KnowledgeBaseItemWriter(
        KnowledgeBaseSourceService knowledgeBaseSourceService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService) {

        this.knowledgeBaseSourceService = knowledgeBaseSourceService;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
    }

    @Override
    public void open(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        this.sourceId = inputParameters.getRequiredLong(SOURCE_ID);
        this.mode = inputParameters.getString(MODE, MODE_FULL_REPLACE);

        if (!MODE_FULL_REPLACE.equals(mode) && !MODE_PARTIAL.equals(mode)) {
            throw new IllegalArgumentException(
                "Unknown sync mode '" + mode + "': expected '" + MODE_FULL_REPLACE + "' or '" + MODE_PARTIAL + "'");
        }

        KnowledgeBaseSource source = knowledgeBaseSourceService.fetch(sourceId)
            .orElseThrow(() -> new IllegalStateException("KnowledgeBaseSource " + sourceId + " not found"));

        this.kbId = source.getKnowledgeBaseId();
        this.seenRecordIds = new HashSet<>();
        // Captured once at open() rather than per-record so a mid-job edit of the source row does not split the run's
        // tags between two whitelist policies.
        this.metadataFieldsWhitelist = source.getMetadataFields();
    }

    @Override
    @SuppressFBWarnings(
        value = "UNSAFE_HASH_EQUALS",
        justification = "Hash comparison is used for change detection only, not for security-sensitive verification.")
    public void write(List<? extends Map<String, Object>> items) {
        Instant now = Instant.now();

        for (Map<String, Object> sourceRecord : items) {
            Object idValue = sourceRecord.get("id");

            if (idValue == null) {
                throw new IllegalArgumentException(
                    "Record missing required 'id' field: " + sourceRecord);
            }

            String sourceRecordId = String.valueOf(idValue);

            seenRecordIds.add(sourceRecordId);

            String payloadHash = PayloadHashUtil.hash(sourceRecord);

            Optional<KnowledgeBaseDocument> existingOptional = knowledgeBaseDocumentService.findSyncedDocument(
                sourceId, sourceRecordId);

            String name = extractName(sourceRecord, sourceRecordId);
            String text = extractText(sourceRecord);

            if (existingOptional.isPresent()
                && payloadHash.equals(existingOptional.get()
                    .getSyncedPayloadHash())
                && existingOptional.get()
                    .getDeletedAt() == null) {

                // Unchanged-record fast path: bump heartbeat only -- no file rewrite, no chunker re-run, no
                // KnowledgeBaseDocumentEvent. Upstream of the service-side fast path in replaceSyncedDocument so we
                // also avoid that method's findById round-trip on every unchanged record.
                knowledgeBaseDocumentService.bumpLastSeenAt(existingOptional.get(), now);
            } else if (existingOptional.isPresent()) {
                // Changed-record path (or reappeared after tombstone). replaceSyncedDocument also clears deletedAt
                // and resets status to STATUS_UPLOADED to re-trigger the chunker pipeline.
                knowledgeBaseDocumentService.replaceSyncedDocument(
                    existingOptional.get()
                        .getId(),
                    name, text, sourceRecord, metadataFieldsWhitelist, payloadHash, now);
            } else {
                knowledgeBaseDocumentService.createSyncedDocument(
                    kbId, sourceId, sourceRecordId, name, text, sourceRecord, metadataFieldsWhitelist, payloadHash,
                    now);
            }
        }
    }

    @Override
    public void update(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        executionContext.put(SEEN_RECORD_IDS_KEY, new ArrayList<>(seenRecordIds));
        executionContext.put(MODE_KEY, mode);
    }

    @Override
    public void close() {
        // No-op; tombstone sweep + status flip are handled by the KnowledgeBaseSourceSyncJobListener after the job
        // completes (Task 32), gated by mode.
    }

    private static String extractName(Map<String, Object> sourceRecord, String sourceRecordId) {
        Object nameValue = sourceRecord.get("name");

        if (nameValue == null) {
            return sourceRecordId;
        }

        return String.valueOf(nameValue);
    }

    private static String extractText(Map<String, Object> sourceRecord) {
        Object textValue = sourceRecord.get("text");

        if (textValue == null) {
            return "";
        }

        return String.valueOf(textValue);
    }
}
