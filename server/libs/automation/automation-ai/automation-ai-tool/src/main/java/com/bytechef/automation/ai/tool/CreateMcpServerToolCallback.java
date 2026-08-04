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
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that creates a platform {@link McpServer} in the caller's workspace. The server type
 * is always {@link PlatformType#AUTOMATION} on this surface (an MCP server exposing workflows-as-tools), so the LLM
 * never supplies it. The server is created disabled by default — {@code updateMcpServer} brings it online after the
 * user has attached workflows via {@code createMcpProject}.
 *
 * <p>
 * Delegates to {@link WorkspaceMcpServerFacade#createWorkspaceMcpServer}, whose {@code @PreAuthorize} check enforces
 * that the resolved workspace is one the caller may create servers in — the tenant boundary lives in the facade, not
 * here.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class CreateMcpServerToolCallback implements ToolCallback {

    static final String TOOL_NAME = "createMcpServer";

    private static final String SUPPORTED_ENVIRONMENTS = Arrays.stream(Environment.values())
        .map(Environment::name)
        .collect(Collectors.joining(", "));

    private static final String DESCRIPTION = """
        Create a new MCP server in the current workspace. An MCP server exposes selected workflows as MCP
        tools to external MCP clients. Supply name and environment (one of: %s). Optionally supply enabled
        (defaults to false); prefer leaving it disabled and enabling later with updateMcpServer once
        workflows are attached via createMcpProject, but you may set enabled=true to create it already
        online. Returns {mcpServerId, name, type, environment, enabled} on success or {error: <message>}
        on failure.""".formatted(SUPPORTED_ENVIRONMENTS);

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Human-readable MCP server name"},
                    "environment": {"type": "string", "description": "Target environment: DEVELOPMENT, STAGING, or PRODUCTION"},
                    "enabled": {"type": "boolean", "description": "Whether the server is enabled (optional, default false)"}
                },
                "required": ["name", "environment"]
            }""";

    private final WorkspaceMcpServerFacade workspaceMcpServerFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateMcpServerToolCallback(WorkspaceMcpServerFacade workspaceMcpServerFacade) {
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
            AutomationToolInvocationContext context = AutomationToolInvocationContext.fromToolContext(toolContext);

            if (context == null || context.workspaceId() == null) {
                return toolError("Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            CreateMcpServerInput input = jsonMapper.readValue(toolInput, CreateMcpServerInput.class);

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            if (input.environment() == null || input.environment()
                .isBlank()) {
                return toolError("environment is required (one of: " + SUPPORTED_ENVIRONMENTS + ")");
            }

            Environment environment;

            try {
                environment = Environment.valueOf(input.environment()
                    .toUpperCase());
            } catch (IllegalArgumentException exception) {
                return toolError(
                    "Unknown environment '" + input.environment() + "'. Supported: " + SUPPORTED_ENVIRONMENTS);
            }

            boolean enabled = input.enabled() != null && input.enabled();

            McpServer mcpServer = workspaceMcpServerFacade.createWorkspaceMcpServer(
                input.name(), PlatformType.AUTOMATION, environment, enabled, null, context.workspaceId());

            return jsonMapper.writeValueAsString(
                new CreateMcpServerOutput(
                    mcpServer.getId(), mcpServer.getName(),
                    mcpServer.getType()
                        .name(),
                    mcpServer.getEnvironment()
                        .name(),
                    mcpServer.isEnabled()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CreateMcpServerToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CreateMcpServerInput(String name, String environment, @Nullable Boolean enabled) {
    }

    public record CreateMcpServerOutput(
        long mcpServerId, String name, String type, String environment, boolean enabled) {
    }
}
