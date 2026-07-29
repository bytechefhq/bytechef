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

import com.bytechef.platform.plan.domain.PlanOveragePolicy;

/**
 * Resolves a tenant's on-demand overage terms for the monthly-cost cap. This is a stub SPI for the billing integration:
 * whether a tenant opted into overage — and how much unbilled overage is tolerated before the hard stop — is billing
 * state, so no default bean exists. Without a bean the cost cap keeps its pre-overage behavior and hard-stops at the
 * plan's included monthly cost.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface PlanOveragePolicyProvider {

    PlanOveragePolicy getOveragePolicy(String tenantId);
}
