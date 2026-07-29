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

import com.bytechef.platform.plan.config.PlanProperties;
import com.bytechef.platform.plan.domain.PlanLimits;

/**
 * Default {@link PlanLimitsProvider}: one static tier for the whole deployment, resolved from
 * {@code bytechef.plan.tier} with per-field {@code bytechef.plan.limits.*} overrides. The tenant id is deliberately
 * ignored here — per-tenant tiers come from a billing-backed provider that replaces this bean.
 *
 * @author Ivica Cardic
 */
public class PropertiesPlanLimitsProvider implements PlanLimitsProvider {

    private final PlanLimits planLimits;

    public PropertiesPlanLimitsProvider(PlanProperties planProperties) {
        this.planLimits = applyOverrides(DefaultPlanLimits.forTier(planProperties.tier()), planProperties.limits());
    }

    @Override
    public PlanLimits getPlanLimits(String tenantId) {
        return planLimits;
    }

    private static PlanLimits applyOverrides(PlanLimits tierDefaults, PlanProperties.Limits overrides) {
        if (overrides == null) {
            return tierDefaults;
        }

        return new PlanLimits(
            tierDefaults.tier(),
            overrides.includedMonthlyCostUsd() != null
                ? overrides.includedMonthlyCostUsd() : tierDefaults.includedMonthlyCostUsd(),
            overrides.syncRequestsPerMinute() != null
                ? overrides.syncRequestsPerMinute() : tierDefaults.syncRequestsPerMinute(),
            overrides.asyncRequestsPerMinute() != null
                ? overrides.asyncRequestsPerMinute() : tierDefaults.asyncRequestsPerMinute(),
            overrides.apiRequestsPerMinute() != null
                ? overrides.apiRequestsPerMinute() : tierDefaults.apiRequestsPerMinute(),
            overrides.burstMultiplier() != null ? overrides.burstMultiplier() : tierDefaults.burstMultiplier(),
            overrides.maxConcurrentExecutions() != null
                ? overrides.maxConcurrentExecutions() : tierDefaults.maxConcurrentExecutions(),
            overrides.syncRunTimeout() != null ? overrides.syncRunTimeout() : tierDefaults.syncRunTimeout(),
            overrides.asyncRunTimeout() != null ? overrides.asyncRunTimeout() : tierDefaults.asyncRunTimeout(),
            overrides.maxWorkspaces() != null ? overrides.maxWorkspaces() : tierDefaults.maxWorkspaces(),
            overrides.maxStorageBytes() != null ? overrides.maxStorageBytes() : tierDefaults.maxStorageBytes(),
            overrides.logRetentionDays() != null ? overrides.logRetentionDays() : tierDefaults.logRetentionDays(),
            overrides.maxMembers() != null ? overrides.maxMembers() : tierDefaults.maxMembers());
    }
}
