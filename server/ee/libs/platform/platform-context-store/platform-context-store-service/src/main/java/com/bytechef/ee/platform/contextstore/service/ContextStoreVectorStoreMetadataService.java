/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 * @version ee
 */
public final class ContextStoreVectorStoreMetadataService {

    private static final Pattern SAFE_TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

    private final JdbcTemplate pgVectorJdbcTemplate;
    private final Supplier<String> fullTableNameSupplier;

    @SuppressFBWarnings("EI2")
    public ContextStoreVectorStoreMetadataService(
        JdbcTemplate pgVectorJdbcTemplate, ObjectMapper objectMapper, Supplier<String> fullTableNameSupplier) {

        this.pgVectorJdbcTemplate = pgVectorJdbcTemplate;
        this.fullTableNameSupplier = fullTableNameSupplier;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public Map<Long, String> findExistingPayloadHashesByRecordIds(Collection<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return Map.of();
        }

        String fullTableName = resolveTableName();
        String placeholders = String.join(",", Collections.nCopies(recordIds.size(), "?"));

        String sql = "SELECT (metadata->>'recordId')::bigint AS record_id, "
            + "metadata->>'payloadHash' AS payload_hash "
            + "FROM " + fullTableName + " "
            + "WHERE (metadata->>'recordId')::bigint IN (" + placeholders + ")";

        Map<Long, String> result = new HashMap<>();

        pgVectorJdbcTemplate.query(sql, recordIds.toArray(), (resultSet, rowNumber) -> {
            long recordId = resultSet.getLong("record_id");
            String payloadHash = resultSet.getString("payload_hash");

            result.put(recordId, payloadHash);

            return null;
        });

        return result;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<String> findVectorStoreIdsByRecordIds(Collection<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return List.of();
        }

        String fullTableName = resolveTableName();
        String placeholders = String.join(",", Collections.nCopies(recordIds.size(), "?"));

        String sql = "SELECT id::text FROM " + fullTableName + " "
            + "WHERE (metadata->>'recordId')::bigint IN (" + placeholders + ")";

        return pgVectorJdbcTemplate.queryForList(sql, String.class, recordIds.toArray());
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public List<String> findVectorStoreIdsBySourceAndEntity(long sourceId, String entityName) {
        String fullTableName = resolveTableName();

        String sql = "SELECT id::text FROM " + fullTableName + " "
            + "WHERE (metadata->>'sourceId')::bigint = ? "
            + "AND metadata->>'entityName' = ?";

        return pgVectorJdbcTemplate.queryForList(sql, String.class, sourceId, entityName);
    }

    private String resolveTableName() {
        String fullTableName = fullTableNameSupplier.get();

        if (!SAFE_TABLE_NAME_PATTERN.matcher(fullTableName)
            .matches()) {
            throw new IllegalArgumentException("Invalid table name: " + fullTableName);
        }

        return fullTableName;
    }
}
