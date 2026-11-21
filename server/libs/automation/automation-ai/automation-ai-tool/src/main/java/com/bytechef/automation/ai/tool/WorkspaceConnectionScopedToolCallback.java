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
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
public class WorkspaceConnectionScopedToolCallback implements ToolCallback {

    private static final String COMPONENT_NAME_FIELD = "componentName";
    private static final String CONNECTION_ID_FIELD = "connectionId";

    private final ToolCallback delegate;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceConnectionScopedToolCallback(
        ToolCallback delegate, WorkspaceConnectionFacade workspaceConnectionFacade) {

        this.delegate = delegate;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            JsonNode inputNode = readInput(toolInput);

            if (!inputNode.isObject()) {
                return ToolErrors.toolError("Invalid tool input: expected a JSON object");
            }

            JsonNode connectionIdNode = inputNode.get(CONNECTION_ID_FIELD);

            if (!isPresent(connectionIdNode)) {
                return delegate.call(toolInput, toolContext);
            }

            Long connectionId = toLong(connectionIdNode);

            if (connectionId == null) {
                return ToolErrors.toolError(
                    "connectionId must be an integer connection id, got: " + connectionIdNode);
            }

            AutomationToolInvocationContext invocationContext = AutomationToolInvocationContext.fromToolContext(
                toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();
            Long environmentId = invocationContext == null ? null : invocationContext.environmentId();

            if (workspaceId == null || environmentId == null) {
                return ToolErrors.toolError(
                    "Connection " + connectionId + " was not used: the workspace and environment of this call could " +
                        "not be determined. Supply workspaceId (call listWorkspaces to find it) and retry.");
            }

            String componentName = getComponentName(inputNode);

            if (componentName == null) {
                return ToolErrors.toolError(
                    "Connection " + connectionId + " was not used: componentName is required with connectionId.");
            }

            if (!isWorkspaceConnection(connectionId, componentName, workspaceId, environmentId)) {
                return ToolErrors.toolError(
                    "Connection " + connectionId + " is not a " + componentName + " connection of workspace " +
                        workspaceId + " in the target environment. Call listConnections to find a connection this " +
                        "workspace may use.");
            }

            return delegate.call(toolInput, toolContext);
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (RuntimeException exception) {
            ToolDefinition toolDefinition = delegate.getToolDefinition();

            return DelegateToolErrors.toToolError(
                WorkspaceConnectionScopedToolCallback.class, toolDefinition.name(), exception);
        }
    }

    private boolean isWorkspaceConnection(
        long connectionId, String componentName, long workspaceId, long environmentId) {

        List<ConnectionDTO> connectionDTOs = workspaceConnectionFacade.getConnections(
            workspaceId, componentName, null, environmentId, null);

        return connectionDTOs.stream()
            .anyMatch(connectionDTO -> Objects.equals(connectionDTO.id(), connectionId));
    }

    private static JsonNode readInput(@Nullable String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return JsonUtils.createObjectNode();
        }

        return JsonUtils.readTree(toolInput);
    }

    private static @Nullable String getComponentName(JsonNode inputNode) {
        JsonNode componentNameNode = inputNode.get(COMPONENT_NAME_FIELD);

        if (!isPresent(componentNameNode) || !componentNameNode.isString()) {
            return null;
        }

        return componentNameNode.asString();
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
}
