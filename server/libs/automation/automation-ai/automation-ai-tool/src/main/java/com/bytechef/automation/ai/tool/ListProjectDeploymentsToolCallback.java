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
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists {@link com.bytechef.automation.configuration.domain.ProjectDeployment} rows
 * in the current workspace, optionally filtered by project. Used by the AI Hub agent to enumerate active deployments
 * and to obtain {@code projectDeploymentId} values for {@code rollbackProjectDeployment},
 * {@code toggleProjectDeployment}, {@code updateProjectDeployment}, and {@code deleteProjectDeployment}.
 *
 * <p>
 * The optional {@code projectId} filter narrows the list to one project — useful when the user has many projects and
 * only cares about deployments of one of them. The {@code environmentId} comes from
 * {@link AutomationToolInvocationContext}; the result therefore scopes to the chat surface's currently selected
 * environment.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class ListProjectDeploymentsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listProjectDeployments";

    private static final String DESCRIPTION = """
        List project deployments in the current workspace, scoped to the chat surface's current environment.
        Optionally filter by projectId (numeric, from listProjects) — omit to list deployments across every
        project. Returns a JSON array of {projectDeploymentId, projectId, projectVersion, environment,
        enabled, name, description, lastExecutionDate, tags}. Use this to find an existing deployment's id
        before calling rollbackProjectDeployment, toggleProjectDeployment, updateProjectDeployment, or
        deleteProjectDeployment.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "projectId": {"type": "string", "description": "Optional numeric project id to filter by"}
            }
        }""";

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListProjectDeploymentsToolCallback(ProjectDeploymentFacade projectDeploymentFacade) {
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
            ListProjectDeploymentsInput input = jsonMapper.readValue(toolInput, ListProjectDeploymentsInput.class);

            AutomationToolInvocationContext invocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError("Workspace context unavailable - open this chat from the AI Hub of a workspace.");
            }

            Long environmentId = invocationContext.environmentId();
            Long projectIdFilter = null;

            if (input.projectId() != null && !input.projectId()
                .isBlank()) {
                try {
                    projectIdFilter = Long.parseLong(input.projectId());
                } catch (NumberFormatException exception) {
                    return toolError("Invalid projectId — must be a numeric id");
                }
            }

            List<ProjectDeploymentDTO> deployments = projectDeploymentFacade.getWorkspaceProjectDeployments(
                workspaceId, environmentId, projectIdFilter, null, false);

            List<ProjectDeploymentSummary> summaries = deployments.stream()
                .map(ListProjectDeploymentsToolCallback::toSummary)
                .toList();

            return jsonMapper.writeValueAsString(summaries);
        } catch (JacksonException exception) {
            return toolError("Serialization error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ListProjectDeploymentsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static ProjectDeploymentSummary toSummary(ProjectDeploymentDTO dto) {
        List<String> tagNames = dto.tags() == null
            ? List.of()
            : dto.tags()
                .stream()
                .map(tag -> tag.getName())
                .toList();

        return new ProjectDeploymentSummary(
            dto.id(), dto.projectId(), dto.projectVersion(),
            dto.environment() == null ? null : dto.environment()
                .name(),
            dto.enabled(), dto.name(), dto.description(), dto.lastExecutionDate(), tagNames);
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record ListProjectDeploymentsInput(@Nullable String projectId) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressFBWarnings("EI")
    public record ProjectDeploymentSummary(
        Long id, long projectId, int projectVersion, String environment, boolean enabled, String name,
        String description, Instant lastExecutionDate, List<String> tags) {
    }
}
