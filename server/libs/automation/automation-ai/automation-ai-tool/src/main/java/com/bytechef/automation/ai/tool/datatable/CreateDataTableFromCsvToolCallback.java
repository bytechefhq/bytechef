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
import com.bytechef.automation.ai.tool.ToolValidationErrors;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @author Ivica Cardic
 */
public class CreateDataTableFromCsvToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CreateDataTableFromCsvToolCallback.class);

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

    private final WorkspaceDataTableFacade workspaceDataTableFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateDataTableFromCsvToolCallback(WorkspaceDataTableFacade workspaceDataTableFacade) {
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
            CreateDataTableFromCsvInput input = JsonUtils.read(toolInput, CreateDataTableFromCsvInput.class);

            String validationError = validate(input);

            if (validationError != null) {
                return ToolErrors.toolError(validationError);
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return ToolErrors.toolError("Workspace context unavailable - open this chat from a workspace.");
            }

            Long environmentId = invocationContext.environmentId();

            if (environmentId == null) {
                return ToolErrors
                    .toolError("Environment context unavailable - open this chat with an environment selected.");
            }

            ParsedCsv parsed = parseCsv(input.csvContent());

            if (parsed.rows.size() > MAX_TOTAL_ROWS) {
                return ToolErrors
                    .toolError("CSV has " + parsed.rows.size() + " rows; the per-import cap is " + MAX_TOTAL_ROWS
                        + ". Suggest a workflow-based bulk import for larger datasets.");
            }

            List<ColumnSpec> columnSpecs = inferColumnSpecs(parsed);

            String description = input.description() == null ? "" : input.description();

            workspaceDataTableFacade.createTable(input.baseName(), description, columnSpecs, workspaceId,
                environmentId);

            int inserted;

            try {
                inserted = insertRows(parsed, columnSpecs, input.baseName(), environmentId);
            } catch (RuntimeException exception) {
                return dropPartiallyPopulatedTable(input.baseName(), workspaceId, environmentId, exception);
            }

            return JsonUtils.write(new CreateDataTableFromCsvOutput(
                true,
                input.baseName(),
                columnSpecs.stream()
                    .map(spec -> new ColumnSchemaEntry(spec.name(), spec.type()
                        .name()))
                    .toList(),
                inserted));
        } catch (IllegalArgumentException exception) {
            return ToolValidationErrors.validationFailure(CreateDataTableFromCsvToolCallback.class, TOOL_NAME,
                exception);
        } catch (JacksonException exception) {
            return ToolErrors.toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(CreateDataTableFromCsvToolCallback.class, TOOL_NAME,
                exception);
        }
    }

    private int insertRows(
        ParsedCsv parsed, List<ColumnSpec> columnSpecs, String baseName, long environmentId) {

        long dataTableId = workspaceDataTableFacade.getDataTableId(baseName);

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

            workspaceDataTableFacade.insertRow(dataTableId, values, environmentId);
            inserted++;
        }

        return inserted;
    }

    private String dropPartiallyPopulatedTable(
        String baseName, long workspaceId, long environmentId, RuntimeException exception) {

        Class<? extends RuntimeException> exceptionClass = exception.getClass();
        String failureCause = exceptionClass.getSimpleName();

        log.warn(
            "{}: row insert failed for table '{}'; dropping the partially created table", TOOL_NAME, baseName,
            exception);

        Long dataTableId = resolveDataTableId(baseName, workspaceId, environmentId);

        if (dataTableId == null) {
            return ToolErrors
                .toolError("Row insert failed (" + failureCause + ") and the partially created table '" + baseName
                    + "' could not be located for cleanup; drop it manually before retrying.");
        }

        try {
            workspaceDataTableFacade.dropTable(dataTableId, environmentId);
        } catch (RuntimeException dropException) {
            log.warn("{}: cleanup drop of table '{}' failed", TOOL_NAME, baseName, dropException);

            return ToolErrors
                .toolError("Row insert failed (" + failureCause + ") and the partially created table '" + baseName
                    + "' could not be dropped; drop it manually before retrying.");
        }

        return ToolErrors.toolError("Row insert failed (" + failureCause + "). The partially created table '" + baseName
            + "' was dropped, so no rows were kept and retrying with corrected data is safe.");
    }

    private @Nullable Long resolveDataTableId(String baseName, long workspaceId, long environmentId) {
        return workspaceDataTableFacade.listTables(workspaceId, environmentId)
            .stream()
            .filter(dataTableInfo -> baseName.equalsIgnoreCase(dataTableInfo.baseName()))
            .map(dataTableInfo -> dataTableInfo.id())
            .findFirst()
            .orElse(null);
    }

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

    private static @Nullable Object coerceCellValue(String raw, ColumnType type) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        String trimmed = raw.trim();

        try {
            return switch (type) {
                case BOOLEAN -> {
                    String lower = trimmed.toLowerCase(Locale.ROOT);

                    if ("true".equals(lower) || "yes".equals(lower)) {
                        yield Boolean.TRUE;
                    }

                    if ("false".equals(lower) || "no".equals(lower)) {
                        yield Boolean.FALSE;
                    }

                    yield raw;
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
