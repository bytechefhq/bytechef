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
import com.bytechef.platform.data.table.domain.ColumnSpec;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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

    private final DataTableService dataTableService;
    private final WorkspaceDataTableService workspaceDataTableService;

    @SuppressFBWarnings("EI")
    public WorkspaceDataTableFacadeImpl(
        DataTableService dataTableService, WorkspaceDataTableService workspaceDataTableService) {

        this.dataTableService = dataTableService;
        this.workspaceDataTableService = workspaceDataTableService;
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable:ResourceRole', 'EDITOR')")
    public void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId) {
        dataTableService.addColumn(dataTableService.getBaseNameById(dataTableId), columnSpec, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'EDITOR')")
    public void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId) {

        dataTableService.createTable(baseName, description, columnSpecs, environmentId);

        long dataTableId = dataTableService.getIdByBaseName(baseName);

        workspaceDataTableService.assignDataTableToWorkspace(dataTableId, workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable:ResourceRole', 'EDITOR')")
    public void dropTable(long dataTableId, long environmentId) {
        dataTableService.dropTable(dataTableService.getBaseNameById(dataTableId), environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable:ResourceRole', 'EDITOR')")
    public void duplicateTable(long dataTableId, String newBaseName, long environmentId) {
        dataTableService.duplicateTable(dataTableService.getBaseNameById(dataTableId), newBaseName, environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')")
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
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable:ResourceRole', 'EDITOR')")
    public void removeColumn(long dataTableId, String columnName, long environmentId) {
        dataTableService.removeColumn(dataTableService.getBaseNameById(dataTableId), columnName, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable:ResourceRole', 'EDITOR')")
    public void renameColumn(long dataTableId, String fromColumnName, String newName, long environmentId) {
        dataTableService.renameColumn(
            dataTableService.getBaseNameById(dataTableId), fromColumnName, newName, environmentId);
    }

    @Override
    @PreAuthorize("hasPermission(#dataTableId, 'DataTable:ResourceRole', 'EDITOR')")
    public void renameTable(long dataTableId, String newBaseName, long environmentId) {
        dataTableService.renameTable(dataTableService.getBaseNameById(dataTableId), newBaseName, environmentId);
    }
}
