/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage;

import java.math.BigDecimal;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface LlmCostEstimator {

    /**
     * Returns the USD cost of an LLM call given the model name and token counts. Implementations should return
     * {@link BigDecimal#ZERO} (not throw) for unknown models so the recording path stays robust.
     */
    BigDecimal estimateLlmCost(String model, int inputTokens, int outputTokens);
}
