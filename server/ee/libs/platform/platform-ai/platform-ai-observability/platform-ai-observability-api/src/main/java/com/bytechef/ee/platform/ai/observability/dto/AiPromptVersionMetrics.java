/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.dto;

import java.math.BigDecimal;

/**
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
