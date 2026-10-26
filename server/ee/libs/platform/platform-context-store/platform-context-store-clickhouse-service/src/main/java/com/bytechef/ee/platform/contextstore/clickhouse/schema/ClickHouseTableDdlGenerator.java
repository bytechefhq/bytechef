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
 * @author Ivica Cardic
 * @version ee
 */
public final class ClickHouseTableDdlGenerator {

    private ClickHouseTableDdlGenerator() {
    }

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
        Map<String, String> typedFields = new TreeMap<>();

        for (Map.Entry<String, ?> entry : indexedFields.entrySet()) {
            String fieldName = ClickHouseFieldTypeMapper.validateFieldName(entry.getKey());
            String clickHouseType = ClickHouseFieldTypeMapper.toClickHouseType(String.valueOf(entry.getValue()));

            typedFields.put(fieldName, clickHouseType);
        }

        return typedFields;
    }
}
