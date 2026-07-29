/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.listener;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageContext;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageRecorder;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import com.bytechef.platform.ai.usage.WorkflowLlmUsageEvent;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Bridges the CE {@link WorkflowLlmUsageEvent} emission seam into the unified {@code ai_llm_usage} metering store: each
 * event becomes one row with {@code source = AI_AGENT} and {@code ownerId = jobId}, which the terminal-status cost
 * writer later sums into the job's execution cost. Best-effort: unresolvable workspace (embedded surface, editor runs
 * without a deployment) drops the row with a debug log.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkflowLlmUsageEventListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowLlmUsageEventListener.class);

    private static final String UNKNOWN_MODEL = "unknown";

    private final LlmUsageRecorder llmUsageRecorder;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowLlmUsageEventListener(
        LlmUsageRecorder llmUsageRecorder, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService) {

        this.llmUsageRecorder = llmUsageRecorder;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Async
    @EventListener
    public void onWorkflowLlmUsageEvent(WorkflowLlmUsageEvent workflowLlmUsageEvent) {
        try {
            Long workspaceId = resolveWorkspaceId(workflowLlmUsageEvent);

            if (workspaceId == null) {
                log.debug(
                    "Skipping LLM usage recording for job {}: workspace unresolvable (type={}, principalId={})",
                    workflowLlmUsageEvent.jobId(), workflowLlmUsageEvent.type(),
                    workflowLlmUsageEvent.jobPrincipalId());

                return;
            }

            LlmUsageContext llmUsageContext = new LlmUsageContext(
                workspaceId, null, LlmUsageSource.AI_AGENT, workflowLlmUsageEvent.jobId(),
                workflowLlmUsageEvent.componentName() + "/" + workflowLlmUsageEvent.actionName(), null, null);

            String model = workflowLlmUsageEvent.model();

            llmUsageRecorder.recordLlm(
                llmUsageContext, model != null ? model : UNKNOWN_MODEL, workflowLlmUsageEvent.promptTokens(),
                workflowLlmUsageEvent.completionTokens(), workflowLlmUsageEvent.durationMs());
        } catch (RuntimeException exception) {
            // Metering must never affect workflow execution.
            log.warn("Failed to record workflow LLM usage for job {}", workflowLlmUsageEvent.jobId(), exception);
        }
    }

    private Long resolveWorkspaceId(WorkflowLlmUsageEvent workflowLlmUsageEvent) {
        if (workflowLlmUsageEvent.type() != PlatformType.AUTOMATION ||
            workflowLlmUsageEvent.jobPrincipalId() == null) {

            return null;
        }

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            workflowLlmUsageEvent.jobPrincipalId());

        Project project = projectService.getProject(projectDeployment.getProjectId());

        return project.getWorkspaceId();
    }
}
