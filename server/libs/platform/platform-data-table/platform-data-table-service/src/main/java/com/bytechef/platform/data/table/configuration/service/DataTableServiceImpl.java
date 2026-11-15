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

import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.audit.DataTableAuditEvents;
import com.bytechef.platform.data.table.audit.DataTableAuditMapper;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableNames;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.util.DataTablePhysicalNameUtils;
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

    @Override
    @Audited(event = DataTableAuditEvents.DATA_TABLE_COLUMN_ADDED, platform = PlatformType.AUTOMATION, data = {
        @AuditData(key = "dataTableId", value = "#dataTableId"),
        @AuditData(key = "columnName", value = "#columnSpec.name"),
        @AuditData(key = "columnType", value = "#columnSpec.type"),
        @AuditData(key = "environmentId", value = "#environmentId")
    })
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId) {
        Assert.notNull(columnSpec, "column must not be null");

        validateColumnName(columnSpec.name());

        DataTable dataTable = getDataTable(dataTableId);

        String physicalName = getExistingPhysicalName(dataTable, environmentId);

        if (hasColumn(physicalName, columnSpec.name())) {
            throw new DataTableException(
                "Column '" + columnSpec.name() + "' already exists on table '" + dataTable.getName() + "'",
                DataTableErrorType.COLUMN_ALREADY_EXISTS);
        }

        String sql = "ALTER TABLE " + escapeIdentifier(physicalName) + " ADD COLUMN " +
            escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type());

        jdbcTemplate.execute(sql);
    }

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

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTable.getId());

        if (physicalTableExists(physicalName)) {
            throw new DataTableException(
                "Data table '" + normalizedName + "' already exists in this environment",
                DataTableErrorType.DATA_TABLE_ALREADY_EXISTS);
        }

        createPhysicalTable(physicalName, columnSpecs);

        return dataTable.getId();
    }

    @Override
    @Audited(
        event = DataTableAuditEvents.DATA_TABLE_DELETED, mapper = DataTableAuditMapper.class,
        platform = PlatformType.AUTOMATION, data = {
            @AuditData(key = "dataTableId", value = "#dataTableId"),
            @AuditData(key = "environmentId", value = "#environmentId")
        })
    @Transactional
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public boolean dropTable(long dataTableId, long environmentId) {
        if (!dataTableRepository.existsById(dataTableId)) {
            return false;
        }

        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTableId);

        boolean exists = physicalTableExists(physicalName);

        jdbcTemplate.execute("DROP TABLE IF EXISTS " + escapeIdentifier(physicalName));

        if (!hasAnyPhysicalTable(dataTableId)) {
            dataTableRepository.deleteById(dataTableId);
        }

        return exists;
    }

    @Override
    @Transactional
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public long duplicateTable(long dataTableId, String newName, long environmentId) {
        String normalizedName = DataTableNames.normalize(newName);

        DataTable sourceDataTable = getDataTable(dataTableId);

        if (fetchDataTable(sourceDataTable.getWorkspaceId(), normalizedName).isPresent()) {
            throw createAlreadyExistsException(normalizedName);
        }

        String sourcePhysicalName = getExistingPhysicalName(sourceDataTable, environmentId);

        DataTable copyDataTable = insertDataTable(
            sourceDataTable.getWorkspaceId(), normalizedName, sourceDataTable.getDescription());

        String copyPhysicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, copyDataTable.getId());

        List<ColumnSpec> sourceColumnSpecs = listColumns(sourcePhysicalName);

        List<ColumnSpec> columnSpecs = sourceColumnSpecs.stream()
            .filter(columnSpec -> !ReservedColumns.isReserved(columnSpec.name()))
            .toList();

        createPhysicalTable(copyPhysicalName, columnSpecs);

        List<String> copiedColumnNames = new ArrayList<>();

        if (containsColumn(sourceColumnSpecs, ReservedColumns.EXTERNAL_ID)) {
            copiedColumnNames.add(ReservedColumns.EXTERNAL_ID);
        }

        for (ColumnSpec columnSpec : columnSpecs) {
            copiedColumnNames.add(columnSpec.name());
        }

        if (!copiedColumnNames.isEmpty()) {
            String columnList = copiedColumnNames.stream()
                .map(this::escapeIdentifier)
                .collect(Collectors.joining(", "));

            String insertSql = "INSERT INTO " + escapeIdentifier(copyPhysicalName) + " (" + columnList + ") SELECT " +
                columnList + " FROM " + escapeIdentifier(sourcePhysicalName);

            jdbcTemplate.execute(insertSql);
        }

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
                "Data table with id=" + dataTableId + " not found", DataTableErrorType.DATA_TABLE_NOT_FOUND));
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

    @Override
    @Audited(event = DataTableAuditEvents.DATA_TABLE_COLUMN_REMOVED, platform = PlatformType.AUTOMATION, data = {
        @AuditData(key = "dataTableId", value = "#dataTableId"),
        @AuditData(key = "columnName", value = "#columnName"),
        @AuditData(key = "environmentId", value = "#environmentId")
    })
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void removeColumn(long dataTableId, String columnName, long environmentId) {
        validateColumnName(columnName);

        DataTable dataTable = getDataTable(dataTableId);

        String physicalName = getExistingPhysicalName(dataTable, environmentId);

        if (!hasColumn(physicalName, columnName)) {
            throw new DataTableException(
                "Column '" + columnName + "' not found on table '" + dataTable.getName() + "'",
                DataTableErrorType.COLUMN_NOT_FOUND);
        }

        String sql = "ALTER TABLE " + escapeIdentifier(physicalName) + " DROP COLUMN " + escapeIdentifier(columnName);

        jdbcTemplate.execute(sql);
    }

    @Override
    @Audited(event = DataTableAuditEvents.DATA_TABLE_COLUMN_RENAMED, platform = PlatformType.AUTOMATION, data = {
        @AuditData(key = "dataTableId", value = "#dataTableId"),
        @AuditData(key = "fromColumnName", value = "#fromColumnName"),
        @AuditData(key = "toColumnName", value = "#toColumnName"),
        @AuditData(key = "environmentId", value = "#environmentId")
    })
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void renameColumn(long dataTableId, String fromColumnName, String toColumnName, long environmentId) {
        validateColumnName(fromColumnName);
        validateColumnName(toColumnName);

        DataTable dataTable = getDataTable(dataTableId);

        String physicalName = getExistingPhysicalName(dataTable, environmentId);

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

        String sql = "ALTER TABLE " + escapeIdentifier(physicalName) + " RENAME COLUMN " +
            escapeIdentifier(fromColumnName) + " TO " + escapeIdentifier(toColumnName);

        jdbcTemplate.execute(sql);
    }

    @Override
    @Audited(event = DataTableAuditEvents.DATA_TABLE_RENAMED, platform = PlatformType.AUTOMATION, data = {
        @AuditData(key = "dataTableId", value = "#dataTableId"),
        @AuditData(key = "newName", value = "#newName")
    })
    @Transactional
    public void renameTable(long dataTableId, String newName) {
        String normalizedName = DataTableNames.normalize(newName);

        DataTable dataTable = getDataTable(dataTableId);

        if (normalizedName.equals(dataTable.getName())) {
            return;
        }

        if (fetchDataTable(dataTable.getWorkspaceId(), normalizedName).isPresent()) {
            throw createAlreadyExistsException(normalizedName);
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

    private static DataTableException createAlreadyExistsException(String name) {
        return new DataTableException(
            "Data table '" + name + "' already exists in this workspace", DataTableErrorType.DATA_TABLE_ALREADY_EXISTS);
    }

    private String getExistingPhysicalName(DataTable dataTable, long environmentId) {
        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTable.getId());

        if (!physicalTableExists(physicalName)) {
            throw new DataTableException(
                "Data table '" + dataTable.getName() + "' does not exist in this environment",
                DataTableErrorType.DATA_TABLE_NOT_FOUND);
        }

        return physicalName;
    }

    private boolean hasAnyPhysicalTable(long dataTableId) {
        for (Environment environment : Environment.values()) {
            String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environment.ordinal(), dataTableId);

            if (physicalTableExists(physicalName)) {
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
            throw createAlreadyExistsException(dataTable.getName());
        }
    }

    private Optional<DataTableInfo> toDataTableInfo(DataTable dataTable, long environmentId) {
        String physicalName = DataTablePhysicalNameUtils.getPhysicalName(environmentId, dataTable.getId());

        if (!physicalTableExists(physicalName)) {
            return Optional.empty();
        }

        return Optional.of(
            new DataTableInfo(
                dataTable.getId(), dataTable.getName(), dataTable.getWorkspaceId(), dataTable.getDescription(),
                listUserColumns(physicalName), dataTable.getLastModifiedDate()));
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void createPhysicalTable(String physicalName, List<ColumnSpec> columnSpecs) {
        String userColumnsSql = columnSpecs.stream()
            .map(columnSpec -> escapeIdentifier(columnSpec.name()) + " " + sqlType(columnSpec.type()))
            .collect(Collectors.joining(", "));

        jdbcTemplate.execute(
            "CREATE TABLE " + escapeIdentifier(physicalName) + " (\"id\" BIGSERIAL PRIMARY KEY, " +
                escapeIdentifier(ReservedColumns.EXTERNAL_ID) + " VARCHAR(255)" +
                (userColumnsSql.isEmpty() ? "" : ", " + userColumnsSql) + ")");

        jdbcTemplate.execute(
            "CREATE UNIQUE INDEX ON " + escapeIdentifier(physicalName) + " (" +
                escapeIdentifier(ReservedColumns.EXTERNAL_ID) + ")");
    }

    private static boolean containsColumn(List<ColumnSpec> columnSpecs, String columnName) {
        return columnSpecs.stream()
            .anyMatch(columnSpec -> columnName.equalsIgnoreCase(columnSpec.name()));
    }

    private boolean hasColumn(String physicalName, String columnName) {
        return containsColumn(listColumns(physicalName), columnName);
    }

    private List<ColumnSpec> listUserColumns(String physicalName) {
        return listColumns(physicalName).stream()
            .filter(columnSpec -> !ReservedColumns.isReserved(columnSpec.name()))
            .toList();
    }

    private boolean physicalTableExists(String physicalName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
            "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, physicalName);

        return count != null && count > 0;
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
            case LINK -> "BIGINT";
        };
    }

    private static void validateColumnName(@Nullable String columnName) {
        if (columnName == null || !columnName.toLowerCase(Locale.ROOT)
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
