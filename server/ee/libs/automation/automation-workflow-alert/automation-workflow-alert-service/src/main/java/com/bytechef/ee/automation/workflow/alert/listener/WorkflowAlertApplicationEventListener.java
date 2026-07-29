/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.workflow.alert.dispatcher.WorkflowAlertDispatcher;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertEvent;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRuleType;
import com.bytechef.ee.automation.workflow.alert.evaluator.WorkflowAlertEvaluator;
import com.bytechef.ee.automation.workflow.alert.evaluator.WorkflowAlertEvaluator.Breach;
import com.bytechef.ee.automation.workflow.alert.evaluator.WorkflowAlertEvaluator.RunFacts;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertEventService;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertRuleService;
import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkflowExecutionCost;
import com.bytechef.ee.automation.workflow.execution.cost.service.WorkflowExecutionCostService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Feeds every terminal job event through the workspace's enabled alert rules. Joins the coordinator's
 * {@code ApplicationEventListener} fan-out — no atlas change; the engine stays alert-agnostic. Ordered AFTER the
 * execution-cost listener so COST_THRESHOLD rules see the freshly written cost row. Rolling rule state is persisted on
 * every matching event (even without a breach) so counters survive restarts; breaches are recorded as
 * {@code workflow_alert_event} rows and dispatched asynchronously through the rule's {@code Notification} targets.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@Order(200)
public class WorkflowAlertApplicationEventListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowAlertApplicationEventListener.class);

    private static final Set<Job.Status> TERMINAL_STATUSES = EnumSet.of(
        Job.Status.COMPLETED, Job.Status.FAILED, Job.Status.STOPPED, Job.Status.CANCELLED);

    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final WorkflowAlertDispatcher workflowAlertDispatcher;
    private final WorkflowAlertEventService workflowAlertEventService;
    private final WorkflowAlertRuleService workflowAlertRuleService;
    private final ObjectProvider<WorkflowExecutionCostService> workflowExecutionCostServiceProvider;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowAlertApplicationEventListener(
        JobService jobService, PrincipalJobService principalJobService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        WorkflowAlertDispatcher workflowAlertDispatcher, WorkflowAlertEventService workflowAlertEventService,
        WorkflowAlertRuleService workflowAlertRuleService,
        ObjectProvider<WorkflowExecutionCostService> workflowExecutionCostServiceProvider) {

        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.workflowAlertDispatcher = workflowAlertDispatcher;
        this.workflowAlertEventService = workflowAlertEventService;
        this.workflowAlertRuleService = workflowAlertRuleService;
        this.workflowExecutionCostServiceProvider = workflowExecutionCostServiceProvider;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) ||
            !TERMINAL_STATUSES.contains(jobStatusApplicationEvent.getStatus())) {

            return;
        }

        long jobId = jobStatusApplicationEvent.getJobId();

        try {
            Long workspaceId = resolveWorkspaceId(jobId);

            if (workspaceId == null) {
                return;
            }

            List<WorkflowAlertRule> rules = workflowAlertRuleService.getEnabledWorkflowAlertRules(workspaceId);

            if (rules.isEmpty()) {
                return;
            }

            Job job = jobService.getJob(jobId);

            RunFacts runFacts = new RunFacts(
                jobStatusApplicationEvent.getStatus() == Job.Status.FAILED, durationMs(job),
                fetchTotalCost(rules, jobId));

            Instant now = Instant.now();

            for (WorkflowAlertRule rule : rules) {
                if (rule.getWorkflowId() != null && !Objects.equals(rule.getWorkflowId(), job.getWorkflowId())) {
                    continue;
                }

                Breach breach = WorkflowAlertEvaluator.evaluate(rule, runFacts, now);

                if (breach != null && WorkflowAlertEvaluator.isCooldownElapsed(rule, now)) {
                    rule.setLastTriggeredDate(now);

                    WorkflowAlertEvent workflowAlertEvent = workflowAlertEventService.create(
                        new WorkflowAlertEvent(rule.getId(), jobId, breach.triggeredValue(), breach.message()));

                    workflowAlertDispatcher.dispatch(rule, workflowAlertEvent);
                }

                // Rolling state (counters, EWMA, lastActivity) must persist even when nothing fired.
                workflowAlertRuleService.update(rule);
            }
        } catch (RuntimeException exception) {
            // Alerting must never affect the job lifecycle or the other event listeners.
            log.warn("Failed to evaluate workflow alert rules for job {}", jobId, exception);
        }
    }

    private @Nullable Long durationMs(Job job) {
        if (job.getStartDate() == null || job.getEndDate() == null) {
            return null;
        }

        return job.getEndDate()
            .toEpochMilli() -
            job.getStartDate()
                .toEpochMilli();
    }

    private @Nullable BigDecimal fetchTotalCost(List<WorkflowAlertRule> rules, long jobId) {
        boolean hasCostRule = rules.stream()
            .anyMatch(rule -> rule.getRuleType() == WorkflowAlertRuleType.COST_THRESHOLD);

        if (!hasCostRule) {
            return null;
        }

        WorkflowExecutionCostService workflowExecutionCostService =
            workflowExecutionCostServiceProvider.getIfAvailable();

        if (workflowExecutionCostService == null) {
            return null;
        }

        return workflowExecutionCostService.fetchByJobId(jobId)
            .map(WorkflowExecutionCost::getTotalCost)
            .orElse(null);
    }

    private @Nullable Long resolveWorkspaceId(long jobId) {
        return principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION)
            .map(projectDeploymentService::getProjectDeployment)
            .map(ProjectDeployment::getProjectId)
            .map(projectService::getProject)
            .map(project -> project.getWorkspaceId())
            .orElse(null);
    }
}
