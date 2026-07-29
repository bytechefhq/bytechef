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
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled side of the NO_ACTIVITY rule type: terminal-run events only refresh {@code lastActivityDate}; this monitor
 * is what notices silence. Fires when the silence exceeds the rule's window, then goes quiet for the rule's cooldown —
 * i.e. an ongoing outage re-alerts once per cooldown period rather than on every poll tick.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class WorkflowAlertNoActivityMonitor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowAlertNoActivityMonitor.class);

    private final TenantService tenantService;
    private final WorkflowAlertDispatcher workflowAlertDispatcher;
    private final WorkflowAlertEventService workflowAlertEventService;
    private final WorkflowAlertRuleService workflowAlertRuleService;

    public WorkflowAlertNoActivityMonitor(
        TenantService tenantService, WorkflowAlertDispatcher workflowAlertDispatcher,
        WorkflowAlertEventService workflowAlertEventService, WorkflowAlertRuleService workflowAlertRuleService) {

        this.tenantService = tenantService;
        this.workflowAlertDispatcher = workflowAlertDispatcher;
        this.workflowAlertEventService = workflowAlertEventService;
        this.workflowAlertRuleService = workflowAlertRuleService;
    }

    /**
     * Sweeps every tenant under its own context: alert rules and their evaluation state are tenant-scoped, so a sweep
     * under only the scheduler thread's default tenant would never fire NO_ACTIVITY for other tenants.
     */
    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT5M")
    public void checkNoActivity() {
        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::checkNoActivityForCurrentTenant);
            } catch (RuntimeException exception) {
                log.warn("No-activity sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private void checkNoActivityForCurrentTenant() {
        Instant now = Instant.now();

        for (WorkflowAlertRule rule : workflowAlertRuleService.getEnabledWorkflowAlertRules(
            WorkflowAlertRuleType.NO_ACTIVITY)) {

            try {
                Breach breach = WorkflowAlertEvaluator.evaluateNoActivity(rule, now);

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
                log.warn("Failed to evaluate NO_ACTIVITY alert rule {}", rule.getId(), exception);
            }
        }
    }
}
