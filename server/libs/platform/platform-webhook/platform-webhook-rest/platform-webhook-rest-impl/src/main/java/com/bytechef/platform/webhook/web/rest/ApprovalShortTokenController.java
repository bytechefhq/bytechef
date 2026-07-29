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
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mints a short opaque id for a tokenized resume id, used by approval channels whose interactive-button payload cannot
 * carry the whole token (Telegram / Discord). Anonymous — the caller is the approval channel itself, and the token it
 * submits is already the resume capability, so mapping it to a random id leaks nothing. The submitted token is
 * validated to parse as a resume id first, so the endpoint cannot be used to fill the store with arbitrary strings.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class ApprovalShortTokenController {

    private final ApprovalShortTokenStore approvalShortTokenStore;
    private final @Nullable ApprovalTokens approvalTokens;

    // approvalTokensObjectProvider.getIfAvailable() can throw if the ApprovalTokens bean is ambiguous, which
    // SpotBugs flags as a finalizer-attack vector (CT_CONSTRUCTOR_THROW). The class is final, so nothing can
    // subclass it to exploit a partially-constructed instance; the residual warning is safe to suppress.
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public ApprovalShortTokenController(
        ApprovalShortTokenStore approvalShortTokenStore, ObjectProvider<ApprovalTokens> approvalTokensObjectProvider) {

        this.approvalShortTokenStore = approvalShortTokenStore;
        this.approvalTokens = approvalTokensObjectProvider.getIfAvailable();
    }

    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "The submitted token is already the resume capability; mapping it to a random id leaks nothing")
    @PostMapping(
        value = "/approval/short-token", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> mint(@RequestBody String token) {
        if (token == null || token.isBlank() || !parses(token.trim())) {
            return ResponseEntity.badRequest()
                .build();
        }

        return ResponseEntity.ok(approvalShortTokenStore.mint(token.trim()));
    }

    private boolean parses(String token) {
        String innerToken = approvalTokens == null
            ? token
            : approvalTokens.resolveInnerToken(token)
                .orElse(null);

        if (innerToken == null) {
            return false;
        }

        try {
            JobResumeId.parse(innerToken);

            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
