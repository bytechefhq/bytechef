/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import java.time.Instant;
import java.util.List;

/**
 * Facade for querying AI LLM Gateway request logs. Hosts the authorization guards so they apply to every caller of the
 * facade rather than only the GraphQL entry point, and keeps them off the shared {@code AiLlmUsageService} which the
 * gateway data plane relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayRequestLogFacade {

    List<AiLlmUsage> getRequestLogs(Instant startDate, Instant endDate);

    List<AiLlmUsage> getRequestLogsByWorkspace(Long workspaceId, Instant startDate, Instant endDate);
}
