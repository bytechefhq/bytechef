/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage.config;

import com.bytechef.ee.platform.ai.llm.usage.LlmCostEstimator;
import java.math.BigDecimal;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class NoopLlmCostEstimator implements LlmCostEstimator {

    @Override
    public BigDecimal estimateLlmCost(String model, int inputTokens, int outputTokens) {
        return BigDecimal.ZERO;
    }
}
