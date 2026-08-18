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
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adapts a sub-agent delegate {@link ToolCallback} for the management MCP server, where no AI Hub chat state exists to
 * inject the workspace-scoped invocation context. The wrapper extends the delegate's input with an optional
 * {@code workspaceId}, resolves it (explicit input, else the tenant's sole workspace, else a typed error listing the
 * candidates), and forwards it to the specialist through the {@link ToolContext} under both
 * {@link AutomationToolInvocationContext#TOOL_CONTEXT_WORKSPACE_ID_KEY} and
 * {@link AgentToolInvocationContext#TOOL_CONTEXT_WORKSPACE_ID_KEY} — the two key families that different specialists'
 * workspace-scoped tools read on the chat surface (asset-file / automation tools read the former; data-table,
 * knowledge-base and context-store tools read the latter).
 *
 * <p>
 * The wrapper also extends the input with an optional {@code environment}, resolved the same way both key families
 * resolve a missing environment on the chat surface — defaulting to {@code DEVELOPMENT} when omitted — and forwarded
 * under both {@link AutomationToolInvocationContext#TOOL_CONTEXT_ENVIRONMENT_ID_KEY} and
 * {@link AgentToolInvocationContext#TOOL_CONTEXT_ENVIRONMENT_ID_KEY}. Without an explicit value a management MCP client
 * whose data lives in {@code PRODUCTION} would silently see an empty {@code DEVELOPMENT} result instead of an error.
 * </p>
 *
 * <p>
 * Every delegate wrapped here also gets {@link AutomationToolInvocationContext#TOOL_CONTEXT_SOURCE_ORDINAL_KEY} set to
 * {@link AutomationToolInvocationContext#SOURCE_ORDINAL_FILES}. Only the {@code asset_file_agent} delegate reads it —
 * every other delegate ignores the key, so writing it unconditionally here is harmless and keeps this wrapper from
 * needing to know which specific delegate it is adapting.
 * </p>
 *
 * <p>
 * Applies to both automation-owned subagent delegates ({@link SubAgentToolCallback}) and the copilot-domain delegates
 * ({@code data_table_agent}, {@code buildWorkflow}, …). Delegates must declare an input schema of exactly
 * {@code {"request": string}}: this wrapper re-serializes the delegate input as {@code {"request": ...}} and drops any
 * other field. Every delegate on this surface satisfies that today.
 * </p>
 *
 * <p>
 * No authorization is added or bypassed here: the management MCP request is already authenticated, and every mutation
 * behind the specialist goes through {@code @PreAuthorize}-guarded facades. Workspace selection only scopes lookups.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class WorkspaceScopedSubAgentToolCallback implements ToolCallback {

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
                    },
                    "environment": {
                        "type": "string",
                        "description": "Target environment: DEVELOPMENT, STAGING, or PRODUCTION. Optional — defaults to DEVELOPMENT when omitted."
                    }
                },
                "required": ["request"]
            }""";

    private final ToolCallback delegate;
    private final WorkspaceService workspaceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceScopedSubAgentToolCallback(ToolCallback delegate, WorkspaceService workspaceService) {
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
                    " Supply workspaceId when the account has more than one workspace. Supply environment" +
                    " (DEVELOPMENT, STAGING, or PRODUCTION) to target something other than DEVELOPMENT.")
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

            String request = input.request();

            if (request == null || request.isBlank()) {
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

            String requestedEnvironment = input.environment();
            long environmentOrdinal = Environment.DEVELOPMENT.ordinal();

            if (requestedEnvironment != null && !requestedEnvironment.isBlank()) {
                try {
                    environmentOrdinal = Environment.valueOf(requestedEnvironment.toUpperCase(Locale.ROOT))
                        .ordinal();
                } catch (IllegalArgumentException exception) {
                    return ToolErrors.toolError(
                        jsonMapper,
                        "Unknown environment '" + requestedEnvironment
                            + "'. Supported: DEVELOPMENT, STAGING, PRODUCTION");
                }
            }

            Map<String, Object> forwardedContext = new HashMap<>();

            if (toolContext != null) {
                forwardedContext.putAll(toolContext.getContext());
            }

            forwardedContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);
            forwardedContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, environmentOrdinal);

            // A missing sourceOrdinal only matters to the asset-file create tools (createAssetFile,
            // createBinaryAssetFile, createAssetFileFromUrl); every other delegate on this surface ignores the
            // key, so writing it unconditionally is harmless and keeps this wrapper delegate-agnostic.
            forwardedContext.put(
                AutomationToolInvocationContext.TOOL_CONTEXT_SOURCE_ORDINAL_KEY,
                AutomationToolInvocationContext.SOURCE_ORDINAL_FILES);

            // Two workspace-id key families exist: automation/asset-file tools read
            // AutomationToolInvocationContext's key, while the data-table, knowledge-base and
            // context-store tools read AgentToolInvocationContext's. The AI Hub chat surface
            // populates both (see AiHubSpringAIAgent#toolContext), and this surface must too or
            // those delegates still fail with "Workspace context unavailable". The same applies to
            // environmentId — without it, resolveEnvironmentOrDefault silently pins every read to DEVELOPMENT.
            forwardedContext.putAll(
                AgentToolInvocationContext.builder()
                    .workspaceId(workspaceId)
                    .environmentId(environmentOrdinal)
                    .build()
                    .toToolContext());

            String delegateInput = jsonMapper.writeValueAsString(Map.of("request", request));

            return delegate.call(delegateInput, new ToolContext(forwardedContext));
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            ToolDefinition toolDefinition = delegate.getToolDefinition();

            return ToolErrors.runtimeFailure(
                jsonMapper, WorkspaceScopedSubAgentToolCallback.class, toolDefinition.name(), exception);
        }
    }

    public record WorkspaceScopedInput(
        @Nullable String request, @Nullable Long workspaceId, @Nullable String environment) {
    }
}
