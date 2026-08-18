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

package com.bytechef.automation.ai.mcp.validation;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.exception.McpProjectWorkflowErrorType;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.mcp.service.McpServerEnablementValidator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Enforces, in the facade/service tier, the "enable only when mapped" invariant that used to live solely in
 * {@code prompt_mcp_agent.txt}: an MCP server may only be enabled once every workflow it exposes (attached through its
 * MCP projects, with a callable {@code workflow/newWorkflowCall} trigger, riding an enabled
 * project-deployment-workflow) has a complete tool mapping — a non-null {@code toolName}, and a value for every input
 * its trigger's {@code inputSchema} marks required.
 *
 * <p>
 * This mirrors what the serve path ({@code AutomationMcpToolFacade}) actually needs to work correctly: a missing
 * {@code toolName} is the serve path's own skip condition (the workflow silently drops out of the tool list), and a
 * required input with neither a {@code fromAi(...)} expression nor a literal in {@code McpProjectWorkflow.parameters}
 * is never included in the schema shown to the calling model nor forwarded to the workflow — the run would fail at call
 * time. A workflow without a callable trigger, or riding a disabled project-deployment-workflow, is excluded from the
 * check exactly as it is excluded from the served tool list.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
public class McpProjectWorkflowMappingValidator implements McpServerEnablementValidator {

    private static final String REQUIRED = "required";

    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public McpProjectWorkflowMappingValidator(
        McpProjectService mcpProjectService, McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, WorkflowService workflowService) {

        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public void validateEnablement(long mcpServerId) {
        List<String> incompleteWorkflowLabels = new ArrayList<>();

        for (McpProject mcpProject : mcpProjectService.getMcpServerMcpProjects(mcpServerId)) {
            for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
                mcpProject.getId())) {

                String incompleteWorkflowLabel = checkMapping(mcpProjectWorkflow);

                if (incompleteWorkflowLabel != null) {
                    incompleteWorkflowLabels.add(incompleteWorkflowLabel);
                }
            }
        }

        if (!incompleteWorkflowLabels.isEmpty()) {
            throw new ConfigurationException(
                "MCP server has an incomplete tool mapping for: " + String.join(", ", incompleteWorkflowLabels),
                McpProjectWorkflowErrorType.INCOMPLETE_TOOL_MAPPING);
        }
    }

    /**
     * Returns the offending workflow's label when {@code mcpProjectWorkflow} is exposed (callable trigger, enabled
     * deployment) but incompletely mapped, or {@code null} when it is either complete or not exposed at all.
     */
    private @Nullable String checkMapping(McpProjectWorkflow mcpProjectWorkflow) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(mcpProjectWorkflow.getProjectDeploymentWorkflowId());

        if (!projectDeploymentWorkflow.isEnabled()) {
            return null;
        }

        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        WorkflowTrigger callableTrigger = getMcpToolCallableTrigger(workflow);

        if (callableTrigger == null) {
            return null;
        }

        Map<String, ?> parameters = mcpProjectWorkflow.getParameters();

        String toolName = MapUtils.getString(parameters, ToolConstants.TOOL_NAME);

        if (toolName == null || toolName.isBlank()) {
            return workflowLabel(workflow);
        }

        if (hasUnmappedRequiredInput(callableTrigger, parameters)) {
            return workflowLabel(workflow);
        }

        return null;
    }

    private boolean hasUnmappedRequiredInput(WorkflowTrigger callableTrigger, Map<String, ?> parameters) {
        String inputSchema = MapUtils.getString(callableTrigger.getParameters(), WorkflowConstants.INPUT_SCHEMA);

        if (inputSchema == null || inputSchema.isBlank()) {
            return false;
        }

        Map<String, ?> schema = JsonUtils.readMap(inputSchema);

        List<String> requiredInputNames = MapUtils.getList(schema, REQUIRED, String.class, List.of());

        for (String requiredInputName : requiredInputNames) {
            if (parameters.get(requiredInputName) == null) {
                return true;
            }
        }

        return false;
    }

    private static String workflowLabel(Workflow workflow) {
        String label = workflow.getLabel();

        return label == null || label.isBlank() ? workflow.getId() : label;
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
