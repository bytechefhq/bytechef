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

import com.bytechef.automation.ai.mcp.audit.McpProjectAuditEvent;
import com.bytechef.automation.ai.mcp.audit.McpProjectAuditPublisher;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final McpProjectAuditPublisher mcpProjectAuditPublisher;
    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final McpServerService mcpServerService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public McpProjectFacadeImpl(
        McpProjectAuditPublisher mcpProjectAuditPublisher, McpProjectService mcpProjectService,
        McpProjectWorkflowService mcpProjectWorkflowService, McpServerService mcpServerService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.mcpProjectAuditPublisher = mcpProjectAuditPublisher;
        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.mcpServerService = mcpServerService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpProject createMcpProject(
        long mcpServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds) {

        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName(McpServer.MCP_SERVER_NAME_PREFIX + projectId + "_v" + projectVersion);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(mcpServer.getEnvironment());
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

        Map<String, Object> data = new HashMap<>();

        data.put("projectId", String.valueOf(projectId));

        mcpProjectAuditPublisher.publish(McpProjectAuditEvent.MCP_PROJECT_CREATED, mcpProject.getId(), data);

        return mcpProject;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT')")
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

        mcpProjectAuditPublisher.publish(McpProjectAuditEvent.MCP_PROJECT_DELETED, mcpProjectId);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT')")
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

    @Override
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT')")
    public McpProject cloneMcpProject(long mcpProjectId, long targetMcpServerId) {
        // Resolve the source first so a forged mcpProjectId or stale id surfaces a typed IllegalArgumentException
        // before we try to provision the deployment for the clone — failing fast keeps the rollback surface small.
        McpProject source = mcpProjectService.fetchMcpProject(mcpProjectId)
            .orElseThrow(() -> new IllegalArgumentException("McpProject not found: " + mcpProjectId));

        Long sourceProjectDeploymentId = source.getProjectDeploymentId();

        if (sourceProjectDeploymentId == null) {
            throw new IllegalStateException(
                "Source McpProject " + mcpProjectId + " has no project deployment to clone from");
        }

        ProjectDeployment sourceDeployment = projectDeploymentService.getProjectDeployment(sourceProjectDeploymentId);

        // Walk the McpProjectWorkflow → ProjectDeploymentWorkflow chain to recover the exposed workflowIds. We can't
        // just reuse McpProjectWorkflow rows because each is bound to a specific projectDeploymentId; the clone needs
        // its own deployment with its own deployment-workflow rows pointing at the same logical workflowIds.
        List<McpProjectWorkflow> sourceMcpProjectWorkflows =
            mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProjectId);

        List<ProjectDeploymentWorkflow> sourceDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(sourceProjectDeploymentId);

        Map<Long, ProjectDeploymentWorkflow> deploymentWorkflowById = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : sourceDeploymentWorkflows) {
            deploymentWorkflowById.put(projectDeploymentWorkflow.getId(), projectDeploymentWorkflow);
        }

        List<String> selectedWorkflowIds = new ArrayList<>(sourceMcpProjectWorkflows.size());

        for (McpProjectWorkflow mcpProjectWorkflow : sourceMcpProjectWorkflows) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                deploymentWorkflowById.get(mcpProjectWorkflow.getProjectDeploymentWorkflowId());

            // A null match would mean the source's McpProjectWorkflow points at a deployment-workflow that doesn't
            // belong to the source's deployment — corrupt data. Skip rather than fail the whole clone; better to
            // produce a slightly thinner clone than to leave the user without a recovery path. The user can run
            // updateMcpProject afterwards to reconcile.
            if (projectDeploymentWorkflow != null) {
                selectedWorkflowIds.add(projectDeploymentWorkflow.getWorkflowId());
            }
        }

        return createMcpProject(
            targetMcpServerId, sourceDeployment.getProjectId(), sourceDeployment.getProjectVersion(),
            selectedWorkflowIds);
    }
}
