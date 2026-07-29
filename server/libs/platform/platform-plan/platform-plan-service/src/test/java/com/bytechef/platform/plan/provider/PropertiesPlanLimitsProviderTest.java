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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.plan.config.PlanProperties;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanTier;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PropertiesPlanLimitsProviderTest {

    @Test
    void testDefaultTierIsUnlimited() {
        PropertiesPlanLimitsProvider provider = new PropertiesPlanLimitsProvider(
            new PlanProperties(PlanTier.SELF_HOSTED, emptyOverrides()));

        assertThat(provider.getPlanLimits("public")).isEqualTo(PlanLimits.unlimited(PlanTier.SELF_HOSTED));
    }

    @Test
    void testTierActivatesPlaceholderTable() {
        PropertiesPlanLimitsProvider provider = new PropertiesPlanLimitsProvider(
            new PlanProperties(PlanTier.FREE, emptyOverrides()));

        assertThat(provider.getPlanLimits("public")).isEqualTo(DefaultPlanLimits.forTier(PlanTier.FREE));
    }

    @Test
    void testOverrideReplacesSingleFieldOnly() {
        PlanProperties.Limits overrides = new PlanProperties.Limits(
            null, 75, null, null, null, null, null, null, null, null, null, null);

        PropertiesPlanLimitsProvider provider = new PropertiesPlanLimitsProvider(
            new PlanProperties(PlanTier.FREE, overrides));

        PlanLimits planLimits = provider.getPlanLimits("public");

        assertThat(planLimits.syncRequestsPerMinute()).isEqualTo(75);
        assertThat(planLimits.asyncRequestsPerMinute()).isEqualTo(200);
        assertThat(planLimits.maxConcurrentExecutions()).isEqualTo(10);
    }

    @Test
    void testNullOverridesFallBackToTierTable() {
        PropertiesPlanLimitsProvider provider = new PropertiesPlanLimitsProvider(
            new PlanProperties(PlanTier.TEAM, null));

        assertThat(provider.getPlanLimits("public")).isEqualTo(DefaultPlanLimits.forTier(PlanTier.TEAM));
    }

    private static PlanProperties.Limits emptyOverrides() {
        return new PlanProperties.Limits(
            null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
