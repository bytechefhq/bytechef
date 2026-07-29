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
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that attaches a project version's selected workflows to an existing MCP server,
 * exposing each as an MCP tool. Internally creates an {@link McpProject} (which itself stands up a
 * {@link com.bytechef.automation.configuration.domain.ProjectDeployment}) plus one
 * {@link com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow} per supplied workflow id.
 *
 * <p>
 * The MCP server itself must already exist — this callback does not stand up a new server. Use the existing
 * {@code mcpServer} GraphQL mutation flow when no suitable server exists.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class CreateMcpProjectToolCallback implements ToolCallback {

    static final String TOOL_NAME = "createMcpProject";

    private static final String DESCRIPTION = """
        Attach a project version's workflows to an existing MCP server, exposing each workflow as an MCP
        tool that LLM clients can invoke. Supply mcpServerId (numeric, from the MCP servers view),
        projectId, projectVersion (PUBLISHED), and workflowIds — the list of workflow ids in that version
        to expose as tools (use listWorkflows to enumerate them first). Returns {mcpProjectId,
        projectDeploymentId, mcpServerId, exposedWorkflowCount} on success. The MCP server itself must
        already exist; this tool does not stand up a new server.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "mcpServerId": {"type": "string", "description": "Numeric MCP server id"},
                "projectId": {"type": "string", "description": "Numeric project id"},
                "projectVersion": {"type": "integer", "description": "PUBLISHED project version"},
                "workflowIds": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Workflow ids to expose as MCP tools"
                }
            },
            "required": ["mcpServerId", "projectId", "projectVersion", "workflowIds"]
        }""";

    private final McpProjectFacade mcpProjectFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateMcpProjectToolCallback(McpProjectFacade mcpProjectFacade) {
        this.mcpProjectFacade = mcpProjectFacade;
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
            CreateMcpProjectInput input = jsonMapper.readValue(toolInput, CreateMcpProjectInput.class);

            if (input.mcpServerId() == null || input.mcpServerId()
                .isBlank()) {
                return toolError("mcpServerId is required");
            }

            if (input.projectId() == null || input.projectId()
                .isBlank()) {
                return toolError("projectId is required");
            }

            if (input.projectVersion() == null) {
                return toolError("projectVersion is required");
            }

            if (input.workflowIds() == null || input.workflowIds()
                .isEmpty()) {
                return toolError("workflowIds is required and must contain at least one workflow id");
            }

            long mcpServerId;

            try {
                mcpServerId = Long.parseLong(input.mcpServerId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid mcpServerId — must be a numeric id");
            }

            long projectId;

            try {
                projectId = Long.parseLong(input.projectId());
            } catch (NumberFormatException exception) {
                return toolError("Invalid projectId — must be a numeric id");
            }

            McpProject created = mcpProjectFacade.createMcpProject(
                mcpServerId, projectId, input.projectVersion(), input.workflowIds());

            return jsonMapper.writeValueAsString(
                new CreateMcpProjectOutput(
                    created.getId(), created.getProjectDeploymentId(), created.getMcpServerId(),
                    input.workflowIds()
                        .size()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | com.bytechef.exception.ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CreateMcpProjectToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CreateMcpProjectInput(
        String mcpServerId, String projectId, Integer projectVersion, List<String> workflowIds) {

        public CreateMcpProjectInput {
            workflowIds = workflowIds == null ? null : List.copyOf(workflowIds);
        }
    }

    public record CreateMcpProjectOutput(
        long mcpProjectId, long projectDeploymentId, long mcpServerId, int exposedWorkflowCount) {
    }
}
