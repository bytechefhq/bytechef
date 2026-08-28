/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.data.table.configuration.service;

import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.data.table.configuration.audit.DataTableAuditEvent;
import com.bytechef.platform.data.table.configuration.audit.DataTableAuditPublisher;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.owner.Owner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableServiceImpl implements DataTableService {

    private static final Logger log = LoggerFactory.getLogger(DataTableServiceImpl.class);

    private final DataTableAuditPublisher dataTableAuditPublisher;
    private final DataTableRepository dataTableRepository;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableServiceImpl(
        DataTableAuditPublisher dataTableAuditPublisher, DataTableRepository dataTableRepository,
        JdbcTemplate jdbcTemplate) {

        this.dataTableAuditPublisher = dataTableAuditPublisher;
        this.dataTableRepository = dataTableRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Adds a column to an existing data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void addColumn(String baseName, ColumnSpec columnSpec, long environmentId) {
        validateBaseName(baseName);
        Assert.notNull(columnSpec, "column must not be null");

        String physicalName = buildPhysicalName(environmentId, baseName);

        String sql = "ALTER TABLE " + escapeIdentifier(physicalName) + " ADD COLUMN " +
            escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type());

        jdbcTemplate.execute(sql);

        long dataTableId = getIdByBaseName(baseName);

        dataTableAuditPublisher.publish(
            DataTableAuditEvent.DATA_TABLE_COLUMN_ADDED, dataTableId, Map.of("columnName", columnSpec.name()));
    }

    @Override
    public void createTable(String baseName, List<ColumnSpec> columnSpecs, long environmentId) {
        createTable(baseName, null, columnSpecs, environmentId);
    }

    /**
     * Creates a new data table with the specified columns.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @Transactional
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long environmentId) {

        validateBaseName(baseName);

        Assert.notEmpty(columnSpecs, "columns must not be empty");

        boolean hasReservedColumn = columnSpecs.stream()
            .anyMatch(columnSpec -> ReservedColumns.isReserved(columnSpec.name()));

        Assert.isTrue(!hasReservedColumn, "Column names " + ReservedColumns.all() + " are reserved");

        String userColsSql = columnSpecs.stream()
            .map(columnSpec -> escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type()))
            .collect(Collectors.joining(", "));

        String physicalName = buildPhysicalName(environmentId, baseName);

        String sql = "CREATE TABLE " + escapeIdentifier(physicalName) +
            " (\"id\" BIGSERIAL PRIMARY KEY, \"owner_id\" BIGINT, \"owner_type\" INT" +
            (userColsSql.isEmpty() ? "" : ", " + userColsSql) + ")";

        jdbcTemplate.execute(sql);

        jdbcTemplate.execute(
            "CREATE INDEX " + escapeIdentifier("idx_" + physicalName + "_owner") + " ON " +
                escapeIdentifier(physicalName) + " (\"owner_type\", \"owner_id\")");

        long dataTableId = register(baseName, description);

        dataTableAuditPublisher.publish(
            DataTableAuditEvent.DATA_TABLE_CREATED, dataTableId, Map.of("name", baseName));
    }

    /**
     * Drops an existing data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void dropTable(String baseName, long environmentId) {
        validateBaseName(baseName);

        String physicalName = buildPhysicalName(environmentId, baseName);

        String sql = "DROP TABLE IF EXISTS " + escapeIdentifier(physicalName);

        jdbcTemplate.execute(sql);

        if (!hasPhysicalTablesForBaseName(baseName)) {
            Long dataTableId = dataTableRepository.findByName(baseName)
                .map(DataTable::getId)
                .orElse(null);

            dataTableRepository.deleteByName(baseName);

            if (dataTableId != null) {
                dataTableAuditPublisher.publish(DataTableAuditEvent.DATA_TABLE_DELETED, dataTableId, Map.of());
            }
        }
    }

    /**
     * Duplicates an existing data table to a new table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @Transactional
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void duplicateTable(String fromBaseName, String toBaseName, long environmentId) {
        validateBaseName(fromBaseName);
        validateBaseName(toBaseName);

        String fromPhysicalName = buildPhysicalName(environmentId, fromBaseName);
        String toPhysicalName = buildPhysicalName(environmentId, toBaseName);

        List<ColumnSpec> columnSpecs = listColumns(fromPhysicalName)
            .stream()
            .filter(columnSpec -> !ReservedColumns.isReserved(columnSpec.name()))
            .toList();

        String userColumnsSql = columnSpecs.stream()
            .map(columnSpec -> escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type()))
            .collect(Collectors.joining(", "));

        String createSql = "CREATE TABLE " + escapeIdentifier(toPhysicalName) +
            " (\"id\" BIGSERIAL PRIMARY KEY, \"owner_id\" BIGINT, \"owner_type\" INT" +
            (userColumnsSql.isEmpty() ? "" : ", " + userColumnsSql) + ")";

        jdbcTemplate.execute(createSql);

        jdbcTemplate.execute(
            "CREATE INDEX " + escapeIdentifier("idx_" + toPhysicalName + "_owner") + " ON " +
                escapeIdentifier(toPhysicalName) + " (\"owner_type\", \"owner_id\")");

        if (!columnSpecs.isEmpty()) {
            String columnList = columnSpecs.stream()
                .map(columnSpec -> escapeIdentifier(columnSpec.name()))
                .collect(Collectors.joining(", "));

            String insertSql = "INSERT INTO " + escapeIdentifier(toPhysicalName) + " (" + columnList + ") SELECT " +
                columnList + " FROM " + escapeIdentifier(fromPhysicalName);

            jdbcTemplate.execute(insertSql);
        }

        register(toBaseName, dataTableRepository.findByName(fromBaseName)
            .map(DataTable::getDescription)
            .orElse(null));
    }

    @Override
    public String getBaseNameById(long id) {
        DataTable dataTable = dataTableRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Data table with id=" + id + " not found"));

        return dataTable.getName();
    }

    @Override
    public long getIdByBaseName(String baseName) {
        DataTable dataTable = dataTableRepository.findByName(baseName)
            .orElseThrow(() -> new ExecutionException(
                "Unable to find table " + baseName, DataTableErrorType.DATA_TABLE_NOT_FOUND));

        return dataTable.getId();
    }

    @Override
    public List<DataTableInfo> listTables(long environmentId) {
        return listTables(environmentId, Optional.empty());
    }

    /**
     * The same visibility rule the rows follow, one level up: an unowned table belongs to the vendor and is everyone's,
     * and a caller with no owner is an admin or an automation caller and sees everything.
     *
     * <p>
     * The filter runs on the registry row before the column lookup, so a table the caller cannot see costs no
     * {@code information_schema} work.
     */
    @Override
    public List<DataTableInfo> listTables(long environmentId, Optional<Owner> owner) {
        String prefix = "dt_" + environmentId + "_";

        String sqlTables = "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name LIKE ?";

        List<String> tableNames = jdbcTemplate.query(sqlTables, ps -> ps.setString(1, prefix + "%"),
            (rs, rowNum) -> rs.getString("table_name"));

        List<DataTableInfo> dataTableInfos = new ArrayList<>();

        for (String tableName : tableNames) {
            if (!tableName.startsWith(prefix)) {
                continue;
            }

            String baseName = tableName.substring(prefix.length());

            DataTable dataTable = dataTableRepository.findByName(baseName)
                .orElse(null);

            if (dataTable == null) {
                log.warn("Skipping unregistered physical data table '{}' in environment {}", baseName, environmentId);

                continue;
            }

            if (!isReadableBy(dataTable, owner)) {
                continue;
            }

            List<ColumnSpec> columnSpecs = listColumns(tableName)
                .stream()
                .filter(columnSpec -> !ReservedColumns.isReserved(columnSpec.name()))
                .toList();

            dataTableInfos.add(
                new DataTableInfo(
                    dataTable.getId(), baseName, dataTable.getDescription(), columnSpecs,
                    dataTable.getLastModifiedDate()));
        }

        return dataTableInfos;
    }

    @Override
    @Transactional
    public void assignOwner(long dataTableId, @Nullable Owner owner) {
        DataTable dataTable = dataTableRepository.findById(dataTableId)
            .orElseThrow(() -> new IllegalArgumentException("Data table not found: " + dataTableId));

        dataTable.setOwnerId(owner == null ? null : owner.id());
        dataTable.setOwnerType(owner == null ? null : owner.type());

        dataTableRepository.save(dataTable);
    }

    /**
     * Whether {@code owner} may see this table at all. Mirrors {@code KnowledgeBaseServiceImpl.isReadableBy}
     * deliberately: two stores, one rule, and a caller must not be able to tell "somebody else's" from "does not
     * exist".
     *
     * <p>
     * An owner id with no type is treated as owned rather than unowned, so a half-written owner fails closed instead of
     * quietly sharing the table with every account.
     */
    static boolean isReadableBy(DataTable dataTable, Optional<Owner> owner) {
        Long ownerId = dataTable.getOwnerId();

        if (ownerId == null || owner.isEmpty()) {
            return true;
        }

        Owner currentOwner = owner.get();

        return currentOwner.id() == ownerId && currentOwner.type() == dataTable.getOwnerType();
    }

    /**
     * Removes a column from an existing data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void removeColumn(String baseName, String columnName, long environmentId) {
        validateBaseName(baseName);
        Assert.hasText(columnName, "columnName must not be empty");

        String physicalName = buildPhysicalName(environmentId, baseName);

        String sql = "ALTER TABLE " + escapeIdentifier(physicalName) + " DROP COLUMN " + escapeIdentifier(columnName);

        jdbcTemplate.execute(sql);
    }

    /**
     * Renames a column in an existing data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void renameColumn(String baseName, String fromColumnName, String toColumnName, long environmentId) {
        validateBaseName(baseName);
        Assert.hasText(fromColumnName, "fromColumnName must not be empty");
        Assert.hasText(toColumnName, "toColumnName must not be empty");
        Assert.isTrue(!ReservedColumns.isReserved(fromColumnName), "Reserved columns cannot be renamed");
        Assert.isTrue(!ReservedColumns.isReserved(toColumnName), "Cannot rename to a reserved name");

        String physicalName = buildPhysicalName(environmentId, baseName);

        String sql = "ALTER TABLE " + escapeIdentifier(physicalName) + " RENAME COLUMN " +
            escapeIdentifier(fromColumnName) + " TO " + escapeIdentifier(toColumnName);

        jdbcTemplate.execute(sql);
    }

    /**
     * Renames an existing data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because all identifiers are validated
     * through {@link #escapeIdentifier(String)} and {@link #validateBaseName(String)} which enforce a strict allowlist
     * pattern {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void renameTable(String fromBaseName, String toBaseName, long environmentId) {
        validateBaseName(fromBaseName);
        validateBaseName(toBaseName);

        String fromPhysicalName = buildPhysicalName(environmentId, fromBaseName);
        String toPhysicalName = buildPhysicalName(environmentId, toBaseName);

        String sql = "ALTER TABLE " + escapeIdentifier(fromPhysicalName) + " RENAME TO " +
            escapeIdentifier(toPhysicalName);

        jdbcTemplate.execute(sql);

        DataTable dataTable = dataTableRepository.findByName(fromBaseName)
            .orElseThrow(() -> new IllegalArgumentException("Data table '" + fromBaseName + "' not found"));

        dataTable.setName(toBaseName);

        dataTableRepository.save(dataTable);
    }

    private String buildPhysicalName(long environmentId, String baseName) {
        String normalizedBaseName = baseName.toLowerCase(Locale.ROOT);

        return "dt_" + environmentId + "_" + normalizedBaseName;
    }

    private boolean hasPhysicalTablesForBaseName(String baseName) {
        String normalizedBaseName = baseName.toLowerCase(Locale.ROOT);
        String escapedBaseName = normalizedBaseName.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
        String pattern = "dt\\_%\\_" + escapedBaseName;

        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
            "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name LIKE ? ESCAPE '\\'";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, pattern);

        if (count == null) {
            throw new IllegalStateException(
                "Unexpected null result from COUNT(*) query for base name: " + baseName);
        }

        return count > 0;
    }

    /**
     * Writes the {@code data_table} row that gives a physical table its id. Nothing else creates one, and everything
     * keyed on a data table id -- tags, webhooks, the workspace relation, {@code listTables} -- needs it to exist.
     *
     * <p>
     * Called after the DDL and inside the same transaction, so a failed CREATE leaves no registry row and a failed
     * insert leaves no physical table.
     */
    private long register(String baseName, @Nullable String description) {
        Assert.hasText(baseName, "baseName required");

        DataTable dataTable = new DataTable();

        dataTable.setName(baseName);
        dataTable.setDescription(description);

        DataTable savedDataTable = dataTableRepository.save(dataTable);

        return savedDataTable.getId();
    }

    private String escapeIdentifier(String identifier) {
        Assert.hasText(identifier, "identifier must not be empty");

        String normalized = identifier.toLowerCase(Locale.ROOT);

        Assert.isTrue(normalized.matches("[a-z_][a-z0-9_]*"), "Invalid identifier: " + identifier);

        return '"' + normalized + '"';
    }

    private List<ColumnSpec> listColumns(String physicalName) {
        String sql = "SELECT column_name, data_type FROM information_schema.columns " +
            "WHERE table_schema = current_schema() AND table_name = ? ORDER BY ordinal_position";

        return jdbcTemplate.query(sql, ps -> ps.setString(1, physicalName), (rs, rowNum) -> {
            String name = rs.getString("column_name");
            String dataType = rs.getString("data_type");

            return new ColumnSpec(name, mapType(dataType));
        });
    }

    private ColumnType mapType(String pgType) {
        String lowerCaseType = pgType.toLowerCase(Locale.ROOT);

        if (lowerCaseType.startsWith("timestamp")) {
            return ColumnType.DATE_TIME;
        }

        if (lowerCaseType.equals("boolean") || lowerCaseType.equals("bool")) {
            return ColumnType.BOOLEAN;
        }

        switch (lowerCaseType) {
            case "integer", "int4", "smallint", "int2", "bigint", "int8", "serial", "bigserial" -> {
                return ColumnType.INTEGER;
            }
            case "numeric", "decimal", "double precision", "real" -> {
                return ColumnType.NUMBER;
            }
            case "date" -> {
                return ColumnType.DATE;
            }
            default -> {
                return ColumnType.STRING;
            }
        }
    }

    private String sqlType(ColumnType type) {
        return switch (type) {
            case STRING -> "VARCHAR(255)";
            case NUMBER -> "DECIMAL(38,9)";
            case INTEGER -> "INTEGER";
            case DATE -> "DATE";
            case DATE_TIME -> "TIMESTAMP";
            case BOOLEAN -> "BOOLEAN";
        };
    }

    private void validateBaseName(String baseName) {
        Assert.hasText(baseName, "baseName must not be empty");

        String normalized = baseName.toLowerCase(Locale.ROOT);

        Assert.isTrue(!normalized.startsWith("dt_"), "baseName must not start with 'dt_'");
        Assert.isTrue(normalized.matches("[a-z_][a-z0-9_]*"), "Invalid base name: " + baseName);
    }
}
