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
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class WorkspaceScopedSubAgentToolCallback implements ToolCallback {

    private static final JsonMapper SCHEMA_MAPPER = JsonMapper.builder()
        .build();
    private static final TypeReference<Map<String, Object>> SCHEMA_TYPE = new TypeReference<>() {};

    private static final String ENVIRONMENT = "environment";
    private static final String REQUEST = "request";
    private static final String WORKSPACE_ID = "workspaceId";

    private static final Map<String, Object> ENVIRONMENT_PROPERTY = Map.of(
        "type", "string",
        "description",
        "Target environment: DEVELOPMENT, STAGING, or PRODUCTION. Optional - defaults to the chat's environment, or DEVELOPMENT.");

    private static final Map<String, Object> WORKSPACE_ID_PROPERTY = Map.of(
        "type", "integer",
        "description",
        "Target workspace id. Optional inside a Copilot chat, which defaults to the chat's workspace; otherwise required - call listWorkspaces to find it.");

    private final ToolCallback delegate;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceScopedSubAgentToolCallback(ToolCallback delegate, WorkspaceFacade workspaceFacade) {
        this.delegate = delegate;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

        return ToolDefinition.builder()
            .name(delegateToolDefinition.name())
            .description(
                delegateToolDefinition.description() +
                    " Supply workspaceId outside a Copilot chat (call listWorkspaces to find it). Supply environment" +
                    " (DEVELOPMENT, STAGING, or PRODUCTION) to target another environment.")
            .inputSchema(SCHEMA_MAPPER.writeValueAsString(getInputSchema(delegateToolDefinition.inputSchema())))
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            Map<String, Object> delegateInput = new LinkedHashMap<>(JsonUtils.readMap(toolInput));

            Object workspaceIdValue = delegateInput.remove(WORKSPACE_ID);
            Object environmentValue = delegateInput.remove(ENVIRONMENT);

            if (isRequestRequired() && isBlank(delegateInput.get(REQUEST))) {
                return ToolErrors.toolError("request is required and must not be blank");
            }

            Long requestedWorkspaceId = null;

            if (workspaceIdValue != null) {
                requestedWorkspaceId = toWorkspaceId(workspaceIdValue);

                if (requestedWorkspaceId == null) {
                    return ToolErrors.toolError("Invalid tool input: workspaceId must be an integer");
                }
            }

            Long workspaceId;

            try {
                workspaceId = AutomationToolInvocationContext.resolveWorkspaceId(
                    requestedWorkspaceId, toolContext, workspaceFacade);
            } catch (WorkspaceNotAccessibleException exception) {
                return ToolErrors.toolError(exception.getMessage());
            }

            if (workspaceId == null) {
                return ToolErrors.toolError("workspaceId is required - call listWorkspaces to find the workspace id.");
            }

            long environmentId;

            try {
                environmentId = AutomationToolInvocationContext.resolveEnvironmentId(
                    environmentValue == null ? null : environmentValue.toString(), toolContext);
            } catch (UnknownEnvironmentException exception) {
                return ToolErrors.toolError(exception.getMessage());
            }

            Map<String, Object> forwardedContext = AutomationToolInvocationContext.toForwardedContext(
                toolContext, workspaceId, environmentId);

            return delegate.call(JsonUtils.write(delegateInput), new ToolContext(forwardedContext));
        } catch (JacksonException exception) {
            return ToolErrors.toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            ToolDefinition toolDefinition = delegate.getToolDefinition();

            return ToolErrors.runtimeFailure(WorkspaceScopedSubAgentToolCallback.class, toolDefinition.name(),
                exception);
        }
    }

    private boolean isRequestRequired() {
        ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

        Map<String, Object> delegateInputSchema = SCHEMA_MAPPER.readValue(
            delegateToolDefinition.inputSchema(), SCHEMA_TYPE);

        Object required = delegateInputSchema.get("required");

        return required instanceof List<?> requiredNames && requiredNames.contains(REQUEST);
    }

    private static Map<String, Object> getInputSchema(String delegateInputSchema) {
        Map<String, Object> inputSchema =
            new LinkedHashMap<>(SCHEMA_MAPPER.readValue(delegateInputSchema, SCHEMA_TYPE));

        Map<String, Object> properties = new LinkedHashMap<>();

        if (inputSchema.get("properties") instanceof Map<?, ?> delegateProperties) {
            delegateProperties.forEach((name, property) -> properties.put(String.valueOf(name), property));
        }

        properties.put(WORKSPACE_ID, WORKSPACE_ID_PROPERTY);
        properties.put(ENVIRONMENT, ENVIRONMENT_PROPERTY);

        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);

        return inputSchema;
    }

    private static @Nullable Long toWorkspaceId(Object workspaceIdValue) {
        BigDecimal workspaceIdNumber;

        try {
            workspaceIdNumber = new BigDecimal(workspaceIdValue.toString()
                .strip());
        } catch (NumberFormatException exception) {
            return null;
        }

        try {
            return workspaceIdNumber.longValueExact();
        } catch (ArithmeticException exception) {
            return null;
        }
    }

    private static boolean isBlank(@Nullable Object value) {
        return value == null || value.toString()
            .isBlank();
    }
}
