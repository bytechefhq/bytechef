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
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that updates an existing platform {@link McpServer}'s name and/or enabled state. This
 * single tool doubles as the enable/disable toggle — the underlying {@code McpServerService.update(id, name, enabled)}
 * covers both intents, so no separate toggle tool is needed (unlike deployments, which split rename and toggle).
 *
 * <p>
 * Delegates to {@link WorkspaceMcpServerFacade#updateWorkspaceMcpServer}, whose {@code @PreAuthorize} check enforces
 * the tenant boundary — a caller cannot rename or disable a server outside a workspace they may edit.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class UpdateMcpServerToolCallback implements ToolCallback {

    static final String TOOL_NAME = "updateMcpServer";

    private static final String DESCRIPTION = """
        Update an existing MCP server's name and/or enabled state. Supply mcpServerId (numeric, from
        listMcpServers). At least one of name or enabled must be supplied — fields you omit are left
        unchanged. Use enabled=true to bring a server online or enabled=false to take it offline. Returns
        {mcpServerId, name, enabled} reflecting the post-update values.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "mcpServerId": {"type": "string", "description": "Numeric MCP server id from listMcpServers"},
                    "name": {"type": "string", "description": "New server name (optional; leave omitted to keep)"},
                    "enabled": {"type": "boolean", "description": "New enabled state (optional; leave omitted to keep)"}
                },
                "required": ["mcpServerId"]
            }""";

    private final WorkspaceMcpServerFacade workspaceMcpServerFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateMcpServerToolCallback(WorkspaceMcpServerFacade workspaceMcpServerFacade) {
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
            UpdateMcpServerInput input = jsonMapper.readValue(toolInput, UpdateMcpServerInput.class);

            if (input.mcpServerId() == null || input.mcpServerId()
                .isBlank()) {
                return toolError("mcpServerId is required");
            }

            boolean nameSupplied = input.name() != null && !input.name()
                .isBlank();
            boolean enabledSupplied = input.enabled() != null;

            if (!nameSupplied && !enabledSupplied) {
                return toolError("Supply at least one of name or enabled to update");
            }

            long mcpServerId;

            try {
                mcpServerId = Long.parseLong(input.mcpServerId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid mcpServerId — must be a numeric id");
            }

            McpServer updated = workspaceMcpServerFacade.updateWorkspaceMcpServer(
                mcpServerId, nameSupplied ? input.name() : null, input.enabled());

            return jsonMapper.writeValueAsString(
                new UpdateMcpServerOutput(updated.getId(), updated.getName(), updated.isEnabled()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, UpdateMcpServerToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record UpdateMcpServerInput(String mcpServerId, @Nullable String name, @Nullable Boolean enabled) {
    }

    public record UpdateMcpServerOutput(long mcpServerId, String name, boolean enabled) {
    }
}
