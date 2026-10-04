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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Ivica Cardic
 */
public class WorkspaceScopedFlatToolCallback implements ToolCallback {

    private static final String WORKSPACE_ID_FIELD = "workspaceId";
    private static final String ENVIRONMENT_FIELD = "environment";

    private final ToolCallback delegate;
    private final WorkspaceScopeResolver workspaceScopeResolver;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceScopedFlatToolCallback(ToolCallback delegate, WorkspaceScopeResolver workspaceScopeResolver) {
        this.delegate = delegate;
        this.workspaceScopeResolver = workspaceScopeResolver;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

        Set<String> delegatePropertyNames = getPropertyNames(delegateToolDefinition.inputSchema());

        StringBuilder description = new StringBuilder(delegateToolDefinition.description());

        if (!delegatePropertyNames.contains(WORKSPACE_ID_FIELD)) {
            description.append(" Supply workspaceId when the account has more than one workspace.");
        }

        if (!delegatePropertyNames.contains(ENVIRONMENT_FIELD)) {
            description.append(
                " Supply environment (DEVELOPMENT, STAGING, or PRODUCTION) to target something other than" +
                    " DEVELOPMENT.");
        }

        return ToolDefinition.builder()
            .name(delegateToolDefinition.name())
            .description(description.toString())
            .inputSchema(mergeSchema(delegateToolDefinition.inputSchema(), delegatePropertyNames))
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

            if (inputNode == null) {
                return ToolErrors.toolError(jsonMapper, "Invalid tool input: expected a JSON object");
            }

            ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

            Set<String> delegatePropertyNames = getPropertyNames(delegateToolDefinition.inputSchema());

            JsonNode workspaceIdNode = extractField(inputNode, WORKSPACE_ID_FIELD, delegatePropertyNames);
            JsonNode environmentNode = extractField(inputNode, ENVIRONMENT_FIELD, delegatePropertyNames);

            Long workspaceId = null;

            if (isPresent(workspaceIdNode)) {
                workspaceId = toLong(workspaceIdNode);

                if (workspaceId == null) {
                    return ToolErrors.toolError(
                        jsonMapper, "workspaceId must be an integer workspace id, got: " + workspaceIdNode);
                }
            }

            String requestedEnvironment = isPresent(environmentNode) ? environmentNode.asString() : null;

            WorkspaceScopeResolver.WorkspaceScope workspaceScope =
                workspaceScopeResolver.resolve(workspaceId, requestedEnvironment);

            if (workspaceScope instanceof WorkspaceScopeResolver.Rejected rejected) {
                return rejected.response();
            }

            WorkspaceScopeResolver.Resolved resolved = (WorkspaceScopeResolver.Resolved) workspaceScope;

            Map<String, Object> forwardedContext = resolved.toForwardedContext(toolContext);

            return delegate.call(jsonMapper.writeValueAsString(inputNode), new ToolContext(forwardedContext));
        } catch (JacksonException exception) {
            return ToolErrors.toolError(jsonMapper, "Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            ToolDefinition toolDefinition = delegate.getToolDefinition();

            return ToolErrors.runtimeFailure(
                jsonMapper, WorkspaceScopedFlatToolCallback.class, toolDefinition.name(), exception);
        }
    }

    private @Nullable ObjectNode readInputObject(@Nullable String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return jsonMapper.createObjectNode();
        }

        JsonNode node = jsonMapper.readTree(toolInput);

        return node.isObject() ? (ObjectNode) node : null;
    }

    private static @Nullable JsonNode extractField(
        ObjectNode inputNode, String fieldName, Set<String> delegatePropertyNames) {

        JsonNode fieldNode = inputNode.get(fieldName);

        if (!delegatePropertyNames.contains(fieldName)) {
            inputNode.remove(fieldName);
        }

        return fieldNode;
    }

    private static boolean isPresent(@Nullable JsonNode fieldNode) {
        if (fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode()) {
            return false;
        }

        return !fieldNode.isString() || !fieldNode.asString()
            .isBlank();
    }

    private static @Nullable Long toLong(JsonNode fieldNode) {
        if (fieldNode.isIntegralNumber()) {
            return fieldNode.asLong();
        }

        if (fieldNode.isString()) {
            String value = fieldNode.asString();

            try {
                return Long.parseLong(value.strip());
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private Set<String> getPropertyNames(String inputSchema) {
        JsonNode propertiesNode = jsonMapper.readTree(inputSchema)
            .path("properties");

        return propertiesNode.isObject() ? new HashSet<>(propertiesNode.propertyNames()) : Set.of();
    }

    private String mergeSchema(String delegateInputSchema, Set<String> delegatePropertyNames) {
        JsonNode schemaNode = jsonMapper.readTree(delegateInputSchema);

        if (!schemaNode.isObject()) {
            return delegateInputSchema;
        }

        ObjectNode schemaObjectNode = (ObjectNode) schemaNode;
        JsonNode propertiesNode = schemaObjectNode.get("properties");

        ObjectNode propertiesObjectNode = propertiesNode != null && propertiesNode.isObject()
            ? (ObjectNode) propertiesNode
            : schemaObjectNode.putObject("properties");

        if (!delegatePropertyNames.contains(WORKSPACE_ID_FIELD)) {
            propertiesObjectNode.putObject(WORKSPACE_ID_FIELD)
                .put("type", "integer")
                .put(
                    "description",
                    "Target workspace id. Optional when the account has exactly one workspace; otherwise required "
                        + "— an error response lists the candidates.");
        }

        if (!delegatePropertyNames.contains(ENVIRONMENT_FIELD)) {
            propertiesObjectNode.putObject(ENVIRONMENT_FIELD)
                .put("type", "string")
                .put(
                    "description",
                    "Target environment: DEVELOPMENT, STAGING, or PRODUCTION. Optional — defaults to DEVELOPMENT "
                        + "when omitted.");
        }

        return jsonMapper.writeValueAsString(schemaObjectNode);
    }
}
