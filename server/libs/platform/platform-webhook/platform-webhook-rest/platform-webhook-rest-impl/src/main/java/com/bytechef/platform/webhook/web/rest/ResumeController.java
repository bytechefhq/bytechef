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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling suspended job resume callbacks.
 *
 * @author Ivica Cardic
 */
@RestController
@CrossOrigin
@ConditionalOnCoordinator
public class ResumeController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);

    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI")
    public ResumeController(JobFacade jobFacade) {
        this.jobFacade = jobFacade;
    }

    /**
     * Resumes a suspended job.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is intentionally disabled for this endpoint. Resume callbacks may come from
     * external sources (e.g., email links) that cannot include CSRF tokens. Security is maintained through
     * cryptographic resume tokens that are verified before processing.
     */
    @SuppressFBWarnings("SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING")
    @RequestMapping(method = {
        RequestMethod.GET, RequestMethod.POST
    }, value = "/job/resume/{id}")
    public ResponseEntity<Void> resume(@PathVariable String id) {
        JobResumeId jobResumeId;

        try {
            jobResumeId = JobResumeId.parse(id);
        } catch (IllegalArgumentException illegalArgumentException) {
            logger.warn("Invalid resume id: {}", id);

            return ResponseEntity.badRequest()
                .build();
        }

        return TenantContext.callWithTenantId(jobResumeId.getTenantId(), () -> {
            jobFacade.resumeJob(jobResumeId.getJobId());

            return ResponseEntity.noContent()
                .build();
        });
    }

}
