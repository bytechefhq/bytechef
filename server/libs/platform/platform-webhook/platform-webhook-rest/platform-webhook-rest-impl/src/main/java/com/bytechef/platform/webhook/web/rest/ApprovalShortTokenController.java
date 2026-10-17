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
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
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

    private final @Nullable ApprovalTokens approvalTokens;

    @SuppressFBWarnings("EI")
    public ApprovalShortTokenController(ObjectProvider<ApprovalTokens> approvalTokensObjectProvider) {
        this.approvalTokens = approvalTokensObjectProvider.getIfAvailable();
    }

    @SuppressFBWarnings(
        value = "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING",
        justification = "The submitted token is already the resume capability; its short form leaks nothing")
    @PostMapping(
        value = "/approval/short-token", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> mint(@RequestBody String token) {
        String trimmedToken = token == null ? "" : token.trim();

        Optional<String> shortTokenOptional = approvalTokens == null
            ? Optional.empty() : approvalTokens.toShortToken(trimmedToken);

        if (shortTokenOptional.isEmpty()) {
            return ResponseEntity.badRequest()
                .build();
        }

        return ResponseEntity.ok(shortTokenOptional.get());
    }
}
