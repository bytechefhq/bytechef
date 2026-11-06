/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.service;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRuleType;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface WorkflowAlertRuleService {

    WorkflowAlertRule createInWorkspace(WorkflowAlertRule workflowAlertRule, long workspaceId);

    void delete(long id);

    Optional<Long> fetchWorkspaceId(long workflowAlertRuleId);

    WorkflowAlertRule getWorkflowAlertRule(long id);

    List<WorkflowAlertRule> getWorkflowAlertRules(long workspaceId);

    List<WorkflowAlertRule> getWorkflowWorkflowAlertRules(String workflowId);

    List<Long> getWorkflowAlertRuleIds(long workspaceId);

    List<WorkflowAlertRule> getEnabledWorkflowAlertRules(long workspaceId);

    List<WorkflowAlertRule> getEnabledWorkflowAlertRules(WorkflowAlertRuleType ruleType);

    WorkflowAlertRule update(WorkflowAlertRule workflowAlertRule);
}
