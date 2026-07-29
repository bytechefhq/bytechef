/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.constant.EmbeddedToolConstants;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.ai.tool.FromAiResult;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import com.bytechef.platform.ai.tool.facade.AbstractToolFacade;
import com.bytechef.platform.ai.tool.util.FromAiInputSchemaUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
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
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
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
 * Tool facade for the embedded MCP server. Handles both component-level tools (direct action execution) and integration
 * workflow tools (workflow-based execution on the coordinator). Connection resolution is per-connected-user via
 * {@link IntegrationInstanceService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpToolFacade extends AbstractToolFacade {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedMcpToolFacade.class);

    private static final long RESUME_POLL_INTERVAL_MILLIS = 500;

    private final ObjectProvider<ApprovalTokens> approvalTokensObjectProvider;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserService connectedUserService;
    private final JwtTokenService jwtTokenService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final JobCompletionAwaiter jobCompletionAwaiter;
    private final JobResumeFacade jobResumeFacade;
    private final JobService jobService;
    private final McpComponentService mcpComponentService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpServerService mcpServerService;
    private final ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
    private final PrincipalJobFacade principalJobFacade;
    private final String publicUrl;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final ToolExecutionRecorder toolExecutionRecorder;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpToolFacade(
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService, ConnectedUserService connectedUserService,
        Evaluator evaluator, IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        JobCompletionAwaiter jobCompletionAwaiter, JobResumeFacade jobResumeFacade, JobService jobService,
        JwtTokenService jwtTokenService,
        McpComponentService mcpComponentService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService,
        McpServerService mcpServerService, ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider,
        PrincipalJobFacade principalJobFacade, String publicUrl,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        ToolExecutionRecorder toolExecutionRecorder, WorkflowService workflowService) {

        super(evaluator);

        this.approvalTokensObjectProvider = approvalTokensObjectProvider;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserService = connectedUserService;
        this.jwtTokenService = jwtTokenService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.jobCompletionAwaiter = jobCompletionAwaiter;
        this.jobResumeFacade = jobResumeFacade;
        this.jobService = jobService;
        this.mcpComponentService = mcpComponentService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpServerService = mcpServerService;
        this.planLimitsProviderObjectProvider = planLimitsProviderObjectProvider;
        this.principalJobFacade = principalJobFacade;
        this.publicUrl = publicUrl;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.toolExecutionRecorder = toolExecutionRecorder;
        this.workflowService = workflowService;
    }

    /**
     * Resolves the synchronous-run await timeout: the configured default, tightened to the tenant plan's
     * {@code syncRunTimeout} when one is set. The plan can only tighten the limit, never extend it — mirroring the
     * automation MCP and A2A sync surfaces.
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

    public @Nullable FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(
        McpTool mcpTool, String externalUserId, Environment environment, String tenantId) {

        if (!mcpTool.isEnabled()) {
            return null;
        }

        Long integrationInstanceId = fetchIntegrationInstanceId(
            externalUserId, mcpTool.getMcpComponentId(), environment);

        if (integrationInstanceId != null && !isToolEnabled(integrationInstanceId, mcpTool.getId())) {
            return null;
        }

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
                    toolName, externalUserId, clusterElementDefinition.getComponentName(),
                    clusterElementDefinition.getComponentVersion(),
                    clusterElementDefinition.getName(), mcpTool.getParameters(),
                    mcpComponent.getMcpServerId(), environment, tenantId))
            .inputType(Map.class)
            .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

        if (clusterElementDefinition.getDescription() != null) {
            builder.description(clusterElementDefinition.getDescription());
        }

        return builder.build();
    }

    public List<ToolCallback> getFunctionToolCallbacks(
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration, String externalUserId,
        Environment environment, String tenantId) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfiguration.getId())) {

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            if (!integrationInstanceConfigurationWorkflow.isEnabled()) {
                continue;
            }

            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                    integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

            Integration integration = integrationService.getIntegration(
                integrationInstanceConfiguration.getIntegrationId());

            Long integrationInstanceId = fetchIntegrationInstanceId(
                externalUserId, integration.getComponentName(), environment);

            if (integrationInstanceId != null &&
                !isWorkflowEnabled(integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflow.getId())) {

                continue;
            }

            Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

            WorkflowTrigger trigger = getMcpToolCallableTrigger(workflow);

            if (trigger == null) {
                continue;
            }

            Map<String, ?> workflowParameters = mcpIntegrationInstanceConfigurationWorkflow.getParameters();

            String toolName = MapUtils.getString(workflowParameters, ToolConstants.TOOL_NAME);

            if (toolName == null) {
                toolName = MapUtils.getString(trigger.getParameters(), ToolConstants.TOOL_NAME);
            }

            List<FromAiResult> fromAiResults = extractFromAiResults(workflowParameters);

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
                .builder(
                    Objects.requireNonNull(toolName),
                    getWorkflowToolCallbackFunction(
                        toolName, externalUserId, integration.getComponentName(), integration.getId(),
                        integrationInstanceConfigurationWorkflow,
                        trigger.getName(), workflowParameters, mcpIntegrationInstanceConfiguration.getMcpServerId(),
                        environment, tenantId))
                .inputType(Map.class)
                .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

            String description = MapUtils.getString(workflowParameters, ToolConstants.TOOL_DESCRIPTION);

            if (description == null) {
                description = MapUtils.getString(trigger.getParameters(), ToolConstants.TOOL_DESCRIPTION);
            }

            if (description != null) {
                builder.description(description);
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks;
    }

    private @Nullable Long fetchIntegrationInstanceId(
        String externalUserId, long mcpComponentId, Environment environment) {

        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpComponentId);

        return fetchIntegrationInstanceId(externalUserId, mcpComponent.getComponentName(), environment);
    }

    private @Nullable Long fetchConnectionId(String externalUserId, String componentName, Environment environment) {
        return connectedUserService.fetchConnectedUser(externalUserId, environment)
            .map(ConnectedUser::getId)
            .flatMap(connectedUserId -> integrationInstanceService.fetchIntegrationInstance(
                connectedUserId, componentName, environment))
            .map(IntegrationInstance::getConnectionId)
            .orElse(null);
    }

    private @Nullable Long fetchIntegrationInstanceId(
        String externalUserId, String componentName, Environment environment) {

        return connectedUserService.fetchConnectedUser(externalUserId, environment)
            .map(ConnectedUser::getId)
            .flatMap(connectedUserId -> integrationInstanceService.fetchIntegrationInstance(
                connectedUserId, componentName, environment))
            .map(IntegrationInstance::getId)
            .orElse(null);
    }

    private Function<Map<String, Object>, Object> getClusterElementToolCallbackFunction(
        String toolName, String externalUserId, String componentName, int componentVersion, String clusterElementName,
        Map<String, ?> parameters, long mcpServerId, Environment environment, String tenantId) {

        return request -> {
            McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

            if (!mcpServer.isEnabled()) {
                throw new IllegalStateException("MCP server is disabled");
            }

            Long connectionId = fetchConnectionId(externalUserId, componentName, environment);

            ToolExecutionEvent.Builder eventBuilder = ToolExecutionEvent
                .builder(ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionKind.COMPONENT, toolName)
                .tenantId(tenantId)
                .componentName(componentName)
                .componentVersion(componentVersion)
                .operationName(clusterElementName)
                .connectionId(connectionId)
                .mcpServerId(mcpServerId)
                .externalUserId(externalUserId)
                .environment(environment.ordinal());

            if (connectionId == null
                && isConnectionRequired(componentDefinitionService, componentName, componentVersion)) {

                long integrationId = getIntegrationId(componentName);

                Object connectionRequiredResponse = getConnectionRequiredResponse(
                    componentName, environment, externalUserId, integrationId, tenantId);

                toolExecutionRecorder.record(
                    eventBuilder.outcome(ToolExecutionOutcome.CONNECTION_REQUIRED)
                        .build());

                return connectionRequiredResponse;
            }

            Map<String, Object> resolvedParameters = new HashMap<>();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                resolvedParameters.put(entry.getKey(), resolveParameterValue(entry.getValue(), request));
            }

            Map<String, Object> mergedParameters = EmbeddedToolConstants.withConnectedUserContext(
                MapUtils.concat(request, resolvedParameters), externalUserId, environment);

            return toolExecutionRecorder.record(
                eventBuilder,
                () -> clusterElementDefinitionFacade.executeTool(
                    componentName, componentVersion, clusterElementName, mergedParameters, connectionId));
        };
    }

    private long getIntegrationId(String componentName) {
        return integrationService.getIntegrations()
            .stream()
            .filter(integration -> componentName.equals(integration.getComponentName()))
            .map(Integration::getId)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No integration found for component: " + componentName));
    }

    private Function<Map<String, Object>, Object> getWorkflowToolCallbackFunction(
        String toolName, String externalUserId, String componentName, long integrationId,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, String triggerName,
        Map<String, ?> workflowParameters, long mcpServerId, Environment environment, String tenantId) {

        return inputParameters -> {
            McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

            if (!mcpServer.isEnabled()) {
                throw new IllegalStateException("MCP server is disabled");
            }

            Long integrationInstanceId = fetchIntegrationInstanceId(externalUserId, componentName, environment);

            ToolExecutionEvent.Builder eventBuilder = ToolExecutionEvent
                .builder(ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionKind.WORKFLOW, toolName)
                .tenantId(tenantId)
                .componentName(componentName)
                .mcpServerId(mcpServerId)
                .externalUserId(externalUserId)
                .integrationInstanceId(integrationInstanceId)
                .environment(environment.ordinal());

            if (integrationInstanceId == null) {
                Object connectionRequiredResponse = getConnectionRequiredResponse(
                    componentName, environment, externalUserId, integrationId, tenantId);

                toolExecutionRecorder.record(
                    eventBuilder.outcome(ToolExecutionOutcome.CONNECTION_REQUIRED)
                        .build());

                return connectionRequiredResponse;
            }

            Map<String, Object> inputs = new HashMap<>(integrationInstanceConfigurationWorkflow.getInputs());

            Map<String, Object> triggerInputs = new HashMap<>();

            for (Map.Entry<String, ?> entry : workflowParameters.entrySet()) {
                triggerInputs.put(entry.getKey(), resolveParameterValue(entry.getValue(), inputParameters));
            }

            triggerInputs.putAll(inputParameters);

            inputs.put(triggerName, triggerInputs);

            long jobId = principalJobFacade.createJob(
                new JobParametersDTO(integrationInstanceConfigurationWorkflow.getWorkflowId(), inputs),
                integrationInstanceId, PlatformType.EMBEDDED);

            return toolExecutionRecorder.record(
                eventBuilder.jobId(jobId),
                () -> {
                    Job job = jobCompletionAwaiter.await(jobId, resolveSyncTimeout())
                        .join();

                    // A STOPPED run with a stored resume id is paused on a human approval, not finished — return a
                    // clear pointer to the hosted form instead of an empty result (mirrors AutomationMcpToolFacade).
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

        // Carried independently of formUrl so form-mode elicitation works without a configured public URL — the hosted
        // form needs a public URL, an inline approval decision only needs the token.
        ApprovalFormUrls.buildResumeToken(jobResumeIdString, approvalTokens)
            .ifPresent(resumeToken -> result.put("resumeToken", resumeToken));

        if (job.getId() != null) {
            result.put("jobId", job.getId());
        }

        return result;
    }

    /**
     * Returns the server-authoritative hosted approval-form URL for a run genuinely paused on a human approval (STOPPED
     * with a stored resume id), or empty otherwise. The MCP elicitation decorator uses this instead of trusting a
     * {@code formUrl} echoed in tool-output text (a workflow could craft an {@code approval_required} descriptor as its
     * output and point the reviewer at an attacker URL). Resolved under the current tenant, so it only ever names this
     * tenant's own runs.
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
     * Returns the server-authoritative signed resume token for a run genuinely paused on a human approval (STOPPED with
     * a stored resume id), or empty otherwise. Re-derived from the run's OWN state — like
     * {@link #resolvePendingApprovalFormUrl} — and available even when no public URL is configured so form-mode
     * elicitation still functions.
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
     * Re-awaits a workflow run whose initial synchronous wait ended on a pending approval, after the human has been
     * pointed at the hosted form (via MCP URL elicitation). The completion awaiter treats STOPPED as terminal — so a
     * plain await would hand back the still-paused job the instant a client acknowledges the elicitation. Instead this
     * polls until the job leaves STOPPED (the resume flips it to STARTED) or the sync deadline passes, then awaits real
     * completion. Returns the run's outputs on completion, a fresh pending-approval descriptor if still (or again)
     * paused, or throws on a failed run — mirroring the initial call's semantics.
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

    private boolean isToolEnabled(long integrationInstanceId, long mcpToolId) {
        return mcpIntegrationInstanceToolService
            .fetchMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId)
            .map(McpIntegrationInstanceTool::isEnabled)
            .orElse(false);
    }

    private boolean isWorkflowEnabled(long integrationInstanceId, long mcpIntegrationInstanceConfigurationWorkflowId) {
        return mcpIntegrationInstanceConfigurationWorkflowService
            .fetchMcpIntegrationInstanceConfigurationWorkflow(mcpIntegrationInstanceConfigurationWorkflowId)
            .map(mcpConfigWorkflow -> integrationInstanceWorkflowService
                .fetchIntegrationInstanceWorkflow(
                    integrationInstanceId, mcpConfigWorkflow.getIntegrationInstanceConfigurationWorkflowId())
                .map(IntegrationInstanceWorkflow::isEnabled)
                .orElse(false))
            .orElse(false);
    }

    private Map<String, Object> getConnectionRequiredResponse(
        String componentName, Environment environment, String externalUserId, long integrationId, String tenantId) {

        String jwtToken = jwtTokenService.generateJwtToken(
            externalUserId, integrationId, environment.ordinal(), tenantId);

        String setupUrl = publicUrl + "/connect.html?token=" + jwtToken;

        return Map.of(
            "error", "connection_required",
            "message",
            "The " + componentName + " integration is not connected for this user. " +
                "To connect, visit: " + setupUrl + " . " +
                "Instruct the user to visit this link to connect their account.",
            "setupUrl", setupUrl);
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
        } catch (IllegalArgumentException | ClassCastException exception) {
            log.warn(
                "Failed to extract callable response output from job {}: {}", job.getId(), exception.getMessage());

            return Optional.empty();
        }
    }

    static boolean isConnectionRequired(
        ComponentDefinitionService componentDefinitionService, String componentName, int componentVersion) {

        ComponentDefinition componentDefinition =
            componentDefinitionService.getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getConnection() != null;
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
