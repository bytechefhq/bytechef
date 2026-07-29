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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class AggregateDataTableToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private static ToolContext toolContext(long workspaceId) {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(workspaceId)
                .environmentId(0L)
                .build()
                .toToolContext());
    }

    @Test
    void testToolDefinitionExposesAggregateDataTableName() {
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        AggregateDataTableToolCallback callback = new AggregateDataTableToolCallback(
            dataTableRowService, dataTableService);

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("aggregateDataTable");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("aggregations");
    }

    @Test
    void testCallAggregatesSumAndCount() throws Exception {
        long workspaceId = 1L;
        long dataTableId = 42L;

        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(dataTableId)).thenReturn("orders");
        when(dataTableRowService.listRows(eq("orders"), anyInt(), eq(0), eq(0L))).thenReturn(List.of(
            new DataTableRow(1L, Map.of("amount", 100.0, "status", "paid")),
            new DataTableRow(2L, Map.of("amount", 200.0, "status", "paid")),
            new DataTableRow(3L, Map.of("amount", 50.0, "status", "refunded"))));

        AggregateDataTableToolCallback callback = new AggregateDataTableToolCallback(
            dataTableRowService, dataTableService);

        String result = callback.call(
            "{\"dataTableId\":\"42\",\"aggregations\":[{\"column\":\"amount\",\"fn\":\"sum\"},{\"column\":\"*\",\"fn\":\"count\"}]}",
            toolContext(workspaceId));

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).hasSize(1);

        JsonNode row = arrayNode.get(0);

        assertThat(row.get("sum_amount")
            .asDouble()).isEqualTo(350.0);
        assertThat(row.get("count_*")
            .asInt()).isEqualTo(3);
    }

    @Test
    void testCallReturnsGroupedResults() throws Exception {
        long workspaceId = 1L;
        long dataTableId = 42L;

        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(dataTableId)).thenReturn("sales");
        when(dataTableRowService.listRows(eq("sales"), anyInt(), eq(0), eq(0L))).thenReturn(List.of(
            new DataTableRow(1L, Map.of("region", "north", "revenue", 500.0)),
            new DataTableRow(2L, Map.of("region", "south", "revenue", 300.0)),
            new DataTableRow(3L, Map.of("region", "north", "revenue", 250.0))));

        AggregateDataTableToolCallback callback = new AggregateDataTableToolCallback(
            dataTableRowService, dataTableService);

        String result = callback.call(
            "{\"dataTableId\":\"42\",\"groupBy\":\"region\",\"aggregations\":[{\"column\":\"revenue\",\"fn\":\"sum\"}]}",
            toolContext(workspaceId));

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode).hasSize(2);

        Map<String, Double> groupedSums = new java.util.HashMap<>();

        for (JsonNode groupNode : arrayNode) {
            groupedSums.put(groupNode.get("region")
                .asText(),
                groupNode.get("sum_revenue")
                    .asDouble());
        }

        assertThat(groupedSums.get("north")).isEqualTo(750.0);
        assertThat(groupedSums.get("south")).isEqualTo(300.0);
    }

    @Test
    void testCallReturnsErrorOnUnknownAggregationFn() throws Exception {
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        AggregateDataTableToolCallback callback = new AggregateDataTableToolCallback(
            dataTableRowService, dataTableService);

        String result = callback.call(
            "{\"dataTableId\":\"42\",\"aggregations\":[{\"column\":\"amount\",\"fn\":\"median\"}]}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("median");
    }

    @Test
    void testCallClampsRowLimitAtFiftyThousand() throws Exception {
        long workspaceId = 1L;
        long dataTableId = 10L;

        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(dataTableId)).thenReturn("bigdata");

        List<DataTableRow> simulatedFullPage = new ArrayList<>();

        for (int index = 0; index < AggregateDataTableToolCallback.MAX_ROW_LIMIT; index++) {
            simulatedFullPage.add(new DataTableRow((long) index, Map.of("val", (double) index)));
        }

        when(dataTableRowService.listRows(
            eq("bigdata"), eq(AggregateDataTableToolCallback.MAX_ROW_LIMIT), eq(0), eq(0L)))
                .thenReturn(simulatedFullPage);

        AggregateDataTableToolCallback callback = new AggregateDataTableToolCallback(
            dataTableRowService, dataTableService);

        String result = callback.call(
            "{\"dataTableId\":\"10\",\"aggregations\":[{\"column\":\"*\",\"fn\":\"count\"}]}",
            toolContext(workspaceId));

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();
        assertThat(arrayNode.get(0)
            .has("truncated")).isTrue();
        assertThat(arrayNode.get(0)
            .get("truncated")
            .asBoolean()).isTrue();
    }

    @Test
    void testCallEmitsTruncationWarningWhenAtMaxScan() throws Exception {
        long workspaceId = 1L;
        long dataTableId = 99L;

        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(dataTableId)).thenReturn("huge");

        List<DataTableRow> maxRows = new ArrayList<>();

        for (int index = 0; index < AggregateDataTableToolCallback.MAX_ROW_LIMIT; index++) {
            maxRows.add(new DataTableRow((long) index, Map.of("amount", 1.0)));
        }

        when(dataTableRowService.listRows(
            eq("huge"), eq(AggregateDataTableToolCallback.MAX_ROW_LIMIT), eq(0), eq(0L)))
                .thenReturn(maxRows);

        AggregateDataTableToolCallback callback = new AggregateDataTableToolCallback(
            dataTableRowService, dataTableService);

        String result = callback.call(
            "{\"dataTableId\":\"99\",\"aggregations\":[{\"column\":\"*\",\"fn\":\"count\"}]}",
            toolContext(workspaceId));

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();

        JsonNode row = arrayNode.get(0);

        assertThat(row.has("truncated")).isTrue();
        assertThat(row.get("truncated")
            .asBoolean()).isTrue();
        assertThat(row.has("scannedRows")).isTrue();
        assertThat(row.get("scannedRows")
            .asLong()).isEqualTo(AggregateDataTableToolCallback.MAX_ROW_LIMIT);
    }

    @Test
    void testCallEmitsNotTruncatedWhenUnderMaxScan() throws Exception {
        long workspaceId = 1L;
        long dataTableId = 5L;

        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(dataTableId)).thenReturn("small");
        when(dataTableRowService.listRows(eq("small"), anyInt(), eq(0), eq(0L))).thenReturn(List.of(
            new DataTableRow(1L, Map.of("amount", 10.0)),
            new DataTableRow(2L, Map.of("amount", 20.0))));

        AggregateDataTableToolCallback callback = new AggregateDataTableToolCallback(
            dataTableRowService, dataTableService);

        String result = callback.call(
            "{\"dataTableId\":\"5\",\"aggregations\":[{\"column\":\"*\",\"fn\":\"count\"}]}", toolContext(workspaceId));

        JsonNode arrayNode = jsonMapper.readTree(result);

        assertThat(arrayNode.isArray()).isTrue();

        JsonNode row = arrayNode.get(0);

        assertThat(row.has("truncated")).isTrue();
        assertThat(row.get("truncated")
            .asBoolean()).isFalse();
        assertThat(row.has("scannedRows")).isTrue();
        assertThat(row.get("scannedRows")
            .asLong()).isEqualTo(2L);
    }

    @Test
    void testCallReportsSkippedNonNumericCells() throws Exception {
        // Regression guard: non-numeric cells in a sum/avg column must be reported as
        // <key>_skippedNonNumeric so the agent (and user) can tell that the aggregate is over a
        // partial dataset instead of silently dropping the offenders.
        long workspaceId = 1L;
        long dataTableId = 42L;

        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        DataTableService dataTableService = mock(DataTableService.class);

        when(dataTableService.getBaseNameById(dataTableId)).thenReturn("orders");
        when(dataTableRowService.listRows(eq("orders"), anyInt(), eq(0), eq(0L))).thenReturn(List.of(
            new DataTableRow(1L, Map.of("amount", 100.0)),
            new DataTableRow(2L, Map.of("amount", "not-a-number")),
            new DataTableRow(3L, Map.of("amount", "also-bad")),
            new DataTableRow(4L, Map.of("amount", 50.0))));

        AggregateDataTableToolCallback callback = new AggregateDataTableToolCallback(
            dataTableRowService, dataTableService);

        String result = callback.call(
            "{\"dataTableId\":\"42\",\"aggregations\":[{\"column\":\"amount\",\"fn\":\"sum\"}]}",
            toolContext(workspaceId));

        JsonNode arrayNode = jsonMapper.readTree(result);
        JsonNode row = arrayNode.get(0);

        assertThat(row.get("sum_amount")
            .asDouble()).isEqualTo(150.0);
        assertThat(row.has("sum_amount_skippedNonNumeric")).isTrue();
        assertThat(row.get("sum_amount_skippedNonNumeric")
            .asLong()).isEqualTo(2L);
    }
}
