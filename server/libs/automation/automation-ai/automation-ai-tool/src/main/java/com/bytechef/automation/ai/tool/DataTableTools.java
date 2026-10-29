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

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.data.table.configuration.exception.WorkspaceDataTableErrorType;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Lets a workflow-building agent find the data table a dataTable task should write to, or create it with the columns
 * the workflow needs.
 *
 * @author Ivica Cardic
 */
@Component
public class DataTableTools {

    private final ObjectProvider<WorkspaceDataTableFacade> workspaceDataTableFacadeProvider;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public DataTableTools(
        ObjectProvider<WorkspaceDataTableFacade> workspaceDataTableFacadeProvider, WorkspaceFacade workspaceFacade) {

        this.workspaceDataTableFacadeProvider = workspaceDataTableFacadeProvider;
        this.workspaceFacade = workspaceFacade;
    }

    @Tool(
        description = "List the data tables of the workspace with their columns. A dataTable task's table property " +
            "takes a table's name exactly as listed here.")
    public List<DataTableSummary> listDataTables(
        @ToolParam(
            required = false,
            description = AutomationToolInvocationContext.WORKSPACE_ID_DESCRIPTION) @Nullable Long workspaceId,
        ToolContext toolContext) {

        long resolvedWorkspaceId = requireWorkspaceId(
            workspaceId, toolContext, WorkspaceDataTableErrorType.LIST_DATA_TABLES);

        WorkspaceDataTableFacade workspaceDataTableFacade = getWorkspaceDataTableFacade(
            WorkspaceDataTableErrorType.LIST_DATA_TABLES);

        return workspaceDataTableFacade
            .listTables(resolvedWorkspaceId, AutomationToolInvocationContext.resolveEnvironmentId(null, toolContext))
            .stream()
            .map(DataTableTools::toDataTableSummary)
            .toList();
    }

    @Tool(
        description = "Create a data table when the workflow needs one that listDataTables does not show. Give it " +
            "the columns the workflow writes. Returns the table; use its name as the dataTable task's table property.")
    public DataTableSummary createDataTable(
        @ToolParam(description = "The table name, for example Invoices") String name,
        @ToolParam(description = "The columns of the table") List<DataTableColumn> columns,
        @ToolParam(required = false, description = "What the table stores") @Nullable String description,
        @ToolParam(
            required = false,
            description = AutomationToolInvocationContext.WORKSPACE_ID_DESCRIPTION) @Nullable Long workspaceId,
        ToolContext toolContext) {

        if (name == null || name.isBlank()) {
            throw new ExecutionException("name is required", WorkspaceDataTableErrorType.CREATE_DATA_TABLE);
        }

        if (columns == null || columns.isEmpty()) {
            throw new ExecutionException(
                "columns must list at least one column", WorkspaceDataTableErrorType.CREATE_DATA_TABLE);
        }

        long resolvedWorkspaceId = requireWorkspaceId(
            workspaceId, toolContext, WorkspaceDataTableErrorType.CREATE_DATA_TABLE);
        long environmentId = AutomationToolInvocationContext.resolveEnvironmentId(null, toolContext);

        String tableName = name.strip();

        WorkspaceDataTableFacade workspaceDataTableFacade = getWorkspaceDataTableFacade(
            WorkspaceDataTableErrorType.CREATE_DATA_TABLE);

        boolean exists = workspaceDataTableFacade.listTables(resolvedWorkspaceId, environmentId)
            .stream()
            .anyMatch(dataTableInfo -> tableName.equalsIgnoreCase(dataTableInfo.baseName()));

        if (exists) {
            throw new ExecutionException(
                "A data table named '" + tableName + "' already exists; use it instead of creating another.",
                WorkspaceDataTableErrorType.CREATE_DATA_TABLE);
        }

        workspaceDataTableFacade.createTable(
            tableName, description, columns.stream()
                .map(DataTableTools::toColumnSpec)
                .toList(),
            resolvedWorkspaceId, environmentId);

        return workspaceDataTableFacade.listTables(resolvedWorkspaceId, environmentId)
            .stream()
            .filter(dataTableInfo -> tableName.equalsIgnoreCase(dataTableInfo.baseName()))
            .findFirst()
            .map(DataTableTools::toDataTableSummary)
            .orElseThrow(() -> new ExecutionException(
                "The data table '" + tableName + "' was not found after it was created.",
                WorkspaceDataTableErrorType.CREATE_DATA_TABLE));
    }

    private static ColumnSpec toColumnSpec(DataTableColumn dataTableColumn) {
        if (dataTableColumn.name() == null || dataTableColumn.name()
            .isBlank()) {

            throw new ExecutionException("Every column needs a name", WorkspaceDataTableErrorType.CREATE_DATA_TABLE);
        }

        ColumnType columnType;

        try {
            columnType = ColumnType.valueOf(
                Objects.requireNonNullElse(dataTableColumn.type(), "STRING")
                    .strip()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ExecutionException(
                "Unknown column type '" + dataTableColumn.type() + "'. Use STRING, NUMBER, INTEGER, DATE, DATE_TIME " +
                    "or BOOLEAN.",
                exception, WorkspaceDataTableErrorType.CREATE_DATA_TABLE);
        }

        return new ColumnSpec(dataTableColumn.name()
            .strip(), columnType);
    }

    private static DataTableSummary toDataTableSummary(DataTableInfo dataTableInfo) {
        return new DataTableSummary(
            dataTableInfo.baseName(), dataTableInfo.description(), dataTableInfo.columns()
                .stream()
                .map(columnSpec -> new DataTableColumn(columnSpec.name(), columnSpec.type()
                    .name()))
                .toList());
    }

    private WorkspaceDataTableFacade getWorkspaceDataTableFacade(WorkspaceDataTableErrorType errorType) {
        WorkspaceDataTableFacade workspaceDataTableFacade = workspaceDataTableFacadeProvider.getIfAvailable();

        if (workspaceDataTableFacade == null) {
            throw new ExecutionException("Data tables are not available on this server.", errorType);
        }

        return workspaceDataTableFacade;
    }

    private long requireWorkspaceId(
        @Nullable Long workspaceId, ToolContext toolContext, WorkspaceDataTableErrorType errorType) {

        Long resolvedWorkspaceId;

        try {
            resolvedWorkspaceId = AutomationToolInvocationContext.resolveWorkspaceId(
                workspaceId, toolContext, workspaceFacade);
        } catch (WorkspaceNotAccessibleException exception) {
            throw new ExecutionException(exception.getMessage(), exception, errorType);
        }

        if (resolvedWorkspaceId == null) {
            throw new ExecutionException(
                "workspaceId is required - call listWorkspaces to find the workspace id.", errorType);
        }

        return resolvedWorkspaceId;
    }

    @SuppressFBWarnings("EI")
    public record DataTableColumn(
        @JsonPropertyDescription("The column name") String name,
        @JsonPropertyDescription("STRING, NUMBER, INTEGER, DATE, DATE_TIME or BOOLEAN; STRING when omitted") @Nullable String type) {
    }

    @SuppressFBWarnings("EI")
    public record DataTableSummary(
        @JsonPropertyDescription("The table name; a dataTable task's table property takes it as is") String name,
        @Nullable String description, List<DataTableColumn> columns) {
    }
}
