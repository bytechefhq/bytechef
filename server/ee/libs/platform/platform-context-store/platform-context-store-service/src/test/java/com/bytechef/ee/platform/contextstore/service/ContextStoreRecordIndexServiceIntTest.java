/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.contextstore.config.ContextStoreIntTestConfiguration;
import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test for {@link ContextStoreRecordIndexService}. Pins the tombstone purge SQL against a real Postgres via
 * Testcontainers: only index rows belonging to tombstoned records of the given source are deleted; live records and
 * other sources keep their index rows.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = ContextStoreIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class ContextStoreRecordIndexServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private ContextStoreRecordIndexService contextStoreRecordIndexService;

    @Autowired
    private ContextStoreService contextStoreService;

    @Autowired
    private ContextStoreSourceService contextStoreSourceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanup() {
        // CASCADE on context_store_source -> context_store_record -> context_store_record_index removes everything.
        contextStoreSourceService.findAllActiveAcrossWorkspaces()
            .forEach(source -> contextStoreSourceService.delete(source.getId()));
    }

    @Test
    void testDeleteAllForTombstonedRecordsRemovesOnlyTombstonedRecordsIndexRows() {
        Long sourceId = givenSource("Airtable");

        Long aliveRecordId = givenRecord(sourceId, "1", Map.of("name", "Alice", "score", 80));
        Long tombstonedRecordId = givenRecord(sourceId, "2", Map.of("name", "Bob", "score", 65));

        tombstoneRecord(tombstonedRecordId);

        int purged = contextStoreRecordIndexService.deleteAllForTombstonedRecords(sourceId);

        assertThat(purged).isEqualTo(2);
        assertThat(countIndexRows(aliveRecordId)).isEqualTo(2);
        assertThat(countIndexRows(tombstonedRecordId)).isZero();
    }

    @Test
    void testDeleteAllForTombstonedRecordsIsScopedToSource() {
        Long sourceId = givenSource("Airtable");
        Long otherSourceId = givenSource("HubSpot");

        Long tombstonedRecordId = givenRecord(sourceId, "1", Map.of("name", "Alice"));
        Long otherTombstonedRecordId = givenRecord(otherSourceId, "1", Map.of("name", "Bob"));

        tombstoneRecord(tombstonedRecordId);
        tombstoneRecord(otherTombstonedRecordId);

        int purged = contextStoreRecordIndexService.deleteAllForTombstonedRecords(sourceId);

        assertThat(purged).isEqualTo(1);
        assertThat(countIndexRows(tombstonedRecordId)).isZero();
        assertThat(countIndexRows(otherTombstonedRecordId)).isEqualTo(1);
    }

    @Test
    void testDeleteAllForTombstonedRecordsWithNoTombstonesIsNoOp() {
        Long sourceId = givenSource("Airtable");

        Long aliveRecordId = givenRecord(sourceId, "1", Map.of("name", "Alice"));

        int purged = contextStoreRecordIndexService.deleteAllForTombstonedRecords(sourceId);

        assertThat(purged).isZero();
        assertThat(countIndexRows(aliveRecordId)).isEqualTo(1);
    }

    private int countIndexRows(Long recordId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM context_store_record_index WHERE record_id = ?", Integer.class, recordId);

        return Objects.requireNonNull(count);
    }

    private Long givenSource(String name) {
        ContextStore store = new ContextStore();

        store.setName("test-store-" + System.nanoTime());
        store.setEnvironment(Environment.DEVELOPMENT);

        Long storeId = contextStoreService.create(store)
            .getId();

        ContextStoreSource source = new ContextStoreSource();

        source.setContextStoreId(storeId);
        source.setName(name);
        source.setEntityName("contacts");
        source.setIdField("id");
        source.setIndexedFields(Map.of());
        source.setSourceComponentName("hubspot");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("contactsReader");
        source.setCadence("@hourly");
        source.setStatus(ContextStoreSourceStatus.READY);
        source.setWorkspaceId(WORKSPACE_ID);

        return contextStoreSourceService.create(source)
            .getId();
    }

    /**
     * Inserts a record and one {@code context_store_record_index} row per payload key. Writes go through
     * {@link JdbcTemplate} with a {@link PGobject}-typed {@code payload} parameter for the same JSONB-binding reason
     * documented on {@code ContextStoreQueryServiceIntTest#givenRecord}.
     */
    private Long givenRecord(Long sourceId, String sourceRecordId, Map<String, Object> payload) {
        PGobject payloadObject = newPGobject(payload);
        String payloadHash = String.format("%016x", Objects.hash(payload) & 0xFFFFFFFFL);
        Timestamp lastSeenAt = Timestamp.from(Instant.now());

        Long recordId = jdbcTemplate.queryForObject(
            "INSERT INTO context_store_record " +
                "(source_id, source_record_id, payload, payload_hash, " +
                " last_seen_at, created_date, last_modified_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            sourceId, sourceRecordId, payloadObject, payloadHash,
            lastSeenAt, lastSeenAt, lastSeenAt);

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            jdbcTemplate.update(
                "INSERT INTO context_store_record_index (record_id, field_name, value_text) VALUES (?, ?, ?)",
                recordId, entry.getKey(), String.valueOf(entry.getValue()));
        }

        return Objects.requireNonNull(recordId);
    }

    private void tombstoneRecord(Long recordId) {
        jdbcTemplate.update(
            "UPDATE context_store_record SET deleted_at = ?, last_modified_date = ? WHERE id = ?",
            Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), recordId);
    }

    private PGobject newPGobject(Map<String, Object> payload) {
        try {
            PGobject jsonObject = new PGobject();

            jsonObject.setType("jsonb");
            jsonObject.setValue(objectMapper.writeValueAsString(payload));

            return jsonObject;
        } catch (SQLException | JacksonException exception) {
            throw new IllegalStateException("Failed to build PGobject for payload", exception);
        }
    }
}
