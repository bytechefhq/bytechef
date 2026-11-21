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

package com.bytechef.automation.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.dto.McpProjectWorkflowSummaryDTO;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Facade for managing MCP Project Workflow operations that involve multiple services.
 *
 * @author Ivica Cardic
 */
public interface McpProjectWorkflowFacade {

    McpProjectWorkflow createMcpProjectWorkflow(long mcpProjectId, long projectDeploymentWorkflowId);

    /**
     * Deletes an MCP project workflow and its associated project deployment workflow.
     *
     * @param mcpProjectWorkflowId the ID of the MCP project workflow to delete
     */
    void deleteMcpProjectWorkflow(long mcpProjectWorkflowId);

    List<McpProjectWorkflow> getMcpProjectMcpProjectWorkflows(long mcpProjectId);

    /**
     * Returns the workflow of the given project, in any of the project versions, so its tool properties can be built
     * before it is added as an MCP tool.
     *
     * @throws IllegalArgumentException if the workflow does not belong to the project
     */
    Workflow getToolEligibleWorkflow(long projectId, String workflowId);

    List<ProjectWorkflow> getToolEligibleProjectVersionWorkflows(long projectId, int projectVersion);

    McpProjectWorkflow updateMcpProjectWorkflowEnabled(long mcpProjectWorkflowId, boolean enabled);

    /**
     * Updates an existing MCP project workflow, rejecting a project deployment workflow that does not belong to the
     * deployment of the target MCP project.
     *
     * @param mcpProjectWorkflowId        the ID of the MCP project workflow to update
     * @param mcpProjectId                the ID of the MCP project (can be null if not updating)
     * @param projectDeploymentWorkflowId the ID of the project deployment workflow (can be null if not updating)
     * @return the updated MCP project workflow
     */
    McpProjectWorkflow updateMcpProjectWorkflow(
        long mcpProjectWorkflowId, @Nullable Long mcpProjectId, @Nullable Long projectDeploymentWorkflowId);

    /**
     * Returns every workflow attached to the MCP server's projects, with the tool mapping each one exposes.
     *
     * @param mcpServerId the ID of the MCP server
     * @return the attached workflows
     */
    List<McpProjectWorkflowSummaryDTO> getMcpServerMcpProjectWorkflows(long mcpServerId);

    /**
     * Merges the given tool name, tool description and parameters into an MCP project workflow's parameters.
     *
     * @param mcpProjectWorkflowId the ID of the MCP project workflow to update
     * @param toolName             the tool name, or {@code null} to keep the current one
     * @param toolDescription      the tool description, or {@code null} to keep the current one
     * @param parameters           the parameters to merge in, or {@code null} for none
     * @return the updated MCP project workflow
     */
    McpProjectWorkflow updateMcpProjectWorkflowParameters(
        long mcpProjectWorkflowId, @Nullable String toolName, @Nullable String toolDescription,
        @Nullable Map<String, Object> parameters);
}
