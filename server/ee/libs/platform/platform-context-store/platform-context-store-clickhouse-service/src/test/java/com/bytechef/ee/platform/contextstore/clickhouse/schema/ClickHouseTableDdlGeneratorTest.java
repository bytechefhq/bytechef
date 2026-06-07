/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the deterministic DDL shape for typed indexed fields plus the safety rejections (dotted paths, leading-digit
 * names, unsupported types).
 *
 * @author Ivica Cardic
 * @version ee
 */
class ClickHouseTableDdlGeneratorTest {

    @Test
    void testAllThreeTypeMappingsRenderInAlphabeticalOrder() {
        ContextStoreSource source = sourceWith(orderedFields(
            "title", "TEXT",
            "amount", "NUMERIC",
            "updated_at", "TIMESTAMP"));

        String ddl = ClickHouseTableDdlGenerator.createTableDdl("cs_w1_s1_orders", source);

        assertThat(ddl).contains("CREATE TABLE cs_w1_s1_orders (");
        assertThat(ddl).contains("_id String,");
        assertThat(ddl).contains("_payload_hash String,");
        assertThat(ddl).contains("_payload String,");
        assertThat(ddl).contains("_last_seen_at DateTime64(3),");
        assertThat(ddl).contains("_deleted_at Nullable(DateTime64(3))");
        assertThat(ddl).contains("title Nullable(String)");
        assertThat(ddl).contains("amount Nullable(Float64)");
        assertThat(ddl).contains("updated_at Nullable(DateTime64(3))");
        assertThat(ddl).contains("ENGINE = ReplacingMergeTree(_last_seen_at) ORDER BY (_id)");
        // DDL output is deterministic — fields render alphabetically regardless of insertion order so migration
        // diffs stay reviewable and tests stay stable across runs.
        assertThat(ddl.indexOf("amount"))
            .isLessThan(ddl.indexOf("title"));
        assertThat(ddl.indexOf("title"))
            .isLessThan(ddl.indexOf("updated_at"));
    }

    @Test
    void testEmptyIndexedFieldsRendersBaseSkeleton() {
        ContextStoreSource source = sourceWith(Map.of());

        String ddl = ClickHouseTableDdlGenerator.createTableDdl("cs_w1_s1_empty", source);

        assertThat(ddl).contains("_id String");
        assertThat(ddl).contains("_payload String");
        assertThat(ddl).doesNotContain("Nullable(Float64)");
        assertThat(ddl).doesNotContain("Nullable(String),\n)");
        assertThat(ddl).endsWith("ENGINE = ReplacingMergeTree(_last_seen_at) ORDER BY (_id)");
    }

    @Test
    void testUnknownFieldTypeIsRejected() {
        ContextStoreSource source = sourceWith(Map.of("payload", "BLOB"));

        assertThatThrownBy(() -> ClickHouseTableDdlGenerator.createTableDdl("cs_w1_s1_t", source))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("BLOB")
            .hasMessageContaining("TEXT, NUMERIC, TIMESTAMP");
    }

    @Test
    void testDottedFieldNameIsRejectedWithInjectionSafetyMessage() {
        ContextStoreSource source = sourceWith(Map.of("company.name", "TEXT"));

        assertThatThrownBy(() -> ClickHouseTableDdlGenerator.createTableDdl("cs_w1_s1_t", source))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("company.name")
            .hasMessageContaining("dotted paths");
    }

    @Test
    void testFieldNameStartingWithDigitIsRejected() {
        ContextStoreSource source = sourceWith(Map.of("1st_amount", "NUMERIC"));

        assertThatThrownBy(() -> ClickHouseTableDdlGenerator.createTableDdl("cs_w1_s1_t", source))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("1st_amount");
    }

    @Test
    void testMixedCaseFieldNameIsNormalisedToLower() {
        ContextStoreSource source = sourceWith(Map.of("CamelCase", "TEXT"));

        String ddl = ClickHouseTableDdlGenerator.createTableDdl("cs_w1_s1_t", source);

        assertThat(ddl).contains("camelcase Nullable(String)");
        assertThat(ddl).doesNotContain("CamelCase");
    }

    private static ContextStoreSource sourceWith(Map<String, ?> indexedFields) {
        ContextStoreSource source = new ContextStoreSource();

        source.setIndexedFields(indexedFields);

        return source;
    }

    private static Map<String, String> orderedFields(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }

        return map;
    }
}
