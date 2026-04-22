/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimit;

/**
 * @version ee
 */
public interface AiGatewayRateLimitService {

    AiGatewayRateLimit create(AiGatewayRateLimit rateLimit);

    void delete(long id);

    AiGatewayRateLimit getRateLimit(long id);

    AiGatewayRateLimit update(AiGatewayRateLimit rateLimit);
}
