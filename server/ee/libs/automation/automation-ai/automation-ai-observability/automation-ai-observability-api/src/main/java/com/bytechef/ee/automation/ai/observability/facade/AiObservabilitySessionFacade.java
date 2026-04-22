/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import java.util.List;

/**
 * Facade for querying AI Observability sessions by workspace. Hosts the workspace-role authorization guard so it
 * applies to every caller of the facade rather than only the GraphQL entry point, and keeps it off the shared
 * {@code WorkspaceAiObservabilitySessionService} which the export executor relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiObservabilitySessionFacade {

    List<AiObservabilitySession> getSessionsByWorkspace(Long workspaceId);
}
