/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.schema;

import com.bytechef.ee.platform.contextstore.clickhouse.ClickHouseTableMigrator;
import com.bytechef.ee.platform.contextstore.clickhouse.IndexedFieldChange;
import com.bytechef.ee.platform.contextstore.clickhouse.MigrationResult;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies {@link IndexedFieldChange}s to an existing per-source ClickHouse table via {@code ALTER TABLE} statements.
 * Activates only when the {@code clickHouseDataSource} bean is wired (same gate as
 * {@link ClickHouseTableProvisionerImpl}).
 *
 * <p>
 * Idempotency: all statements use {@code IF NOT EXISTS} / {@code IF EXISTS} so a partial-failure retry succeeds without
 * manual cleanup. Empty change list is a no-op returning {@link MigrationResult#NONE}.
 * </p>
 *
 * <p>
 * Type-change handling: {@code TypeChange} first attempts {@code MODIFY COLUMN} (lossless when ClickHouse accepts the
 * conversion). On {@link DataAccessException} it falls back to {@code DROP COLUMN IF EXISTS} +
 * {@code ADD COLUMN IF NOT EXISTS}, surfacing the lossy outcome in {@link MigrationResult#lossyTypeChanges()}. The JSON
 * {@code _payload} preserves the field's value through this — only the typed projection resets to NULL.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnBean(name = "clickHouseDataSource")
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ClickHouseTableMigratorImpl implements ClickHouseTableMigrator {

    static final String METRIC_MIGRATION = "bytechef_context_store_clickhouse_schema_migration";

    static final String METRIC_CHANGE = "bytechef_context_store_clickhouse_schema_change";

    private static final Logger log = LoggerFactory.getLogger(ClickHouseTableMigratorImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    @SuppressFBWarnings("EI2")
    public ClickHouseTableMigratorImpl(
        @Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC",
        justification = "Table name comes from source.getClickhouseTableName() which was sanitised at "
            + "source-creation time by ClickHouseTableNameSanitizer; re-validated by resolveTableName(). Field names "
            + "come through ClickHouseFieldTypeMapper.validateFieldName() which enforces the strict regex. Field "
            + "types come through ClickHouseFieldTypeMapper.toClickHouseType() which constrains output to a closed "
            + "set of ClickHouse type literals.")
    public MigrationResult applyChanges(ContextStoreSource source, List<IndexedFieldChange> changes) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(changes, "changes");

        if (changes.isEmpty()) {
            return MigrationResult.NONE;
        }

        String tableName = resolveTableName(source);

        int adds = 0;
        int drops = 0;
        int compatibleTypeChanges = 0;
        int lossyTypeChanges = 0;

        for (IndexedFieldChange change : changes) {
            switch (change) {
                case IndexedFieldChange.Add add -> {
                    String fieldName = ClickHouseFieldTypeMapper.validateFieldName(add.fieldName());
                    String clickHouseType = ClickHouseFieldTypeMapper.toClickHouseType(add.fieldType());

                    jdbcTemplate.execute(
                        "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + fieldName
                            + " Nullable(" + clickHouseType + ")");

                    adds++;

                    recordChangeMetric("ADD");
                }
                case IndexedFieldChange.Drop drop -> {
                    String fieldName = ClickHouseFieldTypeMapper.validateFieldName(drop.fieldName());

                    jdbcTemplate.execute(
                        "ALTER TABLE " + tableName + " DROP COLUMN IF EXISTS " + fieldName);

                    drops++;

                    recordChangeMetric("DROP");
                }
                case IndexedFieldChange.TypeChange typeChange -> {
                    String fieldName = ClickHouseFieldTypeMapper.validateFieldName(typeChange.fieldName());
                    String newClickHouseType = ClickHouseFieldTypeMapper.toClickHouseType(typeChange.newFieldType());

                    try {
                        jdbcTemplate.execute(
                            "ALTER TABLE " + tableName + " MODIFY COLUMN " + fieldName
                                + " Nullable(" + newClickHouseType + ")");

                        compatibleTypeChanges++;

                        recordChangeMetric("TYPE_COMPATIBLE");
                    } catch (DataAccessException compatibleAttemptFailure) {
                        log.warn(
                            "MODIFY COLUMN for {}.{} from {} to {} failed (type incompatibility?); falling back to "
                                + "DROP + ADD which resets the typed projection. Cause: {}",
                            tableName, fieldName, typeChange.oldFieldType(), typeChange.newFieldType(),
                            compatibleAttemptFailure.getMessage());

                        jdbcTemplate.execute(
                            "ALTER TABLE " + tableName + " DROP COLUMN IF EXISTS " + fieldName);
                        jdbcTemplate.execute(
                            "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + fieldName
                                + " Nullable(" + newClickHouseType + ")");

                        lossyTypeChanges++;

                        recordChangeMetric("TYPE_LOSSY");
                    }
                }
            }
        }

        MigrationResult result = new MigrationResult(adds, drops, compatibleTypeChanges, lossyTypeChanges);

        recordMigrationMetric(lossyTypeChanges > 0 ? "partial" : "success");

        log.info("Applied ClickHouse schema migration on {}: {}", tableName, result);

        return result;
    }

    private String resolveTableName(ContextStoreSource source) {
        String tableName = source.getClickhouseTableName();

        if (tableName == null) {
            throw new IllegalStateException(
                "Source " + source.getId() + " has no clickhouseTableName set — refusing to migrate. Either the "
                    + "deployment is Postgres-only (and the caller shouldn't be reaching the migrator) or the "
                    + "source-creation flow had a bug.");
        }

        if (!tableName.matches(ClickHouseTableNameSanitizer.VALID_TABLE_NAME_REGEX)) {
            throw new IllegalStateException(
                "Source " + source.getId() + " has a clickhouseTableName ('" + tableName
                    + "') that fails the sanitiser regex; refusing to interpolate into a SQL string.");
        }

        return tableName;
    }

    private void recordMigrationMetric(String outcome) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();

        if (registry == null) {
            return;
        }

        registry.counter(METRIC_MIGRATION, "outcome", outcome)
            .increment();
    }

    private void recordChangeMetric(String change) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();

        if (registry == null) {
            return;
        }

        registry.counter(METRIC_CHANGE, "change", change)
            .increment();
    }
}
