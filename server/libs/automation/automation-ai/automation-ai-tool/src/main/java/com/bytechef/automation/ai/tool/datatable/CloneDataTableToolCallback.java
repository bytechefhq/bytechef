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
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class CloneDataTableToolCallback implements ToolCallback {

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

    private final WorkspaceDataTableFacade workspaceDataTableFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CloneDataTableToolCallback(WorkspaceDataTableFacade workspaceDataTableFacade) {
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
            CloneDataTableInput input = JsonUtils.read(toolInput, CloneDataTableInput.class);

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
                    "Workspace context unavailable - open this chat from a workspace.");
            }

            long dataTableId;

            try {
                dataTableId = Long.parseLong(input.dataTableId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid dataTableId — must be a numeric id obtained from listDataTables");
            }

            Long environmentId = invocationContext.environmentId();

            if (environmentId == null) {
                return toolError(
                    "Environment context unavailable - open this chat with an environment selected.");
            }

            DataTableInfo tableInfo = resolveTableInWorkspace(dataTableId, workspaceId, environmentId);

            if (tableInfo == null) {
                return toolError(
                    "Data table " + input.dataTableId() + " not found in the current workspace.");
            }

            try {
                workspaceDataTableFacade.duplicateTable(dataTableId, input.newBaseName(), environmentId);
            } catch (IllegalArgumentException exception) {
                return ToolValidationErrors.validationFailure(CloneDataTableToolCallback.class, TOOL_NAME, exception);
            }

            long newId = workspaceDataTableFacade.getDataTableId(input.newBaseName());

            return JsonUtils.write(new CloneDataTableOutput(newId, input.newBaseName()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(CloneDataTableToolCallback.class, TOOL_NAME, exception);
        }
    }

    private DataTableInfo resolveTableInWorkspace(long dataTableId, long workspaceId, long environmentId) {
        List<DataTableInfo> workspaceTables = workspaceDataTableFacade.listTables(workspaceId, environmentId);

        return workspaceTables.stream()
            .filter(tableInfo -> tableInfo.id() != null && tableInfo.id() == dataTableId)
            .findFirst()
            .orElse(null);
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record CloneDataTableInput(String dataTableId, String newBaseName) {
    }

    public record CloneDataTableOutput(long dataTableId, String baseName) {
    }
}
