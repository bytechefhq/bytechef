/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import java.math.BigDecimal;

/**
 * Aggregated observability metrics for a single prompt version, computed over all spans that recorded usage of it.
 *
 * @param invocationCount number of span rows referencing this version
 * @param avgLatencyMs    arithmetic mean of non-null span {@code latencyMs}
 * @param avgCostUsd      arithmetic mean of non-null span {@code cost}
 * @param errorRate       fraction (0..1) of spans with status=ERROR
 * @version ee
 */
public record AiPromptVersionMetrics(
    long invocationCount,
    Double avgLatencyMs,
    BigDecimal avgCostUsd,
    Double errorRate) {

    public static AiPromptVersionMetrics empty() {
        return new AiPromptVersionMetrics(0, null, null, null);
    }
}
