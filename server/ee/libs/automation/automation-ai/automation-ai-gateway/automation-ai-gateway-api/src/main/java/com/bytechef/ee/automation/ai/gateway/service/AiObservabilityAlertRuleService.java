/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityAlertRuleService {

    AiObservabilityAlertRule create(AiObservabilityAlertRule alertRule);

    void delete(long id);

    AiObservabilityAlertRule getAlertRule(long id);

    List<AiObservabilityAlertRule> getAlertRulesByWorkspace(Long workspaceId);

    List<AiObservabilityAlertRule> getEnabledAlertRules();

    AiObservabilityAlertRule snooze(long id, Instant until);

    AiObservabilityAlertRule unsnooze(long id);

    AiObservabilityAlertRule update(AiObservabilityAlertRule alertRule);
}
