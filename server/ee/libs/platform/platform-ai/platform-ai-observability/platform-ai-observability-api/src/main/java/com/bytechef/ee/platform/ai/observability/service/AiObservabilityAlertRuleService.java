/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import java.time.Instant;
import java.util.List;

/**
 * Workspace-agnostic CRUD + lifecycle for {@link AiObservabilityAlertRule}. The workspace association is the rule's
 * nullable {@code workspace_id} column; see automation-side {@code WorkspaceAiObservabilityAlertRuleService} for
 * {@code createInWorkspace} / {@code getWorkspaceId} / {@code getAlertRulesByWorkspace}.
 *
 * @version ee
 */
public interface AiObservabilityAlertRuleService {

    AiObservabilityAlertRule create(AiObservabilityAlertRule alertRule);

    void delete(long id);

    AiObservabilityAlertRule getAlertRule(long id);

    List<AiObservabilityAlertRule> getEnabledAlertRules();

    AiObservabilityAlertRule snooze(long id, Instant until);

    AiObservabilityAlertRule unsnooze(long id);

    AiObservabilityAlertRule update(AiObservabilityAlertRule alertRule);
}
