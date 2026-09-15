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
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService.Webhook;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final DataTableWebhookService dataTableWebhookService;
    private final WorkspaceDataTableService workspaceDataTableService;

    @SuppressFBWarnings("EI")
    public WorkspaceDataTableFacadeImpl(
        DataTableRowService dataTableRowService, DataTableService dataTableService,
        DataTableTagService dataTableTagService, DataTableWebhookService dataTableWebhookService,
        WorkspaceDataTableService workspaceDataTableService) {

        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
        this.dataTableTagService = dataTableTagService;
        this.dataTableWebhookService = dataTableWebhookService;
        this.workspaceDataTableService = workspaceDataTableService;
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_EDIT', #environmentId)")
    public void addColumn(long tableId, ColumnSpec columnSpec, long environmentId) {
        dataTableService.addColumn(dataTableService.getBaseNameById(tableId), columnSpec, environmentId);
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'DATA_TABLE_CREATE', #environmentId)")
    public void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId) {

        dataTableService.createTable(baseName, description, columnSpecs, environmentId);

        long dataTableId = dataTableService.getIdByBaseName(baseName);

        workspaceDataTableService.assignDataTableToWorkspace(dataTableId, workspaceId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_EDIT', #environmentId)")
    public boolean deleteRow(long tableId, long rowId, long environmentId) {
        return dataTableRowService.deleteRow(dataTableService.getBaseNameById(tableId), rowId, environmentId);
    }

    // Keyed on the table id rather than the base name the service takes, because the base name encodes an environment
    // and is not what the relation is keyed on.
    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_DELETE', #environmentId)")
    public void dropTable(long tableId, long environmentId) {
        dataTableService.dropTable(dataTableService.getBaseNameById(tableId), environmentId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_CREATE', #environmentId)")
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
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public String exportCsv(long tableId, long environmentId) {
        return dataTableRowService.exportCsv(dataTableService.getBaseNameById(tableId), environmentId);
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'DATA_TABLE_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public Map<Long, List<Tag>> getTagsByTableId(long workspaceId, long environmentId) {
        Map<String, Long> tableIdsByBaseName = new HashMap<>();

        for (DataTableInfo dataTableInfo : listWorkspaceTables(workspaceId, environmentId)) {
            if (dataTableInfo.id() != null) {
                tableIdsByBaseName.put(dataTableInfo.baseName(), dataTableInfo.id());
            }
        }

        Map<String, List<Tag>> tagsByTableName = dataTableTagService.getTagsByTableName();
        Map<Long, List<Tag>> tagsByTableId = new LinkedHashMap<>();

        for (Map.Entry<String, List<Tag>> entry : tagsByTableName.entrySet()) {
            Long tableId = tableIdsByBaseName.get(entry.getKey());

            if (tableId != null) {
                tagsByTableId.put(tableId, entry.getValue());
            }
        }

        return tagsByTableId;
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_EDIT', #environmentId)")
    public void importCsv(long tableId, String csv, long environmentId) {
        dataTableRowService.importCsv(dataTableService.getBaseNameById(tableId), csv, environmentId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_EDIT', #environmentId)")
    public DataTableRow insertRow(long tableId, Map<String, Object> values, long environmentId) {
        return dataTableRowService.insertRow(dataTableService.getBaseNameById(tableId), values, environmentId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<DataTableRow> listRows(long tableId, int limit, int offset, long environmentId) {
        return dataTableRowService.listRows(dataTableService.getBaseNameById(tableId), limit, offset, environmentId);
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'DATA_TABLE_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<DataTableInfo> listTables(long workspaceId, long environmentId) {
        return listWorkspaceTables(workspaceId, environmentId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<Webhook> listWebhooks(long tableId, long environmentId) {
        return dataTableWebhookService.listWebhooks(dataTableService.getBaseNameById(tableId), environmentId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_EDIT', #environmentId)")
    public void removeColumn(long tableId, String columnName, long environmentId) {
        dataTableService.removeColumn(dataTableService.getBaseNameById(tableId), columnName, environmentId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_EDIT', #environmentId)")
    public void renameColumn(long tableId, String fromColumnName, String toColumnName, long environmentId) {
        dataTableService.renameColumn(
            dataTableService.getBaseNameById(tableId), fromColumnName, toColumnName, environmentId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_EDIT', #environmentId)")
    public void renameTable(long tableId, String newBaseName, long environmentId) {
        dataTableService.renameTable(dataTableService.getBaseNameById(tableId), newBaseName, environmentId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironmentId(#tableId, 'DataTable', 'DATA_TABLE_EDIT', #environmentId)")
    public DataTableRow updateRow(long tableId, long rowId, Map<String, Object> values, long environmentId) {
        return dataTableRowService.updateRow(dataTableService.getBaseNameById(tableId), rowId, values, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#tableId, 'DataTable', 'DATA_TABLE_EDIT')")
    public void updateTags(long tableId, List<Tag> tags) {
        dataTableTagService.updateTags(tableId, tags);
    }

    private List<DataTableInfo> listWorkspaceTables(long workspaceId, long environmentId) {
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
}
