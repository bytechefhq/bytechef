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
public class RollbackProjectDeploymentToolCallback implements ToolCallback {

    static final String TOOL_NAME = "rollbackProjectDeployment";

    private static final String DESCRIPTION = """
        Re-point an existing project deployment at a different (typically older) PUBLISHED project version.
        Supply projectDeploymentId (numeric, from listWorkflows or the project deployments view) and the
        target projectVersion. Returns {projectDeploymentId, projectVersion, previousProjectVersion} on
        success. Use this to roll a deployment back to a known-good version after a regression, or to roll
        forward to a newer published version. The deployment's enabled state, environment, and per-workflow
        connections are preserved across the version change.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "projectDeploymentId": {"type": "string", "description": "Numeric project deployment id"},
                "projectVersion": {"type": "integer", "description": "Target PUBLISHED project version"}
            },
            "required": ["projectDeploymentId", "projectVersion"]
        }""";

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RollbackProjectDeploymentToolCallback(ProjectDeploymentFacade projectDeploymentFacade) {
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
            RollbackProjectDeploymentInput input = jsonMapper.readValue(
                toolInput, RollbackProjectDeploymentInput.class);

            if (input.projectDeploymentId() == null || input.projectDeploymentId()
                .isBlank()) {
                return toolError("projectDeploymentId is required");
            }

            if (input.projectVersion() == null) {
                return toolError("projectVersion is required");
            }

            long projectDeploymentId;

            try {
                projectDeploymentId = Long.parseLong(input.projectDeploymentId());
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

            int previousProjectVersion = existing.projectVersion();

            if (previousProjectVersion == input.projectVersion()) {
                return toolError(
                    "Deployment is already on projectVersion=" + previousProjectVersion + " — nothing to roll back");
            }

            ProjectDeploymentDTO updated = ProjectDeploymentDTO.builder()
                .createdBy(existing.createdBy())
                .createdDate(existing.createdDate())
                .description(existing.description())
                .enabled(existing.enabled())
                .environment(existing.environment())
                .id(existing.id())
                .name(existing.name())
                .lastModifiedBy(existing.lastModifiedBy())
                .lastModifiedDate(existing.lastModifiedDate())
                .project(existing.project())
                .projectId(existing.projectId())
                .projectVersion(input.projectVersion())
                .projectDeploymentWorkflows(existing.projectDeploymentWorkflows())
                .tags(existing.tags())
                .version(existing.version())
                .build();

            try {
                projectDeploymentFacade.updateProjectDeployment(updated);
            } catch (NoSuchElementException exception) {
                return toolError(
                    "Project deployment " + projectDeploymentId
                        + " could not be rolled back — one of its workflows no longer exists");
            }

            return jsonMapper.writeValueAsString(
                new RollbackProjectDeploymentOutput(
                    projectDeploymentId, input.projectVersion(), previousProjectVersion));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | com.bytechef.exception.ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, RollbackProjectDeploymentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record RollbackProjectDeploymentInput(String projectDeploymentId, Integer projectVersion) {
    }

    public record RollbackProjectDeploymentOutput(
        long projectDeploymentId, int projectVersion, int previousProjectVersion) {
    }
}
