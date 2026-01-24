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

package com.bytechef.automation.data.table.configuration.service;

import com.bytechef.automation.data.table.configuration.domain.DataTable;
import com.bytechef.automation.data.table.configuration.domain.DataTableInfo;
import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.repository.DataTableRepository;
import com.bytechef.automation.data.table.configuration.repository.WorkspaceDataTableRepository;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import com.bytechef.automation.data.table.domain.ColumnType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableServiceImpl implements DataTableService {

    private final DataTableRepository dataTableRepository;
    private final JdbcTemplate jdbcTemplate;
    private final WorkspaceDataTableRepository workspaceDataTableRepository;

    @SuppressFBWarnings("EI")
    public DataTableServiceImpl(
        DataTableRepository dataTableRepository, JdbcTemplate jdbcTemplate,
        WorkspaceDataTableRepository workspaceDataTableRepository) {

        this.dataTableRepository = dataTableRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.workspaceDataTableRepository = workspaceDataTableRepository;
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
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long environmentId) {

        validateBaseName(baseName);

        Assert.notEmpty(columnSpecs, "columns must not be empty");

        boolean hasId = columnSpecs.stream()
            .anyMatch(c -> "id".equalsIgnoreCase(c.name()));

        Assert.isTrue(!hasId, "Column name 'id' is reserved for primary key");

        String userColsSql = columnSpecs.stream()
            .map(columnSpec -> escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type()))
            .collect(Collectors.joining(", "));

        String physicalName = buildPhysicalName(environmentId, baseName);

        String sql = "CREATE TABLE " + escapeIdentifier(physicalName) + " (\"id\" BIGSERIAL PRIMARY KEY" +
            (userColsSql.isEmpty() ? "" : ", " + userColsSql) + ")";

        jdbcTemplate.execute(sql);

        checkRegistry(baseName, description);
    }

    @Override
    public void createTable(long workspaceId, String baseName, List<ColumnSpec> columnSpecs, long environmentId) {
        createTable(baseName, null, columnSpecs, workspaceId, environmentId);
    }

    @Override
    public void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId) {

        createTable(baseName, description, columnSpecs, environmentId);

        long id = getIdByBaseName(baseName);

        WorkspaceDataTable existingWorkspaceDataTable = workspaceDataTableRepository.findByWorkspaceIdAndDataTableId(
            workspaceId, id);

        if (existingWorkspaceDataTable == null) {
            workspaceDataTableRepository.save(new WorkspaceDataTable(id, workspaceId));
        }
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

        dataTableRepository.findByName(baseName)
            .ifPresent(dataTable -> {
                List<WorkspaceDataTable> workspaceDataTables =
                    workspaceDataTableRepository.findByDataTableId(dataTable.getId());

                workspaceDataTableRepository.deleteAll(workspaceDataTables);
            });

        dataTableRepository.deleteByName(baseName);
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
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void duplicateTable(String fromBaseName, String toBaseName, long environmentId) {
        validateBaseName(fromBaseName);
        validateBaseName(toBaseName);

        String fromPhysicalName = buildPhysicalName(environmentId, fromBaseName);
        String toPhysicalName = buildPhysicalName(environmentId, toBaseName);

        List<ColumnSpec> columnSpecs = listColumns(fromPhysicalName)
            .stream()
            .filter(columnSpec -> !"id".equalsIgnoreCase(columnSpec.name()))
            .toList();

        String userColumnsSql = columnSpecs.stream()
            .map(columnSpec -> escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type()))
            .collect(Collectors.joining(", "));

        String createSql = "CREATE TABLE " + escapeIdentifier(toPhysicalName) +
            " (\"id\" BIGSERIAL PRIMARY KEY" + (userColumnsSql.isEmpty() ? "" : ", " + userColumnsSql) + ")";

        jdbcTemplate.execute(createSql);

        if (!columnSpecs.isEmpty()) {
            String columnList = columnSpecs.stream()
                .map(columnSpec -> escapeIdentifier(columnSpec.name()))
                .collect(Collectors.joining(", "));

            String insertSql = "INSERT INTO " + escapeIdentifier(toPhysicalName) + " (" + columnList + ") SELECT " +
                columnList + " FROM " + escapeIdentifier(fromPhysicalName);

            jdbcTemplate.execute(insertSql);
        }

        checkRegistry(toBaseName, null);
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
            .orElse(null);

        if (dataTable != null && dataTable.getId() != null) {
            return dataTable.getId();
        }

        return checkRegistry(baseName, null);
    }

    @Override
    public List<DataTableInfo> listTables(long environmentId) {
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

            List<ColumnSpec> columnSpecs = listColumns(tableName)
                .stream()
                .filter(columnSpec -> !"id".equalsIgnoreCase(columnSpec.name()))
                .toList();
            DataTable dataTable = dataTableRepository.findByName(baseName)
                .orElse(null);

            Long id;
            String description;

            if (dataTable == null) {
                id = checkRegistry(baseName, null);

                dataTableInfos.add(new DataTableInfo(id, baseName, null, columnSpecs, null));

                continue;
            } else {
                id = dataTable.getId();
                description = dataTable.getDescription();
            }

            dataTableInfos.add(
                new DataTableInfo(id, baseName, description, columnSpecs, dataTable.getLastModifiedDate()));
        }

        return dataTableInfos;
    }

    @Override
    public List<DataTableInfo> listTables(long workspaceId, long environmentId) {
        List<DataTableInfo> dataTableInfos = listTables(environmentId);
        Set<Long> dataTableIds = workspaceDataTableRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .map(WorkspaceDataTable::getDataTableId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return dataTableInfos.stream()
            .filter(info -> info.id() != null && dataTableIds.contains(info.id()))
            .toList();
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
        Assert.isTrue(!"id".equalsIgnoreCase(fromColumnName), "Column 'id' cannot be renamed");
        Assert.isTrue(!"id".equalsIgnoreCase(toColumnName), "Cannot rename to reserved name 'id'");

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

    private long checkRegistry(String baseName, String description) {
        Assert.hasText(baseName, "baseName required");

        return dataTableRepository.findByName(baseName)
            .map(DataTable::getId)
            .orElseGet(() -> {
                DataTable dataTable = new DataTable(null, baseName);

                dataTable.setDescription(description);

                DataTable savedDataTable = dataTableRepository.save(dataTable);

                return Objects.requireNonNull(savedDataTable.getId());
            });
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
