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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.McpProject;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.McpProjectService;
import com.bytechef.automation.configuration.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.configuration.domain.McpProjectWorkflow;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link McpProjectFacade}.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpProjectFacadeImpl implements McpProjectFacade {

    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public McpProjectFacadeImpl(
        McpProjectService mcpProjectService, McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    public McpProject createMcpProject(
        long mcpServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds) {

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("__MCP_SERVER__" + projectId + "_v" + projectVersion);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setEnabled(false);

        projectDeployment = projectDeploymentService.create(projectDeployment);

        McpProject mcpProject = new McpProject(projectDeployment.getId(), mcpServerId);

        mcpProject = mcpProjectService.create(mcpProject);

        for (String workflowId : selectedWorkflowIds) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
            projectDeploymentWorkflow.setWorkflowId(workflowId);
            projectDeploymentWorkflow.setEnabled(false);
            projectDeploymentWorkflow.setInputs(Map.of());

            projectDeploymentWorkflow = projectDeploymentWorkflowService.create(projectDeploymentWorkflow);

            mcpProjectWorkflowService.create(mcpProject.getId(), projectDeploymentWorkflow.getId());
        }

        return mcpProject;
    }

    @Override
    public void deleteMcpProject(long mcpProjectId) {
        List<McpProjectWorkflow> mcpProjectWorkflows = mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
            mcpProjectId);

        for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflows) {
            mcpProjectWorkflowService.delete(mcpProjectWorkflow.getId());
        }

        mcpProjectService.delete(mcpProjectId);
    }
}
