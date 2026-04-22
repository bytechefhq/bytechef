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

package com.bytechef.ee.platform.ai.tool.usage;

import java.math.BigDecimal;

/**
 * Computes the estimated USD cost of an expensive tool call (driven by per-tool rates). Implementations read rates from
 * configuration and return {@link BigDecimal#ZERO} for unknown tools rather than failing the request.
 *
 * <p>
 * LLM-side cost estimation lives outside this contract — agent surfaces that need it are expected to keep their own
 * model-rate logic close to where they record LLM rows. Splitting the two contracts means a deployment that only runs
 * tool-using agents (no LLM bookkeeping of its own) doesn't need to depend on an LLM-rate table at all.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface ToolCostEstimator {

    /**
     * Returns the USD cost of an expensive tool call given the tool name and unit count (typically {@code 1}).
     * Implementations should return {@link BigDecimal#ZERO} (not throw) for unknown tools so the recording path stays
     * robust.
     */
    BigDecimal estimateToolCost(String toolName, int unitCount);
}
