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

package com.bytechef.platform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PlanLimitRejectionCounterTest {

    @Test
    void testIncrementTagsTheRejectedLimit() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        PlanLimitRejectionCounter planLimitRejectionCounter = new PlanLimitRejectionCounter(meterRegistry);

        planLimitRejectionCounter.increment("async");
        planLimitRejectionCounter.increment("async");
        planLimitRejectionCounter.increment("cost");

        assertThat(
            meterRegistry.counter(PlanLimitRejectionCounter.METRIC_NAME, "limit", "async")
                .count()).isEqualTo(2.0);
        assertThat(
            meterRegistry.counter(PlanLimitRejectionCounter.METRIC_NAME, "limit", "cost")
                .count()).isEqualTo(1.0);
    }

    @Test
    void testMissingMeterRegistryIsANoOp() {
        PlanLimitRejectionCounter planLimitRejectionCounter = new PlanLimitRejectionCounter(null);

        assertDoesNotThrow(() -> planLimitRejectionCounter.increment("login"));
    }
}
