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

import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Removes the MCP rows referencing a workflow, together with the project deployment workflow rows they point at, before
 * that workflow is deleted.
 * <p>
 * An MCP server is backed by a synthetic project deployment which the deployments list deliberately hides, so the
 * project deployment workflow cleanup performed by the workflow delete itself never sees it. Only the project
 * deployment workflows carrying MCP rows are removed here, which keeps this listener from touching rows owned by
 * another feature or by a regular deployment.
 *
 * @author Ivica Cardic
 */
@Component
public class McpWorkflowPreDeleteListener implements WorkflowPreDeleteListener {

    private static final Logger log = LoggerFactory.getLogger(McpWorkflowPreDeleteListener.class);

    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public McpWorkflowPreDeleteListener(
        McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    public void onWorkflowPreDelete(String workflowId) {
        log.debug("Cleaning up MCP data for workflow {}", workflowId);

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getWorkflowProjectDeploymentWorkflows(workflowId);

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            long projectDeploymentWorkflowId = projectDeploymentWorkflow.getId();

            List<McpProjectWorkflow> mcpProjectWorkflows =
                mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(projectDeploymentWorkflowId);

            if (mcpProjectWorkflows.isEmpty()) {
                continue;
            }

            for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflows) {
                mcpProjectWorkflowService.delete(mcpProjectWorkflow.getId());
            }

            projectDeploymentWorkflowService.delete(projectDeploymentWorkflowId);
        }
    }
}
