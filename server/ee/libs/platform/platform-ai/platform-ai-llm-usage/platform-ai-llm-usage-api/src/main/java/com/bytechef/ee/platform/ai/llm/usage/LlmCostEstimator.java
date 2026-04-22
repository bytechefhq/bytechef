/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.platform.ai.llm.usage;

import java.math.BigDecimal;

/**
 * Computes the estimated USD cost of an LLM call given the model and token counts. Implementations read rates from
 * configuration and return {@link BigDecimal#ZERO} for unknown models rather than failing the request — the cost is
 * bookkeeping; the user request must succeed even if a rate is missing.
 *
 * <p>
 * One implementation lives close to whichever rate sheet drives the deployment. The AI Hub surface ships its own rate
 * properties; the AI Gateway computes cost upstream (provider receipts) and can pass the resolved cost into the
 * recorder directly via the {@link LlmUsageContext}-driven write path. The platform default registered by
 * {@code platform-ai-llm-usage-service} returns ZERO so deployments without a rate sheet still record call counts and
 * token totals.
 * </p>
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
