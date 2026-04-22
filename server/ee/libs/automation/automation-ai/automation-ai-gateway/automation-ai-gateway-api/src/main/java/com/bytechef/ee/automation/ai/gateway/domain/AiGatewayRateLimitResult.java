/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

/**
 * @version ee
 */
public record AiGatewayRateLimitResult(boolean allowed, int remaining, long resetAtEpochMs) {

    public static AiGatewayRateLimitResult allowed(int remaining, long resetAtEpochMs) {
        return new AiGatewayRateLimitResult(true, remaining, resetAtEpochMs);
    }

    public static AiGatewayRateLimitResult rejected(int remaining, long resetAtEpochMs) {
        return new AiGatewayRateLimitResult(false, remaining, resetAtEpochMs);
    }
}
