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
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @author Ivica Cardic
 */
public class DropDataTableToolCallback implements ToolCallback {

    static final String TOOL_NAME = "dropDataTable";

    private static final String DESCRIPTION = """
        Drop an entire data table and all its rows in the current environment. Irreversible.
        Always confirm with the user before calling.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "id": {"type": "integer", "description": "Data table id to drop"}
            },
            "required": ["id"]
        }""";

    private final WorkspaceDataTableFacade workspaceDataTableFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DropDataTableToolCallback(WorkspaceDataTableFacade workspaceDataTableFacade) {
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
            DropDataTableToolInput input = JsonUtils.read(toolInput, DropDataTableToolInput.class);

            if (input.id() == null) {
                return toolError("id is required");
            }

            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError("Workspace context unavailable - open this chat from a workspace.");
            }

            Long environmentId = invocationContext.environmentId();

            if (environmentId == null) {
                return toolError(
                    "Environment context unavailable - open this chat with an environment selected.");
            }

            if (resolveTableInWorkspace(input.id(), workspaceId, environmentId) == null) {
                return toolError("Data table not found: " + input.id());
            }

            workspaceDataTableFacade.dropTable(input.id(), environmentId);

            return JsonUtils.write(Map.of("deleted", true, "id", input.id()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return ToolValidationErrors.validationFailure(DropDataTableToolCallback.class, TOOL_NAME, exception);
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(DropDataTableToolCallback.class, TOOL_NAME, exception);
        }
    }

    private @Nullable DataTableInfo resolveTableInWorkspace(long dataTableId, long workspaceId, long environmentId) {
        List<DataTableInfo> workspaceTables = workspaceDataTableFacade.listTables(workspaceId, environmentId);

        return workspaceTables.stream()
            .filter(dataTableInfo -> dataTableInfo.id() != null && dataTableInfo.id() == dataTableId)
            .findFirst()
            .orElse(null);
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record DropDataTableToolInput(Long id) {
    }
}
