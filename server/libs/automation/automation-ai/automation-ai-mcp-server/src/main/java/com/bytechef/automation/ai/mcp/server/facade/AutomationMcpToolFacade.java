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
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.ai.tool.FromAiResult;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import com.bytechef.platform.ai.tool.facade.AbstractToolFacade;
import com.bytechef.platform.ai.tool.util.FromAiInputSchemaUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
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
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
    private final ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
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
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider,
        PrincipalJobFacade principalJobFacade, ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
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
        this.planLimitsProviderObjectProvider = planLimitsProviderObjectProvider;
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
                    mcpTool.getParameters(), mcpComponent.getConnectionId(), mcpComponent.getMcpServerId()))
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

            Map<String, ?> workflowParameters = mcpProjectWorkflow.getParameters();

            String toolName = MapUtils.getString(workflowParameters, ToolConstants.TOOL_NAME);
            List<FromAiResult> fromAiResults = extractFromAiResults(workflowParameters);

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
                .builder(
                    Objects.requireNonNull(toolName),
                    getWorkflowToolCallbackFunction(
                        toolName, projectDeploymentWorkflow, trigger.getName(), workflowParameters,
                        mcpProject.getMcpServerId()))
                .inputType(Map.class)
                .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

            String description = MapUtils.getString(workflowParameters, ToolConstants.TOOL_DESCRIPTION);

            if (description != null) {
                builder.description(description);
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks;
    }

    private Function<Map<String, Object>, Object> getClusterElementToolCallbackFunction(
        String toolName, String componentName, int componentVersion, String clusterElementName,
        Map<String, ?> parameters, @Nullable Long connectionId, long mcpServerId) {

        Long workspaceId = workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServerId)
            .orElse(null);

        return request -> {
            McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

            if (!mcpServer.isEnabled()) {
                throw new ConfigurationException("MCP server is disabled", McpServerErrorType.MCP_SERVER_DISABLED);
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

        return inputParameters -> {
            McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

            if (!mcpServer.isEnabled()) {
                throw new ConfigurationException("MCP server is disabled", McpServerErrorType.MCP_SERVER_DISABLED);
            }

            Map<String, Object> inputs = new HashMap<>(projectDeploymentWorkflow.getInputs());

            Map<String, Object> resolvedTriggerInputs = new HashMap<>();

            for (Map.Entry<String, ?> entry : workflowParameters.entrySet()) {
                resolvedTriggerInputs.put(entry.getKey(), resolveParameterValue(entry.getValue(), inputParameters));
            }

            resolvedTriggerInputs.putAll(inputParameters);

            inputs.put(triggerName, resolvedTriggerInputs);

            long jobId = principalJobFacade.createJob(
                new JobParametersDTO(projectDeploymentWorkflow.getWorkflowId(), inputs),
                projectDeploymentWorkflow.getProjectDeploymentId(), PlatformType.AUTOMATION);

            return toolExecutionRecorder.record(
                ToolExecutionEvent
                    .builder(ToolExecutionSurface.MCP_AUTOMATION, ToolExecutionKind.WORKFLOW, toolName)
                    .mcpServerId(mcpServerId)
                    .workspaceId(workspaceId)
                    .jobId(jobId),
                () -> {
                    Job job = jobCompletionAwaiter.await(jobId, resolveSyncTimeout())
                        .join();

                    // A STOPPED run with a stored resume id is paused on a human approval, not finished — return a
                    // clear pointer to the hosted form instead of an empty result. Full MCP elicitation remains
                    // future work.
                    if (job.getStatus() == Job.Status.STOPPED) {
                        return describePendingApproval(job);
                    }

                    JobExecutionErrors.checkForError(job, taskExecutionService);

                    if (job.getOutputs() == null) {
                        return null;
                    }

                    return getCallableResponseOutput(job)
                        .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs()));
                });
        };
    }

    /**
     * The effective wait limit for a synchronous MCP tool run: the default sync timeout, capped by the tenant plan's
     * {@code syncRunTimeout} when one is set. The plan can only tighten the limit, never extend it.
     */
    private Duration resolveSyncTimeout() {
        PlanLimitsProvider planLimitsProvider = planLimitsProviderObjectProvider.getIfAvailable();

        if (planLimitsProvider == null) {
            return JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT;
        }

        Duration planSyncRunTimeout = planLimitsProvider.getPlanLimits(TenantContext.getCurrentTenantId())
            .syncRunTimeout();

        if (planSyncRunTimeout == null ||
            planSyncRunTimeout.compareTo(JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT) >= 0) {

            return JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT;
        }

        return planSyncRunTimeout;
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

        // The signed resume token is carried independently of formUrl so form-mode elicitation works on deployments
        // without a configured public URL (where formUrl is absent) — the hosted form is what needs a public URL, an
        // inline approval decision only needs the token.
        ApprovalFormUrls.buildResumeToken(jobResumeIdString, approvalTokens)
            .ifPresent(resumeToken -> result.put("resumeToken", resumeToken));

        if (job.getId() != null) {
            result.put("jobId", job.getId());
        }

        return result;
    }

    /**
     * Returns the server-authoritative signed resume token for a run that is genuinely paused on a human approval
     * (STOPPED with a stored resume id), or empty otherwise. Re-derived from the run's OWN state — like
     * {@link #resolvePendingApprovalFormUrl} — rather than trusting a token echoed in tool output, and available even
     * when no public URL is configured so form-mode elicitation still functions.
     */
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

    /**
     * Verifies that {@code jobId}'s workflow is one this MCP server actually exposes, so the approval-elicitation
     * decorator never resolves or returns the outputs of a run this server has no business seeing. Tenant scoping alone
     * is insufficient: a genuinely-paused run in another workspace of the same tenant would otherwise be readable by id
     * through a crafted {@code approval_required} descriptor. Fail-closed — an unresolvable job, or one whose workflow
     * is not in this server's exposed set, returns {@code false}.
     */
    public boolean isJobWorkflowExposedByMcpServer(long jobId, long mcpServerId) {
        String workflowId = jobService.fetchJob(jobId)
            .map(Job::getWorkflowId)
            .orElse(null);

        if (workflowId == null) {
            return false;
        }

        for (McpProject mcpProject : mcpProjectService.getMcpServerMcpProjects(mcpServerId)) {
            for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
                mcpProject.getId())) {

                try {
                    ProjectDeploymentWorkflow projectDeploymentWorkflow =
                        projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                            mcpProjectWorkflow.getProjectDeploymentWorkflowId());

                    if (workflowId.equals(projectDeploymentWorkflow.getWorkflowId())) {
                        return true;
                    }
                } catch (Exception exception) {
                    // A stale mcp_project_workflow row (its deployment workflow deleted) must not fail the approval
                    // tool call — skip it, staying fail-closed: an entry that cannot be resolved does not expose a job.
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "Skipping unresolvable project-deployment-workflow {} while checking MCP exposure: {}",
                            mcpProjectWorkflow.getProjectDeploymentWorkflowId(), exception.getMessage());
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns the server-authoritative hosted approval-form URL for a run that is genuinely paused on a human approval
     * (STOPPED with a stored resume id), or empty otherwise. The MCP elicitation decorator uses this instead of
     * trusting a {@code formUrl} echoed in tool-output text: a workflow could otherwise emit a crafted
     * {@code approval_required} descriptor as its normal output and point the reviewer at an attacker-controlled URL
     * (or name another run's job id). The job is resolved under the current tenant context, so it can only ever name
     * this tenant's own runs.
     */
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

    /**
     * Re-awaits a workflow run whose initial synchronous wait ended on a pending approval, after the human has been
     * pointed at the hosted form (via MCP URL elicitation). The completion awaiter treats STOPPED as terminal — it IS
     * terminal for an un-resumed run — so a plain await would hand back the still-paused job the instant a client
     * acknowledges the elicitation before the human actually resolves the form. Instead this polls until the job leaves
     * STOPPED (the resume flips it to STARTED) or the sync deadline passes, then awaits real completion. Returns the
     * run's outputs on completion, another pending-approval descriptor if the run is still (or again) paused, or throws
     * on a failed run — mirroring the initial call's semantics.
     */
    public @Nullable Object awaitApprovedWorkflowRun(long jobId) {
        Instant deadline = Instant.now()
            .plus(resolveSyncTimeout());

        Job job = jobService.getJob(jobId);

        while (job.getStatus() == Job.Status.STOPPED && Instant.now()
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

        if (job.getStatus() == Job.Status.STOPPED) {
            return describePendingApproval(job);
        }

        Duration remaining = Duration.between(Instant.now(), deadline);

        job = jobCompletionAwaiter.await(jobId, remaining.isNegative() ? Duration.ofSeconds(1) : remaining)
            .join();

        if (job.getStatus() == Job.Status.STOPPED) {
            return describePendingApproval(job);
        }

        Job completedJob = job;

        JobExecutionErrors.checkForError(completedJob, taskExecutionService);

        if (completedJob.getOutputs() == null) {
            return null;
        }

        return getCallableResponseOutput(completedJob)
            .orElseGet(() -> taskFileStorage.readJobOutputs(completedJob.getOutputs()));
    }

    /**
     * Resolves a pending approval directly with the decision collected through MCP FORM elicitation (approved +
     * optional comment) and then awaits the resumed run. A resume that is no longer possible (already resolved,
     * expired, or an invalid token) returns an {@code approval_unavailable} descriptor instead of throwing.
     */
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

    private static @Nullable WorkflowTrigger getMcpToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            String name = workflowNodeType.name();
            String operation = workflowNodeType.operation();

            if (Objects.equals(name, WorkflowConstants.WORKFLOW) &&
                Objects.equals(operation, WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}
