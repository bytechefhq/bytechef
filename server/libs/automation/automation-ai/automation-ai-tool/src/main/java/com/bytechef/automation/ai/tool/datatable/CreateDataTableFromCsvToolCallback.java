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
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Creates a new data table from a CSV payload, inferring the column schema from the first ~100 sample rows. Direct
 * execution rather than staging — the user explicitly asked for "create a table from this CSV", and the operation
 * doesn't mutate existing data, so the staged-mutation review loop adds friction without a clear payoff.
 *
 * <p>
 * Type inference is heuristic: for each column we test cells in the order BOOLEAN → INTEGER → NUMBER → DATE → DATE_TIME
 * → STRING. The first type that holds for every non-empty sample cell wins. Empty cells are skipped during inference so
 * a column with one missing value isn't downgraded to STRING. The 100-row sample bound keeps inference cheap on huge
 * CSVs and is large enough to avoid type drift on representative data; an "all-INTEGER for 100 rows then a STRING in
 * row 200" outlier is rare in CSV uploads and the consequence (insert fails the LLM can surface back to the user) is
 * recoverable.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class CreateDataTableFromCsvToolCallback implements ToolCallback {

    private static final int INFERENCE_SAMPLE_ROWS = 100;
    private static final int MAX_TOTAL_ROWS = 50_000;
    private static final String TOOL_NAME = "createDataTableFromCsv";

    private static final String DESCRIPTION = """
        Create a new data table from CSV content, inferring column types from the first 100 rows. Use when
        the user pastes or attaches a CSV and asks to "make this a table" or "import this data". The tool
        creates the table immediately (no staged confirmation) and bulk-inserts every row. Returns the
        created table's base name, the inferred column schema, and the row count. Hard-capped at 50,000
        rows; for larger imports the LLM should suggest a workflow-based bulk import instead.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "baseName": {"type": "string", "description": "Snake_case identifier for the new table"},
                    "description": {"type": "string", "description": "Optional human-readable description"},
                    "csvContent": {"type": "string", "description": "Raw CSV text including a header row"}
                },
                "required": ["baseName", "csvContent"]
            }""";

    private final DataTableRowService dataTableRowService;
    private final WorkspaceDataTableFacade workspaceDataTableFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateDataTableFromCsvToolCallback(
        DataTableRowService dataTableRowService, WorkspaceDataTableFacade workspaceDataTableFacade) {

        this.dataTableRowService = dataTableRowService;
        this.workspaceDataTableFacade = workspaceDataTableFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
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
            CreateDataTableFromCsvInput input = jsonMapper.readValue(toolInput, CreateDataTableFromCsvInput.class);

            String validationError = validate(input);

            if (validationError != null) {
                return ToolErrors.toolError(jsonMapper, validationError);
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return ToolErrors.toolError(
                    jsonMapper,
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            long environmentId = invocationContext == null ? 0L : invocationContext.resolveEnvironmentOrDefault();

            ParsedCsv parsed = parseCsv(input.csvContent());

            if (parsed.rows.size() > MAX_TOTAL_ROWS) {
                return ToolErrors.toolError(
                    jsonMapper,
                    "CSV has " + parsed.rows.size() + " rows; the per-import cap is " + MAX_TOTAL_ROWS
                        + ". Suggest a workflow-based bulk import for larger datasets.");
            }

            List<ColumnSpec> columnSpecs = inferColumnSpecs(parsed);

            String description = input.description() == null ? "" : input.description();

            workspaceDataTableFacade.createTable(input.baseName(), description, columnSpecs, workspaceId,
                environmentId);

            int inserted = 0;

            for (List<String> row : parsed.rows) {
                Map<String, Object> values = new HashMap<>();

                for (int i = 0; i < columnSpecs.size() && i < row.size(); i++) {
                    String raw = row.get(i);

                    values.put(columnSpecs.get(i)
                        .name(),
                        coerceCellValue(raw, columnSpecs.get(i)
                            .type()));
                }

                dataTableRowService.insertRow(input.baseName(), values, environmentId);
                inserted++;
            }

            return jsonMapper.writeValueAsString(new CreateDataTableFromCsvOutput(
                true,
                input.baseName(),
                columnSpecs.stream()
                    .map(spec -> new ColumnSchemaEntry(spec.name(), spec.type()
                        .name()))
                    .toList(),
                inserted));
        } catch (IllegalArgumentException exception) {
            return ToolErrors.toolError(jsonMapper, exception.getMessage());
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CreateDataTableFromCsvToolCallback.class, TOOL_NAME,
                exception);
        }
    }

    /**
     * Walks the column-by-column inference matrix. We try the most specific type first (BOOLEAN) and fall back
     * progressively. INTEGER is checked before NUMBER so "1, 2, 3" lands as INTEGER rather than NUMBER; STRING is the
     * universal sink. Empty / null cells are skipped during inference so a column with sparse data is not forced to
     * STRING just because one cell is blank.
     */
    private static List<ColumnSpec> inferColumnSpecs(ParsedCsv parsed) {
        List<ColumnSpec> specs = new ArrayList<>(parsed.headers.size());
        int sampleEnd = Math.min(INFERENCE_SAMPLE_ROWS, parsed.rows.size());

        for (int columnIndex = 0; columnIndex < parsed.headers.size(); columnIndex++) {
            String header = parsed.headers.get(columnIndex);

            ColumnType inferred = ColumnType.STRING;

            for (ColumnType candidate : new ColumnType[] {
                ColumnType.BOOLEAN, ColumnType.INTEGER, ColumnType.NUMBER, ColumnType.DATE, ColumnType.DATE_TIME
            }) {
                if (allCellsMatch(parsed.rows, columnIndex, sampleEnd, candidate)) {
                    inferred = candidate;

                    break;
                }
            }

            specs.add(new ColumnSpec(header, inferred));
        }

        return specs;
    }

    private static boolean allCellsMatch(List<List<String>> rows, int columnIndex, int sampleEnd, ColumnType type) {
        boolean atLeastOneNonEmpty = false;

        for (int rowIndex = 0; rowIndex < sampleEnd; rowIndex++) {
            List<String> row = rows.get(rowIndex);

            if (columnIndex >= row.size()) {
                continue;
            }

            String cell = row.get(columnIndex);

            if (cell == null || cell.isEmpty()) {
                continue;
            }

            atLeastOneNonEmpty = true;

            if (!matchesType(cell, type)) {
                return false;
            }
        }

        // A column where every sample cell is empty has no signal — fall through to STRING by reporting "no match"
        // for every type. The sampleEnd loop above already filters empties, so this guard kicks in when the column
        // is genuinely sparse across the entire 100-row sample.
        return atLeastOneNonEmpty;
    }

    private static boolean matchesType(String cell, ColumnType type) {
        String trimmed = cell.trim();

        return switch (type) {
            case BOOLEAN -> {
                String lower = trimmed.toLowerCase(Locale.ROOT);

                yield "true".equals(lower) || "false".equals(lower) || "yes".equals(lower) || "no".equals(lower);
            }
            case INTEGER -> {
                try {
                    Long.parseLong(trimmed);

                    yield true;
                } catch (NumberFormatException exception) {
                    yield false;
                }
            }
            case NUMBER -> {
                try {
                    Double.parseDouble(trimmed);

                    yield true;
                } catch (NumberFormatException exception) {
                    yield false;
                }
            }
            case DATE -> {
                try {
                    LocalDate.parse(trimmed);

                    yield true;
                } catch (DateTimeParseException exception) {
                    yield false;
                }
            }
            case DATE_TIME -> {
                try {
                    LocalDateTime.parse(trimmed);

                    yield true;
                } catch (DateTimeParseException exception) {
                    yield false;
                }
            }
            case STRING -> true;
        };
    }

    /**
     * Converts the raw cell text to the inferred column type. Empty cells become {@code null} regardless of type so
     * DATE / NUMBER columns can carry missing values without a parse error. A coerce-failure on a non-empty cell falls
     * back to the raw string — the row insert may then fail at the database level, which surfaces back to the LLM as a
     * runtime tool error and is the right outcome for "you said this was an INTEGER column but the data has letters in
     * it".
     */
    private static @Nullable Object coerceCellValue(String raw, ColumnType type) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        String trimmed = raw.trim();

        try {
            return switch (type) {
                case BOOLEAN -> {
                    String lower = trimmed.toLowerCase(Locale.ROOT);

                    yield "true".equals(lower) || "yes".equals(lower);
                }
                case INTEGER -> Long.parseLong(trimmed);
                case NUMBER -> Double.parseDouble(trimmed);
                case DATE -> LocalDate.parse(trimmed);
                case DATE_TIME -> LocalDateTime.parse(trimmed);
                case STRING -> raw;
            };
        } catch (NumberFormatException | DateTimeParseException exception) {
            return raw;
        }
    }

    /**
     * RFC 4180 CSV parser — supports double-quote escaping, quoted commas, embedded newlines inside quoted fields.
     * In-package implementation rather than Apache Commons CSV to avoid the dep — the LLM produces simple shapes and
     * the existing P1 CsvArtifactGenerator already validates against the same RFC 4180 grammar; sharing one parser
     * would be cleaner but the two have different output shapes (column counts vs. cell values) and the duplication is
     * ~40 lines.
     */
    private static ParsedCsv parseCsv(String content) {
        List<List<String>> rows = parseRows(content);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV is empty — at minimum a header row is required");
        }

        List<String> headers = rows.get(0);

        if (headers.isEmpty() || headers.stream()
            .allMatch(header -> header == null || header.isBlank())) {
            throw new IllegalArgumentException("CSV header row has no column names");
        }

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);

            if (header == null || header.isBlank()) {
                throw new IllegalArgumentException("CSV header column " + (i + 1) + " is blank");
            }
        }

        List<List<String>> dataRows = rows.subList(1, rows.size());

        for (int i = 0; i < dataRows.size(); i++) {
            int actualColumns = dataRows.get(i)
                .size();

            if (actualColumns != headers.size()) {
                throw new IllegalArgumentException(
                    "CSV row " + (i + 2) + " has " + actualColumns + " columns; expected " + headers.size()
                        + " (matching the header row)");
            }
        }

        return new ParsedCsv(headers, dataRows);
    }

    private static List<List<String>> parseRows(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }

                continue;
            }

            if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                currentRow.add(cell.toString());
                cell.setLength(0);
            } else if (ch == '\n') {
                currentRow.add(cell.toString());
                cell.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<>();
            } else if (ch != '\r') {
                cell.append(ch);
            }
        }

        // Flush the last row when content didn't end on a newline (common for LLM-pasted CSVs).
        if (cell.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(cell.toString());
            rows.add(currentRow);
        }

        return rows;
    }

    private static String validate(CreateDataTableFromCsvInput input) {
        if (input.baseName() == null || input.baseName()
            .isBlank()) {
            return "baseName is required";
        }

        if (input.csvContent() == null || input.csvContent()
            .isBlank()) {
            return "csvContent is required";
        }

        return null;
    }

    private record ParsedCsv(List<String> headers, List<List<String>> rows) {
    }

    public record CreateDataTableFromCsvInput(String baseName, @Nullable String description, String csvContent) {
    }

    public record CreateDataTableFromCsvOutput(
        boolean created, String baseName, List<ColumnSchemaEntry> columns, int rowCount) {

        public CreateDataTableFromCsvOutput {
            columns = columns == null ? null : List.copyOf(columns);
        }
    }

    public record ColumnSchemaEntry(String name, String type) {
    }
}
