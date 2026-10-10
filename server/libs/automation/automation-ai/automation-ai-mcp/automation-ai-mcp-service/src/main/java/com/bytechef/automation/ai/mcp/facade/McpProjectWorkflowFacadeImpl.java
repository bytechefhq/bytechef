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
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.dto.McpProjectWorkflowSummaryDTO;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
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

    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public McpProjectWorkflowFacadeImpl(
        McpProjectService mcpProjectService, McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, WorkflowService workflowService) {

        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_EDIT')")
    public void deleteMcpProjectWorkflow(long mcpProjectWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.fetchMcpProjectWorkflow(mcpProjectWorkflowId)
            .orElseThrow(
                () -> new IllegalArgumentException("McpProjectWorkflow not found: " + mcpProjectWorkflowId));

        mcpProjectWorkflowService.delete(mcpProjectWorkflowId);

        projectDeploymentWorkflowService.delete(mcpProjectWorkflow.getProjectDeploymentWorkflowId());
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_VIEW')")
    @Transactional(readOnly = true)
    public List<McpProjectWorkflowSummaryDTO> getMcpServerMcpProjectWorkflows(long mcpServerId) {
        List<McpProjectWorkflowSummaryDTO> mcpProjectWorkflowSummaryDTOs = new ArrayList<>();

        for (McpProject mcpProject : mcpProjectService.getMcpServerMcpProjects(mcpServerId)) {
            for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
                mcpProject.getId())) {

                mcpProjectWorkflowSummaryDTOs.add(toMcpProjectWorkflowSummaryDTO(mcpProject, mcpProjectWorkflow));
            }
        }

        return mcpProjectWorkflowSummaryDTOs;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_EDIT')")
    public McpProjectWorkflow updateMcpProjectWorkflowParameters(
        long mcpProjectWorkflowId, @Nullable String toolName, @Nullable String toolDescription,
        @Nullable Map<String, Object> parameters) {

        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.fetchMcpProjectWorkflow(mcpProjectWorkflowId)
            .orElseThrow(
                () -> new IllegalArgumentException("McpProjectWorkflow not found: " + mcpProjectWorkflowId));

        Map<String, Object> mergedParameters = new HashMap<>(mcpProjectWorkflow.getParameters());

        if (toolName != null) {
            mergedParameters.put(ToolConstants.TOOL_NAME, toolName);
        }

        if (toolDescription != null) {
            mergedParameters.put(ToolConstants.TOOL_DESCRIPTION, toolDescription);
        }

        if (parameters != null) {
            mergedParameters.putAll(parameters);
        }

        return mcpProjectWorkflowService.updateParameters(mcpProjectWorkflowId, mergedParameters);
    }

    private McpProjectWorkflowSummaryDTO toMcpProjectWorkflowSummaryDTO(
        McpProject mcpProject, McpProjectWorkflow mcpProjectWorkflow) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(mcpProjectWorkflow.getProjectDeploymentWorkflowId());

        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        WorkflowTrigger callableTrigger = getMcpToolCallableTrigger(workflow);

        String inputSchema = callableTrigger == null
            ? null
            : MapUtils.getString(callableTrigger.getParameters(), WorkflowConstants.INPUT_SCHEMA);

        Map<String, ?> parameters = mcpProjectWorkflow.getParameters();

        return new McpProjectWorkflowSummaryDTO(
            mcpProject.getId(), mcpProjectWorkflow.getId(), workflow.getId(), workflow.getLabel(),
            callableTrigger != null, MapUtils.getString(parameters, ToolConstants.TOOL_NAME),
            MapUtils.getString(parameters, ToolConstants.TOOL_DESCRIPTION), parameters, inputSchema);
    }

    private static @Nullable WorkflowTrigger getMcpToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}
