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
import com.bytechef.atlas.execution.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-key-protected controller for suspended-job resume callbacks from trusted external services (e.g. the target of a
 * wait-on-webhook action). Authentication is enforced by the EE {@code PlatformApiKeySecurityConfigurer}, which
 * requires a {@code Bearer} token on paths matching {@code /api/platform/v[0-9]+/.+}.
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/v1")
@ConditionalOnCoordinator
public class ResumeApiController extends AbstractResumeController {

    public ResumeApiController(JobFacade jobFacade, JobService jobService) {
        super(jobFacade, jobService);
    }

    /**
     * Resumes a suspended job via API-key-authenticated callback. CSRF is already disabled for this path by
     * {@code PlatformApiKeySecurityConfigurer.registerCsrfOverride}.
     */
    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF disabled for authenticated API callbacks")
    @RequestMapping(method = {
        RequestMethod.GET, RequestMethod.POST
    }, value = "/job/resume/{id}")
    public ResponseEntity<Void> resume(
        @PathVariable String id, @RequestBody(required = false) Map<String, Object> data) {

        return doResume(id, data);
    }
}
