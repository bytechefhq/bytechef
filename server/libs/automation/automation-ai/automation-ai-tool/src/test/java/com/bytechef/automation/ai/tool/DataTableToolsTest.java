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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class DataTableToolsTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long WORKSPACE_ID = 7;

    private final ToolContext toolContext = new ToolContext(
        Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID,
            AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, ENVIRONMENT_ID));
    private final WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

    @Test
    void testListDataTablesReturnsTheNamesADataTableTaskTakes() {
        when(workspaceDataTableFacade.listTables(WORKSPACE_ID, ENVIRONMENT_ID))
            .thenReturn(List.of(invoices()));

        assertThat(dataTableTools(workspaceDataTableFacade).listDataTables(null, toolContext))
            .containsExactly(
                new DataTableTools.DataTableSummary(
                    "Invoices", "Invoice attachments",
                    List.of(
                        new DataTableTools.DataTableColumn("subject", "STRING"),
                        new DataTableTools.DataTableColumn("amount", "NUMBER"))));
    }

    @Test
    void testCreateDataTableCreatesTheTableAndReturnsIt() {
        when(workspaceDataTableFacade.listTables(WORKSPACE_ID, ENVIRONMENT_ID))
            .thenReturn(List.of(), List.of(invoices()));

        DataTableTools.DataTableSummary dataTableSummary = dataTableTools(workspaceDataTableFacade).createDataTable(
            " Invoices ",
            List.of(
                new DataTableTools.DataTableColumn("subject", null),
                new DataTableTools.DataTableColumn("amount", "number")),
            "Invoice attachments", null, toolContext);

        verify(workspaceDataTableFacade).createTable(
            "Invoices", "Invoice attachments",
            List.of(new ColumnSpec("subject", ColumnType.STRING), new ColumnSpec("amount", ColumnType.NUMBER)),
            WORKSPACE_ID, ENVIRONMENT_ID);

        assertThat(dataTableSummary.name()).isEqualTo("Invoices");
    }

    @Test
    void testCreateDataTableRefusesANameThatExistsAndAnUnknownColumnType() {
        when(workspaceDataTableFacade.listTables(WORKSPACE_ID, ENVIRONMENT_ID)).thenReturn(List.of(invoices()));

        DataTableTools dataTableTools = dataTableTools(workspaceDataTableFacade);

        List<DataTableTools.DataTableColumn> columns = List.of(new DataTableTools.DataTableColumn("subject", null));

        assertThatThrownBy(() -> dataTableTools.createDataTable("invoices", columns, null, null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("already exists");

        List<DataTableTools.DataTableColumn> badColumns = List.of(new DataTableTools.DataTableColumn("a", "MONEY"));

        assertThatThrownBy(() -> dataTableTools.createDataTable("Receipts", badColumns, null, null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Unknown column type 'MONEY'");

        verify(workspaceDataTableFacade, never()).createTable(anyString(), anyString(), anyList(), anyLong(),
            anyLong());
    }

    @Test
    void testToolsFailClearlyWhereDataTablesAreNotDeployed() {
        DataTableTools dataTableTools = dataTableTools(null);

        assertThatThrownBy(() -> dataTableTools.listDataTables(null, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("not available");
    }

    @SuppressWarnings("unchecked")
    private static DataTableTools dataTableTools(WorkspaceDataTableFacade workspaceDataTableFacade) {
        ObjectProvider<WorkspaceDataTableFacade> workspaceDataTableFacadeProvider = mock(ObjectProvider.class);

        when(workspaceDataTableFacadeProvider.getIfAvailable()).thenReturn(workspaceDataTableFacade);

        return new DataTableTools(workspaceDataTableFacadeProvider, mock(WorkspaceFacade.class));
    }

    private static DataTableInfo invoices() {
        return new DataTableInfo(
            1L, "Invoices", "Invoice attachments",
            List.of(new ColumnSpec("subject", ColumnType.STRING), new ColumnSpec("amount", ColumnType.NUMBER)),
            Instant.EPOCH);
    }
}
