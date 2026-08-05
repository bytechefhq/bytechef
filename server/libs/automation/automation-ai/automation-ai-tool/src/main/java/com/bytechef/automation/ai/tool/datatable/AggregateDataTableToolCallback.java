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

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that performs group-by + aggregation queries over data-table rows in-memory. Fetches
 * up to 50,000 rows via {@link DataTableRowService#listRows} and performs the requested aggregation in Java (no SQL
 * changes). Used primarily by the {@code data_analyst} subagent but also available directly to the ai_hub BUILD agent.
 *
 * <p>
 * Supported aggregation functions: {@code sum}, {@code avg}, {@code count}, {@code min}, {@code max}. For
 * {@code count}, use {@code "*"} as the column name to count all rows, or provide a column name to count non-null
 * values of that column.
 *
 *
 * @author Ivica Cardic
 */
public class AggregateDataTableToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(AggregateDataTableToolCallback.class);

    static final int MAX_ROW_LIMIT = 50_000;
    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;
    private static final Set<String> VALID_AGGREGATION_FUNCTIONS = Set.of("sum", "avg", "count", "min", "max");

    private static final String DESCRIPTION = """
        Perform group-by + aggregation queries over a data table. Fetches up to 50,000 rows and
        performs sum/avg/count/min/max aggregation in-memory. Use this for analytical questions
        that require totals, averages, distributions, or groupings.

        - groupBy (optional): column name to group results by. Omit for a single-row overall result.
        - aggregations: list of {column, fn} objects where fn is one of: sum, avg, count, min, max.
          For count-all, set column to "*".

        Returns a JSON array where each element is one group row with the group key (if groupBy was
        provided) and the aggregated values. A "truncated" field is included if the row count hit the
        50,000 limit.

        Obtain dataTableId from listDataTables first.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "dataTableId": {
                        "type": "string",
                        "description": "Data table id obtained from listDataTables"
                    },
                    "groupBy": {
                        "type": "string",
                        "description": "Optional column name to group results by. Omit for a single overall aggregate row."
                    },
                    "aggregations": {
                        "type": "array",
                        "description": "List of aggregations to compute.",
                        "items": {
                            "type": "object",
                            "properties": {
                                "column": {
                                    "type": "string",
                                    "description": "Column name to aggregate. Use \\"*\\" for count-all."
                                },
                                "fn": {
                                    "type": "string",
                                    "enum": ["sum", "avg", "count", "min", "max"],
                                    "description": "Aggregation function."
                                }
                            },
                            "required": ["column", "fn"]
                        },
                        "minItems": 1
                    }
                },
                "required": ["dataTableId", "aggregations"]
            }""";

    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AggregateDataTableToolCallback(
        DataTableRowService dataTableRowService,
        DataTableService dataTableService) {

        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("aggregateDataTable")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            AggregateDataTableInput input = jsonMapper.readValue(toolInput, AggregateDataTableInput.class);

            String dataTableIdString = input.dataTableId();

            if (dataTableIdString == null || dataTableIdString.isBlank()) {
                return toolError("dataTableId is required");
            }

            List<AggregationSpec> aggregations = input.aggregations();

            if (aggregations == null || aggregations.isEmpty()) {
                return toolError("aggregations is required and must contain at least one entry");
            }

            for (AggregationSpec aggregationSpec : aggregations) {
                if (!VALID_AGGREGATION_FUNCTIONS.contains(aggregationSpec.fn())) {
                    return toolError(
                        "Unknown aggregation function: '" + aggregationSpec.fn()
                            + "'. Valid values: sum, avg, count, min, max");
                }

                String aggregationColumn = aggregationSpec.column();

                if (aggregationColumn == null || aggregationColumn.isBlank()) {
                    return toolError("aggregation column must not be blank (use \"*\" for count-all)");
                }
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            long dataTableId;

            try {
                dataTableId = Long.parseLong(dataTableIdString);
            } catch (NumberFormatException exception) {
                return toolError("Invalid dataTableId - must be a numeric id obtained from listDataTables");
            }

            String baseName = dataTableService.getBaseNameById(dataTableId);

            if (baseName == null || baseName.isBlank()) {
                return toolError("Data table not found: " + dataTableIdString);
            }

            long environmentId = resolveEnvironmentId(invocationContext);

            List<DataTableRow> rows = dataTableRowService.listRows(baseName, MAX_ROW_LIMIT, 0, environmentId);

            boolean truncated = rows.size() >= MAX_ROW_LIMIT;
            long scannedRows = rows.size();

            List<Map<String, Object>> rowValues = rows.stream()
                .map(DataTableRow::values)
                .toList();

            List<Map<String, Object>> results = computeAggregations(rowValues, input.groupBy(), aggregations);

            for (Map<String, Object> resultRow : results) {
                resultRow.put("scannedRows", scannedRows);

                if (truncated) {
                    resultRow.put("truncated", true);
                } else {
                    resultRow.put("truncated", false);
                }
            }

            return jsonMapper.writeValueAsString(results);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, AggregateDataTableToolCallback.class, "aggregateDataTable", exception);
        }
    }

    private List<Map<String, Object>> computeAggregations(
        List<Map<String, Object>> rows,
        @Nullable String groupBy,
        List<AggregationSpec> aggregations) {

        if (groupBy == null || groupBy.isBlank()) {
            Map<String, Object> singleResult = new LinkedHashMap<>();

            for (AggregationSpec aggregationSpec : aggregations) {
                String outputKey = aggregationSpec.fn() + "_" + aggregationSpec.column();
                long[] skipCounter = {
                    0L
                };

                singleResult.put(outputKey, computeAgg(rows, aggregationSpec, skipCounter));

                if (skipCounter[0] > 0L) {
                    singleResult.put(outputKey + "_skippedNonNumeric", skipCounter[0]);
                }
            }

            List<Map<String, Object>> singleRowList = new ArrayList<>();

            singleRowList.add(singleResult);

            return singleRowList;
        }

        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Object groupValue = row.get(groupBy);
            String groupKey = groupValue != null ? groupValue.toString() : "(null)";

            groups.computeIfAbsent(groupKey, key -> new ArrayList<>())
                .add(row);
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            Map<String, Object> resultRow = new LinkedHashMap<>();

            resultRow.put(groupBy, entry.getKey());

            for (AggregationSpec aggregationSpec : aggregations) {
                String outputKey = aggregationSpec.fn() + "_" + aggregationSpec.column();
                long[] skipCounter = {
                    0L
                };

                resultRow.put(outputKey, computeAgg(entry.getValue(), aggregationSpec, skipCounter));

                if (skipCounter[0] > 0L) {
                    resultRow.put(outputKey + "_skippedNonNumeric", skipCounter[0]);
                }
            }

            results.add(resultRow);
        }

        return results;
    }

    private Object computeAgg(List<Map<String, Object>> rows, AggregationSpec aggregationSpec, long[] skipCounter) {
        String columnName = aggregationSpec.column();
        String function = aggregationSpec.fn();

        if ("count".equals(function)) {
            if ("*".equals(columnName)) {
                return rows.size();
            }

            return rows.stream()
                .filter(row -> row.get(columnName) != null)
                .count();
        }

        List<Double> numericValues = rows.stream()
            .map(row -> toDouble(row.get(columnName), skipCounter))
            .filter(value -> value != null)
            .toList();

        if (numericValues.isEmpty()) {
            return null;
        }

        return switch (function) {
            case "sum" -> numericValues.stream()
                .mapToDouble(Double::doubleValue)
                .sum();
            case "avg" -> numericValues.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            case "min" -> numericValues.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
            case "max" -> numericValues.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
            default -> null;
        };
    }

    @Nullable
    private Double toDouble(@Nullable Object value, long[] skipCounter) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException exception) {
            // Non-numeric cell present in a numeric aggregation column. Count it so the response can surface
            // "N cells skipped" — without this signal the agent would happily report a sum/avg over a partially
            // empty dataset and the user would have no way to tell.
            skipCounter[0]++;

            if (log.isDebugEnabled()) {
                log.debug("Skipping non-numeric cell value in aggregation: {}", value);
            }

            return null;
        }
    }

    private long resolveEnvironmentId(AgentToolInvocationContext invocationContext) {
        Long environmentId = invocationContext.environmentId();

        return environmentId != null ? environmentId : DEFAULT_ENVIRONMENT_ORDINAL;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record AggregateDataTableInput(
        String dataTableId,
        @Nullable String groupBy,
        List<AggregationSpec> aggregations) {
    }

    public record AggregationSpec(String column, String fn) {
    }
}
