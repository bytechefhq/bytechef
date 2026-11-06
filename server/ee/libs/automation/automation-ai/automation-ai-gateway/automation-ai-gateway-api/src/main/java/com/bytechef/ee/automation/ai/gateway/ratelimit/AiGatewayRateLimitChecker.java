/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayRateLimitChecker {

    void checkRateLimits(
        long workspaceId, @Nullable Long projectId, @Nullable String userId,
        @Nullable Map<String, String> customProperties);

    AiGatewayRateLimitResult checkOtlpIngest(long workspaceId);

    AiGatewayRateLimitResult checkWorkspaceRequest(long workspaceId, String endpointTag);
}
