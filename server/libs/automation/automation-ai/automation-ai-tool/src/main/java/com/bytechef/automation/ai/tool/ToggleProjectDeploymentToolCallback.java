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
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that flips the {@code enabled} flag on an existing
 * {@link com.bytechef.automation.configuration.domain.ProjectDeployment}. Enabling cascades to enabling each contained
 * {@link com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow}'s static-webhook triggers; disabling
 * tears those triggers down. Both transitions are idempotent at the chat layer — calling enable on an already-enabled
 * deployment is a no-op tool-error rather than a server-side mutation.
 *
 *
 * @author Ivica Cardic
 */
public class ToggleProjectDeploymentToolCallback implements ToolCallback {

    static final String TOOL_NAME = "toggleProjectDeployment";

    private static final String DESCRIPTION = """
        Enable or disable an existing project deployment. Enabling cascades to enabling each contained
        workflow's static-webhook triggers; disabling tears those triggers down. Supply projectDeploymentId
        and enabled (boolean). Returns {projectDeploymentId, enabled} on success.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "projectDeploymentId": {"type": "string", "description": "Numeric project deployment id"},
                "enabled": {"type": "boolean", "description": "Target enabled state"}
            },
            "required": ["projectDeploymentId", "enabled"]
        }""";

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ToggleProjectDeploymentToolCallback(ProjectDeploymentFacade projectDeploymentFacade) {
        this.projectDeploymentFacade = projectDeploymentFacade;
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
            ToggleProjectDeploymentInput input = jsonMapper.readValue(toolInput, ToggleProjectDeploymentInput.class);

            if (input.projectDeploymentId() == null || input.projectDeploymentId()
                .isBlank()) {
                return toolError("projectDeploymentId is required");
            }

            if (input.enabled() == null) {
                return toolError("enabled is required");
            }

            long projectDeploymentId;

            try {
                projectDeploymentId = Long.parseLong(input.projectDeploymentId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid projectDeploymentId — must be a numeric id");
            }

            projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, input.enabled());

            return jsonMapper.writeValueAsString(
                new ToggleProjectDeploymentOutput(projectDeploymentId, input.enabled()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | com.bytechef.exception.ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ToggleProjectDeploymentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record ToggleProjectDeploymentInput(String projectDeploymentId, Boolean enabled) {
    }

    public record ToggleProjectDeploymentOutput(long projectDeploymentId, boolean enabled) {
    }
}
