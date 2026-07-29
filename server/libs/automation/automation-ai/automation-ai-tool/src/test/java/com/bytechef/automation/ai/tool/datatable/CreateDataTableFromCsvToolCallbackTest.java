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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class CreateDataTableFromCsvToolCallbackTest {

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
    void testInfersIntegerNumberDateBooleanAndStringColumns() throws Exception {
        // Pin the heuristic dispatch order: BOOLEAN > INTEGER > NUMBER > DATE > DATE_TIME > STRING. The four
        // sample columns below stress every branch — id/age picks INTEGER, score picks NUMBER, joined picks DATE,
        // active picks BOOLEAN, name falls through to STRING. A regression that swapped INTEGER/NUMBER ordering
        // would land "1, 2, 3" as NUMBER which the dynamic table back-end stores as a different SQL type.
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(dataTableRowService, workspaceDataTableFacade);

        String csv = """
            id,name,age,score,active,joined
            1,Alice,30,9.5,true,2026-01-15
            2,Bob,28,7.2,false,2026-02-01
            3,Carol,35,8.1,yes,2025-12-20""";

        JsonNode result = jsonMapper.readTree(callback.call(buildInput("contacts", csv), toolContextWithWorkspace()));

        assertThat(result.get("created")
            .asBoolean()).isTrue();
        assertThat(result.get("rowCount")
            .asInt()).isEqualTo(3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ColumnSpec>> captor = ArgumentCaptor.forClass(List.class);

        verify(workspaceDataTableFacade).createTable(
            eq("contacts"), eq(""), captor.capture(), eq(1L), eq(0L));

        Map<String, ColumnType> typesByName = captor.getValue()
            .stream()
            .collect(java.util.stream.Collectors.toMap(ColumnSpec::name, ColumnSpec::type));

        assertThat(typesByName)
            .containsEntry("id", ColumnType.INTEGER)
            .containsEntry("name", ColumnType.STRING)
            .containsEntry("age", ColumnType.INTEGER)
            .containsEntry("score", ColumnType.NUMBER)
            .containsEntry("active", ColumnType.BOOLEAN)
            .containsEntry("joined", ColumnType.DATE);
    }

    @Test
    void testEmptyCellsDoNotForceColumnToString() {
        // A column with one missing value would otherwise downgrade to STRING because the empty cell can't be
        // parsed as INTEGER/NUMBER. Pin the "skip empties during inference" rule so a CSV with sparse data still
        // gets a typed schema.
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(dataTableRowService, workspaceDataTableFacade);

        String csv = """
            id,score
            1,9.5
            2,
            3,8.1""";

        callback.call(buildInput("scores", csv), toolContextWithWorkspace());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ColumnSpec>> captor = ArgumentCaptor.forClass(List.class);

        verify(workspaceDataTableFacade).createTable(
            eq("scores"), eq(""), captor.capture(), anyLong(), anyLong());

        Map<String, ColumnType> typesByName = captor.getValue()
            .stream()
            .collect(java.util.stream.Collectors.toMap(ColumnSpec::name, ColumnSpec::type));

        assertThat(typesByName).containsEntry("score", ColumnType.NUMBER);
    }

    @Test
    void testQuotedCommasAndEmbeddedNewlinesParseCorrectly() {
        // RFC 4180 invariants: quoted commas count as one cell; embedded newlines inside quoted fields don't end
        // the row; "" inside quotes is the standard escape. A regression to a naive split would miscount columns
        // here and the row-count guard would falsely reject the CSV.
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(dataTableRowService, workspaceDataTableFacade);

        String csv = "name,note\n\"Doe, John\",\"says \"\"hi\"\"\"\n\"Smith, Jane\",\"line1\nline2\"";

        callback.call(buildInput("contacts", csv), toolContextWithWorkspace());

        verify(dataTableRowService, times(2)).insertRow(eq("contacts"), any(), eq(0L));
    }

    @Test
    void testRejectsMismatchedColumnCount() {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(dataTableRowService, workspaceDataTableFacade);

        String csv = "name,age,city\nalice,30\nbob,25,nyc";

        String result = callback.call(buildInput("bad", csv), toolContextWithWorkspace());

        assertThat(result).contains("error");
        assertThat(result).contains("CSV row 2 has 2 columns");
        verify(workspaceDataTableFacade, never()).createTable(any(), any(String.class), any(), anyLong(), anyLong());
    }

    @Test
    void testRejectsBlankBaseName() {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(dataTableRowService, workspaceDataTableFacade);

        String result = callback.call(
            "{\"baseName\":\"\",\"csvContent\":\"id\\n1\"}", toolContextWithWorkspace());

        assertThat(result).contains("baseName is required");
    }

    @Test
    void testRejectsWhenWorkspaceContextMissing() {
        // Probe-oracle defense: a tool input without bound workspace must NOT default-create into workspace 0.
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(dataTableRowService, workspaceDataTableFacade);

        String result = callback.call(buildInput("t", "id\n1"));

        assertThat(result).contains("Workspace context unavailable");
        verify(workspaceDataTableFacade, never()).createTable(any(), any(String.class), any(), anyLong(), anyLong());
    }

    @Test
    void testCoercesValuesByInferredType() throws Exception {
        // Pin the coercion contract: an INTEGER column's cells must be passed to insertRow as Long, not String.
        // A regression that left them as raw strings would fail at the SQL layer with a type-mismatch error.
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(dataTableRowService, workspaceDataTableFacade);

        callback.call(
            buildInput("typed", "id,name,score,joined\n1,Alice,9.5,2026-01-15"), toolContextWithWorkspace());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> rowCaptor = ArgumentCaptor.forClass(Map.class);

        verify(dataTableRowService, atLeastOnce()).insertRow(eq("typed"), rowCaptor.capture(), eq(0L));

        Map<String, Object> values = rowCaptor.getValue();

        assertThat(values.get("id")).isEqualTo(1L);
        assertThat(values.get("name")).isEqualTo("Alice");
        assertThat(values.get("score")).isEqualTo(9.5);
        assertThat(values.get("joined")).isEqualTo(LocalDate.parse("2026-01-15"));
    }

    private String buildInput(String baseName, String csv) {
        try {
            return jsonMapper.writeValueAsString(Map.of("baseName", baseName, "csvContent", csv));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
