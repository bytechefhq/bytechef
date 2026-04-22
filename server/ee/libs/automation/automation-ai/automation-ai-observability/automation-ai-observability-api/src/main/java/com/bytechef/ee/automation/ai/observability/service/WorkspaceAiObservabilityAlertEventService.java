/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import java.time.Instant;

/**
 * Workspace-scoped retention deletion for
 * {@link com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertEvent}. Alert events link to a workspace
 * via their parent alert rule; this delete subqueries the rule table to prevent cross-tenant deletion.
 *
 * @version ee
 */
public interface WorkspaceAiObservabilityAlertEventService {

    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);
}
