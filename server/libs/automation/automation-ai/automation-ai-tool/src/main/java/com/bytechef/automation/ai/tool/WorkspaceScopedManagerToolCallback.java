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
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.WorkspaceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adapts a {@link ManagerSubAgentToolCallback} for the management MCP server, where no AI Hub chat state exists to
 * inject the workspace-scoped {@link AutomationToolInvocationContext}. The wrapper extends the delegate's input with an
 * optional {@code workspaceId}, resolves it (explicit input, else the tenant's sole workspace, else a typed error
 * listing the candidates), and forwards it to the specialist through the {@link ToolContext} under
 * {@link AutomationToolInvocationContext#TOOL_CONTEXT_WORKSPACE_ID_KEY} — exactly what the specialist's
 * workspace-scoped tools read on the chat surface.
 *
 * <p>
 * No authorization is added or bypassed here: the management MCP request is already authenticated, and every mutation
 * behind the specialist goes through {@code @PreAuthorize}-guarded facades. Workspace selection only scopes lookups.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class WorkspaceScopedManagerToolCallback implements ToolCallback {

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The task for the specialist, plus any ids or decisions already resolved."
                    },
                    "workspaceId": {
                        "type": "integer",
                        "description": "Target workspace id. Optional when the account has exactly one workspace; otherwise required — an error response lists the candidates."
                    }
                },
                "required": ["request"]
            }""";

    private final ManagerSubAgentToolCallback delegate;
    private final WorkspaceService workspaceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceScopedManagerToolCallback(ManagerSubAgentToolCallback delegate, WorkspaceService workspaceService) {
        this.delegate = delegate;
        this.workspaceService = workspaceService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

        return ToolDefinition.builder()
            .name(delegateToolDefinition.name())
            .description(
                delegateToolDefinition.description() +
                    " Supply workspaceId when the account has more than one workspace.")
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
            WorkspaceScopedInput input = jsonMapper.readValue(toolInput, WorkspaceScopedInput.class);

            if (input.request() == null || input.request()
                .isBlank()) {
                return ToolErrors.toolError(jsonMapper, "request is required and must not be blank");
            }

            Long workspaceId = input.workspaceId();

            if (workspaceId == null) {
                List<Workspace> workspaces = workspaceService.getWorkspaces();

                if (workspaces.size() == 1) {
                    Workspace workspace = workspaces.getFirst();

                    workspaceId = workspace.getId();
                } else {
                    return jsonMapper.writeValueAsString(
                        Map.of(
                            "error", "workspace_required",
                            "message",
                            "No single workspace could be auto-selected — retry with an explicit workspaceId " +
                                "from the list.",
                            "workspaces", workspaces.stream()
                                .map(workspace -> Map.of("id", workspace.getId(), "name", workspace.getName()))
                                .toList()));
                }
            }

            Map<String, Object> forwardedContext = new HashMap<>();

            if (toolContext != null) {
                forwardedContext.putAll(toolContext.getContext());
            }

            forwardedContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);

            String delegateInput = jsonMapper.writeValueAsString(Map.of("request", input.request()));

            return delegate.call(delegateInput, new ToolContext(forwardedContext));
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            ToolDefinition toolDefinition = delegate.getToolDefinition();

            return ToolErrors.runtimeFailure(
                jsonMapper, WorkspaceScopedManagerToolCallback.class, toolDefinition.name(), exception);
        }
    }

    public record WorkspaceScopedInput(@Nullable String request, @Nullable Long workspaceId) {
    }
}
