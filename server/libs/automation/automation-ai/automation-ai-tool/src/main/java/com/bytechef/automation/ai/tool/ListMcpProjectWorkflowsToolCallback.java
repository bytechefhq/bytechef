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

package com.bytechef.automation.ai.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class ListMcpProjectWorkflowsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listMcpProjectWorkflows";

    private static final String DESCRIPTION = """
        List the workflows attached to an MCP server (via its MCP projects) together with their MCP tool
        mapping state. Returns a JSON array of {mcpProjectId, mcpProjectWorkflowId, workflowId,
        workflowLabel, toolCallable, toolName, toolDescription, parameters, inputSchema} entries.
        toolName/toolDescription are what MCP clients will see; parameters holds per-input values where a
        value may be a fromAi(...) expression string; inputSchema is the JSON schema the workflow's
        callable trigger declares for its input. A row with toolCallable=false cannot be exposed as a tool
        at all (its workflow lacks a callable trigger). A row with a null toolName is attached but NOT yet
        servable — complete it with updateMcpProjectWorkflowParameters.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "mcpServerId": {"type": "integer", "description": "Numeric MCP server id from listMcpServers"}
            },
            "required": ["mcpServerId"]
        }""";

    private final McpProjectWorkflowFacade mcpProjectWorkflowFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListMcpProjectWorkflowsToolCallback(McpProjectWorkflowFacade mcpProjectWorkflowFacade) {
        this.mcpProjectWorkflowFacade = mcpProjectWorkflowFacade;
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
            ListMcpProjectWorkflowsInput input = jsonMapper.readValue(toolInput, ListMcpProjectWorkflowsInput.class);

            Long mcpServerId = input.mcpServerId();

            if (mcpServerId == null) {
                return ToolErrors.toolError(jsonMapper, "mcpServerId is required");
            }

            return jsonMapper.writeValueAsString(mcpProjectWorkflowFacade.getMcpServerMcpProjectWorkflows(mcpServerId));
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return ToolErrors.toolError(jsonMapper, exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ListMcpProjectWorkflowsToolCallback.class, TOOL_NAME, exception);
        }
    }

    public record ListMcpProjectWorkflowsInput(@Nullable Long mcpServerId) {
    }
}
