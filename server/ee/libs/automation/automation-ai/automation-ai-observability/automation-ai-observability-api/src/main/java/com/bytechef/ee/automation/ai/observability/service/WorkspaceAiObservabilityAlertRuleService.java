/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import java.util.List;

/**
 * Workspace-scoped operations on {@link AiObservabilityAlertRule}. Owns the rule's {@code workspace_id} binding;
 * entity-table CRUD belongs to the platform-side {@code AiObservabilityAlertRuleService}.
 *
 * @version ee
 */
public interface WorkspaceAiObservabilityAlertRuleService {

    AiObservabilityAlertRule createInWorkspace(AiObservabilityAlertRule alertRule, long workspaceId);

    void delete(long id);

    List<AiObservabilityAlertRule> getAlertRulesByWorkspace(Long workspaceId);

    Long getWorkspaceId(long alertRuleId);
}
