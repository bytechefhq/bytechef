/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayRateLimitService {

    AiGatewayRateLimit create(AiGatewayRateLimit rateLimit);

    void delete(long id);

    Optional<AiGatewayRateLimit> fetchRateLimit(long id);

    List<AiGatewayRateLimit> getEnabledRateLimitsByWorkspaceId(long workspaceId);

    AiGatewayRateLimit getRateLimit(long id);

    List<AiGatewayRateLimit> getRateLimitsByWorkspaceId(long workspaceId);

    AiGatewayRateLimit update(AiGatewayRateLimit rateLimit);
}
