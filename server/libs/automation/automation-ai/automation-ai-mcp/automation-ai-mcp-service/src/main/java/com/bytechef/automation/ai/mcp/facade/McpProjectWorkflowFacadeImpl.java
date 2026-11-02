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

import com.bytechef.automation.ai.mcp.audit.McpProjectAuditEvents;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link McpProjectWorkflowFacade}.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpProjectWorkflowFacadeImpl implements McpProjectWorkflowFacade {

    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public McpProjectWorkflowFacadeImpl(
        McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    @Audited(event = McpProjectAuditEvents.MCP_PROJECT_WORKFLOW_CREATED, data = {
        @AuditData(key = "mcpProjectWorkflowId", value = "#result?.id"),
        @AuditData(key = "mcpProjectId", value = "#mcpProjectId"),
        @AuditData(key = "projectDeploymentWorkflowId", value = "#projectDeploymentWorkflowId")
    })
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT')")
    public McpProjectWorkflow createMcpProjectWorkflow(long mcpProjectId, long projectDeploymentWorkflowId) {
        return mcpProjectWorkflowService.create(mcpProjectId, projectDeploymentWorkflowId);
    }

    @Override
    @Audited(event = McpProjectAuditEvents.MCP_PROJECT_WORKFLOW_DELETED, data = {
        @AuditData(key = "mcpProjectWorkflowId", value = "#mcpProjectWorkflowId")
    })
    @PreAuthorize("hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_EDIT')")
    public void deleteMcpProjectWorkflow(long mcpProjectWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.fetchMcpProjectWorkflow(mcpProjectWorkflowId)
            .orElseThrow(
                () -> new IllegalArgumentException("McpProjectWorkflow not found: " + mcpProjectWorkflowId));

        mcpProjectWorkflowService.delete(mcpProjectWorkflowId);

        projectDeploymentWorkflowService.delete(mcpProjectWorkflow.getProjectDeploymentWorkflowId());
    }

    @Override
    @Audited(event = McpProjectAuditEvents.MCP_PROJECT_WORKFLOW_ENABLED_UPDATED, data = {
        @AuditData(key = "mcpProjectWorkflowId", value = "#mcpProjectWorkflowId"),
        @AuditData(key = "mcpProjectId", value = "#result?.mcpProjectId"),
        @AuditData(key = "enabled", value = "#enabled")
    })
    @PreAuthorize("hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_EDIT')")
    public McpProjectWorkflow updateMcpProjectWorkflowEnabled(long mcpProjectWorkflowId, boolean enabled) {
        return mcpProjectWorkflowService.updateEnabled(mcpProjectWorkflowId, enabled);
    }
}
