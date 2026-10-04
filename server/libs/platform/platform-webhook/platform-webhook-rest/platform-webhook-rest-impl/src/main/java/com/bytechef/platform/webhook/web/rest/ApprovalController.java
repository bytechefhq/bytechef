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
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 * @deprecated This controller is deprecated and may be removed in a future version.
 */
@RestController
@CrossOrigin
@ConditionalOnCoordinator
@Deprecated
public class ApprovalController {

    private final ObjectProvider<ApprovalTokens> approvalTokensProvider;
    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI")
    public ApprovalController(ObjectProvider<ApprovalTokens> approvalTokensProvider, JobFacade jobFacade) {
        this.approvalTokensProvider = approvalTokensProvider;
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
        Optional<String> innerTokenOptional = resolveInnerToken(id);

        if (innerTokenOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                .build();
        }

        ApprovalId approvalId;

        try {
            approvalId = ApprovalId.parse(innerTokenOptional.get());
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            return ResponseEntity.badRequest()
                .build();
        }

        return TenantContext.callWithTenantId(approvalId.getTenantId(), () -> {
            jobFacade.resumeApproval(approvalId.getJobId(), approvalId.getUuidAsString(), approvalId.isApproved());

            return ResponseEntity.noContent()
                .build();
        });
    }

    private Optional<String> resolveInnerToken(String id) {
        ApprovalTokens approvalTokens = approvalTokensProvider.getIfAvailable();

        if (approvalTokens == null) {
            return Optional.of(id);
        }

        return approvalTokens.resolveInnerToken(id);
    }
}
