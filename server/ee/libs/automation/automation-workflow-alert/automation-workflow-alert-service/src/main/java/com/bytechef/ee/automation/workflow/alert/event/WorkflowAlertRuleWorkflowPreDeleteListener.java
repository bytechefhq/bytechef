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
 * @version ee
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
