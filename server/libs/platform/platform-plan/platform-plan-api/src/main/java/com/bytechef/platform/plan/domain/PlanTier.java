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

/**
 * Commercial plan tier a tenant is on. {@code SELF_HOSTED} is the default for every deployment that has not opted into
 * plan-based limits (all limits unlimited — the pre-plan behavior). The remaining tiers carry placeholder limit tables
 * in {@code DefaultPlanLimits}; a SaaS billing integration is expected to map its subscription state onto these values
 * through its own {@code PlanLimitsProvider} bean.
 *
 * <p>
 * Not persisted anywhere yet — if this enum ever gets stored as an ordinal, move it under an ordinal-stability pin
 * first.
 * </p>
 *
 * @author Ivica Cardic
 */
public enum PlanTier {

    SELF_HOSTED,
    FREE,
    PRO,
    TEAM,
    ENTERPRISE
}
