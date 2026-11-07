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
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
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
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(workspaceDataTableFacade);

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
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(workspaceDataTableFacade);

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
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(workspaceDataTableFacade);

        String csv = "name,note\n\"Doe, John\",\"says \"\"hi\"\"\"\n\"Smith, Jane\",\"line1\nline2\"";

        when(workspaceDataTableFacade.getDataTableId("contacts")).thenReturn(7L);

        callback.call(buildInput("contacts", csv), toolContextWithWorkspace());

        verify(workspaceDataTableFacade, times(2)).insertRow(eq(7L), any(), eq(0L));
    }

    @Test
    void testRejectsMismatchedColumnCount() {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(workspaceDataTableFacade);

        String csv = "name,age,city\nalice,30\nbob,25,nyc";

        String result = callback.call(buildInput("bad", csv), toolContextWithWorkspace());

        assertThat(result).contains("error");
        assertThat(result).contains("CSV row 2 has 2 columns");
        verify(workspaceDataTableFacade, never()).createTable(any(), any(String.class), any(), anyLong(), anyLong());
    }

    @Test
    void testRejectsBlankBaseName() {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(workspaceDataTableFacade);

        String result = callback.call(
            "{\"baseName\":\"\",\"csvContent\":\"id\\n1\"}", toolContextWithWorkspace());

        assertThat(result).contains("baseName is required");
    }

    @Test
    void testRejectsWhenWorkspaceContextMissing() {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(workspaceDataTableFacade);

        String result = callback.call(buildInput("t", "id\n1"));

        assertThat(result).contains("Workspace context unavailable");
        verify(workspaceDataTableFacade, never()).createTable(any(), any(String.class), any(), anyLong(), anyLong());
    }

    @Test
    void testCoercesValuesByInferredType() throws Exception {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(workspaceDataTableFacade);

        when(workspaceDataTableFacade.getDataTableId("typed")).thenReturn(7L);

        callback.call(
            buildInput("typed", "id,name,score,joined\n1,Alice,9.5,2026-01-15"), toolContextWithWorkspace());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> rowCaptor = ArgumentCaptor.forClass(Map.class);

        verify(workspaceDataTableFacade, atLeastOnce()).insertRow(eq(7L), rowCaptor.capture(), eq(0L));

        Map<String, Object> values = rowCaptor.getValue();

        assertThat(values.get("id")).isEqualTo(1L);
        assertThat(values.get("name")).isEqualTo("Alice");
        assertThat(values.get("score")).isEqualTo(9.5);
        assertThat(values.get("joined")).isEqualTo(LocalDate.parse("2026-01-15"));
    }

    @Test
    void testDropsMixedCaseTableWhenRowInsertFails() throws Exception {
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

        when(workspaceDataTableFacade.listTables(1L, 0L))
            .thenReturn(List.of(new DataTableInfo(42L, "orders", "", List.of(), null)));
        when(workspaceDataTableFacade.getDataTableId("Orders")).thenReturn(42L);
        when(workspaceDataTableFacade.insertRow(eq(42L), any(), eq(0L)))
            .thenThrow(new IllegalStateException("insert failed"));

        CreateDataTableFromCsvToolCallback callback =
            new CreateDataTableFromCsvToolCallback(workspaceDataTableFacade);

        JsonNode result = jsonMapper.readTree(
            callback.call(buildInput("Orders", "id,name\n1,Alice"), toolContextWithWorkspace()));

        verify(workspaceDataTableFacade).dropTable(42L, 0L);
        assertThat(result.get("error")
            .asText()).contains("was dropped");
    }

    private String buildInput(String baseName, String csv) {
        try {
            return jsonMapper.writeValueAsString(Map.of("baseName", baseName, "csvContent", csv));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
