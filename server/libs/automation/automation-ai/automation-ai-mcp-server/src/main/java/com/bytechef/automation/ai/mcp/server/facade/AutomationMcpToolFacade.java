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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.server.exception.McpServerErrorType;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.ai.mcp.util.McpWorkflowUtils;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.ai.tool.FromAiResult;
import com.bytechef.platform.ai.tool.facade.AbstractToolFacade;
import com.bytechef.platform.ai.tool.util.FromAiInputSchemaUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.JobExecutionErrors;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalFormUrls;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Matija Petanjek
 */
public class AutomationMcpToolFacade extends AbstractToolFacade {

    private static final Logger log = LoggerFactory.getLogger(AutomationMcpToolFacade.class);

    private static final long RESUME_POLL_INTERVAL_MILLIS = 500;

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ObjectProvider<ApprovalTokens> approvalTokensObjectProvider;
    private final JobCompletionAwaiter jobCompletionAwaiter;
    private final JobResumeFacade jobResumeFacade;
    private final JobService jobService;
    private final @Nullable String publicUrl;
    private final McpComponentService mcpComponentService;
    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;
    private final PrincipalJobFacade principalJobFacade;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final ToolExecutionRecorder toolExecutionRecorder;
    private final WorkflowService workflowService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public AutomationMcpToolFacade(
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator,
        JobCompletionAwaiter jobCompletionAwaiter, JobResumeFacade jobResumeFacade, JobService jobService,
        McpComponentService mcpComponentService, McpProjectService mcpProjectService,
        McpProjectWorkflowService mcpProjectWorkflowService, McpServerService mcpServerService,
        McpToolService mcpToolService, PrincipalJobFacade principalJobFacade,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        @Nullable String publicUrl, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        ToolExecutionRecorder toolExecutionRecorder, WorkflowService workflowService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        super(evaluator);

        this.approvalTokensObjectProvider = approvalTokensObjectProvider;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.jobCompletionAwaiter = jobCompletionAwaiter;
        this.jobResumeFacade = jobResumeFacade;
        this.jobService = jobService;
        this.publicUrl = publicUrl;
        this.mcpComponentService = mcpComponentService;
        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
        this.principalJobFacade = principalJobFacade;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.toolExecutionRecorder = toolExecutionRecorder;
        this.workflowService = workflowService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    public FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(McpTool mcpTool) {
        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

        ClusterElementDefinition clusterElementDefinition =
            clusterElementDefinitionService.getClusterElementDefinition(
                mcpComponent.getComponentName(), mcpComponent.getComponentVersion(), mcpTool.getName());

        List<FromAiResult> fromAiResults = extractFromAiResults(mcpTool.getParameters());

        String toolName = getToolName(
            clusterElementDefinition.getComponentName(), clusterElementDefinition.getName(), mcpTool.getParameters());

        FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
            .builder(
                toolName,
                getClusterElementToolCallbackFunction(
                    toolName, clusterElementDefinition.getComponentName(),
                    clusterElementDefinition.getComponentVersion(), clusterElementDefinition.getName(),
                    mcpTool.getParameters(), mcpComponent.getConnectionId(), mcpComponent.getMcpServerId(),
                    Objects.requireNonNull(mcpTool.getId())))
            .inputType(Map.class)
            .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

        String toolDescription = getToolDescription(mcpTool.getParameters(), null);

        if (toolDescription == null) {
            toolDescription = clusterElementDefinition.getDescription();
        }

        if (toolDescription != null) {
            builder.description(toolDescription);
        }

        return builder.build();
    }

    public List<ToolCallback> getFunctionToolCallbacks(McpProject mcpProject) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
            mcpProject.getId())) {

            if (!mcpProjectWorkflow.isEnabled()) {
                continue;
            }

            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                    mcpProjectWorkflow.getProjectDeploymentWorkflowId());

            if (!projectDeploymentWorkflow.isEnabled()) {
                continue;
            }

            Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

            WorkflowTrigger trigger = McpWorkflowUtils.getCallableTrigger(workflow);

            if (trigger == null) {
                continue;
            }

            Map<String, ?> workflowParameters = mcpProjectWorkflow.getParameters();

            String toolName = getWorkflowToolName(workflowParameters, workflow.getLabel());

            if (toolName == null) {
                log.warn(
                    "Skipping workflow {} on MCP server {}: tool mapping is not completed (missing toolName)",
                    projectDeploymentWorkflow.getWorkflowId(), mcpProject.getMcpServerId());

                continue;
            }

            List<FromAiResult> fromAiResults = extractFromAiResults(workflowParameters);

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
                .builder(
                    toolName,
                    getWorkflowToolCallbackFunction(
                        toolName, projectDeploymentWorkflow, trigger.getName(), workflowParameters,
                        mcpProject.getMcpServerId()))
                .inputType(Map.class)
                .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

            String description = getToolDescription(workflowParameters, null);

            if (description == null) {
                description = workflow.getDescription();
            }

            if (description != null) {
                builder.description(description);
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks;
    }

    private Function<Map<String, Object>, Object> getClusterElementToolCallbackFunction(
        String toolName, String componentName, int componentVersion, String clusterElementName,
        Map<String, ?> parameters, @Nullable Long connectionId, long mcpServerId, long mcpToolId) {

        Long workspaceId = workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServerId)
            .orElse(null);

        return request -> {
            McpServer mcpServer = readMcpServerConfiguration(() -> mcpServerService.getMcpServer(mcpServerId));

            if (!mcpServer.isEnabled()) {
                throw new ConfigurationException("MCP server is disabled", McpServerErrorType.MCP_SERVER_DISABLED);
            }

            boolean toolEnabled = readMcpServerConfiguration(() -> mcpToolService.fetchMcpTool(mcpToolId))
                .map(McpTool::isEnabled)
                .orElse(false);

            if (!toolEnabled) {
                throw new ConfigurationException("MCP tool is disabled", McpServerErrorType.MCP_TOOL_DISABLED);
            }

            Map<String, Object> resolvedParameters = new HashMap<>();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                resolvedParameters.put(entry.getKey(), resolveParameterValue(entry.getValue(), request));
            }

            return toolExecutionRecorder.record(
                ToolExecutionEvent
                    .builder(ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.COMPONENT, toolName)
                    .componentName(componentName)
                    .componentVersion(componentVersion)
                    .operationName(clusterElementName)
                    .connectionId(connectionId)
                    .mcpServerId(mcpServerId)
                    .workspaceId(workspaceId),
                () -> clusterElementDefinitionFacade.executeTool(
                    componentName, componentVersion, clusterElementName,
                    MapUtils.concat(request, resolvedParameters), connectionId));
        };
    }

    private Function<Map<String, Object>, Object> getWorkflowToolCallbackFunction(
        String toolName, ProjectDeploymentWorkflow projectDeploymentWorkflow, String triggerName,
        Map<String, ?> workflowParameters, long mcpServerId) {

        Long workspaceId = workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServerId)
            .orElse(null);
        long projectDeploymentWorkflowId = Objects.requireNonNull(projectDeploymentWorkflow.getId());

        return inputParameters -> {
            McpServer mcpServer = readMcpServerConfiguration(() -> mcpServerService.getMcpServer(mcpServerId));

            if (!mcpServer.isEnabled()) {
                throw new ConfigurationException("MCP server is disabled", McpServerErrorType.MCP_SERVER_DISABLED);
            }

            ProjectDeploymentWorkflow currentProjectDeploymentWorkflow =
                projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId);

            if (!currentProjectDeploymentWorkflow.isEnabled()) {
                throw new ConfigurationException("MCP tool is disabled", McpServerErrorType.MCP_TOOL_DISABLED);
            }

            Map<String, Object> inputs = new HashMap<>(projectDeploymentWorkflow.getInputs());

            Map<String, Object> resolvedTriggerInputs = new HashMap<>();

            for (Map.Entry<String, ?> entry : workflowParameters.entrySet()) {
                resolvedTriggerInputs.put(entry.getKey(), resolveParameterValue(entry.getValue(), inputParameters));
            }

            resolvedTriggerInputs.putAll(inputParameters);

            inputs.put(triggerName, resolvedTriggerInputs);
            inputs.put(JobInputConstants.TRIGGER_NAME_INPUT, triggerName);

            long jobId = principalJobFacade.createJob(
                new JobParametersDTO(projectDeploymentWorkflow.getWorkflowId(), inputs),
                projectDeploymentWorkflow.getProjectDeploymentId(), PlatformType.AUTOMATION);

            return toolExecutionRecorder.record(
                ToolExecutionEvent
                    .builder(ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.WORKFLOW, toolName)
                    .mcpServerId(mcpServerId)
                    .workspaceId(workspaceId)
                    .jobId(jobId),
                () -> getWorkflowRunResult(
                    jobCompletionAwaiter.await(jobId, JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT)
                        .join()));
        };
    }

    private @Nullable Object getWorkflowRunResult(Job job) {
        if (isPausedOnApproval(job)) {
            return describePendingApproval(job);
        }

        JobExecutionErrors.checkForError(job, taskExecutionService);
        JobExecutionErrors.checkCompleted(job);

        if (job.getOutputs() == null) {
            return null;
        }

        return getCallableResponseOutput(job)
            .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs()));
    }

    private static boolean isPausedOnApproval(Job job) {
        return job.getStatus() == Job.Status.STOPPED && job.getMetadata(MetadataConstants.JOB_RESUME_ID) != null;
    }

    private Map<String, Object> describePendingApproval(Job job) {
        Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        String jobResumeIdString = jobResumeId == null ? null : jobResumeId.toString();
        ApprovalTokens approvalTokens = approvalTokensObjectProvider.getIfAvailable();

        String formUrl = ApprovalFormUrls
            .buildFormUrl(publicUrl, jobResumeIdString, approvalTokens)
            .orElse(null);

        Map<String, Object> result = new HashMap<>();

        result.put("status", "approval_required");
        result.put(
            "message",
            formUrl == null
                ? "Approval required — the workflow run is paused waiting for a human decision."
                : "Approval required — the workflow run is paused waiting for a human decision. " +
                    "Resolve it at: " + formUrl);

        if (formUrl != null) {
            result.put("formUrl", formUrl);
        }

        ApprovalFormUrls.buildResumeToken(jobResumeIdString, approvalTokens)
            .ifPresent(resumeToken -> result.put("resumeToken", resumeToken));

        if (job.getId() != null) {
            result.put("jobId", job.getId());
        }

        return result;
    }

    public Optional<String> resolvePendingApprovalResumeToken(long jobId) {
        Job job = jobService.fetchJob(jobId)
            .orElse(null);

        if (job == null || job.getStatus() != Job.Status.STOPPED) {
            return Optional.empty();
        }

        Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        if (jobResumeId == null) {
            return Optional.empty();
        }

        return ApprovalFormUrls.buildResumeToken(
            jobResumeId.toString(), approvalTokensObjectProvider.getIfAvailable());
    }

    public boolean isJobWorkflowExposedByMcpServer(long jobId, long mcpServerId) {
        String workflowId = jobService.fetchJob(jobId)
            .map(Job::getWorkflowId)
            .orElse(null);

        if (workflowId == null) {
            return false;
        }

        List<McpProject> mcpProjects = readMcpServerConfiguration(
            () -> mcpProjectService.getMcpServerMcpProjects(mcpServerId));

        for (McpProject mcpProject : mcpProjects) {
            for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
                mcpProject.getId())) {

                try {
                    ProjectDeploymentWorkflow projectDeploymentWorkflow =
                        projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                            mcpProjectWorkflow.getProjectDeploymentWorkflowId());

                    if (workflowId.equals(projectDeploymentWorkflow.getWorkflowId())) {
                        return true;
                    }
                } catch (NoSuchElementException noSuchElementException) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "Skipping unresolvable project-deployment-workflow {} while checking MCP exposure: {}",
                            mcpProjectWorkflow.getProjectDeploymentWorkflowId(), noSuchElementException.getMessage());
                    }
                } catch (RuntimeException runtimeException) {
                    log.warn(
                        "Skipping project-deployment-workflow {} while checking MCP exposure: {}",
                        mcpProjectWorkflow.getProjectDeploymentWorkflowId(), runtimeException.getMessage(),
                        runtimeException);
                }
            }
        }

        return false;
    }

    public Optional<String> resolvePendingApprovalFormUrl(long jobId) {
        Job job = jobService.fetchJob(jobId)
            .orElse(null);

        if (job == null || job.getStatus() != Job.Status.STOPPED) {
            return Optional.empty();
        }

        Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        if (jobResumeId == null) {
            return Optional.empty();
        }

        return ApprovalFormUrls.buildFormUrl(
            publicUrl, jobResumeId.toString(), approvalTokensObjectProvider.getIfAvailable());
    }

    public @Nullable Object awaitApprovedWorkflowRun(long jobId) {
        Instant deadline = Instant.now()
            .plus(JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT);

        Job job = jobService.getJob(jobId);

        while (isPausedOnApproval(job) && Instant.now()
            .isBefore(deadline)) {

            try {
                Thread.sleep(RESUME_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                break;
            }

            job = jobService.getJob(jobId);
        }

        if (isPausedOnApproval(job)) {
            return describePendingApproval(job);
        }

        Duration remaining = Duration.between(Instant.now(), deadline);

        job = jobCompletionAwaiter.await(jobId, remaining.isNegative() ? Duration.ofSeconds(1) : remaining)
            .join();

        return getWorkflowRunResult(job);
    }

    public @Nullable Object resolveApprovalAndAwait(String resumeToken, Map<String, Object> data, long jobId) {
        JobResumeFacade.JobResumeOutcome outcome = jobResumeFacade.resumeJob(resumeToken, data);

        if (outcome != JobResumeFacade.JobResumeOutcome.OK) {
            return Map.of(
                "status", "approval_unavailable",
                "message", "The approval could no longer be resolved (" + outcome + ").");
        }

        return awaitApprovedWorkflowRun(jobId);
    }

    private Optional<Object> getCallableResponseOutput(Job job) {
        try {
            return taskExecutionService.fetchLastJobTaskExecution(Objects.requireNonNull(job.getId()))
                .filter(
                    lastTaskExecution -> {
                        Map<String, ?> metadata = lastTaskExecution.getMetadata();

                        return metadata.containsKey(MetadataConstants.CALLABLE_RESPONSE);
                    })
                .map(lastTaskExecution -> {
                    ActionDefinition.CallableResponse callableResponse = ConvertUtils.convertValue(
                        taskFileStorage.readTaskExecutionOutput(lastTaskExecution.getOutput()),
                        ActionDefinition.CallableResponse.class);

                    return callableResponse.output();
                });
        } catch (Exception exception) {
            log.warn(
                "Failed to extract callable response output from job {}: {}", job.getId(), exception.getMessage());

            return Optional.empty();
        }
    }

    private static <T> T readMcpServerConfiguration(Supplier<T> read) {
        try {
            return AutomationAuthorizationContext.callSkippingChecks(read::get);
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }

}
