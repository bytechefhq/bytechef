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

package com.bytechef.automation.ai.tool.datatable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class CreateDataTableToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private static ToolContext toolContextWithWorkspace() {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(1L)
                .environmentId(0L)
                .build()
                .toToolContext());
    }

    @Test
    void testCreatesEmptyTableWithExplicitSchemaAndResolvesId() throws Exception {
        // The whole point of this tool: a typed schema goes straight to createTable with NO seed rows, unlike the
        // CSV path whose type inference forced the agent to insert-then-delete a placeholder row (polluting the
        // task artifact log). Also pins the id resolution by base name, since createTable returns void.
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        when(workspaceDataTableFacade.listTables(1L, 0L)).thenReturn(List.of(
            new DataTableInfo(41L, "other_table", "", List.of(), null, null),
            new DataTableInfo(42L, "invoices", "", List.of(), null, null)));

        CreateDataTableToolCallback callback = new CreateDataTableToolCallback(workspaceDataTableFacade);

        String input = """
            {
                "baseName": "invoices",
                "description": "Captured invoice emails",
                "columns": [
                    {"name": "vendor", "type": "STRING"},
                    {"name": "amount", "type": "NUMBER"},
                    {"name": "invoice_date", "type": "DATE"},
                    {"name": "paid", "type": "BOOLEAN"}
                ]
            }""";

        JsonNode result = jsonMapper.readTree(callback.call(input, toolContextWithWorkspace()));

        assertThat(result.get("created")
            .asBoolean()).isTrue();
        assertThat(result.get("dataTableId")
            .asLong()).isEqualTo(42L);
        assertThat(result.get("baseName")
            .asText()).isEqualTo("invoices");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ColumnSpec>> captor = ArgumentCaptor.forClass(List.class);

        verify(workspaceDataTableFacade).createTable(
            eq("invoices"), eq("Captured invoice emails"), captor.capture(), eq(1L), eq(0L));
        verify(workspaceDataTableFacade, never()).insertRow(anyLong(), anyMap(), anyLong());
        verify(workspaceDataTableFacade, never()).deleteRow(anyLong(), anyLong(), anyLong());

        List<ColumnSpec> columnSpecs = captor.getValue();

        assertThat(columnSpecs).containsExactly(
            new ColumnSpec("vendor", ColumnType.STRING),
            new ColumnSpec("amount", ColumnType.NUMBER),
            new ColumnSpec("invoice_date", ColumnType.DATE),
            new ColumnSpec("paid", ColumnType.BOOLEAN));
    }

    @Test
    void testUnsupportedColumnTypeReturnsToolErrorWithoutCreatingTable() throws Exception {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableToolCallback callback = new CreateDataTableToolCallback(workspaceDataTableFacade);

        String input = """
            {
                "baseName": "invoices",
                "columns": [{"name": "amount", "type": "DECIMAL"}]
            }""";

        JsonNode result = jsonMapper.readTree(callback.call(input, toolContextWithWorkspace()));

        assertThat(result.get("error")
            .asText()).contains("Unsupported column type 'DECIMAL'");

        verify(workspaceDataTableFacade, never()).createTable(anyString(), anyString(), anyList(), anyLong(),
            anyLong());
    }

    @Test
    void testMissingWorkspaceContextReturnsToolError() throws Exception {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableToolCallback callback = new CreateDataTableToolCallback(workspaceDataTableFacade);

        String input = """
            {
                "baseName": "invoices",
                "columns": [{"name": "vendor", "type": "STRING"}]
            }""";

        JsonNode result = jsonMapper.readTree(callback.call(input));

        assertThat(result.get("error")
            .asText()).contains("Workspace context unavailable");

        verify(workspaceDataTableFacade, never()).createTable(anyString(), anyString(), anyList(), anyLong(),
            anyLong());
    }

    @Test
    void testMissingColumnsReturnsValidationError() throws Exception {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableToolCallback callback = new CreateDataTableToolCallback(workspaceDataTableFacade);

        JsonNode result = jsonMapper.readTree(
            callback.call("{\"baseName\": \"invoices\"}", toolContextWithWorkspace()));

        assertThat(result.get("error")
            .asText()).contains("columns is required");
    }
}
