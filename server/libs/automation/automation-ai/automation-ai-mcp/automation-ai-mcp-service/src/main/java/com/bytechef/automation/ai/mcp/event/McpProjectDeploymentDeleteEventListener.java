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

package com.bytechef.automation.ai.mcp.event;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.listener.ProjectDeploymentDeleteEventListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Removes the MCP rows referencing a project deployment before that deployment is deleted, so the deployment of an MCP
 * server can be deleted together with the project backing it.
 *
 * @author Ivica Cardic
 */
@Component
public class McpProjectDeploymentDeleteEventListener implements ProjectDeploymentDeleteEventListener {

    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;

    @SuppressFBWarnings("EI")
    public McpProjectDeploymentDeleteEventListener(
        McpProjectService mcpProjectService, McpProjectWorkflowService mcpProjectWorkflowService) {

        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
    }

    @Override
    public void onBeforeDeleteProjectDeployment(long projectDeploymentId) {
        List<McpProject> mcpProjects = mcpProjectService.getProjectDeploymentMcpProjects(projectDeploymentId);

        for (McpProject mcpProject : mcpProjects) {
            List<McpProjectWorkflow> mcpProjectWorkflows = mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
                mcpProject.getId());

            for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflows) {
                mcpProjectWorkflowService.delete(mcpProjectWorkflow.getId());
            }

            mcpProjectService.delete(mcpProject.getId());
        }
    }
}
