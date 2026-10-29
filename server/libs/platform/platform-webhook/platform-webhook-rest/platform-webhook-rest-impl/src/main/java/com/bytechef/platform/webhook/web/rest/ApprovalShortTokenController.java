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
import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public final class ApprovalShortTokenController {

    private final ApprovalShortTokenService approvalShortTokenService;
    private final @Nullable ApprovalTokens approvalTokens;
    private final Clock clock;

    @Autowired
    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public ApprovalShortTokenController(
        ApprovalShortTokenService approvalShortTokenService,
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider, ObjectProvider<Clock> clockObjectProvider) {

        this.approvalShortTokenService = approvalShortTokenService;
        this.approvalTokens = approvalTokensObjectProvider.getIfAvailable();
        this.clock = clockObjectProvider.getIfAvailable(Clock::systemUTC);
    }

    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "The submitted token is already the resume capability; mapping it to a random id leaks nothing")
    @PostMapping(
        value = "/approval/short-token", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> mint(@RequestBody String token) {
        String trimmedToken = token == null ? "" : token.trim();

        Optional<Instant> expiryDateOptional = resolveExpiryDate(trimmedToken);

        if (expiryDateOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                .build();
        }

        return ResponseEntity.ok(approvalShortTokenService.mint(trimmedToken, expiryDateOptional.get()));
    }

    private Optional<Instant> resolveExpiryDate(String token) {
        if (approvalTokens == null || token.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> innerTokenOptional = approvalTokens.parseSignedToken(token);

        if (innerTokenOptional.isEmpty()) {
            return Optional.empty();
        }

        Instant expiryDate;

        try {
            JobResumeId.parse(innerTokenOptional.get());

            String[] tokenParts = token.split("\\.", -1);

            expiryDate = Instant.ofEpochSecond(Long.parseLong(tokenParts[1]));
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            return Optional.empty();
        }

        if (!expiryDate.isAfter(clock.instant())) {
            return Optional.empty();
        }

        return Optional.of(expiryDate);
    }
}
