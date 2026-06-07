/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.contextstore.destination;

import com.bytechef.commons.util.PayloadHashUtil;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.component.definition.datastream.ItemWriter;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecordIndex;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreRecordIndexService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreRecordService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * DESTINATION cluster element implementation for the {@code contextStore} component. Upserts records into the Context
 * Store replica for a configured {@link ContextStoreSource}; tracks {@code seenIds} per job for the post-step tombstone
 * sweep performed by the JobExecutionListener.
 *
 * <p>
 * Instances are created fresh per batch job by a {@code Supplier} captured in the cluster element definition (see
 * {@link com.bytechef.ee.component.contextstore.ContextStoreComponentHandler}). Each instance carries per-job mutable
 * state (sourceId, seenIds) and is therefore not safe to share across concurrent jobs.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ContextStoreItemWriter implements ItemWriter {

    public static final String SOURCE_ID = "sourceId";
    public static final String MODE = "mode";

    public static final String MODE_FULL_REPLACE = "FULL_REPLACE";
    public static final String MODE_PARTIAL = "PARTIAL";

    public static final String SEEN_IDS_KEY = "contextStore.seenIds";
    public static final String SEMANTIC_RECORD_IDS_KEY = "contextStore.semanticRecordIds";
    public static final String MODE_KEY = "contextStore.mode";

    private final ContextStoreSourceService contextStoreSourceService;
    private final ContextStoreRecordService contextStoreRecordService;
    private final ContextStoreRecordIndexService contextStoreRecordIndexService;

    private Long sourceId;
    private String mode;
    private String idField;
    private Map<String, String> indexedFields;
    private @Nullable Map<String, Object> storedFields;
    private Set<String> seenIds;
    private Set<Long> semanticRecordIds;

    @SuppressFBWarnings("EI2")
    public ContextStoreItemWriter(
        ContextStoreSourceService contextStoreSourceService,
        ContextStoreRecordService contextStoreRecordService,
        ContextStoreRecordIndexService contextStoreRecordIndexService) {

        this.contextStoreSourceService = contextStoreSourceService;
        this.contextStoreRecordService = contextStoreRecordService;
        this.contextStoreRecordIndexService = contextStoreRecordIndexService;
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

        ContextStoreSource source = contextStoreSourceService.fetch(sourceId)
            .orElseThrow(() -> new IllegalStateException("ContextStoreSource " + sourceId + " not found"));

        this.idField = source.getIdField();
        this.indexedFields = extractIndexedFields(source.getIndexedFields());

        Map<String, ?> storedFieldsMap = source.getStoredFields();

        this.storedFields = storedFieldsMap == null ? null : new LinkedHashMap<>(storedFieldsMap);

        this.seenIds = new HashSet<>();
        this.semanticRecordIds = new HashSet<>();
    }

    @Override
    @SuppressFBWarnings(
        value = "UNSAFE_HASH_EQUALS",
        justification = "Hash comparison is used for change detection only, not for security-sensitive verification.")
    public void write(List<? extends Map<String, Object>> items) {
        Instant now = Instant.now();

        for (Map<String, Object> sourceRecord : items) {
            Object idValue = sourceRecord.get(idField);

            if (idValue == null) {
                throw new IllegalArgumentException(
                    "Record missing idField '" + idField + "': " + sourceRecord);
            }

            String sourceRecordId = String.valueOf(idValue);

            seenIds.add(sourceRecordId);

            Map<String, Object> filteredPayload = applyFieldWhitelist(sourceRecord, storedFields);
            String payloadHash = PayloadHashUtil.hash(filteredPayload);

            Optional<ContextStoreRecord> existingOptional = contextStoreRecordService.fetchByKey(
                sourceId, sourceRecordId);

            if (existingOptional.isPresent()
                && existingOptional.get()
                    .getPayloadHash()
                    .equals(payloadHash)
                && existingOptional.get()
                    .getDeletedAt() == null) {

                ContextStoreRecord record = existingOptional.get();

                record.setLastSeenAt(now);

                ContextStoreRecord saved = contextStoreRecordService.save(record);

                semanticRecordIds.add(saved.getId());
            } else if (existingOptional.isPresent()) {
                ContextStoreRecord record = existingOptional.get();

                record.setPayload(filteredPayload);
                record.setPayloadHash(payloadHash);
                record.setLastSeenAt(now);
                record.setDeletedAt(null);

                ContextStoreRecord saved = contextStoreRecordService.save(record);

                contextStoreRecordIndexService.deleteAllByRecordId(saved.getId());
                writeIndexRows(saved.getId(), filteredPayload);

                semanticRecordIds.add(saved.getId());
            } else {
                ContextStoreRecord record = new ContextStoreRecord();

                record.setSourceId(sourceId);
                record.setSourceRecordId(sourceRecordId);
                record.setPayload(filteredPayload);
                record.setPayloadHash(payloadHash);
                record.setLastSeenAt(now);

                ContextStoreRecord saved = contextStoreRecordService.save(record);

                writeIndexRows(saved.getId(), filteredPayload);

                semanticRecordIds.add(saved.getId());
            }
        }
    }

    @Override
    public void update(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        executionContext.put(SEEN_IDS_KEY, new ArrayList<>(seenIds));
        executionContext.put(SEMANTIC_RECORD_IDS_KEY, new ArrayList<>(semanticRecordIds));
        executionContext.put(MODE_KEY, mode);
    }

    @Override
    public void close() {
        // No-op; tombstone sweep is handled by the JobExecutionListener after the job completes.
    }

    private static Map<String, String> extractIndexedFields(@Nullable Map<String, ?> indexedFieldsMap) {
        // Canonical shape is a flat {fieldName: type} map — matches what the dialog persists, what
        // IndexedFieldDiffer diffs, and what ClickHouseTableDdlGenerator projects. Don't introduce a
        // nested envelope here; the column has one shape across all consumers.
        if (indexedFieldsMap == null || indexedFieldsMap.isEmpty()) {
            return Map.of();
        }

        Map<String, String> typedFields = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : indexedFieldsMap.entrySet()) {
            Object type = entry.getValue();

            if (type != null) {
                typedFields.put(entry.getKey(), String.valueOf(type));
            }
        }

        return typedFields;
    }

    private static Map<String, Object> applyFieldWhitelist(
        Map<String, Object> record, @Nullable Map<String, Object> storedFields) {

        if (storedFields == null) {
            return record;
        }

        Object fieldsObj = storedFields.get("fields");

        if (!(fieldsObj instanceof List<?> fields)) {
            return record;
        }

        Map<String, Object> filtered = new LinkedHashMap<>();

        for (Object fieldObj : fields) {
            if (fieldObj instanceof String dottedPath) {
                Object value = resolveDottedPath(record, dottedPath);

                if (value != null) {
                    setDottedPath(filtered, dottedPath, value);
                }
            }
        }

        return filtered;
    }

    private static @Nullable Object resolveDottedPath(Map<String, Object> root, String path) {
        Object current = root;

        for (String part : path.split("\\.")) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    @SuppressWarnings("unchecked")
    private static void setDottedPath(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");

        Map<String, Object> current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], key -> new LinkedHashMap<>());
        }

        current.put(parts[parts.length - 1], value);
    }

    private void writeIndexRows(Long recordId, Map<String, Object> filteredPayload) {
        for (Map.Entry<String, String> entry : indexedFields.entrySet()) {
            String fieldName = entry.getKey();
            String type = entry.getValue();

            Object value = resolveDottedPath(filteredPayload, fieldName);

            if (value == null) {
                continue;
            }

            ContextStoreRecordIndex index = new ContextStoreRecordIndex();

            index.setRecordId(recordId);
            index.setFieldName(fieldName);

            switch (type.toUpperCase(Locale.ROOT)) {
                case "TEXT" -> index.setValueText(value.toString());
                case "NUMERIC" -> index.setValueNumeric(new BigDecimal(value.toString()));
                case "TIMESTAMP" -> index.setValueTimestamp(Instant.parse(value.toString()));
                default -> throw new IllegalArgumentException(
                    "Unknown indexed field type: " + type + " for field " + fieldName);
            }

            contextStoreRecordIndexService.save(index);
        }
    }
}
