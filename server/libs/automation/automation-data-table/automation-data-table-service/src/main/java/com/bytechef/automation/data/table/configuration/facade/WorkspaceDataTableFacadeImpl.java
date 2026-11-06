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

package com.bytechef.automation.data.table.configuration.facade;

import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.data.table.audit.DataTableAuditEvents;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceDataTableFacadeImpl implements WorkspaceDataTableFacade {

    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;
    private final DataTableTagService dataTableTagService;
    private final WorkspaceDataTableService workspaceDataTableService;

    @SuppressFBWarnings("EI")
    public WorkspaceDataTableFacadeImpl(
        DataTableRowService dataTableRowService, DataTableService dataTableService,
        DataTableTagService dataTableTagService, WorkspaceDataTableService workspaceDataTableService) {

        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
        this.dataTableTagService = dataTableTagService;
        this.workspaceDataTableService = workspaceDataTableService;
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId) {
        dataTableService.addColumn(dataTableService.getBaseNameById(dataTableId), columnSpec, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public long clearRows(long dataTableId, long environmentId) {
        List<DataTableRow> deletedDataTableRows = dataTableRowService.deleteAllRows(
            dataTableService.getBaseNameById(dataTableId), environmentId);

        return deletedDataTableRows.size();
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_CREATE')")
    @Audited(event = DataTableAuditEvents.DATA_TABLE_CREATED, data = {
        @AuditData(key = "baseName", value = "#baseName"),
        @AuditData(key = "workspaceId", value = "#workspaceId"),
        @AuditData(key = "environmentId", value = "#environmentId")
    })
    public void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId) {

        dataTableService.createTable(baseName, description, columnSpecs, environmentId);

        long dataTableId = dataTableService.getIdByBaseName(baseName);

        workspaceDataTableService.assignDataTableToWorkspace(dataTableId, workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public boolean deleteRow(long dataTableId, long rowId, long environmentId) {
        return dataTableRowService.deleteRow(dataTableService.getBaseNameById(dataTableId), rowId, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public boolean deleteRowByExternalId(long dataTableId, String externalId, long environmentId) {
        String baseName = dataTableService.getBaseNameById(dataTableId);

        return dataTableRowService.fetchRowByExternalId(baseName, externalId, environmentId)
            .map(dataTableRow -> dataTableRowService.deleteRow(baseName, dataTableRow.id(), environmentId))
            .orElse(false);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public List<Long> deleteRows(long dataTableId, List<Long> rowIds, long environmentId) {
        if (rowIds.isEmpty()) {
            return List.of();
        }

        DataTableRowConditions conditions = DataTableRowConditions.of(
            List.of(new DataTableRowFilter("id", DataTableRowFilter.Operation.IN, List.copyOf(rowIds))));

        return dataTableRowService.deleteRows(dataTableService.getBaseNameById(dataTableId), conditions, environmentId)
            .stream()
            .map(DataTableRow::id)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void dropTable(long dataTableId, long environmentId) {
        dataTableService.dropTable(dataTableService.getBaseNameById(dataTableId), environmentId);
    }

    @Override
    @Audited(event = DataTableAuditEvents.DATA_TABLE_DUPLICATED, data = {
        @AuditData(key = "sourceDataTableId", value = "#tableId"),
        @AuditData(key = "newBaseName", value = "#newBaseName"),
        @AuditData(key = "environmentId", value = "#environmentId")
    })
    public void duplicateTable(long tableId, String newBaseName, long environmentId) {
        String baseName = dataTableService.getBaseNameById(tableId);

        dataTableService.duplicateTable(baseName, newBaseName, environmentId);

        long duplicatedDataTableId = dataTableService.getIdByBaseName(newBaseName);

        List<WorkspaceDataTable> sourceWorkspaceDataTables =
            workspaceDataTableService.getDataTableWorkspaceDataTables(tableId);

        for (WorkspaceDataTable workspaceDataTable : sourceWorkspaceDataTables) {
            workspaceDataTableService.assignDataTableToWorkspace(
                duplicatedDataTableId, workspaceDataTable.getWorkspaceId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public String exportCsv(long dataTableId, long environmentId) {
        return dataTableRowService.exportCsv(dataTableService.getBaseNameById(dataTableId), environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public Optional<DataTableRow> fetchRowByExternalId(long dataTableId, String externalId, long environmentId) {
        return dataTableRowService.fetchRowByExternalId(
            dataTableService.getBaseNameById(dataTableId), externalId, environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public DataTableRow getRow(long dataTableId, long rowId, long environmentId) {
        DataTableRow dataTableRow = dataTableRowService.getRow(
            dataTableService.getBaseNameById(dataTableId), rowId, environmentId);

        if (dataTableRow == null) {
            throw new DataTableException("Row not found: id=" + rowId, DataTableErrorType.ROW_NOT_FOUND);
        }

        return dataTableRow;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public DataTableInfo getTable(long dataTableId, long environmentId) {
        String baseName = dataTableService.getBaseNameById(dataTableId);

        return dataTableService.fetchDataTableInfo(baseName, environmentId)
            .orElseThrow(() -> new DataTableException(
                "Data table '" + baseName + "' does not exist in this environment",
                DataTableErrorType.DATA_TABLE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_VIEW')")
    public Map<Long, List<Tag>> getTagsByTableId(long workspaceId) {
        List<Long> dataTableIds = workspaceDataTableService.getWorkspaceDataTables(workspaceId)
            .stream()
            .map(WorkspaceDataTable::getDataTableId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<String, List<Tag>> tagsByTableName = dataTableTagService.getTagsByTableName();

        return dataTableIds.stream()
            .collect(Collectors.toMap(
                dataTableId -> dataTableId,
                dataTableId -> tagsByTableName.getOrDefault(
                    dataTableService.getBaseNameById(dataTableId), List.of())));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public long getWorkspaceId(long dataTableId) {
        List<WorkspaceDataTable> workspaceDataTables =
            workspaceDataTableService.getDataTableWorkspaceDataTables(dataTableId);

        if (workspaceDataTables.isEmpty()) {
            throw new DataTableException(
                "Data table not found: id=" + dataTableId, DataTableErrorType.DATA_TABLE_NOT_FOUND);
        }

        WorkspaceDataTable workspaceDataTable = workspaceDataTables.getFirst();

        return workspaceDataTable.getWorkspaceId();
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public int importCsv(long dataTableId, String csv, long environmentId) {
        return dataTableRowService.importCsv(dataTableService.getBaseNameById(dataTableId), csv, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public DataTableRow insertRow(
        long dataTableId, Map<String, Object> values, @Nullable String externalId, long environmentId) {

        return dataTableRowService.insertRow(
            dataTableService.getBaseNameById(dataTableId), values, externalId, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public List<DataTableRow> insertRows(
        long dataTableId, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId) {

        return dataTableRowService.insertRows(
            dataTableService.getBaseNameById(dataTableId), newRows, createStrategy, environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public Page<DataTableRow> listRows(
        long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int pageNumber,
        int pageSize, long environmentId) {

        String baseName = dataTableService.getBaseNameById(dataTableId);

        List<DataTableRow> dataTableRows = dataTableRowService.queryRows(
            baseName, conditions, sorts, pageSize, pageNumber * pageSize, environmentId);
        long total = dataTableRowService.countRows(baseName, conditions, environmentId);

        return new PageImpl<>(dataTableRows, PageRequest.of(pageNumber, pageSize), total);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_VIEW')")
    public List<DataTableInfo> listTables(long workspaceId, long environmentId) {
        List<DataTableInfo> dataTableInfos = dataTableService.listTables(environmentId);

        Set<Long> dataTableIds = workspaceDataTableService.getWorkspaceDataTables(workspaceId)
            .stream()
            .map(WorkspaceDataTable::getDataTableId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return dataTableInfos.stream()
            .filter(dataTableInfo -> dataTableInfo.id() != null && dataTableIds.contains(dataTableInfo.id()))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void removeColumn(long dataTableId, String columnName, long environmentId) {
        dataTableService.removeColumn(dataTableService.getBaseNameById(dataTableId), columnName, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void renameColumn(long dataTableId, String fromColumnName, String toColumnName, long environmentId) {
        dataTableService.renameColumn(
            dataTableService.getBaseNameById(dataTableId), fromColumnName, toColumnName, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void updateDescription(long dataTableId, @Nullable String description) {
        dataTableService.updateDescription(dataTableService.getBaseNameById(dataTableId), description);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public DataTableRow updateRow(
        long dataTableId, long rowId, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId) {

        return dataTableRowService.updateRow(
            dataTableService.getBaseNameById(dataTableId), rowId, values, externalIdPatch, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void updateTags(long dataTableId, List<Tag> tags) {
        dataTableTagService.updateTags(dataTableId, tags);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public UpsertResult upsertRow(long dataTableId, String externalId, Map<String, Object> values, long environmentId) {
        return dataTableRowService.upsertRow(
            dataTableService.getBaseNameById(dataTableId), externalId, values, environmentId);
    }
}
