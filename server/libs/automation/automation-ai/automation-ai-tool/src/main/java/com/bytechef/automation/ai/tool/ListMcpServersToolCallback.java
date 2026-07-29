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
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.platform.mcp.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists every MCP server the current workspace can address. Read leg paired with
 * {@link CreateMcpProjectToolCallback} and {@link CloneMcpProjectToolCallback}: those tools require an
 * {@code mcpServerId} parameter, and without this list tool the LLM has no discoverable way to resolve a user-friendly
 * server name into a numeric id — it would either invent one or have to ask the user to read it from the UI.
 *
 * <p>
 * Returns a compact summary per server: {@code id}, {@code name}, {@code type} (the
 * {@link com.bytechef.platform.constant.PlatformType}), {@code environment}, and {@code enabled}. The
 * {@code environment} field lets the LLM filter to "servers in PROD" without an extra round-trip; an MCP project's
 * underlying {@link com.bytechef.automation.configuration.domain.ProjectDeployment} is implicitly DEV today (see
 * {@link CloneMcpProjectToolCallback}), but the server itself is env-scoped and the LLM should still surface the right
 * env to the user.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class ListMcpServersToolCallback implements ToolCallback {

    private static final String TOOL_NAME = "listMcpServers";

    private static final String DESCRIPTION = """
        List the MCP servers belonging to the current workspace. Use this before createMcpProject or
        cloneMcpProject when the user references an MCP server by name ("the staging MCP server", "my main
        MCP") so you can resolve the name to a numeric id without inventing one. Returns a JSON array of
        {id, name, type, environment, enabled} entries.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {}
        }""";

    private final WorkspaceMcpServerFacade workspaceMcpServerFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListMcpServersToolCallback(WorkspaceMcpServerFacade workspaceMcpServerFacade) {
        this.workspaceMcpServerFacade = workspaceMcpServerFacade;
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
            AutomationToolInvocationContext context =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            if (context == null || context.workspaceId() == null) {
                return ToolErrors.toolError(
                    jsonMapper,
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            List<McpServer> servers = workspaceMcpServerFacade.getWorkspaceMcpServers(context.workspaceId());

            return jsonMapper.writeValueAsString(servers.stream()
                .map(ListMcpServersToolCallback::toSummary)
                .toList());
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Serialization error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, ListMcpServersToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static McpServerSummary toSummary(McpServer server) {
        // Stringify the type/environment enums so the JSON shape stays stable across enum reorderings — the LLM
        // matches against the string ("PRODUCTION", "DEVELOPMENT", "AUTOMATION", etc.) rather than an opaque
        // ordinal that could shift when a new enum value is appended.
        return new McpServerSummary(
            server.getId(), server.getName(),
            server.getType()
                .name(),
            server.getEnvironment()
                .name(),
            server.isEnabled());
    }

    public record McpServerSummary(long id, String name, String type, String environment, boolean enabled) {
    }
}
