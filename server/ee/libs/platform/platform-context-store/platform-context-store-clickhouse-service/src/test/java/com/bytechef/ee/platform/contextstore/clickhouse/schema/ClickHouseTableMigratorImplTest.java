/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.contextstore.clickhouse.IndexedFieldChange;
import com.bytechef.ee.platform.contextstore.clickhouse.MigrationResult;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Pins the migrator's SQL output per change shape, including the MODIFY-fallback path that goes through DROP + ADD when
 * ClickHouse rejects the type conversion. Metrics are exercised against a real SimpleMeterRegistry rather than a mock
 * so the tag-key shape is validated end-to-end.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings(
    value = "SQL_INJECTION_SPRING_JDBC",
    justification = "Test class doesn't execute SQL — Mockito verify() reflection on jdbcTemplate.execute calls "
        + "confuses the SpotBugs taint analysis. The migrator's own SQL safety is enforced at the source class.")
class ClickHouseTableMigratorImplTest {

    private static final String TABLE_NAME = "cs_w1_c5_s42_contacts";

    private JdbcTemplate jdbcTemplate;
    private SimpleMeterRegistry meterRegistry;
    private ClickHouseTableMigratorImpl migrator;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        meterRegistry = new SimpleMeterRegistry();

        @SuppressWarnings("unchecked")
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> registryProvider = mock(ObjectProvider.class);

        when(registryProvider.getIfAvailable()).thenReturn(meterRegistry);

        migrator = new ClickHouseTableMigratorImpl(jdbcTemplate, registryProvider);
    }

    @Test
    void testEmptyChangeListIsNoOp() {
        MigrationResult result = migrator.applyChanges(sourceWithTable(), List.of());

        assertThat(result).isEqualTo(MigrationResult.NONE);
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void testAddEmitsAddColumnIfNotExists() {
        migrator.applyChanges(
            sourceWithTable(), List.of(new IndexedFieldChange.Add("email", "TEXT")));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate, times(1)).execute(sqlCaptor.capture());

        assertThat(sqlCaptor.getValue())
            .isEqualTo("ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS email Nullable(String)");
        assertCounter(ClickHouseTableMigratorImpl.METRIC_CHANGE, "change", "ADD", 1);
        assertCounter(ClickHouseTableMigratorImpl.METRIC_MIGRATION, "outcome", "success", 1);
    }

    @Test
    void testDropEmitsDropColumnIfExists() {
        migrator.applyChanges(
            sourceWithTable(), List.of(new IndexedFieldChange.Drop("legacy")));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate, times(1)).execute(sqlCaptor.capture());

        assertThat(sqlCaptor.getValue())
            .isEqualTo("ALTER TABLE " + TABLE_NAME + " DROP COLUMN IF EXISTS legacy");
        assertCounter(ClickHouseTableMigratorImpl.METRIC_CHANGE, "change", "DROP", 1);
    }

    @Test
    void testCompatibleTypeChangeUsesModifyColumn() {
        migrator.applyChanges(
            sourceWithTable(),
            List.of(new IndexedFieldChange.TypeChange("amount", "TEXT", "NUMERIC")));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate, times(1)).execute(sqlCaptor.capture());

        assertThat(sqlCaptor.getValue())
            .isEqualTo("ALTER TABLE " + TABLE_NAME + " MODIFY COLUMN amount Nullable(Float64)");
        assertCounter(ClickHouseTableMigratorImpl.METRIC_CHANGE, "change", "TYPE_COMPATIBLE", 1);
        assertCounter(ClickHouseTableMigratorImpl.METRIC_MIGRATION, "outcome", "success", 1);
    }

    @Test
    void testLossyTypeChangeFallsBackToDropAndAdd() {
        // MODIFY COLUMN rejected → migrator catches DataAccessException and runs DROP + ADD instead.
        doThrow(new DataAccessResourceFailureException("incompatible type conversion"))
            .when(jdbcTemplate)
            .execute("ALTER TABLE " + TABLE_NAME + " MODIFY COLUMN amount Nullable(DateTime64(3))");

        migrator.applyChanges(
            sourceWithTable(),
            List.of(new IndexedFieldChange.TypeChange("amount", "NUMERIC", "TIMESTAMP")));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate, times(3)).execute(sqlCaptor.capture());

        List<String> emitted = sqlCaptor.getAllValues();

        assertThat(emitted.get(0))
            .isEqualTo("ALTER TABLE " + TABLE_NAME + " MODIFY COLUMN amount Nullable(DateTime64(3))");
        assertThat(emitted.get(1))
            .isEqualTo("ALTER TABLE " + TABLE_NAME + " DROP COLUMN IF EXISTS amount");
        assertThat(emitted.get(2))
            .isEqualTo("ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS amount Nullable(DateTime64(3))");
        assertCounter(ClickHouseTableMigratorImpl.METRIC_CHANGE, "change", "TYPE_LOSSY", 1);
        assertCounter(ClickHouseTableMigratorImpl.METRIC_MIGRATION, "outcome", "partial", 1);
    }

    @Test
    void testMixedChangesAccumulateCounts() {
        MigrationResult result = migrator.applyChanges(
            sourceWithTable(),
            List.of(
                new IndexedFieldChange.Add("new1", "TEXT"),
                new IndexedFieldChange.Add("new2", "NUMERIC"),
                new IndexedFieldChange.Drop("old1"),
                new IndexedFieldChange.TypeChange("changed", "TEXT", "NUMERIC")));

        assertThat(result.adds()).isEqualTo(2);
        assertThat(result.drops()).isEqualTo(1);
        assertThat(result.compatibleTypeChanges()).isEqualTo(1);
        assertThat(result.lossyTypeChanges()).isZero();
        assertThat(result.total()).isEqualTo(4);
    }

    @Test
    void testSourceWithoutClickhouseTableNameThrows() {
        ContextStoreSource orphan = new ContextStoreSource();

        orphan.setEntityName("orphan");
        orphan.setIdField("id");
        orphan.setIndexedFields(Map.of());
        orphan.setClickhouseTableName(null);

        assertThatThrownBy(() -> migrator.applyChanges(orphan, List.of(new IndexedFieldChange.Add("x", "TEXT"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no clickhouseTableName");
    }

    @Test
    void testSourceWithInvalidTableNameRefusesToInterpolate() {
        ContextStoreSource bogus = new ContextStoreSource();

        bogus.setEntityName("bogus");
        bogus.setIdField("id");
        bogus.setIndexedFields(Map.of());
        bogus.setClickhouseTableName("DROP TABLE other_table; --");

        assertThatThrownBy(() -> migrator.applyChanges(bogus, List.of(new IndexedFieldChange.Add("x", "TEXT"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("fails the sanitiser regex");
    }

    @Test
    void testInvalidFieldNameRejectedBeforeSqlEmitted() {
        assertThatThrownBy(() -> migrator.applyChanges(
            sourceWithTable(),
            List.of(new IndexedFieldChange.Add("company.name", "TEXT"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company.name")
                .hasMessageContaining("dotted paths");
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void testUnsupportedFieldTypeRejected() {
        assertThatThrownBy(() -> migrator.applyChanges(
            sourceWithTable(),
            List.of(new IndexedFieldChange.Add("x", "BLOB"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BLOB");
    }

    private ContextStoreSource sourceWithTable() {
        ContextStoreSource source = new ContextStoreSource();

        source.setEntityName("contacts");
        source.setIdField("id");
        source.setIndexedFields(Map.of());
        source.setClickhouseTableName(TABLE_NAME);

        return source;
    }

    private void assertCounter(String name, String tagKey, String tagValue, double expected) {
        double actual = meterRegistry.counter(name, tagKey, tagValue)
            .count();

        assertThat(actual).isEqualTo(expected);
    }
}
