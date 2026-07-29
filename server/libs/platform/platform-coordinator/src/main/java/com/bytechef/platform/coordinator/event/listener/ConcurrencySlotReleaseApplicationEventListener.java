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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.platform.ratelimit.ConcurrentExecutionGate;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.EnumSet;
import java.util.Set;

/**
 * Releases the plan-level concurrent-execution slot acquired at job admission
 * ({@code PrincipalJobFacadeImpl.createJob}) when the job reaches terminal status. Joins the coordinator's existing
 * listener fan-out — nothing under {@code server/libs/atlas/} changes. The gate floors at zero, so redelivered terminal
 * events and jobs admitted before enforcement was enabled are harmless.
 *
 * @author Ivica Cardic
 */
public class ConcurrencySlotReleaseApplicationEventListener implements ApplicationEventListener {

    private static final Set<Job.Status> TERMINAL_STATUSES = EnumSet.of(
        Job.Status.COMPLETED, Job.Status.FAILED, Job.Status.STOPPED, Job.Status.CANCELLED);

    private final ConcurrentExecutionGate concurrentExecutionGate;

    @SuppressFBWarnings("EI2")
    public ConcurrencySlotReleaseApplicationEventListener(ConcurrentExecutionGate concurrentExecutionGate) {
        this.concurrentExecutionGate = concurrentExecutionGate;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent &&
            TERMINAL_STATUSES.contains(jobStatusApplicationEvent.getStatus())) {

            concurrentExecutionGate.release("executions:" + TenantContext.getCurrentTenantId());
        }
    }
}
