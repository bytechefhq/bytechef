/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published when an {@code AiObservabilityTrace} reaches a terminal state (COMPLETED or ERROR). Downstream listeners
 * (webhook delivery, telemetry) fan this out to subscribers.
 *
 * @version ee
 */
public record AiGatewayTraceCompletedEvent(
    Long workspaceId,
    Long traceId,
    String externalTraceId,
    String modelName,
    Integer totalInputTokens,
    Integer totalOutputTokens,
    Integer totalLatencyMs,
    BigDecimal totalCost,
    boolean success,
    Instant completedAt) {
}
