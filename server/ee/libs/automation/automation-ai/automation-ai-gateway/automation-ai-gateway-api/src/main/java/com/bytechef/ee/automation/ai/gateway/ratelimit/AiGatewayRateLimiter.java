/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitResult;

/**
 * @version ee
 */
public interface AiGatewayRateLimiter {

    AiGatewayRateLimitResult tryAcquire(String key, int limit, int windowSeconds);

    void reset(String key);
}
