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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PlanLimitsTest {

    @Test
    void testUnlimitedHasNoLimitsSet() {
        PlanLimits planLimits = PlanLimits.unlimited(PlanTier.SELF_HOSTED);

        assertThat(planLimits.tier()).isEqualTo(PlanTier.SELF_HOSTED);
        assertThat(planLimits.includedMonthlyCostUsd()).isNull();
        assertThat(planLimits.syncRequestsPerMinute()).isNull();
        assertThat(planLimits.asyncRequestsPerMinute()).isNull();
        assertThat(planLimits.apiRequestsPerMinute()).isNull();
        assertThat(planLimits.burstMultiplier()).isEqualTo(PlanLimits.DEFAULT_BURST_MULTIPLIER);
        assertThat(planLimits.maxConcurrentExecutions()).isNull();
        assertThat(planLimits.syncRunTimeout()).isNull();
        assertThat(planLimits.asyncRunTimeout()).isNull();
        assertThat(planLimits.maxWorkspaces()).isNull();
        assertThat(planLimits.maxStorageBytes()).isNull();
        assertThat(planLimits.logRetentionDays()).isNull();
        assertThat(planLimits.maxMembers()).isNull();
    }

    @Test
    void testBurstMultiplierMustBePositive() {
        assertThatThrownBy(
            () -> new PlanLimits(
                PlanTier.FREE, null, null, null, null, 0, null, null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
    }
}
