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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Creates a new, empty data table from an explicit column schema. This is the tool for "make me an invoices table with
 * these columns" — before it existed the only creation path was {@code createDataTableFromCsv}, whose type inference
 * needs sample data, so building a table with typed columns (DATE, NUMBER, ...) forced the agent to insert a
 * placeholder seed row and delete it again. That workaround polluted the task's artifact audit log with
 * added-then-deleted row entries; an explicit schema needs no seed data at all.
 *
 *
 * @author Ivica Cardic
 */
public class CreateDataTableToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "createDataTable";

    private static final String DESCRIPTION = """
        Create a new, EMPTY data table with an explicit column schema. Use when the user asks for a new
        table and describes (or you can derive) its columns — e.g. as the storage target of a workflow being
        built. Supply baseName (snake_case identifier) and a columns array of {name, type}; supported types:
        STRING, NUMBER, INTEGER, DATE, DATE_TIME, BOOLEAN. The table is created immediately with no rows —
        NEVER insert placeholder/sample rows to establish a schema; this tool makes that unnecessary. Only
        use createDataTableFromCsv when the user provides actual CSV data to import. Returns the created
        table's id, base name, and column schema.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "baseName": {"type": "string", "description": "Snake_case identifier for the new table"},
                    "description": {"type": "string", "description": "Optional human-readable description"},
                    "columns": {
                        "type": "array",
                        "description": "Column schema for the new table",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string", "description": "Snake_case column name"},
                                "type": {
                                    "type": "string",
                                    "enum": ["STRING", "NUMBER", "INTEGER", "DATE", "DATE_TIME", "BOOLEAN"],
                                    "description": "Column type"
                                }
                            },
                            "required": ["name", "type"]
                        },
                        "minItems": 1
                    }
                },
                "required": ["baseName", "columns"]
            }""";

    private final WorkspaceDataTableFacade workspaceDataTableFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateDataTableToolCallback(WorkspaceDataTableFacade workspaceDataTableFacade) {
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
            CreateDataTableInput input = jsonMapper.readValue(toolInput, CreateDataTableInput.class);

            String validationError = validate(input);

            if (validationError != null) {
                return ToolErrors.toolError(jsonMapper, validationError);
            }

            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return ToolErrors.toolError(
                    jsonMapper,
                    "Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            long environmentId = invocationContext == null ? 0L : invocationContext.resolveEnvironmentOrDefault();

            List<ColumnSpec> columnSpecs = toColumnSpecs(input.columns());

            String description = input.description() == null ? "" : input.description();

            workspaceDataTableFacade.createTable(
                input.baseName(), description, columnSpecs, workspaceId, environmentId);

            // createTable returns void, so resolve the fresh table's id by base name — callers (openDataTableTab,
            // the workflow build) need the id, and making the LLM issue a listDataTables round-trip for it would
            // just invite a fabricated value.
            Long dataTableId = workspaceDataTableFacade.listTables(workspaceId, environmentId)
                .stream()
                .filter(dataTableInfo -> input.baseName()
                    .equals(dataTableInfo.baseName()))
                .map(dataTableInfo -> dataTableInfo.id())
                .findFirst()
                .orElse(null);

            return jsonMapper.writeValueAsString(new CreateDataTableOutput(
                true,
                dataTableId,
                input.baseName(),
                columnSpecs.stream()
                    .map(spec -> new ColumnSchemaEntry(spec.name(), spec.type()
                        .name()))
                    .toList()));
        } catch (IllegalArgumentException exception) {
            return ToolErrors.toolError(jsonMapper, exception.getMessage());
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CreateDataTableToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static List<ColumnSpec> toColumnSpecs(List<ColumnInput> columns) {
        List<ColumnSpec> columnSpecs = new ArrayList<>(columns.size());

        for (ColumnInput column : columns) {
            ColumnType columnType;

            try {
                columnType = ColumnType.valueOf(column.type()
                    .trim()
                    .toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                    "Unsupported column type '" + column.type() + "' for column '" + column.name()
                        + "'; supported types: STRING, NUMBER, INTEGER, DATE, DATE_TIME, BOOLEAN");
            }

            columnSpecs.add(new ColumnSpec(column.name(), columnType));
        }

        return columnSpecs;
    }

    private static @Nullable String validate(CreateDataTableInput input) {
        if (input.baseName() == null || input.baseName()
            .isBlank()) {

            return "baseName is required";
        }

        if (input.columns() == null || input.columns()
            .isEmpty()) {

            return "columns is required and must contain at least one column";
        }

        for (ColumnInput column : input.columns()) {
            if (column.name() == null || column.name()
                .isBlank()) {

                return "Every column needs a non-blank name";
            }

            if (column.type() == null || column.type()
                .isBlank()) {

                return "Column '" + column.name() + "' needs a type";
            }
        }

        return null;
    }

    public record CreateDataTableInput(String baseName, @Nullable String description, List<ColumnInput> columns) {

        public CreateDataTableInput {
            columns = columns == null ? null : List.copyOf(columns);
        }
    }

    public record ColumnInput(String name, String type) {
    }

    public record CreateDataTableOutput(
        boolean created, @Nullable Long dataTableId, String baseName, List<ColumnSchemaEntry> columns) {

        public CreateDataTableOutput {
            columns = columns == null ? null : List.copyOf(columns);
        }
    }

    public record ColumnSchemaEntry(String name, String type) {
    }
}
