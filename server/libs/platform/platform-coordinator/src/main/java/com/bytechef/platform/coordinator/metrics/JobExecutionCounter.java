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

package com.bytechef.platform.coordinator.metrics;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * @author Matija Petanjek
 */
public class JobExecutionCounter {

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "MeterRegistry is intentionally shared as recommended by Micrometer/Spring.")
    private final MeterRegistry meterRegistry;

    public JobExecutionCounter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void process(JobStatusApplicationEvent jobStatusApplicationEvent, Job job) {
        Counter.builder("bytechef_job_execution")
            .tag("job_name", job.getLabel())
            .tag("job_status", jobStatusApplicationEvent.getStatus()
                .toString()
                .toLowerCase())
            .description("Inspect how many 'started' jobs ended with 'completed', 'stopped' or 'failed' status")
            .register(meterRegistry)
            .increment();
    }
}
