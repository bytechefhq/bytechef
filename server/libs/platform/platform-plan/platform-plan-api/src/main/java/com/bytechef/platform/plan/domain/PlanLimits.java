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

import java.math.BigDecimal;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * The numeric limits attached to a {@link PlanTier}. Every limit field is nullable, and {@code null} always means
 * <b>unlimited</b> — enforcement code must treat a null as "no check", never as zero.
 *
 * <p>
 * These are policy numbers only: nothing in this module enforces them. Enforcement points (HTTP rate-limit filter, job
 * admission gate, concurrency slots, storage/retention checks) inject {@code PlanLimitsProvider} and compare against
 * the fields relevant to them. See {@code docs/superpowers/specs/2026-07-20-plan-limits-cost-alerts-design.md}.
 * </p>
 *
 * @param tier                    the tier these limits belong to
 * @param includedMonthlyCostUsd  execution spend included per month (base run charges + AI usage) before runs are
 *                                blocked or overage billing kicks in
 * @param syncRequestsPerMinute   sustained per-minute rate for synchronous workflow executions (API sync + UI runs)
 * @param asyncRequestsPerMinute  sustained per-minute rate for asynchronous workflow submissions (webhooks, schedules,
 *                                API async)
 * @param apiRequestsPerMinute    sustained per-minute rate for general (non-execution) public API requests
 * @param burstMultiplier         token-bucket burst capacity as a multiple of the sustained rate
 * @param maxConcurrentExecutions concurrent execution slots per tenant; an async run holds a slot while queued and
 *                                running, a sync run while running
 * @param syncRunTimeout          wall-clock limit for a synchronous run
 * @param asyncRunTimeout         wall-clock limit for an asynchronous run
 * @param maxWorkspaces           number of workspaces the tenant may create
 * @param maxStorageBytes         total asset-file storage across the tenant
 * @param logRetentionDays        execution log retention window
 * @param maxMembers              number of member accounts
 *
 * @author Ivica Cardic
 */
public record PlanLimits(
    PlanTier tier,
    @Nullable BigDecimal includedMonthlyCostUsd,
    @Nullable Integer syncRequestsPerMinute,
    @Nullable Integer asyncRequestsPerMinute,
    @Nullable Integer apiRequestsPerMinute,
    int burstMultiplier,
    @Nullable Integer maxConcurrentExecutions,
    @Nullable Duration syncRunTimeout,
    @Nullable Duration asyncRunTimeout,
    @Nullable Integer maxWorkspaces,
    @Nullable Long maxStorageBytes,
    @Nullable Integer logRetentionDays,
    @Nullable Integer maxMembers) {

    public static final int DEFAULT_BURST_MULTIPLIER = 2;

    public PlanLimits {
        if (burstMultiplier < 1) {
            throw new IllegalArgumentException("burstMultiplier must be >= 1");
        }
    }

    /**
     * All limits unlimited — the self-hosted default and the behavior of every deployment before plan tiers existed.
     */
    public static PlanLimits unlimited(PlanTier tier) {
        return new PlanLimits(
            tier, null, null, null, null, DEFAULT_BURST_MULTIPLIER, null, null, null, null, null, null, null);
    }
}
