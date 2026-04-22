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

package com.bytechef.ee.platform.ai.tool.usage.config;

import com.bytechef.ee.platform.ai.tool.usage.ToolCostEstimator;
import java.math.BigDecimal;

/**
 * Default fallback estimator that returns {@link BigDecimal#ZERO} for every tool. Lets a deployment record tool call
 * counts (and durations, and call-site metadata) even when no rate sheet is configured. Agent surfaces with a concrete
 * rate table register their own {@link ToolCostEstimator} bean to override this default.
 *
 * @author Ivica Cardic
 */
final class NoopToolCostEstimator implements ToolCostEstimator {

    @Override
    public BigDecimal estimateToolCost(String toolName, int unitCount) {
        return BigDecimal.ZERO;
    }
}
