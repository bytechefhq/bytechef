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

/**
 * Resolves the effective plan limits for a tenant. The default implementation reads static configuration
 * ({@code bytechef.plan.*}); a SaaS billing integration replaces this bean to resolve limits from live subscription
 * state per tenant.
 *
 * <p>
 * Callers must never cache the returned value beyond a single operation — a tenant's plan can change at any time.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface PlanLimitsProvider {

    /**
     * Effective limits for the given tenant. Never returns null — a tenant without plan data gets
     * {@link PlanLimits#unlimited}.
     */
    PlanLimits getPlanLimits(String tenantId);
}
