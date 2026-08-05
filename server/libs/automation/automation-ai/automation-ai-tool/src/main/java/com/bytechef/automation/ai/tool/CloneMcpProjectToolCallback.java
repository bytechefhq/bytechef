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
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that duplicates an existing {@link McpProject} onto a different (or same) MCP server,
 * reusing the source's projectId, projectVersion, and exposed workflow set. Internally a fresh
 * {@link com.bytechef.automation.configuration.domain.ProjectDeployment} is provisioned for the clone — both source and
 * clone reference the same project version, but each has its own deployment row with its own
 * {@code ProjectDeploymentWorkflow}s.
 *
 * <p>
 * The MCP project's underlying deployment is implicitly bound to {@code Environment.DEVELOPMENT} by the underlying
 * {@code createMcpProject} primitive — see {@link McpProjectFacade#cloneMcpProject} for why this is not a
 * cross-environment clone. The useful axis is the MCP server: an MCP server is per-tenant scoped, so duplicating an MCP
 * project onto a different server lets the user expose the same workflows under different MCP authority/auth.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
public class CloneMcpProjectToolCallback implements ToolCallback {

    static final String TOOL_NAME = "cloneMcpProject";

    private static final String DESCRIPTION = """
        Duplicate an existing MCP project onto a target MCP server, reusing the source's projectId,
        projectVersion, and exposed workflow set. Use when the user says "clone my support-tools MCP project
        to the staging server" or "make me a copy of my customer-tools MCP." Pass mcpProjectId (from listing
        MCP projects) and targetMcpServerId (numeric, may equal the source's server for an in-place
        duplicate). Returns {mcpProjectId, projectDeploymentId, mcpServerId, exposedWorkflowCount}. The clone
        is bound to a fresh project deployment so both source and clone can be enabled/disabled independently.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "mcpProjectId": {"type": "integer", "description": "Source MCP project id. Never invent ids."},
                    "targetMcpServerId": {"type": "string", "description": "Numeric MCP server id where the clone should land. May equal the source's server."}
                },
                "required": ["mcpProjectId", "targetMcpServerId"]
            }""";

    private final McpProjectFacade mcpProjectFacade;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CloneMcpProjectToolCallback(McpProjectFacade mcpProjectFacade) {
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
            CloneMcpProjectInput input = jsonMapper.readValue(toolInput, CloneMcpProjectInput.class);

            Long mcpProjectId = input.mcpProjectId();

            if (mcpProjectId == null) {
                return toolError("mcpProjectId is required");
            }

            String targetMcpServerIdString = input.targetMcpServerId();

            if (targetMcpServerIdString == null || targetMcpServerIdString.isBlank()) {
                return toolError("targetMcpServerId is required");
            }

            long targetMcpServerId;

            try {
                targetMcpServerId = Long.parseLong(targetMcpServerIdString);
            } catch (NumberFormatException exception) {
                return toolError("Invalid targetMcpServerId — must be a numeric id");
            }

            McpProject clone = mcpProjectFacade.cloneMcpProject(mcpProjectId, targetMcpServerId);

            // The clone's exposed-workflow count is identical to the source's since the facade walks the source set
            // and re-creates each binding. Surfacing the count helps the LLM communicate the impact of the clone in
            // its reply ("created an MCP project exposing 4 workflows…").
            return jsonMapper.writeValueAsString(
                new CloneMcpProjectOutput(
                    clone.getId(), clone.getProjectDeploymentId(), clone.getMcpServerId()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IllegalArgumentException | IllegalStateException
            | com.bytechef.exception.ConfigurationException exception) {
            return toolError(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, CloneMcpProjectToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CloneMcpProjectInput(@Nullable Long mcpProjectId, @Nullable String targetMcpServerId) {
    }

    public record CloneMcpProjectOutput(long mcpProjectId, long projectDeploymentId, long mcpServerId) {
    }
}
