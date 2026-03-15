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

package com.bytechef.automation.mcp.facade;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.mcp.domain.McpProject;
import com.bytechef.automation.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.mcp.service.McpProjectService;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        projectDeployment.setName(McpServer.MCP_SERVER_NAME_PREFIX + projectId + "_v" + projectVersion);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setEnabled(true);

        projectDeployment = projectDeploymentService.create(projectDeployment);

        McpProject mcpProject = new McpProject(projectDeployment.getId(), mcpServerId);

        mcpProject = mcpProjectService.create(mcpProject);

        for (String workflowId : selectedWorkflowIds) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
            projectDeploymentWorkflow.setWorkflowId(workflowId);
            projectDeploymentWorkflow.setEnabled(true);
            projectDeploymentWorkflow.setInputs(Map.of());

            projectDeploymentWorkflow = projectDeploymentWorkflowService.create(projectDeploymentWorkflow);

            mcpProjectWorkflowService.create(mcpProject.getId(), projectDeploymentWorkflow.getId());
        }

        return mcpProject;
    }

    @Override
    public void deleteMcpProject(long mcpProjectId) {
        McpProject mcpProject = mcpProjectService.fetchMcpProject(mcpProjectId)
            .orElseThrow(() -> new IllegalArgumentException("McpProject not found: " + mcpProjectId));

        List<McpProjectWorkflow> mcpProjectWorkflows = mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
            mcpProjectId);

        for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflows) {
            mcpProjectWorkflowService.delete(mcpProjectWorkflow.getId());

            projectDeploymentWorkflowService.delete(mcpProjectWorkflow.getProjectDeploymentWorkflowId());
        }

        mcpProjectService.delete(mcpProjectId);

        Long projectDeploymentId = mcpProject.getProjectDeploymentId();

        if (projectDeploymentId != null) {
            projectDeploymentService.delete(projectDeploymentId);
        }
    }

    @Override
    public McpProject updateMcpProject(long mcpProjectId, List<String> selectedWorkflowIds) {
        McpProject mcpProject = mcpProjectService.fetchMcpProject(mcpProjectId)
            .orElseThrow(() -> new IllegalArgumentException("McpProject not found: " + mcpProjectId));

        List<McpProjectWorkflow> existingMcpProjectWorkflows =
            mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProjectId);

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(mcpProject.getProjectDeploymentId());

        Map<Long, ProjectDeploymentWorkflow> projectDeploymentWorkflowMap = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            projectDeploymentWorkflowMap.put(projectDeploymentWorkflow.getId(), projectDeploymentWorkflow);
        }

        Map<String, McpProjectWorkflow> existingWorkflowIdMap = new HashMap<>();

        for (McpProjectWorkflow mcpProjectWorkflow : existingMcpProjectWorkflows) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                projectDeploymentWorkflowMap.get(mcpProjectWorkflow.getProjectDeploymentWorkflowId());

            if (projectDeploymentWorkflow != null) {
                existingWorkflowIdMap.put(projectDeploymentWorkflow.getWorkflowId(), mcpProjectWorkflow);
            }
        }

        Set<String> selectedWorkflowIdSet = new HashSet<>(selectedWorkflowIds);

        for (String workflowId : selectedWorkflowIds) {
            if (!existingWorkflowIdMap.containsKey(workflowId)) {
                ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

                projectDeploymentWorkflow.setProjectDeploymentId(mcpProject.getProjectDeploymentId());
                projectDeploymentWorkflow.setWorkflowId(workflowId);
                projectDeploymentWorkflow.setEnabled(true);
                projectDeploymentWorkflow.setInputs(Map.of());

                projectDeploymentWorkflow = projectDeploymentWorkflowService.create(projectDeploymentWorkflow);

                mcpProjectWorkflowService.create(mcpProjectId, projectDeploymentWorkflow.getId());
            }
        }

        for (Map.Entry<String, McpProjectWorkflow> entry : existingWorkflowIdMap.entrySet()) {
            if (!selectedWorkflowIdSet.contains(entry.getKey())) {
                McpProjectWorkflow mcpProjectWorkflow = entry.getValue();

                mcpProjectWorkflowService.delete(mcpProjectWorkflow.getId());

                projectDeploymentWorkflowService.delete(mcpProjectWorkflow.getProjectDeploymentWorkflowId());
            }
        }

        return mcpProject;
    }
}
