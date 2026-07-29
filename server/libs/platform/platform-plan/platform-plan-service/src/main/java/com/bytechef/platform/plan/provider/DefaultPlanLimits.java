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

package com.bytechef.platform.plan.provider;

import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanTier;
import java.math.BigDecimal;
import java.time.Duration;

/**
 * Placeholder per-tier limit tables, modeled on Sim.ai's published plan limits (docs.sim.ai/platform/costs,
 * sim.ai/pricing; verified against the open-source simstudioai/sim rate-limiter and billing constants, July 2026).
 * Credit amounts are converted to USD at Sim's rate of $0.005/credit. The numbers are intentionally provisional —
 * commercial packaging will replace them; enforcement code must only ever read them through
 * {@link com.bytechef.platform.plan.provider.PlanLimitsProvider}.
 *
 * @author Ivica Cardic
 */
public final class DefaultPlanLimits {

    private static final Duration SYNC_RUN_TIMEOUT_FREE = Duration.ofMinutes(5);
    private static final Duration SYNC_RUN_TIMEOUT_PAID = Duration.ofMinutes(50);
    private static final Duration ASYNC_RUN_TIMEOUT = Duration.ofMinutes(90);

    private static final long GIB = 1024L * 1024L * 1024L;

    private DefaultPlanLimits() {
    }

    public static PlanLimits forTier(PlanTier planTier) {
        return switch (planTier) {
            case SELF_HOSTED -> PlanLimits.unlimited(planTier);
            case FREE -> new PlanLimits(
                planTier, new BigDecimal("5.00"), 50, 200, 30, PlanLimits.DEFAULT_BURST_MULTIPLIER, 10,
                SYNC_RUN_TIMEOUT_FREE, ASYNC_RUN_TIMEOUT, 1, 5L * GIB, 7, 1);
            case PRO -> new PlanLimits(
                planTier, new BigDecimal("30.00"), 150, 1000, 100, PlanLimits.DEFAULT_BURST_MULTIPLIER, 50,
                SYNC_RUN_TIMEOUT_PAID, ASYNC_RUN_TIMEOUT, 3, 50L * GIB, null, 1);
            case TEAM -> new PlanLimits(
                planTier, new BigDecimal("125.00"), 300, 2500, 200, PlanLimits.DEFAULT_BURST_MULTIPLIER, 200,
                SYNC_RUN_TIMEOUT_PAID, ASYNC_RUN_TIMEOUT, 10, 500L * GIB, null, null);
            case ENTERPRISE -> new PlanLimits(
                planTier, null, 600, 5000, 500, PlanLimits.DEFAULT_BURST_MULTIPLIER, 1000,
                SYNC_RUN_TIMEOUT_PAID, ASYNC_RUN_TIMEOUT, null, 500L * GIB, null, null);
        };
    }
}
