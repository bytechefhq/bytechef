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

package com.bytechef.automation.ai.mcp.server.facade;

import com.bytechef.ai.tool.FromAiResult;
import com.bytechef.ai.tool.constant.ToolConstants;
import com.bytechef.ai.tool.facade.AbstractToolFacade;
import com.bytechef.ai.tool.util.FromAiInputSchemaUtils;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.mcp.domain.McpProject;
import com.bytechef.automation.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @author Matija Petanjek
 */
public class McpToolFacade extends AbstractToolFacade {

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final JobSyncExecutor jobSyncExecutor;
    private final McpComponentService mcpComponentService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final PrincipalJobFacade principalJobFacade;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public McpToolFacade(
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator,
        JobSyncExecutor jobSyncExecutor, McpComponentService mcpComponentService,
        McpProjectWorkflowService mcpProjectWorkflowService, PrincipalJobFacade principalJobFacade,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, TaskFileStorage taskFileStorage,
        WorkflowService workflowService) {

        super(evaluator);

        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.jobSyncExecutor = jobSyncExecutor;
        this.mcpComponentService = mcpComponentService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.principalJobFacade = principalJobFacade;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    public FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(McpTool mcpTool) {
        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

        ClusterElementDefinition clusterElementDefinition =
            clusterElementDefinitionService.getClusterElementDefinition(
                mcpComponent.getComponentName(), mcpComponent.getComponentVersion(), mcpTool.getName());

        List<FromAiResult> fromAiResults = extractFromAiResults(mcpTool.getParameters());

        FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback.builder(
            getToolName(clusterElementDefinition.getComponentName(), clusterElementDefinition.getName()),
            getFromAiToolCallbackFunction(
                clusterElementDefinition.getComponentName(), clusterElementDefinition.getComponentVersion(),
                clusterElementDefinition.getName(), mcpTool.getParameters(), mcpComponent.getConnectionId()))
            .inputType(Map.class)
            .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

        if (clusterElementDefinition.getDescription() != null) {
            builder.description(clusterElementDefinition.getDescription());
        }

        return builder.build();
    }

    public List<ToolCallback> getFunctionToolCallbacks(McpProject mcpProject) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
            mcpProject.getId())) {

            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                    mcpProjectWorkflow.getProjectDeploymentWorkflowId());

            if (!projectDeploymentWorkflow.isEnabled()) {
                continue;
            }

            Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

            WorkflowTrigger trigger = getMcpToolCallableTrigger(workflow);

            if (trigger == null) {
                continue;
            }

            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(trigger.getType());

            boolean mcpTrigger = Objects.equals(workflowNodeType.name(), "workflow");

            String toolName;
            String description;

            if (mcpTrigger) {
                toolName = MapUtils.getString(trigger.getParameters(), ToolConstants.TOOL_NAME);
                description = MapUtils.getString(trigger.getParameters(), ToolConstants.TOOL_DESCRIPTION);
            } else {
                throw new IllegalArgumentException("MCP trigger not found: " + workflowNodeType.name());
            }

            Map<String, ?> workflowParameters = mcpProjectWorkflow.getParameters();

            List<FromAiResult> fromAiResults = extractFromAiResults(workflowParameters);

            String inputSchema;

            if (!fromAiResults.isEmpty()) {
                inputSchema = FromAiInputSchemaUtils.generateInputSchema(fromAiResults);
            } else {
                inputSchema = MapUtils.getString(trigger.getParameters(), WorkflowConstants.INPUT_SCHEMA);

                if (inputSchema == null || inputSchema.isEmpty()) {
                    continue;
                }
            }

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback.builder(
                Objects.requireNonNull(toolName),
                getWorkflowFromAiToolCallbackFunction(projectDeploymentWorkflow, trigger.getName(),
                    workflowParameters))
                .inputType(Map.class)
                .inputSchema(inputSchema);

            if (description != null) {
                builder.description(description);
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks;
    }

    private Function<Map<String, Object>, Object> getFromAiToolCallbackFunction(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> parameters,
        @Nullable Long connectionId) {

        return request -> {
            Map<String, Object> resolvedParameters = new HashMap<>();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                resolvedParameters.put(entry.getKey(), resolveParameterValue(entry.getValue(), request));
            }

            return clusterElementDefinitionFacade.executeTool(
                componentName, componentVersion, clusterElementName, MapUtils.concat(request, resolvedParameters),
                connectionId);
        };
    }

    private Function<Map<String, Object>, Object> getWorkflowFromAiToolCallbackFunction(
        ProjectDeploymentWorkflow projectDeploymentWorkflow, String triggerName,
        Map<String, ?> workflowParameters) {

        return inputParameters -> {
            Map<String, Object> inputs = new HashMap<>(projectDeploymentWorkflow.getInputs());

            Map<String, Object> resolvedTriggerInputs = new HashMap<>();

            for (Map.Entry<String, ?> entry : workflowParameters.entrySet()) {
                resolvedTriggerInputs.put(entry.getKey(), resolveParameterValue(entry.getValue(), inputParameters));
            }

            resolvedTriggerInputs.putAll(inputParameters);

            inputs.put(triggerName, resolvedTriggerInputs);

            long projectDeploymentId = projectDeploymentWorkflow.getProjectDeploymentId();

            Job job = jobSyncExecutor.execute(
                new JobParametersDTO(projectDeploymentWorkflow.getWorkflowId(), inputs),
                jobParameters -> principalJobFacade.createSyncJob(
                    jobParameters, projectDeploymentId, PlatformType.AUTOMATION),
                true,
                taskExecutionCompleteEvent -> {});

            if (job.getOutputs() == null) {
                return null;
            }

            return taskFileStorage.readJobOutputs(job.getOutputs());
        };
    }

    private static @Nullable WorkflowTrigger getMcpToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            String name = workflowNodeType.name();
            String operation = workflowNodeType.operation();

            if (Objects.equals(name, WorkflowConstants.WORKFLOW) &&
                Objects.equals(operation, WorkflowConstants.TOOL_CALLABLE)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}
