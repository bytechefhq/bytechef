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
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@CrossOrigin
@ConditionalOnCoordinator
public class ApprovalController {

    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI")
    public ApprovalController(JobFacade jobFacade) {
        this.jobFacade = jobFacade;
    }

    /**
     * Security Note: SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING - CSRF protection is intentionally disabled for this
     * endpoint. Approval links are sent via email with cryptographically secure tokens (UUID) embedded in the URL. The
     * token itself provides authentication, and this endpoint must be accessible without a session.
     */
    @SuppressFBWarnings("SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING")
    @RequestMapping(method = {
        RequestMethod.GET, RequestMethod.POST
    }, value = "/approvals/{id}")
    public ResponseEntity<Void> approve(@PathVariable String id) {
        ApprovalId approvalId = ApprovalId.parse(id);

        return TenantContext.callWithTenantId(approvalId.getTenantId(), () -> {
            jobFacade.resumeApproval(approvalId.getJobId(), approvalId.getUuidAsString(), approvalId.isApproved());

            return ResponseEntity.noContent()
                .build();
        });
    }
}
