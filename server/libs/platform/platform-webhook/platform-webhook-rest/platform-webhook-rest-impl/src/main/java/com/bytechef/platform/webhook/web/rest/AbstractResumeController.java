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

package com.bytechef.platform.webhook.web.rest;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Shared logic for parsing a {@link JobResumeId} and resuming the corresponding suspended job within the matching
 * tenant context. Subclasses provide the request mapping that exposes this operation to a specific transport (anonymous
 * webhook callback vs. API-key-protected endpoint).
 *
 * @author Ivica Cardic
 */
abstract class AbstractResumeController {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResumeController.class);

    protected final JobFacade jobFacade;
    protected final JobService jobService;

    @SuppressFBWarnings("EI")
    protected AbstractResumeController(JobFacade jobFacade, JobService jobService) {
        this.jobFacade = jobFacade;
        this.jobService = jobService;
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    protected ResponseEntity<Void> doResume(String id, Map<String, Object> data) {
        JobResumeId jobResumeId;

        try {
            jobResumeId = JobResumeId.parse(id);
        } catch (IllegalArgumentException illegalArgumentException) {
            logger.warn("Invalid resume id: {}", id.replaceAll("[\\r\\n]", ""));

            return ResponseEntity.badRequest()
                .build();
        }

        return TenantContext.callWithTenantId(jobResumeId.getTenantId(), () -> {
            Job job = jobService.getJob(jobResumeId.getJobId());

            if (job.getStatus() != Job.Status.STOPPED) {
                logger.warn("Cannot resume job {}; status is {}", jobResumeId.getJobId(), job.getStatus());

                return ResponseEntity.status(HttpStatus.GONE)
                    .build();
            }

            jobFacade.resumeJob(jobResumeId.getJobId(), data);

            return ResponseEntity.noContent()
                .build();
        });
    }
}
