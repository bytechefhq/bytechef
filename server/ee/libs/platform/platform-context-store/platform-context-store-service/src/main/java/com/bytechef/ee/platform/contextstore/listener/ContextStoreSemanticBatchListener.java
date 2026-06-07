/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.listener;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreRecordService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreVectorStoreMetadataService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Best-effort semantic-embedding pass that runs after every Context Store sync job. Disappears from the application
 * context when no {@link EmbeddingModel} is registered (CE without embeddings) or when the Context Store add-on is
 * disabled. The backing {@code contextStorePgVectorStore} {@link VectorStore} is supplied by
 * {@link com.bytechef.ee.platform.contextstore.config.ContextStorePgVectorConfiguration} (single-tenant) or its
 * multi-tenant twin {@code MultiTenantContextStorePgVectorConfiguration}.
 *
 * <p>
 * On a {@link BatchStatus#COMPLETED} {@code contextStore.writeToReplica} job, the listener:
 * <ol>
 * <li>Looks up the {@link ContextStoreSource} for the sourceId. If it has no {@code semanticIndexFields} configured the
 * listener returns early — semantic indexing is opt-in per source.</li>
 * <li>Aggregates the persisted {@code context_store_record.id} values populated by the writer under
 * {@link ContextStoreExecutionContextKeys#SEMANTIC_RECORD_IDS_KEY}.</li>
 * <li>For each chunk of seen ids: loads the records, queries the vector store for the existing {@code payloadHash}, and
 * skips records whose hash is unchanged (the cost-saver). For changed records it deletes the existing vector-store rows
 * and re-adds {@link Document} instances built from the configured {@code semanticIndexFields}.</li>
 * <li>On FULL_REPLACE mode only: sweeps every tombstoned record's vector-store rows so the embedding store stays in
 * sync with the structured replica.</li>
 * </ol>
 *
 * <p>
 * The whole pass is wrapped in a single try/catch so a failure here cannot mark the structured sync as failed — the
 * structured replica has already been written. A subsequent successful sync re-runs the embedding pass for the same
 * records (the hash skip prevents wasted work when the underlying payload didn't change).
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnBean({
    EmbeddingModel.class, VectorStore.class
})
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreSemanticBatchListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ContextStoreSemanticBatchListener.class);

    private static final int BATCH_CHUNK_SIZE = 200;

    private static final String METADATA_RECORD_ID = "recordId";
    private static final String METADATA_SOURCE_ID = "sourceId";
    private static final String METADATA_PAYLOAD_HASH = "payloadHash";

    private final VectorStore vectorStore;
    private final ContextStoreVectorStoreMetadataService metadataService;
    private final ContextStoreSourceService contextStoreSourceService;
    private final ContextStoreRecordService contextStoreRecordService;

    @SuppressFBWarnings("EI2")
    public ContextStoreSemanticBatchListener(
        @Qualifier("contextStorePgVectorStore") VectorStore vectorStore,
        ContextStoreVectorStoreMetadataService metadataService,
        ContextStoreSourceService contextStoreSourceService,
        ContextStoreRecordService contextStoreRecordService) {

        this.vectorStore = vectorStore;
        this.metadataService = metadataService;
        this.contextStoreSourceService = contextStoreSourceService;
        this.contextStoreRecordService = contextStoreRecordService;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        ContextStoreSyncJobParameters parameters = ContextStoreSyncJobParameters.from(jobExecution);

        if (parameters == null) {
            return;
        }

        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            // Structured sync failed; the listener has nothing reliable to embed. The next successful sync will
            // re-run the embedding pass.
            return;
        }

        Long sourceId = parameters.sourceId();
        String mode = parameters.mode();

        try {
            Optional<ContextStoreSource> sourceOptional = contextStoreSourceService.fetch(sourceId);

            if (sourceOptional.isEmpty()) {
                return;
            }

            ContextStoreSource source = sourceOptional.get();
            List<String> semanticFields = extractSemanticFields(source.getSemanticIndexFields());

            if (semanticFields.isEmpty()) {
                return;
            }

            List<Long> seenRecordIds = aggregateSemanticRecordIds(jobExecution);

            embedSeenRecords(sourceId, semanticFields, seenRecordIds);

            if (ContextStoreSyncJobParameters.MODE_FULL_REPLACE.equals(mode)) {
                sweepTombstonedVectorStoreRows(sourceId);
            }
        } catch (RuntimeException exception) {
            log.warn(
                "Context Store semantic embedding pass failed: source={} — structured sync remains intact",
                sourceId, exception);
        }
    }

    @SuppressFBWarnings(
        value = "UNSAFE_HASH_EQUALS",
        justification = "Hash comparison is used for change detection only, not for security-sensitive verification.")
    private void embedSeenRecords(Long sourceId, List<String> semanticFields, List<Long> seenRecordIds) {
        if (seenRecordIds.isEmpty()) {
            return;
        }

        for (int offset = 0; offset < seenRecordIds.size(); offset += BATCH_CHUNK_SIZE) {
            int end = Math.min(offset + BATCH_CHUNK_SIZE, seenRecordIds.size());

            List<Long> chunk = seenRecordIds.subList(offset, end);
            List<ContextStoreRecord> records = contextStoreRecordService.findAllById(chunk);

            if (records.isEmpty()) {
                continue;
            }

            Map<Long, String> existingHashes = metadataService.findExistingPayloadHashesByRecordIds(chunk);

            List<Document> documentsToUpsert = new ArrayList<>();
            List<Long> recordIdsToReplace = new ArrayList<>();

            for (ContextStoreRecord record : records) {
                String currentHash = record.getPayloadHash();
                String existingHash = existingHashes.get(record.getId());

                if (currentHash != null && currentHash.equals(existingHash)) {
                    continue;
                }

                String embeddableText = buildEmbeddableText(record.getPayload(), semanticFields);

                if (embeddableText.isBlank()) {
                    // Nothing to embed — still mark for replacement so a stale embedding gets cleared.
                    recordIdsToReplace.add(record.getId());

                    continue;
                }

                Map<String, Object> documentMetadata = buildDocumentMetadata(sourceId, record, semanticFields);

                documentsToUpsert.add(
                    new Document(UUID.randomUUID()
                        .toString(), embeddableText, documentMetadata));
                recordIdsToReplace.add(record.getId());
            }

            if (!recordIdsToReplace.isEmpty()) {
                List<String> existingVectorStoreIds = metadataService
                    .findVectorStoreIdsByRecordIds(recordIdsToReplace);

                if (!existingVectorStoreIds.isEmpty()) {
                    vectorStore.delete(existingVectorStoreIds);
                }
            }

            if (!documentsToUpsert.isEmpty()) {
                vectorStore.add(documentsToUpsert);

                log.debug(
                    "Context Store semantic embedding upserted {} document(s) for source={}",
                    documentsToUpsert.size(), sourceId);
            }
        }
    }

    private void sweepTombstonedVectorStoreRows(Long sourceId) {
        List<Long> tombstonedRecordIds = contextStoreRecordService.findTombstonedRecordIdsBySourceId(sourceId);

        if (tombstonedRecordIds.isEmpty()) {
            return;
        }

        List<String> orphanedVectorStoreIds = metadataService.findVectorStoreIdsByRecordIds(tombstonedRecordIds);

        if (orphanedVectorStoreIds.isEmpty()) {
            return;
        }

        vectorStore.delete(orphanedVectorStoreIds);

        log.debug(
            "Context Store semantic tombstone sweep evicted {} vector-store row(s) for source={}",
            orphanedVectorStoreIds.size(), sourceId);
    }

    private static List<String> extractSemanticFields(Map<String, ?> semanticIndexFields) {
        if (semanticIndexFields == null || semanticIndexFields.isEmpty()) {
            return List.of();
        }

        Object fieldsValue = semanticIndexFields.get("fields");

        if (!(fieldsValue instanceof List<?> list)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();

        for (Object element : list) {
            if (element instanceof String fieldName && !fieldName.isBlank()) {
                result.add(fieldName);
            } else if (element instanceof Map<?, ?> map && map.get("name") instanceof String fieldName
                && !fieldName.isBlank()) {

                // Tolerant of {"fields": [{"name": "title"}]} shape used by other field-list configs.
                result.add(fieldName);
            }
        }

        return result;
    }

    private static String buildEmbeddableText(Map<String, ?> payload, List<String> semanticFields) {
        StringBuilder builder = new StringBuilder();

        for (String fieldName : semanticFields) {
            Object value = payload.get(fieldName);

            if (value == null) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append('\n');
            }

            builder.append(value);
        }

        return builder.toString();
    }

    private static Map<String, Object> buildDocumentMetadata(
        Long sourceId, ContextStoreRecord record, List<String> semanticFields) {

        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put(METADATA_SOURCE_ID, sourceId);
        metadata.put(METADATA_RECORD_ID, record.getId());
        metadata.put(METADATA_PAYLOAD_HASH, record.getPayloadHash());

        Map<String, ?> payload = record.getPayload();

        for (String fieldName : semanticFields) {
            Object value = payload.get(fieldName);

            if (value != null) {
                metadata.put(fieldName, value);
            }
        }

        return metadata;
    }

    private static List<Long> aggregateSemanticRecordIds(JobExecution jobExecution) {
        Set<Long> recordIds = new HashSet<>();

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            Object value = stepExecution.getExecutionContext()
                .get(ContextStoreExecutionContextKeys.SEMANTIC_RECORD_IDS_KEY);

            if (value instanceof Collection<?> collection) {
                for (Object element : collection) {
                    Long coerced = coerceLong(element);

                    if (coerced != null) {
                        recordIds.add(coerced);
                    }
                }
            }
        }

        return new ArrayList<>(recordIds);
    }

    private static Long coerceLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isEmpty()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}
