/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.repository;

import com.bytechef.ee.platform.contextstore.clickhouse.schema.ClickHouseTableNameSanitizer;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.repository.ContextStoreRecordRepository;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component("contextStoreRecordClickHouseRepository")
@ConditionalOnBean(name = "clickHouseDataSource")
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
@Primary
public class ContextStoreRecordClickHouseRepository implements ContextStoreRecordRepository {

    private static final Logger log = LoggerFactory.getLogger(ContextStoreRecordClickHouseRepository.class);

    private final ConcurrentHashMap<Long, String> tableNameCache = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbcTemplate;
    private final ContextStoreSourceService contextStoreSourceService;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public ContextStoreRecordClickHouseRepository(
        @Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate,
        ContextStoreSourceService contextStoreSourceService, ObjectMapper objectMapper) {

        this.jdbcTemplate = jdbcTemplate;
        this.contextStoreSourceService = contextStoreSourceService;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC",
        justification = "Table name is sanitised at source-creation time via ClickHouseTableNameSanitizer and "
            + "re-validated by resolveTableName(); it always matches VALID_TABLE_NAME_REGEX. The only user-controlled "
            + "fragment of every statement is a regex-bounded identifier.")
    public ContextStoreRecord save(ContextStoreRecord contextStoreRecord) {
        Objects.requireNonNull(contextStoreRecord, "contextStoreRecord");

        String tableName = resolveTableName(contextStoreRecord.getSourceId());
        String payloadJson = objectMapper.writeValueAsString(contextStoreRecord.getPayload());

        jdbcTemplate.update(
            "INSERT INTO " + tableName
                + " (_id, _payload_hash, _payload, _last_seen_at, _deleted_at) VALUES (?, ?, ?, ?, ?)",
            contextStoreRecord.getSourceRecordId(), contextStoreRecord.getPayloadHash(), payloadJson,
            Timestamp.from(contextStoreRecord.getLastSeenAt()),
            contextStoreRecord.getDeletedAt() == null ? null : Timestamp.from(contextStoreRecord.getDeletedAt()));

        return contextStoreRecord;
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC", justification = "Table name is regex-validated; see save()")
    public Optional<ContextStoreRecord> findBySourceIdAndSourceRecordId(Long sourceId, String sourceRecordId) {
        String tableName = resolveTableName(sourceId);

        List<ContextStoreRecord> rows = jdbcTemplate.query(
            "SELECT _id, _payload_hash, _payload, _last_seen_at, _deleted_at FROM " + tableName
                + " FINAL WHERE _id = ? AND _deleted_at IS NULL LIMIT 1",
            (rs, rowNum) -> mapRow(rs, sourceId), sourceRecordId);

        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC", justification = "Table name is regex-validated; see save()")
    public int tombstoneUnseen(Long sourceId, Collection<String> seenIds, Instant deletedAt) {
        Objects.requireNonNull(deletedAt, "deletedAt");

        String tableName = resolveTableName(sourceId);

        if (seenIds.isEmpty()) {
            jdbcTemplate.update(
                "ALTER TABLE " + tableName + " UPDATE _deleted_at = ? WHERE _deleted_at IS NULL",
                Timestamp.from(deletedAt));
        } else {
            String inList = String.join(",", Collections.nCopies(seenIds.size(), "?"));
            Object[] params = new Object[seenIds.size() + 1];
            params[0] = Timestamp.from(deletedAt);
            int index = 1;

            for (String id : seenIds) {
                params[index++] = id;
            }

            jdbcTemplate.update(
                "ALTER TABLE " + tableName
                    + " UPDATE _deleted_at = ? WHERE _id NOT IN (" + inList + ") AND _deleted_at IS NULL",
                params);
        }

        log.debug("Queued ClickHouse tombstone mutation on table={} (async; row count unavailable)", tableName);

        return -1;
    }

    @Override
    public List<ContextStoreRecord> findAllById(Iterable<Long> ids) {
        throw new UnsupportedOperationException(
            "findAllById is not supported on ClickHouse-backed Context Store sources — the chunker pipeline and "
                + "semantic-indexing paths that use this method are Postgres-only.");
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC", justification = "Table name is regex-validated; see save()")
    public List<Long> findTombstonedRecordIdsBySourceId(Long sourceId) {
        String tableName = resolveTableName(sourceId);

        return jdbcTemplate.queryForList(
            "SELECT toInt64(cityHash64(_id)) AS id FROM " + tableName + " FINAL WHERE _deleted_at IS NOT NULL",
            Long.class);
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException(
            "deleteById is not supported on ClickHouse-backed Context Store sources — the admin delete path is "
                + "Postgres-only. Use ALTER TABLE ... DELETE WHERE _id = ? directly if hard-delete is needed "
                + "operationally.");
    }

    private String resolveTableName(Long sourceId) {
        Objects.requireNonNull(sourceId, "sourceId");

        return tableNameCache.computeIfAbsent(sourceId, id -> {
            String name = contextStoreSourceService.fetch(id)
                .map(source -> {
                    String tableName = source.getClickhouseTableName();

                    if (tableName == null) {
                        throw new IllegalStateException(
                            "Context Store source " + id + " is routed to ClickHouse but has no "
                                + "clickhouseTableName set — the facade should have populated it at create time.");
                    }

                    return tableName;
                })
                .orElseThrow(() -> new IllegalStateException(
                    "Context Store source " + id + " not found while resolving ClickHouse table name."));

            if (!name.matches(ClickHouseTableNameSanitizer.VALID_TABLE_NAME_REGEX)) {
                throw new IllegalStateException(
                    "Context Store source " + id + " has a clickhouseTableName ('" + name
                        + "') that fails the sanitiser regex; refusing to interpolate into a SQL string.");
            }

            return name;
        });
    }

    private ContextStoreRecord mapRow(ResultSet rs, Long sourceId) throws SQLException {
        ContextStoreRecord contextStoreRecord = new ContextStoreRecord();

        contextStoreRecord.setSourceId(sourceId);
        contextStoreRecord.setSourceRecordId(rs.getString("_id"));
        contextStoreRecord.setPayloadHash(rs.getString("_payload_hash"));

        String payloadJson = rs.getString("_payload");

        if (payloadJson != null && !payloadJson.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            contextStoreRecord.setPayload(payload);
        } else {
            contextStoreRecord.setPayload(Map.of());
        }

        Timestamp lastSeenTs = rs.getTimestamp("_last_seen_at");

        if (lastSeenTs != null) {
            contextStoreRecord.setLastSeenAt(lastSeenTs.toInstant());
        }

        Timestamp deletedTs = rs.getTimestamp("_deleted_at");

        if (deletedTs != null) {
            contextStoreRecord.setDeletedAt(deletedTs.toInstant());
        }

        return contextStoreRecord;
    }
}
