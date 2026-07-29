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
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that duplicates an existing dynamic data table (column schema + rows) under a fresh
 * base name in the same environment.
 *
 * <p>
 * Same-environment scope: DataTable physical naming is keyed by environment ordinal ({@code dt_<env>_<baseName>}) so a
 * "cross-env clone" would mean copying rows across environments. We don't expose that here — the user's typical
 * phrasing is "make me a copy of the orders table to mess with" rather than "promote orders to PROD." For cross-env
 * data table propagation, prefer driving it through a workflow that reads from one env and writes to another so the
 * copy is auditable and re-runnable.
 * </p>
 *
 * <p>
 * Direct execution rather than staged: the existing {@link DataTableService#duplicateTable(String, String, long)}
 * implementation is the same primitive {@code AddDataTableRowToolCallback}-style staged mutations would commit, but
 * cloning is fully additive (creates a brand-new physical table, never mutates the source). The staged-review loop adds
 * friction without a clear payoff — same rationale as {@link CreateDataTableFromCsvToolCallback}.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class CloneDataTableToolCallback implements ToolCallback {

    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;
    private static final String TOOL_NAME = "cloneDataTable";

    private static final String DESCRIPTION = """
        Duplicate an existing data table (column schema + rows) under a new base name in the same environment.
        Use when the user says "clone the orders table as orders_test" or "make me a copy of products to
        experiment with." Pass the source table's dataTableId (from listDataTables) and a newBaseName for the
        copy. Both source and clone live in the same environment — for cross-environment propagation, drive a
        workflow that reads from one env and writes to another. Returns {dataTableId, baseName} for the new
        table.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "dataTableId": {"type": "string", "description": "Source data table id from listDataTables. Never invent ids."},
                    "newBaseName": {"type": "string", "description": "Snake_case identifier for the clone (must not collide with an existing table in the same environment)"}
                },
                "required": ["dataTableId", "newBaseName"]
            }""";

    private final DataTableService dataTableService;
    private final WorkspaceDataTableFacade workspaceDataTableFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CloneDataTableToolCallback(
        DataTableService dataTableService, WorkspaceDataTableFacade workspaceDataTableFacade) {

        this.dataTableService = dataTableService;
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
            CloneDataTableInput input = jsonMapper.readValue(toolInput, CloneDataTableInput.class);

            if (input.dataTableId() == null || input.dataTableId()
                .isBlank()) {
                return toolError("dataTableId is required");
            }

            if (input.newBaseName() == null || input.newBaseName()
                .isBlank()) {
                return toolError("newBaseName is required");
            }

            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            long dataTableId;

            try {
                dataTableId = Long.parseLong(input.dataTableId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid dataTableId — must be a numeric id obtained from listDataTables");
            }

            long environmentId = resolveEnvironmentId(invocationContext);

            // Defense-in-depth: scope the lookup to (workspaceId, environmentId) so a forged dataTableId from another
            // workspace can't be cloned. Same pattern as AddDataTableColumnToolCallback's resolveTableInWorkspace.
            DataTableInfo tableInfo = resolveTableInWorkspace(dataTableId, workspaceId, environmentId);

            if (tableInfo == null) {
                return toolError(
                    "Data table " + input.dataTableId() + " not found in the current workspace.");
            }

            try {
                dataTableService.duplicateTable(tableInfo.baseName(), input.newBaseName(), environmentId);
            } catch (IllegalArgumentException exception) {
                // Captures both validateBaseName failures (invalid characters) and registry collisions when the new
                // base name is already taken in this environment. Surface verbatim so the LLM can pick a different
                // newBaseName instead of silently overwriting.
                return toolError(exception.getMessage());
            }

            long newId = dataTableService.getIdByBaseName(input.newBaseName());

            return jsonMapper.writeValueAsString(new CloneDataTableOutput(newId, input.newBaseName()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CloneDataTableToolCallback.class, TOOL_NAME, exception);
        }
    }

    private DataTableInfo resolveTableInWorkspace(long dataTableId, long workspaceId, long environmentId) {
        List<DataTableInfo> workspaceTables = workspaceDataTableFacade.listTables(workspaceId, environmentId);

        return workspaceTables.stream()
            .filter(tableInfo -> tableInfo.id() != null && tableInfo.id() == dataTableId)
            .findFirst()
            .orElse(null);
    }

    private long resolveEnvironmentId(AgentToolInvocationContext invocationContext) {
        Long environmentId = invocationContext.environmentId();

        return environmentId != null ? environmentId : DEFAULT_ENVIRONMENT_ORDINAL;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CloneDataTableInput(String dataTableId, String newBaseName) {
    }

    public record CloneDataTableOutput(long dataTableId, String baseName) {
    }
}
