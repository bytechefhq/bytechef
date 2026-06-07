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
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter.FilterOp;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuerySort;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuerySort.SortDirection;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 * Integration test for {@link ContextStoreQueryService}. Exercises every supported {@link FilterOp}, sort asc/desc,
 * cursor pagination, the {@code includeDeleted} switch, and the by-key {@code get(...)} path against a real Postgres
 * via Testcontainers.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = ContextStoreIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class ContextStoreQueryServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private ContextStoreQueryService contextStoreQueryService;

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
    void testSearchWithEqFilter() {
        Long sourceId = givenSource("HubSpot");

        Long aliceId = givenRecord(sourceId, "1", Map.of("name", "Alice", "score", 80));
        givenRecord(sourceId, "2", Map.of("name", "Bob", "score", 65));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("name", FilterOp.EQ, "Alice"));

        assertThat(extractIds(result)).containsExactly(aliceId);
    }

    @Test
    void testSearchWithNeqFilter() {
        Long sourceId = givenSource("HubSpot");

        givenRecord(sourceId, "1", Map.of("name", "Alice"));
        Long bobId = givenRecord(sourceId, "2", Map.of("name", "Bob"));
        Long charlieId = givenRecord(sourceId, "3", Map.of("name", "Charlie"));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("name", FilterOp.NEQ, "Alice"));

        assertThat(extractIds(result)).containsExactlyInAnyOrder(bobId, charlieId);
    }

    @Test
    void testSearchWithInFilter() {
        Long sourceId = givenSource("HubSpot");

        Long aliceId = givenRecord(sourceId, "1", Map.of("name", "Alice"));
        Long bobId = givenRecord(sourceId, "2", Map.of("name", "Bob"));
        givenRecord(sourceId, "3", Map.of("name", "Charlie"));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("name", FilterOp.IN, List.of("Alice", "Bob")));

        assertThat(extractIds(result)).containsExactlyInAnyOrder(aliceId, bobId);
    }

    @Test
    void testSearchWithEmptyInFilterReturnsNoRows() {
        Long sourceId = givenSource("HubSpot");

        givenRecord(sourceId, "1", Map.of("name", "Alice"));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("name", FilterOp.IN, List.of()));

        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void testSearchWithContainsFilter() {
        Long sourceId = givenSource("HubSpot");

        Long acmeId = givenRecord(sourceId, "1", Map.of("name", "Acme Corporation"));
        givenRecord(sourceId, "2", Map.of("name", "Globex"));
        Long acmeBranchId = givenRecord(sourceId, "3", Map.of("name", "Acme Branch"));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("name", FilterOp.CONTAINS, "acme"));

        assertThat(extractIds(result)).containsExactlyInAnyOrder(acmeId, acmeBranchId);
    }

    @Test
    void testSearchWithStartsWithFilter() {
        Long sourceId = givenSource("HubSpot");

        Long acmeId = givenRecord(sourceId, "1", Map.of("name", "Acme Corporation"));
        Long acmeBranchId = givenRecord(sourceId, "2", Map.of("name", "Acme Branch"));
        givenRecord(sourceId, "3", Map.of("name", "Globex"));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("name", FilterOp.STARTS_WITH, "Acme"));

        assertThat(extractIds(result)).containsExactlyInAnyOrder(acmeId, acmeBranchId);
    }

    @Test
    void testSearchWithGtFilter() {
        Long sourceId = givenSource("HubSpot");

        givenRecord(sourceId, "1", Map.of("name", "Alice", "score", 50));
        Long bobId = givenRecord(sourceId, "2", Map.of("name", "Bob", "score", 80));
        Long charlieId = givenRecord(sourceId, "3", Map.of("name", "Charlie", "score", 90));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("score", FilterOp.GT, 75));

        assertThat(extractIds(result)).containsExactlyInAnyOrder(bobId, charlieId);
    }

    @Test
    void testSearchWithGteFilter() {
        Long sourceId = givenSource("HubSpot");

        givenRecord(sourceId, "1", Map.of("score", 50));
        Long bobId = givenRecord(sourceId, "2", Map.of("score", 75));
        Long charlieId = givenRecord(sourceId, "3", Map.of("score", 90));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("score", FilterOp.GTE, 75));

        assertThat(extractIds(result)).containsExactlyInAnyOrder(bobId, charlieId);
    }

    @Test
    void testSearchWithLtFilter() {
        Long sourceId = givenSource("HubSpot");

        Instant earlier = Instant.parse("2026-01-01T00:00:00Z");
        Instant middle = Instant.parse("2026-03-01T00:00:00Z");
        Instant later = Instant.parse("2026-06-01T00:00:00Z");

        Long earlierId = givenRecord(sourceId, "1", Map.of("createdAt", earlier.toString()));
        Long middleId = givenRecord(sourceId, "2", Map.of("createdAt", middle.toString()));
        givenRecord(sourceId, "3", Map.of("createdAt", later.toString()));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("createdAt", FilterOp.LT, "2026-04-01T00:00:00Z"));

        assertThat(extractIds(result)).containsExactlyInAnyOrder(earlierId, middleId);
    }

    @Test
    void testSearchWithLteFilter() {
        Long sourceId = givenSource("HubSpot");

        Instant cutoff = Instant.parse("2026-03-01T00:00:00Z");

        Long earlierId = givenRecord(sourceId, "1", Map.of("createdAt", "2026-01-01T00:00:00Z"));
        Long cutoffId = givenRecord(sourceId, "2", Map.of("createdAt", cutoff.toString()));
        givenRecord(sourceId, "3", Map.of("createdAt", "2026-06-01T00:00:00Z"));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("createdAt", FilterOp.LTE, cutoff.toString()));

        assertThat(extractIds(result)).containsExactlyInAnyOrder(earlierId, cutoffId);
    }

    @Test
    void testSearchWithBetweenFilter() {
        Long sourceId = givenSource("HubSpot");

        givenRecord(sourceId, "1", Map.of("score", 30));
        Long bobId = givenRecord(sourceId, "2", Map.of("score", 50));
        Long charlieId = givenRecord(sourceId, "3", Map.of("score", 75));
        givenRecord(sourceId, "4", Map.of("score", 95));

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("score", FilterOp.BETWEEN, List.of(40, 80)));

        assertThat(extractIds(result)).containsExactlyInAnyOrder(bobId, charlieId);
    }

    @Test
    void testSearchExcludesDeletedByDefault() {
        Long sourceId = givenSource("HubSpot");

        Long aliveId = givenRecord(sourceId, "1", Map.of("name", "Alice"));
        Long tombstonedId = givenRecord(sourceId, "2", Map.of("name", "Alice"));

        tombstoneRecord(tombstonedId);

        ContextStoreSearchResult result = search(sourceId,
            new ContextStoreQueryFilter("name", FilterOp.EQ, "Alice"));

        assertThat(extractIds(result)).containsExactly(aliveId);
    }

    @Test
    void testSearchIncludesDeletedWhenRequested() {
        Long sourceId = givenSource("HubSpot");

        Long aliveId = givenRecord(sourceId, "1", Map.of("name", "Alice"));
        Long tombstonedId = givenRecord(sourceId, "2", Map.of("name", "Alice"));

        tombstoneRecord(tombstonedId);

        ContextStoreQuery query = new ContextStoreQuery(
            sourceId,
            List.of(new ContextStoreQueryFilter("name", FilterOp.EQ, "Alice")),
            List.of(),
            ContextStoreQuery.DEFAULT_LIMIT, null, true, null);

        ContextStoreSearchResult result = contextStoreQueryService.search(query);

        assertThat(extractIds(result)).containsExactlyInAnyOrder(aliveId, tombstonedId);
    }

    @Test
    void testSearchSortAscending() {
        Long sourceId = givenSource("HubSpot");

        Long charlieId = givenRecord(sourceId, "1", Map.of("name", "Charlie"));
        Long aliceId = givenRecord(sourceId, "2", Map.of("name", "Alice"));
        Long bobId = givenRecord(sourceId, "3", Map.of("name", "Bob"));

        ContextStoreQuery query = new ContextStoreQuery(
            sourceId,
            List.of(),
            List.of(new ContextStoreQuerySort("name", SortDirection.ASC)),
            ContextStoreQuery.DEFAULT_LIMIT, null, false, null);

        ContextStoreSearchResult result = contextStoreQueryService.search(query);

        assertThat(extractIds(result)).containsExactly(aliceId, bobId, charlieId);
    }

    @Test
    void testSearchSortDescending() {
        Long sourceId = givenSource("HubSpot");

        Long charlieId = givenRecord(sourceId, "1", Map.of("name", "Charlie"));
        Long aliceId = givenRecord(sourceId, "2", Map.of("name", "Alice"));
        Long bobId = givenRecord(sourceId, "3", Map.of("name", "Bob"));

        ContextStoreQuery query = new ContextStoreQuery(
            sourceId,
            List.of(),
            List.of(new ContextStoreQuerySort("name", SortDirection.DESC)),
            ContextStoreQuery.DEFAULT_LIMIT, null, false, null);

        ContextStoreSearchResult result = contextStoreQueryService.search(query);

        assertThat(extractIds(result)).containsExactly(charlieId, bobId, aliceId);
    }

    @Test
    void testCursorPaginationIsStable() {
        Long sourceId = givenSource("HubSpot");

        List<Long> insertedIds = new java.util.ArrayList<>();

        for (int i = 1; i <= 7; i++) {
            insertedIds.add(givenRecord(sourceId, String.valueOf(i), Map.of("name", "User-" + i)));
        }

        // Page 1: limit 3.
        ContextStoreQuery firstPageQuery = new ContextStoreQuery(
            sourceId, List.of(), List.of(), 3, null, false, null);

        ContextStoreSearchResult firstPage = contextStoreQueryService.search(firstPageQuery);

        assertThat(firstPage.items()).hasSize(3);
        assertThat(firstPage.nextCursor()).isNotNull();
        assertThat(extractIds(firstPage)).containsExactly(insertedIds.get(0), insertedIds.get(1), insertedIds.get(2));

        // Page 2: limit 3 with cursor.
        ContextStoreQuery secondPageQuery = new ContextStoreQuery(
            sourceId, List.of(), List.of(), 3, firstPage.nextCursor(), false, null);

        ContextStoreSearchResult secondPage = contextStoreQueryService.search(secondPageQuery);

        assertThat(secondPage.items()).hasSize(3);
        assertThat(secondPage.nextCursor()).isNotNull();
        assertThat(extractIds(secondPage)).containsExactly(insertedIds.get(3), insertedIds.get(4), insertedIds.get(5));

        // Page 3: limit 3, only one record left → no next cursor.
        ContextStoreQuery thirdPageQuery = new ContextStoreQuery(
            sourceId, List.of(), List.of(), 3, secondPage.nextCursor(), false, null);

        ContextStoreSearchResult thirdPage = contextStoreQueryService.search(thirdPageQuery);

        assertThat(thirdPage.items()).hasSize(1);
        assertThat(thirdPage.nextCursor()).isNull();
        assertThat(extractIds(thirdPage)).containsExactly(insertedIds.get(6));
    }

    @Test
    void testGetByKeyReturnsRecord() {
        Long sourceId = givenSource("HubSpot");

        Long aliceId = givenRecord(sourceId, "alice-001", Map.of("name", "Alice"));

        Optional<ContextStoreRecord> fetched =
            contextStoreQueryService.get(sourceId, "alice-001");

        assertThat(fetched).isPresent();
        assertThat(fetched.get()
            .getId())
                .isEqualTo(aliceId);
        assertThat(fetched.get()
            .getPayload()
            .get("name"))
                .isEqualTo("Alice");
    }

    @Test
    void testGetByKeyReturnsEmptyWhenMissing() {
        Long sourceId = givenSource("HubSpot");

        Optional<ContextStoreRecord> fetched =
            contextStoreQueryService.get(sourceId, "missing");

        assertThat(fetched).isEmpty();
    }

    @Test
    void testSearchRecordIdsReturnsIdsMatchingFilter() {
        Long sourceId = givenSource("HubSpot");

        Long aliceId = givenRecord(sourceId, "1", Map.of("name", "Alice", "score", 80));
        givenRecord(sourceId, "2", Map.of("name", "Bob", "score", 65));
        Long charlieId = givenRecord(sourceId, "3", Map.of("name", "Charlie", "score", 90));

        List<Long> ids = contextStoreQueryService.searchRecordIds(
            sourceId, new ContextStoreQueryFilter("score", FilterOp.GTE, 75));

        assertThat(ids).containsExactlyInAnyOrder(aliceId, charlieId);
    }

    @Test
    void testSearchRecordIdsHonoursEqualityFilter() {
        Long sourceId = givenSource("HubSpot");

        Long aliceId = givenRecord(sourceId, "1", Map.of("name", "Alice"));
        givenRecord(sourceId, "2", Map.of("name", "Bob"));

        List<Long> ids = contextStoreQueryService.searchRecordIds(
            sourceId, new ContextStoreQueryFilter("name", FilterOp.EQ, "Alice"));

        assertThat(ids).containsExactly(aliceId);
    }

    @Test
    void testSearchRecordIdsReturnsEmptyForEmptyInFilter() {
        Long sourceId = givenSource("HubSpot");

        givenRecord(sourceId, "1", Map.of("name", "Alice"));

        List<Long> ids = contextStoreQueryService.searchRecordIds(
            sourceId, new ContextStoreQueryFilter("name", FilterOp.IN, List.of()));

        assertThat(ids).isEmpty();
    }

    @Test
    void testSearchRecordIdsExcludesTombstonedRecords() {
        Long sourceId = givenSource("HubSpot");

        Long aliveId = givenRecord(sourceId, "1", Map.of("name", "Alice"));
        Long tombstonedId = givenRecord(sourceId, "2", Map.of("name", "Alice"));

        tombstoneRecord(tombstonedId);

        List<Long> ids = contextStoreQueryService.searchRecordIds(
            sourceId, new ContextStoreQueryFilter("name", FilterOp.EQ, "Alice"));

        assertThat(ids).containsExactly(aliveId);
    }

    @Test
    void testSearchScopesToWorkspaceAndSource() {
        Long firstSourceId = givenSource("HubSpot Prod");
        Long secondSourceId = givenSource("HubSpot Staging");

        Long firstId = givenRecord(firstSourceId, "1", Map.of("name", "Alice"));
        givenRecord(secondSourceId, "1", Map.of("name", "Alice"));

        ContextStoreSearchResult result = search(firstSourceId,
            new ContextStoreQueryFilter("name", FilterOp.EQ, "Alice"));

        assertThat(extractIds(result)).containsExactly(firstId);
    }

    private ContextStoreSearchResult search(Long sourceId, ContextStoreQueryFilter filter) {
        ContextStoreQuery query = new ContextStoreQuery(
            sourceId, List.of(filter), List.of(),
            ContextStoreQuery.DEFAULT_LIMIT, null, false, null);

        return contextStoreQueryService.search(query);
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

        Long sourceId = contextStoreSourceService.create(source)
            .getId();

        // Workspace scoping for the search() path now flows through the workspace_context_store_source relation
        // table; insert the membership row directly so the platform-side IntTest doesn't need the automation-side
        // service. (Production code paths use WorkspaceContextStoreSourceService.create(...) under the facade.)
        Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.update(
            "INSERT INTO workspace_context_store_source " +
                "(workspace_id, context_store_source_id, created_date, last_modified_date, version) " +
                "VALUES (?, ?, ?, ?, 0)",
            WORKSPACE_ID, sourceId, now, now);

        return sourceId;
    }

    /**
     * Inserts a record and creates {@code context_store_record_index} rows for every key in {@code payload}. Each
     * indexed-field row populates the typed columns appropriate to the value's runtime type — strings hit
     * {@code value_text}, numbers hit both {@code value_text} and {@code value_numeric}, ISO-8601 timestamp strings hit
     * both {@code value_text} and {@code value_timestamp}.
     *
     * <p>
     * Writes go through {@link JdbcTemplate} with a {@link PGobject}-typed {@code payload} parameter rather than Spring
     * Data JDBC — the project's {@code MapWrapperToStringConverter} binds JSON as {@code varchar}, which Postgres
     * rejects against a {@code JSONB} column. Bypassing the repository for the JSONB write keeps the test
     * self-contained until the production wiring grows a dedicated JSONB converter.
     * </p>
     */
    private Long givenRecord(Long sourceId, String sourceRecordId, Map<String, Object> payload) {
        // Postgres rejects character varying ↔ jsonb implicit casts; bind the payload as a typed PGobject.
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
            indexField(recordId, entry.getKey(), entry.getValue());
        }

        return Objects.requireNonNull(recordId);
    }

    private void indexField(Long recordId, String fieldName, Object value) {
        // Always populate value_text — it is the canonical column for EQ/NEQ/IN/CONTAINS/STARTS_WITH.
        String textValue = value == null ? null : value.toString();
        BigDecimal numericValue = null;
        Timestamp timestampValue = null;

        if (value instanceof Number number) {
            numericValue = new BigDecimal(number.toString());
        } else if (value instanceof String stringValue) {
            try {
                timestampValue = Timestamp.from(Instant.parse(stringValue));
            } catch (Exception ignored) {
                // not a timestamp — leave value_timestamp null.
            }
        } else if (value instanceof Instant instant) {
            timestampValue = Timestamp.from(instant);
        }

        jdbcTemplate.update(
            "INSERT INTO context_store_record_index (record_id, field_name, value_text, value_numeric, " +
                " value_timestamp) VALUES (?, ?, ?, ?, ?)",
            recordId, fieldName, textValue, numericValue, timestampValue);
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

    private static List<Long> extractIds(ContextStoreSearchResult result) {
        return result.items()
            .stream()
            .map(ContextStoreRecord::getId)
            .toList();
    }
}
