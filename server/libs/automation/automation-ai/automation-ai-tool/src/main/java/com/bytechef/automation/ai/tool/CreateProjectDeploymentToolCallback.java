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
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.platform.configuration.domain.Environment;
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
 * Spring AI {@link ToolCallback} that creates a {@link com.bytechef.automation.configuration.domain.ProjectDeployment}
 * pinning a published project version to a target {@link Environment}. The deployment is created with no per-workflow
 * connection or input overrides — chat-driven flows produce a baseline deployment, and any per-workflow customization
 * (connections, inputs) is set afterwards through the existing UI.
 *
 * <p>
 * The callback delegates to {@link ProjectDeploymentFacade#createProjectDeployment(ProjectDeploymentDTO)} which itself
 * enforces that the project is published and that the supplied {@code projectVersion} is not the current DRAFT —
 * surfacing those errors as tool-error JSON for the LLM to recover from rather than aborting the agent run.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class CreateProjectDeploymentToolCallback implements ToolCallback {

    static final String TOOL_NAME = "createProjectDeployment";

    private static final String SUPPORTED_ENVIRONMENTS = Arrays.stream(Environment.values())
        .map(Environment::name)
        .collect(Collectors.joining(", "));

    private static final String DESCRIPTION = """
        Create a new project deployment that pins a published project version to a target environment.
        Supply projectId (numeric, from listWorkflows or context), projectVersion (a PUBLISHED version
        — not the current DRAFT), and environment (one of: %s). Optionally supply name and description.
        Returns {projectDeploymentId, projectId, projectVersion, environment, enabled} on success or
        {error: <message>} when the project is not published or the version is in DRAFT. The deployment
        is created disabled by default — call toggleProjectDeployment to enable it after configuring
        per-workflow connections in the UI.""".formatted(SUPPORTED_ENVIRONMENTS);

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "projectId": {"type": "string", "description": "Numeric project id"},
                    "projectVersion": {"type": "integer", "description": "A PUBLISHED project version (not the current DRAFT)"},
                    "environment": {"type": "string", "description": "Target environment: DEVELOPMENT, STAGING, or PRODUCTION"},
                    "name": {"type": "string", "description": "Optional human-readable deployment name"},
                    "description": {"type": "string", "description": "Optional description"}
                },
                "required": ["projectId", "projectVersion", "environment"]
            }""";

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateProjectDeploymentToolCallback(ProjectDeploymentFacade projectDeploymentFacade) {
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
            CreateProjectDeploymentInput input = jsonMapper.readValue(toolInput, CreateProjectDeploymentInput.class);

            if (input.projectId() == null || input.projectId()
                .isBlank()) {
                return toolError("projectId is required");
            }

            if (input.projectVersion() == null) {
                return toolError("projectVersion is required");
            }

            if (input.environment() == null || input.environment()
                .isBlank()) {
                return toolError("environment is required (one of: " + SUPPORTED_ENVIRONMENTS + ")");
            }

            long projectId;

            try {
                projectId = Long.parseLong(input.projectId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid projectId — must be a numeric id");
            }

            Environment environment;

            try {
                environment = Environment.valueOf(input.environment()
                    .toUpperCase());
            } catch (IllegalArgumentException exception) {
                return toolError(
                    "Unknown environment '" + input.environment() + "'. Supported: " + SUPPORTED_ENVIRONMENTS);
            }

            ProjectDeploymentDTO dto = ProjectDeploymentDTO.builder()
                .projectId(projectId)
                .projectVersion(input.projectVersion())
                .environment(environment)
                .name(input.name())
                .description(input.description())
                .enabled(false)
                .build();

            long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(dto);

            return jsonMapper.writeValueAsString(
                new CreateProjectDeploymentOutput(
                    projectDeploymentId, projectId, input.projectVersion(), environment.name(), false));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | com.bytechef.exception.ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CreateProjectDeploymentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CreateProjectDeploymentInput(
        String projectId, Integer projectVersion, String environment, @Nullable String name,
        @Nullable String description) {
    }

    public record CreateProjectDeploymentOutput(
        long projectDeploymentId, long projectId, int projectVersion, String environment, boolean enabled) {
    }
}
