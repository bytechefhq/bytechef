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
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public controller for anonymous suspended-job resume webhook callbacks. Authentication is provided by the
 * cryptographically signed token embedded in the path, so the endpoint lives outside the {@code /api/**} namespace —
 * alongside other anonymous webhook-style callbacks — and is permitted in the Order-4 filter chain where CSRF is
 * disabled by convention.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class JobResumeController {

    private final JobResumeFacade jobResumeFacade;

    @SuppressFBWarnings("EI")
    public JobResumeController(JobResumeFacade jobResumeFacade) {
        this.jobResumeFacade = jobResumeFacade;
    }

    /**
     * Resumes a suspended job via anonymous webhook callback (typically submitted by the approval form in the SPA).
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is not required for this endpoint. Resume callbacks may come from external
     * sources (e.g., email links) that cannot include CSRF tokens. Security is maintained through cryptographic resume
     * tokens that are verified before processing.
     */
    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF disabled for external resume callbacks")
    @RequestMapping(method = {
        RequestMethod.GET, RequestMethod.POST
    }, value = "/job/resume/{id}")
    public ResponseEntity<Void> resume(
        @PathVariable String id, @RequestBody(required = false) Map<String, Object> data) {

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(id, data);

        return switch (outcome) {
            case OK -> ResponseEntity.noContent()
                .build();
            case INVALID_ID -> ResponseEntity.badRequest()
                .build();
            case GONE -> ResponseEntity.status(HttpStatus.GONE)
                .build();
        };
    }
}
