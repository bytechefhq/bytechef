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
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
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

            String projectDeploymentIdString = input.projectDeploymentId();

            if (projectDeploymentIdString == null || projectDeploymentIdString.isBlank()) {
                return toolError("projectDeploymentId is required");
            }

            String name = input.name();

            boolean nameSupplied = name != null && !name.isBlank();
            boolean descriptionSupplied = input.description() != null;

            if (!nameSupplied && !descriptionSupplied) {
                return toolError("Supply at least one of name or description to update");
            }

            long projectDeploymentId;

            try {
                projectDeploymentId = Long.parseLong(projectDeploymentIdString);
            } catch (NumberFormatException exception) {
                return toolError("Invalid projectDeploymentId — must be a numeric id");
            }

            String workspaceError = ProjectDeploymentWorkspaceGuard.findWorkspaceError(
                projectDeploymentFacade, toolContext, projectDeploymentId);

            if (workspaceError != null) {
                return toolError(workspaceError);
            }

            ProjectDeploymentDTO existing;

            try {
                existing = projectDeploymentFacade.getProjectDeployment(projectDeploymentId);
            } catch (NoSuchElementException exception) {
                return toolError("Project deployment " + projectDeploymentId + " not found");
            }

            ProjectDeploymentDTO updated = ProjectDeploymentDTO.builder()
                .createdBy(existing.createdBy())
                .createdDate(existing.createdDate())
                .description(descriptionSupplied ? input.description() : existing.description())
                .enabled(existing.enabled())
                .environment(existing.environment())
                .id(existing.id())
                .name(nameSupplied ? name : existing.name())
                .lastModifiedBy(existing.lastModifiedBy())
                .lastModifiedDate(existing.lastModifiedDate())
                .project(existing.project())
                .projectId(existing.projectId())
                .projectVersion(existing.projectVersion())
                .projectDeploymentWorkflows(existing.projectDeploymentWorkflows())
                .tags(existing.tags())
                .version(existing.version())
                .build();

            try {
                projectDeploymentFacade.updateProjectDeployment(updated);
            } catch (NoSuchElementException exception) {
                return toolError(
                    "Project deployment " + projectDeploymentId
                        + " could not be updated — one of its workflows no longer exists");
            }

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
