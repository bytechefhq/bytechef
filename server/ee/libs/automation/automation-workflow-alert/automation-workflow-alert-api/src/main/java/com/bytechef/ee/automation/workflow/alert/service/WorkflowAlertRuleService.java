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
 * Read/write service for {@link WorkflowAlertRule}. A rule belongs to at most one workspace, carried by the nullable
 * {@code workflow_alert_rule.workspace_id} column, and every workspace-scoped read here filters that column.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkflowAlertRuleService {

    WorkflowAlertRule createInWorkspace(WorkflowAlertRule workflowAlertRule, long workspaceId);

    void delete(long id);

    /**
     * The owning workspace of {@code workflowAlertRuleId}. Empty when the rule has no workspace or does not exist —
     * callers use it as a probe, so an unknown id does not throw.
     */
    Optional<Long> fetchWorkspaceId(long workflowAlertRuleId);

    WorkflowAlertRule getWorkflowAlertRule(long id);

    List<WorkflowAlertRule> getWorkflowAlertRules(long workspaceId);

    /**
     * Every rule scoped to {@code workflowId}. Used by the workflow-delete cascade; a rule left pointing at a deleted
     * workflow matches no run again and is silently dead.
     */
    List<WorkflowAlertRule> getWorkflowWorkflowAlertRules(String workflowId);

    List<Long> getWorkflowAlertRuleIds(long workspaceId);

    List<WorkflowAlertRule> getEnabledWorkflowAlertRules(long workspaceId);

    List<WorkflowAlertRule> getEnabledWorkflowAlertRules(WorkflowAlertRuleType ruleType);

    WorkflowAlertRule update(WorkflowAlertRule workflowAlertRule);
}
