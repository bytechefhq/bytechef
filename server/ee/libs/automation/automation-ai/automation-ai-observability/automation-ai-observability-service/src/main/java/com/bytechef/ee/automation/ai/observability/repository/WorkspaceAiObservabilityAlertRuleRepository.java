/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.repository;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiObservabilityAlertRuleRepository
    extends ListCrudRepository<WorkspaceAiObservabilityAlertRule, Long> {

    Optional<WorkspaceAiObservabilityAlertRule> findByAiObservabilityAlertRuleId(long aiObservabilityAlertRuleId);

    @Query("""
        SELECT ai_observability_alert_rule.*
        FROM ai_observability_alert_rule
        JOIN workspace_ai_observability_alert_rule
            ON workspace_ai_observability_alert_rule.ai_observability_alert_rule_id = ai_observability_alert_rule.id
        WHERE workspace_ai_observability_alert_rule.workspace_id = :workspaceId
        """)
    List<AiObservabilityAlertRule> findAllAlertRulesByWorkspaceId(@Param("workspaceId") Long workspaceId);

    /**
     * Workspace-scoped deletion of alert events. Alert events have no direct workspace column; they're scoped via their
     * parent alert rule's membership row in {@code workspace_ai_observability_alert_rule}. Lives here (rather than on
     * the alert-event repo) because that's where the workspace JOIN lives.
     */
    @org.springframework.data.jdbc.repository.query.Modifying
    @Query("""
        DELETE FROM ai_observability_alert_event
        WHERE created_date < :date
          AND alert_rule_id IN (
            SELECT ai_observability_alert_rule_id
            FROM workspace_ai_observability_alert_rule
            WHERE workspace_id = :workspaceId)
        """)
    void deleteAllAlertEventsByWorkspaceIdAndCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") java.time.Instant date);
}
