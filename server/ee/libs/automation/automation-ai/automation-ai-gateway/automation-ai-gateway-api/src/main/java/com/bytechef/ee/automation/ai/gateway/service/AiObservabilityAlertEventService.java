/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiObservabilityAlertEventService {

    AiObservabilityAlertEvent create(AiObservabilityAlertEvent alertEvent);

    void deleteOlderThan(Instant date);

    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);

    AiObservabilityAlertEvent getAlertEvent(Long id);

    List<AiObservabilityAlertEvent> getAlertEventsByRule(Long alertRuleId);

    Optional<AiObservabilityAlertEvent> getLatestEventByRule(Long alertRuleId);

    List<AiObservabilityAlertEvent> getAlertEventsByRuleAfter(Long alertRuleId, Instant after);

    AiObservabilityAlertEvent update(AiObservabilityAlertEvent alertEvent);
}
