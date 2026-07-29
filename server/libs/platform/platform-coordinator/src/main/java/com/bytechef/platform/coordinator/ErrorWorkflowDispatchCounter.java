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

package com.bytechef.platform.coordinator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.Nullable;

/**
 * Counts error-workflow dispatch outcomes. No workspace or project tag: this fires on every failed run, so the tag set
 * stays bounded.
 *
 * @author Ivica Cardic
 */
public class ErrorWorkflowDispatchCounter {

    private final @Nullable MeterRegistry meterRegistry;

    @SuppressFBWarnings("EI2")
    public ErrorWorkflowDispatchCounter(@Nullable MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String outcome) {
        if (meterRegistry == null) {
            return;
        }

        Counter.builder("bytechef_error_workflow_dispatch")
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();
    }
}
