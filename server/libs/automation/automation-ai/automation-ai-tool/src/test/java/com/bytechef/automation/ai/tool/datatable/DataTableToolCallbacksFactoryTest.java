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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DataTableToolCallbacksFactoryTest {

    private static final long DATA_TABLE_ID = 5L;
    private static final long ENVIRONMENT_ID = 1L;
    private static final long WORKSPACE_ID = 3L;

    private final ToolArtifactRecorder artifactRecorder = mock(ToolArtifactRecorder.class);
    private final WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
    private final DataTableToolCallbacksFactory factory = new DataTableToolCallbacksFactory(
        workspaceDataTableFacade, artifactRecorder);

    @BeforeEach
    void beforeEach() {
        when(workspaceDataTableFacade.listTables(WORKSPACE_ID, ENVIRONMENT_ID))
            .thenReturn(List.of(new DataTableInfo(DATA_TABLE_ID, "orders", null, List.of(), Instant.now())));
    }

    @Test
    void readListExcludesMutations() {
        List<String> names = toolNames(factory.readToolCallbacks());

        assertThat(names).contains("listDataTables", "queryDataTable", "aggregateDataTable");
        assertThat(names).doesNotContain("dropDataTable", "createDataTable");
    }

    @Test
    void writeListIncludesReadsAndMutations() {
        List<String> names = toolNames(factory.writeToolCallbacks());

        assertThat(names).contains(
            "listDataTables", "queryDataTable", "aggregateDataTable", "addDataTableRow", "updateDataTableRow",
            "deleteDataTableRow", "addDataTableColumn", "createDataTable", "createDataTableFromCsv",
            "cloneDataTable", "dropDataTable");
    }

    @Test
    void addDataTableRowRecordsTheAddedRow() {
        when(workspaceDataTableFacade.insertRow(DATA_TABLE_ID, Map.of("name", "Ada"), ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(9L, Map.of("name", "Ada")));

        writeToolCallback("addDataTableRow").call(
            "{\"dataTableId\": \"5\", \"values\": {\"name\": \"Ada\"}}", chatToolContext());

        verify(artifactRecorder).record(
            "conversation-1", 7L, AddDataTableRowToolCallback.ARTIFACT_KIND_DATA_TABLE_ROW_ADDED, "9", "orders row 9",
            null);
    }

    @Test
    void updateDataTableRowRecordsThePriorValues() {
        when(workspaceDataTableFacade.getRow(DATA_TABLE_ID, 9L, ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(9L, Map.of("name", "Ada")));
        when(workspaceDataTableFacade.updateRow(DATA_TABLE_ID, 9L, Map.of("name", "Grace"), ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(9L, Map.of("name", "Grace")));

        writeToolCallback("updateDataTableRow").call(
            "{\"dataTableId\": \"5\", \"rowId\": \"9\", \"values\": {\"name\": \"Grace\"}}", chatToolContext());

        Map<String, Object> metadata = captureMetadata(
            UpdateDataTableRowToolCallback.ARTIFACT_KIND_DATA_TABLE_ROW_UPDATED, "9", "orders row 9");

        assertThat(metadata).containsEntry("priorValues", Map.of("name", "Ada"))
            .containsEntry("baseName", "orders")
            .containsEntry("dataTableId", "5")
            .containsEntry("environmentId", ENVIRONMENT_ID);
    }

    @Test
    void deleteDataTableRowRecordsThePriorValues() {
        when(workspaceDataTableFacade.getRow(DATA_TABLE_ID, 9L, ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(9L, Map.of("name", "Ada")));
        when(workspaceDataTableFacade.deleteRow(DATA_TABLE_ID, 9L, ENVIRONMENT_ID)).thenReturn(true);

        writeToolCallback("deleteDataTableRow").call(
            "{\"dataTableId\": \"5\", \"rowId\": \"9\"}", chatToolContext());

        Map<String, Object> metadata = captureMetadata(
            DeleteDataTableRowToolCallback.ARTIFACT_KIND_DATA_TABLE_ROW_DELETED, "9", "orders row 9");

        assertThat(metadata).containsEntry("priorValues", Map.of("name", "Ada"));
    }

    @Test
    void deleteDataTableRowRecordsNothingWhenNoRowWasDeleted() {
        when(workspaceDataTableFacade.deleteRow(DATA_TABLE_ID, 9L, ENVIRONMENT_ID)).thenReturn(false);

        writeToolCallback("deleteDataTableRow").call(
            "{\"dataTableId\": \"5\", \"rowId\": \"9\"}", chatToolContext());

        verify(artifactRecorder, never()).record(anyString(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void addDataTableColumnRecordsTheAddedColumn() {
        writeToolCallback("addDataTableColumn").call(
            "{\"dataTableId\": \"5\", \"columnName\": \"email\", \"columnType\": \"STRING\"}", chatToolContext());

        verify(workspaceDataTableFacade).addColumn(eq(DATA_TABLE_ID), any(ColumnSpec.class), eq(ENVIRONMENT_ID));

        Map<String, Object> metadata = captureMetadata(
            AddDataTableColumnToolCallback.ARTIFACT_KIND_DATA_TABLE_COLUMN_ADDED, "5", "orders.email");

        assertThat(metadata).containsEntry("columnName", "email");
    }

    @Test
    void mutationsRecordNothingOutsideAConversation() {
        when(workspaceDataTableFacade.insertRow(DATA_TABLE_ID, Map.of("name", "Ada"), ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(9L, Map.of("name", "Ada")));

        Map<String, Object> context = new HashMap<>(chatToolContext().getContext());

        context.remove(AgentToolInvocationContext.TOOL_CONTEXT_CONVERSATION_ID_KEY);

        writeToolCallback("addDataTableRow").call(
            "{\"dataTableId\": \"5\", \"values\": {\"name\": \"Ada\"}}", new ToolContext(context));

        verify(artifactRecorder, never()).record(anyString(), any(), anyString(), anyString(), anyString(), any());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureMetadata(String artifactKind, String artifactId, String artifactName) {
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(artifactRecorder).record(
            eq("conversation-1"), eq(7L), eq(artifactKind), eq(artifactId), eq(artifactName),
            metadataCaptor.capture());

        return metadataCaptor.getValue();
    }

    private ToolCallback writeToolCallback(String name) {
        return factory.writeToolCallbacks()
            .stream()
            .filter(toolCallback -> name.equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();
    }

    private static ToolContext chatToolContext() {
        return new ToolContext(
            Map.of(
                AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID,
                AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, ENVIRONMENT_ID,
                AgentToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 7L,
                AgentToolInvocationContext.TOOL_CONTEXT_CONVERSATION_ID_KEY, "conversation-1"));
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toList());
    }
}
