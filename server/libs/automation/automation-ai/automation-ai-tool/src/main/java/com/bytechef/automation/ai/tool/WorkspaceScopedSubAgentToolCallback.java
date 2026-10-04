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
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
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
    private final WorkspaceScopeResolver workspaceScopeResolver;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceScopedSubAgentToolCallback(ToolCallback delegate, WorkspaceScopeResolver workspaceScopeResolver) {
        this.delegate = delegate;
        this.workspaceScopeResolver = workspaceScopeResolver;
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

            WorkspaceScopeResolver.WorkspaceScope workspaceScope =
                workspaceScopeResolver.resolve(input.workspaceId(), input.environment());

            if (workspaceScope instanceof WorkspaceScopeResolver.Rejected rejected) {
                return rejected.response();
            }

            WorkspaceScopeResolver.Resolved resolved = (WorkspaceScopeResolver.Resolved) workspaceScope;

            Map<String, Object> forwardedContext = resolved.toForwardedContext(toolContext);

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
