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
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that adds a new column to an existing data table. The mutation is executed immediately
 * — every server-side mutation lands in real time and, when a {@link ToolArtifactRecorder} is supplied (AI Hub only),
 * is recorded as a task artifact for audit purposes.
 *
 * <p>
 * This callback is registered on {@code aiHubBuildSpringAIAgent} only — the ASK variant is read-only.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class AddDataTableColumnToolCallback implements ToolCallback {

    /**
     * Name of the artifact kind recorded on success, matching the {@code AiHubTaskArtifactKind.DATA_TABLE_COLUMN_ADDED}
     * enum constant on the AI Hub side. Carried as a plain string so this shared lib does not depend on ai-hub.
     */
    static final String ARTIFACT_KIND_DATA_TABLE_COLUMN_ADDED = "DATA_TABLE_COLUMN_ADDED";

    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;
    private static final String TOOL_NAME = "addDataTableColumn";

    private static final String SUPPORTED_TYPES = Arrays.stream(ColumnType.values())
        .map(ColumnType::name)
        .collect(Collectors.joining(", "));

    private static final String DESCRIPTION = """
        Add a new column to an existing data table (additive only — no rename or drop). Supply the
        dataTableId (from listDataTables), a columnName, and a columnType. Supported types: %s.
        The column is added immediately. The dataTableId must belong to the current workspace.""".formatted(
        SUPPORTED_TYPES);

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "dataTableId": {"type": "string", "description": "Data table id obtained from listDataTables"},
                    "columnName": {"type": "string", "description": "Name of the new column to add"},
                    "columnType": {"type": "string", "description": "Column type: STRING, NUMBER, INTEGER, DATE, DATE_TIME, or BOOLEAN"}
                },
                "required": ["dataTableId", "columnName", "columnType"]
            }""";

    private final DataTableService dataTableService;
    private final WorkspaceDataTableFacade workspaceDataTableFacade;
    private final @Nullable ToolArtifactRecorder artifactRecorder;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AddDataTableColumnToolCallback(
        DataTableService dataTableService, WorkspaceDataTableFacade workspaceDataTableFacade,
        @Nullable ToolArtifactRecorder artifactRecorder) {

        this.dataTableService = dataTableService;
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
            AddDataTableColumnInput input = jsonMapper.readValue(toolInput, AddDataTableColumnInput.class);

            if (input.dataTableId() == null || input.dataTableId()
                .isBlank()) {
                return toolError("dataTableId is required");
            }

            if (input.columnName() == null || input.columnName()
                .isBlank()) {
                return toolError("columnName is required");
            }

            if (input.columnType() == null || input.columnType()
                .isBlank()) {
                return toolError("columnType is required");
            }

            ColumnType columnType;

            try {
                columnType = ColumnType.valueOf(input.columnType()
                    .toUpperCase());
            } catch (IllegalArgumentException exception) {
                return toolError(
                    "Unknown columnType '" + input.columnType() + "'. Supported types: " + SUPPORTED_TYPES);
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
                dataTableId = Long.parseLong(input.dataTableId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid dataTableId - must be a numeric id obtained from listDataTables");
            }

            long environmentId = resolveEnvironmentId(invocationContext);

            DataTableInfo tableInfo = resolveTableInWorkspace(dataTableId, workspaceId, environmentId);

            if (tableInfo == null) {
                return toolError(
                    "Data table " + input.dataTableId() + " not found in the current workspace.");
            }

            String baseName = tableInfo.baseName();

            dataTableService.addColumn(baseName, new ColumnSpec(input.columnName(), columnType), environmentId);

            recordArtifact(invocationContext, baseName, input.dataTableId(), input.columnName(), environmentId);

            return jsonMapper.writeValueAsString(new AddDataTableColumnOutput(true, input.columnName()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, AddDataTableColumnToolCallback.class, TOOL_NAME, exception);
        }
    }

    private void recordArtifact(
        AgentToolInvocationContext invocationContext, String baseName, String dataTableId, String columnName,
        long environmentId) {

        String conversationId = invocationContext.conversationId();
        Long userId = invocationContext.userId();

        if (artifactRecorder == null || conversationId == null || userId == null) {
            return;
        }

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("baseName", baseName);
        metadata.put("columnName", columnName);
        metadata.put("dataTableId", dataTableId);
        metadata.put("environmentId", environmentId);

        artifactRecorder.record(
            conversationId, userId, ARTIFACT_KIND_DATA_TABLE_COLUMN_ADDED,
            dataTableId, baseName + "." + columnName, metadata);
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

    public record AddDataTableColumnInput(String dataTableId, String columnName, String columnType) {
    }

    public record AddDataTableColumnOutput(boolean added, String columnName) {
    }
}
