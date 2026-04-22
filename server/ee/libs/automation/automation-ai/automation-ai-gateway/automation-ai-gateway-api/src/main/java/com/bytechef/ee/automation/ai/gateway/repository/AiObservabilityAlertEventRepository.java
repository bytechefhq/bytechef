/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 */
public interface AiObservabilityAlertEventRepository extends ListCrudRepository<AiObservabilityAlertEvent, Long> {

    List<AiObservabilityAlertEvent> findAllByAlertRuleIdOrderByCreatedDateDesc(Long alertRuleId);

    Optional<AiObservabilityAlertEvent> findFirstByAlertRuleIdOrderByCreatedDateDesc(Long alertRuleId);

    List<AiObservabilityAlertEvent> findAllByAlertRuleIdAndCreatedDateAfter(Long alertRuleId, Instant after);

    void deleteAllByCreatedDateBefore(Instant date);

    /**
     * Workspace-scoped deletion. Alert events link to workspace via their alert rule; this subquery filters to events
     * whose rule belongs to the given workspace, preventing cross-tenant deletion when called in a per-workspace loop.
     */
    @Modifying
    @Query("""
        DELETE FROM ai_observability_alert_event
        WHERE created_date < :date
          AND alert_rule_id IN (SELECT id FROM ai_observability_alert_rule WHERE workspace_id = :workspaceId)
        """)
    void deleteAllByWorkspaceIdAndCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") Instant date);
}
