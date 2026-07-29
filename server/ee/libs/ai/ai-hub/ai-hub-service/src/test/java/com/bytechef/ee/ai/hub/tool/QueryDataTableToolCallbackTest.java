/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.ee.ai.hub.artifact.ArtifactGeneratorRegistry;
import com.bytechef.ee.ai.hub.artifact.GenerationRequest;
import com.bytechef.ee.ai.hub.artifact.GenerationResult;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class QueryDataTableToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesQueryDataTableName() {
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        QueryDataTableToolCallback callback = new QueryDataTableToolCallback(
            dataTableRowService, dataTableService);

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("queryDataTable");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("dataTableId");
    }

    @Test
    void testCallReturnsFilteredRows() throws Exception {
        long workspaceId = 1L;
        long dataTableId = 42L;

        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(dataTableId)).thenReturn("contacts");
        when(dataTableRowService.listRows(eq("contacts"), anyInt(), eq(0), eq(0L))).thenReturn(List.of(
            new DataTableRow(1L, Map.of("name", "Alice", "status", "qualified")),
            new DataTableRow(2L, Map.of("name", "Bob", "status", "prospect")),
            new DataTableRow(3L, Map.of("name", "Carol", "status", "qualified"))));

        QueryDataTableToolCallback callback = new QueryDataTableToolCallback(
            dataTableRowService, dataTableService);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(workspaceId, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call("{\"dataTableId\":\"42\",\"where\":\"status = 'qualified'\"}", toolContext);

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).hasSize(2);
        assertThat(arrayNode.get(0)
            .get("name")
            .asText()).isEqualTo("Alice");
        assertThat(arrayNode.get(1)
            .get("name")
            .asText()).isEqualTo("Carol");
    }

    @Test
    void testCallReturnsAllRowsWhenWhereOmitted() throws Exception {
        long workspaceId = 1L;
        long dataTableId = 42L;

        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(dataTableId)).thenReturn("contacts");
        when(dataTableRowService.listRows(eq("contacts"), anyInt(), eq(0), eq(0L))).thenReturn(List.of(
            new DataTableRow(1L, Map.of("name", "Alice")),
            new DataTableRow(2L, Map.of("name", "Bob"))));

        QueryDataTableToolCallback callback = new QueryDataTableToolCallback(
            dataTableRowService, dataTableService);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(workspaceId, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call("{\"dataTableId\":\"42\"}", toolContext);

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).hasSize(2);
    }

    @Test
    void testCallClampsLimitAtFifty() throws Exception {
        long workspaceId = 1L;
        long dataTableId = 42L;

        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(dataTableId)).thenReturn("contacts");
        when(dataTableRowService.listRows(anyString(), eq(DataTableQuerySupport.MAX_LIMIT), eq(0), eq(0L)))
            .thenReturn(List.of(new DataTableRow(1L, Map.of("name", "Alice"))));

        QueryDataTableToolCallback callback = new QueryDataTableToolCallback(
            dataTableRowService, dataTableService);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(workspaceId, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call("{\"dataTableId\":\"42\",\"limit\":200}", toolContext);

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).hasSize(1);
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() throws Exception {
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        QueryDataTableToolCallback callback = new QueryDataTableToolCallback(
            dataTableRowService, dataTableService);

        String result = callback.call("not-json");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    @Test
    void testExportToCsvWritesAssetFileAndReturnsEnvelope() throws Exception {
        // Pin the export-CSV branch end-to-end: rows go to the CSV generator, the result envelope keys on
        // exported=true so the LLM dispatches differently from the inline path, the asset_file id surfaces back
        // for the chip, and taskLinked propagates so the LLM can mention the file in chat.
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        when(dataTableService.getBaseNameById(42L)).thenReturn("contacts");
        when(dataTableRowService.listRows(eq("contacts"), anyInt(), eq(0), anyLong()))
            .thenReturn(List.of(
                new DataTableRow(1L, Map.of("name", "Alice", "city", "NA")),
                new DataTableRow(2L, Map.of("name", "Bob", "city", "EU"))));

        AiHubTask task = mock(AiHubTask.class);

        when(task.getId()).thenReturn(7L);
        when(taskService.findByThreadId("thread-1")).thenReturn(Optional.of(task));

        when(registry.generate(eq(AssetFileFormat.CSV), any(GenerationRequest.class)))
            .thenReturn(new GenerationResult(101L, "contacts-export.csv", AssetFileFormat.CSV, true));

        QueryDataTableToolCallback callback = new QueryDataTableToolCallback(
            registry, taskService, dataTableRowService, dataTableService);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "give me a csv", 0L, "thread-1")
                .toToolContext());

        String result = callback.call(
            "{\"dataTableId\":\"42\",\"exportToCsv\":true}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("exported")
            .asBoolean()).isTrue();
        assertThat(node.get("assetFileId")
            .asLong()).isEqualTo(101L);
        assertThat(node.get("rowCount")
            .asInt()).isEqualTo(2);
        assertThat(node.get("taskLinked")
            .asBoolean()).isTrue();

        ArgumentCaptor<GenerationRequest> requestCaptor = ArgumentCaptor.forClass(GenerationRequest.class);

        verify(registry).generate(eq(AssetFileFormat.CSV), requestCaptor.capture());

        GenerationRequest captured = requestCaptor.getValue();

        assertThat(captured.workspaceId()).isEqualTo(1L);
        assertThat(captured.userId()).isEqualTo(10L);
        assertThat(captured.taskId()).isEqualTo(7L);
        // Header row + two data rows; quoted-comma escaping happens via escapeCsvField — the cell values here are
        // simple so the rendered CSV is naive comma-joined; the test pins the row count rather than the exact
        // string to avoid coupling to header iteration order (LinkedHashSet is insertion-ordered, but per-row
        // Map.of() iteration order is not).
        assertThat(captured.payload()).contains("Alice");
        assertThat(captured.payload()).contains("Bob");
        assertThat(captured.payload()).contains("NA");
        assertThat(captured.payload()).contains("EU");
    }

    @Test
    void testExportToCsvRejectedInSubAgentContextWithoutRegistry() {
        // The 2-arg sub-agent constructor (data analyst path) intentionally has no registry — a tool input that
        // requested exportToCsv from that context must surface an actionable error rather than NPE on the
        // null registry deref.
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(42L)).thenReturn("contacts");
        when(dataTableRowService.listRows(eq("contacts"), anyInt(), eq(0), anyLong())).thenReturn(List.of());

        QueryDataTableToolCallback callback = new QueryDataTableToolCallback(
            dataTableRowService, dataTableService);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call(
            "{\"dataTableId\":\"42\",\"exportToCsv\":true}", toolContext);

        assertThat(result).contains("exportToCsv is not available");
    }
}
