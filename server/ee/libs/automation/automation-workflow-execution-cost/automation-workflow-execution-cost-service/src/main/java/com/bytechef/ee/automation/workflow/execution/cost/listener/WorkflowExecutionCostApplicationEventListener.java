/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.workflow.execution.cost.config.WorkflowExecutionCostProperties;
import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkflowExecutionCost;
import com.bytechef.ee.automation.workflow.execution.cost.service.WorkflowExecutionCostService;
import com.bytechef.ee.automation.workflow.execution.cost.service.WorkspaceWorkflowExecutionCostService;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Writes one {@code workflow_execution_cost} row when a job reaches terminal status:
 * {@code totalCost = baseRunCharge + Σ ai_llm_usage(source = AI_AGENT, ownerId = jobId).cost}. Joins the coordinator's
 * {@code ApplicationEventListener} fan-out — no atlas change; the engine stays cost-agnostic. Idempotent per job
 * (unique constraint + pre-check) so broker redeliveries don't double-charge.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
// Ordered BEFORE WorkflowAlertApplicationEventListener (200) so COST_THRESHOLD alert rules can read the cost row
// written here within the same event fan-out.
@Component
@ConditionalOnEEVersion
@Order(100)
public class WorkflowExecutionCostApplicationEventListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionCostApplicationEventListener.class);

    private static final Set<Job.Status> TERMINAL_STATUSES = EnumSet.of(
        Job.Status.COMPLETED, Job.Status.FAILED, Job.Status.STOPPED, Job.Status.CANCELLED);

    private final AiLlmUsageService aiLlmUsageService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final WorkflowExecutionCostProperties workflowExecutionCostProperties;
    private final WorkflowExecutionCostService workflowExecutionCostService;
    private final WorkspaceWorkflowExecutionCostService workspaceWorkflowExecutionCostService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowExecutionCostApplicationEventListener(
        AiLlmUsageService aiLlmUsageService, PrincipalJobService principalJobService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        WorkflowExecutionCostProperties workflowExecutionCostProperties,
        WorkflowExecutionCostService workflowExecutionCostService,
        WorkspaceWorkflowExecutionCostService workspaceWorkflowExecutionCostService) {

        this.aiLlmUsageService = aiLlmUsageService;
        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.workflowExecutionCostProperties = workflowExecutionCostProperties;
        this.workflowExecutionCostService = workflowExecutionCostService;
        this.workspaceWorkflowExecutionCostService = workspaceWorkflowExecutionCostService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        if (!workflowExecutionCostProperties.enabled() ||
            !TERMINAL_STATUSES.contains(jobStatusApplicationEvent.getStatus())) {

            return;
        }

        long jobId = jobStatusApplicationEvent.getJobId();

        try {
            if (workflowExecutionCostService.fetchByJobId(jobId)
                .isPresent()) {

                return;
            }

            BigDecimal aiCost = aiLlmUsageService.getUsagesByOwner(LlmUsageSource.AI_AGENT, jobId)
                .stream()
                .map(AiLlmUsage::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            WorkflowExecutionCost workflowExecutionCost = new WorkflowExecutionCost(
                jobId, workflowExecutionCostProperties.baseRunChargeUsd(), aiCost);

            // Workspace attribution goes through the workspace_* membership table — workspace is an
            // automation-configuration-owned concept, so the cost row itself carries no workspace column.
            workspaceWorkflowExecutionCostService.createInWorkspace(workflowExecutionCost, resolveWorkspaceId(jobId));
        } catch (RuntimeException exception) {
            // Cost bookkeeping must never affect the job lifecycle or the other event listeners.
            log.warn("Failed to record execution cost for job {}", jobId, exception);
        }
    }

    private Long resolveWorkspaceId(long jobId) {
        return principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION)
            .map(projectDeploymentService::getProjectDeployment)
            .map(ProjectDeployment::getProjectId)
            .map(projectService::getProject)
            .map(project -> project.getWorkspaceId())
            .orElse(null);
    }
}
