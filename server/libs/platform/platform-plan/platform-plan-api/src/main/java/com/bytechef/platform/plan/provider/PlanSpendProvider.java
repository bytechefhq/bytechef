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

import java.math.BigDecimal;

/**
 * Resolves a tenant's execution spend for the current billing period, so admission gates can compare it against
 * {@link com.bytechef.platform.plan.domain.PlanLimits#includedMonthlyCostUsd}. The billing period is the current
 * calendar month in UTC unless an implementation aligns it with an external billing cycle.
 *
 * <p>
 * There is no default implementation: without a bean, the monthly-cost admission gate is a no-op (the pre-plan
 * behavior, matching how absent rate-limiter or plan-limits beans behave). The EE cost module contributes an
 * implementation backed by the per-execution cost rows; implementations should cache briefly — admission runs on every
 * async job submission, and a spend total that lags by a minute is an acceptable trade for not running a SUM per
 * submission.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface PlanSpendProvider {

    /**
     * Total execution spend (base run charges + AI usage, USD) accrued by the tenant in the current billing period.
     * Never returns null; a tenant without cost data reports {@link BigDecimal#ZERO}.
     */
    BigDecimal getCurrentPeriodSpendUsd(String tenantId);
}
