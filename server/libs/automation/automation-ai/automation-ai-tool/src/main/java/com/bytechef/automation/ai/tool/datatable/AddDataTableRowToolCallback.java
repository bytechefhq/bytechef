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
import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that inserts a new row into a data table. The mutation is executed immediately — every
 * server-side mutation lands in real time and, when a {@link ToolArtifactRecorder} is supplied (AI Hub only), is
 * recorded as a task artifact for audit purposes.
 *
 * <p>
 * This callback is registered on {@code aiHubBuildSpringAIAgent} only — the ASK variant is read-only.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class AddDataTableRowToolCallback implements ToolCallback {

    /**
     * Name of the artifact kind recorded on success, matching the {@code AiHubChatArtifactKind.DATA_TABLE_ROW_ADDED}
     * enum constant on the AI Hub side. Carried as a plain string so this shared lib does not depend on ai-hub.
     */
    static final String ARTIFACT_KIND_DATA_TABLE_ROW_ADDED = "DATA_TABLE_ROW_ADDED";

    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;
    private static final String TOOL_NAME = "addDataTableRow";

    private static final String DESCRIPTION = """
        Insert a new row into a data table. Supply the dataTableId (from listDataTables) and a
        values object mapping column names to their values. The row is inserted immediately and
        the new row id is returned. The dataTableId must belong to the current workspace.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "dataTableId": {"type": "string", "description": "Data table id obtained from listDataTables"},
                    "values": {"type": "object", "description": "Column name to value mapping for the new row",
                               "additionalProperties": true}
                },
                "required": ["dataTableId", "values"]
            }""";

    private final DataTableRowService dataTableRowService;
    private final WorkspaceDataTableFacade workspaceDataTableFacade;
    private final @Nullable ToolArtifactRecorder artifactRecorder;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AddDataTableRowToolCallback(
        DataTableRowService dataTableRowService, WorkspaceDataTableFacade workspaceDataTableFacade,
        @Nullable ToolArtifactRecorder artifactRecorder) {

        this.dataTableRowService = dataTableRowService;
        this.workspaceDataTableFacade = workspaceDataTableFacade;
        this.artifactRecorder = artifactRecorder;
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
            AddDataTableRowInput input = jsonMapper.readValue(toolInput, AddDataTableRowInput.class);

            String dataTableIdString = input.dataTableId();

            if (dataTableIdString == null || dataTableIdString.isBlank()) {
                return toolError("dataTableId is required");
            }

            Map<String, Object> values = input.values();

            if (values == null || values.isEmpty()) {
                return toolError("values must not be empty");
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

            long environmentId = resolveEnvironmentId(invocationContext);

            DataTableInfo tableInfo = resolveTableInWorkspace(dataTableId, workspaceId, environmentId);

            if (tableInfo == null) {
                return toolError(
                    "Data table " + dataTableIdString + " not found in the current workspace.");
            }

            DataTableRow inserted = dataTableRowService.insertRow(tableInfo.baseName(), values, environmentId);

            recordArtifact(invocationContext, tableInfo.baseName(), inserted.id());

            return jsonMapper.writeValueAsString(new AddDataTableRowOutput(true, inserted.id()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, AddDataTableRowToolCallback.class, TOOL_NAME, exception);
        }
    }

    private void recordArtifact(AgentToolInvocationContext invocationContext, String baseName, long rowId) {
        String conversationId = invocationContext.conversationId();
        Long userId = invocationContext.userId();

        if (artifactRecorder != null && conversationId != null && userId != null) {
            artifactRecorder.record(
                conversationId, userId, ARTIFACT_KIND_DATA_TABLE_ROW_ADDED,
                String.valueOf(rowId), baseName + " row " + rowId, null);
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

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record AddDataTableRowInput(String dataTableId, @Nullable Map<String, Object> values) {
    }

    public record AddDataTableRowOutput(boolean added, long rowId) {
    }
}
