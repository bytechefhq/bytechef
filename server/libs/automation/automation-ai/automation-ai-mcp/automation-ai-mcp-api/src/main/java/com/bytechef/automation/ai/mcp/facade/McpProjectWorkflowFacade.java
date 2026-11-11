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
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import java.util.List;

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
}
