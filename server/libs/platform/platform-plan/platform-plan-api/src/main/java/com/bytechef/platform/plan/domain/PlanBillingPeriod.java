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

package com.bytechef.platform.plan.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * The single definition of the plan billing period: the current calendar month in UTC. Every consumer that compares
 * spend against {@link PlanLimits#includedMonthlyCostUsd} — the monthly-cost admission gate's spend provider and the
 * USAGE_THRESHOLD alert monitor — must derive the period start from here, so admission and alerting can never disagree
 * about which spend counts. A billing integration that aligns periods with external cycles should replace both
 * consumers together, not fork this definition.
 *
 * @author Ivica Cardic
 */
public final class PlanBillingPeriod {

    private PlanBillingPeriod() {
    }

    /** Start of the current billing period: the first day of the current month at midnight UTC. */
    public static Instant currentPeriodStart() {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);

        return currentMonth.atDay(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant();
    }
}
