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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Adapts a flat, multi-field CRUD {@link ToolCallback} — one whose own input schema is NOT the uniform
 * {@code {request: string}} shape every subagent delegate declares — for the management MCP server, where no AI Hub
 * chat state exists to inject the workspace-scoped invocation context. {@link WorkspaceScopedSubAgentToolCallback} is
 * unsuitable here because it re-serializes the delegate input as {@code {"request": ...}}, discarding every other
 * field; a flat tool like {@code createMcpServer} needs its own {@code name}/{@code environment}/{@code enabled} fields
 * to survive intact. This wrapper instead merges two OPTIONAL top-level properties — {@code workspaceId} and
 * {@code environment} — into the delegate's existing schema, strips them back out of the JSON object before forwarding,
 * and leaves every other field untouched.
 *
 * <p>
 * Resolution mirrors the sibling wrapper: an explicit {@code workspaceId} wins; otherwise the tenant's sole workspace
 * auto-selects; otherwise a typed {@code workspace_required} error lists the candidates. {@code environment} defaults
 * to {@code DEVELOPMENT} when omitted.
 * </p>
 *
 * <p>
 * Only writes {@link AutomationToolInvocationContext}'s key family. Both delegates this wrapper is built for
 * ({@code listMcpServers}, {@code createMcpServer}) read exclusively that family, never
 * {@code com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext}'s — unlike the subagent delegates
 * {@link WorkspaceScopedSubAgentToolCallback} wraps, which read either family depending on which specialist. A future
 * delegate wrapped here that also reads the other family would need this class extended the same way.
 * </p>
 *
 * @author Ivica Cardic
 */
public class WorkspaceScopedFlatToolCallback implements ToolCallback {

    private static final String WORKSPACE_ID_FIELD = "workspaceId";
    private static final String ENVIRONMENT_FIELD = "environment";

    private final ToolCallback delegate;
    private final WorkspaceService workspaceService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceScopedFlatToolCallback(ToolCallback delegate, WorkspaceService workspaceService) {
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
            .inputSchema(mergeSchema(delegateToolDefinition.inputSchema()))
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            ObjectNode inputNode = readInputObject(toolInput);

            Long workspaceId = extractAndRemoveLong(inputNode, WORKSPACE_ID_FIELD);
            String requestedEnvironment = extractAndRemoveString(inputNode, ENVIRONMENT_FIELD);

            if (workspaceId == null) {
                List<Workspace> workspaces = workspaceService.getWorkspaces();

                if (workspaces.size() == 1) {
                    workspaceId = workspaces.getFirst()
                        .getId();
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

            return delegate.call(jsonMapper.writeValueAsString(inputNode), new ToolContext(forwardedContext));
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            ToolDefinition toolDefinition = delegate.getToolDefinition();

            return ToolErrors.runtimeFailure(
                jsonMapper, WorkspaceScopedFlatToolCallback.class, toolDefinition.name(), exception);
        }
    }

    private ObjectNode readInputObject(@Nullable String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return jsonMapper.createObjectNode();
        }

        JsonNode node = jsonMapper.readTree(toolInput);

        return node.isObject() ? (ObjectNode) node : jsonMapper.createObjectNode();
    }

    private static @Nullable Long extractAndRemoveLong(ObjectNode inputNode, String fieldName) {
        JsonNode fieldNode = inputNode.get(fieldName);

        inputNode.remove(fieldName);

        if (fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode()) {
            return null;
        }

        if (fieldNode.isNumber()) {
            return fieldNode.asLong();
        }

        if (fieldNode.isString() && !fieldNode.asString()
            .isBlank()) {

            try {
                return Long.parseLong(fieldNode.asString());
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private static @Nullable String extractAndRemoveString(ObjectNode inputNode, String fieldName) {
        JsonNode fieldNode = inputNode.get(fieldName);

        inputNode.remove(fieldName);

        if (fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode()) {
            return null;
        }

        return fieldNode.asString();
    }

    private String mergeSchema(String delegateInputSchema) {
        JsonNode schemaNode = jsonMapper.readTree(delegateInputSchema);

        if (!schemaNode.isObject()) {
            return delegateInputSchema;
        }

        ObjectNode schemaObjectNode = (ObjectNode) schemaNode;
        JsonNode propertiesNode = schemaObjectNode.get("properties");

        ObjectNode propertiesObjectNode = propertiesNode != null && propertiesNode.isObject()
            ? (ObjectNode) propertiesNode
            : schemaObjectNode.putObject("properties");

        propertiesObjectNode.putObject(WORKSPACE_ID_FIELD)
            .put("type", "integer")
            .put(
                "description",
                "Target workspace id. Optional when the account has exactly one workspace; otherwise required "
                    + "— an error response lists the candidates.");
        propertiesObjectNode.putObject(ENVIRONMENT_FIELD)
            .put("type", "string")
            .put(
                "description",
                "Target environment: DEVELOPMENT, STAGING, or PRODUCTION. Optional — defaults to DEVELOPMENT "
                    + "when omitted.");

        return jsonMapper.writeValueAsString(schemaObjectNode);
    }
}
