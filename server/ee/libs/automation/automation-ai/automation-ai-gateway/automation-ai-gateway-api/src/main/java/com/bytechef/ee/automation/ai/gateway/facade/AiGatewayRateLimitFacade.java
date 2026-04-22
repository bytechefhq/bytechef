/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;
import java.util.List;

/**
 * Facade for managing AI Gateway rate limits. Hosts the authorization guards so they apply to every caller of the
 * facade rather than only the GraphQL entry point, and keeps them off the shared services the gateway data plane relies
 * on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayRateLimitFacade {

    List<AiGatewayRateLimit> getRateLimitsByWorkspaceId(long workspaceId);

    AiGatewayRateLimit getRateLimit(long id);

    AiGatewayRateLimit createInWorkspace(AiGatewayRateLimit rateLimit, long workspaceId);

    void delete(long id);

    AiGatewayRateLimit update(AiGatewayRateLimit rateLimit);
}
