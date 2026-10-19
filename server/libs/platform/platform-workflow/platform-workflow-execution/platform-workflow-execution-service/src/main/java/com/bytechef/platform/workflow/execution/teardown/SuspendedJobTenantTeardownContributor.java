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

package com.bytechef.platform.workflow.execution.teardown;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Cancels the one-time resume task of every suspended job of the tenant.
 *
 * @author Ivica Cardic
 */
@Component
@Order(300)
class SuspendedJobTenantTeardownContributor implements TenantTeardownContributor {

    private static final Logger log = LoggerFactory.getLogger(SuspendedJobTenantTeardownContributor.class);

    private final JobService jobService;
    private final TriggerScheduler triggerScheduler;

    @SuppressFBWarnings("EI")
    SuspendedJobTenantTeardownContributor(JobService jobService, TriggerScheduler triggerScheduler) {
        this.jobService = jobService;
        this.triggerScheduler = triggerScheduler;
    }

    @Override
    public List<String> onDeletionRequested(String tenantId) {
        List<String> failures = new ArrayList<>();

        for (long jobId : jobService.getJobIds(Job.Status.STOPPED)) {
            try {
                triggerScheduler.cancelOneTimeTask(jobId);
            } catch (RuntimeException e) {
                log.error("Unable to cancel the one-time task of job {} of tenant {}", jobId, tenantId, e);

                failures.add("job " + jobId + ": " + e.getMessage());
            }
        }

        return failures;
    }
}
