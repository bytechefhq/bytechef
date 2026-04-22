/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import java.time.Instant;
import java.util.List;

/**
 * Facade for querying AI LLM Gateway spend summaries. Hosts the authorization guard so it applies to every caller of
 * the facade rather than only the GraphQL entry point, and keeps it off the shared {@code AiGatewaySpendService} which
 * the gateway data plane relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewaySpendFacade {

    List<AiGatewaySpendSummary> getSpendSummaries(Instant startDate, Instant endDate);
}
