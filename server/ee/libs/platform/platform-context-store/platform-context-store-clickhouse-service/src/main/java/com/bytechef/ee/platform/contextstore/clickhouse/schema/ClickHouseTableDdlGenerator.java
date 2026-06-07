/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.schema;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Produces the {@code CREATE TABLE} DDL for the per-source ClickHouse record table from a {@link ContextStoreSource}'s
 * {@code indexedFields} map.
 *
 * <p>
 * Output shape:
 *
 * <pre>{@code
 * CREATE TABLE {tableName} (
 *     _id String,
 *     _payload_hash String,
 *     _payload String,
 *     _last_seen_at DateTime64(3),
 *     _deleted_at Nullable(DateTime64(3)),
 *     {field1} Nullable({chType1}),
 *     {field2} Nullable({chType2}),
 *     ...
 * ) ENGINE = ReplacingMergeTree(_last_seen_at) ORDER BY (_id)
 * }</pre>
 *
 * <p>
 * Field-type mapping mirrors the Postgres {@code context_store_record_index} typed-column dispatch in
 * {@code ContextStoreQueryServiceImpl} so query semantics stay consistent across backends:
 * <ul>
 * <li>{@code TEXT} → ClickHouse {@code String}</li>
 * <li>{@code NUMERIC} → ClickHouse {@code Float64} (Postgres uses unbounded {@code NUMERIC}; {@code Float64} is the
 * pragmatic ClickHouse equivalent — ClickHouse {@code Decimal} needs explicit precision args which would force a second
 * design decision per field)</li>
 * <li>{@code TIMESTAMP} → ClickHouse {@code DateTime64(3)} (millisecond precision matches Postgres
 * {@code TIMESTAMP WITH TIME ZONE} for our query purposes)</li>
 * </ul>
 *
 * <p>
 * {@code _payload} is stored as {@code String} (not the experimental {@code JSON} type) so the repository serialises
 * JSON text via Jackson — keeps the migration story simple if ClickHouse {@code JSON} stabilises and we want to switch
 * later.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public final class ClickHouseTableDdlGenerator {

    private ClickHouseTableDdlGenerator() {
    }

    /**
     * Builds the {@code CREATE TABLE} DDL for the given source. Field names are validated against
     * {@link ClickHouseFieldTypeMapper#VALID_FIELD_NAME_PATTERN} after lowercasing; anything that doesn't match (dotted
     * paths like {@code "company.name"}, names starting with digits, names over 128 chars) is rejected with
     * {@link IllegalArgumentException}.
     *
     * <p>
     * Dotted paths are a deliberate exclusion: the Postgres side stores them flat in
     * {@code context_store_record_index.field_name}, so ClickHouse would need a parallel decision (column-per-path vs
     * nested type). Sources with dotted-path fields must pre-flatten before the projection is populated.
     * </p>
     */
    public static String createTableDdl(String tableName, ContextStoreSource source) {
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(source, "source");

        Map<String, ?> indexedFields = source.getIndexedFields();

        StringBuilder ddl = new StringBuilder(256)
            .append("CREATE TABLE ")
            .append(tableName)
            .append(" (\n")
            .append("    _id String,\n")
            .append("    _payload_hash String,\n")
            .append("    _payload String,\n")
            .append("    _last_seen_at DateTime64(3),\n")
            .append("    _deleted_at Nullable(DateTime64(3))");

        Map<String, String> typedFields = resolveTypedFields(indexedFields);

        for (Map.Entry<String, String> typedField : typedFields.entrySet()) {
            ddl.append(",\n    ")
                .append(typedField.getKey())
                .append(' ')
                .append("Nullable(")
                .append(typedField.getValue())
                .append(')');
        }

        ddl.append("\n) ENGINE = ReplacingMergeTree(_last_seen_at) ORDER BY (_id)");

        return ddl.toString();
    }

    private static Map<String, String> resolveTypedFields(Map<String, ?> indexedFields) {
        // Sort by field name so DDL is deterministic — ContextStoreSource.getIndexedFields() comes back from a
        // HashMap-backed MapWrapper whose iteration order is unstable. Determinism matters for code review of
        // migration diffs and for repeatable test assertions.
        Map<String, String> typedFields = new TreeMap<>();

        for (Map.Entry<String, ?> entry : indexedFields.entrySet()) {
            String fieldName = ClickHouseFieldTypeMapper.validateFieldName(entry.getKey());
            String clickHouseType = ClickHouseFieldTypeMapper.toClickHouseType(String.valueOf(entry.getValue()));

            typedFields.put(fieldName, clickHouseType);
        }

        return typedFields;
    }
}
