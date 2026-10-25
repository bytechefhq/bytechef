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
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.DataTableResolution;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import com.bytechef.platform.data.table.internal.PhysicalTableNaming;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Pattern PHYSICAL_NAME = Pattern.compile("^dt_(\\d++)_([a-z_][a-z0-9_]*+)$");

    private final DataTableRepository dataTableRepository;
    private final DataTableStorageService dataTableStorageService;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableServiceImpl(
        DataTableRepository dataTableRepository, DataTableStorageService dataTableStorageService,
        JdbcTemplate jdbcTemplate) {

        this.dataTableRepository = dataTableRepository;
        this.dataTableStorageService = dataTableStorageService;
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
        validateColumnName(columnSpec.name());

        getDataTable(baseName);

        DataTableRef dataTableRef = new DataTableRef(baseName, environmentId);

        if (hasColumn(dataTableRef.physicalName(), columnSpec.name())) {
            throw new DataTableException(
                "Column '" + columnSpec.name() + "' already exists on table '" + baseName + "'",
                DataTableErrorType.COLUMN_ALREADY_EXISTS);
        }

        String sql = "ALTER TABLE " + escapeIdentifier(dataTableRef.physicalName()) + " ADD COLUMN " +
            escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type());

        jdbcTemplate.execute(sql);
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

        for (ColumnSpec columnSpec : columnSpecs) {
            validateColumnName(columnSpec.name());
        }

        DataTableRef dataTableRef = new DataTableRef(baseName, environmentId);

        if (physicalTableExists(dataTableRef.physicalName())) {
            throw new DataTableException(
                "Data table '" + baseName + "' already exists in this environment",
                DataTableErrorType.DATA_TABLE_ALREADY_EXISTS);
        }

        createPhysicalTable(dataTableRef.physicalName(), columnSpecs);

        register(baseName, description);
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

        Optional<DataTable> dataTableOptional = fetchDataTable(baseName);

        if (dataTableOptional.isEmpty()) {
            return;
        }

        DataTable dataTable = dataTableOptional.get();

        DataTableRef dataTableRef = new DataTableRef(baseName, environmentId);

        String sql = "DROP TABLE IF EXISTS " + escapeIdentifier(dataTableRef.physicalName());

        jdbcTemplate.execute(sql);

        if (!hasPhysicalTablesForBaseName(baseName)) {
            dataTableRepository.deleteById(dataTable.getId());
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
    public void duplicateTable(
        String fromBaseName, String toBaseName, long environmentId) {

        validateBaseName(fromBaseName);
        validateBaseName(toBaseName);

        DataTable fromDataTable = getDataTable(fromBaseName);

        DataTableRef fromDataTableRef = new DataTableRef(fromBaseName, environmentId);
        DataTableRef toDataTableRef = new DataTableRef(toBaseName, environmentId);

        String fromPhysicalName = fromDataTableRef.physicalName();
        String toPhysicalName = toDataTableRef.physicalName();

        List<ColumnSpec> columnSpecs = listColumns(fromPhysicalName)
            .stream()
            .filter(columnSpec -> !ReservedColumns.isReserved(columnSpec.name()))
            .toList();

        dataTableStorageService.checkWithinLimit(dataTableStorageService.getTableBytes(fromPhysicalName));

        createPhysicalTable(toPhysicalName, columnSpecs);

        List<String> copiedColumnNames = new ArrayList<>();

        copiedColumnNames.add(ReservedColumns.EXTERNAL_ID);
        copiedColumnNames.addAll(columnSpecs.stream()
            .map(ColumnSpec::name)
            .toList());

        String columnList = copiedColumnNames.stream()
            .map(DataTableServiceImpl::escapeIdentifier)
            .collect(Collectors.joining(", "));

        String insertSql = "INSERT INTO " + escapeIdentifier(toPhysicalName) + " (" + columnList + ") SELECT " +
            columnList + " FROM " + escapeIdentifier(fromPhysicalName);

        jdbcTemplate.execute(insertSql);

        register(toBaseName, fromDataTable.getDescription());
    }

    @Override
    public String getBaseNameById(long id) {
        DataTable dataTable = dataTableRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Data table with id=" + id + " not found"));

        return dataTable.getName();
    }

    @Override
    public long getIdByBaseName(String baseName) {
        DataTable dataTable = fetchDataTable(baseName)
            .orElseThrow(() -> new ExecutionException(
                "Unable to find table " + baseName, DataTableErrorType.DATA_TABLE_NOT_FOUND));

        return dataTable.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataTable> fetchDataTable(String baseName) {
        String normalizedBaseName = normalizeBaseName(baseName);

        return dataTableRepository.findByName(normalizedBaseName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataTable> fetchDataTable(DataTableRef dataTableRef) {
        String baseName = dataTableRef.baseName();

        return dataTableRepository.findByName(baseName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataTableResolution> fetchDataTableResolution(
        String baseName, long environmentId) {

        return fetchDataTable(baseName)
            .map(
                dataTable -> new DataTableResolution(
                    dataTable.getId(),
                    new DataTableRef(baseName, environmentId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataTableInfo> fetchDataTableInfo(
        String baseName, long environmentId) {

        validateBaseName(baseName);

        Optional<DataTable> dataTableOptional = fetchDataTable(baseName);

        if (dataTableOptional.isEmpty()) {
            return Optional.empty();
        }

        DataTable dataTable = dataTableOptional.get();

        DataTableRef dataTableRef = new DataTableRef(baseName, environmentId);
        String physicalName = dataTableRef.physicalName();

        if (!physicalTableExists(physicalName)) {
            return Optional.empty();
        }

        List<ColumnSpec> columnSpecs = listColumns(physicalName).stream()
            .filter(columnSpec -> !ReservedColumns.isReserved(columnSpec.name()))
            .toList();

        return Optional.of(
            new DataTableInfo(
                dataTable.getId(), dataTable.getName(), dataTable.getDescription(), columnSpecs,
                dataTable.getLastModifiedDate()));
    }

    @Override
    @Transactional
    public void updateDescription(String baseName, @Nullable String description) {
        DataTable dataTable = getDataTable(baseName);

        dataTable.setDescription(description);

        dataTableRepository.save(dataTable);
    }

    private DataTable getDataTable(String baseName) {
        return fetchDataTable(baseName)
            .orElseThrow(() -> new ExecutionException(
                "Unable to find table " + baseName, DataTableErrorType.DATA_TABLE_NOT_FOUND));
    }

    private DataTableRef getDataTableRef(String baseName, long environmentId) {
        getDataTable(baseName);

        return new DataTableRef(baseName, environmentId);
    }

    @Override
    public List<DataTableInfo> listTables(long environmentId) {
        String prefix = PhysicalTableNaming.prefix(environmentId);

        String sqlTables = "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name LIKE ?";

        List<String> tableNames = jdbcTemplate.query(
            sqlTables, ps -> ps.setString(1, prefix + "%"), (rs, rowNum) -> rs.getString("table_name"));

        List<DataTableInfo> dataTableInfos = new ArrayList<>();

        for (String tableName : tableNames) {
            if (!tableName.startsWith(prefix)) {
                continue;
            }

            String baseName = tableName.substring(prefix.length());

            DataTable dataTable = dataTableRepository
                .findByName(baseName)
                .orElse(null);

            if (dataTable == null) {
                log.warn("Skipping unregistered physical data table '{}' in environment {}", baseName, environmentId);

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
    public void removeColumn(
        String baseName, String columnName, long environmentId) {

        validateBaseName(baseName);
        validateColumnName(columnName);

        DataTableRef dataTableRef = getDataTableRef(baseName, environmentId);

        if (!hasColumn(dataTableRef.physicalName(), columnName)) {
            throw new DataTableException(
                "Column '" + columnName + "' not found on table '" + baseName + "'",
                DataTableErrorType.COLUMN_NOT_FOUND);
        }

        String sql = "ALTER TABLE " + escapeIdentifier(dataTableRef.physicalName()) + " DROP COLUMN " +
            escapeIdentifier(columnName);

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
    public void renameColumn(
        String baseName, String fromColumnName, String toColumnName, long environmentId) {

        validateBaseName(baseName);
        validateColumnName(fromColumnName);
        validateColumnName(toColumnName);

        DataTableRef dataTableRef = getDataTableRef(baseName, environmentId);

        if (!hasColumn(dataTableRef.physicalName(), fromColumnName)) {
            throw new DataTableException(
                "Column '" + fromColumnName + "' not found on table '" + baseName + "'",
                DataTableErrorType.COLUMN_NOT_FOUND);
        }

        if (hasColumn(dataTableRef.physicalName(), toColumnName)) {
            throw new DataTableException(
                "Column '" + toColumnName + "' already exists on table '" + baseName + "'",
                DataTableErrorType.COLUMN_ALREADY_EXISTS);
        }

        String sql = "ALTER TABLE " + escapeIdentifier(dataTableRef.physicalName()) + " RENAME COLUMN " +
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
    public void renameTable(
        String fromBaseName, String toBaseName, long environmentId) {

        validateBaseName(fromBaseName);
        validateBaseName(toBaseName);

        DataTable dataTable = getDataTable(fromBaseName);

        DataTableRef fromDataTableRef = new DataTableRef(fromBaseName, environmentId);
        DataTableRef toDataTableRef = new DataTableRef(toBaseName, environmentId);

        String sql = "ALTER TABLE " + escapeIdentifier(fromDataTableRef.physicalName()) + " RENAME TO " +
            escapeIdentifier(toDataTableRef.physicalName());

        jdbcTemplate.execute(sql);

        dataTable.setName(toDataTableRef.baseName());

        dataTableRepository.save(dataTable);
    }

    private boolean hasPhysicalTablesForBaseName(String baseName) {
        List<String> physicalNames = physicalNames(baseName);

        return !physicalNames.isEmpty();
    }

    private List<String> physicalNames(String baseName) {
        String normalizedBaseName = baseName.toLowerCase(Locale.ROOT);
        String escapedBaseName = normalizedBaseName.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
        String pattern = "dt\\_%\\_" + escapedBaseName;

        String sql = "SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name LIKE ? ESCAPE '\\'";

        List<String> tableNames = jdbcTemplate.query(
            sql, ps -> ps.setString(1, pattern), (resultSet, rowNum) -> resultSet.getString("table_name"));

        return tableNames.stream()
            .filter(tableName -> matchesBaseName(tableName, normalizedBaseName))
            .toList();
    }

    private static boolean matchesBaseName(String tableName, String baseName) {
        Matcher matcher = PHYSICAL_NAME.matcher(tableName);

        if (!matcher.matches()) {
            return false;
        }

        return baseName.equals(matcher.group(2));
    }

    private long register(String baseName, @Nullable String description) {
        Assert.hasText(baseName, "baseName required");

        String normalizedBaseName = normalizeBaseName(baseName);

        Optional<DataTable> existingDataTable =
            dataTableRepository.findByName(normalizedBaseName);

        return existingDataTable
            .map(DataTable::getId)
            .orElseGet(() -> {
                DataTable dataTable = new DataTable();

                dataTable.setName(normalizedBaseName);
                dataTable.setDescription(description);

                DataTable savedDataTable = dataTableRepository.save(dataTable);

                return savedDataTable.getId();
            });
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void createPhysicalTable(String physicalName, List<ColumnSpec> columnSpecs) {
        jdbcTemplate.execute(buildCreateTableSql(physicalName, columnSpecs));
        jdbcTemplate.execute(buildExternalIdIndexSql(physicalName));
    }

    static String buildCreateTableSql(String physicalName, List<ColumnSpec> columnSpecs) {
        String userColumnsSql = columnSpecs.stream()
            .map(columnSpec -> escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type()))
            .collect(Collectors.joining(", "));

        return "CREATE TABLE " + escapeIdentifier(physicalName) + " (\"id\" BIGSERIAL PRIMARY KEY, " +
            escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " VARCHAR(255)" +
            (userColumnsSql.isEmpty() ? "" : ", " + userColumnsSql) + ")";
    }

    static String buildExternalIdIndexSql(String physicalName) {
        return "CREATE UNIQUE INDEX ON " + escapeIdentifier(physicalName) + " (" +
            escapeIdentifier(ReservedColumns.EXTERNAL_ID) + ")";
    }

    private static String escapeIdentifier(String identifier) {
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

    private static String sqlType(ColumnType type) {
        return switch (type) {
            case STRING -> "VARCHAR(255)";
            case NUMBER -> "DECIMAL(38,9)";
            case INTEGER -> "INTEGER";
            case DATE -> "DATE";
            case DATE_TIME -> "TIMESTAMP";
            case BOOLEAN -> "BOOLEAN";
        };
    }

    private static String normalizeBaseName(String baseName) {
        return baseName.toLowerCase(Locale.ROOT);
    }

    private boolean physicalTableExists(String physicalName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?",
            Integer.class, physicalName);

        return count != null && count > 0;
    }

    private boolean hasColumn(String physicalName, String columnName) {
        return listColumns(physicalName).stream()
            .anyMatch(columnSpec -> columnSpec.name()
                .equalsIgnoreCase(columnName));
    }

    static void validateBaseName(String baseName) {
        if (baseName == null || baseName.isBlank()) {
            throw new DataTableException("baseName must not be empty", DataTableErrorType.DATA_TABLE_NAME_INVALID);
        }

        String normalized = baseName.toLowerCase(Locale.ROOT);

        if (normalized.startsWith("dt_") || !normalized.matches("[a-z_][a-z0-9_]*")) {
            throw new DataTableException(
                "Invalid base name: " + baseName, DataTableErrorType.DATA_TABLE_NAME_INVALID);
        }
    }

    private static void validateColumnName(String columnName) {
        if (columnName == null || columnName.isBlank() ||
            !columnName.toLowerCase(Locale.ROOT)
                .matches("[a-z_][a-z0-9_]*")) {

            throw new DataTableException(
                "Invalid column name: " + columnName, DataTableErrorType.COLUMN_NAME_INVALID);
        }

        if (ReservedColumns.isReserved(columnName)) {
            throw new DataTableException(
                "Column name '" + columnName + "' is reserved", DataTableErrorType.COLUMN_NAME_INVALID);
        }
    }
}
