/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.listener;

import com.bytechef.ee.automation.workflow.alert.dispatcher.WorkflowAlertDispatcher;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertEvent;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRuleType;
import com.bytechef.ee.automation.workflow.alert.evaluator.WorkflowAlertEvaluator;
import com.bytechef.ee.automation.workflow.alert.evaluator.WorkflowAlertEvaluator.Breach;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertEventService;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertRuleService;
import com.bytechef.ee.automation.workflow.execution.cost.service.WorkspaceWorkflowExecutionCostService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.plan.domain.PlanBillingPeriod;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly side of the USAGE_THRESHOLD rule type — Sim's budget email folded into the alert-rule engine. Compares each
 * workspace's month-to-date execution spend (sum of {@code workflow_execution_cost} rows attached via the workspace
 * membership table) against the plan's {@code includedMonthlyCostUsd} from {@code PlanLimitsProvider}; fires when the
 * usage percentage reaches the rule's threshold, then respects the rule's cooldown — so a workspace sitting above the
 * threshold re-alerts once per cooldown period, not once per poll. Unlimited plans (no ceiling) never fire.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class WorkflowAlertUsageMonitor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowAlertUsageMonitor.class);

    private final ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
    private final TenantService tenantService;
    private final WorkflowAlertDispatcher workflowAlertDispatcher;
    private final WorkflowAlertEventService workflowAlertEventService;
    private final WorkflowAlertRuleService workflowAlertRuleService;
    private final ObjectProvider<WorkspaceWorkflowExecutionCostService> workspaceWorkflowExecutionCostServiceProvider;

    public WorkflowAlertUsageMonitor(
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider, TenantService tenantService,
        WorkflowAlertDispatcher workflowAlertDispatcher, WorkflowAlertEventService workflowAlertEventService,
        WorkflowAlertRuleService workflowAlertRuleService,
        ObjectProvider<WorkspaceWorkflowExecutionCostService> workspaceWorkflowExecutionCostServiceProvider) {

        this.planLimitsProviderObjectProvider = planLimitsProviderObjectProvider;
        this.tenantService = tenantService;
        this.workflowAlertDispatcher = workflowAlertDispatcher;
        this.workflowAlertEventService = workflowAlertEventService;
        this.workflowAlertRuleService = workflowAlertRuleService;
        this.workspaceWorkflowExecutionCostServiceProvider = workspaceWorkflowExecutionCostServiceProvider;
    }

    /** Sweeps every tenant under its own context — rules, workspaces, and plan limits are all tenant-scoped. */
    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT1H")
    public void checkUsage() {
        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::checkUsageForCurrentTenant);
            } catch (RuntimeException exception) {
                log.warn("Usage-threshold sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private void checkUsageForCurrentTenant() {
        PlanLimitsProvider planLimitsProvider = planLimitsProviderObjectProvider.getIfAvailable();
        WorkspaceWorkflowExecutionCostService workspaceWorkflowExecutionCostService =
            workspaceWorkflowExecutionCostServiceProvider.getIfAvailable();

        if (planLimitsProvider == null || workspaceWorkflowExecutionCostService == null) {
            return;
        }

        PlanLimits planLimits = planLimitsProvider.getPlanLimits(TenantContext.getCurrentTenantId());

        BigDecimal includedMonthlyCostUsd = planLimits.includedMonthlyCostUsd();

        if (includedMonthlyCostUsd == null) {
            // Unlimited plan — USAGE_THRESHOLD rules can never fire.
            return;
        }

        Instant now = Instant.now();

        // Single source of truth for the billing period, shared with the monthly-cost admission gate.
        Instant monthStart = PlanBillingPeriod.currentPeriodStart();

        for (WorkflowAlertRule rule : workflowAlertRuleService.getEnabledWorkflowAlertRules(
            WorkflowAlertRuleType.USAGE_THRESHOLD)) {

            try {
                Long workspaceId = workflowAlertRuleService.fetchWorkspaceId(rule.getId())
                    .orElse(null);

                if (workspaceId == null) {
                    continue;
                }

                BigDecimal monthToDateSpend = workspaceWorkflowExecutionCostService.sumTotalCostByWorkspaceSince(
                    workspaceId, monthStart);

                Breach breach = WorkflowAlertEvaluator.evaluateUsageThreshold(
                    rule, monthToDateSpend, includedMonthlyCostUsd);

                if (breach == null || !WorkflowAlertEvaluator.isCooldownElapsed(rule, now)) {
                    continue;
                }

                rule.setLastTriggeredDate(now);

                WorkflowAlertEvent workflowAlertEvent = workflowAlertEventService.create(
                    new WorkflowAlertEvent(rule.getId(), null, breach.triggeredValue(), breach.message()));

                workflowAlertRuleService.update(rule);

                workflowAlertDispatcher.dispatch(rule, workflowAlertEvent);
            } catch (RuntimeException exception) {
                // One broken rule must not stop the sweep for the rest.
                log.warn("Failed to evaluate USAGE_THRESHOLD alert rule {}", rule.getId(), exception);
            }
        }
    }
}
