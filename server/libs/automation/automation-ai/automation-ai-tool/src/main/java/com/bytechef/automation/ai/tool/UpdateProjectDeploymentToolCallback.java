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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that updates the metadata (name, description) of an existing
 * {@link com.bytechef.automation.configuration.domain.ProjectDeployment}. Complement to the focused mutators we already
 * have — {@code toggleProjectDeployment} flips enabled, {@code rollbackProjectDeployment} swaps the active project
 * version. This one covers the remaining "rename / re-describe a deployment" intent.
 *
 * <p>
 * Per-workflow connection / input overrides are NOT mutated through this tool — those flow through the workflow-builder
 * UI because they require component + connection introspection the chat surface does not always have. Trying to wire
 * them in here would create a confusing tool with a much larger schema than the rest of the deployment lifecycle.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class UpdateProjectDeploymentToolCallback implements ToolCallback {

    static final String TOOL_NAME = "updateProjectDeployment";

    private static final String DESCRIPTION = """
        Update the metadata (name and/or description) of an existing project deployment. Supply
        projectDeploymentId (numeric, from listProjectDeployments). At least one of name or description must
        be supplied — fields you omit are left unchanged. Returns {projectDeploymentId, name, description}
        reflecting the post-update values. Does NOT change enabled, environment, or projectVersion — use
        toggleProjectDeployment / rollbackProjectDeployment for those.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "projectDeploymentId": {"type": "string", "description": "Numeric project deployment id from listProjectDeployments"},
                    "name": {"type": "string", "description": "New deployment name (optional; leave omitted to keep)"},
                    "description": {"type": "string", "description": "New description (optional; leave omitted to keep)"}
                },
                "required": ["projectDeploymentId"]
            }""";

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UpdateProjectDeploymentToolCallback(ProjectDeploymentFacade projectDeploymentFacade) {
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
            UpdateProjectDeploymentInput input = jsonMapper.readValue(
                toolInput, UpdateProjectDeploymentInput.class);

            if (input.projectDeploymentId() == null || input.projectDeploymentId()
                .isBlank()) {
                return toolError("projectDeploymentId is required");
            }

            boolean nameSupplied = input.name() != null && !input.name()
                .isBlank();
            boolean descriptionSupplied = input.description() != null;

            if (!nameSupplied && !descriptionSupplied) {
                return toolError("Supply at least one of name or description to update");
            }

            long projectDeploymentId;

            try {
                projectDeploymentId = Long.parseLong(input.projectDeploymentId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid projectDeploymentId — must be a numeric id");
            }

            ProjectDeploymentDTO existing = projectDeploymentFacade.getProjectDeployment(projectDeploymentId);

            ProjectDeploymentDTO updated = ProjectDeploymentDTO.builder()
                .createdBy(existing.createdBy())
                .createdDate(existing.createdDate())
                .description(descriptionSupplied ? input.description() : existing.description())
                .enabled(existing.enabled())
                .environment(existing.environment())
                .id(existing.id())
                .name(nameSupplied ? input.name() : existing.name())
                .lastModifiedBy(existing.lastModifiedBy())
                .lastModifiedDate(existing.lastModifiedDate())
                .project(existing.project())
                .projectId(existing.projectId())
                .projectVersion(existing.projectVersion())
                .projectDeploymentWorkflows(existing.projectDeploymentWorkflows())
                .tags(existing.tags())
                .version(existing.version())
                .build();

            projectDeploymentFacade.updateProjectDeployment(updated);

            return jsonMapper.writeValueAsString(
                new UpdateProjectDeploymentOutput(projectDeploymentId, updated.name(), updated.description()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | com.bytechef.exception.ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, UpdateProjectDeploymentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record UpdateProjectDeploymentInput(
        String projectDeploymentId, @Nullable String name, @Nullable String description) {
    }

    public record UpdateProjectDeploymentOutput(
        long projectDeploymentId, String name, @Nullable String description) {
    }
}
