/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayRateLimitService {

    AiGatewayRateLimit create(AiGatewayRateLimit rateLimit);

    void delete(long id);

    AiGatewayRateLimit getRateLimit(long id);

    List<AiGatewayRateLimit> getRateLimitsByWorkspaceId(long workspaceId);

    List<AiGatewayRateLimit> getEnabledRateLimitsByWorkspaceId(long workspaceId);

    AiGatewayRateLimit update(AiGatewayRateLimit rateLimit);
}
