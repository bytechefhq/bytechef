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
import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport.DataTableNotFoundException;
import com.bytechef.automation.ai.tool.datatable.DataTableQuerySupport.WhereParseException;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
public class QueryDataTableToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(QueryDataTableToolCallback.class);

    private static final long DEFAULT_ENVIRONMENT_ORDINAL = 0L;
    private static final String TOOL_NAME = "queryDataTable";

    private static final String DESCRIPTION = """
        Query rows from a data table by its id. Optionally filter with a simple equals expression
        via the 'where' parameter (e.g. "status = 'qualified'" or "age = '30'"). Results are
        limited to 50 rows maximum by default. Returns a JSON array where each element is a row
        object with column names as keys. Obtain the dataTableId from listDataTables first.

        exportToCsv is not supported in this tool context; requesting it returns a structured
        error pointing you at the inline rows instead.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "dataTableId": {"type": "string", "description": "Data table id obtained from listDataTables"},
                    "where": {"type": "string", "description": "Optional simple equals filter, e.g. \\"status = 'qualified'\\""},
                    "limit": {"type": "integer", "description": "Maximum number of rows to return (capped at 50)"},
                    "exportToCsv": {"type": "boolean", "description": "Not supported in this tool context; requesting true is rejected with a structured error"}
                },
                "required": ["dataTableId"]
            }""";

    private final WorkspaceDataTableFacade workspaceDataTableFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public QueryDataTableToolCallback(WorkspaceDataTableFacade workspaceDataTableFacade) {
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
            QueryDataTableInput input = JsonUtils.read(toolInput, QueryDataTableInput.class);

            String dataTableIdString = input.dataTableId();

            if (dataTableIdString == null || dataTableIdString.isBlank()) {
                return toolError("dataTableId is required");
            }

            if (Boolean.TRUE.equals(input.exportToCsv())) {
                return toolError(
                    "exportToCsv is not available in this tool context — request the inline rows instead.");
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
                dataTableId = Long.parseLong(dataTableIdString);
            } catch (NumberFormatException exception) {
                return toolError("Invalid dataTableId - must be a numeric id obtained from listDataTables");
            }

            long environmentId = resolveEnvironmentId(invocationContext);

            try {
                DataTableQuerySupport.resolveBaseName(
                    workspaceDataTableFacade, dataTableId, workspaceId, environmentId);
            } catch (DataTableNotFoundException exception) {
                return toolError("Data table not found: " + dataTableIdString);
            }

            int fetchLimit = DataTableQuerySupport.resolveLimit(input.limit());

            List<Map<String, Object>> rowMaps = DataTableQuerySupport.queryRowMaps(
                workspaceDataTableFacade, dataTableId, input.where(), fetchLimit, environmentId);

            return JsonUtils.write(rowMaps);
        } catch (WhereParseException exception) {
            return toolError("Invalid where clause: " + exception.getMessage()
                + ". Use simple equals syntax, e.g. \"status = 'qualified'\"");
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(QueryDataTableToolCallback.class, TOOL_NAME, exception);
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

    public record QueryDataTableInput(
        String dataTableId, @Nullable String where, @Nullable Integer limit, @Nullable Boolean exportToCsv) {
    }
}
