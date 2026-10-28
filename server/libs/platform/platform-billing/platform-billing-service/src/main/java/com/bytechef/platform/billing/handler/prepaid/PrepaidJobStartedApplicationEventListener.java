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

package com.bytechef.platform.billing.handler.prepaid;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Temporary: counts one prepaid unit when a job is put in {@code STARTED} state. The next session replaces this
 * listener with a synchronous check at the STARTED transition that can also block jobs;
 * {@link PrepaidUnitConsumptionService#consumeUnit(long)} is what gets reused.
 *
 * <p>
 * Known gaps: jobs run on the sync path may not reach this listener, and a job whose first task throws publishes no
 * STARTED event and is therefore not counted.
 *
 * @author Matija Petanjek
 */
@Component
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class PrepaidJobStartedApplicationEventListener implements ApplicationEventListener {

    private final PrepaidUnitConsumptionService prepaidUnitConsumptionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PrepaidJobStartedApplicationEventListener(PrepaidUnitConsumptionService prepaidUnitConsumptionService) {
        this.prepaidUnitConsumptionService = prepaidUnitConsumptionService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent &&
            jobStatusApplicationEvent.getStatus() == Job.Status.STARTED) {

            prepaidUnitConsumptionService.consumeUnit(jobStatusApplicationEvent.getJobId());
        }
    }
}
