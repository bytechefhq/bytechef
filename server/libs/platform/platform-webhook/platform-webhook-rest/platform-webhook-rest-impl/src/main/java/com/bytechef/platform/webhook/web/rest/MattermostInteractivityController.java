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
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for Mattermost interactive message button callbacks resolving approvals in place — each button's
 * integration URL points here. Mattermost does not sign these callbacks, so authorization rests on the tokenized resume
 * id carried in the button's integration context, exactly the capability the hosted-form link already exposes; the
 * reviewer identity Mattermost reports is therefore not trusted and not recorded as {@code approvedBy}.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class MattermostInteractivityController {

    private static final Logger log = LoggerFactory.getLogger(MattermostInteractivityController.class);

    private final JobResumeFacade jobResumeFacade;
    private final @Nullable ApprovalTokens approvalTokens;

    // approvalTokensObjectProvider.getIfAvailable() can throw if the ApprovalTokens bean is ambiguous, which
    // SpotBugs flags as a finalizer-attack vector (CT_CONSTRUCTOR_THROW). The class is final, so nothing can
    // subclass it to exploit a partially-constructed instance; the residual warning is safe to suppress.
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public MattermostInteractivityController(
        JobResumeFacade jobResumeFacade, ObjectProvider<ApprovalTokens> approvalTokensObjectProvider) {

        this.jobResumeFacade = jobResumeFacade;
        this.approvalTokens = approvalTokensObjectProvider.getIfAvailable();
    }

    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "CSRF does not apply: Mattermost posts here directly; the resume token in the context is the "
            + "capability, same as the hosted-form link")
    @PostMapping(value = "/mattermost/interactivity", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleInteractivity(@RequestBody Map<String, Object> payload) {
        Object contextObject = payload.get("context");

        if (!(contextObject instanceof Map<?, ?> context)) {
            return ResponseEntity.ok(update("This approval request is malformed."));
        }

        Object resumeIdObject = context.get("resumeId");

        if (!(resumeIdObject instanceof String resumeId)) {
            return ResponseEntity.ok(update("This approval request is malformed."));
        }

        boolean approved = Boolean.TRUE.equals(context.get("approved"));

        String tenantId = resolveTenantId(resumeId);

        if (tenantId == null) {
            return ResponseEntity.ok(update("This approval is no longer available."));
        }

        JobResumeOutcome outcome = TenantContext.callWithTenantId(
            tenantId, () -> jobResumeFacade.resumeJob(resumeId, Map.of("approved", approved)));

        return ResponseEntity.ok(update(switch (outcome) {
            case OK -> approved ? ":white_check_mark: Approved." : ":no_entry_sign: Discarded.";
            case GONE -> ":hourglass: This approval expired before it was resolved.";
            case INVALID_ID -> ":warning: This approval is no longer available.";
        }));
    }

    private @Nullable String resolveTenantId(String resumeId) {
        String innerToken = approvalTokens == null
            ? resumeId
            : approvalTokens.resolveInnerToken(resumeId)
                .orElse(null);

        if (innerToken == null) {
            return null;
        }

        try {
            return JobResumeId.parse(innerToken)
                .getTenantId();
        } catch (Exception exception) {
            if (log.isDebugEnabled()) {
                log.debug("Could not parse the Mattermost approval resume id: {}", exception.getMessage());
            }

            return null;
        }
    }

    private static Map<String, Object> update(String message) {
        return Map.of("update", Map.of("message", message));
    }
}
