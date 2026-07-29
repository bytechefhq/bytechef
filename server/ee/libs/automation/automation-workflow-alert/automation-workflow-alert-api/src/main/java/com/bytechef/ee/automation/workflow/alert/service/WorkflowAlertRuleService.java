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
 * Workspace resolution goes through the {@code workspace_workflow_alert_rule} membership table — workspace is an
 * automation-configuration-owned concept, so rules carry no workspace column of their own.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkflowAlertRuleService {

    WorkflowAlertRule createInWorkspace(WorkflowAlertRule workflowAlertRule, long workspaceId);

    void delete(long id);

    Optional<Long> fetchWorkspaceId(long workflowAlertRuleId);

    WorkflowAlertRule getWorkflowAlertRule(long id);

    List<WorkflowAlertRule> getWorkflowAlertRules(long workspaceId);

    List<Long> getWorkflowAlertRuleIds(long workspaceId);

    List<WorkflowAlertRule> getEnabledWorkflowAlertRules(long workspaceId);

    List<WorkflowAlertRule> getEnabledWorkflowAlertRules(WorkflowAlertRuleType ruleType);

    WorkflowAlertRule update(WorkflowAlertRule workflowAlertRule);
}
