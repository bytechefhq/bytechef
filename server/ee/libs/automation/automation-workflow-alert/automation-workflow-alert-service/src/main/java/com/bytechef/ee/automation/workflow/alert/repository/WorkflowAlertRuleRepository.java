/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.repository;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Workspace-scoped reads filter {@code workflow_alert_rule.workspace_id} directly; a workspace-less rule (null column)
 * is invisible to them, which is the intended behavior.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface WorkflowAlertRuleRepository extends ListCrudRepository<WorkflowAlertRule, Long> {

    List<WorkflowAlertRule> findAllByRuleTypeAndEnabledTrue(int ruleType);

    List<WorkflowAlertRule> findAllByWorkflowId(String workflowId);

    List<WorkflowAlertRule> findAllByWorkspaceId(long workspaceId);

    /**
     * Ids only, for callers that just need the workspace's rule set without materialising each rule and its
     * notification collection.
     */
    @Query("SELECT id FROM workflow_alert_rule WHERE workspace_id = :workspaceId")
    List<Long> findAllIdsByWorkspaceId(@Param("workspaceId") long workspaceId);
}
