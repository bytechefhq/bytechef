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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.Nullable;

/**
 * Emits the {@code bytechef_plan_limit_rejection} counter with a {@code limit} tag naming the enforcement point that
 * refused the request, so plan-limit rejections are visible to operators instead of happening silently. Tag values in
 * use: {@code login}, {@code sync}, {@code api}, {@code preauth}, {@code resume} (the HTTP filter), {@code async},
 * {@code concurrency}, {@code cost} (job admission), {@code timeout} (the run-timeout monitor), and the quota gates
 * {@code workspace}, {@code member} and {@code storage}. Wired with an optional {@link MeterRegistry} so lightweight
 * app variants without actuator start cleanly — without one every call is a no-op.
 *
 * @author Ivica Cardic
 */
public class PlanLimitRejectionCounter {

    public static final String METRIC_NAME = "bytechef_plan_limit_rejection";

    @Nullable
    private final MeterRegistry meterRegistry;

    @SuppressFBWarnings("EI2")
    public PlanLimitRejectionCounter(@Nullable MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void increment(String limit) {
        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(METRIC_NAME, "limit", limit)
            .increment();
    }
}
