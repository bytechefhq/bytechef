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

import com.bytechef.automation.data.table.configuration.domain.DataTableLinkOption;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.data.table.audit.DataTableAuditEvents;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService.Webhook;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.facade.DataTableRowFacade;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final int DEFAULT_LINK_OPTIONS_LIMIT = 20;
    private static final int MAX_LINK_OPTIONS_LIMIT = 100;
    private static final Pattern ROW_ID_SEARCH_PATTERN = Pattern.compile("#?(\\d{1,18})");

    private final DataTableRowFacade dataTableRowFacade;
    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;
    private final DataTableStorageService dataTableStorageService;
    private final DataTableTagService dataTableTagService;
    private final DataTableWebhookService dataTableWebhookService;

    @SuppressFBWarnings("EI")
    public WorkspaceDataTableFacadeImpl(
        DataTableRowFacade dataTableRowFacade, DataTableRowService dataTableRowService,
        DataTableService dataTableService, DataTableStorageService dataTableStorageService,
        DataTableTagService dataTableTagService, DataTableWebhookService dataTableWebhookService) {

        this.dataTableRowFacade = dataTableRowFacade;
        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
        this.dataTableStorageService = dataTableStorageService;
        this.dataTableTagService = dataTableTagService;
        this.dataTableWebhookService = dataTableWebhookService;
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId) {
        dataTableService.addColumn(dataTableId, columnSpec, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_CREATE')")
    @Audited(event = DataTableAuditEvents.DATA_TABLE_CREATED, data = {
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "workspaceId", value = "#workspaceId"),
        @AuditData(key = "environmentId", value = "#environmentId")
    })
    public long createTable(
        String name, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId) {

        return dataTableService.createTable(workspaceId, name, description, columnSpecs, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void dropTable(long dataTableId, long environmentId) {
        dataTableService.dropTable(dataTableId, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    @Audited(event = DataTableAuditEvents.DATA_TABLE_DUPLICATED, data = {
        @AuditData(key = "sourceDataTableId", value = "#dataTableId"),
        @AuditData(key = "newName", value = "#newName"),
        @AuditData(key = "environmentId", value = "#environmentId")
    })
    public long duplicateTable(long dataTableId, String newName, long environmentId) {
        Optional<DataTableInfo> dataTableInfoOptional = dataTableService.fetchDataTableInfo(dataTableId, environmentId);

        if (dataTableInfoOptional.isEmpty()) {
            throw new DataTableException(
                "Data table does not exist in this environment: id=" + dataTableId,
                DataTableErrorType.DATA_TABLE_NOT_FOUND);
        }

        dataTableStorageService.checkLimit(dataTableStorageService.getTableBytes(dataTableId, environmentId));

        return dataTableService.duplicateTable(dataTableId, newName, environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_VIEW')")
    public List<Tag> getDataTableTags(long workspaceId) {
        List<Long> dataTableIds = dataTableService.getWorkspaceDataTables(workspaceId)
            .stream()
            .map(DataTable::getId)
            .toList();

        return dataTableTagService.getTags(dataTableIds);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_VIEW')")
    public List<DataTableInfo> listTables(long workspaceId, long environmentId) {
        return dataTableService.listTables(workspaceId, environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public DataTableStorageUsage getStorageUsage() {
        return dataTableStorageService.getUsage();
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void removeColumn(long dataTableId, String columnName, long environmentId) {
        dataTableService.removeColumn(dataTableId, columnName, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void renameColumn(long dataTableId, String fromColumnName, String newName, long environmentId) {
        dataTableService.renameColumn(
            dataTableId, fromColumnName, newName, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void renameTable(long dataTableId, String newName) {
        dataTableService.renameTable(dataTableId, newName);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public List<DataTableRow> listRows(long dataTableId, int limit, int offset, long environmentId) {
        return dataTableRowService.listRows(dataTableId, limit, offset,
            environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public List<DataTableRow> listRows(
        long dataTableId, int limit, int offset, List<String> expand, long environmentId) {

        return dataTableRowService.findRows(
            dataTableId, DataTableRowConditions.matchAll(), List.of(), limit, offset, expand, environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public List<DataTableLinkOption> getLinkOptions(
        long dataTableId, String columnName, @Nullable String search, int limit, long environmentId) {

        DataTableInfo dataTableInfo = dataTableService.fetchDataTableInfo(dataTableId, environmentId)
            .orElseThrow(() -> new DataTableException(
                "Data table does not exist in this environment: id=" + dataTableId,
                DataTableErrorType.DATA_TABLE_NOT_FOUND));

        LinkSpec linkSpec = null;

        for (ColumnSpec columnSpec : dataTableInfo.columns()) {
            if (columnSpec.type() == ColumnType.LINK && columnSpec.name()
                .equalsIgnoreCase(columnName)) {

                linkSpec = columnSpec.link();

                break;
            }
        }

        if (linkSpec == null) {
            throw new DataTableException(
                "Link column does not exist: " + columnName, DataTableErrorType.COLUMN_NOT_FOUND);
        }

        String displayColumnName = linkSpec.displayColumnName();
        List<List<DataTableRowFilter>> conditionGroups = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            if (displayColumnName != null
                && isTextColumn(linkSpec.targetDataTableId(), displayColumnName, environmentId)) {
                conditionGroups.add(
                    List.of(new DataTableRowFilter(displayColumnName, Operation.CONTAINS_IGNORE_CASE, search.trim())));
            }

            Matcher rowIdMatcher = ROW_ID_SEARCH_PATTERN.matcher(search.trim());

            if (rowIdMatcher.matches()) {
                conditionGroups.add(
                    List.of(new DataTableRowFilter("id", Operation.EQUALS, Long.valueOf(rowIdMatcher.group(1)))));
            }
        }

        DataTableRowConditions conditions = conditionGroups.isEmpty()
            ? DataTableRowConditions.matchAll() : DataTableRowConditions.anyOf(conditionGroups);

        int pageSize = limit <= 0 ? DEFAULT_LINK_OPTIONS_LIMIT : Math.min(limit, MAX_LINK_OPTIONS_LIMIT);

        List<DataTableRow> targetRows = dataTableRowService.findRows(
            linkSpec.targetDataTableId(), conditions, List.of(), pageSize, 0, environmentId);

        List<DataTableLinkOption> dataTableLinkOptions = new ArrayList<>();

        for (DataTableRow targetRow : targetRows) {
            String label = "#" + targetRow.id();

            if (displayColumnName != null) {
                Map<String, Object> targetValues = targetRow.values();
                Object displayValue = targetValues.get(displayColumnName);

                if (displayValue != null && !displayValue.toString()
                    .isEmpty()) {

                    label = displayValue.toString();
                }
            }

            dataTableLinkOptions.add(new DataTableLinkOption(targetRow.id(), label));
        }

        return dataTableLinkOptions;
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public DataTableRow insertRow(long dataTableId, Map<String, Object> values, long environmentId) {
        return dataTableRowFacade.insertRow(dataTableId, values, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public DataTableRow updateRow(long dataTableId, long rowId, Map<String, Object> values, long environmentId) {
        return dataTableRowFacade.updateRow(
            dataTableId, rowId, values, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public boolean deleteRow(long dataTableId, long rowId, long environmentId) {
        return dataTableRowService.deleteRow(dataTableId, rowId, environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public String exportCsv(long dataTableId, long environmentId) {
        return dataTableRowService.exportCsv(dataTableId, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public int importCsv(long dataTableId, String csv, long environmentId) {
        return dataTableRowFacade.importCsv(dataTableId, csv, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void updateTags(long dataTableId, List<Tag> tags) {
        dataTableTagService.updateTags(dataTableId, tags);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public List<Webhook> listWebhooks(long dataTableId, long environmentId) {
        return dataTableWebhookService.listWebhooks(dataTableId, environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public DataTableInfo getTable(long dataTableId, long environmentId) {
        return dataTableService.fetchDataTableInfo(dataTableId, environmentId)
            .orElseThrow(() -> new DataTableException(
                "Data table does not exist in this environment: id=" + dataTableId,
                DataTableErrorType.DATA_TABLE_NOT_FOUND));
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void updateDescription(long dataTableId, @Nullable String description) {
        dataTableService.updateDescription(dataTableId, description);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public Page<DataTableRow> listRows(
        long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int pageNumber,
        int pageSize, long environmentId) {

        return listRows(dataTableId, conditions, sorts, pageNumber, pageSize, List.of(), environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public Page<DataTableRow> listRows(
        long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int pageNumber,
        int pageSize, List<String> expand, long environmentId) {

        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);

        long total = dataTableRowService.countRows(dataTableId, conditions, environmentId);
        long offset = pageRequest.getOffset();

        if (offset >= total || offset > Integer.MAX_VALUE) {
            return new PageImpl<>(List.of(), pageRequest, total);
        }

        List<DataTableRow> dataTableRows = dataTableRowService.findRows(
            dataTableId, conditions, sorts, pageSize, (int) offset, expand, environmentId);

        return new PageImpl<>(dataTableRows, pageRequest, total);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public DataTableRow getRow(long dataTableId, long rowId, long environmentId) {
        return getRow(dataTableId, rowId, List.of(), environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public DataTableRow getRow(long dataTableId, long rowId, List<String> expand, long environmentId) {
        DataTableRow dataTableRow = dataTableRowService.getRow(dataTableId, rowId, expand, environmentId);

        if (dataTableRow == null) {
            throw new DataTableException("Row not found: id=" + rowId, DataTableErrorType.ROW_NOT_FOUND);
        }

        return dataTableRow;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public Optional<DataTableRow> fetchRowByExternalId(long dataTableId, String externalId, long environmentId) {
        return dataTableRowService.fetchRowByExternalId(
            dataTableId, externalId, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public DataTableRow insertRow(
        long dataTableId, Map<String, Object> values, @Nullable String externalId, long environmentId) {

        return dataTableRowFacade.insertRow(
            dataTableId, values, externalId, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public DataTableRow updateRow(
        long dataTableId, long rowId, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId) {

        if (dataTableRowService.getRow(dataTableId, rowId, environmentId) == null) {
            throw new DataTableException("Row not found: id=" + rowId, DataTableErrorType.ROW_NOT_FOUND);
        }

        return dataTableRowFacade.updateRow(dataTableId, rowId, values, externalIdPatch, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public UpsertResult upsertRow(long dataTableId, String externalId, Map<String, Object> values, long environmentId) {
        return dataTableRowFacade.upsertRow(
            dataTableId, externalId, values, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public boolean deleteRowByExternalId(long dataTableId, String externalId, long environmentId) {
        Optional<DataTableRow> dataTableRowOptional = dataTableRowService.fetchRowByExternalId(
            dataTableId, externalId, environmentId);

        return dataTableRowOptional
            .map(dataTableRow -> dataTableRowService.deleteRow(dataTableId, dataTableRow.id(), environmentId))
            .orElse(false);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public List<DataTableRow> insertRows(
        long dataTableId, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId) {

        return dataTableRowFacade.insertRows(
            dataTableId, newRows, createStrategy, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public List<Long> deleteRows(long dataTableId, List<Long> rowIds, long environmentId) {
        if (rowIds.isEmpty()) {
            return List.of();
        }

        List<DataTableRow> deletedDataTableRows = dataTableRowService.deleteRows(
            dataTableId,
            DataTableRowConditions.of(List.of(new DataTableRowFilter("id", Operation.IN, rowIds))), environmentId);

        return deletedDataTableRows.stream()
            .map(DataTableRow::id)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public long clearRows(long dataTableId, long environmentId) {
        List<DataTableRow> deletedDataTableRows = dataTableRowService.deleteAllRows(
            dataTableId, environmentId);

        return deletedDataTableRows.size();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_VIEW')")
    public Map<Long, List<Tag>> getTagsByTableId(long workspaceId) {
        Map<Long, List<Tag>> tagsByTableId = dataTableTagService.getTagsByTableId();

        return dataTableService.getWorkspaceDataTables(workspaceId)
            .stream()
            .collect(Collectors.toMap(
                DataTable::getId, dataTable -> tagsByTableId.getOrDefault(dataTable.getId(), List.of())));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')")
    public long getWorkspaceId(long dataTableId) {
        DataTable dataTable = dataTableService.getDataTable(dataTableId);

        Long workspaceId = dataTable.getWorkspaceId();

        if (workspaceId == null) {
            throw new DataTableException(
                "Data table not found: id=" + dataTableId, DataTableErrorType.DATA_TABLE_NOT_FOUND);
        }

        return workspaceId;
    }

    private boolean isTextColumn(long dataTableId, String columnName, long environmentId) {
        Optional<DataTableInfo> dataTableInfoOptional = dataTableService.fetchDataTableInfo(
            dataTableId, environmentId);

        if (dataTableInfoOptional.isEmpty()) {
            return false;
        }

        DataTableInfo dataTableInfo = dataTableInfoOptional.get();

        for (ColumnSpec columnSpec : dataTableInfo.columns()) {
            if (columnSpec.name()
                .equalsIgnoreCase(columnName)) {

                return columnSpec.type() == ColumnType.STRING;
            }
        }

        return false;
    }
}
