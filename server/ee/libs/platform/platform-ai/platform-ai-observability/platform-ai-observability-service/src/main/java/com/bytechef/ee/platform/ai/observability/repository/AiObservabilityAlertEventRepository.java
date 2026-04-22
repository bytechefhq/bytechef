/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.repository;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Workspace-agnostic alert-event queries. Per-workspace retention deletes (which need to JOIN to
 * workspace_ai_observability_alert_rule) live on the automation-side
 * {@code WorkspaceAiObservabilityAlertRuleRepository}.
 *
 * @version ee
 */
public interface AiObservabilityAlertEventRepository extends ListCrudRepository<AiObservabilityAlertEvent, Long> {

    List<AiObservabilityAlertEvent> findAllByAlertRuleIdOrderByCreatedDateDesc(Long alertRuleId);

    Optional<AiObservabilityAlertEvent> findFirstByAlertRuleIdOrderByCreatedDateDesc(Long alertRuleId);

    List<AiObservabilityAlertEvent> findAllByAlertRuleIdAndCreatedDateAfterOrderByCreatedDateDesc(
        Long alertRuleId, Instant after);

    void deleteAllByCreatedDateBefore(Instant date);
}
