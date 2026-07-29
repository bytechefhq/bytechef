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

package com.bytechef.platform.plan.config;

import com.bytechef.platform.plan.domain.PlanTier;
import java.math.BigDecimal;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Static plan configuration for deployments without a billing integration. The default tier {@code SELF_HOSTED} keeps
 * every limit unlimited (the pre-plan behavior); setting {@code bytechef.plan.tier} activates the placeholder limit
 * table in {@code DefaultPlanLimits}, and any individual number can be pinned via {@code bytechef.plan.limits.*}.
 *
 * @param tier   the deployment-wide plan tier
 * @param limits per-field overrides applied on top of the tier's default table; null = keep the tier default
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.plan")
public record PlanProperties(@DefaultValue("SELF_HOSTED") PlanTier tier, @DefaultValue Limits limits) {

    /**
     * Mirror of the {@code PlanLimits} fields as optional overrides. All nullable; a set value replaces the tier
     * default, including the ability to lift a limit by not being settable to "unlimited" here — use a higher tier for
     * that instead.
     */
    public record Limits(
        @Nullable BigDecimal includedMonthlyCostUsd,
        @Nullable Integer syncRequestsPerMinute,
        @Nullable Integer asyncRequestsPerMinute,
        @Nullable Integer apiRequestsPerMinute,
        @Nullable Integer burstMultiplier,
        @Nullable Integer maxConcurrentExecutions,
        @Nullable Duration syncRunTimeout,
        @Nullable Duration asyncRunTimeout,
        @Nullable Integer maxWorkspaces,
        @Nullable Long maxStorageBytes,
        @Nullable Integer logRetentionDays,
        @Nullable Integer maxMembers) {
    }
}
