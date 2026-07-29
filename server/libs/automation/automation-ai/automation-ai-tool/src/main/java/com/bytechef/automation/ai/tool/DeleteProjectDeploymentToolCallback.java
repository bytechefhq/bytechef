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
 * Spring AI {@link ToolCallback} that deletes a {@link com.bytechef.automation.configuration.domain.ProjectDeployment}
 * along with its per-workflow deployment rows and connection bindings.
 *
 * <p>
 * The parent project, project versions, and workflow definitions are NOT touched — only the deployment binding is
 * removed. To get rid of an entire project use {@code deleteProject}. The deletion path goes through
 * {@link ProjectDeploymentFacade#deleteProjectDeployment(long)} which cascades to {@code project_deployment_workflow}
 * and {@code project_deployment_workflow_connection} rows.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class DeleteProjectDeploymentToolCallback implements ToolCallback {

    static final String TOOL_NAME = "deleteProjectDeployment";

    private static final String DESCRIPTION = """
        Delete a project deployment along with its per-workflow deployment rows and connection bindings.
        Supply projectDeploymentId (numeric, from listProjectDeployments). The parent project, project
        versions, and workflow definitions are NOT touched — only the deployment binding is removed. This is
        IRREVERSIBLE; confirm destructive intent with the user in chat before calling. Returns
        {projectDeploymentId, deleted: true} on success or {error: <message>} when the id is unknown or the
        facade rejects the call.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "projectDeploymentId": {"type": "string", "description": "Numeric project deployment id from listProjectDeployments"}
                },
                "required": ["projectDeploymentId"]
            }""";

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DeleteProjectDeploymentToolCallback(ProjectDeploymentFacade projectDeploymentFacade) {
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
            DeleteProjectDeploymentInput input = jsonMapper.readValue(
                toolInput, DeleteProjectDeploymentInput.class);

            if (input.projectDeploymentId() == null || input.projectDeploymentId()
                .isBlank()) {
                return toolError("projectDeploymentId is required");
            }

            long projectDeploymentId;

            try {
                projectDeploymentId = Long.parseLong(input.projectDeploymentId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid projectDeploymentId — must be a numeric id");
            }

            projectDeploymentFacade.deleteProjectDeployment(projectDeploymentId);

            return jsonMapper.writeValueAsString(
                new DeleteProjectDeploymentOutput(projectDeploymentId, true));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | com.bytechef.exception.ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, DeleteProjectDeploymentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record DeleteProjectDeploymentInput(String projectDeploymentId) {
    }

    public record DeleteProjectDeploymentOutput(long projectDeploymentId, boolean deleted) {
    }
}
