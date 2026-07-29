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

import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanTier;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Pins the Sim-modeled placeholder tables so an accidental edit shows up as a test diff, not a silent limit change.
 *
 * @author Ivica Cardic
 */
class DefaultPlanLimitsTest {

    @Test
    void testSelfHostedIsUnlimited() {
        assertThat(DefaultPlanLimits.forTier(PlanTier.SELF_HOSTED))
            .isEqualTo(PlanLimits.unlimited(PlanTier.SELF_HOSTED));
    }

    @Test
    void testFreeTierTable() {
        PlanLimits planLimits = DefaultPlanLimits.forTier(PlanTier.FREE);

        assertThat(planLimits.includedMonthlyCostUsd()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(planLimits.syncRequestsPerMinute()).isEqualTo(50);
        assertThat(planLimits.asyncRequestsPerMinute()).isEqualTo(200);
        assertThat(planLimits.apiRequestsPerMinute()).isEqualTo(30);
        assertThat(planLimits.maxConcurrentExecutions()).isEqualTo(10);
        assertThat(planLimits.syncRunTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(planLimits.asyncRunTimeout()).isEqualTo(Duration.ofMinutes(90));
        assertThat(planLimits.maxWorkspaces()).isEqualTo(1);
        assertThat(planLimits.maxStorageBytes()).isEqualTo(5L * 1024 * 1024 * 1024);
        assertThat(planLimits.logRetentionDays()).isEqualTo(7);
        assertThat(planLimits.maxMembers()).isEqualTo(1);
    }

    @Test
    void testProTierTable() {
        PlanLimits planLimits = DefaultPlanLimits.forTier(PlanTier.PRO);

        assertThat(planLimits.includedMonthlyCostUsd()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(planLimits.syncRequestsPerMinute()).isEqualTo(150);
        assertThat(planLimits.asyncRequestsPerMinute()).isEqualTo(1000);
        assertThat(planLimits.apiRequestsPerMinute()).isEqualTo(100);
        assertThat(planLimits.maxConcurrentExecutions()).isEqualTo(50);
        assertThat(planLimits.syncRunTimeout()).isEqualTo(Duration.ofMinutes(50));
        assertThat(planLimits.maxWorkspaces()).isEqualTo(3);
        assertThat(planLimits.logRetentionDays()).isNull();
    }

    @Test
    void testTeamTierTable() {
        PlanLimits planLimits = DefaultPlanLimits.forTier(PlanTier.TEAM);

        assertThat(planLimits.includedMonthlyCostUsd()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(planLimits.syncRequestsPerMinute()).isEqualTo(300);
        assertThat(planLimits.asyncRequestsPerMinute()).isEqualTo(2500);
        assertThat(planLimits.apiRequestsPerMinute()).isEqualTo(200);
        assertThat(planLimits.maxConcurrentExecutions()).isEqualTo(200);
        assertThat(planLimits.maxWorkspaces()).isEqualTo(10);
        assertThat(planLimits.maxMembers()).isNull();
    }

    @Test
    void testEnterpriseTierTable() {
        PlanLimits planLimits = DefaultPlanLimits.forTier(PlanTier.ENTERPRISE);

        assertThat(planLimits.includedMonthlyCostUsd()).isNull();
        assertThat(planLimits.syncRequestsPerMinute()).isEqualTo(600);
        assertThat(planLimits.asyncRequestsPerMinute()).isEqualTo(5000);
        assertThat(planLimits.apiRequestsPerMinute()).isEqualTo(500);
        assertThat(planLimits.maxConcurrentExecutions()).isEqualTo(1000);
        assertThat(planLimits.maxWorkspaces()).isNull();
    }
}
