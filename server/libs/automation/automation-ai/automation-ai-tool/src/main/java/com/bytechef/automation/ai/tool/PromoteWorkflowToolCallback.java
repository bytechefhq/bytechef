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
 * Spring AI {@link ToolCallback} that promotes a workflow to a target environment by creating a fresh
 * {@link com.bytechef.automation.configuration.domain.ProjectDeployment} pinning the supplied published project version
 * to the target environment, and optionally enabling it in one step.
 *
 * <p>
 * The tool name is intentionally <b>promoteWorkflow</b> rather than {@code promoteWorkflowDeployment} for three
 * reasons:
 * </p>
 * <ol>
 * <li><b>User phrasing</b> — users say "promote my workflow to production," not "promote my workflow deployment." The
 * LLM matches against natural language; the tool name should mirror it.</li>
 * <li><b>Hides implementation detail</b> — ByteChef's data model separates the project version (definition) from the
 * project deployment (env-bound binding). The user doesn't need to know that distinction; the tool description carries
 * it.</li>
 * <li><b>Lives in the workflow-noun family</b> — alongside {@code createWorkflow}, {@code listWorkflows},
 * {@code runChatWorkflow}. {@code createProjectDeployment} stays as the lower-level infrastructure tool for cases where
 * the LLM has explicit deployment intent.</li>
 * </ol>
 *
 * <p>
 * Sibling to {@link CreateProjectDeploymentToolCallback}: this one wraps the same facade method but adds an optional
 * {@code enable} flag that toggles the deployment on after creation, so the LLM can do "promote and enable" in one tool
 * call rather than chaining two.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
public class PromoteWorkflowToolCallback implements ToolCallback {

    static final String TOOL_NAME = "promoteWorkflow";

    private static final String SUPPORTED_ENVIRONMENTS = Arrays.stream(Environment.values())
        .map(Environment::name)
        .collect(Collectors.joining(", "));

    private static final String DESCRIPTION = """
        Promote a workflow to another environment. Internally creates a new project deployment in the target
        environment using the supplied published project version, and optionally enables it. Use this when the
        user says "promote my customer-support workflow to production" or "deploy v3 of my workflow to staging."
        Pass projectId (numeric, from listWorkflows), projectVersion (a PUBLISHED version — not the current
        DRAFT), targetEnvironment (one of: %s), and optionally enable (default false) to toggle the deployment
        on in one step. Returns {projectDeploymentId, projectId, projectVersion, environment, enabled}.""".formatted(
        SUPPORTED_ENVIRONMENTS);

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "projectId": {"type": "string", "description": "Numeric project id"},
                    "projectVersion": {"type": "integer", "description": "A PUBLISHED project version (not the current DRAFT)"},
                    "targetEnvironment": {"type": "string", "description": "Target environment: DEVELOPMENT, STAGING, or PRODUCTION"},
                    "enable": {"type": "boolean", "description": "Optional. When true, the deployment is toggled on after creation. Default: false."},
                    "name": {"type": "string", "description": "Optional human-readable deployment name"},
                    "description": {"type": "string", "description": "Optional description"}
                },
                "required": ["projectId", "projectVersion", "targetEnvironment"]
            }""";

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PromoteWorkflowToolCallback(ProjectDeploymentFacade projectDeploymentFacade) {
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
            PromoteWorkflowInput input = jsonMapper.readValue(toolInput, PromoteWorkflowInput.class);

            if (input.projectId() == null || input.projectId()
                .isBlank()) {
                return toolError("projectId is required");
            }

            if (input.projectVersion() == null) {
                return toolError("projectVersion is required");
            }

            if (input.targetEnvironment() == null || input.targetEnvironment()
                .isBlank()) {
                return toolError("targetEnvironment is required (one of: " + SUPPORTED_ENVIRONMENTS + ")");
            }

            long projectId;

            try {
                projectId = Long.parseLong(input.projectId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid projectId — must be a numeric id");
            }

            Environment targetEnvironment;

            try {
                targetEnvironment = Environment.valueOf(input.targetEnvironment()
                    .toUpperCase());
            } catch (IllegalArgumentException exception) {
                return toolError(
                    "Unknown targetEnvironment '" + input.targetEnvironment() + "'. Supported: "
                        + SUPPORTED_ENVIRONMENTS);
            }

            ProjectDeploymentDTO dto = ProjectDeploymentDTO.builder()
                .projectId(projectId)
                .projectVersion(input.projectVersion())
                .environment(targetEnvironment)
                .name(input.name())
                .description(input.description())
                .enabled(false)
                .build();

            long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(dto);

            // Optional enable-after-create. Same idempotency contract as toggleProjectDeployment — calling
            // enableProjectDeployment(true) on a deployment already enabled is a no-op. We still pass the user's
            // intent through verbatim rather than computing a delta because the facade's check is cheap and the
            // tool's purpose is to make "promote AND enable" one round-trip from the LLM's perspective.
            boolean enabled = Boolean.TRUE.equals(input.enable());

            if (enabled) {
                projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);
            }

            return jsonMapper.writeValueAsString(
                new PromoteWorkflowOutput(
                    projectDeploymentId, projectId, input.projectVersion(), targetEnvironment.name(), enabled));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | com.bytechef.exception.ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, PromoteWorkflowToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record PromoteWorkflowInput(
        String projectId, Integer projectVersion, String targetEnvironment, @Nullable Boolean enable,
        @Nullable String name, @Nullable String description) {
    }

    public record PromoteWorkflowOutput(
        long projectDeploymentId, long projectId, int projectVersion, String environment, boolean enabled) {
    }
}
