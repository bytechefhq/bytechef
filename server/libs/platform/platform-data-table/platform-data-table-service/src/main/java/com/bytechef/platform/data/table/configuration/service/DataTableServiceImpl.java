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

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableNames;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableServiceImpl implements DataTableService {

    private final DataTableRepository dataTableRepository;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableServiceImpl(DataTableRepository dataTableRepository, JdbcTemplate jdbcTemplate) {
        this.dataTableRepository = dataTableRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Adds a column to an existing data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because every physical name is built by
     * {@link DataTableRef} from numeric ids, table names are validated by {@link DataTableNames}, and every identifier
     * passes through {@link #escapeIdentifier(String)}, which enforces the strict allowlist pattern
     * {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId) {
        Assert.notNull(columnSpec, "column must not be null");
        validateColumnName(columnSpec.name());

        DataTable dataTable = getDataTable(dataTableId);

        String physicalName = existingPhysicalName(dataTable, environmentId);

        if (hasColumn(physicalName, columnSpec.name())) {
            throw new DataTableException(
                "Column '" + columnSpec.name() + "' already exists on table '" + dataTable.getName() + "'",
                DataTableErrorType.COLUMN_ALREADY_EXISTS);
        }

        jdbcTemplate.execute(
            "ALTER TABLE " + escapeIdentifier(physicalName) + " ADD COLUMN " + escapeIdentifier(columnSpec.name()) +
                " " + sqlType(columnSpec.type()));
    }

    /**
     * Creates a new data table with the specified columns.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because every physical name is built by
     * {@link DataTableRef} from numeric ids, table names are validated by {@link DataTableNames}, and every identifier
     * passes through {@link #escapeIdentifier(String)}, which enforces the strict allowlist pattern
     * {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @Transactional
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public long createTable(
        @Nullable Long workspaceId, String name, @Nullable String description, List<ColumnSpec> columnSpecs,
        long environmentId) {

        String normalizedName = DataTableNames.normalize(name);

        Assert.notEmpty(columnSpecs, "columns must not be empty");

        for (ColumnSpec columnSpec : columnSpecs) {
            validateColumnName(columnSpec.name());
        }

        DataTable dataTable = fetchDataTable(workspaceId, normalizedName)
            .orElseGet(() -> insertDataTable(workspaceId, normalizedName, description));

        DataTableRef dataTableRef = new DataTableRef(dataTable.getId(), environmentId);

        if (physicalTableExists(dataTableRef.physicalName())) {
            throw alreadyExists(normalizedName);
        }

        createPhysicalTable(dataTableRef.physicalName(), columnSpecs);

        return dataTable.getId();
    }

    /**
     * Drops an existing data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because every physical name is built by
     * {@link DataTableRef} from numeric ids, table names are validated by {@link DataTableNames}, and every identifier
     * passes through {@link #escapeIdentifier(String)}, which enforces the strict allowlist pattern
     * {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @Transactional
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void dropTable(long dataTableId, long environmentId) {
        Optional<DataTable> dataTableOptional = dataTableRepository.findById(dataTableId);

        if (dataTableOptional.isEmpty()) {
            return;
        }

        DataTableRef dataTableRef = new DataTableRef(dataTableId, environmentId);

        jdbcTemplate.execute("DROP TABLE IF EXISTS " + escapeIdentifier(dataTableRef.physicalName()));

        if (!hasAnyPhysicalTable(dataTableId)) {
            dataTableRepository.deleteById(dataTableId);
        }
    }

    /**
     * Duplicates an existing data table to a new table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because every physical name is built by
     * {@link DataTableRef} from numeric ids, table names are validated by {@link DataTableNames}, and every identifier
     * passes through {@link #escapeIdentifier(String)}, which enforces the strict allowlist pattern
     * {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @Transactional
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public long duplicateTable(long dataTableId, String newName, long environmentId) {
        String normalizedName = DataTableNames.normalize(newName);

        DataTable sourceDataTable = getDataTable(dataTableId);

        if (fetchDataTable(sourceDataTable.getWorkspaceId(), normalizedName).isPresent()) {
            throw alreadyExists(normalizedName);
        }

        String sourcePhysicalName = existingPhysicalName(sourceDataTable, environmentId);

        DataTable copyDataTable = insertDataTable(
            sourceDataTable.getWorkspaceId(), normalizedName, sourceDataTable.getDescription());

        String copyPhysicalName = new DataTableRef(copyDataTable.getId(), environmentId).physicalName();

        List<ColumnSpec> columnSpecs = listColumns(sourcePhysicalName)
            .stream()
            .filter(columnSpec -> !ReservedColumns.isReserved(columnSpec.name()))
            .toList();

        createPhysicalTable(copyPhysicalName, columnSpecs);

        List<String> copiedColumnNames = new ArrayList<>();

        copiedColumnNames.add(ReservedColumns.EXTERNAL_ID);
        copiedColumnNames.addAll(
            columnSpecs.stream()
                .map(ColumnSpec::name)
                .toList());

        String columnList = copiedColumnNames.stream()
            .map(DataTableServiceImpl::escapeIdentifier)
            .collect(Collectors.joining(", "));

        jdbcTemplate.execute(
            "INSERT INTO " + escapeIdentifier(copyPhysicalName) + " (" + columnList + ") SELECT " + columnList +
                " FROM " + escapeIdentifier(sourcePhysicalName));

        return copyDataTable.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataTable> fetchDataTable(@Nullable Long workspaceId, String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);

        if (workspaceId == null) {
            return dataTableRepository.findByWorkspaceIdIsNullAndName(normalizedName);
        }

        return dataTableRepository.findByWorkspaceIdAndName(workspaceId, normalizedName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DataTableInfo> fetchDataTableInfo(long dataTableId, long environmentId) {
        return dataTableRepository.findById(dataTableId)
            .flatMap(dataTable -> toDataTableInfo(dataTable, environmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public DataTable getDataTable(long dataTableId) {
        return dataTableRepository.findById(dataTableId)
            .orElseThrow(() -> new DataTableException(
                "Data table not found: id=" + dataTableId, DataTableErrorType.DATA_TABLE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataTable> getWorkspaceDataTables(long workspaceId) {
        return dataTableRepository.findAllByWorkspaceIdOrderByName(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataTableInfo> listAllTables(long environmentId) {
        List<DataTableInfo> dataTableInfos = new ArrayList<>();

        for (DataTable dataTable : dataTableRepository.findAll()) {
            toDataTableInfo(dataTable, environmentId).ifPresent(dataTableInfos::add);
        }

        return dataTableInfos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataTableInfo> listTables(@Nullable Long workspaceId, long environmentId) {
        List<DataTable> dataTables = workspaceId == null
            ? dataTableRepository.findAllByWorkspaceIdIsNullOrderByName()
            : dataTableRepository.findAllByWorkspaceIdOrderByName(workspaceId);

        List<DataTableInfo> dataTableInfos = new ArrayList<>();

        for (DataTable dataTable : dataTables) {
            toDataTableInfo(dataTable, environmentId).ifPresent(dataTableInfos::add);
        }

        return dataTableInfos;
    }

    /**
     * Removes a column from an existing data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because every physical name is built by
     * {@link DataTableRef} from numeric ids, table names are validated by {@link DataTableNames}, and every identifier
     * passes through {@link #escapeIdentifier(String)}, which enforces the strict allowlist pattern
     * {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void removeColumn(long dataTableId, String columnName, long environmentId) {
        validateColumnName(columnName);

        DataTable dataTable = getDataTable(dataTableId);

        String physicalName = existingPhysicalName(dataTable, environmentId);

        if (!hasColumn(physicalName, columnName)) {
            throw new DataTableException(
                "Column '" + columnName + "' not found on table '" + dataTable.getName() + "'",
                DataTableErrorType.COLUMN_NOT_FOUND);
        }

        jdbcTemplate.execute(
            "ALTER TABLE " + escapeIdentifier(physicalName) + " DROP COLUMN " + escapeIdentifier(columnName));
    }

    /**
     * Renames a column in an existing data table.
     *
     * <p>
     * <b>Security Note:</b> The SQL_INJECTION_SPRING_JDBC suppression is safe because every physical name is built by
     * {@link DataTableRef} from numeric ids, table names are validated by {@link DataTableNames}, and every identifier
     * passes through {@link #escapeIdentifier(String)}, which enforces the strict allowlist pattern
     * {@code [a-z_][a-z0-9_]*}, preventing SQL injection.
     */
    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void renameColumn(long dataTableId, String fromColumnName, String toColumnName, long environmentId) {
        validateColumnName(fromColumnName);
        validateColumnName(toColumnName);

        DataTable dataTable = getDataTable(dataTableId);

        String physicalName = existingPhysicalName(dataTable, environmentId);

        if (!hasColumn(physicalName, fromColumnName)) {
            throw new DataTableException(
                "Column '" + fromColumnName + "' not found on table '" + dataTable.getName() + "'",
                DataTableErrorType.COLUMN_NOT_FOUND);
        }

        if (hasColumn(physicalName, toColumnName)) {
            throw new DataTableException(
                "Column '" + toColumnName + "' already exists on table '" + dataTable.getName() + "'",
                DataTableErrorType.COLUMN_ALREADY_EXISTS);
        }

        jdbcTemplate.execute(
            "ALTER TABLE " + escapeIdentifier(physicalName) + " RENAME COLUMN " + escapeIdentifier(fromColumnName) +
                " TO " + escapeIdentifier(toColumnName));
    }

    @Override
    @Transactional
    public void renameTable(long dataTableId, String newName) {
        String normalizedName = DataTableNames.normalize(newName);

        DataTable dataTable = getDataTable(dataTableId);

        if (normalizedName.equals(dataTable.getName())) {
            return;
        }

        if (fetchDataTable(dataTable.getWorkspaceId(), normalizedName).isPresent()) {
            throw alreadyExists(normalizedName);
        }

        dataTable.setName(normalizedName);

        saveDataTable(dataTable);
    }

    @Override
    @Transactional
    public void updateDescription(long dataTableId, @Nullable String description) {
        DataTable dataTable = getDataTable(dataTableId);

        dataTable.setDescription(description);

        dataTableRepository.save(dataTable);
    }

    private DataTableException alreadyExists(String name) {
        return new DataTableException(
            "Data table '" + name + "' already exists in this workspace", DataTableErrorType.DATA_TABLE_ALREADY_EXISTS);
    }

    private String existingPhysicalName(DataTable dataTable, long environmentId) {
        String physicalName = new DataTableRef(dataTable.getId(), environmentId).physicalName();

        if (!physicalTableExists(physicalName)) {
            throw new DataTableException(
                "Data table '" + dataTable.getName() + "' does not exist in this environment",
                DataTableErrorType.DATA_TABLE_NOT_FOUND);
        }

        return physicalName;
    }

    private boolean hasAnyPhysicalTable(long dataTableId) {
        for (Environment environment : Environment.values()) {
            if (physicalTableExists(new DataTableRef(dataTableId, environment.ordinal()).physicalName())) {
                return true;
            }
        }

        return false;
    }

    private DataTable insertDataTable(@Nullable Long workspaceId, String name, @Nullable String description) {
        DataTable dataTable = new DataTable();

        dataTable.setDescription(description);
        dataTable.setName(name);
        dataTable.setWorkspaceId(workspaceId);

        return saveDataTable(dataTable);
    }

    private DataTable saveDataTable(DataTable dataTable) {
        try {
            return dataTableRepository.save(dataTable);
        } catch (DuplicateKeyException duplicateKeyException) {
            throw alreadyExists(dataTable.getName());
        }
    }

    private Optional<DataTableInfo> toDataTableInfo(DataTable dataTable, long environmentId) {
        String physicalName = new DataTableRef(dataTable.getId(), environmentId).physicalName();

        if (!physicalTableExists(physicalName)) {
            return Optional.empty();
        }

        List<ColumnSpec> columnSpecs = listColumns(physicalName)
            .stream()
            .filter(columnSpec -> !ReservedColumns.isReserved(columnSpec.name()))
            .toList();

        return Optional.of(
            new DataTableInfo(
                dataTable.getId(), dataTable.getName(), dataTable.getWorkspaceId(), dataTable.getDescription(),
                columnSpecs, dataTable.getLastModifiedDate()));
    }

    /**
     * Creates one physical table: the table itself and the index its external id is keyed through.
     *
     * <p>
     * Extracted because {@code createTable} and {@code duplicateTable} used to hold near-copies of the statement, and a
     * column added to one and forgotten in the other produces a duplicate whose rows the row service cannot read. One
     * helper makes that drift impossible rather than merely unlikely.
     */
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void createPhysicalTable(String physicalName, List<ColumnSpec> columnSpecs) {
        jdbcTemplate.execute(buildCreateTableSql(physicalName, columnSpecs));
        jdbcTemplate.execute(buildExternalIdIndexSql(physicalName));
    }

    /**
     * The one CREATE TABLE statement a data table is ever built from.
     */
    static String buildCreateTableSql(String physicalName, List<ColumnSpec> columnSpecs) {
        String userColumnsSql = columnSpecs.stream()
            .map(columnSpec -> escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type()))
            .collect(Collectors.joining(", "));

        return "CREATE TABLE " + escapeIdentifier(physicalName) + " (\"id\" BIGSERIAL PRIMARY KEY, " +
            escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " VARCHAR(255)" +
            (userColumnsSql.isEmpty() ? "" : ", " + userColumnsSql) + ")";
    }

    /**
     * The upsert key. Standard NULL semantics are what is wanted here: two rows with no external id compare as distinct
     * and so do not collide, while two rows carrying the same one do. That needs neither {@code NULLS NOT DISTINCT} nor
     * a partial predicate, so the statement is the same on Postgres and on H2.
     *
     * <p>
     * Deliberately unnamed, so the database derives the name itself and a unique name per physical table never has to
     * be kept in step with the table naming here.
     */
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

    /**
     * Whether a physical table by this name already exists. Checked before every CREATE so that a name collision in
     * this environment surfaces as a typed registry decision rather than as a raw {@code BadSqlGrammarException} from
     * Postgres refusing a duplicate CREATE TABLE.
     */
    private boolean physicalTableExists(String physicalName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?",
            Integer.class, physicalName);

        return count != null && count > 0;
    }

    /**
     * Whether a physical table already carries a column by this name, case-insensitively -- Postgres itself folds
     * unquoted identifiers to lower case, so a caller asking about "Title" and one asking about "title" must get the
     * same answer.
     */
    private boolean hasColumn(String physicalName, String columnName) {
        return listColumns(physicalName).stream()
            .anyMatch(columnSpec -> columnSpec.name()
                .equalsIgnoreCase(columnName));
    }

    /**
     * A column name must be a valid identifier and must not be one of the platform's own reserved columns -- reserved
     * names are neither addable, removable, nor a valid rename target, and this is the one gate every column mutation
     * runs through.
     */
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
