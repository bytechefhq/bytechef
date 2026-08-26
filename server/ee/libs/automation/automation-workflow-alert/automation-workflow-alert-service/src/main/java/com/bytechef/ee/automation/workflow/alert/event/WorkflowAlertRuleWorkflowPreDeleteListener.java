/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.event;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.service.WorkflowAlertRuleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deletes the alert rules scoped to a workflow when that workflow is deleted.
 *
 * <p>
 * {@code workflow_alert_rule.workflow_id} is an optional scope on a nullable column, so nothing at the database level
 * stops a rule from outliving its workflow. A rule left behind is not merely untidy: the evaluator skips any rule whose
 * {@code workflowId} does not equal the finished job's, so a rule pointing at a deleted workflow matches nothing ever
 * again — it sits in the workspace's rule list looking active while being permanently dead.
 * </p>
 *
 * <p>
 * <b>Deleted rather than widened to workspace scope.</b> Nulling the column would be the other repair, and it is the
 * wrong one: {@code workflowId == null} means "every run in the workspace", so a rule the user deliberately narrowed to
 * one workflow would silently start alerting on every unrelated run in the workspace. A rule whose subject is gone has
 * no correct scope left, and removing it is the only outcome that cannot surprise anyone.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkflowAlertRuleWorkflowPreDeleteListener implements WorkflowPreDeleteListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowAlertRuleWorkflowPreDeleteListener.class);

    private final WorkflowAlertRuleService workflowAlertRuleService;

    @SuppressFBWarnings("EI")
    public WorkflowAlertRuleWorkflowPreDeleteListener(WorkflowAlertRuleService workflowAlertRuleService) {
        this.workflowAlertRuleService = workflowAlertRuleService;
    }

    @Override
    public void onWorkflowPreDelete(String workflowId) {
        List<WorkflowAlertRule> workflowAlertRules = workflowAlertRuleService.getWorkflowWorkflowAlertRules(workflowId);

        if (workflowAlertRules.isEmpty()) {
            return;
        }

        log.debug("Deleting {} alert rule(s) scoped to workflow {}", workflowAlertRules.size(), workflowId);

        for (WorkflowAlertRule workflowAlertRule : workflowAlertRules) {
            workflowAlertRuleService.delete(Objects.requireNonNull(workflowAlertRule.getId(), "id"));
        }
    }
}
