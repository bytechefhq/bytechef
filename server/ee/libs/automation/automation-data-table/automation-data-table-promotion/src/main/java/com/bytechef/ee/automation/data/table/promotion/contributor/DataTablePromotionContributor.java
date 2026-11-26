/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.promotion.contributor;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.configuration.promotion.connection.PromotionConnectionScope;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionSchemaChange;
import com.bytechef.ee.automation.promotion.dto.PromotionSchemaChange.Kind;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.util.PromotionChangeHashes;
import com.bytechef.ee.automation.promotion.util.PromotionEnvironmentUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.ColumnTypeConversion;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class DataTablePromotionContributor implements EnvironmentPromotionContributor {

    public static final String RESOURCE_TYPE = "DATA_TABLE";

    private static final String COLUMN_ASSET_TYPE = "COLUMN";
    private static final String DATA_TABLE_ASSET_TYPE = "DATA_TABLE";

    private final WorkspaceDataTableFacade workspaceDataTableFacade;

    @SuppressFBWarnings("EI")
    public DataTablePromotionContributor(WorkspaceDataTableFacade workspaceDataTableFacade) {
        this.workspaceDataTableFacade = workspaceDataTableFacade;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.AUTOMATION;
    }

    @Override
    @PreAuthorize("hasPermission(#sourceId, 'DataTable', 'DATA_TABLE_EDIT')")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {

        checkEnvironments(sourceId, sourceEnvironment, targetEnvironment);

        long workspaceId = workspaceDataTableFacade.getWorkspaceId(sourceId);

        List<DataTableInfo> promotedTables = collectPromotedTables(sourceId, workspaceId, sourceEnvironment);
        Map<Long, DataTableInfo> targetTablesById = listTablesById(workspaceId, targetEnvironment);

        String environmentName = PromotionEnvironmentUtils.displayName(targetEnvironment);
        List<PromotionSchemaChange> schemaChanges = new ArrayList<>();
        List<PromotionChange> changes = new ArrayList<>();
        List<PromotionChange> hashedChanges = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (DataTableInfo promotedTable : promotedTables) {
            DataTableInfo targetTable = targetTablesById.get(promotedTable.id());

            List<PromotionSchemaChange> tableSchemaChanges = DataTableSchemaDiff.diff(
                promotedTable.name(), targetTable == null ? null : targetTable.columns(), promotedTable.columns());

            schemaChanges.addAll(tableSchemaChanges);

            PromotionChange tableChange = tableChange(promotedTable, targetTable, tableSchemaChanges);

            if (tableChange != null) {
                changes.add(tableChange);
            }

            if (targetTable != null) {
                addLinkTargetChangeWarnings(warnings, promotedTable, targetTable, tableSchemaChanges, environmentName);
                addColumnTypeChangeWarnings(warnings, promotedTable, targetTable, tableSchemaChanges, environmentName);
            }

            if (targetTable == null && promotedTable.id() != sourceId) {
                warnings.add(
                    "Linked data table '%s' does not exist in %s and will be created empty."
                        .formatted(promotedTable.name(), environmentName));
            }

            if (targetTable == null && userColumns(promotedTable).isEmpty()) {
                warnings.add(
                    "Data table '%s' has no columns, so it cannot be created in %s; the promotion will fail."
                        .formatted(promotedTable.name(), environmentName));
            }
        }

        hashedChanges.addAll(changes);
        hashedChanges.addAll(predictColumnChanges(promotedTables, targetTablesById));

        DataTableInfo sourceTable = promotedTables.getFirst();
        DataTableInfo existingTargetTable = targetTablesById.get(sourceId);

        if (existingTargetTable == null) {
            warnings.addFirst(
                "The data table is created empty in %s; rows are not promoted.".formatted(environmentName));
        }

        if (schemaChanges.stream()
            .anyMatch(schemaChange -> schemaChange.kind() == Kind.DROP_COLUMN)) {

            warnings.add("Dropped columns delete their data in %s.".formatted(environmentName));
        }

        return new EnvironmentPromotionPreview(
            RESOURCE_TYPE, sourceId, sourceEnvironment, targetEnvironment,
            existingTargetTable == null ? null : sourceId,
            existingTargetTable == null ? null : sourceTable.name(), List.of(), List.of(), warnings, schemaChanges,
            changes, PromotionChangeHashes.destructiveChangesHash(hashedChanges));
    }

    @Override
    @PreAuthorize("hasPermission(#sourceId, 'DataTable', 'DATA_TABLE_EDIT')")
    @Transactional
    public EnvironmentPromotionResult promote(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment,
        Map<Long, Long> connectionMappings, @Nullable String expectedDestructiveChangesHash) {

        checkEnvironments(sourceId, sourceEnvironment, targetEnvironment);

        PromotionConnectionScope.checkMappedConnectionsBelongToSource(Set.of(), connectionMappings);

        long workspaceId = workspaceDataTableFacade.getWorkspaceId(sourceId);
        long targetEnvironmentId = targetEnvironment.ordinal();

        List<DataTableInfo> promotedTables = collectPromotedTables(sourceId, workspaceId, sourceEnvironment);
        Map<Long, DataTableInfo> targetTablesById = listTablesById(workspaceId, targetEnvironment);

        PromotionChangeHashes.checkNotStale(
            expectedDestructiveChangesHash,
            PromotionChangeHashes.destructiveChangesHash(predictColumnChanges(promotedTables, targetTablesById)));

        boolean created = !targetTablesById.containsKey(sourceId);

        Set<Long> existingTargetTableIds = new HashSet<>(targetTablesById.keySet());

        for (DataTableInfo promotedTable : promotedTables) {
            if (!existingTargetTableIds.contains(promotedTable.id())) {
                createTargetTable(promotedTable, workspaceId, targetEnvironmentId, existingTargetTableIds);

                existingTargetTableIds.add(promotedTable.id());
            }
        }

        targetTablesById = listTablesById(workspaceId, targetEnvironment);

        for (DataTableInfo promotedTable : promotedTables) {
            DataTableInfo targetTable = Objects.requireNonNull(targetTablesById.get(promotedTable.id()));

            syncScalarColumns(promotedTable, targetTable.columns(), targetEnvironmentId);
        }

        targetTablesById = listTablesById(workspaceId, targetEnvironment);

        for (DataTableInfo promotedTable : promotedTables) {
            DataTableInfo targetTable = Objects.requireNonNull(targetTablesById.get(promotedTable.id()));

            syncLinkColumns(promotedTable, targetTable.columns(), targetEnvironmentId);
        }

        return new EnvironmentPromotionResult(sourceId, created, null, List.of());
    }

    private static List<PromotionChange> predictColumnChanges(
        List<DataTableInfo> promotedTables, Map<Long, DataTableInfo> targetTablesById) {

        List<PromotionChange> columnChanges = new ArrayList<>();

        for (DataTableInfo promotedTable : promotedTables) {
            DataTableInfo targetTable = targetTablesById.get(promotedTable.id());

            if (targetTable == null) {
                continue;
            }

            for (PromotionSchemaChange schemaChange : DataTableSchemaDiff.diff(
                promotedTable.name(), targetTable.columns(), promotedTable.columns())) {

                if (!schemaChange.destructive()) {
                    continue;
                }

                String columnName = Objects.requireNonNull(schemaChange.columnName());

                columnChanges.add(
                    new PromotionChange(
                        schemaChange.kind() == Kind.DROP_COLUMN ? PromotionChange.Kind.DELETE
                            : PromotionChange.Kind.UPDATE,
                        COLUMN_ASSET_TYPE, columnName, promotedTable.name(), columnName, true));
            }
        }

        return columnChanges;
    }

    private static @Nullable PromotionChange tableChange(
        DataTableInfo promotedTable, @Nullable DataTableInfo targetTable, List<PromotionSchemaChange> schemaChanges) {

        String tableName = promotedTable.name();

        if (targetTable == null) {
            return new PromotionChange(PromotionChange.Kind.CREATE, DATA_TABLE_ASSET_TYPE, tableName, null, tableName,
                false);
        }

        if (schemaChanges.isEmpty()) {
            return null;
        }

        return new PromotionChange(PromotionChange.Kind.UPDATE, DATA_TABLE_ASSET_TYPE, tableName, null, tableName,
            false);
    }

    private static void addLinkTargetChangeWarnings(
        List<String> warnings, DataTableInfo sourceTable, DataTableInfo targetTable,
        List<PromotionSchemaChange> tableSchemaChanges, String environmentName) {

        for (PromotionSchemaChange schemaChange : tableSchemaChanges) {
            if (schemaChange.kind() != Kind.CHANGE_LINK) {
                continue;
            }

            ColumnSpec sourceColumn = findColumn(sourceTable, schemaChange);
            ColumnSpec targetColumn = findColumn(targetTable, schemaChange);

            LinkSpec sourceLink = Objects.requireNonNull(sourceColumn.link());
            LinkSpec targetLink = Objects.requireNonNull(targetColumn.link());

            if (sourceLink.targetDataTableId() != targetLink.targetDataTableId()) {
                warnings.add(
                    "Link column '%s' of data table '%s' points to a different table; its values are cleared in %s."
                        .formatted(schemaChange.columnName(), sourceTable.name(), environmentName));
            }
        }
    }

    private static void addColumnTypeChangeWarnings(
        List<String> warnings, DataTableInfo sourceTable, DataTableInfo targetTable,
        List<PromotionSchemaChange> tableSchemaChanges, String environmentName) {

        for (PromotionSchemaChange schemaChange : tableSchemaChanges) {
            if (schemaChange.kind() != Kind.CHANGE_COLUMN_TYPE) {
                continue;
            }

            ColumnSpec sourceColumn = findColumn(sourceTable, schemaChange);
            ColumnSpec targetColumn = findColumn(targetTable, schemaChange);

            ColumnTypeConversion columnTypeConversion =
                ColumnTypeConversion.of(targetColumn.type(), sourceColumn.type());

            if (columnTypeConversion == ColumnTypeConversion.UNSUPPORTED) {
                warnings.add(
                    "Column '%s' of data table '%s' cannot be changed from %s to %s; the promotion will fail."
                        .formatted(
                            schemaChange.columnName(), sourceTable.name(), targetColumn.type(), sourceColumn.type()));
            } else if (columnTypeConversion == ColumnTypeConversion.LOSSY) {
                warnings.add(
                    ("Changing column '%s' of data table '%s' from %s to %s may truncate its existing values in %s, " +
                        "or fail if a value cannot be converted.").formatted(
                            schemaChange.columnName(), sourceTable.name(), targetColumn.type(), sourceColumn.type(),
                            environmentName));
            }
        }
    }

    private void addColumn(DataTableInfo sourceTable, ColumnSpec columnSpec, long targetEnvironmentId) {
        try {
            workspaceDataTableFacade.addColumn(sourceTable.id(), columnSpec, targetEnvironmentId);
        } catch (DataAccessException | DataTableException exception) {
            throw schemaChangeFailed(
                "Adding column '%s' to data table '%s' failed".formatted(columnSpec.name(), sourceTable.name()),
                exception);
        }
    }

    private void changeColumnType(
        DataTableInfo sourceTable, PromotionSchemaChange schemaChange, ColumnSpec sourceColumn,
        long targetEnvironmentId) {

        String failureMessage = "Changing column '%s' of data table '%s' from %s to %s failed".formatted(
            sourceColumn.name(), sourceTable.name(), schemaChange.fromType(), schemaChange.toType());

        ColumnType fromColumnType = ColumnType.valueOf(Objects.requireNonNull(schemaChange.fromType()));

        if (ColumnTypeConversion.of(fromColumnType, sourceColumn.type()) == ColumnTypeConversion.UNSUPPORTED) {
            throw new ConfigurationException(
                failureMessage + ": the conversion is not supported",
                EnvironmentPromotionErrorType.SCHEMA_CHANGE_FAILED);
        }

        try {
            workspaceDataTableFacade.changeColumnType(
                sourceTable.id(), sourceColumn.name(), sourceColumn.type(), targetEnvironmentId);
        } catch (DataAccessException exception) {
            throw schemaChangeFailed(failureMessage + ": existing values cannot be converted", exception);
        } catch (DataTableException exception) {
            throw schemaChangeFailed(failureMessage, exception);
        }
    }

    private void changeLinkColumn(DataTableInfo sourceTable, ColumnSpec sourceColumn, long targetEnvironmentId) {
        try {
            workspaceDataTableFacade.changeLinkColumn(
                sourceTable.id(), sourceColumn.name(), Objects.requireNonNull(sourceColumn.link()),
                targetEnvironmentId);
        } catch (DataAccessException | DataTableException exception) {
            throw schemaChangeFailed(
                "Changing link column '%s' of data table '%s' failed".formatted(
                    sourceColumn.name(), sourceTable.name()),
                exception);
        }
    }

    private static void checkEnvironments(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {

        if (sourceEnvironment == targetEnvironment) {
            throw new ConfigurationException(
                "Data table id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }
    }

    private List<DataTableInfo> collectPromotedTables(
        long sourceId, long workspaceId, Environment sourceEnvironment) {

        Map<Long, DataTableInfo> sourceTablesById = listTablesById(workspaceId, sourceEnvironment);

        DataTableInfo sourceTable = sourceTablesById.get(sourceId);

        if (sourceTable == null) {
            throw new ConfigurationException(
                "Data table id=%s does not exist in %s".formatted(sourceId, sourceEnvironment),
                EnvironmentPromotionErrorType.SOURCE_NOT_FOUND);
        }

        Set<Long> visitedTableIds = new LinkedHashSet<>();
        Deque<DataTableInfo> pendingTables = new ArrayDeque<>();
        List<DataTableInfo> promotedTables = new ArrayList<>();

        visitedTableIds.add(sourceId);
        pendingTables.add(sourceTable);

        while (!pendingTables.isEmpty()) {
            DataTableInfo promotedTable = pendingTables.removeFirst();

            promotedTables.add(promotedTable);

            for (ColumnSpec columnSpec : promotedTable.columns()) {
                LinkSpec linkSpec = columnSpec.link();

                if (linkSpec == null || !visitedTableIds.add(linkSpec.targetDataTableId())) {
                    continue;
                }

                DataTableInfo linkedTable = sourceTablesById.get(linkSpec.targetDataTableId());

                if (linkedTable == null) {
                    throw new ConfigurationException(
                        "Data table '%s' links to data table id=%s, which does not exist in %s".formatted(
                            promotedTable.name(), linkSpec.targetDataTableId(), sourceEnvironment),
                        EnvironmentPromotionErrorType.SOURCE_NOT_FOUND);
                }

                pendingTables.add(linkedTable);
            }
        }

        return promotedTables;
    }

    private void createTargetTable(
        DataTableInfo sourceTable, long workspaceId, long targetEnvironmentId, Set<Long> existingTargetTableIds) {

        List<ColumnSpec> userColumns = userColumns(sourceTable);

        if (userColumns.isEmpty()) {
            throw new ConfigurationException(
                "Data table '%s' has no columns, so it cannot be created; add a column to it first"
                    .formatted(sourceTable.name()),
                EnvironmentPromotionErrorType.SCHEMA_CHANGE_FAILED);
        }

        List<ColumnSpec> initialColumns = new ArrayList<>();

        for (ColumnSpec columnSpec : userColumns) {
            if (columnSpec.type() != ColumnType.LINK) {
                initialColumns.add(columnSpec);
            }
        }

        if (initialColumns.isEmpty()) {
            for (ColumnSpec columnSpec : userColumns) {
                LinkSpec linkSpec = Objects.requireNonNull(columnSpec.link());

                if (existingTargetTableIds.contains(linkSpec.targetDataTableId())) {
                    initialColumns.add(columnSpec);
                }
            }
        }

        if (initialColumns.isEmpty()) {
            throw new ConfigurationException(
                "Data table '%s' cannot be created before the tables it links to, as it has no other column"
                    .formatted(sourceTable.name()),
                EnvironmentPromotionErrorType.SCHEMA_CHANGE_FAILED);
        }

        String description = sourceTable.description();

        try {
            workspaceDataTableFacade.createTable(
                sourceTable.name(), description == null ? "" : description, initialColumns, workspaceId,
                targetEnvironmentId);
        } catch (DataAccessException | DataTableException exception) {
            throw schemaChangeFailed("Creating data table '%s' failed".formatted(sourceTable.name()), exception);
        }
    }

    private Map<Long, DataTableInfo> listTablesById(long workspaceId, Environment environment) {
        Map<Long, DataTableInfo> tablesById = new LinkedHashMap<>();

        for (DataTableInfo dataTableInfo : workspaceDataTableFacade.listTables(workspaceId, environment.ordinal())) {
            tablesById.put(dataTableInfo.id(), dataTableInfo);
        }

        return tablesById;
    }

    private void removeColumn(DataTableInfo sourceTable, String columnName, long targetEnvironmentId) {
        try {
            workspaceDataTableFacade.removeColumn(sourceTable.id(), columnName, targetEnvironmentId);
        } catch (DataAccessException | DataTableException exception) {
            throw schemaChangeFailed(
                "Removing column '%s' from data table '%s' failed".formatted(columnName, sourceTable.name()),
                exception);
        }
    }

    private static ConfigurationException schemaChangeFailed(String message, RuntimeException exception) {
        return new ConfigurationException(message, exception, EnvironmentPromotionErrorType.SCHEMA_CHANGE_FAILED);
    }

    private static ColumnSpec findColumn(DataTableInfo dataTableInfo, PromotionSchemaChange schemaChange) {
        String columnName = Objects.requireNonNull(schemaChange.columnName());

        for (ColumnSpec columnSpec : dataTableInfo.columns()) {
            if (columnName.equalsIgnoreCase(columnSpec.name())) {
                return columnSpec;
            }
        }

        throw new IllegalStateException(
            "Column '%s' not found on data table '%s'".formatted(columnName, dataTableInfo.name()));
    }

    private void syncLinkColumns(DataTableInfo sourceTable, List<ColumnSpec> targetColumns, long targetEnvironmentId) {
        for (PromotionSchemaChange schemaChange : DataTableSchemaDiff.diff(
            sourceTable.name(), targetColumns, sourceTable.columns())) {

            if (schemaChange.kind() == Kind.ADD_COLUMN) {
                addColumn(sourceTable, findColumn(sourceTable, schemaChange), targetEnvironmentId);
            } else if (schemaChange.kind() == Kind.CHANGE_LINK) {
                changeLinkColumn(sourceTable, findColumn(sourceTable, schemaChange), targetEnvironmentId);
            }
        }
    }

    private void syncScalarColumns(
        DataTableInfo sourceTable, List<ColumnSpec> targetColumns, long targetEnvironmentId) {

        for (PromotionSchemaChange schemaChange : DataTableSchemaDiff.diff(
            sourceTable.name(), targetColumns, sourceTable.columns())) {

            Kind kind = schemaChange.kind();

            if (kind == Kind.DROP_COLUMN) {
                removeColumn(sourceTable, Objects.requireNonNull(schemaChange.columnName()), targetEnvironmentId);
            } else if (kind == Kind.ADD_COLUMN) {
                ColumnSpec sourceColumn = findColumn(sourceTable, schemaChange);

                if (sourceColumn.type() != ColumnType.LINK) {
                    addColumn(sourceTable, sourceColumn, targetEnvironmentId);
                }
            } else if (kind == Kind.CHANGE_COLUMN_TYPE) {
                changeColumnType(
                    sourceTable, schemaChange, findColumn(sourceTable, schemaChange), targetEnvironmentId);
            }
        }
    }

    private static List<ColumnSpec> userColumns(DataTableInfo dataTableInfo) {
        List<ColumnSpec> userColumns = new ArrayList<>();

        for (ColumnSpec columnSpec : dataTableInfo.columns()) {
            if (!ReservedColumns.isReserved(columnSpec.name())) {
                userColumns.add(columnSpec);
            }
        }

        return userColumns;
    }
}
