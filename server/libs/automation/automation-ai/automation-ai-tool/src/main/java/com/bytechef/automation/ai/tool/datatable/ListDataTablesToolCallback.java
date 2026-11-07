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
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
public class ListDataTablesToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ListDataTablesToolCallback.class);

    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;
    private static final String TOOL_NAME = "listDataTables";

    private static final String DESCRIPTION = """
        List all data tables in the current workspace. Returns a JSON array of table summaries
        including id, name, and columns (each column has name and type). Use the returned id values
        when calling queryDataTable. Call this first when the user asks about their data tables.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private final WorkspaceDataTableFacade workspaceDataTableFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListDataTablesToolCallback(WorkspaceDataTableFacade workspaceDataTableFacade) {
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
            AgentToolInvocationContext invocationContext =
                AgentToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from a workspace.");
            }

            long environmentId = resolveEnvironmentId(invocationContext);

            List<DataTableInfo> dataTableInfos = workspaceDataTableFacade.listTables(workspaceId, environmentId);

            List<DataTableSummary> summaries = dataTableInfos.stream()
                .map(info -> {
                    List<ColumnSummary> columnSummaries = info.columns()
                        .stream()
                        .map(columnSpec -> new ColumnSummary(columnSpec.name(), columnSpec.type()))
                        .toList();

                    return new DataTableSummary(info.id(), info.baseName(), columnSummaries);
                })
                .toList();

            return JsonUtils.write(summaries);
        } catch (JacksonException exception) {
            return toolError("Serialization error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(ListDataTablesToolCallback.class, TOOL_NAME, exception);
        }
    }

    private long resolveEnvironmentId(AgentToolInvocationContext invocationContext) {
        Long environmentId = invocationContext.environmentId();

        if (environmentId == null) {
            log.warn(
                "{}: environment id absent from tool context; falling back to environment ordinal {}", TOOL_NAME,
                DEFAULT_ENVIRONMENT_ORDINAL);

            return DEFAULT_ENVIRONMENT_ORDINAL;
        }

        return environmentId;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record ColumnSummary(String name, ColumnType type) {
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record DataTableSummary(Long id, String name, List<ColumnSummary> columns) {
    }
}
